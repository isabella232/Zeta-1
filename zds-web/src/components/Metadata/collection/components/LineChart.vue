<template>
  <div class="line-chart">
    <table-filter
      :collection-data="collectionData"
      @query-chart="getData"
    />
    <v-chart
      :options="lineChartOptions"
      autoresize
      style="width:100%;height:calc(100% - 86px);"
    />
  </div>
</template>

<script lang="ts">
/* eslint-disable @typescript-eslint/camelcase */
import { Vue, Component, Prop } from 'vue-property-decorator';
import moment from 'moment';
import TableFilter from './TableFilter.vue';
import ECharts from 'vue-echarts';
import 'echarts/lib/chart/line';
import {
  Last30DaysLabel,
  getHistoryDataFormatFloat,
  getFailureData,
} from '@/components/WorkSpace/Metadata/sub-view/Operation/chartOptions';
import DoeRemoteService from '@/services/remote/DoeRemoteService';
import _ from 'lodash';

@Component({
  components: {
    TableFilter,
    'v-chart': ECharts,
  },
})
export default class LineChart extends Vue {
  @Prop() title;
  @Prop() collectionData: any;

  doeRemoteService: DoeRemoteService = DoeRemoteService.instance;
  data: any = [];
  rateData: any = [];
  getData(platform: string, tableList: any) {
    this.data = [];
    this.rateData = [];
    this.doeRemoteService
      .getCollectionFinshTimeLine(platform, tableList)
      .then((res: any) => {
        if (res && res.data && res.data.value) {
          this.data = res.data.value;
        }
      });
    // this.doeRemoteService
    // 	.getReconFailureRate(platform, tableList)
    // 	.then((res: any) => {
    // 		if (res && res.data && res.data.value) {
    // 			this.rateData = res.data.value;
    // 		}
    // 	});
  }

  get lineChartOptions() {
    const dataAll: any = _.union(this.data, this.rateData);
    const series = dataAll.map((h: any) => {
      return {
        type: 'line',
        //name: h.platform,
        name: h.db_name + '.' + h.table_name,
        data: h.db_name
          ? getHistoryDataFormatFloat(h.hist)
          : getFailureData(h.hist),
        yAxisIndex: h.db_name ? 0 : 1,
        // itemStyle: {
        // 	color: this.getSeriesColor(h.platform)
        // },
        lineStyle: {
          type: h.db_name ? 'solid' : 'dashed',
        },
      };
    });
    return {
      tooltip: {
        trigger: 'axis',
        formatter(params) {
          const colorSpan = color =>
            '<span style="display:inline-block;margin-right:5px;border-radius:10px;width:9px;height:9px;background-color:' +
						color +
						'"></span>';
          let html = '<p>' + params[0].axisValue + '</p>';
          const transform = _.transform(params, (rs: any, v: any) => {
            v.platform = dataAll[v.seriesIndex].platform;
            rs.push(v);
          }, []);
          const orderParam = _.orderBy(
            transform,
            ['platform', 'value'],
            ['desc', 'desc']
          );
          let currentPltform = '';
          orderParam.forEach((item: any) => {
            const data = dataAll[item.seriesIndex];
            if (currentPltform != item.platform) {
              currentPltform = item.platform;
              switch (_.toLower(currentPltform)) {
                case 'hercules':
                  html += '<p>Hercules:</p>';
                  break;
                case 'hermes':
                  html += '<p>Hermes:</p>';
                  break;
                case 'apollo':
                  html += '<p>Apollo:</p>';
                  break;
                case 'apollo_rno':
                  html += '<p>Apollo RNO/Hermes:</p>';
                  break;
                case 'ares':
                  html += '<p>Ares:</p>';
                  break;
                case 'mozart':
                  html += '<p>Mozart:</p>';
                  break;
                case 'numozart':
                  html += '<p>Mozart:</p>';
                  break;
                case 'hopper':
                  html += '<p>Hopper:</p>';
                  break;
                default:
                  break;
              }
            }
            const findFinshiTime = _.find(data.hist, (v: any) => {
              return (
                v.finish_time &&
								moment(v.finish_time)
								  .utcOffset('-0700')
								  .format('MM/DD') == item.axisValue
              );
            });
            const findFailureRate = _.find(data.hist, (v: any) => {
              return v.date && moment(v.date).format('MM/DD') == item.axisValue;
            });
            const line =
							'<p>' +
							colorSpan(item.color) +
							' ' +
							(data.db_name && data.table_name
							  ? data.db_name + '.' + data.table_name
							  : 'Failure Rate') +
							': ' +
							(findFinshiTime
							  ? moment(findFinshiTime.finish_time).utcOffset('-0700').format('YYYY-MM-DD HH:mm:ss')
							  : findFailureRate
							    ? findFailureRate.failure_rate * 100 + '%'
							    : '') +
							'</p>';
            html += line;
          });
          return html;
        },
      },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: Last30DaysLabel,
        splitLine: { show: true },
      },
      yAxis: [
        {
          type: 'value',
          min: 0,
          max: 24,
          splitNumber: 12,
          minorSplitLine: {
            show: false,
          },
          splitLine: { show: false },
        },
        {
          type: 'value',
          min: 0,
          max: 1,
          axisLabel: {
            formatter(value) {
              return value * 100 + '%';
            },
          },
          minorSplitLine: {
            show: false,
          },
          splitLine: { show: false },
        },
      ],
      grid: {
        left: '5%',
      },
      series,
      legend: {
        type: 'scroll',
        width: '80%',
        bottom: 0,
        data: _.map(series, 'name'),
      },
    };
  }

  getSeriesColor(platform: string) {
    switch (_.toLower(platform)) {
      case 'hercules':
        return '#d88045';
      case 'hermes':
        return '#e2b258';
      case 'apollo':
        return '#569ce1';
      case 'apollo_rno':
        return '#569ce1';
      case 'ares':
        return '#d85945';
      case 'mozart':
        return '#9a6197';
      case 'numozart':
        return '#9a6197';
      case 'hopper':
        return '#608d75';
      default:
        return undefined;
    }
  }
}
</script>
<style lang="scss" scoped>
@import "@/styles/metadata.scss";

.line-chart {
  height: 100%;
  h1 {
    font-size: 16px;
    padding-bottom: 12px;
  }
}
</style>
