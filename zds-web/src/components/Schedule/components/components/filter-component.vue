<template>
  <el-dropdown
    :hide-on-click="false"
    trigger="click"
    style="width: 100%; padding: 0;"
    @visible-change="onVisibleChange"
  >
    <span class="el-dropdown-link">
      <span>{{ title }}</span><i
        class="zeta-icon-filter el-icon--right"
        :class="isSelected ? 'highlight' : ''"
      />
    </span>
    <el-dropdown-menu
      slot="dropdown"
      style="max-height: 248px; overflow-y: auto; padding: 12px 0;"
    >
      <!-- <el-input
        ref="inputRef"
        v-model="searchTxt"
        placeholder="Search"
        clearable
      /> -->
      <el-checkbox
        v-model="checkAll"
        class="all-chkbox"
        :indeterminate="isIndeterminate"
        @change="handleCheckAllChange"
      >
        All
      </el-checkbox>
      <el-checkbox-group
        v-model="ckValue"
        @change="filter"
      >
        <el-dropdown-item
          v-for="(item, $i) in filteredOptions"
          :key="$i"
        >
          <el-checkbox
            :label="getValue(item)"
          >
            {{ item.label || item }}
          </el-checkbox>
        </el-dropdown-item>
      </el-checkbox-group>
    </el-dropdown-menu>
  </el-dropdown>
</template>

<script lang="ts">
import { Component, Vue, Prop, Emit, Watch } from 'vue-property-decorator';
import _ from 'lodash';
// import { matchString } from '../Metadata/utils';

interface KeyLabel {
  key: string;
  label: string;
}
@Component({
  components: {}
})
export default class FilterComponent extends Vue {
  @Prop() optionsData: (string | KeyLabel)[];
  @Prop() title: string;
  @Prop() type: string;
  @Prop() sortable: boolean;
  @Prop() defaultSelected: any;
  @Prop() property: string;

  searchTxt = '';

  ckValue: any = this.defaultSelected ? this.defaultSelected.split(',') : [];
  checkAll = false;
  isIndeterminate = true;

  get isSelected() {
    return (this.ckValue.length > 0) && (this.ckValue.length != this.optionsData.length);
  }

  get filteredOptions() {
    const searchTxt = this.searchTxt.trim();
    if (!searchTxt) {
      return this.optionsData;
    }
    return this.optionsData.filter((data) => {
      if (typeof data === 'string') {
        return this.matchString(searchTxt, data);
      }
    });
  }

  getValue(item: string | KeyLabel) {
    if (typeof item === 'string') {
      return item;
    } else {
      return item.key || '';
    }
  }

  handleCheckAllChange(val: any) {
    this.ckValue = val ? this.filteredOptions.map(o => {
      if (typeof o === 'string') {
        return o;
      } else {
        return o.key;
      }
    }) : [];
    this.isIndeterminate = false;
    if (this.property) this.$emit('call-back', undefined, this.property);
  }

  filter(val: any) {
    const checkedCount = val.length;
    this.checkAll = checkedCount === this.optionsData.length;
    this.isIndeterminate = checkedCount > 0 && checkedCount < this.optionsData.length;
    if (this.property) this.$emit('call-back', _.isEmpty(this.ckValue) ? undefined : this.ckValue , this.property);
  }

  matchString(input: string | string[], target) {
    if (!input) return true;
    if (typeof input === 'string') {
      input = [input];
    }

    return new RegExp(`(${input.join('|').toLowerCase()})`).test(
      (target || '').toLowerCase()
    );
  }

  @Watch('searchTxt')
  @Emit('change')
  onChange() {
    return this.searchTxt;
  }

  @Emit('visible-change')
  onVisibleChange(visible: any) {
    // visible && Vue.nextTick(() => (this.$refs.inputRef as Element as HTMLInputElement).focus());
    return visible;
  }
}
</script>
<style lang="scss" scoped>
.el-dropdown-link {
  cursor: pointer;
  color: #333;
  font-size: 13px;
  line-height: 40px;
}
.el-dropdown-menu {
  .el-input {
    width: calc(100% - 40px);
    margin: 5px 20px 16px;
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
.all-chkbox {
  padding: 0 20px 6px;
}
</style>
