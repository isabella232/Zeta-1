<template>
  <div
    v-loading="rendering"
    class="table-wrapper"
  >
    <div
      id="mount"
      ref="mount"
    />
    <div class="table-tools">
      <div>
        <span>Average: {{ average | formatNumber }}</span>
        <span>Count: {{ count | formatNumber }}</span>
        <span>Sum: {{ sum | formatNumber }}</span>
      </div>
    </div>
    <div
      v-if="series.length>0"
      class="chart-wrapper"
    >
      <chart-render
        :x-axis="rowHeaders"
        :legend="colHeaders"
        :series="series"
        @remove="removeChart"
        @set-selection="setSelection"
      />
    </div>
  </div>
</template>

<script lang="ts">
/**
 * Component <TableDisplay>. A simple data table.
 * This component is a wrapper of Handsontable.
 *
 * @prop header:Array[String] Column names
 * @prop rows:Array[Array[String]] Table data in 2D array
 */
import { Component, Vue, Prop, Watch, Ref } from 'vue-property-decorator';
import Handsontable from 'handsontable';
import _ from 'lodash';
import ResizeObserver from 'resize-observer-polyfill';
import ChartRender from '@/components/common/chart/chart-render.vue';
import $ from 'jquery';

const ceilRenderer = (instance: Handsontable, td: HTMLElement, row: any, col: any, prop: any, value: any, cellProperties: any) => {
  /** handle NULL value */
  if (value == null){
    td.className += ' ht-null-val';
    value = 'NULL';
  }

  // comments by tianrsun @2019-04-15
  // add an icon to identify the cell which was sliced

  /** cast JSON Object to string*/
  let content = '';
  let sliceFlag = false;
  if (typeof value  === 'object') {
    content = JSON.stringify(value);
  } else {
    content = value.toString();
  }
  content = content.replace(/[\r\n]/g, '');
  content = content.trim();
  /** slice string */
  if (typeof content  === 'string' && content.length > 100){
    td.innerText = content.substring(0, 100);
    sliceFlag = true;
  } else {
    td.innerText = content;
  }

  /** render slice cell */
  if (sliceFlag) {
    const icon = document.createElement('i');
    icon.className = 'icon el-icon-more';
    icon.addEventListener('click', e => {
      instance.getPlugin('contextMenu').open(e);
    });
    td.className += 'ht-slice-cell';
    td.appendChild(icon);
  }
  return td as HTMLTableCellElement;
};
const singleCellRender = (instance: Handsontable, td: HTMLElement, row: any, col: any, prop: any, value: any, cellProperties: any) => {
  /** handle NULL value */
  if (value == null){
    td.className += ' ht-null-val';
    value = 'NULL';
  }
  let content = '';
  if (typeof value  === 'object') {
    content = JSON.stringify(value);
  } else {
    content = value.toString();
  }
  td.innerText = content;
  return td as HTMLTableCellElement;
};
@Component({
  components: {
    ChartRender,
  },
  filters: {
    formatNumber: function (num: number) {
      return num.toString().replace(/\d+/, function (n) {
        return n.replace(/(\d)(?=(\d{3})+$)/g, function ($1) {
          return $1 + ',';
        });
      });
    },
  },
})

export default class TableDisplay extends Vue {
  @Prop() headers: string[];
  @Prop() rows: string[][];
  /** start from 1 */
  @Prop({type: Number, default: 1}) page: number;
  @Prop({type: Number, default: -1}) pageSize: number;
  @Ref('mount')
  tableEl: HTMLElement;
  get isSingleCell () {
    const oneColumn = this.headers && this.headers.length === 1;
    const oneRow = this.rows && this.rows.length === 1;
    return oneColumn && oneRow;
  }
  sheet: Handsontable;
  // render: Function;
  /* First time rendering ? */
  initial: boolean;
  rendering: boolean;
  // debouncedHook: Function;
  debounceRender: Function;
  resizeObserver: ResizeObserver;
  conditions: any[] = [];
  sortConfig: Handsontable.columnSorting.Config[];
  scrollLeft = 0;
  scrollTop = 0;
  average=0;
  count=0;
  sum=0;
  rowHeaders: Array<any>=[];
  colHeaders: Array<any>=[];
  series: Array<any>=[];
  selection: Array<any>;
  selectionRange: Array<number>;
  chartType: string;
  constructor () {
    super();
    this.initial = true;
    this.rendering = true;
    // this.debouncedHook = _.debounce(this.renderSheet, 200, { trailing: true });
    this.debounceRender = _.debounce(this.renderSheet, 200, { trailing: true });
    this.resizeObserver = new ResizeObserver(entries => {
      const $el = this.$refs.mount;
      if (!$el){
        return;
      }
      const resizeObject = _.find(entries, entry => entry.target === $el);
      if (!resizeObject){
        return;
      }
      this.debounceRender(true);
    });
  }

  hook () {

    // if (this.rendering) return;
    this.rendering = true;
    if (this.sheet && this.sheet.destroy){
      this.sheet.destroy();
    }

    this.$nextTick().then(() => {
      /**
             * add by tianrsun @2019-04-12
             * slice data by pagination
             */
      const start = (this.page >=1 && this.pageSize > 0) ? (this.page - 1) * this.pageSize : 0;
      const end = (this.page >=1 && this.pageSize > 0) ? (this.page - 1) * this.pageSize + this.pageSize : this.rows.length;
      const data = this.rows.slice(start, end);
      // end
      let gridSetting: Handsontable.GridSettings = {
        data: data,
        // rowHeaders: true,
        licenseKey: 'non-commercial-and-evaluation',
        dropdownMenu: ['filter_by_condition', 'filter_operators', 'filter_by_condition2', 'filter_by_value', 'filter_action_bar'],
        filters: true,
        /* 'columns' is necessary to display header when given empty table
                * REF: https://github.com/handsontable/handsontable/issues/2410
                */
        columns: _.times(this.headers.length, () => ({ type: 'text', renderer:ceilRenderer })),
        colHeaders: this.headers,
        rowHeaders: ((idx: any) => idx + start + 1) as any,
        contextMenu: {
          items: {
            'copy': {
              name: 'copy',
            },
            'chart': {
              name: 'chart',
              submenu: {
                // Custom option with submenu of items
                items: [
                  {
                    // Key must be in the form "parent_key:child_key"
                    key: 'chart:line',
                    name: 'Line',
                    callback:(key: string, selection: any, clickEvent: any) => {
                      this.handleCallback(key, selection);
                    },
                  },
                  { key: 'chart:bar', name: 'Bar',
                    callback:(key: string, selection: any, clickEvent: any) => {
                      this.handleCallback(key, selection);
                    },
                  },
                ],
              },
            },
          },
        } as any,
        // sortIndicator: true,
        manualColumnResize: true,
        multiColumnSorting: {
          headerAction: false,
        },
        // columnSorting: true,
        readOnly: true,
        autoRowSize: false,
        stretchH: data.length>0?'none':'all',
        copyPaste:{
          rowsLimit: 6000,
        },
        beforeCopy: this.beforeCopy,
        // add by tianrsun @2019-04-24
        // click eventlistener for select all
        afterOnCellMouseDown: (e: any, coords: any, td: any) => {
          // click row:-1 column:-1
          const target = e.realTarget;
          const isTarget = _.includes(target.className, 'relative');
          if (coords && coords.row == 0 && coords.col ===0 && this.sheet && isTarget){
            this.sheet.selectAll();
          }
          // click header columns && not filter button
          if (coords && coords.row == -1 && !_.includes(target.className, 'changeType')) {

            const col: number = coords.col;
            const columnSortPlugin = this.sheet.getPlugin('multiColumnSorting');
            const configs = columnSortPlugin.getSortConfig();
            const currentColumn = configs.find(cfg => cfg.column === col);
            if (!currentColumn) {
              configs.push({ column: col, sortOrder: 'asc'});
            } else {
              if (currentColumn.sortOrder === 'asc') {
                currentColumn.sortOrder = 'desc';
              } else if (currentColumn.sortOrder === 'desc') {
                _.remove(configs, cfg => cfg.column === col);
              }
            }
            // columnSortPlugin.setSortConfig(configs);
            columnSortPlugin.sort(configs);
          }

        },
        afterSelectionEnd: (row, column, row2, column2, selectionLayerLevel) => {
          this.handleSelect(row, column, row2, column2);
        },
        afterColumnSort: () => {
          this.sortConfig = this.sheet.getPlugin('multiColumnSorting').getSortConfig();
        },
        afterFilter: (conditions) => {
          this.conditions = conditions;
        },
        afterScrollHorizontally: () => {
          this.scrollLeft = $(this.tableEl).find('.wtHolder').scrollLeft() || 0;
        },
        afterScrollVertically: () => {
          this.scrollTop = $(this.tableEl).find('.wtHolder').scrollTop() || 0;
        },
      };
      if (this.isSingleCell) {
        const singleCellSetting: Partial<Handsontable.GridSettings> = {
          dropdownMenu: false,
          rowHeaders: false,
          // colWidths:800,
          columns: [{ type: 'text', renderer: singleCellRender }],
          modifyColWidth: (w) => {
            const maxWidth = this.tableEl && this.tableEl.offsetWidth && this.tableEl.offsetWidth > 20? this.tableEl.offsetWidth - 20: 800;
            if (w > maxWidth) {
              return maxWidth;
            }
          },
        };
        gridSetting = {
          ...gridSetting,
          ...singleCellSetting,
        };
      }
      this.sheet = new Handsontable(this.$el.firstChild as Element, gridSetting);
      // restore filters
      this.restoreConditions();
      this.restoreScroll();
      this.restoreSort();
      return this.sheet;
    }).then((sheet)=>{
      this.rendering = false;
      this.initial = false;
      return sheet;
    });
  }
  handleCallback (key: string, selection: Array<any>){
    this.chartType = key.split(':')[1];
    this.selection = [];
    const column2 = selection[0].end.col;
    const column = selection[0].start.col;
    const row2 = selection[0].end.row;
    const row = selection[0].start.row;
    this.selection.push(row, column, row2, column2);
    this.handleChartData();
  }
  handleSelect (row: number, column: number, row2: number, column2: number){
    const selectData = this.sheet.getData(row, column, row2, column2);
    this.average=this.count=this.sum=0;
    if (row2<0 || row<0){
      this.count = Math.abs(column2-column)+1;
    } else if (row === row2){
      this.count = Math.abs(column2-column)+1;
    } else if (column === column2){
      this.count = Math.abs(row2-row)+1;

    } else {
      this.count = (Math.abs(row2-row)+1)*(Math.abs(column2-column)+1);
    }
    if (selectData.length>0){
      const array = selectData.reduce(( acc, cur ) => {
        return acc.concat(cur);
      }, []);
      this.sum = parseFloat((array.reduce(( acc: number, cur: any )=>{
        return acc + ((isNaN(cur)||(cur===null))?0:parseFloat(cur));
      }, 0)).toFixed(5));
      this.average = parseFloat((this.sum/this.count).toFixed(5));
    }
    // if(this.chartType){
    //     if(row>row2){
    //         [row,row2] = [row2,row];
    //     }
    //     if(column>column2){
    //         [column,column2] = [column2,column];
    //     }
    //     this.selection = [row, column, row2, column2];
    //     this.handleChartData();
    // }
  }
  handleChartData (){
    const chartType = this.chartType;
    const selection = this.selection;
    this.selectionRange = [];
    const column2 = selection[3];
    const column = selection[1];
    const row2 = selection[2];
    const row = selection[0];
    this.selectionRange.push(row, column, row2, column2);
    this.colHeaders = [];
    for (let i=column;i<=column2;i++){
      this.colHeaders.push(this.sheet.getColHeader(i));
    }

    this.rowHeaders = [];
    for (let i=row;i<=row2;i++){
      this.rowHeaders.push(this.sheet.getRowHeader(i));
    }
    const selectData = this.sheet.getData(row, column, row2, column2);
    const len=selectData.length;
    this.series = [];
    for (let j=0;j<(column2-column)+1;j++){
      const arr: any[] = [];
      for (let i=0;i<len;i++){
        arr.push(selectData[i][j]);
      }
      const obj = {type:chartType, data:arr, name:this.colHeaders[j]};
      this.series.push(obj);
    }
    const legend = this.colHeaders;
  }
  removeChart (){
    this.series = this.colHeaders = this.rowHeaders = [];
    this.chartType = '';
  }
  setSelection (){
    const arr: Array<any>=[];
    arr.push(this.selectionRange);
    this.sheet.selectCells(arr);
  }
  renderSheet (force = false){
    if (!force && this.sheet){
      this.sheet.render();
    }
    else {
      this.hook();
    }
  }

  mounted () {
    this.resizeObserver.observe(this.$el.firstChild as Element);
    this.debounceRender();
    this.$el.addEventListener('wheel', function (this, ev) {
      // #mount > div.ht_master.handsontable.innerBorderTop.innerBorderLeft > div
      const masterEl = this.getElementsByClassName('ht_master')[0];
      if (masterEl) {
        const scrollEl = masterEl.firstElementChild;
        if (scrollEl && ev.target && ($(scrollEl).has(ev.target as Element) || scrollEl === ev.target)) {
          if (Math.abs(ev.deltaX) > Math.abs(ev.deltaY)) {
            if (scrollEl.scrollLeft == 0 && ev.deltaX < 0) {
              ev.preventDefault();
            } else if ((scrollEl.scrollLeft + scrollEl.clientWidth) == scrollEl.scrollWidth && ev.deltaX > 0) {
              ev.preventDefault();
            }
          }
        }
      }
      // ev.preventDefault();
    }, { passive: false });
  }

  activated () {
    this.debounceRender();
  }

  @Watch('headers')
  onHeadersChange () {
    this.debounceRender(true);
  }

  @Watch('rows')
  onRowsChange (val: any, old_val: any) {
    if (!_.isEqual(val, old_val)) {
      this.debounceRender(true);
    }
  }

  @Watch('page')
  onPageChange (){
    this.debounceRender(true);
  }
  @Watch('pageSize')
  onPageSizeChange (){
    this.average=this.count=this.sum=0;
    this.debounceRender(true);
  }

  beforeCopy (data: any, range: any) {
    // get col headers
    const colHeaders: any[] = [];
    for (let i = range[0].startCol; i <= range[0].endCol; i++) {
      colHeaders.push(this.sheet.getColHeader(i));
    }
    /**
         * modify by tianrsun @2019-04-24
         *  only one cell selected, copy cell's value
         *  more than one cell,copy both cell's value and column's name
         */
    const oneCell = Boolean(data.length === 1 && (data[0] && data[0].length && data[0].length === 1));
    if (!oneCell){
      data.unshift(colHeaders);
    }
    // data to string
    for (const i in data){
      const row: any[] = data[i];
      for (const j in row){
        const cellVal = row[j];
        // let cellStrVal = cellVal.toString();
        if (typeof cellVal === 'object'){
          row[j] = JSON.stringify(cellVal);
        }

      }
    }
  }
  restoreConditions () {
    if (!this.conditions) {
      return;
    }
    this.$nextTick(() => {
      const filter = this.sheet.getPlugin('filters');
      filter.clearConditions();
      _.forEach(this.conditions, (columnConditions: Handsontable.plugins.FiltersPlugin.ColumnConditions) => {
        _.forEach(columnConditions.conditions, condition => {
          // typing file is out of date
          (filter as any).addCondition(columnConditions.column, condition.name, condition.args);
        });
      });
      filter.filter();
    });
  }
  restoreScroll () {
    const $el = $(this.tableEl).find('.wtHolder');
    $el.scrollTop(this.scrollTop);
    $el.scrollLeft(this.scrollLeft);
  }
  restoreSort () {
    if (!this.sortConfig || this.sortConfig.length == 0) {
      return;
    }
    this.sheet.getPlugin('multiColumnSorting').setSortConfig(this.sortConfig);
  }
}
</script>

<style lang="scss" >
.table-wrapper{
    position: relative;
    height: 100%;
    overflow: scroll;
}
#mount{
    height: calc(100% - 20px);
    overflow: hidden;
}
.handsontable .htDimmed {
    color: black !important;
}

.ht_master {
    height: 100% !important;
}

.ht_master .wtHolder {
    height: 100% !important;
}
.htContextMenu.handsontable, .htDropdownMenu.handsontable, .htFiltersConditionsMenu:not(.htGhostTable) {
  z-index: 2100 !important;
}
.ht_master td.ht-null-val{
    color: #999;
}
.ht-slice-cell{
    position: relative;
    padding-right: 15px !important;
    > .icon{
        cursor: pointer;
        position: absolute;
        top: 5px;
        right: 2px;
        font-size: 10px !important;
        background-color: #CCC;
    }
}
.table-tools{
    background-color: #333;
    padding: 2px;
    border-radius: 2px;
    >div{
        display: flex;
        color: #fff;
        justify-content: flex-end;
        span{
            margin-right: 20px;
        }
    }
}
.handsontable .htDimmed{
    word-break: keep-all;
    white-space: nowrap;
}
.chart-wrapper{
    // position: absolute;
    // top: 50px;
    // left: 300px;
    // min-width: 400px;
    // overflow: hidden;
    // background-color: #fff;
    // border: 1px solid #ccc;
}
</style>
