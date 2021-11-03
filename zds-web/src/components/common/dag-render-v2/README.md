# dag render change

## layout change

change is below

old:
```js
function position(g) {
  g = util.asNonCompoundGraph(g);

  positionY(g);
  _.forEach(positionX(g), function(x, v) {
    g.node(v).x = x;
  });
}

function positionY(g) {
  var layering = util.buildLayerMatrix(g),
      rankSep = g.graph().ranksep,
      prevY = 0;
  _.forEach(layering, function(layer) {
    var maxHeight = _.max(_.map(layer, function(v) { return g.node(v).height; }));
    _.forEach(layer, function(v) {
      g.node(v).y = prevY + maxHeight / 2;
    });
    prevY += maxHeight + rankSep;
  });
}
```
new:
```js
function position(g) {
	var oldG = g;
	g = util.asNonCompoundGraph(g);

	positionY(g, oldG);

	_.forEach(positionX(g), function (x, v) {
		g.node(v).x = x;
	});
}

function positionY(g, oldG) {
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
				labelHeight = 100
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
```
### about the change
1. find the top node of one cluster & push the node to the array
2. if node or borderTop node's parent in the array then add the labelHeight = 100

## label change
- if the label is skewed or spill then change label style bold and change dy attribute to make line-space
