<template>
  <el-dialog :visible.sync="visible" custom-class="apply-package-dialog" @opened="opened">
    <PackageList ref="packageList" :notebook-id="notebookId" @close="close"/>
  </el-dialog>
</template>
<script lang="ts">
import { Component, Vue, Prop, Watch, Emit, Provide, Inject, Ref, } from "vue-property-decorator";
import PackageList from './package-list.vue';
@Component({
  components: { PackageList, },
})
export default class ApplyPackageWrapper extends Vue {
  @Prop()
  notebookId: string;

  @Ref('packageList')
  packageList: PackageList;

  private visible: boolean = false;
  public open() {
    this.visible = true;

  }
  private opened() {
    this.packageList.init()
  }
  public close() {
    this.visible = false;
  }
}
</script>
<style lang="scss" scoped>
.el-dialog__wrapper /deep/ .apply-package-dialog {
  .el-dialog__body {
    padding: 30px 30px;
  }
}
</style>