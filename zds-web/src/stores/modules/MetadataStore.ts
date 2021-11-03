import { StoreOptions } from 'vuex';
import * as TYPE from '@/stores/MutationTypes';
import DoeRemoteService from '@/services/remote/DoeRemoteService';

const doeRemoteService = new DoeRemoteService().props({
  path:'metadata',
});


// Vue.use(Vuex)
const option: StoreOptions<any> = {
  state: {
    metadataSearch: {},
    reload: false,
    registerStepOne: {},
    registerStepTwo: [],
    registerStepThree: [],
    updateColumns: [],
    allPlatform: [],
    smartQueryArr: [],
    searchTablesArr: [],
    browseRoute: {},
    browseSearchRoute: {},
    tableManagementRoute: {},
    udfList: {
      data: [],
      total: 0,
      skip: 0,
      limit: 0,
    },
    udfData: {
      name: '',
      owner: '',
      platform: '',
      className: '',
      description: '',
      parameters: '',
      example: '',
    },
  },
  mutations: {
    [TYPE.SET_METADATA_SEARCH](state, params) {
      state.metadataSearch = params;
    },
    [TYPE.SET_METADATA_RELOAD](state, params) {
      state.reload = params;
    },
    [TYPE.SET_REGISTER_STEP_ONE](state, params) {
      state.registerStepOne = params;
    },
    [TYPE.SET_REGISTER_STEP_TWO](state, params) {
      state.registerStepTwo = params;
    },
    [TYPE.SET_REGISTER_STEP_TWO](state, params) {
      state.registerStepTwo = params;
    },
    [TYPE.SET_REGISTER_STEP_THREE](state, params) {
      state.registerStepThree = params;
    },
    [TYPE.SET_UPDATE_COLUMNS](state, params) {
      state.updateColumns = params;
    },
    [TYPE.SET_ALL_PLATFORM](state, params) {
      state.allPlatform = params;
    },
    [TYPE.SET_SMART_QUERY_ARR](state, params) {
      state.smartQueryArr = params;
    },
    [TYPE.SET_SEARCH_TABLES_ARR](state, params) {
      state.searchTablesArr = params;
    },
    [TYPE.SET_BROWSE_ROUTE](state, params) {
      state.browseRoute = params;
    },
    [TYPE.SET_BROWSE_SEARCH_ROUTE](state, params) {
      state.browseSearchRoute = params;
    },
    [TYPE.SET_UDF_LIST](state, payload) {
      state.udfList = payload;
    },
    [TYPE.SET_UDF](state, payload) {
      state.udfData = payload;
    },
    [TYPE.SET_TABLE_MANAGEMENT_ROUTE](state, params) {
      state.tableManagementRoute = params;
    },
  },
  getters: {
    getMetadataSearch: state => state.metadataSearch,
    getMetadataReload: state => state.reload,
    getRegisterStepOne: state => state.registerStepOne,
    getRegisterStepTwo: state => state.registerStepTwo,
    getRegisterStepThree: state => state.registerStepThree,
    getUpdateColumns: state => state.updateColumns,
    getAllPlatform: state => state.allPlatform,
    getSmartQueryArr: state => state.smartQueryArr,
    getSearchTablesArr: state => state.searchTablesArr,
    getBrowseRoute: state => state.browseRoute,
    getBrowseSearchRoute: state => state.browseSearchRoute,
    getTableManagementRoute: state => state.tableManagementRoute,
  },
  actions: {
    setMetadataSearch({ commit }, value) {
      commit(TYPE.SET_METADATA_SEARCH, value);
    },
    setMetadataReload({ commit }, value) {
      commit(TYPE.SET_METADATA_RELOAD, value);
    },
    setRegisterStepOne({ commit }, value) {
      commit(TYPE.SET_REGISTER_STEP_ONE, value);
    },
    setRegisterStepTwo({ commit }, value) {
      commit(TYPE.SET_REGISTER_STEP_TWO, value);
    },
    setRegisterStepThree({ commit }, value) {
      commit(TYPE.SET_REGISTER_STEP_THREE, value);
    },
    setUpdateColumns({ commit }, value) {
      commit(TYPE.SET_UPDATE_COLUMNS, value);
    },
    setAllPlatform({ commit }, value) {
      commit(TYPE.SET_ALL_PLATFORM, value);
    },
    setSmartQueryArr({ commit }, value) {
      commit(TYPE.SET_SMART_QUERY_ARR, value);
    },
    setSearchTablesArr({ commit }, value) {
      commit(TYPE.SET_SEARCH_TABLES_ARR, value);
    },
    setBrowseRoute({ commit }, value) {
      commit(TYPE.SET_BROWSE_ROUTE, value);
    },
    setBrowseSearchRoute({ commit }, value) {
      commit(TYPE.SET_BROWSE_SEARCH_ROUTE, value);
    },
    setTableManagementRoute({ commit }, value) {
      commit(TYPE.SET_TABLE_MANAGEMENT_ROUTE, value);
    },
    async getUdfList({ commit }, params) {
      const { data } = await doeRemoteService.getUDFList(params);
      commit(TYPE.SET_UDF_LIST, data);
    },
    async getUdfByName({ commit }, { name, db }) {
      const { data } = await doeRemoteService.getUDFByName(name, db);
      commit(TYPE.SET_UDF, data);
    },
  },
};
export default option;
