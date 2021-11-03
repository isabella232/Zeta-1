import { QueryInfo } from './query-info';
import { QueryContent } from './query-content';
import { QueryState, QueryStateEnum } from './query-state';
import moment from 'moment';
import _ from 'lodash';

export class Query {
  seqId: number;
  /// (ISO 8601, no fractional seconds)
  /// e.g. "2013-02-04T22:44:30.652Z"
  submitTime?: string;
  startTime?: string;
  endTime?: string;

  status: QueryInfo;
  query: string;
  headers?: string[];
  rows?: Array<string[]>;
  content?: QueryContent;
  // progress: number,
  errorMessage?: string;
  statementId?: number;
  count?: number;
  updatedCount?: number;


  constructor(seqId = 0, state = QueryStateEnum.WAITING) {
    this.seqId = seqId;
    this.status = new QueryInfo(state);
    this.submitTime = moment().format();
  }

  executable() {
    // TODO declare executable in QueryState
    if (this.status.valueOf() === QueryStateEnum.CANCELED
    || this.status.valueOf() === QueryStateEnum.SUCCESS
    || this.status.valueOf() === QueryStateEnum.ERROR ) {
      return false;
    } else {
      return true;
    }
  }
  submit(info: Partial<Query>) {
    if (this.status.submit(info.status || this.status)) {
      this.assign(info);
      return true;
    }
    return false;
  }

  onWaiting(info: Partial<Query>) {
    if (this.status.onWaiting(info.status || this.status)) {
      this.assign(info);
      return true;
    }
    return false;
  }

  onRun(info: Partial<Query>) {
    if (this.status.onRun(info.status || this.status)) {
      this.assign(info);
      return true;
    }
    return false;
  }
  onProgress(info: Partial<Query>) {
    if (this.status.onProgress(info.status || this.status)) {
      this.assign(info);
      return true;
    }
    return false;
  }
  onCancel(info: Partial<Query>) {
    if (this.status.onCancel(info.status || this.status)) {
      this.assign(info);
      return true;
    }
    return false;
  }
  onError(info: Partial<Query>) {
    if (this.status.onError(info.status || this.status)) {
      this.assign(info);
      return true;
    }
    return false;
  }
  onSuccess(info: Partial<Query>) {
    if (this.status.onSuccess(info.status || this.status)) {
      this.assign(info);
      return true;
    }
    return false;
  }

  private assign(info: Partial<Query>) {
    Object.assign(this, _.omit(info, ['status']));
  }
}
