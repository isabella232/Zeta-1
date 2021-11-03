import Vue from 'vue';
import _ from 'lodash';
import Util from '@/services/Util.service';
import {
  MetaSheetTableRow,
  MS_SYS,
  MSG_TYPE,
  ACCESS,
  MetaConfigTableRow,
  AuthorizationUsers,
  MetaSheetAccess,
  PLATFORM,
  MetaSheetTableItem
} from '@/types/meta-sheet';
export default class MetaSheetUtil {
  static isOk(response) {
    return response.status == 200 && response.data.code == 200;
  }

  static showMessage(vue: Vue, type: MSG_TYPE, message: string, duration?: number) {
    let time = 2000;
    if (type == MSG_TYPE.ERROR && _.isNil(duration)) {
      time = 0;
    }
    vue.$message({
      showClose: true,
      message: message,
      type: type,
      duration: time
    });
  }

  static isValid(row: MetaConfigTableRow, columnName: 'column' | 'type' | 'desc' | 'length') {
    if (columnName == 'type' || columnName == 'desc') {
      return row[columnName] && row[columnName]!.trim() != '';
    } else if (columnName == 'column') {
      return (
        row[columnName] &&
        row[columnName]!.trim() != '' &&
        BLACKLIST.map(d => d.toLowerCase()).indexOf(row[columnName].toLowerCase()) == -1
      );
    } else if (columnName == 'length') {
      let type = row.type;
      let result = false;
      switch (type) {
        case 'string':
        case 'date':
        case 'timestamp':
          result = true;
          break;
        case 'int':
          if (!_.isNil(row.length) && row.length) {
            let len = Number(row.length);
            if (len >= 1 && len <= 11) {
              result = true;
            } else {
              result = false;
            }
          } else {
            result = true;
          }
          break;
        case 'bigint':
          if (!_.isNil(row.length) && row.length) {
            let len = Number(row.length);
            if (len >= 1 && len <= 20) {
              result = true;
            } else {
              result = false;
            }
          } else {
            result = true;
          }
          break;
        case 'varchar':
          if (!_.isNil(row.length) && row.length) {
            let len = Number(row.length);
            if (len >= 1 && len <= 255) {
              result = true;
            } else {
              result = false;
            }
          } else {
            result = true;
          }
          break;
        // case 'double':
        case 'decimal':
          if (!_.isNil(row.length) && row.length) {
            if (/\d+,\d+/.test(row.length)) {
              result = true;
            } else {
              result = false;
            }
          } else {
            result = true;
          }
          break;
        default:
          result = true;
      }
      return result;
    }
  }

  static isInvalidRow(row: MetaConfigTableRow) {
    return (
      !MetaSheetUtil.isValid(row, 'column') ||
      !MetaSheetUtil.isValid(row, 'type') ||
      !MetaSheetUtil.isValid(row, 'desc') ||
      !MetaSheetUtil.isValid(row, 'length')
    );
  }

  static processSingleItem(metasheet: MetaSheetTableItem): MetaSheetTableRow {
    let nt = Util.getNt();
    let authInfo: AuthorizationUsers = JSON.parse(metasheet.authInfo!);
    let schema: Array<MetaConfigTableRow> = JSON.parse(metasheet.schemaInfo);
    let tableData: Array<MetaConfigTableRow> = _.map(schema, row => {
      return {
        ...row,
        editable: false,
        validate: false
      };
    });
    let access: MetaSheetAccess = ACCESS.READER;
    if (nt == metasheet.nt) {
      access = ACCESS.CREATOR;
    } else {
      let ownerObj = _.find(authInfo.OWNERS, owner => owner.nt == nt);
      let readerObj = _.find(authInfo.READERS, reader => reader.nt == nt);
      let writerObj = _.find(authInfo.WRITERS, writer => writer.nt == nt);
      if (ownerObj) {
        access = ACCESS.OWNER;
      } else if (readerObj) {
        access = ACCESS.READER;
      } else if (writerObj) {
        access = ACCESS.WRITER;
      }
    }
    let sourceConfig = {
      metaTableType: metasheet.metaTableType || MS_SYS.HDM,
      db: metasheet.db || '',
      tbl: metasheet.tbl || '',
      platform: metasheet.platform || PLATFORM.HERMES,
      account: metasheet.account || metasheet.nt
    };
    let fullTableName = `${metasheet.db}.${metasheet.tbl}`;
    if (!metasheet.db && !metasheet.tbl) {
      fullTableName = '';
    }
    return {
      id: metasheet.id!,
      metaTableName: metasheet.metaTableName!,
      nt: metasheet.nt!,
      access: access!,
      platform: metasheet.platform!,
      fullTableName: fullTableName,
      updateTime: metasheet.updateTime!,
      authInfo: authInfo!,
      metaTableStatus: metasheet.metaTableStatus!,
      schemeConfig: {
        tableData: tableData
      },
      sourceConfig: sourceConfig,
      failLog: metasheet.failLog,
      syncTime: metasheet.syncTime,
      path: metasheet.path
    };
  }

  static processMetaTableItem(metasheetTableItems) {
    let processedMetaSheets: Array<MetaSheetTableRow> = _.map(metasheetTableItems, metasheet => {
      let option = MetaSheetUtil.processSingleItem(metasheet);
      option['folderPath'] = MetaSheetUtil.processPath(option.path);
      return option;
    });
    return processedMetaSheets;
  }

  static processPath(path) {
    let pathOption = {};
    const nt = Util.getNt();
    try {
      pathOption = JSON.parse(path);
      if(_.isNil(pathOption)) {
        pathOption = {};
      }
    }catch(e) {
    }
    let folderPath = pathOption[nt];
    let _path: string = folderPath || '/';
    _path.indexOf('/') === 0 ? _path : (_path = '/' + _path);
    _path.indexOf('/') === 0 ? _path : (_path = '/' + _path);
    _.findLast(_path) !== '/' ? (_path = _path + '/') : _path;
    return _path;
  }
}
export const _ID_ = '_ID_';
export const _VERSION_ = '_VERSION_';
export const BLACKLIST = ['string', 'varchar', 'int', 'bigint', 'decimal', 'date', 'timestamp', _ID_, _VERSION_];

export const FOLDER_VALIDATORS = [
  {
    regex: /(^\/$)|(^\/[a-zA-Z0-9\/\-_]+\/$)/,
    message: 'Must start and end with `/`, Only alphabet _, -, are allowed.'
  }
];
