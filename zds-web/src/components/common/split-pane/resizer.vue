<template>
  <div :class="classes" />
</template>

<script lang="ts">
import { Vue, Component, Prop } from 'vue-property-decorator';

@Component({

})
export default class ResizeBar extends Vue {
  @Prop() split: string;
  @Prop() className: string;

  get classes () {
    const classes = ['splitter-pane-resizer', this.split, this.className];
    return classes.join(' ');
  }
}
</script>

<style scoped>
.splitter-pane-resizer {
  -moz-box-sizing: border-box;
	-webkit-box-sizing: border-box;
	box-sizing: border-box;
	background: #b0b3bb;
	position: absolute;
	/* opacity: .2; */
	z-index: 10;
	-moz-background-clip: padding;
	-webkit-background-clip: padding;
	background-clip: padding-box;
}
.splitter-pane-resizer:hover,
.splitter-pane-resizer.active{
  background-color: #409eff;
}
.splitter-pane-resizer.horizontal {
  height: 8px;
  width: 100%;
	margin: -3px 0;
	border-top: 3px solid transparent;
	border-bottom: 3px solid transparent;
	cursor: row-resize;

}

.splitter-pane-resizer.vertical {
  width: 8px;
	height: 100%;
	margin-left: -3px;
	border-left: 3px solid transparent;
	border-right: 3px solid transparent;
	cursor: col-resize;
}
.splitter-pane-resizer.horizontal:before {
  display: block;
  content: "";
  width: 40px;
  height: 0px;
  position: absolute;
  top: 50%;
  left: 50%;
  margin-left: -20px;
  margin-top: -1px;
  border-top: 1px solid #409eff;
  border-bottom: 1px solid #409eff;
}
.splitter-pane-resizer.vertical:before {
  display: block;
  content: "";
  width: 0px;
  height: 40px;
  position: absolute;
  top: 50%;
  left: 50%;
  margin-top: -20px;
  margin-left: -1px;
  border-left: 1px solid #409eff;
  border-right: 1px solid #409eff;
}
.only-code .splitter-pane-resizer{
  display: none;
}
</style>
