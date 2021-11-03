
export interface AuthorizationUsers {
    OWNERS?: Array<MetaShareUser> | [];
    READERS?: Array<MetaShareUser> | [];
    WRITERS?: Array<MetaShareUser> | [];
}
export interface MetaConfigTableRow {
    column: string;
    type: string;
    length?: string;
    desc?: string;
    nullValue?: boolean;
    primaryKey?: boolean;
    editable?: boolean;
    validate?: boolean;
}
export interface MetaSheetTableItem {
    id?: string;
    nt?: string; // this is table admin
    authInfo?: string;
    metaTableName: string;
    platform?: MetaSheetPlatform | '';
    account?: string; // this is source configuration page, batch account field
    db?: string;
    tbl?: string;
    schemaInfo: string;
    metaTableType?: MetaSheetType,
    cron?: string;
    metaTableStatus?: MetaSheetStatus;
    createTime?: number;
    updateTime?: number;
    failLog?: string;
    syncTime?: number | null;
    hadoopSchemaInfo?: string;
    path?: string;
}
export interface MetaSheetTableRow {
    id?: string;
    metaTableName: string;
    nt?: string;
    access?: MetaSheetAccess;
    platform?: string;
    fullTableName?: string;
    updateTime?: number;
    authInfo?: AuthorizationUsers;
    schemeConfig?: SchemeConfig;
    sourceConfig?: SourceConfig;
    metaTableStatus?: MetaSheetStatus;
    syncTime?: number | null;
    failLog?: string;
    path?: string; // store all the users path
    selectable?: boolean;
    isFile?:boolean;
    folderPath?: string; // store current user path
}

export interface MetaSheetConfig {
    id?: string;
    name: string;
    metaTableStatus?: MetaSheetStatus;
    schemeConfig?: SchemeConfig;
    sourceConfig?: SourceConfig;
}

export interface SchemeConfig {
    tableData: Array<MetaConfigTableRow>;
}

export interface Frequency {
    jobType?: freq;
    year?: number;
    hour?: number;
    minute?: number;
}

export interface SourceConfig {
    metaTableType: MetaSheetType; // HDM or Production
    platform?: MetaSheetPlatform | ''; // Hermes / appollo rno
    db: string;
    tbl: string;
    cron?: Frequency;
    account?: string;
}

export interface MetaShareUser {
    nt: string;
    email: string;
    name: string;
}


export enum MSG_TYPE {
    SUCCESS = 'success',
    ERROR = 'error',
    INFO = 'info',
    WARNING = 'warning'

}
export enum FREQ {
    DAILY = 'DAILY',
    HOURLY = 'HOURLY',
    MINUTELY = 'MINUTELY'
}
export enum MS_SYS {
    HDM = 'HDM',
    PROD = 'PROD'
}
export enum MODE {
    ADD = 'ADD', // create new zetasheet
    EDIT = 'EDIT',
    VIEW = 'VIEW',
    DELETE = 'DELETE'
}
export enum MS_STATUS {
    CREATED = 'CREATED',
    FAILED = 'FAILED',
    SYNC = 'SYNCING',
    SUCCESS = 'SUCCESS',
    REGISTER_FAIL = 'REGISTER_FAIL',
    LOAD_FAIL = 'LOAD_FAIL'
}
export enum PLATFORM {
    HERMES = 'hermes',
    HERCULES = 'hercules',
    ARES = 'ARES',
    APOLLORNO = 'apollorno'
}
export enum ACCESS {
    CREATOR = 'Creator',
    OWNER = 'Admin',
    READER = 'Reader',
    WRITER = 'Writer'
}
export type freq = FREQ;
export type MetaSheetType = MS_SYS | '';
export type Mode = MODE;
export type MetaSheetStatus = MS_STATUS;
export type MetaSheetPlatform = PLATFORM;
export type MetaSheetAccess = ACCESS;
