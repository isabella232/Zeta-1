<template>
  <div class="release">
    <template v-if="!task.released">
      <el-alert
        v-if="!needApproval && task.status === 'NEW'"
        title="Auto DA approved"
        type="success"
        :description="`at ${autoApprovedAt} PDT`"
        show-icon
        class="notification"
        :closable="false"
      />
      <el-alert
        v-if="task.status === 'PENDING'"
        title="Email was sent for approval"
        type="warning"
        show-icon
        class="notification"
        :closable="false"
      />
      <el-alert
        v-if="task.status === 'APPROVED'"
        title="DA approved"
        type="success"
        :description="`at ${task.upd_date} PDT`"
        show-icon
        class="notification"
        :closable="false"
      />
      <el-alert
        v-if="task.editable && needApproval"
        title=""
        type="warning"
        show-icon
        class="notification"
        :closable="false"
      >
        <template slot="title">
          <div>
            <span>Data model didn’t pass auto DA rules, please send to DA for manual approval.</span>
            <el-button
              type="warning"
              style="margin-left: 24px"
              @click="sendToDA"
            >
              Send to DA
            </el-button>
          </div>
        </template>
      </el-alert>
    </template>
    <el-row :gutter="20">
      <el-col :span="8">
        <labeled label="Source Type">
          {{ task.src_type }}
        </labeled>
      </el-col>
      <el-col :span="16">
        <labeled label="Source Table">
          <div class="inner-flex">
            <span>
              {{ task.src_table }}
            </span>
            <el-alert
              v-if="jobExisted"
              title="This table has already been launched by AutoEL tool"
              type="warning"
              show-icon
              class="inline-notification"
              :closable="false"
            />
          </div>
        </labeled>
      </el-col>
    </el-row>
    <el-row :gutter="20">
      <el-col :span="8">
        <labeled label="Target Platform">
          {{ task.tgt_platform }}
        </labeled>
      </el-col>
      <el-col :span="8">
        <labeled label="Target DB">
          {{ task.tgt_db }}
        </labeled>
      </el-col>
      <el-col :span="8">
        <labeled label="Target Table">
          {{ task.tgt_table }}
          <span v-if="task.upd_freq === 'one_time'">_w</span>
        </labeled>
      </el-col>
    </el-row>
    <el-divider />
    <h1>Job Configuration</h1>
    <el-row :gutter="20">
      <el-col :span="8">
        <labeled label="Subject Area">
          {{ task.sa_name }} ({{ task.sa_code }})
        </labeled>
      </el-col>
      <el-col :span="8">
        <labeled label="HD Batch Account">
          {{ batchAccount }}
        </labeled>
      </el-col>
      <el-col :span="8">
        <labeled label="HD Queue">
          <el-select
            v-model="form.queue"
            placeholder="Select a hadoop queue"
            :disabled="disabled"
          >
            <el-option
              v-for="q in queues"
              :key="q"
              :label="q"
              :value="q"
            />
          </el-select>
        </labeled>
      </el-col>
    </el-row>
    <el-row :gutter="20">
      <el-col :span="8">
        <labeled label="Touch File">
          <span style="line-break: anywhere">
            {{ doneFile }}
          </span>
        </labeled>
      </el-col>
      <el-col :span="8">
        <labeled label="UC4 Client">
          1000
        </labeled>
      </el-col>
      <el-col :span="8">
        <labeled label="UC4 Folder">
          /CORE/{{ task.sa_name }}
        </labeled>
      </el-col>
    </el-row>

    <el-divider />
    <el-row :gutter="20">
      <el-col :span="8">
        <labeled label="Update Frequency">
          {{ task.upd_freq }}
        </labeled>
      </el-col>
      <el-col :span="8">
        <labeled label="Schedule">
          <el-time-picker
            v-model="form.schedule"
            :picker-options="{
              selectableRange,
            }"
            format="HH:mm"
            value-format="HH:mm"
            placeholder="Select time"
            :disabled="disabled"
            :clearable="false"
          />
        </labeled>
      </el-col>
      <el-col :span="8">
        <labeled>
          <el-tooltip
            slot="label"
            content="This will create access views of related tables. Batch views will be created by default."
            placement="top"
          >
            <label>
              Need Create View
              <i class="el-icon-question" />
            </label>
          </el-tooltip>
          <el-checkbox
            v-model="form.createView"
            :disabled="task.upd_freq === 'one_time' || disabled"
          />
        </labeled>
      </el-col>
    </el-row>
    <el-divider />
    <el-row :gutter="20">
      <el-col :span="8">
        <labeled label="Update Strategy">
          {{ task.upd_strategy }} <sup style="color: orange">T-1</sup>
        </labeled>
      </el-col>
      <el-col :span="8">
        <labeled label="First Run Date">
          <el-tooltip
            slot="label"
            content="First Run Date is first UC4 scheduled running date. Initial load will run on previous date and please make sure it has enough time for initial load before first run date"
            placement="top"
          >
            <label>
              First Run Date
              <i class="el-icon-question" />
            </label>
          </el-tooltip>
          <el-date-picker
            v-model="form.firstRunDate"
            type="date"
            placeholder="Pick a day"
            :disabled="disabled"
            :clearable="false"
            format="yyyy-MM-dd"
            value-format="yyyy-MM-dd"
            :picker-options="{
              disabledDate(time) {
                return time.getTime() < Date.now() - 3600 * 1000 * 24 * 7; // FIXME: add this 1-week margin for testing
              },
            }"
          />
        </labeled>
      </el-col>
      <el-col :span="8">
        <labeled label="Need Initial Load">
          <el-checkbox
            v-model="form.initLoad"
            :disabled="task.upd_strategy === 'Full Table' || disabled"
          />
        </labeled>
      </el-col>
    </el-row>
    <el-row
      v-if="form.initLoad"
      :gutter="20"
      type="flex"
      justify="end"
    >
      <el-col :span="8">
        <labeled label="Start Date">
          <el-date-picker
            v-model="form.startDate"
            type="date"
            placeholder="data before this date won't be loaded"
            :disabled="disabled"
            :clearable="false"
            format="yyyy-MM-dd"
            value-format="yyyy-MM-dd"
            :picker-options="{
              disabledDate(time) {
                return time.getTime() > Date.now(); 
              },
            }"
          />
        </labeled>
      </el-col>
    </el-row>

    <div class="btn-wrapper">
      <el-button @click="onPrev">
        Previous
      </el-button>
      <div>
        <el-button
          v-if="!disabled"
          @click="onSave"
        >
          Save
        </el-button>
        <el-button
          v-if="task.status_code > 0"
          @click="onNext"
        >
          Next
        </el-button>
        <el-button
          type="primary"
          :loading="loading"
          :disabled="needApproval"
          @click="onRelease"
        >
          Release
        </el-button>
        <template v-if="task.status === 'PENDING' && isDA && needDAApproval">
          <el-button
            type="danger"
            @click="onReject('DA')"
          >
            Reject
          </el-button>
          <el-button
            type="success"
            @click="onApprove('DA')"
          >
            Approve
          </el-button>
        </template>
        <template v-else-if="task.status === 'PENDING' && isSAOwner && needSAOwnerApproval && !needDAApproval">
          <el-button
            type="danger"
            @click="onReject('SA_OWNER')"
          >
            Reject
          </el-button>
          <el-button
            type="success"
            @click="onApprove('SA_OWNER')"
          >
            Approve
          </el-button>
        </template>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Mixins, Watch } from 'vue-property-decorator';
import Labeled from './components/Labeled.vue';
import { Actions, Getters } from '../store';
import { Action, State, Getter } from 'vuex-class';
import moment from 'moment';
import AutoELService from '@/services/remote/AutoELService';
import Util from '@/services/Util.service';
import _ from 'lodash';
import { isEmpty, attempt } from '@drewxiu/utils';
import { FormStatus } from './mixins/Form';
import GroupService from '@/services/remote/GroupService';
import { sentToDAForApproval, sentToSAOwnerForApproval } from './emailHelper';
import { GroupIdTemplate } from '@/components/common/AccessControl.vue';

const API = AutoELService.instance;

@Component({
  components: {
    Labeled,
  },
})
export default class Release extends Mixins(FormStatus) {
  @State((state) => state.AutoEL.task || {}) task;
  @State((state) => state.AutoEL.saList) saList;
  @State((state) => state.AutoEL.dataModel) dataModel;
  @State((state) => state.AutoEL.tableOnPlatform) tableOnPlatform;
  @Getter(Getters.TaskStatus) taskStatus;
  @Action(Actions.GetSAList) getSAList;
  @Action(Actions.GetTask) getTask;
  @Action(Actions.GetDataModel) getDataModel;
  @Action(Actions.SaveTask) saveTask;
  @Action(Actions.CheckIsDa) checkIsDA;

  autoApprovedAt = moment().utcOffset(-7).format('YYYY-MM-DD hh:mm:ss');
  jobExisted = false;
  doneFile = '';
  loading = false;
  queues = [];
  selectableRange = '00:00:00 - 23:59:59';
  form = {
    queue: null,
    schedule: '07:00',
    firstRunDate: moment().startOf('day').add(1, 'day').format('YYYY-MM-DD'),
    startDate: null,
    initLoad: true,
    createView: true,
  };
  get id () {
    return this.$route.params.id;
  }
  get sa () {
    const { saList, task } = this;
    return saList.find(
      (s) => s.sa_code === task.sa_code && s.sbjct_area === task.sa_name
    );
  }
  get batchAccount () {
    return attempt(() => this.sa.batch_acct, '');
  }
  get DAEmail () {
    return attempt(() => `${this.sa.prmry_da_nt}@ebay.com`);
  }
  get released () {
    return this.task && this.task.released;
  }
  get updateFreq () {
    const numb = {
      TWO: 2,
      THREE: 3,
      FOUR: 4,
      SIX: 6,
      EIGHT: 8,
      TWELVE: 12,
    };
    return attempt(() => numb[this.task.upd_freq.split('_')[1]]);
  }
  @Watch('updateFreq', { immediate: true })
  setSchedule (freq) {
    if (freq <= 7) {
      this.form.schedule = `${freq - 1}:59`;
    }
    this.selectableRange = `00:00:00 - ${freq ? freq - 1 : '23'}:59:59`;
  }
  @Watch('batchAccount', { immediate: true })
  setQueue (batchAccount) {
    if (this.disabled || !batchAccount) {
      return;
    }
    API.getHadoopQueue(batchAccount).then(
      (res) =>
        (this.queues = res.filter((i) => i.accessible).map((i) => i.queueName))
    );
  }

  @Watch('form.queue')
  @Watch('queues')
  setDefaultQueue () {
    if (!this.disabled && !this.form.queue && !isEmpty(this.queues)) {
      this.form.queue =
        this.queues.find((i) => i === 'hdlq-data-batch-low') ||
        this.queues.find((i) => i === 'hdlq-data-default') ||
        this.queues[0];
    }
  }

  @Watch('task')
  setInitials (task) {
    if (!task) {
      return;
    }
    if (task.upd_strategy === 'Full Table') {
      this.form.initLoad = false;
    } else {
      this.form.initLoad = task.initial_load === 1;
    }
    if (task.upd_freq === 'one_time') {
      this.form.createView = false;
    } else {
      this.form.createView = task.cre_view === 1;
    }
    this.form.queue = task.hadoop_queue;
    this.form.schedule = task.schedule_time || this.form.schedule;
    this.form.firstRunDate = task.first_run_dt || this.form.firstRunDate;
    this.form.startDate = task.init_start_dt || this.form.startDate;

    API.getJobInfoByTable(task.tgt_table)
      .then((data) => (this.jobExisted = !!data))
      .catch((e) => e.resolve());
    API.getDoneFile(task.sa_code, task.tgt_table).then(
      (val) => (this.doneFile = val)
    );
  }
  async mounted () {
    const { id } = this.$route.params;
    if (id) {
      if (!this.task || this.task.id !== id) {
        this.getTask(id);
        this.getDataModel(id);
      }
      if (isEmpty(this.saList)) {
        this.getSAList();
      }
    } else {
      this.$router.push('/autoel/task');
    }
  }
  onPrev () {
    if (this.disabled) {
      this.$router.push(`/autoel/task/${this.id}/datamodel`);
    } else {
      this.$router.push(`/autoel/task/${this.id}/datamodel?preview=true`);
    }
  }
  buildTask () {
    const excluded = [
      'cre_date',
      'cre_user',
      'da_status_code',
      'status_code',
      'upd_date',
      'upd_user',
    ];
    const revKeys = Object.keys(this.task).filter(
      (el) => !excluded.includes(el)
    );
    const savedTask = _.pick(this.task, revKeys);
    const task = {
      ...savedTask,
      hadoop_queue: this.form.queue,
      schedule_time: this.form.schedule,
      first_run_dt: this.form.firstRunDate,
      init_start_dt: this.form.startDate,
      initial_load: this.form.initLoad ? 1 : 0,
      cre_view: this.form.createView ? 1 : 0,
      user: Util.getNt(),
      user_full_name: null,
    };
    return task;
  }
  async onSave () {
    const task = this.buildTask();
    await this.saveTask(task);
    this.$message.success('Task successfully saved.');
  }
  get emailList () {
    return Array.from(new Set([this.task.cre_user, this.task.upd_user]))
      .filter(Boolean)
      .map((nt) => nt + '@ebay.com');
  }
  async sendToDA () {
    await this.onSave();
    await API.sendApprovalRequest(this.id);
    this.getTask(this.id);
    sentToDAForApproval(this.DAEmail!, this.emailList, this.task, this.needSAOwnerApproval)
      .then(() => {
        this.$message.success('Email successfully send to DA');
      })
      .catch(() => {
        this.$message.info('Sending email failed');
      });
  }
  async sendToSAOwner () {
    const group = await GroupService.instance.getGroup(GroupIdTemplate.SA(this.task.sa_name));
    const toAddr = group.items.map(i => `${i.id}@ebay.com`);
    await sentToSAOwnerForApproval(toAddr, this.emailList, this.task);
    this.getTask(this.id);
  }
  async onRelease () {
    try {
      this.loading = true;
      if (this.jobExisted) {
        await this.$confirm('This table was already released by the tool before and you will overwrite existing table and code.');
      }
      await this.onSave();
      const id = this.id;
      await API.releaseTask(id);
      this.$router.push(`/autoel/task/${this.id}/postrelease`);
    } catch (e) {
      e.resolve();
      const errMsg = e.originalResponse.data.msg;
      this.$alert(errMsg, 'Release failed');
    } finally { 
      this.loading = false;
    }
  }
  onNext () {
    this.$router.push(`/autoel/task/${this.id}/postrelease`);
  }
  async onReject (role: 'DA' | 'SA_OWNER') {
    await API.approve({
      id: this.task.id,
      role,
      approved: false,
    });
    this.$message.success('This task has been rejected by ' + role);
    this.$router.push('/autoel');
  }
  async onApprove (role: 'DA' | 'SA_OWNER') {
    await API.approve({
      id: this.task.id,
      role,
    });
    this.$message.success('This task has been approved by ' + role);
    if (role === 'DA' && this.needSAOwnerApproval) {
      await this.sendToSAOwner();
    }
    this.$router.push('/autoel');
  }
}
</script>

<style lang="scss" scoped>
@import '@/styles/global.scss';

.release {
  padding-top: 24px;
  margin: auto;
  width: 85%;
  h1 {
    font-size: 14px;
    margin-bottom: 1em;
  }
  .notification {
    padding: 24px 36px;
    margin-bottom: 36px;
    & /deep/ .el-alert__icon {
      width: 40px;
      &::before {
        font-size: 36px;
      }
    }
    & /deep/ .el-alert__title {
      font-size: 1.3em;
    }
  }
  .inline-notification {
    width: fit-content;
    margin-left: 30px;
    margin-top: -10px;
  }
  .inner-flex {
    display: flex;
    // align-items: center;
  }
  .btn-wrapper {
    display: flex;
    margin-top: 48px;
    justify-content: space-between;
  }
}
</style>
