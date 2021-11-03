import { Query } from './query';
import { NotebookJobStatus, JobStateEnum } from './notebook-job-state';
import _ from 'lodash';

export class NotebookJob {
  reqId: string;
  jobId: string;
  submitTime?: string;
  startTime?: string;
  endTime?: string;
  queries: Query[];
  status: NotebookJobStatus;
  info?: string;
  seq = -1;
  constructor(state = JobStateEnum.SUBMITTED, queries: Query[] = []) {
    this.status = new NotebookJobStatus(state);
    this.queries = queries;
  }

  getQuery(seqId: number) {
    return this.queries.find(q => q.seqId === seqId);
  }

  submit(job: Partial<NotebookJob>) {
    if (this.status.submit()) {
      this.assign(job);
      return true;
    }
    return false;
  }
  onSubmitting(job: Partial<NotebookJob>) {
    if (this.status.onSubmitting()) {
      this.assign(job);
      return true;
    }
    return false;
  }
  onWaiting(job: Partial<NotebookJob>) {
    if (this.status.onWaiting()) {
      this.assign(job);
      return true;
    }
    return false;
  }
  onRunning(job: Partial<NotebookJob>) {
    if (this.status.onRunning()) {
      this.assign(job);
      return true;
    }
    return false;
  }
  onSuccess(job: Partial<NotebookJob>) {
    if (this.status.onSuccess()) {
      this.assign(job);
      return true;
    }
    return false;
  }
  onError(job: Partial<NotebookJob>) {
    if (this.status.onError()) {
      this.assign(job);
      return true;
    }
    return false;
  }
  onCanceled(job: Partial<NotebookJob>) {
    if (this.status.onCanceled()) {
      this.assign(job);
      return true;
    }
    return false;
  }
  executable() {
    if (this.queries.length === 0) {
      return true;
    }
    return _.chain(this.queries).map( q => q.executable()).includes(true).value();
  }
  private assign(job: Partial<NotebookJob>) {
    Object.assign(this, _.omit(job, ['queries', 'status']));
  }
}
