import { WorkspaceBase, WorkspaceDecorator, WorkSpaceType } from './workspace.internal';
interface Column {
  index: number;
  column: string;
  isDecimal: boolean;
}

interface ColumnTypeMap {
  [i: number]: boolean;
}
@WorkspaceDecorator({
  type: WorkSpaceType.META_SHEET,
  iconClass: 'meta-sheet',
})
export class MetaSheet extends WorkspaceBase {
  get sheetName() {
    return this.name;
  }

  // set sheetName(name) {
  //   this.sheetName = name;
  // }
  columns: Array<Column>;
  columnTypeMap: ColumnTypeMap;
}
