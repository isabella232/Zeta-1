import { Notebook, CodeType } from './notebook.internal';
import { WorkspaceDecorator, WorkSpaceType } from './workspace.internal';

@WorkspaceDecorator({
  type: WorkSpaceType.NOTEBOOK_ZEPPELIN
})
export class ZeppelinNotebook extends Notebook {
  get iconClass() {
    const cnn = this.preference && this.preference['notebook.connection'] ? this.preference['notebook.connection'] : undefined;
    const defaultAlias = 'apollo';
    if (!cnn) {
      return defaultAlias.toLowerCase();
    }
    if (cnn.codeType === CodeType.KYLIN) {
      return 'kylin';
    } else {
      const alias = (cnn.alias || defaultAlias).toLowerCase();
      if (alias.indexOf('apollo') >= 0) {
        return 'apollo';
      } else if (alias.indexOf('zeta') >= 0) {
        return 'apollo';
      } else {
        return alias;
      }
    }
  }
  constructor(...args: any[]){
    super(...args);
    this.nbType = 'zeppelin';
  }
}
