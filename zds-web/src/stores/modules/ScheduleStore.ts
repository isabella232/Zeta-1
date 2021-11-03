import { StoreOptions } from 'vuex';
import * as TYPE from '@/stores/MutationTypes';
import _ from 'lodash';
import Util from '@/services/Util.service';
import ScheduleUtil from '@/components/Schedule/util';
import { ScheduleConfig, ScheduleHistory } from '@/components/common/schedule-container';
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import Vue from 'vue';

const notebookRemoteService = new NotebookRemoteService().props({path: 'schedule'});
const option: StoreOptions<any> = {
  state: {
    taskList: [] as Array<ScheduleConfig>,
    watchList: [] as Array<ScheduleConfig>,
    runList: [] as Array<ScheduleHistory>,
  },
  mutations: {
    [TYPE.SCHE_INIT_LIST](state, taskList) {
      state.taskList = taskList;
    },
    [TYPE.SCHE_INIT_RUN_LIST](state, list) {
      state.runList = list;
    },
    [TYPE.SCHE_INIT_WATCH_LIST](state, list) {
      state.watchList = list;
    },
    [TYPE.SCHE_UPDATE](state, scheduleInfo){
      const index = _.findIndex(state.taskList, (task: ScheduleConfig) => task.id===scheduleInfo.id );
      if(index < 0){
        //create
        state.taskList.push(scheduleInfo);
      }else{
        // update
        Vue.set(state.taskList, index, _.assign({}, state.taskList[index],scheduleInfo));
      }
    },
    [TYPE.SCHE_TASK_DEL](state, taskId){
      const index = _.findIndex(state.taskList, (task: ScheduleConfig) => task.id===taskId );
      if(index > -1){
        //delete
        state.taskList.splice(index, 1);
      }
    },
    [TYPE.SCHE_UPDATE_AUTH](state, {id, authInfo}) {
      const sche_task = _.find(state.taskList, task => task.id === id);
      if (sche_task) {
        Object.assign(sche_task, {authInfo: authInfo});
      }
    },
  },
  getters: {
    taskList: state => state.taskList,
    runList: state => state.runList,
    taskByNotebookId: (state) => (notebookId: string) => {
      for (let i = 0; i < state.taskList.length; i++) {
        if (state.taskList[i].type === 'Notebook') {
          if (state.taskList[i].task.req.notebookId === notebookId) {
            return state.taskList[i];
          }
        }
      }
      return null;
    },
    findTaskById: (state) => (id: number) => {
      return _.find(state.taskList, task => task.id === id);
    },
  },
  actions: {
    initTaskList({ commit }) {
      return notebookRemoteService.getScheduleTaskList().then(({ data }) => {
        const scheduleTaskList: Array<ScheduleConfig> = [];
        data.forEach((item: any) => {
          const task = ScheduleUtil.parseConfig(item);
          scheduleTaskList.push(task);
        });
        commit(TYPE.SCHE_INIT_LIST, scheduleTaskList);
      }).catch(err => {
        console.error(err);
      });
    },
    initRunList({ commit }) {
      return notebookRemoteService.getTodayRun().then(({ data }) => {
        commit(TYPE.SCHE_INIT_RUN_LIST, data);
      }).catch(err => {
        console.error(err);
      });
    },
    createOrUpdateSchedule({ commit }, scheduleInfo ) {

      const dependencyTables = scheduleInfo.dependency.dependencyTables.map((d)=>{
        return  _.omit(d, ['lastUpdateTime']);
      });
      const dependency = _.assign({}, scheduleInfo.dependency,{dependencyTables});
      const task = scheduleInfo.task;
      const newTask = _.cloneDeep(task);
      if(newTask && newTask.history && (newTask.history.type == 4 ||newTask.history.type == 5)) {
        newTask.history.sourceTable = JSON.stringify((newTask.history.sourceTable).split(';'));
      }
      const config = {
        ...scheduleInfo,
        scheduleTime: JSON.stringify(scheduleInfo.scheduleTime),
        task: JSON.stringify(newTask),
        nt: Util.getNt(),
        ccAddr: (scheduleInfo.mailSwitch && scheduleInfo.ccAddr)?JSON.stringify(scheduleInfo.ccAddr):null,
        dependency: JSON.stringify(dependency),
        dependencySignal: JSON.stringify(scheduleInfo.dependencySignal),
        authInfo: JSON.stringify(scheduleInfo.authInfo),
      };
      if(config.id){
        return notebookRemoteService.updateScheduleTask(config).then(({ data }) => {
          const info = ScheduleUtil.parseConfig(data);
          commit(TYPE.SCHE_UPDATE, info);
        });
      }
      return notebookRemoteService.createScheduleTask(config).then(({ data }) => {
        const info = ScheduleUtil.parseConfig(data);
        commit(TYPE.SCHE_UPDATE, info);
      });
    },
    deleteSchedule({ commit }, taskId) {
      commit(TYPE.SCHE_TASK_DEL, taskId);
    },
    updateScheduleAccess({ commit }, {id, authInfo} ) {
      commit(TYPE.SCHE_UPDATE_AUTH, {id, authInfo});
    },
    getJobById({ commit }, id ) {
      return notebookRemoteService.getJobListByIds(id).then(({ data }) => {
        if(data.length>0){
          const info = ScheduleUtil.parseConfig(data[0]);
          commit(TYPE.SCHE_UPDATE, info);
          return info;
        }else{
          return null;
        }
      }).catch(e => {
        console.error(e);
      });
    },
    syncDependencySignal({ commit }, { platform, dependencySignalTables }) {
      const params = ScheduleUtil.parseDoeDependencySignal(platform, dependencySignalTables);
      return notebookRemoteService.syncDependencySignal(params).catch(e => {
        console.error(e);
      });
    },
  },
};
export default option;
