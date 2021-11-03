<template>
  <el-dialog
    ref="dialog"
    title="Hint"
    :visible.sync="_visible"
    custom-class="download-dialog"
    width="600px"
    :show-close="false"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
  >
    <div class="content">
      <div
        class="alert"
      >Browser may block the large file download. Please make sure allow the pop-up to finish the download process.</div>
      <div class="image">
        <img :src="'./img/blocked.png'" />
      </div>
      <div class="dialog-footer">
        <el-checkbox v-model="showAgain">Never show again.</el-checkbox>
        <el-button type="primary" @click="confirm">Ok</el-button>
      </div>
    </div>
  </el-dialog>
</template>
<script lang="ts">
import { Component, Vue, Prop } from 'vue-property-decorator';
import { Getter, Action } from 'vuex-class';
import { ZetaException } from '@/types/exception';
@Component({
  components: {}
})
export default class DownloadNotification extends Vue {
  @Action updateUserPreference2: (preference: Dict<any>) => Promise<any>;
  @Prop() visible: boolean;
  showAgain = false;

  get _visible() {
    return this.visible;
  }
  set _visible(e) {
    this.$emit('update:visible', e);
  }
  confirm() {
    this.$emit('confirm-download', this.showAgain);
    this._visible = false;
    this.updatePreference();
  }
  updatePreference(){
    this.updateUserPreference2({
      'download-notification': this.showAgain
    }).catch((e: ZetaException) => {
      e.resolve();
    });
  }
}
</script>
<style lang="scss" scoped>
/deep/ .download-dialog {
  .alert {
    word-break: normal;
    font-size: 15px;
  }
  .image {
    margin: 20px 0;
    img {
      max-width: 100%;
    }
  }
  .dialog-footer {
    padding: 15px 0;
    display: flex;
    justify-content: space-between;
  }
}
</style>