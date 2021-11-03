<template>
  <div
    v-click-metric:NAV_CLICK="{name: 'Logo'}"
    class="nav-item-wrapper"
    @click="onClick"
  >
    <div class="nav-item">
      <div class="nav-item-left">
        <div class="nav-item-icon workspace">
          <i :class="icon" />
        </div>

        <div class="nav-item-name">
          {{ name }}
        </div>
      </div>

      <div
        v-if="!isShared && wssSorted && wssSorted.length > 0"
        class="nav-item-clear nav-item-right"
      >
        <i
          class="el-icon-close"
          @click="closeAll"
        />
      </div>
    </div>
    <div
      v-if="!isShared"
      v-show="opened && wssSorted.length > 0"
      class="nav-item-list"
    >
      <navigation-workspace-item
        v-for="ws in wssSorted"
        :key="ws.notebookId"
        :workspace-id="ws.notebookId"
        :class="{'active': activeId === ws.notebookId}"
      />
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Inject } from 'vue-property-decorator';
import { State } from 'vuex-class';
import { INotebook } from '@/types/workspace';
import NavigationWorkspaceItem from './navigation-workspace-item.vue';
import _ from 'lodash';
import { WorkspaceManager } from '@/types/workspace/workspace-manager';
import Util from '@/services/Util.service';

@Component({
  components: {
    NavigationWorkspaceItem
  },
})
export default class NavigationWorkspaceList extends Vue {
  name = 'Workspace';
  icon = 'zeta-icon-workspace';

  opened = false;

  @State(state => state.workspace ? state.workspace.workspaces : {})
  wss: Dict<INotebook>;

  @State(state => state.workspace ? state.workspace.activeWorkspaceId: null)
  activeId: string| null;

  @Inject()
  pathTo: (path: string) => void;

  get isShared() {
    return Util.isShareApp();
  }
  get wssSorted() {
    return _.chain(this.wss)
      .sortBy(['seq'])
      .value();
  }
  close() {
    this.opened = false;
  }
  open() {
    this.opened = true;
  }
  onClick() {
    this.pathTo('/notebook');
  }
  closeAll() {
    _.forEach(this.wss, (wId) => {
      WorkspaceManager.getInstance(this).closeTab(wId.notebookId);
    });
  }

}
</script>
<style lang="scss" scoped>

$color:#94BFE1;
$wsColor:#00A7C8;
.nav-item {
  justify-content: space-between;
  .nav-item-left {
    display: flex;
    justify-content: flex-start;
    .nav-item-icon.workspace {
      i.zeta-icon-workspace{
        color:$wsColor;
      }
    }
    .nav-item-name {
      width: auto;
      font-weight: 800;
    }
  }
  &:hover .nav-item-right {
    display: flex;
  }

}
.nav-item-clear {
  margin-right: 15px;
  display: none;
  align-items: center;
}
.nav-item-list {
  max-height: 200px;
  overflow-y: auto;
}

</style>
