"use strict";

var _ = require("lodash"),
addLabel = require("./add-label"),
util = require("../../../../node_modules/dagre-d3/lib/util");
var d3 = require("d3");

module.exports = createNodes;

function createNodes(selection, g, shapes) {
  var simpleNodes = g.nodes().filter(function(v) { return !util.isSubgraph(g, v); });
  var svgNodes = selection.selectAll("g.node")
    .data(simpleNodes, function(v) { return v; })
    .classed("update", true);

  svgNodes.exit().remove();

  svgNodes.enter()
    .append("g")
      .attr("class", function(v){
          return v.class ? 'node ' + v.class : 'node'
      })
      .style("opacity", 0);

  svgNodes = selection.selectAll("g.node");

  svgNodes.each(function(v) {
    var node = g.node(v),
        thisGroup = d3.select(this);
    util.applyClass(thisGroup, node["class"],
      (thisGroup.classed("update") ? "update " : "") + "node");

    thisGroup.select("g.label").remove();
    var labelGroup = thisGroup.append("g").attr("class", "label"),
        lableLine = getTextLabelLine(labelGroup, node),
        labelDom = addLabel(labelGroup, node),
        shape = shapes[node.shape],
        bbox = _.pick(labelDom.node().getBBox(), "width", "height");

    node.elem = this;
    node.line = lableLine;

    if (node.id) { thisGroup.attr("id", node.id); }
    if (node.labelId) { labelGroup.attr("id", node.labelId); }

    if (_.has(node, "width")) { bbox.width = node.width; }
    if (_.has(node, "height")) { bbox.height = node.height; }

    bbox.width += node.paddingLeft + node.paddingRight;
    bbox.height += node.paddingTop + node.paddingBottom;
    labelGroup.attr("transform", "translate(" +
      ((node.paddingLeft - node.paddingRight) / 2) + "," +
      ((node.paddingTop - node.paddingBottom) / 2) + ")");

    var root = d3.select(this);
    root.select(".label-container").remove();
    var shapeSvg = shape(root, bbox, node).classed("label-container", true);
    util.applyStyle(shapeSvg, node.style);

    var shapeBBox = shapeSvg.node().getBBox();
    node.width = shapeBBox.width;
    node.height = shapeBBox.height;
  });

  var exitSelection;

  if (svgNodes.exit) {
    exitSelection = svgNodes.exit();
  } else {
    exitSelection = svgNodes.selectAll(null); // empty selection
  }

  util.applyTransition(exitSelection, g)
    .style("opacity", 0)
    .remove();

  return svgNodes;
}

function getTextLabelLine(root, node){
  var label = node.label;
  var lines = 0;
  if (node.labelType === "svg") {
  } else if (typeof label !== "string" || node.labelType === "html") {
  } else {
    lines = processEscapeSequences(node.label).split("\n");
  }
  
  return lines.length;
}
function processEscapeSequences(text) {
  var newText = "",
      escaped = false,
      ch;
  for (var i = 0; i < text.length; ++i) {
    ch = text[i];
    if (escaped) {
      switch(ch) {
        case "n": newText += "\n"; break;
        default: newText += ch;
      }
      escaped = false;
    } else if (ch === "\\") {
      escaped = true;
    } else {
      newText += ch;
    }
  }
  return newText;
}
