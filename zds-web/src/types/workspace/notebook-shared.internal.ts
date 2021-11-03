import { Notebook } from './notebook.internal';
import { WorkspaceDecorator, WorkSpaceType } from './workspace.internal';
import uuid from 'uuid';
@WorkspaceDecorator({
  type: WorkSpaceType.SHARED_NOTEBOOK,
})
export class SharedNotebook extends Notebook {
  constructor (id: string = uuid(), name = 'Untitled') {
    super(id, name);
    this.name = `[Shared] ${name}`;
  }
}
