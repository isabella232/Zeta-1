import MetadataLineage from './metadata-lineage.vue';
import DoeRemoteService from "@/services/remote/DoeRemoteService";
import * as d3 from 'd3'
import _ from 'lodash';
import Viz from 'viz.js';
import { EventBus } from '@/components/EventBus';

const { Module, render } = require('viz.js/full.render.js');
var viz = new Viz({ Module, render });
const Constant: any = {
  setting: {
    duration: 250,
    hoverdelay: 300,
    scaleExtent: [0, 5],
    scale: 1.3,
    chevroniconwidth: 20,
    chevroniconheight: 20,
    chevronblockwidth: 20,
    expandiconwidth: 14,
    expandiconheight: 14,
    expandblockwidth: 0,
    statusiconwidth: 14,
    statusiconheight: 14,
    statusiconspace: 6,
    textleftmargin: 25,
    GROUP_THRESHOLD: 10,
    nodeBorderHeight: 16
  }
}

export {
  initSvg,
  renderPlanViz,
  getActiveNode,
  zoomIn,
  zoomOut
}
export default MetadataLineage;

/*
 * name:
 * x:  // position represent the whole node
 * y:
 * nodes: [{ x, y, height, width, text, text_x, text_y }]
 * expanded:
 */
let nodes: any = [];

/*
 * source(ref), target(ref), path, arrow
 */
let links: any = [];
let nodesData: any = [];
let linksData: any = [];
let svg: any;
let graph: any;
let zoomListener: any;
let width: any;
let height: any;
let activeNode: any = "";
let scale: any = "";

function getActiveNode() {
  return activeNode;
}

function initSvg(el: any) {
  width = el.offsetWidth;
  height = el.offsetHeight;
  scale = Constant.setting.scale;
  activeNode = "";
  zoomListener = d3.zoom()
                  .scaleExtent(Constant.setting.scaleExtent)
                  .on("zoom", zoomed);
  svg = d3.select(el).select("svg").empty() ? d3.select(el).append("svg") : d3.select(el).select("svg");
  graph = svg.select("g").empty() ? svg.append("g") : svg.select("g");
  svg.attr("viewBox", [0, 0, width, height])
    .call(zoomListener)
    .on('wheel.zoom', null)
    .on('wheel', () => {
      zoomListener.translateBy(svg, 0, d3.event.deltaY);
    });
}

function zoomed() {
  graph.attr("transform", d3.event.transform);
  scale = d3.event.transform.k;
}

function zoomIn() {
  zoomListener.scaleBy(svg, 1.1);
}

function zoomOut() {
  zoomListener.scaleBy(svg, 0.9);
}

function renderPlanViz(nodesSrc: any, linksSrc: any, active: any) {
  nodesData = _.uniqBy(nodesSrc, 'name');
  linksData = linksSrc;
  let subgraph: any = [];
  let src = 'digraph g {\n  graph [rankdir = "LR"];\n  node [shape = "rect", height=.2];\n';
  _.forEach(linksData, (v: any, i: any) => {
    const srcIndex: any = _.findIndex(nodesData, (sv: any) => {return sv.name == v.source});
    const tarIndex: any = _.findIndex(nodesData, (sv: any) => {return sv.name == v.target});
    src += srcIndex + '->' + tarIndex + ";\n";
    if (tarIndex == 0) {
      subgraph.push(srcIndex);
    }
  });
  _.forEach(nodesData, (v: any, i: any) => {
    src += i + ' [label="###\n' + v.name + '\n###"];\n';
  });
  src += "subgraph cluster_0 {\ncolor=white;\n";
  _.forEach(_.uniq(subgraph), (v: any) => {
    src += v + "\n";
  });
  src += '}\n';
  src += '}';
  viz.renderString(src).then((result: any) => {
    const parser = new DOMParser();
    const dom = parser.parseFromString(result, "text/xml");
    const domnodes = dom.getElementsByClassName('node');
    const domedges = dom.getElementsByClassName('edge');
    nodes = [];
    _.forEach(domnodes, (i: any) => {
      const name = i.getElementsByTagName('title')[0].textContent.trim();
      const polygon = i.getElementsByTagName('polygon')[0].getAttribute('points').trim();
      const texts = _.map(i.getElementsByTagName('text'), (txt: any) => {
        return {
          text: txt.textContent.trim().replace(/#/g, ''),
          text_x: txt.getAttribute('x'),
          text_y: txt.getAttribute('y')
        };
      });

      const polylines = _.map(i.getElementsByTagName('polyline'), (ply: any) => {
        return ply.getAttribute('points').trim();
      });

      const polygon_points = _.map(polygon.split(' '), (ply: any) => {
        return _.map(ply.split(','), function(p) {
          return parseFloat(p);
        });
      });

      const polygon_x_minmax = minmax((() => {
        let _j, _len1, _results;
        _results = [];
        for (_j = 0, _len1 = polygon_points.length; _j < _len1; _j++) {
          const p = polygon_points[_j];
          _results.push(p[0]);
        }
        return _results;
      })());

      const polygon_y_minmax = minmax((() => {
        var _j, _len1, _results;
        _results = [];
        for (_j = 0, _len1 = polygon_points.length; _j < _len1; _j++) {
          const p = polygon_points[_j];
          _results.push(p[1]);
        }
        return _results;
      })());

      const width = polygon_x_minmax[1] - polygon_x_minmax[0];
      const x = polygon_x_minmax[0];
      let ys = (() => {
        var _j, _len1, _results;
        _results = [];
        for (_j = 0, _len1 = polylines.length; _j < _len1; _j++) {
          const ply = polylines[_j];
          _results.push(parseFloat(ply.split(' ')[0].split(',')[1]));
        }
        return _results;
      })();

      ys = [polygon_y_minmax[0]].concat(ys).concat([polygon_y_minmax[1]]);
      const iconPosition = {x:x, y: ys[0], height: ys[1]-ys[0], width: width };
      const node: any = _.find(nodesData, (v: any) => { return v.name == texts[1].text });
      const nodeInfo: any = node && node.info ? node.info : {};
      const icons: any = {name: 'icon-job', pos: iconPosition, jobs: nodeInfo.jobs};
      const rects: any[] = [];
      let j = 0;
      while (j < ys.length - 1) {
        const rect_style = [j === 0 ? 1 : 0, j === ys.length - 2 ? 1 : 0, j === ys.length - 2 ? 1 : 0, j === 0 ? 1 : 0];
        const rct = {
          isTable: j === 0 ? true : false,
          path: _rect_path(x, ys[j], width, ys[j + 1] - ys[j], rect_style),
          path_head: _rect_path(x, ys[j], width, Constant.setting.nodeBorderHeight - 1, [1, 0, 0, 1]),
          line_head: _rect_path(x, ys[j] + Constant.setting.nodeBorderHeight - 1, width, 1, [0, 0, 0, 0]),
          path_center: _rect_path(x, ys[j] + Constant.setting.nodeBorderHeight, width, ys[j + 1] - ys[j] - 2 * Constant.setting.nodeBorderHeight, [0, 0, 0, 0]),
          line_foot: _rect_path(x, ys[j + 1] - Constant.setting.nodeBorderHeight, width, 1, [0, 0, 0, 0]),
          path_foot: _rect_path(x, ys[j + 1] - Constant.setting.nodeBorderHeight + 1, width, Constant.setting.nodeBorderHeight - 1, [0, 1, 1, 0]),
          text: texts[j + 1].text,
          text_x: texts[j + 1].text_x,
          text_y: texts[j + 1].text_y,
          x: x,
          y: ys[j],
          width: width,
          height: ys[j + 1] - ys[j],
          index: j
        };
        rects.push(rct);
        j++;
      }
      const whole_node_x = x + width / 2;
      const whole_node_y = ys[0];
      const parent = {
        name: name,
        x: whole_node_x,
        y: whole_node_y,
        nodes: rects,
        platform: nodesData[name].platform,
        db: nodesData[name].db,
        table: nodesData[name].table,
        info: nodeInfo,
        icons: icons
      };
      const _ref = parent.nodes;
      for (let _j = 0, _len1 = _ref.length; _j < _len1; _j++) {
        let chd: any = _ref[_j];
        chd.parent = parent;
      }
      nodes.push(parent);
    })

    links = [];
    _.forEach(domedges, (i: any) => {
      const source_target = i.getElementsByTagName('title')[0].textContent.split('->');
      const source = source_target[0].trim().replace(/#/g, '');
      const target = source_target[1].trim().replace(/#/g, '');
      const path = i.getElementsByTagName('path')[0].getAttribute('d');
      const arrow = i.getElementsByTagName('polygon')[0].getAttribute('points');
      const name = source + '->' + target;
      links.push({
        source: source,
        target: target,
        path: path,
        arrow: arrow,
        name: name
      });
    });
    const center: any = getGraphCenter();
    const centerNode: any = _.find(nodes, (v: any) => { return (v.db + "." + v.table) == active });
    const moveX = centerNode ? centerNode.x || center.x : center.x;
    const moveY = centerNode ? centerNode.y || center.y : center.y;
    coordMove(width/2 - scale * moveX, height/2 - scale * moveY, Constant.setting.duration);
    activeNode = active;
    sync();
  })
  .catch((error: any) => {
    // Create a new Viz instance (@see Caveats page for more info)
    var viz = new Viz({ Module, render });

    // Possibly display the error
    console.error(error);
  });
}

function minmax(list: any) {
  let i, m0, m1, _i, _len;
  m0 = Infinity;
  m1 = -Infinity;
  for (_i = 0, _len = list.length; _i < _len; _i++) {
    i = list[_i];
    if (m0 > i) {
      m0 = i;
    }
    if (m1 < i) {
      m1 = i;
    }
  }
  return [m0, m1];
};

function _rect_path(x: any, y: any, width: any, height: any, style: any) {
  var drx1, drx2, dry1, dry2, i, offset, path, r, xs, ys, _i;
  offset = Constant.setting.expandblockwidth;
  r = 4;
  drx1 = [-r, 0, r, 0];
  dry1 = [0, -r, 0, r];
  drx2 = [0, -r, 0, r];
  dry2 = [r, 0, -r, 0];
  xs = [x + width, x + width, x + offset, x + offset];
  ys = [y, y + height, y + height, y];
  path = "M" + (x + width / 2) + " " + y + " ";
  for (i = _i = 0; _i <= 3; i = ++_i) {
    if (style[i]) {
      path += "L" + (xs[i] + drx1[i]) + " " + (ys[i] + dry1[i]) + " ";
      path += "A" + r + " " + r + " 0 0 1 " + (xs[i] + drx2[i]) + " " + (ys[i] + dry2[i]) + " ";
    } else {
      path += "L" + xs[i] + " " + ys[i] + " ";
      path += "A0 0 0 0 1 " + xs[i] + " " + ys[i] + " ";
    }
  }
  path += "Z";
  return path;
};

function sync() {
  writeStorage();
  graph.selectAll('g').remove();
  generateLinks();
  generateNodes();
}

function writeStorage() {
  const node: any = _.find(nodesData, (v: any) => { return v.name == activeNode });
  getTableInfo(node);
}

function generateLinks() {
  const link = graph.selectAll('g.link').data(links, (d: any) => d.name);
  const linkEnter = link.enter().append('g').attr('class', 'link').attr('opacity', 1).attr('stroke-opacity', 1);
  linkEnter.append('path')
           .attr('stroke-opacity', 0)
           .attr('d', (d: any) => d.path)
           .transition()
           .duration(Constant.setting.duration)
           .attr('stroke-opacity', 1);

  linkEnter.append('polygon')
           .attr('opacity', 0)
           .attr('points', (d: any) => d.arrow)
           .transition()
           .duration(Constant.setting.duration)
           .attr('opacity', 1);
}

function generateNodes() {
  const nodegrp: any = graph.selectAll('g.node-grp').data(nodes , (d: any) => d.name);
  nodegrp.attr('class', 'node-grp non-expanded');
  nodegrp.enter().append('g').attr('class', 'node-grp non-expanded').attr('opacity', 1).attr('stroke-opacity', 1);

  const node: any = graph.selectAll('g.node-grp').selectAll('g.node')
                         .data((d: any) => d.nodes, (d: any) => d.text);

  const nodeEnter = node.enter()
                        .append('g')
                        .attr('class', 'node')
                        .on('click', nodeClick);

  nodeEnter.append('path')
           .attr('class', 'node-rect')
           .attr('d', (d: any) => d.path)
           .classed('active', (d: any) => d.text == activeNode);

  nodeEnter.append('path')
           .attr('class', 'node-rect-head')
           .classed('rect-not-ready', (d: any) => {return jobNotReady(d)})
           .classed('rect-pass', (d: any) => {return jobPass(d)})
           .classed('rect-not-job', (d: any) => {return jobNot(d)})
           //.classed('rect-partly', (d: any) => {return !jobNotReady(d) && !jobPass(d) && !jobNot(d)})
           .attr('d', (d: any) => d.path_head);

  nodeEnter.append('path')
           .attr('class', 'rect-seperator-line')
           .attr('d', (d: any) => d.line_head);

  nodeEnter.append('path')
           .attr('class', 'node-rect-center')
           .attr('d', (d: any) => d.path_center);

  nodeEnter.append('path')
           .attr('class', 'rect-seperator-line')
           .attr('d', (d: any) => d.line_foot);

  nodeEnter.append('path')
           .attr('class', 'node-rect-foot')
           .classed('rect-not-ready', (d: any) => {return jobNotReady(d)})
           .classed('rect-pass', (d: any) => {return jobPass(d)})
           .classed('rect-not-job', (d: any) => {return jobNot(d)})
           //.classed('rect-partly', (d: any) => {return !jobNotReady(d) && !jobPass(d) && !jobNot(d)})
           .attr('d', (d: any) => d.path_foot);

  nodeEnter.append('text')
           .attr('class', 'node-text')
           .classed('active', (d: any) => d.text == activeNode)
           .attr('opacity', 0)
           .text((d: any) => d.text)
           .attr('x', (d: any) => d.x + 3)
           .attr('y', (d: any) => d.text_y)
           .transition()
           .duration(Constant.setting.duration)
           .attr('opacity', 1);

  nodeEnter.filter(function(d: any) {
        return hasSourceNode(d);
      }).append('text')
      //}).append('image')
      .attr('class', 'icon-expand')
      .on('click', srcClick)
      .attr('width', Constant.setting.expandiconwidth)
      .attr('height', Constant.setting.expandiconheight)
      .attr('x', (d: any) => d.x - Constant.setting.expandiconwidth)
      .attr('y', (d: any) => (parseFloat(d.text_y) + 3))
      //.attr('y', (d: any) => d.y + (d.height - Constant.setting.expandiconheight) / 2)
      //.attr('xlink:href', "https://doe.corp.ebay.com/lineage/image/svg/expand-plus.png");
      .html(function(d: any){
        return "&#xe65c";
      });

  nodeEnter.filter(function(d: any) {
        return hasTargetNode(d);
      }).append('text')
      .attr('class', 'icon-expand')
      .on('click', tarClick)
      .attr('width', Constant.setting.expandiconwidth)
      .attr('height', Constant.setting.expandiconheight)
      .attr('x', (d: any) => d.x + d.width)
      .attr('y', (d: any) => (parseFloat(d.text_y) + 3))
      //.attr('xlink:href', "https://doe.corp.ebay.com/lineage/image/svg/expand-plus.png");
      .html(function(d: any){
        return "&#xe65c";
      });
}

function hasSourceNode(d: any) {
  //return (_.findIndex(linksData, (v: any) => { return v.target == d.text }) == -1) && d.parent.info.srcExpand;
  return d.parent.info.srcExpand;
}

function hasTargetNode(d: any) {
  //return (_.findIndex(linksData, (v: any) => { return v.source == d.text }) == -1) && d.parent.info.tarExpand;
  return d.parent.info.tarExpand;
}

function jobNotReady(d: any) {
  let rs: boolean = true;
  const dailyJob: any = _.filter(d.parent.icons.jobs, (v: any) => { return v.job_frequency == "daily" });
  if (d.parent.info.static_flag == 1) {
    rs = false;
  } else if (_.isEmpty(dailyJob)) {
    rs = false;
  } else {
    _.forEach(dailyJob, (v: any) => {
      if (_.toString(v.job_state) == "0") {
        rs = true;
        return;
      }
    });
  }

  return rs;
}

function jobPass(d: any) {
  let rs: boolean = true;
  const dailyJob: any = _.filter(d.parent.icons.jobs, (v: any) => { return v.job_frequency == "daily" });
  if (d.parent.info.static_flag == 1) {
    rs = true;
  }else if (_.isEmpty(dailyJob)) {
    rs = false;
  }else {
    _.forEach(dailyJob, (v: any) => {
      if (_.toString(v.job_state) == "0") {
        rs = false;
        return;
      }
    });
  }

  return rs;
}

function jobNot(d: any) {
  let rs: boolean = false;
  const dailyJob: any = _.filter(d.parent.icons.jobs, (v: any) => { return v.job_frequency == "daily" });
  if (d.parent.info.static_flag != 1 && _.isEmpty(dailyJob)) {
    rs = true;
  }

  return rs;
}

function getGraphCenter() {
  var i, x, x_max, x_min, y, y_max, y_min, _i, _len, _ref;
  x_min = y_min = Infinity;
  x_max = y_max = -Infinity;
  _ref = nodes;
  for (_i = 0, _len = _ref.length; _i < _len; _i++) {
    i = _ref[_i];
    if (x_min > i.x) {
      x_min = i.x;
    }
    if (y_min > i.y) {
      y_min = i.y;
    }
    if (x_max < i.x) {
      x_max = i.x;
    }
    if (y_max < i.y) {
      y_max = i.y;
    }
  }
  if (nodes.length) {
    x = (x_min + x_max) / 2;
    y = (y_min + y_max) / 2;
  } else {
    x = y = 0;
  }
  return {
    x: x,
    y: y
  };
};

function coordMove(dx: any, dy: any, duration: any) {
  svg.call(zoomListener.transform, d3.zoomIdentity.translate(dx, dy).scale(scale));
};

function statusIconX(d: any, i: any) {
  return d.x + d.width - (Constant.setting.chevroniconwidth + Constant.setting.chevronblockwidth) / 2;
}

function statusIconY(d: any, i: any) {
  return d.y + (d.height - Constant.setting.chevroniconheight);
}

function srcClick(event: any) {
  const node: any = event.parent;
  const find: any = _.find(nodesData, (v: any) => { return v.platform == node.platform && v.db == node.db && v.table == node.table });
  if (find && find.info && find.info.srcExpand) find.info.srcExpand = false;
  const params: any = {
    platform: _.toLower(node.platform),
    db: _.toLower(node.db),
    table: _.toLower(node.table),
    depth: 2
  }
  activeNode = params.db + "." + params.table;
  getSource(params);
}

function tarClick(event: any) {
  const node: any = event.parent;
  const find: any = _.find(nodesData, (v: any) => { return v.platform == node.platform && v.db == node.db && v.table == node.table });
  if (find && find.info && find.info.tarExpand) find.info.tarExpand = false;
  const params: any = {
    platform: _.toLower(node.platform),
    db: _.toLower(node.db),
    table: _.toLower(node.table),
    depth: 2
  }
  activeNode = params.db + "." + params.table;
  getTarget(params);
}

function nodeClick(event: any) {
  const node: any = event.parent;
  activeNode = _.toLower(node.db) + "." + _.toLower(node.table);
  //coordMove(width/2 - scale * node.x, height/3 - scale * node.y, Constant.setting.duration);
  sync();
}

function getSource(params: any) {
  const doeRemoteService = new DoeRemoteService();
  doeRemoteService.getProductionSource(params).then((res: any) => {
    let linksRs = linksData;
    let nodesRs = nodesData;
    if (res && res.data && res.data.value) {
      const arr: any = _.concat(_.filter(res.data.value, (v: any) => { return v.db_name == params.db && v.table_name == params.table && v.down_table == null }),
                                _.filter(res.data.value, (v: any) => { return v.down_db == params.db && v.down_table == params.table }));
      _.forEach(arr, (v: any) => {
        if (v.down_table != null) {
          const link: any = {
            source: v.db_name + "." + v.table_name,
            target: v.down_db + "." + v.down_table,
          }
          let info: any = v.info;
          info.srcExpand = _.find(res.data.value, (sv: any) => { return sv.down_db == v.db_name && sv.down_table == v.table_name && v.down_table != null && (sv.db_name != params.db || sv.table_name != params.table) }) ? true : false;
          const node: any = {
            name: v.db_name + "." + v.table_name,
            platform: v.platform,
            db: v.db_name,
            table: v.table_name,
            info: info
          }
          linksRs.push(link);
          nodesRs.push(node);
        }
      });
      renderPlanViz(nodesRs, linksRs, params.db + "." + params.table);
    }
  }).catch((err: any) => {

  });
}

function getTarget(params: any) {
  const doeRemoteService = new DoeRemoteService();
  doeRemoteService.getProductionTarget(params).then((res: any) => {
    let linksRs = linksData;
    let nodesRs = nodesData;
    if (res && res.data && res.data.value) {
      const arr: any = _.concat(_.filter(res.data.value, (v: any) => { return v.db_name == params.db && v.table_name == params.table && v.up_table == null }),
                                _.filter(res.data.value, (v: any) => { return v.up_db == params.db && v.up_table == params.table }));
      _.forEach(arr, (v: any) => {
        if (v.up_table != null) {
          const link: any = {
            source: v.up_db + "." + v.up_table,
            target: v.db_name + "." + v.table_name
          }
          let info: any = v.info;
          info.tarExpand = _.find(res.data.value, (sv: any) => { return sv.up_db == v.db_name && sv.up_table == v.table_name && v.up_table != null && (sv.db_name != params.db || sv.table_name != params.table) }) ? true : false;
          const node: any = {
            name: v.db_name + "." + v.table_name,
            platform: v.platform,
            db: v.db_name,
            table: v.table_name,
            info: v.info
          }
          linksRs.push(link);
          nodesRs.push(node);
        }
      });
      renderPlanViz(nodesRs, linksRs, params.db + "." + params.table);
    }
  }).catch((err: any) => {

  });
}

function getTableInfo(params: any) {
  let info: any = {
    name: activeNode
  }
  const doeRemoteService = new DoeRemoteService();
  doeRemoteService.getTableInfo(params).then((res: any) => {
    if (res && res.data && res.data.data && res.data.data.value) {
      let basicInfo: any = []
      _.forEach(res.data.data.value, (sv: any) => {
        basicInfo.push({
          subject_area: sv.subject_area,
          primary_dev_sae: sv.primary_dev_sae,
          avg_upd_time: sv.avg_upd_time,
          usage: sv.distinct_batch_cnt + sv.distinct_user_cnt,
          dev_manager: sv.dev_manager,
          owner_name: sv.owner_name
        });
      });
      info.basicInfo = basicInfo;
    }
  }).finally(() => {
    localStorage.setItem('lineage-clk-node', JSON.stringify(info));
    EventBus.$emit('set-lineage-clk-node', info);
  });
}
