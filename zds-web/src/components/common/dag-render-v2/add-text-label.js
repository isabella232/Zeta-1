var util = require("../../../../node_modules/dagre-d3/lib/util");
import { isStageSkewed, isStageSpill } from './utilities';


module.exports = addTextLabel;

/*
 * Attaches a text label to the specified root. Handles escape sequences.
 */
function addTextLabel(root, node) {
  var domNode = root.append("text");

  var lines = processEscapeSequences(node.label).split("\n");
  for (var i = 0; i < lines.length; i++) {
	if (isStageSkewed(lines[i]) || isStageSpill(lines[i])) {
		domNode
		  .append("tspan")
		  .attr("xml:space", "preserve")
		  .attr("dy", "1.3em")
		  .attr("x", "1")
		  .attr("style", "font-weight: bold;")
		  .text(lines[i]);
	} else if (i > 0 && (isStageSkewed(lines[i - 1]) || isStageSpill(lines[i - 1]))) {
		domNode
		  .append("tspan")
		  .attr("xml:space", "preserve")
		  .attr("dy", "1.3em")
		  .attr("x", "1")
		  .text(lines[i]);
	} else {
		domNode
		  .append("tspan")
		  .attr("xml:space", "preserve")
		  .attr("dy", "1em")
		  .attr("x", "1")
		  .text(lines[i]);
	}
  }

  util.applyStyle(domNode, node.labelStyle);

  return domNode;
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
