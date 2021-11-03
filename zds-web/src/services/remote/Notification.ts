import config from '@/config/config';
import { Notification } from '@/types/ace';
import DssAPI from './dss-api-remote-service';

const baseURL = config.zeta.notification;
const queue = (process.env.VUE_APP_ENV === 'production') ? 'production' : 'staging';
export default class NotificationRemoteService extends DssAPI {

  getNotifications () {
    return this.get(`${baseURL}noti?queue=${queue}`);
  }
  getNotificationByIds (ids: string){
    return this.get(`${baseURL}noti?queue=${queue}&id=${ids}`);
  }
  createNotification (params: Notification){
    return this.post(`${baseURL}noti?queue=${queue}`, params);
  }
  updateNotification (params: Notification){
    return this.put(`${baseURL}noti?queue=${queue}`, params);
  }
  deleteNotification (id: string){
    return this.delete(`${baseURL}noti/${id}?queue=${queue}`);
  }
  //publish
  publishNotification (id: string){
    return this.post(`${baseURL}pub-noti/${id}?queue=${queue}`);
  }
  deletePublishNotification (id: string){
    return this.delete(`${baseURL}pub-noti/${id}?queue=${queue}`);
  }
  // user
  getUserNotification (nt: string){
    return this.get(`${baseURL}user/${nt}/noti?queue=${queue}`);
  }
  getAckedUserNotification (nt: string){
    return this.get(`${baseURL}user/${nt}/noti-ack?queue=${queue}`);
  }
  ackUserNotification (nt: string, ids: string){
    return this.put(`${baseURL}user/${nt}/noti-ack?id=${ids}&queue=${queue}`);
  }

}
