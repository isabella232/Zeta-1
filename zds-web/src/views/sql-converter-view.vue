<template>
  <div class="tools-container">
    <sql-converter />
  </div>
</template>
<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';

import SqlConverter from '@/components/SQLConverter/SQLConverter.vue';
@Component({
  components: { SqlConverter }
})
export default class SQLConverterView extends Vue {

}
</script>
<style lang="scss" scoped>
.tools-container {
  height: 100%;
  margin-top: -1px;
  margin-bottom: -1px;
  display: flex;
  // justify-content: center;
  // align-items: center;
}
</style>