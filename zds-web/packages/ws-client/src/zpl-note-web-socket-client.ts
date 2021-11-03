import WebSocketClient from './web-socket-client';

export default class ZeppelinNoteWebSocketClient extends WebSocketClient {
  RETRY_WAITING = 10 * 1000;
  ticket: string;
  principal: string;
  noteId: string;
  heartbeadInterval: NodeJS.Timer | null = null;
  constructor(url: string, noteId: string, ticket = '', principal = '') {
    super(url);
    this.noteId = noteId;
    this.ticket = ticket;
    this.principal = principal;
  }
  
  send(data: {[key: string]: any}) {
    data.principal = this.principal;
    data.ticket = this.ticket;
    data.roles = '[]';
    super.send(data);
  }
  onopen = (event: any) => {
    super.onopen(event);
    // bind note
    this.send({op: 'GET_NOTE', data: {id: this.noteId}});
    // heartbeat
    this.heartbeadInterval = setInterval(() => {
      this.send({op: 'PING'});
    }, 10 * 1000);
  };
  onerror(event: any){
    if (this.heartbeadInterval) {
      clearInterval(this.heartbeadInterval);
    }
    super.onerror(event);
  }
  onmessage(msg: any) {
    console.debug('[zeppelin-note-websocket]: on message' + JSON.stringify(msg));
    const data = msg.data;
    super.onmessage(data);
  }
  onclose(event: any) {
    if (this.heartbeadInterval) {
      clearInterval(this.heartbeadInterval);
    }
    super.onclose(event);
  }
  closeConnection() {
    try {
      this.send({
        op: 'DISCONNECT_NOTE',
        data: {
          id: this.noteId,
          interpreter: 'livy',
        },
      });
    } catch {
      console.debug('[zeppelin-note-websocket]: close ' + 'error when send DISCONNECT');
    }
  }
} 