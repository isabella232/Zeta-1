import AutoELService from '@/services/remote/AutoELService';
import {DBC} from '../../types';

const API = AutoELService.instance;

interface Result {
  [key: string]: DBC|undefined; 
}
class DBCInfoService {

  cache = new Map<string, DBC>();
  async getFromCache(dbcNames: string | string []): Promise<Result> {
    if (typeof dbcNames === 'string') {
      dbcNames = [ dbcNames ];
    }
    const result: Result = {};
    const notFound: string[] = [];
    dbcNames.forEach(n => {
      if (this.cache.has(n)) {
        result[n] = this.cache.get(n);
      } else {
        notFound.push(n);
      }
    });
    if (notFound.length > 0) {
      const resp = await Promise.all(notFound.map(key => API.getDBCInfo(key).catch(() => null)));
      resp.forEach((res) => {
        if (!res) return;
        this.cache.set(res.name, res);
        result[res.name] = res;
      });
    }
    return result;
  }

  async validate(dbcList: DBC[], saCode = 'dw_eip') {
    const result = await API.validateTable(dbcList as any, saCode).then(({ list }) => { //todo dbcList type does not match with the API
      return list.map(result => {
        return {
          ...result,
          isValid: result.message === 'Successed', // WTF?
          key: result.dbc_name + result.table_name,
        };
      });
    });
    return result;
  }
}

export default new DBCInfoService();