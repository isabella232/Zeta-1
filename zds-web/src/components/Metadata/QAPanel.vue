<template>
  <div class="editor">
      <ckeditor :editor="editor.InlineEditor" v-model="editData" :disabled="editDisabled" :config="editorConfig"></ckeditor>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch } from "vue-property-decorator";
import CKEDITOR from '@/components/common/ckeditor';
import _ from "lodash";
import $ from 'jquery';

@Component({
  components: {
    
  }
})

export default class QAPanel extends Vue {
  @Prop() data: any;
  @Prop({default: true}) editDisabled: boolean;
  editor: any = CKEDITOR;
  editData: string = "";
  editorConfig: any = {
    editDisabled: true,
    removePlugins: ['MediaEmbed'],
    toolbar: [
      'heading',
      '|',
      'bold',
      'italic',
      'link',
      'bulletedList',
      'numberedList',
      'blockQuote',
      'undo',
      'redo',
      'insertTable'
    ]
  };

  constructor() {
    super();
    this.editData = this.data;
  }

  mounted() {

  }

  save() {
    const conf: any = { tabs: this.editData };
    this.$emit("submit-conf", conf);
  }

  @Watch("data")
  onDataChange() {
    this.editData = this.data;
  }
}
</script>
<style lang="scss" scoped>
.editor {
  height: 100%;
  /deep/ .ck.ck-editor {
    height: 100%;
  }
  /deep/ .ck.ck-content {
    height: 100%;
    >p {
      margin-top: 10px;
    }
  }
  /deep/ .ck.ck-editor__main {
    height: calc(100% - 115px);
  }
  /deep/ .ck-content .table {
    margin: 1em 0em !important;
  }
}
</style>
