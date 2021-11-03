<template>
  <div
    v-loading="loading"
    class="nb-tabs-wrapper"
  >
    <ws-reconnect />
    <WorkSpaceTabs
      @onTabActivate="onTabClick"
      @onTabClose="onTabClose"
    />
    <!-- context by virtual dom -->
    <keep-alive v-show="!_.isEmpty(workspaces) && activeId">
      <notebook
        v-if="!_.isEmpty(workspaces) && activeId && workspaces[activeId].type == 'NOTEBOOK'"
        :ref="activeId"
        :key="activeId"
        :workspace-id="activeId"
        :data="workspaces[activeId]"
        :is-public="workspaces[activeId].publicReferred != null"
        class="workspace-container"
      />
      <SharedNotebook
        v-if="!_.isEmpty(workspaces) && activeId && workspaces[activeId].type == 'SHARED_NOTEBOOK'"
        :ref="activeId"
        :key="activeId"
        :workspace-id="activeId"
        :data="workspaces[activeId]"
        class="workspace-container"
      />
      <MultiLanguageNotebook
        v-else-if="!_.isEmpty(workspaces) && activeId && workspaces[activeId].type == 'NOTEBOOK_COLLECTION'"
        :ref="activeId"
        :key="activeId"
        :workspace-id="activeId"
        :data="workspaces[activeId]"
        :is-public="workspaces[activeId].publicReferred != null"
        class="workspace-container"
      />

      <Dashboard
        v-else-if="!_.isEmpty(workspaces) && activeId && workspaces[activeId].type == 'DASHBOARD'"
        :key="activeId"
        :ref="activeId"
        :workspace-id="activeId"
        :dashboard-id="activeId"
        class="workspace-container"
      />

      <meta-sheet-view
        v-else-if="!_.isEmpty(workspaces) && activeId && workspaces[activeId].type == 'META_SHEET'"
        :key="activeId"
        :ref="activeId"
        :workspace-id="activeId"
        :metasheet-id="activeId"
        class="workspace-container"
      />
    </keep-alive>
    <!-- context by hide/show -->
    <!-- <div class="dom-workspace-container" v-show="currentWorkspaceDisplay === 'dom'"> -->
    <template v-for="ws in domDisplay">
      <ZeppelinNotebook
        v-show="activeId === ws.notebookId"
        :key="ws.notebookId"
        :ref="ws.notebookId"
        :workspace-id="ws.notebookId"
        :data="ws"
        :show="activeId === ws.notebookId"
        class="workspace-container"
      />
    </template>
    <!-- </div> -->
    <!-- landing page for empty -->
    <div
      v-if="currentWorkspaceDisplay === 'empty'"
      class="no-notebook"
    >
      <WorkspaceGuide />
    </div>
  </div>
</template>

<script lang="ts">
/**
 * Component <NotebookTabs>. Notebook tab navigator, wrapper of <Notebook>.
 * Opened notebook retrieved from store.
 */
import { Component, Vue, Provide} from 'vue-property-decorator';
import { State, Getter } from 'vuex-class';
import _ from 'lodash';

import { WorkspaceComponentBase } from '@/types/workspace';
// import WorkSpaceTabs from './WorkSpaceTabs.vue';
import WorkSpaceTabs from '@/components/WorkSpace/workspace-tabs';
import Notebook from '@/components/WorkSpace/Notebook/Notebook.vue';
import SharedNotebook from '@/components/WorkSpace/Notebook/shared-notebook.vue';
import WorkspaceGuide from './workspace-guide.vue';
import WsReconnect from '@/components/common/ws-reconnect.vue';
/** tools */
import DataMoveCmpnt from '@/components/DataMove/DataMove.vue';
import DataValidationCmpnt from '@/components/DataValidation/DataValidation.vue';
import Metadata from '@/components/WorkSpace/Metadata';
import Dashboard from '@/components/WorkSpace/dashboard/dashboard.vue';
import MultiLanguageNotebook from '@/components/WorkSpace/multi-notebook/multi-language-notebook.vue';
import ZeppelinNotebook from '@/components/WorkSpace/zeppelin-notebook/zeppelin-notebook.vue';
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import ZeppelinMessageHandler from '@/services/zeppelin/zeppelin-message-handler';
import { WorkSpaceType, IWorkspace } from '@/types/workspace';
import ZeppelinApi from '@/services/remote/ZeppelinApi';
import { WorkspaceManager } from '@/types/workspace/workspace-manager';
import { customZetaException } from '@/types/exception';
import MetaSheetView from '@/components/MetaSheet/sheet-view.vue';
function isZeppelinNote(nb: IWorkspace): boolean {
  return nb.type == WorkSpaceType.NOTEBOOK_ZEPPELIN;
}
function getComponent($ref: Vue| Vue[]) {
  return (Array.isArray($ref) ? ($ref as Vue[])[0] : $ref as Vue);
}
@Component({
  components: {
    WorkSpaceTabs,
    Notebook,
    SharedNotebook,
    DataMoveCmpnt,
    DataValidationCmpnt,
    Metadata,
    Dashboard,
    MultiLanguageNotebook,
    ZeppelinNotebook,
    WorkspaceGuide,
    MetaSheetView,
    WsReconnect,
  },
  filters: {
    zeppelinNote: isZeppelinNote,
  },
})
export default class WorkSpace extends Vue {

  /** provide service for create dialog */
  @Provide('notebookRemoteService')
  notebookRemoteService = new NotebookRemoteService();
  @Provide('zeppelinApi')
  zeppelinApi = new ZeppelinApi();

  /* computed vars from store */
  @State(state => state.workspace.workspaces)
  workspaces: Dict<IWorkspace>;

  @State(state => state.user.user.kylinProjects)
  kylinProjects: string[];

  @State(state => state.user.user.clusterOption['ApolloReno'].batchAccountOptions)
  batchAccounts: string[];

  @State(state => state.workspace.activeWorkspaceId)
  activeId: string;

  @State(state => state.workspace.loading)
  loading: boolean;

  @Getter('getFavoriteById')
  favoriteGetter: (id: string, type: 'nb' | 'pub_nb') => boolean;

  private zplMsgHanlder: ZeppelinMessageHandler;
  get _() {
    return _;
  }
  get domDisplay(): Array<IWorkspace> {

    const val =  _.chain(this.workspaces).filter((ws: IWorkspace) => ws.type === WorkSpaceType.NOTEBOOK_ZEPPELIN).value();
    // console.log("domDisplay", val);
    return val;
  }
  get currentWorkspaceDisplay(): 'alive' | 'dom' | 'empty' {
    if (_.isEmpty(this.workspaces) || !this.workspaces[this.activeId]) {
      return 'empty';
    }
    const ws = this.workspaces[this.activeId];
    return ws.type === WorkSpaceType.NOTEBOOK_ZEPPELIN ? 'dom' : 'alive';
  }

  mounted() {
    this.notebookRemoteService.props({ path: 'notebook' });
    this.zeppelinApi.props({ path: 'notebook' });
    this.zplMsgHanlder = new ZeppelinMessageHandler();
    this.zplMsgHanlder.register()
      .$on('ZPL_USED_CAPACITY', (msg) => {
        const nId = msg.noteId;
        const isActive = this.activeId === nId;
        if (!isActive) {
          return;
        }
        const cpnt = getComponent(this.$refs[nId] as Vue | Vue[]) as ZeppelinNotebook;
        cpnt.usedCapacity(msg.params);
      })
      .$on('ZPL_USED_CAPACITY_DIALOG', (msg) => {
        const nId = msg.noteId;
        const isActive = this.activeId === nId;
        if (!isActive) {
          return;
        }
        const cpnt = getComponent(this.$refs[nId] as Vue | Vue[]) as ZeppelinNotebook;
        cpnt.openUsedCapacityDialog();
      })
      .$on('ZPL_CONFIG_QUEUE', (msg) => {
        const nId = msg.noteId;
        const isActive = this.activeId === nId;
        if (!isActive) {
          return;
        }
        const cpnt = getComponent(this.$refs[nId] as Vue | Vue[]) as ZeppelinNotebook;
        cpnt.configuration(msg.params);
      })
      .$on('ZPL_UPLOAD_JAR', (msg) => {
        const nId = msg.noteId;
        const isActive = this.activeId === nId;
        if (!isActive) {
          return;
        }
        const cpnt = getComponent(this.$refs[nId] as Vue | Vue[]) as ZeppelinNotebook;
        cpnt.uploadFile();
      })
      .$on('ZPL_SHARE_NOTE', (msg) => {
        const nId = msg.noteId;
        const isActive = this.activeId === nId;
        if (!isActive) {
          return;
        }
        const cpnt = getComponent(this.$refs[nId] as Vue | Vue[]) as ZeppelinNotebook;
        cpnt.openShareUrl();
      })
      .$on('ZPL_GET_KYLIN_PJCTS', (msg) => {
        const nId = msg.noteId;
        const cpnt = getComponent(this.$refs[nId] as Vue | Vue[]) as ZeppelinNotebook;
        const packet = {
          action: 'ZETA_KYLIN_PROJECTS_LIST',
          params: {
            kylinProjects: this.kylinProjects,
          },
        };
        cpnt.postMessage(packet);
      })
      .$on('ZPL_GET_BATCH_ACCOUNT', (msg) => {
        const nId = msg.noteId;
        const cpnt = getComponent(this.$refs[nId] as Vue | Vue[]) as ZeppelinNotebook;
        const packet = {
          action: 'ZETA_BATCH_ACCOUNT_LIST',
          params: {
            batchAccounts: this.batchAccounts,
          },
        };
        cpnt.postMessage(packet);
      })
      .$on('ZPL_GET_NOTE_FVRT', (msg) => {
        const nId = msg.noteId;
        const cpnt = getComponent(this.$refs[nId] as Vue | Vue[]) as ZeppelinNotebook;
        // get favorite from store
        const favorite = this.favoriteGetter(nId, 'nb');
        const packet = {
          action: 'ZETA_FAVORITE',
          params: {
            favorite,
          },
        };
        cpnt.postMessage(packet);
      })
      .$on('ZPL_SET_NOTE_FVRT', (msg) => {
        if (!msg || !msg.params) {
          return;
        }
        const favorite = msg.params.favorite;
        const nId = msg.noteId;
        // set favorite to Store
        this.notebookRemoteService.favoriteNotebook(nId, 'nb', favorite)
          .then(() => {
            const action = favorite ? 'favorite' : 'unfavorite';
            this.$store.dispatch(action, {
              id: nId,
              favoriteType: 'nb',
            });
          });
      })
      .$on('ZPL_SEND_EL_MSG', (msg) => {
        const nId = msg.noteId;
        if (!msg.params) {
          return;
        }
        const content = msg.params.message;
        if (msg.params.type === 'error') {
          const exception = customZetaException(content, {}, {
            path: 'notebook',
            workspaceId: nId,
          });
          this.$store.dispatch('addException', {exception});
        } else {
          this.$message.success(content);
        }
        console.log('ZPL_SEND_EL_MSG', msg);
      });
  }

  destroyed() {
    this.zplMsgHanlder
      .$off('ZPL_CONFIG_QUEUE')
      .$off('ZPL_UPLOAD_JAR')
      .$off('ZPL_GET_KYLIN_PJCTS')
      .$off('ZPL_GET_NOTE_FVRT')
      .$off('ZPL_SET_NOTE_FVRT')
      .$off('ZPL_SEND_EL_MSG')
      .unregister();
  }
  onTabClick(currentId: string, newId: string){
    const oldTab = this.getTab(currentId);
    if (oldTab) oldTab.beforeClose();
    WorkspaceManager.getInstance(this).openTab(newId);
  }
  private getTab(nId: string) {
    if (Array.isArray(this.$refs[nId])) {
      return (this.$refs[nId] as Vue[])[0] as any as WorkspaceComponentBase;
    } else {
      return this.$refs[nId] as any as WorkspaceComponentBase;
    }
  }
  onTabClose(nId: string){
    WorkspaceManager.getInstance(this).closeTab(nId);
  }
}
</script>

<style lang="scss" scoped>
@import '@/styles/global.scss';
$nb-tabs-bar-height: $workspace-tab-height + $workspace-tab-margin-bottom + 15px;

.nb-tabs-wrapper {
  height: 100%;

  .workspace-container {
    height: calc(100% - #{$nb-tabs-bar-height});
  }
}

.add-dropdown-item-icon {
  height: 20px;
  width: 30px;
  position: relative;
  bottom: -4px;
  object-fit: contain;
}

.add-dropdown-item {
  color: inherit;
  text-decoration: none;
}

.running-icon-container {
  padding-left: 5px;
  padding-right: 5px;
}

.normal-placeholder {
  padding-left: 15px;
}

.updated-badge {
  position: absolute;
  top: 16px;
  right: 23px;

  width: 8px;
  height: 8px;

  background-color: rgba(255, 0, 0, 0.6);
  border-radius: 50%;
}

</style>
