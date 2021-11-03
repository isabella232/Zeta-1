import { IFile } from '@/types/repository';
import { RestPacket } from '@/types/RestPacket';
import { INotebook, NotebookStatus, CodeType, ZeppelinNotebook } from '@/types/workspace';
import uuid from 'uuid';
import moment from 'moment';
import _ from 'lodash';
import {
  WorkSpaceType,
  ITools,
  IMetadata,
  Notebook,
  Source,
  Dashboard,
  IWorkspace,
  MultiNotebook,
  MetaSheet
} from '@/types/workspace';
import { IDashboardFile } from '@/types/workspace/dashboard.internal';
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import Util from './Util.service';
import config from '@/config/config';
import { getAvailableStatus, IConnection, NotebookType } from '@/types/workspace/notebook.internal';
import { INotebookMapper } from './mapper';
import { MetaConfigTableRow, MetaSheetTableRow, SchemeConfig, SourceConfig } from '@/types/meta-sheet';
import { SharedNotebook } from '@/types/workspace/notebook-shared.internal';
class WorkspaceService {
  notebookRemoteService: NotebookRemoteService;
  constructor() {
	  this.notebookRemoteService = new NotebookRemoteService().props({path: 'notebook'});
  }
  /**
   * @deprecated
   * @param name
   * @param type
   */
  tool(name: string, type: WorkSpaceType): ITools {
	  return {
	    notebookId: uuid(),
	    name,
	    type,
	    seq: -1
	  } as ITools;
  }
  /**
   * @deprecated
   * @param param0
   */
  metadata({ name, rowType, platformOptions }: Source): IMetadata {
	  return {
	    notebookId: uuid(),
	    name,
	    type: WorkSpaceType.METADATA,
	    platform: platformOptions[0],
	    platformOptions: platformOptions,
	    rowTypeOptions: rowType,
	    init: false,
	    seq: -1
	  } as IMetadata;
  }
  notebook(i: Partial<INotebook>): Notebook | MultiNotebook {
    if (i.nbType && i.nbType === 'zeppelin') {
      return this.zeppelinNotebook(i);
    }
	  const nb: Partial<INotebook> = {
	    notebookId: uuid(),
	    name: 'Untitled',
	    code: '',
	    jobs: [],
	    status_: i.status || NotebookStatus.OFFLINE,
	    createTime: moment().valueOf(),
	    updateTime: moment().valueOf(),
	    preference: undefined,
	    stagedCode: '',
	    dirty: false,
	    loaded: false,
	    resultUpdated: false,
	    monitorUrl: null,
	    seq: -1,
	    type: (i.nbType && i.nbType == 'collection') ? WorkSpaceType.NOTEBOOK_COLLECTION :WorkSpaceType.NOTEBOOK,
	    nt:Util.getNt(),
	    publicReferred: null,
	    publicRole: 'no_pub',
	    nbType: 'single',
	    ...i
	  };
	  const _nb =  (i.nbType && i.nbType == 'collection')?new MultiNotebook(uuid(),'Untitled') : new Notebook(uuid(),'Untitled');
	  _nb.props(nb);
	  return _nb;

  }
  zeppelinNotebook(i: Partial<INotebook>): ZeppelinNotebook {
	  const nb: Partial<INotebook> = {
	    notebookId: uuid(),
	    name: 'Untitled',
	    code: '',
	    jobs: [],
	    status: NotebookStatus.INITIAL,
	    createTime: moment().valueOf(),
	    updateTime: moment().valueOf(),
	    preference: undefined,
	    stagedCode: '',
	    dirty: false,
	    loaded: false,
	    resultUpdated: false,
	    monitorUrl: null,
	    seq: -1,
	    type: WorkSpaceType.NOTEBOOK_ZEPPELIN,
	    nt:Util.getNt(),
	    publicReferred: null,
	    publicRole: 'no_pub',
	    nbType: 'zeppelin',
	    ...i
	  };
	  const _nb =  new ZeppelinNotebook(uuid(),'Untitled');
	  _nb.props(nb);
	  return _nb;

  }

  file2nb = (src: RestPacket.File): INotebook => {
	  const inb = INotebookMapper.packetMapper(src);
	  const nb: INotebook = this.notebook(inb);
	  // TODO enhance multi notebook
	  if(nb.nbType == 'collection') {
	    const subFiles = src.subNotebooks;
	    _.forEach(subFiles, sFile => {
	      const snb = this.file2nb(sFile);
	      if(snb){
	        (nb as MultiNotebook).addSubNotebook(snb);
	      }

	    });
	  }
	  if(src.collectionId) {
	    nb.collectionId = src.collectionId;
	  }
		  // set last choice
	  if(nb.preference && nb.preference['notebook.connection'] && nb.preference['notebook.connection'].codeType){
	    if(!nb.preference['notebook.connection'].alias){
	      const defaultCnn = Util.getDefaultConnectionByPlat(nb.preference['notebook.connection'].codeType).defaultConnection;
	      Object.assign(nb.preference['notebook.connection'], defaultCnn);
	    }
	    nb.connection = nb.preference['notebook.connection'];
	  }
	  // set default value
	  else {
	    nb.connection = Util.getDefaultConnectionByPlat(CodeType.SQL).defaultConnection;
	  }
	  return nb;
  };
  dashboardFromFile(dbFile: IDashboardFile): Dashboard {
	  const instance = new Dashboard();
	  const dashboard = {
	    seq: -1,
	    notebookId: dbFile.id + '',
	    name: dbFile.name,
	    inited: false,
	    type: WorkSpaceType.DASHBOARD,
	    layoutConfigs: dbFile.layoutConfigs,
	    queryMap: {}
    } as Dashboard;
    Object.assign(instance, dashboard);
	  return instance;
  }
  /**
   * export interface MetaSheetTableRow {
    id: string | number;
    name: string;
    admin?: string;
    access?: string;
    platform?: string;
    tableName?: string;
    lastModifyTime?: string;
    users?: AuthorizationUsers;
    schemeConfig?: SchemeConfig;
    sourceConfig?: SourceConfig;
}
   */
  getMetaSheetColumn(tableData: MetaConfigTableRow[]) {
    const columnTypeMap = {};
    const columns = _.map(tableData, (row, index) => {
      const flag = ['int', 'bigint', 'decimal'].indexOf(row.type) != -1;
      columnTypeMap[index + 2] = flag;
      return {
        index: index + 2,
        column: row.column,
        isDecimal: flag
      };
    });
    return {
      columnTypeMap,
      columns
    };
  }

  getMetaSheet(row: MetaSheetTableRow, seq = -1) {
	  const schemeConfig = row.schemeConfig!;
	  const tableData = schemeConfig.tableData;
    const {columnTypeMap, columns} = this.getMetaSheetColumn(tableData);
    const workspaceInstance = new MetaSheet();
	  const metaSheet = {
      seq: seq,
      notebookId: row.id + '',
      name: row.metaTableName,
      type: WorkSpaceType.META_SHEET,
      columns: columns,
      columnTypeMap
    } as any as MetaSheet;
    // return metaSheet;
    Object.assign(workspaceInstance, metaSheet);
    return workspaceInstance;
  }
  dashboard(
	  db: Dashboard,
	  newDb: Partial<Dashboard> = {} as Dashboard
  ): Dashboard {
	  return {
	    ...db,
	    ...newDb
	  };
  }
  sharedNotebook(id: string, name: string, seq = -1) {
    const instance = new SharedNotebook(id);
    const nb = {
	    name,
	    code: '',
	    jobs: [],
	    status_: NotebookStatus.OFFLINE,
	    createTime: moment().valueOf(),
	    updateTime: moment().valueOf(),
	    preference: undefined,
	    stagedCode: '',
	    dirty: false,
	    loaded: false,
	    resultUpdated: false,
	    monitorUrl: null,
	    seq,
	    nt:'',
	    publicReferred: null,
	    publicRole: 'no_pub',
	    nbType: 'single',
	  } as Partial<SharedNotebook>;
    instance.props(nb);
    return instance;
  }
  // async dumpResult(notebookId: string, interpreter: string, requestId: string) {
  // 	try {
  // 		let res = await this.notebookRemoteService.dumpFile(notebookId, interpreter, requestId);
  // 		if (res.status === 200) {
  // 		var aLink = document.createElement('a');
  // 		aLink.href = URL.createObjectURL(res.data);
  // 		aLink.target = '__blank'
  // 		aLink.download = `zeta_sql_result_${new Date().getTime()}.zip`;
  // 		document.body.appendChild(aLink);
  // 		aLink.click();
  // 		document.body.removeChild(aLink);
  // 		} else {
  // 			throw new Error('cannot download files');
  // 		}
  // 	} catch (e) {
  // 		console.error(e);
  // 	}
  // }
  dumpResult(notebookId: string, interpreter: string, requestId: string) {
	  const token = Util.getZetaToken();
    const url = `${config.zeta.base}share/dumpFileProxy/${notebookId}/${interpreter}/${requestId}?token=${token}`;
    const downloadWin = window.open(url,'_target');
    if(!downloadWin){
      Util.getApp().$dumpDialog(url);
    }
	  // const downloadWin = window.open(url,'_target');
	  // // Popup when new window block by broswer
	  // if (!downloadWin) {
	  //   Util.getApp().$alert(`Download file is ready. Download process got blocked by browser.<br> Click <a href="${url}" target="_blank">here</a> to unblock the proceed`,'Your download file is ready',{
	  //     dangerouslyUseHTMLString: true,
	  //     showConfirmButton: false
	  //   });
	  // }

  }
  validateNotebookStatus(oldStatus: NotebookStatus, newStatus: NotebookStatus) {
	  const map = getAvailableStatus();
	  const list = map.get(oldStatus);
	  if(list && list.indexOf(newStatus) >= 0) {
	    return true;
	  }
	  return false;
  }
  getWorkspaceSeq(wss: Dict<IWorkspace>) {
	  let seq = -1;
	  seq = _.chain(wss).map(ws => ws.seq).max().value() || -1;
	  seq += 1;
	  return seq;
  }
  getPlatformIcon(note: INotebook) {
    const cnn = !_.isEmpty(note.connection) ? note.connection : (note.preference ? note.preference['notebook.connection']: undefined);
    return this.getPlatformIconByConnection(cnn, note.nbType);
  }
  getPlatformIconByFile(note: IFile) {
    let cnn: IConnection | undefined = undefined;
    if(note.preference && note.preference && note.preference['notebook.connection'] && note.preference['notebook.connection']){
      cnn = note.preference['notebook.connection'] as IConnection;
    }
    return this.getPlatformIconByConnection(cnn, note.nbType);
  }
  getPlatformIconByConnection(cnn: IConnection | undefined, nbType: NotebookType) {
    const codeType = cnn && cnn.codeType ? cnn.codeType : CodeType.SQL;

    const defaultCnn = Util.getDefaultConnectionByPlat(codeType).defaultConnection;
    const defaultAlias = nbType === 'zeppelin' ? 'apollo' : defaultCnn.alias;
    if (!cnn) {
      return defaultAlias.toLowerCase();
    }
    if (cnn.codeType === CodeType.KYLIN) {
      return 'kylin';
    } else {
      const alias = (cnn.alias || defaultAlias).toLowerCase();
      if (alias.indexOf('apollo') >= 0) {
        return 'apollo';
      } else if (alias.indexOf('zeta') >= 0) {
        return 'apollo';
      } else {
        return alias;
      }
    }
  }
}
export function file2PacketMapper(src: IFile): RestPacket.File {
  const file: Partial<RestPacket.File> = {
    id: src.notebookId,
    nt: src.nt,
    path: src.path,
    content: src.content || undefined,
    title: src.title,
    createDt: src.createTime,
    updateDt: src.updateTime,
    status: src.state,
    preference: JSON.stringify(src.preference)
  };
  const r = _.omitBy(file, val => !val);
  return r as any as RestPacket.File;
}
const WorkspaceSrv = new WorkspaceService();
type TabActions =
	| 'workspace-add-active-tab'
	| 'workspace-add-tab'
	| 'workspace-set-active';
interface TabEvent {
  addTab: (ws: IWorkspace) => void;
  setActive: (id: string) => void;
}
class TabEventDispatcher implements TabEvent {
  eventHandler!: TabEvent;
  private constructor(handler: TabEvent) {
	  this.eventHandler = handler;
  }
  static instance: TabEventDispatcher;
  static getInstance() {
	  if (!this.instance) {
	    this.instance = new TabEventDispatcher({
	      // eslint-disable-next-line @typescript-eslint/no-empty-function
	      addTab() {},
	      // eslint-disable-next-line @typescript-eslint/no-empty-function
	      setActive() {}
	    });
	  }
	  return this.instance;
  }
  static setHandler(handler: TabEvent) {
	  if (!this.instance) {
	    this.getInstance();
	  }
	  this.instance.eventHandler = handler;
  }
  addTab = (ws: IWorkspace) => {
	  this.eventHandler.addTab(ws);
  };
  setActive = (id: string) => {
	  this.eventHandler.setActive(id);
  };
}
// const eventDispatcher = TabEventDispatcher.getInstance();
export { WorkspaceSrv, TabActions, TabEvent, TabEventDispatcher };
