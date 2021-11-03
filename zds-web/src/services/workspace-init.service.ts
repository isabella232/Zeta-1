import Vue from 'vue';
import ZeppelinApi from '@/services/remote/ZeppelinApi';
import WorkspaceRemoteService from '@/services/remote/WorkspaceRemoteService';
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import { ZetaException } from '@/types/exception';
import { IWorkspace, NotebookStatus, WorkspaceBase, WorkSpaceType, ZPLNote } from '@/types/workspace';
import { RestPacket } from '@/types/RestPacket';
import { IFile } from '@/types/repository';
import { IFileMapper, INotebookMapper } from '@/services/mapper';
import { filterZeppelinNotes } from '@/services/zeppelin/Zeppelin.service';
import { WorkspaceSrv as NBSrv, WorkspaceSrv } from '@/services/Workspace.service';
import _ from 'lodash';
import { WorkspaceManager } from '@/types/workspace/workspace-manager';
import { wsclient } from '@/net/ws';
const zeppelinApi = new ZeppelinApi();
const workspaceRemoteService = new WorkspaceRemoteService();
const notebookRemoteService = new NotebookRemoteService();
export const initRepository = (app: Vue) => {
  const store = app.$store;
  const zplRepo = zeppelinApi.getNotes().then(({data}) => {
    return data.body;
  }).catch((err: ZetaException) => {
    err.message = 'Fetch Stacked notebook error';
    // getErrorHandler(app)(err);
    console.error('Fetch Zeppeline Api error', err);
    return [] as ZPLNote[];
  });
  const zetaRepo = notebookRemoteService.getAll().then(({data}) => data);
  // const favoritePromise = notebookRemoteService.favoriteList().then(({data}) => data);
  return Promise.all([zplRepo, zetaRepo]).then((ress) => {
    const zplFilesSrc: ZPLNote[] = ress[0];
    const zetaFilesSrc: RestPacket.File[] = ress[1];
    const zetaFiles: IFile[] = zetaFilesSrc.map(IFileMapper.packetMapper);
    const zeppelinFiles: IFile[] = filterZeppelinNotes(zplFilesSrc).map(IFileMapper.zeppelinNoteMapper);
    const files = _.concat(zeppelinFiles, zetaFiles);
    const filesR: Dict<IFile> = {};
    files.forEach(f => {
      let path = f.path;
      const reg = /^\/$|.+\/$/;
      path.match(reg) ? (path = path) : (path = path + '/');
      // let prefix = path;
      filesR[f.notebookId] = f;
    });
    store.dispatch('initRepoFiles', filesR);
    return filesR;
  });

};

export const initNotebook = (app: Vue, allFiles: Dict<IFile>): Promise<any> => {
  const store = app.$store;
  // fetch last opened nbs
  return workspaceRemoteService.getOpened().then(( {data} ) => {
    /** workspaces from server */
    const wss: IWorkspace[] = _.chain(data)
      .filter(noteSeq => noteSeq.seq >= 0)
      .map(noteSeq => {
        let workspaceInstance: IWorkspace | null = null;
        // convert to notebook if is owner of shared notebook
        if (noteSeq.type as WorkSpaceType === WorkSpaceType.SHARED_NOTEBOOK
          && store.getters.fileByNId(noteSeq.id)) {
          noteSeq.type = WorkSpaceType.NOTEBOOK;
        }
        if (noteSeq.type as WorkSpaceType === WorkSpaceType.META_SHEET) {
          const metasheet = store.getters.metasheetById(noteSeq.id);
          if (metasheet) {
            workspaceInstance = WorkspaceSrv.getMetaSheet(metasheet, noteSeq.seq);
          }
        } else if (noteSeq.type as WorkSpaceType === WorkSpaceType.SHARED_NOTEBOOK) {
          workspaceInstance = WorkspaceSrv.sharedNotebook(noteSeq.id, noteSeq.name, noteSeq.seq);
        } else if (noteSeq.type as WorkSpaceType === WorkSpaceType.NOTEBOOK
          || noteSeq.type as WorkSpaceType === WorkSpaceType.NOTEBOOK_COLLECTION) {
          const file: null | IFile = store.getters.fileByNId(noteSeq.id);
          if (file) {
            workspaceInstance = NBSrv.notebook({
              notebookId: file.notebookId,
              name: file.title,
              code: file.content || '',
              createTime: file.createTime,
              updateTime: file.updateTime,
              preference: file.preference,
              status: file.state === 'Connected' ?  NotebookStatus.IDLE : NotebookStatus.OFFLINE,
              nbType: file.nbType,
            });
          }
        } else if (noteSeq.type as WorkSpaceType === WorkSpaceType.NOTEBOOK_ZEPPELIN) {
          const file: null | IFile = store.getters.fileByNId(noteSeq.id);
          if (file) {
            const iNote = INotebookMapper.zplFileMapper(file);
            workspaceInstance = NBSrv.zeppelinNotebook(iNote);
          }
        }
        if (workspaceInstance) {
          workspaceInstance.seq = noteSeq.seq;
        }
        return workspaceInstance;
      })
      .compact()
      .sortBy('seq')
      .value();
    /** insert workspaces in left */
    WorkspaceManager.getInstance(app).initWorkspaceTabs(wss);
    const activedWorkspaceId = app.$store.state.workspace.activeWorkspaceId;
    if (!activedWorkspaceId && wss && wss[0]) {
      store.dispatch('setActiveNotebookById', wss[0].notebookId);
    }
  });
};

export function syncNotebook(app: Vue,) {
  const wss = app.$store.state.workspace.workspaces;
  const ids = _.chain(wss).filter((ws: WorkspaceBase) => ws.type === WorkSpaceType.NOTEBOOK)
    .map(ws => ws.notebookId).value();
  ids.forEach(id => {
    wsclient.syncNotebookResult(id);
  });
}
