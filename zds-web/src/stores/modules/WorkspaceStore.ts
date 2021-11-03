import Vue from 'vue';
import { Module } from 'vuex';
import { IStore, store } from '../index';
import {
  IJob,
  JobStatus,
  IWorkspaceStore,
  IQuery,
  INotebook,
  ConnStatus,
  NotebookStatus,
  IJobStatus,
  QueryStatus,
  IConnection,
  MultiNotebook,
  Notebook,
  WorkSpaceType,
  WorkspaceBase,
  IWorkspace,
  MetaSheet,
} from '@/types/workspace';
import * as MUT from '../MutationTypes';
import _ from 'lodash';
import { IFile } from '@/types/repository';
import moment from 'moment';
import { Dashboard } from '@/types/workspace/dashboard.internal';
import { MetaSheetTableRow } from '@/types/meta-sheet';
import { WorkspaceSrv } from '@/services/Workspace.service';
import { NotebookJob, JobStateEnum, Query, QueryStateEnum } from '@/types/workspace/notebook-job';

function jobNotError(job: IJob): boolean {
  const status = job.status.valueOf();
  if (
    status === JobStatus.SUBMITTED ||
		status === JobStatus.PARTIAL_SUCCESS ||
		status === JobStatus.RUNNING ||
		status === JobStatus.SUBMITTED ||
		status === JobStatus.SUBMITTING
  )
    return true;
  return false;
}

function updateQueryField(
  state: IWorkspaceStore,
  jobId: string,
  reqId: string,
  seqId: number,
  update: Partial<IQuery>
) {
  // const job = state.jobs[jobId];
  // if (!job) {
  //   console.warn(`JobId ${jobId} does not exist in local store`);
  //   return;
  // }

  // const query = job.queries[seqId];
  // if (!query) {
  //   console.warn(
  //     `JobId ${jobId} ReqId ${reqId} does not exist in local store`
  //   );
  //   return;
  // }

  // if (query.status.status === QueryStatus.SUCCESS) {
  //   console.warn('query is success, will ignore this message');
  //   return;
  // }
  // const nqueries = job.queries.slice();
  // const nquery: IQuery = { ...query, ...update };
  // nqueries[query.seqId] = nquery;

  // state.jobs = {
  //   ...state.jobs,
  //   [jobId]: {
  //     ...job,
  //     queries: nqueries
  //   }
  // };
}
function getRightSeq (nbs: Dict<IWorkspace>) {
  const nb = _.chain(nbs)
    .map(nb => nb)
    .minBy('seq')
    .value();
  return nb && _.isNumber(nb.seq) ?  nb.seq : 0;
}
function getCurrentSeq(nbs: Dict<IWorkspace>) {
  const nb = _.chain(nbs)
    .map(nb => nb)
    .maxBy('seq')
    .value();
  return nb && nb.seq >= 0 ? nb.seq + 1 : 0;
}

/**
 * get notebook by notebook Id
 * @param workspaces
 * @param nId
 */
function getNotebookById(workspaces: Dict<IWorkspace>, nId: string):  Notebook | MultiNotebook | undefined{
  const notebooks = _.filter(workspaces,(ws: IWorkspace, id: string) => ws.type === WorkSpaceType.NOTEBOOK
  || ws.type === WorkSpaceType.SHARED_NOTEBOOK
	|| ws.type === WorkSpaceType.NOTEBOOK_COLLECTION
	|| ws.type === WorkSpaceType.NOTEBOOK_ZEPPELIN) as INotebook | [];
  const nb: Notebook | MultiNotebook | undefined = _.find(notebooks,(n: Notebook | MultiNotebook) => n.notebookId === nId);
  if(nb) {
    return nb;
  } else {
    const cnb: MultiNotebook = _.chain(notebooks)
      .filter((n: Notebook | MultiNotebook) => n.type === WorkSpaceType.NOTEBOOK_COLLECTION)
      .find((n: MultiNotebook) => n.contains(nId))
      .value() as MultiNotebook;
    if(cnb) return cnb.getSubNotebook(nId);
  }
  return undefined;
}

function getNotebookOrDashboardById(workspaces: Dict<IWorkspace>, nId: string):  Notebook | Dashboard | MultiNotebook | undefined{
  const notebooks = _.filter(workspaces,(ws: IWorkspace) => ws.type === WorkSpaceType.NOTEBOOK
  || ws.type === WorkSpaceType.SHARED_NOTEBOOK
	|| ws.type === WorkSpaceType.NOTEBOOK_COLLECTION
  || ws.type === WorkSpaceType.NOTEBOOK_ZEPPELIN
  || ws.type === WorkSpaceType.DASHBOARD
  || ws.type === WorkSpaceType.META_SHEET) as INotebook | [];
  const nb: Notebook | MultiNotebook | Dashboard | undefined = _.find(notebooks,(n: Notebook | MultiNotebook) => n.notebookId === nId);
  if(nb) {
    return nb;
  } else {
    const cnb: MultiNotebook = _.chain(notebooks)
      .filter((n: Notebook | MultiNotebook) => n.type === WorkSpaceType.NOTEBOOK_COLLECTION)
      .find((n: MultiNotebook) => n.contains(nId))
      .value() as MultiNotebook;
    if(cnb) return cnb.getSubNotebook(nId);
  }
  return undefined;
}
const option: Module<IWorkspaceStore, IStore> = {
  state: {
    workspaces: {},
    jobs: {},
    jobSeq: 0,
    loading: false,
    connStatus: ConnStatus.OFFLINE,
  },
  getters: {
    nbByFile: state => (file: IFile) => state.workspaces[file.notebookId],
    nbByNId: state => (nId: string) => getNotebookOrDashboardById(state.workspaces,nId),
    getJob: state => jobId => state.jobs[jobId],
    getQuery: state => (jobId: string, seqId: number) => {
      const job = state.jobs[jobId];
      if (!jobId || !job) {
        return undefined;
      }
      return job.getQuery(seqId);
    },
    isEmptyWorkspace: state => () => _.isEmpty(state.workspaces),
    activeWorkspaceId: state => state.activeWorkspaceId,
    /** @deprecated get notebook instance to watch connection's change*/
    currentConnection: state => {
      const activeNotebook = state.workspaces[state.activeWorkspaceId!];
      if (
        activeNotebook &&
				activeNotebook.type === WorkSpaceType.NOTEBOOK
      ) {
        return <INotebook>activeNotebook
          ? (<INotebook>activeNotebook).connection
          : {};
      } else {
        return {};
      }
    },
    /** @deprecated get notebook instance to watch connection's change*/
    currentCodeType: state => {
      const activeNotebook = state.workspaces[state.activeWorkspaceId!];
      if (
        activeNotebook &&
				activeNotebook.type === WorkSpaceType.NOTEBOOK
      ) {
        return <INotebook>activeNotebook &&
					(<INotebook>activeNotebook).connection
          ? (<INotebook>activeNotebook).connection!.codeType
          : null;
      } else {
        return null;
      }
    },
  },
  actions: {
    clearWorkspaceStore({ state }) {
      state.workspaces = {};
      state.jobs = {};
      state.jobSeq = 0;
    },
    setWorkspaceLoading({ state }, loading) {
      state.loading = loading;
    },
    setCurrentText({ commit, state }, { nid, text }) {
      commit(MUT.NB_SET_NB_TEXT, { nid, text });
    },
    /** @deprecated */
    setActiveNotebookById({ commit, state }, nid: string) {
      commit(MUT.NB_SET_ACTIVE_NB, nid);
      commit(MUT.NB_SET_RESULT_UPDATED_FALSE_WHEN_ACTIVE, nid);
    },
    setActiveJobById({ commit }, { nid, jid }) {
      commit(MUT.NB_SET_ACTIVE_JOB, { nid, jid });
    },
    closeNotebookById({ commit, state }, nid: string) {
      let nactiveId: null | string = null;
      if (state.activeWorkspaceId === nid) {
        const keys = _.chain(state.workspaces)
          .sortBy(['seq'])
          .map(w => w.notebookId)
          .value();
        //Object.keys(state.workspaces);
        const ind = keys.indexOf(state.activeWorkspaceId);
        /* Set active to previous tab */
        nactiveId = keys[ind + 1 == keys.length ? ind - 1 : ind + 1];
        commit(MUT.NB_SET_ACTIVE_NB, nactiveId);
      }
      commit(MUT.NB_CLOSE_NB, nid);
    },
    /** @deprecated */
    addNotebook({ commit }, nb) {
      commit(MUT.NB_ADD_NB, nb);
    },
    /**
		 * only be called in wobsocket handler
		 * */
    updateNotebook({ commit, state }, nb: Partial<INotebook>) {
      const nId = nb.notebookId;
      if(!nId){
        return;
      }
      /**
			 * modify by @tianrsun @2019-04-01
			 * handle sub notebook
			 */
      if(state.workspaces[nId]){
        commit(MUT.NB_UPDATE_NB, nb);
      }
      else{
        console.warn('Action `updateNotebook` won\'t handle subnotebook ');
      }

    },
    updateNotebookVariables({ commit, state }, { notebookId, variables}) {
      if(state.workspaces[notebookId]){
        commit(MUT.NB_UPDATE_NB_VARIABLES, { notebookId, variables });
      }
    },
    updateNotebookLayout({ commit, state }, { notebookId, layout}) {
      if(state.workspaces[notebookId]){
        commit(MUT.NB_UPDATE_NB_LAYOUT, { notebookId, layout });
      }
    },
    initWorkspace({ commit }, ws){
      commit(MUT.NB_INIT_WS, ws);
    },
    initWorkspaceLeft({ commit }, ws){
      commit(MUT.NB_INIT_WS_LEFT, ws);
    },
    addActiveWorkSpace({ commit }, ws: Partial<IWorkspace>){
      commit(MUT.NB_ADD_WS, ws);
      commit(MUT.NB_SET_ACTIVE_NB, ws.notebookId);
    },
    updateWorkspace({ commit }, ws: Partial<IWorkspace>) {
      commit(MUT.NB_UPDATE_WS, ws);
    },
    /**
     * @deprecated replace by @function queryOnProgress
     */
    updateQuery({ commit, state }, { jobId, seqId, query }) {
      updateQueryField(state, jobId, '', seqId, query);
    },
    closeJobById({ commit, state }, { nid, jobId }) {
      /* Set new active job */
      const nb = getNotebookById(state.workspaces,nid);
      if(!nb) {
        console.warn('cannot find notebook: ' + nid);
        return;
      }
      if (jobId === nb.activeJobId) {
        const jobs = _.pick(state.jobs, nb.jobs);
        const indices = Object.keys(jobs).reverse();
        const nActiveJid = indices[indices.indexOf(jobId) - 1];
        commit(MUT.NB_SET_ACTIVE_JOB, { nid, jid: nActiveJid });
      }
      /* Close job */
      commit(MUT.NB_CLOSE_JOB, { jobId });
    },
    setSubNotebookStatus({ commit, state, dispatch }, { nid, status }) {
      const notebook = getNotebookById(state.workspaces, nid) as MultiNotebook;
      if(!notebook) {
        console.warn('cannot find notebook: ' + nid);
        return;
      }
      if(_.isEmpty(notebook.subNotebooks)) {
        console.warn('cannot find subnotebook in notebook: ' + nid);
        return;
      }
      _.forEach(notebook.subNotebooks, (nb: INotebook) => {
        dispatch('setNotebookStatus',{ nid: nb.notebookId, status});
      });

    },
    setNotebookStatus({ commit, state, dispatch }, { nid, status }) {
      /** halt jobs when disconnect */
      const notebook = getNotebookById(state.workspaces, nid);
      if(!notebook) {
        console.warn('cannot find notebook: ' + nid);
        return;
      }
      if(notebook && notebook.status === NotebookStatus.DISCONNECTING && status === NotebookStatus.OFFLINE){
        dispatch('haltJobsByNId',{ nid });
      }
      commit(MUT.NB_SET_NB_STATUS, { nid, status });
    },
    addSubNotebook({ commit }, snb: INotebook){
      commit(MUT.NB_CRT_SUB_NB, snb);
    },
    updateSubNotebook({ commit }, snb: INotebook) {
      commit(MUT.NB_UDT_SUB_NB, snb);
    },
    deleteSubNotebook({ commit }, snb: INotebook) {
      commit(MUT.NB_DEL_SUB_NB, snb);
    },
    /**
     * @deprecated
     * replace by @function jobOnSuccess & @function jobOnError
     */
    setJobStatus({ commit, state }, { jobId, reqId, status, info }) {
      commit(MUT.NB_SET_JOB_STATUS, { jobId, reqId, status, info });
    },
    jobOnSuccess({ commit, state, dispatch }, { nid, jobId, reqId, status, info, init }){
      if (!state.jobs[jobId]) {
        dispatch('handleJobReady', { nid, jobId, reqId });
      }
      commit(MUT.NB_JOB_ONSUCCESS, { jobId, reqId, status, info });
      dispatch('setNotebookStatus', { nid, status: NotebookStatus.IDLE });
      // disable when init
      if (!init) {
        commit(MUT.NB_SET_RESULT_UPDATED_TRUE_WHEN_INACTIVE, nid);
      }

    },
    jobOnError({ commit, state, dispatch }, { nid, jobId, reqId, status, info }){
      if (!state.jobs[jobId]) {
        dispatch('handleJobReady', { nid, jobId, reqId });
      }
      commit(MUT.NB_JOB_ONERROR, { jobId, reqId, status, info });
      // TODO disable when init
      commit(MUT.NB_SET_RESULT_UPDATED_TRUE_WHEN_INACTIVE, nid);
    },
    addJob({ commit, state }, { nid, jobId, reqId }) {
      commit(MUT.NB_ADD_JOB, { nid, jobId, reqId });
      commit(MUT.NB_ADD_JOB_TO_NOTEBOOK, { nid, jobId, reqId });
      commit(MUT.NB_SET_ACTIVE_JOB, { nid, jid: jobId });
    },
    handleJobReady({ commit, dispatch, state }, { nid, jobId, reqId }) {
      /* We need handler for websocket JOB_READY packet,
             * because SQL_SPLITTED message may arrive earlier than JOB_READY.
             * (library bug?)
             * Once we receive SQL_SPLITTED and found no
             * corresponding job in job store. We call handleJobReady()
             */
      if (!state.jobs[jobId]) {
        dispatch('setNotebookStatus', { nid, status: NotebookStatus.RUNNING });
        dispatch('addJob', { nid, jobId, reqId });
      }
    },
    addQueries({ dispatch, commit, state }, { nid, jobId, reqId, codes }) {
      if (!state.jobs[jobId]) {
        dispatch('handleJobReady', { nid, jobId, reqId });
      }
      commit(MUT.NB_ADD_QUERIES, { jobId, codes });
    },
    initQuery({ commit }, { jobId, seqId, code, statementId }) {
      commit(MUT.NB_INIT_QUERIES, { jobId, seqId, code, statementId });
    },
    /**
     * @deprecated replace by @function queryOnSuccess
     */
    addQueryResult(
      { commit, state },
      { nid, jobId, reqId, seqId, headers, rows, content }
    ) {
      commit(MUT.NB_SET_QUERY_RESULT, {
        jobId,
        reqId,
        seqId,
        headers,
        rows,
        content,
      });
      commit(MUT.NB_SET_QUERY_STATUS, {
        jobId,
        reqId,
        seqId,
        status: { status: QueryStatus.SUCCESS } as IJobStatus,
      });
      commit(MUT.NB_SET_RESULT_UPDATED_TRUE_WHEN_INACTIVE, nid);
    },
    queryStart({ commit, state, dispatch }, { nid, jobId, seqId, query: { startTime, statementId }}) {
      if (!state.jobs[jobId]) {
        dispatch('handleJobReady', { nid, jobId });
      }
      const query = state.jobs[jobId].queries[seqId];
      if (!query) {
        dispatch('initQuery', { jobId, seqId });
      }
      commit(MUT.NB_QUERY_START, { jobId, seqId, query: { startTime, statementId } });
    },
    queryOnSuccess({ commit, state, dispatch }, { nid, jobId, reqId, seqId, headers, rows, content, startTime, endTime, code, updatedCount, statementId, init }) {
      if (!state.jobs[jobId]) {
        dispatch('handleJobReady', { nid, jobId, reqId });
      }
      const query = state.jobs[jobId].queries[seqId];
      if (!query) {
        dispatch('initQuery', { jobId, seqId, statementId });
      }
      commit(MUT.NB_QUERY_ONSUCCESS, { nid, jobId, reqId, seqId, headers, rows, content, startTime, endTime, code, updatedCount });
      // disable when init
      if (!init) {
        commit(MUT.NB_SET_RESULT_UPDATED_TRUE_WHEN_INACTIVE, nid);
      }
    },
    // enable init query & job
    queryOnProgress({ commit, state, dispatch }, { nid, jobId, reqId, seqId, status, code, statementId }) {
      if (!state.jobs[jobId]) {
        dispatch('handleJobReady', { nid, jobId, reqId });
      }
      const query = state.jobs[jobId].queries[seqId];
      if (!query) {
        dispatch('initQuery', { jobId, seqId, statementId });
      }
      commit(MUT.NB_QUERY_PROGRESS, { jobId, reqId, seqId, status, code });
    },

    haltAll({ commit, state }) {
      const nbs = state.workspaces;
      const nbIds = Object.keys(nbs);
      const jobs = state.jobs;

      /* Terminate notebooks */
      for (let i = 0; i < nbIds.length; i++) {
        const nid = nbIds[i];
        const nb = nbs[nid] as INotebook;
        let first = true;
        if (
          nb.status === NotebookStatus.RUNNING ||
					nb.status === NotebookStatus.SUBMITTING
        ) {
          /* Terminate Jobs */
          //* Set Notebook Status to Idle
          //* To avoid status check, Notebook status: Submitting/Running => Stopping => Idle
          commit(MUT.NB_SET_NB_STATUS, { nid, status: NotebookStatus.STOPPING });
          commit(MUT.NB_SET_NB_STATUS, { nid, status: NotebookStatus.IDLE });
          for (let j = 0; j < nb.jobs.length; j++) {
            const jid = nb.jobs[j];
            const job = jobs[jid];
            // halt job here

            /* Terminate queries */
            commit(MUT.NB_JOB_ONERROR, {
              jobId: job.jobId,
              reqId: job.reqId,
            });
            for (let i = 0; i < job.queries.length; i++) {
              const query = job.queries[i];
              if (first) {
                commit(MUT.NB_QUERY_ONERROR, {jobId: job.jobId, reqId: job.reqId, seqId: query.seqId});
                if (query.status.status.valueOf() == QueryStateEnum.ERROR) {
                  first = false;
                }
              } else {
                commit(MUT.NB_QUERY_CANCELED, {jobId: job.jobId, reqId: job.reqId, seqId: query.seqId});
              }
            }
          }
        }
      }
    },
    haltSubNotebookJobsByNId({ commit, state, dispatch }, { nid }) {
      const notebook = getNotebookById(state.workspaces, nid) as MultiNotebook;
      if(!notebook) {
        console.warn('cannot find notebook: ' + nid);
        return;
      }
      _.forEach(notebook.subNotebooks, (nb: INotebook) => {
        dispatch('haltJobsByNId',{ nid: nb.notebookId});
      });
    },
    haltJobsByNId({ commit, state }, { nid }){
      const nb = getNotebookById(state.workspaces,nid);
      if(!nb) {
        console.warn('cannot find notebook: ' + nid);
        return;
      }
      const jobs = state.jobs;
      let first = true;
      for (let j = 0; j < nb.jobs.length; j++) {
        const jid = nb.jobs[j];
        const job = jobs[jid];
        if(!job) break;
        const status = job.status.valueOf();
        commit(MUT.NB_JOB_ONERROR, {
          jobId: job.jobId,
          reqId: job.reqId,
        });
        for (let i = 0; i < job.queries.length; i++) {
          const query = job.queries[i];
          if (first) {
            commit(MUT.NB_QUERY_ONERROR, {jobId: job.jobId, reqId: job.reqId, seqId: query.seqId});
            if (query.status.status.valueOf() == QueryStateEnum.ERROR) {
              first = false;
            }
          } else {
            commit(MUT.NB_QUERY_CANCELED, {jobId: job.jobId, reqId: job.reqId, seqId: query.seqId});
          }
        }

      }
    },

    haltJob({ commit, state }, { nid, jobId, reqId, seqId, info, init }) {

      commit(MUT.NB_SET_NB_STATUS, { nid, status: NotebookStatus.IDLE });
      // disable when init
      if (!init) {
        commit(MUT.NB_SET_RESULT_UPDATED_TRUE_WHEN_INACTIVE, nid);
      }
      commit(MUT.NB_JOB_ONERROR, { jobId, reqId, info });
      const job = state.jobs[jobId];
      let first = true;
      for (let i = 0; i < job.queries.length; i++) {
        const query = job.queries[i];
        if (first) {
          commit(MUT.NB_QUERY_ONERROR, {jobId: job.jobId, reqId: job.reqId, seqId: query.seqId});
          if (query.status.status.valueOf() == QueryStateEnum.ERROR) {
            first = false;
          }
        } else {
          commit(MUT.NB_QUERY_CANCELED, {jobId: job.jobId, reqId: job.reqId, seqId: query.seqId});
        }
      }
    },
    haltJobOnCancel({ commit, state }, { nid, jobId, reqId, seqId }) {

      commit(MUT.NB_SET_NB_STATUS, { nid, status: NotebookStatus.IDLE });
      // commit(MUT.NB_SET_JOB_STATUS, { jobId, reqId, status: jobStatus });
      commit(MUT.NB_SET_RESULT_UPDATED_TRUE_WHEN_INACTIVE, nid);

      commit(MUT.NB_JOB_ONERROR, { jobId, reqId });
      const job = state.jobs[jobId];
      for (let i = 0; i < job.queries.length; i++) {
        const query = job.queries[i];
        commit(MUT.NB_QUERY_CANCELED, {jobId: job.jobId, reqId: job.reqId, seqId: query.seqId});
      }
    },
    /**
     * @deprecated replaced by @function queryOnProgress
     */
    setQueryProgress({ commit, state }, { jobId, reqId, seqId, update }) {
      commit(MUT.NB_SET_QUERY_PROGRESS, { jobId, reqId, seqId, update });
    },
    setStagedCode({ commit }, { nid, code }) {
      commit(MUT.NB_SET_STAGED_CODE, { nid, code });
    },
    setConnection(
      { commit },
      {
        notebookId,
        connection,
      }: { notebookId: string; connection: IConnection }
    ) {
      console.warn(connection);
      commit(MUT.NB_SET_CNN_INFO, {
        notebookId,
        connection,
      });
    },
    setConnectProgress({ commit, state }, { nid, progress}) {
      commit(MUT.NB_SET_CONNECT_PROGRESS,{ nid, progress} );
    },
    setUpRatio({commit, state}, { nid, upRatio}){
      commit(MUT.NB_SET_UPRATIO, { nid, upRatio});
    },
    setPackages({commit, state}, { nid, packages}){
      commit(MUT.NB_SET_PACKAGES, { nid, packages});
    },
    updateWSMetaSheet({commit, state}, metasheet: MetaSheetTableRow) {
      commit(MUT.NB_UPDATE_METASHEET, metasheet);
    },
    closeMetaSheetById({ commit, state }, nid: string) {
      let nactiveId: null | string = null;
      if (state.activeWorkspaceId === nid) {
        const keys = _.chain(state.workspaces)
          .sortBy(['seq'])
          .map(w => w.notebookId)
          .value();
        //Object.keys(state.workspaces);
        const ind = keys.indexOf(state.activeWorkspaceId);
        /* Set active to previous tab */
        nactiveId = keys[ind + 1 == keys.length ? ind - 1 : ind + 1];
        commit(MUT.NB_SET_ACTIVE_NB, nactiveId);
      }
    },
  },
  mutations: {
    [MUT.NB_SET_PACKAGES](state, { nid, packages }) {
      const nb: MultiNotebook | Notebook | undefined = getNotebookById(state.workspaces,nid);
      if(!nb) {
        console.warn('cannot find notebook: ' + nid);
        return;
      }
      const nbPackages = nb.packages?nb.packages:{};
      Vue.set(nb, 'packages', packages);
    },
    [MUT.NB_SET_UPRATIO](state, { nid, upRatio }) {
      const nb: MultiNotebook | Notebook | undefined = getNotebookById(state.workspaces,nid);
      if(!nb) {
        console.warn('cannot find notebook: ' + nid);
        return;
      }
      Vue.set(nb, 'upRatio', upRatio);
    },
    [MUT.NB_CLOSE_NB](state, nid) {
      const ws = state.workspaces[nid] as WorkspaceBase;
      if(!ws){
        return;
      }
      const seq = ws.seq;
      state.workspaces = _.omit(state.workspaces, nid);
      _.forEach(state.workspaces, (note, id) => {
        if (
          (<WorkspaceBase>note).type === WorkSpaceType.NOTEBOOK &&
					(<WorkspaceBase>note).seq > seq
        ) {
          (<WorkspaceBase>note).seq -= 1;
        }
      });
    },
    [MUT.NB_SET_ACTIVE_NB](state, nid) {
      Vue.set(state, 'activeWorkspaceId', nid);
    },
    [MUT.NB_SET_RESULT_UPDATED_FALSE_WHEN_ACTIVE](state, nid) {
      //! comments by tianrsun @2019-07-04
      //! enhance sub notebook
      const nb: MultiNotebook | Notebook | undefined = getNotebookById(state.workspaces,nid);
      if(!nb) {
        console.warn('cannot find notebook: ' + nid);
        return;
      } else if (nb && nid === state.activeWorkspaceId) {
        nb.resultUpdated = false;
      }
    },
    [MUT.NB_SET_RESULT_UPDATED_TRUE_WHEN_INACTIVE](state, nid) {
      //! comments by tianrsun @2019-07-04
      //! enhance sub notebook
      const nb: MultiNotebook | Notebook | undefined = getNotebookById(state.workspaces,nid);
      if(!nb) {
        console.warn('cannot find notebook: ' + nid);
        return;
      }
      let ws: INotebook| undefined = undefined;
      if(nb.nbType === 'sub_nb' && nb.collectionId) {
        ws = state.workspaces[nb.collectionId] as INotebook;
      } else if(nb.nbType === 'single' || nb.nbType === 'collection'){
        ws = nb;
      }
      if(!ws) {
        return;
      }
      if (ws && ws.notebookId !== state.activeWorkspaceId) {
        ws.resultUpdated = true;
      }
    },
    [MUT.NB_SET_ACTIVE_JOB](state, { nid, jid }) {
      //! comments by tianrsun @2019-07-04
      //! enhance sub notebook
      const nb: MultiNotebook | Notebook | undefined = getNotebookById(state.workspaces,nid);
      if(!nb) {
        console.warn('cannot find notebook: ' + nid);
        return;
      }
      Vue.set(nb, 'activeJobId', jid);

    },
    // [MUT.NB_SET_ACITVE_QUERY](state, { jid, qid }) {
    //     let job = state.jobs[jid];
    //     if (job) job.activeQueryId = qid;
    // },
    [MUT.NB_SET_NB_TEXT](state, { nid, text }) {
      //! comments by tianrsun @2019-07-04
      //! enhance sub notebook
      const nb: MultiNotebook | Notebook | undefined = getNotebookById(state.workspaces,nid);
      if(!nb) {
        console.warn('cannot find notebook: ' + nid);
        return;
      }
      Vue.set(nb, 'code', text);

    },
    [MUT.NB_SET_NB_STATUS](state, { nid, status }) {
      //! comments by tianrsun @2019-07-04
      //! enhance sub notebook
      const nb: MultiNotebook | Notebook | undefined = getNotebookById(state.workspaces,nid);
      if(!nb) {
        console.warn('cannot find notebook: ' + nid);
        return;
      }
      Vue.set(nb, 'status', status);

    },
    [MUT.NB_SET_JOB_STATUS](state, { jobId, reqId, status, info }) {
      // const job: IJob = state.jobs[jobId];
      // if (!job) {
      //   console.warn(`JobId ${jobId} does not exist in local store`);
      //   return;
      // }

      // const old_info = job.info;
      // state.jobs = {
      //   ...state.jobs,
      //   [jobId]: {
      //     ...job,
      //     status,
      //     info: info || old_info
      //   }
      // };
    },
    [MUT.NB_UPDATE_NB_VARIABLES](state, { notebookId, variables}) {
      const nb = state.workspaces[notebookId] as Notebook;
      const prefrence = nb.preference ? nb.preference : {};
      if (!prefrence['notebook.variables']) {
        prefrence['notebook.variables'] = {};
      }
      if (!prefrence['notebook.vargenerators']) {
        prefrence['notebook.vargenerators'] = {};
      }
      _.forEach(variables, (v, name) => {
        (prefrence['notebook.variables'] as Dict<string>)[name] = v.value;
        (prefrence['notebook.vargenerators'] as Dict<string>)[name] = v.vargenerator;
      });
      Vue.set(nb.preference!, 'notebook.variables', prefrence['notebook.variables']);
      Vue.set(nb.preference!, 'notebook.vargenerators', prefrence['notebook.vargenerators']);

    },
    [MUT.NB_UPDATE_NB_LAYOUT] (state, { notebookId, layout}) {
      const nb = state.workspaces[notebookId] as Notebook;
      nb.preference = nb.preference? nb.preference : {};
      const prefrence = nb.preference;
      if (!prefrence['notebook.layout']) {
        prefrence['notebook.layout'] = {};
      }
      Vue.set(nb.preference!, 'notebook.layout', layout);
    },
    [MUT.NB_SET_CNN_INFO](
      state,
      {
        notebookId,
        connection,
      }: { notebookId: string; connection: IConnection }
    ) {
      const nb = state.workspaces[notebookId] as INotebook;
      if (nb) {
        Object.assign(nb.connection, connection);
      }
    },
    [MUT.NB_ADD_JOB](state, { nid, jobId, reqId }) {
      if (Object.keys(state.jobs).indexOf(jobId) !== -1) {
        console.warn(`JobId ${jobId} is overriden.`);
      }
      // comments by tianrsun @2019-07-05
      // get correct job sequence by notebook's job
      const nb = getNotebookById(state.workspaces, nid);
      if(!nb) {
        console.warn('cannot find notebook id: ' + nid);
      }
      let jobSeq = 0;
      if(nb && !_.isEmpty(nb.jobs)){
        jobSeq = _.chain(nb.jobs).map(id => state.jobs[id] ? state.jobs[id].seq : undefined).max().value() || jobSeq;
      }
      jobSeq++;

      const job = new NotebookJob(JobStateEnum.RUNNING);
      job.jobId = jobId;
      job.seq = jobSeq;
      job.reqId = reqId;
      job.startTime = moment().valueOf() + '';
      Vue.set(state.jobs, jobId, job);
    },
    [MUT.NB_ADD_JOB_TO_NOTEBOOK](state, { nid, jobId, reqId }) {
      //! comments by tianrsun @2019-07-04
      //! enhance sub notebook
      const nb: MultiNotebook | Notebook | undefined = getNotebookById(state.workspaces,nid);
      if(!nb) {
        console.warn('cannot find notebook: ' + nid);
        return;
      }
      const jobs = [...nb.jobs, jobId];
      Object.assign(nb, { jobs });
    },
    [MUT.NB_ADD_QUERIES](
      state,
      {
        jobId,
        codes,
      }: { jobId: string; codes: Array<{ seq: number; code: string }> }
    ) {
      const job = state.jobs[jobId];
      if (!job) {
        console.warn(`JobId ${jobId} does not exist in local store`);
        return;
      }

      // let queries = codes.map(({ code, seq }) => ({
      // 	query: code,
      // 	status: <IJobStatus>{
      // 		status: QueryStatus.WAITING,
      // 		progress: 0
      // 	},
      // 	seqId: seq
      // }));
      const queries = codes.map(({ code, seq }) => {
        const q = new Query(seq, QueryStateEnum.WAITING);
        q.query = code;
        q.status.progress = 0;
        return q;
      });
      Vue.set(state.jobs[jobId], 'queries', queries);
      // state.jobs = {
      // 	...state.jobs,
      // 	[jobId]: {
      // 		...job,
      // 		queries
      // 	}
      // };
    },
    [MUT.NB_INIT_QUERIES](state, { jobId, seqId, code, statementId }) {
      const job = state.jobs[jobId];
      if (!job) {
        console.warn(`JobId ${jobId} does not exist in local store`);
        return;
      }
      if (!Array.isArray(job.queries)) {
        Vue.set(state.jobs[jobId], 'queries', []);
      }

      const query = new Query(seqId);
      if (code) {
        query.query = code;
      }
      query.statementId = statementId;
      Vue.set(state.jobs[jobId].queries, seqId, query);
    },
    [MUT.NB_SET_QUERY_RESULT](
      state,
      { jobId, reqId, seqId, headers, rows, content }
    ) {
      updateQueryField(state, jobId, reqId, seqId, {
        headers: headers ? headers.slice() : null,
        rows: rows ? rows.slice() : null,
        content,
      });
    },
    [MUT.NB_SET_QUERY_STATUS](state, { jobId, reqId, seqId, status }) {
      updateQueryField(state, jobId, reqId, seqId, { status });
    },
    [MUT.NB_SET_QUERY_PROGRESS](state, { jobId, reqId, seqId, update }) {
      updateQueryField(state, jobId, reqId, seqId, update);
    },
    [MUT.NB_CLOSE_JOB](state, { jobId }) {
      // state.jobs = _.omit(state.jobs, jobId);
      Vue.delete(state.jobs, jobId);
    },
    [MUT.NB_SET_STAGED_CODE](state, { nid, code }) {
      //! comments by tianrsun @2019-07-04
      //! enhance sub notebook
      const nb: MultiNotebook | Notebook | undefined = getNotebookById(state.workspaces,nid);
      if(!nb) {
        console.warn('cannot find notebook: ' + nid);
        return;
      }
      Object.assign(nb, { stagedCode: code });
    },
    /**
		 * @deprecated
		 */
    [MUT.NB_ADD_NB](state, nb: INotebook) {
      const notebook = state.workspaces[nb.notebookId];
      if(!_.isEmpty(notebook)) return;
      if (nb.seq >= 0) {
      } else {
        nb.seq = getCurrentSeq(state.workspaces);
      }
      Vue.set(state.workspaces, nb.notebookId, nb);
    },
    [MUT.NB_INIT_WS](state, nb: IWorkspace) {
      const notebook = state.workspaces[nb.notebookId];
      if(!_.isEmpty(notebook)) return;

      if (nb.seq >= 0) {
      } else {
        nb.seq = getCurrentSeq(state.workspaces);
      }
      Vue.set(state.workspaces, nb.notebookId, nb);
    },
    [MUT.NB_INIT_WS_LEFT] (state, nb: IWorkspace) {
      const notebook = state.workspaces[nb.notebookId];
      if (!_.isEmpty(notebook)) return;
      const rightSeq = getRightSeq(state.workspaces);
      nb.seq = rightSeq - 1;
      Vue.set(state.workspaces, nb.notebookId, nb);
    },
    [MUT.NB_ADD_WS](state, nb: IWorkspace) {
      const notebook = state.workspaces[nb.notebookId];
      if(!_.isEmpty(notebook) && notebook.seq >= 0) return;
      // modify by tianrsun
      // ! re-compute the seq of note/workspace
      // ! should be the last one
      // if (nb.seq >= 0) {
      // } else {
      // 	nb.seq = getCurrentSeq(state.workspaces);
      // }
      nb.seq = getCurrentSeq(state.workspaces);
      // state.workspaces[nb.notebookId] = nb
      Vue.set(state.workspaces, nb.notebookId, nb);
    },
    [MUT.NB_UPDATE_NB](state, nb: INotebook) {
      const preference = Object.assign({}, (state.workspaces[nb.notebookId] as INotebook ).preference || {}, nb.preference);
      const connection = Object.assign({}, (state.workspaces[nb.notebookId] as INotebook ).connection || {}, nb.connection);
      Object.assign(state.workspaces[nb.notebookId], nb, { preference, connection });
    },
    [MUT.NB_UPDATE_WS](state, ws: IWorkspace) {
      const old_ws = state.workspaces[ws.notebookId];
      if (!old_ws) {
        return;
      }
      Object.assign(state.workspaces[ws.notebookId], ws);
    },
    [MUT.NB_SET_CONNECT_PROGRESS](state, { nid, progress }) {
      //! comments by tianrsun @2019-07-05
      //! enhance sub notebook
      const nb: MultiNotebook | Notebook | undefined = getNotebookById(state.workspaces,nid);
      if(!nb) {
        console.warn('cannot find notebook: ' + nid);
        return;
      }
      Vue.set(nb, 'progress', progress);
    },
    /** sub notebook  */
    [MUT.NB_CRT_SUB_NB](state, nb: INotebook) {
      if(!nb.collectionId) return;
      const collection: MultiNotebook = state.workspaces[nb.collectionId] as MultiNotebook;
      collection.addSubNotebook(nb);
    },
    [MUT.NB_UDT_SUB_NB](state, nb: INotebook) {
      if(!nb.collectionId) return;
      const collection: MultiNotebook = state.workspaces[nb.collectionId] as MultiNotebook;
      collection.setSubNotebook(nb);
    },
    [MUT.NB_DEL_SUB_NB](state, nb: INotebook) {
      if(!nb.collectionId) return;
      const collection: MultiNotebook = state.workspaces[nb.collectionId] as MultiNotebook;
      collection.delSubNotebook(nb.notebookId);
    },
    [MUT.NB_UPDATE_METASHEET](state, metasheet: MetaSheetTableRow) {
      if(metasheet && metasheet.id) {
        const metaSheetItem = _.cloneDeep(state.workspaces[metasheet.id]) as MetaSheet;
        if(metaSheetItem) {
          if(metasheet.schemeConfig) {
            const {columnTypeMap, columns} = WorkspaceSrv.getMetaSheetColumn(metasheet.schemeConfig.tableData);
            metaSheetItem.columns = columns;
            metaSheetItem.columnTypeMap = columnTypeMap;
          }
          if(metasheet.metaTableName) {
            metaSheetItem.name = metasheet.metaTableName;
          }
          Vue.set(state.workspaces, metasheet.id, metaSheetItem);
          console.log('MUT.NB_UPDATE_METASHEET', JSON.parse(JSON.stringify(state.workspaces[metasheet.id])));
        }
      }
    },
    [MUT.NB_JOB_ONSUCCESS](state, { jobId, reqId, info }) {
      const job = state.jobs[jobId];
      if (!job) {
        return;
      }
      job.onSuccess({jobId, reqId, info});
    },
    [MUT.NB_JOB_ONERROR](state, { jobId, reqId, info }) {
      const job = state.jobs[jobId];
      if (!job) {
        return;
      }
      job.onError({jobId, reqId, info});
    },
    [MUT.NB_QUERY_START](state, { jobId, seqId, query }) {
      const job = state.jobs[jobId];
      if (!job) {
        return;
      }
      const subQuery = job.queries[seqId];
      if (!subQuery) {
        return;
      }
      subQuery.onRun(query);
    },
    [MUT.NB_QUERY_ONSUCCESS](state, { jobId, seqId, headers, rows, content, startTime, endTime, code, updatedCount }) {
      const job = state.jobs[jobId];
      if (!job) {
        return;
      }
      const subQuery = job.queries[seqId];
      if (!subQuery) {
        return;
      }
      const q = { headers, rows, content, endTime, updatedCount, startTime, query: code } as Query;
      subQuery.onSuccess(_.pickBy(q, _.identity));
    },
    [MUT.NB_QUERY_ONERROR](state, {jobId, seqId, code }) {
      const job = state.jobs[jobId];
      if (!job) {
        return;
      }
      const subQuery = job.queries[seqId];
      if (!subQuery) {
        return;
      }
      const q = {} as Query;
      if (code) {
        q.query = code;
      }
      return subQuery.onError(q);
    },
    [MUT.NB_QUERY_PROGRESS](state, {jobId, reqId, seqId, status, code}) {
      const job = state.jobs[jobId];
      if (!job) {
        return;
      }
      const subQuery = job.queries[seqId];
      if (!subQuery) {
        return;
      }
      const q = { status, query: code } as Query;
      subQuery.onProgress(_.pickBy(q, _.identity));
    },
    [MUT.NB_QUERY_CANCELED](state, {jobId, reqId, seqId, code }) {
      const job = state.jobs[jobId];
      if (!job) {
        return;
      }
      const subQuery = job.queries[seqId];
      if (!subQuery) {
        return;
      }
      const q = { query: code} as Query;
      subQuery.onCancel(_.pickBy(q, _.identity));
    },
  },
};

export default option;
