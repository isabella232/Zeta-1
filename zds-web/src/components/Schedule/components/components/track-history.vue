<template>
  <el-popover
    placement="bottom"
    width="500"
    trigger="click"
    @show="init()"
  >
    <el-table
      v-loading="loading"
      :data="list"
      border
      max-height="300"
      class="history-table"
    >
      <el-table-column
        label="Action Taker"
        prop="nt"
      />
      <el-table-column
        label="Action"
        prop="description"
        min-width="160"
      >
        <template slot-scope="scope">
          {{ trackDesc(scope.row) }}
          <span
            v-if="scope.row.comments"
            class="error-info"
          ><strong>Reason:</strong> {{ scope.row.comments }}</span>
        </template>
      </el-table-column>
      <el-table-column
        label="Time"
        prop="createTime"
        width="160"
      >
        <template slot-scope="scope">
          {{ formatDateFromTimestamps(scope.row.createTime) }}
        </template>
      </el-table-column>
    </el-table>
    <span slot="reference" class="view-details">Check Log</span>
  </el-popover>
</template>

<script lang="ts">
import { Component, Vue, Prop, Inject } from 'vue-property-decorator';
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import {
  ScheduleJobStatus,
} from '@/components/common/schedule-container';
import { ZetaException } from '@/types/exception';
import ScheduleUtil from '../../util';

@Component
export default class TrackHistory extends Vue {
  @Inject('notebookRemoteService') notebookRemoteService: NotebookRemoteService;
  @Prop() jobId: number;
  loading = false;
  list = [];

  init (){
    this.loading = true;
    this.notebookRemoteService.getJobTrackLog(this.jobId).then(({ data })=>{
      this.list = data;
    }).catch((e: ZetaException)=>{
      // eslint-disable-next-line no-console
      console.error(e);
    }).finally(()=>{
      this.loading = false;
    });
  }
  jobStatus (desc: string){
    if (!desc)
      return '';
    return ScheduleJobStatus[desc];
  }
  trackDesc (data: any){
    let info = data.description;
    switch (data.description){
      case 'START':
        info =  `#[${this.jobId}] has started.`;
        break;
      case 'FAIL':
        info = `#[${this.jobId}] has failed.`;
        break;
      case 'SUCCESS':
        info = `#[${this.jobId}] has finished successfully.`;
        break;
      case 'RETRY':
        info = `#[${this.jobId}] is retrying.`;
        break;
      case 'RETRY_SUCCESS':
        info = `#[${this.jobId}] is retry successfully.`;
        break;
      case 'RETRY_FAIL':
        info = `#[${this.jobId}] is retry failed.`;
        break;
      case 'SKIP':
        info = `#[${this.jobId}] is skipped by ${data.nt}.`;
        break;
      case 'RUN':
        info = `#[${this.jobId}] is rerunned by ${data.nt}.`;
        break;
      case 'CANCEL':
        info = `#[${this.jobId}] is canceled by ${data.nt}.`;
        break;
      default:
        break;
    }
    return info;
  }
  formatDateFromTimestamps (timestamps: number) {
    return ScheduleUtil.formatDateFromTimestamps(timestamps);
  }
}
</script>

<style lang="scss" scoped>
@import "@/styles/global.scss";

.view-details{
  cursor: pointer;
  color: $zeta-global-color;
  text-decoration: underline;
  outline: none;
}
.error-info{
  display: block;
  font-size: 11px;
  font-style: italic;
  line-height: 16px;
  word-break: break-word;
}
</style>
