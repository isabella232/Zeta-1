<template>
  <div class="list">
    <el-table
      ref="listTable"
      v-loading.sync="loading"
      :data="displayList"
      :default-sort="{prop:'nextRunTime', order:'ascending'}"
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
          {{ scope.row.nt === User?'Owner': 'Editor' }}
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
        min-width="190"
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
        min-width="120"
      >
        <template slot-scope="scope">
          {{ scheduleStatus[scope.row.status] }}
          <el-switch
            v-model="scope.row.status"
            :active-value="1"
            :inactive-value="0"
            @change="onChangeStatus(scope.row)"
          />
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
        align="right"
        min-width="100"
      >
        <template slot-scope="scope">
          <div class="operation">
            <FavoriteIcon
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
            <i
              v-if="scope.row.nt === User"
              class="op-icon zeta-icon-delet"
              @click="onDelete(scope.row)"
            />
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
import { Component, Vue, Inject, Prop } from 'vue-property-decorator';
import Util from '@/services/Util.service';
import _ from 'lodash';
import ScheduleContainer, {
  ScheduleConfig,
  ScheduleStatus,
} from '@/components/common/schedule-container';
import ScheduleUtil from '../util';
import JobStatus from '@/components/Schedule/components/components/job-status.vue';
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import ScheduleShare from './components/share.vue';
import FavoriteIcon from '@/components/common/favorite-icon/favorite-icon-witn-store.vue';
import FilterComponent from './components/filter-component.vue';

@Component({
  components: {
    ScheduleContainer,
    ScheduleShare,
    JobStatus,
    FavoriteIcon,
    FilterComponent,
  },
  filters:{
    searchList: (list: Array<ScheduleConfig>, taskList: Array<ScheduleConfig>, keyword: string) => {
      if (!keyword.trim()) {
        return list;
      }
      return taskList.filter(item => item.jobName.toLowerCase().indexOf(keyword.toLowerCase()) >= 0);
    },
  },
})
export default class TaskList extends Vue {
  @Inject()
  notebookRemoteService: NotebookRemoteService;
  @Prop({ default: false }) loadTask: boolean;
  @Prop() searchKey: string;
  @Prop() activeName: string;
  scheduleStatus = ScheduleStatus;

  pageSize = 10;
  currentPage = 1;
  taskSort: any = {prop:'nextRunTime', order:'ascending'};
  statusCheckedFilter: Array<string> = [];
  statusStrFilter = '';

  constructor() {
    super();
  }
  get loading(){
    return this.loadTask;
  }
  set loading(e){
    this.$emit('update:loadTask', e);
  }
  get User(){
    return Util.getNt();
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

  get taskList(): Array<ScheduleConfig> {

    const list = this.$store.state.ScheduleStore.taskList.slice();
    return list.sort(
      (a: ScheduleConfig, b: ScheduleConfig) => {
        if (a.nextRunTime && b.nextRunTime) {
          return a.nextRunTime - b.nextRunTime;
        } else if (
          a.nextRunTime === undefined || a.nextRunTime === null
        ) {
          return 1;
        } else {
          return -1;
        }
      }
    );
  }
  get computedList(){
    const  { searchKey  } = this;
    const { statusCheckedFilter, statusStrFilter } = this;

    if(searchKey.trim()){
      const searchData = this.taskList.filter(item => item.jobName!.toLowerCase().indexOf(searchKey.toLowerCase()) >= 0);
      return this.filterData(statusCheckedFilter, searchData);
    }
    return this.filterData(statusCheckedFilter, this.taskList);
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
    this.taskList.forEach(i => status.add(ScheduleUtil.jobStatus(i)));
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
  id2Str(id: number){
    return id.toString();
  }
  goDetail(scheduleInfo: ScheduleConfig) {
    this.$router.push({ path: `/scheduleDetail/${scheduleInfo.id}`});
  }

  isHopper(row: any): boolean{
    if('history' in row.task){
      if('sourcePlatform' in row.task.history && row.task.history.sourcePlatform  == 'hopper'){
        return true;
      }
      if('targetPlatform' in row.task.history && row.task.history.targetPlatform  == 'hopper'){
        return true;
      }
    }
    return false;
  }
  onChangeStatus(row: any) {
    // hopper shutdown
    if(this.isHopper(row)){
      this.$message({message:'hopper has been shutdown', type: 'warning'});
      return;
    }
    this.$store.dispatch('createOrUpdateSchedule', row).then(()=>{
      const message = 'update schedule success';
      this.$message({
        message,
        type: 'success',
      });
    })
      .catch(() => {
        row.status = 0;
      });
  }
  async onDelete(row) {
    const message = 'The task record will be deleted too, are you sure to delete?';
    try {
      await this.$confirm(message, 'Confirm Delete', {
        confirmButtonText: 'Delete',
        cancelButtonText: 'Cancel',
        customClass:'del-file-message',
        type: 'warning',
        dangerouslyUseHTMLString: true,
      });
      this.loading = true;
      const taskId = row.id;
      const result = await this.notebookRemoteService.deleteScheduler(taskId);
      this.loading = false;
      if(result && result.data == 'Success') {
        this.$store.dispatch('deleteSchedule', taskId);
        this.$message({
          message: 'delete schedule success!',
          type: 'success',
        });
      }
    }
    catch(e) {
      this.loading = false;
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
