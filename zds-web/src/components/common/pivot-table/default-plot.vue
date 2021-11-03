<template>
  <div>
    <table class='pvtUI'>
      <th><td><h2>{{title}}</h2></td></th>
      <tbody>
        <tr> 
          <td> <Dropdown v-model="selectedChartType" :options="chartOptions"/> </td> 
          <td class='pvtAxisContainer'>
            <draggable v-model="unusedAttrs" :options="{group:'attributes'}" class='drag-group drag-drop-hori'>
              <div v-for="element in unusedAttrs" :key="element" class='drag-item'>{{element}}</div>
            </draggable>  
          </td>
        </tr>
        <tr>
          <td class='pvtVals'>
            <Dropdown v-model='selectedAggregator' :options='aggregatorNames'/>
            <a role="button" class="pvtRowOrder">↕</a>
            <a role="button" class="pvtColOrder">↔</a>
            <div v-if='numberOfAggregatorArgs > 0' >
              <Dropdown v-model='aggregatorArgs[0]' :options='unusedAttrs'/>
            </div>
          </td>
          <td class='pvtAxisContainer'>
            <draggable v-model="horizAttrs" :options="{group:'attributes'}" class='drag-group drag-drop-hori'>
              <div v-for="element in horizAttrs" :key="element" class='drag-item'>{{element}}</div>
            </draggable>  
          </td>
        </tr>
        <tr>
          <td class='pvtAxisContainer pvtVertList'>
            <draggable v-model="vertAttrs" :options="{group:'attributes'}" class='drag-group drag-drop-vert'>
              <div v-for="element in vertAttrs" :key="element" class='drag-item'>{{element}}</div>
            </draggable>  
          </td>
          <td>
            <PivotRender :pivot-data="pivotData" :selected-render="selectedChartType"/>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue, Watch, Mixins } from 'vue-property-decorator';
import Dropdown from './dropdown.vue';
import PivotRender from './renders/pivot-render.vue';
import draggable from 'vuedraggable';

import PlotMixin from './plot-mixin';

@Component({
  components: { Dropdown, draggable, PivotRender, PlotMixin },
})
export default class DefaultPlot extends Mixins(PlotMixin) {
}
</script>
<style scoped>
.drag-group {
  display: flex;
  min-width:10px;
  min-height: 10px;
}
.drag-drop-hori {
  flex-direction: row;
}
.drag-drop-vert {
  flex-direction: column;
}

.drag-item {
  display: inline-flex;
  text-align: left;
  border: 1px solid #a2b1c6;
  border-radius: 4px;
  box-sizing: border-box;
  background: #fff;
  margin: 2px 5px;
  padding: 0px 2px;
  font-size: 12px;
  cursor: pointer;
  width: fit-content;
}
.pvtAxisContainer, .pvtVals {
    border: 1px solid #a2b1c6;
    background: #f2f5fa;
    padding: 5px;
    min-width: 20px;
    min-height: 20px;
}
.pvtUI {
  border-collapse: collapse;
  font-family: Verdana;
}

.pvtVertList {
  width: 15%;
}

.pvtVals {
    text-align: center;
    white-space: nowrap;
    vertical-align: top;
    padding-bottom: 12px;
}

.pvtColOrder, .pvtRowOrder {
    cursor: pointer;
    width: 15px;
    margin-left: 5px;
    display: inline-block;
    user-select: none;
    text-decoration: none!important;
}
</style>