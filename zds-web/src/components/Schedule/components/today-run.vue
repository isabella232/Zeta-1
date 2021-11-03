<template>
  <div class="list">
    <Statistics />
    <el-table
      ref="listTable"
      v-loading.sync="loading"
      :data="displayList"
      :default-sort="{prop: 'startTime', order: 'descending'}"
      @sort-change="onSortChange"
    >
      <el-table-column
        prop="id"
        label="Run ID"
        sortable="custom"
        min-width="100"
      >
        <template slot-scope="scope">
          {{ scope.row.id }}
        </template>
      </el-table-column>
      <el-table-column
        prop="jobName"
        label="Task Name"
        sortable="custom"
        min-width="140"
      >
        <template slot-scope="scope">
          <div
            v-click-metric:SCHEDULE_TASK_CLICK="{name: 'task'}"
            class="task-name"
            @click="goDetail(scope.row)"
          >
            {{ scope.row.jobName }}
          </div>
        </template>
      </el-table-column>
      <el-table-column
        prop="type"
        label="Type"
        sortable="custom"
        min-width="100"
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
        <template slot-scope="scope">
          <JobStatus :row="scope.row" />
        </template>
      </el-table-column>
      <el-table-column
        label="Action"
        align="center"
        min-width="120"
      >
        <template slot-scope="scope">
          <JobAction :row="scope.row" :type="scope.row.type" @init-list="reload" />
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
  </div>
</template>

<script lang="ts">
import { Component, Vue,  Watch, Inject, Ref, Prop } from 'vue-property-decorator';
import _ from 'lodash';
import moment from 'moment';
import {
  ScheduleConfig,
  ScheduleHistory,
} from '@/components/common/schedule-container';
import Statistics from './components/statistics.vue';
import JobStatus from '@/components/Schedule/components/components/job-status.vue';
import JobAction from '@/components/Schedule/components/components/job-action.vue';
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import ResultDetail from './components/result-detail.vue';
import TrackHistory from './components/track-history.vue';
import ScheduleUtil from '../util';
import FilterComponent from './components/filter-component.vue';

@Component({
  components: {
    Statistics,
    JobStatus,
    JobAction,
    ResultDetail,
    TrackHistory,
    FilterComponent,
  },
  filters:{
    searchList: (list: Array<ScheduleConfig>, runList: Array<ScheduleConfig>, keyword: string) => {
      if (!keyword.trim()) {
        return list;
      }
      return runList.filter(item => item.jobName.toLowerCase().indexOf(keyword.toLowerCase()) >= 0);
    },
  },
})
export default class TodayList extends Vue {
  @Inject()
  notebookRemoteService: NotebookRemoteService;
  @Prop() loadRun: boolean;
  @Prop() searchKey: string;
  @Prop() activeName: string;

  pageSize = 10;
  currentPage = 1;
  taskSort: any = {prop: 'startTime', order: 'descending'};
  statusCheckedFilter: Array<string> = [];
  statusStrFilter = '';

  constructor() {
    super();
  }
  get loading(){
    return this.loadRun;
  }
  set loading(e){
    this.$emit('update:loadRun', e);
  }
  get runList(): Array<ScheduleHistory> {
    const data = this.$store.state.ScheduleStore.runList;
    const list: Array<ScheduleHistory> = _.chain(data)
      .map((job)=>{
        const task = this.getTaskById(job.jobId);
        job.duration = job.endTime?moment.duration(job.endTime - job.runTime).asSeconds():0;
        job.jobName = task? task.jobName : '';
        job.type = task? task.type : 'Notebook';
        return  job;
      })
      .sort((a: ScheduleHistory, b: ScheduleHistory)=>{
        if (a.startTime && b.startTime) {
          return a.startTime - b.startTime;
        } else {
          return -1;
        }
      })
      .value();

    return list;
  }
  get displayList() {
    let data: Array<any> = this.computedList;
    // sort
    if (!_.isEmpty(this.taskSort)) {
      const column = this.taskSort.prop;
      const descending = Boolean(this.taskSort.order === 'descending');
      data = _.chain(data)
        .sortBy(column)
        .value();
      data = descending ? data.reverse() : data;
    }
    const dataSlice = data.slice(
      (this.currentPage - 1) * this.pageSize,
      this.currentPage * this.pageSize
    );
    return dataSlice;
  }

  get computedList(){
    // this.currentPage = 1;
    const  { searchKey  } = this;
    const { statusCheckedFilter, statusStrFilter } = this;

    if(searchKey.trim()){
      const searchData = this.runList.filter(item => item.jobName!.toLowerCase().indexOf(searchKey.toLowerCase()) >= 0);
      return this.filterData(statusCheckedFilter, searchData);
    }
    return this.filterData(statusCheckedFilter, this.runList);
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
    this.runList.forEach(i => status.add(ScheduleUtil.jobStatus(i)));
    return Array.from(status);
  }
  onFilterChange(checkedValues = [], property) {
    switch (property) {
      case 'jobRunStatusInfo':
        this.statusCheckedFilter = checkedValues;
        break;
    }
  }
  @Watch('activeName')
  handleTabChange(){
    if(this.activeName === 'today'){
      this.reload();
    }
  }
  reload(){
    this.loading = true;
    this.$store.dispatch('initRunList')
      .finally(()=>{
        this.loading = false;
      });
  }
  getTaskById(id: number){
    return this.$store.getters.findTaskById(id);
  }
  formatDateFromTimestamps(timestamps: number) {
    return ScheduleUtil.formatDateFromTimestamps(timestamps);
  }
  formartDuration(row: ScheduleHistory){
    return ScheduleUtil.formartDuration(row);
  }
  onSortChange(arg: any) {
    this.taskSort = arg;
  }
  goDetail(scheduleInfo: ScheduleHistory) {
    this.$router.push({ path: `/scheduleDetail/${scheduleInfo.jobId}`});
  }
}
</script>
<style lang="scss" scoped>
@import "@/styles/global.scss";

.list{
  flex-grow: 1;
  height: 100%;
  display: flex;
  flex-direction: column;
  .history-popover {
    position: fixed;
    z-index: 2000;
    background: white;
    border: 1px solid #dddddd;
    box-shadow: 0 2px 4px 0 rgba(0, 0, 0, 0.12), 0 0 6px 0 rgba(0, 0, 0, 0.04);
    padding: 1em 1.5em;
    border-radius: 8px;
    min-width: 200px;
    &::before {
      content: '';
      display: block;
      height: 12px;
      width: 12px;
      transform: rotate(-45deg);
      border: 1px solid #dddddd;
      border-left: transparent;
      border-bottom: transparent;
      position: absolute;
      top: -7px;
      left: 450px;
      background: white;
    }
  }
}
</style>
