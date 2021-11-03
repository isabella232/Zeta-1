import { Module } from 'vuex';
import { IRepositoryStore } from '@/types/repository';
import { IStore } from '../index';
import * as MUT from '../MutationTypes';
import { IFile } from '@/types/repository';
import { INotebook, INotebookConfig, IOptimization } from '@/types/workspace';
import _ from 'lodash';
import moment from 'moment';

function getPathPrefix(path: string) {
  console.warn(path);
  if (path.charAt(path.length - 1) !== '/') {
    console.error(path);
  }
  return path + (path.charAt(path.length - 1) === '/' ? '' : '/');
}

function getIndex(file: IFile) {
  // return getPathPrefix(file.path) + file.notebookId;
  return file.notebookId;
}

function getIndexByNotebookId(notebookId: string, files: Dict<IFile>) {
  for (const index in files) {
    if (files[index].notebookId === notebookId) {
      return index;
    }
  }
  return null;
}

const options: Module<IRepositoryStore, IStore> = {
  state: {
    files: {} as Dict<IFile>,
    updateTime: moment().format('x'),
    loading: true
  },
  getters: {
    selectedFiles: state => _.pickBy(state.files, file => file.selected),
    fileByNb: state => (nb: INotebook) => {
      for (const path in state.files) {
        if (path.indexOf(nb.notebookId) !== -1)
          return state.files[path];
      }
      return null;
    },
    fileByNId: state => (nId: string) => {
      for (const path in state.files) {
        if (path.indexOf(nId) !== -1)
          return state.files[path];
      }
      return null;
    },
    cfgs: state => () => {
      const cfgs: Dict<INotebookConfig | undefined> = {};
      const cfgsArr = _.chain(state.files)
        .filter((file: IFile) => file.path.indexOf('/conf') >= 0)
        .forEach((file: IFile) => {
          if (!_.isEmpty(file.content)) {
            try {
              cfgs[file.title] = {
                notebookId: file.notebookId,
                title: file.title,
                optmzt: JSON.parse(file.content as string)
              };
            } catch (e) {
              cfgs[file.title] = {
                notebookId: file.notebookId,
                title: file.title,
                optmzt: {} as IOptimization
              };
            }
          } else {
            cfgs[file.title] = cfgs[file.title] = {
              notebookId: file.notebookId,
              title: file.title,
              optmzt: {} as IOptimization
            };
          }
        })
        .value();
      return cfgs;
    }
  },
  actions: {
    setFileSelected({ commit, state }, { pathname, selected }) {
      commit(MUT.REPO_SET_FILE_SELECTED, { pathname, selected });
    },
    setFolderSelected({ commit }, { folderName, selected }) {
      commit(MUT.REPO_SET_FILE_SELECTED_BY_FOLDER, { folderName, selected });
    },
    clearAllSelectedFiles({ commit, state, getters, dispatch }) {
      const selecteds = Object.keys(getters.selectedFiles);
      selecteds.forEach((file: string) =>
        dispatch('setFileSelected', { pathname: file, selected: false })
      );
    },
    addFile({ commit, state }, file: IFile) {
      commit(MUT.REPO_ADD_FILE, file);
    },
    addFiles({ commit, state }, files: Dict<IFile>) {
      commit(MUT.REPO_ADD_FILES, files);
    },
    initRepoFiles({ commit, state }, files: Dict<IFile>) {
      commit(MUT.REPO_INIT_FILES, files);
    },
    deleteFile({ commit, state }, file: IFile) {
      commit(MUT.REPO_DELETE_FILE, file);
    },
    updateFile({ commit, state }, file: IFile) {
      commit(MUT.REPO_UPDATE_FILE, file);
    },
    syncNbToFile({ commit, state }, nb: INotebook) {
      let file: IFile | undefined;
      for (const path in state.files) {
        if (path.indexOf(nb.notebookId) != -1) {
          file = state.files[path];
          break;
        }
      }

      if (file) {
        state.files = {
          ...state.files,
          [file.notebookId]: {
            ...file,
            content: nb.code,
            title: nb.name
          }
        };
      }
    },
    upadateFileLastRun({ commit, state }, { notebookId, startTime }) {
      /** 
			 * modify by @tianrsun @2019-04-01
			 * enhance sub notebook
			 * won't update when it's subnotebook
			 */
      const path = getIndexByNotebookId(notebookId, state.files);
      if(path === null) return;
      commit(MUT.REPO_UPDATE_FILE_LAST_RUN, {
        pathname: path,
        lastRunTime: startTime
      });
    },
    setFavoriteNote({ commit, state }, { notebookId }) {
      const pathname = getIndexByNotebookId(notebookId, state.files);
      commit(MUT.REPO_SET_FAVORITE_NOTE, { pathname });
      commit('favorite', { id: notebookId, favoriteType: 'nb' });
    },
    setUnfavoriteNote({ commit, state }, { notebookId }) {
      const pathname = getIndexByNotebookId(notebookId, state.files);
      commit(MUT.REPO_SET_UNFAVORITE_NOTE, { pathname });
      commit('unfavorite', { id: notebookId, favoriteType: 'nb' });
    }
  },
  mutations: {
    [MUT.REPO_SET_FILE_SELECTED](state, { pathname, selected }) {
      state.files[pathname].selected = selected;
    },
    [MUT.REPO_SET_FILE_SELECTED_BY_FOLDER](state, { folderName, selected }) {
      _.forEach(state.files, (file, pathName) => {
        if(pathName.indexOf(folderName) >= 0){
          file.selected = selected;
        }
				
      });
    },
    [MUT.REPO_ADD_FILE](state, file: IFile) {
      state.files = {
        ...state.files,
        [file.notebookId]: file
      };
      state.updateTime = moment().format('x');
    },
    [MUT.REPO_DELETE_FILE](state, file: IFile) {
      state.files = _.omit(state.files, [getIndex(file)]);
      state.updateTime = moment().format('x');
    },
    [MUT.REPO_UPDATE_FILE](state, file: IFile) {
      state.files = {
        ...state.files,
        [getIndex(file)]: file
      };
      state.updateTime = moment().format('x');
    },
    [MUT.REPO_ADD_FILES](state, files: Dict<IFile>) {
      // state.files = _.assign(state.files,files);
      _.forEach(files, (file: IFile, k: string) => {
        state.files = {
          ...state.files,
          [getIndex(file)]: file
        };
      });
      state.updateTime = moment().format('x');
    },
    [MUT.REPO_INIT_FILES](state, files: Dict<IFile>) {
      state.files = files;
      state.updateTime = moment().format('x');
      state.loading = false;
    },
    [MUT.REPO_UPDATE_FILE_LAST_RUN](state, { pathname, lastRunTime }) {
      state.files[pathname].lastRunTime = lastRunTime;
    },
    [MUT.REPO_SET_FAVORITE_NOTE](state, { pathname }) {
      state.files[pathname].favorite = true;
    },
    [MUT.REPO_SET_UNFAVORITE_NOTE](state, { pathname }) {
      state.files[pathname].favorite = false;
    },
  }
};

export default options;
