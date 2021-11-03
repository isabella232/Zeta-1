<template>
  <el-button
    type="text"
    class="notebook-tool-btn"
    @click="clone()"
  >
    <i class="el-icon-document-copy" />
    <span class="params-title">{{ title }}</span>
  </el-button>
</template>

<script lang="ts">
import { ZETA_ACTIONS } from '@/components/common/Navigator/nav.config';
import { Notebook } from '@/types/workspace';
import { Component, Vue, Prop } from 'vue-property-decorator';
@Component({
  components: {},
})
export default class NotebookClone extends Vue {
  @Prop()
  notebook: Notebook;
  @Prop() title: string;

  clone () {
    this.$router.push('/repository'
            + `?internal_action=${ZETA_ACTIONS.CLONE_NOTEBOOK}` +
            `&internal_action_params=${JSON.stringify({
              noteboodId: this.notebook.notebookId,
            })}`);
  }
}
</script>
<style lang="scss" scoped>
.notebook-tool-btn {
  position: relative;
  margin-right: 10px;
  margin-top: -3px;
  .el-icon-document-copy{
    transition: .3s;
    font-size: 16px;
  }
  .params-title{
    font-size: 12px;
    line-height: 20px;
    transition: .3s;
    position: absolute;
    display: none;
  }
  &:hover{
    .el-icon-document-copy{
      margin-right: 35px;
    }
    .params-title{
      transform: translateX(-35px);
      display: inline-block;
    }
  }
}
</style>
