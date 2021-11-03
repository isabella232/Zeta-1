<template>
<div class="plot-container">
  <DatabrickLikePlot 
    v-if='showEditor'
    v-on:config-change='configChange'
    v-on:close='closeEditor'
    :dataIn='dataIn' 
    :plot-config='plotConfig' 
    :isEditorMode='true'></DatabrickLikePlot>

  <DatabrickLikePlot :dataIn='dataIn' :plot-config='newPlotConfig' :isEditorMode='false' ref="plotDisplay"></DatabrickLikePlot>
</div>
</template>
<script lang='ts'>
import { Vue, Component, Prop, Watch } from 'vue-property-decorator';
import DefaultPlot from './default-plot.vue';
import DatabrickLikePlot from './databrick-like-plot.vue';
import { DataType, PlotConfig } from './utilities';
import './style.scss';
import { PivotTableRender } from './renders/render-api';
// export type PlotDisplayStyle = 'default'|'databrick-like';

@Component({
  components: { DefaultPlot, DatabrickLikePlot }
})
export default class PlotContainer extends Vue {
  @Prop() dataIn!: DataType;
  // @Prop() displayStyle!: PlotDisplayStyle;
  @Prop() plotConfig!: PlotConfig;
  // @Prop() editorMode!: boolean;
  @Prop({ default: false, type: Boolean })
  showEditor!: boolean;

  newPlotConfig: PlotConfig = {} as PlotConfig;

  constructor() {
    super();
  }

  @Watch('plotConfig')
  plotConfigChange(newVal: PlotConfig) {
    this.newPlotConfig = newVal;
  }

  beforeMount() {
    this.newPlotConfig = this.plotConfig;
  }

  configChange(newVal: PlotConfig) {
    this.newPlotConfig = newVal;
    this.$emit('config-change', newVal);
  }

  closeEditor() {
    this.$emit('update:showEditor', false);
  }
  editClick() {
    this.$emit('update:showEditor', true);
  }
  resize() {
    let plotDisplay = this.$refs.plotDisplay as any;
    let plotRender = plotDisplay.$render as PivotTableRender;
    plotRender.resize();
  }
}
</script>
<style lang="scss" scoped>
.plot-container {
  height: 100%;
}
</style>
