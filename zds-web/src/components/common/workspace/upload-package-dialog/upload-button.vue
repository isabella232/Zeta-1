<template>
  <el-upload
    ref="upload"
    class="upload-file"
    :limit="1"
    :show-file-list="false"
    action
    accept="*"
    :file-list="fileList"
    :multiple="false"
    :auto-upload="false"
    :on-change="handleChange"
    :on-remove="handleRemove"
    :on-exceed="handleExceed"
  >
    <div class="upload">
      <el-button plain>
        Upload
      </el-button>
      <span
        slot="tip"
        class="el-upload__tip"
      >（Single file size cannot exceed 100M）</span>
    </div>
  </el-upload>
</template>

<script lang='ts'>
/**
 * Component <UploadPackageDialog>. Popup dialogue for upload file of HDFSList.
 * Used by <HDFSList>.
 */
import { Component, Vue, Watch } from 'vue-property-decorator';
import _ from 'lodash';
import {
  ZetaUploadFileDetail,
} from './utilities';
import { ElUploadInternalFileDetail } from 'element-ui/types/upload';
@Component({
  components: {},
})
export default class UploadButton extends Vue {
  readonly fileSize: number = 104857600; //File size cannot exceed 100M（104857600byte）
  fileList: Array<ZetaUploadFileDetail>;
  constructor () {
    super();
    this.fileList = [];
  }

  handleChange (fileMeta: ElUploadInternalFileDetail) {
    const contains = _.includes(this.fileList, fileMeta);
    if (!contains) {
      const file = new ZetaUploadFileDetail(fileMeta);
      file.buildReader(
        this.handleReadError
      );
      if (file.size > this.fileSize){
        this.fileList = [];
        this.$message.warning('File size cannot exceed 100M');
      } else {
        this.fileList.push(file);
      }
    }
  }
  handleRemove (file: any, fileList: any) {
    this.fileList = fileList.filter((item: any) => {
      return item.uid !== file.uid;
    });
  }
  handleExceed (files: any, fileList: any) {
    const file = new ZetaUploadFileDetail(files[0], files[0]);
    const contains = _.includes(fileList, file);
    if (!contains) {
      file.buildReader(
        this.handleReadError
      );
      if (file.size > this.fileSize){
        this.$message.warning('File size cannot exceed 100M');
      } else {
        this.fileList = [];
        this.fileList.push(file);
      }
    }
  }
  handleReadError (evt: any) {
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
  @Watch('fileList')
  handleFile (){
    if (this.fileList.length>0){
      this.$emit('show-upload-diaolog', this.fileList);
    }
  }
}
</script>

<style lang='scss' scoped>
@import "@/styles/global.scss";
  .upload-file {
    display: inline-block;
    margin-left: 10px;
    /deep/ .el-upload-list__item:first-child {
      margin-top: 5px;
    }
  }
  .el-upload__tip {
    line-height: 30px;
    margin-top: 0;
    color: $zeta-font-light-color;
  }

</style>
