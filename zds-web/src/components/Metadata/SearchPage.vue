<template>
  <div
    v-loading="loading"
    class="metadata-browse-container"
  >
    <Header 
      :show-back-button="true"
      :breadcrumb="[{ key: 'Metadata', label: 'Metadata' }]"
      :back-action="backTo"
      @breadcrumb-click="backTo"
    />
    <div
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
</template>

<script lang="ts">
import { Component, Vue, Provide, Watch } from 'vue-property-decorator';
import Header from '@/components/Metadata/index/components/Header.vue';
import DoeRemoteService from '@/services/remote/DoeRemoteService';
import MetadataSearchResult from '@/components/Metadata/SearchResult.vue';
import TopDatasets from '@/components/Metadata/TopDatasets.vue';
import Util from '@/services/Util.service';
import _ from 'lodash';
import * as Logger from '@/plugins/logs/zds-logger';
import { TrackingData } from '@/components/Metadata/trackingData.ts';
import { ZetaExceptionProps } from '@/types/exception';
@Component({
  components: {
    MetadataSearchResult,
    TopDatasets,
    Header,
  },
})
export default class MetadataSearchPage extends Vue {
  [x: string]: any;
  @Provide('doeRemoteService') doeRemoteService: DoeRemoteService = new DoeRemoteService();
  readOnly = false;
  loading = false;
  queryType = 'all';
  domainArr: any = [];
  enotifyArr: any = [];
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
      this.queryType = url.type;
      this.search(false, url.offset);
    }

    const props: ZetaExceptionProps = {
      path: 'metadata',
    };
    this.doeRemoteService.props(props);
  }

  get path () {
    return this.$route.path;
  }

  get domain (): any {
    return this.$route.query.domain || '';
  }

  get subDomain (): any {
    return this.$route.query.subdomain || '';
  }

  get keywords (): any {
    return this.$route.query.keyword || '';
  }

  @Watch('keywords')
  handle () {
    this.search(true);
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

  openSubDomain (item: any) {
    this.$router.push({
      path: `/metadata/search/datasets?domain=${item.domain}&subdomain=${item.isDomain ? '' : item.name}`,
    });
    Logger.counter('METADATA_BROWSE', 1, {
      name: 'overview',
      trigger: this.readOnly ? 'share' : 'zeta',
      domain: item.domain,
      subdomain: item.isDomain ? undefined : item.name,
    });
  }

  backTo () {
    this.$router.push('/metadata');
  }

  searchByType (type) {
    this.queryType = type;
    this.search(true);
  }

  search (insertFlg: boolean, offset?: any) {
    if (this.keywords != '') {
      const params: any = {
        keywords: this.keywords,
        objs: this.queryType != 'all' ? this.queryType : 'table,tableau,udf,query',
        size: 10,
        offset: isNaN(offset) ? 0 : offset,
      };

      const transaction = this.$apm.startTransaction('metadata', 'search');
      this.$apm.setUserContext({id: Util.getNt(), username: Util.getCurrUserFullName()});
      this.loading = true;
      this.searchResult = [];
      this.offset = offset;
      if (isNaN(offset)) {
        this.totalSize = 1;
        this.offset = 0;
      }
      window.location.href = `${location.protocol}//${location.host}${location.pathname}#/metadata/search?keyword=${this.keywords}` + `&offset=${isNaN(offset) ? 0 : offset}` + `&type=${this.queryType}`;
      this.doeRemoteService.esSearch(params).then((res: any) => {
        if (res && res.data && res.data.value) {
          this.searchResult = res.data.value.result;
          this.totalSize = res.data.value.total_size;
          if (insertFlg) {
            const result: any = _.unionBy(res.data.value.result, 'name');
            const data: any = {
              nt_login: Util.getNt(),
              search_txt: this.keywords,
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
        const source = {transName: 'metadata', transType: 'search', module: 'browse', user: Util.getNt()};
        const data = {...params, result: JSON.stringify(result), totalSize: this.totalSize};
        const tracking = new TrackingData(source, data);
        transaction.addLabels(tracking.data);
        transaction && transaction.end();
      });
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
      keywords: this.keywords,
      objs: this.queryType != 'all' ? this.queryType : 'table,tableau,udf,query',
      size: 10,
      offset: isNaN(this.offset) ? 0 : this.offset,
    };
    const result = _.transform(this.searchResult, (rs: any, v: any) => { rs.push(_.pick(v, ['name', 'type'])); }, []);
    const source = {transName: transaction_name, transType: transaction_type, module: 'browse', user: Util.getNt()};
    const data = {...params, ...labels, keywords: this.keywords, result: JSON.stringify(result), totalSize: this.totalSize};
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
  padding: 0 16px;
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

</style>
