<template>
  <div class="browse-container">
    <MetadataBrowse
      :domain="domain"
      :sub-domain="subDomain"
      :read-only="readOnly"
    />
  </div>
</template>

<script lang="ts">
import { Component, Vue, Provide } from 'vue-property-decorator';
import MetadataBrowse from '@/components/Metadata/MetadataBrowse.vue';
import DoeRemoteService from '@/services/remote/DoeRemoteService';
import MetadataService from '@/services/Metadata.service';
import UserInfoRemoteService from '@/services/remote/UserInfo';
import { ZetaExceptionProps } from '@/types/exception';
@Component({
  components: {
    MetadataBrowse,
  },
})
export default class Browse extends Vue {
  
  @Provide('doeRemoteService')
  doeRemoteService: DoeRemoteService = DoeRemoteService.instance;
  @Provide('metadataService')
  metadataService: MetadataService = new MetadataService();
  @Provide('userInfoRemoteService')
  userInfoRemoteService: UserInfoRemoteService = new UserInfoRemoteService();
  readOnly = false;

  get domain (): any {
    return this.$route.query.domain || '';
  }

  get subDomain (): any {
    return this.$route.query.subdomain || '';
  }

  mounted () {
    const props: ZetaExceptionProps = {
      path: 'metadata',
    };
    this.doeRemoteService.props(props);
    this.metadataService.metedataRemoteService.props(props);
    this.userInfoRemoteService.props(props);
  }
}
</script>
<style lang="scss" scoped>
@import '@/styles/global.scss';
.browse-container {
  height: calc( 100% - 30px) ;
  overflow-y: hidden;
}
</style>
