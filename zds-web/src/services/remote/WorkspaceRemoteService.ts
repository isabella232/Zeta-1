import config from '@/config/config';
import { WorkspaceBase } from '@/types/workspace';
import DssAPI from './dss-api-remote-service';
const baseUrl = config.zeta.base;
export default class WorkspaceRemoteService extends DssAPI {
  getOpened () {
    return this.get(`${baseUrl}workspace/getOpenedNote`);
  }
  updateNotebookSeq (ws: WorkspaceBase) {
    return this.put(`${baseUrl}workspace/${ws.notebookId}/${ws.type}/seq/${ws.seq}`);
  }
  updateNotebooksSeq (wss: Array<any>) {
    return this.put(`${baseUrl}workspace/seqs`, wss);
  }

}
