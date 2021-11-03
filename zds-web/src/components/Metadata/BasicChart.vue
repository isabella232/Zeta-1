<template>
  <div class='Charts' ref='chartsEle' style='width: 360px; height: 200px;'>
      
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop } from "vue-property-decorator";
import _ from "lodash";
import * as Echarts from 'echarts';
@Component({
  components: {}
})
export default class BasicChart extends Vue {
  @Prop() data: any;
  @Prop() title: any;
  @Prop() subTitle: any;

  chartInstance!: Echarts.ECharts;

  resize() {
    if (this.chartInstance) {
      this.$nextTick(() => {
        this.chartInstance.resize();
      });
    }
  }

  mounted() {
    const ele = this.$refs.chartsEle as HTMLDivElement;
    this.chartInstance = this.initCharts(ele);
    setTimeout(() => {
      this.renderChart();
    }, 100);
  }

  initCharts(element: HTMLDivElement): Echarts.ECharts {
    return Echarts.init(element);
  }

  renderChart(options?: any) {
    if (this.chartInstance) {
      this.chartInstance.clear();
      this.chartInstance.setOption(
        this.getLineChartsOptions(this.data, options)
      );
      this.$nextTick().then(() => {
        this.chartInstance.resize();
      });
    }
  }

  getLineChartsOptions = (data: any, options?: any): Echarts.EChartOption => {
    return {
      title: {
        text: options && options.title ? options.title : this.title,
        textStyle: {
          fontWeight: 700,
          fontSize: 14
        },
        subtext: options && options.subTitle ? options.subTitle : this.subTitle,
        subtextStyle: {
          color: '#333',
          fontWeight: 400,
          fontSize: 10
        }
      },
      grid: {
        left: 50,
        right: 10,
        bottom: 35
      },
      tooltip: {
        trigger: 'axis',
        /*backgroundColor: 'transparent',
        axisPointer: {
          type: 'none'
        },
        position: function (point, params, dom, rect, size) {
          let x: any = point[0];
          let y: any = point[1];
          return [x - 20, y - 30];
        },*/
        /*textStyle: {
          color: '#333'
        },
        formatter: function (params: any, ticket, callback) {
          const data: string = String(params[0].data);
          return data.replace(/\d{1,3}(?=(\d{3})+$)/g, '$&,') || data;
        }*/
      },
      xAxis: {
        type: 'category',
        data: _.map(this.data, 'date'),
        boundaryGap: false,
        axisLabel: {
          rotate: 45
        },
        /*splitLine: {
          show: false
        },
        axisLine: {
          show: false
        },
        axisLabel: {
          inside: true
        },
        axisTick: {
          show: false
        }*/
      },
      yAxis: {
        type: 'value',
        min: 0,
        axisTick: {
          show: false
        }
      },
      color: ['#569CE1'],
      series: [{
        data: _.map(this.data, 'value'),
        type: 'line',
        smooth: true,
        areaStyle: {
          color: "#EEF5FC"
        },
        symbolSize: 6
      }]
    };
  };
}
</script>
<style lang="scss" scoped>

</style>
