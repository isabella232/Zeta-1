<template>
  <div class="list">
    <el-table
      ref="listTable"
      v-loading.sync="loading"
      :data="displayList"
      :default-sort="{prop:'nextRunTime', order: 'ascending'}"
      @sort-change="onSortChange"
    >
      <el-table-column
        prop="jobName"
        label="Task Name"
        sortable="custom"
        min-width="160"
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
        align="center"
        min-width="120"
      />
      <el-table-column
        prop="nt"
        label="Access"
        sortable="custom"
        align="center"
      >
        <template slot-scope="scope">
          {{ getAccess(scope.row) }}
        </template>
      </el-table-column>
      <el-table-column
        prop="nt"
        label="Owner"
        sortable="custom"
        align="center"
      >
        <template slot-scope="scope">
          {{ scope.row.nt === User?'Me': scope.row.nt }}
        </template>
      </el-table-column>
      <el-table-column
        prop="scheduleTime.jobType"
        label="Frequence"
        sortable="custom"
        align="center"
      />
      <el-table-column
        prop="nextRunTime"
        label="Next Run Time (MST)"
        min-width="180"
        sortable="custom"
        align="center"
      >
        <template
          slot-scope="scope"
        >
          {{ formatDateFromTimestamps(scope.row.nextRunTime) }}
        </template>
      </el-table-column>
      <el-table-column
        prop="status"
        label="Task Status"
        align="center"
        min-width="100"
      >
        <template slot-scope="scope">
          {{ scheduleStatus[scope.row.status] }}
        </template>
      </el-table-column>
      <el-table-column
        prop="jobRunStatusInfo"
        label="Last Run Status"
        align="center"
        width="150"
      >
        <template slot="header" slot-scope="scope">
          <filter-component
            title="Last Run Status"
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
        min-width="80"
      >
        <template slot-scope="scope">
          <div class="operation">
            <favorite-icon
              :id="id2Str(scope.row.id)"
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
                <schedule-share :task="scope.row" />
              </div>
              <i
                slot="reference"
                class="op-icon zeta-icon-share"
              />
            </el-popover>
          </div>
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
import { Component, Vue, Inject, Prop, Watch } from 'vue-property-decorator';
import { Getter }  from 'vuex-class';
import Util from '@/services/Util.service';
import _ from 'lodash';
import ScheduleContainer, {
  ScheduleConfig,
  ScheduleStatus,
} from '@/components/common/schedule-container';
import JobStatus from '@/components/Schedule/components/components/job-status.vue';
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import ScheduleShare from './components/share.vue';
import Statistics from './components/statistics.vue';
import FavoriteIcon from '@/components/common/favorite-icon/favorite-icon-witn-store.vue';
import { FavoriteItem } from '@/stores/modules/favorite-store';
import ScheduleUtil from '../util';
import FilterComponent from './components/filter-component.vue';

@Component({
  components: {
    ScheduleContainer,
    ScheduleShare,
    JobStatus,
    Statistics,
    FavoriteIcon,
    FilterComponent,
  },
  filters:{
    searchList: (list: Array<ScheduleConfig>, watchList: Array<ScheduleConfig>, keyword: string) => {
      if (!keyword.trim()) {
        return list;
      }
      return watchList.filter(item => item.jobName.toLowerCase().indexOf(keyword.toLowerCase()) >= 0);
    },
  },
})
export default class WatchList extends Vue {
  @Inject()
  notebookRemoteService: NotebookRemoteService;
  @Prop() loadWatch: boolean;
  @Prop() searchKey: string;
  @Prop() activeName: string;
  @Getter('favoriteSchedules')
  favoriteList: FavoriteItem[];

  scheduleStatus = ScheduleStatus;
  watchList: Array<ScheduleConfig> = [];
  pageSize = 10;
  currentPage = 1;
  taskSort: any = {prop:'nextRunTime', order: 'ascending'};
  statusCheckedFilter: Array<string> = [];
  statusStrFilter = '';

  get loading(){
    return this.loadWatch;
  }
  set loading(e){
    this.$emit('update:loadWatch', e);
  }
  get User(){
    return Util.getNt();
  }
  get isWatch(){
    return this.activeName ==='watch'?true:false;
  }
  get ids(){
    const ids = _.chain(this.favoriteList).map((list)=>list.id).value();
    return ids;
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
  shareUrl(id: number) {
    return `${location.protocol}//${location.host}/${Util.getPath()}share/#/scheduleDetail/${id}`;
  }

  getJobList(){
    this.loading = true;
    this.notebookRemoteService.getJobListByIds(this.ids.join(',')).then(({ data })=>{
      this.watchList = data.map((task)=>{
        return ScheduleUtil.parseConfig(task);
      }).sort(
        (a: ScheduleConfig, b: ScheduleConfig) => {
          if (a.nextRunTime && b.nextRunTime) {
            return a.nextRunTime - b.nextRunTime;
          } else if (
            a.nextRunTime === undefined ||
                      a.nextRunTime === null
          ) {
            return 1;
          } else {
            return -1;
          }
        }
      );
    }).finally(()=>{
      this.loading = false;
    });
  }
  get computedList(){
    // this.currentPage = 1;
    const  { searchKey  } = this;
    const { statusCheckedFilter, statusStrFilter } = this;

    if(searchKey.trim()){
      const searchData = this.watchList.filter(item => item.jobName.toLowerCase().indexOf(searchKey.toLowerCase()) >= 0);
      return this.filterData(statusCheckedFilter, searchData);
    }
    return this.filterData(statusCheckedFilter, this.watchList);
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
    this.watchList.forEach(i => status.add(ScheduleUtil.jobStatus(i)));
    return Array.from(status);
  }
  onFilterChange(checkedValues = [], property) {
    switch (property) {
      case 'jobRunStatusInfo':
        this.statusCheckedFilter = checkedValues;
        break;
    }
  }
  formatDateFromTimestamps(timestamps: number) {
    return ScheduleUtil.formatDateFromTimestamps(timestamps);
  }

  onSortChange(arg: any) {
    this.taskSort = arg;
  }
  getAccess(job: ScheduleConfig){
    const authInfo = job.authInfo;
    if(job.nt === this.User){
      return 'Owner';
    }
    const index = _.find(authInfo.WRITERS,(item)=> item.nt === this.User);
    return index ? 'Editor':'Read Only';
  }
  id2Str(id: number){
    return id.toString();
  }
  goDetail(scheduleInfo: ScheduleConfig) {
    if(this.getAccess(scheduleInfo) === 'Read Only'){
      const id = scheduleInfo.id as number;
      const url = this.shareUrl(id);
      window.open(url, '_blank');
      return;
    }
    this.$router.push({ path: `/scheduleDetail/${scheduleInfo.id}`});
  }
  @Watch('loadWatch')
  handleLoad(){
    if(this.loadWatch){
      this.getJobList();
    }
  }
  @Watch('activeName')
  getList(){
    if(this.isWatch){
      this.getJobList();
    }
  }
  @Watch('ids')
  changeList(newVal, oldVal){
    if(this.isWatch){
      if(newVal.length < oldVal.length){
        const removeId = _.difference( oldVal, newVal)[0];
        this.watchList = this.watchList.filter((item: ScheduleConfig)=>item.id != removeId);
      }
    }
  }
}
</script>
<style lang="scss" scoped>
.list{
  flex-grow: 1;
  height: 100%;
  display: flex;
  flex-direction: column;
}
</style>
