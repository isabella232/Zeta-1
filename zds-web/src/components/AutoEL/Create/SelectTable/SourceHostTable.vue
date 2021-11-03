<template>
  <div class="source-host-table">
    <el-table
      :data="hostData"
    >
      <el-table-column label="Table">
        <template slot-scope="scope">
          <el-input
            v-model="scope.row.table_name"
            @change="onChange"
          />
        </template>
      </el-table-column>
      <el-table-column label="DBC">
        <template slot-scope="scope">
          <el-select
            v-model="scope.row.dbc_name"
            filterable
            allow-create
            placeholder="Select DBC"
            style="width:calc(100% - 35px);"
            @change="onDBCChange"
          >
            <el-option 
              v-for="dbc in scope.row.list"
              :key="dbc.dbc_name"
              :value="dbc.dbc_name"
              :label="dbc.dbc_name"
            >
              {{ dbc.dbc_name }}
            </el-option>
          </el-select>
          <el-popover
            v-if="DBCInfo[scope.row.dbc_name]"
            placement="right"
            width="400"
            trigger="hover"
          >
            <div style="margin:0px 24px">
              <h1 class="dbc-name">
                {{ DBCInfo[scope.row.dbc_name].dbc_name }}
              </h1>
              <labeled label="DB Name">
                {{ DBCInfo[scope.row.dbc_name].db_name }}
              </labeled>
              <labeled label="SID">
                {{ DBCInfo[scope.row.dbc_name].sid }}
              </labeled>
              <labeled label="Physical Host">
                {{ DBCInfo[scope.row.dbc_name].host }}
              </labeled>
            </div>
            <i
              slot="reference"
              class="el-icon-info"
              style="padding-left:12px;font-size:1.3em;"
            />
          </el-popover>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>


<script lang="ts">
import { Component, Vue, Prop, Emit, Watch } from 'vue-property-decorator';
import { Host, DBC } from '../../types';
import Labeled from '../components/Labeled.vue';
import DBCInfoService from './DBCInfoService';

interface HostData {
  table_name: string;
  dbc_name: string;
  list: Array<DBC>;
}

@Component({
  components: {
    Labeled,
  },
})
export default class SourceHostTable extends Vue {
  @Prop() hosts: Array<Host>;
  @Prop() sourceTableDetail: Array<{ table_name: string; list: Array<DBC> }>;

  DBCInfo: { [prop: string]: DBC } = {};

  get hostData (): Array<HostData> {
    return this.hosts.map(host => {
      const matchedRow = (this.sourceTableDetail || []).find(st => st.table_name === host.table_name);
      return {
        table_name: host.table_name,
        dbc_name: host.dbc_name,
        list: matchedRow ? matchedRow.list : [],
      };
    }).sort((a, b) => a.table_name.localeCompare(b.table_name));
  }
  @Watch('hosts', { immediate: true })
  async getInitialDBCInfo (hosts) {
    const res = await DBCInfoService.getFromCache(hosts.map(h => h.dbc_name));
    Object.keys(res).forEach(key => Vue.set(this.DBCInfo, key, res[key]));
  }
  @Emit('change')
  onDBCChange (dbcName) {
    this.getDBCInfo(dbcName);
    this.$parent.$emit('el.form.blur'); // simulate validate event on ElFromItem
    return this.onChange();
  }
  @Emit('change')
  onChange () {
    return this.hostStr();
  }
  hostStr () {
    return this.hostData
      .map(host => {
        return `${host.table_name},${host.dbc_name}`;
      })
      .join('\n');
  }
  async getDBCInfo (dbcName) {
    const res = await DBCInfoService.getFromCache(dbcName);
    Vue.set(this.DBCInfo, dbcName, res[dbcName]);
  }
}
</script>

<style lang="scss" scoped>
.dbc-name {
  font-size: 18px;
  font-weight: normal;
  color: #569ce1;
  padding-bottom: 8px;
}
/deep/ .el-input .el-input__inner {
  border-color: #DCDFE6 !important;
}
</style>
