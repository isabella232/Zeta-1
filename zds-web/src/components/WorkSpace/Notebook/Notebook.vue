<template>
  <div>
    <editor-tool-bar
      ref="editorToolBar"
      :notebook="data"
      :history="history"
      :loading="loading"
      :saving="saving"
      :showParams="showParams"
      @show-result="showResult"
    />
    <split-pane
      :min-percent="10"
      :default-percent="percent_"
      :split="split"
      class="notebookWarpper"
      :class="{'only-code': codeOnly}"
      :style="style"
      @resize="changeResize"
    >
      <!--
        HACK: Set min-width:0 to shrink editor within screen size.
        REF: https://github.com/codemirror/CodeMirror/issues/4895
        -->
      <editor
        v-if="data.loaded"
        ref="editor"
        slot="paneL"
        :notebook="data"
        class="editor"
        @change="onChange"
        @convert="onConvert"
      />
      <result-wrapper
        v-if="!secNote"
        slot="paneR"
        :class="{'result-hide':codeOnly}"
        :notebook="data"
      />
    </split-pane>
  </div>
</template>

<script lang="ts">
/**
 * Component <Notebook>. Display content and results of a notebook.
 * Include of <EditorToolBar>,<Editor> and <ResultWrapper>.
 * Provide actions upon notebook including submit, table check and so on.
 */
import { Component, Prop, Provide, Ref, Watch } from 'vue-property-decorator';
import VPisces from '@/components/common/VPisces.vue';
import ResultWrapper from './Result/ResultWrapper.vue';
import Editor from './Editor/Editor.vue';
import EditorToolBar from './Editor/EditorToolBar.vue';
import { EventBus } from '@/components/EventBus';
import { WorkspaceComponentBase, INotebook, IPreference, IScrollInfo, IEditorHistory } from '@/types/workspace';
import SqlConverterRemoteService from '@/services/remote/SQLConverter';
import TableCheckRemoteService from '@/services/remote/SourceTableCheck';
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import { ZetaExceptionProps } from '@/types/exception';
import DashboardRemoteService from '@/services/remote/Dashboard';
import { wsclient, Status } from '@/net/ws';
import { IFile } from '@/types/repository';
import { RestPacket } from '@/types/RestPacket';
import SplitPane from '@/components/common/split-pane/index.vue';
import _ from 'lodash';
import Util from '@/services/Util.service';
import { WorkspaceSrv as NBSrv } from '@/services/Workspace.service';
@Component({
  components: {
    VPisces, ResultWrapper, SplitPane, Editor, EditorToolBar,
  },
})

export default class Notebook extends WorkspaceComponentBase {
  @Prop() data: INotebook;
  @Ref('editor') $editor: Editor;
  @Provide()
  notebookRemoteService = new NotebookRemoteService();
  @Provide()
  sqlConverterRemoteService = new SqlConverterRemoteService();
  @Provide()
  tableCheckRemoteService = new TableCheckRemoteService();
  @Provide()
  dashboardRemoteService = new DashboardRemoteService();

  @Provide('secNote')
  @Prop({ default: false })
  secNote: boolean;

  @Provide('viewOnly')
  @Prop({default:false, type:Boolean}) viewOnly: boolean;


  @Provide('runAccess')
  @Prop({default:false, type:Boolean}) runAccess = true;

  saving: boolean;
  loading: boolean;
  debouncedSave: Function;
  @Provide()
  debouncedSavePreference:  Function;
  debouncedUpdatePreference: Function;
  history: IEditorHistory;
  convertCount: number;
  showParams = false;
  saveTimeout = -1;
  wsclient = wsclient;
  percent_ = 60;
  get notebook () {
    return this.data;
  }
  get isPublic () {
    return this.notebook.publicReferred != null;
  }
  get wsConnection (): Status {
    return this.wsclient.status;
  }
  get notConnect () {
    return Boolean(this.wsConnection != Status.LOGGED_IN);
  }
  get readOnly () {
    return this.viewOnly || this.isPublic || this.notConnect;
  }
  get style (){
    const h = this.readOnly?'74px':'40px';
    return {
      height:`calc(100% - ${h})`,
    };
  }
  get preference (): IPreference | undefined {
    const notebook: INotebook = this.$store.state.workspace.workspaces[
      this.notebook.notebookId
    ];
    if (notebook) {
      return notebook.preference;
    } else {
      return undefined;
    }
  }
  get layout () {
    const currentLayout =  this.preference && this.preference['notebook.layout'] ?
      this.preference['notebook.layout']
      : {};
    return currentLayout;
  }
  get percent (): number {
    const display = this.layout.display;
    return  display === 'none' ? 100 : this.layout[`${display}`];
  }
  get codeOnly (){
    const display = this.layout.display;
    return  display !== 'none'? false: true;
  }
  get split (){
    const display = this.layout.display;
    return  display !== 'none'? display: this.layout.lastDisplay;
  }

  constructor () {
    super();
    this.scrollInfo = { left: 0, top: 0 };
    this.saving = false;
    this.loading = !this.notebook.loaded;
    this.debouncedSave = _.debounce(this.onSave, 2000, {
      trailing: true,
    });
    this.debouncedSavePreference = _.debounce(this.onSavePreference, 1000, { trailing: true});
    this.debouncedUpdatePreference = _.debounce(this.updatePreference, 500, { trailing: true});
    this.history = {
      redo: 0,
      undo: 0,
    };
    this.convertCount = 0;
    this.percent_ = this.percent || 60;
  }
  reload () {
    if (!this.notebook.loaded) {
      this.notebookRemoteService.getById(this.notebook.notebookId).then((res) => {
        const file = res.data;
        const nb: INotebook = NBSrv.file2nb(file);
        this.$store.dispatch('updateNotebook', _.omit(nb, ['seq', 'status', 'status_', 'jobs']));
        this.loading = false;
        this.onReloaded();
      }).catch(e => {
        this.loading = false;
      });
    }
  }
  onReloaded () {
    if (this.$editor) {
      this.$editor.onEditorWrapperUpdate();
    } else {
      console.warn('cannot find $editor in notebook: ' + this.notebook.notebookId);
    }
  }
  changeResize (percent: number){
    this.percent_ = percent;

    const layout = Object.assign(this.layout,  {[this.layout.display]: percent});
    this.debouncedUpdatePreference(layout);
  }
  // show result when runSql
  showResult (){
    if (this.layout.display === 'none'){
      const layout = Object.assign(this.layout,  {display: this.layout.lastDisplay});
      this.updatePreference(layout);
    }
  }
  updatePreference (layout: any) {
    const preference: IPreference = this.preference? this.preference :{};
    preference['notebook.layout'] = layout;

    this.$store.dispatch('updateNotebookLayout', {
      notebookId: this.notebook.notebookId,
      layout: layout,
    });
    this.debouncedSavePreference();
  }
  onSavePreference () {
    const nb: INotebook = this.$store.getters.nbByNId(
      this.notebook.notebookId
    );
    this.notebookRemoteService.savePreference(this.notebook.notebookId, nb.preference || {});
  }
  @Watch('percent')
  handlePercent (){
    this.percent_ = this.percent;
  }
  /**
     * @deprecated
     */
  scrollInfo: IScrollInfo;
  mounted () {
    const props: ZetaExceptionProps = {
      path: 'notebook',
      workspaceId: this.workspaceId,
    };
    this.sqlConverterRemoteService.props(props);
    this.tableCheckRemoteService.props(props);
    this.notebookRemoteService.props(props);
    this.dashboardRemoteService.props(props);

    // console.log('eventbus:mounted');
    // EventBus.$on('websocket-connected', () => {
    //   this.restoreResult();
    // });
    // if (wsclient.status === Status.LOGGED_IN) {
    //   this.restoreResult();
    // }
  }
  activated () {
    this.reload();
  }
  onSave (code: string) {
    if (this.saving) {
      /** if save code during saving, will create a timer to exec saving 2s later */
      /** clear or extend existing timer */
      if (this.saveTimeout >= 0) {
        clearTimeout(this.saveTimeout);
        this.saveTimeout = -1;
      }
      /** generate timer */
      this.saveTimeout = setTimeout(() => {
        this.onSave(code);
        this.saveTimeout = -1;
      }, 2000) as any as number;
      /** abort saving, and the saving will be exec in Timer */
      return;
    }
    this.$store.dispatch('syncNbToFile', this.notebook);
    const file: IFile = this.$store.getters.fileByNb(this.notebook);
    const nb: INotebook = this.$store.getters.nbByNId(
      this.notebook.notebookId
    );
    /* TODO: wrapper in SerializeIFile */
    const nb_rest: RestPacket.File = {
      id: file.notebookId,
      nt: Util.getNt(),
      content: code,
      title: file.title,
      createDt: file.createTime,
      updateDt: file.updateTime,
      preference: JSON.stringify(nb.preference),
      path: file.path,
      nbType: file.nbType,
      status: '',
    };

    this.saving = true;
    this.notebookRemoteService
      .save(nb_rest)
      // ! test code for mock slow request
      // .then(() => {
      //   return new Promise(re => {
      //     setTimeout(re, 10 * 1000);
      //   });
      // })
      .then(() => {
        this.saving = false;
        const dirty = Boolean(this.saveTimeout >= 0);
        if (dirty) {
          console.log('still not sync', this, this.saveTimeout);
        }
        this.$store.dispatch('updateNotebook', {
          ...this.notebook,
          dirty,
        });
        const file: IFile = this.$store.getters.fileByNb(this.notebook);
        file.content = code;
        this.$store.dispatch('updateFile', file);
      })
      .catch(save => {
        this.saving = false;
      });

    // // check code is empty
    // const oldCode = this.notebook.code;
    // if (!isEmptyString(oldCode) && isEmptyString(code)) {
    //   this.$confirm('This will permanently clear the file. Continue?', 'Warning', {
    //     confirmButtonText: 'OK',
    //     cancelButtonText: 'Cancel',
    //     closeOnClickModal:false,
    //     closeOnPressEscape:false,
    //     type: 'warning'
    //   }).then(() => {
    //     saveCode(code);
    //   }).catch(() => {
    //     // revert
    //     this.$editor.setCodeValue(oldCode);
    //   });
    // } else {
    //   saveCode(code);
    // }
  }
  onChange ({ history, code }) {
    this.$store.dispatch('updateNotebook', {
      notebookId: this.notebook.notebookId,
      dirty: true,
    });
    this.$store.dispatch('setCurrentText', { nid: this.notebook.notebookId, text: code });
    this.debouncedSave(code);
    if (history) this.history = history;
  }
  onConvert (){
    this.convertCount++;
    if (this.convertCount===1){
      this.showParams = true;
    }
  }

  beforeClose () {
    this.$editor.saveScrollInfo();
  }

  restoreResult () {
    const id = this.notebook.notebookId;
    wsclient.syncNotebookResult(id);
  }


}
</script>
