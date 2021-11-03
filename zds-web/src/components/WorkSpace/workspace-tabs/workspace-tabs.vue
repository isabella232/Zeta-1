<template>
  <div class="ws-tabs-nav-bar">
    <ul
      ref="scrollTabs"
      :class="size"
    >
      <draggable
        :list="workspaceSorted"
        class="drag-tabs"
        ghost-class="ghost"
        @start="dragging = true"
        @end="dragging = false"
      >
        <workspace-tab-item
          v-for="nb in workspaceSorted"
          :key="nb.notebookId"
          :nb="nb"
          @tabClick="clickTab"
          @tabClose="closeTab"
        />
      </draggable>
    </ul>
    <el-dropdown
      :show-timeout="0"
      placement="bottom-start"
      size="small"
      @command="addTab"
    >
      <button class="new" />
      <el-dropdown-menu
        slot="dropdown"
        class="workspace-add-drop-down"
      >
        <el-dropdown-item
          v-click-metric:WORKSPACE_TABS_CLICK="{name: 'from-repo'}"
          command="from-repo"
        >
          <i class="zeta-icon-fold1" />
          From Repository
        </el-dropdown-item>
        <el-dropdown-item
          v-click-metric:WORKSPACE_TABS_CLICK="{name: 'createNew'}"
          command="new"
        >
          <i class="zeta-icon-notebook" />
          New Notebook
        </el-dropdown-item>
      </el-dropdown-menu>
    </el-dropdown>
    <!-- New Tab dialog -->
    <!-- @new-notebook-fail="newNbDialog.visible = loading = false" -->
    <new-notebook-dialog
      :visible.sync="newNbDialog.visible"
      @new-notebook-success="newNbDialog.visible = false"
      @new-notebook-fail="newNbDialog.visible = loading = false"
    />
  </div>
</template>

<script lang="ts">
/**
 * Component <NotebookTabs>. Notebook tab navigator, wrapper of <Notebook>.
 * Opened notebook retrieved from store.
 */
import { Component, Vue, Watch, Inject } from 'vue-property-decorator';
import _ from 'lodash';
import draggable from 'vuedraggable';
import WorkspaceStatusIcon from '@/components/common/workspace/workspace-status-icon.vue';
import NewNotebookDialog from '@/components/common/workspace/create-notebook-dialog/create-notebook-dialog.vue';
import WorkspaceTabItem from '@/components/WorkSpace/workspace-tabs/workspace-tab-item.vue';
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import {
  INotebook,
  IWorkspace,
  WorkSpaceType,
  WorkspaceBase
} from '@/types/workspace';
import ZeppelinApi from '@/services/remote/ZeppelinApi';
import { State } from 'vuex-class';
import ResizeObserver from 'resize-observer-polyfill';
import { WorkspaceManager } from '@/types/workspace/workspace-manager';
function isZetaNote(nb: IWorkspace): boolean {
  return nb.type == WorkSpaceType.NOTEBOOK || nb.type == WorkSpaceType.NOTEBOOK_COLLECTION;
}
function isZeppelinNote(nb: IWorkspace): boolean {
  return nb.type == WorkSpaceType.NOTEBOOK_ZEPPELIN;
}

@Component({
  components: {
    WorkspaceStatusIcon,
    WorkspaceTabItem,
    NewNotebookDialog,
    draggable
  }
})
export default class WorkSpaceTabs extends Vue {

  @Inject('notebookRemoteService')
  notebookRemoteService: NotebookRemoteService;

  @Inject('zeppelinApi')
  zeppelinApi: ZeppelinApi;

  @State(state => state.workspace.activeWorkspaceId)
  activeId: string;

  size: '' | 'size-small' | 'size-mini' = '';
  obsrv: ResizeObserver;

  newNbDialog: { visible: boolean } = { visible: false };
  dragging: false;
  workspaceSorted: INotebook[] = [];

  /* computed vars from store */
  get workspaces(): Dict<INotebook> {
    return this.$store.state.workspace.workspaces;
  }

  clickTab(nid: string) {
    const activeId = this.$store.getters.activeWorkspaceId;
    if (nid != activeId) {
      this.$emit('onTabActivate', activeId, nid);
    }
  }
  closeTab(nid: string) {
    this.$emit('onTabClose', nid);
  }

  addTab(command: string) {
    if (command === 'new') {
      this.newNbDialog.visible = true;
      return;
    } else if (command === 'from-repo') {
      this.$router.push({ path: '/repository' });
    }
  }

  mounted() {
    this.notebookRemoteService.props({
      path: 'notebook'
    });
    this.workspaceSorted = _.chain(this.workspaces)
      .sortBy(['seq'])
      .value();
    this.obsrv = new ResizeObserver(entries => {
      const resizeObject = entries.find(entry => entry.target === this.$el);
      if (resizeObject) {
        this.onSizeChange(resizeObject.contentRect.width);
      }
    });
    this.obsrv.observe(this.$el);
  }
  destroyed() {
    this.obsrv.unobserve(this.$el);
  }

  get $scrollTabs(){
    return this.$refs['scrollTabs'] as HTMLElement;
  }

  @Watch('workspaces', {deep: true})
  onWorkspacesChange(newVal: Dict<INotebook>, oldVal: Dict<INotebook>) {
    this.workspaceSorted = _.chain(this.workspaces)
      .sortBy(['seq'])
      .value();
    this.onSizeChange(this.$el.offsetWidth);
  }
  @Watch('workspaceSorted', { deep: true })
  onWorkspaceSortedChange(newVal: INotebook[]) {
    const seqArr = _.map(newVal, (ws, i) => {
      return {
        notebookId: ws.notebookId,
        type: ws.type,
        seq: i,
      } as WorkspaceBase;
    });
    WorkspaceManager.getInstance(this).onTabSequenceChange(seqArr);
  }

  onSizeChange(width: number) {
    const tabCnt = Object.keys(this.workspaces).length;
    const tabWidth = (width - 30 - 200)/ (tabCnt -1);
    if (tabCnt === 0 || tabWidth >= 140) {
      this.size = '';
    } else if (tabWidth > 40){
      this.size = 'size-small';
    } else {
      this.size = 'size-mini';
    }

  }
}
</script>

<style lang="scss" scoped>
@import "@/styles/global.scss";
::-webkit-scrollbar {
    height: 5px;
}
::-webkit-scrollbar-thumb {
    background-color: #c1c1c1;
}
$apolloColor: #569ce1;
$aresColor: #D85945;
$hermesColor: #E2B258;
$herculesColor: #D88045;
$numozartColor: #9a6197;
$hopperColor: #608d75;
$kylinColor: #965D31;
$dashboardColor: #41D3BD;
$metaSheetColor: #E6A23C;
@mixin wsTab($name, $color, $content, $shortContent) {
  &.size-mini {
    /deep/ li:not(.active).#{$name} {
      .tab-info {
        i.workspace-icon {
          &::after {
            content: $shortContent !important;
          }
        }
      }
    }
  }
  /deep/ li.#{$name} {
    .tab-info {
      i.workspace-icon {
        color: $color;
        border: 1px solid $color;
        padding: 1px 3px;
        text-align: center;
        &::after {
            content: $content;
        }
      }
    }
    &:hover {
      color: $color;
      span.workspace-name {
        color: #909399;
      }
    }
    &.active {
      color: $color;
      span.workspace-name {
        color: #909399;
      }
      border-bottom: 2px solid $color;
    }
  }
}

.ws-tabs-nav-bar {
    height: $workspace-tab-height;
    margin-left: -15px;
    margin-right: -15px;
    margin-bottom: $workspace-tab-margin-bottom;
    display: flex;
    align-items: center;
    // justify-content: space-between;
    color: rgba(255, 255, 255, 1);
    -webkit-box-shadow: 0 2px 4px 0 rgba(0, 0, 0, 0.12),
        0 0 6px 0 rgba(0, 0, 0, 0.04);
    box-shadow: 0 2px 4px 0 rgba(0, 0, 0, 0.12), 0 0 6px 0 rgba(0, 0, 0, 0.04);
    > * {
        color: initial;
    }

    button {
        cursor: pointer;
    }
    // move to src/styles/workspace.scss
    ul{
        display: flex;
        .drag-tabs {
            display: flex;
            flex-grow: 1;
            height: 100%;
            width: 100%;
            flex-wrap: nowrap;
            .sortable-ghost {
                opacity: 0.5;
                background: #c8ebfb;
            }
        }
        @include wsTab("dashboard", $dashboardColor, "DAH", "D");
        @include wsTab("hercules", $herculesColor, "HER", "H");
        @include wsTab("herculessub", $herculesColor, "HSB", "H");
        @include wsTab("apollo", $apolloColor, "APO", "A");
        @include wsTab("ares", $aresColor, "ARE", "A");
        @include wsTab("numozart", $numozartColor, "MOZ", "M");
        @include wsTab("mozart", $numozartColor, "MOZ", "M");
        @include wsTab("hopper", $hopperColor, "HOP", "H");
        @include wsTab("hermes", $hermesColor, "HEM", "H");
        @include wsTab("hermeslvs", $hermesColor, "HEM", "H");
        @include wsTab("kylin", $kylinColor, "KYL", "K");
        @include wsTab("meta-sheet", $metaSheetColor, "MET", "M");
        &.size-small, &.size-mini {
          flex-grow:1;
          /deep/ li:not(.active) {
            min-width: 0;
            .tab-info {
              i.workspace-icon {
                flex-shrink: 0;
              }
            }

          }
        }
        &.size-small {
          /deep/ li:not(.active) {
            &:not(:hover) .tab-close {
              display: none;
            }
          }
        }
        &.size-mini {
          /deep/ li:not(.active) {
            justify-content: center;
            .tab-info {
              i.workspace-icon {
                flex-basis: auto;
                flex-shrink: 0;
              }
              .workspace-name {
                display: none;
              }
            }
            .tab-close {
              display: none;
            }
          }
        }

    }

    .el-dropdown {
        button.new {
            width: $workspace-tab-height;
            height: $workspace-tab-height;
            align-self: flex-end;
            outline: none;
            position: relative;
            background-color: transparent;
            color: #fff;
            &:after {
                content: "+";
                font-size: 27px;
                color: $zeta-global-color;
            }

            &:hover {
                &:after {
                    color: $zeta-global-color-heighlight;
                }
            }
        }
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

// .updated-badge {
//     position: absolute;
//     top: 16px;
//     right: 33px;

//     width: 8px;
//     height: 8px;

//     background-color: rgba(255,0,0,0.6);
//     border-radius: 50%;
// }
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
// .message-dot {
//     width: 8px;
//     height: 8px;
//     border-radius: 50%;
//     display: inline-block;
//     &.message-dot-connecting {
//         animation: fade 1000ms infinite;
//     }
//     &.message-dot-connected {
//         background-color: #5cb85c;
//     }
//     &.message-dot-disconnected {
//         background-color: transparent;
//     }
//     &.message-dot-update {
//         background-color: rgba(255, 0, 0, 0.6);
//     }
// }
.el-dropdown-menu__item {
    &:hover {
        /deep/ i {
            color: inherit;
        }
    }
}
</style>
