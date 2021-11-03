<template>
  <el-dialog
    :visible.sync="visible"
    custom-class="used-capacity"
    width="500px"
  >
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
            {{ `memory:${Math.round(scope.row.memory * 100)}%` }}
          </template>
        </el-table-column>
      </el-table>
    </div>
  </el-dialog>
</template>
<script lang="ts">
import { Component, Vue, Prop, Watch, Emit, Provide } from 'vue-property-decorator';
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import _ from 'lodash';

const enableClusterList = [14];

@Component({
  components: {}
})
export default class UsedCapacity extends Vue {
  @Provide('notebookRemoteService')
  notebookRemoteService = new NotebookRemoteService();
  @Prop() show: boolean;
  value: number | null | undefined = null;
  queueName = '';
  percentage = -1;
  interval = 0;
  gridData: any[] = [];
  clusterId = 14;
  visible = false;
  queue_name = 'auto';

  get enable(): boolean {
    const clusterId = this.clusterId;
    return enableClusterList.indexOf(clusterId) !== -1;
  }
  get popupTitle(): string {
    return `Top users in queue ${this.queueName}`;
  }

  @Emit('percentage-change')
  // eslint-disable-next-line @typescript-eslint/no-unused-vars,@typescript-eslint/no-empty-function
  onPercentageChange(percentage: number) {}

  open(){
    this.visible = true;
  }
  setParams(params){
    if(params.queueName  == this.queue_name) return;
    this.queue_name = params.queueName;
    this.clusterId = params.clusterName.indexOf('apollorno')>-1?14:-1;
    this.setInterval();
  }
  destroyed() {
    this.resetValue();
    this.clearInterval();
  }
  @Watch('show')
  onChange(){
    console.log('show',this.show);
    if(this.show){
      this.setInterval();
    }else{
      this.resetValue();
      this.clearInterval();
    }
  }
  private resetValue() {
    this.percentage = -1;
    this.onPercentageChange(this.percentage);
  }
  private setInterval() {
    if(this.interval > 0) {
      this.clearInterval();
    }
    this.interval = window.setInterval(this.getStatus, 60 * 1000);
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
      const res = await this.notebookRemoteService.getClusterQueue(
        this.clusterId,
        this.queue_name
      );
      if (res.status === 200 && res.data[0]) {
        this.queueName = res.data[0].queueName;
        /* force convert to number */
        this.percentage = parseInt('' + res.data[0].usedPct * 100);
        if(!_.isEmpty(res.data[0].users)) {
          this.gridData = _.chain(res.data[0].users)
            .map((memory: number, nt: string) => {
              return {
                memory,
                nt,
              };
            })
            .sortBy(user => user.memory)
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
