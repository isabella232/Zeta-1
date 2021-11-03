import { RestPacket } from '@/types/RestPacket';
import { IFile } from '@/types/repository';
import { ZPLNote, IPreference, ZPLPackages } from '@/types/workspace';
import { parseName, parseInterpreters } from '@/services/zeppelin/Zeppelin.service';
import Util from '@/services/Util.service';
import _ from 'lodash';
import { IPackage } from '@/types/workspace/notebook.internal';

export function packetMapper(packet: RestPacket.File): IFile {
  let path: string = packet.path || '/';
  // first char
  path.indexOf('/') === 0 ? path : (path = '/' + path);
  path.indexOf('/') === 0 ? path : (path = '/' + path);
  _.findLast(path) !== '/' ? (path = path + '/') : path;

  const file: IFile = {
    path: path,
    notebookId: packet.id,
    nt: packet.nt,
    content: packet.content,
    title: packet.title,
    createTime: packet.createDt,
    updateTime: packet.updateDt ? packet.updateDt : packet.createDt,
    lastRunTime: packet.lastRunDt,
    state: packet.status,
    preference: packet.preference
      ? JSON.parse(packet.preference)
      : undefined,
    selected: false,
    nbType: packet.nbType,
    favorite: false,
  };
  return file;
}
export function zeppelinNoteMapper(note: ZPLNote): IFile {
  const { name, path} = parseName(note.name);
  const connection = parseInterpreters(note.interpreterSettings);
  const preference: IPreference = {
    'notebook.connection': connection,
  };

  const packages: IPackage = {};
  const rmtPackages = note.packages ? JSON.parse(note.packages) || null : null;
  if (rmtPackages) {
    (rmtPackages as ZPLPackages).packages.filter(i => i.body.clusterId = 14).forEach(item => {
      if (!packages[14]) {
        packages[14] = [item.body.path];
      } else {
        packages[14].push(item.body.path);
      }
    });
  }
  const file: IFile = {
    path,
    notebookId: note.id,
    nt: Util.getNt(),
    content: '',
    title: name,
    createTime: note.createDt || 0,
    updateTime: note.updateDt || 0,
    lastRunTime: note.lastRunDt || 0,
    state: '',
    preference,
    selected: false,
    nbType: 'zeppelin',
    seq: note.seq,
    opened: note.opened,
    packages,
    favorite: false,
  };
  return file;
}
