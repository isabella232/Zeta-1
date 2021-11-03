import { createLocalVue } from '@vue/test-utils';
import workspace from '@/stores/modules/WorkspaceStore';
import { expect } from 'chai';
import Vue, { VueConstructor } from 'vue';
import Vuex, { Store } from 'vuex';
import { Notebook, WorkSpaceType, NotebookStatus, QueryStatus, JobStatus, IJob } from '@/types/workspace';
import _ from 'lodash';
import { getNotebook, getJob, getQuery } from './util';

describe('notebook execute code: multi', () => {
    const localVue: VueConstructor<Vue> = createLocalVue();
    localVue.use(Vuex);
    const store = new Vuex.Store({ modules: {workspace}});
    beforeEach(() => {
        /**
         * clear store and add new notebook
         */
        store.dispatch('closeNotebookById', "id");
        const nb = new Notebook("id", "testNotebook");
        nb.status = NotebookStatus.OFFLINE;
        nb.jobs = [];
        store.dispatch('addActiveWorkSpace', nb);
        store.dispatch('setNotebookStatus', { nid: nb.notebookId, status: NotebookStatus.CONNECTING});
        store.dispatch('setNotebookStatus', { nid: nb.notebookId, status: NotebookStatus.IDLE});
    })
    it('multi-task', () => {
        const jobId = "JOBID";
        const reqId = "REQID";
        const stateId = "STATEID";
        const startTime = 0;
        const endTime = 1000 * 5;
        const codes = [
            { code: 'show tables;', seq: 0},
            { code: 'select 1;', seq: 1},
        ]

        store.dispatch('setNotebookStatus', { nid: 'id', status: NotebookStatus.SUBMITTING});
        store.dispatch('setNotebookStatus', { nid: 'id', status: NotebookStatus.RUNNING});
        expect(getNotebook(store, 'id').status).to.be.equal(NotebookStatus.RUNNING);

        store.dispatch('addJob', { nid: 'id', jobId, reqId});
        store.dispatch("addQueries", { nid: 'id', jobId, reqId, codes});
        const nb = getNotebook(store, 'id');
        expect(getJob(store, nb, jobId).status).to.be.eq(JobStatus.RUNNING)
        expect(getQuery(store, nb, jobId, 0)).is.exist;
        expect(getQuery(store, nb, jobId, 1)).is.exist;
        expect(getQuery(store, nb, jobId, 0)!.status.status).to.be.eq(QueryStatus.WAITING);
        expect(getQuery(store, nb, jobId, 1)!.status.status).to.be.eq(QueryStatus.WAITING);

        store.dispatch("updateQuery", {jobId, seqId: 0, query: { startTime ,statementId:stateId }});
        store.dispatch("setQueryProgress", { jobId, reqId, seqId: 0, update:  {status:{status: QueryStatus.RUNNING}}});
        expect(getQuery(store, nb, jobId, 0)!.status.status).to.be.eq(QueryStatus.RUNNING);
        expect(getQuery(store, nb, jobId, 1)!.status.status).to.be.eq(QueryStatus.WAITING);

        store.dispatch("updateQuery", { jobId, seqId: 0, query: { endTime }});
        store.dispatch("addQueryResult", { nid: 'id', jobId, reqId, seqId: 0, headers: ["database","tableName","isTemporary"], rows: [["access_views","acct_tax_cmplnc","false"]]});
        expect(getQuery(store, nb, jobId, 0)!.status.status).to.be.eq(QueryStatus.SUCCESS);
        expect(getQuery(store, nb, jobId, 1)!.status.status).to.be.eq(QueryStatus.WAITING);

        store.dispatch("updateQuery", {jobId, seqId: 1, query: { startTime ,statementId:stateId }});
        store.dispatch("setQueryProgress", { jobId, reqId, seqId: 1, update:  {status:{status: QueryStatus.RUNNING}}});
        expect(getQuery(store, nb, jobId, 0)!.status.status).to.be.eq(QueryStatus.SUCCESS);
        expect(getQuery(store, nb, jobId, 1)!.status.status).to.be.eq(QueryStatus.RUNNING);

        store.dispatch("updateQuery", { jobId, seqId: 1, query: { endTime }});
        store.dispatch("addQueryResult", { nid: 'id', jobId, reqId, seqId: 1, headers: ["database","tableName","isTemporary"], rows: [["access_views","acct_tax_cmplnc","false"]]});
        expect(getQuery(store, nb, jobId, 0)!.status.status).to.be.eq(QueryStatus.SUCCESS);
        expect(getQuery(store, nb, jobId, 1)!.status.status).to.be.eq(QueryStatus.SUCCESS);

        store.dispatch("setNotebookStatus", { nid: 'id', status: NotebookStatus.IDLE });
        store.dispatch("setJobStatus", { jobId, reqId, status: JobStatus.SUCCESS });
        expect(getJob(store, nb, jobId).status).to.be.eq(JobStatus.SUCCESS)
        expect(nb.status).to.be.equal(NotebookStatus.IDLE);
    })
    it('message in wrong order', () => {
        const jobId = "JOBID";
        const reqId = "REQID";
        const stateId = "STATEID";
        const startTime = 0;
        const endTime = 1000 * 5;
        const codes = [
            { code: 'show tables;', seq: 0},
            { code: 'select 1;', seq: 1},
        ]

        store.dispatch('setNotebookStatus', { nid: 'id', status: NotebookStatus.SUBMITTING});
        store.dispatch('setNotebookStatus', { nid: 'id', status: NotebookStatus.RUNNING});
        expect(getNotebook(store, 'id').status).to.be.equal(NotebookStatus.RUNNING);

        store.dispatch('addJob', { nid: 'id', jobId, reqId});
        store.dispatch("addQueries", { nid: 'id', jobId, reqId, codes});
        const nb = getNotebook(store, 'id');
        expect(getJob(store, nb, jobId).status).to.be.eq(JobStatus.RUNNING)
        expect(getQuery(store, nb, jobId, 0)).is.exist;
        expect(getQuery(store, nb, jobId, 1)).is.exist;
        expect(getQuery(store, nb, jobId, 0)!.status.status).to.be.eq(QueryStatus.WAITING);
        expect(getQuery(store, nb, jobId, 1)!.status.status).to.be.eq(QueryStatus.WAITING);

        store.dispatch("updateQuery", {jobId, seqId: 0, query: { startTime ,statementId:stateId }});
        store.dispatch("setQueryProgress", { jobId, reqId, seqId: 0, update:  {status:{status: QueryStatus.RUNNING}}});
        expect(getQuery(store, nb, jobId, 0)!.status.status).to.be.eq(QueryStatus.RUNNING);
        expect(getQuery(store, nb, jobId, 1)!.status.status).to.be.eq(QueryStatus.WAITING);

        store.dispatch("updateQuery", { jobId, seqId: 0, query: { endTime }});
        store.dispatch("addQueryResult", { nid: 'id', jobId, reqId, seqId: 0, headers: ["database","tableName","isTemporary"], rows: [["access_views","acct_tax_cmplnc","false"]]});
        expect(getQuery(store, nb, jobId, 0)!.status.status).to.be.eq(QueryStatus.SUCCESS);
        expect(getQuery(store, nb, jobId, 1)!.status.status).to.be.eq(QueryStatus.WAITING);

        //! test if got a progress message for a succeed job
        store.dispatch("setQueryProgress", { jobId, reqId, seqId: 0, update:  {status:{status: QueryStatus.RUNNING}}});

        store.dispatch("updateQuery", {jobId, seqId: 1, query: { startTime ,statementId:stateId }});
        store.dispatch("setQueryProgress", { jobId, reqId, seqId: 1, update:  {status:{status: QueryStatus.RUNNING}}});
        expect(getQuery(store, nb, jobId, 0)!.status.status).to.be.eq(QueryStatus.SUCCESS);
        expect(getQuery(store, nb, jobId, 1)!.status.status).to.be.eq(QueryStatus.RUNNING);

        store.dispatch("updateQuery", { jobId, seqId: 1, query: { endTime }});
        store.dispatch("addQueryResult", { nid: 'id', jobId, reqId, seqId: 1, headers: ["database","tableName","isTemporary"], rows: [["access_views","acct_tax_cmplnc","false"]]});
        expect(getQuery(store, nb, jobId, 0)!.status.status).to.be.eq(QueryStatus.SUCCESS);
        expect(getQuery(store, nb, jobId, 1)!.status.status).to.be.eq(QueryStatus.SUCCESS);

        store.dispatch("setNotebookStatus", { nid: 'id', status: NotebookStatus.IDLE });
        store.dispatch("setJobStatus", { jobId, reqId, status: JobStatus.SUCCESS });
        expect(getJob(store, nb, jobId).status).to.be.eq(JobStatus.SUCCESS)
        expect(nb.status).to.be.equal(NotebookStatus.IDLE);
    })
    it('halt job(error) after submit', () => {
        const jobId = "JOBID";
        const reqId = "REQID";
        const codes = [
            { code: 'show tables;', seq: 0},
            { code: 'select 1;', seq: 1},
        ]

        store.dispatch('setNotebookStatus', { nid: 'id', status: NotebookStatus.SUBMITTING});
        store.dispatch('setNotebookStatus', { nid: 'id', status: NotebookStatus.RUNNING});
        expect(getNotebook(store, 'id').status).to.be.equal(NotebookStatus.RUNNING);

        store.dispatch('addJob', { nid: 'id', jobId, reqId});
        store.dispatch("addQueries", { nid: 'id', jobId, reqId, codes});
        const nb = getNotebook(store, 'id');
        expect(getJob(store, nb, jobId).status).to.be.eq(JobStatus.RUNNING)
        expect(getQuery(store, nb, jobId, 0)).is.exist;
        expect(getQuery(store, nb, jobId, 1)).is.exist;
        expect(getQuery(store, nb, jobId, 0)!.status.status).to.be.eq(QueryStatus.WAITING);
        expect(getQuery(store, nb, jobId, 1)!.status.status).to.be.eq(QueryStatus.WAITING);

        store.dispatch("haltJob", { nid: 'id', jobId, reqId, seqId: 0, status: JobStatus.ERROR });
        store.dispatch("setJobStatus", { jobId, reqId, status: JobStatus.ERROR, info: ''});
        expect(getJob(store, nb, jobId).status).to.be.eq(JobStatus.ERROR)
        expect(getQuery(store, nb, jobId, 0)!.status.status).to.be.eq(QueryStatus.ERROR);
        expect(getQuery(store, nb, jobId, 1)!.status.status).to.be.eq(QueryStatus.CANCELED);
        expect(getNotebook(store, 'id').status).to.be.equal(NotebookStatus.IDLE);
    })
    it('halt job(cancel) after submit', () => {
        const jobId = "JOBID";
        const reqId = "REQID";
        const codes = [
            { code: 'show tables;', seq: 0},
            { code: 'select 1;', seq: 1},
        ]

        store.dispatch('setNotebookStatus', { nid: 'id', status: NotebookStatus.SUBMITTING});
        store.dispatch('setNotebookStatus', { nid: 'id', status: NotebookStatus.RUNNING});
        expect(getNotebook(store, 'id').status).to.be.equal(NotebookStatus.RUNNING);

        store.dispatch('addJob', { nid: 'id', jobId, reqId});
        store.dispatch("addQueries", { nid: 'id', jobId, reqId, codes});
        const nb = getNotebook(store, 'id');
        expect(getJob(store, nb, jobId).status).to.be.eq(JobStatus.RUNNING)
        expect(getQuery(store, nb, jobId, 0)).is.exist;
        expect(getQuery(store, nb, jobId, 1)).is.exist;
        expect(getQuery(store, nb, jobId, 0)!.status.status).to.be.eq(QueryStatus.WAITING);
        expect(getQuery(store, nb, jobId, 1)!.status.status).to.be.eq(QueryStatus.WAITING);

        store.dispatch("haltJob", { nid: 'id', jobId, reqId, seqId: 0, status: JobStatus.CANCELED });
        expect(getJob(store, nb, jobId).status).to.be.eq(JobStatus.CANCELED)
        expect(getQuery(store, nb, jobId, 0)!.status.status).to.be.eq(QueryStatus.CANCELED);
        expect(getQuery(store, nb, jobId, 1)!.status.status).to.be.eq(QueryStatus.CANCELED);
        expect(getNotebook(store, 'id').status).to.be.equal(NotebookStatus.IDLE);
    })
    it('disconnect when running', () => {
        const jobId = "JOBID";
        const reqId = "REQID";
        const codes = [
            { code: 'show tables;', seq: 0},
            { code: 'select 1;', seq: 1},
        ]
        const startTime = 0;
        const endTime = 1000 * 5;
        const stateId = "STATEID";

        store.dispatch('setNotebookStatus', { nid: 'id', status: NotebookStatus.SUBMITTING});
        store.dispatch('setNotebookStatus', { nid: 'id', status: NotebookStatus.RUNNING});
        expect(getNotebook(store, 'id').status).to.be.equal(NotebookStatus.RUNNING);

        store.dispatch('addJob', { nid: 'id', jobId, reqId});
        store.dispatch("addQueries", { nid: 'id', jobId, reqId, codes});
        const nb = getNotebook(store, 'id');
        expect(getJob(store, nb, jobId).status).to.be.eq(JobStatus.RUNNING)
        expect(getQuery(store, nb, jobId, 0)).is.exist;
        expect(getQuery(store, nb, jobId, 1)).is.exist;
        expect(getQuery(store, nb, jobId, 0)!.status.status).to.be.eq(QueryStatus.WAITING);
        expect(getQuery(store, nb, jobId, 1)!.status.status).to.be.eq(QueryStatus.WAITING);

        /** disconnect action */
        store.dispatch("setNotebookStatus", { nid: 'id', status: NotebookStatus.DISCONNECTING })
        store.dispatch("setNotebookStatus", { nid: 'id', status: NotebookStatus.OFFLINE })
        expect(getJob(store, nb, jobId).status).to.be.eq(JobStatus.ERROR)
        expect(getQuery(store, nb, jobId, 0)!.status.status).to.be.eq(QueryStatus.ERROR);
        expect(getQuery(store, nb, jobId, 1)!.status.status).to.be.eq(QueryStatus.CANCELED);
        expect(getNotebook(store, 'id').status).to.be.equal(NotebookStatus.OFFLINE);
    })
})
