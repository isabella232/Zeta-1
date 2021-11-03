<template>
  <div class="join-table">
    <label style="font-weight:bold;padding-right:8px;">
      <i class="el-icon-search" />
      Join Other Table</label>
    <el-select
      v-model="joinTable"
      remote
      filterable
      placeholder="Search the table you want to join"
      :remote-method="fetchTables"
      :loading="loading"
      style="width:320px;"
      @focus="loadOptions"
      @change="() => showDialog = true"
    >
      <el-option
        v-for="o in filteredOptions"
        :key="o.join_table"
        :label="o.join_table"
        :value="o.join_table"
      />
    </el-select>
    <SmartQueryDialog
      :visible.sync="showDialog"
      :join-table="joinTable"
      :main-table="tableName"
      :smart-query-table-arr="filteredOptions"
    />
  </div>
</template>
<script lang="ts">
import { Component, Mixins } from 'vue-property-decorator';
import SmartQueryDialog from '@/components/WorkSpace/Metadata/sub-view/smart-query-dialog.vue';
import DoeRemoteService from '@/services/remote/DoeRemoteService';
import { Common } from '../mixins/Common';
import { matchString } from '../../utils';

const API = DoeRemoteService.instance;

@Component({
  components: {
    SmartQueryDialog,
  },
})
export default class JoinTable extends Mixins(Common) {

  loading = false;
  query = '';
  options: any[] = [];
  joinTable = null;
  showDialog = false;

  get filteredOptions () {
    return this.options.filter(i => {
      if (!this.query) return true;
      return matchString(this.query, i.join_table.split('.')[1]);
    });
  }
  async fetchTables (query) {
    this.query = query;
  }
  async loadOptions () {
    if (this.options.length > 0) {
      return;
    }
    this.loading = true;
    const res = await API.getSearchTables({ tables: '.' + this.tableName });
    this.options = res.data.data.value;
    this.loading = false;
  }
}
</script>
<style lang="scss" scoped>

.join-table {
  display: inline-block;
}
</style>