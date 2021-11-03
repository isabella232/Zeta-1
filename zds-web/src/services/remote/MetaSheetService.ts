import DssAPI from '@/services/remote/dss-api-remote-service';
import { MetaSheetTableItem } from '@/types/meta-sheet';
import config from '@/config/config';
import Util from '../Util.service';
import * as Axios from 'axios';
const baseUrl = config.zeta.base;
const generateConfig = (config?: Axios.AxiosRequestConfig | undefined, data?: any, original = false): Axios.AxiosRequestConfig  => {
  const defaultConfig = {
    headers: {}
  };
  config = Object.assign(defaultConfig, config);
  // set data [DIFF]: below is different with BaseRemoteService.
  if(data) {
    config.data = data;
  }
  // set token
  if(!original) {
    config.headers.Authorization = 'Bearer ' + Util.getZetaToken();
  }
  return config;
};

export default class MetaSheetApi extends DssAPI{

  getMetaSheets() {
    return this.get(`${baseUrl}MetaTable/list`);
  }

  createMetaSheet (item: MetaSheetTableItem) {
    return this.post(`${baseUrl}MetaTable/create`, item);
  }

  updateMetaSheetById (id: string, item: any) {
    return this.put(`${baseUrl}MetaTable/update/${id}`, item);
  }

  grantUserAccess (id: string, item: any) {
    return this.put(`${baseUrl}MetaTable/grant/${id}`, item);
  }

  deleteMetaSheet (id: string) {
    return this.delete(`${baseUrl}MetaTable/delete/${id}`);
  }

  getMetaSheetData (id: string) {
    return this.get(`${baseUrl}MetaTable/${id}/info`);
  }

  getZetaSheetData (id: string) {
    return this.get(`${baseUrl}MetaTable/share/${id}`);
  }

  editMetaSheetData (id: string, operations: any) {
    return this.put(`${baseUrl}MetaTable/edit/${id}`, operations);
  }

  updateNotebookSeq (notebookId: string, seq: number) {
    return this.put(`${baseUrl}Workspace/${notebookId}/seq/${seq}`);
  }

  syncToTarget(id: string, data:any, config?: Axios.AxiosRequestConfig | undefined, canceler?: Axios.Canceler, original = false) {
    config = generateConfig(config,data,original);
    config.method = 'PUT';
    return this.request(`${baseUrl}MetaTable/sync/${id}`, config, canceler);
  }

  getSyncStatus (id: string) {
    return this.get(`${baseUrl}MetaTable/sync/status/${id}`);
  }

  getOpened () {
    return this.get(`${baseUrl}Workspace/getOpendNote`);
  }

  getWhiteList () {
    return this.get(`${baseUrl}MetaTable/whitelist`);
  }

  applyForAccess (nt: string, id: string) {

    return this.put(`${baseUrl}MetaTable/applyAccess/${nt}/${id}`);
  }

  updatePath(operations: any) {
    return this.put(`${baseUrl}MetaTable/updatePath`, operations);
  }

}
