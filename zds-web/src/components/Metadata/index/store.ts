import { MutationTree, ActionTree, GetterTree } from 'vuex';
import VuexLoading from 'vuex-loading-plugin';
import DoeRemoteService from '@/services/remote/DoeRemoteService';
import { attempt } from 'lodash';

const API = DoeRemoteService.instance;

export enum Actions {
  GetDomainList = 'metadata/get/domain',
  GetRecentAnnouncement = 'metadata/get/recentannouncement',

}
export enum Mutations {
  SetDomainList = 'metadata/set/domain',
  SetRecentAnnouncement = 'metadata/set/recentannouncement',
}

export enum Getters {
}
export enum ViewMode {

}
interface State {
  domains: [];
  recentAnnouncements: [];
}

const initState: State = {
  domains: [],
  recentAnnouncements: [],
};

const state: State = {
  ...initState,
};

const getters: GetterTree<State, {}> = {

};

const mutations: MutationTree<State> = {
  [Mutations.SetDomainList] (state, payload) {
    state.domains = payload;
  },
  [Mutations.SetRecentAnnouncement] (state, payload) {
    state.recentAnnouncements = payload;
  },
};

const actions: ActionTree<State, any> = {
  async [Actions.GetDomainList] ({ commit }) {
    const resp = await API.getDomainInfo();
    const data = attempt(() => resp.data.data.value, []);
    data.forEach(domain => {
      domain.sub_domain && (domain.sub_domain = domain.sub_domain.split(','));
    });
    commit(Mutations.SetDomainList, data);
  },
  async [Actions.GetRecentAnnouncement] ({ commit }) {
    const resp = await API.getEnotifyByType({ limit: 15, type: 'PRODUCT' });
    const data = attempt(() => resp.data.data.value, []);
    commit(Mutations.SetRecentAnnouncement, data);
  },
};

const MetadataIndex = {
  namespace: 'MetadataIndex',
  state,
  mutations,
  actions: VuexLoading.wrap(actions),
  getters,
};

export default MetadataIndex;
