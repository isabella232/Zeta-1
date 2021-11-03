
import * as d3 from 'd3'
const render = require("./render")
import * as graphlibDot from 'graphlib-dot';
import $ from 'jquery';
import _ from 'lodash';
export function parseDotFile(dom:string){
  const dotReg = /<div[^>]*class=\"dot-file\"[^>]*>([\s\S]*?)<\/div>/gim
  const result = dotReg.exec(dom)
  return result ? result[1] : null
}

export function parseGlobalInfo(dom: string) {
	const dotReg = /<div[^>]*id=\"plan-viz-summary\"[^>]*>([\s\S]*?)<\/div>/gim
	const result = dotReg.exec(dom)
	return result ? result[1] : null
}

export function htmlEscape(text: string){
    if(!text) return text;
    return text.replace(/&lt;|&gt;|&amp;|&quot;/g, (match:string) => {
    switch(match){
    case "&lt;": return "<";
    case "&gt;":return ">";
    case "&amp;":return "&";
    case "&quot;":return "\"";
    default:return "";
  }
  });
}
const PlanVizConstants = {
  svgMarginX: 16,
  svgMarginY: 16
};
function getDotFile(html:string){
  const dotReg = /<div[^>]*class=\"dot-file\"[^>]*>([\s\S]*?)<\/div>/gim;
  const dom = dotReg.exec(html);
  const result = htmlEscape(dom && dom[1] ? dom[1] : '')
  return result;
}



/* -------------------- *
 * | Helper functions | *
 * -------------------- */

export function planVizContainer(id:string) { return d3.select(`#${id}`); }

/*
 * Set up the tooltip for a SparkPlan node using metadata. When the user moves the mouse on the
 * node, it will display the details of this SparkPlan node in the right.
 */
// function setupTooltipForSparkPlanNode(nodeId) {
//   var nodeTooltip = d3.select("#plan-meta-data-" + nodeId).text()
//   d3.select("svg g .node_" + nodeId)
//     .on('mouseover', function(d) {
//       var domNode = d3.select(this).node();
//       $(domNode).tooltip({
//         title: nodeTooltip, trigger: "manual", container: "body", placement: "right"
//       });
//       $(domNode).tooltip("show");
//     })
//     .on('mouseout', function(d) {
//       var domNode = d3.select(this).node();
//       $(domNode).tooltip("destroy");
//     })
// }
export function getNodeState(stageLabel:string){
  var reg = /Status\: ([A-Z]+)/gim
  var state = reg.exec(stageLabel);
  return state && state[1] ? state[1] : ''
}

export function getStageName(g:any, v:any):string {
  if(v.indexOf('stage') >= 0) return v;
  const parent = g.parent(v)
  if(!parent) return '';
  if(parent.indexOf('cluster') >= 0){
    return getStageName(g,parent);
  }
  if(parent.indexOf('stage') >= 0) return parent;
  return '';
}
export function getClusterName(g:any, v:any):string {
  const parent = g.parent(v)
  if(!parent) return '';
  if(parent.indexOf('cluster') >= 0){
    return parent;
  }
  return '';
}

export function isStageSkewed(stageLabel: string) {
	let reg = /Skewed Tasks Count/gim
	return reg.test(stageLabel)
}

export function isStageSpill(stageLabel: string) {
	let reg = /Over Spilled Tasks Count/gim
	return reg.test(stageLabel)
}

export function isStageDurationMax(stageLabel: string) {
	let reg = /Stage\: (.+) \(duration\: (.+) max/gim
	return reg.test(stageLabel);
}

export function postprocessGraphLayout(svg:any,g:any) {
  const clusters = svg.selectAll('.cluster');
  _.forEach(clusters.nodes(), cNode => {
    let clusterRect =d3.select(cNode).select('rect').nodes()[0] as any;
    let label = d3.select(cNode).select('.label').nodes()[0] as any;
    if(label && clusterRect){
      let rect:SVGRect = label.getBBox()
      // rect.height + 5 + 5
      let height = clusterRect.getBBox().height + rect.height + 5 + 5
      d3.select(cNode).select('rect')
        .attr('height',height)
        .attr("transform", "translate(" + clusterRect.getBBox().x  + ", "+ (clusterRect.getBBox().y + rect.height)+")");
    }

  })
}
/*
 * Helper function to size the SVG appropriately such that all elements are displayed.
 * This assumes that all outermost elements are clusters (rectangles).
 */
export function resizeSvg(id:string,svg:any) {
  var allClusters = svg.selectAll("g rect").nodes();
  // var allClusters = svg.selectAll("g rect");
  var startX = -PlanVizConstants.svgMarginX +
    toFloat(d3.min(allClusters, function(e:any) {
      return getAbsolutePosition(id,d3.select(e)).x;
    }));
  var startY = -PlanVizConstants.svgMarginY +
    toFloat(d3.min(allClusters, function(e:any) {
      return getAbsolutePosition(id,d3.select(e)).y;
    }));
  var endX = PlanVizConstants.svgMarginX +
    toFloat(d3.max(allClusters, function(e:any) {
      var t = d3.select(e);
      return getAbsolutePosition(id,t).x + toFloat(t.attr("width"));
    }));
  var endY = PlanVizConstants.svgMarginY +
    toFloat(d3.max(allClusters, function(e:any) {
      var t = d3.select(e);
      return getAbsolutePosition(id,t).y + toFloat(t.attr("height"));
    }));
  var width = endX - startX;
  var height = endY - startY;
  svg.attr("viewBox", startX + " " + startY + " " + width + " " + height)
     .attr("width", width)
     .attr("height", height);
}

/* Helper function to convert attributes to numeric values. */
function toFloat(f:any) {
  if (f) {
    return parseFloat(f.toString().replace(/px$/, ""));
  } else {
    return f;
  }
}

/*
 * Helper function to compute the absolute position of the specified element in our graph.
 */
function getAbsolutePosition(id:string,d3selection:any) {
  if (d3selection.empty()) {
    throw "Attempted to get absolute position of an empty selection.";
  }
  var obj = d3selection;
  var _x = toFloat(obj.attr("x")) || 0;
  var _y = toFloat(obj.attr("y")) || 0;
  while (!obj.empty()) {
    var transformText = obj.attr("transform");
    if (transformText) {
      var translate = getTranslation(transformText);
      _x += toFloat(translate[0]);
      _y += toFloat(translate[1]);
    }
    // Climb upwards to find how our parents are translated
    obj = d3.select(obj.node().parentNode);
    // Stop when we've reached the graph container itself
    if (obj.node() == planVizContainer(id).node()) {
      break;
    }
  }
  return { x: _x, y: _y };
}

function getTranslation(transform:string) {
  // Create a dummy g for calculation purposes only. This will never
  // be appended to the DOM and will be discarded once this function
  // returns.
  var g = document.createElementNS("http://www.w3.org/2000/svg", "g");

  // Set the transform attribute to the provided string value.
  g.setAttributeNS(null, "transform", transform);

  // consolidate the SVGTransformList containing all transformations
  // to a single SVGTransform of type SVG_TRANSFORM_MATRIX and get
  // its SVGMatrix.
  var matrix = g.transform.baseVal.consolidate().matrix;

  // As per definition values e and f are the ones for the translation.
  return [matrix.e, matrix.f];
}
