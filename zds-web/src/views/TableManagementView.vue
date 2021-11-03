<template>
  <div class="rele-container">
    <TableManagement
      :tab-id="tabId"
      :query="query"
    />
  </div>
</template>

<script lang="ts">
import { Component, Vue, Provide } from 'vue-property-decorator';
import TableManagement from '@/components/Metadata/TableManagement.vue';
import DoeRemoteService from '@/services/remote/DoeRemoteService';
import { ZetaExceptionProps } from '@/types/exception';
import _ from 'lodash';
@Component({
  components: {
    TableManagement,
  },
})
export default class TableManagementView extends Vue {
  @Provide('doeRemoteService')
  doeRemoteService: DoeRemoteService = new DoeRemoteService();

  mounted () {
    const props: ZetaExceptionProps = {
      path: 'metadata',
    };
    this.doeRemoteService.props(props);
  }

  get tabId (): any {
    const { tab_id } = this.$route.query;
    return tab_id;
  }

  get query () {
    const query = {};
    _.forEach(this.$route.query, (v: any, k: any) => {
      if (k != 'tab_id') query[k] = JSON.parse(v);
    });
    return query;
  }
}
</script>
<style lang="scss" scoped>
@import '@/styles/global.scss';
$bc-height:$workspace-tab-height + $workspace-tab-margin-bottom;
.rele-container{
    height: calc( 100% - #{$bc-height}) ;
    overflow-y: hidden;
    margin-top: 0;
}
</style>
