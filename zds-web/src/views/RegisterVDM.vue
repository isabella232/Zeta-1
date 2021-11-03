<template>
  <div class="register-container">
    <MetadataRegisterVDM v-loading="loading" />
  </div>
</template>

<script lang="ts">
import { Component, Vue, Provide } from 'vue-property-decorator';
import MetadataRegisterVDM from '@/components/WorkSpace/Metadata/MetadataRegisterVDM.vue';
import DoeRemoteService from '@/services/remote/DoeRemoteService';
import MetadataService from '@/services/Metadata.service';
import UserInfoRemoteService from '@/services/remote/UserInfo';
import { ZetaExceptionProps } from '@/types/exception';

@Component({
  components: {
    MetadataRegisterVDM
  }
})
export default class RegisterVDM extends Vue {
  @Provide('doeRemoteService')
  doeRemoteService: DoeRemoteService = new DoeRemoteService();

  @Provide('metadataService')
  metadataService: MetadataService = new MetadataService();

  @Provide('userInfoRemoteService')
  userInfoRemoteService: UserInfoRemoteService = new UserInfoRemoteService();

  mounted() {
    const props: ZetaExceptionProps = {
      path: 'registervdm'
    };
    this.doeRemoteService.props(props);
    this.userInfoRemoteService.props(props);
  }

  get loading(): boolean {
    return false;
    //return this.$store.state.repository.loading;
  }

  constructor() {
    super();
  }
}
</script>
<style lang="scss" scoped>
@import '@/styles/global.scss';
$bc-height:$workspace-tab-height+ $workspace-tab-margin-bottom;
.register-container{
    height: calc( 100% - #{$bc-height});
    padding-top: 30px;
}
</style>
