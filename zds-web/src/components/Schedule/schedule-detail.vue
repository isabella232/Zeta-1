<template>
  <div
    class="schedule-detail-page"
  >
    <el-breadcrumb separator-class="el-icon-arrow-right" class="breadcrumb" v-if="!isShared">
      <el-breadcrumb-item :to="{ path: '/schedule' }">Scheduler</el-breadcrumb-item>
      <el-breadcrumb-item>Detail</el-breadcrumb-item>
    </el-breadcrumb>
    <schedule-container
      type="Notebook"
      :schedule-info="scheduleInfo"
    />
    <run-history
      :task-id="scheduleInfo.id"
    />
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch, Provide } from 'vue-property-decorator';
import ScheduleContainer,{ ScheduleConfig } from '@/components/common/schedule-container';
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import RunHistory from './components/components/run-history.vue';
import Util from '@/services/Util.service';

@Component({
  components:{
    ScheduleContainer,
    RunHistory,
  },
})
export default class ScheduleDetail extends Vue {
  @Provide('notebookRemoteService')
  notebookRemoteService = new NotebookRemoteService();
  @Prop() readOnly: boolean;
  @Prop() info: any;
  jobId = 0;

  created () {
    this.jobId = Number(this.$route.params.id);
  }
  get isShared(){
    return Util.isShareApp();
  }
  get scheduleInfo(): ScheduleConfig {
    if(!this.readOnly){
      const task = this.$store.getters.findTaskById(this.jobId);
      return task;
    }else{
      return this.info;
    }
  }
  get shareUrl() {
    return `${location.protocol}//${location.host}/${Util.getPath()}share/#/scheduleDetail/${this.jobId}`;
  }
  @Watch('jobId')
  handle(){
    try {
      const task =  this.$store.getters.findTaskById(this.jobId);
      if(!task){
        window.location.href = this.shareUrl;
      }
    }
    catch{
      window.location.href = this.shareUrl;
    }
  }
  @Watch('$route.params')
  handleId(){
    this.jobId = Number(this.$route.params.id);
  }

}
</script>

<style lang="scss" scoped>
.schedule-detail-page{
  .breadcrumb{
    line-height: 50px;
    margin: 0 20px;
    border-bottom: 2px solid #DEDEE1;
  }
  .schedule-container{
    margin-top: 0px;
    padding: 10px 20px 20px;
    box-sizing: border-box;
    height: auto;
  }
}
</style>
