<template>
  <div v-loading="loading">
    <div id="output" ></div>
  </div>
</template>
<script lang="ts">
import { Component, Vue, Prop, Watch } from 'vue-property-decorator';
import { PivotTable }  from './index';

@Component({

})
export default class JQPivotTable extends Vue{
  @Prop() data: string[];

  loading = true;
  pivot: any = null;
  constructor(){
    super();
  }

  mounted () {
    console.log('mounted');
  }
  @Watch('data')
  setdata(newVal: string[], oldVal: string[]){
    this.pivot = PivotTable('output',newVal);
  }
  @Watch('pivot')
  setLoad(newVal: any, oldVal: any){
    if(newVal){
      this.loading = false;
    }
  }
}
</script>

<style lang="scss" scoped>
$MaxHeight: 800px;
$MaxWidth: 1600px;
#output{
  height: calc(100% - 20px);
  overflow: auto;
  /deep/ .pvtUi{
    border-collapse: collapse;
    border-spacing: 0;
    max-width: 100%;
    tbody{
      td,
      th{
        border: 1px solid #ddd;
      }
    }
    .pvtVertList{
      display: block;
      max-height: $MaxHeight;
      overflow: auto;
    }
    .pvtRendererArea{
      border-style: none;
      padding: 0;
      .pvtTable{
        margin: 5px 0 0 5px;
        max-width: $MaxWidth;
        display: block;
        max-height: $MaxHeight;
        overflow: auto;
      }
      .js-plotly-plot{
        max-height: $MaxHeight;
      }
    }
    .pvtUiCell{
      select{
        background-color: #ffffff;
        border:1px solid #ddd;
      }
      .pvtAttrDropdown{
        width: calc(100% - 10px);
        margin: 0 5px;
        display: block;
      }
      .pvtRenderer{
        width: calc(100% - 20px);
        margin: 0 10px;
      }
      .pvtAggregator{
        width: calc(100% - 50px);
      }
      .pvtRowOrder,
      .pvtColOrder{
        color: #333333;
      }
    }
    .pvtAxisContainer li span.pvtAttr{
      background-color: #ffffff;
    }
    .pvtAxisContainer, .pvtVals{
      min-width: 120px;
    }
    .pvtFilterBox{
      button{
        background-color: #ffffff;
        border: 1px solid #ddd;
        padding: 1px 3px;
        margin-left: 5px;
        border-radius: 4px;
        cursor: pointer;
      }
    }
  }
}
</style>
