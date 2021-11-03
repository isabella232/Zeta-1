<template>
  <div
    v-if="!isShared"
    class="job-action"
  >
    <i
      class="zeta-icon-skip"
      title="skip"
      :class="!showSkip?'disabled':''"
      @click="showSkip&&skipJob('SKIP')"
    />
    <i
      class="zeta-icon-replay"
      title="retry"
      :class="!showRun?'disabled':''"
      @click="showRun&&reRunJob('RUN')"
    />
    <i
      class="zeta-icon-close"
      title="cancel"
      :class="!showCancel?'disabled':''"
      @click="showCancel&&cancelJob('CANCEL')"
    />
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Inject } from 'vue-property-decorator';
import{
  JobRunStatus,
  ScheduleHistory,
  ScheduleType,
} from '@/components/common/schedule-container';
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import { ZetaException } from '@/types/exception';
import _ from 'lodash';
import Util from '@/services/Util.service';

@Component
export default class JobAction extends Vue {
  @Inject('notebookRemoteService') notebookRemoteService: NotebookRemoteService;
  @Prop() row: ScheduleHistory;
  @Prop() type: ScheduleType;
  @Prop() list: Array<ScheduleHistory>;

  get isShared(){
    return Util.isShareApp();
  }

  get jobRunStatusInfo(){
    return this.row.jobRunStatusInfo;
  }
  get showRun(){
    if (!this.jobRunStatusInfo) return false;
    return (this.jobRunStatusInfo.jobRunStatus === JobRunStatus.SUCCESS
    || this.jobRunStatusInfo.jobRunStatus === JobRunStatus.CANCELED
    || this.jobRunStatusInfo.jobRunStatus  ===JobRunStatus.FAIL)
      ? true : false;
  }
  get showCancel(){
    if (!this.jobRunStatusInfo) return false;
    if(this.type === 'Notebook'){
      return (this.jobRunStatusInfo.jobRunStatus === JobRunStatus.PENDING
      || this.jobRunStatusInfo.jobRunStatus === JobRunStatus.WAITING
      || this.jobRunStatusInfo.jobRunStatus === JobRunStatus.RUNNING
      || this.jobRunStatusInfo.jobRunStatus === JobRunStatus.AUTORETRY
      || this.jobRunStatusInfo.jobRunStatus === JobRunStatus.AUTORETRYWAITING)
        ? true : false;
    }
    return (this.jobRunStatusInfo.jobRunStatus === JobRunStatus.PENDING
      || this.jobRunStatusInfo.jobRunStatus === JobRunStatus.WAITING
      || this.jobRunStatusInfo.jobRunStatus === JobRunStatus.AUTORETRY
      || this.jobRunStatusInfo.jobRunStatus === JobRunStatus.AUTORETRYWAITING)
      ? true : false;
  }
  get showSkip(){
    if (!this.jobRunStatusInfo) return false;
    return (this.jobRunStatusInfo.jobRunStatus === JobRunStatus.PENDING  || this.jobRunStatusInfo.jobRunStatus === JobRunStatus.WAITING) ? true : false;
  }
  get runningJob(){
    const job: any = _.find(this.list, (job)=>{
      if (job.jobRunStatusInfo&&((job.jobRunStatusInfo.jobRunStatus===JobRunStatus.RUNNING)||(job.jobRunStatusInfo.jobRunStatus===JobRunStatus.AUTORETRY))){
        return job;
      }
    });
    return job;
  }
  enableActionRun(){
    if(this.runningJob){
      const message = `<p>Run ID ${this.runningJob.id } is still RUNNING. Please wait for completion or you can cancel it manually.</p>`;
      this.$alert(message, 'Hint', {
        dangerouslyUseHTMLString: true,
        confirmButtonText: 'OK',
      });
      return false;
    }
    return true;
  }
  getWaitingInfo(){
    if(!this.jobRunStatusInfo) return [];
    const info = this.jobRunStatusInfo.info;
    if(info){
      if(info.runId){
        return info.runId;
      }else{
        const list: Array<any> = [];
        for(const name in info){
          if(!info[name]){
            list.push(name);
          }
        }
        return list.join(';');
      }
    }
    return '';
  }
  Ops(ops: string){
    this.notebookRemoteService.scheduleJobOps(this.row.jobId, ops, this.row.id).then(()=>{
      const message = this.getMessage(ops);
      this.$message({
        message,
        type: 'success',
      });
      this.$emit('init-list');
      this.$store.dispatch('getJobById', this.row.jobId);
    }).catch((e: ZetaException)=>{
      console.error(e);
    });
  }
  async skipJob(ops: string) {

    if(!this.enableActionRun()) return;

    const waitingInfo = this.getWaitingInfo();
    const message = `<p style="color:red">The job currently pending on: ${waitingInfo}<br/><br/>Are you sure you want to continue the job and skip dependency? (Job will resume without waiting for the dependency table to be fully updated first.)</p>`;
    try {
      await this.$confirm(message, 'Skip Confirm', {
        confirmButtonText: 'Confirm',
        cancelButtonText: 'Cancel',
        customClass:'del-file-message',
        type: 'warning',
        dangerouslyUseHTMLString: true,
      });

      this.Ops(ops);
    }
    catch(e) {
    }
  }
  async cancelJob(ops: string) {
    const message = '<p style="color:red">Are you sure you want to cancel the running job?</p>';
    try {
      await this.$confirm(message, 'Cancel Confirm', {
        confirmButtonText: 'Confirm',
        cancelButtonText: 'Cancel',
        customClass:'del-file-message',
        type: 'warning',
        dangerouslyUseHTMLString: true,
      });

      this.Ops(ops);
    }
    catch(e) {
    }
  }
  async reRunJob(ops: string) {

    if(!this.enableActionRun()) return;

    const message = '<p style="color:red">Rerunning the job will restart the job from the beginning of the scheduled script.<br/><br/>Are you sure you want to rerun the job?</p>';
    try {
      await this.$confirm(message, 'Rerun Confirm', {
        confirmButtonText: 'Confirm',
        cancelButtonText: 'Cancel',
        customClass:'del-file-message',
        type: 'warning',
        dangerouslyUseHTMLString: true,
      });

      this.Ops(ops);
    }
    catch(e) {
    }
  }
  getMessage(ops: string){
    let msg = '';
    switch(ops){
      case 'SKIP':
        msg = 'The job dependency is skipped!';
        break;
      case 'RUN':
        msg = 'The job has started running!';
        break;
      case 'CANCEL':
        msg = 'The job has been cancelled!';
        break;
      default:
        break;
    }
    return msg;
  }
}
</script>

<style lang="scss" scoped>
@import "@/styles/global.scss";

.job-action{
  i{
    margin: 0 6px;
    font-size: 18px;
    cursor: pointer;
    color: $zeta-global-color;
    &:hover{
      color: $zeta-global-color-heighlight;
    }
    &.disabled{
      color: $zeta-global-color-disable
    }
    &:first-child{
      margin-left: 0;
    }
    &:last-child {
      margin-right: 0;
    }
  }
}

</style>
