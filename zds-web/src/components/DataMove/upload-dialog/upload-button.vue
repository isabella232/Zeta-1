<template>
  <div>
    <el-upload
      ref="upload"
      class="upload-file"
      :limit="1"
      :show-file-list="false"
      action
      accept=".csv,.parquet"
      :file-list="fileList"
      :multiple="false"
      :auto-upload="false"
      :on-change="handleChange"
      :on-exceed="handleExceed"
    >
      <div class="upload">
        <el-button plain :loading="uploadLoading">Upload</el-button>
        <span slot="tip" class="el-upload__tip">（.csv/.parquet, size limit 100M）</span>
      </div>
    </el-upload>
    <div
      v-if="fileList.length>0"
      class="file-list"
    >
      <el-row
        v-for="(file,index) in fileList"
        :key="index"
        :gutter="10"
      >
        <el-col :span="1">
          <i class="zeta-icon-udf" />
        </el-col>
        <el-col :span="23">
          <span class="file-name">{{ file.name }}</span>
          <i
            class="el-icon-close"
            @click="handleRemove(file, fileList)"
          />
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script lang='ts'>
/**
 * Component <UploadDialog>. Popup dialogue for upload file of datamove.
 * Used by <dataMove>.
 */
import { Component, Vue, Prop, Emit, Watch, Inject } from 'vue-property-decorator';
import _ from 'lodash';
import {
  ZetaUploadFileDetail
} from './utilities';
import { ElUploadInternalFileDetail } from 'element-ui/types/upload';
import DMRemoteService from '@/services/remote/DataMove';
@Component({
  components: {}
})
export default class UploadButton extends Vue{
  @Inject('dmRemoteService')
  dmRemoteService: DMRemoteService;
  @Prop()
  hasFile: boolean;
  readonly fileSize: number = 104857600; //File size cannot exceed 100M（104857600byte）
  uploadLoading = false;
  fileList: Array<ZetaUploadFileDetail>;
  form: {
    schema: Array<any>;
    path: string;
  };
  constructor() {
    super();
    this.fileList = [];
    this.form={
      schema: [],
      path: ''
    };
  }
  handleChange(fileMeta: ElUploadInternalFileDetail) {
    const contains = _.includes(this.fileList, fileMeta);
    if (!contains) {
      const file = new ZetaUploadFileDetail(fileMeta);
      file.buildReader(
        this.handleReadError
      );
      if(file.size > this.fileSize){
        this.fileList = [];
        this.$message.warning('File size cannot exceed 100M');
      }else{
        this.fileList.push(file);
      }
    }
  }
  handleRemove(file: any, fileList: any) {
    this.fileList = fileList.filter((item: any) => {
      return item.uid !== file.uid;
    });
    this.removeFile();
  }
  handleExceed(files: any, fileList: any) {
    const file = new ZetaUploadFileDetail(files[0], files[0]);
    const contains = _.includes(fileList, file);
    if (!contains) {
      file.buildReader(
        this.handleReadError
      );
      if(file.size > this.fileSize){
        this.$message.warning('File size cannot exceed 100M');
      }else{
        this.fileList = [];
        this.fileList.push(file);
      }
    }
    // this.$message.warning(`Limit 1 files`);
  }
  handleReadError(evt: any) {
    const error = evt.target.error;
    switch (error.code) {
      case error.NOT_FOUND_ERR:
        this.$message.warning('not found file');
        break;
      case error.NOT_READABLE_ERR:
        this.$message.warning('not readable');
        break;
      case error.ABORT_ERR:
        break;
      default:
        this.$message.warning('reader file error');
        break;
    }
  }
  mapToArray(schema: any){
    const arr: any = [];
    _.mapValues(schema,(key: any, val: any)=>{
      const len2 = (key==='decimal')?0:null;
      arr.push({name: _.trim(val), type: key, length1: this.setLen(key), length2: len2 });
    });
    return arr;
  }
  setLen(type: string){
    let len: number | null = null;
    switch(type){
      case 'varchar':
        len = 64;
        break;
      case 'char':
        len = 1;
        break;
      case 'decimal':
        len = 18;
        break;
      default:
        len = null;
    }
    return len;
  }
  submitUpload(){
    this.uploadLoading = true;
    this.dmRemoteService.upload(this.fileList).then(res => {
      if(res.data.code === 200){
        const path = res.data.content;
        this.dmRemoteService.getSchema(path).then(resp => {
          this.uploadLoading = false;
          this.form.schema = this.mapToArray(resp.data);
          this.form.path = path;
          this.showDiaolog(this.form);
        }).catch(e => {
          console.log(e);
          this.uploadLoading = false;
        });
      }else{
        this.uploadLoading = false;
        this.$message.warning(res.data.msg);
      }
    }).catch(e => {
      console.log(e);
      this.fileList = [];
      this.uploadLoading = false;
    });
  }
  emptyFileList() {
    this.fileList = [];
  }
  @Watch('fileList')
  handleUpload(){
    if(this.fileList.length>0){
      this.submitUpload();
    }
  }
  @Emit('show-schema-dialog')
  showDiaolog(form: any){

  }
  @Emit('remove-file')
  removeFile(){

  }
  @Watch('hasFile')
  handleFile(newVal: boolean, oldVal: boolean){
    if(!newVal){
      this.fileList = [];
    }
  }
}
</script>

<style lang='scss' scoped>
@import "@/styles/global.scss";
.upload-file {
  /deep/ .el-upload__tip {
    line-height: 30px;
    margin-top: 0;
    color: $zeta-font-light-color;
  }
}
.schema{
  margin-top: 10px;
  max-height: 200px;
  overflow: auto;
}
.file-list{
  :hover{
    background-color: #eee;
  }
  .el-row{
    padding-top: 5px;
    .el-col{
      display: flex;
      line-height: 20px;
      .file-name{
        display: inline-block;
        max-width: 360px;
        overflow:hidden;
        text-overflow:ellipsis;
        white-space:nowrap;
      }
      .el-icon-close{
        line-height: 20px;
        cursor: pointer;
        color: $zeta-global-color-red;
        margin-left: 6px;
        margin-top: 2px;
      }
    }
  }
}
</style>