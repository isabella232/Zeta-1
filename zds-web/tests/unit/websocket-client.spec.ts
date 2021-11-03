import Util from "@/services/Util.service";
import { spy, stub } from 'sinon';
describe('websocket client', () => {
    before('stub functions', () => {
        stub(Util, 'getZetaToken').returns('zetaToken');
        stub(Util, 'generateSessionId').returns('sessionId');
    })
});