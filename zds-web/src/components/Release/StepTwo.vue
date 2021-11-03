<template>
  <el-form label-position="top" :rules="rule" :model="form" ref="form">
    <div class="step-title">
      <p>
        <span>Release - Push To Github</span>
      </p>
    </div>
    <el-form-item label="Project:" prop="project">
      <el-select v-model="form.project" @change="projectChange">
        <el-option
          v-for="(option,$index) in projectOptions"
          :key="$index"
          :label="option"
          :value="option"
        ></el-option>
      </el-select>
    </el-form-item>
    <!-- Temporarily hide
      <el-form-item label="Branch:" prop="branch">
      <el-select v-model="form.branch">
        <el-input style="margin: 0 20px;" v-model="form.branch" placeholder></el-input>
        <el-option
          v-for="(option,$index) in branchOptions"
          :key="$index"
          :label="option"
          :value="option"
        ></el-option>
      </el-select>
    </el-form-item>-->
    <el-form-item label="Tag:" prop="tag">
      <el-input v-model="form.tag" @input="recordPushInfo"></el-input>
    </el-form-item>
    <el-form-item label="Commit Message:" prop="message">
      <el-input
        type="textarea"
        :rows="5"
        placeholder="Please enter your message"
        v-model="form.message"
        @input="recordPushInfo"
      ></el-input>
    </el-form-item>
    <el-button type="primary" @click="onPublish" :disabled="publishDisabled">Publish</el-button>
    <el-tag v-show="pushResult" :type="pushResultType">{{pushMessage}}</el-tag>
    <push-dialog
      :visible.sync="pushDialogVisble"
      :data="pushDialogCheckResult"
      @close-dialog="closeDialog"
    />
  </el-form>
</template>

<script lang="ts">
import { Component, Vue, Inject } from "vue-property-decorator";
import PushDialog from "@/components/Release/PushDialog.vue";
import GitRemoteService from '@/services/remote/GithubService'
import Util from "@/services/Util.service";
import _ from "lodash";
@Component({
  components: {
    PushDialog
  }
})
export default class StepTwo extends Vue {
  @Inject('gitRemoteService')
  gitRemoteService: GitRemoteService

  form: {
    project: string;
    branch: string;
    tag: string;
    message: string;
  };

  rule: {
    project?: Array<any>;
    branch?: Array<any>;
    tag: Array<any>;
  };

  nt: string = Util.getNt();
  prefixUrl: string = "https://github.corp.ebay.com/APD/";
  projectOptions = JSON.parse(this.$store.getters.getProjectOptions) || [];
  branchOptions = [];
  pushResult: boolean = false;
  pushResultType: string = "";
  pushMessage: string = "";
  pushDialogVisble: boolean = false;
  pushDialogCheckResult = {};

  constructor() {
    super();
    this.form = {
      project: this.$store.getters.getPushTofile["project"] || this.projectOptions.length > 0 ? this.projectOptions[0] : "",
      branch: "master",
      tag: this.$store.getters.getPushTofile["tag"] || "",
      message: this.$store.getters.getPushTofile["message"] || ""
    };
    this.rule = {
      project: [
        {
          required: true,
          message: "please select Project",
          trigger: "blur"
        }
      ],
      branch: [
        {
          required: true,
          message: "please select Branch",
          trigger: "blur"
        }
      ],
      tag: [
        {
          required: true,
          message: "please input Tag",
          trigger: "blur"
        }
      ]
    };
  }

  get publishDisabled() {
    return (
      _.isEmpty(this.form.project) ||
      //_.isEmpty(this.form.branch) ||
      _.isEmpty(this.form.tag)
    );
  }

  // fetch branch info when change selected-project
  projectChange() {
    this.recordPushInfo();
    /*const url: string = this.prefixUrl + this.form.project;
    const promise: Promise<any> = GitRemote.fetchBranches(
      this.nt,
      url
    ) as Promise<any>;
    promise
      .then(res => {
        if (res && res.data && res.data != null) {
          this.branchOptions = res.data;
        }
        console.log("Call Api:fetchBranches successed");
      })
      .catch(err => {
        console.log("Call Api:fetchBranches failed: " + JSON.stringify(err));
      });*/
  }

  recordPushInfo() {
    let pushTofile: any = this.$store.getters.getPushTofile || {};
    pushTofile.project = this.form.project;
    pushTofile.branch = this.form.branch;
    pushTofile.tag = this.form.tag;
    pushTofile.message = this.form.message;
    this.$store.dispatch("setPushTofile", pushTofile);
    this.$store.dispatch("setGithubPushStatus", false);
  }

  initReleaseUrl(tag: string): string {
    const reg = new RegExp("[0-9]+");
    let releaseUrl: string = "";
    if (!_.isEmpty(tag)) {
      releaseUrl = "https://jirap.corp.ebay.com/browse/DWRM-" + reg.exec(tag);
    }

    return releaseUrl;
  }

  // 1. create branch if it`s a new branch
  // 2. file check in github
  // 3. push code
  onPublish() {
    this.$store.dispatch("setReleaseSubmitLoading", true);
    /* create branch
    if (_.indexOf(this.branchOptions, this.form.branch) < 0) {
      const promise: Promise<any> = this.gitCreateBranch();
      promise
        .then(res => {
          // update branch select options
          if (res && res.data && res.data != null) {
            this.branchOptions = res.data;
          }
          this.checkGitFileExists();
          console.log("Call Api:fetchBranches successed");
        })
        .catch(err => {
          this.$store.dispatch("setReleaseSubmitLoading", false);
          if (
            !_.isUndefined(err.response.data) &&
            !_.isUndefined(err.response.data.status) &&
            _.isEqual(err.response.data.status, "008")
          ) {
            this.$message.error(err.response.data.message);
          } else {
            this.$message.error(err.response.data.error);
          }
        });
    }else {
      this.checkGitFileExists();
    }*/

    this.checkGitFileExists();
  }

  gitCreateBranch(): Promise<any> {
    const url: string = this.prefixUrl + this.form.project;
    const branch: string = "master";
    const newBranch: string = this.form.branch;
    let files:any[] = [];
    _.forEach(this.$store.getters.getReleaseFile, o =>
      files.push({ fullPath: o["pathTitle"], sha: o["sha"] })
    );
    return this.gitRemoteService.createBranch(this.nt, url, branch, newBranch) as Promise<
      any
    >;
  }

  checkGitFileExists() {
    const url: string = this.prefixUrl + this.form.project;
    const branch: string = this.form.branch;
    const files = this.$store.getters.getReleaseFile;

    const promise: Promise<any> = this.gitRemoteService.checkGitFileExists(
      this.nt,
      url,
      branch,
      files
    ) as Promise<any>;
    const vm = this;
    promise
      .then(res => {
        if (res && res.data && res.data != null) {
          console.debug("Call Api:checkGitFileExists successed");
          if (_.isEmpty(res.data)) {
            this.gitPush();
          } else {
            this.pushDialogCheckResult = res.data;
            this.pushDialogVisble = true;
          }
        }
      })
      .catch(err => {
        err.resolve();
        this.$store.dispatch("setReleaseSubmitLoading", false);
        console.error(
          "Call Api:checkGitFileExists failed: " + JSON.stringify(err)
        );
      });
  }

  changeStepThreeCache() {
    let manifestInfo: any = this.$store.getters.getValidateJira || {};
    manifestInfo.title = this.form.tag;
    manifestInfo.project = this.form.project;
    manifestInfo.tag = this.form.tag;
    manifestInfo.ecrUrl = this.initReleaseUrl(this.form.tag);
    manifestInfo.type = [];
    manifestInfo.prodTarget = false;
    manifestInfo.qaTarget = false;
    manifestInfo.mavenUrl = "";
    this.$store.dispatch("setValidateJira", manifestInfo);
  }

  gitPush() {
    const url: string = this.prefixUrl + this.form.project;
    const tag = this.form.tag;
    const commitMessage: string = this.form.message;
    const files = this.$store.getters.getReleaseFile;

    const promise: Promise<any> = this.gitRemoteService.push(
      this.nt,
      url,
      commitMessage,
      files,
      undefined,
      undefined,
      tag,
      {timeout: 1000 * 60}
    ) as Promise<any>;
    promise
      .then(res => {
        this.$store.dispatch("setReleaseSubmitLoading", false);
        console.debug("Call Api:push successed");
        this.changeStepThreeCache();
        this.pushResult = true;
        this.pushResultType = "success";
        this.pushMessage = "Success";
        this.$store.dispatch("setGithubPushStatus", true);
      })
      .catch(err => {
        this.$store.dispatch("setReleaseSubmitLoading", false);
        this.pushResult = true;
        this.pushResultType = "danger";
        this.pushMessage = "Fail";
        if (!_.isUndefined(err.response) && !_.isUndefined(err.response.data) && err.response.data.status == "008") {
          this.pushMessage = !_.isUndefined(err.response.data.message) && !_.isEmpty(err.response.data.message) ? err.response.data.message : "Fail";
        }
        this.$store.dispatch("setGithubPushStatus", false);
        console.error("Call Api:push failed: " + JSON.stringify(err));
      });
  }

  closeDialog(override: boolean) {
    this.$store.dispatch("setReleaseSubmitLoading", override);
    if (override) {
      this.gitPush();
    }
  }
}
</script>
<style lang="scss" scoped>
.el-input {
  width: 188px;
}
.el-button {
  width: 90px;
  height: 30px;
  background: inherit;
  background-color: rgba(255, 255, 255, 0);
  border-width: 1px;
  border-style: solid;
  border-color: rgba(86, 156, 225, 1);
  border-radius: 4px;
  box-shadow: none;
  font-size: 14px;
  color: #569ce1;
  margin-bottom: 22px;
}
.step-title {
  font-weight: 700;
  font-style: normal;
  font-size: 24px;
  margin: 22px 0;
}
.el-form-item {
  /deep/ .el-form-item__label {
    padding-bottom: 0px;
    line-height: 20px;
  }
}
</style>
