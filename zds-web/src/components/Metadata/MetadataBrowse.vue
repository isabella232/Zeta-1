<template>
  <div
    v-loading="loading"
    class="metadata-browse-container"
  >
    <div class="header-div">
      <search-bar
        :search="onSearch"
        :close="closeSearch"
      />
      <div
        class="my-collection"
        @click="$router.push('/metadata/collection/')"
      >
        <span>My Collection</span>
      </div>
      <div class="register">
        <el-dropdown
          placement="bottom-start"
          @command="(cmd) => $router.push('/metadata/register/' + cmd)"
        >
          <span
            v-click-metric:REGISTER_DATASET="{ name: 'visit' }"
            class="el-dropdown-link"
          >
            Register Metadata
          </span>
          <el-dropdown-menu slot="dropdown">
            <el-dropdown-item command="vdm">
              VDM
            </el-dropdown-item>
            <el-dropdown-item command="udf">
              UDF
            </el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
      </div>
    </div>

    <div
      v-if="path === '/metadata/search'"
      class="page-content"
    >
      <div
        v-if="!searchVisible"
        class="content-div"
      >
        <div class="domains-div">
          <div
            v-for="item in domainArr"
            :key="item.domain"
            style="width: 49%; display: inline-block;"
          >
            <DomainCard
              :data="item"
              @open-sub-domain="openSubDomain"
            />
          </div>
        </div>
        <div class="news-div">
          <MetadataBrowseNews
            :data="enotifyArr"
            type="enotify"
          />
        </div>
      </div>
      <div
        v-else
        class="search-result-div"
      >
        <div class="filter-bars">
          <ul>
            <li
              :class="{ active: queryType === 'all' }"
              @click="searchByType('all')"
            >
              All
            </li>
            <li
              :class="{ active: queryType === 'table' }"
              @click="searchByType('table')"
            >
              Table
            </li>
            <li
              :class="{ active: queryType === 'tableau' }"
              @click="searchByType('tableau')"
            >
              Tableau Report
            </li>
            <li
              :class="{ active: queryType === 'udf' }"
              @click="searchByType('udf')"
            >
              UDF
            </li>
            <li
              :class="{ active: queryType === 'query' }"
              @click="searchByType('query')"
            >
              Notebook
            </li>
          </ul>
        </div>
        <div class="result-div">
          <MetadataSearchResult
            ref="result"
            :data="searchResult"
            :total="totalSize"
            :offset="offset"
            :loading="loading"
            @search="search"
          />
        </div>
        <div class="news-div">
          <TopDatasets
            :data="topDatasetArr"
            @record-store="record"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Inject, Prop, Provide } from 'vue-property-decorator';
import MetadataBrowseNews from '@/components/Metadata/MetadataBrowseNews.vue';
import DomainCard from '@/components/Metadata/DomainCard.vue';
import MetadataSearch from '@/components/Metadata/MetadataSearch.vue';
import DoeRemoteService from '@/services/remote/DoeRemoteService';
import MetadataSearchResult from '@/components/Metadata/SearchResult.vue';
import TopDatasets from '@/components/Metadata/TopDatasets.vue';
import SearchBar from '@/components/Metadata/components/SearchBar.vue';
import Util from '@/services/Util.service';
import _ from 'lodash';
import * as Logger from '@/plugins/logs/zds-logger';
import { TrackingData } from '@/components/Metadata/trackingData.ts';
@Component({
  components: {
    MetadataBrowseNews,
    DomainCard,
    MetadataSearch,
    MetadataSearchResult,
    TopDatasets,
    SearchBar,
  },
})
export default class MetadataBrowse extends Vue {
  [x: string]: any;
  @Prop() domain: any;
  @Prop() subDomain: any;
  @Prop() readOnly: boolean;
  @Inject('doeRemoteService')
  
  doeRemoteService: DoeRemoteService;

  loading = false;
  queryStr = '';
  queryType = 'all';
  searchVisible = false;
  domainArr: any = [];
  enotifyArr: any = [];
  recommendationArr: any = [];
  searchResult: any = [];
  topDatasetArr: any = [];
  totalSize: any = 1;
  offset: any = 0;

  mounted () {
    Logger.counter('METADATA_BROWSE', 1, { name: 'domain list', trigger: (this.readOnly ? 'share' : 'browse') });
    this.getDomainInfo();
    this.getEnotifyByType();
    this.getDatasets();
    let store: any =  this.$store.getters.getBrowseRoute;
    const url: any = this.$route.query;
    if (store.data) {
      this.openSubDomain(store.data);
      this.$store.dispatch('setBrowseRoute', {data: false});
      this.$store.dispatch('setBrowseSearchRoute', {url: false});
      this.$store.dispatch('setTableManagementRoute', {data: false});
    } else if (url.keyword) {
      store = this.$store.getters.getBrowseSearchRoute;
      if (store && store.url) {
        this.$store.dispatch('setBrowseRoute', {data: false});
        this.$store.dispatch('setBrowseSearchRoute', {url: false});
        this.$store.dispatch('setTableManagementRoute', {data: false});
      }
      this.searchVisible = true;
      this.queryStr = url.keyword;
      this.queryType = url.type;
      this.search(false, url.offset);
    }
  }

  onSearch (keywords: string) {
    this.queryStr = keywords;
    this.search(true);
  }

  get path () {
    return this.$route.path;
  }

  getDomainInfo () {
    this.loading = true;
    this.domainArr = [];
    this.doeRemoteService.getDomainInfo().then((res: any) => {
      if (res && res.data && res.data.data && res.data.data.value) {
        const temp: any = res.data.data.value;
        const other: any = _.find(temp, (v: any) => { return _.toLower(v.domain) == 'others'; });
        _.remove(temp, (v: any) => { return _.toLower(v.domain) == 'others'; });
        temp.push(other);
        this.domainArr = res.data.data.value;
        const find: any = _.find(this.domainArr, v => v.domain == this.domain);
        if (!_.isEmpty(this.domain) && find) {
          const params: any = {
            name: !_.isEmpty(this.subDomain) ? this.subDomain : this.domain,
            isDomain: !_.isEmpty(this.subDomain) ? false : true,
            subDomain: find.sub_domain == null ? [] : _.split(find.sub_domain, ','),
            domain: this.domain,
          };
          this.openSubDomain(params);
        }
      }
      this.loading = false;
    }).catch(err => {
      this.loading = false;
    });
  }

  getEnotifyByType () {
    this.enotifyArr = [];
    const params: any = {
      type: 'PRODUCT',
      limit: 15,
    };
    this.doeRemoteService.getEnotifyByType(params).then((res: any) => {
      if (res && res.data && res.data.data && res.data.data.value) {
        this.enotifyArr = res.data.data.value;
      }
    });
  }

  getDatasets () {
    this.topDatasetArr = [];
    this.doeRemoteService.getDatasets().then((res: any) => {
      if (res && res.data && res.data.data && res.data.data.value) {
        this.topDatasetArr = _.take(_.filter(_.split(res.data.data.value[0].top_datasets, ','), (v) => { return v !== 'null'; }), 10);
      }
    });
  }

  openSearch () {
    this.searchVisible = true;
  }

  closeSearch () {
    this.queryType = 'all';
    this.searchVisible = false;
    this.$store.dispatch('setBrowseSearchRoute', {url: false});
  }

  openSubDomain (item: any) {
    this.$router.push({
      path: `/metadata/datasets?domain=${item.domain}&subdomain=${item.isDomain ? '' : item.name}`,
    });
    Logger.counter('METADATA_BROWSE', 1, {
      name: 'overview',
      trigger: this.readOnly ? 'share' : 'zeta',
      domain: item.domain,
      subdomain: item.isDomain ? undefined : item.name,
    });
  }

  searchByType (type) {
    this.queryType = type;
    this.search(true);
  }

  search (insertFlg: boolean, offset?: any) {
    if (this.queryStr != '') {
      const params: any = {
        keywords: this.queryStr,
        objs: this.queryType != 'all' ? this.queryType : 'table,tableau,udf,query',
        size: 10,
        offset: isNaN(offset) ? 0 : offset,
      };
      // this.apmTransaction('browse-search', 'search', params);
      const transaction = this.$apm.startTransaction('metadata', 'search');
      this.$apm.setUserContext({id: Util.getNt(), username: Util.getCurrUserFullName()});
      this.loading = true;
      this.searchResult = [];
      this.offset = offset;
      if (isNaN(offset)) {
        this.totalSize = 1;
        this.offset = 0;
      }
      // const resultPage: any = this.$refs.result;
      // if (resultPage) {
      //   resultPage.cancelAsync();
      //   resultPage.createCancelToken();
      // }
      window.location.href = `${location.protocol}//${location.host}${location.pathname}#/metadata/search?keyword=${this.queryStr}` + `&offset=${isNaN(offset) ? 0 : offset}` + `&type=${this.queryType}`;
      this.doeRemoteService.esSearch(params).then((res: any) => {
        if (res && res.data && res.data.value) {
          this.searchResult = res.data.value.result;
          this.totalSize = res.data.value.total_size;
          if (insertFlg) {
            const result: any = _.unionBy(res.data.value.result, 'name');
            const data: any = {
              nt_login: Util.getNt(),
              search_txt: this.queryStr,
              result_name: _.join(_.slice(_.map(result, 'name'), 0, 5), ','),
              result_type: _.join(_.slice(_.map(result, 'type'), 0, 5), ','),
            };
            this.doeRemoteService.insertSearchHist(data);
          }
        }
        this.loading = false;
      }).catch((err: any) => {
        this.totalSize = 0;
        console.error('user search failed: ' + JSON.stringify(err));
        this.loading = false;
      }).finally(() => {
        const result = _.transform(this.searchResult, (rs: any, v: any) => { rs.push(_.pick(v, ['name', 'type'])); }, []);
        const source = {transName: "metadata", transType: "search", module: "browse", user: Util.getNt()};
        const data = {...params, result: JSON.stringify(result), totalSize: this.totalSize};
        const tracking = new TrackingData(source, data);
        transaction.addLabels(tracking.data);
        transaction && transaction.end();
      });
      this.openSearch();
    } else {
      this.closeSearch();
    }
  }

  record () {
    this.$store.dispatch('setBrowseSearchRoute', {url: window.location.href});
  }

  @Provide('apmTransaction')
  apmTransaction (transaction_name: string, transaction_type: string, labels?: any) {
    const transaction = this.$apm.startTransaction(transaction_name, transaction_type);
    this.$apm.setUserContext({id: Util.getNt(), username: Util.getCurrUserFullName()});
    const params: any = {
      keywords: this.queryStr,
      objs: this.queryType != 'all' ? this.queryType : 'table,tableau,udf,query',
      size: 10,
      offset: isNaN(this.offset) ? 0 : this.offset,
    };
    const result = _.transform(this.searchResult, (rs: any, v: any) => { rs.push(_.pick(v, ['name', 'type'])); }, []);
    const source = {transName: transaction_name, transType: transaction_type, module: "browse", user: Util.getNt()};
    const data = {...params, ...labels, keywords: this.queryStr, result: JSON.stringify(result), totalSize: this.totalSize};
    const tracking = new TrackingData(source, data);
    transaction.addLabels(tracking.data);
    return transaction;
  }
}
</script>
<style lang="scss" scoped>
@import '@/styles/global.scss';
.metadata-browse-container {
  height: 100%;
  padding: 0px 10px;
  padding-top: 30px;
}
.bar-div {
  background-color: #F9F9FA;
  box-shadow: 0 2px 4px 0 rgba(0, 0, 0, 0.12), 0 0 6px 0 rgba(0, 0, 0, 0.04);
  height: 30px;
  width: calc(100% - 70px);
  position: fixed;
  left: 70px;
  top: 54px;
  overflow: auto;
}
.header-div {
  padding-bottom: 20px;
  display: flex;
  justify-content: center;
  align-items: center;
  .search-input {
    width: 50%;
    /deep/ .el-input__inner {
      height: 40px;
      line-height: 40px;
      font-size: 16px;
    }
    .el-select {
      width: 150px;
    }
  }
  .el-icon-search {
    height: 40px;
    font-size: 18px;
    margin-left: 20px;
    span {
      margin-left: 5px;
      font-family: Arial,Helvetica,sans-serif;
    }
  }
}
.header-div.search-visible {
  padding-bottom: 0 !important;
  display: block;
}

.page-content {

  height: calc(100% - 102px);
  overflow-y: auto;
  overflow-x: hidden;
}

.content-div {
  width: 100%;
  display: flex;
  .domains-div {
    width: calc(100% - 400px);
  }
  .news-div {
    width: 400px;
  }
  .result-div {
    width: 66%;
  }
}
.search-result-div {
  width: 100%;
  display: flex;
  height: calc(100% - 102px);
  overflow: hidden;
  flex-wrap: wrap;
  .filter-bars {
    align-items: center;
    display: flex;
    height: 40px;
    justify-content: space-between;
    border-bottom: 1px solid #cacbcf;
    width: 100%;
    margin-bottom: 20px;
    ul {
      display: flex;
      height: 100%;
      list-style-type: none;
      width: 100%;
      > li {
        align-items: center;
        color:#999;
        cursor: pointer;
        display: flex;
        font-size: 14px;
        padding: 0 20px;
        &.active {
          border-bottom: 1px solid #569ce1;
          background-color: #fff;
          color:$zeta-global-color;
        }
        &:hover{
          color:#569CE1;
        }
      }
    }
  }
  .result-div {
    height: calc(100% - 61px);
    width: calc(100% - 440px);
  }
  .news-div {
    padding: 0 20px;
    width: 400px;
    height: calc(100% - 61px);
  }
}
.search-div {
  width: 100%;
  display: flex;
  height: calc(100% - 102px);
}
.el-autocomplete-suggestion.recommend-css li {
  line-height: normal;
  padding: 7px 0;
  .sub-item {
    font-size: 12px;
    color: #b4b4b4;
  }
}
.register {
  color: #CACBCF;
  cursor: pointer;
}
.register:hover {
  color: #4d8cca;
}
.register-domain {
  position: fixed;
  right: 25px;
}
.register-result {
  position: relative;
  display: inline-block;
  float: right;
  line-height: 40px;
}
.my-collection {
  cursor: pointer;
  color: #606266;
}
</style>
