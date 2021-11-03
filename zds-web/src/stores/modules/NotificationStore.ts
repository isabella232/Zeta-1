
import { StoreOptions } from 'vuex';
import NotificationRemoteService from '@/services/remote/Notification';
import * as TYPE from '@/stores/MutationTypes';
import { ZetaException } from '@/types/exception';
import _ from 'lodash';
const remoteService = new NotificationRemoteService().props({
  path: 'unknown'
});
// Vue.use(Vuex)
const option: StoreOptions<any> = {
  state: {
    notification: {
      list: [],
      ackList: [],
      publishList: [],
      loading: false
    }
  },
  mutations: {
    [TYPE.NTFY_SET](state, { list }) {
      state.notification.list = list;
    },
    [TYPE.NTFY_PUBLISHLIST](state, { list }) {
      state.notification.publishList = list;
    },
    [TYPE.NTFY_SET_ACK](state, { list }) {
      state.notification.ackList = list;
    },
    [TYPE.NTFY_ADD_ACK](state, { notiId }) {
      state.notification.ackList.push(notiId);
    },
    [TYPE.NTFY_ADD](state, { noti }) {
      const hasNoti = state.notification.list.filter((n: any) => (n.id === noti.id)).length>0;
      if(hasNoti) return;
      state.notification.list.push(noti);
    },
    [TYPE.NTFY_DEL](state, { ids }) {
      state.notification.list = _.chain(state.notification.list).filter(l => ids.indexOf(l.id) < 0).value();
    },
    [TYPE.NTFY_LOADING](state, { loading }) {
      state.notification.loading = loading;
    },
  },
  getters: {
    notification: state => state.notification
  },
  actions: {
    getZetaNotification({ commit }) {
      commit(TYPE.NTFY_LOADING, { loading: true });
      return remoteService.getNotifications().then(({ data }) => {
        commit(TYPE.NTFY_LOADING, { loading: false });
        commit(TYPE.NTFY_SET, { list: data.notifications });
      }).catch((e: ZetaException) => {
        /** ignore error message */
        e.resolve();
        commit(TYPE.NTFY_LOADING, { loading: false });
      });
    },
    setNotification({ commit }, list){
      commit(TYPE.NTFY_SET, { list });
    },
    setPublishNotification({ commit }, list ){
      commit(TYPE.NTFY_PUBLISHLIST, { list });
    },
    addNotification({ commit }, noti ) {
      commit(TYPE.NTFY_ADD, { noti });
    },
    delNotification({ commit }, ids ) {
      commit(TYPE.NTFY_DEL, { ids });
    },
    setAckNotification({ commit }, list ) {
      const ids = _.chain(list).map(l => l.id).value();
      commit(TYPE.NTFY_SET_ACK, { list: ids});
    },
    addAckNotification({ commit }, notiId ) {
      commit(TYPE.NTFY_ADD_ACK, { notiId });
    }
  }
};
export default option;
