<template>
  <div>
    <div class="table-title">
      <p class="name">{{ table.name }}<i class="zeta-icon-export" @click="openNewTab(table)" /></p>
      <el-radio-group
        v-model="table.activeTab"
        size="mini"
        @change="changeTab(table.activeTab)"
      >
        <el-radio-button label="overview" border>Overview</el-radio-button>
        <el-radio-button label="schema" border>Schema</el-radio-button>
      </el-radio-group>
    </div>
    <div v-if="table.activeTab === 'overview'">
      <div class="platform">
        <em>Platform:</em>
        <el-radio-group
          v-model="table.platform"
          size="small"
          @change="changePlatform(table.platform)"
        >
          <el-radio-button
            v-for="platform in table.platformArr"
            :key="platform"
            :label="platform"
            border
          >{{ platform.toLowerCase() === 'numozart'?'Mozart':platform }}</el-radio-button>
        </el-radio-group>
      </div>
      <table>
        <tr>
          <th>Table:</th>
          <td>
            <ul>
              <li
                v-for="(t, key) in table.tableArr"
                :key="key"
              >{{(t.db_name != undefined && t.db_name != "" ? t.db_name.toUpperCase() : "DEFAULT")}}.{{t.table_name?t.table_name.toUpperCase():""}}</li>
            </ul>
          </td>
        </tr>
        <tr>
          <th>View:</th>
          <td>
            <ul>
              <li
                v-for="(view, key) in table.viewArr"
                :key="key"
              >{{(view.view_db != undefined && view.view_db != "" ? view.view_db.toUpperCase() : "DEFAULT")}}.{{view.view_name?view.view_name.toUpperCase():""}}</li>
            </ul>
          </td>
        </tr>
      </table>
    </div>
    <SchemaView
      v-else
      :metadataTableDict="table.metadataTable"
      :metadataViewDict="table.metadataView"
    />
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch } from 'vue-property-decorator';
import _ from 'lodash';
import { allPlatform } from '../utilties';
import SchemaView from './schema-view.vue';
import { openTab } from '../utilties';

@Component({
  components: { SchemaView }
})
export default class TableView extends Vue {
  @Prop() data: any;
  get table(){
    return this.data;
  }
  changeTab(tabName: string) {
    this.table.activeTab = tabName;
  }
  changePlatform(platform: string) {
    const pickTable: any = _.pickBy(
      this.table.metadataTable,
      (v: any) => {
        return _.toLower(v.platform) == _.toLower(platform);
      }
    );
    const pickView: any = _.pickBy(
      this.table.metadataView,
      (v: any) => {
        return _.toLower(v.platform) == _.toLower(platform);
      }
    );
    this.table.platform = platform;
    this.table.tableArr = pickTable;
    this.table.viewArr = pickView;
  }
  openNewTab(meta: any){
    openTab(meta);
  }
}
</script>