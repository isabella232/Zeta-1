<template>
  <div
    v-loading="loading"
    class="dashboard-shared"
  >
    <template v-if="dashboardId">
      <Dashboard
        :dashboard-id="dashboardId"
        :data="dashboard"
        class="dashboard"
      />
    </template>
    <template v-else>
      Cannot find Id!
    </template>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Watch } from 'vue-property-decorator';
import { Dashboard, IWorkspace, IDashboardFile, LayoutConfig } from '@/types/workspace';
import { WorkspaceSrv } from '@/services/Workspace.service';
import DashboardComponent from '@/components/WorkSpace/dashboard/dashboard.vue';
import DashboardRemoteService from '@/services/remote/Dashboard';
@Component({
  components: {
    Dashboard: DashboardComponent
  },
})
export default class NotebookSharedView extends Vue {
  dashboardRemoteService: DashboardRemoteService = new DashboardRemoteService();

  dashboardId = '';
  dashboard: Partial<Dashboard> = {} as Dashboard;
  loading = false;
  mounted() {
    this.setParams(this.$route.query);
  }
  setParams(query: any){
    this.dashboardId = query.dashboardId;
    this.load(this.dashboardId);
  }
  @Watch('$route.query')
  routeQueryChange(newVal: any){
    this.setParams(newVal);
  }
  load(id: string){
    this.loading = true;
    this.dashboardRemoteService.getSharedDashboard(id).then(res => {
      const db = res.data;
      this.loading = false;
      this.$breadcrumbs.addSubPath(db.name);
      const dbDict: Dict<IDashboardFile> = {};
      dbDict[db.id] = {
        id: db.id,
        nt: db.nt,
        name: db.name,
        layoutConfigs: db.layoutConfig
          ? (JSON.parse(db.layoutConfig) as LayoutConfig[])
          : [],
        createTime: db.createDt,
        updateTime: db.updateDt ? db.updateDt : null
      } as IDashboardFile;
      const workSpace: IWorkspace = WorkspaceSrv.dashboardFromFile(dbDict[db.id]);
      this.dashboard = workSpace;
    }).catch(() => {
      this.loading = false;
      this.dashboardId = '';
    });
  }
}
</script>
<style lang="scss" scoped>
.dashboard{
    height: 100%;
}
</style>

