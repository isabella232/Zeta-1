import { FaqRes } from '@/types/ace';

import { StoreOptions } from 'vuex';
import * as TYPE from '@/stores/MutationTypes';
// Vue.use(Vuex)
const option: StoreOptions<any> = {
  state: {
    visible: false,
    defaultQuestion: {
      list: []
    },
    faqList: [],
    faqTotal: 0,
    faqScrollTop: 0,
    tags: [],
    enotify: []
  },
  mutations: {
    [TYPE.ACE_VISIBLE](state, visble) {
      state.visible = visble;
    },
    [TYPE.SET_ACE_QUESTION](state, question) {
      state.defaultQuestion.list = [];
      state.defaultQuestion.list.push(question);
    },
    [TYPE.FAQ_INIT](state, {list, totalCount}) {
      state.faqList = list;
      state.faqTotal = totalCount;
    },
    [TYPE.UPDATE_FAQ](state, question){
      state.faqList.map((item: FaqRes) =>{
        if(item.id === question.id){
          Object.assign(item, question);
        }
      });
    },
    [TYPE.ADD_FAQ](state, question){
      state.faqList.splice(0,0,question);
      state.faqTotal++;
    },
    [TYPE.DELETE_FAQ](state, qid){
      state.faqList = state.faqList.filter((q: any) => (q.id !== qid));
      state.faqTotal--;
    },
    [TYPE.FAQ_SCROLLTOP](state, scrollTop) {
      state.faqScrollTop = scrollTop;
    },
    [TYPE.INIT_TAGS](state, list) {
      state.tags = list;
    },
    [TYPE.ADD_TAG](state, tag) {
      state.tags.unshift(tag);
    },
  },
  getters: {
    defaultQuestion: state => state.defaultQuestion.list,
    enotify: state => state.enotify,
  },
  actions: {
    setAceQuestion({ commit }, question) {
      commit(TYPE.SET_ACE_QUESTION, question);
    },
    setAceVisible({ commit }, visible) {
      commit(TYPE.ACE_VISIBLE, visible);
    },
    initFAQ({ commit }, { list, totalCount }){
      commit(TYPE.FAQ_INIT, {list, totalCount});
    },
    updateFAQ({ commit }, question){
      commit(TYPE.UPDATE_FAQ, question);
    },
    addFAQ({ commit }, question){
      commit(TYPE.ADD_FAQ, question);
    },
    deleteFAQ({ commit }, qid){
      commit(TYPE.DELETE_FAQ, qid);
    },
    setFaqScrollTop({ commit }, scrollTop){
      commit(TYPE.FAQ_SCROLLTOP, scrollTop);
    },
    initTags({ commit }, list){
      commit(TYPE.INIT_TAGS, list);
    },
    addTags({ commit }, tag){
      commit(TYPE.ADD_TAG, tag);
    }
  }
};
export default option;
