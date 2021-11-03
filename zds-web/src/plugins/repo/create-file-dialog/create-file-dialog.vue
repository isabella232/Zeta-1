<template>
  <el-dialog
    label-width="80px"
    title="Create Notebook"
    :visible.sync="visible"
    :close-on-click-modal="false"
    width="720px"
    class="crt-notebook-dialog"
  >
    <el-form :rules="rule" label-position="left" label-width="130px" :model="form" ref="form">
      <el-form-item label="Notebook Name" prop="name" :error="nameError">
        <el-input id="newNBName" v-model="form.name"></el-input>
      </el-form-item>
      <el-form-item label="Folder" prop="folder">
        <el-input v-model="form.folder"></el-input>
      </el-form-item>
      <el-form-item label="Notebook Type">
        <el-radio-group v-model="form.nbType">
          <el-radio :label="'single'">Default Notebook</el-radio>
          <el-radio :label="'collection'">
            Spark Python
            <i class="beta">(BETA)</i>
          </el-radio>
          <el-radio :label="'zeppelin'">
            Zeppelin Notebook
          <i class="beta">(BETA)</i>
          </el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="Interpreter" prop="interpreter">
        <el-radio-group v-model="form.interpreter">
          <el-radio
            v-for="(option,$index) in interpreterOptions"
            :key="$index"
            :label="option.val"
          >{{option.name}}</el-radio>
        </el-radio-group>
        <div
          v-if="form.interpreter === 'SQL'||form.interpreter ==='TERADATA'||form.interpreter === 'HIVE'"
        >
          <div v-for="(option,$index) in interpreterOptions" :key="$index" class="comment">
            <div v-if="option.val === form.interpreter">
              Available platform:
              <span v-for="(cluster,key,i) in option.clusters" :key="i">
                <span>{{cluster}}</span>
                <span v-if="i !== Object.keys(option.clusters).length-1">，</span>
              </span>
            </div>
          </div>
        </div>
      </el-form-item>
    </el-form>
    <div slot="footer" class="dialog-footer">
      <el-button type="primary" @click="create" :loading="crtLoading">Create</el-button>
      <el-button type="default" plain @click="close">Cancel</el-button>
    </div>
  </el-dialog>
</template>

<script lang="ts">
/**
 * Component <NewNotebookDialogue>. Popup dialogue for creation of new notebook.
 * Used by <NotebookTabs> and <RepoList>.
 */
import { Component, Vue, Prop, Watch, Inject, Emit } from "vue-property-decorator";
import {
  INotebook,
  CodeTypes,
  CodeType,
  IPreference,
  IConnection,
  NotebookType,
  ZPLNote,
  WorkSpaceType,
  ITools,
  IWorkspace
} from "@/types/workspace";
import {
  WorkspaceSrv as NBSrv,
  WorkspaceSrv
} from "@/services/Workspace.service";
import { RestPacket } from "@/types/RestPacket";
import Util from "@/services/Util.service";
import NotebookRemoteService from "@/services/remote/NotebookRemoteService";
import { IFile } from "@/types/repository";
import _ from "lodash";
import axios from "axios";
import config from "@/config/config";
import { ZetaException } from "@/types/exception";
import moment from "moment";
import RepoPluginBase from "../plugin-base";
import { InterpreterOptions, ISingleNotebookForm } from '.';
import { getInterpreterOptions, genereteIFile } from "./utilities";
@Component({
  components: {}
})
export default class NewNotebookDialog extends RepoPluginBase {
  visible: boolean = true;
  crtLoading = false;
  form: ISingleNotebookForm = {
      name: "Untitled",
      folder: "/",
      interpreter: CodeType.SQL,
      nbType: "single",
      seq: -1
  };
  nameError: string = "";
  readonly rule = {
      name: [
        {
          // required: true,
          message: "please input notebook name",
          trigger: "blur"
        },
        {
          validator: (r: any, f: string, cb: Function) => {
            if (/[^a-zA-Z\-\.0-9_]/.test(f))
              cb(
                new Error(
                  "File name not valid. Only alphanumeric, _, -, . are allowed."
                )
              );
            else cb();
          }
        }
      ],
      folder: [
        {
          validator: (r: any, f: string, cb: Function) => {
            if (/[^a-zA-Z\/\-\.0-9_]/.test(f))
              cb(
                new Error(
                  "Folder name not valid. Only alphanumeric, _, -, . are allowed."
                )
              );
            else cb();
          }
        }
      ]
    };;
  
  get interpreterOptions(): InterpreterOptions[] {
    return getInterpreterOptions(this.form.nbType);
  }

  @Watch("form.nbType")
  onNbTypeChange(newVal: NotebookType) {
    if (newVal == "single") {
      this.form.interpreter = CodeType.SQL;
    } else if (newVal == 'collection'){
      this.form.interpreter = CodeType.SPARK_PYTHON;
    }
  }
  create() {
    let valid: boolean = false;
    (this.$refs["form"] as any).validate((valid_: boolean) => {
      valid = valid_;
    });
    if (!valid) {
      console.warn("Invalid notebook folder/name");
      return;
    }
    this.crtLoading = true;
    
    let file = genereteIFile({ ...this.form });
    this.repoApi.createNotebook(file).then((newFile) => {
        this.crtLoading = false;
        this.visible = false;
        this.onSuccess(newFile)
    }).catch((e: ZetaException) => {
        if (e.code === "EXIST_NOTEBOOK") {
            this.crtLoading = false;
            this.visible = false;
            e.resolve();
            this.nameError = e.message;
        } else {
            this.onError(e);
        }
    })
  }

  close() {
      this.onCancel();
  }

}
</script>

<style lang="scss" scoped>
.crt-notebook-dialog {
  /deep/ .interpreter-select {
    width: 100%;
    // /deep/ .el-input{
    // }
  }
  /deep/ .el-form {
    .el-form-item {
      margin-bottom: 0;
      &.is-error {
        margin-bottom: 10px;
      }
    }
  }
  .comment {
    color: #999;
    font-size: 12px;
    line-height: 30px;
  }
  .beta {
    font-size: 10px;
    font-style: normal;
    display: inline-block;
    margin-left: 3px;
    vertical-align: middle;
    color: rgb(240, 123, 14)
  }
}
</style>