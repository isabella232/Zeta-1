<template>
  <div
    v-loading="loading || wsConnection === 'CONNECTING'"
    class="notebook-wrapper"
    :element-loading-text="loading? 'loading notebook' :'websocket connecting'"
    element-loading-spinner="el-icon-loading"
  >
    <div
      class="control"
      :class="{'readonly':readOnly || viewOnly}"
    >
      <div class="tint fluid-row">
        <div
          v-if="runAccess"
          class="left"
        >
          <connection
            :code-type="currentCodeType"
            :notebook-id="notebook.notebookId"
            :is-public="isPublic"
            :is-public-owner="isPublicOwner"
            :capacity="queueCapacity"
            @onConnectionChange="onPreferenceChange"
          />
          <div
            v-if="!isPublic"
            class="fence"
          />

          <div v-if="!isPublic">
            <el-button
              v-click-metric:NB_TOOLBAR_CLICK="{name: 'undo'}"
              class="edit-button"
              :disabled="history.undo === 0"
              type="text"
              title="undo"
              @click="onUndo"
            >
              <i
                class="zeta-icon-undo1"
                style="font-size: 20px;"
              />
            </el-button>
            <el-button
              v-click-metric:NB_TOOLBAR_CLICK="{name: 'redo'}"
              class="edit-button"
              :disabled="history.redo === 0"
              type="text"
              title="redo"
              @click="onRedo"
            >
              <i
                class="zeta-icon-redo1"
                style="font-size: 20px;"
              />
            </el-button>
            <el-dropdown
              type="primary"
              split-button
              class="nbCommand-button"
              trigger="click"
              size="small"
              placement="bottom-start"
              :hide-on-click="false"
              @click="currentCodeTypeIsSql?onFormatSql():onIndent()"
              @command="nbCommand"
            >
              <div v-click-metric:NB_TOOLBAR_CLICK="{name: currentCodeTypeIsSql? 'format': 'indent'}">
                <template v-if="currentCodeTypeIsSql">
                  <i class="zeta-icon-format" />
                </template>
                <template v-else>
                  <i class="zeta-icon-indent" />
                </template>
              </div>
              <el-dropdown-menu
                slot="dropdown"
                class="batch-list"
              >
                <el-dropdown-item
                  v-if="currentCodeTypeIsSql"
                  v-click-metric:NB_TOOLBAR_CLICK="{name: 'format'}"
                  command="Format"
                >
                  Format
                </el-dropdown-item>
                <el-dropdown-item
                  v-click-metric:NB_TOOLBAR_CLICK="{name: 'indent'}"
                  command="Indent"
                >
                  Indent <span>Tab</span>
                </el-dropdown-item>
                <el-dropdown-item
                  v-click-metric:NB_TOOLBAR_CLICK="{name: 'outdent'}"
                  command="Outdent"
                >
                  Outdent <span>Shift Tab</span>
                </el-dropdown-item>
                <el-dropdown-item
                  v-click-metric:NB_TOOLBAR_CLICK="{name: 'upperSQLReserved'}"
                  command="UpperSQLReserved"
                >
                  Upper SQL Keywords
                </el-dropdown-item>
                <el-dropdown-item
                  v-click-metric:NB_TOOLBAR_CLICK="{name: 'uppercase'}"
                  command="ToUPPERCASE"
                >
                  To UPPERCASE
                </el-dropdown-item>
                <el-dropdown-item
                  v-click-metric:NB_TOOLBAR_CLICK="{name: 'lowercase'}"
                  command="Tolowercase"
                >
                  To lowercase <span v-if="os === 'MAC'">Cmd Shift U</span><span v-else>Ctrl Shift U</span>
                </el-dropdown-item>
                <el-dropdown-item
                  v-click-metric:NB_TOOLBAR_CLICK="{name: 'linecomments'}"
                  command="ToggleLineComments"
                >
                  Toggle Line Comments <span v-if="os === 'MAC'">Cmd /</span><span v-else>Ctrl /</span>
                </el-dropdown-item>
                <el-dropdown-item
                  v-click-metric:NB_TOOLBAR_CLICK="{name: 'blockcomments'}"
                  command="ToggleBlockComments"
                >
                  Toggle Block Comments <span v-if="os === 'MAC'">Cmd Ctrl /</span><span v-else>Ctrl Shift /</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
          </div>
          <div
            v-if="!isPublic&&currentCodeTypeIsSql"
            class="fence"
          />
          <div
            v-if="!isPublic"
            style="display:inline-flex"
          >
            <el-button
              v-if="currentCodeTypeIsSql"
              v-click-metric:NB_TOOLBAR_CLICK="{name: 'sqlConvert'}"
              class="edit-button"
              type="text"
              title="sqlConvert"
              :disabled="!nstagedSQLs || viewOnly"
              @click="onSqlConvert"
            >
              <i
                class="zeta-icon-sqlConvert"
                style="font-size: 18px"
              />
            </el-button>
          </div>
          <!-- run & stop -->
          <template>
            <div class="fence" />
            <el-dropdown
              type="primary"
              class="run-button"
              :class="canRun(notebook) ? '' : 'disabled'"
              split-button
              trigger="click"
              size="small"
              @click="onRun"
              @command="runCommand"
            >
              <div>
                <template v-if="running">
                  Running
                </template>
                <template v-else-if="submitting">
                  Submitting
                </template>
                <template
                  v-else
                  v-click-metric:NB_TOOLBAR_CLICK="{name: 'run'}"
                >
                  Run
                </template>

                <template
                  v-if="currentCodeTypeIsSql && nstagedSQLs"
                >
                  ({{ nstagedSQLs }} SQLs)
                </template>
                <i
                  v-if="submitting"
                  class="el-icon-loading"
                />
              </div>
              <el-dropdown-menu
                slot="dropdown"
                class="batch-list"
              >
                <el-dropdown-item
                  v-click-metric:NB_TOOLBAR_CLICK="{name: 'runExplain'}"
                  :disabled="!canRun(notebook) || !stagedSQL"
                  command="explain"
                >
                  Explain (F6)
                </el-dropdown-item>
                <el-tooltip
                  popper-class="grey-tooltips"
                  effect="light"
                  :content="exportTooltip"
                  placement="right"
                  :disabled="canExport(notebook)"
                  :visible-arrow="false"
                >
                  <el-dropdown-item
                    v-click-metric:NB_TOOLBAR_CLICK="{name: 'runExport'}"
                    :class="canExport(notebook) ? '' : 'disabled'"
                    command="run&export"
                  >
                    {{ isHermes ?'Export':'Run & Export' }}
                  </el-dropdown-item>
                </el-tooltip>
              </el-dropdown-menu>
            </el-dropdown>
            <el-button
              class="stop-button"
              type="default"
              size="small"
              plain
              :disabled="!canStop(notebook)"
              @click="onStop"
            >
              Stop
              <i
                v-if="stopping"
                class="el-icon-loading"
              />
            </el-button>
            <el-button
              type="text"
              class="sv-btn"
              :loading="saving"
            >
              {{ (notebook.dirty ? saving ? "Saving..." : "Unsave" : "Saved") }}
            </el-button>
          </template>
        </div>
        <div
          v-else
          class="left"
        >
          <a
            :href="zetaUrl"
            target="_blank"
          ><el-button
            id="clone"
            plain
          >clone</el-button> </a>
        </div>
        <div class="right">
          <find-sql
            ref="findSql"
            :notebook-id="notebook.notebookId"
          />
          <used-capacity
            v-if="runAccess && currentCodeTypeIsSql"
            slot="reference"
            ref="capacity"
            v-model="queueCapacity"
            :notebook-id="notebook.notebookId"
            class="tool-right"
          />
          <ChangePane
            :notebook-id="notebook.notebookId"
          />
          <NotebookInfo
            v-if="runAccess && !isOwner"
            :notebook="notebook"
          />
          <NotebookClone
            v-if="runAccess && !isOwner"
            :notebook="notebook"
            :title="'Clone'"
          />
          <ApplyPackage
            v-if="runAccess"
            ref="ApplyPackage"
            :notebook="notebook"
            :current-code-type="currentCodeType"
            title="Import"
            :disable="isPublic || notebookStatus === 'CONNECTING' || notebookStatus === 'RUNNING'"
          />
          <params
            v-if="runAccess"
            ref="paramsCpnt"
            class="tool-right"
            :sql="content"
            :notebook-id="notebook.notebookId"
            :is-public="isPublic"
            title="Variables"
            :popover-visible="showParams"
            @onParamsChange="onPreferenceChange"
          />
          <optimization
            v-if="runAccess"
            class="tool-right"
            :notebook="notebook"
            :current-code-type="currentCodeType"
            :disable="isPublic&&!isPublicOwner"
            title="Config"
            @onProfileChange="onProfileChange"
          />
          <schedule-tool
            v-if="!viewOnly"
            title="Schedule"
            class="tool-right"
            :disable="isPublic || !showSchedule"
            :notebook-id="notebook.notebookId"
            :current-code-type="currentCodeType"
          />
          <meta-config
            v-if="!viewOnly"
            ref="metaConfig"
            title="Open"
            class="tool-right"
            :notebook="notebook"
          />
          <Favorite
            :type="viewOnly ? 'share_nb': 'nb'"
            class="tool-right"
            :notebook-id="notebook.notebookId"
          />
          <ShareLink
            v-model="shareUrl"
            title="Share"
            class="tool-right"
            :disable="isPublic"
          />
        </div>
      </div>
      <ReadOnly
        v-if="readOnly || viewOnly"
        :connection-status="wsConnection"
        :message="runAccess ? 'This notebook is SHARED by others, clone or add to favorite for further reference' : 'This notebook is READ ONLY'"
      />
      <ConnectProgress
        v-if="showProgress"
        :progress="progress"
      />
    </div>
    <download-notification
      :visible.sync="downloadVisible"
      @confirm-download="onRunAndExport"
    />
  </div>
</template>

<script lang="ts">
/**
 * Component <EditorWrapper>. Wrapper of notebook edtior and action panel.
 *
 * @prop notebook Current opened notebook.
 */
import { Component, Vue, Prop, Inject, Watch, Ref } from 'vue-property-decorator';
import { Getter } from 'vuex-class';
import {
  INotebook,
  NotebookStatus,
  CodeType,
  IConnection,
  IEditorHistory,
  CodeTypes,
} from '@/types/workspace';
import Editor from './Editor.vue';
import { wsclient, Status } from '@/net/ws';
/** tools */
import TableCheck from '@/components/WorkSpace/Notebook/Editor/Tools/TableCheck/TableCheck.vue';
import Optimization from '@/components/WorkSpace/Notebook/Editor/Tools/OptimizationV2.vue';
import Connection from '@/components/WorkSpace/Notebook/Editor/Tools/connection/connection.vue';
// import TdConnection from "@/components/WorkSpace/Notebook/Editor/Tools/connection-td.vue"
import MetaConfig from '@/components/WorkSpace/Notebook/Editor/Tools/MetaConfig.vue';
import FindSql from '@/components/WorkSpace/Notebook/Editor/Tools/FindSql.vue';
import Params from '@/components/WorkSpace/Notebook/Editor/Tools/Params.vue';
import UsedCapacity from '@/components/WorkSpace/Notebook/Editor/Tools/UsedCapacity.vue';
import ScheduleTool from '@/components/WorkSpace/Notebook/Editor/Tools/ScheduleTool.vue';
import ReadOnly from '@/components/WorkSpace/Notebook/Editor/Tools/ReadOnly.vue';
import Favorite from '@/components/WorkSpace/Notebook/Editor/Tools/favorite.vue';
import ShareLink from '@/components/WorkSpace/Notebook/Editor/Tools/ShareLink.vue';
import ConnectProgress from '@/components/WorkSpace/Notebook/Editor/Tools/Progress.vue';
import ApplyPackage from '@/components/WorkSpace/Notebook/Editor/Tools/ApplyPackage.vue';
import DownloadNotification from '@/components/WorkSpace/Notebook/Editor/Tools/DownloadNotification.vue';
import ChangePane from '@/components/WorkSpace/Notebook/Editor/Tools/ChangePane.vue';
import NotebookInfo from '@/components/WorkSpace/Notebook/Editor/Tools/notebook-info.vue';
import NotebookClone from '@/components/WorkSpace/Notebook/Editor/Tools/notebook-clone.vue';
import Util from '@/services/Util.service';
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import { WorkspaceSrv as NBSrv } from '@/services/Workspace.service';
import _ from 'lodash';
import { ZETA_ACTIONS } from '@/components/common/Navigator/nav.config';
import * as Logger from '@/plugins/logs/zds-logger';
import { isHermes } from '@/services/connection.service';

function isEmptyString(str: string | null) {
  if (!str || !str.trim) {
    return true;
  }
  return _.isEmpty(str.trim());
}
@Component({
  components: {
    Editor,
    TableCheck,
    Connection,
    Optimization,
    FindSql,
    Params,
    UsedCapacity,
    ScheduleTool,
    // TdConnection,
    ShareLink,
    ReadOnly,
    ConnectProgress,
    ApplyPackage,
    Favorite,
    MetaConfig,
    DownloadNotification,
    NotebookInfo,
    NotebookClone,
    ChangePane,
  },
})
export default class NotebookToolBar extends Vue {
  @Prop() notebook: INotebook;
  @Prop() history: IEditorHistory;
  @Prop() loading:  boolean;
  @Prop() showParams: boolean;
  @Inject()
  notebookRemoteService: NotebookRemoteService;
  @Inject() viewOnly: boolean;
  @Inject() runAccess: boolean;
  @Ref('paramsCpnt')
  $params: Params;
  @Ref('capacity')
  $capacity: UsedCapacity;
  @Ref('findSql')
  $findSql: FindSql;
  @Prop() saving: boolean;

  get stopping() {
    return this.notebook.status == NotebookStatus.STOPPING;
  }
  /**
   * if notebook is saving, will create a timer to save latest code
   */
  @Inject()
  debouncedSavePreference: Function;
  queueCapacity: number | null | undefined;
  wsclient = wsclient;
  downloadVisible = false; // download notification
  @Getter('userPreferenceByKey') userPreference: (key: string) => any;
  get isPublic() {
    return this.notebook.publicReferred != null;
  }
  get isPublicOwner() {
    return (this.notebook.publicReferred == null) && (this.notebook.publicRole == 'pub');
  }
  get dirty(): boolean {
    return this.notebook.dirty;
  }
  get name(): string {
    return this.notebook.name;
  }
  get source(): string | undefined {
    return this.notebook.source;
  }
  get connLightClass(): string {
    switch (this.$store.state.workspace.connStatus as string) {
      case 'OFFLINE':
        return 'conn-offline';
      case 'ONLINE':
        return 'conn-online';
      default:
        return '';
    }
  }
  get connHint(): string {
    switch (this.$store.state.workspace.connStatus as string) {
      case 'OFFLINE':
        return 'Offline';
      case 'ONLINE':
        return 'Connected';
      default:
        return 'Unknown connect status';
    }
  }
  get currentCodeType() {
    const defaultVal = CodeType.SQL;
    if (
      this.notebook.preference &&
            this.notebook.preference['notebook.connection']
    ) {
      const connection = this.notebook.preference[
        'notebook.connection'
      ] as IConnection;
      if (connection && connection.codeType) {
        if(connection.codeType === 'TEXT'){
          return defaultVal;
        }
        return connection.codeType;
      }
    }
    return defaultVal;
  }
  get currentCodeTypeIsSql() {
    return this.currentCodeType === CodeType.SQL;
  }
  get showSchedule(): boolean {
    if(this.currentCodeType === CodeType.KYLIN) {
      return false;
    }
    return true;
  }
  get nstagedSQLs(): number {
    if (!this.notebook.stagedCode) return 0;
    const sql = this.notebook.stagedCode.trim();
    return this.splitSql(sql);
  }
  splitSql(sql: string): number {
    /** filter annotation `/* *\/` */
    sql = sql.replace(/\/\*{1,2}[\s\S]*?\*\//g, '');
    // filter annotation `-- multi line` //
    sql = sql.replace(/^-{2}\s{1,}\[[\s|\S]+-{2}\s{1,}].*/gm, '');
    // sql = sql.replace(/--.*[^\\n]*/g, "");
    // filter annotation `--` //
    sql = sql.replace(/-{2}\s{1,}.*/g, '');
    // filter annotation `# multi line`
    sql = sql.replace(/^#{1,}\s*\[[\s|\S]+#{1,}\s*].*/gm, '');
    // sql = sql.replace(/#.*[^\\n]*/g, "");
    //filter annotation `#`
    sql = sql.replace(/#{1,}.*/g, '');
    // replace `XXXX` || 'XXXXX' || "XXXXX"
    sql = sql.replace(/\\'/g, '');
    sql = sql.replace(/'[\s\S]'/g, '');
    sql = sql.replace(/\\"/g, '');
    sql = sql.replace(/"[\s\S]"/g, '');
    sql = sql.replace(/\\`/g, '');
    sql = sql.replace(/`[\s\S]`/g, '');
    // handle `;`
    sql = sql.replace(/[\s]+;/g, ';');
    sql = sql.replace(/;+/g, ';');
    // remove last `;`
    sql = sql.replace(/^;+|;+$/g, '');
    sql = sql.replace(/;[\s]+$/g, '');
    return sql.trim() === '' ? 0 : sql.split(';').length;
  }
  get stagedSQL(): string {
    return this.notebook.stagedCode;
  }
  get stagedOrAllSQL(): string {
    return this.stagedSQL || this.notebook.code;
  }
  get content(): string {
    return this.notebook.code;
  }
  get os() {
    return Util.getOS();
  }
  get submitting(): boolean {
    return this.notebook.status === NotebookStatus.SUBMITTING;
  }
  get running(): boolean {
    return this.notebook.status === NotebookStatus.RUNNING;
  }
  get notebookStatus(): NotebookStatus {
    return this.notebook.status;
  }
  get isHermes() {
    const connection = this.notebook.connection;
    return Boolean(
      this.currentCodeType == CodeType.SQL &&
                connection &&
                isHermes(connection.clusterId)
    );
  }
  get isJDBCConnection() {
    const codetype = CodeTypes[this.currentCodeType];
    if (this.isHermes) {
      return true;
    }
    return Boolean(codetype.jdbcType);
  }
  get wsConnection(): Status {
    return this.wsclient.status;
  }
  get shareUrl() {
    return `${location.protocol}//${location.host}/${Util.getPath()}share/#/notebook?notebookId=${this.notebook.notebookId}`;
  }
  get zetaUrl() {
    const params = {
      noteboodId: this.notebook.notebookId,
    };
    return (
      `${location.protocol}//${location.host}/${Util.getPath()}#/repository` +
            `?internal_action=${ZETA_ACTIONS.CLONE_NOTEBOOK}` +
            `&internal_action_params=${JSON.stringify(params)}`
    );
  }
  get readOnly() {
    return this.viewOnly || this.isPublic || this.notConnect;
  }
  get isOwner(){
    return this.notebook.nt === Util.getNt();
  }
  get notConnect() {
    return Boolean(this.wsConnection != Status.LOGGED_IN);
  }
  get exportTooltip() {
    return this.isHermes
      ? 'This feature can only run when single query selected. Result size should less than 100G(uncompressed).'
      : 'This feature can only run when single query selected. Result size should less than 500 Mb.';
  }
  get progress(){
    return this.notebook.progress;
  }
  get showProgress(){
    return (this.notebookStatus === NotebookStatus.CONNECTING);
  }
  get $editor () {
    return this.$parent.$refs.editor as any;
  }
  constructor() {
    super();
    // this.debouncedSavePreference = _.debounce(this.onSavePreference, 2000, { trailing: true});
    this.queueCapacity = null;
  }
  mounted() {
    if (this.viewOnly) {
      this.registerKeyEvent();
    }
  }
  activated() {
    this.registerKeyEvent();
  }
  deactivated() {
    this.unregisterKeyEvent();
  }
  onDelete() {
    this.$emit('delete');
  }
  onReloaded() {
    if (this.$editor) {
      this.$editor.onEditorWrapperUpdate();
    } else {
      console.warn('cannot find $editor in notebook: ' + this.notebook.notebookId);
    }
  }
  // onSavePreference() {
  //   const nb: INotebook = this.$store.getters.nbByNId(
  //     this.notebook.notebookId
  //   );
  //   this.notebookRemoteService.savePreference(this.notebook.notebookId, nb.preference || {});
  // }
  onUndo() {
    if (this.$editor) {
      this.$editor.undo();
    } else {
      console.warn('cannot find $editor in notebook: ' + this.notebook.notebookId);
    }
  }
  onRedo() {
    if (this.$editor) {
      this.$editor.redo();
    } else {
      console.warn('cannot find $editor in notebook: ' + this.notebook.notebookId);
    }
  }
  onFormatSql() {
    if (this.$editor) {
    this.$editor.formatSql();
    } else {
      console.warn('cannot find $editor in notebook: ' + this.notebook.notebookId);
    }
  }
  onUpperSQLReserved(){
    if (this.$editor) {
      this.$editor.UpperSQLReserved();
    } else {
      console.warn('cannot find $editor in notebook: ' + this.notebook.notebookId);
    }
  }
  onIndent(){
    if (this.$editor) {
      this.$editor.indent();
    } else {
      console.warn('cannot find $editor in notebook: ' + this.notebook.notebookId);
    }
  }
  onOutdent(){
    if (this.$editor) {
      this.$editor.outdent();
    } else {
      console.warn('cannot find $editor in notebook: ' + this.notebook.notebookId);
    }
  }
  onUpperCase(){
    if (this.$editor) {
      this.$editor.upperCase();
    } else {
      console.warn('cannot find $editor in notebook: ' + this.notebook.notebookId);
    }
  }
  onLowerCase(){
    if (this.$editor) {
      this.$editor.lowerCase();
    } else {
      console.warn('cannot find $editor in notebook: ' + this.notebook.notebookId);
    }
  }
  OnLineComment(){
    if (this.$editor) {
      this.$editor.lineComment();
    } else {
      console.warn('cannot find $editor in notebook: ' + this.notebook.notebookId);
    }
  }
  OnBlockComment(){
    if (this.$editor) {
      this.$editor.blockComment();
    } else {
      console.warn('cannot find $editor in notebook: ' + this.notebook.notebookId);
    }
  }
  nbCommand(command: string){
    switch (command) {
      case 'Format':
        this.onFormatSql();
        break;
      case 'Indent':
        this.onIndent();
        break;
      case 'Outdent':
        this.onOutdent();
        break;
      case 'UpperSQLReserved':
        this.onUpperSQLReserved();
        break;
      case 'ToUPPERCASE':
        this.onUpperCase();
        break;
      case 'Tolowercase':
        this.onLowerCase();
        break;
      case 'ToggleLineComments':
        this.OnLineComment();
        break;
      case 'ToggleBlockComments':
        this.OnBlockComment();
        break;
      default:
        break;
    }
  }
  onSqlConvert() {
    if (this.$editor) {
      this.$editor.sqlConvert();
    } else {
      console.warn('cannot find $editor in notebook: ' + this.notebook.notebookId);
    }
  }
  onRun() {
    if (!this.canRun(this.notebook)) {
      return;
    }
    this.$emit('show-result');
    const nb: INotebook = this.$store.getters.nbByNId(
      this.notebook.notebookId
    );
    let code = nb.stagedCode;
    if (!code || code === '') code = nb.code;
    const paramMap = this.$params.getParamsMap();
    const interpreter = this.isHermes ? 'carmel' : null;
    wsclient.jobSubmit(this.notebook.notebookId, code, interpreter, { var: JSON.stringify(paramMap) });
    if (this.$editor) {
      this.$editor.markSelectionText();
    } else {
      console.warn('cannot find $editor in notebook: ' + this.notebook.notebookId);
    }
  }
  onExplain() {
    if (!this.canRun(this.notebook)) {
      return;
    }
    this.$emit('show-result');
    let code = this.notebook.stagedCode;
    if (!code || code === '') {
      return;
    }
    code = 'explain ' + code;
    const paramMap = this.$params.getParamsMap();
    const interpreter = this.isHermes ? 'carmel' : null;
    wsclient.jobSubmit(this.notebook.notebookId, code, interpreter, { var: JSON.stringify(paramMap) });
    if (this.$editor) {
      this.$editor.markSelectionText();
    } else {
      console.warn('cannot find $editor in notebook: ' + this.notebook.notebookId);
    }
  }
  runCommand(command: string) {
    switch (command) {
      case 'run&export':
        this.showPopup();
        break;
      case 'explain':
        this.onExplain();
      default:
        break;
    }
  }
  showPopup(){
    if (!this.canExport(this.notebook)) {
      return;
    }
    const userDownloadNotification= this.userPreference('download-notification');
    if (this.isHermes && !userDownloadNotification) {
      this.downloadVisible = true;
      return;
    }
    this.onRunAndExport();
  }
  onRunAndExport() {
    if (!this.canExport(this.notebook)) {
      return;
    }
    let code = this.notebook.stagedCode;
    if (!code || code === '') {
      code = this.notebook.code;
    }
    const paramMap = this.$params.getParamsMap();
    const interpreter = this.isHermes ? 'carmel' : null;
    wsclient.jobDump(this.notebook.notebookId, code, interpreter, { var: JSON.stringify(paramMap) });
    if (this.$editor) {
      this.$editor.markSelectionText();
    } else {
      console.warn('cannot find this.$editor in notebook: ' + this.notebook.notebookId);
    }
  }
  onStop() {
    if (!this.canStop(this.notebook)) {
      console.debug(
        `Run: Notebook ${this.notebook.notebookId} is ${
          this.notebook.status
        }, not RUNNING`
      );
      return;
    }
    console.info('Stopping...');
    this.$store.dispatch('setNotebookStatus', { nid: this.notebook.notebookId, status: NotebookStatus.STOPPING });
    wsclient
      .jobCancel(this.notebook.notebookId, this.notebook
        .activeJobId as string)
      .then(m => {
        // update store in wclient
        // this.$store.dispatch("setNotebookStatus", { nid: this.notebook.notebookId, status: NotebookStatus.IDLE });
      })
      .catch(e => {
        // update store in wclient
        // this.$store.dispatch("setNotebookStatus", { nid: this.notebook.notebookId, status: NotebookStatus.IDLE });
      });
  }
  onTableCheck() {
    this.$emit('table-check');
  }
  onOptimization() {
    this.$emit('optimization');
  }
  onDataValidation() {
    this.$emit('data-validation');
  }
  getScrollInfo() {
    const scrollInfo = (this.$editor as any).getScrollInfo();
    return scrollInfo;
  }
  canRun(notebook: INotebook): boolean {
    if(notebook.stagedCode && !notebook.stagedCode.trim()){
      return false;
    }
    switch (notebook.status) {
      case NotebookStatus.OFFLINE:
      case NotebookStatus.CONNECTING:
      case NotebookStatus.DISCONNECTING:
      case NotebookStatus.SUBMITTING:
      case NotebookStatus.RUNNING:
      case NotebookStatus.STOPPING:
        return false;
      default:
        return true;
    }
  }
  canExport(notebook: INotebook): boolean {
    if (!this.canRun(notebook)) {
      return false;
    } else {
      const sql = this.notebook.stagedCode || this.notebook.code;
      if (this.splitSql(sql) === 1) {
        return true;
      } else {
        return false;
      }
    }
  }
  canStop(notebook: INotebook): boolean {
    switch (notebook.status) {
      case 'SUBMITTING':
        return (!this.isJDBCConnection && notebook.activeJobId) ? true : false;
      case 'RUNNING':
        /* Stop only works when there's active job */
        return notebook.activeJobId ? true : false;
      default:
        return false;
    }
  }
  onProfileChange() {
    this.$capacity.getStatus();
  }
  onPreferenceChange() {
    this.debouncedSavePreference();
  }

  @Watch('notebookStatus')
  onNotebookStatusChange(status: NotebookStatus, oldStatus: NotebookStatus) {
    console.debug('clearTextMarker', `status: ${status}, oldStatus: ${oldStatus}`);
    const available = (s: NotebookStatus): boolean => {
      return (
        s == NotebookStatus.CONNECTING ||
                s == NotebookStatus.OFFLINE ||
                s == NotebookStatus.DISCONNECTING ||
                s == NotebookStatus.IDLE
      );
    };
    const clearable = available(status) && !available(oldStatus);
    console.debug('clearTextMarker', `clearable: ${clearable}`);
    if (clearable) {
      if (this.$editor) {
        this.$editor.clearTextMarker();
      } else {
        console.warn('cannot find $editor in notebook: ' + this.notebook.notebookId);
      }
      console.debug('clearTextMarker','clear mark text');
    }
  }
  registerKeyEvent() {
    window.onkeydown = (e: KeyboardEvent) => {
      if (e.keyCode == 70) {
        if (
          (this.os == 'WIN' && e.ctrlKey) ||
                    (this.os == 'MAC' && e.metaKey)
        ) {
          e.preventDefault();
          e.stopPropagation();
          Logger.counter('NB_TOOLBAR_CLICK',1, { name: 'searchbox', trigger: 'keyEvent'});
          this.$findSql.toggle(this.stagedSQL);
        }
      }
      if (e.keyCode == 116) {
        e.preventDefault();
        e.stopPropagation();
        if (this.canRun(this.notebook)) {
          Logger.counter('NB_TOOLBAR_CLICK',1, { name: 'run', trigger: 'keyEvent'});
          this.onRun();
        }
      }
      if (e.keyCode == 117) {
        e.preventDefault();
        e.stopPropagation();
        if (this.canRun(this.notebook) && this.stagedSQL) {
          Logger.counter('NB_TOOLBAR_CLICK',1, { name: 'runExplain', trigger: 'keyEvent'});
          this.onExplain();
        }
      }
    };
    window.onkeyup = (e: KeyboardEvent) => {
      if (e.keyCode == 27 && this.$findSql.show) {
        this.$findSql.close();
      }
    };
  }
  unregisterKeyEvent() {
    window.onkeydown = null;
    window.onkeyup = null;
  }
}
</script>
<style lang="scss">
@import "@/styles/global.scss";
.notebook-tool-btn {
    display: inline-flex;
    padding: 0 3px;
    cursor: pointer;
    outline: none;
    img {
        height: 1.5em;
        position: relative;
    }
    &.light {
        font-size: 0.9em;
        color: #8b8b8b;
        img {
            height: 1.5em;
            top: 0;
            left: -2px;
        }
    }
    &:hover {
        color: $zeta-global-color;
        i {
            &[class^="zeta-icon-"],
            &[class*=" zeta-icon-"] {
                color: $zeta-global-color;
            }
        }
    }
    &.disable,
    &[disabled] {
        color: #8b8b8b;
        cursor: not-allowed;
        img {
            filter: grayscale(100%);
        }
        i {
            &[class^="zeta-icon-"],
            &[class*=" zeta-icon-"] {
                color: #8b8b8b;
            }
        }
    }
    i[class^="el-icon-"],
    i[class*=" el-icon-"] {
        line-height: inherit;
    }
}
.el-dropdown-menu__item.el-tooltip.disabled {
    &:hover {
        color: #cacbcf;
    }
    color: #cacbcf;
    cursor: not-allowed;
}
.el-dropdown-menu__item{
    &:hover{
        color: $zeta-global-color !important;
    }
    >span{
        float: right;
        margin-left: 20px;
    }
    overflow: hidden;
}
.red-tooltips.el-tooltip__popper {
    background-color: $zeta-global-color-red-background !important;
    color: $zeta-global-color-red;
    border: 1px solid $zeta-global-color-red !important;
}
.grey-tooltips.el-tooltip__popper {
    background-color: $zeta-global-light-grey !important;
    color: $zeta-font-light-color;
    border: 1px solid $zeta-font-light-color !important;
}
</style>

<style lang="scss" scoped>
@import "@/styles/global.scss";
$control-height: 40px;
$control-font-color: $zeta-font-color;
.notebook-wrapper {
    min-width: 0;
    // height: 100%;
}
.control {
    // height: $control-height;
    // color: $control-font-color;
    // padding: 8px 10px 0 10px;
    box-sizing: border-box;
    position: relative;
    span {
        line-height: 20px;
    }
    .fluid-row {
        display: flex;
        padding: 5px 15px 0 15px;
        height: $control-height;
        align-items: center;
        justify-content: space-between;
        box-sizing: border-box;
        // margin-top: 5px;
        &.tint {
            background-color: $zeta-label-lvl1;
            border-radius: 3px;
            border: 1px solid #ddd;
        }
        .left {
            .sv-btn {
                display: flex;
                align-items: center;
                margin: 0 5px;
                color: $zeta-font-light-color;
                cursor: not-allowed;
            }
            .run-button,
            .nbCommand-button {
                margin-right: 10px;
                &.el-button {
                    padding: 3px 10px;
                }
                &.disabled {
                    /deep/ .el-button {
                        border-color: #ebeef5;
                        background-color: #cacbcf;
                        cursor: not-allowed;
                    }
                }
                i.zeta-icon-run {
                    font-size: 14px;
                }
                /deep/ .el-button {
                    padding: 3px 10px;
                }
                /deep/ .el-dropdown__caret-button {
                    padding: 3px 2px;
                }
            }
            /deep/ .nbCommand-button{
                margin-left: 3px;
                margin-right: 0;
                .el-button--primary{
                    background-color: transparent;
                    border-color: transparent;
                    color: $zeta-font-color;
                    padding: 3px 4px;
                    &:hover{
                        color: $zeta-font-color;
                    }
                }
                .zeta-icon-format{
                    font-size: 14px;
                }
                .zeta-icon-indent{
                    font-size: 14px;
                }
                .el-dropdown__caret-button {
                    padding: 3px 1px;
                }
                .el-dropdown__caret-button::before{
                    background-color: transparent;
                }
            }
            .stop-button {
                padding: 3px 10px;
                i.zeta-icon-stop {
                    font-size: 14px;
                }
            }
        }
        .right {
            // margin-left: auto;
            display: flex;
            align-items: center;
            .tool-right {
                margin-left: 10px;
            }
        }
        img {
            height: 1.5em;
            position: relative;
            top: -0.2em;
        }
    }
    .fence {
        border-right: 1px solid $zeta-global-color-disable;
        margin: 0 10px;
        height: 22px;
    }
    .edit-button {
        color: $zeta-font-color;
    }
    .inline {
        display: inline-flex;
        padding: 0 8px;
    }
    // button.el-button{
    //     &.el-button--text{
    //         color:$zeta-font-color;
    //     }
    // }
    button {
        display: inline-flex;
        padding: 0 3px;
        cursor: pointer;
        outline: none;
        /deep/ img {
            height: 1.5em;
            position: relative;
            // top: em;
        }
        &.light {
            font-size: 0.9em;
            color: #8b8b8b;
            img {
                height: 1.5em;
                top: 0;
                left: -2px;
            }
        }
        &.disable {
            color: #8b8b8b;
            cursor: not-allowed;
            img {
                filter: grayscale(100%);
            }
        }
    }
    .conn-status {
        display: inline-flex;
        font-size: 0.9em;
    }
    .left {
        display: flex;
        align-items: center;
    }
    .el-button + .el-button {
        margin-left: 0;
    }
    #clone{
        display: inline-block;
        padding: 7px 15px;
    }
}
.editor {
    height: calc(100% - #{$control-height});
    border: 1px solid #dddddd;
    box-sizing: border-box;
}
.control.readonly + .editor {
    height: calc(100% - #{$control-height + 34px});
}
@media screen and (max-width: 1360px) {
    .tool-right {
        /deep/ span.tool-name {
            display: none;
        }
    }
    .left {
      /deep/ .ntbk-cnn {
        .ntbk-cnn-codeType {
          display: none;
        }
      }
    }
}
</style>
