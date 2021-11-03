import { ZeppelinNoteWebSocketClient } from '../src/index';
import { spy, stub } from 'sinon';
import { expect } from 'chai';
describe('ws-client test', () => {
  const wsClient =  new ZeppelinNoteWebSocketClient('url', 'id');
  // set retry waiting to be 2 sec for testing
  wsClient.RETRY_WAITING = 2 * 1000;
  // mock constructor of WebSocket cause NodeJS doesn't have this
  (global as any ).WebSocket = function(this: any,url: string, noteId: string) {
    this.url = url;
    this.noteId = noteId;
    
    // eslint-disable-next-line @typescript-eslint/no-empty-function
    this.onopen = () => {};
    // eslint-disable-next-line @typescript-eslint/no-empty-function
    this.onerror = () => {};
    // eslint-disable-next-line @typescript-eslint/no-empty-function
    this.onclose = () => {};
    // eslint-disable-next-line @typescript-eslint/no-empty-function
    this.send = () => {};
    // eslint-disable-next-line @typescript-eslint/no-empty-function
    this.close = () => {};
    // eslint-disable-next-line @typescript-eslint/no-empty-function
    this.onmessage = () => {};
  };

  const spyOnopen = spy(wsClient,'onopen');
  const spySend = spy(wsClient,'send');
  const spyClose = spy(wsClient,'close');
  const spyOpen = spy(wsClient,'open');
  const spyOnclose = spy(wsClient, 'onclose');

  it('connect', () => {
    wsClient.open();
    expect(wsClient.url).to.eq('url');
    expect(wsClient.noteId).to.eq('id');
    expect(wsClient.socket).to.be.exist;
    const socket = wsClient.socket as any;
    // Trigger onopen
    socket.onopen();
    
    expect(spyOnopen.calledOnce).to.be.true;
    expect(spySend.calledWith({op: 'GET_NOTE', data: {id: 'id'}, principal: '', ticket: '', roles: '[]'})).to.be.true;
  });
  it('heartbeat', function(done){
    this.timeout(11000 + 100);
    setTimeout(function() {
      expect(spySend.calledWith({op: 'PING', principal: '', ticket: '', roles: '[]'})).to.be.true;
      done();
    }, 11 * 1000);
  });
  it('reconnect:onerror', function(done) {
    expect(wsClient.socket).to.be.exist;
    const socket = wsClient.socket as any;
    // Trigger onerror
    socket.onerror('err');
    expect(spyClose.calledOnce).to.be.true;
    this.timeout(3000 + 100);
    setTimeout(function() {
      expect(spyOpen.calledWith()).to.be.true;
      done();
    }, 3 * 1000);
  });

  it('custom event hanlder', function() {
    const handlerObject = {
      // eslint-disable-next-line @typescript-eslint/no-unused-vars,@typescript-eslint/no-empty-function
      handler: (para: any) => {}
    };

    const spyHandler = spy(handlerObject, 'handler');
    // register event `TEST`
    wsClient.$on('TEST', handlerObject.handler);

    expect(wsClient.eventHandlers['TEST']).to.be.exist;
    const params = {'op': 'TEST', 'data': 1};
    // Trigger event
    expect(wsClient.socket).to.be.exist;
    const socket = wsClient.socket as any;
    socket.onmessage({data: JSON.stringify(params)});
    expect(spyHandler.calledWith(1)).to.be.true;
  });
  it('reconnect:onclose(by network)', function(done) {
    wsClient.onclose({ code: 1005});
    this.timeout(3000 + 100);
    setTimeout(function() {
      expect(spyOpen.called).to.be.true;
      done();
    }, 3 * 1000);
  });
  it('close', function() {
    wsClient.close();
    this.timeout(0);
    setTimeout(() => {
      expect(wsClient.socket).to.be.exist;
      expect(spySend.calledWith({
        op: 'DISCONNECT_NOTE', 
        data: {id: 'id', interpreter: 'livy',}, 
        principal: '', 
        ticket: '', 
        roles: '[]'
      })).to.be.true;
    }, 0);
   
    // const socket = wsClient.socket as any;
    // socket.onclose();
    
    // this.timeout(100 + 100);
    // setTimeout(() => {
    //   // expect(spyOnclose.called).to.be.true;
    //   expect(wsClient.socket).to.be.not.exist;
    //   done();
    // }, 100 + 100);
  });
});
