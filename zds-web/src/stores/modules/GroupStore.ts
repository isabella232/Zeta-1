import GroupService from '@/services/remote/GroupService';
import Vue from 'vue';
import { MutationTree, ActionTree, GetterTree } from 'vuex';
import VuexLoading from 'vuex-loading-plugin';

const API = GroupService.instance;

interface State  {
  groups: {};
}
export enum Actions {
  GetAdminGroup = 'group/get/admin',
  GetGroup = 'group/get/group',
}

export enum Mutations {
  SetGroup = 'group/set/group',
  Clear = 'group/mut/clear',
}

export enum Getters {
  AdminGroup = 'group/getter/admin'
}

const initState: State = {
  groups: {},
};

const state: State = {
  ...initState,
};

const getters: GetterTree<State, {}> = {
  [Getters.AdminGroup] (state) {
    return state.groups['admin'];
  },
};

const mutations: MutationTree<State> = {
  [Mutations.SetGroup](state, payload) {
    Vue.set(state.groups, payload.id, payload);
  },
  [Mutations.Clear](state) {
    state.groups = {};
  },
};

const actions: ActionTree<State, any> = {
  async [Actions.GetAdminGroup]({ commit, $l }: any) {
    $l.start();
    const group = await API.getGroup('admin');
    commit(Mutations.SetGroup, group);
    $l.end();
  },

  async [Actions.GetGroup]({ commit, $l }: any, id: string) {
    $l.start();
    const group = await API.getGroup(id);
    commit(Mutations.SetGroup, group);
    $l.end();
  },

};

const GroupManagement = {
  namespace: 'GroupManagement',
  state,
  mutations,
  actions: VuexLoading.wrap(actions),
  getters,
};

export default GroupManagement;
