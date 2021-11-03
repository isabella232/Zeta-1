<template>
<div :class="isEditorMode?'':'plot-body'">
  <div v-if='isEditorMode' class='plot-cfg-bg' @click="cfgMaskClick">
    <div class="plot-cfg" ref='cfg'>
      <div class="plot-header">
        <span class="title">Customization</span>
        <i @click="()=> closeCfg()" class="el-icon-close"></i>
      </div>
    
      <div class='plot-body databrick-like-plot'>
        <div class='plot-body-row'>
          <div class='span2 fields-list' style='max-width: 150px;'>
            <div class="filter">
              <div class='label'> Filter</div>
              <el-input v-model="filterKey" />
            </div>
            <div>
              <div class='label'> All fields </div>
              <div class='unused-attrs'> 
                <draggable v-model="unusedAttrs" :options="{group:'attributes'}" class='drag-group'>
                  <div v-for="element in unusedAttrs" :key="element" class='drag-item'>{{element}}</div>
                </draggable>  
              </div>
            </div>
          </div>
          <div class='span2 fields-list' >
            <div class='height33'>
              <div class="label">Display Type</div>
              <Dropdown class="dropdown100p" v-model="selectedChartType" :options="chartOptions"/>
            </div>
            <div class='height33'>
              <div class='label'>xAxis</div>
              <draggable v-model="horizAttrs" :options="{group:'attributes'}" class='drag-group flex-fill-remain'>
                <div v-for="element in horizAttrs" :key="element" class='drag-item'>{{element}}</div>
              </draggable>  
            </div>
            <div class='height33'>
              <div class='label'> Legend</div>
              <draggable v-model="vertAttrs" :options="{group:'attributes'}" class='drag-group flex-fill-remain'>
                <div v-for="element in vertAttrs" :key="element" class='drag-item'>{{element}}</div>
              </draggable>  
            </div>
            <div class='height33' style='float:left'>
              <div class="label">Aggregation</div>
              <Dropdown class="dropdown100p" v-model='selectedAggregator' :options='aggregatorNames'/>
              <template v-if='numberOfAggregatorArgs > 0' >
                <div class="label">Value</div>
                <Dropdown class="dropdown100p" v-model='aggregatorArgs[0]' :options='unusedAttrs'/>
              </template>
            </div>
          </div>
          <div class='span8'>
            <PivotRender :pivot-data="pivotData" :selected-render="selectedChartType" class="render"/>
          </div>
        </div>
      </div>
      <div class="plot-footer">
        <el-button type="primary" @click='apply'>apply</el-button>
        <el-button type="default" plain style="margin-left:10px;" @click="()=> closeCfg()">cancel</el-button>
      </div>
    </div>
  </div>
  <div v-else class='plot-body databrick-like-plot'>
    <PivotRender ref="render" :pivot-data="pivotData" :selected-render="selectedChartType"/>
  </div>
</div>
</template>
<script lang='ts'>
import { Component, Vue, Mixins, Prop, Watch } from 'vue-property-decorator';
import PlotMixin from './plot-mixin';
import draggable from 'vuedraggable';
import PivotRender from './renders/pivot-render.vue';
import Dropdown from './dropdown.vue';
import _ from "lodash";

@Component({
  components: { draggable, PivotRender, Dropdown }
})
export default class DatabrickLikePlot extends Mixins(PlotMixin) {
  // if in editor mode?
  @Prop() isEditorMode!: boolean;
  filterKey: string = '';
  allFields: string[]=[];
  debouncedFilter: Function;
  constructor(){
    super();
    this.debouncedFilter = _.debounce(this.filter, 500, {
          trailing: true
    });
  }

  get $cfg() {
    return this.$refs.cfg;
  }
  cfgMaskClick(e: Event) {
    if (!(this.$cfg as HTMLElement).contains(e.target as any)) {
      this.closeCfg();
    }
  }
  closeCfg() {
    // this.isEditorMode = false;
    this.$emit('close');
  }

  apply() {
    this.$emit('config-change', this.currentPlotConfig);
    this.$emit('close');
  }
  get $render() {
    return this.$refs.render;
  }
  mounted () {
    // this.allFields = this.unusedAttrs;
  }
  filter(){
    this.unusedAttrs = this.getAllFields().filter(item=>{
      return _.toLower(item).indexOf(_.toLower(this.filterKey))>-1;
    })
  }
  getAllFields(){
    let arr1 =  this.allAttrsBackup.filter(item=>!this.horizAttrs.some(ele=>ele===item));
    let arr2 =  arr1.filter(item=>!this.vertAttrs.some(ele=>ele===item));
    return arr2;
  }
  @Watch('filterKey')
  handleFilter(newVal:string,oldVal:string){
    const active = _.trim(newVal).length>0?true:false;
    if(active){
        this.debouncedFilter();
    }else{
        this.unusedAttrs = this.getAllFields();
    }
  }
  // @Watch('unusedAttrs')
  // handleFields(newVal: string[]){
  //   this.allFields = this.unusedAttrs;
  //   console.log(this.allFields,this.unusedAttrs,this.aggregatorArgs);
  // }
  // @Watch('allFields')
  // handleAllFields(newVal: string[]){
  //   this.unusedAttrs = this.allFields;
  // }
}
</script>
<style lang="scss">
$drag-item-background: #569ce1;
$drag-item-border: 1px solid #dcdfe6;
$maxHeight: 500px;

.plot-body {
  width: 100%;
  height: 100%;
  .plot-body-row {
    height: 100%;
    display: grid;
    grid-template-columns: 150px 150px auto;
    grid-column-gap: 20px;
  }
  .plot-body-row:after {
    clear: both;
    display: block;
    content: ' ';
  }
  .plot-body-row [class*='span']:first-child {
    // margin-left: 0;
  }

  .plot-body-row [class*='span'] {
    display: block;
    width: 100%;
    height: 100%;
    // margin-left: 2.127659574468085%;
    box-sizing: border-box;
    // float: left;
  }

  .plot-body-row {
    .span2 {
      // width: 14.893617%;
    }
    .span3 {
      width: 23.4%;
    }
    .span4 {
      width: 31.2%;
    }
    .span5 {
      width: 40.42553191489362%;
    }
    .span7 {
      width: 57.446%;
    }
    .span8 {
      // width: 65.65%;
      overflow: auto;
    }
    .fields-list {
      display: flex;
      flex-direction: column;
      min-width: 150px;
    }
  }

  .label {
    text-align: left;
    padding-top: 5px;
    display: flex;
    align-items: center;
    color: #333;
    // font-family: 'Arial-BoldMT', 'Arial Bold', 'Arial';
    font-family: 'ArialMT', 'Arial';
    font-weight: bold;
    font-size: 14px;
    height: 25px;
    /* line-height: 25px; */
    text-align: left;
    padding-top: 5px;
  }

  .unused-attrs {
    min-height: 150px;
    display: flex;
    flex-direction: row;
    height: 100%;
    max-height: $maxHeight - 85px;
  }

  .drag-item {
    text-align: left;
    // border: $drag-item-border;
    border-radius: 4px;
    box-sizing: border-box;
    margin: 3px 5px;
    padding: 2px 5px;
    /*font-size: 12px;*/
    cursor: move;
    width: fit-content;
    min-width: 100px;
    // float: left;
    background: $drag-item-background;
    color: #fff;
  }

  .drag-group:after {
    clear: both;
    display: table;
    content: '';
  }

  .drag-group {
    overflow: auto;
    display: block;
    min-width: 100px;
    min-height: 20px;
    border: $drag-item-border;
    border-radius: 4px;
    width: 100%;
    box-sizing: border-box;
  }

  .height33 {
    // height: 33%;
    // display: flex;
    // flex-direction: column;
  }

  .text-align-left {
    text-align: left;
  }

  .label-align-middle:before {
    content: '';
    display: inline-block;
    vertical-align: middle;
    height: 100%;
  }

  .label-horizental {
    display: inline-block;
    vertical-align: middle;
  }

  .flex-fill-remain {
    // flex: 1 1 auto;
    max-height: 140px;
  }

  .height100p {
    height: 100%;
  }
  .render {
    max-height: $maxHeight;
  }
}
/* cfg mask */
.plot-cfg-bg {
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  right: 0;
  background-color: rgba(0, 0, 0, 0.2);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
  .plot-cfg {
    background-color: #fff;
    width: 80%;
    max-height: 80%;
    padding: 20px 30px;
    border-radius: 5px;
    .plot-header {
      display: inline-flex;
      justify-content: space-between;
      width: 100%;
      margin-bottom: 10px;
      > span {
        color: #333;
        font-family: 'Arial-BoldMT', 'Arial Bold', 'Arial';
        font-weight: 700;
        font-size: 14px;
        &.title {
          font-size: 20px;
        }
      }
      .el-icon-close{
        font-size: 18px;
        cursor: pointer;
      }
    }
    .plot-footer {
      margin-top: 10px;
      display: inline-flex;
      justify-content: flex-end;
      width: 100%;
    }
  }
}

.dropdown100p {
  width: 100%;
  /deep/ .pvtDropdownCurrent {
    width: 100%;
  }
}
.filter .el-input{
  /deep/ .el-input__inner{
    line-height: 23px;
    height: 23px;
    padding: 0 10px;
  }
}
</style>
