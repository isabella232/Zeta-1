<template>
  <div class="metadata-shared">
    <div class="readonly-popup">
      <span class="left info">
        <i class="zeta-icon-info" />
        <template> This page is READ ONLY</template>
      </span>
    </div>
    <Metadata
      v-if="name != ''"
      :table-name="name"
      :type="type"
      :entrance="entrance"
      :is-share="isShare"
    /> 
    <template v-else>
      Error with paramaters
    </template>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Provide } from 'vue-property-decorator';
import { Metadata } from '@/components/WorkSpace/Metadata';
import MetadataService from '@/services/Metadata.service';
import UserInfoRemoteService from '@/services/remote/UserInfo';
import DoeRemoteService from '@/services/remote/DoeRemoteService';
import { ZetaExceptionProps } from '@/types/exception';
@Component({
  components: {
    Metadata
  }
})
export default class MetadataSharedView extends Vue {
  @Provide() userInfoRemoteService = new UserInfoRemoteService();
  @Provide() metadataService = new MetadataService();
  @Provide('doeRemoteService') doeRemoteService: DoeRemoteService = new DoeRemoteService();

  entrance = 'share';
  isShare = true;

  mounted() {
    const props: ZetaExceptionProps = {
      path: 'metadata'
    };
    this.userInfoRemoteService.props(props);
    this.metadataService.metedataRemoteService.props(props);
    this.doeRemoteService.props(props);
  }

  get name(): any {
    //return this.$store.state.workspace.workspaces[this.metadataId].name || "";
    //return this.$store.getters.getMetadataSearch.name || "";
    const { tableName } = this.$route.query;
    return tableName || sessionStorage.metadata_table || '';
  }

  get type() {
    const { type } = this.$route.query;
    return type || sessionStorage.metadata_table_type || 'table';
  }
}
</script>
<style lang="scss" scoped>
@import '@/styles/global.scss';
$color: #ebb563;
.readonly-popup {
  background-color: #fdf7e9;
  display: flex;
  justify-content: space-between;
  padding: 5px 10px;
  width: 100%;
  span.info {
    &,
    i {
      color: $color;
    }
  }
}
</style>
