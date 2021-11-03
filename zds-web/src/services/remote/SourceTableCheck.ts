import config from '@/config/config';
import DssAPI from '@/services/remote/dss-api-remote-service';
// import * as TableCheckAPI from "@/components/Notebook/Tools/TableCheck/SourceTableCheck";

export default class TableCheckRemoteService extends DssAPI {

  getTables (sql: string){
    return this.post(`${config.zeta.base}SQLConvert/getSparkTableSource`, { Request: 'request', sql: sql });
  }
  getSourceTableCheck (tables: SourceTableCheck.TableInfoBase[]){
    return this.post(`${config.zeta.base}SourceTableCheck/getSourceTableCheck`, tables);
  }
  submitRequest (dbName: string, tblName: string, template: string){
    return this.post(`${config.zeta.base}SourceTableCheck/dailyCopyAction`, {
      dbName: dbName,
      tblName: tblName,
      content: template
    });
  }
}
