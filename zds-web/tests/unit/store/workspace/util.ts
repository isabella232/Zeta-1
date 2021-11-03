import { Store } from 'vuex';
import { IStore } from '@/stores';
import { Notebook } from '@/types/workspace';
import _ from 'lodash';
import { NotebookJob } from '@/types/workspace/notebook-job';

export function getJob(store: Store<IStore>, nb: Notebook, jobId: string) {
  const jobs: Dict<NotebookJob> = _.pick(store.state.workspace.jobs, nb.jobs);
  const job = jobs[jobId];
  return job;
}
export function getQuery(store: Store<IStore>, nb: Notebook, jobId: string, seqId: number) {
  const job = getJob(store, nb, jobId);
  return job.queries.find(q => q.seqId === seqId);
}
export function getNotebook(store: Store<IStore>, id: string) {
  return store.getters.nbByNId(id) as Notebook;
}
