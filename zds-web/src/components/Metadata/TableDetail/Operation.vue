<template>
  <div
    id="operation"
    class="operation"
  >
    <header class="sticky-header">
      Operation
    </header>
    <operation-info
      v-if="table"
      :table="table.table_name"
    />
  </div>
</template>
<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import OperationInfo from '@/components/WorkSpace/Metadata/sub-view/Operation/index.vue';
import { Getter } from 'vuex-class';
import { Getters } from './store';

@Component({
  components: {
    OperationInfo,
  },
})
export default class Operation extends Vue {
  @Getter(Getters.TableSample) table;

}
</script>
<style lang="scss" scoped>

.operation {
  padding: 0 12px;

  ::v-deep {
    .op-info-wrapper {
      padding: 0 12px;
      > h1 {
        margin-top: 8px;
        font-size: 16px;
      }
      .detail-row label {
        text-align: left;
        padding-right: 8px;
      }
    }
  }
}
</style>