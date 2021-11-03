<template>
  <el-dialog
    title="Upload Files"
    :visible.sync="visible_"
    :close-on-click-modal="false"
    width="800px"
    class="upload-dialog"
  >
    <el-form :model="form" ref="form" label-position="left" v-loading="uploadLoading">
      <el-row :gutter="10">
        <el-col :span="10">
          <el-form-item label="Local File" label-width="80px" prop="fileList">
            <el-upload
              class="upload-file"
              ref="upload"
              :limit="10"
              :show-file-list="false"
              action
              accept=".txt, .csv, .xml, .cfg, .dml, .sql, .lis"
              :file-list="form.fileList"
              :multiple="true"
              :auto-upload="false"
              :on-change="handleChange"
              :on-remove="handleRemove"
              :on-exceed="handleExceed"
            >
              <div class="upload">
                <el-button plain>File Upload</el-button>
                <!-- <span class="el-upload__tip" slot="tip">（Single File size cannot exceed 2M）</span> -->
              </div>
            </el-upload>
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="Folder" label-width="55px">
            <el-input v-model="form.folder"></el-input>
          </el-form-item>
        </el-col>
        <el-col :span="7">
          <el-form-item label="Interpreter" label-width="80px">
            <el-select v-model="form.interpreter" @change="handleDefaultChange">
              <el-option
                v-for="(option) in interpreterOptions"
                :key="option.val"
                :label="option.name"
                :value="option.val"
              ></el-option>
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="10">
        <el-col :span="24">
          <el-form-item label-width="80px">
            <div
              class="el-upload__tip"
            >Only allow（.txt/.csv/.xml/.cfg/.dml/.sql/.lis）files with a size less than 2M</div>
          </el-form-item>
        </el-col>
      </el-row>
      <div class="file-list" v-if="form.fileList.length>0">
        <!-- <el-row :gutter="10">
          <el-col :span="10">File Name</el-col>
          <el-col :span="6">Folder</el-col>
          <el-col :span="8">Interpreter</el-col>
        </el-row>-->
        <el-form-item v-for="(file,index) in form.fileList" :key="index">
          <el-row :gutter="10">
            <el-col :span="10">
              <el-form-item
                label="File Name"
                label-width="80px"
                :prop="'fileList.'+ index"
                :rules="rules[0]"
                :error="file.nameError"
              >
                <el-input v-model="file.name" :disabled="file.upload"></el-input>
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item
                label="Folder"
                label-width="55px"
                :prop="'fileList.'+ index"
                :rules="rules[1]"
              >
                <el-input v-model="file.path" :disabled="file.upload"></el-input>
              </el-form-item>
            </el-col>
            <el-col :span="7">
              <el-form-item label="Interpreter" label-width="80px" :prop="'fileList.'+ index">
                <el-select
                  v-model="file.interpreter"
                  :disabled="file.upload"
                >
                  <el-option
                    v-for="(option) in interpreterOptions"
                    :key="option.val"
                    :label="option.name"
                    :value="option.val"
                  ></el-option>
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="1" class="upload-status">
              <el-form-item label-width="10px" :prop="'fileList.'+ index">
                <i class="el-icon-upload-success el-icon-circle-check" v-if="file.upload"></i>
                <i class="el-icon-close" v-else @click="handleRemove(file, form.fileList)"></i>
              </el-form-item>
            </el-col>
          </el-row>
        </el-form-item>
      </div>
    </el-form>
    <div slot="footer" class="dialog-footer">
      <el-button
        type="primary"
        @click="upload"
        :loading="uploadLoading"
        :disabled="(form.fileList.length === 0)"
      >Upload</el-button>
      <el-button type="default" plain @click="visible_ = false">Cancel</el-button>
    </div>
  </el-dialog>
</template>

<script lang='ts'>
/**
 * Component <UploadDialog>. Popup dialogue for upload file of repolist.
 * Used by <RepoList>.
 */
import { Component, Vue, Prop, Watch, Inject } from "vue-property-decorator";
import {
  INotebook,
  CodeTypes,
  CodeType,
  IPreference,
  IConnection
} from "@/types/workspace";
import { WorkspaceSrv as NBSrv } from "@/services/Workspace.service";
import { RestPacket } from "@/types/RestPacket";
import Util from "@/services/Util.service";
import NotebookRemoteService from "@/services/remote/NotebookRemoteService";
import { IFile } from "@/types/repository";
import _ from "lodash";
import config from "@/config/config";
import { ZetaException } from "@/types/exception";
import { debug } from "util";
import { connect } from "echarts";
import {
  getInterpreterOptions,
  parseFolder,
  ZetaUploadFileDetail,
  generateNotebook,
  generateRestFile,
  genereteIFile,
  InterpreterOptions,
  MultiNotebookFrom
} from "./utilities";
import { ElUploadInternalFileDetail } from "element-ui/types/upload";
@Component({
  components: {}
})
export default class UploadDialog extends Vue {
  @Inject() notebookRemoteService: NotebookRemoteService;
  @Prop() visible: boolean;
  @Prop() folder?: string;
  private readStatus: boolean = false;
  private percent: number = 0;
  readonly fileSize: number = 2097152; //File size cannot exceed 2M（2097152byte）

  set visible_(e) {
    this.$emit("update:visible", e);
  }
  get visible_(): boolean {
    return this.visible;
  }
  get interpreterOptions(): InterpreterOptions[] {
    return getInterpreterOptions("single");
  }
  form: MultiNotebookFrom
  rules: {};

  uploadLoading = false;

  constructor() {
    super();
    this.form = {
      folder: this.folder ? parseFolder(this.folder) : "/",
      fileList: [],
      interpreter: CodeType.SQL,
      nbType: "single"
    };
    this.rules = [
      {
        validator: (r: any, f: any, cb: Function) => {
          if (!_.trim(f.name)) cb(new Error("please input notebook name"));
          else if (/[^a-zA-Z\-\.0-9_\s]/.test(f.name))
            cb(
              new Error(
                "File name not valid. Only alphanumeric, _, -, . are allowed."
              )
            );
          else cb();
        },
        trigger: "blur"
      },
      {
        validator: (r: any, f: any, cb: Function) => {
          if (/[^a-zA-Z\/\-\.0-9_]/.test(f.path))
            cb(
              new Error(
                "Folder name not valid. Only alphanumeric, _, -, . are allowed."
              )
            );
          else cb();
        },
        trigger: "blur"
      }
    ];
  }
  handleDefaultChange(value: string) {
    this.form.fileList.map((item: any, index: number) => {
      item.interpreter = value;
    });
  }
  handleChange(fileMeta: ElUploadInternalFileDetail) {
    const contains = _.includes(this.form.fileList, fileMeta);
    if (!contains) {
      let file = new ZetaUploadFileDetail(fileMeta, this.form);
      file.buildReader(
        this.handleReadError,
        this.handleReadStart,
        this.updateProgress
      );
      this.form.fileList.push(file);
    }
  }
  handleRemove(file: any, fileList: any) {
    this.form.fileList = fileList.filter((item: any) => {
      return item.uid !== file.uid;
    });
  }
  handleExceed(files: any, fileList: any) {
    this.$message.warning(`Limit 10 files`);
  }
  handleReadStart() {
    this.readStatus = true;
    this.percent = 0;
  }
  handleReadError(evt: any) {
    let error = evt.target.error;
    switch (error.code) {
      case error.NOT_FOUND_ERR:
        console.log("not found file");
        break;
      case error.NOT_READABLE_ERR:
        console.log("not readable");
        break;
      case error.ABORT_ERR:
        break;
      default:
        console.log("reader file error");
        break;
    }
  }
  updateProgress(evt: any) {
    if (evt.lengthComputable) {
      this.percent = Math.round((evt.loaded / evt.total) * 100);
    }
  }
  upload() {
    let valid: boolean = false;
    (this.$refs["form"] as any).validate((valid_: boolean) => {
      valid = valid_;
    });
    if (!valid) {
      console.log("Invalid Upload folder/name");
      return;
    }
    let list = this.form.fileList.filter(item => {
      return item.upload !== true;
    });
    if (list.length === 0) {
      this.$message.warning("All uploaded succeed");
      this.visible_ = false;
      return;
    }
    this.uploadLoading = true;
    let uploadRequests = _.chain(this.form.fileList)
      .map(file => {
        if (file.upload === true) {
          return;
        } else {
          return this.uploadSingleFile(file);
        }
      })
      .compact()
      .value();
    Promise.all(uploadRequests).then(() => {
      this.visible_ = false;
      this.uploadLoading = false;
      this.$message({
        type: "success",
        message: "Successfully upload notebook on server.",
        customClass: "crt-success-message"
      });
    }).catch(() => {
      this.uploadLoading = false;
    });
  }
  private uploadSingleFile(file: ZetaUploadFileDetail): Promise<any> {
    file.nameError = "";
    // get current seq
    const seq = -1;
    const preference = file.preference;
    let nb_rest: RestPacket.File = file.toFile();
    let nb_file: IFile = file.toIFile();
    return this.notebookRemoteService
      .add(nb_rest)
      .then(({ data }) => {
        /* Server returns a new notebookId */
        file.upload = true;
        const nid = data.id;
        nb_file.notebookId = nid;
        nb_file.preference = data.preference
          ? JSON.parse(data.preference)
          : undefined;
        this.$store.dispatch("addFile", nb_file);
      })
      .catch((e: ZetaException) => {
        console.log("Upload notebook fail.", e);
        file.upload = false;
        if (e.code === "EXIST_NOTEBOOK") {
          e.resolve();
          file.nameError = e.message;
        } else if (e.code === "INVALID_INPUT") {
          e.resolve();
          file.nameError = e.message;
		}
		throw e
      });
  }
  @Watch("visible")
  onChangeVisible(newVal: boolean, oldVal: boolean) {
    if (newVal) {
      this.readStatus = false;
      this.percent = 0;
      this.form = {
        fileList: [],
        nbType: "single",
        interpreter: CodeType.SQL,
        folder: this.folder ? parseFolder(this.folder) : "/"
      };
    }
  }
  @Watch("form.folder")
  onchangeFolder(newVal: string, oldVal: string) {
    this.form.fileList.map(file => {
      file.path = newVal;
    });
  }
  @Watch("folder")
  onFolderChange(val: string) {
    this.form.folder = parseFolder(val);
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
  .comment {
    color: #999;
    font-size: 12px;
    line-height: 30px;
  }
}
</style>