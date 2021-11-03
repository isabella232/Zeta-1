import { TaskBasic, Task, ReleasedTask, DbcTable } from '@/components/AutoEL/types';
import config from '@/config/config';
import BaseRemoteService from '@/services/remote/BaseRemoteService';
import _ from 'lodash';

const BASE_URL = config.doe.doeService;

export default class AutoELService extends BaseRemoteService {
  static readonly instance = new AutoELService();

  getTaskList (): Promise<TaskBasic[]> {
    return this.get(`${BASE_URL}auto_etl/getTaskList`).then(resp => resp.data.data);
  }
  createTask (task: Task) {
    return this.post(`${BASE_URL}auto_etl/task`, task).then(resp => resp.data.data);
  }
  updateTask (task: Task) {
    return this.put(`${BASE_URL}auto_etl/task`, task).then(resp => resp.data.data);
  }
  checkApprovalRequirements (id: number) {
    return this.get(`${BASE_URL}auto_etl/checkApprovalRequirements?id=${id}`).then(resp => resp.data.data);
  }
  sendApprovalRequest (id: number | string) {
    return this.post(`${BASE_URL}auto_etl/setMailSent?id=${id}`).then(resp => resp.data);
  }
  approve (payload: { id: number; role: string; approved?: boolean }) {
    if (payload.approved === undefined) {
      payload.approved = true;
    }
    return this.put(`${BASE_URL}auto_etl/approve`, payload).then(resp => resp.data.data);
  }
  releaseTask (id) {
    return this.post(`${BASE_URL}auto_etl/release?task_id=${id}`).then(resp => resp.data);
  }
  cancelTask (id) {
    return this.post(`${BASE_URL}auto_etl/cancelRelease?id=${id}`).then(resp => resp.data);
  }
  deleteTaskById (id: number) {
    return this.delete(`${BASE_URL}auto_etl/removeTaskById?id=${id}`);
  }
  getTaskById (id: string | number): Promise<ReleasedTask> {
    return this.get(`${BASE_URL}auto_etl/getTaskById?id=${id}`).then(resp => resp.data.data);
  }
  getHadoopLocation (sa: string, db: string, table_name: string) {
    const params = { sbjct_area: sa, db, table_name };
    return this.get(`${BASE_URL}auto_etl/getTableLocation`, { params }).then(resp => resp.data.data);
  }
  getSAList () {
    return this.get(`${BASE_URL}metadata/SAList`).then(resp => resp.data);
  }
  getSourceTables (query: string) {
    return this.get(`${BASE_URL}autoDA/searchOracleTable?name=` + query);
  }
  getSourceTableInfo (table: string) {
    return this.get(`${BASE_URL}auto_etl/getTableInfo?table_name=` + table).then(resp => resp.data.data);
  }
  getTableSize (table: string, tableList: DbcTable[]) {
    const body = { sa_code: table, table_list: tableList };
    return this.post(`${BASE_URL}auto_etl/tableSize`, body)
      .then(resp => resp.data.data)
      .catch(() => ({ total_table_size: -1 }));
  }
  getTableSchema (table: string) {
    return this.get(`${BASE_URL}auto_etl/getOracleTableDDL?table_name=` + table).then(resp => resp.data.data);
  }
  getTableSample (table: string, dbcName: string, saCode?: string) {
    return this.get(`${BASE_URL}auto_etl/getOracleExampleData?table_name=${table}&dbc_name=${dbcName}&sa_code=${saCode}`).then(
      resp => resp.data.data
    );
  }
  getReleaseStatus (id: string, table: string) {
    return this.get(`${BASE_URL}auto_etl/releaseStatus?table_name=${table}&id=${id}`).then(resp => resp.data.data);
  }
  updateReleaseStatus (id: string, table: string) {
    return this.post(`${BASE_URL}auto_etl/updateReleaseStatus?id=${id}&table=${table}`).then(resp => resp.data.data);
  }
  getRecentJobs (id: string) {
    return this.get(`${BASE_URL}auto_etl/getHistory?task_id=${id}`).then(resp => {
      return _.flatMap(resp.data.data, l => l.list.map(i => { i.platform = l.platform; return i; }));
    });
  }
  getDDL (id: string): Promise<string> {
    return this.get(`${BASE_URL}auto_etl/getDDL?task_id=${id}`).then(resp => resp.data.data);
  }
  getUpsertScript (id): Promise<string> {
    return this.get(`${BASE_URL}auto_etl/getUpsertSql?task_id=${id}`).then(resp => resp.data.data.upsert_script);
  }
  getSuggestedNames (names: string[]) {
    return this.post(`${BASE_URL}autoDA/suggestNames`, names).then(resp => resp.data.value);
  }
  getSuggestedTypes (types: string[]) {
    return this.post(`${BASE_URL}autoDA/convertTypes`, types).then(resp => resp.data.value);
  }
  getDataModel (id) {
    return this.get(`${BASE_URL}auto_etl/getDataModel?id=${id}`).then(resp => resp.data.data);
  }
  saveDataModel (id, model) {
    return this.post(`${BASE_URL}auto_etl/saveDataModel?id=${id}`, model);
  }
  getJobInfoByTable (table: string) {
    return this.get(`${BASE_URL}auto_etl/getJobInfoByTable?table=${table}`).then(resp => resp.data.data);
  }
  getDoneFile (saCode: string, table: string) {
    return this.get(`${BASE_URL}autoDA/generateDoneFileName?sa=${saCode}&table=${table}`).then(
      resp => resp.data.value
    );
  }
  getReleaseModel (id: string) {
    return this.get(`${BASE_URL}auto-etl/getReleaseModel?task_id=${id}`).then(resp => resp.data.data);
  }
  getTableOnPlatform (table, db) {
    return this.get(`${BASE_URL}auto_etl/checkTableOnPlatform?db=${db}&table=${table}`).then(resp => resp.data.data);
  }
  getDBCInfo (dbc: string) {
    return this.get(`${BASE_URL}auto_etl/dbcInfo?dbc_name=${dbc}`)
      .then(resp => {
        return {
          ...resp.data.data,
          name: dbc,
        };
      })
      .catch(e => {
        e.resolve();
        return null;
      });
  }
  validateTable (table_list: { table_name: string; dbc_name: string }[], sa_code: string) {
    const body = {
      sa_code,
      table_list,
    };
    return this.post(`${BASE_URL}auto_etl/validateTable`, body).then(resp => resp.data.data);
  }
  getHadoopQueue (batchAccount: string) {
    return this.get(`https://bdp-site.vip.ebay.com/product/queue/user/apollorno/${batchAccount}`).then(resp => resp.data.result);
  }
}
