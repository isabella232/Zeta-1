<template>
  <div class="metadata-search-div">
    <div class="close-div">
      <i class="zeta-icon-close" @click="close"/>
    </div>
    <div class="search-div">
      <el-input v-model="search" placeholder="Example: DW_CHECKOUT_TRANS" clearable autofocus="true"></el-input>
    </div>
    <div class="recommendation-div" v-if="isEmptySearch">
      <span class="title">Recommendation / most popular search</span>
      <span class="search-item" @click="addSearch(name)" v-for="name, $i in recommendationArr" :key="$i">{{ name }}</span>
    </div>
    <div class="search-history-div" v-if="isEmptySearch">
      <span class="title">Search history</span>
      <span class="search-item" @click="addSearch(item.search_txt)" v-for="item, $i in historyArr" :key="$i">{{ item.search_txt }}</span>
    </div>
    <div class="result-div" v-if="!isEmptySearch">
      <div class="table-div">
        <span class="table-title">Table</span>
        <span class="table-name" @click="jumpMetadata(result.name)">{{ result.name }}</span>
        <span class="table-desc">{{ result.desc }}</span>
        <div class="meta-platform">
          <i
              v-for="(p,$i) in result.platformOptions"
              :key="$i"
              :title="p"
              :class="p.toLowerCase()"
              class="platform-icon"
          />
        </div>
        <span class="more" @click="showMore" v-if="!moreVisible">More Result</span>
        <div v-for="item in resultArr" :key="item.name" v-if="moreVisible">
          <span class="table-name" @click="jumpMetadata(item.name)">{{ item.name }}</span>
          <span class="table-desc">{{ item.desc }}</span>
          <div class="meta-platform">
            <i
                v-for="(p,$i) in item.platformOptions"
                :key="$i"
                :title="p"
                :class="p.toLowerCase()"
                class="platform-icon"
            />
          </div>
        </div>
        <span class="more" @click="showMore" v-if="moreVisible">Less Result</span>
      </div>
      <div class="tableau-div">
        <span class="tableau-title">Tableau</span>
        <div class="tableau-item" v-for="(item, $i) in tableauArr" :key="$i">
          <span class="tableau-name" @click="jumpTableau(item.asset_ref_id)">{{ item.asset_name }}</span>
          <span class="owner-panel">Owner</span><span class="owner">{{ item.user }}</span>
          <img class="tableau" :src="item.image_base64" @click="jumpTableau(item.asset_ref_id)" title="Click to jump to Tableau."></img>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch, Inject } from "vue-property-decorator";
import BasicChart from '@/components/Metadata/BasicChart.vue';
import MetadataService from "@/services/Metadata.service";
import DoeRemoteService from "@/services/remote/DoeRemoteService";
import UserInfoRemoteService from "@/services/remote/UserInfo"
import Util from "@/services/Util.service";
import _ from "lodash";
import * as Logger from '@/plugins/logs/zds-logger';
@Component({
  components: {
    BasicChart
  }
})
export default class MetadataSearch extends Vue {
  @Inject()
  metadataService: MetadataService;
  @Inject('doeRemoteService')
  doeRemoteService: DoeRemoteService;
  @Inject('userInfoRemoteService')
  userInfoRemoteService: UserInfoRemoteService;

  search: string = "";
  recommendationArr: any = [];
  historyArr: any = [];
  resultArr: any = [];
  result: any = {};
  tableauArr: any = [];
  moreVisible: boolean = true;
  private debounceSearch: Function;

  constructor() {
    super();
    this.debounceSearch = _.debounce(this.searchHandler, 800);
  }

  mounted() {
    this.getRecommendationSearch();
    this.getSearchHist();
  }

  searchHandler(searchStr: string = this.search) {
    this.resultArr = [];
    this.result = {};
    this.tableauArr = [];
    if (!_.isEmpty(searchStr)) Logger.counter('METADATA_SEARCH', 1, { name: 'metadata_browse' });
    this.metadataService.queryMetadataList(searchStr).then(metas => {
      if (!_.isEmpty(searchStr) && metas) {
        this.resultArr = metas.length > 0 ? _.slice(metas, 1) : [];
        this.result = metas.length > 0 ? metas[0] : {};
        this.insertHist(_.join(_.slice(_.uniq(_.map(metas, "name")), 0, 5), ","));
      }
    });

    if (!_.isEmpty(searchStr)) {
      this.metadataService.queryTableauList(searchStr).then(metas => {
        if (metas) {
          this.tableauArr = metas;
          this.insertHist(_.join(_.slice(_.uniq(_.map(metas, "asset_name")), 0, 5), ","));
        }
      });
    }
  }

  getRecommendationSearch() {
    this.doeRemoteService.getRecommendationSearch().then(res => {
      if (res && res.data && res.data.data && res.data.data.value) {
        this.recommendationArr = _.map(res.data.data.value, 'result_name');
      }
    });
  }

  get isEmptySearch(): boolean {
    return _.isEmpty(this.search) ? true : false;
  }

  close() {
    this.search = "";
    this.$emit("close-search");
  }

  addSearch(searchStr: any) {
    this.search = searchStr;
  }

  showMore() {
    this.moreVisible = !this.moreVisible;
  }

  jumpMetadata(table: string) {
    Logger.counter('METADATA_TABLE_DETAIL', 1, { name: 'overview', trigger: 'metadata_browse_search_click', table: _.toUpper(table) });
    this.$router.push("/metadata?tableName=" + table);
  }

  getSearchHist() {
    this.historyArr = [];
    this.doeRemoteService.getSearchHist({nt_login: Util.getNt()}).then(res => {
      if (res && res.data && res.data.data && res.data.data.value) {
        this.historyArr = res.data.data.value;
      }
    });
  }

  insertHist(result: any) {
    const params: any = {
      nt_login: Util.getNt(),
      search_txt: this.search,
      result_name: result || ""
    }
    
    this.doeRemoteService.insertSearchHist(params);
  }

  jumpTableau(tableauId: string) {
    const url: string = "https://tableau.corp.ebay.com/#/redirect_to_view/" + tableauId;
    window.open(url);
  }

  fullNameByNt(nt: string) {
    this.userInfoRemoteService.getBaseInfo(nt)
      .then((res: any) => {
        let userInfo = res.data.response.docs[0];
        return res.data.response.docs[0].first_name +
            " " +
            res.data.response.docs[0].last_name || nt;
      })
      .catch(err => {
        return nt;
      });
  }

  @Watch("search")
  onSearchChange(txt: string) {
    this.result = {}
    this.resultArr = [];
    this.debounceSearch(txt);
  }
}
</script>
<style lang="scss" scoped>
.metadata-search-div {
  height: 100%;
  width: 100%;
}
.close-div {
  .zeta-icon-close {
    font-size: 40px;
    position: absolute;
    top: 20px;
    right: 20px;
    color: #CACBCF;
    cursor: pointer;
  }
}
.search-div {
  margin: 0 80px;
  padding-top: 70px;
  padding-bottom: 20px;
  border-bottom: 1px solid #CACBCF;
  .el-input {
    display: flex;
    justify-content: center;
    width: 50%;
    margin: 0 auto;
  }
  /deep/ .el-input .el-input__inner {
    line-height: 36px;
    height: 36px;
    font-size: 36px;
    border: 0;
  }
}
.recommendation-div, .search-history-div {
  margin: 0 80px;
  padding-top: 40px;
  .title {
    font-weight: 700;
    font-style: italic;
    font-size: 14px;
    color: #CACBCF;
    display: block;
  }
  .search-item {
    cursor: pointer;
    font-weight: 400;
    font-style: italic;
    font-size: 14px;
    color: #CACBCF;
    display: block;
    padding-top: 10px;
  }
}
.result-div {
  margin: 0 80px;
  padding-top: 40px;
  display: flex;
  height: calc(100% - 167px);
  overflow-y: auto;
  .table-div {
    width: calc(100% - 460px);
    padding: 0 20px;
    .meta-platform {
      padding-bottom: 20px;
    }
    span {
      display: block;
      padding-bottom: 20px;
    }
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
          font-weight: 400;
        }
        > td {
          padding-left: 30px;
          color: #333;
        }
      }
      span {
        padding: 0;
      }
    }
    .table-title {
      font-weight: 700;
      font-size: 18px;
    }
    .table-name {
      font-weight: 700;
      font-size: 14px;
      color: #569CE1;
      cursor: pointer;
    }
    .table-desc {
      font-size: 14px;
      color: #CACBCF;
    }
    .more {
      cursor: pointer;
      text-decoration: underline;
      color: #569CE1;
      font-size: 13px;
    }
  }
  .tableau-div {
    width: 400px;
    padding-right: 20px;
    span {
      display: block;
      padding-bottom: 20px;
    }
    .tableau-title {
      font-weight: 700;
      font-size: 18px;
    }
    .tableau-item {
      .tableau-name {
        font-weight: 700;
        font-size: 14px;
        color: #569CE1;
        cursor: pointer;
        width: fit-content;
      }
      .owner-panel {
        width: 60px;
        display: inline-block;
      }
      .owner {
        display: inline-block;
      }
      .tableau {
        cursor: pointer;
        width: 100%;
        height: 226px;
        background-color: #CACBCF;
        margin-bottom: 40px;
        background-repeat: no-repeat;
        box-shadow: 1px 1px 3px rgb(212, 212, 212), -1px -1px 3px rgb(212, 212, 212);
        opacity: 1;
        background-size: cover;
        display: block;
      }
    }
  }
}
</style>
