<template>
  <div class="metadata">
    <div @click="closeComment($event)">
      <div v-if="getBrowseRoute && !isShare">
        <el-breadcrumb separator-class="el-icon-arrow-right">
          <el-breadcrumb-item>
            <span
              class="breadcrumb-clk"
              @click="cleanStore();back();"
            >Metadata Browse</span>
          </el-breadcrumb-item>
          <el-breadcrumb-item>
            <span
              class="breadcrumb-clk"
              @click="back"
            >{{ getBrowseRoute.domain }}</span>
          </el-breadcrumb-item>
          <el-breadcrumb-item v-if="!getBrowseRoute.isDomain">
            <span
              class="breadcrumb-clk"
              @click="back"
            >{{ getBrowseRoute.name }}</span>
          </el-breadcrumb-item>
          <el-breadcrumb-item>{{ tableName }}</el-breadcrumb-item>
        </el-breadcrumb>
      </div>
      <div v-if="getBrowseSearchRoute && !isShare">
        <span
          class="back-css"
          @click="backSearch(getBrowseSearchRoute);"
        >Return Search Page</span>
      </div>
      <div v-if="getTableManagementRoute && !isShare">
        <span
          class="back-css"
          @click="backTableManagement(getTableManagementRoute);"
        >Return Table Management Page</span>
      </div>
      <div class="title">
        <span class="table-name">{{ tableName }}</span>
        <span
          v-if="isSLA"
          class="sla-icon"
        >SLA/SLE</span>
        <span
          v-if="isIDL"
          class="idl-icon"
        >IDL</span>
        <i
          v-if="!isShare"
          class="icon zeta-icon-export"
          @click="openNewTab"
        />
        <ShareLink
          v-if="!isShare"
          v-model="shareUrl"
          style="position:relative;top:1px;"
        />
        <el-popover
          v-if="existEnotify"
          placement="bottom-start"
          trigger="click"
          width="800"
        >
          <iframe
            :src="enotify.link"
            style="width: 100%; height: 700px; border: 0;"
          />
          <div slot="reference">
            <div class="current-enotify">
              <i class="zeta-icon-notify" /><span class="enotify-content">{{ enotify.enotify_title }}</span>
            </div>
          </div>
        </el-popover>
      </div>
      <el-popover
        placement="bottom-start"
        trigger="hover"
        class="score"
      >
        <div style="margin-bottom: 7px;">
          <span style="font-weight: bolder;">Confidence Score: {{ confidenceScore }}</span>
          <a
            target="_blank"
            href="https://wiki.vip.corp.ebay.com/x/u7QhKw"
            class="zeta-icon-info"
            title="Click to see how confidence score calculated"
          />
        </div>
        <table>
          <tr>
            <th width="120">
              Usage:
            </th>
            <td :title="confidenceDetails.usageToolTip">
              <div
                class="icon-score"
                :class="confidenceDetails.usage"
              />
              <span
                class=""
                :class="confidenceDetails.usage + '-label'"
              >{{ getScoreText(confidenceDetails.usage) }}</span>
            </td>
          </tr>
          <tr>
            <th width="120">
              Availability:
            </th>
            <td :title="confidenceDetails.availabilityToolTip">
              <div
                class="icon-score"
                :class="confidenceDetails.availability"
              />
              <span
                class=""
                :class="confidenceDetails.availability + '-label'"
              >{{ getScoreText(confidenceDetails.availability) }}</span>
            </td>
          </tr>
          <tr>
            <th width="120">
              DQ:
            </th>
            <td :title="confidenceDetails.dqToolTip">
              <div
                class="icon-score"
                :class="confidenceDetails.dq"
              />
              <span
                class=""
                :class="confidenceDetails.dq + '-label'"
              >{{ getScoreText(confidenceDetails.dq) }}</span>
            </td>
          </tr>
          <tr>
            <th width="120">
              Governance:
            </th>
            <td :title="confidenceDetails.governanceToolTip">
              <div
                class="icon-score"
                :class="confidenceDetails.governance"
              />
              <span
                class=""
                :class="confidenceDetails.governance + '-label'"
              >{{ getScoreText(confidenceDetails.governance) }}</span>
            </td>
          </tr>
        </table>
        <div
          v-if="!isLkpTable"
          slot="reference"
          style="display: flex;"
        >
          <span style="color: #ff9900; line-height: 20px; margin-right: 5px;">{{ confidenceScore }}</span>
          <el-rate
            v-model="rate"
            disabled
          />
          <span
            style="color: #cacbcf;"
            title="# of unique users and batch accouts for latest 30 days in all platforms"
            @click="userListVisible = false"
          >(
            <i class="zeta-icon-hot">
              <span style="margin: 0 5px; font-family: 'ArialMT', 'Arial';">{{ usage }}</span>
            </i>)
          </span>
        </div>
      </el-popover>
      <div class="metadata-tools-bar">
        <ul class="metadata-tabs">
          <li
            v-click-metric:METADATA_TABLE_DETAIL="{name: 'overview', table: tableName}"
            :class="{'active':activeTab ==='Overview'}"
            @click="() => activeTab = 'Overview'"
          >
            Overview
          </li>
          <li
            v-click-metric:METADATA_TABLE_DETAIL="{name: 'columns', table: tableName}"
            :class="{'active':activeTab ==='Columns'}"
            @click="() => activeTab = 'Columns'"
          >
            Columns
          </li>
          <li
            v-click-metric:METADATA_TABLE_DETAIL="{name: 'operation', table: tableName}"
            :class="{'active':activeTab ==='Operation'}"
            @click="() => activeTab = 'Operation'"
          >
            Operation Info
          </li>
          <li
            v-if="!isLkpTable"
            v-click-metric:METADATA_TABLE_DETAIL="{name: 'sample', table: tableName}"
            :class="{'active':activeTab ==='Sample'}"
            @click="() => activeTab = 'Sample'"
          >
            Sample Queries
          </li>
          <li
            v-if="!isLkpTable"
            v-click-metric:METADATA_TABLE_DETAIL="{name: 'enotify', table: tableName}"
            :class="{'active':activeTab ==='Enotify'}"
            @click="() => activeTab = 'Enotify'"
          >
            Change History
          </li>
          <li
            v-if="!isLkpTable"
            v-click-metric:METADATA_TABLE_DETAIL="{name: 'usage', table: tableName}"
            :class="{'active':activeTab ==='Usage'}"
            @click="() => activeTab = 'Usage'"
          >
            Usage Persona
          </li>
        </ul>
        <div class="btn-group">
          <template v-if="canEdit">
            <el-button
              v-if="!edit"
              type="primary"
              class="edit-btn"
              @click="edit = true"
            >
              Edit
            </el-button>
            <template v-else>
              <el-button
                type="default"
                plain
                class="cancel-btn"
                @click="revert"
              >
                Cancel
              </el-button>
              <el-button
                type="primary"
                class="save-btn"
                @click="save"
              >
                Save
              </el-button>
            </template>
          </template>
        </div>
      </div>
      <div
        v-loading="loading"
        class="metadata-display"
      >
        <div
          v-if="tableInited && viewInited"
          v-show="activeTab ==='Columns'"
          class="columns"
        >
          <MetadataColumns
            ref="cols"
            :metadata-table-dict="tableDict"
            :metadata-view-dict="viewDict"
            :sample-data-dict="sampleDataDict"
            :edit="edit"
            :loading="loading"
            @edit-model="edit = true"
            @submit-sample-data="addSampleData"
          />
        </div>
        <div
          v-if="tableInited && viewInited"
          v-show="activeTab ==='Overview'"
          class="overview"
        >
          <MetadataSummary 
            ref="smy"
            :metadata-table-dict="tableDict"
            :metadata-view-dict="viewDict"
            :edit="edit"
            :type="type"
            :vdm-all-platform="vdmAllPlatform"
            :labels="tableLabels"
            :restricted-labels="restrictedLabels"
            :unrestricted-labels="unrestrictedLabels"
            @system-change="sys => {system.platform = sys.platform; system.dbName = sys.dbName }"
            @download-usage="download"
          />
        </div> 
        <div
          v-if="tableInited && viewInited && sampleInited && activeTab ==='Sample'"
          class="sample"
        >
          <MetadataSampleQuery 
            ref="spl"
            :data="sampleQueryDict"
            :metadata-table-dict="tableDict"
            :metadata-view-dict="viewDict"
            :smart-query-arr="smartQueryArr"
            :smart-query-table-arr="smartQueryTableArr"
            @submit-sample-query="addSampleQuery"
            @delete-sample-query="deleteSampleQuery"
          />
        </div>
        <div
          v-if="enotifyInited && activeTab ==='Enotify'"
          class="enotify"
        >
          <MetadataEnotify :data="enotifyArr" />
        </div>
        <keep-alive>
          <metadata-usage
            v-if="activeTab ==='Usage'"
            :table-id="tableName"
          />
        </keep-alive>
        <keep-alive>
          <metadata-operation
            v-if="activeTab ==='Operation'"
            :table="tableName"
            :dbs="getAllDatabases"
            :is-share="isShare"
          />
        </keep-alive>
      </div>
      <el-badge
        v-if="activeTab != 'Lineage' && activeTab != 'Usage'"
        :value="commentsVal"
        :max="9999"
        class="comment-icon-div"
        :type="badgeType"
      >
        <el-button
          name="comment-btn"
          icon="zeta-icon-comment-1"
          circle
          @click="openComment"
        />
      </el-badge>
    </div>
    <MetadataComment
      ref="metadataComment"
      :data="commentDict"
      vue-ref="MetaData"
      :loading="commentLoading"
      :scroll-id="getScrollId"
      @submit-comment="addTableComment"
      @delete-comment="deleteTableComment"
    />
    <MetadataColumnsDialog
      :visible.sync="columnsDialogVisible"
      :metadata-table-dict="tableDict"
      :metadata-view-dict="viewDict"
      :data="updateColumns"
      :table-name="tableName"
      @submit-columns="addColDesc"
    />
    <!--el-dialog title="User List" :visible.sync="userListVisible">
			<div class="filter-div" style="display: flex">
				<span class="filter-label">Platform:</span>
				<el-select v-model="userListPlatform" placeholder="Select platform">
				<el-option
					v-for="item in getUserListPltformOptions"
					:key="item"
					:label="item"
					:value="item">
				</el-option>
				</el-select>
			</div>
			<el-table :data="getUserList" max-height="300">
				<el-table-column property="user_name" label="User Name"></el-table-column>
				<el-table-column property="user_mail" label="Mail"></el-table-column>
				<el-table-column property="batch_acct" label="Batch Acct"></el-table-column>
			</el-table>
			<div slot="footer" class="dialog-footer">
				<el-button type="primary" @click="copytext">Copy</el-button>
			</div>
		</el-dialog-->
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Provide, Watch, Inject } from 'vue-property-decorator';
import ShareLink from '@/components/common/share-link';
import MetadataSummary from './sub-view/metadata-summary.vue';
import MetadataColumns from './sub-view/metadata-columns.vue';
import MetadataColumnsDialog from './sub-view/metadata-columns-dialog.vue';
import MetadataComment from './sub-view/metadata-comment.vue';
import MetadataSampleQuery from './sub-view/metadata-sample-query.vue';
import MetadataEnotify from './sub-view/metadata-enotify.vue';
import DoeRemoteService from '@/services/remote/DoeRemoteService';
import MetadataService from '@/services/Metadata.service';
import UserInfoRemoteService from '@/services/remote/UserInfo';
import moment from 'moment';
import Util from '@/services/Util.service';
import copy from 'copy-to-clipboard';
import _ from 'lodash';
import * as Logger from '@/plugins/logs/zds-logger';
import { downloadUsage } from '@/components/Metadata/DownloadUsage.ts';
import { attempt } from '@drewxiu/utils';
import { customZetaException } from '@/types/exception';

@Component({
  components: {
    MetadataSummary,
    MetadataColumns,
    ShareLink,
    MetadataSampleQuery,
    MetadataComment,
    MetadataColumnsDialog,
    MetadataEnotify,
    MetadataUsage: () => import('./sub-view/metadata-usage.vue'),
    MetadataOperation: () => import('./sub-view/Operation/index.vue'),
  },
})
export default class Metadata extends Vue {
  // @Prop() metadataId!: string;
  @Provide('tableName')
  @Prop() tableName!: string;
  @Prop() type!: string;
  @Prop() entrance!: string;
  @Prop() isShare: boolean;
  @Prop() tab: string;

  @Inject() metadataService: MetadataService;
  @Inject('doeRemoteService') doeRemoteService: DoeRemoteService;
  @Inject('userInfoRemoteService') userInfoRemoteService: UserInfoRemoteService;

  edit = false;
  activeTab = 'Overview';
  loading = false;
  tableInited = false;
  viewInited = false;
  sampleInited = false;
  commentLoading = false;
  smartQueryInited = false;
  existEnotify = false;
  enotifyInited = false;
  tableDict: any = [];
  viewDict: any = [];
  sampleQueryDict: any = [];
  sampleDataDict: any = [];
  commentDict: any = { comments: [] };
  system: any = {
    platform: '',
    dbName: '',
  };
  columnsDialogVisible = false;
  updateColumns: any = [];
  commentsVal = 0;
  vdmAllPlatform: Array<any> = [];
  smartQueryArr: any = [];
  smartQueryTableArr: any = [];
  fullName: string = Util.getNt();
  enotify: any = {};
  enotifyArr: any = [];
  tableLabels: any = [];
  restrictedLabels: any = [];
  unrestrictedLabels: any = [];
  rate = 0;
  confidenceScore: any = 0.0;
  confidenceDetails: any = {};
  usage = 0;
  concatArr: Array<any> = [];
  userListPlatform: any = 'All';
  userListVisible = false;

  get canEdit () {
    const { activeTab, isShare } = this;
    return activeTab !== 'Operation' && activeTab != 'Sample' && activeTab != 'Enotify' && activeTab != 'Lineage' && activeTab != 'Usage' && !isShare;
  }
  get iframeURL () {
    const full_name = (this.system.dbName.toUpperCase() || 'DEFAULT') + '.' + this.tableName.toUpperCase();
    const platform = this.system.platform;
    if (full_name && platform) {
      return `https://doe.corp.ebay.com/lineage/?method=Direct&dbtable=${full_name}&working`;
    } else {
      return '';
    }
  }

  get shareUrl () {
    return (
      `${location.protocol}//${location.host}/${Util.getPath()}share/#/metadata?tableName=${this.tableName}` +
      (this.type == 'vdm' ? '&type=vdm' : '')
    );
  }

  get getScrollId () {
    return this.$route.query.scrollId || '';
  }

  get badgeType () {
    return this.commentsVal <= 0 ? 'info' : 'danger';
  }

  get getBrowseRoute () {
    const store: any = this.$store.getters.getBrowseRoute;
    return store && store.data ? store.data : false;
  }

  get getBrowseSearchRoute () {
    const store: any = this.$store.getters.getBrowseSearchRoute;
    return store && store.url ? store.url : false;
  }

  get getTableManagementRoute () {
    const store: any = this.$store.getters.getTableManagementRoute;
    return store && store.data ? store.data : false;
  }

  get getUserList () {
    let rs: any = [];
    if (_.toLower(this.userListPlatform) == 'all') {
      rs = this.concatArr;
    } else {
      rs = _.filter(this.concatArr, (v: any) => {
        return _.toLower(v.platform) == _.toLower(this.userListPlatform);
      });
    }
    //rs = _.uniqBy(rs, (v: any) => { return v.user_nt + v.batch_acct });
    return rs;
  }

  get getUserListPltformOptions () {
    const rs: any = _.transform(_.concat('All', _.uniq(_.map(this.concatArr, 'platform'))), (result: any, n: any) => {
      result.push(_.upperFirst(n));
    });
    return rs;
  }

  get getAllDatabases () {

    const rs = _.union(_.transform(_.map(this.tableDict, 'db_name'), (rs: any, v: any) => {
      if (v && _.trim(v) != '' && _.toLower(_.trim(v)) != 'default') {
        rs.push(_.toLower(v));
      }
    }));

    const defaultDb = _.find(this.tableDict, (v: any) => { return v.db_name && ((_.toLower(_.trim(v.db_name)) == 'default') || _.trim(v.db_name) == ''); });
    if (defaultDb) {
      rs.push(_.toLower('default'));
    }
    return _.union(rs);
  }

  get isLkpTable () {
    return attempt(() => this.tableDict[0].asset_type === 'realtime', false);
  }
  get isSLA () {
    return !!this.tableDict.find(t => t.sla_flag === 'Y' || t.sle_flag === 'Y');
  }
  get isIDL () {
    return !!this.tableDict.find(t => t.information_layer === 'idl');
  }
  async mounted () {
    //const doc: any = $(document);
    //if (doc) $(".metadata-display").height(doc.height() - 126);
    Logger.counter('METADATA_TABLE_DETAIL', 1, { name: 'overview', trigger: this.entrance, table: this.tableName });
    this.activeTab = this.tab ? this.tab : 'Overview';
    this.fullNameByNt(Util.getNt());
    this.getTableLabels(this.tableName);
    this.getConfidenceScore(this.tableName);
    if (this.type == 'vdm') {
      this.viewInited = true;
      this.viewDict = [];
      this.getVDMInfo(this.tableName);
      this.checkVDM(this.tableName);
      this.getVDMViewInfo(this.tableName);
    } else {
      this.getMetaDataTable(this.tableName);
      this.getMetaDataView(this.tableName);
    }
    //this.getContactList(this.tableName);
    this.getSampleQuery(this.tableName);
    this.getSampleData(this.tableName);
    this.getTableComment(this.tableName);
    this.getSmartQuery('.' + this.tableName);
    this.getSearchTables('.' + this.tableName);
    this.get24HourEnotify(this.tableName);
    this.getAllEnotify(this.tableName);
  }

  openNewTab () {
    window.open(this.shareUrl, this.tableName);
  }

  getEmailLink (): string {
    return (
      `${location.protocol}//${location.host}/${Util.getPath()}#/metadata?` +
      'tableName=' +
      this.tableName +
      (this.type == 'vdm' ? '&type=vdm' : '')
    );
  }

  copytext () {
    const copyText: any = _.join(_.uniq(_.map(this.getUserList, 'user_mail')), ';');
    const copyFlg: any = copy(copyText);
    if (copyFlg) {
      this.$message.success('Copy to clipboard success!');
      this.userListVisible = false;
    }
  }

  getScoreText (txt: string): string {
    if (_.toLower(txt) == 'high') {
      return 'Normal';
    } else {
      return _.upperFirst(_.toLower(txt));
    }
    return '';
  }

  async getContactList (tableName: string) {
    const params: any = {
      table: tableName,
      startDate: moment()
        .subtract(30, 'days')
        .format('YYYY-MM-DD'),
      endDate: moment().format('YYYY-MM-DD'),
    };
    this.doeRemoteService
      .getContactList(params)
      .then((res: any) => {
        console.debug('Call Api:getMetaDataTable success');
        if (res && res.data && res.data.data && res.data.data.value) {
          this.concatArr = res.data.data.value;
        }
      })
      .catch(err => {
        console.error('Call Api:getMetaDataTable failed: ' + JSON.stringify(err));
      });
  }

  async getMetaDataTable (tableName: string) {
    this.loading = true;
    this.doeRemoteService
      .getMetaDataTable(tableName || '')
      .then((res: any) => {
        console.debug('Call Api:getMetaDataTable success');
        if (res && res.data && res.data != null) {
          if (!_.isUndefined(res.data.data) && res.data.data.hasOwnProperty('value')) {
            const arr: any = _.orderBy(res.data.data.value, ['platform', 'db_name'], ['asc', 'desc']);
            this.tableDict = arr;
            this.tableInited = true;
          }
        }
        if (this.viewInited) {
          this.loading = false;
        }
      })
      .catch(err => {
        console.error('Call Api:getMetaDataTable failed: ' + JSON.stringify(err));
        this.loading = false;
      });
  }

  async getVDMInfo (tableName: string) {
    this.loading = true;
    const paramsArr = tableName.split('.');
    const param = {
      db: paramsArr.length > 0 ? paramsArr[0] : '',
      table: paramsArr.length > 1 ? paramsArr[1] : '',
    };
    this.doeRemoteService
      .getVDMInfo(param)
      .then((res: any) => {
        console.debug('Call Api:getVDMInfo success');
        if (res && res.data && res.data != null) {
          if (!_.isUndefined(res.data.data) && res.data.data.hasOwnProperty('value')) {
            const arr: any = _.orderBy(res.data.data.value, ['platform', 'db_name'], ['asc', 'desc']);
            this.tableDict = arr;
            this.tableInited = true;
          }
        }
        if (this.viewInited) this.loading = false;
      })
      .catch(err => {
        console.error('Call Api:getVDMInfo failed: ' + JSON.stringify(err));
        this.loading = false;
      });
  }

  async getVDMViewInfo (tableName: string) {
    this.loading = true;
    const paramsArr = tableName.split('.');
    this.getMetaDataView(_.toString(_.last(paramsArr)));
  }

  async getTableLabels (tableName: string) {
    this.loading = true;
    this.tableLabels = [];
    this.restrictedLabels = [];
    this.unrestrictedLabels = [];
    this.doeRemoteService
      .getTableLabels(tableName)
      .then((res: any) => {
        if (res && res.data && res.data != null) {
          if (
            !_.isUndefined(res.data.data) &&
            res.data.data.hasOwnProperty('value')
          ) {
            this.tableLabels = _.isEmpty(res.data.data.value.label[0]['labels'])
              ? []
              : _.split(res.data.data.value.label[0]['labels'], ',');
            this.restrictedLabels = _.isEmpty(res.data.data.value.restrictedLabel[0]['restricted_label'])
              ? []
              : _.split(res.data.data.value.restrictedLabel[0]['restricted_label'], ',');
            this.unrestrictedLabels = _.isEmpty(res.data.data.value.unrestrictedLabel[0]['unrestricted_label'])
              ? []
              : _.split(res.data.data.value.unrestrictedLabel[0]['unrestricted_label'], ',');
          }
        }
      })
      .catch(err => {
        console.error('Call Api:getTableLabels failed: ' + JSON.stringify(err));
        this.loading = false;
      });
  }

  async checkVDM (tableName: string) {
    if (tableName.split('.') && tableName.split('.').length > 1) {
      const params: any = {
        db: _.toLower(tableName.split('.')[0]),
        table: _.toLower(tableName.split('.')[1]),
      };
      this.doeRemoteService
        .checkVDM(params)
        .then(
          res => {
            if (res && res.data && res.data.data && res.data.data.value) {
              const findSt_2: any = _.find(res.data.data.value, v => {
                return v.status == 2;
              });
              if (findSt_2) {
                this.vdmAllPlatform = _.transform(
                  findSt_2.status_info.split(','),
                  (rs: any, v: any) => {
                    rs.push(_.upperFirst(_.toLower(v)).replace('mozart', 'Mozart'));
                  },
                  []
                );
              }
            }
          },
          function (error: any) {
            error.resolve();
            throw error;
          }
        )
        .catch(err => {
          console.error('Call Api:checkVDM failed: ' + JSON.stringify(err));
        });
    }
  }

  async getMetaDataView (tableName: string) {
    this.loading = true;
    this.doeRemoteService
      .getMetaDataView(tableName || '')
      .then(
        (res: any) => {
          console.debug('Call Api:getMetaDataView success');
          if (res && res.data && res.data != null) {
            if (!_.isUndefined(res.data.data) && res.data.data.hasOwnProperty('value')) {
              this.viewDict = res.data.data.value;
              this.viewInited = true;
            }
          }
          if (this.tableInited) {
            this.loading = false;
          }
        },
        function (error: any) {
          error.resolve();
          throw error;
        }
      )
      .catch(err => {
        console.error('Call Api:getMetaDataView failed: ' + JSON.stringify(err));
        this.loading = false;
      });
  }

  async getSampleQuery (tableName: string) {
    this.loading = true;
    const params: any = { table: tableName || '' };
    this.doeRemoteService
      .getSampleQuery(params)
      .then(
        (res: any) => {
          console.debug('Call Api:getSampleQuery success');
          if (res && res.data && res.data != null) {
            if (!_.isUndefined(res.data.data) && res.data.data.hasOwnProperty('value')) {
              const arr: any = _.orderBy(res.data.data.value, ['upd_date'], ['desc']);
              this.sampleQueryDict = arr;
              this.sampleInited = true;
            }
          }
          if (this.tableInited && this.viewInited) this.loading = false;
        },
        function (error: any) {
          error.resolve();
          throw error;
        }
      )
      .catch(err => {
        console.error('Call Api:getSampleQuery failed: ' + JSON.stringify(err));
        this.loading = false;
      });
  }

  async getSampleData (tableName: string) {
    this.loading = true;
    const params: any = { table: tableName || '' };
    this.doeRemoteService
      .getSampleData(params)
      .then(
        (res: any) => {
          console.debug('Call Api:getSampleData success');
          if (res && res.data && res.data != null) {
            if (!_.isUndefined(res.data.data) && res.data.data.hasOwnProperty('value')) {
              this.sampleDataDict = res.data.data.value;
            }
          }
          if (this.tableInited && this.viewInited) this.loading = false;
        },
        function (error: any) {
          error.resolve();
          throw error;
        }
      )
      .catch(err => {
        console.error('Call Api:getSampleData failed: ' + JSON.stringify(err));
        this.loading = false;
      });
  }

  async getTableComment (tableName: string) {
    this.commentLoading = true;
    this.commentsVal = 0;
    this.commentDict = { comments: [] };
    const params: any = { table: tableName || '' };
    this.doeRemoteService
      .getTableComment(params)
      .then(
        (res: any) => {
          console.debug('Call Api:getTableComment success');
          if (res && res.data && res.data != null) {
            if (!_.isUndefined(res.data.data) && res.data.data.hasOwnProperty('value')) {
              this.commentsVal = _.size(_.uniq(_.map(res.data.data.value, 'comment_id')));
              this.commentDict = { comments: res.data.data.value };
            } else {
              this.commentsVal = 0;
              this.commentDict = { comments: [] };
            }
          } else {
            this.commentsVal = 0;
            this.commentDict = { comments: [] };
          }
          const child: any = this.$refs.metadataComment;
          child.cancel();
          child.allReplyCancel();
          this.commentLoading = false;
        },
        function (error: any) {
          error.resolve();
          throw error;
        }
      )
      .catch(err => {
        console.error('Call Api:getTableComment failed: ' + JSON.stringify(err));
        this.commentsVal = 0;
        this.commentDict = { comments: [] };
        this.commentLoading = false;
      });
  }

  async getSmartQuery (tables: any) {
    const params: any = { tables: tables, num: 3, isOr: true };
    this.smartQueryArr = [];
    this.doeRemoteService
      .getSmartQuery(params)
      .then(res => {
        if (res && res.data && res.data.data && res.data.data.value && res.data.data.value.length > 0) {
          this.smartQueryArr = res.data.data.value;
        }
      })
      .catch(err => {
        console.error('getSmartQuery: ' + JSON.stringify(err));
      });
  }

  async getSearchTables (tables: any) {
    const params: any = { tables: tables };
    this.smartQueryTableArr = [];
    this.doeRemoteService
      .getSearchTables(params)
      .then(res => {
        if (res && res.data && res.data.data && res.data.data.value && res.data.data.value.length > 0) {
          this.smartQueryTableArr = res.data.data.value;
        }
      })
      .catch(err => {
        console.error('getSearchTables: ' + JSON.stringify(err));
      });
  }

  async get24HourEnotify (tables: any) {
    const startTime: any = moment()
      .utcOffset('-07:00')
      .subtract(1, 'days')
      .format('YYYY-MM-DD HH:mm:ss');
    const params: any = { table: tables, start_time: startTime };
    this.existEnotify = false;
    this.enotify = {};
    this.doeRemoteService
      .getEnotify(params)
      .then(res => {
        if (res && res.data && res.data.data && res.data.data.value && res.data.data.value.length > 0) {
          this.existEnotify = true;
          this.enotify = res.data.data.value[0];
        }
      })
      .catch(err => {
        console.error('get24HourEnotify: ' + JSON.stringify(err));
      });
  }

  async getAllEnotify (tables: any) {
    const params: any = { table: tables };
    this.enotifyInited = false;
    this.enotify = {};
    this.doeRemoteService
      .getEnotify(params)
      .then(res => {
        if (res && res.data && res.data.data && res.data.data.value && res.data.data.value.length > 0) {
          this.enotifyInited = true;
          this.enotifyArr = res.data.data.value;
        }
      })
      .catch(err => {
        console.error('getAllEnotify: ' + JSON.stringify(err));
      });
  }

  revert () {
    this.edit = false;
    const $smy: any = this.$refs.smy;
    const $cols: any = this.$refs.cols;
    if ($smy && $smy.revert) {
      $smy.revert();
    }

    if ($cols && $cols.revert) {
      $cols.revert();
    }
  }

  async save () {
    const $smy: any = this.$refs.smy;
    const $cols: any = this.$refs.cols;
    if (this.activeTab == 'Overview') {
      await this.addTblLables();
      if (this.type == 'vdm') {
        $smy.saveVdm();
        this.registerVDM();
      } else {
        this.saveTbl();
      }
    }

    if (this.activeTab == 'Columns' && $cols && !_.isUndefined($cols.update_columns)) {
      this.updateColumns = $cols.update_columns;
      this.columnsDialogVisible = true;
    }
  }

  async addTblLables () {
    const $smy: any = this.$refs.smy;
    if ($smy && $smy.dynamicLabels) {
      this.doeRemoteService
        .replaceTableLabels(this.tableName, $smy.dynamicLabels)
        .then(
          (res: any) => {
            this.getTableLabels(this.tableName);
            console.debug('Call Api:replaceTableLabels success');
          },
          function (error: any) {
            if (error.message == 'Unknown error') error.message = 'Save table lables error';
            throw error;
          }
        )
        .catch(err => {
          err.resolve();
          const exception = customZetaException(err.message.info, {}, {
            path: 'metadata',
          });
          this.$store.dispatch('addException', {exception});
          console.error('Call Api:replaceTableLabels failed: ' + JSON.stringify(err));
          this.loading = false;
        });
    }
  }

  async saveTbl () {
    this.loading = true;
    const $smy: any = this.$refs.smy;
    const link: any = this.getEmailLink();
    let msg = this.fullName + ' updated the metadata for <a href=' + link + '>' + this.tableName + '</a>';
    let sendMailFlg = false;
    // table desc save
    if ($smy && !_.isUndefined($smy.temp_summary) && !_.isUndefined($smy.temp_summary.table_desc)) {
      if ($smy.temp_metadataDict.table_desc != $smy.temp_summary.table_desc) {
        const res: any = await this.doeRemoteService.addTblDesc(this.tableName, $smy.temp_summary.table_desc).catch(err => { this.loading = false; });
  
        if (res && res.status == 200) {
          msg +=
            '<br/><br/><span style=\'font-weight: bold;\'>Description:</span><span>' + $smy.temp_summary.table_desc + '</span>';
          sendMailFlg = true;
        } else {
          return;
        }
      }
    }

    // table owner save
    if ($smy && !_.isUndefined($smy.temp_summary) && !_.isUndefined($smy.ownerSel)) {
      if (_.toLower(_.trim(_.isEmpty($smy.temp_metadataDict.owner) ? $smy.temp_metadataDict.product_owner : $smy.temp_metadataDict.owner)) != _.toLower(_.trim($smy.ownerSel))) {
        const params: any = {
          table: this.tableName,
          cre_user: Util.getNt(),
          owner: $smy.ownerSel,
          owner_name: $smy.ownerSel == Util.getNt() ? $smy.currentName : $smy.fullName,
          isReset: $smy.ownerSel == Util.getNt() ? false : true,
        };

        const res: any = await this.addTblOwner(params);

        if (res && res.status == 200) {
          msg +=
            '<br/><br/><span style=\'font-weight: bold;\'>Owner:<span style=\'color: red; text-decoration:line-through; font-weight: normal;\'>' +
            (_.isEmpty($smy.temp_metadataDict.owner_name) ? $smy.fullName : $smy.temp_metadataDict.owner_name) +
            '</span><span style=\'font-weight: normal;\'>  ' +
            params.owner_name +
            '</span></span>';
          sendMailFlg = true;
        } else {
          return;
        }
      }
    }

    if (sendMailFlg) {
      // send email
      const content: any = {
        name: 'All',
        msg: msg,
      };
      const nts = [Util.getNt()];
      if ($smy && $smy.temp_summary) {
        nts.push(this.type == 'vdm' ? $smy.temp_summary.owner_nt || '' : (_.isEmpty($smy.temp_summary.owner) ? $smy.temp_summary.product_owner || '' : $smy.temp_summary.owner)); // add form o
        if (_.toLower(_.trim(_.isEmpty($smy.temp_metadataDict.owner) ? $smy.temp_metadataDict.product_owner : $smy.temp_metadataDict.owner)) != _.toLower(_.trim($smy.ownerSel))) {
          nts.push($smy.ownerSel);
        }
      }
      const param = {
        subject: this.tableName + ' - Metadata Overview Updated',
        content: content,
        nts: _.union(nts),
        teamDl: $smy && $smy.temp_summary ? $smy.temp_summary.team_dl : '',
      };
      this.sendMail(param, 'Submit success');
    }

    this.reload(this.tableName);
  }

  async addTblOwner (params: any) {
    return this.doeRemoteService.addTblOwner(params).catch(err => { this.loading = false; });
  }

  async addColDesc () {
    const now: string = moment()
      .utcOffset('-07:00')
      .format('YYYY-MM-DD HH:mm:ss');
    const columns: Array<any> = [];
    _.forEach(this.updateColumns, (v: any) => {
      if (!_.isUndefined(v.column_desc)) {
        const row: any = {
          table: this.tableName,
          name: v.column_name,
          upd_date: now,
          upd_user: Util.getNt(),
          desc: v.column_desc.replace(/<[^>]*>|/g, ''),
          desc_html: v.column_desc,
        };

        columns.push(row);
      }
    });

    if (!_.isEmpty(columns)) {
      this.loading = true;
      const params: any = { columns: columns };
      this.doeRemoteService
        .addColDesc(params)
        .then(
          (res: any) => {
            console.debug('Call Api:addColDesc success');
            if (res && res.status == 200) {
              const link: any = this.getEmailLink();
              const column: any = _.join(_.map(columns, 'name'), ',');
              const content: any = {
                name: 'All',
                msg:
                  this.fullName +
                  ' updated metatdata columns at column ' +
                  column +
                  '.<br/>' +
                  'Please <a href=' +
                  link +
                  '>click</a>',
              };
              const nts = [Util.getNt()];
              const $smy: any = this.$refs.smy;
              if ($smy && $smy.temp_summary) {
                nts.push(this.type == 'vdm' ? $smy.temp_summary.owner_nt || '' : $smy.temp_summary.product_owner || ''); // add form o
              }
              const param = {
                subject: this.tableName + ' - Metadata Columns Updated',
                content: content,
                nts: nts,
                teamDl: $smy && $smy.temp_summary ? $smy.temp_summary.team_dl : '',
              };
              this.sendMail(param, 'Submit success');
              this.reload(this.tableName);
            }
          },
          function (error: any) {
            if (error.message == 'Unknown error') error.message = 'Save cols description error';
            throw error;
          }
        )
        .catch(err => {
          console.error('Call Api:addColDesc failed: ' + JSON.stringify(err));
          this.loading = false;
        });
    } else {
      this.edit = false;
      this.$message.info('No column update.');
    }
  }

  async addSampleQuery (params: any, isNew: boolean) {
    if (params) {
      const now: string = moment()
        .utcOffset('-07:00')
        .format('YYYY-MM-DD HH:mm:ss');
      params.table = this.tableName;
      params.upd_date = now;
      params.upd_user = Util.getNt();
      this.loading = true;
      this.sampleInited = false;
      this.doeRemoteService
        .addSampleQuery(params)
        .then(
          (res: any) => {
            console.debug('Call Api:addSampleQuery success');
            if (res && res.status == 200) {
              const link: any = this.getEmailLink();
              const content: any = {
                name: 'All',
                msg:
                  this.fullName +
                  (isNew ? ' added a new sample query.<br/>' : ' updated a sample query.<br/>') +
                  'Please <a href=' +
                  link +
                  '>click</a>',
              };
              const nts = [Util.getNt()];
              const $smy: any = this.$refs.smy;
              if ($smy && $smy.temp_summary) {
                nts.push(this.type == 'vdm' ? $smy.temp_summary.owner_nt || '' : $smy.temp_summary.product_owner || ''); // add form o
              }
              const param = {
                subject: this.tableName + (isNew ? ' - New Sample Queries' : ' - Updated Sample Queries'),
                content: content,
                nts: nts,
                teamDl: $smy && $smy.temp_summary ? $smy.temp_summary.team_dl : '',
              };
              this.sendMail(param, 'Submit success');
              this.reload(this.tableName);
            }
          },
          function (error: any) {
            if (error.message == 'Unknown error') error.message = 'Save sample query error';
            throw error;
          }
        )
        .catch(err => {
          console.error('Call Api:addSampleQuery failed: ' + JSON.stringify(err));
          this.loading = false;
        });
    } else {
      this.$message.info('No sample query add.');
    }
  }

  async deleteSampleQuery (params: any) {
    if (params) {
      this.loading = true;
      this.sampleInited = false;
      params.table = this.tableName;
      this.doeRemoteService
        .deleteSampleQuery(params)
        .then(
          (res: any) => {
            console.debug('Call Api:deleteSampleQuery success');
            if (res && res.status == 200) {
              const link: any = this.getEmailLink();
              const content: any = {
                name: 'All',
                msg: this.fullName + ' deleted a sample query for table ' + this.tableName + '.<br/>' + 'Click <a href=' + link + '>here</a> to check latest change.',
              };
              const nts = [Util.getNt()];
              const $smy: any = this.$refs.smy;
              if ($smy && $smy.temp_summary) {
                nts.push(this.type == 'vdm' ? $smy.temp_summary.owner_nt || '' : $smy.temp_summary.product_owner || ''); // add form o
              }
              const param = {
                subject: this.tableName + ' - Delete Sample Queries',
                content: content,
                nts: nts,
                teamDl: $smy && $smy.temp_summary ? $smy.temp_summary.team_dl : '',
              };
              this.sendMail(param, 'Submit success');
              this.reload(this.tableName);
            }
          },
          function (error: any) {
            if (error.message == 'Unknown error') error.message = 'Delete sample query error';
            throw error;
          }
        )
        .catch(err => {
          console.error('Call Api:deleteSampleQuery failed: ' + JSON.stringify(err));
          this.loading = false;
        });
    } else {
      this.$message.info('No sample data delete.');
    }
  }

  async addSampleData (params: any) {
    if (params) {
      const now: string = moment()
        .utcOffset('-07:00')
        .format('YYYY-MM-DD HH:mm:ss');
      params.table = this.tableName;
      params.upd_date = now;
      params.upd_user = Util.getNt();
      this.loading = true;
      this.doeRemoteService
        .addSampleData({ columns: [params] })
        .then(
          (res: any) => {
            console.debug('Call Api:addSampleData success');
            if (res && res.status == 200) {
              const link: any = this.getEmailLink();
              const content: any = {
                name: 'All',
                msg:
                  this.fullName +
                  ' added a new sample data at column ' +
                  params.name +
                  '.<br/>' +
                  'Please <a href=' +
                  link +
                  '>click</a>',
              };
              const nts = [Util.getNt()];
              const $smy: any = this.$refs.smy;
              if ($smy && $smy.temp_summary) {
                nts.push(this.type == 'vdm' ? $smy.temp_summary.owner_nt || '' : $smy.temp_summary.product_owner || ''); // add form o
              }
              const param = {
                subject: this.tableName + ' - Metadata Sample Data Updated',
                content: content,
                nts: nts,
                teamDl: $smy && $smy.temp_summary ? $smy.temp_summary.team_dl : '',
              };
              this.sendMail(param, 'Submit success');
              this.getSampleData(this.tableName);
              const $cols: any = this.$refs.cols;
              if ($cols && $cols.dialogClose) {
                $cols.dialogClose();
              }
              //this.$message.success("Submit success!");
            }
            this.loading = false;
          },
          function (error: any) {
            if (error.message == 'Unknown error') error.message = 'Save sample data error';
            throw error;
          }
        )
        .catch(err => {
          console.error('Call Api:addSampleData failed: ' + JSON.stringify(err));
          this.loading = false;
        });
    } else {
      this.$message.info('No sample data add.');
    }
  }

  async addTableComment (params: any) {
    if (params) {
      const now: string = moment()
        .utcOffset('-07:00')
        .format('YYYY-MM-DD HH:mm:ss');
      params.table = this.tableName;
      params.cre_date = now;
      params.cre_user = Util.getNt();
      this.commentLoading = true;
      this.doeRemoteService
        .addTableComment(params)
        .then(
          (res: any) => {
            console.debug('Call Api:addTableComment success');
            if (res && res.status == 200) {
              this.getTableComment(this.tableName);
              const link: any = this.getEmailLink() + '&scrollId=' + params.comment_id;
              const content: any = {
                name: 'All',
                msg:
                  this.fullName +
                  ' added a new comment.<br/>' +
                  params.comment +
                  '<br/>' +
                  'Please <a href=' +
                  link +
                  '>click</a>',
              };
              const nts = params.nts;
              const $smy: any = this.$refs.smy;
              if ($smy && $smy.temp_summary) {
                nts.push(this.type == 'vdm' ? $smy.temp_summary.owner_nt || '' : $smy.temp_summary.product_owner || ''); // add form o
              }
              const param = {
                subject: this.tableName + ' - New comment/reply',
                content: content,
                nts: nts,
                teamDl: $smy && $smy.temp_summary ? $smy.temp_summary.team_dl : '',
              };
              this.sendMail(param, 'Submit success');
            }
            this.commentLoading = false;
          },
          function (error: any) {
            if (error.message == 'Unknown error') error.message = 'Add comment error';
            throw error;
          }
        )
        .catch(err => {
          console.error('Call Api:addTableComment failed: ' + JSON.stringify(err));
          this.commentLoading = false;
        });
    } else {
      this.$message.info('No comment add.');
    }
  }

  async deleteTableComment (params: any) {
    if (params) {
      this.commentLoading = true;
      params.table = this.tableName;
      this.doeRemoteService
        .deleteTableComments(params)
        .then(
          (res: any) => {
            console.debug('Call Api:deleteTableComments success');
            if (res && res.status == 200) {
              this.getTableComment(this.tableName);
              const link: any = this.getEmailLink() + '&scrollId=' + params.comment_id;
              const content: any = {
                name: 'All',
                msg: this.fullName + ' deleted a new comment.<br/>' + 'Please <a href=' + link + '>click</a>',
              };
              const nts = params.nts;
              const $smy: any = this.$refs.smy;
              if ($smy && $smy.temp_summary) {
                nts.push(this.type == 'vdm' ? $smy.temp_summary.owner_nt || '' : $smy.temp_summary.product_owner || ''); // add form o
              }
              const param = {
                subject: this.tableName + ' - Delete comment',
                content: content,
                nts: nts,
                teamDl: $smy && $smy.temp_summary ? $smy.temp_summary.team_dl : '',
              };
              this.sendMail(param, 'Submit success');
            }
            this.commentLoading = false;
          },
          function (error: any) {
            if (error.message == 'Unknown error') error.message = 'Delete comment error';
            throw error;
          }
        )
        .catch(err => {
          console.error('Call Api:deleteTableComment failed: ' + JSON.stringify(err));
          this.commentLoading = false;
        });
    } else {
      this.$message.info('No comment delete.');
    }
  }

  async sendMail (param: any, message: any) {
    const teamDl: any = param.teamDl ? param.teamDl + ';' : '';
    const params: any = {
      fromAddr: 'DL-eBay-Metadata@ebay.com',
      toAddr: '',
      subject: param.subject,
      content: JSON.stringify(param.content),
      template: 'ZetaNotification',
      ccAddr: param.ccAddr ? param.ccAddr : 'DL-eBay-Metadata@ebay.com',
      type: 3, //1: html; 2: txt
    };
    const nts = _.uniq(param.nts);
    for await (const nt of nts) {
      if (!_.isEmpty(nt)) {
        const res = await this.doeRemoteService.getEmailAddr(nt);
        if (
          res &&
          res.data &&
          res.data != null &&
          !_.isEmpty(res.data.data) &&
          !_.isEmpty(res.data.data.value) &&
          res.data.data.value.length > 0
        ) {
          _.forEach(res.data.data.value, (v: any) => {
            params.toAddr = params.toAddr + v.mail + ';';
          });
        } else {
          // TODO LOG
          // window.$logger.error(["EMAIL ADDR ERROR", "Get empty email address by " + nt, res.config.url], "Get empty email address by " + nt);
        }
      }
    }
    params.toAddr = params.toAddr + teamDl;
    if (!_.isEmpty(params.toAddr)) {
      this.doeRemoteService
        .createEmail(params)
        .then(res => {
          console.debug('Call Api:createEmail successed');
          this.loading = false;
          if (res && res.status == 200) {
            if (message) this.$message.success(message);
          }
        })
        .catch(err => {
          console.error('Call Api:createEmail failed: ' + JSON.stringify(err));
          this.$message.info(message + ', but send email failed');
        });
    } else {
      this.loading = false;
      this.$message.info(message + ', but empty recipient mailbox');
    }
  }

  async registerVDM () {
    const $smy: any = this.$refs.smy;
    if ($smy && $smy.temp_summary && this.tableName.split('.') && this.tableName.split('.').length > 1) {
      // if (_.isEmpty($smy.temp_summary.owner_nt) && _.isEmpty($smy.temp_summary.owner_name)) {
      //   this.$message.info('Must select onwer');
      //   return;
      // }
      if (_.isEmpty($smy.ownerList)) {
        this.$message.info('Must add onwer');
        return;
      }

      const reg = new RegExp('^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(.[a-zA-Z0-9_-]+)+$');
      if (reg.exec($smy.temp_summary.team_dl) === null || typeof reg.exec($smy.temp_summary.team_dl) === 'undefined') {
        this.$message.info('Team DL wrong format');
        return;
      }

      const params: any = {
        // owner_name: $smy.temp_summary.owner_name,
        // owner_nt: $smy.temp_summary.owner_nt,
        team: $smy.temp_summary.team_name,
        team_dl: $smy.temp_summary.team_dl,
        table_desc: $smy.temp_summary.table_desc,
        db: $smy.temp_summary.db_name,
        table: $smy.temp_summary.table_name,
        platforms: _.toLower(_.join($smy.vdmPlatform, ',')),
        git_link: $smy.temp_summary.git_link,
        script: $smy.temp_summary.script,
        cre_user: Util.getNt(),
        ownerList: $smy.ownerList,
      };
      this.loading = true;
      this.doeRemoteService
        .registerVDM(params)
        .then(
          (res: any) => {
            console.debug('Call Api:registerVDM success');
            if (res && res.status == 200) {
              this.getTableComment(this.tableName);
              const $smy: any = this.$refs.smy;
              const link: any = this.getEmailLink();
              let msg = this.fullName + ' updated the metadata for <a href=' + link + '>' + this.tableName + '</a>';
              msg +=
                '<br/><br/><span style=\'font-weight: bold;\'>Description:</span><span>' + params.table_desc + '</span>';
              if (!_.isEmpty(_.xorBy(params.ownerList, $smy.temp_metadataDict.ownerList, 'nt'))) {
                const intersection = _.intersectionBy(params.ownerList, $smy.temp_metadataDict.ownerList, 'nt');
                const removeSecvtion = _.xorBy($smy.temp_metadataDict.ownerList, intersection, 'nt');
                const addSecvtion = _.xorBy(params.ownerList, intersection, 'nt');
                msg +=
                  '<br/><span style=\'font-weight: bold;\'>Owner:</span>';
                _.forEach(removeSecvtion, (v: any) => {
                  msg +=
                    '<br/><span style=\'color: red; text-decoration:line-through; font-weight: normal;\'>' + v.name + '</span>';
                });
                _.forEach(addSecvtion, (v: any) => {
                  msg +=
                    '<br/><span style=\'font-weight: normal;\'>' + v.name + '</span>';
                });
              }
              // if (params.owner_name != $smy.temp_metadataDict.owner_name) {
              //   msg +=
              //     '<br/><br/><span style=\'font-weight: bold;\'>DL:<span style=\'color: red; text-decoration:line-through; font-weight: normal;\'>' +
              //     $smy.temp_metadataDict.owner_name +
              //     '</span><span style=\'font-weight: normal;\'>  ' +
              //     params.owner_name +
              //     '</span></span>';
              // }
              if (params.team != $smy.temp_metadataDict.team_name) {
                msg +=
                  '<br/><br/><span style=\'font-weight: bold;\'>Team:<span style=\'color: red; text-decoration:line-through; font-weight: normal;\'>' +
                  $smy.temp_metadataDict.team_name +
                  '</span><span style=\'font-weight: normal;\'>  ' +
                  params.team +
                  '</span></span>';
              }
              if (params.team_dl != $smy.temp_metadataDict.team_dl) {
                msg +=
                  '<br/><br/><span style=\'font-weight: bold;\'>Team DL:<span style=\'color: red; text-decoration:line-through; font-weight: normal;\'>' +
                  $smy.temp_metadataDict.team_dl +
                  '</span><span style=\'font-weight: normal;\'>  ' +
                  params.team_dl +
                  '</span></span>';
              }
              const content: any = {
                name: 'All',
                msg: msg,
              };
              const unionOwner = _.map(_.unionBy(params.ownerList, $smy.temp_metadataDict.ownerList, 'nt'), 'nt');
              const nts = _.union([Util.getNt()], unionOwner);
              if ($smy && $smy.platform) {
                const find = _.find(this.tableDict, (v: any) => {
                  return _.toLower(v.platform) == _.toLower($smy.platform);
                });
                if (find) nts.push(find.owner_nt);
              }
              const param = {
                subject: this.tableName + ' - Metadata Overview Updated',
                content: content,
                nts: nts,
                teamDl: $smy && $smy.temp_summary ? $smy.temp_summary.team_dl : '',
              };
              this.sendMail(param, 'Submit success');
              this.reload(this.tableName);
              const data: any = {
                name: _.toLower(this.tableName),
                row_type: 'vdm',
                platform: params.platforms,
                desc: params.table_desc.replace(/<[^>]*>|/g, ''),
                total_score: 0,
                'normal name': _.union(_.split(_.toLower(this.tableName), '_')),
                'tags': _.union(_.split(_.toLower(this.tableName), '_')),
              };
              this.metadataService
                .putESTable({obj: 'table', id: _.toLower(this.tableName)}, data)
                .then(metas => {
                  console.debug('update es success');
                  this.$store.dispatch('setMetadataReload', true);
                })
                .catch(err => {
                  console.error('update es failed: ' + JSON.stringify(err));
                });
            }
          },
          function (error: any) {
            if (error.message == 'Unknown error') error.message = 'Submit error';
            throw error;
          }
        )
        .catch(err => {
          console.error('Call Api:registerVDM failed: ' + JSON.stringify(err));
          this.loading = false;
        });
    } else {
      this.$message.info('Cannot save');
    }
  }

  async getConfidenceScore (tableName: string) {
    const table = this.type == 'vdm' ? tableName.split('.')[1] : tableName;
    this.rate = 0;
    this.confidenceScore = (0).toFixed(1);
    this.usage = 0;
    this.confidenceDetails = {};
    this.confidenceDetails.usage = 'unknown';
    this.confidenceDetails.usageToolTip = 'No usage';
    this.confidenceDetails.availability = 'unknown';
    this.confidenceDetails.availabilityToolTip = 'No daily jobs found in DOE metadata';
    this.confidenceDetails.dq = 'unknown';
    this.confidenceDetails.dqToolTip = 'No DQ rule';
    this.confidenceDetails.governance = 'unknown';
    this.confidenceDetails.governanceToolTip = 'No data model';
    this.doeRemoteService
      .getConfidenceScore(table)
      .then((res: any) => {
        if (res && res.data && res.data.data && res.data.data.value) {
          if (res.data.data.value.length > 0) {
            this.rate = isNaN(res.data.data.value[0]['score']) ? 0 : res.data.data.value[0]['score'] / 2;
            this.confidenceScore = isNaN(res.data.data.value[0]['score'])
              ? 0.0
              : res.data.data.value[0]['score'].toFixed(1);
            this.usage =
              (isNaN(res.data.data.value[0]['all_platform_user_cnt'])
                ? 0
                : res.data.data.value[0]['all_platform_user_cnt']) +
              (isNaN(res.data.data.value[0]['all_platform_batch_cnt'])
                ? 0
                : res.data.data.value[0]['all_platform_batch_cnt']);
            this.confidenceDetails.usage = res.data.data.value[0]['usage_cfd'] || 'unknown';
            if (this.confidenceDetails.usage == 'unknown' || this.confidenceDetails.usage == 'critical') {
              this.confidenceDetails.usageToolTip = 'No usage';
            } else if (this.confidenceDetails.usage == 'warning') {
              this.confidenceDetails.usageToolTip = 'Low usage';
            } else if (this.confidenceDetails.usage == 'high') {
              this.confidenceDetails.usageToolTip = 'High usage';
            } else {
              this.confidenceDetails.usageToolTip = '';
            }
            this.confidenceDetails.availability = res.data.data.value[0]['avail_cfd'] || 'unknown';
            if (this.confidenceDetails.availability == 'unknown') {
              this.confidenceDetails.availabilityToolTip = 'No daily jobs found in DOE metadata';
            } else if (
              this.confidenceDetails.availability == 'critical' ||
              this.confidenceDetails.availability == 'warning'
            ) {
              this.confidenceDetails.availabilityToolTip =
                'Job finish time of this table is not stable on ' +
                res.data.data.value[0]['pltfrm_name'] +
                ' for recent 30 days';
            } else {
              this.confidenceDetails.availabilityToolTip = '';
            }
            this.confidenceDetails.dq = res.data.data.value[0]['dq_cfd'] || 'unknown';
            if (this.confidenceDetails.dq == 'unknown') {
              this.confidenceDetails.dqToolTip = 'No DQ rule';
            } else if (this.confidenceDetails.dq == 'critical') {
              this.confidenceDetails.dqToolTip = 'DQ rule is failed';
            } else {
              this.confidenceDetails.dqToolTip = '';
            }
            this.confidenceDetails.governance = res.data.data.value[0]['govn_cfd'] || 'unknown';
            if (this.confidenceDetails.governance == 'unknown' || this.confidenceDetails.governance == 'critical') {
              this.confidenceDetails.governanceToolTip = 'No data model';
            } else if (this.confidenceDetails.governance == 'warning') {
              this.confidenceDetails.governanceToolTip = 'Table has not been reviewed by data architect';
            } else {
              this.confidenceDetails.governanceToolTip = '';
            }
          }
        }
      })
      .catch(err => {
        console.error('Call Api:getConfidenceScore failed: ' + JSON.stringify(err));
        this.loading = false;
      });
  }

  formatDatabaseName (dbName: string) {
    return (dbName || 'DEFAULT').toUpperCase();
  }

  reload (tableName: any) {
    this.tableInited = false;
    this.viewInited = false;
    this.getTableLabels(tableName);
    this.getConfidenceScore(tableName);
    if (this.type == 'vdm') {
      this.viewInited = true;
      this.viewDict = [];
      this.getVDMInfo(tableName);
      this.checkVDM(tableName);
    } else {
      this.getMetaDataTable(tableName);
      this.getMetaDataView(tableName);
    }
    //this.getContactList(tableName);
    this.getSampleQuery(tableName);
    this.getSampleData(tableName);
    this.getTableComment(tableName);
    this.getSmartQuery('.' + tableName);
    this.getSearchTables('.' + tableName);
    this.get24HourEnotify(tableName);
    this.getAllEnotify(tableName);
    this.edit = false;
    const $cols: any = this.$refs.cols;
    if ($cols && $cols.cleanCache) {
      $cols.cleanCache();
    }
    const $spl: any = this.$refs.spl;
    if ($spl && $spl.cancel) {
      $spl.cancel();
    }
  }

  openComment () {
    const child: any = this.$refs.metadataComment;
    child.open();
  }

  closeComment (e: any) {
    if (e.target.name !== 'comment-btn' && e.target.className !== 'zeta-icon-comment-1') {
      const child: any = this.$refs.metadataComment;
      child.close();
    }
  }

  fullNameByNt (nt: string) {
    this.userInfoRemoteService
      .getBaseInfo(nt)
      .then((res: any) => {
        const userInfo = res.data.response.docs[0];
        this.fullName = res.data.response.docs[0].last_name + ' ' + res.data.response.docs[0].first_name || nt;
      })
      .catch(err => {
        this.fullName = nt;
      });
  }

  cleanStore () {
    this.$store.dispatch('setBrowseRoute', { data: false });
    this.$store.dispatch('setBrowseSearchRoute', { url: false });
    this.$store.dispatch('setTableManagementRoute', { data: false });
  }

  back () {
    this.$router.push('/metadatabrowse');
  }

  backSearch (url: any) {
    this.$router.push(url.substr(url.indexOf('/metadatabrowse')));
  }

  backTableManagement (data: any) {
    this.$router.push(data.readOnly ? '/tablemanagement_view' : data);
  }

  download () {
    const params: any = {
      table: this.tableName,
      startDate: moment()
        .subtract(30, 'days')
        .format('YYYY-MM-DD'),
      endDate: moment().format('YYYY-MM-DD'),
    };

    downloadUsage(params);
  }

  @Watch('tableName')
  onTableChange (val: any) {
    this.activeTab = 'Overview';
    this.reload(val);
  }
}
</script>

<style lang="scss" scoped>
@import '@/styles/global.scss';
$bc-height: $workspace-tab-height + $workspace-tab-margin-bottom;
.metadata {
  box-sizing: border-box;
  font-style: normal;
  height: 100%;
  overflow: hidden;
  padding: 0 25px;
  padding-top: 30px;
  width: 100%;
  > div {
    height: 100%;
  }
}
.el-breadcrumb {
  margin-bottom: 10px;
  .breadcrumb-clk {
    cursor: pointer;
    color: #4d8cca;
  }
  .breadcrumb-clk:hover {
    color: #569ce1;
    text-decoration: underline;
  }
}
.title {
  color: #1e1e1e;
  display: flex;
  align-items: center;
  .table-name {
    font-size: 18px;
    font-weight: 700;
  }
  .sla-icon {
    color: #cacbcf;
    border: 1px solid #cacbcf;
    border-radius: 4px;
    font-size: 11px;
    padding: 2px 4px;
    margin-left: 5px;
    display: inline-block;
    height: 16px;
  }
  .idl-icon {
    display: inline-block;
    border-radius: 861112px;
    border: 1px solid #cacbcf;
    color: #cacbcf;
    margin-left: 5px;
    margin-right: 3px;
    width: 18px;
    height: 18px;
    line-height: 18px;
    font-size: 8px;
    text-align: center;
    position: relative;
    top: -1px;
  }
  > span {
    margin-right: 5px;
  }
  /deep/ .zeta-icon-share {
    color: #cacbcf;
  }
  /deep/ .zeta-icon-share:hover {
    color: #569ce1;
  }
}
.score {
  .zeta-icon-hot {
    color: #cacbcf;
    font-size: 14px;
  }
}
.el-popover {
  .zeta-icon-info {
    font-size: 16px;
    margin: 0px 5px;
    text-decoration: none;
    color: #cacbcf;
  }
}
.metadata-tools-bar {
  align-items: center;
  display: flex;
  height: 40px;
  justify-content: space-between;
  border-bottom: 1px solid #cacbcf;
  ul.metadata-tabs {
    display: flex;
    height: 100%;
    list-style-type: none;
    > li {
      align-items: center;
      color: #999;
      cursor: pointer;
      display: flex;
      font-size: 14px;
      padding: 0 20px;
      &.active {
        background-color: #fff;
        color: $zeta-global-color;
        position: relative;
        &::after {
          display: inline-block;
          position: absolute;
          content: '';
          height: 1.5px;
          width: 100%;
          background: #569ce1 ;
          left: 0;
          bottom: -1.5px;
        }
      }
      &:hover {
        color: #569ce1;
      }
    }
  }
}
.metadata-display {
  display: block;
  height: calc(100% - #{$bc-height} - 65px);
  > div {
    height: 100%;
  }
  .overview {
    overflow-y: auto;
  }
  .overview::-webkit-scrollbar {
    width: 0;
  }
  .columns {
    padding: 10px 0;
    div.el-table {
      /deep/ div.el-table__body-wrapper {
        tr > td > .cell {
          word-break: break-word;
          white-space: pre-line;
        }
      }
    }
  }
  .sample {
    padding: 10px 0;
  }
}
.btn-group {
  display: inline-block;
  height: 39px;
  padding-right: 10px;
  text-align: right;
}
.comment-icon-div {
  bottom: 20px;
  position: fixed;
  right: 20px;
  width: 73px;
  /deep/ .el-badge__content {
    top: 10px;
    right: 35px;
  }
  .el-button {
    border-color: #fff;
    box-shadow: 0 0 10px #cacbcf;
    color: #569ce1;
    min-width: 0;
  }
  .el-button:hover {
    background-color: #fff;
    color: #4d8cca;
  }
  /deep/ [class^='zeta-icon-'],
  [class*=' zeta-icon-'] {
    font-size: 25px !important;
  }
}
.cancel-btn {
  background: inherit;
  background-color: #fff;
  border: 1px solid #569ce1;
  border-radius: 4px;
  box-shadow: none;
  color: #569ce1;
}
.el-icon-back {
  cursor: pointer;
  font-size: 18px;
  float: left;
  color: #569ce1;
  margin-right: 10px;
}
.current-enotify {
  display: flex;
  margin-left: 20px;
  cursor: pointer;
  background-color: rgba(86, 156, 225, 0.0980392156862745);
}
.enotify-content {
  line-height: 23px;
  padding-right: 5px;
  padding-top: 3px;
}
.enotify-content:hover {
  color: #569ce1;
}
.zeta-icon-notify {
  font-size: 20px;
  color: #569ce1;
  padding: 0 5px;
}
.zeta-icon-export {
  cursor: pointer;
  font-size: 18px;
  color: #cacbcf;
  margin-left: 5px;
  margin-right: 5px;
}
.zeta-icon-export:hover {
  color: #4d8cca;
}
.back-css {
  cursor: pointer;
  color: #569ce1;
  display: block;
  margin-bottom: 10px;
}
.back-css:hover {
  color: #4d8cca;
  text-decoration: underline;
}
.filter-label {
  line-height: 35px;
  margin-right: 5px;
}
.icon-score {
  height: 16px;
  width: 16px;
}
.high {
  //background-color: #52A552;
  background-color: #5cb85c;
}
.normal {
  background-color: #5cb85c;
}
.warning {
  background-color: #f3af2b;
}
.critical {
  background-color: #e53917;
}
.unknown {
  background-color: #cacbcf;
}
.high-label {
  margin-left: 5px;
  color: #5cb85c;
  font-size: 13px;
}
.normal-label {
  margin-left: 5px;
  color: #5cb85c;
  font-size: 13px;
}
.warning-label {
  margin-left: 5px;
  color: #f3af2b;
  font-size: 13px;
}
.critical-label {
  margin-left: 5px;
  color: #e53917;
  font-size: 13px;
}
.unknown-label {
  margin-left: 5px;
  color: #cacbcf;
  font-size: 13px;
}
table {
  width: 100%;
  > tr {
    padding: 5px 0;
    > th {
      font-weight: 400;
    }
    > td {
      display: flex;
      padding: 0;
    }
  }
}
</style>
