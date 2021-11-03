'use strict';

import Neo4j from 'neo4j-driver';
import * as vis from 'vis-network/dist/vis-network.min.js';
import 'vis-network/dist/dist/vis-network.min.css';
import { defaults } from './defaults';
import {
  EventController,
  CompletionEvent,
  ClickEdgeEvent,
  ClickNodeEvent,
  ErrorEvent,
  BeforeDrawing,
  AfterDrawing,
  StartStabilizing,
  StabilizationIterationsDone
} from './events';

export const NEOVIS_DEFAULT_CONFIG = Symbol();

export default class NeoVis {
  _nodes = {};
  _edges = {};
  _data = {};
  _network = null;
  _events = new EventController();

  /**
   *
   * @constructor
   * @param {object} config - configures the visualization and Neo4j server connection
   *  {
   *    container:
   *    server_url:
   *    server_password?:
   *    server_username?:
   *    labels:
   *
   *  }
   *
   */
  constructor(config) {
    this._init(config);
  }

  _consoleLog(message, level = 'log') {
    if (level !== 'log' || this._config.console_debug) {
      // eslint-disable-next-line no-console
      console[level](message);
    }
  }

  _init(config) {
    if (config.labels && config.labels[NEOVIS_DEFAULT_CONFIG]) {
      for (const key of Object.keys(config.labels)) {
        // getting out of my for not changing the original config object
        config = {
          ...config,
          labels: {
            ...config.labels,
            [key]: { ...config.labels[NEOVIS_DEFAULT_CONFIG], ...config.labels[key] }
          }
        };
      }
    }
    if (config.relationships && config.relationships[NEOVIS_DEFAULT_CONFIG]) {
      // getting out of my for not changing the original config object
      for (const key of Object.keys(config.relationships)) {
        config = {
          ...config,
          relationships: {
            ...config.relationships,
            [key]: { ...config.relationships[NEOVIS_DEFAULT_CONFIG], ...config.relationships[key] }
          }
        };
      }
    }
    this._config = config;
    this._encrypted = config.encrypted || defaults.neo4j.encrypted;
    this._trust = config.trust || defaults.neo4j.trust;
    this._driver = Neo4j.driver(
      config.server_url || defaults.neo4j.neo4jUri,
      Neo4j.auth.basic(
        config.server_user || defaults.neo4j.neo4jUser,
        config.server_password || defaults.neo4j.neo4jPassword
      ),
      {
        encrypted: this._encrypted,
        trust: this._trust
      }
    );
    this._container = document.getElementById(config.container_id);
  }

  _addNode(node) {
    this._nodes[node.id] = node;
  }

  _addEdge(edge) {
    this._edges[edge.id] = edge;
  }

  /**
   * Build node object for vis from a neo4j Node
   * FIXME: use config
   * FIXME: move to private api
   * @param neo4jNode
   * @param session
   * @returns {{}}
   */
  async buildNodeVisObject(neo4jNode, session = null) {
    const node = {};
    const label = neo4jNode.labels[0];

    const labelConfig =
      this._config && this._config.labels && (this._config.labels[label] || this._config.labels[NEOVIS_DEFAULT_CONFIG]);

    const captionKey = labelConfig && labelConfig['caption'];
    const sizeKey = labelConfig && labelConfig['size'];
    const sizeCypher = labelConfig && labelConfig['sizeCypher'];
    const communityKey = labelConfig && labelConfig['community'];

    const title_properties = (labelConfig && labelConfig.title_properties) || Object.keys(neo4jNode.properties);

    node.id = neo4jNode.identity.toInt();

    // node size

    if (sizeCypher) {
      // use a cypher statement to determine the size of the node
      // the cypher statement will be passed a parameter {id} with the value
      // of the internal node id

      session = session || this._driver.session();
      const result = await session.run(sizeCypher, { id: Neo4j.int(node.id) });
      for (const record of result.records) {
        record.forEach(v => {
          if (typeof v === 'number') {
            node.value = v;
          } else if (Neo4j.isInt(v)) {
            node.value = v.toNumber();
          }
        });
      }
    } else if (typeof sizeKey === 'number') {
      node.value = sizeKey;
    } else {
      const sizeProp = neo4jNode.properties[sizeKey];

      if (sizeProp && typeof sizeProp === 'number') {
        // property value is a number, OK to use
        node.value = sizeProp;
      } else if (sizeProp && typeof sizeProp === 'object' && Neo4j.isInt(sizeProp)) {
        // property value might be a Neo4j Integer, check if we can call toNumber on it:
        if (sizeProp.inSafeRange()) {
          node.value = sizeProp.toNumber();
        } else {
          // couldn't convert to Number, use default
          node.value = 1.0;
        }
      } else {
        node.value = 1.0;
      }
    }
    // node caption
    if (typeof captionKey === 'function') {
      node.label = captionKey(neo4jNode);
    } else {
      node.label = neo4jNode.properties[captionKey] || label || '';
    }

    // community
    // behavior: color by value of community property (if set in config), then color by label
    if (!communityKey) {
      node.group = label;
    } else {
      try {
        if (neo4jNode.properties[communityKey]) {
          node.group = neo4jNode.properties[communityKey].toNumber() || label || 0; // FIXME: cast to Integer
        } else {
          node.group = 0;
        }
      } catch (e) {
        node.group = 0;
      }
    }
    // set configured/all properties as tooltip
    node.title = '';
    for (const key of title_properties) {
      if (neo4jNode.properties.hasOwnProperty(key)) {
        node.title += this.propertyToString(key, neo4jNode.properties[key]);
      }
    }
    return node;
  }
  /**
   * Build nodes from backend formats
   * @param backendNodes
   * @returns {[]}
   */
  buildBackendNodeVisObject(backendNodes) {
    const toRetNodes = {};
    for (const node of backendNodes) {
      const key = node.id;
      toRetNodes[key] = _.pick(node, ['id', 'properties']);
      toRetNodes[key].label = node.properties.firstName + ' ' + node.properties.lastName;

      const title = '';
      for (const [key, innerValue] of Object.entries(node.properties)) {
        title += this.propertyToString(key, innerValue);
      }
      toRetNodes[key].title = title;
    }
    return toRetNodes;
  }

  /**
   * Build edge object for vis from a neo4j Relationship
   * @param r
   * @returns {{}}
   */
  buildEdgeVisObject(r) {
    const nodeTypeConfig =
      this._config &&
      this._config.relationships &&
      (this._config.relationships[r.type] || this._config.relationships[NEOVIS_DEFAULT_CONFIG]);
    const weightKey = nodeTypeConfig && nodeTypeConfig.thickness,
      captionKey = nodeTypeConfig && nodeTypeConfig.caption;

    const edge = {};
    edge.id = r.identity.toInt();
    edge.from = r.start.toInt();
    edge.to = r.end.toInt();

    // hover tooltip. show all properties in the format <strong>key:</strong> value
    edge.title = '';
    for (const key in r.properties) {
      if (r.properties.hasOwnProperty(key)) {
        edge['title'] += this.propertyToString(key, r.properties[key]);
      }
    }

    // set relationship thickness
    if (weightKey && typeof weightKey === 'string') {
      edge.value = r.properties[weightKey];
    } else if (weightKey && typeof weightKey === 'number') {
      edge.value = weightKey;
    } else {
      edge.value = 1;
    }
    edge.value = 0.41;
    // set caption

    if (typeof captionKey === 'boolean') {
      if (!captionKey) {
        edge.label = '';
      } else {
        edge.label = r.type;
      }
    } else if (captionKey && typeof captionKey === 'string') {
      edge.label = r.properties[captionKey] || '';
    } else {
      edge.label = r.type;
    }

    return edge;
  }

  /**
   * Build edge object for vis from backend edges
   * @param r
   * @returns {{}}
   */
  buildBackendEdgeVisObject(neo4jEdges) {
    const toRetEdges = {};
    for (const key in neo4jEdges) {
      const edge = neo4jEdges[key];
      toRetEdges[key] = _.pick(edge, ['id', 'from', 'to', 'label']);
      toRetEdges[key].id = key;
      toRetEdges[key].from = edge.down_id;
      toRetEdges[key].to = edge.up_id;
      toRetEdges[key].label = '';

      toRetEdges[key].value = this._config[edge.type] || 0.41;
      const title = '';
      const props = {
        is_visitor: edge.is_visitor,
        distance: edge.distance
      };

      for (const [key, innerValue] of Object.entries(props)) {
        title += this.propertyToString(key, innerValue);
      }
      toRetEdges[key].title = title;
    }
    return toRetEdges;
  }

  propertyToString(key, value) {
    if (Array.isArray(value) && value.length > 1) {
      let out = `<strong>${key}:</strong><br /><ul>`;
      for (const val of value) {
        out += `<li>${val}</li>`;
      }
      return out + '</ul>';
    }
    return `<strong>${key}:</strong> ${value}<br>`;
  }

  // public API

  render() {
    if (this._network) this.clearNetwork();

    if (typeof this.data === 'string') {
      this.renderWithCypher();
    } else {
      this.renderWithBackend();
    }
  }

  renderWithCypher() {
    // connect to Neo4j instance
    // run query
    let recordCount = 0;

    const session = this._driver.session();
    const dataBuildPromises = [];
    session.run(this.data, { limit: 30 }).subscribe({
      onNext: record => {
        recordCount++;

        const dataPromises = Object.values(record.toObject()).map(async v => {
          if (v instanceof Neo4j.types.Node) {
            const node = await this.buildNodeVisObject(v, session);
            try {
              this._addNode(node);
            } catch (e) {}
          } else if (v instanceof Neo4j.types.Relationship) {
            const edge = this.buildEdgeVisObject(v);
            this._addEdge(edge);
          } else if (v instanceof Neo4j.types.Path) {
            const startNode = await this.buildNodeVisObject(v.start, session);
            const endNode = await this.buildNodeVisObject(v.end, session);
            this._addNode(startNode);
            this._addNode(endNode);
            for (const obj of v.segments) {
              this._addNode(await this.buildNodeVisObject(obj.start, session));
              this._addNode(await this.buildNodeVisObject(obj.end, session));
              this._addEdge(this.buildEdgeVisObject(obj.relationship));
            }
          } else if (v instanceof Array) {
            for (const obj of v) {
              if (obj instanceof Neo4j.types.Node) {
                const node = await this.buildNodeVisObject(obj, session);
                this._addNode(node);
              } else if (obj instanceof Neo4j.types.Relationship) {
                const edge = this.buildEdgeVisObject(obj);
                this._addEdge(edge);
              }
            }
          }
        });
        dataBuildPromises.push(Promise.all(dataPromises));
      },
      onCompleted: async () => {
        await Promise.all(dataBuildPromises);
        session.close();
        const options = {
          configure: {
            enabled: !!this._config.enabled,
            container: document.getElementById(this._config.configure.container_id)
          },
          interaction: {
            zoomView: false
          },
          nodes: {
            shape: 'dot',
            font: {
              size: 26,
              strokeWidth: 7
            },
            scaling: {}
          },
          edges: {
            arrows: {
              middle: { enabled: this._config.arrows || false }
            },
            length: 150,
            color: '#cccccc'
          },
          layout: {
            randomSeed: 1,
            improvedLayout: true,
            clusterThreshold: 500,
            hierarchical: {
              enabled: this._config.hierarchical || false,
              sortMethod: this._config.hierarchical_sort_method || 'hubsize'
            }
          },
          physics: {
            timestep: 1,
            // adaptiveTimestep: true,
            barnesHut: {
              // theta: 1,
              gravitationalConstant: -10000,
              springConstant: 0.1
              // springLength: 1090,
              // damping: 0.4,
            },
            stabilization: {
              iterations: 1000,
              fit: true,
              updateInterval: 100
            }
          }
        };

        const container = this._container;
        this._data = {
          nodes: new vis.DataSet(Object.values(this._nodes)),
          edges: new vis.DataSet(Object.values(this._edges))
        };
        this._network = new vis.Network(container, this._data, options);
        this._network.addEventListener('startStabilizing', () => {
          this._events.generateEvent(StartStabilizing);
        });
        this._network.addEventListener('stabilizationIterationsDone', () => {
          this._events.generateEvent(StabilizationIterationsDone);
        });
        this._network.addEventListener('afterDrawing', () => {
          this._events.generateEvent(AfterDrawing);
        });
        this._network.addEventListener('beforeDrawing', () => {
          this._events.generateEvent(BeforeDrawing);
        });
        setTimeout(() => {
          this._network.stopSimulation();
        }, 30000);
        this._events.generateEvent(CompletionEvent, { record_count: recordCount });

        const neoVis = this;
        this._network.on('click', function(params) {
          if (params.nodes.length > 0) {
            const nodeId = this.getNodeAt(params.pointer.DOM);
            neoVis._events.generateEvent(ClickNodeEvent, { nodeId: nodeId, node: neoVis._nodes[nodeId] });
          } else if (params.edges.length > 0) {
            const edgeId = this.getEdgeAt(params.pointer.DOM);
            neoVis._events.generateEvent(ClickEdgeEvent, { edgeId: edgeId, edge: neoVis._edges[edgeId] });
          }
        });
      },
      onError: error => {
        this._events.generateEvent(ErrorEvent, { error_msg: error });
      }
    });
  }

  async renderWithBackend() {
    const res = await this.data();
    if (!res.value || _.isEmpty(res.value) || res.value.nodes.length === 0) {
      this._events.generateEvent(CompletionEvent, { record_count: 0 });
      return;
    }

    this._nodes = this.buildBackendNodeVisObject(res.value.nodes);
    this._edges = this.buildBackendEdgeVisObject(res.value.edges);
    const options = {
      configure: {
        enabled: !!this._config.enabled,
        container: document.getElementById(this._config.configure.container_id)
      },
      interaction: {
        zoomView: false
      },
      nodes: {
        shape: 'dot',
        font: {
          size: 26,
          strokeWidth: 7
        },
        scaling: {}
      },
      edges: {
        arrows: {
          middle: { enabled: this._config.arrows || false }
        },
        length: 150,
        color: '#cccccc'
      },
      layout: {
        randomSeed: 1,
        improvedLayout: true,
        clusterThreshold: 500,
        hierarchical: {
          enabled: this._config.hierarchical || false,
          sortMethod: this._config.hierarchical_sort_method || 'hubsize'
        }
      },
      physics: {
        timestep: 1,
        // adaptiveTimestep: true,
        barnesHut: {
          // theta: 1,
          gravitationalConstant: -10000,
          springConstant: 0.1
          // springLength: 1090,
          // damping: 0.4,
        },
        stabilization: {
          iterations: 1000,
          fit: true,
          updateInterval: 100
        }
      }
    };
    const container = this._container;
    this._data = {
      nodes: new vis.DataSet(Object.values(this._nodes)),
      edges: new vis.DataSet(Object.values(this._edges))
    };

    this._network = new vis.Network(container, this._data, options);
    this._network.addEventListener('startStabilizing', () => {
      this._events.generateEvent(StartStabilizing);
    });
    this._network.addEventListener('stabilizationIterationsDone', () => {
      this._events.generateEvent(StabilizationIterationsDone);
    });
    this._network.addEventListener('afterDrawing', () => {
      this._events.generateEvent(AfterDrawing);
    });
    this._network.addEventListener('beforeDrawing', () => {
      this._events.generateEvent(BeforeDrawing);
    });
    setTimeout(() => {
      this._network.stopSimulation();
    }, 30000);
    this._events.generateEvent(CompletionEvent, { record_count: this._nodes.length });

    const neoVis = this;
    this._network.on('click', function(params) {
      if (params.nodes.length > 0) {
        const nodeId = this.getNodeAt(params.pointer.DOM);
        neoVis._events.generateEvent(ClickNodeEvent, { nodeId: nodeId, node: neoVis._nodes[nodeId] });
      } else if (params.edges.length > 0) {
        const edgeId = this.getEdgeAt(params.pointer.DOM);
        neoVis._events.generateEvent(ClickEdgeEvent, { edgeId: edgeId, edge: neoVis._edges[edgeId] });
      }
    });
  }

  /**
   * Clear the data for the visualization
   */
  clearNetwork() {
    this._nodes = {};
    this._edges = {};
    this._network.setData([]);
  }

  /**
   *
   * @param {string} eventType Event type to be handled
   * @param {callback} handler Handler to manage the event
   */
  registerOnEvent(eventType, handler) {
    this._events.register(eventType, handler);
  }

  /**
   * Reset the config object and reload data
   * @param config
   */
  reinit(config) {
    this._init(config);
    this.render();
  }

  /**
   * Fetch live data form the server and reload the visualization
   */
  reload() {
    this.clearNetwork();
    this.render();
  }

  /**
   * Stabilize the visuzliation
   */
  stabilize() {
    this._network.stopSimulation();
  }
}
