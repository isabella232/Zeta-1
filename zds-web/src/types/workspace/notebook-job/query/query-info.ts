import { QueryState, QueryStateEnum } from './query-state';
import _ from 'lodash';

export class QueryInfo {
  info?: any;
  progress: number;
  status: QueryState;
  totalNumTasks?: number;
  totalNumCompletedTasks?: number;
  numActiveTasks?: number;
  numCompletedCurrentStageTasks?: number;
  dag?: string;

  constructor(state: QueryStateEnum = QueryStateEnum.WAITING) {
    this.progress = 0;
    this.status = new QueryState(state);
  }

  submit(info: Partial<QueryInfo>) {
    if (this.status.submit()) {
      this.assign(info);
      return true;
    }
    return false;
  }

  onWaiting(info: Partial<QueryInfo>) {
    if (this.status.onWaiting()) {
      this.assign(info);
      return true;
    }
    return false;
  }

  onRun(info: Partial<QueryInfo>) {
    if (this.status.onRun()) {
      this.assign(info);
      return true;
    }
    return false;
  }
  onProgress(info: Partial<QueryInfo>) {
    if (this.status.onProgress()) {
      this.assign(info);
      return true;
    }
    return false;
  }
  onCancel(info: Partial<QueryInfo>) {
    if (this.status.onCancel()) {
      this.assign(info);
      return true;
    }
    return false;
  }
  onError(info: Partial<QueryInfo>) {
    if (this.status.onError()) {
      this.assign(info);
      return true;
    }
    return false;
  }
  onSuccess(info: Partial<QueryInfo>) {
    if (this.status.onSuccess()) {
      this.assign(info);
      return true;
    }
    return false;
  }

  toString() {
    return this.status.toString();
  }

  valueOf() {
    return this.status.valueOf();
  }

  private assign(info: Partial<QueryInfo>) {
    Object.assign(this, _.omit(info, ['status']));
  }
}
