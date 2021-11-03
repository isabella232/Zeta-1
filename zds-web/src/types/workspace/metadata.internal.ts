import { WorkspaceBase } from './workspace.internal'
export type Source = {
    name: string;
    desc?: string;
    platformOptions: platform[];
    rowType: rowType[];
    sort: number;
};
export type platform =
    | 'Hercules'
    | 'Apollo'
    | 'Ares'
    | 'Mozart'
    | 'NuMozart'
    | 'Vivaldi'
    | 'Hopper'
    | '';
export type rowType = 'table' | 'view';

export type column = {
    column_name: string;
    column_id: number;
    data_type: string;
};
export type ViewDetail = {
    platform: platform;
    column: column;
    view_db: string;
    view_name: string;
    table: {
        db_name: string;
        table_name: string;
    };
};
export type Detail = ViewDetail;
export interface Metadata {
    platform: platform;
    platformOptions: platform[];
    rowTypeOptions: rowType[];
    init: boolean;
    name: string;
    detail?: Dict<Detail>;
    [k: string]: any;
};


export type IMetadata = WorkspaceBase & Metadata;