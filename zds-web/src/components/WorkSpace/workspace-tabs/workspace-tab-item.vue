<template>
  <li
    :class="[nb.iconClass, isActive ? 'active': '']"
    :title="nb.name"
    @click="clickTab(nb.notebookId)"
  >
    <div class="tab-info">
      <workspace-status-icon :notebook-id="nb.notebookId" />
      <i
        class="workspace-icon"
      />
      <!-- PREFIX -->
      <span class="workspace-name">{{ getNamePrefix(nb) }}{{ nb.name }}</span>
    </div>

    <div class="tab-close">
      <i
        class="zeta-icon-close"
        @click.stop="closeTab(nb.notebookId)"
      />
    </div>
  </li>
</template>

<script lang="ts">
/**
 * Component <NotebookTabs>. Notebook tab navigator, wrapper of <Notebook>.
 * Opened notebook retrieved from store.
 */
import { Component, Vue, Prop } from 'vue-property-decorator';
import WorkspaceStatusIcon from '@/components/common/workspace/workspace-status-icon.vue';
import {
  INotebook,
  IWorkspace,
  WorkSpaceType,
} from '@/types/workspace';
// import ResizeObserver from 'resize-observer-polyfill';
import { State } from 'vuex-class';
import { WorkspaceSrv } from '../../../services/Workspace.service';

@Component({
  components: {
    WorkspaceStatusIcon,
  },
})
export default class WorkSpaceTabs extends Vue {
  @Prop()
  nb: INotebook;
  @State(state => state.workspace.activeWorkspaceId)
  activeId: string;

  get isActive () {
    return this.activeId === this.nb.notebookId;
  }


  getIconClass (ws: IWorkspace): string {
    // const active =this.isActive ? ' active' : '';
    if (ws.type === WorkSpaceType.NOTEBOOK || ws.type === WorkSpaceType.NOTEBOOK_COLLECTION || ws.type === WorkSpaceType.NOTEBOOK_ZEPPELIN) {
      return this.getPlatformIcon(ws as INotebook);
    } else if (ws.type === WorkSpaceType.DASHBOARD) {
      return 'dashboard';
    } else if (ws.type === WorkSpaceType.META_SHEET) {
      return 'meta-sheet';
    } else {
      return '';
    }
  }
  getPlatformIcon (note: INotebook) {
    const icon = WorkspaceSrv.getPlatformIcon(note);
    return icon;
  }
  clickTab (nid: string) {
    this.$emit('tabClick', nid);
  }
  closeTab (nid: string) {
    this.$emit('tabClose', nid);
  }
  getNamePrefix (ws: IWorkspace) {
    if (ws.type === WorkSpaceType.SHARED_NOTEBOOK) {
      return '[Shared] ';
    } else {
      return '';
    }
  }
}
</script>

<style lang="scss" scoped>
@import '@/styles/global.scss';
li {
  min-width: 0;
  flex: 1 120px;
  min-width: 120px;
  position: relative;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: 200px;
  background-color: $zeta-label-lvl2;
  padding: 0px 10px;
  color: $zeta-global-color-disable;
  // opacity: .5;
  border-right: 1px solid #dadbdd;
  .tab-info {
    font-weight: 600;
    display: flex;
    align-items: center;
    overflow: hidden;
    flex-grow: 1;
    /deep/ .notebook-status-icon {
      margin-right: 5px;
    }
    i.workspace-icon {
      min-width: 0;
      padding: 1px;
      font-size: 9px;
      border-radius: 2px;
      font-family: "Arial";
      font-style: normal;
      font-weight: bold;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      margin-right: 5px;
      flex-basis: 21px;
      flex-shrink: 0;
    }
    span.workspace-name {
      font-size: 12px;
      text-overflow: ellipsis;
      white-space: nowrap;
      overflow: hidden;
    }
  }
  .tab-close {
      display: flex;
      margin-left: 5px;
      cursor: pointer;
      i.zeta-icon-close {
        color: $zeta-global-color-disable;
        border-color: $zeta-global-color-disable;

      }
      &:hover i {
        color: $zeta-font-light-color;
      }

  }
  &:hover {
    .tab-close i.zeta-icon-close {
      color: $zeta-font-light-color;
    }
  }
  &.active {
    background-color: #fff;
    opacity: 1;
    flex-shrink: 0;
    flex-basis: 120px;
  }

}
</style>
