import 'pivottable/dist/pivot.css';

import $ from 'jquery';
import 'jquery-ui/ui/widgets/sortable';
import 'pivottable';
import 'pivottable/dist/plotly_renderers.js'
import GuessType from '@/components/common/pivot-table/utilities/guess-type';
let data;
let typeGuessed= {};
let allAttrs = [];

function guessSuitableAttrIndex(attrs, types) {
  if ( !attrs ) return -1;
  if ( attrs.length === 0 ) return -1;
  for ( let idx = 0; idx < attrs.length; idx++ ) {
    const attr = attrs[idx];
    const guessType = typeGuessed[attr];
    if ( types.has(guessType) ) return idx;
  }
  return -1;
}

function getAllAttrs(data) {
  if ( Array.isArray(data[0]) ) {
    // first row is the header row.
    allAttrs = data[0];
    return (data[0]);
  }
  // first row is an object, get its keys
  const record = (data[0]);
  allAttrs = Object.keys(record);
  return allAttrs;
}

function getTypeGuessed(data) {
  let input;
  if ( Array.isArray(data[0]) ) {
    input = (data[1]).slice();
  } else {
    input = Object.assign({}, data[0]);
  }
  const types = GuessType.guess(input);

  getAllAttrs(data).forEach( (key, idx) => {
    typeGuessed[key] = types[idx];
  });
}

function initInputOptions(){
  let horizAttrs = [];
  let vertAttrs = [];
  let aggregatorArgs = [];
  const attrsExclude = allAttrs.filter( (x) => !((horizAttrs.indexOf(x) >= 0) || (vertAttrs.indexOf(x)) >= 0));
    let index = guessSuitableAttrIndex(attrsExclude, new Set(['date']));
    if ( index === -1 ) index = guessSuitableAttrIndex(attrsExclude, new Set(['string']));
    if ( index !== -1 ) horizAttrs = attrsExclude.splice(index, 1);

    // set unusedAttrs
    const unusedAttrs = allAttrs.filter( (x) => !((horizAttrs.indexOf(x) >= 0) || (vertAttrs.indexOf(x)) >= 0));
    // make sure aggregator selection has the default value.
    if (unusedAttrs.length > 0 ) {
      aggregatorArgs = unusedAttrs.slice();
    }

    // select a num value for yAxis
    const index_1 = guessSuitableAttrIndex(aggregatorArgs, new Set(['number']));
    if(index_1 !== -1) {
      let copyAggregators = [...aggregatorArgs];
      let numberMetric = copyAggregators.splice(index_1, 1);
      aggregatorArgs = [numberMetric[0], ...copyAggregators];
    }
    return {
      horizAttrs: horizAttrs,
      vertAttrs: vertAttrs,
      aggregatorArgs: aggregatorArgs,
      data: data
    };
}
function PivotTable(ele, dataIn){
  data = dataIn;
  getTypeGuessed(data);
  const pivotData = initInputOptions();
  let pivot = $("#"+ ele).pivotUI(
      data, {
        rows: [],
        cols: pivotData.horizAttrs,
        vals: pivotData.aggregatorArgs,
        aggregatorName: "Sum",
        rendererName: "Line Chart",
        renderers: $.extend(
            $.pivotUtilities.renderers,
            $.pivotUtilities.plotly_renderers
        )
      }
  );
  return pivot;
}
export  { PivotTable };
