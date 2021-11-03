<template>
  <div
    v-if="metadataTableDict || metadataViewDict"
    class="overview"
  >
    <div class="metadata">
      <table class="display">
        <!-- Description -->
        <tr>
          <th
            width="150"
            valign="top"
          >
            Description
          </th>
          <td valign="top">
            <template v-if="!edit">
              <div
                class="ql-editor"
                v-html="summary.table_desc"
              />
            </template>
            <template v-else>
              <quill-editor
                v-model="summary.table_desc"
                :options="editorOptions"
              />
            </template>
          </td>
        </tr>
        <!-- SA -->
        <tr v-if="type != 'vdm'">
          <th valign="top">
            SA
          </th>
          <td valign="top">
            <span>{{ summary.subject_area || "" }}</span>
          </td>
        </tr>
        <!-- Owner -->
        <tr>
          <th>Owner</th>
          <td>
            <template v-if="type != 'vdm' && edit">
              <el-select
                v-model="ownerSel"
                placeholder="Select Owner"
              >
                <el-option
                  v-if="summary.owner != null && summary.owner != currentNt"
                  key="currentOwner"
                  :label="'current owner: ' + summary.owner_name"
                  :value="summary.owner"
                />
                <el-option
                  key="yourself"
                  :label="currentName"
                  :value="currentNt"
                />
                <el-option
                  key="saowner"
                  :label="'SA owner: ' + fullName"
                  :value="summary.product_owner"
                />
              </el-select>
            </template>
            <template v-else-if="type != 'vdm' && !edit">
              <div class="nav-item-icon">
                <span class="avatar-bg">
                  <dss-avatar
                    inline
                    size="small"
                    cycle
                    :nt="getAvatarNt"
                    class="avatar"
                  />
                </span>
                {{ hasOwner(summary.owner) ? summary.owner_name : (fullName || "") }}
              </div>
            </template>
            <template v-else-if="type == 'vdm' && !edit">
              <div style="display: flex;">
                <div
                  v-for="owner in ownerList"
                  :key="owner.nt"
                  class="nav-item-icon"
                  style="width: auto; margin-right: 10px;"
                >
                  <span class="avatar-bg">
                    <dss-avatar
                      inline
                      size="small"
                      cycle
                      :nt="owner.nt"
                      class="avatar"
                    />
                  </span>
                  {{ owner.name }}
                </div>
              </div>
            </template>
            <template v-else>
              <el-tag
                v-for="tag in ownerList"
                :key="tag.nt"
                closable
                :disable-transitions="false"
                @close="handleOwnerClose(tag)"
              >
                {{ tag.name }}
              </el-tag>
              <el-autocomplete
                v-if="inputOwnerVisible"
                ref="saveOwnerInput"
                v-model="newOwnerName"
                class="inline-input"
                style="margin-left: 10px;"
                :fetch-suggestions="userSearchHandler"
                :trigger-on-focus="false"
                :debounce="debounce"
                @select="handleSelect"
              />
              <el-button
                v-else
                class="button-new-tag"
                @click="showOwnerInput"
              >
                + New Owner
              </el-button>
            </template>
          </td>
        </tr>
        <!-- SAE -->
        <tr v-if="type != 'vdm'">
          <th>SAE</th>
          <td>
            <span>{{ saeFullName || "" }}</span>
          </td>
        </tr>
        <!-- Team -->
        <tr v-if="type == 'vdm'">
          <th>Team</th>
          <td>
            <template v-if="!edit">
              <span>{{ summary.team_name || "" }}</span>
            </template>
            <template v-else>
              <el-input v-model="team_name" />
            </template>
          </td>
        </tr>
        <!-- DL -->
        <tr>
          <th>Team DL</th>
          <td>
            <template v-if="type != 'vdm' || !edit">
              <a
                :href="`mailTo:${summary.team_dl}`"
                target="_blank"
              >{{ summary.team_dl || "" }}</a>
            </template>
            <template v-else>
              <el-input v-model="team_dl" />
            </template>
          </td>
        </tr>
        <tr />
        <!-- Labels -->
        <tr>
          <th>Labels</th>
          <td>
            <template v-if="!edit">
              <el-tag
                v-for="tag in dynamicLabels"
                :key="tag"
              >
                {{ tag }}
              </el-tag>
            </template>
            <template v-else>
              <el-tag
                v-for="tag in dynamicLabels"
                :key="tag"
                :closable="closable(tag)"
                @close="handleClose(tag)"
              >
                {{ tag }}
              </el-tag>
              <el-input
                v-if="inputVisible"
                ref="saveTagInput"
                v-model="inputValue"
                class="input-new-tag"
                size="small"
                @keyup.enter.native="handleInputConfirm"
                @blur="handleInputConfirm"
              />
              <el-button
                v-if="!inputVisible"
                class="button-new-tag"
                @click="showInput"
              >
                + New Label
              </el-button>
            </template>
          </td>
        </tr>
        <tr />
        <tr v-if="isLkpTable">
          <th>Platform</th>
          <td>
            {{ platform }}
          </td>
        </tr>
        <template v-if="!isLkpTable">
          <tr>
            <th>Usage for last 30 days</th>
            <td>
              <span title="# of unique users and batch accouts for latest 30 days in all platforms">{{ getAllPlatformUsage(summary) }}</span>
              <i
                class="down-icon zeta-icon-download"
                title="download detail user&batch list"
                @click="downloadUsage"
              />
            </td>
          </tr>
          <tr>
            <th style="font-size: 18px; font-weight: 700;">
              Platform
            </th>
          </tr>
          <tr v-if="type != 'vdm' || !edit">
            <td
              colspan="2"
              style="padding-left: 0;"
            >
              <el-radio-group
                v-model="platform"
                size="small"
              >
                <el-radio-button
                  v-for="item in platformArr"
                  :key="item"
                  :label="item"
                  border
                >
                  {{ item.replace("NuMozart", "Mozart") }}
                </el-radio-button>
              </el-radio-group>
            </td>
          </tr>
          <tr v-if="type == 'vdm' && edit">
            <td
              colspan="2"
              style="padding-left: 0;"
            >
              <el-checkbox-group
                v-model="vdmPlatform"
                size="small"
                :min="1"
              >
                <el-checkbox-button
                  v-for="item in AllPlatform"
                  :key="item"
                  :label="item"
                  border
                >
                  {{ item.replace("NuMozart", "Mozart") }}
                </el-checkbox-button>
              </el-checkbox-group>
            </td>
          </tr>
          <tr>
            <th valign="top">
              Table
            </th>
            <td>
              <ul class="sub-item-list">
                <li
                  v-for="item in tableArr"
                  :key="item.db_name"
                >
                  {{ (item.db_name != undefined && item.db_name != "" ? item.db_name.toUpperCase() : "DEFAULT") + "." + (item.table_name != undefined ? item.table_name.toUpperCase() : "") }}
                  <span class="popularity">
                    <span
                      v-if="getPartition(summary) != ''"
                      style="margin-left: 20px;"
                    >{{ getPartition(summary) }}</span>
                    <span
                      v-if="getIndex(summary) != ''"
                      style="margin-left: 20px;"
                    >{{ getIndex(summary) }}</span>
                    <span
                      v-if="getBucket(summary) != ''"
                      style="margin-left: 20px;"
                    >{{ getBucket(summary) }}</span>
                    <i
                      class="zeta-icon-hot"
                      style="margin-left: 20px;"
                    >
                      <span style="margin-left: 5px; font-family: 'ArialMT', 'Arial';">{{ getPopularity(item) }}</span>
                    </i>
                  </span>
                </li>
              </ul>
            </td>
          </tr>
          <tr>
            <th valign="top">
              View
            </th>
            <td>
              <ul class="sub-item-list">
                <li
                  v-for="item in viewArr"
                  :key="item.view_db"
                >
                  <span>{{ (item.view_db != undefined && item.view_db != "" ? item.view_db.toUpperCase() : "DEFAULT") + "." + (item.view_name != undefined ? item.view_name.toUpperCase() : "") }}</span>
                  <i
                    v-if="item.view_db.toUpperCase() == 'BATCH_VIEWS'"
                    class="zeta-icon-info"
                    title="Batch view only serve for data development purpose, no analytics usage allowed."
                  />
                  <span class="popularity">
                    <i
                      class="zeta-icon-hot"
                      style="margin-left: 10px;"
                    >
                      <span style="margin-left: 5px; font-family: 'ArialMT', 'Arial';">{{ getPopularity(item) }}</span>
                    </i>
                  </span>
                </li>
              </ul>
            </td>
          </tr>
          <tr>
            <th class="separator-line" />
            <td class="separator-line" />
          </tr>
          <tr v-if="type != 'vdm'">
            <th>Unique Batch Usage</th>
            <td>
              <span>{{ summary.distinct_batch_cnt || "" }}</span>
            </td>
          </tr>
          <tr>
            <th>Unique User Usage</th>
            <td>
              <span>{{ summary.distinct_user_cnt || "" }}</span>
            </td>
          </tr>
          <tr v-if="getVisibleByPlatform != 'hadoop'">
            <th>Table Size</th>
            <td>
              <span>{{ getTableSize(summary.table_size) || "" }}</span>
            </td>
          </tr>
          <!--tr v-if="getVisibleByPlatform == 'td' && type != 'vdm'">
            <th>Batch Account</th>
            <td>
              <span>{{ getBatchAccount(summary) }}</span>
            </td>
          </tr>
          <tr v-if="getVisibleByPlatform == 'td' && type != 'vdm'">
            <th valign="top">
              ETL Script
            </th>
            <td>
              <ul class="sub-item-list">
                <li
                  v-for="item in summary.job"
                  :key="item.job_name+item.script_name"
                >
                  <span
                    :title="item.script_name"
                    style="cursor: pointer;"
                  >{{ item.job_name }}</span>
                </li>
              </ul>
            </td>
          </tr-->
          <tr v-if="type == 'vdm'">
            <th>Github link</th>
            <td>
              <span>{{ summary.git_link || "" }}</span>
            </td>
          </tr>
          <tr v-if="type == 'vdm'">
            <th>Script</th>
            <td>
              <span>{{ summary.script || "" }}</span>
            </td>
          </tr>
          <!--tr v-if="getVisibleByPlatform != 'hadoop'">
            <th>Hadoop Format</th>
            <td>
              <span />
            </td>
          </tr-->
        </template>
      </table>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch, Inject } from 'vue-property-decorator';
import _ from 'lodash';
import Util from '@/services/Util.service';
import { quillEditor } from 'vue-quill-editor';
import SqlHighlight from '@/components/common/SqlHighlight.vue';
import UserInfoRemoteService from '@/services/remote/UserInfo';
import DoeRemoteService from '@/services/remote/DoeRemoteService';
import conf from './../metadata-config';
import { copy2Clipboard } from '@/components/Metadata/utils';
import { attempt } from '@drewxiu/utils';

const EDITOR_OPTION = {
  modules: {
    toolbar: [
      [{ size: ['small', false, 'large'] }],
      ['bold', 'italic'],
      [{ list: 'ordered' }, { list: 'bullet' }],
      ['link', 'image'],
    ],
  },
};

@Component({
  components: {
    quillEditor,
    SqlHighlight,
  },
})
export default class MetadataSummary extends Vue {
  @Inject('userInfoRemoteService')
  userInfoRemoteService: UserInfoRemoteService;

  @Inject('doeRemoteService')
  doeRemoteService: DoeRemoteService;

  @Prop() metadataTableDict: any;
  @Prop() metadataViewDict: any;
  @Prop() edit: boolean;
  @Prop() type: string;
  @Prop() vdmAllPlatform: any;
  @Prop() labels: any;
  @Prop() restrictedLabels: any;
  @Prop() unrestrictedLabels: any;

  editorOptions = EDITOR_OPTION;
  debounce = 500;
  temp_summary: any | undefined;
  temp_metadataDict: any = {};
  fullName = '';
  saeFullName = '';
  showMoreFlag = false;
  platform = '';
  platformArr: Array<any> = [];
  tableArr: Array<any> = [];
  viewArr: Array<any> = [];
  frequencyOptions = ['Daily', 'Weekly', 'Bi-Weekly', 'Monthly'];
  vdmPlatform: Array<any> = [];
  team_dl = '';
  team_name = '';
  dynamicLabels: any = this.labels || [];
  inputValue = '';
  inputVisible = false;
  currentNt: string = Util.getNt();
  currentName = '';
  ownerSel: any = false;
  confidenceScore: any = 0.0;
  rate = 0;
  usage = '';
  availability = '';
  dq = '';
  governance = '';
  usageToolTip = '';
  availabilityToolTip = '';
  dqToolTip = '';
  governanceToolTip = '';
  visibleArrow = false;
  showDDL = false;
  ownerList: any = [];
  inputOwnerVisible = false;
  newOwnerName = '';

  get summary () {
    this.temp_summary = _.cloneDeep(_.find(this.metadataTableDict, (v: any) => { return _.toLower(v.platform) == _.toLower(this.platform); }));
    const pickView: any = _.pickBy(this.metadataViewDict, (v: any) => { return _.toLower(v.platform) == _.toLower(this.platform); });
    if (_.isUndefined(this.temp_summary)) {
      this.temp_summary = {};
      this.fullName = '';
      this.saeFullName = '';
      this.tableArr = [];
      _.forEach(pickView, (v: any) => {
        if (_.isUndefined(this.temp_summary.table_desc)) this.temp_summary.table_desc = v.view_desc;
        if (_.isUndefined(this.temp_summary.product_owner)) this.temp_summary.product_owner = v.dev_manager;
        if (_.isUndefined(this.temp_summary.subject_area)) this.temp_summary.subject_area = v.subject_area;
        if (_.isUndefined(this.temp_summary.primary_dev_sae)) this.temp_summary.primary_dev_sae = v.primary_dev_sae;
        if (_.isUndefined(this.temp_summary.team_dl)) this.temp_summary.team_dl = v.team_dl;
        if (_.isUndefined(this.temp_summary.distinct_batch_cnt)) this.temp_summary.distinct_batch_cnt = v.distinct_batch_cnt;
        if (_.isUndefined(this.temp_summary.distinct_user_cnt)) this.temp_summary.distinct_user_cnt = v.distinct_user_cnt;
      });
    } else {
      const pickTable: any = _.pickBy(this.metadataTableDict, (v: any) => { return _.toLower(v.platform) == _.toLower(this.platform); });
      this.tableArr = _.sortBy(pickTable, [(v: any) => -(v.distinct_batch_cnt + v.distinct_user_cnt)]);
    }

    this.fullNameByNt(this.type == 'vdm' ? (this.temp_summary.owner_nt || '') : (this.temp_summary.product_owner || ''));
    this.saeFullNameByNt(this.temp_summary.primary_dev_sae || '');
    this.viewArr = _.sortBy(pickView, [(v: any) => -(v.distinct_batch_cnt + v.distinct_user_cnt)]);
    if (this.type == 'vdm' && this.vdmPlatform.length == 0) this.vdmPlatform = this.platformArr;
    this.temp_metadataDict = _.cloneDeep(this.temp_summary);
    this.team_dl = this.temp_summary.team_dl;
    this.team_name = this.temp_summary.team_name;
    this.ownerSel = _.isEmpty(this.temp_summary.owner) ? (this.temp_summary.product_owner || '') : this.temp_summary.owner;
    this.ownerList = this.temp_summary.ownerList;
    return this.temp_summary;
  }

  get getAvatarNt () {
    let nt = '';

    if (this.type != 'vdm' && (this.summary.owner && !_.isEmpty(this.summary.owner))) {
      nt = this.summary.owner;
    } else if (this.type != 'vdm' && this.summary.product_owner && !_.isEmpty(this.summary.product_owner)) {
      nt = this.summary.product_owner;
    } else {
      nt = this.summary.owner_nt;
    }
    return nt;
  }

  get isLkpTable () {
    return attempt(() => this.summary.asset_type === 'realtime', false);
  }

  get getVisibleByPlatform (): string {
    if (_.indexOf(conf.hadoop_platform, this.platform) > -1) {
      return 'hadoop';
    }
    if (_.indexOf(conf.td_platform, this.platform) > -1) {
      return 'td';
    }
    return '';
  }

  get AllPlatform (): Array<any> {
    if (this.vdmAllPlatform && this.vdmAllPlatform.length > 0) {
      return _.intersection(conf.allPlatform, _.uniq(this.vdmAllPlatform));
    } else {
      return [this.platform];
    }
  }

  hasOwner (nt: string) {
    return nt && !_.isEmpty(nt);
  }

  getCurrentFullName () {
    this.userInfoRemoteService.getBaseInfo(Util.getNt()).then((res: any) => {
      const userInfo = res.data.response.docs[0];
      this.currentName =
        res.data.response.docs[0].first_name +
          ' ' +
          res.data.response.docs[0].last_name || Util.getNt();
    })
      .catch(err => {
        this.currentName = Util.getNt();
      });
  }

  fullNameByNt (nt: string) {
    this.userInfoRemoteService.getBaseInfo(nt)
      .then((res: any) => {
        const userInfo = res.data.response.docs[0];
        this.fullName =
          res.data.response.docs[0].first_name +
            ' ' +
            res.data.response.docs[0].last_name || nt;
      })
      .catch(err => {
        this.fullName = nt;
      });
  }

  saeFullNameByNt (nt: string) {
    this.userInfoRemoteService.getBaseInfo(nt)
      .then((res: any) => {
        const userInfo = res.data.response.docs[0];
        this.saeFullName =
          res.data.response.docs[0].first_name +
            ' ' +
            res.data.response.docs[0].last_name || nt;
      })
      .catch(err => {
        this.saeFullName = nt;
      });
  }

  userSearchHandler (queryStr: string, cb: any) {
    console.debug('user search ' + queryStr);
    this.temp_summary.owner_name = '';
    this.temp_summary.owner_nt = '';
    const opntions: Array<any> = [];
    this.doeRemoteService.getUser(queryStr).then(res => {
      if (res && res.data && res.data.response && res.data.response.docs) {
        _.forEach(res.data.response.docs, (v: any) => {
          const option: any = {
            nt: v.nt,
            value: (v.last_name ? v.last_name : '') +
                  (v.last_name && v.preferred_name ? ',' : '') +
                  (v.preferred_name ? v.preferred_name : ''),
          };

          opntions.push(option);
        });
      }
      cb(opntions);
    }).catch(err => {
      console.error('user search failed: ' + JSON.stringify(err));
      cb([]);
    });
  }

  closable (tag: string) {
    const findRestricted = _.find(this.restrictedLabels, (v) => { return v == tag; } );
    const findUnrestricted = _.find(this.unrestrictedLabels, (v) => { return v == tag; } );
    if (findRestricted && !findUnrestricted) return false;
    return true;
  }

  async copy2Clipboard (str: string) {
    await copy2Clipboard(str);
    this.$message.success('Successfully copied');
  }
  // handleInput() {
  //   this.temp_summary.owner_name = '';
  //   this.temp_summary.owner_nt = '';
  // }

  handleSelect (item: any) {
    // this.temp_summary.owner_name = item.value;
    // this.temp_summary.owner_nt = item.nt;
    this.ownerList.push({nt: item.nt, name: item.value});
    this.newOwnerName = '';
    this.inputOwnerVisible = false;
  }

  constructor () {
    super();

    const rs: any = [];
    if (this.metadataTableDict && !_.isEmpty(this.metadataTableDict)) {
      const arr = _.uniq(_.map(this.metadataTableDict, 'platform'));
      _.forEach(arr, (v: any) => {
        rs.push(v.toLowerCase().replace(/( |^)[a-z]/g, (L: any) => L.toUpperCase()).replace('mozart', 'Mozart'));
      });
    }

    if (this.metadataViewDict && !_.isEmpty(this.metadataViewDict)) {
      const arr = _.uniq(_.map(this.metadataViewDict, 'platform'));
      _.forEach(arr, (v: any) => {
        rs.push(v.toLowerCase().replace(/( |^)[a-z]/g, (L: any) => L.toUpperCase()).replace('mozart', 'Mozart'));
      });
    }
    this.platformArr = _.intersection(conf.allPlatform, _.uniq(rs));
    this.platform = !_.isEmpty(this.platformArr) ? this.platformArr[0] : '';
    this.getCurrentFullName();
  }

  mounted () {
    const param: any = {
      platform: this.platform,
      table: this.summary['table_name'] || '',
    };
    //this.getConfidenceScore(param);
  }

  getBatchAccount (summary: any): string {
    let batchAccount = '';
    if (!_.isUndefined(summary.job) && !_.isEmpty(summary.job)) {
      batchAccount = summary.job[0]['batch_account'] || '';
    }
    return _.toUpper(batchAccount);
  }

  getPopularity (item: any): any {
    let rs: any = 0;
    if (item.distinct_batch_cnt) rs = item.distinct_batch_cnt;
    if (item.distinct_user_cnt) rs = rs ? rs + item.distinct_user_cnt : item.distinct_user_cnt;
    return rs;
  }

  getAllPlatformUsage (item: any) {
    let rs: any = 0;
    if (item.all_distinct_batch_cnt) rs = item.all_distinct_batch_cnt;
    if (item.all_distinct_user_cnt) rs = rs ? rs + item.all_distinct_user_cnt : item.all_distinct_user_cnt;
    return rs;
  }

  getPartition (summary: any): string {
    let partition = '';
    if (!_.isUndefined(summary.column) && !_.isEmpty(summary.column)) {
      const pick: any = _.pickBy(summary.column, (v: any) => { return v.ppi_flag == 'Y'; });
      const columnNames: any = [];
      _.forEach(pick, (v: any) => {
        columnNames.push(v.column_name);
      });
      if (!_.isEmpty(columnNames)) {
        partition = 'Partition: ' + _.join(columnNames, ',');
      }
    }
    return partition;
  }

  getBucket (summary: any): string {
    let partition = '';
    if (!_.isUndefined(summary.column) && !_.isEmpty(summary.column)) {
      const pick: any = _.pickBy(summary.column, (v: any) => { return v.bucket_flag == 'Y'; });
      const columnNames: any = [];
      _.forEach(pick, (v: any) => {
        columnNames.push(v.column_name);
      });
      if (!_.isEmpty(columnNames)) {
        partition = 'Bucket: ' + _.join(columnNames, ',');
      }
    }
    return partition;
  }

  getIndex (summary: any): string {
    let partition = '';
    if (!_.isUndefined(summary.column) && !_.isEmpty(summary.column)) {
      const pick: any = _.pickBy(summary.column, (v: any) => { return v.index_flag == 'Y'; });
      const columnNames: any = [];
      _.forEach(pick, (v: any) => {
        columnNames.push(v.column_name);
      });
      if (!_.isEmpty(columnNames)) {
        partition = 'Index: ' + _.join(columnNames, ',');
      }
    }
    return partition;
  }

  changePlatform (platform: any) {
    this.platform = platform;
    const sys: any = {
      platform: platform,
      dbName: this.summary()['db_name'] || '',
    };
    this.getConfidenceScore(this.summary()['table_name'] );
    this.$emit('system-change', sys);
  }

  getConfidenceScore (table: string) {
    this.rate = 0;
    this.confidenceScore = (0).toFixed(1);
    this.usage = 'unknown';
    this.usageToolTip = 'No usage';
    this.availability = 'unknown';
    this.availabilityToolTip = 'No daily jobs found in DOE metadata';
    this.dq = 'unknown';
    this.dqToolTip = 'No DQ rule';
    this.governance = 'unknown';
    this.governanceToolTip = 'No data model';
    if (!table) {
      return;
    }
    this.doeRemoteService.getConfidenceScore(table).then((res: any) => {
      if (res && res.data && res.data.data && res.data.data.value) {
        if (res.data.data.value.length > 0 ) {
          this.rate = isNaN(res.data.data.value[0]['score']) ? 0 : (res.data.data.value[0]['score'] / 2);
          this.confidenceScore = isNaN(res.data.data.value[0]['score']) ? (0).toFixed(1) : res.data.data.value[0]['score'].toFixed(1);
          this.usage = res.data.data.value[0]['usage_cfd'] || 'unknown';
          if (this.usage == 'unknown' || this.usage == 'critical') {
            this.usageToolTip = 'No usage';
          } else if (this.usage == 'warning') {
            this.usageToolTip = 'Low usage';
          } else if (this.usage == 'high') {
            this.usageToolTip = 'High usage';
          } else {
            this.usageToolTip = '';
          }
          this.availability = res.data.data.value[0]['avail_cfd'] || 'unknown';
          if (this.availability == 'unknown') {
            this.availabilityToolTip = 'No daily jobs found in DOE metadata';
          } else if (this.availability == 'critical' || this.usage == 'warning') {
            this.availabilityToolTip = 'Job finish time of this table is not stable for recent 30 days';
          } else {
            this.availabilityToolTip = '';
          }
          this.dq = res.data.data.value[0]['dq_cfd'] || 'unknown';
          if (this.dq == 'unknown') {
            this.dqToolTip = 'No DQ rule';
          } else if (this.dq == 'critical') {
            this.dqToolTip = 'DQ rule is failed';
          } else {
            this.dqToolTip = '';
          }
          this.governance = res.data.data.value[0]['govn_cfd'] || 'unknown';
          if (this.governance == 'unknown' || this.governance == 'critical' || this.governance == 'warning') {
            this.governanceToolTip = 'No data model';
          } else {
            this.governanceToolTip = '';
          }
        }
      }
    }).catch(err => {
      console.error('Call Api:getConfidenceScore failed: ' + JSON.stringify(err));
    });
  }

  revert () {
    this.temp_summary.table_desc = this.temp_metadataDict.table_desc;
    if (this.type == 'vdm') {
      this.temp_summary.owner_name = this.temp_metadataDict.owner_name;
      this.temp_summary.owner_nt = this.temp_metadataDict.owner_nt;
      this.temp_summary.team_name = this.temp_metadataDict.team_name;
      this.temp_summary.team_dl = this.temp_metadataDict.team_dl;
      this.temp_summary.platforms = this.temp_metadataDict.platforms;
      this.team_dl = this.temp_metadataDict.team_dl;
      this.team_name = this.temp_metadataDict.team_name;
      this.ownerList = this.temp_metadataDict.ownerList;
    }
  }

  saveVdm () {
    this.temp_summary.team_name = this.team_name;
    this.temp_summary.team_dl = this.team_dl;
  }

  save () {
    const txtDesc: string = this.temp_summary.table_desc ? this.temp_summary.table_desc.replace(/<[^>]*>|/g, '') : '';
    const htmlDesc: string = this.temp_summary.table_desc;
  }
  getTableSize (tableSize: any) {
    if (_.isNumber(tableSize)) {
      const reg = /\d{1,3}(?=(\d{3})+$)/g;
      return (tableSize + '').replace(reg, '$&,') + ' byte';
    }

    return '';
  }

  handleClose (tag: any) {
    this.dynamicLabels.splice(this.dynamicLabels.indexOf(tag), 1);
  }

  handleOwnerClose (tag: any) {
    this.ownerList.splice(this.ownerList.indexOf(tag), 1);
  }

  showInput () {
    this.inputVisible = true;
    this.$nextTick(() => {
      const saveTagInput: any = this.$refs.saveTagInput;
      if (saveTagInput) saveTagInput.$refs.input.focus();
    });
  }

  showOwnerInput () {
    this.inputOwnerVisible = true;
    this.$nextTick(() => {
      const saveOwnerInput: any = this.$refs.saveOwnerInput;
      if (saveOwnerInput) saveOwnerInput.$refs.input.focus();
    });
  }

  handleInputConfirm () {
    const inputValue = this.inputValue;
    if (inputValue) {
      this.dynamicLabels.push(inputValue);
    }
    this.inputVisible = false;
    this.inputValue = '';
  }

  downloadUsage () {
    this.$emit('download-usage');
  }

  @Watch('labels')
  onLabelsChange () {
    this.dynamicLabels = this.labels;
  }

  @Watch('platform')
  onPlatformChange (val: any) {
    const sys: any = {
      platform: val,
      dbName: this.summary['db_name'] || '',
    };
    const param: any = {
      platform: val,
      table: this.summary['table_name'] || '',
    };
    //this.getConfidenceScore(param);
    this.$emit('system-change', sys);
  }
}
</script>

<style lang="scss" scoped>
/deep/ .ql-snow {
  .ql-formats,
  .ql-toolbar,
  &.ql-toolbar {
    &:after {
      display: block !important;
    }
  }
}
.ql-editor {
  padding-left: 0px;
}
.el-radio-button {
    box-shadow: none !important;
    /deep/ .el-radio-button__inner {
        background: inherit;
        background-color: #fff;
        border: 1px solid #569ce1 !important;
        border-radius: 4px !important;
        box-shadow: none !important;
        color: #569ce1;
        font-size: 14px;
        height: 30px;
        line-height: 10px;
        margin-right: 10px;
        min-width: 90px;
    }
    /deep/ .el-radio-button__orig-radio:checked + .el-radio-button__inner:hover {
      background-color:#4d8cca;
      border: 1px solid #4d8cca !important;
      color: #fff;
    }

    /deep/ .el-radio-button__inner:hover {
      border: 1px solid #4d8cca !important;
      color: #4d8cca;
    }
}
.el-checkbox-button {
    /deep/ .el-checkbox-button__inner {
        background: inherit;
        background-color: #fff;
        border: 1px solid #569ce1 !important;
        border-radius: 4px !important;
        box-shadow: none;
        color: #569ce1;
        font-size: 14px;
        height: 30px;
        line-height: 10px;
        margin-right: 10px;
        min-width: 90px;
    }
    /deep/ .el-checkbox-button__inner:hover {
        border: 1px solid #4d8cca !important;
        color: #4d8cca;
    }
}
.el-checkbox-button.is-checked /deep/.el-checkbox-button__inner {
    background-color: #569ce1;
    color: #fff;
}
.el-checkbox-button.is-checked /deep/.el-checkbox-button__inner:hover {
    background-color:#4d8cca !important;
    border: 1px solid #4d8cca !important;
    color: #fff;
}
.overview {
  > div {
    margin-bottom: 30px;
    h3 {
      margin-bottom: 10px;
    }
  }
  .metadata {
    table.display {
      border-spacing: 0 20px;
      width: 100%;
      > tr {
        min-height: 35px;
        padding: 5px 0;
        > th,
        > td {
          padding: 0;
        }
        > th {
          text-align: left;
        }
        > td {
          padding-left: 30px;
          color: #333;
        }
      }
    }
  }
}
.nav-item-icon {
  align-items: center;
  display: flex;
  justify-content: left;
  width: 200px;
  [class^="zeta-icon-"],
  [class*=" zeta-icon-"] {
    color: #94bfe1;
    font-size: 24px;
  }
  .avatar-bg {
    background-position: center;
    background-size: 30px 30px;
    border-radius: 15px;
    display: block;
    height: 30px;
    margin-right: 5px;
    width: 30px;
  }
  .avatar {
    background-position: center;
    background-size: 30px 40px;
    border-radius: 15px;
    display: block;
    height: 30px;
    width: 30px;
  }
}
ul.sub-item-list {
  display: inline-block;
  list-style-type: none;
  margin-top: -5px;
  max-height: 250px;
  overflow-y: auto;
  width: 100%;
  > li {
    margin: 5px 0;
  }
}
.show-more {
  color: #569ce1;
  cursor: pointer;
  line-height: normal;
  font-size: 13px;
  font-weight: 400;
  text-decoration: underline;
}
.show-more:hover {
  color: #4d8cca;
}
.popularity {
  font-weight: 700;
}
.popularity, .popularity > i {
  color: #CACBCF;
  font-size: 14px;
  height: 16px;
}
.zeta-icon-hot {
  font-weight: 400;
}
.separator-line {
  border-bottom: 1px solid #f2f2f2;
  height: 1px;
}
.zeta-icon-info {
  padding-left: 5px;
  cursor: pointer;
  color: #E53917;
  font-weight: bold;
}
.el-tag + .el-tag {
  margin-left: 10px;
}
.button-new-tag {
  margin-left: 10px;
  height: 32px;
  line-height: 30px;
  padding-top: 0;
  padding-bottom: 0;
}
.input-new-tag {
  width: 90px;
  margin-left: 10px;
  vertical-align: bottom;
  /deep/ .el-input__inner {
    width: 110px;
  }
}
.icon-label {
  display: inline-block;
}
.icon-score {
  height: 16px;
  width: 16px;
  float: right;
  display: inline-block;
}
.high {
 background-color: #52A552;
}
.normal {
 background-color: #5CB85C;
}
.warning {
 background-color: #F3AF2B;
}
.critical {
 background-color: #E53917;
}
.unknown {
 background-color: #F3F4F6;
}
.el-tag.el-icon-star-on {
  font-size: 16px;
}
.el-tag.el-icon-star-on:before {
  margin-right: 5px;
}
.zeta-icon-download {
  cursor: pointer;
  margin-left: 5px;
}
</style>
