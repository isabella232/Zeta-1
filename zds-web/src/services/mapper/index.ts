import * as IFileMapper from './file.mapper';
import * as INotebookMapper from './notebook.mapper';
import * as IPacketMapper from './packet.mapper';
import * as IZeppelinNoteMapper from './zeppelin-note.mapper';
import _ from 'lodash';
export default {
  dictMapper<T>(dict: Dict<T>): Array<T> {
    return _.map(dict, v => v);
  }
};
export {
  IFileMapper,
  INotebookMapper,
  IPacketMapper,
  IZeppelinNoteMapper
};
