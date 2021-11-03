import { WorkspaceDecorator, WorkSpaceType } from './workspace.internal';
import { Notebook, NotebookStatus } from './notebook.internal';
import Vue from 'vue';
import _ from 'lodash';

export interface NotebookConfig {
  notebookId: string;
  title: string;
  optmzt: Optimization;
  [k: string]: any;
}
export interface Optimization {
  'livy'?: {
    [k: string]: string;
  };
  'jdbc'?: {
    [k: string]: string;
  };
}
export class SubNotebook extends Notebook {
  constructor(...args: any[]){
    super(...args);
    this.nbType = 'sub_nb';
  }
}
@WorkspaceDecorator({
  type: WorkSpaceType.NOTEBOOK_COLLECTION
})
export class MultiNotebook extends Notebook {
  type: WorkSpaceType = WorkSpaceType.NOTEBOOK_COLLECTION;
  subNotebooks: Dict<SubNotebook>;
  constructor(id: string, name: string, subNotebooks: Dict<SubNotebook> = {}) {
    super(id, name);
    this.nbType = 'collection';
	  this.subNotebooks = subNotebooks;
  }
  set status(status: NotebookStatus) {
	  super.status = status;
	  //! comments by tianrsun @2019-07-04
	  //! set sub notebook status
	  //! force set status_
	  _.forEach(this.subNotebooks, (snb: SubNotebook) => {
	    snb.status_ = status;
	  });
  }
  get status() {
	  return super.status;
  }
  contains(notebookId: string) {
	  return _.has(this.subNotebooks, notebookId);
  }
  getSubNotebook(id: string): SubNotebook{
	  return this.subNotebooks[id];
  }
  addSubNotebook(snb: SubNotebook) {
	  const snbObj = new SubNotebook().props(snb);
	  //! comments by tianrsun @2019-07-05
	  //! set sub notebook status
	  //! force set status_
	  snbObj.status_ = this.isOnline ? NotebookStatus.IDLE : NotebookStatus.OFFLINE;

	  Vue.set(this.subNotebooks,snbObj.notebookId, snbObj);
	  return this;
  }
  addSubNotebooks(subNotebooks: Dict<SubNotebook>) {
	  for(const id in subNotebooks) {
	    this.addSubNotebook(subNotebooks[id]);
	  }
	  return this;
  }
  updateSubNotebook(notebookId: string,snb: Partial<SubNotebook>): boolean {
	  const exists = this.contains(notebookId);
	  if(exists) {
	    const subNotebook = this.getSubNotebook(notebookId);
	    subNotebook.props(snb);
	    return true;
	  }
	  return false;
  }
  /**
	 * create or update
	 * @param sub
	 */
  setSubNotebook(sub: SubNotebook) {
	  const exists = this.updateSubNotebook(sub.notebookId, sub);
	  if(!exists) {
	    this.addSubNotebook(sub);
	  }
	  return this;
  }

  delSubNotebook(notebookId: string) {
	  const exists = this.contains(notebookId);
	  if(exists) {
	    Vue.delete(this.subNotebooks, notebookId);
	    return true;
	  }
	  return false;
  }
}
