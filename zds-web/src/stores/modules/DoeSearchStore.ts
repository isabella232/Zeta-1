import Vue from 'vue'
import { StoreOptions } from 'vuex'
import axios from 'axios';
import * as TYPE from '@/stores/MutationTypes'
import config from '@/config/config'
import _ from 'lodash'
import * as Settings from "@/components/Settings/Settings"
// Vue.use(Vuex)
const option: StoreOptions<any> = {
    state: {
        iframe:"",
        keywords:""
    },
    mutations: {
        [TYPE.DOE_SEARCH_TYPEHEAD](state,{src}) {
            state.iframe = src
        },
        [TYPE.DOE_SEARCH_KEYWORDS](state,{keywords}){
            state.keywords = keywords
        }
    },
    getters: {
        iframeURL: state => state.iframe,
        keywords: state => state.keywords
    },
    actions: {
        setIframe({commit},{src}){
            commit(TYPE.DOE_SEARCH_TYPEHEAD,{src});
        },
        setKeywords({commit},{keywords}){
            commit(TYPE.DOE_SEARCH_KEYWORDS,{keywords});
        }
    }
}
export default option;
