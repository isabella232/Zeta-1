<template>
  <div
    class="schedule-list"
    style="height: calc(100% - 30px);"
  >
    <el-alert
      v-if="showScheduleAlationAler"
      type="success"
      :title="scheduleAlationAlertContent"
      effect="dark"
      @close="ignoreScheduleAlationAlert"
    />
    <div class="content-wrapper">
      <div class="search-tool">
        <el-input
          v-model="searchKey"
          placeholder="search for ..."
        />
      </div>
      <el-tabs
        ref="tabs"
        v-model="activeName"
        class="content"
      >
        <el-tab-pane name="task">
          <span
            slot="label"
            v-click-metric:SCHEDULE_TABS_CLICK="{name: 'task'}"
          >
            Tasks
            <i
              class="refresh-icon"
              :class="{'el-icon-refresh':!loadTask,'el-icon-loading':loadTask}"
              @click="()=> reloadTaskList()"
            />
          </span>
          <task-list
            :search-key="searchStr"
            :active-name="activeName"
            :load-task.sync="loadTask"
          />
        </el-tab-pane>
        <el-tab-pane name="today">
          <span
            slot="label"
            v-click-metric:SCHEDULE_TABS_CLICK="{name: 'today'}"
          >
            Running Daily Summary
            <i
              class="refresh-icon"
              :class="{'el-icon-refresh':!loadRun,'el-icon-loading':loadRun}"
              @click="()=> reloadRunList()"
            />
          </span>
          <today-list
            :search-key="searchStr"
            :active-name="activeName"
            :load-run.sync="loadRun"
          />
        </el-tab-pane>
        <el-tab-pane name="watch">
          <span
            slot="label"
            v-click-metric:SCHEDULE_TABS_CLICK="{name: 'watch'}"
          >
            Watch List
            <i
              class="refresh-icon"
              :class="{'el-icon-refresh':!loadWatch,'el-icon-loading':loadWatch}"
              @click="()=> reloadWatchList()"
            />
          </span>
          <watch-list
            :search-key="searchStr"
            :active-name="activeName"
            :load-watch.sync="loadWatch"
          />
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Provide, Watch } from 'vue-property-decorator';
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import AlationAlertMixin from '../Repository/alation-alert-mixin';
import TaskList from './components/task-list.vue';
import WatchList from './components/watch-list.vue';
import TodayList from './components/today-run.vue';
import _ from 'lodash';

type Folder = string;

@Component({
  components: {
    TaskList,
    WatchList,
    TodayList,
  },
  mixins: [
    AlationAlertMixin,
  ],
})
export default class ScheduleWrapper extends Vue {
  @Provide('notebookRemoteService')
  notebookRemoteService= new NotebookRemoteService();
  activeName = 'task';
  searchKey = '';
  searchStr = '';
  loadTask = false;
  loadWatch = false;
  loadRun = false;
  d: Function;

  constructor () {
    super();
    this.d = _.debounce( this.deb, 300, { leading: false, trailing: true });
  }

  @Watch('searchKey')
  handleSearch (){
    this.d();
  }
  deb (){
    this.searchStr = this.searchKey;
  }
  activated () {
    this.$nextTick(() => {
      (this.$refs.tabs as any).calcPaneInstances(true);
    });
  }

  reloadTaskList (){
    this.loadTask = true;
    this.$store.dispatch('initTaskList')
      .finally(()=>{
        this.loadTask = false;
      });
  }
  reloadRunList (){
    this.loadRun = true;
    this.$store.dispatch('initRunList')
      .finally(()=>{
        this.loadRun = false;
      });
  }
  reloadWatchList (){
    this.loadWatch = true;
  }

}
</script>

<style lang="scss">
@import "@/styles/global.scss";
.schedule-list {
    display: flex;
    flex-direction: column;
    .content-wrapper{
      position: relative;
      flex-grow: 1;
      display: flex;
      flex-direction: column;
      .content {
          height: 0;
          flex-grow: 1;
          display: flex;
          flex-direction: column;
          /deep/ .el-tabs__item {
              text-align: center;
          }
          /deep/ .el-tabs__content {
              flex-grow: 1;
              display: flex;
              flex-direction: column;
              .el-tab-pane {
                  flex-grow: 1;
                  height: 100%;
                  display: flex;
                  flex-direction: column;
              }
              .el-table {
                display: flex;
                flex-direction: column;
                  td,
                  tr {
                      font-size: 14px;
                  }
                  .cell {
                      line-height: 23px;
                      font-size: 14px;
                  }
                  .el-table__header-wrapper {
                    min-height: 55px;
                  }
                  .el-table__body-wrapper {
                    overflow: auto;
                  }
              }
          }
          .task-name{
            cursor: pointer;
            color: $zeta-global-color;
            &:hover{
              text-decoration: underline;
            }
          }
      }
      .search-tool{
        position: absolute;
        top: 5px;
        right: 0;
        z-index: 2;
        width: 260px;
      }
    }

    .el-dialog__wrapper /deep/ .el-dialog .el-dialog__header {
        padding: 0;
    }
    .dialog-table-title {
        font-size: 14px;
        font-weight: 600;
        margin-bottom: 10px;
    }
    /deep/ .dv-result-table {
        tr td,
        tr th {
            padding: 6px 0;
        }
    }

    .toolbar {
        padding: 8px 0;
        min-height: 26px;
    }
    .op-icon {
      margin-right: 10px;
      color: $zeta-global-color;
      cursor: pointer;
      font-size: 18px;
      vertical-align: middle;
      &.zeta-icon-share{
        font-size: 16px;
      }
    }
    .refresh-icon{
      margin-left: 2px;
    }
}
</style>
