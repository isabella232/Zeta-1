<template>
  <el-dialog :visible.sync="visible_" @close="close">
    <el-table :data="tableData" border height="420">
        <el-table-column property="path" label="Path" width="700"></el-table-column>
        <el-table-column property="status" label="Status"></el-table-column>
    </el-table>
    <div slot="footer" class="dialog-footer">
      <el-button type="primary" @click="cancel()">cancel</el-button>
      <el-button type="primary" @click="override()">override</el-button>
    </div>
  </el-dialog>
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch } from "vue-property-decorator";
import _ from "lodash";
@Component({
  components: {}
})
export default class PushDialog extends Vue {
  @Prop() visible: boolean;
  @Prop() data: any;

  overrideFlag: boolean = false;

  set visible_(e) {
    this.$emit("update:visible", e);
  }

  get visible_() {
    return this.visible;
  }

  get tableData() {
    let rs: any = [];
    _.forEach(this.data, (v, k) => {
      rs.push({ path: k, status: v});
    })
    return rs;
  }

  cancel() {
    this.$emit("update:visible", false);
  }

  override() {
    this.overrideFlag = true;
    this.$emit("update:visible", false);
  }

  close() {
    this.$emit("close-dialog", this.overrideFlag);
  }
}
</script>
<style lang="scss" scoped>
.el-dialog {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}
.el-input {
  margin-bottom: 22px;
}
</style>
