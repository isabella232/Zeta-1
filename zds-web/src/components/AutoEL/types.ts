export interface State {
  tasks: TaskBasic[];
  task: Task | null;
  saList: SA[];
  sourceTables: SourceTable[];
  sourceTableInfo: { [key: string]: SourceTable};
  sourceTableSchema: { [key: string]: string};
  sourceTableSample: { [key: string]: any};
  releaseStatus: ReleaseStatus | null;
  recentJobs: any[];
  hadoopLocation: string;
  ddl: string;
  incrementalScript: string;
  dataModel: any;
  isDA: boolean;
  isSAOwner: boolean;
}

interface SA {
  batch_acct: string;
  sa_code: string;
  sbjct_area: string;
  team_dl: string;
  team_name: string;
}

interface SourceTable {
  id: string;
  [props: string]: string;
}

type SourceType = 'Oracle' | 'NuData';
type TargetPlatform = 'hercules' | 'apollo_rno | ares';
type UpdateFrequency= 'Daily' | 'Weekly' | 'Monthly' | 'One Time' | string ;
type UpdateStrategy = 'Upsert' | 'Append' | 'Full Table';

export interface TaskBasic {
  id: number;
  sa_code: string;
  tgt_db: string;
  tgt_table: string;
  tgt_platform: TargetPlatform;
  src_type: SourceType;
  release_time: string;
}

export interface ReleasedTask extends Task, TaskBasic {
  id: number;
  schedule_time: string;
  first_run_dt: string;
  initial_load: number;
  release_time: string;
  uc4_job_folder: string;
  touch_file: string;
  cre_date: string;
  cre_user: string;
  upd_date: string;
  upd_user?: any;
}

export interface Task {
  id?: string | number;
  src_type: SourceType;
  src_platform: string;
  src_db: string;
  src_table: string;
  tgt_type: 'HD';
  tgt_platform: TargetPlatform;
  tgt_db: string;
  tgt_table: string;
  sa_code: string;
  sa_name: string;
  upd_freq: UpdateFrequency;
  upd_strategy: UpdateStrategy;
  user: string;
  user_full_name: string;
  release_time?: string;
  source_list: any[];
  approvalStatus?: any;
}

export interface ReleaseStatus {
  id: number;
  table_name: string;
  git_status: string;
  git_upd_time: string;
  uc4_status: string;
  uc4_upd_time: string;
  uc4_job_name: string;
  uc4_job_client: string;
  uc4_job_uow: string;
  ddl_status: string;
  ddl_upd_time: string;
  initial_run_status: string;
  initial_run_upd_time: string;
  dv_status: string;
  dv_upd_time: string;
  uc4_schedule_status: string;
  uc4_schedule_upd_time: string;
  overall_status: string;
  cre_date: string;
  cre_user: string;
  upd_date: string;
  upd_user: string;
}

export interface DbcTable{
  dbc_name: string;
  table_name: string;
}

export interface DBC {
  db_name: string;
  dbc_name: string;
  physical_host: string;
  port: string;
  sid: string;
}

export interface Host{
  table_name: string;
  list: Array<DBC>;
  dbc_name: string;
}