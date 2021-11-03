<template>
  <div class="auto-el-list">
    <el-header class="header">
      <div
        class="title"
        @click="goCreateTask"
      >
        <h1>Task List</h1>
        <i
          class="el-icon-circle-plus"
          style="font-size: 1.5em;position:relative;top:2px;"
        />
      </div>
      <div>
        <a
          href="https://wiki.vip.corp.ebay.com/display/ND/AutoEL+-+User+Manual"
          target="_blank"
        >
          <i class="el-icon-question" />
          User Manual
        </a>
      </div>
    </el-header>

    <el-table
      v-loading="loading"
      :data="currentPageData"
      border
      height="calc(100vh - 160px)"
      @sort-change="sortChange"
    >
      <el-table-column
        property="sa_code"
        width="150"
      >
        <template
          slot="header"
          slot-scope="scope"
        >
          <filter-component
            title="Subject Area"
            property="sa_code"
            :options-data="saOptions"
            @change="(val) => {currentPage = 0; saStrFilter = val.toLowerCase()}"
            @call-back="onFilterChange"
          />
        </template>
      </el-table-column>
      <el-table-column
        property="tgt_db"
        width="250"
      >
        <template
          slot="header"
          slot-scope="scope"
        >
          <filter-component
            title="Target DB"
            property="tgt_db"
            :options-data="dbOptions"
            @change="(val) => {currentPage = 0; dbStrFilter = val.toLowerCase()}"
            @call-back="onFilterChange"
          />
        </template>
        <template slot-scope="scope">
          {{ scope.row.tgt_db.toUpperCase() }}
        </template>
      </el-table-column>
      <el-table-column
        property="tgt_table"
      >
        <template
          slot="header"
          slot-scope="scope"
        >
          <search-component
            title="Target Table"
            @call-back="(val)=>{currentPage = 0; tableStrFilter=val.toLowerCase();}"
          />
        </template>
      </el-table-column>
      <el-table-column
        property="tgt_platform"
      >
        <template
          slot="header"
          slot-scope="scope"
        >
          <filter-component
            title="Target Platform"
            property="platform"
            :options-data="platOptions"
            @change="(val) => {currentPage = 0; platStrFilter = val.toLowerCase()}"
            @call-back="onFilterChange"
          />
        </template>
        <template slot-scope="scope">
          <i
            v-for="p in scope.row.tgt_platform.split(',')"
            :key="p"
            :title="p"
            :class="p.toLowerCase()"
            class="platform-icon"
          />
        </template>
      </el-table-column>
      <el-table-column
        property="src_type"
        label="Source Type"
        width="150"
      />
      <el-table-column
        property="release_time"
        sortable="custom"
        label="Release Time"
        :formatter="(row, col, val) => dateFormatter(val)"
      />
      <el-table-column
        property="cre_user"
        label="Created By"
        width="150"
      >
        <template
          slot="header"
          slot-scope="scope"
        >
          <filter-component 
            title="Created By"
            property="user"
            :options-data="userOptions"
            @change="(val) => {currentPage = 0; userStrFilter = val.toLowerCase()}"
            @call-back="onFilterChange"
          />
        </template>
        <template slot-scope="scope">
          <div
            @mouseenter="(evt) => showUserCard(evt, scope.row.cre_user)"
            @mouseleave="mouseLeave"
          >
            <i class="zeta-icon-user" />
            {{ scope.row.cre_user }}
          </div>
        </template>
      </el-table-column>
      <el-table-column
        property="status_text"
        label="Status"
        width="150"
      >
        <template
          slot="header"
          slot-scope="scope"
        >
          <filter-component 
            title="Status"
            property="status_text"
            :options-data="statusOptions"
            @change="(val) => {currentPage = 0; statusFilter = val}"
            @call-back="onFilterChange"
          />
        </template>
      </el-table-column>
      <el-table-column
        label="Actions"
        width="200"
        align="center"
      >
        <template slot-scope="scope">
          <div class="action-box">
            <el-button
              v-if="scope.row.status === 'RELEASED'"
              type="text"
              size="small"
              @click="handleClone(scope.row.id)"
            >
              Clone
            </el-button>
            <el-button
              v-if="scope.row.editable"
              type="text"
              size="small"
              :disabled="!isTaskOwner(scope.row.cre_user)"
              @click="handleEdit(scope.row.id)"
            >
              Edit
            </el-button>
            <el-button
              v-if="scope.row.modelCompleted"
              type="text"
              size="small"
              :disabled="!isTaskOwner(scope.row.cre_user)"
              @click="handleRelease(scope.row.id)"
            >
              Release
            </el-button>
            <el-button
              v-if="scope.row.editable"
              type="text"
              size="small"
              :disabled="!isTaskOwner(scope.row.cre_user)"
              @click="handleDelete(scope.row.id)"
            >
              Delete
            </el-button>
            <el-popconfirm
              v-if="scope.row.status === 'RELEASING'"
              title="Are you sure to cancel this task?"
              style="padding-right:8px;padding-top:2px;"
              @onConfirm="onCancel(scope.row.id)"
            >
              <el-button
                slot="reference"
                type="text"
                size="small"
              >
                Cancel
              </el-button>
            </el-popconfirm>
            <el-button
              v-if="scope.row.releaseStarted"
              type="text"
              size="small"
              @click="handleDetail(scope.row)"
            >
              Detail
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
    <div
      v-if="showCard"
      class="user-popover"
      :style="{left: hoveredUser.x + 'px', top: hoveredUser.y + 'px'}"
      @mouseenter="() => showCard += 1"
      @mouseleave="mouseLeave"
    >
      <user-card :nt="hoveredUser.nt" />
    </div>

    <footer>
      <span>Total 
        <strong>{{ computedTasks.length }}</strong>
      </span>
      <el-pagination
        :page-size="pageSize"
        :current-page="currentPage + 1"
        layout="sizes, prev, pager, next"
        :total="computedTasks.length"
        :page-sizes="[20, 50, 100]"
        @size-change="onSizeChange"
        @current-change="(val) => currentPage = val - 1"
      />
    </footer>
  </div>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import { State, Action, Getter } from 'vuex-class';
import AutoELModule, { Actions } from '@/components/AutoEL/store';
import { Getters, Actions as GroupActions } from '@/stores/modules/GroupStore';
import { matchString } from '@/components/Metadata/utils';
import SearchComponent from '@/components/DA/search-component.vue';
import FilterComponent from '@/components/DA/filter-component.vue';
import _ from 'lodash';
import UserCard from '@/components/common/UserCard.vue';
import Util from '@/services/Util.service';
import moment from 'moment';

@Component({
  components: { SearchComponent, FilterComponent, UserCard },
})
export default class TaskList extends Vue {
  @State(s => s.AutoEL.tasks) tasks;
  @Action(Actions.GetTaskList) getTaskList;
  @Action(Actions.DeleteTask) deleteTask;
  @Action(Actions.CancelTask) cancelTask;
  @Action(GroupActions.GetAdminGroup) getAdminGroup;
  @Getter('$globalLoading') loading;
  @Getter(Getters.AdminGroup) adminGroup;

  showCard = 0;
  typeOptions = [];
  pageSize = 20;
  currentPage = 0;
  saStrFilter = '';
  saCheckedFilter = [];
  dbStrFilter = '';
  dbCheckedFilter=[];
  tableStrFilter = '';
  platStrFilter = '';
  platCheckedFilter = [];
  userStrFilter = '';
  userCheckedFilter = [];
  statusStrFilter = '';
  statusCheckedFilter = [];
  hoveredUser = {
    nt: '',
    x: 0,
    y: 0,
  };
  sortOrder = null;

  nt = Util.getNt();

  get isAdmin () {
    return !!this.adminGroup && !!this.adminGroup.items.find(i => i.id === this.nt);
  }
  get sortedTasks (){
    if (this.sortOrder === null){
      return this.tasks;
    }
    return [...this.tasks].sort(this.sortByDate) as any;
  }
  get computedTasks () {
    const { saStrFilter, dbStrFilter,  platStrFilter, userStrFilter, statusStrFilter } = this;
    const { dbCheckedFilter, platCheckedFilter, userCheckedFilter, saCheckedFilter, statusCheckedFilter } = this;

    return this.sortedTasks.filter(data => {
      const table = matchString(this.tableStrFilter, data.tgt_table);
      const platform = matchString([platStrFilter, ...platCheckedFilter].filter(Boolean), data.tgt_platform);
      const user = matchString([userStrFilter, ...userCheckedFilter].filter(Boolean), data.cre_user);
      const db = matchString([dbStrFilter, ...dbCheckedFilter].filter(Boolean), data.tgt_db);
      const sa = matchString([saStrFilter, ...saCheckedFilter].filter(Boolean), data.sa_code);
      const status = matchString([statusStrFilter, ...statusCheckedFilter].filter(Boolean), data.status_text);
      return sa && db && table && platform && user && status;
    });
  }
  get platOptions () {
    const platforms = new Set();
    this.tasks.forEach(i => {
      i.tgt_platform.split(',').forEach(p => platforms.add(_.capitalize(p)));
    });
    return Array.from(platforms);
  }
  get userOptions () {
    const user = new Set();
    this.tasks.forEach(i => user.add(_.capitalize(i.cre_user)));
    return Array.from(user);
  }
  get statusOptions () {
    const status = new Set();
    this.tasks.forEach(i => status.add(i.status_text));
    return Array.from(status);
  }
  get dbOptions () {
    const dbNames = new Set();
    this.tasks.forEach(i=>dbNames.add(_.capitalize(i.tgt_db)));
    return Array.from(dbNames);
  }
  get saOptions (){
    const saNames = new Set();
    this.tasks.forEach(i=>saNames.add(_.capitalize(i.sa_code)));
    return Array.from(saNames);
  }
  get currentPageData () {
    const { pageSize, currentPage } = this;
    const start = pageSize * currentPage;
    return this.computedTasks.slice(start, start + pageSize);
  }
  dateFormatter (dateStr: string) {
    return dateStr ? moment(dateStr).format('YYYY-MM-DD hh:mm:ss') : '';
  }
  isTaskOwner (creator: string) {
    return this.isAdmin || creator === this.nt;
  }
  onFilterChange (checkedValues = [], property) {
    this.currentPage = 0;
    switch (property) {
      case 'platform':
        this.platCheckedFilter = checkedValues;
        break;
      case 'user':
        this.userCheckedFilter = checkedValues;
        break;
      case 'sa_code':
        this.saCheckedFilter = checkedValues;
        break;
      case 'tgt_db':
        this.dbCheckedFilter = checkedValues;
        break;
      case 'status_text':
        this.statusCheckedFilter = checkedValues;
        break;
    }
  }
  onSizeChange (size) {
    this.pageSize = size;
    this.currentPage = 0;
  }
  statusCheck (task) {
    return task.status_code > 0;
  }
  showUserCard (evt, nt) {
    this.showCard += 1;
    const bound = evt.target.getBoundingClientRect();
    Object.assign(this.hoveredUser, {
      nt,
      x: bound.x - 25,
      y: bound.y + 30,
    });
  }
  mouseLeave () {
    setTimeout(() => this.showCard -= 1, 300);
  }
  sortByDate (a, b){
    if (a.release_time === b.release_time) return 0;
    if (this.sortOrder === 'ascending'){
      if (a.release_time === null) return 1;
      if (b.release_time === null) return -1;
      return a.release_time < b.release_time ? -1 : 1;
    }
    else {
      if (a.release_time === null) return -1;
      if (b.release_time === null) return 1;
      return a.release_time < b.release_time ? 1 : -1;
    }
  }
  goCreateTask () {
    this.$router.push('/autoel/task');
  }
  handleClone (id) {
    this.$router.push('/autoel/task?clone=' + id);
  }
  handleEdit (id){
    this.$router.push(`/autoel/task/${id}`);
  }
  handleRelease (id) {
    this.$router.push(`/autoel/task/${id}/release`);
  }
  handleDetail (task) {
    const id = task.id;
    if (task.status === 'PENDING') {
      this.$router.push(`/autoel/task/${id}/release?preview=true`);
    } else {
      this.$router.push(`/autoel/task/${id}/postrelease?preview=true`);
    }
  }
  handleDelete (id) {
    this.$confirm('This action will delete the task. Continue?', 'Warning').then(() => {
      if (this.currentPageData.length == 1 && this.currentPage > 0){
        this.currentPage -= 1;
      }
      this.deleteTask(id);
    });
  }
  async onCancel (id) {
    await this.cancelTask(id);
    this.getTaskList();
  }
  created () {
    if (!this.$store.state[AutoELModule.namespace]) {
      this.$store.registerModule(AutoELModule.namespace, AutoELModule);
    }
  }
  mounted () {
    this.getTaskList();
    this.getAdminGroup();
  }
  sortChange (event){
    this.sortOrder = event.order;
  }
}
</script>

<style lang="scss" scoped>
@import '@/styles/global.scss';
@import '@/styles/metadata.scss';

.auto-el-list {
  padding: 16px 8px;
  .header {
    padding: 8px 0;
    display: flex;
    justify-content: space-between;
    .title {
      cursor: pointer;
      h1 {
        font-size: 16px;
        display: inline-block;
        padding-right: 8px;
        color: $header-color;
      }
      h1 ~ i {
        color: $zeta-global-color;
      }
    }
  }
  .action-box {
    display: flex;
    justify-content: center;
  }
  .user-popover {
    position: fixed;
    z-index: 2000;
    background: white;
    border: 1px solid #999;
    padding: 1em 1.5em;
    border-radius: 8px;
    min-width: 200px;
    &::before {
      content: '';
      display: block;
      height: 12px;
      width: 12px;
      transform: rotate(-45deg);
      border: 1px solid #999;
      border-left: transparent;
      border-bottom: transparent;
      position: absolute;
      top: -7px;
      left: 80px;
      background: white;
    }
  }
  footer {
    text-align: right;
    > span {
      display: inline-block;
      transform: translateY(40%);
    }
    .el-pagination {
      display: inline-block;
      margin: 24px 0;
    }
  }
}
</style>
