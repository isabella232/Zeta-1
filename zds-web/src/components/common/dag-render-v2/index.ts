import { htmlEscape,getNodeState, getStageName, getClusterName, parseDotFile, parseGlobalInfo, isStageSkewed, isStageDurationMax } from './utilities';
import DagRender from './dag-render.vue';
import * as d3 from 'd3'
const render = require("./render");
import * as graphlibDot from 'graphlib-dot';
import $ from 'jquery';
import _ from 'lodash';

export {
  initPlanViz,
  renderPlanViz,
  parseDotFile,
  parseGlobalInfo
}
export default DagRender;

const PlanVizConstants = {
  svgMarginX: 16,
  svgMarginY: 16
};

function initPlanViz(el:any,src:string){
  const svg = d3.select(el).select("svg").empty() ? d3.select(el).append("svg") : d3.select(el).select("svg");
  const graph = svg.select("g").empty() ? svg.append("g") : svg.select("g") ;
  const g = renderPlanViz(el,svg,graph,src)
}
function renderPlanViz(el:any,svg:any,graph:any, src:string) {
  const dot = htmlEscape(src)

  // var graph = svg.append("g");
  var g = graphlibDot.read(dot);
  preprocessGraphLayout(g);
  var renderer = new render();
  try {
    renderer(graph, g);
    // postprocessGraphLayout(svg,g);
  }
  catch(e){
    console.error(e)
  }


  // Round corners on rectangles
  svg
    .selectAll("rect")
    .attr("rx", "5")
    .attr("ry", "5");

  var nodeSize = parseInt($("#plan-viz-metadata-size").text());
  // for (var i = 0; i < nodeSize; i++) {
  //   setupTooltipForSparkPlanNode(i);
  // }

  resizeSvg(el,svg)
  return g;

}

/* -------------------- *
 * | Helper functions | *
 * -------------------- */

function planVizContainer(id:string) { return d3.select(`#${id}`); }

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

/*
 * Helper function to pre-process the graph layout.
 * This step is necessary for certain styles that affect the positioning
 * and sizes of graph elements, e.g. padding, font style, shape.
 */
function preprocessGraphLayout(g:any) {
  var nodes = g.nodes();
  for (var i = 0; i < nodes.length; i++) {
      var node = g.node(nodes[i]);
      let stageName = getStageName(g,nodes[i])
      if(stageName){
        let stageNode = g.node(stageName);
		const stageStatus = getNodeState(stageNode.label) || ''
		node.class = stageStatus.toLocaleLowerCase()
		let isSkewed = isStageSkewed(stageNode.label);
		let isDurationMax = isStageDurationMax(stageNode.label);
		if (isSkewed) {
		  node.class += ' skewed'
		}
		if (isDurationMax) {
		  node.class += ' duration-max'
		}
      }
      let clusterName = getClusterName(g,nodes[i])
      if(!stageName && clusterName){
        let clusterNode = g.node(clusterName);
        const clusterStatus = getNodeState(clusterNode.label) || ''
        node.class = clusterStatus.toLocaleLowerCase()
      }
      if(stageName && clusterName){
        node.padding = "10";
      }
  }
  // add by tianrsun
  for (var i = 0; i < nodes.length; i++) {
    var node = g.node(nodes[i]);
    const children = g.children(nodes[i]);
    if(children && children.length > 0){
      node.clusterLabelPos = 'top';
      node.paddingTop = "10";
      node.paddingBottom = "0";
    }
  }
  // end

  // Curve the edges
  var edges = g.edges();
  for (var j = 0; j < edges.length; j++) {
    var edge = g.edge(edges[j].v,edges[j].w);
    // modify by tianrsun
    // scale edge length when cross cluster/stage
    const sourceNode = edges[j].v;
    const src = {
      stage:getStageName(g,sourceNode),
      cluster:getClusterName(g,sourceNode)
    }
    const targetNode = edges[j].w
    const tgt = {
      stage:getStageName(g,targetNode),
      cluster:getClusterName(g,targetNode)
    }
    if(src.stage !== tgt.stage){
      edge.minlen = 3;
    }
    else if(src.stage === tgt.stage && src.stage != ''
    && src.cluster !== tgt.cluster){
      edge.minlen = 2;
    }
    // if(tgt.cluster == '' || tgt.stage == ''){
    //   edge.minlen = 1;
    // }
    // end
    edge.lineInterpolate = "basis";
  }
}

// function postprocessGraphLayout(svg:any,g:any) {
//   const clusters = svg.selectAll('.cluster');
//   _.forEach(clusters.nodes(), cNode => {
//     let clusterRect =d3.select(cNode).select('rect').nodes()[0] as any;
//     let label = d3.select(cNode).select('.label').nodes()[0] as any;
//     if(label && clusterRect){
//       let rect:SVGRect = label.getBBox()
//       // rect.height + 5 + 5
//       let height = clusterRect.getBBox().height + rect.height + 5 + 5
//       d3.select(cNode).select('rect')
//         .attr('height',height)
//         .attr("transform", "translate(" + clusterRect.getBBox().x  + ", "+ (clusterRect.getBBox().y + rect.height)+")");
//     }

//   })
// }
/*
 * Helper function to size the SVG appropriately such that all elements are displayed.
 * This assumes that all outermost elements are clusters (rectangles).
 */
function resizeSvg(id:HTMLElement,svg:any) {
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
     .attr("width", '100%')
     .attr("height", getHeight(id, width, height));
}
function getHeight(id: HTMLElement, w: number, h: number){
  let height = h;
  if(id && id.offsetWidth){
    if(w > id.offsetWidth){
      height = h * id.offsetWidth / w;
    }
  }
  return height;
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
function getAbsolutePosition(el:HTMLElement,d3selection:any) {
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
    if (obj.node() == d3.select(el).node()) {
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
