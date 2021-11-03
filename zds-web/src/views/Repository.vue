<template>
  <div class="repo-container">
    <repo-list v-loading="loading" />
  </div>
</template>

<script lang="ts">
import { Component, Vue, Provide } from 'vue-property-decorator';
import RepoList from '@/components/Repository/RepoList.vue';
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import DashboardRemoteService from '@/services/remote/Dashboard';
import HDFSRemoteService from '@/services/remote/HDFSRemoteService';
import GitRemoteService from '@/services/remote/GithubService';
import {
  ZetaExceptionProps
} from '@/types/exception';
import ZeppelinApi from '@/services/remote/ZeppelinApi';
@Component({
  components: {
    RepoList
  }
})
export default class Repository extends Vue {
  @Provide('notebookRemoteService')
  notebookRemoteService = new NotebookRemoteService();
  @Provide('dashboardRemoteService')
  dashboardRemoteService = new DashboardRemoteService();
  @Provide('gitRemoteService')
  gitRemoteService = new GitRemoteService();
  @Provide('zeppelinApi')
  zeppelinApi = new ZeppelinApi();
  @Provide('HDFSRemoteService')
  HDFSRemoteService = new HDFSRemoteService();
  get loading(): boolean {
    return this.$store.state.repository.loading;
  }

  constructor() {
    super();
  }
  mounted() {
    const props: ZetaExceptionProps = {
      path: 'repository'
    };
    this.notebookRemoteService.props(props);
    this.dashboardRemoteService.props(props);
    this.gitRemoteService.props(props);
    this.zeppelinApi.props(props);
  }
}
</script>
