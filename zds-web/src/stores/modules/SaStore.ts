import { MutationTree, ActionTree } from 'vuex';
import DoeRemoteService from '@/services/remote/DoeRemoteService';
import { SaDetail } from '@/types/metadata/sa';
import Util from '@/services/Util.service';
import VuexLoading from 'vuex-loading-plugin';

const doeRemoteService = DoeRemoteService.instance;

interface State {
  daList: any[];
  saList: SaDetail[];
  saDetail: SaDetail | null;
}

const initState: State = {
  daList: [],
  saList: [],
  saDetail: null,
};

const state: State = {
  ...initState,
};

const mutations: MutationTree<State> = {
  setDAList: (state: State, payload) => {
    state.daList = payload;
  },
  setSAList: (state: State, payload) => {
    state.saList = payload;
  },
  setSADetail: (state: State, payload) => {
    state.saDetail = payload;
  },
  clear (state: State) {
    state.saDetail = null;
    state.saList.length = 0;
  },
};

const actions: ActionTree<State, any> = {
  async getDAList ({ commit }: any) {
    const { data } = await doeRemoteService.getDA();
    commit('setDAList', data);
  },
  async getSAList ({ commit, $l }: any) {
    $l.start();
    const { data } = await doeRemoteService.getSAList();
    commit('setSAList', data);
    $l.end();
  },
  async getSADetail ({ commit, $l }: any, value: string) {
    $l.start();
    const data = await doeRemoteService.getSADetail(value);
    commit('setSADetail', data);
    $l.end();
  },
  async saveSADetail ({ dispatch, $l }: any, value: SaDetail ) {
    $l.start();
    await doeRemoteService.updateSADetail({...value, nt: Util.getNt()});
    dispatch('getSADetail', value.sbjct_area);
    $l.end();
  },
};

const SAStore = {
  namespaced: true,
  state,
  mutations,
  actions: VuexLoading.wrap(actions),
};

export default SAStore;