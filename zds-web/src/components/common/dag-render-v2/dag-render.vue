<template>
  <div class="dag-render" v-loading="loading || !domSrc" element-loading-text="loading...">
    <!-- <div class="no-data" v-if="!src" v-loading="loading">No Data</div> -->
    <template v-if="src">
      <div class="dag-view">
        <div class="dag-global-info" v-html="globalInfo"></div>
        <div ref="dagCtnr" class="dag-container" :style="`transform: scale(${scale})`"></div>
      </div>
      <div class="zoom-tool">
        <i class="el-icon-remove-outline" @click="zoomOut"></i>
        <i class="el-icon-circle-plus-outline" @click="zoomIn"></i>
        <i class="el-icon-refresh" @click="zoomReset"></i>
      </div>
    </template>
    <div class="dag-view-error" v-else>
      Execution plan not available now...
    </div>
    
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch } from "vue-property-decorator";
import _ from "lodash";
import { initPlanViz, renderPlanViz, parseDotFile, parseGlobalInfo } from ".";
import * as d3 from "d3";
@Component({
  components: {}
})
export default class DagRenderV2 extends Vue {
  @Prop()
  domSrc: string;
  @Prop({ default: false })
  loading: boolean;
  rerenderHook: (el: any, dotFile: string) => void;

  scale: number = 1;

  get src() {
    return parseDotFile(this.domSrc);
  }

  get globalInfo() {
    return parseGlobalInfo(this.domSrc);
  }

  constructor() {
    super();
    this.rerenderHook = _.throttle(initPlanViz, 1000 * 3, {
      leading: true
    });
  }
  mounted() {
    const $el = this.$refs.dagCtnr;
    if (!$el || !this.src) {
      return;
    }
    initPlanViz($el, this.src);
  }

  @Watch("domSrc")
  onSrcChange(domSrc: string) {
    if(this.src){
      this.rerenderHook(this.$refs.dagCtnr, this.src as string);
    }
  }

  zoomIn() {
    if (this.scale >= 2) return;
    this.scale += 0.1;
  }

  zoomOut() {
    if (this.scale <= 0.5) return;
    this.scale -= 0.1;
  }

  zoomReset() {
    this.scale = 1;
  }
}
</script>
<style lang="scss" scoped>
$white: #fff;
$green: #69ad85;
$green1: #c7e2d1;
$green2: #e8efea;

$blue: rgb(86, 156, 226);
$blue1: rgb(221, 235, 249);
$blue2: #eef5fc;

$grey: #ddd;
$grey1: #e9eaeb;
$grey2: #f4f5f5;

$red: #d66363;
$red1: #efb4b9;
$red2: #f9dcdf;

$yellow: #f6ba68;
$yellow1: #fce9d0;
$yellow2: #fef5ea;
/* */
$bgMap: (
  finished: (
    node: $green,
    cluster: $green1,
    stage: $green2
  ),
  running: (
    node: $blue,
    cluster: $blue1,
    stage: $blue2
  ),
  pending: (
    node: $grey,
    cluster: $grey1,
    stage: $grey2
  ),
  failed: (
    node: $red,
    cluster: $red1,
    stage: $red2
  ),
  skewed: (
    node: $yellow,
    cluster: $yellow1,
    stage: $yellow2
  )
);
$textMap: (
  finished: (
    node: $white,
    cluster: $green,
    stage: $green
  ),
  running: (
    node: $white,
    cluster: $blue,
    stage: $blue
  ),
  pending: (
    node: #999,
    cluster: #999,
    stage: #999
  ),
  failed: (
    node: $white,
    cluster: $red,
    stage: $red
  ),
  skewed: (
    node: $white,
    cluster: $yellow,
    stage: $yellow
  )
);

@mixin nodeColor($bg: #ddd, $text: #999, $stroke: #fff, $strokeWidth: 1px) {
  rect {
    fill: $bg;
    @if $stroke {
      stroke: $stroke;
    }
    @if $strokeWidth {
      stroke-width: $strokeWidth;
    }
  }
  text {
    fill: $text;
  }
}
@mixin clusterColor($bg: #ddd, $text: #999) {
  @include nodeColor($bg, $text, false, false);
}
@mixin gStyle {
  @each $status, $subMap in $bgMap {
    @each $type, $bgColor in $subMap {
      $textColor: map-get(map-get($textMap, $status), $type);
      @if $type == node {
        g.node.#{$status} {
          @include nodeColor($bgColor, $textColor);
        }
      } @else if $type == cluster {
        g.cluster.#{$status} {
          @include clusterColor($bgColor, $textColor);
        }
      } @else if $type == stage {
        g.cluster.stage.#{$status} {
          @include clusterColor($bgColor, $textColor);
        }
      }
    }
  }
}
.no-data {
  font-size: 14px;
  text-align: center;
  font-weight: 700px;
  height: 60px;
  line-height: 60px;
}
.zoom-tool {
  line-height: 40px;
  position: absolute;
  bottom: 100px;
  right: 0;
  > i{
    font-size: 20px;
    width: 20px;
    height: 20px;
    color: #569ce1;
    margin-right: 10px;
    cursor: pointer;
    &:hover{
         color: #39a758;
    }
  }
}
.dag-view {
  height: 100%;
  width: 100%;
  overflow: auto;
  .dag-container {
    transform-origin: top left;
  }
}
.dag-render /deep/ {
  height: 100%;
  font-size: 14px;
  position: relative;
  overflow: auto;
  .label {
    font-weight: normal;
    text-shadow: none;
  }
  svg {
    @include gStyle;
    g.cluster {
      rect {
        fill: $grey1;
        stroke-width: 0px;
      }
    }
    g.node {
      rect {
        fill: $grey;
        stroke: #fff;
        stroke-width: 1px;
      }
    }
    path {
      fill: #999;
      stroke: #999;
      stroke-width: 1.5px;
    }
    text {
      fill: #999;
      :first-child {
        font-weight: bold;
      }
      > tspan {
        font-family: "ArialMT", "Arial";
      }
    }
  }
}
.tooltip-inner {
  word-wrap: break-word;
}
.dag-view-error {
  padding-top: 40px;
  text-align: center;
  font-size: 20px;
}
</style>



