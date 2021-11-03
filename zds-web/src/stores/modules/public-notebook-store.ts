
import * as TYPE from '@/stores/MutationTypes';
import { PublicNotebookItem } from '@/types/workspace';
import { StoreOptions } from 'vuex';

const option: StoreOptions<any> = {
  state: {
    publicNotebooks: [] as PublicNotebookItem[],
  },
  mutations: {
    [TYPE.PUB_NOTE_INIT](state, notes) {
      state.publicNotebooks = notes;
    },
  },
  actions: {
    initPublicNotebooks({ commit }, publicNotebooks) {
      commit(TYPE.PUB_NOTE_INIT, publicNotebooks);
    },
  }
};
export default option;