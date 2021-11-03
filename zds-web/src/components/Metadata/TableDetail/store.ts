import { MutationTree, ActionTree, GetterTree } from 'vuex';
import VuexLoading from 'vuex-loading-plugin';
import DoeRemoteService from '@/services/remote/DoeRemoteService';
import { uniq, get } from 'lodash';

const API = DoeRemoteService.instance;
const PlatformOrder = {
  Hermes: 0,
  Apollo_rno: 1,
  Hercules: 2,
  Ares: 3,
  NuMozart: 4,
};
export enum Actions {
  GetTableInfo = 'md/table/get/info',
  GetViewInfo = 'md/table/get/viewinfo',
  GetLabels = 'md/table/get/labels',
  GetSampleQueries = 'md/table/get/samplequeries',
}

export enum Mutations {
  Clear = 'md/table/mut/clear',
}

export enum Getters {
  TableSample = 'md/table/getters/tablesample',
  AllPlatforms = 'md/table/getters/allplatform',
  TableSubjectArea = 'md/table/getters/tablesa',
}

export interface State {
  tableInfo: any[];
  viewInfo: any[];
  labels: string[];
  queries: any[];
}
const initState: State = {
  tableInfo: [],
  viewInfo: [],
  labels: [],
  queries: [],
};

const state: State = {
  ...initState,
};

const getters: GetterTree<State, {}> = {
  [Getters.TableSample] (state) {
    return state.tableInfo[0];
  },
  [Getters.TableSubjectArea] (_state, getters) {
    return getters[Getters.TableSample] && getters[Getters.TableSample].subject_area;
  },
  [Getters.AllPlatforms] (state) {
    let platforms = uniq(state.tableInfo.map(t => t.platform));
    platforms.sort((a, b) => {
      return PlatformOrder[a] - PlatformOrder[b]
    });
    return platforms;
  },
};

const mutations: MutationTree<State> = {
  setTableInfo (state, payload) {
    state.tableInfo = payload;
  },
  setViewInfo (state, payload) {
    state.viewInfo = payload;
  },
  setLabels (state, payload) {
    state.labels = payload;
  },
  setSampleQueries (state, payload) {
    state.queries = payload;
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
  async [Actions.GetTableInfo] ({ commit, $l, dispatch }: any, { table, platform = '', db = '' }: { table: string; platform?: string; db?: string }) {
    $l.start();
    const {data: { data: { value }}} = await API.getTableInfo({ table, platform, db });
    commit('setTableInfo', value);
    dispatch(Actions.GetLabels, table);
    $l.end();
  },
  async [Actions.GetViewInfo] ({ commit, $l }: any, view: string) {
    $l.start();
    const {data: { data: { value }}} = await API.getMetaDataView(view);
    commit('setViewInfo', value);
    $l.end();
  },
  async [Actions.GetLabels] ({ commit, $l }: any, table: string) {
    $l.start();
    const res = await API.getTableLabels(table);
    commit('setLabels', get(res, 'data.data.value.label[0].labels', '').split(',').filter(Boolean));
    $l.end();
  },
  async [Actions.GetSampleQueries] ({ commit, $l }: any, table: string) {
    $l.start();
    const [samples, smartQueries] = await Promise.all([
      API.getSampleQuery({ table }),
      API.getSmartQuery({ tables: '.' + table, num: 3, isOr: true }),
    ]);
    const queries = [...samples.data.data.value, ...smartQueries.data.data.value];
    commit('setSampleQueries', queries);
    $l.end();
  },

};

const TableDetail = {
  namespace: 'TableDetail',
  state,
  mutations,
  actions: VuexLoading.wrap(actions),
  getters,
};

export default TableDetail;
