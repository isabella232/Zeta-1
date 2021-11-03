/**
 * This file is an re-export of types used by notebook
 * As is a workaround for the warning bug in TS compiler.
 * As of TS 2.8.1, the bug still exists.
 * REF. https://github.com/angular/angular-cli/issues/2034
 */
import { WorkSpaceType, IWorkspace } from './workspace.internal';
import { IJob, ConnStatus } from './notebook.internal';
import { NotebookJob } from './notebook-job';

export {
  NotebookStatus,
  JobStatus,
  QueryStatus,
  IQueryContent,
  IQuery,
  IJob,
  StatusCodeMap,
  INotebook,
  Notebook,
  IScrollInfo,
  IConnection,
  IPreference,
  IOptimization,
  INotebookConfig,
  variables,
  CodeType,
  CodeTypeInfo,
  CodeTypes,
  IJobStatus,
  ISubJob,
  ConnStatus,
  Cluster,
  ClusterDict,
  IEditorHistory,
  BDP_STATUS,
  NotebookType,
} from './notebook.internal';
export { Rectangle, LayoutConfig, IDashboardFile, LayoutType, Dashboard, DashboardQuery} from './dashboard.internal';
export { WorkspaceComponentBase, WorkSpaceType, IWorkspace, WorkspaceBase, ITools } from './workspace.internal';
export { IMetadata, platform, Metadata, rowType, Source } from './metadata.internal';
export { IHDFSFile, IPackageFile, IFile } from './hdfs.internal';
export * from './zeppelin.internal';
export * from './public-notebook.internal';
export * from './metasheet.internal';
export * from './multi-notebook.internal';
export * from './zeppelin-notebook.internal';
export * from './feedback.internal';

export interface IWorkspaceStore {
  workspaces: Dict<IWorkspace>;
  jobs: Dict<NotebookJob>;
  jobSeq: number;
  /* --- */
  activeWorkspaceId?: string;
  connStatus: ConnStatus;
  loading: boolean;
}

