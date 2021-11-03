import config from '@/config/config';
import DssAPI from '@/services/remote/dss-api-remote-service';

export default class DataMoveRemoteService extends DssAPI {
  getColumns (platform: string, db: string, table: string){
    return this.get(`${config.zeta.base}DataMover/getColumns?platform=${platform}&dbName=${db}&tblName=${table}`);
  }
  history (nt: string){
    return this.get(`${config.zeta.base}DataMover/history?nt=${nt}`);
  }
  getDetail (historyId: number){
    return this.get(`${config.zeta.base}DataMover/history/detail?historyId=${historyId}`);
  }
  move (nt: string, src: DataMove.TDTable, tgt: DataMove.HerculesTable, isDrop: number, isConvert: number){
    return this.post(`${config.zeta.base}DataMover/move`, {
      filter: src.filter? src.filter : '',
      history: {
        nT: nt,
        sourceTable: src.fullTableName,
        sourcePlatform: src.source,
        targetTable: tgt.srcDB+'.'+tgt.table,
        targetPlatform: tgt.source,
      },
      isDrop:isDrop,
      isConvert:isConvert,
    });
  }
  upload (fileList: any) {
    const formData = new FormData();
    formData.append('file', fileList[0].raw);
    formData.append('fileType', fileList[0].fileType);
    const conf = {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      // 'onUploadProgress': (progressEvent: any) => {
      //     if (progressEvent.lengthComputable) {
      //         let val = (progressEvent.loaded / progressEvent.total * 100).toFixed(0);
      //         console.log('uploadfile progress',parseInt(val))
      //     }
      // }
    };
    return this.post(`${config.zeta.base}DataMover/file/upload`, formData,  conf);
  }
  getSchema (path: string){
    return this.get(`${config.zeta.base}DataMover/file/parse?fullPath=${path}`);
  }
  moveFromFile (nt: string, fullPath: string, tgt: DataMove.HerculesTable, isDrop: number, isConvert: number, ddl: string){

    return this.post(`${config.zeta.base}DataMover/move/LC2HD`, {
      history: {
        nT: nt,
        sourceTable: fullPath.substring(fullPath.lastIndexOf('/')+1, fullPath.length),
        sourcePlatform: fullPath.substring(0, fullPath.lastIndexOf('/')+1),
        targetTable: tgt.srcDB+'.'+tgt.table,
        targetPlatform: tgt.source,
      },
      isDrop:isDrop,
      isConvert:isConvert,
      ddl: ddl,
    });
  }
  movePlus (nt: string, src: DataMove.VDMTable, tgt: DataMove.HerculesTable, overrideTables = []){
    // type:  TD2HD(1), LC2HD(3), VDM2HD(4), VDMVIEW2HD(5);
    const conf = {
      timeout: 1000 * 60 * 5,
    };
    const url = `${config.zeta.base}DataMover/move_plus`;
    const db = src.database;
    const tables = src.tableName.split(';');
    const views = src.viewName.split(';');
    const fullTables = tables.filter(d => !!d && d.trim() != '').map((table: string) => {
      return db + '.' + table.trim();
    });
    const fullViews = views.filter(d => !!d && d.trim() != '').map((view: string) => {
      return db + '.' + view.trim();
    });
    const sourceTables = fullTables.length>0?fullTables:fullViews;
    const type = fullTables.length>0?  4 : 5;
    return this.post(url, {
      history: {
        nT: nt,
        sourceTable: JSON.stringify(sourceTables),
        sourcePlatform: src.source,
        targetTable: tgt.srcDB,
        targetPlatform: tgt.source,
        type: type, //  TD2HD(1), LC2HD(3), VDM2HD(4), VDMVIEW2HD(5);
      },
      isDrop: tgt.override ? 1 : 0,
      overrideTables: overrideTables,
    }, conf);
  }
  getVDMWorkspace (platform: string){
    return this.get(`${config.zeta.base}DataMover/VDMWorkspace?platform=${platform}`);
  }

  getVDMDataBases (platform: string, isRealTime = false) {
    return this.get(`${config.zeta.base}DataMover/VDMDatabase?platform=${platform}&isRealtime=${isRealTime}`);
  }

  getVDMTables (platform: string, database: string, isRealTime = false) {
    return this.get(`${config.zeta.base}DataMover/VDMTable?platform=${platform}&database=${database}&isRealtime=${isRealTime}`);
  }
  getVDMViews (platform: string, database: string) {
    return this.get(`${config.zeta.base}DataMover/VDMView?platform=${platform}&database=${database}`);
  }

  validateVDM (nt: string, src: DataMove.VDMTable, tgt: DataMove.HerculesTable){
    const conf = {
      timeout: 1000 * 60 * 5,
    };
    const db = src.database;
    const tables = src.tableName.split(';');
    const fullTables = tables.map((table: string) => {
      return db + '.' + table;
    });
    return this.post(`${config.zeta.base}DataMover/validate/VDM2HD`, {
      history: {
        nT: nt,
        sourceTable: JSON.stringify(fullTables),
        sourcePlatform: src.source,
        targetTable: tgt.srcDB,
        targetPlatform: tgt.source,
      },
      isDrop: tgt.override ? 1 : 0,
    }, conf);
  }
  retryVDMFailJobs (id: number) {
    return this.put(`${config.zeta.base}DataMover/retry/VDM2HD/${id}`);
  }

}
