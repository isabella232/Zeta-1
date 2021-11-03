import { createLocalVue } from '@vue/test-utils';
import workspace from '@/stores/modules/WorkspaceStore';
import { expect } from 'chai';
import Vue, { VueConstructor } from 'vue';
import Vuex, { Store } from 'vuex';
import { Notebook, WorkSpaceType, NotebookStatus, QueryStatus, JobStatus, IJob } from '@/types/workspace';
import _ from 'lodash';
import { getNotebook, getJob, getQuery } from './util';

describe('notebook execute code: `show tables;`', () => {
    const localVue: VueConstructor<Vue> = createLocalVue();
    localVue.use(Vuex);
    const store = new Vuex.Store({ modules: {workspace}});
    const jobId = "JOBID";
    const reqId = "REQID";
    const stateId = "STATEID";
    const startTime = 0;
    const endTime = 1000 * 5;
    const query = {
        seqId: 0
    };
    before(() => {
        const nb = new Notebook("id", "testNotebook");
        nb.status = NotebookStatus.OFFLINE;
        nb.jobs = [];
        store.dispatch('addActiveWorkSpace', nb);
    })
    it('connect notebook', () => {
        const nb = getNotebook(store, 'id');
        expect(nb.status).to.be.equal(NotebookStatus.OFFLINE);

        store.dispatch('setNotebookStatus', { nid: 'id', status: NotebookStatus.CONNECTING});
        store.dispatch('setNotebookStatus', { nid: 'id', status: NotebookStatus.IDLE});
        expect(nb.status).to.be.equal(NotebookStatus.IDLE);
    })
    it('submit code', () => {
        const nb = getNotebook(store, 'id');
        store.dispatch('setNotebookStatus', { nid: 'id', status: NotebookStatus.SUBMITTING});
        expect(nb.status).to.be.equal(NotebookStatus.SUBMITTING);
        store.dispatch('setNotebookStatus', { nid: 'id', status: NotebookStatus.RUNNING});
        expect(nb.status).to.be.equal(NotebookStatus.RUNNING);
    })
    /** NB_CODE_JOB_READY */
    it('job ready: NB_CODE_JOB_READY', () => {
        const nb = getNotebook(store, 'id');
        store.dispatch('addJob', { nid: 'id', jobId, reqId});
        expect(getJob(store, nb, jobId)).is.exist;
    })
    /** NB_CODE_PREPROCESSED */
    it('code preprecessed: NB_CODE_PREPROCESSED', () => {
        const nb = getNotebook(store, 'id');
        store.dispatch("addQueries", { nid: 'id', jobId, reqId, codes: [{ code: 'show tables;', seq: query.seqId}] });
        expect(getJob(store, nb, jobId).status).to.be.eq(JobStatus.RUNNING)
        expect(getQuery(store, nb, jobId, query.seqId)).is.exist;
        expect(getQuery(store, nb, jobId, query.seqId)!.status.status).to.be.eq(QueryStatus.WAITING);
    })
    /** NB_CODE_STATEMENT_START */
    it('statement start: NB_CODE_STATEMENT_START', ()=> {
        const nb = getNotebook(store, 'id');
        store.dispatch("updateQuery", {jobId, seqId: query.seqId, query: { startTime ,statementId:stateId }});
        expect(getQuery(store, nb, jobId, query.seqId)!.status.status).to.not.eq(QueryStatus.RUNNING);
    })

    /** NB_CODE_STATEMENT_PROGRESS */
    it('statement progress: NB_CODE_STATEMENT_PROGRESS', ()=> {
        const nb = getNotebook(store, 'id');
        const newQuery = {status:{status: QueryStatus.RUNNING}};
        store.dispatch("setQueryProgress", { jobId, reqId, seqId: query.seqId, update: newQuery});
        expect(getQuery(store, nb, jobId, query.seqId)!.status.status).to.be.eq(QueryStatus.RUNNING);
    })
    /** NB_CODE_STATEMENT_SUCCESS */
    it('statement success: NB_CODE_STATEMENT_SUCCESS', () => {
        const nb = getNotebook(store, 'id');
		store.dispatch("updateQuery", { jobId, seqId: 0, query: { endTime }});
        store.dispatch("addQueryResult", { nid: 'id', jobId, reqId, seqId: query.seqId, headers: ["database","tableName","isTemporary"], rows: [["access_views","acct_tax_cmplnc","false"]]});
        expect(getQuery(store, nb, jobId, query.seqId)!.status.status).to.be.eq(QueryStatus.SUCCESS);
    })
    /** NB_CODE_JOB_DONE */
    it('job done', () => {
        const nb = getNotebook(store, 'id');
        store.dispatch("setNotebookStatus", { nid: 'id', status: NotebookStatus.IDLE });
        store.dispatch("setJobStatus", { jobId, reqId, status: JobStatus.SUCCESS });
        expect(getJob(store, nb, jobId).status).to.be.eq(JobStatus.SUCCESS)
        expect(nb.status).to.be.equal(NotebookStatus.IDLE);
    })
})
