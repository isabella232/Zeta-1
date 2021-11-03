import { Vue, Component, Watch } from 'vue-property-decorator';
import ZetaAceRemoteService from '@/services/remote/ZetaAce.ts';
import { WorkSpaceType } from '@/types/workspace';
import moment from 'moment';
import { EnotifyItem } from '@/types/ace';
import { ZetaException } from '@/types/exception';

const ProductsMap = {
  'Ares': 'Ares',
  'Hermes': 'Hermes_RNO',
  'ApolloReno': 'Apollo_RNO',
  'Hercules': 'Hercules',
  'HerculesSub': 'Hercules',
  'Hopper': 'Hopper',
  'Mozart': 'Mozart',
  'NuMozart': 'Mozart',
  'Kylin-rno': 'Kylin',
  'Kylin-rno-qa': 'Kylin',
  'Zeta': 'Zeta'
} as Dict<string>;
@Component
export default class EnotifyMixin extends Vue {
  zetaAceRemoteService = new ZetaAceRemoteService();
  products: string = ProductsMap.Zeta;
  enotify: Array<EnotifyItem> = [];
  enotifyCount = 0;
  timer: any = null;
  constructor() {
    super();
  }
  mounted () {
    if(this.timer) clearInterval(this.timer);
    this.setupTask();
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
  @Watch('currentConnectionStatus', { deep: true, immediate: true })
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
    const products = `${this.products + ',' + ProductsMap[alias]}`;
    const date =  moment().local().subtract(3, 'days').format('YYYY-MM-DD');
    this.getEnotify(products, date);
  }
  getEnotifyProducts() {
    this.zetaAceRemoteService.getEnotifyProducts().then(res => {
      console.log(res);
    });
  }
  getEnotifyTyps() {
    this.zetaAceRemoteService.getEnotifyTypes().then(res => {
      console.log(res);
    });
  }
  getEnotify(product: string, after = '2020-01-01') {
    this.zetaAceRemoteService.getEnotify(product, after).then(res => {
      if (res && res.data && res.data.enotifies) {
        this.enotify = res.data.enotifies.filter((item: EnotifyItem) => item.read !== true);
        this.enotifyCount = this.enotify.length;
      } else {
        this.enotify = [];
        this.enotifyCount = 0;
      }
    }).catch((err: ZetaException) => {
      err.resolve();
      console.log(err);
    });
  }
  readEnotify(ids: Array<number>) {
    this.zetaAceRemoteService.readEnotify(ids).then(res => {
      if (res && res.data && res.data.result) {
        this.enotifyCount = 0;
        this.enotify = [];
      }
    }).catch(err => {
      console.log(err);
    });
  }

  setupTask(){
    let success = true;
    this.timer = window.setInterval(async()=>{
      const currentRoute = this.currentConnectionStatus.currentRouteName;
      const alias = this.currentConnectionStatus.currentConnectionAlias;
      let products = this.products;
      if(currentRoute === 'Workspace' && alias && this.isNotebook){
        products = `${products+','+ProductsMap[alias]}`;
      }
      const date =  moment().local().subtract(3, 'days').format('YYYY-MM-DD');

      if(success){
        this.zetaAceRemoteService.getEnotify(products, date).then(res => {
          if (res && res.data && res.data.enotifies) {
            this.enotify = res.data.enotifies.filter((item: EnotifyItem) => item.read !== true);
            this.enotifyCount = this.enotify.length;
          } else {
            this.enotify = [];
            this.enotifyCount = 0;
          }
        }).catch((e: ZetaException) => {
          e.resolve();
          success = false;
          clearInterval(this.timer);
        });
      }else{
        clearInterval(this.timer);
      }
    }, 5 * 60 * 1000);
  }
}
