/* eslint-disable no-console */
import StompWebsocketClient from './stomp-websocket-client';
import { WSMessage, WSMessageMetadataExecutorKey, WSMessageMetadataKey, WSMessageOptions } from './ws-message';
import Vue from 'vue';
import { WorkspaceSrv } from '@/services/Workspace.service';
import { INotebook, NotebookStatus, JobStatus, MultiNotebook, CodeTypes, CodeType, IPreference } from '@/types/workspace';
import { OP } from './endpoints';
import { WSExceptionPacket, ZetaException, StackTrace } from '@/types/exception';
import Util from '@/services/Util.service';
import _ from 'lodash';
import uuid from 'uuid';
import { WSPacket } from '@/types/WSPackets';
import { NotebookJob, Query, QueryContent } from '@/types/workspace/notebook-job';
import { Store } from 'vuex';
import { initNotebook, initRepository, syncNotebook } from '@/services/workspace-init.service';
import { isHermes } from '@/services/connection.service';
const pretty = require('js-object-pretty-print').pretty;

function ZetaExceptionFacotry (m: any) {
  const notebookId = m.data && m.data ? m.data.notebookId || m.data.noteId || '' : '';
  const e = new ZetaException({
    code: 'WS_EXCEPTION',
    errorDetail: {
      message: m.op,
      context: {
        message: '',
        stackTrace: [] as StackTrace[],
        context: '',
      },
    },
    responseHeaders: {},
  } as WSExceptionPacket, { path: 'notebook', workspaceId: notebookId });
  return e;
}
function hasNotebook (store: Store<any>, notebookId: string) {
  const nb = store.getters.nbByNId(notebookId) as INotebook;
  return !_.isEmpty(nb);
}
function isQueryExecutable (store: Store<any>, jobId: string, seqId: string | number) {
  const query = store.getters.getQuery(jobId, parseInt(seqId + ''));
  if (!query) {
    return true;
  }
  return (query as Query).executable();
}
function isJobExecutable (store: Store<any>, jobId: string) {
  const job = store.getters.getJob(jobId);
  if (!job) {
    return true;
  }
  return (job as NotebookJob).executable();
}
export class ZetaWebSocketClient extends StompWebsocketClient {
  context: Vue;
  workspaceActive = false;
  responseHeaders = {};
  get store () {
    return this.context.$store;
  }
  get router () {
    return this.context.$router;
  }
  constructor (context?: Vue) {
    super();
    if (context) {
      this.context = context;
    }
  }
  private validateNotebookStatus (notebookId: string, newStatus: NotebookStatus) {
    if (this.store) {
      const nb = this.store.getters.nbByNId(notebookId) as INotebook;
      if (!nb) {
        console.warn(`cannot find note:${notebookId} in workspace store`);
        return false;
      }
      return WorkspaceSrv.validateNotebookStatus(nb.status, newStatus);
    }
    return false;
  }
  onErrorMessage (ex: ZetaException) {
    this.store.dispatch('addException', { exception: ex });
  }
  onSubscribed () {
    // syncNotebook(this.context);
  }
  dispatchMessage (messageBody: any) {
    const opOption: WSMessageOptions = Reflect.getMetadata(WSMessageMetadataKey, this, messageBody.op);
    const executor: Function = Reflect.getMetadata(WSMessageMetadataExecutorKey, this, messageBody.op);
    if (!executor) {
      return;
    }
    if (!this.workspaceActive && opOption.topic === 'workspace') {
      // ignore workspace message
      return;
    }
    const result: undefined | ZetaException = executor.call(this, messageBody.data);
    if (opOption.isError && result) {
      this.onErrorMessage(result);
    }

    if (opOption.alias && opOption.mappingPath) {
      const asyncRequest = this.findAsyncRequest(messageBody, opOption);
      if (asyncRequest) {
        if (opOption.isError) {
          asyncRequest.rejecter(result);
        } else {
          asyncRequest.resolver(result || messageBody.data);
        }
        /** @unregister_async_request */
        _.remove(this.asyncRequestList, req => req.id === asyncRequest.id);
      }
    }
  }
  /* Handlers for packets defined in OP */
  @WSMessage({ topic: 'workspace'})
  [OP.RECOVER_TRIED] (m: any) {
    console.info('OP.RECOVER_TRIED', m);
    const noteId = m.noteId;
    const tabs = m.tabState.tab;
    const nbStatus = m.tabState.connection;
    //! set notebook status
    // set IDLE before inject Job if connected
    if (nbStatus === 'Connected') {
      this.store.dispatch('setNotebookStatus', { nid: noteId, status: NotebookStatus.CONNECTING });
      this.store.dispatch('setNotebookStatus', { nid: noteId, status: NotebookStatus.IDLE });
    }
    let error = '';
    let jobId = '';
    let reqId = '';
    _.forEach(tabs, (tab, seqId) => {
      const executeCodeResult = tab.executeCodeResult;
      const endDt = executeCodeResult.endDt;
      const startDt = executeCodeResult.startDt;
      const status = executeCodeResult.code.status;
      const statementId = executeCodeResult.code.statementId;
      const code = executeCodeResult.code;
      jobId = executeCodeResult.jobId;
      reqId = executeCodeResult.reqId;
      const result: any = executeCodeResult.result;

      if (status === 'DONE') {
        // same like OP.NB_CODE_STATEMENT_SUCCESS
        this[OP.NB_CODE_STATEMENT_SUCCESS]({ startDt, endDt, jobId, reqId, notebookId: noteId, code, result }, true, statementId);
      } else if (status === 'RUNNING') {
        // to OP.NB_CODE_STATEMENT_START
        this[OP.NB_CODE_STATEMENT_START]({ noteId, jobId, seq: parseInt(seqId), startDt: 0, zetaStatementKey: statementId }, true);
      } else if (status === 'FAIL') {
        // init query
        this.store.dispatch('handleJobReady', { nid: noteId, jobId, reqId });
        this.store.dispatch('initQuery', { jobId, seqId: parseInt(seqId), code: code.code, statementId});
        // parse error
        error = 'Unknown error';
        try {
          const resultObj = JSON.parse(result);
          const content = _.find(resultObj.result, r => r.type === 'TEXT');
          if (content) {
            error = content.content;
          }
        } catch {

        }

      } else if (status === 'READY') {
        // init query
        this.store.dispatch('handleJobReady', { nid: noteId, jobId, reqId });
        this.store.dispatch('initQuery', {jobId, seqId: parseInt(seqId), code: code.code});
      }

    });
    if (!jobId || !reqId) {
      return;
    }
    if (error) {
      this.store.dispatch('haltJob', { nid: noteId, jobId, reqId, seqId: 0, info: pretty([error]), init: true });
    } else {
      this.store.dispatch('jobOnSuccess', { nid: noteId, jobId, reqId, init: true });
    }
    //! set notebook status
    // set OFFLINE after inject Job if note is offline
    if (nbStatus === 'Disconnected') {
      this.store.dispatch('setNotebookStatus', { nid: noteId, status: NotebookStatus.DISCONNECTING });
      this.store.dispatch('setNotebookStatus', { nid: noteId, status: NotebookStatus.OFFLINE });
    } else if (nbStatus === 'Connecting') {
      this.store.dispatch('setNotebookStatus', { nid: noteId, status: NotebookStatus.DISCONNECTING });
      this.store.dispatch('setNotebookStatus', { nid: noteId, status: NotebookStatus.OFFLINE });
      this.store.dispatch('setNotebookStatus', { nid: noteId, status: NotebookStatus.CONNECTING });
    }
  }
  @WSMessage()
  [OP.SESSION_ACTIVE] (m: string) {
    console.info('WS', 'SESSION_ACTIVE', m);
    if (this.sessionId != m) {
      this.workspaceActive = false;
      Util.unregisterBeforeunload();
      this.store.dispatch('clearWorkspaceStore');
      if (this.router.currentRoute.path === '/notebook') {
        this.router.push({ name: 'HomePage', query: { workspaceAlert: 'true'}});
      }

    } else {
      // init Workspace
      if (this.store.getters.isEmptyWorkspace() || !this.workspaceActive) {
        this.store.dispatch('setWorkspaceLoading', true);
        initRepository(this.context)
          .then(files => initNotebook(this.context, files))
          .then(() => {
            this.store.dispatch('setWorkspaceLoading', false);
            syncNotebook(this.context);
          });
      }

      this.workspaceActive = true;

    }
  }
  @WSMessage({ topic: 'workspace', isError: true })
  [OP.INTERNAL_ERROR] (m: WSExceptionPacket) {
    console.warn('WS: ', 'INTERNAL_ERROR', m);
    // TODO @exception handler
    /* Update store */
    this.store.dispatch('haltAll');
    return new ZetaException(m, { path: 'notebook' }, this.responseHeaders);
  }
  @WSMessage({ topic: 'workspace', isError: true })
  [OP.NB_CODE_EXECUTE_ERROR] (m: WSExceptionPacket) {
    console.warn('WS: ', 'NB_CODE_EXECUTE_ERROR', m);
    const ex = new ZetaException(m, { path: 'notebook' }, this.responseHeaders);
    /**
     * // comment by @tianrsun @2019-04-01
     * // enhance sub notebook
     * // codes below won't be excuted when running subnotebook
     * // the error would be hanlded in function `sendAsync`
     * comment by tianrsun @2019-06-12
     *
     */

    try {
      const { code, errors, jobId, notebookId, reqId } = m.errorDetail.context;
      /* Update store */
      this.store.dispatch('haltJob', { nid: notebookId, jobId, reqId, seqId: code.seq, status: JobStatus.ERROR, info: pretty(errors) });
      // this.store.dispatch('setJobStatus', { jobId, reqId, status: JobStatus.ERROR, info: pretty(errors) });
      ex.props({ workspaceId: notebookId }).resolve();
    } catch (e) {
      ex.resolve(false);
    }
    return ex;
  }
  @WSMessage({ topic: 'workspace', isError: true })
  [OP.NB_CODE_SESSION_EXPIRED] (m: WSExceptionPacket) {
    console.warn('WS: ', 'NB_CODE_SESSION_EXPIRED', m);
    const notebookId = m.errorDetail.context.notebookId;
    const body = m.errorDetail.context;
    const notebook: INotebook = this.store.getters.nbByNId(body.notebookId);
    const e = new ZetaException(m).props({ path: 'notebook', workspaceId: notebookId }).resHeaders(this.responseHeaders);
    // enhance subNotebook session expired
    if (notebook.nbType === 'sub_nb') {
      const collectionId = notebook.collectionId;
      const parentNotebook: MultiNotebook = this.store.getters.nbByNId(collectionId);
      if (body.jobId) {
        this.store.dispatch('haltSubNotebookJobsByNId', { nid: parentNotebook.notebookId });
        this.store.dispatch('setJobStatus', { jobId: body.jobId, reqId: body.reqId, status: JobStatus.ERROR, info: pretty(body.errors) });
      }
      // halt jobs for multi notebook
      this.store.dispatch('setSubNotebookStatus', { nid: parentNotebook.notebookId, status: NotebookStatus.DISCONNECTING });
      this.store.dispatch('setSubNotebookStatus', { nid: parentNotebook.notebookId, status: NotebookStatus.OFFLINE });
      e.message = 'Connect closed because of connection expired.';

    } else {
    /* Update store */
      if (body.jobId) {
        this.store.dispatch('haltJob', { nid: body.notebookId, jobId: body.jobId, reqId: body.reqId, seqId: body.code.seq, status: JobStatus.ERROR });
        // this.store.dispatch('setJobStatus', { jobId: body.jobId, reqId: body.reqId, status: JobStatus.ERROR, info: pretty(body.errors) });
      }
      this.store.dispatch('setNotebookStatus', { nid: body.notebookId, status: NotebookStatus.DISCONNECTING });
      this.store.dispatch('setNotebookStatus', { nid: body.notebookId, status: NotebookStatus.OFFLINE });
      e.message = 'Connect closed because of connection expired.';
    }
    return e;
  }
  @WSMessage({ topic: 'workspace', isError: true })
  [OP.NB_CODE_INVALID_NOTEBOOK_STATUS] (m: WSPacket.JobErrorRes) {
    console.warn('WS: ', 'NB_CODE_INVALID_NOTEBOOK_STATUS', m);
    /* Update store */
    this.store.dispatch('setNotebookStatus', { nid: m.notebookId, status: NotebookStatus.IDLE });
    // this.store.dispatch('setJobStatus', { jobId: m.jobId, reqId: m.jobId, status: JobStatus.ERROR });
    this.store.dispatch('jobOnError', { nid: m.notebookId, jobId: m.jobId, reqId: m.jobId});
    /* When this error is trigger, there should be not queries in store.
     * Because SQLSplitted will not be generated.
     * So no need to set query status.
     */

    return ZetaExceptionFacotry({ data: m }).resolve();
  }
  @WSMessage({ topic: 'workspace' })
  [OP.NB_CODE_JOB_DONE] (m: WSPacket.JobDoneRes) {
    console.info('WS: ', 'NB_CODE_JOB_DONE', m);
    /** check notebook status */
    if (!this.validateNotebookStatus(m.notebookId, NotebookStatus.IDLE)) {
      console.warn('invalid notebook status', 'set: ' + NotebookStatus.IDLE);
      return;
    }
    /**
     * // comment by @tianrsun @2019-04-01
     * // enhance sub notebook
     * // codes below won't be excuted when running subnotebook
     * // the job done message would be hanlded in function `sendAsync`
     */

    const nb: INotebook | undefined = this.store.getters.nbByNId(m.notebookId);
    if (!nb) {
      console.warn('cannot find notebook id:' + m.notebookId);
      return;
    }

    this.store.dispatch('setNotebookStatus', { nid: m.notebookId, status: NotebookStatus.IDLE });
    // this.store.dispatch('setJobStatus', { jobId: m.jobId, reqId: m.reqId, status: JobStatus.SUCCESS });
    this.store.dispatch('jobOnSuccess', { nid: m.notebookId, jobId: m.jobId, reqId: m.reqId});
  }
  @WSMessage({ topic: 'workspace' })
  [OP.NB_CODE_DUMP_DONE] (m: WSPacket.JobDoneRes) {
    console.info('WS: ', 'NB_CODE_DUMP_DONE', m);
    /** check notebook status */
    if (!this.validateNotebookStatus(m.notebookId, NotebookStatus.IDLE)) {
      console.warn('invalid notebook status', 'set: ' + NotebookStatus.IDLE);
      return;
    }
    /* Update store */
    this.store.dispatch('setNotebookStatus', { nid: m.notebookId, status: NotebookStatus.IDLE });
    // this.store.dispatch('setJobStatus', { jobId: m.jobId, reqId: m.reqId, status: JobStatus.SUCCESS });
    this.store.dispatch('jobOnSuccess', { nid: m.notebookId, jobId: m.jobId, reqId: m.reqId});
    const notebook = this.store.getters.nbByNId(m.notebookId);
    let interpreter = CodeTypes[notebook.connection.codeType].interpreter;
    if (isHermes(notebook.connection.clusterId)) {
      interpreter = 'carmel';
    }
    const workspaceId = m.notebookId;
    WorkspaceSrv.notebookRemoteService.props({
      path: 'notebook',
      workspaceId,
    });
    WorkspaceSrv.dumpResult(m.notebookId, interpreter, m.jobId);
  }
  @WSMessage({ topic: 'workspace' })
  [OP.NB_CODE_JOB_READY] (m: WSPacket.JobReadyRes) {
    console.info('WS: ', 'NB_CODE_JOB_READY', m);
    if (!hasNotebook(this.store, m.notebookId)) {
      return;
    }
    /** check notebook status */
    if (!this.validateNotebookStatus(m.notebookId, NotebookStatus.RUNNING)) {
      console.warn('invalid notebook status', 'set: ' + NotebookStatus.RUNNING);
      return;
    }
    this.store.dispatch('setNotebookStatus', { nid: m.notebookId, status: NotebookStatus.RUNNING });
    /* Update store */
    this.store.dispatch('handleJobReady', { nid: m.notebookId, jobId: m.jobId, reqId: m.reqId, status: NotebookStatus.RUNNING });
  }
  @WSMessage({ topic: 'workspace' })
  [OP.NB_CODE_PREPROCESSED] (m: WSPacket.SplittedRes) {
    console.info('WS: ', 'NB_CODE_PREPROCESSED', m);
    if (!hasNotebook(this.store, m.notebookId)) {
      return;
    }
    this.store.dispatch('setNotebookStatus', { nid: m.notebookId, status: NotebookStatus.RUNNING });

    // verify job
    if (!isJobExecutable(this.store, m.jobId)) {
      console.warn('cannot find executable job by jobId: ' + m.jobId);
      return;
    }
    /* Update store */
    this.store.dispatch('addQueries', { nid: m.notebookId, jobId: m.jobId, reqId: m.reqId, codes: m.codes });
  }
  @WSMessage({ topic: 'workspace' })
  [OP.NB_CODE_STATEMENT_START] (m: WSPacket.QueryStart, init = false) {
    console.info('WS: ', 'NB_CODE_STATEMENT_START', m);
    if (!hasNotebook(this.store, m.noteId)) {
      return;
    }
    // verify query
    if (!isQueryExecutable(this.store, m.jobId, m.seq) && !init){
      console.warn('cannot find executable query by jobId: ' + m.jobId);
      return;
    }
    this.store.dispatch('setNotebookStatus', { nid: m.noteId, status: NotebookStatus.RUNNING });
    /* Update store */
    // query onRunning here
    this.store.dispatch('queryStart', { nid: m.noteId, jobId: m.jobId, seqId: m.seq, query: { startTime: m.startDt, statementId: m.zetaStatementKey }});
    // this.store.dispatch('updateQuery', { jobId: m.jobId, seqId: m.seq, query: { startTime: m.startDt, statementId: m.zetaStatementKey } });
    // update repository lastRunTime
    this.store.dispatch('upadateFileLastRun', { notebookId: m.noteId, startTime: m.startDt });
  }
  @WSMessage({ topic: 'workspace' })
  [OP.NB_CODE_STATEMENT_PROGRESS] (m: WSPacket.QueryProgressRes) {
    console.info('WS: ', 'NB_CODE_STATEMENT_PROGRESS', m);
    if (!hasNotebook(this.store, m.notebookId)) {
      return;
    }
    // verify query
    if (!isQueryExecutable(this.store, m.jobId, m.code.seq)){
      console.warn('cannot find executable query by jobId: ' + m.jobId);
      return;
    }
    this.store.dispatch('setNotebookStatus', { nid: m.notebookId, status: NotebookStatus.RUNNING });
    /* Update store */
    // const newQuery = <IQuery>{ status: m.status };
    const code = m.code && m.code.code ? m.code.code : '';
    const statementId = m.code.statementId;
    this.store.dispatch('updateNotebook', { notebookId: m.notebookId, monitorUrl: m.sparkJobUrl });
    this.store.dispatch('queryOnProgress', { nid: m.notebookId, jobId: m.jobId, reqId: m.reqId, seqId: m.code.seq, status: m.status, code, statementId });
  }
  @WSMessage({ topic: 'workspace' })
  [OP.NB_CODE_STATEMENT_SUCCESS] (m: WSPacket.QueryResultRes, init = false, statementId?: string) {
    if (!init) {
      console.info('WS: ', 'NB_CODE_STATEMENT_SUCCESS', m);
    }
    if (!hasNotebook(this.store, m.notebookId)) {
      return;
    }
    if (!isQueryExecutable(this.store, m.jobId, m.code.seq) && !init){
      console.warn('cannot find executable query by jobId: ' + m.jobId);
      return;
    }
    /* Update store */
    const result = JSON.parse(m.result) as WSPacket.ResultField;
    const resultList = result.result;
    const code = m.code && m.code.code ? m.code.code : '';
    if (!resultList || resultList.length === 0) {
      console.warn('ResultList is empty', result);
      return;
    }
    else if (resultList.length > 1) {
      console.warn('ResultList.length > 1', result);
    }

    let resultIndex = -1;
    for (let i = 0; i < resultList.length; i++) {
      if (resultList[i].type === 'TABLE') {
        resultIndex = i;
        break;
      }
    }

    if (resultIndex !== -1) {
      if (resultIndex >= resultList.length) {
        console.warn('Result Type is not table but OP is NB_CODE_JOB_DONE', result);
        return;
      }

      const resultTable = resultList[resultIndex] as WSPacket.ResultTable;
      if (!resultTable) {
        console.warn('ResultTable not exist', result);
        return;
      }

      const headers = resultTable.schema;
      /* transform resultTable.rows from (key, value) style
         * to array style
         */
      // let indices: { [K: string]: number } = headers.reduce((pre, col, index) => ({ ...pre, [col]: index }), {})
      const rows = resultTable.rows.map(row => {
        const cols = Object.keys(row);
        const arr = cols.map(col => row[col]);
        return arr;
      });

      const updatedCount = resultTable.updatedCount;
      // query onsuccess
      this.store.dispatch('queryOnSuccess',
        { nid: m.notebookId, jobId: m.jobId, reqId: m.reqId, seqId: m.code.seq, headers: headers, rows: rows, startTime: m.startDt, endTime: m.endDt, code, updatedCount, statementId, init });
      // this.store.dispatch('updateQuery', { jobId: m.jobId, seqId: m.code.seq, query: { endTime: m.endDt } });
      // this.store.dispatch('addQueryResult', { nid: m.notebookId, jobId: m.jobId, reqId: m.reqId, seqId: m.code.seq, headers: headers, rows: rows });
    } else {
      // query onsuccess
      // this.store.dispatch('updateQuery', { jobId: m.jobId, seqId: m.code.seq, query: { endTime: m.endDt } });
      const content: Array<QueryContent> = [];
      for (const resultContent of resultList) {
        content.push((resultContent as WSPacket.ResultContent) as QueryContent);
      }
      // this.store.dispatch('addQueryResult', { nid: m.notebookId, jobId: m.jobId, reqId: m.reqId, seqId: m.code.seq, content });
      this.store.dispatch('queryOnSuccess',
        { nid: m.notebookId, jobId: m.jobId, reqId: m.reqId, seqId: m.code.seq, content, endTime: m.endDt, code, statementId, init });
    }
  }
  @WSMessage()
  [OP.GREETING] (m: any) {
    console.info('WS: ', 'CONNECTIONGREETING_SUCCESS', m);
    this.responseHeaders = { 'zds-server-req-id': m };
  }
  @WSMessage({ alias: 'CONNECTION_RESP', mappingPath: { notebookId: 'noteId' }, topic: 'workspace' })
  [OP.CONNECTION_SUCCESS] (m: any) {
    console.info('WS: ', 'CONNECTION_SUCCESS', m);
    const notebookId = m.noteId;
    /** check notebook status */
    if (!this.validateNotebookStatus(notebookId, NotebookStatus.IDLE)) {
      console.warn('invalid notebook status', 'set: ' + NotebookStatus.IDLE);
      return;
    }
    const notebook: INotebook = this.store.getters.nbByNId(notebookId);
    this.store.dispatch('setNotebookStatus', { nid: notebookId, status: NotebookStatus.IDLE });
    Util.getApp().$message.success(`\`${notebook.name || ''}\` Connect succeed`);
  }
  @WSMessage({ isError: true, alias: 'CONNECTION_RESP', mappingPath: { notebookId: 'errorDetail.context.notebookId' }, topic: 'workspace' })
  [OP.CONNECTION_ABORT] (m: WSExceptionPacket) {
  // comments by tianrsun @2019-05-20
  // ```CONNECTION_ABORT``` should not show any message popup
    console.info('WS: ', 'CONNECTION_ABORT', m);
    const notebookId = m.errorDetail.context.notebookId;

    /** check notebook status */
    if (!this.validateNotebookStatus(notebookId, NotebookStatus.OFFLINE)) {
      console.warn('invalid notebook status', 'set: ' + NotebookStatus.OFFLINE);
      return;
    }
    if (notebookId) {
      this.store.dispatch('setNotebookStatus', { nid: notebookId, status: NotebookStatus.OFFLINE });
      return new ZetaException(m, { path: 'notebook', workspaceId: notebookId }, this.responseHeaders).resolve();
    }
  }
  @WSMessage({ topic: 'workspace' })
  [OP.CONNECTION_PROGRESS] (m: WSPacket.ConnectProgressRes) {
    console.info('WS: ', 'CONNECTION_PROGRESS', m);

    this.store.dispatch('setConnectProgress', { nid: m.noteId, progress: m.progress });
  }
  @WSMessage({ isError: true, alias: 'CONNECTION_RESP', mappingPath: { notebookId: 'errorDetail.context.notebookId' }, topic: 'workspace' })
  [OP.CONNECTION_ERROR] (m: WSExceptionPacket) {
    console.warn('WS: ', 'CONNECTION_ERROR', m);
    // const msg = 'Unknown Error';
    const notebookId = m.errorDetail.context.notebookId;
    /** check notebook status */
    if (!this.validateNotebookStatus(notebookId, NotebookStatus.OFFLINE)) {
      console.warn('invalid notebook status', 'set: ' + NotebookStatus.OFFLINE);
      return;
    }
    if (!notebookId) return;
    const notebook: INotebook = this.store.getters.nbByNId(notebookId);
    let codeType: CodeType;
    // let notebookName: string;
    if (!notebook) {
      console.warn('cannot find notebook');
      return;
    } else {
      codeType = notebook.connection!.codeType;
      // notebookName = notebook.name;
    }
    // const statusCode = null;
    const ex = new ZetaException(m, { path: 'notebook', workspaceId: notebookId }, this.responseHeaders);
    switch (codeType) {
      case CodeType.SQL:
      // try {
      //     statusCode = m.errorDetail.cause.zetaStatus.statusCode

        // }
        // catch(e){
        //     console.error("parse msg error", e)
        // }
        // comments by tianrsun @2019-05-16
        // // popup message for current
        // // TODO should dispach connect err in Vue context
        // comments by tianrsun @2019-06-04
        // // popup by zeta exception
        // comments by tianrsun @2019-06-21
        // handle error in adaptee
        // if(statusCode && statusCode == '9003'){
        //     let url = `https://wiki.vip.corp.ebay.com/x/4J6sJ`;
        //     let template = `\`${notebookName}\` Connect failed. ` +
        //     `Windows PET password expired. Click <a href=${url}>here</a> to reset password`
        //     ex.template(template);
        // }else if(statusCode && statusCode == '9002') {
        //     let url = `https://wiki.vip.corp.ebay.com/x/4J6sJ`;
        //     ex.template(`\`${notebookName}\` Connect failed. ` +
        //     `Windows PET account locked. Click <a href=${url}>here</a> to unlock.`)
        // }else if(statusCode && statusCode == '9001'){
        //     let url = `${location.protocol}//${location.host}/${Util.getPath()}#/settings`;
        // 	ex.template(`\`${notebookName}\` Connect failed. ` +
        //     `Windows PET password incorrect, please <a href=${url}>change your password</a>`)
        // } else {
        //     ex.template(`\`${notebookName}\` Connect failed.`)
        //     ex.causeMessage = msg
        // }
        break;
      case CodeType.TERADATA:
        try {
          if (/UserId, Password or Account is invalid/.test(m.errorDetail.message)) {
            const url = `${location.protocol}//${location.host}/${Util.getPath()}#/settings`;
            // ex.template(`\`${notebookName}\` Connect failed. ` +
            ex.template('Connect failed. ' +
                        `Password or Account is invalid, Please reset your <a href=${url}> TD pass in Zeta</a>`);
          } else if (/Password is not set/.test(m.errorDetail.message)) {
            const url = `${location.protocol}//${location.host}/${Util.getPath()}#/settings`;
            // ex.template(`\`${notebookName}\` Connect failed. ` +
            ex.template('Connect failed. ' +
                        `Password is not set, Please set your <a href=${url}> TD pass in Zeta</a>`);
          } else if (_.has(ex, 'context.zetaStatus.statusCode') && ex.context.zetaStatus.statusCode === '9100') {
            let message = ex.message;
            message = message.replace(/notebook.*(\[.*?\])/, '');
            // ex.template(`\`${notebookName}\` Connect failed.<br>` + message)
            ex.template('Connect failed.<br>' + message);
          } else {
          // ex.template(`\`${notebookName}\` Connect failed.`)
            ex.template('Connect failed.');
          }
        }
        catch (e) {
          console.error('parse msg error', e);
          // e.message = ex.template(`\`${notebookName}\` Connect failed.`)
          e.message = ex.template('Connect failed.');
        }
        break;
      default:
      // ex.template(`\`${notebookName}\` Connect failed.`)
        ex.template('Connect failed.');
        break;
    }
    this.store.dispatch('setNotebookStatus', { nid: notebookId, status: NotebookStatus.OFFLINE });
    return ex;
  }
  @WSMessage({ alias: 'DISCONNECTION_RESP', mappingPath: { notebookId: 'noteId' }, topic: 'workspace' })
  [OP.DISCONNECTION_SUCCESS] (m: any) {
    console.info('WS: ', 'DISCONNECTION_SUCCESS', m);
    /** check notebook status */
    if (!this.validateNotebookStatus(m.noteId, NotebookStatus.OFFLINE)) {
      console.warn('invalid notebook status', 'set: ' + NotebookStatus.OFFLINE);
      return;
    }
    this.store.dispatch('setNotebookStatus', {
      nid: m.noteId,
      status: NotebookStatus.OFFLINE,
    });
  }
  @WSMessage({ isError: true, alias: 'DISCONNECTION_RESP', mappingPath: { notebookId: 'errorDetail.context.notebookId' }, topic: 'workspace' })
  [OP.DISCONNECTION_ERROR] (m: WSExceptionPacket) {
    console.info('WS: ', 'DISCONNECTION_ERROR', m);
    const notebookId = m.errorDetail.context.notebookId;
    /** check notebook status */
    if (!this.validateNotebookStatus(notebookId, NotebookStatus.OFFLINE)) {
      console.warn('invalid notebook status', 'set: ' + NotebookStatus.OFFLINE);
      return;
    }

    this.store.dispatch('setNotebookStatus', {
      nid: notebookId,
      status: NotebookStatus.OFFLINE,
    });
    // const notebook: INotebook = this.store.getters.nbByNId(notebookId);
    const e = new ZetaException(m, { path: 'notebook', workspaceId: notebookId }, this.responseHeaders);
    e.message = 'Disconnect failed';
    return e;
  }
  @WSMessage({ alias: 'JOB_CANCEL_RESP', mappingPath: { notebookId: 'notebookId', jobId: 'jobId' }, topic: 'workspace' })
  [OP.CANCEL_SUCCESS] (m: any) {
    console.info('WS: ', 'CANCEL_SUCCESS', m);
    if (!this.validateNotebookStatus(m.notebookId, NotebookStatus.IDLE)) {
      console.warn('invalid notebook status', 'set: ' + NotebookStatus.IDLE);
      return;
    }
    // verify query
    if (!isJobExecutable(this.store, m.jobId)){
      console.warn('cannot find executable query by jobId: ' + m.jobId);
      return;
    }
    this.store.dispatch('setNotebookStatus', { nid: m.notebookId, status: NotebookStatus.IDLE });
    this.store.dispatch('haltJobOnCancel', {
      nid: m.notebookId,
      jobId: m.jobId,
    });
  }
  @WSMessage({ isError: true, alias: 'JOB_CANCEL_RESP', mappingPath: { notebookId: 'notebookId', jobId: 'jobId' }, topic: 'workspace'})
  [OP.CANCEL_ERROR] (m: any) {
    console.info('WS: ', 'CANCEL_ERROR', m);
    //! const notebookId = m.errorDetail.cause.error.notebookId;

    if (!this.validateNotebookStatus(m.notebookId, NotebookStatus.IDLE)) {
      console.warn('invalid notebook status', 'set: ' + NotebookStatus.IDLE);
      return;
    }
    this.store.dispatch('setNotebookStatus', { nid: m.notebookId, status: NotebookStatus.IDLE });
    return ZetaExceptionFacotry({ data: m });
  }
  @WSMessage()
  [OP.NB_VAR_REFRESH] (m: any) {
    console.info('WS: ', 'NB_VAR_REFRESH', m);
    const { notebookId, vars } = m;
    this.store.dispatch('updateNotebookVariables', {
      notebookId,
      variables: vars,
    });
  }

  public notebookConnect (notebookId: string, clusterId: number, clusterName: string, proxyUser: string, interpreter: string, preference: IPreference) {
    return this.sendAsync(OP.NB_CONNECT, {
      userName: Util.getNt(),
      noteId: notebookId,
      interpreter,
      prop: {
        clusterId,
        proxyUser,
        preference: JSON.stringify(preference),
      },
    }, {
      opAlias: 'CONNECTION_RESP',
      mapping: {
        notebookId,
      },
    });
  }
  public multiNotebookConnect (notebookId: string, clusterId: number, clusterName: string, proxyUser: string, interpreter: string, preference: IPreference) {
    return this.sendAsync(OP.NB_CONNECT, {
      userName: Util.getNt(),
      noteId: notebookId,
      interpreter,
      prop: {
        'zds.livy.code.type': 'sql',
        clusterId,
        proxyUser,
        preference: JSON.stringify(preference),
      },
      isCollectionAware: true,
    }, {
      opAlias: 'CONNECTION_RESP',
      mapping: {
        notebookId,
      },
    });
  }

  public jdbcNotebookConnect (notebookId: string, host: string, interpreter: string, jdbcType: string, preference: IPreference, user?: string, password?: string, database?: string, ssl?: boolean, port?: number, principal?: string, props: any = {}) {
    return this.sendAsync(OP.NB_CONNECT, {
      userName: Util.getNt(),
      noteId: notebookId,
      interpreter,
      prop: {
        host,
        user,
        password,
        database,
        ssl,
        'jdbc.props.hive.server2.remote.principal': principal,
        port,
        // eslint-disable-next-line @typescript-eslint/camelcase
        jdbc_type: jdbcType,
        preference: JSON.stringify(preference),
        ...props,
      },
    }, {
      opAlias: 'CONNECTION_RESP',
      mapping: {
        notebookId,
      },
    });
  }

  public notebookDisconnect (notebookId: string): Promise<any> {
    return this.sendAsync(OP.NB_DISCONNECT, {
      userName: Util.getNt(),
      noteId: notebookId,
    }, {
      opAlias: 'DISCONNECTION_RESP',
      mapping: {
        notebookId,
      },
    }) as Promise<any>;
  }

  /* Request controller defined in OP */
  public jobSubmit (notebookId: string, codes: string, interpreter?: string | null, prop?: any) {

    /* Update store */
    this.store.dispatch('setNotebookStatus', { nid: notebookId, status: NotebookStatus.SUBMITTING });
    /* Inform backend */
    this.send(OP.NB_CODE_JOB_SUBMIT, {
      notebookId,
      reqId: uuid(),
      codes,
      interpreter: interpreter ? interpreter : CodeTypes[this.store.getters.currentCodeType].interpreter,
      prop,
    });
  }
  public jobSubmitInMulti (subNotebookId: string, codes: string, interpreter: string, codeType: 'sql' | 'pyspark' | 'sparkr' = 'sql') {

    /**
     *
     *  Update store */
    this.store.dispatch('setNotebookStatus', { nid: subNotebookId, status: NotebookStatus.SUBMITTING });
    /* Inform backend */
    return this.send(OP.NB_CODE_JOB_SUBMIT, {
      notebookId: subNotebookId,
      reqId: uuid(),
      codes,
      interpreter: interpreter,
      isCollectionAware: true,
      prop: {
        'zds.livy.code.type': codeType,
      },
    });
  }

  public jobDump (notebookId: string, codes: string, interpreter?: string | null, prop?: any) {

    /* Update store */
    this.store.dispatch('setNotebookStatus', { nid: notebookId, status: NotebookStatus.SUBMITTING });

    /* Inform backend */
    this.send(OP.NB_CODE_JOB_DUMP, {
      notebookId,
      reqId: uuid(),
      codes,
      interpreter: interpreter ? interpreter : CodeTypes[this.store.getters.currentCodeType].interpreter,
      prop,
    });
  }

  public jobCancel (notebookId: string, jobId: string): Promise<any> {
    return this.sendAsync(OP.NB_CODE_JOB_CANCEL, {
      userName: Util.getNt(),
      notebookId,
      jobId,
    }, {
      opAlias: 'JOB_CANCEL_RESP',
      mapping: {
        notebookId,
        jobId,
      },
    }) as Promise<any>;
  }
  public syncNotebookResult (noteId: string) {
    this.send(OP.NB_RECOVER, {
      noteId,
    });
  }
  activeThisSession () {
    this.sendWithQueue(OP.ACTIVE, {});
  }
}

export const wsclient = new ZetaWebSocketClient();
