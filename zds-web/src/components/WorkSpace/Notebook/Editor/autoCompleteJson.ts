
import SqlConverterRemoteService from '@/services/remote/SQLConverter';
import tableJson from './default_tables.json';
import columnsJson from './default_columns.json';
import _ from 'lodash';
const remoteService = new SqlConverterRemoteService().props({
  path: 'settings',
});

const AUTO_COMPLETE_EXEC_TABLE = 'auto_complete_exec_table';
const LAST_MODIFIED = 'auto_complete_last_modified';

function getLocalStorage (): Array<any> {
  return JSON.parse(localStorage.getItem(AUTO_COMPLETE_EXEC_TABLE) || '{}');
}

function saveLocalStorage (data: any) {
  localStorage.setItem(AUTO_COMPLETE_EXEC_TABLE, JSON.stringify(data));
}

function removeLocalStorage (lastModified: string) {
  if (localStorage.getItem(LAST_MODIFIED) != lastModified) {
    localStorage.removeItem(AUTO_COMPLETE_EXEC_TABLE);
    localStorage.setItem(LAST_MODIFIED, lastModified);
  }
}

export let tables = tableJson;
export let columns = _.assign(columnsJson, getLocalStorage());

export async function updateTableJson () {
  const res = await remoteService.getTableJson().catch((e) => {
    e.resolved = true;
    const rs = { data: { data: [] } };
    return rs;
  });
  tables = _.assign(tables, res.data.data);
}

export async function updateColumnJson () {
  const batchSize = 1000;
  const res = await remoteService.getColumnJson({ batchSize: batchSize, offset: 0 }).catch((e) => {
    e.resolved = true;
    const rs = { data: { data: [] }, headers: {} };
    return rs;
  });
  if (res.headers['last-modified']) removeLocalStorage(res.headers['last-modified']);
  columns = _.assign(columns, res.data.data.data);
}

export async function getColumnJsonByTableName (tableName: string) {
  const res = await remoteService.getSpecColumnJSON(tableName).catch((e) => {
    e.resolved = true;
    const rs = { data: { data: [] } };
    return rs;
  });
  let exec_table: Array<any> = getLocalStorage();
  columns = _.assign(columns, res.data.data);
  exec_table = _.assign(exec_table, res.data.data);
  saveLocalStorage(exec_table);
  return res.data.data[tableName];
}