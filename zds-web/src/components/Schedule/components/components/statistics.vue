<template>
  <div>
    <div class="dashboard">
      <el-row :gutter="0">
        <el-col :span="3">
          <div class="status">
            <i class="waiting" /> Waiting
            <span>{{ waitingCount }}</span>
          </div>
        </el-col>
        <el-col :span="3">
          <div class="status">
            <i class="progress" /> Running
            <span>{{ runningCount }}</span>
          </div>
        </el-col>
        <el-col :span="3">
          <div class="status">
            <i class="success" /> Succeeded
            <span>{{ successCount }}</span>
          </div>
        </el-col>
        <el-col :span="3">
          <div class="status">
            <i class="failed" /> Failed
            <span>{{ failCount }}</span>
          </div>
        </el-col>
        <el-col :span="3">
          <div class="status" style="border-width:0">
            <i class="canceled" /> Canceled
            <span>{{ canceledCount }}</span>
          </div>
        </el-col>
        <el-col :span="9">
          <div class="current-time">
            <p>Daily Statistics Last Updated:<span>{{ getCurrentTime() }} (MST)</span></p>
          </div>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import moment from 'moment';
import { Getter } from  'vuex-class';
import { JobRunStatus, ScheduleHistory } from '@/components/common/schedule-container';
@Component
export default class Statistics  extends Vue {
  @Getter('runList')
  runList: ScheduleHistory[];

  jobStatus(task: ScheduleHistory ){
    if(!task.jobRunStatusInfo)
      return '';
    return task.jobRunStatusInfo.jobRunStatus;
  }
  get notStartCount(){
    return this.runList.filter((task: ScheduleHistory) => this.jobStatus(task) === JobRunStatus.NOTSTART ).length;
  }
  get waitingCount(){
    return this.runList.filter((task: ScheduleHistory) => (this.jobStatus(task) === JobRunStatus.WAITING) || (this.jobStatus(task) === JobRunStatus.PENDING) ).length;
  }
  get runningCount(){
    return this.runList.filter((task: ScheduleHistory) => this.jobStatus(task) === JobRunStatus.RUNNING ).length;
  }
  get successCount(){
    return this.runList.filter((task: ScheduleHistory) => this.jobStatus(task) === JobRunStatus.SUCCESS ).length;
  }
  get failCount(){
    return this.runList.filter((task: ScheduleHistory) => this.jobStatus(task) === JobRunStatus.FAIL ).length;
  }
  get canceledCount(){
    return this.runList.filter((task: ScheduleHistory) => this.jobStatus(task) === JobRunStatus.CANCELED ).length;
  }
  getCurrentTime(){
    return moment()
      .utcOffset('-07:00')
      .format('MMM DD, YYYY HH:mm');
  }
}
</script>

<style lang="scss" scoped>
@import "@/styles/global.scss";
.dashboard{
  padding: 0 0 10px;
  border-bottom: 2px solid #E4E7ED;
  .el-col{
    text-align: center;
  }
  .current-time{
    text-align: right;
    height: 74px;
    position: relative;
    > p{
      color: $zeta-font-light-color;
      font-size: 12px;
      position: absolute;
      bottom: 0;
      right: 0;
      > span{
        margin-left: 5px;
        display: inline-block;
        color: $zeta-font-color;
      }
    }
  }
  .status{
    border-right: 1px solid #E4E7ED;
    font-size: 12px;
    color: rgba(0, 0, 0, 0.45);
    margin: 10px 0;
    >span{
      display: block;
      color: rgba(0, 0, 0, 0.85);
      font-size: 24px;
    }
    i{
      display: inline-block;
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background-color: #a2a2a2;
      margin-right: 5px;
      &.notStart{
        background-color: #a2a2a2
      }
      &.waiting{
        background-color: #facc14;
      }
      &.progress{
        background-color: #659cdc;
      }
      &.success{
        background-color: #51a755;
      }
      &.failed{
        background-color: #e75064;
      }
      &.canceled{
        background-color: #111820;
      }
    }
  }

}
</style>
