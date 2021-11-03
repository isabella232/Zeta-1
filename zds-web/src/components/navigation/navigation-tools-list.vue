<template>
  <el-dropdown
    class="nav-tools-list"
    :placement="'right'"
    @command="handleCommand"
  >
    <button
      class="nav-item-wrapper"
    >
      <div class="nav-item" :class="{'active': isToolkits}">
        <div class="nav-item-icon">
          <i :class="icon" />
        </div>

        <div class="nav-item-name">
          {{ name }}
        </div>
      </div>
    </button>
    <el-dropdown-menu
      slot="dropdown"
      :append-to-body="false"
      :visible-arrow="false"
    >
      <el-dropdown-item
        v-for="(tool, $i) in toolsList"
        :key="$i"
        class="nav-tools-item"
        :command="tool"
      >
        <navigation-item
          v-click-metric:NAV_CLICK="{name: tool.name}"
          :name="tool.name"
          :icon="tool.icon"
          :path="tool.path"
          :url="tool.url"
        />
      </el-dropdown-item>
    </el-dropdown-menu>
  </el-dropdown>
</template>

<script lang="ts">
import { Component, Vue, Inject } from 'vue-property-decorator';
import Navigation from './navigation.vue';
import * as Logger from '@/plugins/logs/zds-logger';
import NavigationItem from './navigation-item.vue';
const TOOLS_LIST = [
  {name: 'HDFS Management', path: '/hdfs', icon: 'zeta-icon-table'},
  {name: 'SQL Guide', path: '/sqlGuide', icon: 'zeta-icon-sqlConvert'},
  {name: 'Data Move', path: '/dataMove', icon: 'zeta-icon-dataMove'},
  {name: 'Data Validation', path: '/dataValidation', icon: 'zeta-icon-DataValidation'},
  // {name: 'Scheduler', path: '/schedule', icon: 'zeta-icon-workbook'},
  {name: 'Auto ETL', path: '/autoel', icon: 'zeta-icon-AutoEL2'},
  {name: 'Data Model', path: '/da', icon: 'zeta-icon-DA'},
  {name: 'Zeta Sheets', path: '/zeta-sheet', icon: 'zeta-icon-zetasheet'},
  {name: 'Realtime(alpha)', url: 'http://zeta.dss.vip.ebay.com/zpl-realtime/#/', icon: 'zeta-icon-monitor'},
];
@Component({
  components: {
    NavigationItem,
  },
})
export default class NavigationToolsList extends Vue {
  name = 'ToolKits';
  icon = 'zeta-icon-DA';

  toolsList = TOOLS_LIST;

  @Inject()
  pathTo: (path: string) => void;

  get opened() {
    const $parent = this.$parent as Navigation;
    return $parent.opened;
  }
  get isToolkits(){
    const path = this.$route.path;
    const cfg = this.toolsList.find(cfg => cfg.path === path);
    return cfg ? true: false;
  }
  handleCommand(tool: any) {
    // Logger.counter('NAV_CLICK',1, { name: tool.name });
    // this.$router.push(tool.path);
	  this.pathTo(tool.path);
    (this.$parent as Navigation).debounceClose();
  }

}
</script>
<style lang="scss" scoped>
$nav-bg-color: #282e35;
.nav-tools-list {
  /deep/ .el-popper {
    margin-left: 0 !important;
  }
  /deep/ .el-dropdown-menu__item {
    padding: 0;
  }
  .nav-tools-item {
    /deep/ .nav-item-icon {
      width: 50px !important;
    }
  }
  &:hover{
    .nav-item-wrapper .nav-item{
      background-color: mix(#fff, $nav-bg-color, 5%);
    }
  }
}
</style>
