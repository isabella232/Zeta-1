<template>
  <div
    class="query-brief"
  >
    <!-- <CodeDisplayDialog
      ref="dialog"
      :value="query"
      :title="'SQL'"
    /> -->
    <CodeDisplay
      v-if="query"
      class="display"
      :value="lineLimit(2)(query)"
      :options="{'lineNumbers' : false}"
    />
    <i
      class="zeta-icon-sql"
      @click="openCodeDialog(query)"
    />
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Ref, Inject } from 'vue-property-decorator';
import CodeDisplay from '@/components/common/Visualization/CodeDisplay.vue';
// import CodeDisplayDialog from '@/components/common/Visualization/code-display-dialog.vue';
@Component({
  components: {
    CodeDisplay,
    // CodeDisplayDialog
  }
})

export default class QueryBrief extends Vue {
  // @Ref()
  // dialog: CodeDisplayDialog;
  
  @Prop() query: string;

  @Inject()
  openCodeDialog: (code: string) => void;

  lineLimit(maxLine: number) {
    return function(code: string) {
      const splitCode = code.split('\n');
      const line = maxLine > splitCode.length ? splitCode.length : maxLine;
      let str = '';
      for(let i = 0; i < line; i++) {
        str += splitCode[i];
        if (i != (line -1)) {
          str += '\n';
        }
      }
      return str;
    };
  }
}
</script>

<style lang="scss" >
@import "@/styles/global.scss";
.query-brief {
  background: #FBFBFB;
  padding: 10px;
  border-bottom: 1px solid #D7D7D7;
  display: flex;
  justify-content: space-between;
  i.zeta-icon-sql {
    align-self: center;
    color: $zeta-global-color;
    cursor: pointer;
  }
  .display /deep/ .CodeMirror {
    background: #FBFBFB;
  }
}
</style>