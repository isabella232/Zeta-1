<template>
  <el-dialog
    :visible="show"
    width="50%"
    top="8vh"
    lock-scroll
    :show-close="false"
  >
    <template slot="title">
      <div class="dialog-title">
        Sample data of <strong v-if="dbc">{{ dbc.table_name }}</strong>
      </div>
    </template>
    <div
      v-loading="loading"
      class="json-wrapper"
    >
      <JsonArrayAsTable
        v-if="dbc"
        :json="sourceTableSample[dbc.table_name]"
      />
    </div>
    <footer slot="footer">
      <el-button @click="onClose">
        Close
      </el-button>
    </footer>
  </el-dialog>
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch, Emit } from 'vue-property-decorator';
import { Action, Getter, State } from 'vuex-class';
import { Actions } from '@/components/AutoEL/store';
import JsonArrayAsTable from '@/components/common/JsonArrayAsTable.vue';

@Component({
  components: {
    JsonArrayAsTable,
  },
})
export default class SampleDialog extends Vue {
  @Prop() show: boolean;
  @Prop() dbc: { table_name: string; dbc_name: string };
  @Prop() saCode: string;
  @Action(Actions.GetSourceTableSample) getSourceTableSample;
  @State(state => state.AutoEL.sourceTableSample) sourceTableSample;
  @Getter('$globalLoading') loading;

  @Watch('show')
  loadData(show) {
    if (!this.dbc) return;
    const { getSourceTableSample, sourceTableSample, dbc: { table_name, dbc_name }, saCode} = this;
    if (show) {
      if (!sourceTableSample[table_name]) {
        getSourceTableSample({ table: table_name, dbcName: dbc_name, saCode });
      }
    }
  }
  @Emit('close')
  onClose() {
    //
  }
}
</script>

<style lang="scss" scoped>

</style>