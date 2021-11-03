<template>
  <div
    v-if="scheduleJobInfo"
    v-loading="loading"
    class="schedule-container"
    :style="{'max-height': maxHeightStyle()}"
    :class="getSchedulerClass"
  >
    <h2
      v-if="detailPage"
      class="header"
    >
      <span class="job-name">{{ scheduleJobInfo.jobName }}</span>
      <span
        class="operation"
      >
        <FavoriteIcon
          :id="scheduleJobInfo.id.toString()"
          :type="'schedule'"
          class="op-icon"
        />
        <el-popover
          placement="bottom"
          trigger="click"
          visible-arrow="true"
          width="370"
        >
          <div>
            <schedule-share :task="scheduleJobInfo" />
          </div>
          <i
            slot="reference"
            class="op-icon zeta-icon-share"
          />
        </el-popover>
      </span>
    </h2>
    <h2
      v-else
      class="header"
    >
      Scheduler
    </h2>
    <div class="schedule-info base-info">
      <div class="row">
        <div class="row-name">
          Task Name
        </div>
        <div class="row-info">
          <el-form
            :rules="jobNameRule"
            :model="scheduleJobInfo"
          >
            <el-form-item
              prop="jobName"
              :error="nameError"
            >
              <el-input
                v-model="scheduleJobInfo.jobName"
                class="job-name"
                :disabled="isShared"
              />
            </el-form-item>
          </el-form>
        </div>
      </div>
      <div class="row">
        <div class="row-name">
          Task Status
        </div>
        <div class="row-info">
          <span v-if="detailPage">{{ scheduleStatus[scheduleJobInfo.status] }}</span>
          <el-switch
            v-model="scheduleJobInfo.status"
            :active-value="1"
            :inactive-value="0"
            :disabled="isShared"
          />
        </div>
      </div>
      <div class="row run-time">
        <div class="row-name">
          Last Run Time
        </div>
        <div class="row-info">
          {{ formatDateFromTimestamps(scheduleJobInfo.lastRunTime) }}
        </div>
      </div>
      <div class="row run-time">
        <div class="row-name">
          Next Run Time
        </div>
        <div class="row-info">
          {{ formatDateFromTimestamps(scheduleJobInfo.nextRunTime) }}
        </div>
      </div>
      <TimePicker v-model="scheduleJobInfo.scheduleTime" />
      <div class="email">
        <div class="row">
          <div class="row-name">
            Email Notification
          </div>
          <div class="row-info">
            <el-switch
              v-model="scheduleJobInfo.mailSwitch"
              :active-value="1"
              :inactive-value="0"
              :disabled="isShared"
            />
          </div>
        </div>
        <input-email
          v-model="scheduleJobInfo.ccAddr"
          :mail-switch="scheduleJobInfo.mailSwitch"
        />
      </div>
      <div class="failure-rule">
        <div class="row">
          <div class="row-name">
            Auto Inactive
          </div>
          <div class="row-info-wrapper">
            <div class="row-info">
              <el-switch
                v-model="scheduleJobInfo.failTimesToBlock"
                :active-value="computedValue"
                :inactive-value="0"
                :disabled="isShared"
              />
              <span v-if="scheduleJobInfo.failTimesToBlock>0">
                After<el-input
                  v-model="scheduleJobInfo.failTimesToBlock"
                  type="number"
                  size="mini"
                  min="1"
                  max="30"
                  class="times"
                  :readonly="isShared"
                  @change="validTimes"
                />times consecutive failure</span>
            </div>
            <div
              v-if="!scheduleJobInfo.failTimesToBlock"
              class="tips"
            >
              No block for any failure,task will run as planned.
            </div>
            <div
              v-else
              class="tips"
            >
              A task can be inactived automatically.
            </div>
          </div>
        </div>
      </div>
      <div class="failure-rule">
        <div class="row">
          <div class="row-name">
            Auto Retry
          </div>
          <div class="row-info-wrapper">
            <div class="row-info">
              <el-switch
                v-model="scheduleJobInfo.autoRetry"
                :active-value="true"
                :inactive-value="false"
                :disabled="isShared"
              />
            </div>
            <div
              class="tips"
            >
              <span v-if="scheduleJobInfo.autoRetry">When Hermes platform downtime happen, Zeta will auto fully re-run your task after waiting for Hermes recover in 25 mins. The maximum retry times is 3 for a job instance.</span>
              <span v-else>Only for notebook on Hermes.</span>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div class="schedule-info detail-info">
      <notebook-schedule-info
        v-if="scheduleJobInfo.type === 'Notebook'"
        v-model="scheduleJobInfo.task"
        :schedule-info="scheduleJobInfo"
      />
      <data-move-schedule-info
        v-else-if="scheduleJobInfo.type === 'DataMove'"
        :task="scheduleJobInfo.task"
      />
      <data-validation-schedule-info
        v-else-if="scheduleJobInfo.type === 'DataValidate'"
        :task="scheduleJobInfo.task"
      />
    </div>
    <div class="btn-group">
      <el-button
        v-if="!isShared"
        type="primary"
        :disabled="scheduleJobInfo.jobName === ''"
        @click="createOrUpdate"
      >
        Apply
      </el-button>
      <el-button
        v-if="!detailPage"
        plain
        @click="close"
      >
        Cancel
      </el-button>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch } from 'vue-property-decorator';
import {
  ScheduleType,
  ScheduleStatus,
  ScheduleConfig,
  NotebookSchedule,
  scheduleConfigFactory,
} from '@/components/common/schedule-container';
import TimePicker from '@/components/common/time-picker';
import { CodeType, INotebook, IConnection } from '@/types/workspace';
import NotebookScheduleInfo from './NotebookScheduleInfo.vue';
import DataMoveScheduleInfo from './DataMoveScheduleInfo.vue';
import DataValidationScheduleInfo from './DataValidationScheduleInfo.vue';
import InputEmail from './input-email.vue';
import Util from '@/services/Util.service';
import { ZetaException } from '@/types/exception';
import _ from 'lodash';
import ScheduleShare from '@/components/Schedule/components/components/share.vue';
import FavoriteIcon from '@/components/common/favorite-icon/favorite-icon-witn-store.vue';
import ScheduleUtil from '@/components/Schedule/util';
import Config from '@/config/config';
import { isHermes } from '@/services/connection.service';

@Component({
  components: {
    TimePicker,
    NotebookScheduleInfo,
    DataMoveScheduleInfo,
    DataValidationScheduleInfo,
    InputEmail,
    ScheduleShare,
    FavoriteIcon,
  },
})
export default class ScheduleContainer extends Vue {
  @Prop({
    default: null,
  })
  scheduleInfo: ScheduleConfig;
  @Prop()
  type: ScheduleType;
  @Prop()
  notebookId: string;
  scheduleJobInfo: ScheduleConfig;
  scheduleStatus = ScheduleStatus;
  innerHeight = window.innerHeight;
  detailPage = false;
  validApply = false;
  constructor (){
    super();
    this.scheduleJobInfo = _.cloneDeep(this.scheduleInfo);
  }

  loading = false;
  jobNameRule: object = {
    jobName: [
      {
        required: true,
        message: 'please input job name',
        trigger: 'change',
      },
    ],
  };
  nameError = '';

  get isShared (){
    return Util.isShareApp();
  }
  get currentCodeType (): CodeType {
    if (this.notebook) {
      return this.notebook.connection.codeType;
    }
    return CodeType.SQL;
  }
  get notebook (){
    if (this.nId) return this.$store.state.workspace.workspaces[this.nId] as INotebook;
  }
  get nId () {
    if (this.notebookId) return this.notebookId;
    if (_.has(this.scheduleJobInfo, 'task') && this.type === 'Notebook'){
      const task = this.scheduleJobInfo.task as NotebookSchedule;
      if (_.has(task, 'req.notebookId')) return task.req.notebookId;
    }
  }
  get getSchedulerClass () {
    return (this.scheduleJobInfo&&this.scheduleJobInfo.type)? this.scheduleJobInfo.type.toLowerCase(): '';
  }
  get computedValue (){
    if  (this.scheduleJobInfo.failTimesToBlock>0)
      return this.scheduleJobInfo.failTimesToBlock;
    else
      return 1;
  }
  validTimes (times){
    if (times<1){
      this.scheduleJobInfo.failTimesToBlock = 1;
    } else if (times>30){
      this.scheduleJobInfo.failTimesToBlock = 30;
    }
  }
  maxHeightStyle () {
    const visibleHeight = this.innerHeight * 0.8;
    if (this.$route.path.indexOf('scheduleDetail')>-1){
      return '100%';
    }
    if (visibleHeight < 670) {
      return `${visibleHeight}px !important`;
    } else {
      return '670px !important';
    }
    return '';
  }
  mounted () {
    this.detailPage = this.$route.name === 'SchedulerDetail'?true:false;
    /** set default value */
    if (_.isEmpty(this.scheduleJobInfo) && this.type === 'Notebook' && this.notebook) {
      const sc = ScheduleUtil.getDefaultConfig(this.type);
      const cnn = this.notebook.connection as IConnection;
      const task = scheduleConfigFactory(this.currentCodeType, this.notebookId, cnn.clusterId, cnn.alias);
      sc.task = task;
      this.$set(this, 'scheduleJobInfo', sc);
    }

    window.onresize = () =>{
      this.innerHeight = window.innerHeight;
    };
  }

  formatDateFromTimestamps (timestamps: number) {
    return ScheduleUtil.formatDateFromTimestamps(timestamps);
  }

  open () {
    this.$emit('visableChange', true);
  }

  close () {
    this.$emit('visableChange', false);
  }
  goToList (){
    if (this.isShared) return;
    this.$router.push('/schedule');
  }
  isHopper (): boolean{
    if ('history' in this.scheduleJobInfo.task){
      if ('sourcePlatform' in this.scheduleJobInfo.task.history && this.scheduleJobInfo.task.history.sourcePlatform  == 'hopper'){
        return true;
      }
      if ('targetPlatform' in this.scheduleJobInfo.task.history && this.scheduleJobInfo.task.history.targetPlatform  == 'hopper'){
        return true;
      }
    }
    return false;
  }
  createOrUpdate () {
    if (this.scheduleJobInfo.jobName === '') {
      return;
    }
    // hopper shutdown
    if (this.isHopper()){
      this.$message({message:'hopper has been shutdown', type: 'warning'});
      return;
    }
    this.loading = true;
    this.nameError = '';
    this.$store.dispatch('createOrUpdateSchedule', this.scheduleJobInfo)
      .then(() => {
        this.loading = false;
        this.close();
        let message = '';
        if (this.scheduleJobInfo.updateTime) {
          message = 'Changes have been applied successfully!';
        } else {
          message = 'Create schedule success!';
        }
        this.$message({
          message,
          type: 'success',
        });
        this.syncDoeSignal(this.scheduleJobInfo);
      })
      .catch((e: ZetaException) => {
        this.loading = false;
        if (e.message.indexOf('exist')>-1){
          e.resolve();
          this.nameError = e.message;
        }
      });
  }
  getPlatform (task){
    const allAlias = Config.zeta.notebook.connection.allAliasIdMap;
    const alias = _.findKey(allAlias, (v)=>{
      return v.toString() == this.getClusterId(task);
    });
    if (alias){
      return alias.toLowerCase().indexOf('apollo')>-1?'Apollo_Rno': alias;
    }
    return alias;
  }
  getClusterId (task) {
	  if (!task) return '';
	  return task.clusterId;
  }
  getShowDependency (task) {
    return (this.scheduleInfo.type === 'Notebook')
    && (this.scheduleInfo.scheduleTime.jobType === 'DAILY')
    && (this.getClusterId(task) == 10 || this.getClusterId(task) == 14 || isHermes(this.getClusterId(task))); // only support Hercules/ApolloReno/Hermes
  }
  syncDoeSignal (scheduleInfo: ScheduleConfig){
    if (scheduleInfo.dependencySignal.enabled && scheduleInfo.dependencySignal.signalTables.length>0){
      if (!this.getShowDependency(scheduleInfo.task)) return;
      const platform = this.getPlatform(scheduleInfo.task);
      const tables = scheduleInfo.dependencySignal.signalTables;
      this.$store.dispatch('syncDependencySignal', { platform: platform, dependencySignalTables: tables});
    }
  }
  @Watch('scheduleInfo', { deep: true})
  scheduleInfoChange () {
    this.scheduleJobInfo = _.cloneDeep(this.scheduleInfo);
  }
}
</script>
<style lang="scss" scoped>
@import "@/styles/global.scss";
.schedule-container {
  width: 500px;
  margin: 0 auto;
  max-height: 600px;
  overflow-y: auto;
  .router-link{
    color: #333333;
    text-decoration: none;
    &:hover{
      color: $zeta-global-color;
      text-decoration: underline;
    }
  }
  .header{
    line-height: 40px;
    display: flex;
    align-items: center;
    .job-name{
      display: inline-block;
      max-width: 450px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    .operation{
      margin-left: 10px;
      .op-icon{
        cursor: pointer;
        color: $zeta-global-color !important;
        margin-right: 10px;
      }
    }
  }
  .schedule-info {
    // display: flex;
    // flex-direction: column;
    /deep/ .schedule-title{
      font-size: 18px;
      font-weight: bold;
      margin: 10px 0;
    }
    /deep/ .row {
      display: flex;
      height: 40px;
      align-items: center;
      .row-name {
        width: 130px;
        flex-shrink: 0;
      }
      .row-info {
        flex: 1;
        display: flex;
        color: #535866;
        /deep/ .el-form {
          height: 40px;
          width: 100%;
        }
        .zeta-icon-edit{
          color: $zeta-global-color;
          cursor: pointer;
        }
        .job-name{
          max-width: 450px;
        }
      }
      &.run-time{
        display: none;
      }
    }
    .failure-rule{
      .row{
        height: 60px;
      }
      .row-info{
        align-items: center;
      }
      .row-info-wrapper{
        .el-switch{
          margin-right: 5px;
        }
        .times{
          width: 50px;
          margin-left: 5px;
          margin-right: 5px;
          /deep/ .el-input__inner{
            padding: 0 0 0 5px;
            line-height: 22px;
            height: 22px;
          }
        }
      }
    }
    .tips{
      color: #767676;
      font-size: 12px;
      line-height: 12px;
      margin-top: 5px;
      word-break: break-word;
    }
  }
  .detail-info {
    /deep/ h2 {
      margin: 15px 0 10px;
    }
  }
}
.btn-group {
  margin: 20px 0 5px 0;
  text-align: right;
}
.schedule-container.datavalidate {
  .schedule-info {
    /deep/ .row {
      .row-name {
        width: 140px;
      }
    }
  }
}
</style>
<style lang="scss">
.schedule-detail-page{
    .schedule-container{
      width: 100%;
    }
    .schedule-info{
      border-bottom: 1px solid  #e0e0e0;
      padding: 10px 0;
      .row-name{
        width: 140px !important;
        font-weight: bold;
      }
      .run-time{
        display: flex !important;
      }
    }
    .base-info{
      display: grid;
      grid-template-columns: repeat(2, calc(50% - 20px));
      grid-column-gap: 20px;
    }
    .dataMove-info{
      display: grid;
      grid-template-columns: repeat(2, calc(50% - 20px));
      grid-template-rows: repeat(4, auto);
      grid-column-gap: 20px;
      grid-auto-flow: column;
      .row{
        min-height: 40px;
        height: auto !important;
      }
      .disable{
        background-color: #ffffff;
        border: none;
        color: inherit;
        word-break: break-word;
        max-width: 100%;
        height: auto;
      }
    }
  }
</style>
