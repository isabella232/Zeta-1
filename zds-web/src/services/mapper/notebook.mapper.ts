import { RestPacket } from '@/types/RestPacket';
import { NotebookStatus, WorkSpaceType, Notebook, ZeppelinNotebook } from '@/types/workspace';
import Util from '@/services/Util.service';
import { IFile } from '@/types/repository';
import { MetaSheetTableRow } from '@/types/meta-sheet';
import _ from 'lodash';
export function packetMapper (src: RestPacket.File): Notebook {
  const status: string = src.status ? src.status.toUpperCase() : '';
  let notebookStatus: NotebookStatus = NotebookStatus.OFFLINE;
  switch (status) {
    case 'CONNECTED':
      notebookStatus = NotebookStatus.IDLE;
      break;
    default:
      break;
  }
  return {
    notebookId: src.id,
    name: src.title,
    code: src.content,
    status: notebookStatus,
    createTime: src.createDt,
    updateTime: src.updateDt,
    preference: src.preference ? JSON.parse(src.preference) : undefined,
    loaded: true,
    nt: src.nt,
    publicReferred: src.publicReferred,
    publicRole: src.publicRole,
    nbType: src.nbType,
    packages: src.packages ? JSON.parse(src.packages) : null,
  } as Notebook;
}
export function zplFileMapper (note: IFile): ZeppelinNotebook {
  return {
    name: note.title,
    notebookId: note.notebookId,
    preference: note.preference,
    packages: note.packages,
    nbType: note.nbType,
    type: 'NOTEBOOK_ZEPPELIN',
  } as ZeppelinNotebook;
}
