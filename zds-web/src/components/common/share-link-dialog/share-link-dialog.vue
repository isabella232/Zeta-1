<template>
  <el-dialog
    :visible.sync="visible"
    custom-class="share-link-dialog"
    :title="title"
  >
    <div>
      <el-input
        ref="input"
        v-model="url"
        readonly
        class="link"
      >
        <el-button
          slot="append"
          v-click-metric:NB_TOOLBAR_CLICK="{name: 'shareLinkcopyClick'}"
          @click="copy"
        >
          Copy link
        </el-button>
      </el-input>
      <p class="notify-info">please don't share the notebook to user should not have data access.</p>
    </div>
  </el-dialog>
</template>

<script lang="ts">
import { Component, Vue, Prop } from 'vue-property-decorator';

@Component({
  components: {}
})
export default class ShareLinkDialog extends Vue {
  @Prop()
  value: string;
  @Prop() title: string;
  visible = false;
  get url() {
    return this.value;
  }
  copy() {
    const $input = this.$refs.input as any;
    $input.select();
    document.execCommand('copy');
  }
  open() {
    this.visible = true;
  }
}
</script>
<style lang="scss" scoped>
/deep/ .share-link-dialog {
  .el-dialog__body {
    padding: 10px 10px 20px 10px;
  }
}
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
.notify-info{
    padding-top: 5px;
    margin-left: 5px;
    font-size: 12px;
    color:#999;
    word-break: break-word;
}
</style>



