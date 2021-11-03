<template>
  <div class="tools-container">
    <data-move />
  </div>
</template>
<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';

import DataMove from '@/components/DataMove/DataMove.vue';
@Component({
  components: { DataMove }
})
export default class DataMoveView extends Vue {

}
</script>
<style lang="scss" scoped>
.settings-container {
  width: 700px;
  margin: 50px auto;
}
.tools-container {
  padding-top: 30px;
  margin-top: 0 !important;
  overflow-y: auto;
}
</style>