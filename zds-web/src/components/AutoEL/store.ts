import { State, Task } from '@/components/AutoEL/types';
import AutoELService from '@/services/remote/AutoELService';
import { MutationTree, ActionTree, GetterTree } from 'vuex';
import VuexLoading from 'vuex-loading-plugin';
import DoeRemoteService from '@/services/remote/DoeRemoteService';
import Util from '@/services/Util.service';
import GroupService from '@/services/remote/GroupService';
import { GroupIdTemplate } from '@/components/common/AccessControl.vue';
const API = AutoELService.instance;
const doeRemoteService = DoeRemoteService.instance;

export enum Actions {
  GetTask = 'autoel/get/task',
  CloneTask = 'autoel/clone/task',
  GetTaskList = 'autoel/get/tasks',
  DeleteTask = 'autoel/delete/task',
  CancelTask = 'autoel/cancel/task',
  SaveTask = 'autoel/save/task',
  GetDataModel = 'autoel/get/datamodel',
  SaveDataModel = 'autoel/save/datamodel',
  GetSAList = 'autoel/get/salist',
  GetSourceTableList = 'autoel/get/sourcetables',
  GetSourceTableInfo = 'autoel/get/sourcetableinfo',
  GetSourceTableSchema = 'autoel/get/sourcetableschema',
  GetSourceTableSample = 'autoel/get/sourcetablesample',
  GetReleaseStatus = 'autoel/get/releasestatus',
  GetRecentJobs = 'autoel/get/recentjobs',
  GetHadoopLocation = 'autoel/get/hdlocation',
  GetDDL = 'autoel/get/ddl',
  GetUpsertScript = 'autoel/get/upsertscript',
  CheckIsDa = 'autoel/get/checkisda',
  CheckIsSAOwner = 'autoel/get/checkissaowner',
}

export enum Mutations {
  Clear = 'autoel/mut/clear',
}

export enum Getters {
  TaskStatus = 'autoel/getters/taskstatus',
}

const initState: State = {
  tasks: [],
  task: null,
  sourceTables: [],
  saList: [],
  sourceTableInfo: {},
  sourceTableSchema: {},
  sourceTableSample: {},
  releaseStatus: null,
  recentJobs: [],
  hadoopLocation: '',
  ddl: '',
  incrementalScript: '',
  dataModel: null,
  isDA: false,
  isSAOwner: false,
};

const state: State = {
  ...initState,
};

const getters: GetterTree<State, {}> = {
  [Getters.TaskStatus] (state) {
    if (!state.task) return 'init';
    if (state.task.id === null) return 'clone';
    if (state.task.release_time) return 'released';
    return 'created';
  },
};

const mutations: MutationTree<State> = {
  setTasks: (state: State, payload) => {
    state.tasks = payload.tasks;
  },
  setTask: (state: State, payload) => {
    state.task = payload;
  },
  setSA: (state: State, payload) => {
    state.saList = payload;
  },
  setSourceTables: (state: State, payload) => {
    state.sourceTables = payload;
  },
  setSourceTableInfo: (state: State, payload) => {
    if (!payload) return;
    state.sourceTableInfo = {
      ...state.sourceTableInfo,
      [payload.table_name]: payload,
    };
  },
  setSourceTableSchema: (state: State, { name, data }) => {
    state.sourceTableSchema = {
      ...state.sourceTableSchema,
      [name]: data,
    };
  },
  setSourceTableSample: (state: State, { name, data }) => {
    state.sourceTableSample = {
      ...state.sourceTableSample,
      [name]: data,
    };
  },
  setReleaseStatus: (state: State, payload) => {
    state.releaseStatus = payload;
  },
  setRecentJobs: (state: State, payload) => {
    state.recentJobs = payload;
  },
  setHadoopLocation: (state: State, payload) => {
    state.hadoopLocation = payload;
  },
  setDDL (state: State, payload) {
    state.ddl = payload;
  },
  setIncrementalScript (state: State, payload) {
    state.incrementalScript = payload;
  },
  setDataModel (state: State, payload) {
    state.dataModel = payload;
  },
  setIsDA (state: State, payload) {
    state.isDA = payload;
  },
  setIsSAOwner (state: State, payload) {
    state.isSAOwner = payload;
  },
  [Mutations.Clear] (state: State, payload: (keyof State)[] = []) {
    if (payload.length > 0) {
      payload.forEach(key => ((state as any)[key] = initState[key]));
    } else {
      Object.keys(state).forEach(key => {
        state[key] = initState[key];
      });
    }
  },
};

const actions: ActionTree<State, any> = {
  async [Actions.GetTaskList] ({ commit, $l }: any) {
    $l.start();
    const tasks = await API.getTaskList();
    commit('setTasks', { tasks });
    $l.end();
  },
  async [Actions.GetTask] ({ commit }, id: number) {
    const task = await API.getTaskById(id);
    task.approvalStatus = {
      pendingApprovalsFrom: [],
    };
    try {
      task.approvalStatus = await API.checkApprovalRequirements(id);
    } catch (error) {
      error.resolved = true;
    }
    commit('setTask', task);
  },
  async [Actions.CloneTask] ({ commit }, id: number) {
    const task = await API.getTaskById(id);
    commit('setTask', {
      ...task,
      id: null,
      release_time: null,
      status: 'NEW',
      editable: true,
      release: false,
      cre_date: null,
      cre_user: Util.getNt(),
    });
  },
  async [Actions.SaveTask] ({ dispatch }, task: Task) {
    const { GENERATED_KEY } = await API.updateTask(task);
    const id = task.id || GENERATED_KEY;
    await dispatch(Actions.GetTask, id);
  },
  async [Actions.GetDataModel] ({ commit }, id) {
    try {
      const model = await API.getDataModel(id);
      commit('setDataModel', model);
    } catch (error) {
      error.resolve && error.resolve();
    }
  },
  async [Actions.SaveDataModel] ({ dispatch }, { id, model }) {
    await API.saveDataModel(id, model);
    await dispatch(Actions.GetDataModel, id);
  },
  async [Actions.DeleteTask] ({ dispatch }, id: number) {
    await API.deleteTaskById(id);
    dispatch(Actions.GetTaskList);
  },
  async [Actions.CancelTask] ({}, id: number) {
    return await API.cancelTask(id);
  },
  async [Actions.GetSAList] ({ commit }) {
    const { data } = await API.getSAList();
    commit('setSA', data);
  },
  async [Actions.GetSourceTableList] ({ commit }, query = '') {
    const {
      data: { value },
    } = await API.getSourceTables(query);
    commit('setSourceTables', value);
    return value;
  },
  async [Actions.GetSourceTableInfo] ({ commit, $l }: any, name: string) {
    try {
      $l.start();
      const data = await API.getSourceTableInfo(name);
      commit('setSourceTableInfo', data);
    } finally {
      $l.end();
    }
  },
  async [Actions.GetSourceTableSchema] ({ commit }, name: string) {
    const data = await API.getTableSchema(name);
    commit('setSourceTableSchema', { name, data: data.table_ddl });
  },
  async [Actions.GetSourceTableSample] ({ commit, $l }: any, params: { table: string; dbcName: string; saCode?: string }) {
    try {
      $l.start();
      const { table, dbcName, saCode } = params;
      const data = await API.getTableSample(table, dbcName, saCode);
      commit('setSourceTableSample', { name: table, data: data && data.example });
    } finally {
      $l.end();
    }
  },
  async [Actions.GetReleaseStatus] ({ commit }, payload) {
    const { id, table } = payload;
    const data = await API.getReleaseStatus(id, table);
    commit('setReleaseStatus', data);
  },
  async [Actions.GetRecentJobs] ({ commit }, id: string) {
    const data = await API.getRecentJobs(id);
    commit('setRecentJobs', data);
  },
  async [Actions.GetHadoopLocation] ({ commit, state }) {
    if (!state.task) {
      return;
    }
    const task = state.task;
    const location = await API.getHadoopLocation(task.sa_name, task.tgt_db, task.tgt_table);
    commit('setHadoopLocation', location);
  },
  async [Actions.GetDDL] ({ commit }, id: string) {
    const ddl = await API.getDDL(id);
    commit('setDDL', ddl);
  },
  async [Actions.GetUpsertScript] ({ commit, $l }: any, id: string) {
    try {
      $l.start();
      const script = await API.getUpsertScript(id);
      commit('setIncrementalScript', script);
    } finally {
      $l.end();
    }
  },
  async [Actions.CheckIsDa] ({ commit }, payload: string) {
    const sa = await doeRemoteService.getSADetail(payload);
    commit('setIsDA', sa.prmry_da_nt === Util.getNt());
  },
  async [Actions.CheckIsSAOwner] ({ commit }, payload: string) {
    const group = await GroupService.instance.getGroup(GroupIdTemplate.SA(payload));
    commit('setIsSAOwner', !!group.items.find(i => i.id === Util.getNt()));
  },
};

const AutoEL = {
  namespace: 'AutoEL',
  state,
  mutations,
  actions: VuexLoading.wrap(actions),
  getters,
};

export default AutoEL;
