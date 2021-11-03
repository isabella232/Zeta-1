<template>
  <div>
    <el-dialog
      title="Select Database"
      :visible.sync="_visible"
      :close-on-click-modal="false"
      :append-to-body="true"
      top="calc(50vh - 105px)"
      width="500px"
      class="database-dialog"
    >
      <div class="password">
        <el-form ref="form" label-width="110px">
          <el-form-item label="Kylin Password">
            <el-input
              v-model="form.password"
              :type="pwShow?'text':'password'"
              placeholder="This is same as your NT password"
              :disabled="showDatabase"
            >
            <i slot="suffix" @click="pwShow=!pwShow" class="el-input__icon el-icon-view"></i>
            </el-input>
             <div class="url-error-message" v-if="alertShow"><i class="el-icon-warning"/>{{alertMessage}}</div>
          </el-form-item>
          <el-form-item class="actions">
            <el-button
              type="primary"
              @click="queryDatabase"
              :disabled="!(form.password) || showDatabase"
            >Submit</el-button>
          </el-form-item>
        </el-form>
      </div>
      <div class="db" v-if="showDatabase" v-loading="loading">
        <div class="db-setting">
          <span>Database</span>
          <el-select v-model="databaseValue" placeholder="select" @change="handleChange">
            <el-option
              v-for="(item,index) in database"
              :key="index"
              :label="item.name"
              :value="item.name"
            ></el-option>
          </el-select>
        </div>
        <div slot="footer" class="dialog-footer">
          <el-button type="primary" @click="handleConnect" :disabled="databaseValue===''">Connect</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>
<script lang="ts">
import { Component, Vue, Prop, Emit, Watch } from "vue-property-decorator";
import { IConnection, CodeType } from "@/types/workspace";
import NotebookRemoteService from "@/services/remote/NotebookRemoteService";
import Util from "@/services/Util.service";
import { ZetaException } from "@/types/exception";
const KYLIN_CONNECTION = Util.getDefaultConnectionByPlat(CodeType.KYLIN);

const notebookRemoteService = new NotebookRemoteService();
@Component({
  components: {}
})
export default class Database extends Vue {
  @Prop() visible: boolean;
  @Prop() connection: IConnection;
  private database: Array<any> = [];
  private databaseValue: string = "";
  private pwShow: Boolean = false;
  private showDatabase: Boolean = false;
  private loading: Boolean = false;
  private alertShow = false;
  private alertMessage = '';

  set _visible(e) {
    this.$emit("update:visible", e);
  }
  get _visible(): boolean {
    return this.visible;
  }
  form = {
    password: ""
  };
  handleChange(e: string) {
    this.databaseValue = e;
  }
  queryDatabase() {
    const host = KYLIN_CONNECTION.hostMap[this.connection.alias];
    const ssl = /rno/.test(this.connection.alias) || false;

    let payload = {
      ssl: ssl,
      host: host,
      user: Util.getNt(),
      password: this.form.password
    };
    this.showDatabase = true;
    this.loading = true;
    notebookRemoteService.getKylinProject(payload).then(res => {
      let data = res.data;
      this.loading = false;
      this.database = data;
    }).catch((e: ZetaException)=>{
      e.resolve();
      this.showDatabase = false;
      this.loading = false;
      this.alertShow = true;
      this.alertMessage = e.message?e.message:"UnKnown Exception";
    });
  }
  handleConnect() {
    this._visible = false;
    this.loginKylin(this.databaseValue, this.form.password);
  }
  @Watch("form.password")
  onchangePassword(){
    if(this.alertShow) this.alertShow = false;
  }
  @Watch("_visible")
  onChangeVisible(newVal:Boolean, oldVal:Boolean){
    if(newVal!==oldVal && newVal === true){
      this.form.password = '';
      this.showDatabase = false;
      this.databaseValue = '';
    }
  }
  @Emit("loginKylin")
  loginKylin(value: string, password:string) {}
}
</script>
<style lang="scss" scoped>
@import "@/styles/global.scss";
.database-dialog {
  /deep/ .el-dialog__body {
    padding-bottom: 30px;
  }
  .db-setting {
    display: flex;
    align-items: center;
    margin: 15px 0;
    > span {
      width: 110px;
      text-align: right;
      padding-right: 12px;
      box-sizing: border-box;
    }
    .el-select {
      flex: 1;
    }
  }
  .dialog-footer{
    text-align: right;
  }
  .actions {
    text-align: right;
    .el-button {
      min-width: 90px;
    }
  }
  .url-error-message{
    line-height: 16px;
    font-size: 14px;
    color: $zeta-global-color-red;
    display: flex;
    align-items: center;
  }
  /deep/ .el-input__suffix{
      cursor: pointer;
  }
}
</style>


