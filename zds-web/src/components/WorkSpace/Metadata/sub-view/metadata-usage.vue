<template>
  <div class="graph-chart-wrapper">
    <div class="input-wrapper">
      <span>Choose Manager Hierarchy: </span>
      <el-select
        v-model="depth"
        style="width: 84px"
      >
        <el-option
          v-for="i in [2, 3, 4, 5]"
          :key="i"
          :label="`Lvl ${i}`"
          :value="i"
        />
      </el-select>
      <el-radio-group
        v-model="view"
        size="mini"
        style="vertical-align:super;"
      >
        <el-radio-button label="table">
          <i class="el-icon-date" />
        </el-radio-button>
        <el-radio-button label="chart">
          <i class="el-icon-pie-chart" />
        </el-radio-button>
      </el-radio-group>
    </div>
    <div class="btn-wrapper">
      <el-button
        class="refresh-btn"
        icon="el-icon-refresh"
        circle
        @click="reload"
      />
      <!-- <el-button
        v-if="!loading"
        class="config-btn"
        :type="showConfig ? 'primary' : undefined"
        icon="el-icon-setting"
        circle
        @click="showConfig = !showConfig"
      /> -->
    </div>
    <div
      v-loading="loading"
      style="min-height:500px;margin-top:10px;"
      :element-loading-text="loadingText"
    >
      <div
        v-show="view === 'table'"
        class="table-wrapper"
      >
        <el-table
          :data="tableData"
          style="width: 60%"
        >
          <el-table-column
            :label="`L${depth} Manager`"
          >
            <template slot-scope="scope">
              {{ scope.row.mngrInfo.firstName }}, {{ scope.row.mngrInfo.lastName }} ({{ scope.row.mngrInfo.ntlogin }})
            </template>
          </el-table-column>
          <el-table-column
            prop="userCnt"
            label="Unique User Count"
          />
        </el-table>
      </div>
      <div v-show="view === 'chart'">
        <div id="viz" />
        <div class="zoom-wrapper">
          <i
            class="el-icon-zoom-in"
            @click="zoom(0.1)"
          />
          <i
            class="el-icon-zoom-out"
            @click="zoom(-0.1)"
          />
        </div>
      </div>
    </div>
    <div
      v-show="showConfig"
      id="config-wrapper"
      ref="configWrapper"
    />
  </div>
</template>

<script lang="ts">
/* eslint-disable @typescript-eslint/camelcase */
import { Component, Vue, Prop, Watch } from 'vue-property-decorator';
import NeoVis from '@/components/common/neovis/neovis';
import DoeRemoteService from '@/services/remote/DoeRemoteService';
import { orderBy } from 'lodash';

const API = DoeRemoteService.instance;
const DEFAULT_CONFIG = {
  container_id: 'viz',
  // console_debug: true,
  arrows: true,
  configure: {
    enabled: true,
    container_id: 'config-wrapper',
  },
  labels: {
    Employee: {
      caption: ({ properties: p }) => `${p.firstName} ${p.lastName}`, 
    },
  },
  relationships: {
    'REPORT_TO': {
      caption: false,
    },
  },
};

@Component({})
export default class MetadataUsage extends Vue {

  static viz: any = null;
  static renderedTable = '';
  @Prop() tableId;

  loading = false;
  loadingText = 'Querying...';
  showConfig = false;
  view: 'table' | 'chart' = 'table';
  depth = 2;
  tableData: any[] = [];

  mounted () {
    if (!MetadataUsage.renderedTable !== this.tableId) {
      this.reload();
    }
  }
  beforeDestroy () {
    if (MetadataUsage.viz) {
      MetadataUsage.viz.stabilize();
      MetadataUsage.viz.clearNetwork();
      MetadataUsage.viz = null;
    }
  }
  zoom (step: -.1 | .1) {
    if (MetadataUsage.viz) {
      const visNetwork = MetadataUsage.viz._network;
      visNetwork.moveTo({
        animation: true,
        scale: visNetwork.getScale() + step,
      });
    }
  }
  ensureVis () {
    if (MetadataUsage.viz) {
      return;
    }
    const config = { ...DEFAULT_CONFIG };
    const viz = new NeoVis(config);
    MetadataUsage.viz = viz;
    this.registerEvents();
  }
  registerEvents () {
    const viz = MetadataUsage.viz;
    viz.registerOnEvent('completed', ({ record_count }) => {
      if (record_count === 0) {
        this.$message.warning({ message: 'No User Usage', offset: 450 });
        this.loading = false;
      }
    });
    viz.registerOnEvent('startStabilizing', () => {
      this.loadingText = 'Stabilizing & Drawing...';
    });
    viz.registerOnEvent('stabilizationIterationsDone', () => {
      this.loading = false;
    });
  }

  @Watch('depth')
  @Watch('tableId')
  @Watch('view')
  reload () {
    this.loading = true;
    this.loadingText = 'Querying...';
    this.showConfig = false;
    if (this.view === 'table') {
      API.getUsagePersonaSummary(this.tableId, this.depth).then(res => {
        this.tableData = orderBy(res, i => i.userCnt, ['desc']);
      }).finally(() => this.loading = false);
    } else {
      if (MetadataUsage.renderedTable === this.tableId) {
        this.loading = false;
        return;
      }
      (this.$refs.configWrapper as Element).innerHTML = '';
      this.ensureVis();
      MetadataUsage.viz!.data  = async ()=>{
        const res = await API.getUsagePersona(this.tableId, this.depth);
        return res;
      };
      MetadataUsage.viz!.render();
      MetadataUsage.renderedTable = this.tableId;
    }
  }
}
</script>

<style lang="scss" scoped>
.graph-chart-wrapper {
  position: relative;
  padding: 20px;

  .btn-wrapper {
    position: absolute;
    right: 20px;
    top: 20px;
    z-index: 999;
    > * {
      min-width: 0;
    }
  }
  .zoom-wrapper {
    position: absolute;
    right: 20px;
    top: 40%;
    z-index: 999;
    i {
      display: block;
      font-size: 30px;
      cursor: pointer;
      margin: 20px 0;
      color: #569CE1;
    }
    i:hover {
      color: #4D8CCA;
    }
  }
  .table-wrapper {
    ::v-deep .el-table th {
      background-color: #F5F5F5;
    }
  }
  #config-wrapper {
    position: absolute;
    top: 16px;
    right: 0;
    background: white;
    border: 1px solid #ccc;
    padding: 0 20px;
    height: 80vh;
    overflow: scroll;
  }
  #viz {
    width: 100%;
    height: calc(100vh - 220px);
    *:focus {
      outline: none;
    }
  }
}
</style>