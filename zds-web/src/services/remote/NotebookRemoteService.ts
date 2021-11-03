import DssAPI from '@/services/remote/dss-api-remote-service';
import config from '@/config/config';
import { RestPacket } from '@/types/RestPacket';
import Util from '../Util.service';
import _ from 'lodash';
import { IPreference } from '@/types/workspace';
const baseUrl = config.zeta.base;
const doeBaseUrl = config.doe.doeService;
export default class NotebookRemoteService extends DssAPI {
  /**
   * @deprecated
   */
  getOpened () {
    return this.get(`${baseUrl}notebooks/opened`);
  }
  /**
   * @deprecated
   * @param notebookId
   * @param seq
   */
  updateNotebookSeq (notebookId: string, seq: number) {
    return this.put(`${baseUrl}notebooks/${notebookId}/seq/${seq}`);
  }
  /**
   * @deprecated
   * @param seqMap
   */
  updateNotebooksSeq (seqMap: Dict<number>) {
    return this.put(`${baseUrl}notebooks/seqs`, seqMap);
  }
  closeNotebook (notebookId: string) {
    return this.put(`${baseUrl}notebooks/closed/${notebookId}`);
  }
  getById (notebookId: string) {
    return this.get(`${baseUrl}notebooks/${notebookId}?opened=1`);
  }
  getReadOnlyById (notebookId: string) {
    return this.get(`${baseUrl}notebooks/${notebookId}/readOnly`);
  }
  getCfgById (notebookId: string) {
    return this.get(`${baseUrl}notebooks/${notebookId}`);
  }
  getAll () {
    return this.get(`${baseUrl}notebooks/nt/${Util.getNt()}`);
  }
  add (nb: RestPacket.File) {
    return this.post(`${baseUrl}notebooks`, nb);
  }
  save (nb: RestPacket.File) {
    return this.put(`${baseUrl}notebooks`, nb);
  }
  del (id: string) {
    return this.delete(`${baseUrl}notebooks/${id}`);
  }
  //old api
  rename (nb: Partial<RestPacket.File>) {
    return this.put(`${baseUrl}notebooks`, nb);
  }
  //updated es
  // rename(nb: Partial<RestPacket.File>){
  //   return this.put(`${baseUrl}notebooks/${nb.id}/move`,nb);
  // }
  changeCodeType (nb: Partial<RestPacket.File>) {
    return this.put(`${baseUrl}notebooks`, nb);
  }
  getHistory (id: string) {
    return this.get(`${baseUrl}notebooks/${id}/history`);
  }
  getMultiNotebookHistory (id: string) {
    return this.get(`${baseUrl}notebooks/${id}/multinotebook/history`);
  }
  getQuery (id: string) {
    return this.get(`${baseUrl}statements/${id}`);
  }

  savePreference (notebookId: string, preference: IPreference, isPublic = false){
    const url = `${baseUrl}` + (isPublic ? `pub_notebooks/${notebookId}`: `notebooks/${notebookId}/preference`);
    return this.put(url, preference);
  }
  downloadNotebooks (notebookId: string) {
    return this.get(`${baseUrl}notebooks/downloadNotebook?ids=${notebookId}`, {responseType: 'blob'});
  }
  getClusterStatus(notebookId: string, clusterId: number) {
    return this.get(`${baseUrl}monitor/status/queue/notebookId/${notebookId}?clusterId=${clusterId}&nt=${Util.getNt()}`);
  }
  getClusterQueue(clusterId: number, queueName: string) {
    return this.get(`${baseUrl}monitor/status/queue/${clusterId}/${queueName}`);
  }

  createScheduleTask(data: any) {
    return this.post(`${baseUrl}Scheduler/create`,data);
  }
  updateScheduleTask(data: any) {
    return this.put(`${baseUrl}Scheduler/update`,data);
  }
  getScheduleTaskList() {
    return this.get(`${baseUrl}Scheduler/getJobList`);
  }
  grantUserAccess(taskId: number, params: any){
    return this.put(`${baseUrl}Scheduler/grant/${taskId}`, params);
  }
  getScheduleHistoryById(taskId: number) {
    return this.get(`${baseUrl}Scheduler/getHistory/${taskId}`);
  }
  scheduleJobOps(taskId: number, Ops: string, runId: number){
    return this.put(`${baseUrl}Scheduler/${taskId}/${Ops}?runId=${runId}`);
  }
  getJobTrackLog(runId: number) {
    return this.get(`${baseUrl}Scheduler/getTrackLog/${runId}`);
  }
  getTodayRun(){
    return this.get(`${baseUrl}Scheduler/getTodayRunList`);
  }
  getJobListByIds(ids: string){
    return this.get(`${baseUrl}Scheduler/getJobListById?id=${ids}`);
  }
  applyForAccess(nt: string, jobId: number){
    return this.put(`${baseUrl}Scheduler/applyAccess/${nt}/${jobId}`);
  }
  getScheduleHistory() {
    return this.get(`${baseUrl}Scheduler/history?nt=${Util.getNt()}`);
  }
  skipPending(id: number){
    return this.put(`${baseUrl}Scheduler/${id}/skip`);
  }
  getScheduleNotebookHistory(reqId: string) {
    return this.get(`${baseUrl}notebooks/${reqId}/reqhistory`);
  }
  getPublicNotebook(id: string){
    return this.get(`${baseUrl}pub_notebooks/ref/${id}`);
  }
  createPublicNotebook(id: string){
    return this.post(`${baseUrl}pub_notebooks/ref/${id}`);
  }
  getAllPublicNotebook(){
    return this.get(`${baseUrl}pub_notebooks/`);
  }
  getCloneById(notebookId: string): Promise<any> {
    return this.get(`${baseUrl}notebooks/${notebookId}/clone`);
  }

  getUserQueue(clusterName: string, account: string) {
    return this.get(`${baseUrl}monitor/info/user_queue_access/${clusterName}/${account}`);
  }
  dumpFile (notebookId: string, interpreter: string, reqId: string): Promise<any> {
    return this.get(`${baseUrl}statements/dump/${notebookId}/${interpreter}/${reqId}`, {responseType: 'blob'});
  }
  getKylinProject(payload: any) {
    return this.post(`${baseUrl}kylin/readable_projects`, payload);
  }

  getAccessClusters(clusterName: string) {
    return this.get(`${baseUrl}users/me/${clusterName}`);
  }
  removeFolder(path: string) {
    return this.delete(`${baseUrl}notebooks/folders`, { folders: [ path ], recursive: true });
  }
  clearResult(notebookId: string) {
    return this.get(`${baseUrl}notebooks/status/clear/note/${notebookId}`);
  }
  // =========== apply package ============================
  apply(notebookId: string, params: any){
    return this.put(`${baseUrl}notebooks/${notebookId}/packages`, params);
  }
  // ===================== release =======================
  getReleaseCheckStatus(releaseTag: string, issueName: string): Promise<any> {
    return this.get(`${baseUrl}Release/checkStatus?releaseTag=${releaseTag}&issueName=${issueName}`);
  }
  getReleaseStatus(releaseTagName: string): Promise<any> {
    return this.get(`${baseUrl}Release/status/${releaseTagName}`);
  }
  getReleaseHistory(): Promise<any> {
    return this.get(`${baseUrl}Release/history`);
  }
  submitManifest(params: any): Promise<any> {
    return this.post(`${baseUrl}Release/manifest`, params);
  }
  checkManifest(tag: string): Promise<any> {
    return this.get(`${baseUrl}Release/releaseRecord/${tag}`);
  }
  getAbinitioManifest(tag: string): Promise<any> {
    return this.get(`${baseUrl}Release/abinitioManifest/${tag}`);
  }
  sendManifestValidate(params: any): Promise<any> {
    return this.post(`${baseUrl}Release/sendValidateMail`, params);
  }
  rollout(params: any): Promise<any> {
    return this.post(`${baseUrl}Release/rollout`, params);
  }
  subscribe(id: any) {
    return this.get(`${baseUrl}mail/subscribe?subscribeId=${id}`);
  }
  unsubscribe(id: any) {
    return this.get(`${baseUrl}mail/unsubscribe?nt=${Util.getNt()}&unsubscribeId=${id}`);
  }
  getUnsubscribeList() {
    return this.get(`${baseUrl}mail/unsubscribelist?nt=${Util.getNt()}`);
  }
  getSummaryByIds(ids: string){
    return this.get(`${baseUrl}share/openNotebookSummary?ids=${ids}`);
  }
  // favorite
  favoriteList() {
    return this.get(`${baseUrl}notebooks/favorite`);
  }
  favoriteShareNotebooks() {
    return this.get(`${baseUrl}notebooks/favoriteShareNotebook`);
  }
  favoriteNotebook(id: string, type = 'nb', status = true) {
    return this.put(`${baseUrl}notebooks/${id}/${type}/${status ? 'favorite' : 'unFavorite'}`);
  }
  // unfavoriteNotebook(id: string) {
  //   return this.put(`${baseUrl}notebooks/${id}/nb/unFavorite`);
  // }
  // favoritePublicNotebook(id: string) {
  //   return this.put(`${baseUrl}notebooks/${id}/pub_nb/favorite`);
  // }
  // unfavoritePublicNotebook(id: string) {
  //   return this.put(`${baseUrl}notebooks/${id}/pub_nb/unFavorite`);
  // }

  // ===== Notebook Scheduler Dependency
  getNotebookSchedulerDependencyItems(nid: string, clusterId: string) {
    return this.get(`${baseUrl}notebooks/${nid}/getSourceTables/${clusterId}`);
  }
  // single table
  getNotebookSchedulerDependencyTableCheck(platform: string, db: string, table: string) {
    return this.get(`${doeBaseUrl}dependency/checkTable?platform=${platform}&db=${db}&table=${table}`);
  }
  //batch query table
  queryNotebookSchedulerDependencyTablesCheck(list: Array<any>) {
    return this.post(`${doeBaseUrl}dependency/checkTables`, list);
  }
  queryTable(table: string, platform: any, size = 20){
    return this.get(`${doeBaseUrl}dependency/getTableList?table=${table}&platform=${platform}&size=${size}`);
  }
  isVDMOwner(nt: string, pltfrm_name: any, full_name: string){
    const params = {
      nt,
      pltfrm_name,
      full_name,
    };
    return this.post(`${doeBaseUrl}vdm/isVDMOwner`, params);
  }
  syncDependencySignal(params: Array<any>){
    return this.post(`${doeBaseUrl}vdm/setVDMScheduling`, params);
  }
  getDependencyTables(params: any){
    return this.post(`${doeBaseUrl}dependency/tablesFinishtime`, params);
  }
  //========= Notebook Add Metadata config =====
  getNotebookMetaConfig(nid: string){
    return this.get(`${baseUrl}notebooks/meta/${nid}`);
  }
  addNotebookMetaConfig(param: any){
    const params = _.assign({isPublic: 0, type: 'zeta'}, param);
    return this.post(`${baseUrl}notebooks/meta`, params);
  }
  updateNotebookMetaConfig(param: any){
    const params = _.assign({isPublic: 0, type: 'zeta'}, param);
    return this.put(`${baseUrl}notebooks/meta`, params);
  }
  deleteNotebooksMetaConfig(nids: Array<string>){
    return this.delete(`${baseUrl}notebooks/metas`, nids);
  }
  delNotebookMetaConfig(nid: string){
    return this.delete(`${baseUrl}notebooks/meta/${nid}`);
  }
  deleteScheduler(taskId: number) {
    return this.delete(`${baseUrl}Scheduler/delete/${taskId}`);
  }
  // ================== share ====================
  getSharedNotebook(nId: string){
    return this.get(`${baseUrl}share/${nId}`,undefined,undefined, true);
  }
  getSharedNotebookHistory = (nId: string) => {
    return this.get(`${baseUrl}share/${nId}/history`,undefined,undefined, true);
  };
  getShareStatement = (id: string) => {
    return this.get(`${baseUrl}share/statements/${id}`,undefined,undefined, true);
  };
  favoriteSharedNotebook(id: string, status = true, nt: string, type = 'share_nb') {
    return this.get(`${baseUrl}share/${status ? 'favorite' : 'unfavorite'}/${type}/${id}?nt=${nt}`,undefined,undefined, true);
  }
  favoriteShareList(nt: string) {
    return this.get(`${baseUrl}share/favorite?nt=${nt}`,undefined,undefined, true);
  }
}
