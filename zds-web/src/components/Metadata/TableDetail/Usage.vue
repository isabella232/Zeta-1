<template>
  <div
    id="usage"
    class="usage"
  >
    <header class="sticky-header">
      Usage Persona
    </header>
    <usage :table-id="tableName" />
  </div>
</template>
<script lang="ts">
import { Component, Mixins } from 'vue-property-decorator';
import Usage from '@/components/WorkSpace/Metadata/sub-view/metadata-usage.vue';
import { Common } from './mixins/Common';

@Component({
  components: {
    Usage,
  },
})
export default class UsageWrapper extends Mixins(Common) {

}
</script>
<style lang="scss" scoped>

.usage {
  padding: 24px 0;

}
</style>