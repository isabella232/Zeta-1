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
        Schema of <strong>{{ tableName }}</strong>
      </div>
    </template>
    <div class="sql-wrapper">
      <sql-highlight :sql="sourceTableSchema[tableName]" />
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
import SqlHighlight from '@/components/common/SqlHighlight.vue';
import { Actions } from '@/components/AutoEL/store';
import { Action, Getter, State } from 'vuex-class';

@Component({
  components: {
    SqlHighlight,
  },
})
export default class SchemaDialog extends Vue {
  @Prop() show: boolean;
  @Prop() tableName: string;
  @Action(Actions.GetSourceTableSchema) getSourceTableSchema;
  @State(state => state.AutoEL.sourceTableSchema) sourceTableSchema;
  @Getter('$globalLoading') loading;

  @Watch('show')
  loadData(show) {
    const { getSourceTableSchema, sourceTableSchema, tableName } = this;
    if (show) {
      if (!sourceTableSchema[tableName]) {
        getSourceTableSchema(tableName);

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