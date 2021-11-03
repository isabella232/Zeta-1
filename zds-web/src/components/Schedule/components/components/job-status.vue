<template>
  <div class="job-status">
    <el-popover
      v-if="isPendingStatus"
      placement="bottom"
      width="400"
      trigger="hover"
      popper-class="waiting-popover"
    >
      <div v-if="isPending">
        <h3>Waiting for last job</h3>
        <el-table
          :data="list"
          border
          max-height="300"
          class="table"
        >
          <el-table-column
            label="Run Id"
            prop="runId"
          />
          <el-table-column
            label="Status"
            prop="status"
          />
          <el-table-column
            label="Start Time"
            prop="startTime"
            width="180"
          />
        </el-table>
      </div>
      <div v-else-if="isWaiting">
        <h3 v-if="list.length>0">Waiting for dependency</h3>
        <el-table
          v-if="list.length>0"
          :data="list"
          border
          max-height="300"
          class="table"
        >
          <el-table-column
            label="Table"
            prop="table"
          />
          <el-table-column
            label="Dependency Signal"
            prop="status"
            width="100"
            align="center"
          >
            <template slot-scope="scope">
              <p :class="scope.row.status?'ready':'not_ready'">{{ getDpStatus(scope.row.status) }}</p>
            </template>
          </el-table-column>
        </el-table>
        <div v-else>
          <h3>Waiting to start</h3>
        </div>
      </div>
      <div slot="reference" :class="getClass()">{{ jobStatus() }}</div>
    </el-popover>
    <span :class="getClass()" v-else>{{ jobStatus() }}</span>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Inject } from 'vue-property-decorator';
import {
  ScheduleJobStatus,
  JobRunStatus,
} from '@/components/common/schedule-container';
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import ScheduleUtil from '../../util';
import _ from 'lodash';

interface PendingInfo {
  table: string;
  status: boolean;
}
interface WaitingInfo {
  runId: number;
  startTime: number;
  status: JobRunStatus;
}

@Component
export default class JobStatus extends Vue {
  @Inject('notebookRemoteService') notebookRemoteService: NotebookRemoteService;
  @Prop() row: any;
  get isPending (){
    if (!this.jobRunStatusInfo){
      return false;
    }
    return (this.jobRunStatusInfo.jobRunStatus === JobRunStatus.PENDING) ? true : false;
  }
  get isWaiting (){
    if (!this.jobRunStatusInfo){
      return false;
    }
    return (this.jobRunStatusInfo.jobRunStatus === JobRunStatus.WAITING) ? true : false;
  }
  get isPendingStatus (){
    if (!this.jobRunStatusInfo){
      return false;
    }
    return (this.jobRunStatusInfo.jobRunStatus === JobRunStatus.PENDING || this.jobRunStatusInfo.jobRunStatus ===JobRunStatus.WAITING) ? true : false;
  }
  get jobRunStatusInfo (){
    return this.row.jobRunStatusInfo;
  }
  jobStatus (){
    if (!this.jobRunStatusInfo)
      return '';
    return ScheduleJobStatus[this.jobRunStatusInfo.jobRunStatus];
  }

  getClass (){
    const status = this.jobStatus();
    return status.toLowerCase().split(' ').join('_');
  }
  get list (){
    if (!this.jobRunStatusInfo) return [];
    const info = this.jobRunStatusInfo.info;
    if (info){
      let list: Array<any> = [];
      if (this.isPending){
        const obj = {
          runId: info.runId,
          startTime: this.formatDateFromTimestamps(info.startTime),
          status: ScheduleJobStatus[info.status],
        };
        list.push(obj);
      } else {
        for (const name in info){
          list.push({
            table: name,
            status: info[name],
          });
        }
        const a  = list.filter(l => l.status);
        const b  = list.filter(l => !l.status);
        list = _.concat(a,b);
      }
      return list;
    }

    return [];
  }
  formatDateFromTimestamps (timestamps: number) {
    return ScheduleUtil.formatDateFromTimestamps(timestamps);
  }

  getDpStatus (status: boolean) {
    return status ? 'Ready' : 'Not Ready';
  }
}
</script>

<style lang="scss" scoped>
h3{
  font-size: 14px;
  line-height: 30px;
  font-weight: normal;
}
.job-status{
  >span{
    display: inline-block;
    padding: 2px 10px;
    border-radius: 33px;
    color: #767676;
    word-break: break-word;
  }
  .not_start{
    background-color: #e5e5e5;
    color: #767676;
  }
  .running{
    background-color: #ccedff;
    color: #659CDC;
  }
  .waiting{
    padding: 2px 10px;
    border-radius: 33px;
    margin: -2px -10px;
    background-color: #ffedcd;
    color: #EDB23E;
  }
  .canceled{
    background: #A2A2A2;
    color: #ffffff;
  }
  .failed{
    background-color: rgba(255, 170, 166, 0.1);
    color: #DC1840
  }
  .succeeded{
    background-color: rgba(117, 255, 131, 0.1);
    color: #28A443
  }
  .retrying{
    background-color: #ffedcd;
    color: #EDB23E;
  }
}

.waiting-popover{
  .ready{
    color: #28A443
  }
  .not_ready{
    color: #DC1840;
  }
}
</style>
