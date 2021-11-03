<template>
  <div class="task-history">
    <div class="title">
      <h3 class="label">
        Running History
        <i
          :class="{'el-icon-refresh':!historyLoading,'el-icon-loading':historyLoading}"
          @click="()=> reload()"
        />
      </h3>
      <el-date-picker
        v-model="date"
        type="daterange"
        range-separator="to"
        start-placeholder="startDate"
        end-placeholder="endDate"
        size="medium"
        class="date"
        format="yyyy - MM - dd"
        value-format="timestamp"
        :default-time="['00:00:00', '23:59:59']"
        :clearable="true"
      />
    </div>
    <el-table
      ref="historyTable"
      v-loading="historyLoading"
      :data="displayHistoryList"
      max-height="550"
      style="width: 100%"
      :default-sort="{prop: 'startTime', order: 'descending'}"
      @sort-change="onHistorySortChange"
    >
      <el-table-column
        prop="id"
        label="Run ID"
        min-width="120"
        sortable="custom"
      />
      <el-table-column
        prop="duration"
        label="RunTime"
        align="center"
        min-width="150"
        sortable="custom"
      >
        <template
          slot-scope="scope"
        >
          {{ formartDuration(scope.row) }}
        </template>
      </el-table-column>
      <el-table-column
        prop="startTime"
        label="Activation"
        align="center"
        min-width="150"
        sortable="custom"
      >
        <template
          slot-scope="scope"
        >
          {{ formatDateFromTimestamps(scope.row.startTime) }}
        </template>
      </el-table-column>
      <el-table-column
        prop="runTime"
        label="Start time"
        align="center"
        min-width="150"
        sortable="custom"
      >
        <template
          slot-scope="scope"
        >
          {{ formatDateFromTimestamps(scope.row.runTime) }}
        </template>
      </el-table-column>
      <el-table-column
        prop="endTime"
        label="End time"
        align="center"
        min-width="150"
        sortable="custom"
      >
        <template
          slot-scope="scope"
        >
          {{ formatDateFromTimestamps(scope.row.endTime) }}
        </template>
      </el-table-column>
      <el-table-column
        prop="jobRunStatusInfo"
        label="Status"
        align="center"
        width="120"
      >
        <template slot="header" slot-scope="scope">
          <filter-component
            title="Status"
            property="jobRunStatusInfo"
            :options-data="statusOptions"
            @change="(val) => {currentPage = 1; statusStrFilter = val.toLowerCase()}"
            @call-back="onFilterChange"
          />
        </template>
        <template
          slot-scope="scope"
        >
          <JobStatus :row="scope.row" />
        </template>
      </el-table-column>
      <el-table-column
        label="Action"
        align="center"
        min-width="120"
      >
        <template
          slot-scope="scope"
        >
          <JobAction :row="scope.row" :type="type" @init-list="reload" :list="historyList"  />
        </template>
      </el-table-column>
      <el-table-column
        label="Result"
        align="center"
        min-width="100"
      >
        <template slot-scope="scope">
          <result-detail :task="scope.row" />
        </template>
      </el-table-column>
      <el-table-column
        label="Change History"
        align="center"
        min-width="100"
      >
        <template slot-scope="scope">
          <TrackHistory :job-id="scope.row.id" />
        </template>
      </el-table-column>
    </el-table>
    <div
      class="zeta-el-table-wrapper-footer"
    >
      <el-pagination
        layout="prev, pager, next"
        :current-page.sync="currentPage"
        :page-size="pageSize"
        :total="computedList.length"
      />
    </div>
    <run-chart :list="computedList" />
  </div>
</template>

<script lang="ts">
import { Component, Vue, Watch, Prop, Inject } from 'vue-property-decorator';
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import { ScheduleHistory, ScheduleType, ScheduleConfig, NotebookSchedule } from '@/components/common/schedule-container';
import JobStatus from  '@/components/Schedule/components/components/job-status.vue';
import JobAction from '@/components/Schedule/components/components/job-action.vue';
import TrackHistory from '@/components/Schedule/components/components/track-history.vue';
import _ from 'lodash';
import moment from 'moment';
import ResultDetail from './result-detail.vue';
import RunChart  from './chart.vue';
import ScheduleUtil from '../../util';
import FilterComponent from './filter-component.vue';

@Component({
  components:{
    JobStatus,
    JobAction,
    TrackHistory,
    ResultDetail,
    RunChart,
    FilterComponent,
  },
})
export default class RunHistory extends Vue {
  @Inject()
  notebookRemoteService: NotebookRemoteService;
  @Prop() taskId: number;
  historyList: Array<any> = [];
  historyLoading = true;
  historySort: any = {prop:'startTime', order:'descending'};

  currentPage = 1;
  pageSize = 10;
  date: any = null;
  DateFilter: Array<ScheduleHistory> = [];
  statusCheckedFilter: Array<string> = [];
  statusStrFilter = '';

  get scheduleTask(): ScheduleConfig{
    return this.$store.getters.findTaskById(this.taskId);
  }

  get type(): ScheduleType{
    return this.scheduleTask ? this.scheduleTask.type : 'Notebook';
  }

  get displayHistoryList() {
    let data: Array<any> = this.computedList;
    // sort
    if (!_.isEmpty(this.historySort)) {
      const column = this.historySort.prop;
      const descending = Boolean(this.historySort.order === 'descending');
      data = _.chain(data)
        .sortBy(column)
        .value();
      data = descending ? data.reverse() : data;
    }
    const dataSlice = data.slice(
      (this.currentPage - 1) * this.pageSize,
      this.currentPage * this.pageSize
    );
    _.forEach(dataSlice, (job: ScheduleHistory)=>{
      const task = this.getTaskById(job.jobId);
      job.jobName = task? task.jobName : '';
      job.type = task? task.type : 'Notebook';

    });
    return dataSlice;
  }
  get computedList(){
    // this.currentPage = 1;
    const { statusCheckedFilter, statusStrFilter } = this;
    if(this.date){
      return this.filterData(statusCheckedFilter, this.DateFilter);
    }
    return this.filterData(statusCheckedFilter, this.historyList);
  }

  filterData(filter, data){
    return filter.length > 0 ?
      data.filter((item)=>{
        if(filter.indexOf(ScheduleUtil.jobStatus(item))>-1){
          return item;
        }
      })
      :
      data;
  }
  get statusOptions(){
    const status = new Set();
    this.historyList.forEach(i => status.add(ScheduleUtil.jobStatus(i)));
    return Array.from(status);
  }
  onFilterChange(checkedValues = [], property) {
    switch (property) {
      case 'jobRunStatusInfo':
        this.statusCheckedFilter = checkedValues;
        break;
    }
  }
  created () {
    this.reload();
  }
  reload() {
    this.historyLoading = true;
    this.notebookRemoteService
      .getScheduleHistoryById(this.taskId)
      .then(({ data }: any) => {
        this.historyList = _.chain(data)
          .map((instance)=>{
            instance.duration = instance.endTime?moment.duration(instance.endTime - instance.runTime).asSeconds():0;
            return instance;
          })
          .sort(
            (a: ScheduleHistory, b: ScheduleHistory) => {
              return b.startTime - a.startTime;
            })
          .value();
        this.historyLoading = false;
        this.$store.dispatch('getJobById', this.taskId);
      })
      .catch(err => {
        this.historyLoading = false;
        console.error(err);
      });
  }
  getTaskById(id: number): ScheduleConfig{
    return this.$store.getters.findTaskById(id);
  }
  onHistorySortChange(arg: any) {
    this.historySort = arg;
  }
  formatDateFromTimestamps(timestamps: number) {
    return ScheduleUtil.formatDateFromTimestamps(timestamps);
  }
  formartDuration(row: ScheduleHistory){
    return ScheduleUtil.formartDuration(row);
  }

  @Watch('date')
  handleDate(){
    if(!this.date) return;
    this.DateFilter = this.historyList.filter((list: ScheduleHistory) => {
      const d1 = moment(this.date[0]).format('YYYY/MM/DD HH:mm:ss');
      const d2 = moment(this.date[1]).format('YYYY/MM/DD HH:mm:ss');
      if(ScheduleUtil.formatDateFromTimestamps(list.startTime)>= d1  && ScheduleUtil.formatDateFromTimestamps(list.startTime)<= d2){
        return list;
      }
    });
  }
}
</script>

<style lang="scss" scoped>
@import '@/styles/global.scss';

.task-history{
  .title{
    display: flex;
    justify-content: space-between;
    align-items: center;
    color: #333333;
    background-color: #F3F4F6;
    font-size: 13px;
    font-weight: 700;
    height: 43px;
    line-height: 43px;
    padding-left: 5px;
    .date{
      margin-right: 5px;
      font-weight: normal;
    }
  }

  .view-details {
      color: $zeta-global-color;
      cursor: pointer;
      width: 100%;
  }
}
</style>
