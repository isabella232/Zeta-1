import { ZPLMessage, ZPLAction, ZPLMessageHandler } from "@/types/workspace";
import _ from "lodash";
function validateMessage(ev: MessageEvent) {
    const data = ev.data;
    if (!data || typeof data !== 'object') {
        return false;
    }
    const keys = Object.keys(data);
    if (!_.includes(keys, 'action') || !_.includes(keys, 'noteId')) {
        return false;
    }
    return true;
}
export default class ZeppelinMessageHandler {
    private msgHandlers: Dict<ZPLMessageHandler | null> = {};
    public register = () => {
        window.addEventListener('message', this.listener);
        this.msgHandlers = {};
        return this;
    }

    public unregister = () => {
        window.removeEventListener('message', this.listener);
    }

    public $on = (action: ZPLAction, handler: ZPLMessageHandler) => {
        this.msgHandlers[action] = handler;
        return this;
    }
    public $off = (action: ZPLAction) => {
        this.msgHandlers[action] = null;
        return this;
    }

    private listener = (ev: MessageEvent) => {
        if (!validateMessage(ev)) {
            console.debug('invalid message', ev);
            return;
        }
        const body = ev.data as ZPLMessage;
        this.dispatcher(body);
    }
    private dispatcher = (msg: ZPLMessage) => {
        console.debug('handle zpl message', msg);
        const action = msg.action;
        const handler = this.msgHandlers[action];
        if (handler) {
            handler(msg);
        } else {
            console.warn('unregister action handler: ' + action);
        }
    }
}