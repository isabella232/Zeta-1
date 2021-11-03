<template>

	<div v-if='pivotData != null' class="pivot-render">
		<div v-if='selectedRender === "Table"' class="pivot-table-render">
			<TableRender ref="render" :plot-data='pivotData'></TableRender>
		</div>
		<div v-else-if='selectedRender === "LineCharts"' class="pivot-chart-render pivot-linechart-render" style="height:100%;"> 
			<LineChartsRender ref="render" :plot-data='pivotData'/>
		</div>
		<div v-else-if='selectedRender === "BarCharts"' class="pivot-chart-render pivot-barchart-render" style="height:100%;"> 
			<BarChartsRender ref="render" :plot-data='pivotData'/>
		</div>
		<div v-else> 
			No render selected
		</div>
	</div>

</template>
<script lang='ts'>
import { Component, Vue, Prop } from 'vue-property-decorator';
import TableRender from './table-render.vue';
import LineChartsRender from './line-charts-render.vue';
import BarChartsRender from './bar-charts-render.vue';
import { PivotData } from '../utilities';
import { PivotTableRender } from './render-api';
@Component({
  components: { TableRender, LineChartsRender, BarChartsRender }
})
export default class PivotRender extends Vue implements PivotTableRender {
  @Prop() pivotData!: PivotData;
  @Prop() selectedRender!: string;
  resize() {
    let render = (this.$refs.render as any) as PivotTableRender;
    console.debug('pivot resize', render);
    if (render) {
      render.resize();
    }
  }
}
</script>
<style lang="scss" scoped>
.pivot-render {
	height: 100%;
	width: 100%;
	.pivot-table-render{
		max-height: 100%;
		max-width: 100%;
		overflow: auto;
	}
}
</style>
