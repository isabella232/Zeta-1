import Vue from 'vue'
import { StoreOptions } from 'vuex'
import moment from "moment"
import * as TYPE from '@/stores/MutationTypes'
import config from '@/config/config'
import _ from 'lodash'
// Vue.use(Vuex)
const option: StoreOptions<any> = {
    state: {
        history: {
            list: [],
            lastUpdateTime:null,
            loading: false
        }
    },
    mutations: {
        /**
         * history list
         */
        [TYPE.DV_HISTORY_AJAX_LOADING](state) {
            state.history.list = []
            state.history.loading = true;
        },
        [TYPE.DV_HISTORY_AJAX_FINISH](state, { historyList }:{historyList:any[]}) {
            if(state.history.lastUpdateTime){
                let udtTime = moment(state.history.lastUpdateTime)
                historyList.map(his =>{
                    let hisCreDt = moment(his.createDate);
                    if(hisCreDt.isAfter(udtTime)){
                        his.isNew = true;
                    }
                })
            }
            state.history.list = historyList
            state.history.loading = false;
            state.history.lastUpdateTime = historyList[0] ? historyList[0].createDate : null
        }
    },
    getters: {
        dataValidation:(state) =>{
            return {
                historyList:state.history.list,
                historyLoading:state.history.loading
            }
        }
        // historyList: (state) => { return state.history.list },
        // historyLoading: (state) => { return state.history.loading }
    },
    actions: {
        setDVHistoryLoading({ commit }, ) {
            commit(TYPE.DV_HISTORY_AJAX_LOADING);
        },
        setDVHistoryFinish({commit}, { historyList}) {
            commit(TYPE.DV_HISTORY_AJAX_FINISH, { historyList});
        },
        // getDVHistoryList({ commit }, { nt }) {
        //     commit(TYPE.DV_HISTORY_AJAX_LOADING);
        //     return RemoteService.history(nt).then((res)=>{
        //       if(res && res.data && res.data != null){
        //         let historyList = (res && res.data ) || [];
        //         // sort by createDate
        //         historyList = _.sortBy(historyList, [(o:any) => {return o.dataValidateDetail.createDate} ]).reverse()
        //         commit(TYPE.DV_HISTORY_AJAX_FINISH, { historyList});

        //       }
        //     }).catch((e)=>{
        //         commit(TYPE.DV_HISTORY_AJAX_FINISH, { historyList:[] });
        //         throw e
        //     })
        // }
    }
}
export default option;
