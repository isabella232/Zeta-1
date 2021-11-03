import Util from '@/services/Util.service';
import DssAPI from '@/services/remote/dss-api-remote-service';
import config from '@/config/config';
const baseUrl = config.zeta.base;
export default class ZetaAceRemoteService extends DssAPI {
  get aceAdmin (){
    return Util.getApp().$store.state.user.user.aceAdmin;
  }
  aceSearch (query: string, scopes = 'table,faq,udf', size = 20) {
    return this.get(`${baseUrl}ace/search?query=${encodeURIComponent(query)}&scopes=${scopes}&size=${size}`);
  }
  queryNotebook (query: string, scopes = 'query', size = 20) {
    return this.get(`${baseUrl}ace/search?query=${encodeURIComponent(query)}&scopes=${scopes}&size=${size}`);
  }
  aceHint (query: string) {
    return this.get(`${baseUrl}ace/hint?query=${encodeURIComponent(query)}`);
  }
  googleSearch (query: string, stop = 3) {
    const queryStr = encodeURIComponent(`${query} site:databricks.com`);
    return this.get(`${config.zeta.google}search?query=${queryStr}&stop=${stop}`);
  }
  addQuestion (title: string, content?: string) {
    return this.post(`${baseUrl}ace/questions`, {
      title,
      content,
    });
  }
  updateQuestion (id: number, title: string, content: string) {
    return this.post(`${baseUrl}ace/${this.aceAdmin?'admin/':''}questions`, {
      id,
      title,
      content,
    });
  }
  getQuestion (qid: number) {
    return this.get(`${baseUrl}ace/question/${qid}`);
  }
  getQuestionsByIds (qids: string) {
    return this.get(`${baseUrl}ace/v1/question/${qids}`);
  }
  deleteQuestion (qid: number) {
    return this.delete(`${baseUrl}ace/${this.aceAdmin?'admin/':''}question/${qid}`);
  }
  getAllQuestion (scope = 'all', sortType = 'updateTime', size = 10, page = 1) {
    return this.get(`${baseUrl}ace/questions?scope=${scope}&sortType=${sortType}&size=${size}&page=${page}`);
  }
  getDefaultQuestion (pickUp = true) {
    return this.get(`${baseUrl}ace/questions?pickUp=${pickUp}`);
  }
  reply (qid: number, comment: string, replyTo?: number) {
    return this.post(`${baseUrl}ace/question/${qid}/posts`, {
      comment,
      replyTo,
    });
  }
  updateReply (qid: number, pid: number, comment: string) {
    return this.post(`${baseUrl}ace/${this.aceAdmin?'admin/':''}question/${qid}/posts`, {
      id: pid,
      comment,
    });
  }
  deletePost (qid: number, pid: number) {
    return this.delete(`${baseUrl}ace/${this.aceAdmin?'admin/':''}question/${qid}/post/${pid}`);
  }
  questionVote (qid: number, value: number) {
    return this.put(`${baseUrl}ace/question/${qid}/vote`, {
      value,
    });
  }
  postVote (qid: number, pid: number, value: number) {
    return this.put(`${baseUrl}ace/question/${qid}/post/${pid}/vote`, {
      value,
    });
  }
  setAccepted (qid: number, pid: number){
    return this.put(`${baseUrl}ace/admin/question/${qid}/post/${pid}/accepted`);
  }
  cancelAccepted (qid: number, pid: number){
    return this.delete(`${baseUrl}ace/admin/question/${qid}/post/${pid}/accepted`);
  }
  createEmail (params: any) {
    return this.post('https://zeta.dss.vip.ebay.com/defensor/Email/create', params);
  }
  //enotify
  getEnotifyTypes () {
    return this.get(`${baseUrl}ace/enotifies/types`);
  }
  getEnotifyProducts () {
    return this.get(`${baseUrl}ace/enotifies/products`);
  }
  getEnotify (products: string, after?: string, type?: string, ) {
    let url = `${baseUrl}ace/enotifies?products=${products}&after=${after}`;
    if (type) {
      url += `&type=${type}`;
    }
    return this.get(url);
  }
  readEnotify (ids: Array<number>) {
    return this.put(`${baseUrl}ace/enotifies/read`, {
      'enotifyIds': ids,
    });
  }
  attachments (file: any, onUploadProgress: (e: ProgressEvent) => void){
    const formData = new FormData();
    formData.append('file', file);
    const config = {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      onUploadProgress,
    };
    return this.post(`${baseUrl}attachments`, formData,  config);
  }
  //tags
  getAllTags (nameStart ='', page = 0, size = 100){
    return this.get(`${baseUrl}ace/tags?page=${page}&size=${size}&nameStart=${nameStart}`);
  }
  getTags (ids: string){
    return this.get(`${baseUrl}ace/tag/${ids}`);
  }
  getHotTags (page = 0, size = 100){
    return this.get(`${baseUrl}ace/hottags?page=${page}&size=${size}`);
  }
  addTag (name: string, desc?: string){
    return this.post(`${baseUrl}ace/tags`, {
      name,
      description: desc,
    });
  }
  getTagsByQids (qids: string){
    return this.get(`${baseUrl}ace/question/${qids}/tags`);
  }
  getQuestionIdsByTid (tid: number, page = 1, size = 20){
    return this.get(`${baseUrl}ace/tag/${tid}/questions?page=${page}&size=${size}`);
  }
  addTagsToQuestion (qid: number, tids: string){
    return this.put(`${baseUrl}ace/${this.aceAdmin?'admin/':''}question/${qid}/tags/${tids}`);
  }
  removeTagsFromQuestion (qid: number, tids: string){
    return this.delete(`${baseUrl}ace/${this.aceAdmin?'admin/':''}question/${qid}/tags/${tids}`);
  }

  // Alation article migration
  getFaqId (str: string){
    return this.get(`${baseUrl}ace/alation/article/${str}`);
  }
  getQuery (str: string){
    return this.get(`${baseUrl}ace/alation/query/${str}`);
  }
}
