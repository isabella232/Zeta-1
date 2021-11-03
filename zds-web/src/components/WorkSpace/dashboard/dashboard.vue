<template>
  <div
    v-if="dashboard"
    class="dashboard"
  >
    <div class="dashboard-header">
      <div class="tools">
        <div v-if="!isShare">
          <!-- add text mode -->
          <el-button
            type="text"
            @click="addTextMode = !addTextMode"
          >
            <img
              class="tool-icon"
              :class="{'active':addTextMode}"
              :src="'./img/icons/icon_text.svg'"
            >
          </el-button>
          <!-- plot  -->
          <el-button
            v-if="activeItem && activeItem.type == 'plot'"
            type="text"
            @click="showEditor()"
          >
            <i
              style="font-size:20px;"
              class="zeta-icon-set"
            />
          </el-button>
          <!-- remove -->
          <el-button
            v-if="selectedItem >= 0"
            type="text"
            @click="deleteItem"
          >
            <i
              style="font-size:20px;"
              class="zeta-icon-delet"
            />
          </el-button>
        </div>
        <div v-else>
          <a
            :href="zetaUrl"
            target="_blank"
          ><el-button
            id="clone"
            plain
          >clone</el-button> </a>
        </div>
      </div>
      <div class="actions">
        <el-button
          v-if="!isShare"
          size="mini"
          type="primary"
          @click="saveDashboard()"
        >
          save
        </el-button>
        <ShareLink
          v-model="shareUrl"
          title="Share"
          class="tool-right"
        />
      </div>
    </div>
    <div
      v-if="items"
      v-loading="loading"
      class="dashboard-content"
    >
      <div
        ref="layoutContainer"
        class="layout-container"
        :class="{'add-text':addTextMode}"
        @mousemove="containerMouseMove"
        @mousedown="containerMouseDown"
        @mouseup="containerMouseUp"
        @click="(e) => itemUnselected()"
      >
        <grid-layout
          :layout="items"
          :col-num="12"
          :row-height="30"
          :is-draggable="!addTextMode"
          :is-resizable="!addTextMode"
          :vertical-compact="true"
          :use-css-transforms="false"
        >
          <div
            v-for="(item,$i) in items"
            :key="$i"
            @click.stop="(e) => itemSelected(e,item)"
          >
            <grid-item
              v-if="item.type == 'plot'"
              class="viz-item"
              :class="{'selected':selectedItem == item.i}"
              drag-allow-from=".dot"
              :x="item.x"
              :y="item.y"
              :w="item.w"
              :h="item.h"
              :i="item.i"
              @resized="() => onItemResized(item.statementId)"
            >
              <span class="dot">
                <i class="icon el-icon-rank" />
              </span>
              <!-- v-loading="!dashboard.queryMap[item.statementId]"  -->
              <!-- v-on:config-change='plotConfigChange' -->
              <!-- :showEditor.sync="showEditor" -->
              <VuePivoTable
                v-if="dashboard && dashboard.plotConfigMap"
                :ref="'pivotTable_'+ item.statementId"
                class="viz-item-render"
                :show-editor.sync="item.showEditor"
                :data-in="dashboard.dataMap[item.statementId]"
                :plot-config="dashboard.plotConfigMap[item.statementId]?dashboard.plotConfigMap[item.statementId]:{}"
                @config-change="(cfg) => vizItemConfigChange(item.statementId,cfg)"
              />
            </grid-item>
            <grid-item
              v-else-if="item.type == 'text'"
              class="viz-item viz-item-text"
              :class="{'selected':selectedItem == item.i}"
              drag-allow-from=".dot"
              :x="item.x"
              :y="item.y"
              :w="item.w"
              :h="item.h"
              :i="item.i"
            >
              <span class="dot">
                <i class="icon el-icon-rank" />
              </span>
              <textarea
                v-model="item.value"
                class="viz-item-textarea"
                style="height:calc(100% - 20px);width:calc(100% - 20px); padding: 10px"
                placeholder="Text here"
              />
            </grid-item>
          </div>
        </grid-layout>
        <div
          ref="selectedRectangle"
          class="selected-rectangle"
          :class="{'selected':adding}"
        />
      </div>
      <!-- <div class="perference-container">
                preference
            </div> -->
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Mixins, Prop, Inject, Provide, Watch } from 'vue-property-decorator';
import AddTextMixin from './add-text-mixin';
import $ from 'jquery';
import { GridLayout, GridItem } from 'vue-grid-layout';
import {
  Rectangle,
  LayoutConfig,
  LayoutType,
  Dashboard,
  DashboardQuery,
} from '@/types/workspace';
import DashboardRemoteService from '@/services/remote/Dashboard';
import _ from 'lodash';
import { WorkspaceSrv } from '@/services/Workspace.service';
import VuePivoTable, { PlotConfig } from '@/components/common/pivot-table';
import TableDisplay from '@/components/common/Visualization/TableDisplay.vue';
import ShareLink from '@/components/common/share-link';
import Util from '@/services/Util.service';
import { ZETA_ACTIONS } from '@/components/common/Navigator/nav.config';

const MAX_COL = 12;
const MAX_HEIGHT = 30;
function getXLength(lenth: number, containerLength: number) {
  return Math.round(lenth / containerLength * MAX_COL);
}
function getYLength(lenth: number, containerLength: number) {
  return Math.round(lenth / containerLength * MAX_HEIGHT);
}
function getGridItemInBottom(items: LayoutConfig[]): LayoutConfig | undefined {
  const lastItem = _.chain(items)
    .filter(item => item.x >= 0 && item.y >= 0)
    .maxBy('i')
    .value();
  return lastItem;
}
function preComputePosition(items: LayoutConfig[]): LayoutConfig[] {
  const configs = items;
  const unhandleItems = _.chain(items)
    .filter(item => item.x >= 0 && item.y >= 0)
    .value();
  if (!unhandleItems || unhandleItems.length <= 0) {
    return configs;
  }
  let lastItem = getGridItemInBottom(items);
  _.forEach(unhandleItems, item => {
    if (!lastItem) {
      item.x = 0;
      item.y = 0;
      lastItem = item;
    } else {
      const y = lastItem.y + lastItem.h + 1;
      item.x = item.x ? item.x : 0;
      item.y = y;
      lastItem = item;
    }
  });

  console.debug('preComputePosition', configs);

  return configs;
}
function postComputePosition(items: LayoutConfig[]): LayoutConfig[] {
  const configs = _.chain(items)
    .sortBy(['y', 'x'])
    .map((item: LayoutConfig, $i) => {
      item.i = $i;
      return item;
    })
    .value();
  console.debug('postComputePosition', configs);
  return configs;
}
@Component({
  components: {
    GridLayout,
    GridItem,
    VuePivoTable,
    TableDisplay,
    ShareLink
  },
  mixins: [AddTextMixin]
})
export default class DashboardComponent extends AddTextMixin {

  @Provide('dashboardRemoteService')
  dashboardRemoteService: DashboardRemoteService = new DashboardRemoteService();

  @Prop() dashboardId: string;
  @Prop() data: Dashboard;
  selectedItem = -1;
  items: LayoutConfig[] = [];
  unsaveConfig: Dict<PlotConfig | undefined> = {};
  loading = true;
  shareDashboard:  Dashboard = {} as Dashboard;
  isShare: boolean = Util.isShareApp();

  get $layoutContainer() {
    return $('.layout-container');
  }
  get dashboard(): Dashboard {
    if(this.isShare){
      return _.keys(this.shareDashboard).length > 0?this.shareDashboard:this.data;
    }
    return this.$store.state.workspace.workspaces[this.dashboardId];
  }
  get activeItem(): LayoutConfig | undefined {
    return _.find(this.items, cfg => cfg.i == this.selectedItem);
  }
  get shareUrl() {
    return `${location.protocol}//${location.host}/${Util.getPath()}share/#/dashboard?dashboardId=${this.dashboardId}`;
  }
  get zetaUrl() {
    const params = {
      dashboardId: this.dashboardId
    };
    return (
      `${location.protocol}//${location.host}/${Util.getPath()}#/repository` +
        `?internal_action=${ZETA_ACTIONS.CLONE_DASHBOARD}` +
        `&internal_action_params=${JSON.stringify(params)}`
    );
  }
  // eslint-disable-next-line @typescript-eslint/no-empty-function
  mounted() {}
  activated() {
    this.init();
  }
  deactivated() {
    this.saveDashboard();
  }
  getLayoutConfig(): LayoutConfig[] {
    let items = _.map(this.dashboard.layoutConfigs, cfg => {
      cfg.showEditor = false;
      const newCfg = this.computeSizeByCfg(cfg);
      return {
        ...cfg,
        ...newCfg
      };
    });
    items = preComputePosition(items);
    return items;
  }
  getStatementIds(items: LayoutConfig[]): number[] {
    const statementIds = _.chain(this.dashboard.layoutConfigs)
      .filter((cfg: LayoutConfig) => cfg.type != 'text')
      .map((cfg: LayoutConfig) => cfg.statementId)
      .value() as number[];
    return statementIds;
  }
  init() {
    // if (this.dashboard && this.dashboard.inited) {
    //   return;
    // }
    this.loading = true;
    this.items = this.getLayoutConfig();
    const statementIds = this.getStatementIds(this.items);
    if (statementIds && statementIds.length > 0) {
      this.loadPlotData(statementIds).then(() => (this.loading = false));
    } else {
      this.loading = false;
    }
  }
  loadPlotData(statementIds: number[]) {
    const queryMap: Dict<DashboardQuery> = {};
    const plotConfigMap: Dict<PlotConfig | undefined> = {};
    const plotDataDict: Dict<any[]> = {};
    const getQuery = (this.isShare) ? this.dashboardRemoteService.getShareStatementByIds:this.dashboardRemoteService.getQueryInfoByIds;
    return getQuery.call(this.dashboardRemoteService,statementIds).then(({ data }) => {
      _.chain(data)
        .map((d: any) => {
          const resultArr = JSON.parse(d.result).result as any[];
          const result: any = resultArr.find((r: any) => r.type === 'TABLE');
          if (!result || !result.schema || !result.rows) {
            return undefined;
          }
          const headers: any[] = result.schema;
          const rows = result.rows.map((row: any) => {
            const cols = Object.keys(row);
            const arr = cols.map(col => row[col]);
            return arr;
          });
          return {
            statementId: d.id,
            headers: headers,
            rows: rows,
            plotConfig: JSON.parse(d.plotConfig) || undefined
          };
        })
        .forEach(d => {
          if (!d) {
            return;
          }
          const statementId = d.statementId + '';
          queryMap[statementId] = {
            statementId: d.statementId,
            headers: d.headers,
            rows: d.rows
          };
          let dataIn: any[] = [];
          if (d.headers && d.rows) {
            dataIn = [d.headers, ...d.rows];
          }
          plotDataDict[statementId] = dataIn;
          plotConfigMap[statementId] = d.plotConfig;
        })
        .value();
      const db = WorkspaceSrv.dashboard(this.dashboard, {
        inited: true,
        queryMap: queryMap,
        plotConfigMap: plotConfigMap,
        dataMap: plotDataDict
      } as Dashboard);
      // add by huhan @2019-6-13
      if(this.isShare){
        this.shareDashboard = db;
        return;
      }
      //end
      this.$store.dispatch('updateWorkspace', db);
    });
  }
  onItemResized(id: string) {
    const pivotTable = (this.$refs['pivotTable_' + id] as any)[0];
    console.debug('onItemResized');
    if (pivotTable) {
      pivotTable.resize();
    }
  }
  saveDashboard() {
    this.loading = true;
    this.items = postComputePosition(this.items);
    console.debug('save',this.items);
    if (_.keys(this.unsaveConfig).length > 0) {
      this.dashboardRemoteService.updatePlottingConfigs(this.unsaveConfig)
        .then(() => {
          // reset unsave config
          const configMap = _.cloneDeep(this.dashboard.plotConfigMap) as Dict<
          PlotConfig | undefined
          >;
          _.forEach(this.unsaveConfig, (cfg, id) => {
            configMap[id] = cfg;
          });
          this.$store.dispatch('updateWorkspace', {
            notebookId: this.dashboardId,
            plotConfigMap: configMap
          });

          return this.dashboardRemoteService.updateDashboard(
            this.dashboardId,
            JSON.stringify(this.items)
          );
        })
        .then(() => {
          this.$store.dispatch('updateWorkspace', {
            layoutConfigs: this.items,
            notebookId: this.dashboardId
          });
          this.$store.dispatch('updateDashboard', {
            layoutConfigs: this.items,
            id: this.dashboardId
          });

          this.loading = false;
        });
    } else {
      this.dashboardRemoteService.updateDashboard(
        this.dashboardId,
        JSON.stringify(this.items)
      ).then(() => {
        this.$store.dispatch('updateWorkspace', {
          layoutConfigs: this.items,
          notebookId: this.dashboardId
        });
        this.$store.dispatch('updateDashboard', {
          layoutConfigs: this.items,
          id: this.dashboardId
        });
        this.loading = false;
      });
    }
  }
  itemSelected(e: Event, item: any) {
    if (this.addTextMode) {
      e.preventDefault();
      return;
    }

    console.debug('item click');
    this.selectedItem = item.i;
  }
  itemUnselected() {
    if (!this.addTextMode) {
      this.selectedItem = -1;
    }
  }
  vizItemConfigChange(
    statementId: string,
    cfg: PlotConfig,
    type: LayoutType = 'plot'
  ) {
    console.debug('config change');
    this.unsaveConfig[statementId] = cfg;
  }
  addText(rect: Rectangle) {
    const maxItem = _.maxBy(this.items, cfg => cfg.i);
    const newItem = this.computeSize(rect, 'text', maxItem ? maxItem.i + 1 : 0);
    this.items.push(newItem);
    this.addTextMode = false;
  }
  computeSizeByCfg(cfg: LayoutConfig): LayoutConfig {
    const type = cfg.type;
    // let rect = new Rectangle(cfg.x, cfg.y, cfg.w, cfg.h);
    return {
      x: cfg.x > 0 ? cfg.x : 0,
      y: cfg.y > 0 ? cfg.y : 0,
      w: cfg.w > 1 ? cfg.w : 1,
      h: cfg.h > 1 ? cfg.h : 1,
      i: cfg.i,
      type: type
    } as LayoutConfig;
  }
  computeSize(
    rect: Rectangle,
    type: LayoutType = 'text',
    index: number = this.items.length
  ): LayoutConfig {
    const containerWidth = $('.layout-container').width() as number;
    const containerHeight = $('.layout-container').height() as number;
    const x = getXLength(rect.left, containerWidth);
    const y = getYLength(rect.top, containerHeight);
    const w = getXLength(rect.width, containerWidth);
    const h = getYLength(rect.height, containerHeight);
    return {
      x: x > 0 ? x : 0,
      y: y > 0 ? y : 0,
      w: w > 1 ? w : 1,
      h: h > 1 ? h : 1,
      i: index,
      type: type
    } as LayoutConfig;
  }

  deleteItem() {
    this.items = _.filter(
      this.items,
      (cfg: LayoutConfig) => cfg.i != this.selectedItem
    );
    this.selectedItem = -1;
  }
  showEditor() {
    const item: LayoutConfig = this.items[this.selectedItem];
    if (item && item.type == 'plot') {
      item.showEditor = true;
    }
  }
  @Watch('dashboard')
  onChangeDashboard(newVal: Dashboard, oldVal: Dashboard){
    if(newVal && this.isShare){
      this.init();
    }
  }
}
</script>
<style lang="scss" scoped>
@import '@/styles/global.scss';
.dashboard {
  height: 100%;
  display: flex;
  flex-direction: column;
  border: 1px solid #ddd;
  > .dashboard-header {
    background-color: $zeta-label-lvl1;
    display: flex;
    justify-content: space-between;
    height: 40px;
    align-items: center;
    .tools {
      padding-left: 15px;
      img.tool-icon {
        width: 14px;
        &.active {
          background-color: #999;
        }
      }
      #clone{
        display: inline-block;
      }
    }
    .actions {
      display: flex;
      justify-content: flex-start;
      align-items: center;
      margin: 0 10px;
      >.tool-right{
        margin-left: 10px;
      }
    }
  }
  > .dashboard-content {
    display: flex;
    flex: 1 0;
    overflow: auto;
    > .layout-container {
      flex: 1 0;
      padding: 10px;
      border: 1px solid #ddd;
      position: relative;
      overflow-y: auto;
      &.add-text {
        cursor: crosshair;
      }
      .viz-item {
        border: 1px solid #999;
        position: relative;
        overflow: auto;
        padding-bottom: 5px;
        &.viz-item-text {
          overflow: hidden;
          .viz-item-textarea {
            outline-color: snow;
            resize: none;
            font-size: 18px;
          }
        }
        &.selected {
          border: 1px solid red;
        }
        .dot {
          position: absolute;
          width: 10px;
          height: 10px;
          top: 0;
          right: 5px;
          cursor: pointer;
          // border-radius: 5px;
          // background-color: #999;
          &::after {
            content: ' ';
          }
        }
        /deep/ .vue-resizable-handle {
          z-index: 1000;
        }
        .viz-item-render {
          overflow: auto;
          height: calc(100% - 10px);
          margin-right: 10px;
          margin-bottom: 10px;
        }
      }
      .selected-rectangle {
        display: none;
        position: absolute;
        border: 1px solid blue;
        &.selected {
          display: block;
        }
      }
    }
    > .perference-container {
      width: 200px;
      border-left: 1px solid #999;
    }
  }
}
</style>

