import { Vue, Component } from 'vue-property-decorator';
import { IDashboardFile } from '@/types/workspace';
import _ from 'lodash';
import { WorkspaceSrv } from '@/services/Workspace.service';
import { IWorkspace } from '@/types/workspace';
import { WorkspaceManager } from '@/types/workspace/workspace-manager';
@Component
export default class DashboardMixin extends Vue {
  get dashboards(): Dict<IDashboardFile> {
    return this.$store.getters.dashboardFiles;
  }
  get dashboardList(): IDashboardFile[] {
    return _.chain(this.dashboards)
      .map(db => db)
      .value();
  }
  openDashboard(db: IDashboardFile) {
    const ws: IWorkspace = WorkspaceSrv.dashboardFromFile(db);
    WorkspaceManager.getInstance(this).addActiveTabAndOpen(ws);
  }
}
