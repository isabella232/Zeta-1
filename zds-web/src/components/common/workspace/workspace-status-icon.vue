<template>
  <div
    v-if="isNote && display"
    class="notebook-status-icon"
  >
    <i
      v-if="isRunning"
      class="icon el-icon-loading"
    />
    <span
      v-else
      class="message-dot"
      :class="iconClass"
    />
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop } from 'vue-property-decorator';
import { INotebook, WorkSpaceType, NotebookStatus } from '@/types/workspace';
import { Getter, State } from 'vuex-class';


@Component
export default class WorkspaceStatusIcon extends Vue {
  @Prop()
  notebookId: string;

  @State(state => state.workspace.activeWorkspaceId)
  activeId: string;
  // FIXME@Note2.0
  @Getter('nbByNId')
  getterNB: (nId: string) => INotebook;

  get note () {
    return this.getterNB(this.notebookId);
  }
  get display () {
    return this.iconClass != 'message-dot-disconnected';
  }
  get isNote () {
    return this.note.type === 'NOTEBOOK' || this.note.type === 'NOTEBOOK_COLLECTION' || this.note.type === 'SHARED_NOTEBOOK';
  }
  get isRunning () {
    if (this.isNote) {
      if (this.note.status.toString() === 'RUNNING') {
        return true;
      }
    }
    return false;
  }
  get iconClass () {
    if (this.note.notebookId !== this.activeId && this.note.resultUpdated) {
      return 'message-dot-update';
    }
    if (this.isConnect(this.note)) {
      return 'message-dot-connected';
    }
    if (this.note.status == 'CONNECTING') {
      return 'message-dot-connecting';
    }
    return 'message-dot-disconnected';
  }

  isConnect (nb: INotebook): boolean {
    if (nb.type == WorkSpaceType.NOTEBOOK_ZEPPELIN) return false;
    switch (nb.status) {
      case NotebookStatus.INITIAL:
      case NotebookStatus.OFFLINE:
      case NotebookStatus.DISCONNECTING:
      case NotebookStatus.CONNECTING:
        return false;
      default:
        return true;
    }
  }
}
</script>

<style lang="scss" scoped>
@keyframes fade {
    from {
        background-color: transparent;
    }
    50% {
        background-color: #5cb85c;
    }
    to {
        background-color: transparent;
    }
}
.notebook-status-icon {
  display: flex;
  justify-content: center;
  align-items: center;
}
.message-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
  &.message-dot-connecting {
    animation: fade 1000ms infinite;
    font-weight: 600;
    color: #8b8b8b;
  }
  &.message-dot-connected {
    background-color: #5cb85c;
  }
  &.message-dot-disconnected {
    display: none;
    background-color: transparent;
  }
  &.message-dot-update {
    background-color: rgba(255, 0, 0, 0.6);
  }
}
</style>
