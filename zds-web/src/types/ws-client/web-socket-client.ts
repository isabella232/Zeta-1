export const CLOSE_CODE = {
  NORMAL_CLOSE: 1000,
  CLOSE_WHEN_RETRY: 4000
};
export default abstract class WebSocketClient {
  readonly RETRY_TIMES = -1;
  RETRY_WAITING = 60 * 1000;
  retryTimes = 0;
  retryWaiting = 0;
  retryInterval: NodeJS.Timer | null = null;

  url: string;
  socket: WebSocket | null;
  
  eventHandlers: {[key: string]: (...arg: any[]) => void};
  constructor(url: string) {
    this.eventHandlers = {};
    this.url = url;
    this.retryTimes = this.RETRY_TIMES;
  }
  open() {
    if (!this.url) throw new Error('Cannot find websocket url');
    this.socket = new WebSocket(this.url);
    this.socket.onopen = (event) => {
      this.onopen(event);
    };
    this.socket.onerror = (error) => {
      this.onerror(error);
    };
    this.socket.onmessage = (msg) => {
      this.onmessage(msg);
    };
    this.socket.onclose = (msg) => {
      this.onclose(msg);
    };
  }
  close(code = CLOSE_CODE.NORMAL_CLOSE) {
    if (this.socket) {
      this.socket.close(code);
      return;
    } else {
      throw new Error('Cannot find websocket instance!');
    }
    
  }
  send(data: {[key: string]: any}) {
    if (this.socket) {
      this.socket.send(JSON.stringify(data));
      return;
    } else {
      throw new Error('Cannot find websocket instance!');
    }
  }
  onopen(event: any){
    console.debug('[websocket client]: on open' + JSON.stringify(event));
    this.retryTimes = this.RETRY_TIMES;
  }
  onerror(event: any){
    console.debug('[websocket client]: on error' + JSON.stringify(event));
    this.reopen();
  }
  onmessage(msg: any) {
    console.debug('[websocket client]: on message' + JSON.stringify(msg));
    const body = JSON.parse(msg);
    if (body.op) {
      const op = body.op;
      if (this.eventHandlers[op]) {
        const handler = this.eventHandlers[op];
        handler(body.data);
      }
    }
  }
  onclose(event: CloseEvent) {
    console.debug('[websocket client]: on close' + JSON.stringify(event));
    if (this.socket) {
      this.socket.onopen = null;
      this.socket.onerror = null;
      this.socket.onmessage = null;
      this.socket.onclose = null;
      this.socket = null;
    }
    // when calleed by retry
    if (event.code === CLOSE_CODE.CLOSE_WHEN_RETRY) {
      return;
    }
    if (event.code !== CLOSE_CODE.NORMAL_CLOSE) {
      this.reopen();
    }
  }
  
  // retry
  reopen() {
    try {
      this.close(CLOSE_CODE.CLOSE_WHEN_RETRY);
    } catch {
      console.debug('[websocket client]: close failed when reopen');
    }
    
    if (this.retryTimes ===0) {
      throw new Error('Maximum number of retry times exceeded');
    } else {
      this.retryWaiting = this.RETRY_WAITING;
      this.retryInterval = setInterval(() => {
        this.retryWaiting -= 1000;
        if (this.retryWaiting === 0) {
          clearInterval(this.retryInterval as NodeJS.Timer);
          if (this.retryTimes > 0) {
            this.retryTimes--;
          }
          this.open();
        }
      }, 1000);
    }
  }

  $on(op: string, handler: (...args: any[]) => void) {
    this.eventHandlers[op] = handler;
    return this;
  }
}