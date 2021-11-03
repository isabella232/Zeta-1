<template>
    <div class="connect-progress">
        <el-progress :text-inside="true" :stroke-width="10" :percentage="formatProgress"></el-progress>
    </div>
</template>
<script lang="ts">
import { Component, Vue, Prop} from "vue-property-decorator";

@Component({})
export default class Progress extends Vue{
    @Prop() progress: number;
    get formatProgress(){
        return this.progress?this.progress*100:0;
    }
}
</script>
<style lang="scss" scoped>
    .connect-progress{
        border-left: 1px solid #dddddd;
        border-right: 1px solid #dddddd;
        .el-progress{
            line-height: 9px;
            font-size: 10px;
        }
    }
</style>
