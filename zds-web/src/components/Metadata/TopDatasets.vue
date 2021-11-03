<template>
  <div>
    <span class="top_title">Recommended Datasets</span>
    <ul>
      <li
        v-for="(table, $i) in data"
        :key="$i"
        class="table_name"
        @click="jumpMetadata(table)"
      >
        {{ table.replace(/\"/g, "").toUpperCase() }}
      </li>
    </ul>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop } from 'vue-property-decorator';
import * as Logger from '@/plugins/logs/zds-logger';
import _ from 'lodash';
@Component({
  components: {},
})
export default class TopDatasets extends Vue {
  @Prop() data: any;
  
  jumpMetadata (table: string) {
    Logger.counter('METADATA_TABLE_DETAIL', 1, { name: 'overview', trigger: 'metadata_browse_datasets_click', table: table.replace(/\"/g, '').toUpperCase() });
    this.$emit('record-store');
    sessionStorage.setItem('metadata_table', table.replace(/\"/g, '').toUpperCase());
    sessionStorage.setItem('metadata_table_type', 'table');
    this.$router.push(`/metadata/tables/${table.replace(/\"/g, '').toUpperCase()}`);
  }
}
</script>
<style lang="scss" scoped>
ul {
  display: inline-block;
  list-style-type: none;
  width: 100%;
  > li {
    margin: 5px 0;
  }
}
.top_title {
  color: #CACBCF;
  font-size: 14px;
  font-weight: 700;
}
.table_name {
  margin: 10px 0;
  cursor: pointer;
  color: #569CE1;
}
.table_name:hover {
  color: #4D8CCA;
}
</style>
