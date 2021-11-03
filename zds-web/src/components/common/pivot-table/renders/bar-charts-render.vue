<template>
  <div class='Charts' ref='chartsEle' v-loading='loading'>

  </div>
</template>

<script lang='ts'>
	import {Component, Prop, Vue, Watch} from 'vue-property-decorator';
	import * as Echarts from 'echarts';
	import {PivotData, EChartsHelper} from '../utilities';
	import {PivotTableRender} from './render-api';
	import * as _ from 'lodash';

	@Component
export default class BarChartRender extends Vue implements PivotTableRender {
  @Prop() plotData!: PivotData;

  chartInstance!: Echarts.ECharts;
  loading: boolean = false;
  resize() {
    if (this.chartInstance) {
      this.$nextTick(() => {
        this.chartInstance.resize();
      });
    }
  }
  // ----- computed properties
  get colAttrs() {
    return this.plotData.props.cols;
  }

  get rowAttrs() {
    return this.plotData.props.rows;
  }

  get rowKeys() {
    return this.plotData.getRowKeys();
  }

  get colKeys() {
    return this.plotData.getColKeys();
  }

  get grandTotalAggregator() {
    return this.plotData.getAggregator([], []);
  }

  rowsGenerator() {
    return this.rowKeys.map((rowKey:any, index:any) => {
      return { rowKey, aggr: this.plotData.getAggregator(rowKey, []), index };
    });
  }
  rowDataGenerator(rowKey: string[], colKeys: string[][], rowIndex: number) {
    return colKeys.map((colKey, index) => {
      return {
        aggr: this.plotData.getAggregator(rowKey, colKey),
        colIndex: index
      };
    });
  }
  colTotalGenerator(colKeys: string[][]) {
    return colKeys.map((colKey, index) => {
      return { aggr: this.plotData.getAggregator([], colKey), index };
    });
  }

  getBarChartsOptions = (
    rowKeys: any[],
    colKeys: string[][]
  ): Echarts.EChartOption => {
    const xAxis: string[] = colKeys.map((colArr: string[]) => {
      return colArr.join('-');
    });
    let series: any[] = rowKeys.slice(0, 20).map((rk: any, index: number) => {
      const rData = this.rowDataGenerator(rk.rowKey, colKeys, index);
      const serieData = rData.map((d: any) => d.aggr.value() || 0);
      const serie = {
        name: rk.rowKey.join('-'),
        type: 'bar',
        data: serieData,
        barMinHeight: 10,
        barMaxWidth: 30
      };

      return serie;
    });
    if (rowKeys.length === 0) {
      const rData = this.colTotalGenerator(colKeys);
      const serieData = rData.map((d: any) => d.aggr.value() || 0);
      series = [
        {
          name: 'Total',
          type: 'bar',
          data: serieData
        }
      ];
    }
    let axisNameOption = EChartsHelper.getAxisLabelName(this.plotData);
    return {
    grid: EChartsHelper.getDefaultChartGrid(),
		xAxis: {
			type: 'category',
      data: xAxis,
      name: axisNameOption.xAxis
		},
		yAxis: {
      type: 'value',
      name: axisNameOption.yAxis,
      nameTextStyle: {
        fontSize: 14
      }
		},
		tooltip: {
      trigger: 'axis',
      confine: true
    },
    legend:{
      type: 'scroll',
      bottom: '0px'
    },
     toolbox: EChartsHelper.getDefaultToolbox(),
		series
	};
  };
  mounted() {
    this.loading = true;
    const ele = this.$refs.chartsEle as HTMLDivElement;
    this.chartInstance = this.initCharts(ele);
    setTimeout(() => {
      this.renderChart();
    }, 100);
  }
  initCharts(element: HTMLDivElement): Echarts.ECharts {
    return Echarts.init(element);
  }
  renderChart() {
    // const options = getLineChartsOptions(data);
    if (this.chartInstance) {
      this.chartInstance.clear();
      this.chartInstance.setOption(
        this.getBarChartsOptions(this.rowsGenerator(), this.colKeys)
      );
      this.$nextTick().then(() => {
        this.chartInstance.resize();
        this.loading = false;
      });
    }
  }

  @Watch('plotData', { deep: true })
  onPlotDataChange(newVal: PivotData, oldVal: PivotData) {
    if (!_.isEqual(newVal, oldVal)) {
      this.renderChart();
    }
  }
}
</script>

<style scoped>
</style>
