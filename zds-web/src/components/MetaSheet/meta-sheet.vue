<template>
  <div class="tools-container">
    <div class="header">
      <el-button
        @click="createNew()"
        plain
        v-click-metric:ZS_CLICK="{name: 'createNewSheet'}"
      >Create New</el-button>
      <el-button @click="refresh()" plain :loading="flag.hisLoading">Refresh</el-button>
      <el-button
        @click="moveToFolder()"
        plain
        :disabled="multipleSelectedRows.length == 0"
      >Move To Folder</el-button>
      <div class="search">
        <el-input v-model="search" prefix-icon="el-icon-search" placeholder="Search" />
      </div>
    </div>
    <div class="divider"></div>
    <el-table
      :data="visibleTableData"
      ref="multipleTable"
      v-loading="flag.hisLoading"
      @sort-change="onSortChange"
      header-row-class-name="meta-sheet-header"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="50" :selectable="rowSelectAble"></el-table-column>
      <el-table-column prop="metaTableName" sortable="custom">
        <template slot="header" slot-scope="scope">Current Path: {{pwd}}</template>
        <template slot-scope="scope">
          <template v-if="scope.row.isFile">
            <span class="open-link" @click.stop="openSheet(scope.row)">{{scope.row.metaTableName}}</span>
          </template>
          <template v-else>
            <span class="open-link" @click="clickFolder(scope.row.metaTableName)">
              <i class="icon-folder-empty"></i>
              {{ scope.row.metaTableName }}
            </span>
          </template>
        </template>
      </el-table-column>
      <el-table-column prop="nt" label="Creator" sortable="custom">
        <template slot-scope="scope">{{scope.row.nt}}</template>
      </el-table-column>
      <el-table-column prop="access" label="Access" sortable="custom">
        <template slot-scope="scope">{{scope.row.access}}</template>
      </el-table-column>
      <el-table-column prop="platform" label="Platform" sortable="custom">
        <template slot-scope="scope">
          <i :class="getPlatformIcon(scope.row.platform)" class="workspace-icon" />
        </template>
      </el-table-column>
      <el-table-column prop="fullTableName" label="Table" sortable="custom">
        <template slot-scope="scope">{{scope.row.fullTableName}}</template>
      </el-table-column>
      <el-table-column prop="updateTime" label="Last Modify Date" sortable="custom">
        <template slot-scope="scope">{{formatModifyDate(scope.row.updateTime)}}</template>
      </el-table-column>
      <el-table-column align="right" width="180" label>
        <template slot-scope="scope">
          <template v-if="scope.row.isFile">
            <div class="operation">
              <el-popover placement="bottom" trigger="click" visible-arrow="true" width="370">
                <div>
                  <meta-share :sheet-id="scope.row.id" :mode="rowMode(scope.row)" />
                </div>
                <el-button type="text" slot="reference" class="meta-btn">
                  <i class="op-icon zeta-icon-share"></i>
                </el-button>
              </el-popover>
              <el-button
                type="text"
                class="meta-btn"
                :disabled="disableClick(scope.row, 'delete')"
                @click="deleteMetaSheet(scope.row)"
              >
                <i class="op-icon el-icon-delete"></i>
              </el-button>
            </div>
          </template>
          <template v-else></template>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="tableData && tableData.length > 0 && pwd == '/'"
      class="pager"
      layout="prev, pager, next"
      :total="totalLength"
      :page-size="pageSize"
      :current-page.sync="pageIndex"
    ></el-pagination>

    <el-dialog
      :visible.sync="flag.showMetaConfigTable"
      title
      :close-on-click-modal="flag.closeOnClickModal"
      custom-class="meta-config-dialog"
    >
      <meta-config-edit
        v-if="flag.showMetaConfigTable"
        :mode="mode"
        :config="metaSheetConfig"
        @created="createdSchemaCb"
        @cancel="cancel"
      />
    </el-dialog>

    <el-dialog
      title="Move To Folder"
      :visible.sync="flag.moveToFolder"
      :close-on-click-modal="flag.closeOnClickModal"
      width="720px"
    >
      <el-form :rules="rules" :model="form" ref="form" label-width="130px" label-position="left">
        <el-form-item label="Folder" prop="path">
          <el-input v-model="form.path"></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button plain @click="close">Cancel</el-button>
        <el-button type="primary" @click="confirm">Confirm</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script lang="ts">
import { Component,Vue,Inject,Prop,Provide,Watch,Mixins} from "vue-property-decorator";
import {MetaSheetTableRow,SchemeConfig,SourceConfig,MetaSheetConfig,MODE,MS_SYS,MS_STATUS,MSG_TYPE,Mode,ACCESS} from "@/types/meta-sheet";
import MetaConfigEdit from "./meta-config-edit.vue";
import MetaShare from "./meta-share.vue";
import _ from "lodash";
import { Form } from "element-ui";
import Util from "@/services/Util.service";
import { WorkspaceSrv } from "@/services/Workspace.service";
import { IWorkspace } from "@/types/workspace";
import { WorkspaceManager } from "@/types/workspace/workspace-manager";
import moment from "moment";
import MetaSheetMixin from "./meta-sheet-mixin";
import MetaSheetUtil, { FOLDER_VALIDATORS } from "./util";
import { Table as ElTable } from "element-ui";
function getAllFilesByPath(files, path: string) {
  return _.chain(files).filter(f => _.startsWith(f.folderPath, path)).value();
}
@Component({
  components: {
    MetaConfigEdit,
    MetaShare
  }
})
export default class MetaTableConfigure extends Mixins(MetaSheetMixin) {
  debounceSearch: Function;
  search = '';
  pageSize = 15;
  pageIndex = 1;
  pwd: string = '/';
  visibleTableData: Array<MetaSheetTableRow> = [];
  form: any = {
    path: '/'
  };
  totalLength = 0;
  multipleSelectedRows = [];
  rules = {
    path: [
      {
        validator: (r: any, f: string, cb: Function) => {
          _.each(FOLDER_VALIDATORS, validator => {
            if (!validator.regex.test(f)) {
              cb(new Error(validator.message));
            } else {
              cb();
            }
          });
        }
      }
    ]
  };
  constructor() {
    super();
    this.debounceSearch = _.debounce(this.getVisibleTableData, 500);
  }

  sortOption: {
    prop: string;
    isAsc: boolean;
  } | null = null;

  get metasheets() {
    let metasheets = this.$store.getters.metasheets;
    return metasheets;
  }

  get visibleData() {
    return _.slice(
      this.tableData,
      (this.pageIndex - 1) * this.pageSize,
      this.pageIndex * this.pageSize
    );
  }

  get pwdDirectFiles() {
    // directly files avaliable in current pwd, {nbid: {}}
    let ret = _.filter(this.tableData, (metasheet: any) => {
      return this.directlyHas(this.pwd, metasheet.folderPath);
    });
    return ret;
  }

  get pwdFiles() {
    let ret = _.filter(this.tableData, (metasheet: any) => {
      return this.has(this.pwd, metasheet.folderPath);
    });
    return ret;
  }

  get pwdFolder() {
    // current folder names in pwd ["test", "conf"]
    let folders: { [K: string]: null } = {};
    _.forEach(this.pwdFiles, (file: any) => {
      let subs = file.folderPath.slice(this.pwd.length).split('/');
      if (subs.length >= 2) {
        folders[subs[0]] = null;
      }
    });
    let ret = Object.keys(folders);
    return ret;
  }

  get tableData() {
    let arr: any = [];
    if (this.search) {
      arr = this.rawTableData.filter((row: MetaSheetTableRow) => {
        return ['metaTableName','nt','access','platform','fullTableName'].some((key: string) => {
          if (row[key]) {
            return (
              row[key].toLowerCase().indexOf(this.search.toLowerCase()) > -1
            );
          }
          return false;
        });
      });
    } else {
      arr = _.cloneDeep(this.rawTableData);
    }
    return arr;
  }



  created() {
    if (!this.metasheets) {
      this.rawTableData = [];
      this.visibleTableData = [];
    } else {
      let tableData = _.cloneDeep(this.metasheets);
      this.rawTableData = tableData;
      this.getVisibleTableData();
    }
  }

  close() {
    this.flag.moveToFolder = false;
    this.form.path = '/';
  }

  handleSelectionChange(val) {
    this.multipleSelectedRows = val;
  }

  has(root: string, path: string): boolean {
    return path.indexOf(root) === 0;
  }

  directlyHas(root: string, path: string): boolean {
    let has = path.indexOf(root) === 0;
    if (!has) return false;
    let direct = path.slice(root.length).indexOf('/') === -1;
    return direct;
  }

  rowSelectAble(row: MetaSheetTableRow, index: number) {
    if (!row.selectable) return false;
    if (row.id == 'default') {
      return false;
    }
    return true;
  }

  clickFolder(folder: string) {
    this.clearTableSelection();
    if (folder === '..') {
      let pwd = this.pwd.slice(0, -1);
      this.pwd = pwd.slice(0, pwd.lastIndexOf('/') + 1);
    } else {
      this.pwd += folder + '/';
    }
    this.getVisibleTableData();
  }

  moveToFolder() {
    this.flag.moveToFolder = true;
  }

  getVisibleTableData() {
    let list: Array<MetaSheetTableRow> = [];
    const isRoot = this.pwd == '/' ? true : false;
    if (!isRoot) {
      list.push({
        id: '..',
        metaTableName: '..',
        isFile: false,
        selectable: false,
        updateTime: 0
      });
    }
    for (let folder of this.pwdFolder) {
      const subFiles = getAllFilesByPath(this.pwdFiles, this.pwd + folder);
      const updateTimeFile = _.maxBy(subFiles, 'updateTime');
      list.push({
        id: this.pwd + folder + '/',
        metaTableName: folder,
        isFile: false,
        selectable: true,
        updateTime: updateTimeFile ? updateTimeFile.updateTime : 0
      });
    }
    _.each(this.pwdDirectFiles, (file: any) => {
      list.push({
        isFile: true,
        selectable: true,
        ...file
      });
    });
    let ret = list.sort(this.sortByProp(this.sortOption));
    if(isRoot) {
      this.totalLength = ret.length;
      this.visibleTableData = _.slice(ret, (this.pageIndex - 1) * this.pageSize, this.pageIndex * this.pageSize);
    } else {
      this.visibleTableData = ret;
    }
  }

  confirm() {
    (this.$refs['form'] as Form).validate(valid => {
      if (valid) {
        let multipleSelectedRows = _.cloneDeep(this.multipleSelectedRows);
        let tableData = _.cloneDeep(this.tableData);
        const path = this.form.path;
        let folders = _.remove(multipleSelectedRows, (row: any) => !row.isFile);
        let body: any = [];
        _.each(folders, (folder: any) => {
          let ret = _.chain(tableData)
            .filter(d => _.startsWith(d.folderPath, folder.id))
            .each(d => {
              d.folderPath = path + d.folderPath.replace(this.pwd, '');
            })
            .value();
          body = body.concat(ret);
        });
        _.each(multipleSelectedRows, (row: any) => {
          let ret = _.chain(tableData)
            .filter(d => d.folderPath == row.folderPath && d.metaTableName == row.metaTableName)
            .each(d => {
              d.folderPath = path;
            })
            .value();
          body = body.concat(ret);
        });
        const nt = Util.getNt();
        let params = _.map(body, metasheetRow => {
          return {
            id: metasheetRow.id,
            path: metasheetRow.folderPath
          };
        });
        this.close();
        this.updateFolderPath(params);
      }
    });
  }

  updateFolderPath(params) {
    this.metaSheetService.updatePath(params).then(response => {
      if (response.status == 200 && _.isArray(response.data)) {
        MetaSheetUtil.showMessage(this, MSG_TYPE.SUCCESS, 'Success');
        let processedMetaSheets = MetaSheetUtil.processMetaTableItem(response.data);
        this.$store.dispatch('initMetasheet', {
          metasheets: processedMetaSheets
        });
      }
    });
  }

  private sortByProp(sortProp) {
    return (a, b) => {
      if (sortProp) {
        const key = sortProp.prop;
        let score_a = a.isFile ? 0 : a.id === '..' ? 100 : 10;
        let score_b = b.isFile ? 0 : b.id === '..' ? 100 : 10;
        let score_sort = (a[key] || 0) >= (b[key] || 0) ? -1 : 1;
        if (sortProp.isAsc) {
          score_sort = -score_sort;
        }
        return score_b - score_a + score_sort;
      }
      return a - b;
    };
  }

  createNew() {
    this.mode = MODE.ADD;
    this.metaSheetConfig = _.cloneDeep(this.getDefaultMetaConfig());
    this.flag.showMetaConfigTable = true;
  }

  openSheet(row: MetaSheetTableRow) {
    let zetasheet = this.$store.getters.nbByFile({ notebookId: row.id });
    if (!zetasheet) {
      const ws: IWorkspace = WorkspaceSrv.getMetaSheet(row);
      WorkspaceManager.getInstance(this).addActiveTabAndOpen(ws);
    } else {
      WorkspaceManager.getInstance(this).addActiveTabAndOpen(zetasheet);
    }
  }

  getDefaultMetaConfig(): MetaSheetConfig {
    return {
      name: '',
      schemeConfig: {
        tableData: [
          {
            column: '',
            type: '',
            length: '',
            desc: '',
            nullValue: true,
            primaryKey: false,
            editable: true,
            validate: false
          }
        ]
      },
      sourceConfig: {
        metaTableType: MS_SYS.HDM,
        db: '',
        tbl: ''
      }
    };
  }

  getPlatformIcon(platform: string) {
    return platform;
  }

  formatModifyDate(timestamp: number) {
    if(timestamp > 0) {
      return moment(timestamp).format('YYYY-MM-DD HH:mm');
    }
  }

  async deleteMetaSheet(row: MetaSheetTableRow) {
    await this.$confirm('Are you sure to delete?', 'Alert', {
      confirmButtonText: 'Confirm',
      cancelButtonText: 'Cancel',
      type: MSG_TYPE.WARNING
    });
    let { id, metaTableStatus } = row;
    let response = await this.metaSheetService.deleteMetaSheet(id!);
    if (MetaSheetUtil.isOk(response)) {
      MetaSheetUtil.showMessage(this, MSG_TYPE.SUCCESS, 'success');
      this.$store.dispatch('deleteMetaSheet', id);
      WorkspaceManager.getInstance(this).closeTab(id!);
    } else {
      MetaSheetUtil.showMessage(this, MSG_TYPE.ERROR, response.data.msg);
    }
  }

  activated() {
    this.getMetaSheets();
  }

  disableClick(row: MetaSheetTableRow, action: string) {
    let { metaTableStatus } = row;
    let result = false;
    switch (action) {
      case 'edit-table':
      case 'share':
      case 'open-sheet':
        result = metaTableStatus != MS_STATUS.CREATED;
        break;
      case 'delete':
        result =
          metaTableStatus == MS_STATUS.SYNC ||
          row.access == ACCESS.READER ||
          row.access == ACCESS.WRITER;
    }
    return result;
  }

  refresh() {
    this.sortOption = null;
    this.pageIndex = 1;
    this.flag.hisLoading = true;
    this.clearTableSelection();
    this.getMetaSheets();
  }

  async createdSchemaCb(id: string) {
    await this.getMetaSheets();
    this.flag.showMetaConfigTable = false;
    let metaSheetRow: MetaSheetTableRow = this.$store.getters.metasheetById(id);
    this.openSheet(metaSheetRow);
  }

  onSortChange({ column, order, prop }) {
    if (order === null) {
      return;
    }
    let isAsc = order == 'ascending' ? true : false;
    this.sortOption = {
      prop,
      isAsc
    };
    if (this.pageIndex != 1) {
      this.pageIndex = 1;
    } else {
      this.getVisibleTableData();
    }
  }

  clearTableSelection() {
    (this.$refs.multipleTable as ElTable).clearSelection();
    let $trs = document.querySelectorAll('.meta-sheet-header th');
    if ($trs) {
      [].forEach.call($trs, function(el: HTMLElement) {
        el.classList.remove('ascending');
        el.classList.remove('descending');
      });
    }
  }

  @Watch("pageIndex")
  pageIndexChange(newVal) {
    this.getVisibleTableData();
  }

  @Watch("metasheets")
  metaSheetsChange(newVal, oldVal) {
    if (newVal) {
      this.rawTableData = _.cloneDeep(newVal);
      this.getVisibleTableData();
      this.$forceUpdate();
    }
  }

  @Watch("search")
  searchTableName(val: string) {
    this.pageIndex = 1;
    this.debounceSearch(val);
  }
}
</script>

<style lang="scss" scoped>
@import "@/styles/global.scss";
$grey: #cacbcf;
$lightgrey: #dedee1;
$p10: 10px;
.tools-container {
  .header {
    .search {
      float: right;
      margin-right: $p10 * 2;
      line-height: $p10 * 3;
    }
  }

  .divider {
    margin-top: $p10;
    border: 1px solid $lightgrey;
  }

  .operation {
    display: flex;
    justify-content: center;

    .meta-btn {
      margin-right: $p10 * 2;
    }
  }

  .op-icon {
    margin-right: $p10 / 2;
    color: $zeta-global-color;
    cursor: pointer;
    font-size: 16px;
    vertical-align: middle;
  }

  .workspace-icon {
    opacity: 1 !important;
  }

  .open-link {
    cursor: pointer;
    &:hover {
      color: $zeta-global-color-heighlight;
    }
  }

  .meta-btn[disabled="disabled"] {
    cursor: not-allowed;

    .op-icon {
      color: $grey !important;
      cursor: not-allowed !important;
    }
  }

  .pager {
    margin: 15px 0;
    text-align: right;
  }
}
.disabled {
  color: $grey !important;
  cursor: default !important;
}
/deep/ .meta-config-dialog {
  width: 65%;
  min-width: 900px;
}
</style>
