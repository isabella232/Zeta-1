import config from '@/config/config';
import DssAPI from '@/services/remote/dss-api-remote-service';
export default class SqlConverterRemoteService extends DssAPI {
  saveToNotebook (nt: string, tableName: string, fileLists: any){
    const url = `${config.zeta.base}SQLConvert/saveToNotebook`;
    return this.post(url, {nt, tableName, fileLists});
  }
  findFDTWithSQLScripts (platform: string, dbName: string, tblName: string){
    const url = `${config.zeta.base}metadata/findFDTWithSQLScripts?platform=${platform}&dbName=${dbName}&tblName=${tblName}`;
    return this.get(url);
  }
  getTdTableSource (sqlList: any[]){
    const url = `${config.zeta.base}SQLConvert/getTdTableSource`;
    return this.post(url, sqlList);
  }
  manualconvertsql (SQL: string){
    const url = `${config.zeta.base}SQLConvert/manualconvertsql`;
    return this.post(url, { 'Request': 'request', 'sql': SQL });
  }
  convertsql (scriptList: any[]){
    const url = `${config.zeta.base}SQLConvert/convertsql`;
    return this.post(url, scriptList);
  }
  convert (tableInfo: any){
    const url = `${config.zeta.base}SQLConvert/convert`;
    return this.post(url, tableInfo);
  }
  getDDL (tableList: any[]){
    const url = `${config.zeta.base}SQLConvert/getDDL`;
    return this.post(url, tableList);
  }
  getTableJson () {
    return this.get(`${config.doe.doeService}metadata/getTableJSON`, {
      headers: {
        'Cache-Control': 'public',
      },
    });
  }
  getColumnJson (param: any) {
    let paramStr = '';
    if (param.batchSize != undefined && param.offset != undefined) {
      paramStr = 'batchSize=' + param.batchSize + '&offset=' + param.offset;
    } else {
      paramStr = (param.batchSize ? 'batchSize=' + param.batchSize : '') + (param.offset ? 'offset=' + param.offset : '');
    }

    return this.get(`${config.doe.doeService}metadata/getColumnJSON` + (paramStr.length > 0 ? '?' + paramStr : ''), {
      headers: {
        'Cache-Control': 'public',
      },
    });
  }
  getSpecColumnJSON (tableName: string) {
    return this.get(`${config.doe.doeService}metadata/getSpecColumnJSON?tableName=` + tableName, {
      headers: {
        'Cache-Control': 'public',
      },
    });
  }
}
