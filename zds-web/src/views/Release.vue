<template>
    <div class="rele-container">
        <release-step/>
    </div>
</template>

<script lang="ts">
import { Component, Vue, Provide } from 'vue-property-decorator';
import ReleaseStep from '@/components/Release/ReleaseStep.vue'
import GitRemoteService from '@/services/remote/GithubService'
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import JIRARemoteService from "@/services/remote/JIRARemoteService";
import GithubService from '@/services/remote/GithubService';
import { ZetaExceptionProps } from '@/types/exception';
@Component({
    components: {
        ReleaseStep
    }
})
export default class Release extends Vue {
    @Provide('gitRemoteService')
    gitRemoteService: GitRemoteService = new GithubService()

    @Provide()
    notebookRemoteService: NotebookRemoteService = new NotebookRemoteService()

    @Provide('jiraRemoteService')
    jiraRemoteService: JIRARemoteService = new JIRARemoteService();

    created() {
        this.$store.dispatch("setSA", "");
        this.$store.dispatch("setReleaseFile", []);
        this.$store.dispatch("setPushTofile", {});
        this.$store.dispatch("setBranchOptions", []);
        this.$store.dispatch("setGithubPushStatus", false);
        this.$store.dispatch("setReleaseSubmitLoading", false);
        this.$store.dispatch("setValidateJira", {});
        this.$store.dispatch("setJiraStatus", false);
        this.$store.dispatch("setSendManifestValidateStatus", false);
        this.$store.dispatch("setCommitData", []);
        this.$store.dispatch("setRolloutStatus", false);
        this.$store.dispatch("setExecTaskStatus", false);
    }
    mounted() {
        const props: ZetaExceptionProps = {
            path: 'release'
        }   
        this.gitRemoteService.props(props);
        this.notebookRemoteService.props(props);
        this.jiraRemoteService.props(props);
    }
}
</script>
<style lang="scss" scoped>
@import '@/styles/global.scss';
$bc-height:$workspace-tab-height + $workspace-tab-margin-bottom;
.rele-container{
    height: calc( 100% - #{$bc-height}) ;
}
</style>
