import { expect } from 'chai';
import { QueryState } from '@/types/workspace/notebook-job/query/query-state';
import { State } from '@/types/workspace/notebook-job/query/query-state/state';

describe('query state', () => {
  const query = new QueryState();
  before(() => {
    console.log('test start');
    query.submit();
  });
  it('submitted query', () => {
    expect(query.stateController.valueOf()).to.be.equal(State.SUBMITTED);
    expect(query.stateController.onWaiting).to.be.true;
    expect(query.stateController.onSuccess).to.be.true;
  });
  it('onWaiting query', () => {
    //set waiting
    query.onWaiting();
    expect(query.stateController.valueOf()).to.be.equal(State.WAITING);
    expect(query.stateController.onProgress).to.be.true;
  });
  it('onRun query', () => {
    query.onRun();
    expect(query.stateController.valueOf()).to.be.equal(State.RUNNING);
    // not equal waiting
    expect(query.stateController.onWaiting).to.be.false;
  });
  it('onCancel query', () => {
    query.onCancel();
    expect(query.stateController.valueOf()).to.be.equal(State.CANCELED);

    query.onError();
    expect(query.stateController.onError).to.be.false;
  });
  // it('onError notebook', () => {
  //   query.onError();
  //   expect(query.stateController.valueOf()).to.be.equal(State.ERROR);
  // });
  // it('onSuccess notebook', () => {
  //   query.onSuccess();
  //   expect(query.stateController.valueOf()).to.be.equal(State.SUCCESS);
  // });
});

describe('query state: RUNNING', () => {
  const query = new QueryState(State.RUNNING);
  before(() => {
    console.log('test start');
  });
  it('onRun query', () => {
    expect(query.stateController.valueOf()).to.be.equal(State.RUNNING);
    expect(query.stateController.onProgress).to.be.true;
    expect(query.stateController.onCancel).to.be.true;

    expect(query.stateController.onRun).to.be.false;
    expect(query.stateController.onWaiting).to.be.false;
  });

  it('onSuccess query', () => {
    query.onSuccess();
    expect(query.stateController.valueOf()).to.be.equal(State.SUCCESS);

    expect(query.stateController.onRun).to.be.equal(false);
  });
});

describe('query state: SUCCESS', () => {
  const query = new QueryState(State.SUCCESS);

  it('onSuccess query', () => {
    expect(query.stateController.valueOf()).to.be.equal(State.SUCCESS);
    if(query.stateController.onSuccess){
      expect(query.stateController.valueOf()).to.be.equal(State.SUCCESS);
    }
  });
});

describe('query state: CANCELED', () => {
  const query = new QueryState(State.CANCELED);

  it('onCancel query', () => {
    expect(query.stateController.valueOf()).to.be.equal(State.CANCELED);
    expect(query.stateController.onRun).to.be.equal(false);
  });
});
