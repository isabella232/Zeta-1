<template>
  <div class="result-display">
    <div class="tool-bar">
      <span class="query-info">
        <span>
          Retrieved
          <strong>{{ rows.length }}</strong>
          rows in {{ elapsedTime }} seconds.
        </span>
        <span>
          <strong>{{ updatedCount }}</strong>
          rows affected.
        </span>
      </span>
      <br>
    </div>
    <div class="tool-bar-btn-group">
      <div class="tool-bar-group-left">
        <span class="display">
          <!-- <el-radio v-model="type" label="table">table</el-radio>
                    <el-radio v-model="type" label="plot">plot</el-radio>-->
          <el-button-group>
            <el-button
              v-click-metric:NB_RESULT_CLICK="{name: 'defaultTable'}"
              size="mini"
              :class="{'active':type=='table'}"
              @click="type = 'table'"
            >
              <!-- <img :src="'./img/icons/result_table.svg'"/> -->
              <i class="icon zeta-icon-table" />
            </el-button>
            <el-button
              v-click-metric:NB_RESULT_CLICK="{name: 'pivotTable'}"
              size="mini"
              :class="{'active':type=='plot'}"
              @click="type = 'plot'"
            >
              <!-- <img :src="'./img/icons/result_chart.svg'"/> -->
              <i class="icon zeta-icon-chart" />
            </el-button>
          </el-button-group>
        </span>
        <span class="func">
          <!-- <i  v-if="type == 'plot'" class="icon zeta-icon-set" @click="() => showEditor = true"/> -->
          <el-button-group
            v-if="type == 'plot'"
            class="plot-btn-group"
          >
            <el-button
              v-click-metric:NB_RESULT_CLICK="{name: 'pivotTableSetting'}"
              size="mini"
              @click="() => showEditor = true"
            >
              <i class="icon zeta-icon-set" />
            </el-button>
            <el-popover
              v-if="type =='plot'"
              placement="bottom"
              trigger="click"
              @after-leave="closePopover"
            >
              <el-button
                slot="reference"
                size="mini"
              >
                <i class="icon zeta-icon-add" />
              </el-button>
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
                        v-click-metric:NB_RESULT_CLICK="{name: 'createDashboard'}"
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
                        v-click-metric:NB_RESULT_CLICK="{name: 'add2Dashboard'}"
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
            </el-popover>
          </el-button-group>
          <el-button
            v-click-metric:NB_RESULT_CLICK="{name: 'exportCSV'}"
            size="mini"
            @click="exportCSVFile"
          >
            <i class="icon zeta-icon-download" />
          </el-button>
          <el-button
            size="mini"
            @click="openCodeDialog(query)"
          >
            <i class="zeta-icon-sql" />
          </el-button>
          <el-button
            v-click-metric:NB_RESULT_CLICK="{name: 'popupNewWindow'}"
            size="mini"
            @click="openNewTab"
          >
            <i class="icon zeta-icon-export" />
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
      <div class="tool-bar-group-right">
        <el-pagination
          v-show="type ==='table'"
          layout="sizes, prev, pager, next"
          :current-page.sync="currentPage"
          :page-sizes="[100,500,1000,2000,3000,5000]"
          :page-size="pageSize"
          :total="rows.length"
          @size-change="handleSizeChange"
        />
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
</template>

<script lang="ts">
const CHAR_CARRIAGE_RETURN = String.fromCharCode(13);
const CHAR_DOUBLE_QUOTES = String.fromCharCode(34);
const CHAR_LINE_FEED = String.fromCharCode(10);
const columnDelimiter = ',';
const rowDelimiter = '\n';
import { Component, Vue, Prop, Inject } from 'vue-property-decorator';
import _ from 'lodash';
import Util from '@/services/Util.service';
import VuePivoTable, { PlotConfig } from '@/components/common/pivot-table';
import TableDisplay from '@/components/common/Visualization/TableDisplay.vue';
import FavoriteIcon from '@/components/common/favorite-icon/favorite-icon-witn-store.vue';
import DashboardRemoteService from '@/services/remote/Dashboard';
import { IDashboardFile, LayoutConfig } from '@/types/workspace';
import { WorkspaceSrv } from '@/services/Workspace.service';
import { INotebook, IWorkspace } from '@/types/workspace';
import { WorkspaceManager } from '@/types/workspace/workspace-manager';
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import { Action, Getter } from 'vuex-class';
import { ZetaException } from '@/types/exception';
@Component({
  components: { VuePivoTable, TableDisplay, FavoriteIcon },
})
export default class PlotDisplay extends Vue {
  @Inject('dashboardRemoteService')
  dashboardRemoteService: DashboardRemoteService;
  @Inject()
  notebookRemoteService: NotebookRemoteService;
  @Inject()
  openCodeDialog: (code: string) => void;

  @Getter('userPreferenceByKey') userPreference: (key: string) => any;
  @Action updateUserPreference: (preference: Dict<any>) => Promise<any>;

  @Prop() notebook: INotebook;
  @Prop() statementId: number;
  @Prop() headers!: string[];
  @Prop() rows!: any[][];
  // @Prop() plotConfig!: PlotConfig;
  @Prop() elapsedTime!: number;
  @Prop() query: string;
  @Prop({ default: 0 }) updatedCount: number;
  showEditor = false;
  type: 'table' | 'plot' = 'table';
  newDashboard = '';
  createDBLoading = false;
  createNew = false;
  currentPage = 1;
  plotConfig: PlotConfig = {} as PlotConfig;

  get dataIn() {
    if (this.headers && this.rows) {
      const headers = this.headers;
      const rows = this.rows;
      return [headers, ...rows];
    } else {
      return [];
    }
  }
  get dashboardList(): IDashboardFile[] {
    const dashboards: Dict<IDashboardFile> = this.$store.getters
      .dashboardFiles;
    return _.chain(dashboards)
      .map(db => db)
      .value();
  }

  get pageSize () {
    const ps = this.userPreference('note_result_page_size') || 100;
    return ps;
  }
  set pageSize (val: number) {
    this.updateUserPreference({
      'note_result_page_size': val,
    }).catch((ex: ZetaException) => {
      ex.resolve();
    });
  }

  openNewTab() {
    const url= `${location.protocol}//${location.host}/${Util.getPath()}share/#/ResultView?statementId=${this.statementId}`;
    window.open(url, 'statement_' + this.statementId);
  }
  handleSizeChange(size: number){
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

  addToDashboard(db: IDashboardFile) {
    const layoutConfigs = db.layoutConfigs || [];
    const textCfg = new LayoutConfig(layoutConfigs.length, 'text');
    textCfg.value = 'Default Chart Title';
    layoutConfigs.push(textCfg);
    const plotCfg = new LayoutConfig(layoutConfigs.length, this.type);
    plotCfg.statementId = this.statementId;
    layoutConfigs.push(plotCfg);
    console.log('layoutConfigs', layoutConfigs);
    const dashboard =  this.$store.state.workspace.workspaces[db.id];
    if(dashboard){
      const plotDataDict: Dict<any[]> = dashboard.dataMap;
      plotDataDict[this.statementId] = this.dataIn;
      const newDB = WorkspaceSrv.dashboard(dashboard, {
        inited: dashboard.inited,
        queryMap: dashboard.queryMap,
        plotConfigMap: dashboard.plotConfigMap,
        dataMap: plotDataDict,
      });
      this.$store.dispatch('updateWorkspace', newDB);
    }
    this.createDBLoading = true;
    /**
     * favorite statement
     * @here add statement to dashboad is nothing to do with favorite statement
     */
    this.notebookRemoteService.favoriteSharedNotebook(this.statementId + '', true, Util.getNt(), 'statement').then(() => {
      this.$store.dispatch('favorite', {
        id: this.statementId + '',
        favoriteType: 'statement',
      });
    });
    this.dashboardRemoteService.updateDashboard(db.id, JSON.stringify(layoutConfigs)).then(
      () => {
        this.$store.dispatch('updateDashboard', {
          inited: false,
          id: db.id,
          layoutConfigs: layoutConfigs,
        });
        this.createDBLoading = false;
      }
    );
  }
  createDashboard(name: string = this.newDashboard) {
    this.createDBLoading = true;
    this.dashboardRemoteService.createDashboard(name).then(({ data: db }) => {
      this.$store.dispatch('addDashboard', { dashboard: db });
      this.createDBLoading = false;
      this.newDashboard = '';
    });
  }

  plotConfigChange(newCfg: PlotConfig) {
    this.plotConfig = newCfg;
    this.dashboardRemoteService.updatePlottingConfig(this.statementId, this.plotConfig);
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
}
</script>

<style lang="scss" scoped>
@import "@/styles/global.scss";
.result-display {
    box-sizing: border-box;
    padding: 10px;
    height: 100%;
    width: 100%;
    min-width: 850px;
    overflow: hidden;
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
        .tool-bar-group-left{
            display: flex;
        }
    }
    .data-display {
        height: calc(100% - 70px); //30px + 30px for button and information, 10px for padding.
        width: 100%;
        overflow: auto;
        margin-top: 10px;
        .result-table {
            overflow: hidden;
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
// fix issues in safari
/deep/ .plot-btn-group {
  >  span {
    margin-left: 0 !important;
    display: block;
    float: left;
  }

}
</style>
