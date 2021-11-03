import { StoreOptions } from 'vuex';
import { AxiosResponse } from 'axios';
import UserInfoRemoteService from '@/services/remote/UserInfo';
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import * as TYPE from '@/stores/MutationTypes';
import config from '@/config/config';
import _ from 'lodash';
import * as Settings from '@/components/Settings/Settings';
import {BDP_STATUS, ClusterDict, Cluster} from '@/types/workspace';
import { ZetaException } from '@/types/exception';
import Util from '@/services/Util.service';
import DoeRemoteService from '@/services/remote/DoeRemoteService';
import Vue from 'vue';
const remoteService = new UserInfoRemoteService().props({
  path:'settings',
});
const notebookRemoteService = new NotebookRemoteService().props({ path: 'settings'});
const getClusterInfo = () => {
  const promises = _.map(config.zeta.notebook.connection.clusters, (clusterName: string) => {
    const clusterAlias = config.zeta.notebook.connection.clustersMap[clusterName];
    const clusterId = config.zeta.notebook.connection.clustersIdMap[clusterName];
    const result = {
      [clusterAlias]: {
        clusterAlias:clusterAlias,
        clusterId:clusterId,
        clusterName:clusterName,
        access:true,
        batchAccountOptions:[],
      } as Cluster,
    };
    return notebookRemoteService.getAccessClusters(clusterName).then((res: AxiosResponse<string[]>) => {
      const accounts = res.data;

      result[clusterAlias].batchAccountOptions = accounts;
      const validateUser = _.includes(accounts, Util.getNt());

      if (_.isEmpty(accounts) || !validateUser){
        result[clusterAlias].access = false;
      }
      return result;
    }).catch((e: ZetaException) => {
      e.resolve();
      result[clusterAlias].batchAccountOptions = [];
      result[clusterAlias].access = false;
      return result;
    });
  });
  return Promise.all(promises).then((resArr) => {

    const result = {};
    _.forEach(resArr, (option) => {
      Object.assign(result, option);
    });
    return result;
  });
};

interface UserCache {
  [props: string]: User;
}

interface User {
  eBayManagerID: string;
  mail: string;
  eBayFloor: string;
  employeeID: string;
  nt: string;
  userDisplay: string[];
  _id: string;
  eBaySite: string;
  eBayCostCenterText: string;
  preferred_name: string;
  company: string;
  last_name: string;
  eBayPersonnelStatus: string;
  department: string;
  eBayPositionText: string;
  eBayCompanyCode: string;
  givenName: string;
  co: string;
  eBayBuildingNumber: string;
  eBayEmployeeStatusCode: string;
  eBayCostCenter: string;
  eBayPersonnelID: string;
  employeeType: string;
  eBayOfficeNumber: string;
}

const option: StoreOptions<any> = {
  state: {
    user: {
      profile:{
        nt_login:'',
        firstName:'',
        lastName:'',
      } as Settings.Profile,
      bdpStatus: BDP_STATUS.unload,
      bdpMsg: '',
      batchAcctOptions:[],
      clusterOption:{} as ClusterDict,
      name:'',
      tdPass:null,
      githubToken:null,
      winPass: null,
      winPassValid: true,
      admin:0,
      aceAdmin: false,
      preference:{
        'editor-font-size':'14',
      } as Settings.Preference,
      kylinProjects: [],
      loading:false,
      tokenValid: true,
    },
    generalUserInfo: {} as UserCache,
  },
  mutations: {
    [TYPE.USR_PRFL_AJAX_LOADING](state) {
      state.user.loading = true;
    },
    [TYPE.USR_PRFL_AJAX_FINISH](state) {
      state.user.loading = false;
    },
    [TYPE.USR_SET_USR_PROFILE](state, { tdPass, preference  , githubToken, winPass} ) {
      function validatePass(current: any, newPass: any) {
        if(newPass === undefined){
          return current;
        } else {
          return newPass;
        }
      }
      state.user.tdPass = validatePass(state.user.tdPass, tdPass) || '';
      state.user.githubToken = validatePass(state.user.githubToken, githubToken) || '';
      state.user.winPass = validatePass(state.user.winPass, winPass) || '';
      Object.assign(state.user.preference,preference);
    },
    [TYPE.USR_SET_USR_PERSONAL_PREFERENCE](state, preference) {
      const p = Object.assign({}, state.user.preference, preference);
      Vue.set(state.user,'preference', p);
    },
    [TYPE.USR_SET_USR_INFO](state,{userInfo,nt}){
      if(userInfo && userInfo.first_name && userInfo.last_name){
        state.user.profile.firstName = userInfo.first_name;
        state.user.profile.lastName = userInfo.last_name;
      }
      state.user.profile.nt_login = nt;
      state.user.loading = false;
    },
    [TYPE.USR_SET_CLUSTER_OPTN](state,{clusterOption}){
      state.user.clusterOption = clusterOption;
    },
    [TYPE.USR_SET_BDP_MSG](state,{msg}){
      state.user.bdpMsg = msg;
    },
    [TYPE.USR_WINPASS_VALID](state, {valid}) {
      state.use.winPassValid = valid;
    },
    [TYPE.USR_SET_KYLIN_PJCT] (state, {projects}) {
      Object.assign(state.user.kylinProjects, projects);
    },
    [TYPE.USR_SET_OTHER_USER](state, payload) {
      // Vue.set(state.generalUserInfo[payload.nt] = payload;
      Vue.set(state.generalUserInfo, payload.nt, payload);
    },
    [TYPE.USR_SET_ACE_ADMIN](state, {aceAdmin}) {
      state.user.aceAdmin = aceAdmin;
    },
  },
  getters: {
    user: state => state.user,
    hasWinPass: state => Boolean(state.user.winPass),
    winPassValid: state => state.user.winPassValid,
    userPreference: state => state.user.preference,
    userPreferenceByKey: state => (key: string) => {
      return state.user.preference[key];
    },
  },
  actions: {
    setTokenValid({commit, state}, valid){
      state.user.tokenValid = valid;
    },
    setWinPassValid({commit}, {valid}) {
      commit(TYPE.USR_WINPASS_VALID, {valid});
    },
    initUserInfo({commit},{nt}){
      commit(TYPE.USR_PRFL_AJAX_LOADING);
      const url = `//doe.corp.ebay.com/api/solr/typeAhead?core=user&q=${nt}&field=nt&type=nt`;
      const url1 = `${config.zeta.base}users/me`;
      const baseInfo =  remoteService.getBaseInfo(nt).then((res)=>{
        const userInfo = res.data.response.docs[0];
        commit(TYPE.USR_SET_USR_INFO,{userInfo,nt});
        commit(TYPE.USR_PRFL_AJAX_FINISH);
      });
    },
    initUserPreference({commit},{nt}){
      commit(TYPE.USR_PRFL_AJAX_LOADING);
      const url1 = `${config.zeta.base}users/me`;
      const preference = remoteService.getUserPreference().then((res1)=>{
        const preferenceStr = res1.data;
        const tdPass = preferenceStr.tdPass;
        const admin = preferenceStr.admin;
        const aceAdmin = preferenceStr.aceAdmin;
        const githubToken = preferenceStr.githubToken;
        const winPass = preferenceStr.windowsAutoAccountPass;
        const preference: Settings.Preference = JSON.parse(preferenceStr.preference) || {sparkQueue:'',sparkQueryMaxResult:''};
        commit(TYPE.USR_SET_USR_PROFILE,{tdPass,preference,admin, githubToken, winPass});
        commit(TYPE.USR_SET_ACE_ADMIN,{aceAdmin});
        commit(TYPE.USR_PRFL_AJAX_FINISH);
        return preference;
      });
      return preference;
    },
    setUserInfo({commit},{nt,name,tdPass, githubToken, winPass, preference}){
      commit(TYPE.USR_PRFL_AJAX_LOADING);
      const pTdPass = tdPass === '***' ? null : tdPass;
      const pGithubToken = githubToken === '***' ? null : githubToken;
      const pWinPass = winPass === '***' ? null : winPass;
      return remoteService.setUserInfo(nt,name,pTdPass,pGithubToken, pWinPass, preference).then((res)=>{
        const {tdPass, githubToken} = res.data;
        const winPass = res.data.windowsAutoAccountPass;
        commit(TYPE.USR_SET_USR_PROFILE,{tdPass, githubToken,winPass});
        commit(TYPE.USR_SET_USR_PROFILE,{preference});
        commit(TYPE.USR_PRFL_AJAX_FINISH);
        return res;
      });
    },
    setWinPass({commit, state},{winPass}) {
      commit(TYPE.USR_PRFL_AJAX_LOADING);
      const nt = state.user.profile.nt_login;
      const name = `${state.user.profile.firstName} ${state.user.profile.lastName}`;
      return remoteService.setUserInfo(nt,name,null,null, winPass).then((res)=>{
        commit(TYPE.USR_SET_USR_PROFILE,{winPass});
        commit(TYPE.USR_PRFL_AJAX_FINISH);
        return res;
      });
    },
    setUserPreference({commit, state},{nt,preference}){
      const url = `${config.zeta.base}users/me`;
      const name = state.user.name;
      const body: Settings.UserInfo = {
        nt:nt,
        name,
        preference:JSON.stringify(preference),
      };
      commit(TYPE.USR_PRFL_AJAX_LOADING);
      return remoteService.setUserPreference(nt, name, preference).then((res)=>{
        commit(TYPE.USR_SET_USR_PROFILE,{preference});
        commit(TYPE.USR_PRFL_AJAX_FINISH);
        return res;
      });
    },
    updateUserPreference({commit, state}, partialPreference) {
      const preference = {
        ...state.user.preference,
        ...partialPreference,
      };
      const nt = state.user.profile.nt_login;
      const name = state.user.name;
      commit(TYPE.USR_PRFL_AJAX_LOADING);
      return remoteService.setUserPreference(nt, name, preference).then((res)=>{
        commit(TYPE.USR_SET_USR_PERSONAL_PREFERENCE, preference);
        commit(TYPE.USR_PRFL_AJAX_FINISH);
        return res;
      });
    },
    updateUserPreference2({commit, state}, partialPreference) {
      const preference = {
        ...state.user.preference,
        ...partialPreference,
      };
      const nt = state.user.profile.nt_login;
      const name = state.user.name;
      return remoteService.setUserPreference(nt, name, preference).then((res)=>{
        commit(TYPE.USR_SET_USR_PERSONAL_PREFERENCE, preference);
        return res;
      });
    },
    setUserBatchAcct({commit,state}){
      state.user.bdpStatus = BDP_STATUS.loading;
      return getClusterInfo().then((options) => {
        commit(TYPE.USR_SET_CLUSTER_OPTN,{clusterOption:options});
        state.user.bdpStatus = BDP_STATUS.success;
      }).catch(e => {
        e.resolve();
        state.user.bdpStatus = BDP_STATUS.error;
      });
    },
    getKylinProjects({ commit }) {
      remoteService.kylinAuthInfo().then((data) => {
        return data.data;
      }).catch(() => {
        return [];
      }).then((list) => {
        const pjcts = list.map((item: any) => item.name);
        commit(TYPE.USR_SET_KYLIN_PJCT, { projects: pjcts});
      });
    },
    async getUserByNT({ commit, state }, nt) {
      state.user.loading = true;
      try {
        const { response: { docs: [user] }} = await DoeRemoteService.instance.getUserByNT(nt);
        commit(TYPE.USR_SET_OTHER_USER, user);
      } catch (error) {

      } finally {
        state.user.loading = false;
      }
    },
  },
};
export default option;
