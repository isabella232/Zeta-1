import { Vue, Component, Watch } from 'vue-property-decorator';
import ZetaAceRemoteService from '@/services/remote/ZetaAce.ts';
import NotificationService from './notification';
import _ from 'lodash';
import Util from '@/services/Util.service';
import { allPlatform, allPlatformLowercase} from './utilties';
import { WorkSpaceType } from '@/types/workspace';
import { ZetaException } from '@/types/exception';
import { EnotifyProduct } from '@/types/ace';

@Component
export default class ACEMixin extends Vue {
  zetaAceRemoteService = new ZetaAceRemoteService();
  notificationService: NotificationService;
  constructor() {
    super();
  }
  mounted () {
    // new NotificationService(this).init();
    this.notificationService = new NotificationService(this);
    this.notificationService.init();
  }
  get user() {
    return Util.getNt();
  }
  get tags(){
    return this.$store.state.zetaAceStore.tags;
  }
  async googleSearch(query: string) {
    try {
      const res = await this.zetaAceRemoteService.googleSearch(query);
      return res.data.results;
    } catch (err) {
      console.error(err);
      return [];
    }
  }
  async getQuestion(qid: number) {
    try {
      const res = await this.zetaAceRemoteService.getQuestion(qid);
      return res.data;
    } catch (err) {
      console.error(err);
      return {};
    }
  }
  async getTagsByQuestions(faq: Array<any>){
    const qids = _.chain(faq).map(q => {return q.questionId; }).value().toString();
    const result = await this.zetaAceRemoteService.getTagsByQids(qids);
    const tags = result.data || {};
    faq.map((item: any)=>{
      item.tags = tags[item.questionId]?tags[item.questionId]:[];
    });
    return faq;
  }
  handleSearchResult(data: any, keyWords: any, hint?: boolean) {
    let faq = data.faq?data.faq.slice(0,10):[];
    let table = data.table?data.table.slice(0,10):[];
    let udf = data.udf?data.udf.slice(0,10):[];
    let notebook = data.query?data.query.slice(0,10):[];
    const arr: Dict<any> = {};
    faq = _.chain(faq).map((item: any) => {
      const obj = _.assign(item, {
        searchType:'FAQ',
        name: item.title,
        nameHTML: hint ? this.replace(item.title, keyWords) : item.title
      });
      if (item.title) {
        return obj;
      }
    }).value();
    table = _.chain(table).map((item: any) => {
      const obj = _.assign(item, {
        searchType: 'Table',
        name: _.toUpper(item.name), 
        nameHTML: hint ? this.replace(item.name, keyWords) : item.name, 
        platform: _.intersection(allPlatformLowercase, item.platform.toLowerCase().split(',')),
        desc: this.stripHtmlTags(item.desc || ''),
        rowType: item.rowType
      });
      return obj;
    }).value();
    udf = _.chain(udf).map((item: any) => {
      const obj = _.assign(item, {
        searchType: 'UDF',
        name: _.toUpper(item.name),
        nameHTML: hint ? this.replace(item.name, keyWords) : item.name,
        desc: this.stripHtmlTags(item.description || '')
      });
      return obj;
    }).value();
    notebook = _.chain(notebook).map((item: any) => {
      const obj = _.assign(item, {
        searchType: 'Notebook',
        name: item.title,
        nameHTML: hint ? this.replace(item.title, keyWords) : item.title,
        desc: this.stripHtmlTags(item.desc || '')
      });
      return obj;
    }).value();
    arr.faq = faq;
    arr.table = table;
    arr.udf = udf;
    arr.notebook = notebook;
    const obj1 = [{
      searchType: 'SparkSQL',
      name: keyWords,
      nameHTML: 'Grammer - ' + this.replace(keyWords, keyWords)
    }];
    const obj2 = [{
      searchType: 'New',
      name: keyWords,
      nameHTML: 'New Question - ' + this.replace(keyWords, keyWords)
    }];
    arr.sql = obj1;
    arr.newQ = obj2;
    return arr;
  }
  stripHtmlTags(origin: string): string {
    return origin.replace(/(<([^>]+)>)/gi, '');
  }
  replace(item: string, keyWords: string) {
    const reg = new RegExp('(' + keyWords + ')', 'gi');
    return item.replace(reg, '<span class="highlight">$1</span>');
  }
  get visible() {
    return this.$store.state.zetaAceStore.visible;
  }
  move(e: MouseEvent) {
    this.$data.isDrag = false;
    const parentNode = document.getElementsByClassName(
      'zeta-ace-wrapper'
    )[0] as HTMLElement;
    const aceMainNode = document.getElementsByClassName(
      'zeta-ace-main'
    )[0] as HTMLElement;
    const disX = e.clientX - parentNode.offsetLeft;
    const disY = e.clientY - parentNode.offsetTop;
    const originClientX = e.clientX;
    const originClientY = e.clientY;
    const maxLeft = document.documentElement.clientWidth - parentNode.offsetWidth;
    const maxTop =
      document.documentElement.clientHeight - parentNode.offsetHeight;
    document.onmousemove = e => {
      let left = e.clientX - disX;
      let top = e.clientY - disY;
      // (mainWidth+leftnav) mainHeight-50
      const aceWidth = aceMainNode.offsetWidth + 70 + 10;
      const aceHeight = aceMainNode.offsetHeight - 50;
      const minLeft = this.visible ? (this.$data.fullScreen?480:aceWidth) : 70;
      const minTop = this.visible ? (this.$data.fullScreen?250:aceHeight) : 0;
      if (left <= minLeft) {
        left = minLeft;
      } else if (left >= maxLeft) {
        left = maxLeft;
      }

      if (top <= minTop) {
        top = minTop;
      } else if (top >= maxTop) {
        top = maxTop;
      }

      parentNode.style.left = left + 'px';
      parentNode.style.top = top + 'px';
      if (originClientX !== e.clientX || originClientY !== e.clientY) {
        this.$data.isDrag = true;
      }
    };
    document.onmouseup = e => {
      document.onmousemove = null;
      document.onmouseup = null;
    };
  }

  onMousedown(e: MouseEvent){
    this.$data.dragging = true;
    this.$data.lastClientY = e.clientY;
    this.$data.lastClientX = e.clientX;
    const parentNode = document.getElementsByClassName(
      'zeta-ace-wrapper'
    )[0] as HTMLElement;
    (document as any).onselectstart = function() { return false; };
    document.onmousemove = e => {
      if(!this.$data.dragging) return;
      e.stopPropagation();
      const dy = e.clientY - this.$data.lastClientY;
      const dx = e.clientX - this.$data.lastClientX;
      const maxWidth = parentNode.offsetLeft - 80;
      const maxHeight = parentNode.offsetTop;
      const minWidth = 400;
      const minHeight = 300;
      let h = this.$data.height - dy;
      let w = this.$data.width - dx; 
      if (w <= minWidth) {
        w = minWidth;
      } else if (w >= maxWidth) {
        w = maxWidth;
      }

      if (h <= minHeight) {
        h = minHeight;
      } else if (h >= maxHeight) {
        h = maxHeight;
      }
      this.$data.height = h;
      this.$data.width = w;
      this.$data.lastClientY = e.clientY;
      this.$data.lastClientX = e.clientX;

      document.onmouseup = e => {
        this.$data.dragging = false;
        (document as any).onselectstart = null;
      };
    };
  }

  handleFullscreen(flag: number){
    if(flag){
      const maxHeight = window.innerHeight - 100;
      const maxWidth = window.innerWidth - 200;
      this.$data.fullScreen = false;
      this.$data.height = 650 > maxHeight? maxHeight: 650;
      this.$data.width =  850 > maxWidth ? maxWidth : 850; 
    }else{
      this.$data.fullScreen = true;
      this.$data.height = 300;
      this.$data.width = 400; 
    }
  }

  // watch notification
  get notification(){
    return this.$store.getters.notification.list;
  }
  get ackList(){
    return this.$store.getters.notification.ackList;
  }
  get filterNotification(){
    const arr: any[] = [];
    _.forEach(this.notification, (n)=>{
      if( this.ackList.indexOf(n.id) < 0 && 
        (
          n.type !== 'enotify' || 
        (
          n.type === 'enotify' && (n.body.product === EnotifyProduct[this.currentConnectionStatus.currentConnectionAlias] || n.body.product === 'Zeta')
        )
        )
      ){
        arr.push(n);
      }
    });
    return arr;
  }
  get isNotebook(): boolean {
    const nid = this.$store.getters.activeWorkspaceId;
    const nb = this.$store.state.workspace.workspaces[nid];

    return nb ? (nb.type == WorkSpaceType.NOTEBOOK || nb.type == WorkSpaceType.NOTEBOOK_COLLECTION) : false;
  }
  get currentConnectionStatus() {
    const current = {
      currentConnectionAlias: this.$store.getters.currentConnection?this.$store.getters.currentConnection.alias: '',
      currentRouteName: this.$route.name
    };
    return current;
  }
  @Watch('currentConnectionStatus', { deep: true})
  handleConnectionChange(val: any, oldVal: any) {
    // console.log('currentConnectionStatus',val, oldVal);
    const currentRoute = val.currentRouteName;
    const oldRoute = oldVal?oldVal.currentRouteName: '';
    const oldAlias = oldVal?oldVal.currentConnectionAlias: '';
    const alias = val.currentConnectionAlias;
    if(val.currentRouteName !== 'Workspace' || ( (alias == oldAlias) && ( currentRoute == oldRoute)) ){
      return;
    }
    if (!alias || !this.isNotebook) return;
    console.log('change platform');
    this.$data.changePlatform = true;
    this.notificationService.queryNotification().catch((err: ZetaException)=>{
      err.resolve();
      console.error('Fatch notification error', err);
    });
  }
  readNotify(ids: string[]) {
    this.notificationService.readMsg(ids);
  }
}
