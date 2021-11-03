<template>
<table class='pvtTable'>
  <!-- header -->
  <thead>
    <tr v-for='(col, colIdx) in colAttrs' :key='col'>
      <th v-if='colIdx === 0 && rowAttrs.length !== 0' :colSpan='rowAttrs.length' :rowSpan='colAttrs.length'></th>
      <th className='pvtAxisLabel'>{{col}}</th>
      <template v-for='(colKey, keyIdx) in colKeys'>
        <th
          :key='"colKey" + keyIdx'
          class='pvtColLabel'
          :colSpan='spanSize(colKeys, keyIdx, colIdx)'
          :rowSpan='colIdx === colAttrs.length - 1 && rowAttrs.length !== 0 ? 2 : 1' 
          v-if='spanSize(colKeys, keyIdx, colIdx) !== -1'
        >
          {{colKey[colIdx]}} 
        </th>
      </template>

      <th v-if='colIdx === 0' class='pvtTotalLabel' :rowSpan='colAttrs.length + (rowAttrs.length === 0 ? 0 : 1)'>
        Totals
      </th>
    </tr>
    <tr v-if='rowAttrs.length !== 0'>
      <th v-for='(row, rowIdx) in rowAttrs' :key='row+rowIdx' >
        {{row}}
      </th>
      <th className="pvtTotalLabel">
        {{colAttrs.length === 0 ? 'Totals' : null}}
      </th>
    </tr>
  </thead>

  <tbody>
    <tr v-for='{rowKey, aggr: totalAggr, index} in rowsGenerator()' :key='"rowKeyRow"+index' :id='"rowKeyRow"+index'>
      <!-- row header -->
      <template v-for='{key: header, spanSize, colIndex} in rowHeaderGenerator(rowKey, rowKeys, index)'>
        <th v-if='spanSize !== -1'
          :key='"rowHeader" + index + "-" + colIndex' :id='"rowHeader" + index + "-" + colIndex'
          :rowSpan='spanSize'
          :colSpan='colIndex === rowAttrs.length -1 && colAttrs.length !== 0 ? 2 : 1'
          > {{header}}
        </th>
      </template>
      <!-- data -->
      <td v-for='{aggr, colIndex} in rowDataGenerator(rowKey, colKeys, index)' :key='"pvtVal" + index + "-" + colIndex'>
        {{aggr.format(aggr.value())}}
      </td>
      <!-- total -->
      <td class='pvtTotal'>{{totalAggr.format(totalAggr.value())}}</td>
    </tr>

    <!-- grand total -->
    <tr>
      <th class="pvtTotalLabel" :colSpan='rowAttrs.length + (colAttrs.length === 0 ? 0 : 1)'> Totals </th>
      <td v-for='{aggr, index} in colTotalGenerator(colKeys)' :key='"colTotal" + index'>
        {{ aggr.format(aggr.value()) }}
      </td>
      <td class="pvtGrandTotal"> {{grandTotalAggregator.format(grandTotalAggregator.value())}} </td>
    </tr>
  </tbody>
</table>
</template>
<script lang='ts'>
import { Component, Prop, Vue, Watch } from 'vue-property-decorator';
import { PivotData } from '../utilities';
import { VNode, CreateElement } from 'vue';
import { PivotTableRender } from './render-api';
@Component({
  components: {}
})
export default class TableRender extends Vue implements PivotTableRender {
  @Prop() plotData!: PivotData;

  constructor() {
    super();
  }
  resize() {}

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

  // ------ helper functions
  rowsGenerator() {
    return this.rowKeys.map((rowKey, index) => {
      return { rowKey, aggr: this.plotData.getAggregator(rowKey, []), index };
    });
  }

  // -----
  rowHeaderGenerator(rowKey: string[], rowKeys: string[][], rowIndex: number) {
    return rowKey.map((key, index) => {
      return {
        key,
        spanSize: this.spanSize(rowKeys, rowIndex, index),
        colIndex: index
      };
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

  // helper function for setting row/col-span in pivotTableRenderer
  spanSize(arr: string[][], i: number, j: number) {
    let x;
    if (i !== 0) {
      let asc;
      let end;
      let noDraw = true;
      for (
        x = 0, end = j, asc = end >= 0;
        asc ? x <= end : x >= end;
        asc ? x++ : x--
      ) {
        if (arr[i - 1][x] !== arr[i][x]) {
          noDraw = false;
        }
      }
      if (noDraw) {
        return -1;
      }
    }
    let len = 0;
    while (i + len < arr.length) {
      let asc1;
      let end1;
      let stop = false;
      for (
        x = 0, end1 = j, asc1 = end1 >= 0;
        asc1 ? x <= end1 : x >= end1;
        asc1 ? x++ : x--
      ) {
        if (arr[i][x] !== arr[i + len][x]) {
          stop = true;
        }
      }
      if (stop) {
        break;
      }
      len++;
    }
    return len;
  } // -- spanSize
}
</script>