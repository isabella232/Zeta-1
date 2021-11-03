import Vue from 'vue';

import { Notebook } from './notebook.internal';
import { Dashboard } from './dashboard.internal';
import { Prop } from 'vue-property-decorator';
import { MetaSheet } from './metasheet.internal';
import { SharedNotebook } from './notebook-shared.internal';

export enum WorkSpaceType {
  NOTEBOOK = 'NOTEBOOK',
  NOTEBOOK_COLLECTION = 'NOTEBOOK_COLLECTION',
  SHARED_NOTEBOOK = 'SHARED_NOTEBOOK',
  DATAMOVE = 'DATAMOVE',
  DATAVALIDATION = 'DATAVALIDATION',
  SQLCONVERTER = 'SQLCONVERTER',
  METADATA = 'METADATA',
  DASHBOARD = 'DASHBOARD',
  NOTEBOOK_ZEPPELIN = 'NOTEBOOK_ZEPPELIN',
  META_SHEET = 'META_SHEET',
}
export abstract class WorkspaceBase {
  type: WorkSpaceType;
  notebookId: string;
  name: string;
  seq: number;
  iconClass?: string;
}
export function WorkspaceDecorator(params: Partial<WorkspaceBase>) {
  const defaultProperties: WorkspaceBase = {
    type: params.type || WorkSpaceType.NOTEBOOK,
    notebookId: params.notebookId || '',
    name: params.name || 'Untitled',
    seq: params.seq !== undefined ? params.seq : -1,
    iconClass: params.iconClass,
  };
  return function <WorkspaceBase extends {new(...args: any[]): {}}>(constructor: WorkspaceBase) {
    return class extends constructor {
      type = defaultProperties.type;
      // notebookId = defaultProperties.notebookId;
      name = defaultProperties.name;
      seq = defaultProperties.seq;
      iconClass: string;
      // iconClass = defaultProperties.iconClass;
      constructor(...args: any[]) {
        super(...args);
        if (!this.iconClass && defaultProperties.iconClass) {
          this.iconClass = defaultProperties.iconClass;
        }
      }
    };
  };
}
export type IWorkspace = Notebook | Dashboard | MetaSheet | SharedNotebook;
export class ITools extends WorkspaceBase{
  [k: string]: any
}
export class WorkspaceComponentBase extends Vue {
  @Prop()
  workspaceId: string;

  beforeClose() {
    console.debug('before close workspace tab', this.workspaceId);
  }
}

