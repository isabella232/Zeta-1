<template>
    <div class="rele-container">
        <DAList/>
        <!--MetadataBrowse/-->
    </div>
</template>

<script lang="ts">
import { Component, Vue, Provide } from 'vue-property-decorator';
import DAList from '@/components/DA/DAList.vue';
import DoeRemoteService from "@/services/remote/DoeRemoteService";
import { ZetaExceptionProps } from '@/types/exception';
@Component({
    components: {
        DAList
    }
})
export default class Release extends Vue {
    @Provide('doeRemoteService')
    doeRemoteService: DoeRemoteService = new DoeRemoteService();

    mounted() {
        const props: ZetaExceptionProps = {
            path: 'da'
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
