<template>
  <div
    :style="{ cursor, userSelect}"
    class="vue-splitter-container clearfix"
    @mouseup="onMouseUp"
    @mousemove="onMouseMove"
  >
    <Pane
      class="splitter-pane splitter-paneL"
      :split="split"
      :style="{ [type]: percent+'%'}"
    >
      <slot name="paneL" />
    </Pane>

    <resizer
      :class-name="className"
      :style="{ [resizeType]: percent+'%'}"
      :split="split"
      :class="active?'active':''"
      @mousedown.native="onMouseDown"
      @click.native="onClick"
    />

    <Pane
      class="splitter-pane splitter-paneR"
      :split="split"
      :style="{ [type]: 100 - percent + '%'}"
    >
      <slot name="paneR" />
    </Pane>
    <div
      v-if="active"
      class="vue-splitter-container-mask"
    />
  </div>
</template>

<script lang="ts">
import { Vue, Component, Prop, Watch } from 'vue-property-decorator';
import Resizer from './resizer.vue';
import Pane from './pane.vue';

@Component({
  components:{
    Resizer,
    Pane,
  },
})
export default class SplitPane extends Vue {
  @Prop({default: 10}) minPercent: number;
  @Prop({default: 60}) defaultPercent: number;
  @Prop({default: 'horizontal'}) split: string;

  @Prop() className: string;
  active = false;
  hasMoved = false;
  percent = this.defaultPercent;

  constructor () {
    super();
    this.percent = this.defaultPercent;
  }
  get type (){
    return this.split === 'vertical' ? 'width' : 'height';
  }
  get resizeType (){
    return this.split === 'vertical' ? 'left' : 'top';
  }
  get userSelect () {
    return this.active ? 'none' : '';
  }
  get cursor () {
    return this.active ? (this.split === 'vertical' ? 'col-resize' : 'row-resize') : '';
  }
  get paneRPercent (){
    return `calc(100% - ${this.percent}%)`;
  }
  @Watch('defaultPercent')
  handleDefaultPercent (newValue, oldValue){
    this.percent = newValue;
  }

  onClick () {
    if (!this.hasMoved) {
      this.percent = 50;
      this.$emit('resize', this.percent);
    }
  }
  onMouseDown () {
    this.active = true;
    this.hasMoved = false;
  }
  onMouseUp () {
    this.active = false;
  }
  onMouseMove (e) {
    if (e.buttons === 0 || e.which === 0) {
      this.active = false;
    }

    if (this.active) {
      let offset = 0;
      let target = e.currentTarget;
      if (this.split === 'vertical') {
        while (target) {
          offset += target.offsetLeft;
          target = target.offsetParent;
        }
      } else {
        while (target) {
          offset += target.offsetTop;
          target = target.offsetParent;
        }
      }

      const currentPage = this.split === 'vertical' ? e.pageX : e.pageY;
      const targetOffset = this.split === 'vertical' ? e.currentTarget.offsetWidth : e.currentTarget.offsetHeight;
      const percent = Math.floor(((currentPage - offset) / targetOffset) * 10000) / 100;

      if (percent > this.minPercent && percent < 100 - this.minPercent) {
        this.percent = percent;
      }

      this.$emit('resize', this.percent);
      this.hasMoved = true;
    }
  }
}
</script>

<style scoped>
.clearfix:after {
  visibility: hidden;
  display: block;
  font-size: 0;
  content: " ";
  clear: both;
  height: 0;
}

.vue-splitter-container {
  height: 100%;
  position: relative;
}

.vue-splitter-container-mask {
  z-index: 9999;
  width: 100%;
  height: 100%;
  position: absolute;
  top: 0;
  left: 0;
}
</style>
