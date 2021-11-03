<template>
  <el-dialog
    :visible.sync="visible"
    custom-class="code-display-dialog"
    :title="title"
  >
    <div v-if="visible">
      <CodeDisplay :value="code" />
    </div>
  </el-dialog>
</template>

<script lang="ts">
import { Component, Vue, Prop } from 'vue-property-decorator';
import CodeDisplay from './CodeDisplay.vue';

@Component({
  components: {
    CodeDisplay
  }
})
export default class CodeDisplayDialog extends Vue {
  @Prop()
  value: string;
  @Prop() title: string;
  visible = false;
  get code() {
    return this.value;
  }
  open() {
    this.visible = true;
  }
}
</script>
<style lang="scss" scoped>
/deep/ .code-display-dialog {
  .el-dialog__body {
    padding: 10px 10px 20px 10px;
  }
}
/deep/ .CodeMirror {
    background: #FBFBFB;
    .CodeMirror-scroll {
      max-height: 500px;
    }
  }
</style>



