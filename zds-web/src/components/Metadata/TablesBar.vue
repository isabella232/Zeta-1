<template>
  <div class="bar">
      <div class="item-div" v-for="(item, index) in data" :keys="item.title">
        <el-popover placement="bottom-start" trigger="hover" v-if="!isCompare(item.compare)">
          <BasicChart :title="item.title" :subTitle="item.subTitle" :data='formatChartData(item.data)'/>
          <div style="width: 100%;" slot="reference" class="separator-line">
            <span class="bar-name">{{ item.title }}</span>
            <span class="count down-color" v-if="item.compare < 0">{{ formatThousandth(item.value) }}<i class="zeta-icon-arrow down-icon"></i></span>
            <span class="count eq-color" v-if="item.compare == 0">{{ formatThousandth(item.value) }}<i class="zeta-icon-arrow eq-icon"></i></span>
            <span class="count up-color" v-if="item.compare > 0">{{ formatThousandth(item.value) }}<i class="zeta-icon-arrow up-icon"></i></span>
          </div>
        </el-popover>
        <div style="width: 100%;" class="separator-line" v-else>
          <span class="bar-name">{{ item.title }}</span>
          <span class="count eq-color">{{ formatThousandth(item.value) }}</span>
        </div>
      </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop } from "vue-property-decorator";
import BasicChart from '@/components/Metadata/BasicChart.vue';
import moment from 'moment';
import _ from "lodash";
import $ from 'jquery';
@Component({
  components: {
    BasicChart
  }
})
export default class TablesBar extends Vue {
  @Prop() data: any;

  isCompare(compare: any) {
    return compare === false;
  }
  
  formatThousandth(number: any) {
    if (_.isNumber(number)) {
      var reg = /\d{1,3}(?=(\d{3})+$)/g;
      return (number + '').replace(reg, '$&,');
    }

    return number;
  }

  formatChartData(data: any) {
    let rs: any = [];
    _.forEach(data, (v: any) => {
      const obj: any = {
        date: moment(v.cre_date).format("MM/DD"),
        value: v.table_cnt,
        tooltipDate: v.cre_date
      }
      rs.push(obj);
    })
    return rs;
  }

  mounted() {
    //this.resize();
  }

  resize() {
    let currentWidth = 0;
    _.forEach($(".item-div"), (v: any) => {
      $(v).css("left", currentWidth);
      $(v).css("position", "absolute");
      currentWidth = currentWidth + ($(v).width() || 0);
    })
  }
}
</script>
<style lang="scss" scoped>
.bar {
  height: 100%;
  width: 100%;
  display: flex;
  justify-items: center;
  overflow-x: auto;
  overflow-y: hidden;
  .item-div {
    cursor: pointer;
    line-height: 29px;
    padding-left: 20px;
    display: inline-block;
    flex: 0 0 auto;
    .bar-name {
      font-size: 14px;
    }
  }
  .eq-color {
    color: #CACBCF;
    font-weight: 700;
    padding: 0 10px;
  }
  .up-color {
    color: #5CB85C;
    font-weight: 700;
    padding: 0 10px;
  }
  .down-color {
    color: #E53917;
    font-weight: 700;
    padding: 0 10px;
  }
  .eq-icon {
    color: #CACBCF;
    display: inline-block;
    transform: rotate(-90deg);
  }
  .up-icon {
    color: #5CB85C;
    display: inline-block;
    transform: rotate(180deg);
  }
  .down-icon {
    color: #E53917;
    display: inline-block;
  }
}
.bar::-webkit-scrollbar {
    height: 5px;
}
.bar::-webkit-scrollbar-thumb {
    background-color: #c1c1c1;
}
.separator-line {
  width: 1px;
  border-right: 1px solid #cacbcf;
}
</style>
