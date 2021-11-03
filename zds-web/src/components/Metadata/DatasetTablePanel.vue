<template>
  <el-row class="dataSet-usage-div">
    <div class="dataSet-div">
      <p class="dataSet-label">
        Integration Data Layer
      </p>
      <el-input
        v-model="search"
        class="search-ipt"
        placeholder="Search"
      />
      <div class="dropdown">
        <span>Label: </span>
        <el-select
          v-model="label"
          multiple
          filterable
          collapse-tags
        >
          <el-option
            v-for="item in getAllLabels"
            :key="item"
            :label="item"
            :value="item"
          />
        </el-select>
      </div>
      <div class="dropdown">
        <span>Platform: </span>
        <el-select
          v-model="platform"
          multiple
          collapse-tags
        >
          <el-option
            key="All"
            label="All"
            value="All"
          />
          <el-option
            v-for="item in getAllPlatforms"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </div>
      <el-table
        ref="dataSetTable"
        :data="topTableData"
        class="top-table"
        highlight-current-row
        :row-class-name="tableRowClassName"
        @current-change="handleCurrentChange"
      >
        <el-table-column
          v-if="isOwner"
          prop="star"
          label=""
          width="40"
        >
          <template slot-scope="scope">
            <i
              class="zeta-icon-download top-color"
              title="remove from top dataset area"
              @click="delTop(scope.row)"
            />
          </template>
        </el-table-column>
        <el-table-column
          prop="table_name"
          label="Dataset Name"
          width="250"
        >
          <template slot-scope="scope">
            <span
              class="table-name-label"
              @click="jumpMetadata(scope.row)"
            >
              {{ scope.row.table_name }}
            </span>
          </template>
        </el-table-column>
        <el-table-column
          label="Usage"
          width="80"
        >
          <template slot-scope="scope">
            <el-slider
              v-model="scope.row.score"
              :show-tooltip="false"
              :max="usageMax"
              disabled
            />
          </template>
        </el-table-column>
        <el-table-column
          prop="score"
          width="80"
        >
          <template slot-scope="scope">
            <span title="# of unique users and batch accouts for latest 30 days in all platforms">{{ scope.row.score }}</span>
            <i
              class="down-icon zeta-icon-download"
              title="download detail user&batch list"
              @click="download(scope.row.table_name)"
            />
          </template>
        </el-table-column>
        <el-table-column
          prop="platforms"
          label="Platform"
          width="150"
        >
          <template slot-scope="scope">
            <i
              v-for="obj in platformArr(scope.row.platforms)"
              :key="obj"
              :class="obj.toLowerCase()"
              class="platform-icon"
            />
          </template>
        </el-table-column>
        <el-table-column
          prop="labels"
          label="Label"
          width="200"
        >
          <template slot-scope="scope">
            <div v-if="existLabels(scope.row.labels)">
              <el-tag
                v-for="tag in getLabels(scope.row.labels)"
                :key="tag"
              >
                {{ tag }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column
          prop="table_desc"
          label="Description"
        >
          <template slot-scope="scope">
            <div
              class="table-desc-label"
              v-html="scope.row.table_desc"
            />
          </template>
        </el-table-column>
      </el-table>
      <p
        class="dataSet-label"
        style="margin: 5px 0px;"
      >
        Dataset
      </p>
      <el-table
        class="dataset-table"
        :data="tableData"
        height="calc(100% - 319px)" 
        highlight-current-row
        :row-class-name="tableRowClassName"
        @current-change="handleCurrentChange"
      >
        <el-table-column
          v-if="isOwner"
          prop="star"
          label=""
          width="40"
        >
          <template slot-scope="scope">
            <i
              class="zeta-icon-download"
              title="highlight table to top dataset area"
              @click="setTop(scope.row)"
            />
          </template>
        </el-table-column>
        <el-table-column
          prop="table_name"
          label="Dataset Name"
          width="250"
        >
          <template slot-scope="scope">
            <span
              class="table-name-label"
              @click="jumpMetadata(scope.row)"
            >{{ scope.row.table_name }}</span>
          </template>
        </el-table-column>
        <el-table-column
          label="Usage"
          width="80"
        >
          <template slot-scope="scope">
            <el-slider
              v-model="scope.row.score"
              :show-tooltip="false"
              :max="usageMax"
              disabled
            />
          </template>
        </el-table-column>
        <el-table-column
          prop="score"
          width="80"
        >
          <template slot-scope="scope">
            <span title="# of unique users and batch accouts for latest 30 days in all platforms">{{ scope.row.score }}</span>
            <i
              class="down-icon zeta-icon-download"
              title="download detail user&batch list"
              @click="download(scope.row.table_name)"
            />
          </template>
        </el-table-column>
        <el-table-column
          prop="platforms"
          label="Platform"
          width="150"
        >
          <template slot-scope="scope">
            <i
              v-for="obj in platformArr(scope.row.platforms)"
              :key="obj"
              :class="obj.toLowerCase()"
              class="platform-icon"
            />
          </template>
        </el-table-column>
        <el-table-column
          prop="labels"
          label="Label"
          width="200"
        >
          <template slot-scope="scope">
            <div v-if="existLabels(scope.row.labels)">
              <el-tag
                v-for="tag in getLabels(scope.row.labels)"
                :key="tag"
              >
                {{ tag }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column
          prop="table_desc"
          label="Description"
        >
          <template slot-scope="scope">
            <div
              class="table-desc-label"
              v-html="scope.row.table_desc"
            />
          </template>
        </el-table-column>
      </el-table>
      <div class="footer">
        <el-pagination
          layout="prev, pager, next"
          :current-page.sync="currentPage"
          :page-size="pageSize"
          :total="total"
        />
      </div>
    </div>
  </el-row>
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch, Inject } from 'vue-property-decorator';
import DoeRemoteService from '@/services/remote/DoeRemoteService';
import sortable from 'sortablejs';
import _ from 'lodash';
import * as Logger from '@/plugins/logs/zds-logger';
import { downloadUsage } from '@/components/Metadata/DownloadUsage.ts';
import moment from 'moment';
@Component({
  components: {
  },
})
export default class DatasetTablePanel extends Vue {
  @Inject('doeRemoteService')
  doeRemoteService: DoeRemoteService;

  @Prop() data: any;
  @Prop({default: false}) isOwner: any;
  @Prop() newArr: any;
  @Prop() owner: any;
  @Prop() top: any;
  @Prop() reference: any;
  @Prop() origin: any;
  @Prop() domain: any;
  @Prop() subDomain: any;

  private debounceSearch: Function;
  debounce = 500;
  originData: any = this.data;
  currentRow: any;
  topArr: any = this.top.filter(Boolean);

  // search func
  search: any = '';
  userArr: any = [];
  platform: any = ['All'];
  label: any = [];

  // owner func
  newOwner = '';
  newOwnerNt = '';
  ownerArr: any = this.owner;

  // reference func
  refTitle: any = '';
  refLink: any = '';
  refArr: any = this.reference;

  // pagination 
  currentPage = 1;
  pageSize = 10;

  // enotify func
  productData: any = [];
  productArr: any = [];
  newProduct = '';
  newProductId = '';

  get hasNew () {
    return _.size(this.newArr) > 0;
  }

  get hasOwner () {
    return _.size(this.ownerArr) > 0;
  }

  get hasRef () {
    return _.size(this.refArr) > 0;
  }

  get topArrSet () {
    return new Set(this.topArr.map(s => s.toUpperCase()));
  }

  get isSubDomain () {
    this.productData = [];
    if (!_.isEmpty(this.subDomain)) this.getDomainProduct();
    return _.isEmpty(this.subDomain);
  }

  get topTableData () {
    const top = this.originData.filter(t => t.info_layer === 'idl' || this.topArrSet.has(t.table_name));
    return _.orderBy(top, ['info_layer', 'table_name'], ['desc', 'asc']);
  }

  get tableData () {
    let rs: any = [];
    rs = _.cloneDeep(this.originData);
    _.forEach(this.topArr, (v: any) => {
      _.remove(rs, (sv: any) => { return _.toLower(sv.table_name) == _.toLower(v);});
    });

    return _.size(rs) > 0 ? rs.slice((this.currentPage - 1) * this.pageSize, this.currentPage * this.pageSize) : rs;
  }

  get total (): number {
    let rs: any = [];
    rs = _.cloneDeep(this.originData);
    _.forEach(this.topArr, (v: any) => {
      _.remove(rs, (sv: any) => { return _.toLower(sv.table_name) == _.toLower(v);});
    });
    return rs ? _.size(rs) : 0;
  }
  
  get usageMax (): number {
    const find: any = _.maxBy(this.data, 'score');
    return !_.isUndefined(find) && _.isNumber(find['score']) ? _.toNumber(find['score']) : 0;
  }

  get getAllLabels () {
    let rs: any = [];
    const arr: any = _.uniq(_.map(this.data, 'labels'));
    _.remove(arr, v => v == null);
    const arrStr: string = _.join(arr, ',');
    rs = _.uniq(_.split(arrStr, ','));
    return _.isEmpty(arrStr) ? [] : _.sortBy(rs, v => v);
  }

  get getAllPlatforms () {
    let rs: any = [];
    const arr: any = _.uniq(_.map(this.data, 'platforms'));
    const arrStr: string = _.join(arr, ',');
    rs = _.sortBy(_.uniq(_.split(arrStr, ',')), v => v);
    return  _.isEmpty(arrStr) ? [] : _.transform(rs, (result: any, v: string) => { result.push({ value: _.upperFirst(v), label: _.toLower(v) == 'numozart' ? 'Mozart' : _.upperFirst(v) });});
  }
  
  mounted () {
    this.debounceSearch = _.debounce(this.searchHandler, 500);
    document.body.ondrop = function (event) {
      event.preventDefault();
      event.stopPropagation();
    };
    this.rowDrop();
  }

  rowDrop () {
    const tbody: any = document.querySelector('.top-table .el-table__body-wrapper tbody');
    sortable.create(tbody, {
      onEnd: ({ newIndex, oldIndex }: any) => {
        const tempList = _.cloneDeep(this.topArr);
        const currRow = tempList.splice(oldIndex, 1)[0];
        tempList.splice(newIndex, 0, currRow);
        const conf: any = { top_datasets: JSON.stringify(tempList) };
        this.$emit('submit-conf', conf);
      },
    });
  }

  // search func
  searchHandler (searchStr: string = this.search) {
    const platformArr: any = (_.isEmpty(this.platform) || _.findIndex(this.platform, v => v == 'All') > -1) ? [] : _.split(_.toLower(_.join(this.platform, ',')), ',');
    const labelArr: any = (_.isEmpty(this.label) || _.findIndex(this.label, v => v == 'All') > -1) ? [] : _.split(_.toLower(_.join(this.label, ',')), ',');
    this.originData = _.filter(this.data, (v: any) => {
      if (v.table_name == 'EIP_LYLTY_OFFER_FREQ') {
        console.log(_.intersection(labelArr, _.split(v.labels, ',')));
      }
      return (_.toLower(v.table_name).indexOf(_.toLower(searchStr)) > -1 &&
              (_.isEmpty(platformArr) || _.size(_.intersection(platformArr, _.split(_.toLower(v.platforms), ','))) > 0) &&
              (_.isEmpty(labelArr) || _.size(_.intersection(labelArr, _.split(_.toLower(v.labels), ','))) > 0));
    });
  }

  userSearchHandler (queryStr: string, cb: any) {
    this.doeRemoteService.getUser(queryStr).then((res: any) => {
      const opntions: Array<any> = [];
      if (res && res.data && res.data.response && res.data.response.docs) {
        _.forEach(res.data.response.docs, (v: any) => {
          const option: any = {
            nt: v.nt,
            value: (v.last_name ? v.last_name : '') + 
                  (v.last_name && v.preferred_name ? ',' : '') + 
                  (v.preferred_name ? v.preferred_name : '') + 
                  '(' + v.nt + ')',
            name: (v.last_name ? v.last_name : '') + 
                (v.last_name && v.preferred_name ? ',' : '') + 
                (v.preferred_name ? v.preferred_name : ''),
          };

          opntions.push(option);
        });
      }

      this.productArr = opntions;
      cb(this.productArr);
    }).catch((err: any) => {
      console.error('user search failed: ' + JSON.stringify(err));
      cb([]);
    });
  }

  platformHandleCommand (command: string) {
    const platformArr = _.isEmpty(this.platform) ? [] : _.split(this.platform, ',');
    const find = _.find(platformArr, (v: any) => v == command);
    if (!find) {
      platformArr.push(command);
    } else {
      _.remove(platformArr, (v: any) => v == command);
    }
    _.remove(platformArr, (v: any) => v == 'All');
    this.platform = (command == 'All' || _.isEmpty(platformArr)) ? 'All' : _.join(platformArr, ',');
  }

  // table func
  handleCurrentChange (val: any) {
    this.currentRow = val;
  }

  tableRowClassName ({ row, rowIndex }: any) {
    if (row.star) {
      return 'top-row';
    }
    return '';
  }

  setTop (row: any) {
    if (!this.isOwner) return;
    this.topArr.splice(0, 0, _.toLower(row.table_name));
    const $dataSetTable: any = this.$refs.dataSetTable;
    $dataSetTable.setCurrentRow(row);
    const conf: any = { top_datasets: JSON.stringify(this.topArr) };
    this.$emit('submit-conf', conf);
  }

  delTop (row: any) {
    if (!this.isOwner) return;
    const tempList = _.cloneDeep(this.topArr);
    const find: any = _.findIndex(this.topArr, (v: any) => { return v == _.toLower(row.table_name); });
    if (find > -1) {
      tempList.splice(find, 1);
    }
    this.topArr = tempList;
    const conf: any = { top_datasets: JSON.stringify(this.topArr) };
    this.$emit('submit-conf', conf);
  }

  // owner func
  handleInput () {
    this.newOwnerNt = '';
  }
	
  handleSelect (item: any) {
    this.newOwnerNt = item.nt;
  }

  addOwner () {
    if (_.isEmpty(this.newOwnerNt)) {
      this.$message.error('Please select owner');
      return;
    }
    const findIndex = _.findIndex(this.ownerArr, (v: any) => { return v.nt == this.newOwnerNt; });
    if (findIndex < 0) {
      const user: any = {nt: this.newOwnerNt, name: this.newOwner};
      this.ownerArr.push(user);
      const conf: any = { owner: JSON.stringify(this.ownerArr) };
      this.$emit('submit-conf', conf);
      const params: any = {
        action: 'addowner',
        to: this.newOwner,
        nts: [this.newOwnerNt],
      };
      this.$emit('send-email', params);
    }
    this.newOwnerNt = '';
    this.newOwner = '';
  }

  removeOwner (nt: any) {
    const ownerArr = _.cloneDeep(this.ownerArr);
    const find = _.find(this.ownerArr, (v: any) => { return v.nt == nt; });
    if (find) {
      const removeNt = find.nt;
      const removeName = find.name;
      _.remove(ownerArr, (v: any) => { return v.nt == nt; });
      this.ownerArr = ownerArr;
      const conf: any = { owner: JSON.stringify(this.ownerArr) };
      this.$emit('submit-conf', conf);
      const params: any = {
        action: 'removeowner',
        to: removeName,
        nts: [removeNt],
      };
      this.$emit('send-email', params);
    }
  }

  platformArr (platforms: string): Array<any> {
    return _.sortBy(_.split(_.toLower(platforms), ','), v => v);
  }

  existLabels (labels: any) {
    return !_.isEmpty(labels);
  }

  getLabels (labels: any) {
    return _.split(labels, ',') || [];
  }

  // references fun
  addReferences () {
    if (_.isEmpty(this.refTitle)) {
      this.$message.error('Please input title');
      return;
    }
    const findIndex = _.findIndex(this.refArr, (v: any) => { return v.title == this.refTitle; });
    if (findIndex < 0) {
      const ref: any = {title: this.refTitle, link: this.refLink};
      this.refArr.push(ref);
      this.refTitle = '';
      this.refLink = '';
      const conf: any = { reference: JSON.stringify(this.refArr) };
      this.$emit('submit-conf', conf);
    } else {
      this.$message.error('Duplicate title');
      return;
    }
  }

  removeReferences (title: any) {
    const refArr = _.cloneDeep(this.refArr);
    const findIndex = _.findIndex(this.refArr, (v: any) => { return v.title == title; });
    if (findIndex > -1) {
      _.remove(refArr, (v: any) => { return v.title == title; });
      this.refArr = refArr;
      const conf: any = { reference: JSON.stringify(this.refArr) };
      this.$emit('submit-conf', conf);
    }
  }

  // enotify func
  getIndex (index: any): string {
    return _.padStart(index, 2, '0');
  }

  jump (url: string) {
    window.open(url);
  }

  jumpMetadata (row: any) {
    const table: string = row.type == 'vdm' ? _.toUpper(row.db_name + '.' + row.table_name) : _.toUpper(row.table_name);
    Logger.counter('METADATA_TABLE_DETAIL', 1, { name: 'overview', trigger: 'subdomain_dataset_click', table: table });
    this.$router.push('/metadata/tables/' + table);
  }

  getDomainProduct () {
    const params: any = { sub_domain: this.subDomain };
    this.newProductId = '';
    this.newProduct = '';
    this.doeRemoteService.getDomainProduct(params).then((res: any) => {
      if (res && res.data && res.data.data && res.data.data.value) {
        this.productData = res.data.data.value;
      }
    });
  }

  productSearchHandler (queryStr: string, cb: any) {
    const params: any = {domain_name: this.subDomain, product_name: queryStr};
    this.doeRemoteService.getSubDomainProductOption(params).then((res: any) => {
      const opntions: Array<any> = [];
      if (res && res.data && res.data.data && res.data.data.value) {
        _.forEach(res.data.data.value, (v: any) => {
          const option: any = {
            id: v.product_id,
            value: v.product_name,
          };

          opntions.push(option);
        });
      }

      this.userArr = opntions;
      cb(this.userArr);
    }).catch((err: any) => {
      console.error('user search failed: ' + JSON.stringify(err));
      cb([]);
    });
  }

  handleProductInput () {
    this.newProductId = '';
  }
  
  handleProductSelect (item: any) {
    this.newProductId = item.id;
  }

  addProduct () {
    if (this.newProductId == '') {
      this.$message.error('Please select product');
      return;
    }

    const params: any = { domain_name: this.subDomain, product_id: this.newProductId };
    this.doeRemoteService.insertDomainProduct(params).then((res: any) => {
      if (res && res.status == 200) {
        this.getDomainProduct();
        this.$emit('refresh-enotify', { sub_domain: this.subDomain });
      }
    });
  }

  deleteProduct (row: any) {
    const params: any = { domain_name: this.subDomain, product_id: row.product_id };
    this.doeRemoteService.delDomainProduct(params).then((res: any) => {
      if (res && res.status == 200) {
        this.getDomainProduct();
        this.$emit('refresh-enotify', { sub_domain: this.subDomain });
      }
    });
  }

  download (tableName: any) {
    const params: any = {
      table: tableName,
      startDate: moment().subtract(30, 'days').format('YYYY-MM-DD'),
      endDate: moment().format('YYYY-MM-DD'),
    };

    downloadUsage(params);
  }

  @Watch('data')
  onDataChange () {
    this.search = '';
    this.currentPage = 1;
    this.originData = this.data;
  }

  @Watch('top')
  onTopChange () {
    this.topArr = this.top.filter(Boolean);
  }

  @Watch('search')
  onSearchChange (txt: string) {
    this.debounceSearch(txt);
  }

  @Watch('label')
  onLabelChange () {
    this.debounceSearch();
  }

  @Watch('platform')
  onPlatformChange () {
    if (_.size(this.platform) == 0) {
      this.platform = ['All'];
    } else if (_.size(this.platform) > 1) {
      if (_.last(this.platform) == 'All') {
        this.platform = ['All'];
      } else {
        _.remove(this.platform, v => v == 'All');
      }
    }
    this.debounceSearch();
  }
}
</script>
<style lang="scss" scoped>
@import '@/styles/metadata.scss';
.dataSet-usage-div {
  display: flex;
  width: 100%;
  height: 100%;
  overflow-y: hidden;
  .dataSet-div {
    height: 100%;
    width: 100%;
  }
}
.el-table {
  border-bottom: 2px solid #CACBCF;
  border-top: 2px solid #CACBCF;
  width: 100%;
  .table-desc-label {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  /deep/ th, td {
    font-size: 14px;
  }
  /deep/ .top-row {
    background-color: #F9F9FA;
  }
  /deep/ .el-tag {
    margin-right: 10px;
  }
  /deep/ .el-slider__button-wrapper {
    display: none !important;
  }
  /deep/ .el-slider__bar {
    height: 15px !important;
    background-color: #569ce1 !important;
    border-top-right-radius: 3px;
    border-bottom-right-radius: 3px;
  }
  /deep/ .el-slider__runway {
    height: 15px !important;
    margin: 0px 0px !important;
  }
}
.table-name-label {
  cursor: pointer;
}
.table-name-label:hover{
  color: rbg(86, 156, 225) !important;
}
.search-ipt {
  width: 180px;
  float: right;
  margin-bottom: 10px;
}
.down-icon.zeta-icon-download {
  margin-left: 5px;
}
.down-icon.zeta-icon-download:before {
  transform: rotate(0) !important;
}
.zeta-icon-download:before {
  transform: rotate(180deg);
  display: inline-block;
}
.zeta-icon-download {
  cursor: pointer;
}
.top-color {
  color: #4d8cca;
}
.footer {
  float: right;
}
.dropdown {
  float: right;
  line-height: 30px;
  margin-right: 10px;
  cursor: pointer;
  /deep/ .el-input__inner {
    height: 30px !important;
  }
}
.cursor {
  cursor: pointer;
}
@media screen and (max-height: 900px) {
  .dataset-table.el-table {
    min-height: 375px !important;
  }
  .dataSet-div {
    overflow-y: auto;
  }
}
.dataSet-label {
  font-weight: 700;
  font-size: 16px;
  color: #1E1E1E;
  padding-left: 10px;
  display: inline-block;
  height: 30px;
  line-height: 30px;
}
</style>
