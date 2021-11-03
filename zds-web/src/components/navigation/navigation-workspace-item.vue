<template>
  <div
    v-if="workspace"
    class="nav-workspace-item"
    @click="onClick"
    @mouseover="mouseIn"
    @mouseout="mouseOut"
  >
    <div class="nav-workspace-item-left">
      <workspace-status-icon :notebook-id="workspace.notebookId" />
      <i
        class="workspace-icon"
        :class="getIconClass(workspace)"
      />
      <span
        class="nav-workspace-item-name"
        :title="workspace.name"
      >
        {{ workspace.name }}
      </span>
    </div>
    <div
      v-show="showClose"
      class="nav-workspace-item-right"
      @click.stop="closeNote"
    >
      <i class="el-icon-close" />
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop } from 'vue-property-decorator';
import { INotebook, IWorkspace, WorkSpaceType } from '@/types/workspace';
import WorkspaceStatusIcon from '@/components/common/workspace/workspace-status-icon.vue';
import { WorkspaceManager } from '@/types/workspace/workspace-manager';
import { WorkspaceSrv } from '@/services/Workspace.service';
@Component({
  components: {
    WorkspaceStatusIcon,
  },
})
export default class NavigationWorkspaceItem extends Vue {
  @Prop()
  workspaceId: string;

  showClose = false;
  get workspace () {
    if (this.workspaceId) {
      return this.$store.state.workspace.workspaces[this.workspaceId];
    }
    return undefined;
  }
  onClick () {
    WorkspaceManager.getInstance(this).openTab(this.workspaceId);
  }
  mouseIn () {
    this.showClose = true;
  }
  mouseOut () {
    this.showClose = false;
  }
  closeNote () {
    WorkspaceManager.getInstance(this).closeTab(this.workspaceId);
  }
  getIconClass (ws: IWorkspace): string {
    if (ws.type === WorkSpaceType.NOTEBOOK || ws.type === WorkSpaceType.NOTEBOOK_COLLECTION || ws.type === WorkSpaceType.NOTEBOOK_ZEPPELIN || ws.type === WorkSpaceType.SHARED_NOTEBOOK) {
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
    return WorkspaceSrv.getPlatformIcon(note);
  }

}
</script>
<style lang="scss" scoped>
.nav-workspace-item-left {
  /deep/ .notebook-status-icon {
    margin-left: -10px;
    margin-right: 2px;
  }
}
</style>
