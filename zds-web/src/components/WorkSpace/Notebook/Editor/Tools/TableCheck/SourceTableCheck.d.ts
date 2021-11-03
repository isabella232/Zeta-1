declare namespace SourceTableCheck{

  export interface TableCheckComponent {
    reset: () => void;
    open: (sql: string) => Promise<TableInfo[]>;
  }
  export interface TableCheckStatus {
    sql: string;
  }
  interface MatchStatus {
    status: number;
    logic_run_date: string;
    type: string;
    platforms: string;
  }
  export interface TableInfoBase{
    'dbName': string;
    'tblName': string;
  }
  export interface TableInfo extends TableInfoBase{
    'dbName': string;
    'tblName': string;
    'isMigrate': number | null;
    'isMatch': MatchStatus | null;
    'isExist': number | null;
    'isDailyCopy': number | null;
    'jira': string | null;
  }
  export interface CopyModel {
    requester: string;
    dbName: string;
    tblName: string;
    sa: string;
    batchId: string;
    tableSize: string;
    owner: string;
    dataRange: string;
    logic: string;
    touchFile: string;
    primaryKey: string;
    PII: string;
    auditCrtCol: string;
    auditUdtCol: string;
    // constructor(nt: string, dbName: string, tblName: string)
  }
  export interface FormCol {
    key: string;
    val: string;
  }

}
