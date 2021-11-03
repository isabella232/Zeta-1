<template>
  <div class="search-box">
    <div class="icon-search">
      <i class="el-icon-search" />
    </div>
    <div class="search-input">
      <el-autocomplete
        ref="search-input"
        v-model="queryStr"
        :popper-class="queryStr == '' ? 'recommend-css' : ''"
        :fetch-suggestions="fetchSuggestions"
        :debounce="500"
        placeholder="Search everything in Zeta..."
        @select="handleSearch"
        @keyup.enter.native="handleEnter($event)"
      />
      <i
        v-if="queryStr"
        class="el-icon-close"
        @click="() => queryStr = ''"
      />
    </div>
  </div>
</template>
<script lang="ts">
import { Component, Vue, Watch } from 'vue-property-decorator';
import Util from '@/services/Util.service';
import DoeRemoteService from '@/services/remote/DoeRemoteService.ts';
import { isEmpty, uniqBy } from 'lodash';
import * as Logger from '@/plugins/logs/zds-logger';

const API = DoeRemoteService.instance;
@Component({})

export default class SearchBox extends Vue {
  queryStr = '';
  isActive = false;

  async fetchRecommendation () {
    const params = {
      nt_login: Util.getNt(),
    };
    return await API
      .getRecommendationSearchHist(params)
      .then(res => {
        return res.data.data.value.map(i => ({
          value: i.result_name.trim(),
          item_type: i.item_type,
        }));
      });
  }
  @Watch('$route.query.keyword', { immediate: true })
  initQueryStr (keyword) {
    if (keyword) {
      this.queryStr = keyword;
    }
  }
  fetchSuggestions (queryStr: string, cb: any) {
    if (isEmpty(queryStr)) {
      this.fetchRecommendation().then(res => cb(res));
    } else {
      const params: any = {
        keywords: this.queryStr,
        size: 10,
      };
      API.esNameCompletion(params)
        .then((res: any) => {
          const options = uniqBy(res.data.value, 'name').map((i: any) => ({ value: i.name }));
          cb(options);
        });
    }
  }
  handleSearch ({ value }) {
    this.$router.push(`/metadata/search?keyword=${value}&offset=0&type=all`);
  }
  handleEnter () {
    Logger.counter('METADATA_SEARCH', 1, {
      name: 'metadata_browse',
      trigger: 'press_enter',
    });
    this.$router.push(`/metadata/search?keyword=${this.queryStr}&offset=0&type=all`);
  }
}
</script>
<style lang="scss" scoped>
.search-box {
  display: inline-block;
  height: 56px;
  overflow: hidden;
  transition: 0.3s;

  .icon-search {
    display: inline-block;
    padding-top: 15px;
  }
  .search-input {
    float: right;
    width: 360px;
    border: 0;
    border-bottom: 2px solid #659cdc;
  }
  .inactive {
    width: 1px;
  }
  i {
    cursor: pointer;
  }

  ::v-deep input {
    display: flex;
    flex-grow: 1;
    border: 0;
    height: 52px;
    width: 300px;
  }
}
</style>