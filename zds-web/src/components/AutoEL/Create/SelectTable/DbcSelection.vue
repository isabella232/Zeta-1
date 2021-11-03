<template>
  <el-table
    :data="hostsCopy"
    max-height="250"
  >
    <el-table-column
      prop="table_name"
      label="Table"
    />
    <el-table-column label="DBC">
      <template slot-scope="scope">
        <div class="dbc-selection">
          <el-dropdown
            placement="bottom"
            @command="cmd => onChange(cmd, scope.row)"
          >
            <span class="dropdown-link">
              {{ scope.row.value }}<i class="el-icon-arrow-down el-icon--right" />
            </span>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item
                v-for="dbc in scope.row.list"
                :key="dbc.dbc_name"
                class="dropdown-item"
                :command="dbc.dbc_name"
              >
                <h1 class="dbc-name">
                  {{ dbc.dbc_name }}
                </h1>
                <labeled label="DB Name">
                  {{ dbc.db_name }}
                </labeled>
                <labeled label="SID">
                  {{ dbc.sid }}
                </labeled>
                <labeled label="Physical Host">
                  {{ dbc.physical_host }}:{{ dbc.port }}
                </labeled>
                <div class="separator" />
              </el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </div>
      </template>
    </el-table-column>
  </el-table>
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch } from 'vue-property-decorator';
import Labeled from '../components/Labeled.vue';

interface DBC {
  db_name: string;
  dbc_name: string;
  physical_host: string;
  port: string;
  sid: string;
}

interface Host {
  table_name: string;
  list: DBC[];
  value?: string;
}

@Component({
  components: { Labeled },
})
export default class DbcSelection extends Vue {

  @Prop() hosts: Host[];

  hostsCopy: Host[] = [];
  get raw() {
    return  this.hostsCopy.map(h => `${h.table_name},${h.value}`).join('\n');
  }
  onChange(cmd, model) {
    model.value = cmd;
    this.$emit('change', this.raw);
  }
  @Watch('hosts', { deep: true, immediate: true })
  makeCopy() {
    const hosts = this.hosts || [];
    this.hostsCopy = hosts.map(h => ({
      ...h,
      value: h.list[0].dbc_name,
    }));
    this.$emit('change', this.raw);
  }
}
</script>

<style lang="scss" scoped>
.dbc-selection {
  .dropdown-link {
    color: #569ce1;
  }

}
/deep/ h1.dbc-name {
  font-size: 14px;
  font-weight: normal;
  color: #569ce1;
}
/deep/ .separator {
  margin: 10px 6px; 
  background: #ddd;
  height: 1px;
}
/deep/ .labeled {
  padding: 0 0;
  font-size: 0.8em;
  label {
    width: 80px;
  }
}
</style>