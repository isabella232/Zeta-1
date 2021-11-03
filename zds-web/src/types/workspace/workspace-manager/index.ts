import Vuex from 'vuex';
import Vue from 'vue';
// import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
// import MetaSheetRemoteService from '@/services/remote/MetaSheetService';
import ZeppelinApi from '@/services/remote/ZeppelinApi';
import { IWorkspace, WorkSpaceType, WorkspaceBase } from '../workspace.internal';
import { INotebook, NotebookStatus } from '../notebook.internal';
import { wsclient } from '@/net/ws';
import ZeppelinWorkspaceImpl from './zeppelin-workspace-impl';
import WorkspaceRemoteService from '@/services/remote/WorkspaceRemoteService';
import _ from 'lodash';


function diffSequences(newVal: WorkspaceBase[], oldVal: WorkspaceBase[]) {
  if (_.isEmpty(newVal) && _.isEmpty(oldVal)) {
    return false;
  }
  const modifiedList: WorkspaceBase[] = [];
  const deletedList: WorkspaceBase[] = [];
  _.forEach(newVal, currWs => {
    const prevWs = _.find(oldVal, prevWs =>
      currWs.notebookId === prevWs.notebookId
      && currWs.type === prevWs.type);
    if ((prevWs && prevWs.seq != currWs.seq) || !prevWs) {
      // modified sequence
      modifiedList.push({
        notebookId: currWs.notebookId,
        type: currWs.type,
        seq: currWs.seq,
      } as WorkspaceBase);
    }
  });
  _.forEach(oldVal, prevWs => {
    const currWs = _.find(newVal, currWs =>
      currWs.notebookId === prevWs.notebookId
      && currWs.type === prevWs.type);
    if (!currWs) {
      // modified sequence
      deletedList.push({
        notebookId: prevWs.notebookId,
        type: prevWs.type,
        seq: -1,
      } as WorkspaceBase);
    }
  });
  return _.concat(modifiedList, deletedList);
}
export class WorkspaceManager {
  private workspaceRemoteService: WorkspaceRemoteService;
  private zeppelinApi: ZeppelinApi;

  static instance: WorkspaceManager;
  private context: Vue;

  private zplImpl: ZeppelinWorkspaceImpl;

  private _updateSeq: Function;
  private _seqDict: Dict<WorkspaceBase> = {};
  private constructor(context: Vue) {
    this.context = context;
    this.workspaceRemoteService = new WorkspaceRemoteService().props({ path: 'notebook' });
    this.zeppelinApi = new ZeppelinApi().props({ path: 'notebook' });
    this.zplImpl = new ZeppelinWorkspaceImpl(this.zeppelinApi, context);
    this._updateSeq = _.debounce(this.updateSeq, 1000, { trailing: true});
  }
  get $store() {
    return this.context.$store;
  }
  get workspaces(): Dict<WorkspaceBase> {
    return this.$store.state.workspace.workspaces;
  }
  get $confirm() {
    return this.context.$confirm;
  }
  public static getInstance(context: Vue) {
    if (this.instance) {
      return this.instance;
    }
    else {
      this.instance = new WorkspaceManager(context);
      return this.instance;
    }
  }
  /**
   * @deprecated
   * @param ws
   */
  public initWorkspaceTab(ws: IWorkspace) {
    this.$store.dispatch('initWorkspace', ws);

  }

  public initWorkspaceTabs(wss: IWorkspace[]) {
    const wssSorted = _.chain(wss).sortBy('seq').reverse().value();
    wssSorted.forEach((ws, seq) => {
      ws.seq = seq;
      this.$store.dispatch('initWorkspaceLeft', ws);
      if (ws.type === WorkSpaceType.NOTEBOOK_ZEPPELIN){
        this.zplImpl.openTab(ws);
      }
    });
    this.debounceUpdateSeqs(wssSorted);
  }
  public addActiveTabAndOpen(ws: IWorkspace) {
    this.addActiveTab(ws);
    this.openTab(ws.notebookId);
    const wsInstance = this.getWorkspace(ws.notebookId);
    if (wsInstance) {
      this.debounceUpdateSeq(ws.notebookId, ws.type, wsInstance.seq);
      if (ws.type === WorkSpaceType.NOTEBOOK_ZEPPELIN){
        this.zplImpl.openTab(ws);
      }
    }

  }
  private addActiveTab(ws: IWorkspace) {
    const wsInstance = this.getWorkspace(ws.notebookId);
    this.$store.dispatch('addActiveWorkSpace', ws);

  }
  public openTab(id: string) {
    this.$store.dispatch('setActiveNotebookById', id);
    this.context.$router.push('/notebook');
  }
  public async closeTab(id: string) {
    const ws = this.getWorkspace(id) as IWorkspace;
    if (!ws) {
      return;
    }

    if (ws.type === WorkSpaceType.NOTEBOOK
      || ws.type === WorkSpaceType.NOTEBOOK_COLLECTION
      || ws.type === WorkSpaceType.SHARED_NOTEBOOK) {
      const nb = ws as INotebook;
      if (nb.dirty) {
        await this.$confirm(
          'This notebook is not saved. Are you sure to close it?',
          'Close',
          {
            confirmButtonText: 'Close anyway',
            cancelButtonText: 'Cancel',
            type: 'warning',
          }
        );
      }
      // check isRunning
      if (nb.status == NotebookStatus.RUNNING || nb.status == NotebookStatus.SUBMITTING) {
        await this.$confirm(
          'This notebook is running. Are you sure to close it?',
          'Close',
          {
            confirmButtonText: 'Close anyway',
            cancelButtonText: 'Cancel',
            type: 'warning',
          }
        );
      }

      // close connection if is online
      this.$store.dispatch('setNotebookStatus', {
        nid: nb.notebookId,
        status: NotebookStatus.IDLE,
      });
      this.$store.dispatch('setNotebookStatus', {
        nid: nb.notebookId,
        status: NotebookStatus.DISCONNECTING,
      });
      // close livy session
      wsclient.notebookDisconnect(id);
      // call api to set opened = 0, seq = -1
    } else if (ws.type === WorkSpaceType.NOTEBOOK_ZEPPELIN){
      this.zplImpl.closeTab(ws);
    }
    this.$store.dispatch('closeNotebookById', id);
    this.debounceUpdateSeq(id, ws.type);
  }

  public onTabSequenceChange(seqArr: WorkspaceBase[]) {
    const currentSeqArr = _.map(this.workspaces);
    const seqDiff = diffSequences(seqArr, currentSeqArr);
    if (seqDiff) {
      _.forEach(seqDiff, ({ notebookId, seq }) => {
        this.$store.dispatch('updateWorkspace', { notebookId, seq});
      });

      this.debounceUpdateSeqs(seqDiff);
    }
  }
  private getWorkspace(id: string): IWorkspace | undefined{
    return this.$store.getters.nbByNId(id);
  }

  debounceUpdateSeq(id: string, type: WorkSpaceType, seq = -1) {
    const ws = {
      notebookId: id,
      type,
      seq,
    } as WorkspaceBase;
    return this.debounceUpdateSeqs([ws]);
  }

  debounceUpdateSeqs(seqArr: WorkspaceBase[]) {
    _.forEach(seqArr, ws => {
      const uid = ws.notebookId + '_' + ws.type;
      this._seqDict[uid] = ws;
    });
    this._updateSeq(this._seqDict);
  }

  private updateSeq(seqDict: Dict<WorkspaceBase>) {
    if (_.isEmpty(seqDict)) {
      return;
    }
    const seqArr = _.map(seqDict, ({ notebookId, seq, type}) => {
      return {
        id: notebookId,
        seq,
        type,
      };
    });
    this.workspaceRemoteService.updateNotebooksSeq(seqArr);
    this._seqDict = {};
  }

}
