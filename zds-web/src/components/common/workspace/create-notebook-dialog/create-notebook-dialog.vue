<template>
  <el-dialog
    label-width="80px"
    :title="dialogTitle"
    :visible.sync="visible_"
    :close-on-click-modal="false"
    width="800px"
    class="crt-notebook-dialog"
  >
    <CloneNotebook
      v-if="isClone"
      :folder="folder"
      :clone-id="cloneId"
      :clone="clone"
      v-on="$listeners"
      @new-notebook-canceled="onCanceled"
    />
    <CreateNotebook
      v-else
      :folder="folder"
      v-on="$listeners"
      @new-notebook-canceled="onCanceled"
    />
  </el-dialog>
</template>

<script lang="ts">
/**
 * Component <NewNotebookDialogue>. Popup dialogue for creation of new notebook.
 * Used by <NotebookTabs> and <RepoList>.
 */
import { Component, Prop, Vue, } from 'vue-property-decorator';
import CreateNotebook from './create-notebook.vue';
import CloneNotebook from './clone-notebook.vue';

@Component({
  components: {
    CreateNotebook,
    CloneNotebook
  }
})
export default class CreateNotebookDialog extends Vue {

  @Prop()
  visible: boolean;

  @Prop({ default: ''})
  clone: 'zeta' | 'zeppelin' | '';

  @Prop()
  cloneId: string;

  @Prop({ default: '/', type: String })
  folder: string;

  get dialogTitle () {
    return this.isClone ? 'Clone Notebook' : 'Create Notebook';
  }

  set visible_ (e) {
    this.$emit('update:visible', e);
  }
  get visible_ (): boolean {
    return this.visible;
  }

  get isClone () {
    return this.clone && this.cloneId;
  }

  onCanceled () {
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
