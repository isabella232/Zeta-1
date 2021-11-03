import { StoreOptions } from 'vuex';
import * as TYPE from '@/stores/MutationTypes';
import _ from 'lodash';
import { MetaSheetTableRow } from '@/types/meta-sheet';
import Vue from 'vue';
const option: StoreOptions<any> = {
	state: {
		metasheets: [] as Array<MetaSheetTableRow>
	},
	mutations: {
		[TYPE.MS_INIT_METASHEETS](state, metasheets) {
			state.metasheets = metasheets;
        },
        [TYPE.MS_DELETE_METASHEET](state, id) {
			let metaSheets = _.cloneDeep(state.metasheets);
			_.remove(metaSheets, (d:any) => d.id == id);
			state.metasheets = metaSheets;
		},
		[TYPE.MS_UPDATE_METASHEET_AUTH](state, {id, authInfo}) {
			let metasheet = _.find(state.metasheets, sheet => sheet.id === id);
			if(metasheet) {
				let index = _.findIndex(state.metasheets, (sheet:any) => sheet.id === id);
				if(index !=-1 ) {
					Vue.set(state.metasheets, index, {
						...metasheet,
						authInfo
					})
					// console.log('TYPE.MS_UPDATE_METASHEET_AUTH', JSON.parse(JSON.stringify(state.metasheets)));
				}
			}
		},
		[TYPE.MS_UPDATE_METASHEET](state, metasheetRow:MetaSheetTableRow) {
			let metasheet = _.find(state.metasheets, sheet => sheet.id === metasheetRow.id);
			if(metasheet) {
				let index = _.findIndex(state.metasheets, (sheet:any) => sheet.id === metasheetRow.id);
				if(index !=-1 ) {
					Vue.set(state.metasheets, index, {
						...metasheet,
						...metasheetRow
					})
					console.log('TYPE.MS_UPDATE_METASHEET', JSON.parse(JSON.stringify(state.metasheets[index])));
				}
			}
		}
	},
	getters: {
		metasheets: state => state.metasheets,
		metasheetById: (state) => (sheetId: string):MetaSheetTableRow => {
            return _.find(state.metasheets, sheet => sheet.id === sheetId);
		}
	},
	actions: {
		initMetasheet({ commit }, { metasheets }) {
			commit(TYPE.MS_INIT_METASHEETS, metasheets);
		},
		deleteMetaSheet({ commit }, id ) {
			commit(TYPE.MS_DELETE_METASHEET, id);
		},
		updateMetaSheetUsers({ commit }, {id, authInfo}) {
			commit(TYPE.MS_UPDATE_METASHEET_AUTH, {id, authInfo});
		},
		updateMetaSheet({commit, state}, metasheet: MetaSheetTableRow) {
			commit(TYPE.MS_UPDATE_METASHEET, metasheet);
		},
	}
};
export default option;
