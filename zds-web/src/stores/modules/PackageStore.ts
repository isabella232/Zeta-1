import { StoreOptions } from 'vuex';
import * as TYPE from '@/stores/MutationTypes';
import _ from 'lodash';
import moment from 'moment';
import Util from '@/services/Util.service';
import { IHDFSFile, IFile } from '@/types/workspace';
function genereteFolder (path: string) {
  return {
    path: path,
    isFile: false,
    accessTime: moment().valueOf(),
    modifyTime: moment().valueOf(),
    owner: Util.getNt(),
    permission: moment().valueOf(),
  } as IFile;
}
function getPath (pg: IHDFSFile) {
  return pg.filePath;
}
function getNewPath (pg: IHDFSFile){
  return pg.filePath.substring(0, pg.filePath.lastIndexOf('/')+1)+ pg.fileName;
}
const option: StoreOptions<any> = {
  state: {
    packageFiles: {} as Dict<IFile>,
    progress: 0,
    folder: '/user/'+ Util.getNt() + '/',
    cluster: 14,
  },
  mutations: {
    [TYPE.PG_UPDATE_CLUSTER] (state, cluster) {
      state.cluster = cluster;
    },
    [TYPE.PG_UPDATE_FOLDER] (state, folder) {
      state.folder = folder;
    },
    [TYPE.PG_INIT] (state, packages) {
      state.packageFiles = packages;
    },
    [TYPE.PG_ADD] (state, fullPath) {
      state.packageFiles = {
        ...state.packageFiles,
        [fullPath]: genereteFolder(fullPath),
      };
    },
    [TYPE.PG_UPDATE_PACKAGE] (state, pg) {
      if ( getNewPath(pg) === getPath(pg)) return;
      state.packageFiles = {
        ...state.packageFiles,
        [getNewPath(pg)]: Object.assign(state.packageFiles[getPath(pg)], {path: getNewPath(pg)}),
      };
      state.packageFiles = _.omit(state.packageFiles, [getPath(pg)]);
    },
    [TYPE.PG_DELETE_PACKAGE] (state, pg) {
      state.packageFiles = _.omit(state.packageFiles, [getPath(pg)]);
    },
    [TYPE.PG_UPDATE_PROGRESS] (state, progress) {
      state.progress = progress;
    },
  },
  getters: {
    packageFiles: state => state.packageFiles,
    progress: state => state.progress,
    folder: state => state.folder,
    hdfsCluster: state => state.cluster,
  },
  actions: {
    initPackage ({ commit }, { packages }) {
      commit(TYPE.PG_INIT, packages);
    },
    addFolder ({ commit }, fullPath ) {
      commit(TYPE.PG_ADD, fullPath);
    },
    deletePackage ({ commit },  pg ) {
      commit(TYPE.PG_DELETE_PACKAGE, pg);
    },
    updatePackage ({ commit },  pg ) {
      commit(TYPE.PG_UPDATE_PACKAGE, pg);
    },
    updateUploadProgress ({ commit }, progress ){
      commit(TYPE.PG_UPDATE_PROGRESS, progress );
    },
    setFolder ({ commit }, folder ){
      commit(TYPE.PG_UPDATE_FOLDER, folder );
    },
    updateCluster ({ commit }, cluster ){
      commit(TYPE.PG_UPDATE_CLUSTER, cluster );
    },
  },
};
export default option;
