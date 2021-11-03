<template>
  <el-dialog label-width="80px" title="Sample" :visible.sync="visible_" @close="close" v-loading="loading">
    <el-button :disabled="!edit" type="primary" @click="addRow" icon="el-icon-plus" class="add-btn"></el-button>
    <el-table :data="tableData" height="420" :show-header="showHeader">
      <el-table-column property="text">
        <template slot-scope="scope">
          <span v-if="edit">
            <el-input style="width: 100%;" v-model="scope.row.text"></el-input>
          </span>
          <span v-else>{{ scope.row.text }}</span>
        </template>
      </el-table-column>
      <el-table-column fixed="right" width="60">
        <template slot-scope="scope">
          <el-button :disabled="!edit" @click.native.prevent="remove(scope.$index, tableData)" type="primary" size="small" icon="el-icon-delete"></el-button>
        </template>
      </el-table-column>
    </el-table>
  
    <div slot="footer" class="dialog-footer">
      <el-button type="primary" @click="confirm" v-if="edit">Confirm</el-button>
      <el-button type="default" plain @click="close">Cancel</el-button>
    </div>
  </el-dialog>
</template>

<script lang="ts">

import { Component, Vue, Prop, Watch } from "vue-property-decorator";
import Util from '@/services/Util.service';
import _ from 'lodash';
@Component({
  components: {}
})
export default class MetadataSampleDataDialog extends Vue {
  @Prop() loading: boolean;
  @Prop() visible: boolean;
  @Prop() data: any;
  @Prop() edit: boolean;
  @Prop() column: any;
  showHeader:boolean = false;


  get tableData() {
    return this.data;
  }

  set visible_(e) {
    this.$emit("update:visible", e);
  }

  get visible_(): boolean {
    return this.visible;
  }

  confirm() {
    const params: any = {
      name: this.column,
      values: _.map(this.tableData, "text")
    }
    this.$emit("dialog-comfirm", params);
  }

  close() {
    this.$emit("close");
    this.$emit("update:visible", false);
  }

  addRow() {
    const defRow = {
      text: ""
    };
    this.tableData.push(defRow);
  }

  remove(index: number, rows: Array<any>) {
    rows.splice(index, 1);
  }
}
</script>

<style lang="scss" scoped>
.el-dialog__wrapper {
  /deep/ .el-dialog__headerbtn {
    top: 20px !important;
    right: 20px !important;
  }
  /deep/ .el-dialog__body {
    padding-top: 10px;
  }
}
.el-table {
  border-top: 2px solid #cacbcf;
  border-bottom: 2px solid #cacbcf;
}
.add-btn {
  float: right;
  margin-bottom: 5px;
  margin-right: 5px;
  min-width: 0;
}
</style>
