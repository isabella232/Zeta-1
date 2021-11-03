<template>
  <div
    v-loading="loading"
    class="result-display"
  >
    <el-row v-if="resultType ==='TABLE'">
      <el-col :span="24">
        <el-tabs
          v-model="activeName"
          type="border-card"
        >
          <el-tab-pane name="result">
            <span slot="label">Result</span>
            <div
              class="result-display"
              :style="style"
            >
              <!-- <QueryBrief :query="statement" /> -->
              <div class="tool-bar">
                <span class="query-info">
                  Retrieved
                  <strong>{{ rows.length }}</strong>
                  rows.
                </span>
                <br>
              </div>
              <div class="tool-bar-btn-group">
                <div class="tool-bar-group-left">
                  <span class="display">
                    <el-button-group>
                      <el-button
                        size="mini"
                        :class="{'active':type=='table'}"
                        @click="type = 'table'"
                      >
                        <i class="icon zeta-icon-table" />
                      </el-button>
                      <el-button
                        size="mini"
                        :class="{'active':type=='plot'}"
                        @click="type = 'plot'"
                      >
                        <i class="icon zeta-icon-chart" />
                      </el-button>
                    </el-button-group>
                  </span>
                  <span class="func">
                    <el-button-group
                      v-if="type == 'plot'"
                      class="plot-btn-group"
                    >
                      <el-button
                        size="mini"
                        @click="() => showEditor = true"
                      >
                        <i class="icon zeta-icon-set" />
                      </el-button>
                      <el-popover
                        v-if="false"
                        placement="bottom"
                        trigger="click"
                        @after-leave="closePopover"
                        @show="getDashboard()"
                      >
                        <el-button
                          slot="reference"
                          size="mini"
                        >
                          <i class="icon zeta-icon-add" />
                        </el-button>
                        <div v-loading="dbLoading">
                          <div
                            v-if="dashboardList && dashboardList.length || createNew"
                            v-loading="createDBLoading"
                            class="dashboard-popup"
                          >
                            <ul class="dashboard-list">
                              <li class="create-dashboard">
                                <span class="dashboard-item-name">
                                  <el-input
                                    v-model="newDashboard"
                                    placeholder="Dashboard"
                                  />
                                </span>
                                <span class="dashboard-item-actions">
                                  <el-button
                                    type="text"
                                    :disabled="!newDashboard"
                                    @click="() => createDashboard()"
                                  >add</el-button>
                                </span>
                              </li>
                              <li
                                v-for="(db,$i) in dashboardList"
                                :key="$i"
                                class="dashboard-item"
                              >
                                <span
                                  class="dashboard-item-name"
                                  :title="db.name"
                                >{{ db.name }}</span>
                                <span class="dashboard-item-actions">
                                  <el-button
                                    v-if="!hadStatement(db)"
                                    type="text"
                                    @click="() => addToDashboard(db)"
                                  >add</el-button>
                                  <el-button
                                    type="text"
                                    @click="() => open(db)"
                                  >view</el-button>
                                </span>
                              </li>
                            </ul>
                          </div>
                          <div
                            v-else
                            class="dashboard-no-data"
                          >
                            <h3>No Dashboard found</h3>
                            <el-button
                              type="text"
                              @click="createNew = true"
                            >Add new Dashboard</el-button>
                          </div>
                        </div>
                      </el-popover>
                    </el-button-group>
                    <el-button
                      size="mini"
                      class="download"
                      @click="exportCSVFile"
                    >
                      <i class="icon zeta-icon-download" />
                    </el-button>
                    <el-button
                      size="mini"
                      @click="openCodeDialog(statement)"
                    >
                      <i class="zeta-icon-sql" />
                    </el-button>
                    <el-button
                      size="mini"
                    >
                      <FavoriteIcon
                        :id="statementId"
                        type="statement"
                        class="favorite-inner"
                      />
                    </el-button>
                  </span>
                </div>
              </div>
              <div class="data-display">
                <table-display
                  v-if="type ==='table'"
                  ref="tableDisplay"
                  class="result-table"
                  :page="currentPage"
                  :page-size="pageSize"
                  :headers="headers"
                  :rows="rows"
                />
                <VuePivoTable
                  v-else
                  :data-in="dataIn"
                  :plot-config="plotConfig"
                  :show-editor.sync="showEditor"
                  @config-change="plotConfigChange"
                />
              </div>
            </div>
          </el-tab-pane>
          <el-tab-pane name="pivot">
            <span slot="label">Pivot</span>
            <div
              id="pivot-table"
              :style="style"
            >
              <PivotTable :data="dataIn" />
            </div>
          </el-tab-pane>
        </el-tabs>
      </el-col>
    </el-row>
    <div
      v-if="resultType ==='TEXT'"
      class="info-overflow"
    >
      <template v-if="result">
        {{ result }}
      </template>
    </div>
    <CodeDisplayDialog
      ref="codeDisplayDialog"
      :value="displayCode"
      :title="'SQL'"
    />
  </div>
</template>

<script lang="ts">
const CHAR_CARRIAGE_RETURN = String.fromCharCode(13);
const CHAR_DOUBLE_QUOTES = String.fromCharCode(34);
const CHAR_LINE_FEED = String.fromCharCode(10);
const columnDelimiter = ',';
const rowDelimiter = '\n';
import { Component, Vue, Watch, Ref, Provide } from 'vue-property-decorator';
import _ from 'lodash';
import VuePivoTable, { PlotConfig } from '@/components/common/pivot-table';
import TableDisplay from '@/components/common/Visualization/TableDisplay.vue';
import FavoriteIcon from '@/components/common/favorite-icon/favorite-icon-witn-store.vue';
import DashboardRemoteService from '@/services/remote/Dashboard';
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import { IDashboardFile, LayoutConfig, IWorkspace } from '@/types/workspace';
import { WorkspaceSrv } from '@/services/Workspace.service';
import PivotTable from './Pivot/pivot.vue';

import CodeDisplayDialog from '@/components/common/Visualization/code-display-dialog.vue';
import { WorkspaceManager } from '@/types/workspace/workspace-manager';

type Style = { [K: string]: string | number };
@Component({
  components: {
    VuePivoTable,
    TableDisplay,
    PivotTable,
    CodeDisplayDialog,
    FavoriteIcon,
  },
})
export default class PlotDisplay extends Vue {
  dashboardRemoteService = new DashboardRemoteService();
  @Provide()
  notebookRemoteService = new NotebookRemoteService();

  @Ref('codeDisplayDialog')
  dialog: CodeDisplayDialog;
  displayCode = '';

  @Provide()
  openCodeDialog(code: string) {
    this.displayCode = code;
    this.dialog.open();
  }

  // @Prop() plotConfig!: PlotConfig;
  statementId: number;
  statement = '';
  loading = false;
  result: Dict<string> | string | null | undefined;
  resultLoaded = false;
  schema: Array<string> | null;
  showEditor = false;
  type: 'table' | 'plot' = 'table';
  newDashboard = '';
  createDBLoading = false;
  createNew = false;
  pageSize = 100;
  currentPage = 1;
  dbLoading = false;
  dashboardList: IDashboardFile[] = [];
  plotConfig: PlotConfig = {} as PlotConfig;
  activeName = 'result';
  pivot: any = null;
  style: Style;
  resultType = 'TABLE';
  constructor() {
    super();
    this.style = {
      height: document.documentElement.clientHeight - 60 - 40 - 20 - 20+'px',
    };

  }
  get dataIn() {
    if (this.headers && this.rows) {
      const headers = this.headers;
      const rows = this.rows;
      return [headers, ...rows];
    } else {
      return [];
    }
  }
  // get dashboardList(): IDashboardFile[] {
  //   const dashboards: Dict<IDashboardFile> = this.$store.getters.dashboardFiles;
  //   return _.chain(dashboards)
  //     .map(db => db)
  //     .value();
  // }
  get headers(): string[] {
    if (!this.resultLoaded) return [];
    return this.schema as string[];
  }

  get rows(): string[][] {
    if (!this.resultLoaded) return [];
    const dict = this.result as Dict<string>;
    return Object.keys(dict).map(key => {
      return _.values(dict[key]).map(v => {
        return _.isObject(v) ? JSON.stringify(v) : v;
      });
    });
  }
  mounted() {
    this.setParams(this.$route.query);
  }
  setParams(query: any) {
    this.statementId = parseInt(query.statementId);
    this.load(this.statementId);
  }

  load(id: number) {
    this.loading = true;
    this.notebookRemoteService
      .getShareStatement(id.toString())
      .then(e => {
        try {
          this.loading = false;
          this.statement = e.data.statement;
          let dag = JSON.parse(e.data.result).dag || null;
          if (/<div>Waiting.../.test(dag) || /<div>Watting/.test(dag)) {
            dag = null;
          }
          const result = JSON.parse(e.data.result).result;
          if (!result) {
            this.resultLoaded = true;
            return;
          }
          const pkg = result[0];
          this.resultType = pkg.type;
          if (this.resultType === 'TEXT') {
            this.result = pkg.content;
          } else if (this.resultType === 'TABLE') {
            this.result = pkg.rows;
            this.schema = pkg.schema;
          }
          this.resultLoaded = true;
          if(e.data.plotConfig){
            this.plotConfig =JSON.parse(e.data.plotConfig);
          }
          document.getElementsByTagName('body')[0].className = 'data-grid-page';
        } catch (e) {
          console.warn('Unexpected packet from server', e);
          this.loading = false;
          this.result = null;
        }
        this.loading = false;
      })
      .catch(e => {
        this.loading = false;
      });
  }
  handleSizeChange(size: number) {
    this.pageSize = size;
  }
  hadStatement(db: IDashboardFile): boolean {
    const layoutConfigs = db.layoutConfigs;
    const cfg = _.find(layoutConfigs, cfg =>
      cfg.statementId ? this.statementId == cfg.statementId : false
    );
    return Boolean(cfg);
  }
  closePopover() {
    this.createNew = false;
  }
  getDashboard() {
    this.dbLoading = true;
    this.dashboardRemoteService
      .getAll()
      .then(data => {
        const dbs = data.data;
        const dbDict: Dict<IDashboardFile> = {};
        _.chain(dbs)
          .forEach(db => {
            dbDict[db.id] = {
              id: db.id,
              nt: db.nt,
              name: db.name,
              layoutConfigs: db.layoutConfig
                ? (JSON.parse(db.layoutConfig) as LayoutConfig[])
                : [],
              createTime: db.createDt,
              updateTime: db.updateDt ? db.updateDt : null,
            } as IDashboardFile;
          })
          .value();
        this.dbLoading = false;
        this.dashboardList = _.chain(dbDict)
          .map(db => db)
          .value();
      })
      .catch(err => {
        console.log(err);
      });
  }
  addToDashboard(db: IDashboardFile) {
    const layoutConfigs = db.layoutConfigs || [];
    const textCfg = new LayoutConfig(layoutConfigs.length + 1, 'text');
    textCfg.value = 'Default Chart Title';
    layoutConfigs.push(textCfg);
    const plotCfg = new LayoutConfig(layoutConfigs.length + 1, this.type);
    plotCfg.statementId = this.statementId;
    layoutConfigs.push(plotCfg);
    this.createDBLoading = true;
    console.debug('layoutConfigs', layoutConfigs);
    this.dashboardRemoteService
      .updateDashboard(db.id, JSON.stringify(layoutConfigs))
      .then(() => {
        this.createDBLoading = false;
      });
  }
  createDashboard(name: string = this.newDashboard) {
    this.createDBLoading = true;
    this.dashboardRemoteService.createDashboard(name).then(({ data: db }) => {
      // this.$store.dispatch("addDashboard", { dashboard: db });
      this.dashboardList.push({...db});
      this.createDBLoading = false;
      this.newDashboard = '';
    });
  }

  plotConfigChange(newCfg: PlotConfig) {
    this.plotConfig = newCfg;
    // this.dashboardRemoteService.updatePlottingConfig(
    //   this.statementId,
    //   this.plotConfig
    // );
  }
  open(db: IDashboardFile) {
    const ws: IWorkspace = WorkspaceSrv.dashboardFromFile(db);
    WorkspaceManager.getInstance(this).addActiveTabAndOpen(ws);
  }
  /* CSV export */
  exportCSVFile() {
    let arr: (string | number | null)[][] = [];
    arr.push(this.headers);
    arr = arr.concat(this.rows);
    let str = '';
    for (let i = 0; i < arr.length; i++) {
      for (let j = 0; j < arr[i].length; j++) {
        let ceilValue = arr[i][j];
        if (ceilValue === null) {
          ceilValue = '';
        } else if (
          typeof ceilValue === 'number' ||
          typeof ceilValue === 'boolean'
        ) {
          ceilValue = ceilValue.toString();
        }
        str += this.parseCSVCell(ceilValue);
        if (j < arr[i].length - 1) {
          str += columnDelimiter;
        }
      }
      str += rowDelimiter;
    }

    const blob = new Blob(['\ufeff' + str], { type: 'text/csv' });

    const aLink = document.createElement('a');
    aLink.href = URL.createObjectURL(blob);
    document.body.appendChild(aLink);

    aLink.download = `result_${new Date().getTime()}.csv`;
    document.body.appendChild(aLink);
    aLink.click();
    document.body.removeChild(aLink);
  }

  parseCSVCell(value: string) {
    if (
      value !== '' &&
      (value.indexOf(CHAR_CARRIAGE_RETURN) > -1 ||
        value.indexOf(CHAR_DOUBLE_QUOTES) > -1 ||
        value.indexOf(CHAR_LINE_FEED) > -1 ||
        value.indexOf(columnDelimiter) > -1)
    ) {
      value = value.replace(new RegExp('"', 'g'), '""');
      value = '"' + value + '"';
    }
    return value;
  }
  @Watch('$route.query')
  routeQueryChange(newVal: any) {
    this.setParams(newVal);
  }
  @Watch('rows')
  setPageSize() {
    this.pageSize = this.rows.length;
  }
}
</script>
<style lang="scss">
.data-grid-page {
  // .nav-items-list {
  //   display: none;
  // }
  .Charts {
    min-height: 300px !important;
  }
  .table-wrapper {
    overflow: auto;
  }
}
</style>
<style lang="scss" scoped>
@import "@/styles/global.scss";
.result-display {
  box-sizing: border-box;
  height: 100%;
  width: 100%;
  .tool-bar,
  .tool-bar-btn-group {
    height: 30px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    span:nth-child(n + 2) {
      margin: 0 5px;
      &.display,
      &.func {
        padding-left: 10px;
        button:hover,
        button {
          background-color: white;
          color: $zeta-font-color;
          border-color: #dcdfe6;
        }
      }
    }
    .el-radio {
      /deep/ .el-radio__label {
        padding-left: 3px;
      }
    }
    .el-radio + .el-radio {
      margin-left: 5px !important;
    }
    .plot-btn-group {
      vertical-align: top;
    }
    i.icon {
      cursor: pointer;
      font-size: 18px;
      color: $zeta-font-color;
    }
    .el-button:not(.favorite-inner) {
      &:hover,
      &:focus {
        color: #60626b;
        border: 1px solid #dcdfe6;
        background-color: #fff;
      }
      &.active,
      &.active:focus {
        color: #569ce1;
        border-color: #cce1f6;
        background-color: #eef5fc;
      }
    }
    .tool-bar-group-left {
      display: flex;
    }
    .download{
      margin-left: 10px;
    }
  }
  .data-display {
    height: calc(
      100% - 70px
    ); // 30px + 30px + 10px for toolbar, 10px for padding.
    overflow: hidden;
    margin-top: 10px;
    .result-table {
      overflow: auto;
      height: 100%;
      width: 100%;
    }
    /deep/ .pivot-chart-render {
      overflow: auto;
    }
  }
}
.dashboard-popup {
  > ul.dashboard-list {
    min-height: 100px;
    overflow-y: auto;
    list-style: none;
    margin-top: -5px;
    > li {
      height: 25px;
      margin: 5px 5px 5px 0;
      &.create-dashboard {
        .el-input {
          width: 150px;
          /deep/ .el-input__inner {
            height: 25px;
          }
        }
      }
      &.dashboard-item,
      &.create-dashboard {
        display: flex;
        justify-content: space-between;
        .dashboard-item-actions {
          display: flex;
          align-items: center;
          /deep/ .el-button--text {
            color: $zeta-global-color;
          }
        }
        .dashboard-item-name {
          max-width: 150px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }
    }
  }
}
div.dashboard-no-data {
  text-align: center;
  font-size: 12px;
  margin: 0 20px;
  > .el-button {
    font-size: 12px;
  }
}
/deep/ .el-tabs__content{
  overflow: auto;
}
.el-tabs--border-card{
  height: calc(100% - 70px);
}
.info-overflow{
  overflow: auto;
  word-break: break-word;
  height: 100%;
}
</style>
