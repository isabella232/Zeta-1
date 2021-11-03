import Vue from 'vue';
import config from '@/config/config';
import Util from '@/services/Util.service';
import axios from 'axios';
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import ZeppelinApi from '@/services/remote/ZeppelinApi';
import { WorkspaceSrv } from '@/services/Workspace.service';
import { parseName } from '@/services/zeppelin/Zeppelin.service';
import _ from 'lodash';
import DashboardRemoteService from '@/services/remote/Dashboard';
import { EventBus } from '@/components/EventBus';
import { IDashboardFile, LayoutConfig, ZPLNote, WorkSpaceType, PublicNotebookItem, CodeType, IConnection, NotebookStatus, WorkspaceBase, IWorkspace, IFile } from '@/types/workspace';
import { config as NavCfg, NavAction, NavSubView, ZETA_ACTIONS } from '@/components/common/Navigator/nav.config';
import { IFileMapper } from '@/services/mapper';
import moment from 'moment';
import { WorkspaceManager } from '@/types/workspace/workspace-manager';
import Cookies from 'js-cookie';
import { ZetaException } from '@/types/exception';
import MetaSheetService from '@/services/remote/MetaSheetService';

import MetaSheetUtil from '@/components/MetaSheet/util';
import { DSSLogin } from './dss-login';

import { wsclient } from '@/net/ws';
import { initRepository } from './workspace-init.service';
import { updateTableJson, updateColumnJson } from '@/components/WorkSpace/Notebook/Editor/autoCompleteJson';
const notebookRemoteService = new NotebookRemoteService();
const zeppelinApi = new ZeppelinApi();
const dashboardRemoteService = new DashboardRemoteService();
const metaSheetService = new MetaSheetService();
export const getErrorHandler = (app: Vue) => {
  return (ex: ZetaException) => {
    // const ex = new ZetaException({ code: '', errorDetail: { message: err.message } });
    app.$store.dispatch('addException', {exception: ex});
  };
};

const getZetaToken = (nt: string, ihubtoken: string) => {
  const url = config.zeta.auth as string;
  const auth = btoa(`${nt}:${ihubtoken}`);
  return axios(url, {
    method: 'post',
    data: {},
    headers: {
      Authorization: 'Basic ' + auth,
    },
  });
};

const renewZetaToken = (nt: string, ihubToken = '') => {
  return new Promise((resolve, reject) => {
    getZetaToken(nt, ihubToken).then(res => {
      if (res && res.data) {
        Util.setZetaToken(res.data.token);
        resolve(res.data.token);
      } else {
        reject('error');
      }
    }).catch(reject);
  });
};

export const loginWithToken = () => {
  return DSSLogin().then(nt => renewZetaToken(nt, Util.getDacToken()));
};
export const loginWithoutToken = () => {
  return DSSLogin();
};
export const clearAllToken = () => {
  Cookies.remove('ihub-nt');
  Cookies.remove('ihub-redirect');
  Cookies.remove('ihub-token');
  Cookies.remove('ihub-user');
  Cookies.remove('ihub-otkninsighthub');
  Util.removeZetaToken();
};


export const initNt = (app: Vue): Promise<any> => {
  const store = app.$store;
  const nt = Util.getNt();
  const promise0 = store.dispatch('initUserInfo', { nt });
  const promise1 = store.dispatch('initUserPreference', {nt});
  // async fetch
  const promise2 = store.dispatch('setUserBatchAcct', { nt });
  return Promise.all([promise0, promise1, promise2]);
};


export const initZetaSheets = (app: Vue) => {
  const store = app.$store;
  return metaSheetService.getMetaSheets().then(({data}) => {
    const processedMetaSheets = MetaSheetUtil.processMetaTableItem(data);
    store.dispatch('initMetasheet', {metasheets: processedMetaSheets});
  }).catch((err: ZetaException) => {
    err.message = 'init Zeta Sheet notebook error';
  });
};

export const initWorkspace = (app: Vue) => {
  updateTableJson();
  updateColumnJson();
  wsclient.context = app;
  wsclient.connect();
  return initRepository(app);
};
function zetaMapper (src: any): PublicNotebookItem {
  const connection = JSON.parse(src.preference)['notebook.connection'] as IConnection;
  let interpreter = '';
  switch (connection.codeType) {
    case CodeType.TERADATA:
      interpreter = 'TD SQL';
      break;
    case CodeType.SQL:
      interpreter = 'SPARK SQL';
      break;
    case CodeType.HIVE:
      interpreter = 'HIVE SQL';
      break;
    case CodeType.KYLIN:
      interpreter = 'KYLIN SQL';
      break;
    case CodeType.TEXT:
      interpreter = 'TEXT';
      break;
    case CodeType.SPARK_PYTHON:
      interpreter = 'SPARK PYTHON';
      break;
  }
  const platform = WorkspaceSrv.getPlatformIconByConnection(connection, 'single');
  return {
    id: src.id,
    title: src.title,
    createDt: src.createDt,
    updateDt: src.updateDt,
    nt: src.nt,
    interpreter,
    platform,
    path: '/',
    isZeppelin: false,
  } as PublicNotebookItem;
}
function zeppelinMapper (src: any): PublicNotebookItem {
  const interpreter = src.interpreterSettings && src.interpreterSettings[0] && src.interpreterSettings[0].name ?
    src.interpreterSettings[0].name : '';
  let platform = 'apollo';
  if (interpreter === 'kylin') {
    platform = 'kylin';
  } else if (interpreter === 'python') {
    platform = 'python';
  }
  const owner = src.owners && src.owners[0]? src.owners[0]: '';
  const { path, name } = parseName(src.title);
  // const file = parseName(src.title);
  return {
    id: src.id,
    title: name,
    createDt: src.createDt,
    updateDt: src.updateDt,
    nt: owner,
    interpreter: 'Stacked',
    platform,
    isZeppelin: true,
    path,
  } as PublicNotebookItem;
}
export const initPublicNotebookStore = (app: Vue) => {
  const store = app.$store;
  const zetaReq = notebookRemoteService.getAllPublicNotebook()
    .catch((err: ZetaException) => {
      err.message = 'Fetch Public notebook error';
      // getErrorHandler(app)(err);
      return {
        data: [],
      };
    });
  const zplReq = zeppelinApi.getPublicNotes()
    .catch((err: ZetaException) => {
      err.message = 'Fetch Stacked Public notebook error';
      // getErrorHandler(app)(err);
      return {
        data: {
          body: [],
        },
      };
    }).then(res => res);
  return Promise.all([zetaReq, zplReq]).then((ress) => {
    const zetaNbs = ress[0].data.map(zetaMapper) as PublicNotebookItem[];
    const zplNbs = ress[1].data.body.map(zeppelinMapper) as PublicNotebookItem[];
    const nbs: PublicNotebookItem[] = zetaNbs;
    return nbs.concat(zplNbs);
  }).then(nbs => store.dispatch('initPublicNotebooks', nbs));
};
export const initFavoriteStore = (app: Vue) => {
  const store = app.$store;
  return notebookRemoteService.favoriteList().then(({data}) => {
    store.dispatch('initFavoriteList', data);
  }).catch((err: ZetaException) => {
    err.message = 'Fetch Favorite notebook error';
    // getErrorHandler(app)(err);
  });
};
export const initSharedFavoriteStore = (app: Vue) => {
  const store = app.$store;
  return notebookRemoteService.favoriteShareList(Util.getNt()).then(({data}) => {
    store.dispatch('initFavoriteList', data);
  }).catch((err: ZetaException) => {
    err.message = 'Fetch Favorite notebook error';
    // getErrorHandler(app)(err);
  });
};

export const initDashboard = (app: Vue) => {
  const store = app.$store;

  return dashboardRemoteService.getAll().then(data => {
    const dbs = data.data;
    const dbDict: Dict<IDashboardFile> = {};
    _.chain(dbs)
      .forEach(db => {
        dbDict[db.id] = {
          id: db.id,
          nt: db.nt,
          name: db.name,
          layoutConfigs: db.layoutConfig
            ? (JSON.parse(db.layoutConfig) as LayoutConfig[])
            : [],
          createTime: db.createDt,
          updateTime: db.updateDt ? db.updateDt : null,
        } as IDashboardFile;
      })
      .value();
    store.dispatch('initDashboard', { dashboards: dbDict });
  }).catch((err: ZetaException) => {
    err.message = 'Fetch dashbroads error';
    // getErrorHandler(app)(err);
  });
};
export const initSchedule = (app: Vue) => {
  const store = app.$store;
  return store.dispatch('initTaskList')
    .catch((err: ZetaException )=>{
      err.message = 'Fetch Schedule error';
    });
};
export const initStore = (app: Vue) => {

  return new Promise((resolve, reject) => {
    // app.$zetaLoading('loading user info', 0);
    initNt(app)
      .then(() => {
        app.$zetaLoading('Loading favorite notebook', 10);
        return initFavoriteStore(app);
      })
      .then(() => {
        app.$zetaLoading('Loading public notebook', 20);
        return initPublicNotebookStore(app);
      })
      .then(() => app.$store.dispatch('getKylinProjects'))
      .then(() => {
        // console.log('LOADINGTEST','load workspace')
        app.$zetaLoading('init zetasheet', 40);
        return initZetaSheets(app);
      })
      .then(() => {
        // console.log('LOADINGTEST','load workspace')
        app.$zetaLoading('init workspace', 60);
        return initWorkspace(app);
      })
      .then(() => {
        app.$zetaLoading('loading dashboards', 80);
        return initDashboard(app);
      })
      .then(() => {
        app.$zetaLoading('loading schedule', 90);
        return initSchedule(app);
      })
      .then(() => {
        // console.log('LOADINGTEST','after load schedule');
        app.$zetaLoading('loading schedule', 100);
        resolve();
      })
      .catch(e => {
        app.$zetaLoading('loading schedule', 100);
        reject(e);
      });
  });
};
export const initKeyboardListener = () => {
  if (document) {
    document.onkeydown = (event: KeyboardEvent) => {
      const metaKey =
				Util.getOS() === 'MAC' ? event.metaKey : event.ctrlKey;
      if (event.keyCode == 83 && metaKey) {
        // event.preventDefault();
      }
    };
  }
};

function openOrCreateZplPubNote (id: string, fullname: string, context: Vue) {
  const zeppelinApi = new ZeppelinApi().props({path: 'notebook'});
  const loading = context.$loading({
    lock: true,
    text: 'Cloning Notebook',
    spinner: 'el-icon-loading',
    background: 'rgba(0, 0, 0, 0.7)',
  });
  zeppelinApi.publish(id, { name: fullname }).then((res) => {
    const notebookId = res.data.body;
    const { name } = parseName(fullname);
    const ws = WorkspaceSrv.notebook({
      notebookId,
      name,
      type: WorkSpaceType.NOTEBOOK_ZEPPELIN,
    });
    WorkspaceManager.getInstance(context).addActiveTabAndOpen(ws);

    const file = IFileMapper.zeppelinNoteMapper({
      name: fullname,
      id: notebookId,
      opened: 1,
      interpreterSettings: [],
      seq: -1,
      createDt: moment().valueOf(),
    });
    context.$store.dispatch('addFile', file);
    loading.close();
  }).catch(() => {
    loading.close();
  });
}
function openSharedNotebook(app: Vue, notebookId: string) {
  let ws: IWorkspace = WorkspaceSrv.sharedNotebook(notebookId, 'Untitled');
  // case owner is current user;
  const file = app.$store.getters.fileByNId(notebookId);
  if (file) {
    ws = WorkspaceSrv.notebook({
      notebookId: file.notebookId,
      name: file.title,
      /* TODO: file.content is of type string?
               * May need lazy load.
               */
      code: file.content || '',
      createTime: file.createTime,
      updateTime: file.updateTime,
      preference: file.preference,
      nbType: file.nbType,
    });
  }
  WorkspaceManager.getInstance(app).addActiveTabAndOpen(ws);
}
export function	excuteInternalAction (app: Vue){
  app.$watch('$route.query.internal_action', (action) => {
    if (!action){
      return ;
    }
    const internalActionParams: string | string[] = app.$route.query.internal_action_params;
    const params: any = (app.$route.query.internal_action_params) ?
      JSON.parse(internalActionParams.toString()) : {};

    console.log('internal action', action);
    console.log('internal action params', params);
    let actionCfg, nav, url;
    switch (action){
      case ZETA_ACTIONS.OPEN_METADATA:
        nav = NavCfg.find(c => c.id == 'Metadata') as NavSubView;
        EventBus.$emit('open-sub-view', nav);
        url = `${location.protocol}//${location.host}/${Util.getPath()}#${app.$route.path}`;
        location.href = url;
        break;
      case ZETA_ACTIONS.ADD_DATA_MOVE:
				 actionCfg = NavCfg.find(c => c.id == 'datamove') as NavAction;
				 break;
      case ZETA_ACTIONS.ADD_DATA_VALIDATION:
        actionCfg = NavCfg.find(c => c.id == 'datavalidation') as NavAction;
        break;
      case ZETA_ACTIONS.ADD_SQL_CONVERT:
        actionCfg = NavCfg.find(c => c.id == 'covert') as NavAction;
        break;
      case ZETA_ACTIONS.OPEN_PUBLIC_REPORTS:
        nav = NavCfg.find(c => c.id == 'PublicReport') as NavSubView;
        EventBus.$emit('open-sub-view', nav);
        url = `${location.protocol}//${location.host}/${Util.getPath()}#${app.$route.path}`;
        location.href = url;
        break;
      case ZETA_ACTIONS.CLONE_ZPL_PUB_NOTE:
        openOrCreateZplPubNote(params.noteId, params.fullname, app);
        break;
      case ZETA_ACTIONS.OPEN_SHARED_NOTEBOOK:
        openSharedNotebook(app, params.notebookId);
        break;
      // case ZETA_ACTIONS.CLONE_ZPL_NOTE:
      //   openOrCreateZplNote(params.noteId, params.fullname, app);
      //   break;
    }
    if (actionCfg) {
      actionCfg.action(EventBus, app.$router, app.$store);
    }
  }, {immediate: true});
}



