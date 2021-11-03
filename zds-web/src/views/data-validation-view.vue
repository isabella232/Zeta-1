<template>
  <div class="tools-container">
    <data-validation />
  </div>
</template>
<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';

import DataValidation from '@/components/DataValidation/DataValidation.vue';
@Component({
  components: { DataValidation }
})
export default class DataValidationView extends Vue {

}
</script>
<style lang="scss" scoped>
</style>