/* eslint-disable no-console */
import { Client, Message, IFrame, IPublishParams } from '@stomp/stompjs';
import { OPEndpoints } from './endpoints';
import ZetaConfig from '@/config/config';
import _ from 'lodash';
import Util from '@/services/Util.service';
import SockJS from 'sockjs-client';
import { WSMessageMetadataKey, WSMessageOptions, WSMessageMetadataExecutorKey } from './ws-message';
import { AsyncWSRequest, AsyncWSMessageOption } from './async-websocket-request';
import { ZetaException } from '@/types/exception';
import uuid from 'uuid';
import { Status } from './status';
import { EventBus } from '@/components/EventBus';
function verifyUsername (frame?: IFrame): boolean {
  if (!frame || !frame.headers) return false;
  const username = (frame.headers as any)['user-name'];
  return !_.isEmpty(username);
}
export default class StompWebsocketClient {
  client: Client;
  reconnectCnt = 0;
  asyncRequestList: AsyncWSRequest[];
  msgQueue: IPublishParams[] = [];
  status: Status = Status.NOT_INITILIZED;
  sessionId: string;
  constructor () {
    this.sessionId = Util.generateSessionId();
    this.asyncRequestList = [];
    this.client = new Client();
    this.client.webSocketFactory = () => {
      return new SockJS(ZetaConfig.zeta.notebook.endpoints.connect, undefined, {
        sessionId: () => { this.sessionId = Util.generateSessionId(); return this.sessionId; },
        transports: ['websocket', 'xhr-polling'],
      });
    };
    // this.socket = getSocket(ZetaConfig.zeta.notebook.endpoints.connect);
    // this.client = Stomp.over(this.socket);
    this.client.brokerURL = ZetaConfig.zeta.notebook.endpoints.connect;

    this.client.splitLargeFrames = true;
    this.client.maxWebSocketChunkSize = 8 * 1024;
    this.client.heartbeatIncoming = 10 * 1000;
    this.client.heartbeatOutgoing = 30 * 1000;
    // this.client.reconnectDelay = .1 * 1000;

    this.client.onConnect = this.onConnect;
    this.client.onStompError = this.onStompError;
    this.client.onWebSocketClose = this.onWebSocketClose;
    this.client.onWebSocketError = this.onWebSocketError;
    this.status = Status.INITILIZED;
  }
  connect () {
    this.client.connectHeaders = {
      'Zeta-Authorization': 'Bearer ' + Util.getZetaToken(),
    };
    this.client.activate();
    console.log('StompWebsocketClient', this.client);
    this.status = Status.CONNECTING;
  }

  disconnect () {
    this.client.deactivate();
  }
  onConnect = (frame: IFrame) => {
    if (!verifyUsername(frame)) {
      console.warn('StompWebsocketClient', 'invalid header', frame);
      this.status = Status.LOG_UNKNOWN_FAIL;
      this.forceReconnect();
      return;
    }
    // reset reconnect count
    this.reconnectCnt = 0;
    this.client.subscribe('/user/queue/square', (msg: Message) => {
      this.messageHandler('/user/queue/square', msg);
    });
    this.greating();
    // clear unsend message
    while (this.msgQueue.length > 0) {
      const msg = this.msgQueue.shift();
      if (msg) {
        this.client.publish(msg);
      }
    }
    this.status = Status.LOGGED_IN;
    console.log('eventbus:emit');
    EventBus.$emit('websocket-connected', {});
    this.onSubscribed();
  };
  onSubscribed () {
    console.log('StompWebsocketClient', 'onSubscribed');
  }
  forceReconnect () {
    this.client.deactivate();
    setTimeout(() => {
      this.client.activate();
    }, 10);
  }
  /** Events */
  onStompError = (frame: IFrame) => {
    console.warn('StompWebsocketClient', 'onStompError', frame);
  };

  onWebSocketClose = (event: CloseEvent) => {
    this.status = Status.OFFLINE;
    console.warn('StompWebsocketClient', 'onWebSocketClose', event);
    this.reconnectCnt++;
    this.client.reconnectDelay = 1000 * (10 * this.reconnectCnt);
  };

  onWebSocketError = (event: Event) => {
    console.warn('StompWebsocketClient', 'onWebSocketError', event);
  };

  messageHandler (topic: string, msg: Message) {
    console.debug('StompWebsocketClient', `msg from ${topic}`, msg);
    try {
      const body = JSON.parse(msg.body);
      this.dispatchMessage(body);
    }
    catch (e) {
      console.warn('StompWebsocketClient', 'error happened when parse message', e);
    }

  }
  findAsyncRequest (messageBody: any, opOption: WSMessageOptions): AsyncWSRequest | undefined {
    if (!opOption.alias || !opOption.mappingPath) return undefined;
    return _.chain(this.asyncRequestList)
      .filter((req: AsyncWSRequest) => req.opAlias === opOption.alias)
      .find((req: AsyncWSRequest) => {
        const message = messageBody.data;
        const isReq = _.find(req.mapping, (sendVal, key) => {
          if (opOption.mappingPath && opOption.mappingPath[key] != undefined) {
            const path = opOption.mappingPath[key];
            const reqVal = _.get(message, path); // message[path];
            return Boolean(reqVal == sendVal);
          }
          return false;
        });
        return Boolean(isReq);
      }).value();
  }
  dispatchMessage (messageBody: any) {
    const opOption: WSMessageOptions = Reflect.getMetadata(WSMessageMetadataKey, this, messageBody.op);
    const executor: Function = Reflect.getMetadata(WSMessageMetadataExecutorKey, this, messageBody.op);
    if (!executor) {
      return;
    }
    const result: undefined | ZetaException = executor.call(this, messageBody.data);
    if (opOption.isError && result) {
      this.onErrorMessage(result);
    }

    if (opOption.alias && opOption.mappingPath) {
      const asyncRequest = this.findAsyncRequest(messageBody, opOption);
      if (asyncRequest) {
        if (opOption.isError) {
          asyncRequest.rejecter(result);
        } else {
          asyncRequest.resolver(result || messageBody.data);
        }
        /** @unregister_async_request */
        _.remove(this.asyncRequestList, req => req.id === asyncRequest.id);
      }
    }
  }

  // eslint-disable-next-line @typescript-eslint/no-unused-vars, @typescript-eslint/no-empty-function
  onErrorMessage (ex: ZetaException) {}
  send (op: string, data: any) {
    const destination = OPEndpoints[op];
    this.client.publish({
      destination,
      body: JSON.stringify(data),
    });
  }
  sendWithQueue (op: string, data: any) {
    if (this.client && this.client.connected) {
      this.send(op, data);
    } else {
      const destination = OPEndpoints[op];
      this.msgQueue.push({
        destination,
        body: JSON.stringify(data),
      });
    }

  }

  sendAsync (op: string, data: any, options: AsyncWSMessageOption) {
    this.send(op, data);

    const timeout = options.timeout;
    const requestKey = uuid();
    const promise = new Promise((resolve, reject) => {
      let resolver = resolve;
      if (timeout) {
        const to = setTimeout(() => {
          reject('timeout');
        }, timeout);
        resolver = (m: any) => {
          clearTimeout(to);
          resolve(m);
        };
      }

      const opts: AsyncWSRequest = {
        id: requestKey,
        rejecter: reject,
        resolver,
        opAlias: options.opAlias,
        mapping: options.mapping,
      };
      this.asyncRequestList.push(opts);
    });

    return promise;
  }

  greating () {
    this.client.publish({
      destination: '/wsapp/id',
      body: JSON.stringify({}),
    });
  }
}
