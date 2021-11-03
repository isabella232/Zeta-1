<template>
  <div class="step-4">
    <div class="timeline">
      <el-timeline>
        <el-timeline-item :color="getColorByStatus(releaseStatus.git_code_release_status)">
          <div class="title">
            <template
              v-if="releaseStatus.git_code_release_status === 'SUCCEEDED'"
            >
              <span class="name">Github Code Released at</span>
              <span class="timestamp">{{ releaseStatus.git_code_release_time }}</span>
            </template>
            <span
              v-else
              class="name"
            >Github Code Not Released</span>
          </div>
        </el-timeline-item>

        <el-timeline-item :color="getColorByStatus(releaseStatus.ddl_release_status)">
          <div class="title">
            <span
              v-if="releaseStatus.ddl_release_status === 'SUCCEEDED'"
              class="name"
            >One-time DDL Executed At</span>
            <span
              v-else-if="releaseStatus.ddl_release_status === 'IN PROGRESS'"
              class="name"
            >One-time DDL Started At</span>
            <span
              v-else-if="releaseStatus.ddl_release_status === 'FAILED'"
              class="name"
            >One-time DDL Failed At
            </span>
            <span class="timestamp">{{ releaseStatus.ddl_release_time }}</span>
            <el-button
              v-if="releaseStatus.ddl_release_status === 'FAILED'"
              type="text"
              @click="showLog = true"
            >
              Show Log
            </el-button>
          </div>
        </el-timeline-item>

        <el-timeline-item
          v-if="releaseStatus.initial_load === 'Y'"
          :color="getColorByStatus(releaseStatus.initial_run_status)"
        >
          <div
            v-if="releaseStatus.initial_run_status === 'SUCCEEDED'"
            class="title"
          >
            <span class="name">Initial Load Finished at</span>
            <span class="timestamp">{{ releaseStatus.initial_run_time }}</span>
          </div>
          <div
            v-else-if="releaseStatus.initial_run_status === 'FAILED'"
            class="title"
          >
            <span class="name">Initial Load Failed At</span>
            <span class="timestamp">{{ releaseStatus.initial_run_time }}</span>
            <el-button
              type="text"
              @click="showLog = true"
            >
              Show Log
            </el-button>
          </div>
          <div
            v-else-if="releaseStatus.initial_run_status === 'IN PROGRESS'"
            class="title"
          >
            <span class="name">Initial Load In Progress ({{ formatedProgress }}%). Started At</span>
            <span class="timestamp">{{ releaseStatus.initial_run_time }}</span>
          </div>
          <div
            v-else
            class="title"
          >
            <span class="name">Initial Load Not Finished</span>
          </div>
        </el-timeline-item>

        <el-timeline-item
          v-if="task.upd_freq !== 'one_time'"
          :color="getColorByStatus(releaseStatus.uc4_release_status)"
        >
          <div
            v-if="releaseStatus.uc4_release_status === 'SUCCEEDED'"
            class="title"
          >
            <span class="name">UC4 Jobplan Released at</span>
            <span class="timestamp">{{ releaseStatus.uc4_release_time }}</span>
          </div>
          <div
            v-else
            class="title"
          >
            <span class="name">UC4 Jobplan Not Released</span>
          </div>
          <div class="content">
            <div>Jobplan: {{ releaseStatus.uc4_jobplan_name }}</div>
            <div>Client: {{ releaseStatus.uc4_client }}</div>
            <div>UOW: {{ releaseStatus.uc4_uow }}</div>
          </div>
        </el-timeline-item>

        <el-timeline-item 
          v-if="task.upd_freq !== 'one_time'"
          :color="getColorByStatus(releaseStatus.uc4_schedule_status)"
        >
          <div
            v-if="releaseStatus.uc4_schedule_status === 'SUCCEEDED'"
            class="title"
          >
            <span class="name">UC4 Jobplan Scheduled at</span>
            <span class="timestamp">{{ releaseStatus.uc4_schedule_time }}</span>
          </div>
          <div
            v-else
            class="title"
          >
            <span class="name">UC4 Jobplan Schedule Status: {{ releaseStatus.uc4_schedule_status }}</span>
          </div>
        </el-timeline-item>
      </el-timeline>
    </div>
    <el-divider />
    <div class="recent-info">
      <h1>Recent Running Information</h1>
      <el-table
        :data="recentJobs"
        border
      >
        <el-table-column
          property="platform"
          label="Platform"
        />
        <el-table-column
          property="runningStartTime"
          label="Run Start Time"
        />
        <el-table-column
          property="runningEndTime"
          label="Run End Time"
        />
        <el-table-column
          property="status"
          label="Status"
        />
      </el-table>
    </div>
    <div class="btn-wrapper">
      <div>
        <el-button @click="() => onPrev(true)">
          Previous
        </el-button>
      </div>
      <div>
        <el-button
          v-if="task.status === 'RELEASING'"
          type="danger"
          @click="onCancel"
        >
          Cancel
        </el-button>
        <el-button
          type="primary"
          @click="onBackToList"
        >
          Go to Task List
        </el-button>
      </div>
    </div>
    <el-dialog
      title="Error Log"
      :visible.sync="showLog"
    >
      <div style="padding-bottom:32px">
        {{ releaseStatus.message }}
      </div>
    </el-dialog>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Watch } from 'vue-property-decorator';
import Labeled from './components/Labeled.vue';
import { Actions } from '../store';
import { Action, State } from 'vuex-class';

@Component({
  components: {
    Labeled,
  },
})
export default class PostRelease extends Vue {

  @State(state => state.AutoEL.releaseStatus || {}) releaseStatus;
  @State(state => state.AutoEL.recentJobs) recentJobs;
  @State(state => state.AutoEL.task || {}) task;
  @Action(Actions.GetTask) getTask;
  @Action(Actions.GetReleaseStatus) getReleaseStatus;
  @Action(Actions.GetRecentJobs) getRecentJobs;
  @Action(Actions.CancelTask) cancelTask;

  showLog = false;

  @Watch('task')
  getStatus (task) {
    if (task.id) {
      const id = task.id;
      this.getReleaseStatus({ id, table: task.tgt_table });
      this.getRecentJobs(id);
    }
  }
  get formatedProgress () {
    return this.releaseStatus.progress.toFixed(1);
  }
  getColorByStatus (status = '') {
    switch (status.toLowerCase()) {
      case 'succeeded':
        return '#0bbd87';
      case 'failed':
        return 'red';
      default:
        return undefined;
    }
  }
  onPrev (preview = false) {
    this.$router.push(`/autoel/task/${this.task.id}/release?preview=${preview}`);
  }
  onBackToList () {
    this.$router.push('/autoel');
  }
  async onCancel () {
    await this.$confirm('Are you sure?', 'This task will be cancelled');
    await this.cancelTask(this.task.id);
    this.$message.success('This task is cancelling, it may take a while to finish.');
    this.getTask(this.task.id);
  }
  async mounted () {
    const { id } = this.$route.params;
    if (id) {
      if (this.task.id !== id) {
        this.getTask(id);
      }
    } else {
      this.$router.push('/autoel/task');
    }
  }
}
</script>

<style lang="scss" scoped>
@import '@/styles/global.scss';

.step-4 {
  padding-top: 24px;
  margin: auto;
  width: 85%;
  > div.timeline {
    padding-top: 1em;
    .name {
      color: #333;
      padding-right: 1em;
    }
    .timestamp {
      color: #999;
    }
    .content {
      display: inline-block;
      margin-top: 1em;
      min-width: 60%;
      border-radius: 8px;
      background-color: #eee;
      padding: .5em 1em;
    }
  }

  > div.recent-info {
    padding-bottom: 24px;
    h1 {
      font-size: 13px;
      margin: 1.5em 0;
    }
  }

  .btn-wrapper {
    display: flex;
    justify-content: space-between;
  }
}
</style>