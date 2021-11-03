<template>
  <div class="chart-container">
    <h3 class="label">History Statistics</h3>
    <div
      id="chart"
      ref="chartsEle"
    />
    <div class="legend">
      <span><i class="success" />Success</span>
      <span><i class="failed" />Failed</span>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch } from 'vue-property-decorator';
import Echarts from 'echarts';
import { ScheduleHistory, JobRunStatus } from '@/components/common/schedule-container';
import moment from 'moment';

@Component
export default class TaskChart extends Vue {
  @Prop() list: ScheduleHistory[];

  chartInstance: Echarts.ECharts;
  loading = false;

  @Watch('list')
  handleData(){
    if(this.list.length===0) return;
    const data = this.list.slice(0,30).sort((a: ScheduleHistory, b: ScheduleHistory)=>{
      return a.startTime - b.startTime;
    });
    const BASE_TIME = this.formatDate(data[0].startTime);
    const arr: any = [];
    for(let i=0;  i<data.length; i++){
      if(data[i].endTime){
        const day = this.formatDate(data[i].endTime);
        const time = this.getTime(data[i].endTime);
        const status = this.getStatus(data[i]);
        arr.push([day, `${BASE_TIME} ${time}`, status]);
      }else{
        const day = this.formatDate(data[i].startTime);
        arr.push([day, null]);
      }
    }
    arr.sort((a,b)=>{
      const x = moment(a[0]).valueOf();
      const y = moment(b[0]).valueOf();
      return x - y;
    });
    this.renderChart(BASE_TIME, arr);
  }
  formatTime(time){
    return moment(time).format('HH:mm');
  }
  formatDate(time){
    return moment(time).utcOffset('-07:00').format('YYYY/MM/DD');
  }
  getTime(time){
    return moment(time).utcOffset('-07:00').format('HH:mm');
  }
  getStatus(job: ScheduleHistory){
    return job.jobRunStatusInfo&&job.jobRunStatusInfo.jobRunStatus === JobRunStatus.SUCCESS ? 1:0;
  }
  mounted () {
    this.loading = true;
    const ele = this.$refs.chartsEle as HTMLDivElement;
    this.chartInstance = Echarts.init(ele);
    window.addEventListener('resize', this.resize);
  }

  destroyed () {
    window.removeEventListener('resize', this.resize);
  }

  renderChart(base_time, data){
    const option: Echarts.EChartOption = {
      grid: {
        left: '3%',
        right: '3%',
        bottom: '3%',
        containLabel: true,
      },
      tooltip: {
        show: true,
        formatter: (param,a)=>{
          const _param = param as any;
          return `${_param.data[0]} ${_param.data[1].split(' ')[1]}`;
        },
      },
      xAxis: {
        type: 'category',
        // boundaryGap: false,
        axisTick: {
          alignWithLabel: true,
        },
      },
      yAxis: {
        type: 'time',
        name: 'Finished Time',
        splitLine: {
          lineStyle: {
            type: 'dashed',
            color: '#DDDDDD',
          },
        },
        min: `${base_time} 00:00:00`,
        max: `${base_time} 23:59:59`,
        axisLabel:{
          formatter:  (value)=> {
            return this.formatTime(value);
          },
        },
      },
      series: [{
        symbolSize: 10,
        itemStyle: {
          color: (data) => {
            return data.value[2]===1?'#67C23A':'#f00';
          },
        },
        data: data,
        type: 'scatter',
      }],
    };
    this.chartInstance.setOption(option);
  }

  resize() {
    if (this.chartInstance) {
      this.$nextTick(() => {
        this.chartInstance.resize();
      });
    }
  }

}
</script>

<style lang="scss"  scoped>
.chart-container{
  margin: 20px 0;
}
.label{
  line-height: 43px;

}
#chart{
  height: 400px;
  width: 100%;
}
.legend{
  text-align: center;
  line-height: 20px;
  >span{
    margin-left: 10px;
    i{
      display: inline-block;
      width: 10px;
      height: 10px;
      border-radius: 50%;
      margin-right: 5px;
      &.success{
        background-color:#67C23A;
      }
      &.failed{
        background-color: #ff0000;
      }
    }
  }
}
</style>
