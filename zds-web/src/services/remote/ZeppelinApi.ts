import config from '@/config/config';
import { ZPLNote } from '@/types/workspace/zeppelin.internal';
import _ from 'lodash';
import { ZetaException } from '@/types/exception';
import BaseRemoteService from './BaseRemoteService';
import * as Axios from 'axios';
import Util from '@/services/Util.service';
const BASE_URL = config.zeta.zeppelin.api + 'api/notebook/';
function ignoreException (ex: ZetaException) {
  throw ex.resolve();
}
export default class ZeppelinApi extends BaseRemoteService {
  protected generateConfig (config?: Axios.AxiosRequestConfig | undefined, data?: any, original = false): Axios.AxiosRequestConfig {
    const defaultConfig = super.generateConfig(config, data);
    if (!original) {
      defaultConfig.headers['authorization'] = Util.getNt();
      defaultConfig.withCredentials = true;
    }
    return defaultConfig;
  }
  getNotes () {
    return this.get(BASE_URL);
  }
  createNote (note: Partial<ZPLNote>) {
    return this.post(BASE_URL, note);
  }
  delNote (id: string) {
    return this.delete(`${BASE_URL}${id}`, {});
  }
  openNote (id: string) {
    return this.put(`${BASE_URL}${id}/opened/1`, {}).catch(ignoreException);
  }
  closeNote (id: string) {
    return this.put(`${BASE_URL}${id}/opened/0`, {}).catch(ignoreException);
  }
  noteSeq (note: ZPLNote) {
    return this.put(`${BASE_URL}${note.id}/sequences/${note.seq}`, {}).catch(ignoreException);
  }
  noteSeqs (note: Dict<number>) {
    return this.put(`${BASE_URL}sequences`, note).catch(ignoreException);
  }
  renameNote (id: string, name: string) {
    return this.put(`${BASE_URL}${id}`, { name });
  }
  removeFolder (folderId: string) {
    return this.delete(`${BASE_URL}folder/${encodeURIComponent(folderId)}`, { });
  }
  publish (id: string, body: any) {
    return this.post(`${BASE_URL}${id}/publish`, _.isEmpty(body) ? {}: body);
  }
  clone (id: string, body: any) {
    return this.post(`${BASE_URL}${id}`, _.isEmpty(body) ? {}: body);
  }
  getPackages (id: string, clusterId: number, path: string) {
    return this.get(`${BASE_URL}${id}/${clusterId}/packages?path=${encodeURIComponent(path)}`);
  }
  updatePackages (id: string, body: any) {
    return this.put(`${BASE_URL}${id}/packages/apply`, body);
  }
  getPublicNotes () {
    return this.get(`${BASE_URL}publish`,);
  }
  getNotesBrief (ids: string) {
    return this.get(`${BASE_URL}notesBrief/${ids}`,);
  }
  getTicket () {
    return this.get(`${config.zeta.zeppelin.api}api/security/ticket`,);
  }
}
