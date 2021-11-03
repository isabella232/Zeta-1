'use stricts';

import { Aggregators } from './aggregator';

type AnyArgFunctionType1 = (... args: any[]) => any;
type SorterType = AnyArgFunctionType1 | {
  [key: string]: AnyArgFunctionType1
};

interface DerivedAttributeType {
  [key: string]: AnyArgFunctionType1;
}

interface Record {
  [key: string]: string|number;
}

/* type of plot configuration */
interface PlotConfig {
  cols?: string[];
  rows?: string[];
  aggregatorName?: string;
  vals?: string[];
  chartType?: string;
}

type DataType = Array<Array<(string|number)>> | Record[];

interface PivotDataPropType {
  aggregators?: {[key: string]: any};
  /**
   *  data could have 2 formats:
   *  1. compact format:
   *  [
   *    ['f1','f2','f3'],     -- header row
   *    [1, 2, 3],     -- data row
   *    [4, 5, 6],     -- data row
   *    ...
   *    [8, 9, 10]      -- data row
   *  ]
   *
   *  2. objects array
   *  [
   *    {f1: 1, f2: 2, f3: 3},
   *    {f1: 4, f2: 5, f3: 6},
   *    ...
   *  ]
   */
  // data?: any[];
  data?: DataType;
  aggregatorName?: string;
  cols?: string[];
  rows?: string[];
  vals?: string[];
  derivedAttributes?: DerivedAttributeType; // (...args: any[]) => any
  rowOrder?: 'key_a_to_z'|'value_a_to_z'|'value_z_to_a';
  colOrder?: 'key_a_to_z'|'value_a_to_z'|'value_z_to_a';
  sorters?: SorterType;
}

type Aggregator = any;

class PivotData {
  defaultProp: PivotDataPropType = {
    data: undefined,
    aggregators: Aggregators,
    cols: [],
    rows: [],
    vals: [],
    aggregatorName: 'Sum',
    sorters: {},
    // valueFilter: {},
    rowOrder: 'key_a_to_z',
    colOrder: 'key_a_to_z',
    derivedAttributes: {}
  };
  aggregatorGenerator: any;

  props: PivotDataPropType;
  tree: {[key: string]: any} = {};
  rowKeys: string[][] = [];
  colKeys: string[][] = [];
  rowTotals: {[key: string]: Aggregator} = {};
  colTotals: {[key: string]: Aggregator} = {};
  sorted = false;
  allTotal: Aggregator;

  constructor(inputProp: PivotDataPropType) {
    this.props = Object.assign(this.defaultProp, inputProp);

    this.aggregatorGenerator = this.props.aggregators![this.props.aggregatorName!](this.props.vals);
    this.allTotal = this.aggregatorGenerator(this, [], []);

    this.forEachRecord(this.props.data!, this.props.derivedAttributes!, (record: Record, index?: number) => {
      this.processRecord(record);
    });
  }

  // this function is called in a tight loop
  processRecord(record: Record) {
    const colKey: string[] = [];
    const rowKey: string[] = [];
    for (const x of Array.from(this.props.cols!)) {
      // colKey.push(x in record ? record[x] as string : 'null');
      // to fix: 0 || null -> null
      colKey.push(record[x] != undefined ? String(record[x]) : String(null));
    }
    for (const x of Array.from(this.props.rows!)) {
      // rowKey.push(x in record ? record[x] as string : 'null');
      rowKey.push(String(record[x] || null));
    }
    const flatRowKey = rowKey.join(String.fromCharCode(0));
    const flatColKey = colKey.join(String.fromCharCode(0));

    this.allTotal.push(record);

    if (rowKey.length !== 0) {
      if (!this.rowTotals[flatRowKey]) {
        this.rowKeys.push(rowKey);
        this.rowTotals[flatRowKey] = this.aggregatorGenerator(this, rowKey, []);
      }
      this.rowTotals[flatRowKey].push(record);
    }

    if (colKey.length !== 0) {
      if (!this.colTotals[flatColKey]) {
        this.colKeys.push(colKey);
        this.colTotals[flatColKey] = this.aggregatorGenerator(this, [], colKey);
      }
      this.colTotals[flatColKey].push(record);
    }

    if (colKey.length !== 0 && rowKey.length !== 0) {
      if (!this.tree[flatRowKey]) {
        this.tree[flatRowKey] = {};
      }
      if (!this.tree[flatRowKey][flatColKey]) {
        this.tree[flatRowKey][flatColKey] = this.aggregatorGenerator(
          this,
          rowKey,
          colKey
        );
      }
      this.tree[flatRowKey][flatColKey].push(record);
    }
  } // -- end of processRecord

  forEachRecord(records: any[], derivedAttributes: DerivedAttributeType, f: (record: Record, index?: number) => void) {
    let handler: (record: Record, index?: number) => void;
    if ( Object.getOwnPropertyNames(derivedAttributes).length === 0 ) {
      handler = f;
    } else {
      handler = (record1: Record, index?: number) => {
        // add derived attributes into record firstly
        for ( const k of Object.keys(derivedAttributes) ) {
          const derived = derivedAttributes[k](record1);
          if ( derived !== null ) { record1[k] = derived; }
        }
        f(record1, index);
      };
    }

    if ( Array.isArray(records[0]) ) {
      // compact format
      const size = records[0].length;
      records.forEach( (v: any, index: number) => {
        if ( index > 0 ) {
          const record: Record = {};
          for ( let j = 0; j < size; j++ ) { record[records[0][j]] = v[j]; }
          handler(record, index - 1);
        }
      });
    } else {
      // non-compact format
      let index = 0;
      for ( const record of (records) ) {
        const record1 = Object.assign({}, record);
        handler(record1, index++);
      }
    } // else
  } // --  forEachRecord

  getColKeys() {
    this.sortKeys();
    return this.colKeys;
  }

  getRowKeys() {
    this.sortKeys();
    return this.rowKeys;
  }

  sortKeys() {
    if (!this.sorted) {
      this.sorted = true;
      const v = (r: string[], c: string[]) => this.getAggregator(r, c).value();
      switch (this.props.rowOrder) {
        case 'value_a_to_z':
          this.rowKeys.sort((a, b) => naturalSort(v(a, []), v(b, [])));
          break;
        case 'value_z_to_a':
          this.rowKeys.sort((a, b) => -naturalSort(v(a, []), v(b, [])));
          break;
        default:
          this.rowKeys.sort(this.arrSort(this.props.rows!));
      }
      switch (this.props.colOrder) {
        case 'value_a_to_z':
          this.colKeys.sort((a, b) => naturalSort(v([], a), v([], b)));
          break;
        case 'value_z_to_a':
          this.colKeys.sort((a, b) => -naturalSort(v([], a), v([], b)));
          break;
        default:
          this.colKeys.sort(this.arrSort(this.props.cols!));
      }

    }
  }


  getAggregator(rowKey: string[], colKey: string[]) {
    let agg;
    const flatRowKey = rowKey.join(String.fromCharCode(0));
    const flatColKey = colKey.join(String.fromCharCode(0));
    if (rowKey.length === 0 && colKey.length === 0) {
      agg = this.allTotal;
    } else if (rowKey.length === 0) {
      agg = this.colTotals[flatColKey];
    } else if (colKey.length === 0) {
      agg = this.rowTotals[flatRowKey];
    } else {
      agg = this.tree[flatRowKey][flatColKey];
    }
    return (
      agg || {
        value() {
          return null;
        },
        format() {
          return '';
        },
      }
    );
  } // -- end of getAggregator

  arrSort(attrs: string[]) {
    const sortersArr = ((): any => {
      const result: any[] = [];
      for (const a of Array.from(attrs)) {
        result.push(getSort(this.props.sorters, a));
      }
      return result;
    })();
    return (a: any, b: any) => {
      for (const i of Object.keys(sortersArr || {})) {
        const sorter = sortersArr[i];
        const comparison = sorter(a[i], b[i]);
        if (comparison !== 0) {
          return comparison;
        }
      }
      return 0;
    };
  } // -- end of arrSort

} // -- end of class

const getSort = (sorters: any, attr: any) => {
  if (sorters) {
    if (typeof sorters === 'function') {
      const sort = sorters(attr);
      if (typeof sort === 'function') {
        return sort;
      }
    } else if (attr in sorters) {
      return sorters[attr];
    }
  }
  return naturalSort;
};

const rx: RegExp = /(\d+)|(\D+)/g;
const rd: RegExp = /\d/;
const rz: RegExp = /^0/;

const naturalSort = (as: (string|number), bs: (string|number)) => {
  // nulls first
  if (bs !== null && as === null) {
    return -1;
  }
  if (as !== null && bs === null) {
    return 1;
  }

  // then raw NaNs
  if (typeof as === 'number' && isNaN(as)) {
    return -1;
  }
  if (typeof bs === 'number' && isNaN(bs)) {
    return 1;
  }

  // numbers and numbery strings group together
  const nas = Number(as);
  const nbs = Number(bs);
  if (nas < nbs) {
    return -1;
  }
  if (nas > nbs) {
    return 1;
  }

  // within that, true numbers before numbery strings
  if (typeof as === 'number' && typeof bs !== 'number') {
    return -1;
  }
  if (typeof bs === 'number' && typeof as !== 'number') {
    return 1;
  }
  if (typeof as === 'number' && typeof bs === 'number') {
    return 0;
  }

  // 'Infinity' is a textual number, so less than 'A'
  if (isNaN(nbs) && !isNaN(nas)) {
    return -1;
  }
  if (isNaN(nas) && !isNaN(nbs)) {
    return 1;
  }

  // finally, "smart" string sorting per http://stackoverflow.com/a/4373421/112871
  let a: any = String(as);
  let b: any = String(bs);
  if (a === b) {
    return 0;
  }
  if (!rd.test(a) || !rd.test(b)) {
    return a > b ? 1 : -1;
  }

  // special treatment for strings containing digits
  a = a.match(rx);
  b = b.match(rx);
  if ( a === null && b === null ) { return 0; }
  if ( a === null && b !== null ) { return -1; }
  if ( a !== null && b === null ) { return 1; }
  while (a.length && b.length) {
    const a1 = a.shift();
    const b1 = b.shift();
    if (a1 !== b1) {
      if (rd.test(a1) && rd.test(b1)) {
        return a1.replace(rz, '.0') - b1.replace(rz, '.0');
      }
      return a1 > b1 ? 1 : -1;
    }
  }
  return a.length - b.length;
};

export { PivotDataPropType, PivotData, DataType, Record, PlotConfig };
