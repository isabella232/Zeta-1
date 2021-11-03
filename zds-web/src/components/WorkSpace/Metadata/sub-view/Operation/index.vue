<template>
  <div class="op-info-wrapper">
    <h1 id="general">
      General Info
    </h1>
    <div class="general-info">
      <div class="section-left">
        <div class="detail-row">
          <label>Batch Account</label>
          <span>{{ general.batchAccount }}</span>
        </div>
        <div class="detail-row">
          <label>Last Update Time</label>
          <span>
            <div
              v-for="i in general.generalInfo"
              :key="i.pltfrm_name"
            >
              {{ i.latest_finish_time }} PDT
              <i
                class="platform-icon"
                :class="i.pltfrm_name.toLowerCase()"
              />
            </div>
          </span>
        </div>
        <div class="detail-row">
          <label>Avg Update Time</label>
          <span>
            <div
              v-for="i in general.generalInfo"
              :key="i.pltfrm_name"
            >
              {{ i.avg_finish_time.substr(0, 8) }} PDT
              <i
                class="platform-icon"
                :class="i.pltfrm_name.toLowerCase()"
              />
            </div>
          </span>
        </div>
        <div class="detail-row">
          <label>Data Latency</label>
          <span>{{ general.latency }}</span>
        </div>
        <div class="detail-row">
          <label>Done File
            <el-tooltip
              content="Bottom center"
              placement="bottom"
              effect="light"
              popper-class="dependency-info-popup"
            >
              <div slot="content">
                <p>We support done files under folder /dw/etl/home/prod/watch on ETL Servers.</p>
                <p>Currently we don't support done file on Hadoop HDFS.</p>
              </div>
              <i class="el-icon-warning" />
            </el-tooltip>
          </label>
          <span>
            <div
              v-for="(val, key, index) in doneFiles"
              :key="key"
            >
              {{ val[0].file_name }}
              <i
                v-for="p in val"
                :key="p.pltfrm_name + index"
                class="platform-icon"
                :class="p.pltfrm_name.toLowerCase()"
              />
            </div>
          </span>
        </div>
        <div class="detail-row">
          <label>Update Frequency</label>
          <span style="text-transform:capitalize;">{{ general.frequency }}</span>
        </div>
      </div>
      <div class="section-right">
        <div class="detail-row">
          <label>Quality Validation</label>
          <span>
            <div>As of {{ general.currentDate }}</div>
            <div
              v-for="rule in general.dqRules"
              :key="rule.id"
              class="rule-status"
            >
              <a
                :href="`https://ido.corp.ebay.com/rules/recon/detail?id=${rule.id}`"
                target="_blank"
              >Rule {{ rule.id }}</a>
              <span :class="rule.status">
                <i
                  class="el-icon"
                  :class="rule.status === 'pass' ? 'el-icon-success' : 'el-icon-error'"
                />
                {{ rule.status }}
              </span>

            </div>
          </span>
        </div>
      </div>
    </div>
    <h1 id="job">
      Job Detail
    </h1>
    <el-tabs
      v-model="chart"
      type="card"
    >
      <el-tab-pane
        v-loading="treeLoading"
        name="flow"
        style="position:relative;"
      >
        <span slot="label">Daily Job Flow 
          <el-popover
            trigger="hover"
            content="Status of Hercules"
          >
            <i
              slot="reference"
              class="zeta-icon-info"
              style="color:#aaa"
            />
          </el-popover>
        </span>
        <div class="chart-header">
          <div
            class="tree-breadcrumb"
            :style="{ visibility: treeChartBreadcrumb.length <= 1 ? 'hidden' : 'visible' }"
          >
            <a
              v-for="i in treeChartBreadcrumb"
              :key="i"
              @click="() => chartNav(i)"
            >{{ i }}</a>
          </div>
          <div class="tree-legend">
            <span
              v-for="s in tableStatus"
              :key="s.name"
            >
              <i :style="{ backgroundColor: s.color }" />
              <span>{{ s.name }}</span>
            </span>
          </div>
        </div>
        <tree-chart
          :data="treeData"
          :width="1000"
          @expand="expand"
          @mouseenter="showTooltip"
          @mouseleave="mouseLeave"
        />
        <tooltip
          :data="tooltipData"
          @mouseenter="() => tooltipCounter++"
          @mouseleave="() => tooltipCounter--"
          @request="onRequest"
        />
      </el-tab-pane>
      <el-tab-pane
        v-loading="historyLoading"
        label="Last 30D History"
        name="history"
        style="position:relative;"
      >
        <el-select
          v-model="historyPlatform"
          size="mini"
          style="position:absolute;top:0;z-index:99;"
        >
          <el-option
            label="Apollo RNO/Hermes"
            value="apollo_rno"
          />
          <el-option
            label="Hercules"
            value="hercules"
          />
          <el-option
            label="Mozart"
            value="numozart"
          />
        </el-select>
        <v-chart
          ref="line"
          :options="lineChartOptions"
          autoresize
          style="width:100%;height:400px;"
        />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch } from 'vue-property-decorator';
import TreeChart from './Tree.vue';
import Tooltip from './Tooltip.vue';
import ECharts from 'vue-echarts';
import moment from 'moment';
import 'echarts/lib/chart/line';
import DoeRemoteService from '@/services/remote/DoeRemoteService';
import _, { forEach } from 'lodash';
import { lineInitOptions, tableStatus, labelFormatter, getHistoryData } from './chartOptions';
import Util from '@/services/Util.service';
import NotebookRemoteService from '../../../../../services/remote/NotebookRemoteService';
import { ZetaExceptionProps } from '@/types/exception';

const api = DoeRemoteService.instance;
const notebookRemoteService = new NotebookRemoteService();
@Component({
  components: {
    'v-chart': ECharts,
    TreeChart,
    Tooltip,
  },
})
export default class OperationInfo extends Vue {

  @Prop() 
  table: string;
  @Prop()
  dbs: any;

  get tableName () {
    const [db, name] = this.table.split('.');
    return (name || db).toLowerCase();
  }

  general = {
    batchAccount : '',
    currentDate : moment().format('YYYY-MM-DD'),
    dqRules: [],
    generalInfo: [],
    latency : 'n/a',
    frequency : 'n/a',
  };

  chart: 'flow' | 'history' = 'flow';
  tableStatus = tableStatus;
  tables: Array<any> = [];
  treeLoading = false;
  doneFileloading = false;
  treeChartBreadcrumb: string[] = [];
  tooltipData: object | null = null;
  tooltipCounter = 0;
  historyLoading = false;
  historyPlatform: 'hercules' | 'apollo_rno' | 'numozart' = 'hercules';
  history: Array<any> = [];

  remove (index: number, rows: Array<any>) {
    rows.splice(index, 1);
  }

  get currentTableKey () {
    // pick last in the stack
    return this.treeChartBreadcrumb[this.treeChartBreadcrumb.length - 1];
  }

  get treeData () {
    this.tables.forEach(t => {
      const parent = this.tables.find(i => i.full_name === t.down_table);
      parent && parent.children.push(t);
    });
    return this.tables[0]; // the first node is the root
  }

  // get treeChartSize() {
  //   // get size of each level and pick max
  //   let size = 0;
  //   let level = 0;
  //   let levelItems = [this.treeData].filter(Boolean);
  //   while (levelItems.length > 0) {
  //     const currentLevelSize = _.sumBy(levelItems, i => i.children.length);
  //     if (currentLevelSize > size) size = currentLevelSize;
  //     levelItems = _.flatten(levelItems.map(i => i.children));
  //     level++;
  //   }
  //   return [level < 3 ? '60%' : '100%', size * 34 + 'px'];
  // }

  get doneFiles () {
    return _.groupBy(this.general.generalInfo, 'file_name');
  }

  get doneFileIsEmpty () {
    return _.isEmpty(this.doneFiles)
  }

  get lineChartOptions () {
    const series = this.history.filter(h => h.hist.length > 0).map(h => {
      return {
        type: 'line', 
        name: h.table_name,
        data: getHistoryData(h.hist),
        markLine: {
          lineStyle: {
            type: 'dashed',
          },
          label: {
            formatter: ({ value }) => 'SLA: ' + labelFormatter(value),
          },
          data: [
            { 
              name: 'SLA',
              yAxis: moment(h.sla_time, 'HH:mm:ssZ').valueOf(), 
            },
          ],
        },
      };
    });
    return {
      ...lineInitOptions,
      legend: {
        right: 0,
        icon: 'roundRect',
        width: '82%',
        selected: series.reduce((prev, next, i) => { prev[next.name] = i === 0; return prev;},  {}),
        selector: [
          {
            type: 'all or inverse',
            title: 'Inverse selection',
          },
        ],
        selectorLabel: {
          borderWidth: 0,
          color: '#569ce1',
          backgroundColor: 'transparent',
        },
        data: series.map(s => s.name),
      },
      series,
    };
  }

  @Watch('treeData')
  resize () {
    if (this.treeData) {
      Vue.nextTick(() => {
        // (this.$refs.tree as any).chart.resize();
      });
    }
  }

  @Watch('historyPlatform')
  @Watch('chart')
  loadHistory () {
    if (this.chart === 'history') {
      this.historyLoading = true;
      api.getUpstreamHistory(this.tableName, this.historyPlatform).then(res => {
        this.history = res;
      }).finally(() => this.historyLoading = false);
    }
  }

  mounted () {
    this.getData();
  }

  getData () {
    const props: ZetaExceptionProps  = {
      path: 'metadata',
    };
    api.props(props);

    api.getDQStatus(this.tableName).then(dq => {
      this.general.dqRules = dq;
    });
    api.getBatchAccount(this.tableName).then((res) => this.general.batchAccount = res.batch_acct);
    api.getTableOperationInfo(this.tableName).then((res) => {
      this.general.generalInfo = res;
    });
    this.getTreeNodes(this.tableName);
    
    api.getLatency(this.tableName).then(res => {
      this.general.latency = res.latency;
    });
    api.getFrequency(this.tableName).then(res => {
      this.general.frequency = res.freq;
    });
  }

  expand ({ data }) {
    if (data.expandable && data.children.length === 0) {
      this.tables = [];
      this.getTreeNodes(data.table_name);
    }
  }

  async onRequest ({ data }) {
    if (data.info.status === 'unknown') {
      const { data: { value: [tableInfo] }} = await notebookRemoteService.queryTable(data.table_name, data.platform); // FIXME: what about DB
      const userInfo = await api.getUser(Util.getNt()).then(res => {return res.data.response.docs[0];});
      const name = userInfo.first_name || Util.getNt();
      if (!tableInfo) {
        this.$message.error('Table not found');
        return;
      }
      const link = `${location.protocol}//${location.host}/${Util.getPath()}#/metadata?tableName=${data.table_name}&tab=Operation`;
      const content ={
        name: tableInfo.info.prmry_dev_sae || tableInfo.info.prmry_dev_sae_nt,
        msg: `[${name}] requests dependency signal of table [${data.full_name}] on platform [${data.platform}]
        <br/>
        Please update <strong>done file</strong> for <a href='${link}'>link</a> in Zeta.
      `,
      };
      const param = {
        subject: `Request Dependency Signal of table [${data.full_name}]`,
        content: content,
        ccAddr: '',
        sae: tableInfo.info.prmry_dev_sae_nt,
      };
      this.sendMail(param);
    }
  }
  async sendMail (param: any) {
    const toAddr = ['stachen@ebay.com', 'gzhu@ebay.com', 'binsong@ebay.com'];
    toAddr.push(`${Util.getNt()}@ebay.com`);
    if (param.sae){
      toAddr.push(`${param.sae}@ebay.com`);
    }
    const params: any = {
      fromAddr: 'DL-eBay-ZETA@ebay.com',
      toAddr: toAddr.join(';'),
      subject: param.subject,
      content: JSON.stringify(param.content),
      template: 'ZetaNotification',
      ccAddr: '',
      type: 3, //1: html; 2: txt
    };
    api
      .createEmail(params)
      .then(res => {
        if (res && res.status == 200) {
          this.$message.success('Your request successfully sent.');
        }
      })
      .catch(() => {
        this.$message.error('Request failed');
      });
  }

  showTooltip (evt, data) {
    this.tooltipCounter ++;
    this.tooltipData = {
      data: data.data,
      x: evt.layerX,
      y: evt.layerY,
    };
  }

  mouseLeave () {
    setTimeout(() => {
      this.tooltipCounter--;
    }, 100);
  }

  @Watch('tooltipCounter')
  hideTooltip () {
    if (this.tooltipCounter === 0) {
      this.tooltipData = null;
    }
  }

  chartNav (tableName: string) {
    const i = this.treeChartBreadcrumb.findIndex(v => v === tableName);
    this.getTreeNodes(tableName);
    this.treeChartBreadcrumb.splice(i);
  }

  async getTreeNodes (tableName: string) {
    this.treeLoading = true;
    const res = await api.getTableProcessStatus(tableName);
    this.treeChartBreadcrumb.push(tableName);
    const root = res.value[0];
    this.tables = res.value.filter((t, i) => {
      // filter out the target table itself
      return i === 0 || t.full_name !== root.full_name;
    }).map(t => {
      return {
        ...t,
        name: t.full_name,
        children: [],
      };
    });
    this.treeLoading = false;
  }
}
</script>

<style lang="scss" scoped>
@import '@/styles/metadata.scss';

.op-info-wrapper {
  overflow: scroll;
  h1 {
    font-size: 20px;
    margin: 20px 0 8px;
  }
  .general-info {
    display: flex;
    > div {
      width: 100%;
    }
  } 
  .detail-row {
    padding: 8px 0;
    label {
      display: inline-block;
      width: 180px;
      font-weight: bold;
    }
    > span {
      display: inline-block;
      vertical-align: text-top;
      word-break: break-word;
    }
    .rule-status {
      > a {
        text-decoration: none;
      }
      > span:first-child {
        min-width: 60px;
        display: inline-block;
      }
      i {
        position: relative;
        top: 3px;
      }
      .pass,
      .fail {
        padding-left: 6px;
        text-transform: capitalize;
      }
      .pass {
        color: rgb(104,204, 147);
      }
      .fail {
        color: rgb(212, 45, 31);
      }
    }
  }

  .chart-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    .tree-legend {
      font-weight: bold;
      i {
        display: inline-block;
        margin-left: 50px;
        margin-right: 6px;
        width: .7em;
        height: .7em;
        border-radius: 861112px;
      }
    }
    .tree-breadcrumb {
      padding-left: 20px;
      > a {
        cursor: pointer;
        &:not(:last-of-type)::after {
          content: '>';
          color: #999;
          display: inline-block;
          margin: 0 12px;
          top: 1px;
          position: relative;
        }

        &:last-of-type {
          cursor: initial;
          pointer-events: none;
          color: inherit;
        }
      }
    }
  }

}
.zeta-icon-edit {
  cursor: pointer;
  color: #569ce1;
}
.zeta-icon-edit:hover {
  color: #4d8cca;
}
</style>