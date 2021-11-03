<template>
  <el-dialog
    title="Upload Files"
    :visible.sync="visible_"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :before-close="handleRemove"
    width="720px"
    class="upload-dialog"
  >
    <el-form
      ref="form"
      v-loading="uploadLoading"
      :model="form"
      label-position="left"
    >
      <div
        v-if="form.fileList.length>0"
        class="file-list"
      >
        <el-form-item
          v-for="(file,index) in form.fileList"
          :key="index"
        >
          <el-row :gutter="10">
            <el-col :span="23">
              <el-form-item
                label="File Name"
                label-width="80px"
                :prop="'fileList.'+ index"
                :rules="rules[0]"
                :error="file.nameError"
              >
                <el-input v-model="file.fileName" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="10">
            <el-col :span="23">
              <el-form-item
                label="File Path"
                label-width="80px"
                prop="path"
                :rules="rules[1]"
              >
                <el-input
                  v-model="form.path"
                  placeholder="path"
                >
                  <template slot="prepend">
                    {{ rootPath }}
                  </template>
                </el-input>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="10">
            <el-col :span="23">
              <el-form-item
                label="File Type"
                label-width="80px"
                :prop="'fileList.'+ index"
              >
                <el-input
                  v-model="file.fileType"
                  :disabled="true"
                />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="10">
            <el-col :span="23">
              <el-form-item
                label="Platform"
                label-width="80px"
                :prop="'fileList.'+ index"
              >
                <el-radio-group v-model="form.cluster">
                  <el-radio
                    :label="2"
                  >
                    Ares
                  </el-radio>
                  <el-radio
                    :label="14"
                  >
                    Apollo Reno
                  </el-radio>
                  <el-radio
                    :label="10"
                  >
                    Hercules
                  </el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="10">
            <el-col :span="23">
              <el-form-item
                label="OverWrite"
                label-width="80px"
                :prop="'fileList.'+ index"
              >
                <el-checkbox v-model="form.isOverWrite" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form-item>
      </div>
    </el-form>
    <div
      slot="footer"
      class="dialog-footer"
    >
      <el-button
        type="primary"
        :loading="uploadLoading"
        :disabled="(form.fileList.length === 0)"
        @click="upload"
      >
        Upload
      </el-button>
    </div>
    <el-progress
      v-if="uploadLoading"
      type="circle"
      :percentage="progress"
      :status="uploadProgressStatus"
    />
  </el-dialog>
</template>

<script lang='ts'>
/**
 * Component <UploadPackageDialog>. Popup dialogue for upload file of HDFS Management.
 * Used by <HDFSList>.
 */
import { Component, Vue, Prop, Watch, Inject } from 'vue-property-decorator';
import Util from '@/services/Util.service';
import HDFSRemoteService from '@/services/remote/HDFSRemoteService';
import _ from 'lodash';
import {
  ZetaUploadFileDetail,
  MultiPackageForm,
  parseFolder,
} from './utilities';
@Component({
  components: {},
})
export default class UploadPackageDialog extends Vue {
  @Inject('HDFSRemoteService') HDFSRemoteService: HDFSRemoteService;
  @Prop() visible: boolean;
  @Prop() fileList: Array<ZetaUploadFileDetail>;
  private uploadLoading= false;
  private rootPath = '/user/';
  private progress = 0;
  private cluster = 14;
  set visible_ (e) {
    this.$emit('update:visible', e);
  }
  get visible_ (): boolean {
    return this.visible;
  }
  get nt (){
    return Util.getNt();
  }
  get folder (){
    return this.$store.getters.folder;
  }
  get uploadProgressStatus (){
    if (this.progress<100){
      return null;
    } else if (this.progress===100){
      return 'success';
    }
  }
  get subPath (){
    const subP = this.folder.slice(this.rootPath.length);
    return subP;
  }
  form: MultiPackageForm;
  rules: {};
  constructor () {
    super();
    this.rootPath += this.nt;
    this.form = {
      fileList: this.fileList,
      cluster: this.cluster,
      isOverWrite: false,
      path: this.subPath,
    };
    this.rules = [
      {
        validator: (r: any, f: any, cb: Function) => {
          if (!_.trim(f.fileName)) cb(new Error('please input file name'));
          else if (_.trim(f.fileName).length>90) cb(new Error('max length 90'));
          else if (/^(\.|_)/.test(f.fileName)) cb(new Error('Not allowed . or _ beginning'));
          else if (/[^a-zA-Z\-\.0-9_]/.test(f.fileName))
            cb(
              new Error(
                'File name not valid. Only alphanumeric, _, -, . are allowed.'
              )
            );
          else cb();
        },
        trigger: 'blur',
      }, {
        validator: (r: any, f: any, cb: Function) => {
          if (/[^a-zA-Z\/\-0-9_]/.test(f))
            cb(
              new Error(
                'Folder name not valid. Only alphanumeric,/, _, -,are allowed.'
              )
            );
          else cb();
        },
      },
    ];
  }
  handleRemove () {
    this.form.fileList = [];
    this.visible_ = false;
    this.$emit('remove-file');
  }
  upload () {
    let valid = false;
    (this.$refs['form'] as any).validate((valid_: boolean) => {
      valid = valid_;
    });
    if (!valid) {
      // eslint-disable-next-line no-console
      console.log('Invalid Upload name');
      return;
    }
    this.form.fileList[0].nameError = '';

    this.uploadLoading = true;
    const path = parseFolder(this.rootPath+this.form.path);
    this.progress = 0;
    this.HDFSRemoteService.upload(this.form, path, (progressEvent: ProgressEvent)=>{
      if (progressEvent.lengthComputable) {
        const val = (progressEvent.loaded / progressEvent.total * 100).toFixed(0);
        this.progress = parseInt(val);
      }
    }).then(res =>{
      this.uploadLoading = false;
      if (res.data.code == 200){
        this.visible_ = false;
        this.$emit('upload-success');
      } else {
        this.form.fileList[0].nameError = res.data.msg || 'Exist PackageName, please click overWrite';
      }
    }).catch(e=>{
      this.uploadLoading = false;
    });
  }
  @Watch('visible')
  onChangeVisible (newVal: boolean) {
    if (newVal) {
      this.uploadLoading = false;
      this.form = {
        fileList: this.fileList,
        cluster: this.cluster,
        isOverWrite: false,
        path: this.subPath,
      };
    }
  }
}
</script>

<style lang='scss' scoped>
@import "@/styles/global.scss";
.upload-dialog {
  /deep/ .interpreter-select {
    width: 100%;
  }
  .upload-file {
    /deep/ .el-upload-list__item:first-child {
      margin-top: 5px;
    }
  }
  .el-upload__tip {
    line-height: 30px;
    margin-top: 0;
    color: $zeta-font-light-color;
  }
  .file-list {
    margin-top: 10px;
    margin-bottom: 10px;
  }
  .upload-status {
    .el-icon-upload-success {
      color: #67c23a;
    }
    .el-icon-close {
      cursor: pointer;
    }
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
    }
  }
  /deep/ .el-dialog__body{
    position: relative;
    .el-progress{
      position: absolute;
      top: 50%;
      left: 50%;
      margin-left: -63px;
      margin-top: -63px;
      z-index: 2001;
      background-color: rgba( 255, 255, 255, .8);
    }
  }
}
</style>
