import { createLocalVue } from '@vue/test-utils';
import workspace from '@/stores/modules/WorkspaceStore';
import { expect } from 'chai';
import Vue, { VueConstructor } from 'vue';
import Vuex, { Store } from 'vuex';
import { Notebook, WorkSpaceType, NotebookStatus, QueryStatus, JobStatus, IJob } from '@/types/workspace';
import _ from 'lodash';
import { IStore } from '@/stores';
import { NotebookJob } from '@/types/workspace/notebook-job';
function getJob(store: Store<IStore>, nb: Notebook, jobId: string) {
  const jobs: Dict<NotebookJob> = _.pick(store.state.workspace.jobs, nb.jobs);
  const job = jobs[jobId];
  return job;
}
function getQuery(store: Store<IStore>, nb: Notebook, jobId: string, seqId: number) {
  const job = getJob(store, nb, jobId);
  return job.queries.find(q => q.seqId === seqId);
}
describe('workspace.store', () => {
  const localVue: VueConstructor<Vue> = createLocalVue();
  localVue.use(Vuex);
  const store = new Vuex.Store({ modules: {workspace}});

  before(() => {
    const nb = new Notebook('id', 'testNotebook');
    nb.jobs = [];
    const nb2 = new Notebook('id_1', 'testNotebook1');
    store.dispatch('addActiveWorkSpace', nb);
    store.dispatch('addWorkSpace', nb2);
  });

  it('active workspace', () => {
    const activeId = store.getters.activeWorkspaceId;
    expect(activeId).to.be.equal('id');
  });
});
