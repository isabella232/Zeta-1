<template>
  <el-dialog
    label-width="80px"
    :title="dialogTitle"
    :visible.sync="visible_"
    :close-on-click-modal="false"
    width="800px"
    class="crt-notebook-dialog"
  >
    <EditNotebook
      v-if="visible"
      :file="file"
      v-on="$listeners"
      @new-notebook-canceled="onCanceled"
    />
  </el-dialog>
</template>

<script lang="ts">
/**
 * Component <EditNotebookDialog>. Popup dialogue for creation of new notebook.
 * Used by <RepoList>.
 */
import { Component, Prop, Vue, } from 'vue-property-decorator';
import EditNotebook from './edit-notebook.vue';
import { IFile } from '@/types/repository';

@Component({
  components: {
    EditNotebook
  }
})
export default class EditNotebookDialog extends Vue {

  @Prop()
  visible: boolean;

  @Prop()
  file: IFile;

  get dialogTitle() {
    return 'Edit Notebook';
  }

  set visible_(e) {
    this.$emit('update:visible', e);
  }
  get visible_(): boolean {
    return this.visible;
  }

  onCanceled() {
    this.visible_ = false;
  }
}
</script>

<style lang="scss" scoped>
.crt-notebook-dialog {

  /deep/ .el-form {
    .el-form-item {
      margin-bottom: 0;
      &.is-error {
        margin-bottom: 10px;
      }
      .block-input{
        width: 100%;
      }
      .el-select{
        width: 220px;
      }
    }
  }
}
</style>
