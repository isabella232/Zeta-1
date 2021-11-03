<template>
  <div class="sheet-view-container" v-loading="flag.metaSheetLoading">
    <el-alert :title="syncItem.title" :type="syncItem.type" v-if="showMsgInfo" class="sync-alert" @close="closeAlert"></el-alert>
    <div class="header">
      <span class="name">{{metasheet.sheetName}}</span>
      <div class="tool">
        <span class="sync-time">{{syncTime}}</span>
        <el-button type="text" class="btn" @click="refresh()" title="Refresh" :disabled="disableSave">
          <i class="op-icon el-icon-refresh"></i>
        </el-button>
        <el-button type="text" class="btn" v-if="shouldShowSchema" @click="editMetaConfig(metaSheetRow)" title="Schema Config" :disabled="disableSave" >
          <i class="op-icon zeta-icon-zs-config"></i>
        </el-button>
        <el-popover
          placement="bottom"
          trigger="click"
          visible-arrow="true"
          width="370"
          :disabled="disableSave">
          <div v-if="!disableSave">
              <meta-share :mode="rowMode(metaSheetRow)" :sheet-id="metasheetId"/>
          </div>
          <span slot="reference" title="Share"><i class="op-icon zeta-icon-share" :class="{disabled: disableSave}"></i></span>
        </el-popover>
        <el-button plain class="btn" v-if="isProdViewMode && isSyncAvaliable()" @click="sync" v-click-metric:ZS_CLICK="{name: 'Sync'}" :loading="flag.metaSheetSyncing" :disabled="disableSave">
          Sync
        </el-button>
        <el-popover
          placement="bottom"
          trigger="click"
          visible-arrow="true"
          width="350"
          v-model="flag.popoverShow"
          :disabled="disableSave"
          v-if="!isProdViewMode && isSyncAvaliable()">
          <div v-if="flag.popoverShow">
            <meta-source-config
            :mode="rowMode(metaSheetRow)"
            :config="sourceConfig"
            :field-editable="sourceConfigEditable"
            :is-creator="isCreator"
            :is-whitelist="isWhiteList"
            @close="close"/>
            <div class="btn-group">
              <el-button type="primary" class="btn" :disabled="disableApply" @click="sync" v-click-metric:ZS_CLICK="{name: 'Sync'}">Apply</el-button>
              <el-button plain @click="close" class="btn">Cancel</el-button>
            </div>
          </div>
          <el-button plain slot="reference" class="btn" :loading="flag.metaSheetSyncing" :disabled="disableSave">
            Sync
          </el-button>
        </el-popover>
        <el-button type="primary" class="btn save-btn" @click="save()" v-if="!isSheetReadyOnly" :disabled="disableSave">Save</el-button>
      </div>
    </div>
    <div ref="spreadsheet" id="spreadsheet"></div>
    <el-dialog
      :visible.sync="flag.showMetaConfigTable"
      :title="title"
      :close-on-click-modal="flag.closeOnClickModal"
      custom-class="meta-config-dialog">
      <div v-if="flag.showMetaConfigTable">
        <meta-config-view
        v-if="isViewMode"
        :mode="mode"
        :config="metaSheetConfig"
        />
        <meta-config-edit
        v-else
        :mode="mode"
        :config="metaSheetConfig"
        @updated="updateSchemaCb"
        @cancel="cancel"/>
      </div>
    </el-dialog>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Inject, Prop, Ref, Watch, Provide,Mixins } from "vue-property-decorator";
import { MetaConfigTableRow, MetaSheetTableRow, MetaSheetConfig,Mode, MODE, PLATFORM, ACCESS, MS_SYS, MSG_TYPE, MS_STATUS, SourceConfig} from '@/types/meta-sheet';
import MetaShare from './meta-share.vue';
import _ from "lodash";
import Handsontable from 'handsontable';
import MetaSheetService from '@/services/remote/MetaSheetService';
import MetaSheetMixin from './meta-sheet-mixin';
import MetaConfigEdit from './meta-config-edit.vue';
import MetaConfigView from './meta-config-view.vue';
import MetaSourceConfig from './source-config.vue';
import MetaSheetUtil, {_ID_, _VERSION_} from './util';
import moment from 'moment';
let decodeHTML = (html) =>{
	let txt = document.createElement('textarea');
	txt.innerHTML = html;
	return txt.value;
};
@Component({
  components: {
    MetaConfigEdit,
    MetaConfigView,
    MetaShare,
    MetaSourceConfig
  }
})
export default class MetaSheetView extends Mixins(MetaSheetMixin) {
  @Prop() metasheetId: string;
  @Ref('spreadsheet') tableEl: HTMLElement;
  @Provide('metaSheetService') metaSheetService  = new MetaSheetService();
  sheet: Handsontable;
  removeRows = new Array<Object>();
  editRowIds = new Set<string>();
  debounceRender: Function;
  metaSheetData:any = [];
  scrollLeft = 0;
  scrollTop = 0;
  syncItem = {
    title: '',
    type: MSG_TYPE.WARNING
  }
  interval: NodeJS.Timer | null = null;
  isWhiteList = false;

  constructor() {
    super();
    this.debounceRender = _.debounce(this.renderSheet, 300, { trailing: true });
  }

  get metasheet() {
    if(this.$store.state.workspace.workspaces[this.metasheetId]) {
        return this.$store.state.workspace.workspaces[this.metasheetId];
    }
    return {};
  }

  get sourceConfig() :SourceConfig {
    return this.metaSheetRow.sourceConfig!;
  }

  get syncTime() {
    if(this.metaSheetRow && this.metaSheetRow.syncTime) {
      let $moment = moment(this.metaSheetRow.syncTime);
      if($moment.isValid()) {
        let formatTime = moment(this.metaSheetRow.syncTime!).utcOffset('-07:00').format('YYYY-MM-DD HH:mm:ss');
        return `Last sync time: ${formatTime} MST`;
      }
    }
    return '';
  }

  get isCreator() {
    return this.metaSheetRow.access == ACCESS.CREATOR;
  }


  get columns() {
    return this.metasheet.columns;
  }

  get isSheetReadyOnly() {
    if(!this.flag.editable) {
      return true;
    }
    if(this.metaSheetRow) {
      return (this.metaSheetRow.access == ACCESS.READER) ? true: false;
    }
    return true;
  }

  get sourceConfigEditable() {
    return this.metaSheetRow.metaTableStatus == MS_STATUS.REGISTER_FAIL
      || this.metaSheetRow.metaTableStatus == MS_STATUS.CREATED;
  }

  get headers() {
    return _.map(this.metasheet.columns, d => d.column);
  }

  get colHeaders() {
    return [_ID_, _VERSION_, ...this.headers];
  }

  get metaSheetRow() {
    let metaSheetRow :MetaSheetTableRow= this.$store.getters.metasheetById(this.metasheetId);
    if(metaSheetRow) {
      this.setSyncItem(metaSheetRow.metaTableStatus!, metaSheetRow.failLog);
    }
    return metaSheetRow || {};
  }

  get showMsgInfo() {
    return this.metaSheetRow.metaTableStatus == MS_STATUS.SYNC ||
        this.flag.syncClicked && (
        this.metaSheetRow.metaTableStatus == MS_STATUS.SUCCESS
        || this.metaSheetRow.metaTableStatus == MS_STATUS.REGISTER_FAIL
        || this.metaSheetRow.metaTableStatus == MS_STATUS.LOAD_FAIL
    )
  }

  get disableApply() {
    let valid = false;
    let sourceConfig = this.sourceConfig;
    if(sourceConfig) {
      if(sourceConfig.metaTableType == MS_SYS.HDM) {
        valid = !!sourceConfig.db && !!sourceConfig.tbl;
      } else {
        valid = !!sourceConfig.db && !!sourceConfig.tbl && !!sourceConfig.platform && !!sourceConfig.account;
      }
    }
    return !valid;
  }

  get disableSave() {
    return this.metaSheetRow.metaTableStatus == MS_STATUS.SYNC;
  }

  get columnTypeMap() {
    return this.metasheet.columnTypeMap;
  }

  get isProdViewMode() {
    return this.metaSheetRow.sourceConfig && this.metaSheetRow.sourceConfig.metaTableType == MS_SYS.PROD && this.metaSheetRow.fullTableName;
  }

  get isProdWhiteListViewMode() {
    return this.isWhiteList && this.isProdViewMode;
  }

  get shouldShowSchema() {
      // 1. Not Sync yet
      // 2. Sync and in HDM mode
      // 3. Sync and in PROD mode && in white list
    return (this.metaSheetRow.sourceConfig && (this.metaSheetRow.sourceConfig.metaTableType == MS_SYS.HDM || !this.metaSheetRow.sourceConfig.metaTableType))
      || this.isProdWhiteListViewMode;
  }

  @Watch("isSheetReadyOnly")
  sheetReadOnlyChange(newVal, oldVal) {
    if(newVal === false) {
      if(this.sheet && !this.sheet.isDestroyed) {
        this.sheet.destroy();
      }
      this.debounceRender();
    }
  }

  @Watch('columns')
  columnsChange(newVal: string[], oldVal){
    if(newVal && newVal.length > 0) {
      if(!(_.differenceWith(newVal, oldVal, _.isEqual).length == 0 && _.differenceWith(oldVal, newVal, _.isEqual).length == 0)) {
        this.refresh();
      }
    }
  }

  isSyncAvaliable() {
    if(this.metaSheetRow && this.metaSheetRow.access == ACCESS.CREATOR) {
      return true;
    }
    if(this.metaSheetRow && this.metaSheetRow.access == ACCESS.OWNER &&
        this.metaSheetRow.fullTableName != '') {
      return true;
    }
    return false;
  }

  closeAlert() {
    let alertDiv = (this.$el as HTMLElement).querySelector(".sync-alert");
    alertDiv && ((alertDiv as HTMLElement).style.display = 'none');
  }

  resetEditRows() {
    this.removeRows = new Array<Object>();
    this.editRowIds = new Set<string>();
  }

  renderSheet() {
    if(this.sheet && !this.sheet.isDestroyed){
      this.sheet.render();
      this.restoreScroll();
    }
    else{
      this.hook();
    }
    this.flag.metaSheetLoading = false;
  }

  hook() {
    let spreadsheetEl = document.querySelector('#spreadsheet');
    if(spreadsheetEl) {
      const rowsLimit = 20000;
      this.sheet = new Handsontable(this.tableEl, {
        licenseKey: 'non-commercial-and-evaluation',
        // readOnly: this.isSheetReadyOnly,
        data: this.metaSheetData,
        rowHeaders: true,
        colHeaders: this.colHeaders,
        colWidths: _.times(this.headers.length + 2, () => 100),
        filters: false,
        height: '90%',
        contextMenu: ['row_above','row_below','remove_row'],
        columnSorting: false,
        // search: true,
        hiddenColumns: {
          columns: [0, 1],
          indicators: false
        },
        autoRowSize: false,
        autoColumnSize: false,
        manualColumnResize: true,
        undo: true,
        rowHeights: 23,
        maxCols: this.colHeaders.length,
        copyPaste:{
          rowsLimit: rowsLimit
        },
        beforeRemoveRow: (index, amount, physicalRows) => {
          physicalRows.forEach(row => {
            let rowItem = this.sheet.getDataAtRow(row);
            if(!_.isNil(rowItem[0])) {
              let option = {};
              this.colHeaders.forEach((value, index) => {
                option[value] = this.trimValue(rowItem[index]);
              })
              this.removeRows.push(option);
            }
          })
        },
        beforeChange: (changes, source) => {
          if(source == 'edit' && this.columnTypeMap[changes[0][1]]) {
            _.each(changes, d => {
              if(d[3]) {
                d[3] = this.toThousands(d[3]);
              }
            });
          }
        },
        afterChange: (changes) => {
          if(changes) {
            changes.forEach(change => {
              let number = change[0];
              let rowItem = this.sheet.getDataAtRow(number);
              let id = rowItem[0];
              if(!_.isNil(id)) {
                this.editRowIds.add(id);
              }
            })
          }
        },
        afterScrollHorizontally: () => {
          let $wtHolders= this.tableEl.getElementsByClassName("wtHolder");
          if($wtHolders && $wtHolders[0]) {
            this.scrollLeft = $wtHolders[0].scrollLeft || 0;
          }
        },
        afterScrollVertically: () => {
          let $wtHolders= this.tableEl.getElementsByClassName("wtHolder");
          if($wtHolders && $wtHolders[0]) {
            this.scrollTop = $wtHolders[0].scrollTop || 0;
          }
        },
        afterColumnResize: () => {
          setTimeout(() => {
            this.sheet.render()
          }, 20);
        },
        beforePaste: (changes) => {
          if(changes.length > rowsLimit) {
            changes.splice(rowsLimit, changes.length - rowsLimit);
          }
          for (let i = 0; i < changes.length; i++) {
            for (let j = 0; j < changes[i].length; j++) {
              changes[i][j] = decodeHTML(changes[i][j]);
            }
          }
        },
        afterBeginEditing: (row, column) => {
          let $input = document.querySelector(".handsontableInput");
          if($input) {
            if(this.isSheetReadyOnly) {
              ($input as HTMLTextAreaElement).setAttribute('readonly', 'true');
            } else {
              ($input as HTMLTextAreaElement).removeAttribute('readonly');
            }
            if(this.columnTypeMap[column]) {
              let val = ($input as HTMLTextAreaElement).value;
              ($input as HTMLTextAreaElement).value = val.replace(/,/g, '');
            }
          }
        },
        afterRedo: (val:any) => {
          let {index, data, actionType} = val;
          if(actionType == 'remove_row') {
            _.each(data, row => {
              let option = {};
              this.colHeaders.forEach((value, index) => {
                option[value] = this.trimValue(row[index]);
              })
              this.removeRows.push(option);
            })
            this.removeRows = _.uniqBy(this.removeRows, _ID_);
          }
        },
        afterUndo: (val:any) => {
          let {index, data, actionType} = val;
          if(actionType == 'remove_row') {
            _.each(data, row => {
              _.remove(this.removeRows, d => d[_ID_] == row[0]);
            })
          }
        },
      });
      this.restoreScroll();
    }
  }

  restoreScroll() {
    let $wtHolders= this.tableEl.getElementsByClassName("wtHolder");
    if($wtHolders && $wtHolders[0]) {
      $wtHolders[0].scrollLeft = this.scrollLeft;
      $wtHolders[0].scrollTop = this.scrollTop;
    }
  }

  processedData(number) {
    let data = new Array(number);
    for(let i = 0; i < number; i++) {
      data[i] = [null,null]; //_ID_, _VERSION_
      for(let j = 0; j < this.headers.length; j++) {
        data[i].push(null)
      }
    }
    return data;
  }

  async refresh(isSync = false) {
    if(this.sheet && !this.sheet.isDestroyed) {
      this.sheet.destroy();
    }
    this.resetEditRows();
    await this.getMetaSheetData(isSync);
  }

  async getMetaSheetData(initialLoad = false) {
    this.flag.metaSheetLoading = true;
    let sheetData:any = [];
    try {
      let response = await this.metaSheetService.getMetaSheetData(this.metasheetId);
      if(MetaSheetUtil.isOk(response)) {
        let {zetaMetaTable, data} = response.data.content;
        sheetData = data;
        this.flag.editable = true;
        let metaSheet =  MetaSheetUtil.processSingleItem(zetaMetaTable);
        if(!initialLoad) {
          this.$store.dispatch('updateMetaSheet', metaSheet);
          this.$store.dispatch('updateWSMetaSheet',metaSheet);
        }
        if(metaSheet.metaTableStatus != MS_STATUS.CREATED) {
          this.getSyncStatus();
        }
      }
    }
    catch(e) {
      this.flag.metaSheetLoading = false;
      this.flag.editable = false;
    }
    sheetData = _.map(sheetData, (d:any) => {
      let arr:any = [];
      arr.push(d[_ID_]);
      arr.push(d[_VERSION_]);
      for(let j = 0; j < this.headers.length; j++) {
        if(this.columnTypeMap[j+2]) {
          arr.push(this.toThousands(d[this.headers[j]]))
        } else {
          arr.push(d[this.headers[j]])
        }
      }
      return arr;
    })
    let newRows = this.processedData(100);
    this.metaSheetData = sheetData.concat(newRows);
    this.debounceRender();
  }

  trimValue(value) {
    if(!_.isNil(value)) {
      value = _.trim(value);
    }
    return value;
  }

  toThousands(num) {
    if(!_.isNil(num) && !_.isNaN(Number(num))) {
      let numStr = num + '';
      let splits = numStr.split(".");
      let value = splits[0];
      let decimal = splits[1];
      let result = value.replace(/(\d)(?=(?:\d{3})+$)/g, '$1,') ;
      if(splits.length > 1) {
        result = result + "." + decimal;
      }
      return result;
    }
    return num;
  }

  getSaveParams() {
    let sheetData = this.sheet.getData();
    let updateRows = _.chain(sheetData).filter(sheet => _.includes([...this.editRowIds], sheet[0]))
                    .map(sheet => {
                      sheet = _.map(sheet, (d:any) => this.trimValue(d));
                      return _.zipObject(this.colHeaders, sheet);
                    }).value();
    let insertRows =  _.chain(sheetData).filter(sheet => {
            if(_.isNil(sheet[0])) {
              let option = _.find(sheet, d => !!d);
              return !option ? false : true;
            } else {
              return false;
            }
        }).map(sheet => {
            sheet = _.map(sheet, (d:any) => this.trimValue(d));
            return _.zipObject(this.colHeaders, sheet);
        }).value();
    return {
        operations: {
          UPDATE: updateRows,
          INSERT: insertRows,
          DELETE: this.removeRows
        }
    }
  }

  async save(isSync = false) {
    if(!this.flag.metaSheetLoading) {
      this.flag.metaSheetLoading = true;
      const param = this.getSaveParams();
      try {
        let response = await this.metaSheetService.editMetaSheetData(this.metasheetId, param);
        if(MetaSheetUtil.isOk(response)) {
          await this.refresh(isSync);
          !isSync && MetaSheetUtil.showMessage(this, MSG_TYPE.SUCCESS, 'success');
        } else {
          this.flag.metaSheetLoading = false;
          MetaSheetUtil.showMessage(this, MSG_TYPE.ERROR, response.data.msg);
          throw new Error(response.data.msg);
        }
      }
      catch(e) {
        this.flag.metaSheetLoading = false;
        throw e;
      }
    }
  }

  async getWhiteList() {
    try {
      let response = await this.metaSheetService.getWhiteList();
      if(response.status == 200 && response.data) {
        let whitelists = response.data;
        if(whitelists.indexOf(this.nt) != -1) {
          this.isWhiteList = true;
        }
      }
    }
    catch(e) {}
  }

  async created() {
    await this.getMetaSheetData(true);
    await this.getWhiteList();
    this.getSyncStatus();
  }

  async mounted() {
    if(this.metaSheetRow.metaTableStatus == MS_STATUS.REGISTER_FAIL
    || this.metaSheetRow.metaTableStatus == MS_STATUS.LOAD_FAIL
    || this.metaSheetRow.metaTableStatus == MS_STATUS.SUCCESS)
    this.syncItem = {
      title: '',
      type: MSG_TYPE.WARNING
    }
  }

  activated() {
    if(!this.flag.metaSheetLoading) {
      this.debounceRender();
    }
    this.getSyncStatus();
  }

  async updateSchemaCb(id: string) {
    this.flag.showMetaConfigTable = false;
  }

  beforeClose() {
  }

  getSyncParam() {
    let {account, db, tbl, platform, metaTableType} = this.sourceConfig;
    if(metaTableType == MS_SYS.HDM) {
      account = this.metaSheetRow.nt;
      platform = PLATFORM.HERMES;
    }
    let option = {};
    if(this.metaSheetRow.metaTableStatus == MS_STATUS.CREATED
    || this.metaSheetRow.metaTableStatus == MS_STATUS.REGISTER_FAIL) {
      option = {account,db,tbl,platform, metaTableType};
    }
    return option;
  }

  async sync() {
    this.flag.popoverShow = false;
    this.flag.metaSheetSyncing = true;
    const option = this.getSyncParam();
    try {
      await this.save(true);
    }
    catch(e) {
      this.flag.metaSheetSyncing = false;
      return;
    }
    try {
      let response = await this.metaSheetService.syncToTarget(this.metaSheetRow.id!, option);
      if(MetaSheetUtil.isOk(response)) {
        let alertDiv = (this.$el as HTMLElement).querySelector(".sync-alert");
        alertDiv && ((alertDiv as HTMLElement).style.display = 'block');
        this.flag.syncClicked = true;
        let {metaTableStatus,failLog, syncTime} = response.data.content;
        let newTarget:MetaSheetTableRow = {
          ...this.metaSheetRow,
          sourceConfig: this.sourceConfig,
          metaTableStatus: metaTableStatus,
          syncTime: syncTime
        }
        this.getSyncStatus();
        this.$store.dispatch('updateMetaSheet',newTarget);
      } else {
        this.flag.metaSheetSyncing = false;
        MetaSheetUtil.showMessage(this, MSG_TYPE.ERROR, response.data.msg);
      }
    }
    catch(e) {
      this.flag.metaSheetSyncing = false;
    }
  }

  setSyncItem(metaTableStatus: MS_STATUS, failLog = 'Table Sync Failed') {
    if(metaTableStatus == MS_STATUS.SYNC) {
      this.syncItem = {
        title: 'Table is syncing data now, it may take 1 or more mins, please wait on the page to get the sync done.',
        type: MSG_TYPE.WARNING
      }
    } else if(metaTableStatus == MS_STATUS.SUCCESS) {
      this.syncItem = {
        title: 'Table sync is done!',
        type: MSG_TYPE.SUCCESS
      }
    } else if(metaTableStatus == MS_STATUS.REGISTER_FAIL) {
      this.syncItem = {
        title: `Table schema created failed. ${failLog}`,
        type: MSG_TYPE.ERROR
      }
    } else if(metaTableStatus == MS_STATUS.LOAD_FAIL) {
      this.syncItem = {
        title: `Table sync data failed. ${failLog}`,
        type: MSG_TYPE.ERROR
      }
    }
  }

  clearInterval() {
    if(this.interval) {
      clearInterval(this.interval as NodeJS.Timer);
      this.interval = null;
    }
  }

  getSyncStatus() {
    this.clearInterval();
    this.interval = setInterval(() => {
      if(!_.isEmpty(this.metaSheetRow)) {
        let { id, metaTableStatus } = this.metaSheetRow;
        if(id) {
          this.metaSheetService.getSyncStatus(id).then((response) => {
            if(MetaSheetUtil.isOk(response) && response.data.content) {
              let { metaTableStatus,failLog, syncTime} = response.data.content;
              let oldMetaTableStatus = this.metaSheetRow.metaTableStatus;
              this.updateMetaSheetTable(metaTableStatus, failLog, syncTime);
              if(metaTableStatus == MS_STATUS.SUCCESS) {
                this.flag.metaSheetSyncing = false;
                this.clearInterval();
              }
            }
          })
          .catch(error => {
            error.resolve();
            let {code, message} = error;
            const codeMap = {
              'SHEET_SYNC_LOAD_FAIL': MS_STATUS.LOAD_FAIL,
              'SHEET_SYNC_REGISTER_FAIL': MS_STATUS.REGISTER_FAIL
            }
            const metaTableStatus = codeMap[code];
            if(metaTableStatus) {
              this.flag.metaSheetSyncing = false;
              this.updateMetaSheetTable(metaTableStatus, message);
            }
          });
        }

      }
    }, 1000 * 10);
  }

  updateMetaSheetTable(metaTableStatus, failLog, syncTime?) {
    let oldMetaTableStatus = this.metaSheetRow.metaTableStatus;
    if(oldMetaTableStatus != MS_STATUS.CREATED && oldMetaTableStatus != metaTableStatus) {
      let newTarget:MetaSheetTableRow = {
        ...this.metaSheetRow,
        metaTableStatus: metaTableStatus,
        failLog: failLog,
      }
      if(syncTime) {
        newTarget.syncTime = syncTime;
      }
      this.$store.dispatch('updateMetaSheet',newTarget);
      this.setSyncItem(metaTableStatus, failLog);
    }
  }

  close() {
    this.flag.popoverShow = false;
  }

  editMetaConfig(row: MetaSheetTableRow) {
    if(this.isProdViewMode) {
      this.mode == MODE.VIEW;
    } else {
      this.mode = this.rowMode(row);
    }
    if(this.mode == MODE.VIEW) {
      this.title = row.metaTableName;
    } else {
      this.title = '';
    }
    if(this.mode == MODE.VIEW || this.metaSheetRow.metaTableStatus != MS_STATUS.SYNC) {
      this.metaSheetConfig = {
      id: row.id,
      name: row.metaTableName,
      metaTableStatus: row.metaTableStatus,
      schemeConfig: row.schemeConfig,
      sourceConfig: row.sourceConfig
      }
      this.flag.showMetaConfigTable = true;
    }
  }

}
</script>

<style lang="scss" scoped>
@import '@/styles/global.scss';
$grey: #C4C4C4;
$margin10: 10px;
$bfs: 18px;
.sheet-view-container {
  .header {
    display: flex;
    margin-top: $margin10;
    margin-bottom: $margin10;

    .name {
      flex:1;
      font-size: 20px;
      font-weight: 700;
      word-break: break-word;
    }
    .tool {
      display: flex;
      align-items: center;
      justify-content: flex-end;
      width: 470px;
      cursor: pointer;

      .sync-time {
        cursor: default;
        font-size: 12px;
        color: $grey;
      }

      .save-btn {
        margin-left: 12px;
      }
    }
  }
  .op-icon {
    color: $zeta-global-color;
    font-size: $bfs;
  }

  .zeta-icon-share {
    margin-left: 12px;
  }

  #spreadsheet {
    overflow: hidden;
    height: calc(100% - 50px);
  }

}
/deep/ .btn-group {
  margin-right: 5px;
  text-align: right;
}
/deep/ .btn {
  margin-left: 12px !important;
  padding: 5px 10px;
  min-width: 50px;
}
/deep/ .meta-config-dialog {
  width: 65%;
  min-width: 900px;
}

/deep/ .el-dialog__header {
  text-align: center;

  .el-dialog__title {
    font-size: $input-font-size;
  }
}

/deep/ button.is-disabled .op-icon {
  color: $grey;
}

/deep/ .htCore td {
  white-space: nowrap;
}

/deep/ .disabled {
  color: $grey !important;
  cursor: not-allowed;
}
/deep/ textarea.handsontableInput {
  overflow-y: auto !important;
}
/deep/ .el-alert__content {
  max-height: 50px;
  word-break: break-all;
  overflow-y: auto;
  overflow-x: hidden;
  margin: 0 20px 0 0;
}


</style>
