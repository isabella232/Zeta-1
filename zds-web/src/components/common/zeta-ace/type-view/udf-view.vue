<template>
  <div>
    <div class="name">{{ item.detail.udf.name }}</div>
    <table>
      <tr>
        <th>DataBaseName:</th>
        <td>{{ item.detail.udf.dbName?item.detail.udf.dbName.replace(/[\r\n]/g,""):'' }}</td>
      </tr>
      <tr>
        <th>HadoopName:</th>
        <td>{{ item.detail.udf.name }}</td>
      </tr>
      <tr>
        <th>TeraDataName:</th>
        <td>{{ item.detail.udf.teradata }}</td>
      </tr>
      <tr>
        <th>ClassPath:</th>
        <td>{{ item.detail.udf.classpath }}</td>
      </tr>
      <tr>
        <th>Description:</th>
        <td>{{ item.detail.udf.comment }}</td>
      </tr>
      <tr>
        <th>Example:</th>
        <td>{{ item.detail.udf.example }}</td>
      </tr>
    </table>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop } from "vue-property-decorator";
import { Message } from "@/types/ace";
@Component({
  components: {}
})
export default class UDFView extends Vue {
  @Prop() item: Message;
}
</script>
<style lang="scss" scoped>
.name {
  margin-bottom: 10px;
  font-size: 14px;
  font-weight: bold;
}
table {
  border-spacing: 0;
  font-size: 13px;
  th,
  td {
    padding-bottom: 2px;
  }
  th {
    vertical-align: top;
    white-space: nowrap;
    text-align: left;
    padding-right: 5px;
  }
}
</style>
