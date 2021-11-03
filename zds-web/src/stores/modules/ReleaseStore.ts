import { StoreOptions } from 'vuex';
import * as TYPE from '@/stores/MutationTypes';
import _ from 'lodash';
// Vue.use(Vuex)
const option: StoreOptions<any> = {
    state: {
        sa: "",
        releaseFile: [],
        pushTofile: {},
        projectOptions: process.env.VUE_APP_DSSRM_PROJECT,
        branchOptions: [],
        githubPushStatus: false,
        releaseSubmitLoading: false,
        validateJira: {},
        jiraStatus: false,
        iframeIdx: 0,
        execTask: [],
        sendManifestValidateStatus: false,
        commitData: [],
        rolloutStatus: false,
        execTaskStatus: false
    },
    mutations: {
        [TYPE.SET_SA](state, params) {
            state.sa = params
        },
        [TYPE.SET_RELEASE_FILE](state, params) {
            state.releaseFile = params
        },
        [TYPE.SET_PUSH_TO_FILE](state, params) {
            state.pushTofile = params
        },
        [TYPE.SET_PRODUCT_OPTIONS](state, params) {
            state.projectOptions = params
        },
        [TYPE.SET_BRANCH_OPTIONS](state, params) {
            state.branchOptions = params
        },
        [TYPE.SET_GITHUB_PUSH_STATUS](state, params) {
            state.githubPushStatus = params
        },
        [TYPE.SET_RELEASE_SUBMIT_LOADING](state, params) {
            state.releaseSubmitLoading = params
        },
        [TYPE.SET_VALIDATE_JIRA](state, params) {
            state.validateJira = params
        },
        [TYPE.SET_JIRA_STATUS](state, params) {
            state.jiraStatus = params
        },
        [TYPE.SET_IFRAME_IDX](state, params) {
            state.iframeIdx = params
        },
        [TYPE.SET_EXEC_TASK](state, params) {
            state.execTask = params
        },
        [TYPE.SET_SEND_MANIFEST_VALIDATE_STATUS](state, params) {
            state.sendManifestValidateStatus = params
        },
        [TYPE.SET_COMMIT_DATA](state, params) {
            state.commitData = params
        },
        [TYPE.SET_ROLLOUT_STATUS](state, params) {
            state.rolloutStatus = params
        },
        [TYPE.SET_EXEC_TASK_STATUS](state, params) {
            state.execTaskStatus = params
        }
    },
    getters: {
        getSA: state => state.sa,
        getReleaseFile: state => state.releaseFile,
        getPushTofile: state => state.pushTofile,
        getProjectOptions: state => state.projectOptions,
        getBranchOptions: state => state.branchOptions,
        getGithubPushStatus: state => state.githubPushStatus,
        getReleaseSubmitLoading: state => state.releaseSubmitLoading,
        getValidateJira: state => state.validateJira,
        getJiraStatus: state => state.jiraStatus,
        getIframeIdx: state => state.iframeIdx,
        getExecTask: state => state.execTask,
        getSendManifestValidateStatus: state => state.sendManifestValidateStatus,
        getCommitData: state => state.commitData,
        getRolloutStatus: state => state.rolloutStatus,
        getExecTaskStatus: state => state.execTaskStatus
    },
    actions: {
        setSA({ commit }, value) {
            commit(TYPE.SET_SA, value)
        },
        setReleaseFile({ commit }, value) {
            commit(TYPE.SET_RELEASE_FILE, value)
        },
        setPushTofile({ commit }, value) {
            commit(TYPE.SET_PUSH_TO_FILE, value)
        },
        setProjectOptions({ commit }, value) {
            commit(TYPE.SET_PRODUCT_OPTIONS, value)
        },
        setBranchOptions({ commit }, value) {
            commit(TYPE.SET_BRANCH_OPTIONS, value)
        },
        setGithubPushStatus({ commit }, value) {
            commit(TYPE.SET_GITHUB_PUSH_STATUS, value)
        },
        setReleaseSubmitLoading({ commit }, value) {
            commit(TYPE.SET_RELEASE_SUBMIT_LOADING, value)
        },
        setValidateJira({ commit }, value) {
            commit(TYPE.SET_VALIDATE_JIRA, value)
        },
        setJiraStatus({ commit }, value) {
            commit(TYPE.SET_JIRA_STATUS, value)
        },
        setIfameIdx({ commit }, value) {
            commit(TYPE.SET_IFRAME_IDX, value)
        },
        setExecTask({ commit }, value) {
            commit(TYPE.SET_EXEC_TASK, value)
        },
        setSendManifestValidateStatus({ commit }, value) {
            commit(TYPE.SET_SEND_MANIFEST_VALIDATE_STATUS, value)
        },
        setCommitData({ commit }, value) {
            commit(TYPE.SET_COMMIT_DATA, value)
        },
        setRolloutStatus({ commit }, value) {
            commit(TYPE.SET_ROLLOUT_STATUS, value)
        },
        setExecTaskStatus({ commit }, value) {
            commit(TYPE.SET_EXEC_TASK_STATUS, value)
        }
    }
};
export default option;
