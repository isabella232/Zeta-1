declare namespace DataMove{
  export interface SourceTable{
    tdTableOption?: TDTable;
    vdmTableOption?: VDMTable;
    uploadTableOption?: UploadFileTable;
  }

  export interface TDTable {
    source: string;
    fullTableName: string;
    filter?: string;
  }
  export interface VDMTable {
    source: string;
    database: string;
    tableName: string;
    viewName: string;
  }

  export interface UploadFileTable {
    schema: Array<any>;
    path: string;
  }

  export interface UploadParam {
    schema: any;
    path: string;
  }
  export interface HerculesTable{
    source: string;
    table: string;
    override: boolean;
    convert: boolean;
    $self?: any;
    loading?: boolean;
    filter?: string;
    srcDB?: string;
    srcTable?: string;
  }
  interface FileTable{
    schema: string;
    path: string;
  }
  export interface TableAPI {
    reset(enableVDM?: boolean): void;
    apply(tableInfo: TDTable| HerculesTable, enableVDM?: boolean): void;
    checkParams(): boolean;
    migration(): TDTable| HerculesTable;
    getParams(): any;
  }
}

