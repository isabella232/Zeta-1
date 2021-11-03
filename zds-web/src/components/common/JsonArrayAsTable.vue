<template>
  <el-table :data="jsonArray">
    <el-table-column
      v-for="col in columns"
      :key="col"
      :label="col"
      :prop="col"
    />
  </el-table>
</template>

<script lang="ts">
import { Component, Vue, Prop } from 'vue-property-decorator';

@Component({
})
export default class JsonArrayAsTable extends Vue {

  @Prop() json: string | object;

  get jsonArray() {
    const json = typeof(this.json) === 'string' ? JSON.parse(this.json) : this.json;
    if (!Array.isArray(json)) {
      return [];
    }
    return json;
  }

  get columns() {
    const sample = this.jsonArray[0] || {};
    return Object.keys(sample);
  }
}
</script>
<style lang="scss">

</style>


