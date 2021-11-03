<template>
  <el-dialog label-width="80px" title="Smart Query" :visible.sync="visible_" v-loading="loading" append-to-body>
    <div class="row">
      <div class="label">Join With:</div>
      <el-select v-model="selectTable" placeholder="Search the table you want to join..." filterable @change="onSelectTableChange($event)" style="width:350px">
        <el-option v-for="item in smartQueryTableArr" :key="item.join_table" :label="item.join_table.toUpperCase()" :value="item.join_table"></el-option>
      </el-select>
    </div>
    <div class="row">
      <div class="label">Platform:</div>
      <span style="line-height: 30px;">{{ pltfrm.replace("NuMozart", "Mozart") }}</span>
    </div>
    <SqlEditor ref="editor" class="editor" :value="formatSQL(query)"></SqlEditor>
    <div slot="footer" class="dialog-footer">
      <el-button type="default" plain @click="copytext">Copy</el-button>
    </div>
  </el-dialog>
</template>

<script lang="ts">

import { Component, Vue, Prop, Inject, Watch } from "vue-property-decorator";
import DoeRemoteService from "@/services/remote/DoeRemoteService";
import SqlEditor from "@/components/common/Visualization/CodeDisplay.vue";
import Util from '@/services/Util.service';
import copy from 'copy-to-clipboard';
import _ from 'lodash';

@Component({
  components: {
    SqlEditor
  }
})
export default class SmartQueryDialog extends Vue {
  doeRemoteService = DoeRemoteService.instance;
  
  @Prop() mainTable: any;
  @Prop() visible: boolean;
  @Prop() joinTable: any;
  @Prop() smartQueryTableArr: any;

  loading: boolean = false;
  selectTable: any = "";
  pltfrm: string = "";
  query: string = "";
  rsObj: any = {};

  set visible_(e) {
    this.$emit("update:visible", e);
  }

  get visible_(): boolean {
    this.selectTable = _.clone(this.joinTable);
    if (!_.isEmpty(this.joinTable) || !_.isEmpty(this.mainTable)) this.getSmartQuery(this.mainTable + "," + this.joinTable);
    return this.visible;
  }

  getSmartQuery(tables: any) {
    const params: any = { tables: tables };
    this.loading = true;
    this.rsObj = {};
    this.query = "";
    this.pltfrm = "";
    this.doeRemoteService.getSmartQuery(params).then(res => {
      this.loading = false;
      if (res && res.data && res.data.data && res.data.data.value && res.data.data.value.length > 0) {
        this.rsObj = res.data.data.value[0];
        this.query = this.rsObj.query_text;
        this.pltfrm = this.rsObj.platform;
      }
    }).catch(err => {
      this.loading = false;
			console.error("getSmartQuery: " + JSON.stringify(err));
		});
  }
  
  formatSQL(sql: string) {
    return Util.SQLFormatter(sql);
  }
  
  copytext() {
    const copyFlg: any = copy(Util.SQLFormatter(this.query));
    if (copyFlg) {
      this.$message.success("Copy to clipboard success!")
    }
  }

  onSelectTableChange(e: any) {
    this.getSmartQuery(this.mainTable + "," + e);
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
.row {
  display: flex;
  padding-bottom: 20px;
}
.label {
  font-weight: 700;
  line-height: 30px;
  width: 100px;
}
.el-input {
  width: 200px;
  /deep/ .el-input__inner {
      width: 90px;
  }
}
.add-btn {
  float: right;
  margin-bottom: 5px;
  margin-right: 5px;
  min-width: 0;
}
.editor {
  height: 400px;
  border: 1px solid #dddddd;
  /deep/ .CodeMirror-gutters {
    border: 0px !important;
  }
}
</style>
