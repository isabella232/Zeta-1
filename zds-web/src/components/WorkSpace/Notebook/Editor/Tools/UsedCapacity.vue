<template>
  <el-popover
    placement="bottom"
    trigger="click"
    class="used-capacity"
  >
    <button
      slot="reference"
      v-click-metric:NB_TOOLBAR_CLICK="{name: 'capacity'}"
      type="text"
      class="notebook-tool-btn"
      :disabled="!enable"
    >
      <span class="tool-name">Queue Capacity:</span>
      <div
        v-if="enable && percentage >= 0"
        class="used-capacity-percentage"
        :class="percentage === -1 ? 'used-capacity-grey' : percentage < 60 ? 'used-capacity-green' : percentage < 80 ? 'used-capacity-orange': 'used-capacity-red'"
      >
        {{ percentage }}%
      </div>
      <div
        v-else
        class="used-capacity-percentage used-capacity-grey"
      >
        N/A
      </div>
    </button>
    <div class="notebook-tool">
      <h2>{{ popupTitle }}</h2>
      <el-table
        border
        :data="gridData"
        height="350px"
        :show-header="false"
      >
        <el-table-column
          width="120px"
          property="nt"
        />
        <el-table-column
          min-width="260px"
          label
        >
          <template
            slot-scope="scope"
          >
            {{ `usage:${Math.round(scope.row.usage * 100)}%` }}
          </template>
        </el-table-column>
      </el-table>
    </div>
  </el-popover>
</template>
<script lang="ts">
import { Component, Vue, Prop, Watch, Emit, Inject } from 'vue-property-decorator';
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import _ from 'lodash';
import { INotebook } from '@/types/workspace';
import { isHermes } from '@/services/connection.service';

const enableClusterList = [16, 14, 10, 11, 2, 3];

@Component
export default class UsedCapacity extends Vue {
  @Inject()
  notebookRemoteService: NotebookRemoteService;
  @Prop() notebookId: string;
  @Prop() value: number | null | undefined;
  queueName = '';
  percentage = -1;
  interval = 0;
  gridData: any[] = [];

  get notebook(): INotebook | undefined {
    return this.$store.getters.nbByNId(this.notebookId);
  }
  get enable(): boolean {
    const clusterId = this.notebook && this.notebook.connection ? this.notebook.connection.clusterId : -1;
    return enableClusterList.indexOf(clusterId) !== -1;
  }
  get popupTitle(): string {
    return `Top users in queue ${this.queueName}`;
  }
  @Watch('notebook.connection.clusterId')
  onConnectionClusterChange() {
    if (this.interval) {
      this.clearInterval();
    }
    this.getStatus();
    this.setInterval();
  }
  @Emit('input')
  // eslint-disable-next-line @typescript-eslint/no-unused-vars,@typescript-eslint/no-empty-function
  onPercentageChange(percentage: number) {}

  mounted() {
    this.setInterval();
  }
  activated() {
    this.setInterval();
  }
  destroyed() {
    this.resetValue();
    this.clearInterval();
  }
  deactivated() {
    this.resetValue();
    this.clearInterval();
  }
  private resetValue() {
    this.percentage = -1;
    this.onPercentageChange(this.percentage);
  }
  private setInterval() {
    if(this.interval > 0) {
      return;
    }
    const clusterId = this.notebook && this.notebook.connection ? this.notebook.connection.clusterId : -1;
    const time = isHermes(clusterId) ? 30 * 1000 : 60 * 1000;
    this.interval = window.setInterval(this.getStatus, time);
    this.getStatus();
  }
  private clearInterval() {
    window.clearInterval(this.interval);
    this.interval = 0;
  }
  async getStatus() {
    if (!this.enable) {
      this.percentage = -1;
      this.onPercentageChange(this.percentage);
      return;
    }
    try {
      const clusterId = this.notebook && this.notebook.connection ? this.notebook.connection.clusterId : -1;
      const res = await this.notebookRemoteService.getClusterStatus(
        this.notebookId,
        clusterId
      );
      if (res.status === 200 && res.data[0]) {
        this.queueName = res.data[0].queueName;
        /* force convert to number */
        this.percentage = parseInt('' + res.data[0].usedPct * 100);
        if(!_.isEmpty(res.data[0].users)) {
          this.gridData = _.chain(res.data[0].users)
            .map((usage: number, nt: string) => {
              return {
                usage,
                nt,
              };
            })
            .sortBy(user => user.usage)
            .value()
            .reverse();
        }
      } else {
        throw new Error('response error: cannot get used capacity');
      }
    } catch (e) {
      this.percentage = -1;
      this.gridData = [];
      console.error(e);
      if(e.resolve) {
        e.resolve();
      }
    } finally {
      this.onPercentageChange(this.percentage);
    }
  }
}
</script>
<style lang="scss" scoped>
.notebook-tool {
    margin: 0 12px 12px 12px;
    > h2 {
        line-height: 40px;
        margin-bottom: 5px;
    }
    // /deep/ .el-table__row {
        // td {
            // padding: 5px 0;
        // }
    // }
}
.used-capacity {
    padding-right: 30px;
    > .notebook-tool-btn[disabled="disabled"] {
        cursor: not-allowed;
    }
}

.used-capacity-percentage {
    width: 42px;
    height: 20px;
    line-height: 20px;
    margin-left: 10px;
    text-align: center;
    color: #fff;
    border-radius: 5px;
    font-size: 11px;
    &.used-capacity-grey {
        background-color: #d7d7d7;
    }
    &.used-capacity-green {
        background-color: #5cb85c;
    }
    &.used-capacity-orange {
        background-color: #ff6600;
    }
    &.used-capacity-red {
        background-color: #cc0000;
    }
}
</style>
