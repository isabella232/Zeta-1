# Workspace in Zeta
current Zeta supports below workspace instances
- Notebook
- ~~In house MultiNotebook~~
- MultiNotebook
- ZetaSheet
- Legacy in house Dashboard
## How to define a new Workspace Type
### WorkspaceDecorator
workspace decorator will create some properties like `sequence`, `notebookId`
Sample
```
@WorkspaceDecorator({
  type: WorkSpaceType.META_SHEET,
})
export class MetaSheet extends WorkspaceBase {
  get sheetName() {
    return this.name;
  }

  set sheetName(name) {
    this.sheetName = name;
  }
  columns: Array<Column>;
  columnTypeMap: ColumnTypeMap;
}
```
## How to create a worksapce instance
```
const workspaceInstance = new MetaSheet();
```

## Workspace sequence
### 1. key frame
- create/init workspace
  set seq = X
- close workspace
  set seq = -1
- drag workspace

