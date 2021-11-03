import { MutationTree, ActionTree } from 'vuex';
import DoeRemoteService from '@/services/remote/DoeRemoteService';
const api = DoeRemoteService.instance;

interface State {
  userInfo: Record<string, any>;
  pendingRequest: Record<string, boolean>; // record http sending status
}

export enum Actions {
  getUserInfo = 'userSelection/get/userInfo',
}

const initState: State = {
  userInfo: {},
  pendingRequest: {},
};

const state: State = {
  ...initState,
};

const mutations: MutationTree<State> = {
  setUserInfo (state: State, payload) {
    state.userInfo = Object.assign({}, state.userInfo, payload);
  },
  setPendingRequest (state: State, payload) {
    state.pendingRequest = Object.assign({}, state.pendingRequest, payload);
  },
};

const actions: ActionTree<State, any> = {
  async [Actions.getUserInfo] ({ commit }: any, nt: string) {
    if (!state.pendingRequest[nt]) {
      commit('setPendingRequest', { [nt]: true });
      const resp = await api.getUserByNT(nt);
      const [user] = resp.response.docs;
      commit('setUserInfo', { [nt]: user });
      commit('setPendingRequest', { [nt]: false });
    } 
  },
};

const UserSelectionStore = {
  namespace: 'UserSelection',
  state,
  mutations,
  actions,
};

export default UserSelectionStore;
