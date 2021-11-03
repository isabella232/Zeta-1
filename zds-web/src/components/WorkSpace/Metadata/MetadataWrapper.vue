<template>
  <div>
    <Metadata
      v-if="name != ''"
      :table-name="name"
      :type="type"
      :entrance="entrance"
      :is-share="isShare"
      :tab="tab"
    />
  </div>
</template>

<script lang="ts">
import { Component, Prop, Provide } from 'vue-property-decorator';
import { WorkspaceComponentBase } from '@/types/workspace';
import Metadata from './Metadata.vue';
import MetadataService from '@/services/Metadata.service';
import UserInfoRemoteService from '@/services/remote/UserInfo';
import DoeRemoteService from '@/services/remote/DoeRemoteService';
import { ZetaExceptionProps } from '@/types/exception';
@Component({
  components: {
    Metadata,
  },
})
export default class MetadataWrapper extends WorkspaceComponentBase{
  @Provide()
  userInfoRemoteService = new UserInfoRemoteService();
  @Provide()
  metadataService = new MetadataService();
  @Provide('doeRemoteService')
  doeRemoteService: DoeRemoteService = new DoeRemoteService();

  @Prop() metadataId!: string;

  entrance = 'zeta';
  isShare = false;

  mounted () {
    const props: ZetaExceptionProps = {
      path: 'metadata',
    };
    this.userInfoRemoteService.props(props);
    this.metadataService.metedataRemoteService.props(props);
    this.doeRemoteService.props(props);
  }
  get name (): any {
    const { tableName } = this.$route.query;
    return tableName || sessionStorage.metadata_table || '';
  }

  get type () {
    const { type } = this.$route.query;
    return type || sessionStorage.metadata_table_type || 'table';
  }

  get tab () {
    const { tab } = this.$route.query;
    return tab;
  }
}
</script>

<style lang="scss" scoped>
.router-view {
  height: calc( 100% - 30px) !important;
}
</style>

