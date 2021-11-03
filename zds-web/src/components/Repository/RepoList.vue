<template>
<div class="repo" style="height: 100%;">
    <!-- <el-alert
      v-if="showAlationAlert"
      @close="ignoreAlationAlert"
      :title="alationAlertContent"
      type="success"
      effect="dark">
    </el-alert> -->
    <div class="toolbar">
        <el-button id="createBtn" plain v-if="activeName != 'public notebook'" @click="createNew(activeName)" v-click-metric:REPO_TOOLBAR_CLICK="{name: 'createNew => '+activeName}">Create New</el-button>
        <el-button plain v-if="activeName == 'public notebook'" v-click-metric:REPO_TOOLBAR_CLICK="{name: 'createNew => '+activeName}">
          <a class="create-email" :href="emailUrl" target="_blank" >Create New</a>
        </el-button>
        <template v-if='activeName == "notebook"'>
            <el-tooltip :disabled="!(numSelectedFolders > 0 || numSelectedFiles !== 1)" content="Rename currently only support one single file, folder is not supported."
              placement="bottom" effect="light">
              <span class="el-button-holder">
                <el-button id="renameBtn" plain @click="onrename" :disabled='numSelectedFolders > 0 || numSelectedFiles !== 1' v-click-metric:REPO_TOOLBAR_CLICK="{name: 'rename'}">Edit</el-button>
              </span>
            </el-tooltip>

            <el-tooltip :disabled="!(numSelectedFolders > 0 || numSelectedFiles <= 0)" content="Download currently only support to SparkSQL files. Folder/Multi-language is not supported."
              placement="bottom" effect="light">
              <span class="el-button-holder">
              <el-button id="downloadBtn" plain @click="onDownload" :disabled='selectedContainsZpl || numSelectedFolders > 0 || numSelectedFiles <= 0' v-click-metric:REPO_TOOLBAR_CLICK="{name: 'download'}">Download</el-button>
              </span>
            </el-tooltip>

            <el-button id="deleteBtn" type="danger" @click="ondelete" :disabled='(numSelectedFiles + numSelectedFolders) <= 0' class="delete" v-click-metric:REPO_TOOLBAR_CLICK="{name: 'delete'}">Delete</el-button>
            <el-button id="uploadBtn" plain @click="uploadFile" class="upload" v-click-metric:REPO_TOOLBAR_CLICK="{name: 'upload'}">Upload</el-button>
            <span class="fence"></span>
            Github
            <PullRequest
              :default-link="defaultLink"
              :zetaPath="pwd"
              @pullSuccess="onPullSuccess"
              @pullFail="onPullFail"/>
            <PushRequest
              :default-link="defaultLink"
              :zetaPath="pwd"
              :disabled='numSelectedFiles <= 0'
              :files="getFilesPath(selectedFiles)"
              @pushSuccess="onPushSuccess"
              @pushFail="onPushFail"/>
              <el-input
                class="notebook-search"
                placeholder="search notebook"
                v-model="searchKey"
                v-click-metric:REPO_TOOLBAR_CLICK="{name: 'searchbox'}"
                >
                <i slot="prefix" class="el-input__icon el-icon-search"></i>
              </el-input>
        </template>
    </div>
    <el-tabs ref="el-tabs" class="content" v-model="activeName" >
        <el-tab-pane name="notebook">
            <span slot="label" v-click-metric:REPO_TABS_CLICK="{name: 'notebook'}">
                Notebook
            </span>
            <el-table ref="repoListTable" :data="repoList | searchFilter(searchKey)" @selection-change="handleSelectionChange" row-key="notebookId" :default-sort = "{prop: 'updateTime', order: 'descending'}" @sort-change="sortChange">
                <el-table-column type="selection" width="40" :selectable="rowSelectAble" :reserve-selection="true"></el-table-column>
                <el-table-column prop="title" :render-header="titleHeaderRender" sortable="custom" min-width="260px">
                    <template slot-scope="scope">
                        <template v-if="scope.row.isFile">
                            <span class="f-link" @click.stop='onclickFile(scope.row.notebook)' v-click-metric:NOTEBOOK_CLICK="{name: 'open'}">
                                <i :class="getIconClass(scope.row.notebook)"></i>
                                {{ scope.row.title }}
                            </span>
                        </template>
                        <template v-else>
                            <span class="f-link" @click='onclickFolder(scope.row.title)'>
                                <i class="icon-folder-empty"></i>
                                {{ scope.row.title }}
                            </span>
                        </template>
                    </template>
                </el-table-column>
                <el-table-column prop="favorite" label="Action" min-width="100px" sortable="custom" align='center'>
                  <template slot-scope="scope">
                    <favorite-icon
                      v-if="scope.row.isFile"
                      :id="scope.row.notebookId"
                    />
                  </template>
                </el-table-column>
                <el-table-column label="Platform" min-width="120px" sortable="custom" align='center'>
                  <template slot-scope="scope">
                    <i v-if="scope.row.isFile" :class="getPlatformIcon(scope.row.notebook)" class="workspace-icon"/>
                  </template>
                </el-table-column>
                <el-table-column label="Notebook Type" min-width="150px" sortable="custom" class-name="nb-type">
                  <template slot-scope="scope" v-if="scope.row.isFile">
                    <span>{{ scope.row.interpretor | noteType }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="runTime" label="Last Run Date" min-width="160px" sortable="custom">
                    <template slot-scope="scope">
                        {{ formatDateFromTimestamps(scope.row.runTime) }}
                    </template>
                </el-table-column>
                <el-table-column prop="updateTime" label="Last Update Date" min-width="160px" sortable="custom">
                    <template slot-scope="scope">
                        {{ formatDateFromTimestamps(scope.row.updateTime) }}
                    </template>
                </el-table-column>
            </el-table>
        </el-tab-pane>
        <el-tab-pane  name="dashboard">
            <span slot="label" v-click-metric:REPO_TABS_CLICK="{name: 'dashboard'}">
                Dashboard
            </span>
            <el-table ref="dashboardListTable" :data="dashboardList" @selection-change="handleSelectionChange" height="calc(100% - 60px)">
                <el-table-column type="selection" width="40" :selectable="rowSelectAble"></el-table-column>
                <el-table-column prop="name" :render-header="titleHeaderRender" sortable>
                    <template slot-scope="scope">
                            <span class="f-link" @click.stop="openDashboard(scope.row)" v-click-metric:DASHBOARD_CLICK="{name: 'open'}">
                                <i class="zeta-icon-udf"></i>
                                {{ scope.row.name }}
                            </span>

                    </template>
                </el-table-column>

                <el-table-column prop="runTime" label="Create Date" width="260px" sortable>
                    <template slot-scope="scope">
                        {{ formatDateFromTimestamps(scope.row.createTime) }}
                    </template>
                </el-table-column>
                <el-table-column prop="updateTime" label="Last Update Date" width="260px" sortable>
                    <template slot-scope="scope">
                        {{ formatDateFromTimestamps(scope.row.updateTime) }}
                    </template>
                </el-table-column>
            </el-table>
        </el-tab-pane>
        <el-tab-pane name="public notebook">
            <span slot="label" v-click-metric:REPO_TABS_CLICK="{name: 'public notebook'}">
                Public Notebook
            </span>
          <PublicNotebook />
        </el-tab-pane>
    </el-tabs>

    <new-notebook-dialog
        :visible.sync="newNbDialogVisible"
        :folder="pwd.replace(/^\/([\s\S]*)$/, '$1').replace(/^([\s\S]*)\/$/, '$1')"
        :clone="this.clone"
        :clone-id="this.cloneId"
        @new-notebook-success="newNbDialogVisible = false" />

    <edit-notebook-dialog
        :visible.sync="editNbDialogVisible"
        :file="editFile"
        @new-notebook-success="editNbDialogVisible = false" />

    <new-dashboard-dialog
        :visible.sync="newDBDialogVisible" :cloneDb="this.cloneDb" :cloneDashboard="this.cloneDashboard" @clone-dashboard-success="submitCloneDBSuccess()"/>
    <upload-dialog
        :visible.sync="uploadDialogVisible" :folder="pwd.replace(/^\/([\s\S]*)$/, '$1').replace(/^([\s\S]*)\/$/, '$1')" />
</div>
</template>

<script lang="ts">
import { Component, Vue, Inject, Ref, Watch, Provide } from 'vue-property-decorator';
import { IFile } from '@/types/repository';
import Moment from 'moment';
import Util from '@/services/Util.service';
import _ from 'lodash';
import { RestPacket } from '@/types/RestPacket';
import { INotebook, NotebookStatus, IPreference, IConnection, CodeType, CodeTypes, IDashboardFile, LayoutConfig, WorkSpaceType, ITools, Notebook, IPackageFile } from '@/types/workspace';
import { WorkspaceSrv as NBSrv, file2PacketMapper, WorkspaceSrv } from '@/services/Workspace.service';
import NewNotebookDialog from '@/components/common/workspace/create-notebook-dialog/create-notebook-dialog.vue';
import EditNotebookDialog from '@/components/common/workspace/create-notebook-dialog/edit-notebook-dialog.vue';
import NewDashboardDialog from '@/components/common/workspace/create-dashboard-dialog/create-dashboard-dialog.vue';
import UploadDialog from '@/components/common/workspace/create-notebook-dialog/upload-notebook-dialog.vue';
import { Table as ElTable, Tabs as ElTabs } from 'element-ui';
import PullRequest from './github/pull-request'
import PushRequest from './github/push-request'
import DashboardMixin from './dashboard-mixin';
import GithubMixin from './github-mixin';
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import DashboardRemoteService from '@/services/remote/Dashboard';
import { ZetaException } from '@/types/exception';
import ZeppelinApi from '@/services/remote/ZeppelinApi';
import { IPacketMapper, INotebookMapper } from '@/services/mapper';
import { IRenameOption } from '@/plugins/repo/rename-file-dialog';
import RepoApiHelper from '../../services/repo-api';
import AlationAlertMixin from './alation-alert-mixin';
import PublicNotebook from './public-notebook.vue';
import FavoriteIcon from '@/components/common/favorite-icon/favorite-icon-witn-store.vue';
import { WorkspaceManager } from '@/types/workspace/workspace-manager';
import { noteType } from '@/components/Repository/note-type-filter';
// import { ElTabs } from 'element-ui/types/tabs';
type Folder = string;
type SortProp = { prop: 'title' | 'interpretor' | 'runTime' | 'updateTime', order: 'descending' | 'ascending' | null};
function getFilesByPath(files: IFile[], path: string) {
  return _.chain(files).filter((f: IFile) => _.startsWith(f.path, path)).value();
}
function isConnected(nb: INotebook) {
  let connectedStatus = [NotebookStatus.CONNECTING, NotebookStatus.IDLE, NotebookStatus.SUBMITTING, NotebookStatus.RUNNING, NotebookStatus.STOPPING];
  return _.includes(connectedStatus, nb.status);
}
function isReadOnly(file: IFile) {
  return file.notebookId === 'default';
}
function getAllFilesByPath(files: Dict<IFile>, path: string): IFile[] {
  return _.chain(files).filter(f => _.startsWith(f.path, path)).value()
}
interface IRepoItem {
  title: string;
  isFile: boolean;
  notebook?: IFile;
  notebookId?: string;
  runTime?: number;
  updateTime?: number;
  interpretor?:string,
  state: string;
  selectable: boolean;
  favorite: boolean;
}
@Component({
  components: {
    NewNotebookDialog,
    EditNotebookDialog,
    NewDashboardDialog,
    UploadDialog,
    PullRequest,
    PushRequest,
    PublicNotebook,
    FavoriteIcon
  },
  mixins: [
    DashboardMixin,
    GithubMixin,
    // AlationAlertMixin
  ],
  filters:{
    searchFilter: (list: Array<IRepoItem>, keyword: string)=>{
      if (!keyword.trim()) {
        return list;
      }
      return list.filter(item => item.title.toLowerCase().indexOf(keyword.toLowerCase()) >= 0);
    },
    noteType,
  }
})
export default class RepoList extends Vue {
  @Inject('notebookRemoteService')
  notebookRemoteService: NotebookRemoteService;
  @Inject('zeppelinApi')
  zeppelinApi: ZeppelinApi;
  @Inject('dashboardRemoteService')
  dashboardRemoteServive: DashboardRemoteService
  @Ref('repoListTable')
  readonly repoTable!: ElTable;
  @Ref('el-tabs')
  readonly tabs!: ElTabs;
  activeName: string = 'notebook';
  searchKey: string = '';
  get files(): Dict<IFile> {
    return this.$store.state.repository.files;
  }
  get numSelectedFiles(): number {
    return this.selectedFiles.length;
  }
  get numSelectedFolders() {
    return this.selectedFolders.length
  }
  get selectedContainsZpl() {
    const hasZpl = _.find(this.selectedFiles, file => file.nbType === 'zeppelin');
    return Boolean(hasZpl)
  }
  get _() {
    return _;
  }
  pwd: string;
  selectedFolders: string[] = [];
  selectedFiles: IFile[] = [];
  newNbDialogVisible: boolean;
  editNbDialogVisible = false;
  editFile: IFile  = {} as IFile;
  newDBDialogVisible: boolean = false;
  uploadDialogVisible: boolean = false;
  // cloneFlg: boolean = false;
  // cloneNoteBook: any = {};
  // cloneZPLNb: boolean = false;
  // cloneZPLNoteBook: any = {};
  clone: 'zeta' | 'zeppelin' | '' = '';
  cloneId = '';

  cloneDb: boolean = false;
  cloneDashboard: any = {};
  get emailUrl(){
    var subject = "Create Public Notebook";
    var to = "DL-eBay-ZETA-Support@ebay.com";
    var cc = "zhouhuang@ebay.com";
    var body = "Body: create public notebook";
    var url="mailto:"+to+"?cc="+cc+"&subject="+subject+"&body="+body;
    return url;
  }
  get pwdDirectFiles(): Dict<IFile> {
    return _.pickBy(this.files, (file, path) =>
      this.directlyHas(this.pwd, file.path)
    ) as Dict<IFile>;
  }

  get pwdFiles(): Dict<IFile> {
    return _.pickBy(this.files, (file, path) =>
      this.has(this.pwd, file.path)
    ) as Dict<IFile>;
  }

  get pwdFolder(): Folder[] {
    let folders: { [K: string]: null } = {};
    _.forEach(this.pwdFiles, file => {
      let subs = file.path.slice(this.pwd.length).split('/');
      if (subs.length >= 2) {
        folders[subs[0]] = null;
      }
    });
    return Object.keys(folders);
  }

  get repoList(): Array<IRepoItem> {
    let list: Array<IRepoItem> = [];
    if (this.pwd !== '/') {
      list.push({
        notebookId: '..',
        title: '..',
        isFile: false,
        state: '',
        selectable: false,
        favorite: false,
      });
    }
    for (let folder of this.pwdFolder) {
      const subFiles = getAllFilesByPath(this.pwdFiles, this.pwd + folder);
      const runTimeFile = _.maxBy(subFiles, 'lastRunTime')
      const updateTimeFile = _.maxBy(subFiles, 'updateTime')
      const hasFavorite = Boolean(subFiles.find(f => f.favorite));
      list.push({
        notebookId: this.pwd + folder,
        title: folder,
        isFile: false,
        state:'',
        runTime: runTimeFile? runTimeFile.lastRunTime : 0,
        updateTime: updateTimeFile? updateTimeFile.updateTime : 0,
        selectable: true,
        favorite: hasFavorite,
      });
    }
    for (let noteBookId in this.pwdDirectFiles) {
      // filter default cfg file
      if (noteBookId !== '/conf/default') {
        const interpretor = this.parseCodeType(this.pwdDirectFiles[noteBookId]);
        list.push({
          title: this.pwdDirectFiles[noteBookId].title,
          isFile: true,
          notebook: this.pwdDirectFiles[noteBookId],
          notebookId: noteBookId,
          runTime: this.pwdDirectFiles[noteBookId].lastRunTime,
          updateTime: this.pwdDirectFiles[noteBookId].updateTime,
          interpretor: interpretor,
          state: this.pwdDirectFiles[noteBookId].state,
          selectable: true,
          favorite: this.pwdDirectFiles[noteBookId].favorite,
        });
      }
    }
    // _.sortBy(list, this.sortByProp(this.sortProp))

    return list.sort(this.sortByProp(this.sortProp));
  }

  get interpreterOptions(): any[] {
    const result: any[] = []
    _.forEach(CodeTypes,(ct, name) => {
      // filter
      if(ct.isCollection){
        return
      }
      const opt = {
        name: ct.name,
        val: name
      }
      result.push(opt)
    })
    return result
  }

  rowSelectAble(row: IRepoItem, index: number) {
    if(!row.selectable) return false;
    if(row.notebook && isReadOnly(row.notebook)) {
      return false;
    }
    return true
  }

  handleSelectionChange(val: any) {
    // const ids = _.chain(val).filter(v => v.isFile).map((notebook: any) => notebook.notebookId).value();
    this.selectedFolders = _.chain(val).filter(v => !v.isFile).map( item => { return this.pwd + item.title }).value();
    this.selectedFiles = _.chain(val).filter(v => v.isFile).map((item: any) => { return item.notebook as IFile }).value();
    // for (let notebookId in this.pwdDirectFiles) {
    //   this.$store.commit('REPO_SET_FILE_SELECTED', {
    //     pathname: notebookId,
    //     selected: Boolean(ids.indexOf(notebookId) >= 0)
    //   });
    // }
  }
  // onFavoriteClick(notebookId: string) {
  //   const file = this.pwdDirectFiles[notebookId];
  //   if (!file) {
  //     return;
  //   }
  //   const favoriteStatus = file.favorite;
  //   if (favoriteStatus) {
  //     this.files[notebookId].favorite = false;
  //     this.notebookRemoteService.unfavoriteNotebook(notebookId)
  //     .then(() => {
  //       this.$store.dispatch('setUnfavoriteNote', { notebookId });
  //     });
  //   } else {
  //     this.files[notebookId].favorite = true;
  //     this.notebookRemoteService.favoriteNotebook(notebookId)
  //     .then(() => {
  //       this.$store.dispatch('setFavoriteNote', { notebookId });
  //     });
  //   }
  // }
  // onFavoriteChange(notebookId: string, favorite: boolean) {
  //   const action = favorite ? 'setFavoriteNote' : 'setUnfavoriteNote';
  //   // this.files[notebookId].favorite = favorite;
  //   this.$store.dispatch(action, { notebookId });
  // }
  createNew(activeName: string) {
    if (activeName == 'notebook') {
      this.clone = '';
      this.newNbDialogVisible = true;
    } else {
      this.cloneDb = false;
      this.cloneDashboard = {};
      this.newDBDialogVisible = true;
    }
  }
  isConnectionNB(nId: string) {
    if (nId) {
      const nb: Notebook = this.$store.getters.nbByNId(nId);
      if (nb && isConnected(nb)) {
        return true;
      }
    }
    return false;
  }
  sortProp: SortProp = {
    prop: 'updateTime',
    order: 'descending'
  }
  sortChange(arg: SortProp) {
    this.sortProp.prop = arg.prop;
    this.sortProp.order = arg.order;
  }
  private sortByProp(sortProp: SortProp) {
    return (a: IRepoItem, b: IRepoItem) => {
      const key = sortProp.prop;
      let score_a = a.isFile ? 0 : (a.notebookId === '..') ? 100 : 10;
      let score_b = b.isFile ? 0 : (b.notebookId === '..') ? 100 : 10;
      let score_sort = (a[key] || 0) >= (b[key] || 0) ? -1: 1;
      if (sortProp.order === null) {
        score_sort = 0;
      } else if (sortProp.order === 'ascending') {
        score_sort = -score_sort;
      }
      return score_b - score_a + score_sort
    }
  }
  // set hasSelected(all: boolean) {
  //     if (this.hasSelected)
  //         this.$store.dispatch("clearAllSelectedFiles");
  //     else {
  //         for(let index in this.pwdDirectFiles) {
  //             this.$store.dispatch("setFileSelected", { pathname: index, selected: true })
  //         }
  //     }
  // }

  // get hasSelected(): boolean {
  //     return !_.isEmpty(this.selectedFiles)
  // }

  has(root: string, path: string): boolean {
    return path.indexOf(root) === 0;
  }

  directlyHas(root: string, path: string): boolean {
    let has = path.indexOf(root) === 0;
    if (!has) return false;
    let direct = path.slice(root.length).indexOf('/') === -1;
    return direct;
  }

  constructor() {
    super();
    this.pwd = '/';
    this.newNbDialogVisible = false;
  }

  formatDateFromTimestamps(timestamps: number) {
    return timestamps
      ?
      // Moment(timestamps).format('MMMM Do YYYY, h:mm:ss a')
      Moment(timestamps).format('YYYY/MM/DD HH:mm')
      : '';
  }
  parseCodeType(file:IFile){
    if(file.nbType === 'zeppelin') return 'Multi-language';
    if(file.preference && file.preference && file.preference['notebook.connection'] && file.preference['notebook.connection']){
      const connection = file.preference['notebook.connection'] as IConnection;
      let codeTypeDisplay = ''
      switch(connection.codeType) {
        case CodeType.TERADATA:
          codeTypeDisplay = 'TD SQL';
          break;
        case CodeType.SQL:
          codeTypeDisplay = 'Spark SQL';
          break;
        case CodeType.HIVE:
          codeTypeDisplay = 'Hive SQL';
          break;
        case CodeType.KYLIN:
          codeTypeDisplay = 'Kylin SQL';
          break;
        case CodeType.TEXT:
          codeTypeDisplay = 'Text';
          break;
        case CodeType.SPARK_PYTHON:
          codeTypeDisplay = 'Spark Python';
          break;
      }
      return codeTypeDisplay
    }
    return ''
  }
  getPlatformIcon(note: IFile) {
    // let cnn: IConnection | undefined = undefined;
    // if(note.preference && note.preference && note.preference['notebook.connection'] && note.preference['notebook.connection']){
    //   cnn = note.preference['notebook.connection'] as IConnection;
    // }
    return WorkspaceSrv.getPlatformIconByFile(note);
  }
  titleHeaderRender(h: any, item: Object) {
    const selectedCnt = this.numSelectedFiles + this.numSelectedFolders;
    return [
      h(
        'span',
        {
          style: {
            margin: '0 20px 0 0'
          }
        },
        'Current Path: ' + this.pwd
      ),
      h(
        'span',
        selectedCnt > 0 ? selectedCnt + ' selected' : ''
      )
    ];
  }
  getPathname(nb: IFile) {
    if (nb.path.charAt(nb.path.length - 1) !== '/') {
      console.error(nb.path, nb.notebookId);
    }
    let prefix =
      nb.path + (nb.path.charAt(nb.path.length - 1) === '/' ? '' : '/');
    return prefix + nb.notebookId;
  }

  // onselect(nb: IFile) {
  //     this.$store.dispatch("setFileSelected", { pathname: this.getPathname(nb), selected: !nb.selected });
  // }

  // onclickAll() {
  //     // this.$store.dispatch('setAllFileSelected', { pathname: });
  // }

  handleChangeType(scope:any){
    let file: IFile = scope.row.notebook;
    let preference = {
      "notebook.connection": {
        codeType: scope.command
      }
    } as IPreference;
    let nb_rest: Partial<RestPacket.File> = IPacketMapper.fileMapper(file);
    nb_rest.preference = preference ? JSON.stringify(preference) : "",
    this.notebookRemoteService
      .changeCodeType(nb_rest)
      .then(() => {
        let nb: INotebook = this.$store.getters.nbByFile(file);
        this.$message({
          type: 'success',
          message:"Successfully Change CodeType."
        })
        this.$store.dispatch(
          'updateFile',
          Object.assign(file, { ...file, preference})
        );
        if (nb) {
          this.$store.dispatch(
            'updateNotebook',
            Object.assign(nb, { ...nb, preference})
          );
        }
      })
      .catch(e => {
        console.error(e);
        e.message = 'Fail to change codeType. See console for detail';
      });
  }
  onrename() {
    // let fileId = Object.keys(this.selectedFiles)[0];
    let file: IFile = this.selectedFiles[0];
    this.editNbDialogVisible = true;
    this.editFile = file;
    // this.$renameFile(file).then((n_file: IFile) => {
    //   let nb: INotebook = this.$store.getters.nbByFile(n_file);
    //   this.$message({
    //     type: 'success',
    //     message:"Successfully rename.",
    //     customClass: 'rename-success-message'
    //   })
    //   this.$store.dispatch(
    //     'updateFile',
    //     Object.assign(file, { ...n_file })
    //   );
    //   if (nb) {
    //     this.$store.dispatch(
    //       'updateNotebook',
    //       Object.assign(nb, { ...nb, name: n_file.title })
    //     );
    //   }
    //   file.title = n_file.title;
    // }).catch(e => {

    //   console.error(e);
    //   e.message = 'Fail to rename. See console for detail';
    // });

  }

  onmove() {
    this.$message.warning('Move not implemented');
  }

  async onDownload() {
    try {
      let notebookList: Array<string> = [];
      let fileName = `notebook${new Date().getTime()}.zip`;
      for (let index in this.selectedFiles) {
        notebookList.push(this.selectedFiles[index].notebookId);
      }
      let res = await this.notebookRemoteService.downloadNotebooks(encodeURIComponent(notebookList.join("+")));
      if (res.status === 200) {
        var aLink = document.createElement('a');
        aLink.href = URL.createObjectURL(res.data);
        aLink.download = fileName;
        /** resolve the a tag click method cannot trigger the download event in firefox */
        document.body.appendChild(aLink);
        aLink.click();
        document.body.removeChild(aLink);
      } else {
        throw new Error('cannot download files');
      }
    } catch (e) {
      console.error(e);
    }
  }
  private vaildateFile(file: IFile) {
    let nb: INotebook | null = this.$store.getters.nbByFile(file);
    if(!nb) return true;
    if(isReadOnly(file)) return false;
    if(isConnected(nb)) return false;
    return true
  }
  private vaildateFolder(folderName: string) {
    let fileArr = _.map(this.files);
    let files = getFilesByPath(fileArr, folderName);
    const containsConnected = _.chain(files).filter((file: IFile) => file.nbType != 'zeppelin').find(file => !this.vaildateFile(file)).value()
    return !containsConnected;
  }
  private hasZetaFile(folderName: string) {
    let fileArr = _.map(this.files);
    let files = getFilesByPath(fileArr, folderName);
    return Boolean(_.find(files, (file: IFile) => file.nbType === 'single' || file.nbType === 'collection'));
  }
  private hasZeppelinFile(folderName: string) {
    let fileArr = _.map(this.files);
    let files = getFilesByPath(fileArr, folderName);
    return Boolean(_.find(files, (file: IFile) => file.nbType === 'zeppelin'));
  }
  async ondelete() {
    const repoApi = new RepoApiHelper(this.notebookRemoteService, this.zeppelinApi);
    let selectedFiles: IFile[] = _.clone(this.selectedFiles);
    for (let index in selectedFiles) {
      let file: IFile = selectedFiles[index];
      let enableDel = this.vaildateFile(file)

      if (!enableDel) {
        /* Forbid deleting running notebook */
        await this.$alert(
          `Notebook <br><u><b>${Util.getPathname(
            file
          )}</b></u><br> is connected. You cannot delete it.`,
          {
            type: 'warning',
            confirmButtonText: 'OK',
            dangerouslyUseHTMLString: true
          }
        );
        continue;
      }

      try {
        let message: string = `Are you sure to delete <br><u><b>${Util.getPathname(
            file
          )}</b></u><br> from repository?`;

        await this.$confirm(message, 'Confirm Delete', {
          confirmButtonText: 'Delete',
          cancelButtonText: 'Cancel',
          customClass:"del-file-message",
          type: 'warning',
          dangerouslyUseHTMLString: true
        });
        repoApi.deleteNotebook(file).then(() => {
          this.toogleSelection(file.notebookId);
          this.$store.dispatch('deleteFile', file);
          this.$store.dispatch('closeNotebookById', file.notebookId);
        })
      } catch {
        /* Blank */
      }
    }
    for (let folderName of this.selectedFolders) {
      const enableDel = this.vaildateFolder(folderName);
      if (!enableDel) {
        /* Forbid deleting running notebook */
        await this.$alert(
          `Cannot delete folder: ${folderName}, it contains connected notebook`,
          {
            type: 'warning',
            confirmButtonText: 'OK',
            dangerouslyUseHTMLString: true
          }
        );
        continue;
      }
      try {
        let message: string = `Are you sure to delete <br><u><b>${folderName}</b></u><br> from repository?`;
        let containsZetaNote = this.hasZetaFile(folderName);
        let containsZeppelinNote = this.hasZeppelinFile(folderName);
        await this.$confirm(message, 'Confirm Delete', {
          confirmButtonText: 'Delete',
          cancelButtonText: 'Cancel',
          customClass:"del-file-message",
          type: 'warning',
          dangerouslyUseHTMLString: true
        });
        repoApi.deleteFolder(folderName, containsZetaNote, containsZeppelinNote).then((res: any) => {
          let subFiles = getFilesByPath(_.map(this.files), folderName);
          subFiles.forEach(sf => {
            if(isReadOnly(sf)) return;
            this.toogleSelection(folderName);
            this.$store.dispatch('deleteFile', sf);
            this.$store.dispatch('closeNotebookById', sf.notebookId);
          })
        })
      } catch {
        /* Blank */
      }

    }
  }
  private toogleSelection(id: string) {
    const item = this.repoList.find(r => r.notebookId == id);
    if (item) {
      this.repoTable.toggleRowSelection(item, false);
    }
  }
  uploadFile(){
    this.uploadDialogVisible = true;
  }

  onclickFolder(folder: Folder) {
    if (folder === '.') return;
    // use @ref instead of $ref
    // (this.$refs.repoListTable as ElTable).clearSelection();
    this.repoTable.clearSelection();
    this.$store.dispatch('setFolderSelected',{
      folderName: this.pwd,
      selected: false
    })
    if (folder === '..') {
      if (this.pwd === '/') return;
      else {
        let pwd = this.pwd.slice(0, -1);
        this.pwd = pwd.slice(0, pwd.lastIndexOf('/') + 1);
      }
    } else {
      this.pwd += folder + '/';
    }
  }
  onclickFile(nb: IFile) {
    let notebook: INotebook
    if(nb.nbType === 'zeppelin') {
      const notebookObj = INotebookMapper.zplFileMapper(nb)
      notebook = NBSrv.zeppelinNotebook(notebookObj)
    } else {
      notebook = this.$store.getters.nbByFile(nb);
    }

    if (!notebook) {
      // this.$store.dispatch('openFile', nb);
      notebook = NBSrv.notebook({
        notebookId: nb.notebookId,
        name: nb.title,
        /* TODO: file.content is of type string?
                 * May need lazy load.
                 */
        code: nb.content || '',
        createTime: nb.createTime,
        updateTime: nb.updateTime,
        preference: nb.preference,
        nbType: nb.nbType
      });
    }
    WorkspaceManager.getInstance(this).addActiveTabAndOpen(notebook);

    // this.$nextTick(() => {
    //   if(nb.nbType === 'zeppelin') {
    //     this.zeppelinApi.openNote(nb.notebookId)
    //   } else {
    //     //! caculate sequences in Workspace tab
    //     // notebook = this.$store.getters.nbByFile(nb);
    //     // this.notebookRemoteService.updateNotebookSeq(notebook.notebookId, notebook.seq);
    //   }
    // });
  }

  getIconClass(file: IFile): string {
    let nb: INotebook | null = this.$store.getters.nbByFile(file);
    return nb ? 'zeta-icon-notebook2 opened' : 'zeta-icon-notebook';
  }


  submitCloneDBSuccess(){
    this.cloneDb = false;
    this.cloneDashboard = {};
  }
  mounted() {

  }
  activated () {
    let name = this.$route.query.activeName as string || this.activeName;
    // if(name){
    this.activeName = 'notebook';
    this.$nextTick(() => {
      this.activeName = name;
    })
    // }
    // console.log("el-tabs", this.tabs);
  }

  @Watch('$route.query', { deep: true, immediate: true })
  watchInternalActions() {
     const cloneZetaNote: Boolean = _.isEqual(this.$route.query.internal_action, "CLONE_NOTEBOOK") || false;
    const cloneDb: Boolean = _.isEqual(this.$route.query.internal_action, "CLONE_DASHBOARD") || false;
    const cloneZplNote: Boolean = _.isEqual(this.$route.query.internal_action, "CLONE_ZPL_NOTE") || false;
    if (cloneZetaNote) {
      const params: any = this.$route.query.internal_action_params;
      const id: string = JSON.parse(params).noteboodId || "";
      this.clone = 'zeta';
      this.cloneId = id;
      this.newNbDialogVisible = true;

      // clear params
      this.$router.push(this.$route.path);
    }
    if(cloneZplNote){
      const params: any = this.$route.query.internal_action_params;
      const id: string = JSON.parse(params).noteId || '';
      this.clone = 'zeppelin';
      this.cloneId = id;
      this.newNbDialogVisible = true;

      // clear params
      this.$router.push(this.$route.path);
    }
    if (cloneDb) {
      const params: any = this.$route.query.internal_action_params;
      const id: string = JSON.parse(params).dashboardId || "";
      const thisVue = this;
      this.dashboardRemoteServive.getDashboard(id).then(res => {
        let db = res.data;
        this.activeName = 'dashboard';
        thisVue.cloneDb = true;
        let dbDict: Dict<IDashboardFile> = {};
				dbDict[db.id] = <IDashboardFile>{
					id: db.id,
					nt: db.nt,
					name: db.name,
					layoutConfigs: db.layoutConfig
						? (JSON.parse(db.layoutConfig) as LayoutConfig[])
						: [],
					createTime: db.createDt,
					updateTime: db.updateDt ? db.updateDt : null
				};
        thisVue.cloneDashboard = dbDict[db.id];
        thisVue.newDBDialogVisible = true;
      }).catch(err => {
        console.error("get Dashboard error: " + err);
        err.message = 'Fail to get dashboard';
			});
    }
  }
  private renameRemoteService(file: RestPacket.File, isZeppelin = false){
    if(isZeppelin) {
      let title = file.path + file.title;
      return this.zeppelinApi.renameNote(file.id, title)
    } else {
      return this.notebookRemoteService.rename(file)
    }
  }
}
</script>
<style lang="scss">
td.nb-type .cell {
  font-style: italic;
}
</style>
<style lang="scss" scoped>
@import '@/styles/global.scss';
.workspace-icon {
  opacity: 1 !important;
}
.toolbar {
  padding: 8px 0;
  min-height: 26px;
  .fence {
    border-right: 1px solid $zeta-global-color-disable;
    margin: 0 10px;
  }
  .el-button + .el-button-holder,
  .el-button-holder + .el-button-holder,
  .el-button-holder + .el-button {
    margin-left: 10px;
  }
  .create-email{
    text-decoration: none;
  }
}

.f-link {
  cursor: pointer;
  text-decoration: none;
  padding: 0 20px 0 0;
  // color: rgb(51, 110, 123);
  &:hover {
    color: $zeta-global-color-heighlight;
    > i {
      color: $zeta-global-color-heighlight;
    }
  }
}
.repo {
  display: flex;
  flex-direction: column;
  .content {
    height: 0;
    flex-grow: 1;
    display: flex;
    flex-direction: column;
    /deep/ .el-tabs__item {
      width: 120px;
      text-align: center;
    }
    /deep/ .el-tabs__content {
      flex-grow: 1;
      display: flex;
      flex-direction: column;
      .el-tab-pane {
        flex-grow: 1;
        height: 100%;
        display: flex;
        flex-direction: column;
      }
      .el-table {
        display: flex;
        flex-direction: column;
        td,tr {
          font-size: 14px;
          padding: 12px 0;
        }
        .cell {
          line-height: 23px;
          font-size: 14px;
        }
        .el-table-column--selection .cell {
          text-overflow: clip;
        }
        .el-table__header-wrapper {
          min-height: 55px;
        }
        .el-table__body-wrapper {
          overflow: auto;
        }

      }
    }
  }
  .el-dropdown-link {
    cursor: pointer;
    color: $zeta-global-color;
  }
  .notebook-search{
    width: 200px;
    float: right;
  }
}
</style>
