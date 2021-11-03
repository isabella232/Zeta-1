<template>
  <div style="height: 100%; width: 100%;">
    <div class="legend-div">
      <div style="display: flex; margin: 10px 0;">
        <div class="icon-job-status pass" />
        <span class="icon-label">Job Ready</span>
      </div>
      <div style="display: flex; margin: 10px 0;">
        <div class="icon-job-status fail" />
        <span class="icon-label">Job Not Ready</span>
      </div>
      <div style="display: flex; margin: 10px 0;">
        <div class="icon-job-status not-job" />
        <span
          class="icon-label"
          title="Could not find jobs of this table from DOE metadata"
        >Unknown</span>
      </div>
    </div>
    <div class="zoom-icon-div">
      <i
        class="zoom-icon el-icon-zoom-in"
        @click="clkZoomIn"
      />
      <i
        class="zoom-icon el-icon-zoom-out"
        @click="clkZoomOut"
      />
      <i
        class="zoom-icon el-icon-refresh"
        @click="onDbChange"
      />
    </div>
    <div
      v-if="!svgVisible"
      ref="lineageArea"
      class="lineage-svg"
    />
    <div
      v-if="svgVisible"
      style="text-align: center; padding-top: 10px;"
    >
      No lineage
    </div>
    <el-card
      class="table-info-div"
      shadow="never"
    >
      <div
        class="table-head"
        @click="visible = !visible"
      >
        <span v-if="activeNode.name">{{ activeNode.name }}</span>
        <span
          v-if="activeNode.name"
          style="color: #999;"
        > (Status of {{ platform }} on {{ date }})</span>
        <i
          class="detail-icon"
          :class="visible ? 'el-icon-caret-bottom' : 'el-icon-caret-top'"
        />
      </div>
      <div class="table-div">
        <el-table
          v-if="visible"
          border
          :data="activeNode.basicInfo"
        >
          <el-table-column
            property="subject_area"
            label="SA"
          />
          <el-table-column
            property="owner_name"
            label="Owner"
          />
          <el-table-column
            property="primary_dev_sae"
            label="SAE"
          />
          <el-table-column
            property="avg_upd_time"
            label="Avg. Table ready time"
            :formatter="avgReadyTimeFormatter"
          />
          <el-table-column
            property="usage_all"
            label="Usage"
          />
        </el-table>
      </div>
    </el-card>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch, Inject } from 'vue-property-decorator';
import DoeRemoteService from '@/services/remote/DoeRemoteService';
import UserInfoRemoteService from '@/services/remote/UserInfo';
import DagRender from '@/components/common/dag-render-v2/';
import { initSvg, renderPlanViz, getActiveNode, zoomIn, zoomOut } from '.';
import { ZetaException, ExceptionPacket } from '@/types/exception';
import moment from 'moment';
import * as d3 from 'd3';
import _ from 'lodash';
import $ from 'jquery';
import conf from './../metadata-config';
import { EventBus } from '@/components/EventBus';
@Component({
  components: {
    DagRender
  }
})
export default class MetadataLineage extends Vue {
  @Inject('doeRemoteService')
  doeRemoteService: DoeRemoteService;
  @Inject('userInfoRemoteService')
  userInfoRemoteService: UserInfoRemoteService;
  @Prop() tableName: any;
  @Prop() metadataTableDict: any;
  platform = '';
  db = '';
  platformOptions: any = [];
  dbOptions: any = [];
  linksData: any = [];
  nodesData: any = [];
  activeNode: any = {};
  visible = true;
  svgVisible = false;
  date = '';
  constructor() {
    super();
    /*this.platformOptions = [];
    if (this.metadataTableDict && !_.isEmpty(this.metadataTableDict)) {
      const arr = _.uniq(_.map(this.metadataTableDict, "platform"));
      _.forEach(conf.lineage_platform, (v: any) => {
        const find = _.find(arr, (sv: any) => { return _.toLower(sv) == _.toLower(v) });
        if (find) this.platformOptions.push({ label: v, value: _.toLower(v)});
      });
    }
    this.platform = !_.isEmpty(this.platformOptions) ? this.platformOptions[0].value : "";*/
  }
  mounted() {
    //if (this.platform != '') this.setDb(this.platform);
    this.init();
    this.getServerCurrent();
    this.getMainSourceTarget();
    EventBus.$on('set-lineage-clk-node', (data: any) => {
      _.forEach(data.basicInfo, (sv: any) => {
        const table = data.name.split('.').pop();
        this.doeRemoteService.getConfidenceScore(table).then(({ data }) => {
          const batchCount = _.get(data, 'data.value[0].all_platform_batch_cnt', 0);
          const userCount = _.get(data, 'data.value[0].all_platform_user_cnt', 0);
          Vue.set(sv, 'usage_all', batchCount + userCount);
        });
        this.getFullNameByNt(sv.primary_dev_sae).then((rs: any) => {
          sv.primary_dev_sae = rs;
        });
        if (!(sv.owner_name && !_.isEmpty(sv.owner_name))) {
          this.getFullNameByNt(sv.dev_manager).then((rs: any) => {
            sv.owner_name = rs;
          });
        }
      });
      this.activeNode = data;
    });
  }
  init() {
    this.svgVisible = false;
    const $el = this.$refs.lineageArea;
    if (!$el) {
      return;
    }
    initSvg($el);
  }
  getServerCurrent() {
    this.doeRemoteService.getServerCurrent().then((res: any) => {
      if (res && res.data && res.data.data && res.data.data.value && res.data.data.value[0]) {
        this.date = moment(res.data.data.value[0].datetime).format('YYYY.MM.DD');
      } else {
        this.date = moment.utc().utcOffset('-08:00').format('YYYY.MM.DD');
      }
    }).catch(err => {
      console.log(err);
      this.date = moment.utc().utcOffset('-08:00').format('YYYY.MM.DD');
    });
  }
  getMainSourceTarget() {
    const params: any = {
      table: _.toLower(this.tableName),
      depth: 2
    };
    this.doeRemoteService.getMainSourceTarget(params).then((res: any) => {
      const linksRs: any = [];
      const nodesRs: any = [];
      if (res && res.data && res.data.status == 500) {
        this.svgVisible = true;
        return;
      }
      if (res && res.data && res.data.src && res.data.src.value) {
        const find: any = _.find(res.data.src.value, (v: any) => { return v.table_name == params.table; });
        this.db = find ? find.db_name : '';
        this.platform = find && find.platform ? (_.toLower(find.platform) == 'numozart' ? 'Mozart' : _.upperFirst(_.toLower(find.platform))) : '';
        const srcArr: any = _.concat(_.filter(res.data.src.value, (v: any) => { return v.table_name == params.table && v.down_table == null; }),
          _.filter(res.data.src.value, (v: any) => { return v.down_table == params.table && v.down_db == this.db; }));
        _.forEach(srcArr, (v: any) => {
          if (v.down_table != null) {
            const link: any = {
              source: v.db_name + '.' + v.table_name,
              target: v.down_db + '.' + v.down_table,
            };
            linksRs.push(link);
          }
          const info: any = v.info;
          info.srcExpand = _.find(res.data.src.value, (sv: any) => { return sv.down_db == v.db_name && sv.down_table == v.table_name && v.down_table != null && (sv.db_name != this.db || sv.table_name != params.table); }) ? true : false;
          const node: any = {
            name: v.db_name + '.' + v.table_name,
            platform: v.platform,
            db: v.db_name,
            table: v.table_name,
            info: info
          };
          nodesRs.push(node);
        });
      }
      if (res && res.data && res.data.tar && res.data.tar.value) {
        const tarArr: any = _.concat(_.filter(res.data.tar.value, (v: any) => { return v.table_name == params.table && v.up_table == null; }),
          _.filter(res.data.tar.value, (v: any) => { return v.up_table == params.table && v.up_db == this.db; }));
        _.forEach(tarArr, (v: any) => {
          if (v.up_table != null) {
            const link: any = {
              source: v.up_db + '.' + v.up_table,
              target: v.db_name + '.' + v.table_name
            };
            linksRs.push(link);
          }
          const info: any = v.info;
          info.tarExpand = _.find(res.data.tar.value, (sv: any) => { return sv.up_db == v.db_name && sv.up_table == v.table_name && v.up_table != null && (sv.db_name != this.db || sv.table_name != params.table); }) ? true : false;
          const node: any = {
            name: v.db_name + '.' + v.table_name,
            platform: v.platform,
            db: v.db_name,
            table: v.table_name,
            info: info
          };

          nodesRs.push(node);
        });
      }
      this.linksData = linksRs;
      this.nodesData = nodesRs;
      renderPlanViz(this.nodesData, this.linksData, this.db + '.' + params.table);
    }).catch(err => {
      console.log(err);
    });
  }
  setDb(platform: string) {
    const arr = _.map(_.filter(this.metadataTableDict, (v: any) => { return _.toLower(platform) == _.toLower(v.platform); }), 'db_name');
    this.dbOptions = [];
    _.forEachRight(_.sortedUniq(arr), (v: any) => {
      this.dbOptions.push({ label: v == '' ? 'DEFAULT' : _.toUpper(v),
        value: v == '' ? 'default' : _.toLower(v),
        sort: v == '' ? 1 : (_.toLower(v).indexOf('app') == 0 ? 2 : 0) });
    });
    this.dbOptions = _.sortBy(this.dbOptions, ['sort', 'label']);
    this.db = !_.isEmpty(this.dbOptions) ? this.dbOptions[0].value : '';
    this.init();
    this.getMainSourceTarget();
  }
  onDbChange() {
    this.init();
    this.getMainSourceTarget();
  }
  async getFullNameByNt(nt: string) {
    const name = await this.userInfoRemoteService.getBaseInfo(nt)
      .then((res: any) => {
        const userInfo = res.data.response.docs[0];
        return res.data.response.docs[0].first_name +
            ' ' +
            res.data.response.docs[0].last_name || nt;
      })
      .catch(err => {
        return nt;
      });
    return name;
  }
  clkZoomIn() {
    zoomIn();
  }
  clkZoomOut() {
    zoomOut();
  }
  @Watch('getActiveNode')
  onClkNodeChange(val: any) {
    console.log(val);
  }
  @Watch('platform')
  onPlatformChange(val: any) {
    this.setDb(val);
  }
  avgReadyTimeFormatter(_row: any, _col: any, value: string) {
    return value ? value + ' PST' : '';
  }
}
</script>
<style lang="scss" scoped>
.filter-div {
  width: 100%;
  height: 40px;
  padding-top: 10px;
}
.filter-label {
  line-height: 30px;
  margin-right: 5px;
}
.el-select {
  margin-right: 10px;
}
.lineage-svg {
  width: 100%;
  height: calc(100% - 40px);
}
.table-info-div {
  position: absolute;
  bottom: 30px;
  width: calc(100% - 152px);
  overflow-y: hidden;
  /deep/ .el-card__body {
    padding-top: 0;
  }
  .table-head {
    cursor: pointer;
    padding-top: 20px;
    height: 30px;
    line-height: 30px;
  }
  .detail-icon {
    float: right;
    font-size: 20px;
    line-height: 30px;
    cursor: pointer;
  }
  .el-table {
    margin-top: 20px;
  }
  .table-div {
    max-height: 230px;
    overflow: auto;
  }
  .icon-label {
    line-height: 23px;
    margin-right: 5px;
  }
}
$colorSuccess: #020202;
$colorFail: #d9534f;
$link-color: #9a9a9a;
$link-active-color: #1277bf;
$colorHadoop: #FFA500;
svg{
  background-color: #fcfcfc;
}
.icon-job-status {
  height: 16px;
  width: 16px;
  margin-right: 5px;
  border: 1px solid #333;
}
/deep/ .not-ready {
  font-size: 20px;
  fill: #EEEEEE;
  color: #EEEEEE;
}
/deep/ .fail {
  font-size: 20px;
  fill: #EEEEEE;
  color: #EEEEEE;
  background-color: #EEEEEE;
}
/deep/ .pass {
  font-size: 20px;
  fill: #808080;
  color: #808080;
  background-color: #808080;
}
/deep/ .not-job {
  font-size: 20px;
  fill: #FFF;
  color: #FFF;
  background-color: #FFF;
}
/deep/ .partly {
  font-size: 20px;
  fill: #F3AF2B;
  color: #F3AF2B;
}
/deep/ .icon-expand {
  cursor: pointer;
  font-family: "zeta-font" !important;
  font-size: 14px;
  fill: #569CE1;
}
//link
/deep/ .link path
{
  fill: none;
  stroke: $link-color;
}
/deep/ .link polygon {
  fill: $link-color;
  stroke: $link-color;
}
/deep/ .link.highlight path {
  stroke: $link-active-color;
}
/deep/ .link.highlight polygon {
  stroke: $link-active-color;
  fill: $link-active-color;
}
// node
/deep/ .node {
  path.node-rect {
    cursor: pointer;
    fill: #fff;
    stroke: #595959;
    stroke-width: 3px;
  }
  path.node-rect-center {
    cursor: pointer;
    fill: #FFF;
  }
  path.rect-not-ready {
    cursor: pointer;
    fill: #EEE;
  }
  path.rect-pass {
    cursor: pointer;
    fill: #808080;
  }
  path.rect-not-job {
    cursor: pointer;
    fill: #FFF;
  }
  path.rect-partly {
    cursor: pointer;
    fill: #008000;
  }
  text.node-text {
    cursor: pointer;
  }
  path.active {
    stroke: #569CE1 !important;
  }
  text.active {
    fill: #569CE1 !important;
  }
  &.table {
    .node-rect {
      fill: #f2f0f1;
    }
    &.ultimate {
      .node-rect {
        fill: $colorSuccess;
        stroke: $colorSuccess;
      }
      .node-text {
        fill: #f0f0f0;
      }
    }
    &.hadoop {
      .node-rect {
        fill: $colorHadoop;
        stroke: $colorHadoop;
      }
      .node-text {
        fill: #f0f0f0;
      }
    }
  }
  &.highlight {
    .node-rect {
      stroke: #78baee;
    }
  }
}
/deep/ .node-grp.dropdown {
  .node:not(.active):hover .node-rect {
    fill: #d1e4f2;
  }
  .node.active .node-rect {
    stroke: #1277bf;
  }
}
/deep/ .node-grp:not(.dropdown):not(.non-expanded) {
}
/deep/ .node.active{
  .node-rect {
    fill: #1277bf !important;
  }
  .node-text {
    fill: #f0f0f0;
  }
}
/deep/ .delimit {
  stroke: #d3d3d3;
}
/deep/ .node-text {
  cursor: pointer;
  font-size: 13px;
  font-family: Times,serif;
  fill: #333333;
  //text-anchor: middle;
}
// chevron
/deep/ .chevron {
  cursor: pointer;
}
/deep/ .non-expanded .chevron {
}
/deep/ .k-v-box{
  width: 100%;
}
/deep/ .k-v-box .k {
  width: 10%;
  font-weight: 700;
  font-family: Arial,Helvetica,sans-serif;
  font-size: 14px;
  color: #666;
}
/deep/ .k-v-box .v {
  width: 30%;
  font-family: Arial,Helvetica,sans-serif;
  font-size: 14px;
  color: #333;
}
// svg
g.node-grp {
  g.node {
    &.grouped {
      path {
        fill: #1F77B4;
        fill-opacity: .25;
        stroke: #1F77B4;
        stroke-width: 1px;
      }
      &:hover {
        path {
          fill-opacity: .5;
        }
      }
    }
  }
}
path {
  &.job-success {
    stroke: $colorSuccess;
  }
  &.job-fail {
    stroke: $colorFail;
  }
}
polygon {
  &.job-success {
    fill: $colorSuccess;
    stroke: $colorSuccess;
  }
  &.job-fail {
    fill: $colorFail;
    stroke: $colorFail;
  }
}
text.node-colinfo-text {
  fill: #f0ad4e;
  stroke: #f0ad4e;
  stroke-width: 1;
  font-size: 11px;
  word-spacing: 5px;
}
.legend-div {
  display: flex;
  flex-direction: column;
  position: absolute;
  margin: 10px;
  padding: 10px;
  border: 1px solid #333;
  background-color: #FFF;
}
.zoom-icon-div {
  display: flex;
  justify-content: center;
  flex-direction: column;
  position: absolute;
  height: calc(100% - 320px);
  right: 40px;
  .zoom-icon {
    font-size: 30px;
    cursor: pointer;
    margin: 20px 0;
    color: #569CE1;
  }
  .zoom-icon:hover {
    color: #4D8CCA;
  }
}
</style>
