<template>
  <div class="search-bar">
    <el-popover
      placement="bottom-end"
      width="500"
      trigger="click"
      :offset="420"
    >
      <div class="popover-content">
        <ul class="sub-menu">
          <h1>Metadata</h1>
          <!-- <li @click="() => $router.push('/metadata')">
            Domain
          </li> -->
          <li @click="() => $router.push('/metadata/sa')">
            Subject Area
          </li>
          <li @click="() => $router.push('/metadata/udf')">
            UDF
          </li>
        </ul>
      </div>
      <span
        slot="reference"
        class="el-dropdown-link"
      >
        <span>Browse by category</span>
        <i class="el-icon-caret-bottom el-icon--right" />
      </span>
    </el-popover>

    <el-autocomplete
      ref="search-input"
      v-model="queryStr"
      class="search-input"
      :popper-class="queryStr == '' ? 'recommend-css' : ''"
      :fetch-suggestions="fetchSuggestions"
      :debounce="500"
      placeholder="Search everything in Zeta..."
      @select="handleSearch"
      @keyup.enter.native="handleEnter($event)"
    >
      <i
        v-show="getIsQueryEmpty"
        slot="suffix"
        class="el-input__icon el-icon-close close-icon"
        @click="closeSearch"
      />
    </el-autocomplete>
    <el-button
      ref="search-button"
      v-click-metric:METADATA_SEARCH="{
        name: 'metadata_browse',
        trigger: 'click',
      }"
      type="primary"
      class="el-icon-search"
      @click="handleSearch()"
    >
      <span>Search</span>
    </el-button>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Inject, Prop, Watch } from 'vue-property-decorator';
import Util from '@/services/Util.service';
import _ from 'lodash';
import * as Logger from '@/plugins/logs/zds-logger';
import DoeRemoteService from '@/services/remote/DoeRemoteService.ts';


@Component({})
export default class MetadataBrowse extends Vue {
  @Prop() search: (keyword: string) => void;
  @Prop() close: () => void;
  @Inject('doeRemoteService')
  doeRemoteService: DoeRemoteService;

  queryStr = '';
  queryType = 'all';
  params: any = {};
  domainArr: any = [];

  @Watch('$route.query.keyword', { immediate: true })
  syncQueryStr (query) {
    this.queryStr = query;
  }
  @Watch('$route.query.type', { immediate: true })
  syncType (type) {
    this.queryType = type;
  }

  get getIsQueryEmpty () {
    return this.queryStr != '';
  }
  handleSearch () {
    this.search(this.queryStr);
  }
  handleClose () {
    this.close();
  }
  fetchSuggestions (queryStr: string, cb: any) {
    if (_.isEmpty(queryStr)) {
      const params: any = {
        nt_login: Util.getNt(),
      };
      if (this.queryType != 'all') {
        params.type = this.queryType;
      }
      this.doeRemoteService
        .getRecommendationSearchHist(params)
        .then(res => {
          const opntions: Array<any> = [];
          if (res && res.data && res.data.data && res.data.data.value) {
            _.forEach(res.data.data.value, (v: any) => {
              opntions.push({
                value: v.result_name.trim(),
                item_type: v.item_type,
              });
            });
          }
          cb(opntions);
        })
        .catch((err: any) => {
          console.error('getRecommendationSearch failed: ' + JSON.stringify(err));
          cb([]);
        });
    } else {
      const params: any = {
        keywords: this.queryStr,
        objs: this.queryType != 'all' ? this.queryType : 'table,tableau,udf',
        size: 10,
      };
      this.doeRemoteService
        .esNameCompletion(params)
        .then((res: any) => {
          const opntions: Array<any> = [];
          if (res && res.data && res.data.value) {
            _.forEach(_.uniqBy(res.data.value, 'name'), (v: any) => {
              opntions.push({ value: v.name.trim() });
            });
          }
          cb(opntions);
        })
        .catch((err: any) => {
          console.error('user search failed: ' + JSON.stringify(err));
          cb([]);
        });
    }
  }

  handleEnter (e: any) {
    if (e) {
      e.target.blur();
    }
    Logger.counter('METADATA_SEARCH', 1, {
      name: 'metadata_browse',
      trigger: 'press_enter',
    });
    this.handleSearch();
    const input = this.$refs['search-input'] as any;
    if (input) {
      input.activated = false;
    }
  }

  closeSearch () {
    this.queryStr = '';
    this.close();
  }

  navByCategory (cmd: string) {
    cmd && this.$router.push(`/metadata/${cmd}`);
  }
}
</script>
<style lang="scss" scoped>
@import '@/styles/global.scss';

.popover-content {
  display: grid;
  height: 300px;
  grid-template-columns: auto auto;
  padding: 18px 24px;
  .sub-menu {
    list-style-type: none;
      li {
        padding: 3px 0;
        cursor: pointer;
        color: #4d8cca;
        &:hover {
          text-decoration: underline;
          color: darken(#4d8cca, 20);
        }
      }
    h1 {
      font-size: .8em;
      color: #999;
      padding-bottom: 8px;
      &::after {
        content: '>';
        font-weight: lighter;
        color: #CCC;
        padding-left: 8px;
        position: relative;
        top: 1px;
      }
    }
  }
}

.search-bar {
  width: 100%;
  padding-bottom: 20px;
  display: flex;
  justify-content: center;
  align-items: center;


  .el-dropdown-link {
    cursor: pointer;
    > span {
      display: inline-block;
      width: 70px;
      text-align: center;
      & ~ i {
        vertical-align: super;
        margin-right: 15px;
      }
    }
  }
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
      font-family: Arial, Helvetica, sans-serif;
    }
  }
  .search-visible {
    padding-bottom: 0 !important;
    display: block;
  }
}
.el-autocomplete-suggestion.recommend-css li {
  line-height: normal;
  padding: 7px 0;
  .sub-item {
    font-size: 12px;
    color: #b4b4b4;
  }
}
.close-icon {
  font-size: 24px !important;
  color: #80868b;
  line-height: 40px !important;
  cursor: pointer;
}
</style>
