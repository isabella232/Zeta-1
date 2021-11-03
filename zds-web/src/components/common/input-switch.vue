<template>
    <div class="input-switch" @click="onClick">
      <el-input v-if="!disabled && modify" class="input-switch-modify" ref="elInput" :value="text" @input="onValueChange" @blur="onBlur" maxlength="10"/>
      <div v-else class="input-switch-display">{{text}}</div>
    </div>
</template>

<script lang="ts">
import { Component, Vue, Watch, Prop, Emit } from "vue-property-decorator";
import { ComponentSize, Input } from "element-ui";

@Component({
    components: {}
})
export default class InputSwitch extends Vue {
    @Prop({default: false})
    disabled: boolean

    @Prop({default: ''})
    value: string

    /**
     * @deprecated
     */
    @Prop({default: 'mini'})
    size: ComponentSize

    get text() {
        return this.value;
    }
    modify = false;
    @Emit('input')
    onValueChange(val: string){

    }
    onClick() {
        if(this.disabled) {
            return
        }
        this.modify = true;
        this.$nextTick(() => {
            (this.$refs['elInput'] as Input).focus()
        })
    }
    onBlur() {
        this.modify = false;
    }
}
</script>
<style lang="scss" scoped>
@import "@/styles/global.scss";
</style>


