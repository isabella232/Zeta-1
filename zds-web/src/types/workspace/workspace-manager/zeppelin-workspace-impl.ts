import { ZeppelinNoteWebSocketClient } from '@/types/ws-client';
import ZeppelinApi from '@/services/remote/ZeppelinApi';
import _ from 'lodash';
import { ZPLNote } from '../zeppelin.internal';
import { IWorkspace } from '../workspace.internal';
import Util from '@/services/Util.service';
import Vue from 'vue';
import { ZetaException } from '@/types/exception';
import config from '@/config/config';
function getWsUrl() {
  const endpoint = config.zeta.zeppelin.ws;
  // start with protocol
  if (_.startsWith(endpoint, 'ws') || _.startsWith(endpoint, 'wss')) {
    return endpoint;
  }
  const protocol = location.protocol.indexOf('https') >= 0 ? 'wss:': 'ws:';
  // start with double slash
  if (_.startsWith(endpoint, '//')) {
    return protocol + config.zeta.zeppelin.ws;
  }
  // start with single slash
  if (/^\/[a-zA-Z0-9^/].*/.test(endpoint)) {
    return protocol + '//' + location.hostname + config.zeta.zeppelin.ws;
  }
  // fallback
  return protocol + '//' + location.hostname + '/' + config.zeta.zeppelin.ws;
}
function parseError(msg: any) {
  try {

    const result = msg.ConnectionResult.interpreterResult.msg;
    const error = result.find(r => r.type === 'ERROR');
    return error.data;
  }
  catch {
    return '';
  }
}
export default class ZeppelinWorkspaceImpl {
  content: Vue;
  wsClients: Dict<ZeppelinNoteWebSocketClient> = {};
  zplApi: ZeppelinApi;
  ticket: string;
  constructor(zplApi: ZeppelinApi, content: Vue) {
    this.content = content;
    this.zplApi = zplApi;
    setInterval(() => {
      this.clearDeadWsClient();
    }, 60 * 1000);
  }
  getTicket(): Promise<string> {
    return new Promise((resolve, reject) => {
      if (this.ticket) {
        resolve(this.ticket);
      } else {
        this.zplApi.getTicket().then((res) => {
          resolve(res.data.body.ticket as string);
        }).catch(reject);
      }
    });
  }
  hasClient(id: string) {
    return !_.isEmpty(this.wsClients[id]);
    // const client = new ZeppelinNoteWebSocketClient('', id, ticket);

  }
  getClient(id: string) {
    return this.wsClients[id];
  }
  async createClient(ws: IWorkspace) {
    const ticket = await this.getTicket();
    return this.initWebsocketClient(ws, ticket);
    // return new ZeppelinNoteWebSocketClient('', id, ticket);
  }
  openTab(ws: IWorkspace) {
    const hasClient = this.hasClient(ws.notebookId);
    if (!hasClient) {
      this.createClient(ws).then(client => {
        this.wsClients[ws.notebookId] = client;
      });
    }
  }
  closeTab(ws: IWorkspace) {
    this.getClient(ws.notebookId).closeConnection();
  }
  clearDeadWsClient() {
    _.map(this.wsClients, (client, id) => {
      if (this.isOpenedNote(id)) {
        return;
      }
      client.close();
      delete this.wsClients[id];
    });
  }
  private isOpenedNote(id: string) {
    const wss = this.content.$store.state.workspace.workspaces;
    return Object.keys(wss).indexOf(id) >= 0;
  }
  private initWebsocketClient(ws: IWorkspace, ticket: string) {
    const client = new ZeppelinNoteWebSocketClient(getWsUrl(), ws.notebookId, ticket, Util.getNt());
    client.$on('CONNECTION_SUCCESS',() => {
      if (!this.isOpenedNote(ws.notebookId)) {
        return;
      }
      this.content.$message.success(` \`${ws.name}\` connect succeed!`);
    }).$on('DISCONNECTION_SUCCESS',() => {
      if (!this.isOpenedNote(ws.notebookId)) {
        return;
      }
      this.content.$message.success(` \`${ws.name}\` disconnect succeed!`);
    }).$on('CONNECTION_ABORT',() => {
      // if (!this.isOpenedNote(ws.notebookId)) {
      //   return;
      // }
      // this.content.$message.success(` \`${ws.name}\` disconnect succeed!`);
    }).$on('CONNECTION_ERROR',(msg: any) => {
      console.warn(`${ws.name} connect error`, msg);
      if (!this.isOpenedNote(ws.notebookId)) {
        return;
      }
      const cause = parseError(msg);
      const ex = new ZetaException({ code: '', errorDetail: { message: 'Connect error', cause: { cause, stackTrace: [], message: '', localizedMessage: '', suppressed: []} } }).props({
        path: 'notebook',
        workspaceId: ws.notebookId,
      });
      this.content.$store.dispatch('addException', {exception: ex});
    }).$on('DISCONNECTION_ERROR',(msg: any) => {
      console.warn(`${ws.name} connect error`, msg);
      if (!this.isOpenedNote(ws.notebookId)) {
        return;
      }
      const cause = parseError(msg);
      const ex = new ZetaException({ code: '', errorDetail: { message: 'Connect error', cause: { cause, stackTrace: [], message: '', localizedMessage: '', suppressed: []} } }).props({
        path: 'notebook',
        workspaceId: ws.notebookId,
      });
      this.content.$store.dispatch('addException', {exception: ex});
    });
    client.open();
    return client;
  }
}
