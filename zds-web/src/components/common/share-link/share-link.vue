<template>
    <el-popover placement="bottom" width="400" trigger="click">
        <div>
            <el-input v-model="url" readonly ref="input" class="link">
                <el-button slot="append" @click="copy" 
                    v-click-metric:NB_TOOLBAR_CLICK="{name: 'shareLinkcopyClick'}">Copy link</el-button>
            </el-input>
        </div>
        <el-button type="text" slot="reference" class="notebook-tool-btn" v-click-metric:NB_TOOLBAR_CLICK="{name: 'shareLink'}">
            <i class="zeta-icon-share"></i>
            <span class="params-title">{{title}}</span>
        </el-button>
    </el-popover>
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch } from "vue-property-decorator";

@Component({
    components: {}
})
export default class ShareLink extends Vue {
    @Prop()
    value: string;
    @Prop() title: string;
    get url() {
        return this.value;
    }
    copy() {
        const $input = this.$refs.input as any;
        $input.select();
        document.execCommand("copy");
    }
}
</script>
<style lang="scss" scoped>
    .notebook-tool-btn {
        position: relative;
    .zeta-icon-share{
        transition: .3s;
        font-size: 20px;
    }
    .params-title{
        font-size: 12px;
        line-height: 20px;
        transition: .3s;
        position: absolute;
        display: none;
    }
    &:hover{
        .zeta-icon-share{
            margin-right: 30px;
        }
        .params-title{
            transform: translateX(-30px);
            display: inline-block;
        }
    }
}
</style>



