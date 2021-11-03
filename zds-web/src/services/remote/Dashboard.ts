import config from '@/config/config';
import DssAPI from '@/services/remote/dss-api-remote-service';
import { PlotConfig } from '@/components/common/pivot-table';
import _ from 'lodash';
export default class DashboardRemoteService extends DssAPI {
  updatePlottingConfigs (cfgs: Dict<PlotConfig | undefined>) {
    // const cfgString = JSON.stringify(cfg);
    const queries = _.chain(cfgs)
      .map((cfg: PlotConfig | undefined, id: string) => {
        return this.post(
          `${config.zeta.base}statements/plotconfig/update`,
          {
            id: parseInt(id),
            plotConfig: JSON.stringify(cfg)
          }
        );
      })
      .value();
    return Promise.all(queries);
  }
  updatePlottingConfig (id: number, cfg: PlotConfig | undefined) {
    const cfgString = JSON.stringify(cfg);
    return this.post(
      `${config.zeta.base}statements/plotconfig/update`,
      {
        id: id,
        plotConfig: cfgString
      }
    );
  }
  getAll () {
    return this.get(`${config.zeta.base}dashboard`);
  }
  getDashboard (nId: string){
    return this.get(`${config.zeta.base}dashboard/${nId}`);
  }
  createDashboard (name: string) {
    return this.post(`${config.zeta.base}dashboard`, {
      name
    });
  }
  updateDashboard (id: string, layout: string) {
    return this.put(`${config.zeta.base}dashboard/${id}/layout`, {
      layoutConfig: layout
    });
  }
  getQueryInfoByIds (ids: number[]) {
    return this.get(
      `${config.zeta.base}statements/multiget/${ids.join(',')}`
    );
  }

  // ================== share ====================
  getSharedDashboard (nId: string){
    return this.get(`${config.zeta.base}share/dashboard/${nId}`, undefined, undefined, true);
  }
  getShareStatementByIds = (id: number[]) => {
    return this.get(`${config.zeta.base}share/statements/multiget/${id.join(',')}`, undefined, undefined, true);
  };
}
