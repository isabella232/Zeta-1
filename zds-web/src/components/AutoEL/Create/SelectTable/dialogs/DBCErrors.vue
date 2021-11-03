<template>
  <el-dialog
    :visible="show"
    width="50%"
    top="8vh"
    lock-scroll
  >
    <template slot="title">
      <div class="dialog-title">
        DBC validation result
      </div>
    </template>

    <el-table :data="data">
      <el-table-column
        v-for="col in columns"
        :key="col.prop"
        :label="col.label"
        :prop="col.prop"
      />
    </el-table>

    <footer slot="footer">
      <el-button @click="onClose">
        Close
      </el-button>
    </footer>
  </el-dialog>
</template>

<script lang="ts">
import { Component, Vue, Prop, Emit } from 'vue-property-decorator';

@Component({
  components: {
  },
})
export default class DBCErrorsDialog extends Vue {
  @Prop() show: boolean;
  @Prop() data: any;

  columns = [
    { 
      label: 'Table',
      prop: 'table_name',
    },
    { 
      label: 'DBC',
      prop: 'dbc_name',
    },
    { 
      label: 'Message',
      prop: 'message',
    },
  ];

  @Emit('close')
  onClose() {
    //
  }
}
</script>

<style lang="scss" scoped>

</style>