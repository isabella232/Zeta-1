<template>
    <div v-on:mousemove.capture="onMousemove" @mouseup="onMouseup" ref="wrapper">
        <div :style="upStyle" class="up">
            <slot name="up"></slot>
        </div>
        <div @mousedown.stop="onMousedown" :style="seperatorStyle" class="seperator"></div>
        <div :style="downStyle" ref="result-panel" class="down">
            <slot name="down"></slot>
        </div>
    </div>
</template>

<script lang='ts'>
/**
 * Component <VPisces>, a vertically resizable UI layout component.
 * This component divides wrapper container into two vertically aligned sub-containers.
 * The initial ratio of height of two sub-container can be specified via properties.
 * In addition, the sub-container seperator allows dragging to manipulate ratio dynamically.
 *
 * @prop minUpRatio Minimum ratio of height of upper sub-container / height of wrapper container.
 * @prop maxUpRatio Maximum ratio of height of upper sub-container / height of wrapper container.
 * @prop upRatio Initial ration of height of upper sub-container / height of wrapper container.
 * @param slot="up" Element to be placed in upper sub-container.
 * @param slot="down" Element to be placed in lower sub-container.
 */
import { Vue, Component, Prop, Watch } from "vue-property-decorator";

let pct = (r: number) => r * 100 + "%";
type Style = { [K: string]: string | number };

@Component({
    components: {}
})
export default class VPisces extends Vue {
    @Prop({ default: 0.1 }) minUpRatio: number;
    @Prop({ default: 0.9 }) maxUpRatio: number;
    @Prop({ default: 0.5 }) upRatio: number;

    upStyle_: Style;
    seperatorStyle_: Style;
    downStyle_: Style;
    dragging: boolean;
    lastClientY: number | null;
    seperatorTop: number;

    constructor() {
        super();

        this.upStyle_ = {
            height: pct(this.upRatio)
        };

        this.seperatorStyle_ = {
            top: pct(this.upRatio)
        };

        this.downStyle_ = {
            height: `calc(${pct(1 - this.upRatio)} - 30px)`
        };

        this.dragging = false;
        this.lastClientY = null;
        this.seperatorTop = 0;
    }

    get seperatorTopRatio() {
        return this.seperatorTop / this.getWrapperHeight();
    }

    get upStyle() {
        return {
            ...this.upStyle_,
            height: this.seperatorTopRatio
                ? pct(this.seperatorTopRatio)
                : this.upStyle_.height
        };
    }

    get seperatorStyle() {
        return {
            ...this.seperatorStyle_,
            top: this.seperatorTopRatio
                ? pct(this.seperatorTopRatio)
                : this.upStyle_.height
        };
    }

    get downStyle() {
        return {
            ...this.downStyle_,
            height: this.seperatorTopRatio
                ? `calc(${pct(1 - this.seperatorTopRatio)} - 30px)`
                : this.downStyle_.height
        };
    }

    getWrapperHeight() {
        let wrapper = this.$refs["wrapper"] as HTMLElement;
        return wrapper ? wrapper.clientHeight : 0;
    }

    onMousedown(e: MouseEvent) {
        this.$data.dragging = true;
        this.$data.lastClientY = e.clientY;
        this.$data.seperatorTop =
            this.getWrapperHeight() * this.seperatorTopRatio;
    }

    onMouseup(e: MouseEvent) {
        this.$data.dragging = false;
    }

    onMousemove(e: MouseEvent) {
        if (!this.$data.dragging) return;
        e.stopPropagation();
        let dy = e.clientY - this.$data.lastClientY;
        let top = this.seperatorTop + dy;
        let ratio = top / this.getWrapperHeight();
        if (ratio > this.$props.maxUpRatio || ratio < this.$props.minUpRatio)
            return;
        this.$data.seperatorTop = top;
        this.$data.lastClientY = e.clientY;
    }

    mounted() {
        this.$data.seperatorTop = this.getWrapperHeight() * this.$props.upRatio;
        this.$on("draggerMousedown", (e: MouseEvent) => this.onMousedown(e));
    }
    @Watch('upRatio')
    handle(newVal: number, oldVal: number){

        this.upStyle_ = {
            height: pct(this.upRatio)
        };

        this.seperatorStyle_ = {
            top: pct(this.upRatio)
        };

        this.downStyle_ = {
            height: `calc(${pct(1 - this.upRatio)} - 30px)`
        };
        this.$data.seperatorTop = this.getWrapperHeight() * this.$props.upRatio;
        this.$on("draggerMousedown", (e: MouseEvent) => this.onMousedown(e));
    }
}
</script>

<style lang="scss" scoped>
.wrapper {
    border: "1px solid black";
    position: "relative";
    height: "100%";
    width: "100%";
}

.seperator {
    width: 100%;
    border-top: 1px solid #cccccc;
    cursor: ns-resize;
    z-index: 2;
    height: 30px;
}
</style>


