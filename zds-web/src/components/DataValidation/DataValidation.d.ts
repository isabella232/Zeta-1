declare namespace DataValidation{
  export interface TDTable{
    source: string;
    table: string;
    filter?: string;
    batchAccount?: string;
    $self?: any;
    columnLoading?: boolean;
    columns?: Array<any>;
    srcDB?: string;
    srcTable?: string;
  }
  export interface HerculesTable{
    source: string;
    table: string;
    $self?: any;
    loading?: boolean;
    filter?: string;
    batchAccount?: string;
    srcDB?: string;
    srcTable?: string;
  }
  export interface TableAPI {
    reset(): void;
    apply(tableInfo: TDTable| HerculesTable): void;
    checkParams(): boolean;
    validate(): TDTable| HerculesTable;
  }
}