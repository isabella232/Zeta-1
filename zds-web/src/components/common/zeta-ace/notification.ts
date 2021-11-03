import Vue from 'vue';
import { Store } from 'vuex';
import _ from 'lodash';
import Util from '@/services/Util.service';
import { ZetaException } from '@/types/exception/exception';
import NotificationRemoteService from '@/services/remote/Notification';
import { Notification } from '@/types/ace';
import moment from 'moment';

export default class NotificationService {
  notificationRemoteService: NotificationRemoteService;
  $store: Store<any>;
  $app: Vue;
  get notification(): Notification[] {
    return this.$store.getters.notification.list;
  }
  get nt(){
    return Util.getNt();
  }
  get readMsgList(){
    return this.$store.getters.notification.ackList;
  }
  get tokenValid(){
    return this.$store.getters.user.tokenValid;
  }
  constructor(app: Vue) {
    this.$store = app.$store;
    this.notificationRemoteService = new NotificationRemoteService();
  }
  readMsg(ids: string[]) {
    this.notificationRemoteService.ackUserNotification(this.nt, ids.join(',')).then(() => {
      _.forEach(ids,(id)=>{
        this.$store.dispatch('addAckNotification', id);
      });
    }).catch((e: ZetaException) =>{
      // ignore error message
      e.resolve();
    });
  }
  init() {
    this.notificationRemoteService.getAckedUserNotification(this.nt)
      .then((res)=>{
        this.$store.dispatch('setAckNotification', res.data);
        return this.queryNotification();
      })
      .catch((e: ZetaException)=>{
        /** ignore error message */
        e.resolve();
        console.error('Fetch notification error', e);
      });
    // set task
    this.setupTask();
  }
  queryNotificationByIds(ids: string[]){
    return this.notificationRemoteService.getNotificationByIds(ids.join(',')).then(res =>{
      const data = res.data.sort((a, b) => moment(a.createAt).valueOf()-moment(b.createAt).valueOf());
      this.$store.dispatch('setNotification', data);
    }).catch((e: ZetaException)=>{
      // ignore error message
      e.resolve();
      console.error(e);
    });
  }
  queryNotification() {
    return this.notificationRemoteService.getUserNotification(this.nt).then(res => {
      // this.$store.dispatch('setPublishNotification', res.data);
      const notiIds = _.chain(res.data).map(n => n.id).value();
      // const ids = _.difference(notiIds, this.readMsgList);
      // const notiIds = _.chain(this.notification).map(n => n.id).value();
      // const newIds = _.difference(ids, notiIds); //add noti
      // const rmIds = _.difference(notiIds, ids); // del noti
      // if(rmIds.length>0){
      //   this.$store.dispatch('delNotification', rmIds);
      // }
      if(notiIds.length>0){
        this.queryNotificationByIds(notiIds);
      }
    }).catch((e: ZetaException)=>{
      e.resolve();
      console.error('Fetch notification error', e);
    });
  }
  setupTask() {
    const timer = window.setInterval(() => {
      if(!this.tokenValid){
        clearInterval(timer);
        return;
      }
      this.queryNotification();
    }, 5 * 60 * 1000);
  }


}
