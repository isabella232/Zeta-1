import Vue from 'vue';
import { StoreOptions } from 'vuex';
import * as TYPE from '@/stores/MutationTypes';
import _ from 'lodash';
import { IDashboardFile } from '@/types/workspace';
// Vue.use(Vuex)
const option: StoreOptions<any> = {
	state: {
		dashboardFiles: {} as Dict<IDashboardFile>
	},
	mutations: {
		[TYPE.DB_INIT](state, dashboards) {
			state.dashboardFiles = dashboards;
		},
		[TYPE.DB_ADD](state, dashboard) {
			state.dashboardFiles = {
				...state.dashboardFiles,
				[dashboard.id]: dashboard
			};
		},
		[TYPE.DB_UPDATE_INFO](state, { id, layoutConfigs }) {
			state.dashboardFiles[id].layoutConfigs = layoutConfigs;
		}
	},
	getters: {
		dashboardFiles: state => state.dashboardFiles
	},
	actions: {
		initDashboard({ commit }, { dashboards }) {
			commit(TYPE.DB_INIT, dashboards);
		},
		addDashboard({ commit }, { dashboard }) {
			commit(TYPE.DB_ADD, dashboard);
		},
		updateDashboard({ commit }, { id, layoutConfigs }) {
			commit(TYPE.DB_UPDATE_INFO, { id, layoutConfigs });
		}
		// openDashboard({ state, commit }, { id }) {
		// 	const dbFile = state.dashboardFiles[id];
		// 	const statementIds = _.chain(dbFile.layoutConfigs)
		// 		.filter((cfg: LayoutConfig) => cfg.type != 'text')
		// 		.map((cfg: LayoutConfig) => cfg.statementId)
		// 		.value() as number[];
		// 	return DashboardSrv.getQueryInfoByIds(statementIds).then(({data})=>);
		// }
	}
};
export default option;
