<template>
  <div class="browse-container">
    <div class="readonly-popup">
        <span class="left info">
            <i class="zeta-icon-info"/>
            <template> This page is READ ONLY</template>
        </span>
    </div>
    <MetadataBrowse :domain="domain" :subDomain="subDomain" :readOnly="readOnly"/>
</div>
</template>

<script lang="ts">
import { Component, Vue, Provide } from 'vue-property-decorator';
import MetadataBrowse from '@/components/Metadata/MetadataBrowse.vue';
import DoeRemoteService from "@/services/remote/DoeRemoteService";
import MetadataService from "@/services/Metadata.service";
import UserInfoRemoteService from "@/services/remote/UserInfo"
import { ZetaExceptionProps } from '@/types/exception';
@Component({
  components: {
      MetadataBrowse
  },
})
export default class MetadataBrowseSharedView extends Vue {
    @Provide('doeRemoteService')
    doeRemoteService: DoeRemoteService = new DoeRemoteService();
    @Provide('metadataService')
    metadataService: MetadataService = new MetadataService();
    @Provide('userInfoRemoteService')
    userInfoRemoteService: UserInfoRemoteService = new UserInfoRemoteService();
    readOnly: boolean = true;

    get domain(): any {
        return this.$route.query.domain || "";
    }

    get subDomain(): any {
        return this.$route.query.subdomain || "";
    }

    mounted() {
        const props: ZetaExceptionProps = {
            path: 'metadata'
        };
        this.doeRemoteService.props(props);
        this.metadataService.metedataRemoteService.props(props);
        this.userInfoRemoteService.props(props);
    }
}
</script>
<style lang="scss" scoped>
@import '@/styles/global.scss';
$bc-height:$workspace-tab-height + $workspace-tab-margin-bottom;
.browse-container{
    height: calc( 100% - #{$bc-height} - 30px) ;
    overflow-y: hidden;
}
$color:#EBB563;
.readonly-popup{
    position: absolute;
    background-color: #FDF7E9;
    display: flex;
    justify-content: space-between;
    padding: 5px 10px;
    width: calc(100% - 120px);
    span.info{
        &,
        i{
            color: $color;
        }
    }
}
</style>