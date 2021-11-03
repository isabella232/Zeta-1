import { QueryState, QueryStateEnum} from '@/types/workspace/notebook-job/query/query-state';
import { expect } from 'chai';

describe('notebook -> job -> query state', () => {
  it('submit -> waiting', () => {
    const submited = new QueryState(QueryStateEnum.SUBMITTED);
    expect(submited.onWaiting()).to.be.true;

    const waiting = submited;
    expect(waiting.valueOf()).to.be.eq(QueryStateEnum.WAITING);
    expect(waiting.submit()).to.be.false;
    expect(waiting.submit()).to.be.false;
    expect(waiting.onProgress()).to.be.true;
    expect(waiting.onSuccess()).to.be.true;
  });
  it('submit -> success', () => {
    const submited = new QueryState(QueryStateEnum.SUBMITTED);
    expect(submited.onSuccess()).to.be.true;

    const succeed = submited;
    expect(succeed.valueOf()).to.be.eq(QueryStateEnum.SUCCESS);
  });
  it('success', () => {
    const succeed = new QueryState(QueryStateEnum.SUCCESS);

    expect(succeed.submit()).to.be.false;
    expect(succeed.onWaiting()).to.be.false;
    expect(succeed.onProgress()).to.be.false;
    expect(succeed.onCancel()).to.be.false;
    expect(succeed.onError()).to.be.false;
  });

  it('error', () => {
    const error = new QueryState(QueryStateEnum.ERROR);

    expect(error.submit()).to.be.false;
    expect(error.onWaiting()).to.be.false;
    expect(error.onProgress()).to.be.false;
    expect(error.onCancel()).to.be.false;
    expect(error.onSuccess()).to.be.false;
  });
});
