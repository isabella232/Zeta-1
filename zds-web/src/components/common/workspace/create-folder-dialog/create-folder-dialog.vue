<template>
  <el-dialog title="Create Folder" :visible.sync="visible_" @close="close" width="520px">
    <el-form :rules="rules" :model="form" ref="form" label-width="130px" label-position="left">
      <el-form-item label="Folder Name" prop="name">
        <el-input v-model="form.name" />
      </el-form-item>
    </el-form>
    <div slot="footer" class="dialog-footer">
      <el-button plain @click="close">Cancel</el-button>
      <el-button v-loading="loading" type="primary" @click="confirm" >Confirm</el-button>
    </div>
  </el-dialog>
</template>


<script lang="ts">
import { Component, Vue, Prop, Watch, Inject } from "vue-property-decorator";
import { Form } from "element-ui";
import { ZetaException } from "@/types/exception";
import { IHDFSFile } from "@/types/workspace";
import HDFSRemoteService from "@/services/remote/HDFSRemoteService";
import _ from "lodash";

interface RenamePackageOption {
  name: string;
}

@Component({
  components: {}
})
export default class CreateFolderDialog extends Vue {
  @Inject("HDFSRemoteService") HDFSRemoteService: HDFSRemoteService;
  @Prop() visible: boolean;
  public form: RenamePackageOption;
  public loading = false;
  constructor() {
    super();
    this.form = {
      name: ""
    };
  }
  get cluster(){
    return this.$store.getters.hdfsCluster;
  }
  get folder(){
    return this.$store.getters.folder;
  }
  public validateName = (r: any, value: any, cb: Function) => {
    if (/[^a-zA-Z\/\-0-9_]/.test(value))
      cb(
        new Error(
          "Folder name not valid. Only alphanumeric,/, _, -,are allowed."
        )
      );
    else cb();
  };
  public rules = {
    name: [
      {
        validator: this.validateName,
        trigger: "blur"
      }
    ]
  };
  set visible_(e) {
    this.$emit("update:visible", e);
  }
  get visible_(): boolean {
    return this.visible;
  }
  close() {
    console.log("close");
    this.visible_ = false;
  }
  confirm() {
    (this.$refs["form"] as Form).validate(valid => {
      if (valid) {
        this.loading = true;
        const name = this.folder+this.form.name;
        this.HDFSRemoteService.create(
          this.cluster,
          name
        )
          .then(res => {
            this.loading = false;
            const data = res.data;
            if (data.code === 302) {
              this.$message({
                type: "warning",
                message: "folder is exist",
                customClass: "create-fail-message"
              });
            } else {
              this.visible_ = false;
              this.$store.dispatch("addFolder",name);
              this.$message({
                type: "success",
                message: "Successfully create folder.",
                customClass: "create-success-message"
              });
            }
          })
          .catch((e: ZetaException) => {
            this.loading = false;
            console.error(e);
            e.message = "Fail to rename. See console for detail";
          });
      }
    });
  }
  @Watch("visible")
  handleVisible(newVal: boolean) {
    if (newVal) {
      this.form = {
        name: ""
      };
    }
  }
}
</script>