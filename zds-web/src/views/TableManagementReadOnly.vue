<template>
  <div class="rele-container">
    <TableManagement :readOnly="readOnly"/>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Provide } from 'vue-property-decorator';
import TableManagement from '@/components/Metadata/TableManagement.vue';
import DoeRemoteService from '@/services/remote/DoeRemoteService';
import { ZetaExceptionProps } from '@/types/exception';
@Component({
  components: {
    TableManagement
  }
})
export default class TableManagementReadOnly extends Vue {
  @Provide('doeRemoteService')
  doeRemoteService: DoeRemoteService = new DoeRemoteService();
  readOnly: boolean = true;

  mounted() {
    const props: ZetaExceptionProps = {
      path: 'metadata'
    };
    this.doeRemoteService.props(props);
  }
}
</script>
<style lang="scss" scoped>
@import '@/styles/global.scss';
$bc-height:$workspace-tab-height + $workspace-tab-margin-bottom;
.rele-container{
    height: calc( 100% - #{$bc-height}) ;
    overflow-y: hidden;
}
</style>
