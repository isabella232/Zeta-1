/* eslint-disable @typescript-eslint/interface-name-prefix */

import { WorkspaceBase, WorkSpaceType, WorkspaceDecorator } from './workspace.internal';
import Util from '@/services/Util.service';
import _ from 'lodash';
import Vue from 'vue';
import uuid from'uuid';
/* Store data types */

/* Notebook */
// export enum WorkSpaceType {
// 	NOTEBOOK = 'NOTEBOOK',
// 	DATAMOVE = 'DATAMOVE',
// 	DATAVALIDATION = 'DATAVALIDATION',
// 	SQLCONVERTER = 'SQLCONVERTER',
// 	METADATA = 'METADATA',
// 	DASHBOARD = 'DASHBOARD'
// }
/**
 * @deprecated
 */
export enum ConnStatus {
  OFFLINE = 'OFFLINE',
  ONLINE = 'ONLINE',
  CONNECTING = 'CONNECTING'
}

export enum NotebookStatus {
  INITIAL = 'INITIAL',
  OFFLINE = 'OFFLINE',
  CONNECTING = 'CONNECTING',
  DISCONNECTING = 'DISCONNECTING',
  /** @deprecated */ ERROR = 'ERROR',
  IDLE = 'IDLE',
  SUBMITTING = 'SUBMITTING',
  RUNNING = 'RUNNING',
  STOPPING = 'STOPPING',
  /** @deprecated */ FATAL = 'FATAL'
}
export type NotebookType = 'single' | 'collection' | 'sub_nb' | 'zeppelin' | 'zeppelin-article' | 'zeppelin-dashboard' | 'zeppelin-native';
export enum JobStatus {
  SUBMITTING = 'SUBMITTING',
  SUBMITTED = 'SUBMITTED',
  RUNNING = 'RUNNING',
  SUCCESS = 'SUCCESS',
  PARTIAL_SUCCESS = 'PARTIAL_SUCCESS',
  CANCELED = 'CANCELED',
  ERROR = 'ERROR',
  WAITING = 'WAITING'
}

export enum QueryStatus {
  SUBMITTED = 'SUBMITTED',
  RUNNING = 'RUNNING',
  SUCCESS = 'SUCCESS',
  CANCELED = 'CANCELED',
  UNKONOWN = 'UNKNOWN',
  ERROR = 'ERROR',
  WAITING = 'WAITING'
}

export const StatusCodeMap: Dict<string> = {
  '2000': 'Success',
  '2001': 'Finished',
  '2002': 'Created',
  '2003': 'Running',
  '2004': 'Waiting',
  '2005': 'Connected',
  '2006': 'Submitted',
  '2007': 'Canceled',
  '4009': 'Unknown exception',
  '4001': 'Failed',
  '4002': 'Timeout',
  '4003': 'Disconnected',
  '4004': 'Notebook is not running',
  '6001': 'Duplication violation',
  '6002': 'Invalid input parameter',
  '6003': 'Entity is NULL',
  '6004': 'Entity ID is NULL',
  '6005': 'Entity not found',
  '6006': 'Bad SQL gramma',
  '6007': 'Conflict exception',
  '6008': 'Cannot get JDBC connection',
  '6009': 'Incorrect result set column count',
  '7001': 'Illegal status',
};
export interface IQueryContent {
  type: 'TABLE' | 'HTML' | 'TEXT' | 'IMG';
  content: string;
}
export interface IQuery {
  seqId: number;
  /// (ISO 8601, no fractional seconds)
  /// e.g. "2013-02-04T22:44:30.652Z"
  submitTime?: string;
  startTime?: string;
  endTime?: string;
  // status: QueryStatus,
  status: IJobStatus;
  query: string;
  headers?: string[];
  rows?: Array<string[]>;
  content?: IQueryContent;
  // progress: number,
  errorMessage?: string;
  statementId?: number;
}

export interface IJob {
  reqId: string;
  jobId: string;
  submitTime?: string;
  startTime?: string;
  endTime?: string;
  queries: IQuery[];
  status: JobStatus;
  info?: string;
  seq: number;
  /* --- */
  // activeQueryId: number
}
export interface IJobStatus {
  info?: any;
  progress: number;
  status: QueryStatus;
  totalNumTasks?: number;
  totalNumCompletedTasks?: number;
  numActiveTasks?: number;
  numCompletedCurrentStageTasks?: number;
  dag?: string;
}
/**
 * @deprecated
 */
export interface ISubJob {
  completionTime: any;
  description: any;
  jobGroup: number;
  jobId: number;
  name: string;
  numActiveStages: number;
  numActiveTasks: number;
  numCompletedStages: number;
  numCompletedTasks: number;
  numFailedStages: number;
  numFailedTasks: number;
  numSkippedStages: number;
  numSkippedTasks: number;
  numTasks: number;
  stageIds: number[];
  status: string;
  submissionTime: string;
}


export function getAvailableStatus() {
  const availableStatuesMap = new Map<NotebookStatus, NotebookStatus[]>();
  /** INITAIL */
  availableStatuesMap.set(NotebookStatus.INITIAL,[NotebookStatus.OFFLINE, NotebookStatus.IDLE]);
  /** OFFLINE */
  availableStatuesMap.set(NotebookStatus.OFFLINE,[NotebookStatus.CONNECTING]);
  /** CONNECTING */
  availableStatuesMap.set(NotebookStatus.CONNECTING,[NotebookStatus.OFFLINE, NotebookStatus.DISCONNECTING, NotebookStatus.IDLE ]);
  /** DISCONNECTING */
  availableStatuesMap.set(NotebookStatus.DISCONNECTING,[NotebookStatus.OFFLINE]);
  /** IDLE */
  availableStatuesMap.set(NotebookStatus.IDLE,[NotebookStatus.DISCONNECTING, NotebookStatus.SUBMITTING, NotebookStatus.RUNNING]);
  /** SUBMITTING/WAITING */
  availableStatuesMap.set(NotebookStatus.SUBMITTING,[NotebookStatus.DISCONNECTING, NotebookStatus.RUNNING, NotebookStatus.STOPPING]);
  /** RUNNING */
  availableStatuesMap.set(NotebookStatus.RUNNING,[NotebookStatus.RUNNING, NotebookStatus.DISCONNECTING, NotebookStatus.IDLE, NotebookStatus.STOPPING]);
  /** STOPPING */
  availableStatuesMap.set(NotebookStatus.STOPPING,[NotebookStatus.DISCONNECTING, NotebookStatus.IDLE]);
  return availableStatuesMap;
}
export type INotebook = Notebook;
/**
 * multi language notebook
 */
@WorkspaceDecorator({
  type: WorkSpaceType.NOTEBOOK,
})
export class Notebook extends WorkspaceBase {
  code: string;
  jobs: string[];
  source?: string | undefined;
  createTime: number;
  updateTime: number;
  preference?: IPreference | undefined;
  activeJobId?: string | undefined;
  stagedCode: string;
  dirty: boolean;
  loaded: boolean;
  resultUpdated?: boolean | undefined;
  monitorUrl: string | null;
  connection: IConnection;
  nt: string;
  publicReferred: string | null;
  publicRole: 'ref' | 'pub' | 'no_pub' | null;
  nbType: NotebookType = 'single';
  collectionId?: string | undefined;
  progress: number;
  message?: string;
  upRatio: number;
  status_: NotebookStatus;
  packages: IPackage ;
  get status(): NotebookStatus {
	  return this.status_;
  }
  set status(status: NotebookStatus) {
	  const statuesMap = getAvailableStatus();
	  const avaliableStatue = statuesMap.get(this.status_);
	  if(!avaliableStatue) {
	    console.warn('===== Notebook status =====',`[${this.notebookId}]: cannot find next status`);
	  } else if(avaliableStatue.indexOf(status) < 0){
	    console.warn('===== Notebook status =====',`[${this.notebookId}]: illegal status \`${status}\` for current status: \`${this.status_}\` `);
	  } else {
      console.warn('===== Notebook status =====',`[${this.notebookId}]: \`${this.status_}\` => \`${status}\` `);
	    this.status_ = status;
	  }
  }
  get isOnline(): boolean {
	  switch(this.status){
	    case NotebookStatus.IDLE:
	    case NotebookStatus.SUBMITTING:
	    case NotebookStatus.RUNNING:
	    case NotebookStatus.STOPPING:
	      return true;
	    default:
	      return false;
	  }
  }

  get iconClass() {
    const cnn = this.preference && this.preference['notebook.connection'] ? this.preference['notebook.connection'] : undefined;
    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    const codeType = cnn && cnn.codeType ? cnn.codeType : CodeType.SQL;

    const defaultCnn = Util.getDefaultConnectionByPlat(codeType).defaultConnection;
    const defaultAlias = defaultCnn.alias;
    if (!cnn) {
      return defaultAlias.toLowerCase();
    }
    if (cnn.codeType === CodeType.KYLIN) {
      return 'kylin';
    } else {
      const alias = (cnn.alias || defaultAlias).toLowerCase();
      if (alias.indexOf('apollo') >= 0) {
        return 'apollo';
      } else {
        return alias;
      }
    }
  }
  constructor(id: string = uuid(), name = 'Untitled') {
    super();
    this.notebookId = id;
    this.name = name;
	  this.status_ = NotebookStatus.INITIAL;
	  this.connection = {} as IConnection;
	  this.upRatio = 0.6;
	  this.packages = {};
  }
  props(nb: Partial<Notebook>) {
	  Object.assign(this, nb);
	  return this;
  }

}

/* View data types */
export interface IScrollInfo {
  left: number;
  top: number;
}

export interface IEditorHistory {
  redo: number;
  undo: number;
}

/** Connectiotn Info
 * @argument source
 * @argument batchAccount
 */
export interface IConnection {
  alias: string;
  clusterId: number;
  source: string;
  batchAccount: string;
  codeType: CodeType;
  user?: string;
  password?: string;
  database?: string;
}

export enum CodeType {
  SQL = 'SQL',
  SCALA = 'SCALA',
  TERADATA = 'TERADATA',
  KYLIN = 'KYLIN',
  HIVE = 'HIVE',
  SPARK_PYTHON = 'SPARK_PYTHON',
  TEXT = 'TEXT'
}

export interface CodeTypeInfo {
  name: string;
  mime: string;
  interpreter: string;
  jdbcType?: string;
  /** define is collection notebook */
  isCollection: boolean;
}

export const CodeTypes: Dict<CodeTypeInfo> = {
  [CodeType.SQL]: {
    name: 'Spark SQL',
    mime: 'text/z-x-sparksql',
    interpreter: 'livy-sparksql',
    isCollection: false,
  },
  [CodeType.SPARK_PYTHON]: {
    name: 'Spark Python',
    mime: 'text/z-x-mysql',
    interpreter: 'livy-shared',
    isCollection: true,
  },
  [CodeType.TERADATA]: {
    name: 'TD SQL',
    mime: 'text/z-x-mysql',
    interpreter: 'jdbc',
    jdbcType: 'teradata',
    isCollection: false,
  },
  [CodeType.HIVE]: {
    name: 'Hive SQL',
    mime: 'text/z-x-hive',
    interpreter: 'jdbc',
    jdbcType: 'hive',
    isCollection: false,
  },
  [CodeType.KYLIN]: {
    name: 'Kylin SQL',
    mime: 'text/z-x-mysql',
    interpreter: 'jdbc',
    jdbcType: 'kylin',
    isCollection: false,
  },
  [CodeType.TEXT]: {
    name: 'TEXT',
    mime: 'text/z-x-sparksql',
    interpreter: 'jdbc',
    jdbcType: 'text',
    isCollection: false,
  },



  // [CodeType.SCALA]: {
  //     name: CodeType.SCALA,
  //     mime: "text/x-scala",
  //     interpreter: 'livy-spark'
  // }
};

import * as v from '@/assets/notebook.variables.json';
export const variables = v as Dict<any>;
export interface INotebookConfig {
  notebookId: string;
  title: string;
  optmzt: IOptimization;
  [k: string]: any;
}
export interface Cluster {
  clusterAlias?: string;
  clusterId: number;
  clusterName: string;
  access: boolean;
  batchAccountOptions: string[];
}
export interface ClusterDict {
  [clusterName: string]: Cluster;
}
export interface IOptimization {
  'zds.livy': {
    [k: string]: string;
  };
  'zds.jdbc': {
    [k: string]: string;
  };
}
export interface IPreference {
  'notebook.profile'?: string;
  'notebook.variables'?: Dict<string>;
  'notebook.vargenerators'?: Dict<string>;
  'notebook.connection'?: IConnection;

  'subNotebook.lang'?: string;
  /** @deprecated */
  'zds.livy'?: IOptimization;
}
export interface IPackage {
  [k: string]: string[];
}
export enum BDP_STATUS{
  unload = '0',
  loading = '1',
  success = '2',
  failed = '-2',
  error = '-1'
}
