<template>
  <el-popover
    placement="bottom"
    width="400"
    trigger="click"
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
    <el-button
      slot="reference"
      v-click-metric:NB_TOOLBAR_CLICK="{name: 'shareLink'}"
      type="text"
      class="notebook-tool-btn"
    >
      <i class="zeta-icon-share" />
      <span class="params-title">{{ title }}</span>
    </el-button>
  </el-popover>
</template>

<script lang="ts">
/**
 * Component <ShareLink>  Only use notebook share link
 * Other should use '@/components/common/share-link'
 */
import { Component, Vue, Prop } from 'vue-property-decorator';

@Component({
  components: {},
})
export default class ShareLink extends Vue {
  @Prop()
  value: string;
  @Prop() title: string;
  get url() {
    return this.value;
  }
  copy() {
    const $input = this.$refs.input as any;
    $input.select();
    document.execCommand('copy');
  }
}
</script>
<style lang="scss" scoped>
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
