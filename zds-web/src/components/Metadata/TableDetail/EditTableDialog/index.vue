<template>
  <el-dialog
    :visible.sync="showSync"
    :title="table"
    :width="activeTab === 'columns' ? '70%' : '50%'"
    destroy-on-close
  >
    <el-tabs v-model="activeTab">
      <el-tab-pane
        label="Overview"
        name="overview"
      >
        <overview-form
          @cancel="close"
          @saved="close"
        />
      </el-tab-pane>
      <el-tab-pane
        label="Columns"
        name="columns"
      >
        <column-form @cancel="close" />
      </el-tab-pane>
      <el-tab-pane
        label="Sample Queries"
        name="sample"
      >
        <sample-query-form @cancel="close" />
      </el-tab-pane>
      <el-tab-pane
        label="Operation Info"
        name="operation"
      >
        <operation-form @cancel="close" />
      </el-tab-pane>
    </el-tabs>
  </el-dialog>
</template>
<script lang="ts">
import { Component, Mixins, PropSync, Prop } from 'vue-property-decorator';
import OverviewForm from './OverviewForm.vue';
import ColumnForm from './ColumnForm.vue';
import SampleQueryForm from './SampleQueryForm.vue';
import OperationForm from './OperationForm.vue';
import { Common } from '../mixins/Common';

@Component({
  components: {
    OverviewForm,
    ColumnForm,
    SampleQueryForm,
    OperationForm,
  },
})
export default class EditTableDialog extends Mixins(Common) {

  @PropSync('show') showSync: boolean;
  @Prop() table: string;

  activeTab: 'overview' | 'columns' | 'sample' | 'operation' = 'overview';

  close () {
    this.showSync = false;
  }
}
</script>
<style lang="scss" scoped>

</style>