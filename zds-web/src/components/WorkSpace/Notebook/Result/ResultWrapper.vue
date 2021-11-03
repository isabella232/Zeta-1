<template>
  <!--
    HACK: Set min-width:0 to shrink editor within screen size.
    REF: https://github.com/codemirror/CodeMirror/issues/4895
-->
  <div
    ref="result-wrapper"
    row
    style="min-width: 0; height: 100%;"
    class="result-wrapper"
  >
    <el-tabs
      type="border-card"
      @tab-click="ontabClick"
    >
      <el-tab-pane v-if="runAccess">
        <span slot="label">Result</span>
        <ResultTabs
          v-if="activeTab === 'result'"
          :notebook="notebook"
        />
      </el-tab-pane>
      <el-tab-pane>
        <span slot="label">History</span>
        <History :notebook="notebook" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script lang="ts">
/**
 * Component <ResultWrapper>. Wrapper of <ResultTabs> and <History>.
 *
 * @prop notebook Current notebook.
 */
import { Component, Vue, Prop, Inject } from 'vue-property-decorator';
import ResultTabs from './QueryResult/ResultTabs.vue';
import History from './QueryHistory/History.vue';
import { INotebook } from '@/types/workspace';
import '@/styles/result.scss';

@Component({
  components: {
    ResultTabs, History,
  },
})

export default class ResultWrapper extends Vue {
  @Inject() viewOnly: boolean;
  @Inject() runAccess: boolean;

  @Prop() notebook: INotebook;
  activeTab = 'result';

  constructor () {
    super();
  }

  ontabClick (e: any) {
    /* e.index === "1", i.e. the second tab.
         * Notify <History> to reload when activate history tab.
         */
    this.activeTab = e.index === '0' ? 'result' : 'history';
    // let clickHistory = e.index === "1";
    // if (clickHistory) {
    //     EventBus.$emit("reload-history");
    // }
  }
}

</script>

<style lang="scss" scoped>
.el-tabs /deep/ .el-tabs__nav-scroll {
    // cursor: ns-resize;

    .el-tabs__nav {
        user-select: none;
    }
}
.result-wrapper{
  position: relative;
  transition: 0.3s;
  &.result-hide{
    display: none;
  }
}
</style>


