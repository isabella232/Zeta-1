import config from '@/config/config';
import DssAPI from '@/services/remote/dss-api-remote-service';

export default class DataValidationRemoteService extends DssAPI{
  getColumns (platform: string, db: string, table: string){
    return this.get(`${config.zeta.base}DataMover/getColumns?platform=${platform}&dbName=${db}&tblName=${table}`);
  }
  history (nt: string){
    return this.get(`${config.zeta.base}DataValidation/history?nt=${nt}`);
  }
  getDetail (historyId: number){
    return this.get(`${config.zeta.base}DataValidation/history/detail?historyId=${historyId}`);
  }
  validate (nt: string, src: DataValidation.TDTable, tgt: DataValidation.HerculesTable){
    return this.post(`${config.zeta.base}DataValidation/validate`, {
      sourceFilter: src.filter ? src.filter : '',
      targetFilter: tgt.filter ? tgt.filter : '',
      sourceBatchAccount:src.batchAccount ? src.batchAccount : '',
      targetBatchAccount:tgt.batchAccount ? tgt.batchAccount : '',
      history: {
        nT: nt,
        sourceTable: src.table,
        sourcePlatform: src.source,
        targetTable: tgt.table,
        targetPlatform: tgt.source
      }
    });
  }
}
