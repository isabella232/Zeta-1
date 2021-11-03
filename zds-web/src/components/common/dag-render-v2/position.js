"use strict";

var _ = require("lodash"),
	util = require("../../../../node_modules/dagre/lib/util"),
	positionX = require("../../../../node_modules/dagre/lib/position/bk").positionX;

module.exports = position;

function position(g, originG) {
	var oldG = g;
	g = util.asNonCompoundGraph(g);

	positionY(g, oldG, originG);

	_.forEach(positionX(g), function (x, v) {
		g.node(v).x = x;
	});
}

function positionY(g, oldG, originG) {
	var layering = util.buildLayerMatrix(g),
		rankSep = g.graph().ranksep,
		prevY = 0;
	let nodes = oldG.nodes();
	let minRankNodeList = []
	_.forEach(nodes, node => {
		if (oldG.children(node).length) {
			let children = oldG.children(node).filter(child => ['border', 'edge'].indexOf(oldG.node(child).dummy) === -1)
			if (children.length) {
				let minRank = getNodeRank(oldG.node(children[0]));
				let minRankNode = children[0];
				for (let i = 1; i < children.length; i++) {
					let rank = getNodeRank(oldG.node(children[i]));
					if (rank < minRank) {
						minRank = rank;
						minRankNode = children[i];
					}
				}
				minRankNodeList.push(minRankNode);
			}
		}
	});
	_.forEach(layering, function (layer) {
		var maxHeight = _.max(_.map(layer, function (v) { return g.node(v).height; }));
		var labelHeight = 0;
		_.forEach(layer, function (v) {
			if ((minRankNodeList.indexOf(oldG.parent(v)) !== -1 && g.node(v).dummy === 'border' && /^_bt/.test(v)) || minRankNodeList.indexOf(v) !== -1) {
				labelHeight = 100;
				var pId = oldG.parent(v);
				var p =  originG.node(pId);
				while(p&&p.label){
					let lines = processEscapeSequences(p.label).split("\n").length;
					labelHeight = 14*(lines+1);
					pId = oldG.parent(pId);
					p = originG.node(pId);
				}
				// if(p&&p.label){
				// 	let lines = processEscapeSequences(p.label).split("\n").length;
				// 	// console.log('--line',lines);
				// 	labelHeight = 14*(lines);
				// }
			}
			g.node(v).y = prevY + maxHeight / 2 + labelHeight;
		});
		prevY += maxHeight + rankSep + labelHeight;
	});
	function getNodeRank(node) {
		if (node.rank) {
			return node.rank
		}
		if (node.minRank) {
			return node.minRank
		}
	}
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