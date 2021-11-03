<template>
  <el-popover
    placement="bottom-end"
    width="350"
    visible-arrow="false"
    trigger="click"
    style="width: 100%; padding: 0;"
  >
    <el-date-picker
      v-model="dateValue"
      type="daterange"
      align="right"
      unlink-panels
      range-separator="To"
      start-placeholder="Start Date"
      end-placeholder="End Date"
      :picker-options="pickerOptions"
      value-format="yyyy-MM-dd"
      @change="handleDateChange"
    />
    <div
      slot="reference"
      style="width: 100%; padding: 0;"
    >
      <span class="el-dropdown-link">
        {{ title }}<i
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
    </div>
  </el-popover>
</template>

<script lang="ts">
import { Component, Vue, Prop } from 'vue-property-decorator';
import _ from 'lodash';
@Component({
  components: {}
})
export default class DatepickerFilterComponent extends Vue {
  @Prop() title: any;
  @Prop() type: any;
  @Prop() sortable: any;
  @Prop() property: any;

  dateValue: any = '';
  pickerOptions: any = {
    shortcuts: [{
      text: 'Last Week',
      onClick(picker: any) {
        const end: any = new Date();
        const start: any = new Date();
        start.setTime(start.getTime() - 3600 * 1000 * 24 * 7);
        picker.$emit('pick', [start, end]);
      }
    }, {
      text: 'Last Month',
      onClick(picker: any) {
        const end: any = new Date();
        const start: any = new Date();
        start.setTime(start.getTime() - 3600 * 1000 * 24 * 30);
        picker.$emit('pick', [start, end]);
      }
    }, {
      text: 'Last 3 Months',
      onClick(picker: any) {
        const end: any = new Date();
        const start: any = new Date();
        start.setTime(start.getTime() - 3600 * 1000 * 24 * 90);
        picker.$emit('pick', [start, end]);
      }
    }]
  };

  get isSelected() {
    return _.isEmpty(this.dateValue);
  }

  handleDateChange() {
    if (this.property) this.$emit('call-back', _.isEmpty(this.dateValue) ? undefined : this.dateValue, this.property);
  }

  sortClk(sort: any) {
    this.$emit('sort-clk', sort, this.property);
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
.zeta-icon-filter {
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
