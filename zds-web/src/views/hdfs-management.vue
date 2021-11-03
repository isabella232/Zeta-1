<template>
  <div class="hdfs-container">
    <HDFSList />
  </div>
</template>

<script lang="ts">
import { Component, Vue, Provide } from 'vue-property-decorator';
import HDFSList from '@/components/HDFS/hdfsList.vue';
import HDFSRemoteService from '@/services/remote/HDFSRemoteService';
@Component({
  components: {
    HDFSList
  }
})
export default class Repository extends Vue {
  @Provide('HDFSRemoteService')
  HDFSRemoteService = new HDFSRemoteService();

  constructor() {
    super();
  }
}
</script>
<style lang="scss" scoped>
@import "@/styles/global.scss";
$bc-height: $workspace-tab-height + $workspace-tab-margin-bottom;
.hdfs-container {
  height: calc(100% - #{$bc-height});
}
</style>
