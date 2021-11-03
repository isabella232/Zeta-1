import { ZPLNote, IConnection } from '@/types/workspace';
import _ from 'lodash';
import { settings } from 'cluster';
export function filterZeppelinNotes(notes: ZPLNote[]) {
  return notes.filter(note => {
    /** filter trash */
    const isTrash = _.startsWith(note.name, '~Trash/');
    const isTutorial = _.startsWith(note.name, 'Zeppelin Tutorial/');
    return !isTrash && !isTutorial;
  });
}

export function parseName(name: string): { name: string; path: string} {
  let path = '/';
  let filename = '';
  const paths = name.split('/');
  filename = _.last(paths) || name;
  if (paths.length > 1) {
    path = _.chain(paths).splice(0, paths.length -1).compact().join('/').value();
    path = path ? `/${path}/` : '/';
  }
  return {
    path,
    name: filename
  };
}

export function parseInterpreters(interpreterSettings: any[]) {
  let alias = 'apollo';
  // const isKylin = _.chain(interpreterSettings)
  //   .filter(setting => setting.selected)
  //   .find(setting => setting.name === 'kylin')
  //   .value();
  // const isHermes = _.chain(interpreterSettings)
  //   .filter(setting => setting.selected)
  //   .find(setting => setting.name === 'hermes')
  //   .value();
  const interpreter = _.chain(interpreterSettings)
    .filter(setting => setting.selected)
    .filter(setting => setting.id != 'md' && setting.id != 'angular')
    .find()
    .value();
  if (interpreter && interpreter.id) {
    switch (interpreter.id) {
      case 'kylin':
        alias = 'kylin';
        break;
      case 'hermes':
        alias = 'hermes';
        break;
    }
  }
  // const defaultAlias = isKylin ? 'kylin' : 'apollo';
  return {
    alias,
  } as IConnection;
}
export function parseDefaultInterpreters(interpreterSettings: any[]) {
  const interpreter = _.chain(interpreterSettings)
    .filter(setting => setting.selected)
    .find()
    .value();
  return interpreter && interpreter.name ? interpreter.name : 'pyspark';
}