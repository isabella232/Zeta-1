<template>
  <el-dropdown
    :hide-on-click="false"
    trigger="click"
    style="width: 100%; padding: 0;"
    @visible-change="onVisibleChange"
  >
    <span class="el-dropdown-link">
      <span style="line-height:40px">{{ title }}</span>
      <i
        class="zeta-icon-filter el-icon--right"
        :class="isSelected ? '' : 'highlight'"
      />
    </span>
    <span
      v-if="sortable"
      class="caret-wrapper"
    >
      <i
        class="sort-caret ascending"
        @click="sortClk('asc')"
      />
      <i
        class="sort-caret descending"
        @click="sortClk('desc')"
      />
    </span>
    <el-dropdown-menu
      slot="dropdown"
      style="max-height: 200px; overflow-y: auto;"
    >
      <el-input
        ref="inputRef"
        v-model="searchTxt"
        placeholder="Search"
        clearable
      />
    </el-dropdown-menu>
  </el-dropdown>
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch } from 'vue-property-decorator';
import _ from 'lodash';
@Component({
  components: {},
})
export default class SearchComponent extends Vue {
  @Prop() title: any;
  @Prop() sortable: any;
  @Prop() property: any;

  searchTxt: any = '';
  private debounceSearch: Function;

  constructor() {
    super();
    this.debounceSearch = _.debounce(this.searchHandler, 500);
  }

  get isSelected() {
    return _.isEmpty(this.searchTxt);
  }

  searchHandler(queryStr: string = this.searchTxt) {
    this.$emit('call-back', queryStr, this.property);
  }

  sortClk(sort: any) {
    this.$emit('sort-clk', sort, this.property);
  }

  onVisibleChange(visible: boolean) {
    visible && Vue.nextTick(() => (this.$refs.inputRef as HTMLInputElement).focus());
  }

  @Watch('searchTxt')
  onSearchChange(txt: string) {
    this.debounceSearch(txt);
  }
}
</script>
<style lang="scss" scoped>
.el-dropdown-link {
  cursor: pointer;
  color: #333;
  font-size: 13px;
}
.el-dropdown-menu {
  .el-input {
    width: calc(100% - 40px);
    margin: 5px 20px;
  }
}
.zeta-icon-filter {
  cursor: pointer;
  float: right;
  line-height: 40px;
  color: #569CE1;
  font-weight: bold;
}
.zeta-icon-filter:hover {
  color:#4D8CCA;
}
.highlight {
  color:#DB9D26;
}
</style>
