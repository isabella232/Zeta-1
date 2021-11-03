import Vue from 'vue'
import { StoreOptions } from 'vuex'
import DMRemoteService from '@/services/remote/DataMove'
import moment from "moment"
import * as TYPE from '@/stores/MutationTypes'
import config from '@/config/config'
import _ from 'lodash'

const option: StoreOptions<any> = {
    state: {
        history: {
            list: [],
            lastUpdateTime:null,
            loading: false
        },
        redo: false
    },
    mutations: {
        /**
         * history list
         */
        [TYPE.DM_HISTORY_AJAX_LOADING](state) {
            state.history.list = []
            state.history.loading = true;
        },
        [TYPE.DM_HISTORY_AJAX_FINISH](state, { historyList }:{historyList:any[]}) {
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
        },
        [TYPE.DM_ACTION_REDO](state, redo){
          state.redo = redo;
        }
    },
    getters: {
        dataMove:(state) =>{
            return {
                historyList:state.history.list,
                historyLoading:state.history.loading
            }
        }
        // historyList: (state) => { return state.history.list },
        // historyLoading: (state) => { return state.history.loading }
    },
    actions: {
        setRedo({ commit,state }, redo){
          commit(TYPE.DM_ACTION_REDO, redo);
        },
        setDMHistoryLoading({ commit }, ) {
            commit(TYPE.DM_HISTORY_AJAX_LOADING);
        },
        setDMHistoryFinish({commit}, { historyList}) {
            commit(TYPE.DM_HISTORY_AJAX_FINISH, { historyList});
        },
        // getDMHistoryList({ commit }, { nt }) {
        //     let url = `${config.zeta.base}DataMover/history?nt=${nt}`
        //     commit(TYPE.DM_HISTORY_AJAX_LOADING);
        //     return RemoteService.history(nt).then((res)=>{
        //       if(res && res.data && res.data != null){
        //         let historyList = (res && res.data ) || [];
        //         // sort by createDate
        //         historyList = _.sortBy(historyList, [(o:any) => {return o.dataMoveDetail.createDate} ]).reverse()
        //         commit(TYPE.DM_HISTORY_AJAX_FINISH, { historyList});
      
        //       }
        //     }).catch((e)=>{
        //         commit(TYPE.DM_HISTORY_AJAX_FINISH, { historyList:[] });
        //         throw e
        //     })
        // }
    }
}
export default option;
