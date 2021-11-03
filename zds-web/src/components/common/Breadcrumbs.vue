<template>
    <div class="breadcrumbs">
        {{appName}}
        <span class="separator">></span>
        {{path}}
        <template v-for="(sp,$i) in subPath" v-if="sp">
            <span class="separator" :key="$i">></span>
            {{sp}}
        </template>
    </div>
</template>

<script lang="ts">
import { Component, Vue, Watch } from "vue-property-decorator";

@Component({
    components: {}
})
export default class Breadcrumbs extends Vue {
    subPath: string[] = [];
    get appName() {
        return this.$appName == "zeta" ? "Zeta" : "Zeta Share";
    }
    get path(): string {
        return this.$route.name || "";
    }
    addSubPath(path: string) {
        this.subPath.push(path);
    }
    setSubPath(paths: string[]) {
        this.subPath = paths;
    }
    mounted() {
        Vue.prototype.$breadcrumbs = this;
    }
}
</script>
<style lang="scss" scoped>
@import "@/styles/global.scss";
.breadcrumbs {
    height: $workspace-tab-height;
    margin-left: -15px;
    margin-right: -15px;
    padding: 0 15px;
    margin-bottom: $workspace-tab-margin-bottom;
    color: #999;
    display: flex;
    align-items: center;
    -webkit-box-shadow: 0 2px 4px 0 rgba(0, 0, 0, 0.12),
        0 0 6px 0 rgba(0, 0, 0, 0.04);
    box-shadow: 0 2px 4px 0 rgba(0, 0, 0, 0.12), 0 0 6px 0 rgba(0, 0, 0, 0.04);
    .separator {
        margin-left: 10px;
        margin-right: 10px;
        color: $zeta-font-color;
    }
}
</style>


