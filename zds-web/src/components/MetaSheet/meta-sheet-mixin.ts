import { Component, Vue, Provide } from 'vue-property-decorator';
import { MetaSheetTableRow, MetaSheetConfig, Mode, MetaSheetTableItem, MODE, ACCESS, MSG_TYPE } from '@/types/meta-sheet';
import MetaSheetService from '@/services/remote/MetaSheetService';
import DMRemoteService from '@/services/remote/DataMove';
import MetaSheetUtil from './util';
import _ from 'lodash';
import Util from '@/services/Util.service';
@Component
export default class MetaSheetMixin extends Vue {
  @Provide('metaSheetService') metaSheetService = new MetaSheetService();
  @Provide('dmRemoteService') dmRemoteService = new DMRemoteService();
  mode: Mode = MODE.VIEW;
  title = '';
  // tableData: Array<MetaSheetTableRow> = [];
  rawTableData: Array<MetaSheetTableRow> = [];
  metaSheetConfig: MetaSheetConfig | {} = {};
  flag = {
    showMetaConfigTable: false,
    closeOnClickModal: false,
    hisLoading: false,
    metaSheetLoading: false,
    metaSheetSyncing: false,
    popoverShow: false,
    clickSearchBtn: false,
    editable: false,
    syncClicked: false,
    showSyncTime: false,
    moveToFolder: false
  };

  constructor() {
    super();
  }

  get isViewMode() {
    return this.mode == MODE.VIEW;
  }

  get nt(): string {
    return Util.getNt();
  }

  async getMetaSheets() {
    this.flag.hisLoading = true;
    let metasheets: Array<MetaSheetTableItem> = [];
    let response = await this.metaSheetService.getMetaSheets();
    if (response.status == 200 && _.isArray(response.data)) {
      metasheets = response.data;
    } else {
      MetaSheetUtil.showMessage(this, MSG_TYPE.ERROR, response.data);
    }
    let processedMetaSheets = MetaSheetUtil.processMetaTableItem(metasheets);
    this.$store.dispatch('initMetasheet', { metasheets: processedMetaSheets });
    this.flag.hisLoading = false;
  }

  cancel() {
    this.flag.showMetaConfigTable = false;
  }

  rowMode(metaSheetRow: MetaSheetTableRow) {
    if (metaSheetRow) {
      let access = metaSheetRow.access;
      return access == ACCESS.OWNER || access == ACCESS.CREATOR ? MODE.EDIT : MODE.VIEW;
    }
    return MODE.VIEW;
  }
}
