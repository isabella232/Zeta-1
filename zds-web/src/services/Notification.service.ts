import Vue from 'vue'
import { Store } from 'vuex'
import { ElNotification, ElNotificationComponent } from '../../node_modules/element-ui/types/notification';
import _ from "lodash"
interface Notification {
  id: number
  title: string
  content: string
  [key: string]: any
}

export default class NotificationService {
  $store: Store<any>
  $app: Vue
  notifyPopupMap: Map<number, ElNotificationComponent>
  readMsgList: number[]
  get notification(): Notification[] {
    // return this.$store.getters.notification.list.filter()
    return _.chain(this.$store.getters.notification.list)
      .filter((notify: Notification) => this.readMsgList.indexOf(notify.id) < 0)
      .value()
  }
  get loading(): boolean {
    return this.$store.getters.notification.loading
  }

  constructor(store: Store<any>, app: Vue) {
    this.$store = store
    this.$app = app;
    this.notifyPopupMap = new Map<number, ElNotificationComponent>()
    this.readMsgList = this.getReadMsg();

  }
  getReadMsg() {
    let readMsgList: number[] = []
    try {
      let readMsgStr: string = localStorage.getItem("ReadMsg") || ""
      let tmp_readMsgList: number[] = JSON.parse(readMsgStr)
      readMsgList = tmp_readMsgList;
    }
    catch (e) {
      console.log("Notification", "did not find in localstorage")
    }
    return readMsgList

  }
  setReadMsg(readMsgList: number[]) {
    let readMsgStr = JSON.stringify(readMsgList)
    localStorage.setItem("ReadMsg", readMsgStr)
  }
  readMsg(id: number) {
    this.readMsgList.push(id);
    this.setReadMsg(this.readMsgList);
  }
  init() {
    this.queryNotification()
    this.setupTask()
  }
  onRefresh() {
    let newMap = new Map<number, ElNotificationComponent>()
    _.chain(this.notification).forEach((notify: Notification, index: number) => {
      let id = notify.id;
      if (this.notifyPopupMap.has(id)) {
        let popupInstance = this.notifyPopupMap.get(id) as any
        if (popupInstance && !popupInstance.closed == false) {
          newMap.set(id, popupInstance)
          this.notifyPopupMap.delete(id)
        }
        else {
          this.notifyPopupMap.delete(id)
          this.readMsgList.push(id)
        }
      }
      else {
        setTimeout(() => {
          let popupInstance = this.$app.$notify({
            title: notify.title,
            message: notify.content,
            offset: 100,
            type: "warning",
            duration: 0,
            onClose: () => {
              this.readMsg(notify.id)
            }
          });
          console.log("popupInstance", popupInstance)
          newMap.set(id, popupInstance)
        }, 10)
      }
    }).value()

    /** clear expire popup */
    this.notifyPopupMap.forEach((popupInstance: ElNotificationComponent) => {
      popupInstance.close()
    })
    this.notifyPopupMap.clear()
    this.setReadMsg(this.readMsgList);
    return this.notifyPopupMap = newMap;
  }
  queryNotification() {

    return this.$store.dispatch("getZetaNotification").then(() => {
      return this.onRefresh()
    })

  }
  setupTask() {
    window.setInterval(() => {
      this.queryNotification()
    }, 5 * 60 * 1000)
  }


}