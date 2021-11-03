import {FrequenceOptions, Frequence, TimePickerResult} from '@/components/common/time-picker';
import Config from '@/config/config';
import ScheduleContainer from './ScheduleContainer.vue';
import { CodeType, CodeTypes } from '@/types/workspace';
import Util from '@/services/Util.service';
import { isHermes, getHermesCnnConfig } from '@/services/connection.service';
interface NotebookScheduleProp {
  host: string;
  user: string;
  password: string;
  database: string;
  port: number;
  [principal: string]: any;
  jdbc_type: string;
}
/** TD */
interface TDNotebookScheduleProp {
  host: string;
  jdbc_type: string;
}
interface NotebookSchedule{
  clusterId?: number;
  jdbcType?: string;
  proxyUser: string;
  prop?: NotebookScheduleProp | TDNotebookScheduleProp;
  req: {
    notebookId: string;
    reqId: null;
    codes: null;
    interpreter: string;
  };
  history?: any;
}

interface DataMoveSchedule{
  filter: string;
  history: {
    nT: string;
    sourceTable: string;
    sourcePlatform: string;
    targetTable: string;
    targetPlatform: string;
    type?: number;
  };
  isDrop: 1;
  isConvert: number;
}

interface DataValidationSchedule{
  history: {
    nT: string;
    sourceTable: string;
    targetTable: string;
    sourcePlatform: string;
    targetPlatform: string;
  };
  sourceFilter: string;
  targetFilter: string;
  sourceBatchAccount: string;
  targetBatchAccount: string;
}

enum ScheduleStatus {
  'Active' = 1,
  'Inactive' = 0
}
export enum ScheduleJobStatus {
  'NOTSTART' = 'Not Start',
  'PENDING' = 'Waiting',
  'WAITING' = 'Waiting',
  'RUNNING' = 'Running',
  'AUTORETRY' = 'Retrying',
  'AUTORETRYWAITING' = 'Retrying',
  'SUCCESS' = 'Succeeded',
  'CANCELED' =  'Canceled',
  'FAIL' = 'Failed'
}
type ScheduleType = 'Notebook' | 'DataMove' | 'DataValidate';
// export const FrequenceOptions = ['OneTime' , 'Monthly' , 'Weekly' , 'Daily','Hourly'];
export enum JobRunStatus {
  NOTSTART = 'NOTSTART',
  PENDING = 'PENDING',
  WAITING = 'WAITING',
  RUNNING = 'RUNNING',
  AUTORETRY = 'AUTORETRY',
  AUTORETRYWAITING = 'AUTORETRYWAITING',
  SUCCESS = 'SUCCESS',
  CANCELED = 'CANCELED',
  FAIL = 'FAIL',
}
interface AuthorizationUsers {
  OWNERS?: Array<ShareUser> | [];
  READERS?: Array<ShareUser> | [];
  WRITERS?: Array<ShareUser> | [];
}
interface ShareUser {
  nt: string;
  email: string;
  name: string;
}
interface JobRunStatusInfo{
  jobRunStatus: JobRunStatus;
  info: {
    [k: string]: any;
  } | null;
}
interface ScheduleConfig {
  id?: number;
  jobName: string;
  type: ScheduleType;
  status: ScheduleStatus;
  scheduleTime: TimePickerResult;
  nt: string;
  task: NotebookSchedule | DataMoveSchedule | DataValidationSchedule;
  ccAddr?: string[] | null;
  createTime?: number;
  lastRunTime?: number;
  nextRunTime?: number;
  jobRunStatusInfo: JobRunStatusInfo | null;
  jobRunStatus?: JobRunStatus;
  dependency: NoteScheduleDependency;
  dependencySignal: NoteScheduleDependencySignal;
  mailSwitch: number;
  failTimesToBlock: number;
  autoRetry: boolean;
  authInfo: AuthorizationUsers;
  updateTime?: number;
}


interface NoteScheduleDependency {
  enabled: boolean;
  dependencyTables: Array<NoteScheduleDependencyItems>;
  waitingHrs: number;
}

interface NoteScheduleDependencyItems {
  tableName: string;
  wait: boolean;
  timeLag: number | string;
}
interface NoteScheduleDependencyTablesItems{
  platform: string;
  db: string;
  table: string;
}
interface  NoteScheduleDependencySignal {
  enabled: boolean;
  signalTables: Array<string>;
}
interface DoeDependencySignal {
  pltfrm_name: string;
  db_name: string;
  table_name: string;
  scheduling: boolean;
}
enum ScheduleHistoryStatus {
  'In progress' = 0,
  'Succeed' = 1,
  'Failed' = 2
}

interface ScheduleHistory {
  id: number;
  jobId: number;
  jobName?: string;
  type?: ScheduleType;
  jobHistoryId: number | null;
  jobRunStatusInfo: JobRunStatusInfo | null;
  jobRunStatus: JobRunStatus;
  startTime: number;
  endTime: number;
  runTime: number;
  result?: string;
  log: string | null;
}
function getDefaultConnectionConfig(codeType: CodeType, notebookId: string, clusterId?: number): NotebookSchedule {
  const lang = CodeTypes[codeType];
  const hermes = (codeType == CodeType.SQL && isHermes(clusterId!));
  const interpreter = hermes ? 'carmel' : lang.interpreter || 'jdbc';

  const config: NotebookSchedule =  {
    proxyUser: Util.getNt(),
    req: {
      notebookId,
      'reqId': null,
      'codes': null,
      interpreter,
    },
  };
  if(codeType === CodeType.SQL) {
    config.clusterId = clusterId;
  }
  return config;
}
function getHermesConfigProps(clusterId: number): NotebookScheduleProp{
  const { host, pricipal, jdbcType } = getHermesCnnConfig(clusterId);
  return {
    host,
    user: Util.getNt(),
    password: '',
    database: 'access_views',
    'jdbc.props.hive.server2.remote.principal': pricipal,
    port: 10000,
    jdbc_type: jdbcType,
  };
}
function getJDBCConfigProps(codeType: CodeType, clusterAlias: string): NotebookScheduleProp | TDNotebookScheduleProp{
  const lang = CodeTypes[codeType];
  const { hostMap, principalMap } = Util.getDefaultConnectionByPlat(codeType);
  let pricipal = '';
  let host  = '';
  if(hostMap && hostMap[clusterAlias]) {
    host = hostMap[clusterAlias];
  }
  if(principalMap && principalMap[clusterAlias]) {
    pricipal = principalMap[clusterAlias];
  }
  if(codeType === CodeType.TERADATA) {
    return {
      host,
      // eslint-disable-next-line @typescript-eslint/camelcase
      jdbc_type: lang.jdbcType
    } as TDNotebookScheduleProp;
  }
  return {
    host,
    user: Util.getNt(),
    password: '',
    database: 'default',
    'jdbc.props.hive.server2.remote.principal': pricipal,
    port: 10000,
    // eslint-disable-next-line @typescript-eslint/camelcase
    jdbc_type: lang.jdbcType || 'jdbc',
  };
}
function getScheduleJDBCProp(codeType: CodeType, clusterId: number, clusterAlias: string): any {
  const hermes = (codeType == CodeType.SQL && isHermes(clusterId));
  const isJDBC = codeType == CodeType.HIVE || codeType == CodeType.TERADATA || hermes;
  if(isJDBC) {
    const prop = hermes ? getHermesConfigProps(clusterId) : getJDBCConfigProps(codeType, clusterAlias);
    return prop;
  }
  return null;
}
function scheduleConfigFactory(codeType: CodeType, notebookId: string, clusterId: number, clusterAlias: string): NotebookSchedule {
  const config: NotebookSchedule = getDefaultConnectionConfig(codeType, notebookId, clusterId);
  const jdbcProp = getScheduleJDBCProp(codeType, clusterId, clusterAlias);
  if(jdbcProp) {
    config.prop = jdbcProp;
    config.clusterId = Config.zeta.notebook.connection.allAliasIdMap[clusterAlias];
  }
  return config;
}

export default ScheduleContainer;
export {
  NotebookSchedule,
  DataMoveSchedule,
  DataValidationSchedule,
  ScheduleStatus,
  ScheduleType,
  ScheduleConfig,
  ScheduleHistoryStatus,
  ScheduleHistory,
  FrequenceOptions,
  Frequence,
  NotebookScheduleProp,
  TDNotebookScheduleProp,
  scheduleConfigFactory,
  getHermesConfigProps,
  getScheduleJDBCProp,
  NoteScheduleDependency,
  NoteScheduleDependencyItems,
  NoteScheduleDependencyTablesItems,
  NoteScheduleDependencySignal,
  DoeDependencySignal,
};
