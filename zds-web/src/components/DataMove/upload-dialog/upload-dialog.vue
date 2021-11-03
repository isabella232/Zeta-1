<template>
  <el-dialog
    title="Schema"
    :visible.sync="visible_"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="false"
    width="720px"
    class="upload-dialog"
  >
    <div class="file-list" >
      <el-form :model="form" ref="form" class="schema" v-if="form.schema.length>0">
          <el-row :gutter="10">
            <el-col :span="14">Column Name</el-col>
            <el-col :span="10">Type</el-col>
          </el-row>
          <el-form-item v-for="(item, index) in form.schema" :key="index">
            <el-row :gutter="10">
              <el-col :span="14">
                <el-form-item 
                  :prop="'schema.'+ index"
                  :rules="rules.name"
                >
                  <el-input v-model="item.name"></el-input>
                </el-form-item>
              </el-col>
              <el-col :span="10" class="type">
                <el-form-item :prop="'schema.'+ index">
                  <el-select v-model="item.type" >
                    <el-option
                      v-for="(option) in initType"
                      :key="option.value"
                      :label="option.value"
                      :value="option.value"
                    ></el-option>
                  </el-select>
                </el-form-item>
                <el-form-item :prop="'schema.'+ index" :rules="rules.length1" v-if="showLength(item.type)">
                  <el-input  v-model="item.length1" :placeholder="setPlaceholder(item.type)"></el-input>
                </el-form-item>
                <el-form-item :prop="'schema.'+ index" :rules="rules.length2" v-if="item.type === 'decimal'">
                  <el-input  v-model="item.length2" placeholder="0"></el-input>
                </el-form-item>
              </el-col>
            </el-row>
          </el-form-item>
      </el-form>
    </div>
    <div slot="footer" class="dialog-footer">
      <el-button
        type="primary"
        @click="save"
        :loading="uploadLoading"
      >{{edit?"OK":"Next"}}</el-button>
      <el-button type="default" plain @click="edit?(visible_ = false):removeFile()">Cancel</el-button>
    </div>
  </el-dialog>
</template>

<script lang='ts'>
/**
 * Component <UploadPackageDialog>. Popup dialogue for upload file of packagelist.
 * Used by <PackageList>.
 */
import { Component, Vue, Prop, Emit, Watch, Inject } from "vue-property-decorator";
import {
  INotebook,
  IPreference,
} from "@/types/workspace";
import { WorkspaceSrv as NBSrv } from "@/services/Workspace.service";
import { RestPacket } from "@/types/RestPacket";
import Util from "@/services/Util.service";
import DMRemoteService from '@/services/remote/DataMove'
import _ from "lodash";
import config from "@/config/config";
import { ZetaException } from "@/types/exception";
import { Form } from 'element-ui'
import {
  ZetaUploadFileDetail,
  MultiPackageForm,
  parseFolder
} from "./utilities";
import { ElUploadInternalFileDetail } from "element-ui/types/upload";
@Component({
  components: {}
})
export default class UploadPackageDialog extends Vue {
  @Inject('dmRemoteService') dmRemoteService: DMRemoteService;
  @Prop() visible: boolean;
  @Prop() schema: Array<any>;
  @Prop() edit: boolean;
  private uploadLoading: boolean= false;
  private initType = [
    {value: 'string'},
    {value: 'varchar'},
    {value: 'int'},
    {value: 'bigint'},
    {value: 'float'},
    {value: 'double'},
    {value: 'decimal'},
    {value: 'date'},
    {value: 'timestamp'}
  ]
  set visible_(e) {
    this.$emit("update:visible", e);
  }
  get visible_(): boolean {
    return this.visible;
  }
  get nt(){
    return Util.getNt();
  }

  showLength(type: string){
    return type==='varchar'||type==='char'||type === 'decimal'?true: false
  }
  setPlaceholder(type: string){
    let placeholder = '64';
    switch(type){
      case 'varchar':
        break;
      case 'char':
        placeholder = '1';
        break;
      case 'decimal':
        placeholder = '18'
        break;
    }
    return placeholder;
  }
  form: any;
  constructor() {
    super();
    this.form = {
      schema: []
    };
  }
  public rules = {
      name: [
        {
          validator: (r: any, f: any, cb: Function) => {
            if (!_.trim(f.name)) cb(new Error("please input column name"));
            else if (/^[0-9]+$/.test(f.name))cb(new Error("Column name not valid. Can't be a pure number"))
            else if (/[^a-zA-Z\-0-9_]/.test(f.name))
              cb(
                new Error(
                  "Column name not valid. Only alphanumeric, _, - are allowed."
                )
              );
            else cb();
          },
          trigger: "blur"
      }],
      length1:[
        {
          validator: (r: any, f: any, cb: Function) => {
            if (!_.trim(f.length1)) cb(new Error("please input length"));
            else if (/[^0-9]/.test(f.length1))
              cb(
                new Error(
                  "not valid. Only number are allowed."
                )
              );
            else cb();
          },
          trigger: "blur"
      }],
    length2:[
      {
        validator: (r: any, f: any, cb: Function) => {
          if (!_.trim(f.length2)) cb(new Error("please input length"));
          else if (/[^0-9]/.test(f.length2))
            cb(
              new Error(
                "not valid. Only number are allowed."
              )
            );
          else cb();
        },
        trigger: "blur"
    }]
  }
  save(){
    (this.$refs['form'] as Form).validate(valid => {
        if(valid) {
          this.visible_ = false;
        }
    })
  }
  @Emit('remove-file')
  removeFile(){
    this.visible_ = false;
  }
  @Watch('schema')
  handleSchema(newVal: any, oldVal: any){
    this.form.schema = newVal;
  }
}
</script>

<style lang='scss' scoped>
@import "@/styles/global.scss";
.upload-dialog {
  .el-upload__tip {
    line-height: 30px;
    margin-top: 0;
    color: $zeta-font-light-color;
  }
  .file-list {
    width: 100%;
    max-height: 570px;
    overflow-x: hidden;
    overflow-y: auto;
  }
  /deep/ .el-input__inner {
    height: 30px !important;
  }
  /deep/ .el-form {
    .el-form-item {
      margin-bottom: 0;
      &.is-error {
        margin-bottom: 22px;
      }
      .type.el-col{
        display: flex;
      }
    }
  }
}
</style>