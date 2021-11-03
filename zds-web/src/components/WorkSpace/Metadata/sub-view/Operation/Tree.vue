<template>
  <div
    class="tree-chart"
    @click="update"
  >
    <div id="tree" />
  </div>
</template>

<script lang="ts">
import { Vue, Component, Prop, Watch } from 'vue-property-decorator';
import { Gray, FontColor, Green, Blue, Orange } from './chartOptions';
import * as d3 from 'd3';
import 'd3-hierarchy';
import { attempt } from '@drewxiu/utils';

@Component({
  components: {
  }
})
export default class TreeChart extends Vue {

  @Prop() data;

  width: number;
  container;
  root: any;
  svg: any;
  wrapper: any;

  mounted() {
    this.container = document.getElementById('tree');
    this.width = attempt(() => this.container.parentElement.getBoundingClientRect().width, 1000) * 0.85;
  }

  renderSvgWrapper() {
    const width = this.width;
    let x0 = Infinity;
    let x1 = -x0;
    this.root.each(d => {
      if (d.x > x1) x1 = d.x;
      if (d.x < x0) x0 = d.x;
    });
    const svg = d3.create('svg')
      .attr('viewBox', [0, 0, width, x1 - x0 + this.root.dx * 2].join());

    const g = svg.append('g')
      .attr('font-family', 'sans-serif')
      .attr('font-size', 8)
      .attr('transform', `translate(${this.root.dy / 3},${this.root.dx - x0})`);
    this.svg = svg;
    this.wrapper = g;
  }

  renderLinks() {
    const links = this.wrapper.append('g')
      .attr('fill', 'none')
      .attr('stroke', '#ccc')
      .attr('stroke-width', 0.5)
      .selectAll('path')
      .data(this.root.links())
      .enter()
      .append('path')
      .attr('d', d3.linkHorizontal()
        .x((d: any) => (this.width - this.root.dy) -d.y)
        .y((d: any) => d.x) as any);
    return links;
  }

  renderNodes() {
    const { width, root } = this;
    const node = this.wrapper.append('g')
      .attr('stroke-linejoin', 'round')
      .attr('stroke-width', 2)
      .selectAll('g')
      .data(this.root.descendants())
      .enter()
      .append('g')
      .attr('transform', (d: any) => `translate(${(width - root.dy) -d.y},${d.x})`);

    node.append('text')
      .attr('dy', '0.04em')
      .attr('text-anchor', 'middle')
      .text((d: any) => d.data.name)
      .on('mouseenter', (d: any) => {
        this.onMouseEnter(d3.event, d);
      })
      .on('mouseleave', () => {
        this.onMouseLeave();
      })
      .on('click', this.onLabelClick)
      .style('cursor', 'pointer')
      .style('fill', (d: any) => {
        if (d.data.info.status === 'unknown') {
          return FontColor;
        }
        return 'white';
      });

    node.insert('rect','text')
      .attr('width', (d: any) => d.data.name.length * 4.5)
      .attr('height', 15)
      .attr('rx', 4)
      .attr('transform', (d: any) => `translate(${-(d.data.name.length * 4.5 / 2)}, ${-9.5})`)
      .style('stroke', 'black')
      .style('stroke-width', 0.1)
      .style('fill', (d: any) => {
        switch (d.data.info.status) {
          case 'unknown':
            return 'white';
          case 'warning':
          case 'ready':
            return Green;
          case 'not ready':
            if (
              (d.data.children.length > 0 && !d.data.children.find(i => i.info.status === 'not ready')) ||
              (d.data.children.length === 0 && d.data.expandable)
            ) {
              return Blue;
            }
            // otherwise fallthrough
          default:
            return Gray;
        }
      });

    node.insert('text','text')
      .attr('dy', '0.04em')
      .attr('dx', (d: any) => -(d.data.name.length * 4.5 / 2) - 6)
      .attr('font-weight', 'bold')
      .attr('font-size', 12)
      .attr('text-anchor', 'middle')
      .text('+')
      .on('click', (d: any) => {
        this.onExpand(d);
      })
      .style('cursor', 'pointer')
      .style('fill', (d: any) => {
        if (d.data.children.length === 0 && d.data.expandable) {
          return Green;
        }
        return 'transparent';
      });

    node.append('circle')
      .attr('r', 2.5)
      .attr('transform', (d: any) => `translate(${(d.data.name.length * 4.5 / 2)}, -9)`)
      .style('fill', (d: any) => {
        const status = d.data.info.status;
        switch (status) {
          case 'warning':
          case 'unknown':
            return Orange;
          default:
            return 'transparent';
        }
      });
  }

  @Watch('data')
  update() {
    if (!this.data) return;

    this.root = this.tree(this.data) as any;
    
    this.renderSvgWrapper();
    this.renderLinks();
    this.renderNodes();  
  
    this.container.innerHTML = '';
    // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
    this.container.appendChild(this.svg.node()!);
  }

  tree(data) {
    const width = 954;
    const root = d3.hierarchy(data) as any;
    root.dx = 20;
    root.dy = width / (root.height + 1);
    return d3.tree().nodeSize([root.dx, root.dy])(root);
  }

  onExpand(tableData) {
    this.$emit('expand', tableData);
  }
  onLabelClick(tableData) {
    this.$emit('click', tableData);
  }
  onMouseEnter(event, tableData) {
    this.$emit('mouseenter', event, tableData);
  }
  onMouseLeave() {
    this.$emit('mouseleave');
  }
}
</script>
<style lang="scss" scoped>
.tree-chart {
  min-height: 200px;
}
</style>
