<template>
  <div class="vdm-container">
    <div class="row">
      <div class="source-info-label">
        Source
      </div>
      <el-select
        v-model="vdmtable.source"
        style="width: 150px"
        placeholder="platform"
        @change="sourceChange"
      >
        <el-option
          v-for="item in srcPlatformOptions"
          :key="item.value"
          :label="item.key"
          :value="item.value"
        />
      </el-select>
    </div>
    <div class="row">
      <div class="source-info-label">
        Database
      </div>
      <el-select
        v-model="vdmtable.database"
        v-loading="dbLoading"
        placeholder="Please Select"
        @change="databaseChange"
      >
        <el-option
          v-for="item in dataBaseOptions[vdmtable.source]"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </el-select>

      <span
        class="refresh-info"
        @click="()=> refreshDataBase()"
      >
        <i
          class="icon"
          :class="{'el-icon-refresh':!refreshLoading,'el-icon-loading':refreshLoading}"
        />
        Refresh DB &amp; table list
      </span>
    </div>
    <div class="row">
      <div class="row-name source-info-label">
        Table
      </div>
      <div
        class="input-section surfix"
        :class="{ disabled: tableValid }"
      >
        <input
          v-model.trim="vdmtable.tableName"
          type="text"
          placeholder="multiple tables separated with semicolon"
          :disabled="tableValid"
        >
        <i
          class="icon zeta-icon-fullScreen"
          @click="!tableValid&&openDialog(4)"
        />
      </div>
    </div>
    <div class="row">
      <div class="row-name source-info-label">
        View
      </div>
      <div
        class="input-section surfix"
        :class="{ disabled: viewValid }"
      >
        <input
          v-model.trim="vdmtable.viewName"
          type="text"
          placeholder="multiple views separated with semicolon"
          :disabled="viewValid"
        >
        <i
          class="icon zeta-icon-fullScreen"
          @click="!viewValid&&openDialog(5)"
        />
      </div>
      <span class="row-tooltip">See the
        <a
          href="https://wiki.vip.corp.ebay.com/x/CgPGLg"
          target="_blank"
        >prerequisites and limitations wiki</a>
      </span>
    </div>
    <div class="row row-tooltip">
      * Powered by ADI
    </div>

    <el-dialog
      ref="dialog"
      :visible.sync="dialogVisible"
      custom-class="vdm-table-dialog"
      width="500"
      :show-close="false"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      @closed="dialogCloseCallback"
    >
      <slot name="title">
        <div class="header-section">
          <div class="title">
            {{ vdmType === 4?'Select Tables':'Select Views' }}
          </div>
          <div class="close">
            <i
              class="icon zeta-icon-fullScreen-exit"
              @click="handleClose"
            />
          </div>
        </div>
      </slot>
      <div class="tool-section">
        <div
          class="note"
          :class="{hide: vdmType===4}"
        >
          Note: Please ensure to have the underlying VDM tables moved before initiating view migration
        </div>
        <div class="input-section prefix">
          <input
            v-model="search"
            type="text"
            placeholder="search"
            @input="filterTableData"
          >
          <i class="icon zeta-icon-search1" />
        </div>
      </div>
      <div class="table-section">
        <pl-table
          ref="multipleTable"
          v-loading="loading"
          :data="tableData"
          max-height="400"
          border
          use-virtual
          big-data-checkbox
          style="width: 100%"
          @selection-change="handleSelectionChange"
        >
          <pl-table-column
            type="selection"
            width="55"
          />
          <pl-table-column
            :label="vdmType===4? 'Table (Job Title)':'View(s)'"
            prop="name"
          />
          <template slot="empty">
            No tables found
          </template>
        </pl-table>
      </div>
    </el-dialog>
  </div>
</template>
<script lang="ts">
import { Component, Vue, Prop, Inject, Watch } from 'vue-property-decorator';
import _ from 'lodash';
import DMRemoteService from '@/services/remote/DataMove';
import DataMove from './DataMove.vue';
import { Mode } from './internal';
import 'pl-table/themes/index.css';
import { PlTable, PlTableColumn } from 'pl-table';
interface TableRow {
  name: string;
}
const DELIMETER = ';';
@Component({
  components: {
    PlTable,
    PlTableColumn,
  },
})
export default class VMDTableComponent extends Vue {
  @Inject('dmRemoteService') dmRemoteService: DMRemoteService;
  @Prop() vdmtable: DataMove.VDMTable;

  dataBaseOptions = {
    'hermes': [],
    'hopper': [],
  };
  dialogVisible = false;
  loading = true;
  search = '';
  tableData: Array<TableRow> = [];
  originTableData: Array<TableRow> = [];
  multipleSelection: Array<any> = [];
  refreshLoading = false; // for VDM Database
  refreshTable = false; // for VDM table

  srcPlatformOptions = [
    { key: 'hermes', value: 'hermesrno'},
  ];
  dbLoading = true;
  manual = false; // source change type
  vdmType = 4; // 4 is table; 5 is view
  get sourceValid () {
    return this.vdmtable.source != '' && this.vdmtable.database != '' && (this.vdmtable.tableName.trim() != '' || this.vdmtable.viewName.trim() != '');
  }
  get redo (){
    return  this.$store.state.dataMove.redo;
  }
  get tableValid (){
    return this.vdmtable.database == '' || this.vdmtable.viewName !=  '';
  }
  get viewValid (){
    return this.vdmtable.database == '' || this.vdmtable.tableName !=  '';
  }
  created () {
    this.$emit('is-table-valid', {mode: Mode.VDM, valid: false});
    this.getVDMDataBases();
  }
  async getVDMDataBases (isRealTime = false) {
    try {
      if (isRealTime) {
        this.refreshLoading = true;
        this.refreshTable = true;
      }
      this.dbLoading = true;
      const result = await this.dmRemoteService.getVDMDataBases(this.vdmtable.source, isRealTime);
      this.dataBaseOptions[this.vdmtable.source] = _.map(result.data.sort(), (db: string) => {
        return {
          label: db,
          value: db,
        };
      });
      if (!this.redo && this.dataBaseOptions[this.vdmtable.source].length > 0 ) {
        this.vdmtable.database = this.dataBaseOptions[this.vdmtable.source][0].value;
        this.vdmtable.tableName = '';
        this.vdmtable.viewName  = '';
      }
      if (this.dataBaseOptions[this.vdmtable.source].length === 0){
        this.vdmtable.database = '';
        this.vdmtable.tableName = '';
        this.vdmtable.viewName = '';
      }
      this.refreshLoading = false;
      this.dbLoading = false;
    }
    catch (e) {
      this.dbLoading = false;
      this.refreshLoading = false;
      this.dataBaseOptions[this.vdmtable.source] = [];
      this.vdmtable.database = '';
      this.vdmtable.tableName = '';
      this.vdmtable.viewName = '';
    }
  }
  selectVDMTableRow () {
    const tbs = this.vdmType === 4 ? this.vdmtable.tableName:this.vdmtable.viewName;
    const selectedRows = tbs.trim().split(DELIMETER).map((tableName: string) => {
      const row = this.tableData.find((tableOption: {name: string}) => tableName.trim().toLowerCase() === tableOption.name.toLowerCase());
      if (row) {
        return {
          row: row,
          selected: true,
        };
      }
      return null;
    }).filter(d => !!d);
    if (selectedRows && selectedRows.length) {
      (this.$refs.multipleTable as PlTable).toggleRowSelection(selectedRows);
    }
    this.loading = false;
  }
  async getVDMTables () {
    const isRealTime = this.refreshTable ? true: false;
    this.loading = true;
    try {
      const result = await this.dmRemoteService.getVDMTables(this.vdmtable.source, this.vdmtable.database, isRealTime);
      if (isRealTime) {
        this.refreshTable = false;
      }
      const sortedTableData: string[] = _.sortBy(result.data, (name: string) => name);
      this.originTableData = _.map(sortedTableData, (name: string) => {
        return { name };
      });
      this.setTableData();
      setTimeout(() => {
        this.selectVDMTableRow();
      }, 50);
    }
    catch (e) {
      this.loading = false;
    }
  }
  async getVDMViews () {
    this.loading = true;
    try {
      const result = await this.dmRemoteService.getVDMViews(this.vdmtable.source, this.vdmtable.database);
      const sortedTableData: string[] = _.sortBy(result.data, (name: string) => name);
      this.originTableData = _.map(sortedTableData, (name: string) => {
        return { name };
      });
      this.setTableData();
      setTimeout(() => {
        this.selectVDMTableRow();
      }, 50);
    }
    catch (e) {
      this.loading = false;
    }
  }
  openDialog (vdmType: number) {
    if (this.vdmtable.database == '') return;
    this.dialogVisible = true;
    this.vdmType = vdmType;
    if (vdmType === 4){
      this.getVDMTables();
    } else {
      this.getVDMViews();
    }
  }
  destroyed () {
    this.reset();
  }
  filterTableData () {
    this.clearTableSelection();
    this.setTableData();
  }
  setTableData () {
    this.tableData = _.cloneDeep(this.originTableData.filter(d => {
      if (d.name.toLowerCase().includes(this.search.toLowerCase())) {
        return d;
      }
    }));
  }
  clearTableSelection () {
    (this.$refs.multipleTable as PlTable).clearSelection();
  }
  dialogCloseCallback () {
    const tbs = this.vdmType === 4 ? this.vdmtable.tableName:this.vdmtable.viewName;
    const tables = tbs.trim().split(DELIMETER).map(d => d.trim());
    const selectTables = this.multipleSelection.map(d => d.name);
    const userCustomeTables = _.differenceWith(tables, this.originTableData.map(d => d.name), function (m, n) {
      if (m.toLowerCase() == n.toLowerCase()) {
        return true;
      }
      return false;
    });
    const newTables = _.uniq(userCustomeTables.concat(selectTables)).filter(d => !!(d.trim()));
    // this.vdmtable.tableName = newTables.join(DELIMETER);
    if (this.vdmType === 4){
      this.vdmtable.tableName = newTables.join(DELIMETER);
    } else {
      this.vdmtable.viewName = newTables.join(DELIMETER);
    }
    this.originTableData = [];
    this.tableData = [];
    this.search = '';
  }
  handleSelectionChange (val: Array<any>) {
    this.multipleSelection = val;
  }
  handleClose () {
    this.dialogVisible = false;
  }
  refreshDataBase () {
    this.refreshLoading = true;
    this.getVDMDataBases(true);
  }
  databaseChange () {
    this.vdmtable.tableName = '';
    this.vdmtable.viewName =  '';
  }
  sourceChange (){
    this.manual = true;
  }
  reset () {
    this.$store.dispatch('setRedo', false);
    this.vdmtable.tableName = '';
    this.vdmtable.viewName = '';
    this.vdmtable.source = 'hermesrno';
    // console.log(this.dataBaseOptions);
    if (this.dataBaseOptions[this.vdmtable.source] && this.dataBaseOptions[this.vdmtable.source].length>0) {
      this.vdmtable.database = this.dataBaseOptions[this.vdmtable.source][0].value;
    } else {
      this.vdmtable.database = '';
    }
  }
  validateSource (tableInfo: DataMove.VDMTable) {
    const source = tableInfo.source;
    if (!source) {
      return;
    }
    const hasSource = (_.map(this.srcPlatformOptions, option => option.value).indexOf(source) >= 0);
    if (!hasSource) {
      tableInfo.source = '';
    }
  }
  apply (vdmTable: DataMove.VDMTable) {
    if (!_.isEmpty(vdmTable)) {
      this.validateSource(vdmTable);
      this.$store.dispatch('setRedo', true);
      this.vdmtable.source = vdmTable.source;
      if (!vdmTable.tableName) {
        if (this.dataBaseOptions[this.vdmtable.source] && this.dataBaseOptions[this.vdmtable.source].length>0) {
          this.vdmtable.database = this.dataBaseOptions[this.vdmtable.source][0].value;
        }
      }
      this.vdmtable.database = vdmTable.database;
      this.vdmtable.tableName = vdmTable.tableName;
      this.vdmtable.viewName = vdmTable.viewName;
    }
  }
  @Watch('sourceValid')
  isSourceValid (val: boolean) {
    this.$emit('is-table-valid', {mode: Mode.VDM, valid: val});
  }
  get vdmTable (){
    return JSON.parse(JSON.stringify(this.vdmtable));
  }
  @Watch('vdmTable', {deep: true})
  handleVDMChange (val: DataMove.VDMTable, oldVal: DataMove.VDMTable){
    if (val.source &&  (val.source !== oldVal.source)){
      if (this.dataBaseOptions[val.source].length===0){
        this.getVDMDataBases();
      } else {
        if (this.manual){
          this.vdmtable.database = this.dataBaseOptions[this.vdmtable.source][0].value;
          this.vdmtable.tableName = '';
          this.vdmtable.viewName  = '';
        }
      }
      if (this.manual){
        this.$store.dispatch('setRedo', false);
        this.manual = false;
      }
    }
    this.$emit('vdm-change', this.vdmtable);
  }
}
</script>

<style lang="scss" scoped>
.vdm-container {
    .input-section {
        position: relative;
        width: 350px;
        input {
            -webkit-appearance: none;
            background-color: #FFFFFF;
            background-image: none;
            border-radius: 4px;
            border: 1px solid #DCDFE6;
            -webkit-box-sizing: border-box;
            box-sizing: border-box;
            color: #606266;
            display: inline-block;
            font-size: inherit;
            height: 30px;
            line-height: 30px;
            outline: none;
            -webkit-transition: border-color 0.2s cubic-bezier(0.645, 0.045, 0.355, 1);
            transition: border-color 0.2s cubic-bezier(0.645, 0.045, 0.355, 1);
            width: 100%;
            &::placeholder {
                color: #C0C4CC;
            }
        }
        &.surfix {
            input {
                padding: 0 30px 0 15px;
            }
            .zeta-icon-fullScreen {
                position: absolute;
                right: 6px;
                height: 30px;
                line-height: 30px;
                cursor: pointer;
                color: #ccc;
            }
        }
        &.prefix {
            input {
                padding: 0 15px 0 30px;
            }
            .zeta-icon-search1 {
                position: absolute;
                left: 6px;
                height: 30px;
                line-height: 30px;
                cursor: pointer;
                color: #ccc;
            }
        }
        &.disabled {
            input {
                cursor: not-allowed !important;
            }
            .icon {
                cursor: not-allowed !important;
            }
        }
    }
    .vdm-table-dialog {

        .tool-section{
          display: flex;
          justify-content: space-between;
          align-items: center;
          .note{
            word-break: normal;
            color: #569ce1;
            font-size: 12px;
            &.hide{
              visibility: hidden;
            }
          }
        }
        .input-section {
            float: right;
            width: 150px;
            margin-bottom: 10px;
        }
        .table-section {
            // max-height: 400px;
            // overflow-y: auto;
            clear: both;
        }
        .header-section {
            display: flex;
            justify-content: space-between;
            margin-bottom: 10px;
            font-weight: bold;
            font-size: 18px;
            color: #333;
            .zeta-icon-fullScreen-exit {
                cursor: pointer;
                color: #ccc;
            }
        }
    }
    /deep/ .el-dialog__header {
        padding: 10px;
    }
    /deep/ .el-dialog__body {
            padding-bottom: 30px;
    }
    .refresh-info {
        margin-left: 10px;
        color: #666;
        cursor: pointer;
        .icon {
            cursor: pointer;
        }
    }
    .row-tooltip {
        font-size: 12px;
        color: #666;
    }
}
</style>
