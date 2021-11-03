import { createLocalVue } from '@vue/test-utils';
import workspace from '@/stores/modules/WorkspaceStore';
import { expect } from 'chai';
import Vue, { VueConstructor } from 'vue';
import Vuex, { Store } from 'vuex';
import { Notebook, NotebookStatus } from '@/types/workspace';
import _ from 'lodash';
import { getNotebook, getJob, getQuery } from './util';
import { JobStateEnum, QueryStateEnum } from '@/types/workspace/notebook-job';

describe('notebook execute code: multi', () => {
  const localVue: VueConstructor<Vue> = createLocalVue();
  localVue.use(Vuex);
  const store = new Vuex.Store({ modules: {workspace}});
  before(() => {
    /**
         * clear store and add new notebook
         */
    store.dispatch('closeNotebookById', 'id');
    const nb = new Notebook('id', 'testNotebook');

    nb.status = NotebookStatus.OFFLINE;
    nb.jobs = [];
    store.dispatch('addActiveWorkSpace', nb);
    store.dispatch('setNotebookStatus', { nid: nb.notebookId, status: NotebookStatus.CONNECTING});
    store.dispatch('setNotebookStatus', { nid: nb.notebookId, status: NotebookStatus.IDLE});
  });
  it('message in wrong order', () => {
    const jobId = 'JOBID';
    const reqId = 'REQID';
    const stateId = 'STATEID';
    const startTime = 0;
    const endTime = 1000 * 5;
    const codes = [
      { code: 'show tables;', seq: 0},
      { code: 'select 1;', seq: 1},
    ];
    debugger;
    store.dispatch('setNotebookStatus', { nid: 'id', status: NotebookStatus.SUBMITTING});
    store.dispatch('setNotebookStatus', { nid: 'id', status: NotebookStatus.RUNNING});
    expect(getNotebook(store, 'id').status).to.be.equal(NotebookStatus.RUNNING);

    // OP:NB_CODE_JOB_READY
    store.dispatch('handleJobReady', { nid: 'id', jobId, reqId });

    // OP:NB_CODE_PREPROCESSED
    store.dispatch('addQueries', { nid: 'id', jobId, reqId, codes });
    const nb = getNotebook(store, 'id');
    expect(getJob(store, nb, jobId).status.toString()).to.be.eq(JobStateEnum.RUNNING);
    expect(getQuery(store, nb, jobId, 0)).is.exist;
    expect(getQuery(store, nb, jobId, 1)).is.exist;
    expect(getQuery(store, nb, jobId, 0)!.status.status.valueOf()).to.be.eq(QueryStateEnum.WAITING);
    expect(getQuery(store, nb, jobId, 1)!.status.status.valueOf()).to.be.eq(QueryStateEnum.WAITING);

    // OP: NB_CODE_STATEMENT_START
    store.dispatch('queryStart', { nid: 'id', jobId, seqId: 0, query: { startTime, statementId: 'm.zetaStatementKey' }});
    expect(getJob(store, nb, jobId).status.toString()).to.be.eq(JobStateEnum.RUNNING);
    expect(getQuery(store, nb, jobId, 0)!.status.status.valueOf()).to.be.eq(QueryStateEnum.RUNNING);
    expect(getQuery(store, nb, jobId, 1)!.status.status.valueOf()).to.be.eq(QueryStateEnum.WAITING);

    // OP: NB_CODE_STATEMENT_PROGRESS
    store.dispatch('queryOnProgress', { nid: 'id', jobId, reqId, seqId: 0, status: {}, code: 'show tables;' });
    expect(getJob(store, nb, jobId).status.toString()).to.be.eq(JobStateEnum.RUNNING);
    expect(getQuery(store, nb, jobId, 0)!.status.status.valueOf()).to.be.eq(QueryStateEnum.RUNNING);
    expect(getQuery(store, nb, jobId, 1)!.status.status.valueOf()).to.be.eq(QueryStateEnum.WAITING);

    // OP: NB_CODE_STATEMENT_SUCCESS
    store.dispatch('queryOnSuccess', {
      nid: 'id', jobId, reqId, seqId: 0,
      headers: ['database','tableName','isTemporary'],
      rows: [['access_views','acct_tax_cmplnc','false']],
      startTime,
      endTime, code: 'show tables;', updatedCount: 0,
    });
    expect(getJob(store, nb, jobId).status.toString()).to.be.eq(JobStateEnum.RUNNING);
    expect(getQuery(store, nb, jobId, 0)!.status.status.valueOf()).to.be.eq(QueryStateEnum.SUCCESS);
    expect(getQuery(store, nb, jobId, 1)!.status.status.valueOf()).to.be.eq(QueryStateEnum.WAITING);

    // store.dispatch('queryOnProgress', { nid: 'id', jobId, reqId, seqId: 0, status: {}, code: 'show tables;' });
    // expect(getJob(store, nb, jobId).status.toString()).to.be.eq(JobStateEnum.RUNNING);
    // expect(getQuery(store, nb, jobId, 0)!.status.status.valueOf()).to.be.eq(QueryStateEnum.SUCCESS);
    // expect(getQuery(store, nb, jobId, 1)!.status.status.valueOf()).to.be.eq(QueryStateEnum.WAITING);

    // OP:CANCEL_SUCCESS
    store.dispatch('haltJobOnCancel', { nid: 'id', jobId });
    expect(getJob(store, nb, jobId).status.toString()).to.be.eq(JobStateEnum.ERROR);
    expect(getQuery(store, nb, jobId, 0)!.status.status.valueOf()).to.be.eq(QueryStateEnum.SUCCESS);
    expect(getQuery(store, nb, jobId, 1)!.status.status.valueOf()).to.be.eq(QueryStateEnum.CANCELED);

    store.dispatch('queryOnProgress', { nid: 'id', jobId, reqId, seqId: 1, status: {}, code: 'show tables;' });
    expect(getJob(store, nb, jobId).status.toString()).to.be.eq(JobStateEnum.ERROR);
    expect(getQuery(store, nb, jobId, 0)!.status.status.valueOf()).to.be.eq(QueryStateEnum.SUCCESS);
    expect(getQuery(store, nb, jobId, 1)!.status.status.valueOf()).to.be.eq(QueryStateEnum.CANCELED);
  });

});
