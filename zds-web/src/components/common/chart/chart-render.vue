<template>
  <div>
    <div class="chart-box">
        <div class="tools" @mousedown="drag" @click="setSelection">
            <i class="el-icon-close" @click.stop="remove"></i>
        </div>
        <div class="Charts" ref="chartsEle" style="minHeight:200px;height:100%;"></div>
    </div>
  </div>
</template>

<script lang='ts'>
import { Component, Prop, Vue, Watch } from "vue-property-decorator";
import * as Echarts from "echarts";
import * as _ from "lodash";

@Component({
    components:{
    }
})
export default class ChartRender extends Vue {
  @Prop() xAxis: Array<any>;
  @Prop() legend: Array<any>;
  @Prop() series: Array<any>;
  chartInstance!: Echarts.ECharts;
  testLayout = [
	    {"x":0,"y":0,"w":12,"h":6,"i":"0"}
	];
  constructor() {
    super();
  }
  resize() {
    if (this.chartInstance) {
      this.$nextTick(() => {
        this.chartInstance.resize();
      });
    }
  }
  getChartsOptions = (): Echarts.EChartOption => {
    return {
      grid: {
          top: '20px',
          containLabel: true
      },
      xAxis: {
        type: "category",
        data: this.xAxis
      },
      yAxis: {
        type: "value"
      },
      tooltip: {
        trigger: "axis"
      },
      legend: {
        type: "scroll",
        bottom: "0px",
        data: this.legend
      },
      series: this.series
    };
  };
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
  renderChart() {
    if (this.chartInstance) {
      this.chartInstance.clear();
      this.chartInstance.setOption(this.getChartsOptions());
      this.$nextTick().then(() => {
        this.chartInstance.resize();
      });
    }
  }
  resizeEvent(){
      this.renderChart();
  }
  remove(){
      this.$emit("remove")
  }
  setSelection(){
      this.$emit('set-selection')
  }
  drag(e: any){
      let target = e.target; 
      let parentNode = target.parentNode;
      let disX = e.clientX - parentNode.offsetLeft;
      let disY = e.clientY - parentNode.offsetTop;
      let originClientX = e.clientX;
      let originClientY = e.clientY;
      document.onmousemove = (e)=>{  
          let left = e.clientX - disX;    
          let top = e.clientY - disY;
          let minLeft = 0;
          let minTop = 0;
          if (left <= minLeft) {
              left = minLeft;
          } else if (left >= document.documentElement.clientWidth - parentNode.offsetWidth){
              left = document.documentElement.clientWidth - parentNode.offsetWidth;
          }
          
          if (top <= minTop) {
              top = minTop;
          } else if (top >= document.documentElement.clientHeight - parentNode.offsetHeight){
              top = document.documentElement.clientHeight - parentNode.offsetHeight
          }

          parentNode.style.left = left + 'px';
          parentNode.style.top = top + 'px';
      };
      document.onmouseup = (e) => {
          document.onmousemove = null;
          document.onmouseup = null;
      };
  }
  @Watch("series")
  handle() {
    this.renderChart();
  }
}
</script>

<style scoped lang="scss">
.vue-grid-layout{
    position: absolute;
    top: 50px;
    left: 100px;
    min-width: 400px;
}
.vue-grid-item{
    background-color: #fff;
    border: 1px solid #ccc;
}
.chart-box{
    position: absolute;
    top: 50px;
    left: 100px;
    z-index: 99;
    width: 500px;
    background-color: #fff;
    border: 1px solid #ccc;
    .tools{
        cursor: move;
        background-color: #f0f0f0;
        border-radius: 2px 2px 0 0;
        padding: 3px 5px;
        border-bottom: 1px solid #ccc;
        display: flex;
        justify-content: flex-end;
        i{
            margin-left: 10px;
            cursor: pointer;
        }
    }
}
</style>
