<template>
  <el-form label-position="top" :rules="rule" :model="form" ref="form">
    <div class="step-title">
      <p>
        <span>Release - Validate Jira Status</span>
      </p>
    </div>
    <el-form-item label="Title:" prop="title">
      <el-input v-model="form.title" placeholder="Describe this release" @blur="recordJiraInfo"></el-input>
    </el-form-item>
    <el-form-item label="Release type:">
      <el-button-group>
        <el-button v-show="groupFlag" type="primary">Abinitio</el-button>
        <el-button plain @click="changeReleaseType"></el-button>
        <el-button v-show="!groupFlag" type="primary">Maven</el-button>
      </el-button-group>
    </el-form-item>
    <el-form-item v-show="groupFlag" label="Project:" prop="project">
      <el-select v-model="form.project" @change="recordJiraInfo">
        <el-option
          v-for="(option,$index) in projectOptions"
          :key="$index"
          :label="option"
          :value="option"
        ></el-option>
      </el-select>
    </el-form-item>
    <el-form-item v-show="!groupFlag" label="Organization:" prop="org">
      <el-input v-model="form.org"></el-input>
    </el-form-item>
    <el-form-item v-show="!groupFlag" label="Maven Porject Name:" prop="mavenName">
      <el-input v-model="form.mavenName"></el-input>
    </el-form-item>
    <el-form-item label="Label:" prop="label">
      <el-input v-model="form.label" placeholder="DWRM1001" @input="labelChange"></el-input>
    </el-form-item>
    <el-form-item label="Release Ticket URL:" prop="releaseUrl">
      <el-input v-model="form.releaseUrl" @blur="recordJiraInfo" @input="initReleaseAllStatus"></el-input>
    </el-form-item>
    <el-form-item label prop="type">
      <el-checkbox-group v-model="form.type" @change="recordJiraInfo">
        <el-checkbox label="Production" name="type"></el-checkbox>
        <el-checkbox label="QA" name="type"></el-checkbox>
      </el-checkbox-group>
    </el-form-item>
    <div>
      <el-button
        class="validate-btn"
        type="primary"
        @click="onValidate"
        :disabled="validateDisabled"
      >Validate</el-button>
      <el-tag v-show="validateResult" :type="validateResultType">{{validateMessage}}</el-tag>
    </div>
  </el-form>
</template>

<script lang="ts">
import { Component, Vue, Inject } from "vue-property-decorator";
import Util from "@/services/Util.service";
import _ from "lodash";
import JIRARemoteService from "@/services/remote/JIRARemoteService";
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import { ZetaException, ZetaExceptionProps, ZETA_EXCEPTION_TAG } from "@/types/exception";

@Component({
  components: {}
})
export default class StepThree extends Vue {
  @Inject()
  notebookRemoteService: NotebookRemoteService;
  @Inject('jiraRemoteService')
  jiraRemoteService: JIRARemoteService

  form: {
    title: string;
    project: string;
    org: string;
    mavenName: string;
    label: string;
    releaseUrl: string;
    type: Array<string>;
  };

  rule: {
    title: Array<any>;
    label: Array<any>;
    releaseUrl: Array<any>;
    type: Array<any>;
  };

  groupFlag: boolean = true;
  projectOptions = JSON.parse(this.$store.getters.getProjectOptions) || [];
  validateResult: boolean = false;
  validateResultType: string = "";
  validateMessage: string = "";
  constructor() {
    super();
    const validateJira: any = this.$store.getters.getValidateJira;
    const title: string = !_.isEmpty(validateJira["title"]) ? validateJira["title"] : "";
    const project: string = !_.isEmpty(validateJira["project"]) ? validateJira["project"] : this.projectOptions.length > 0
      ? this.projectOptions[0]
      : "";
    const label: string = !_.isEmpty(validateJira["tag"]) ? validateJira["tag"] : "";
    const releaseUrl: string = !_.isEmpty(validateJira["ecrUrl"]) ? validateJira["ecrUrl"] : "";

    this.form = {
      title: title,
      project: project,
      org: "",
      mavenName: "",
      label: label,
      releaseUrl: releaseUrl,
      type: validateJira["type"] || []
    };
    this.rule = {
      title: [
        {
          required: true,
          message: "please input Title",
          trigger: "blur"
        }
      ],
      label: [
        {
          required: true,
          message: "please input Label",
          trigger: "blur"
        },
        {
          validator: (r: any, f: string, cb: Function) => {
            const reg = new RegExp("[0-9]+");
            if (reg.exec(this.form.label) === null || typeof reg.exec(this.form.label) === "undefined")
              cb(
                new Error(
                  "The release label must include jira number."
                )
              );
            else cb();
          }
        }
      ],
      releaseUrl: [
        {
          required: true,
          message: "please input Release Ticket URL",
          trigger: "blur"
        }
      ],
      type: [
        {
          required: true,
          message: "please select Environment",
          trigger: "change"
        }
      ]
    };
  }

  initReleaseUrl(tag: string): string {
    const reg = new RegExp("[0-9]+");
    let releaseUrl: string = "";
    if (!_.isEmpty(tag)) {
      releaseUrl = "https://jirap.corp.ebay.com/browse/DWRM-" + reg.exec(tag);
    }

    return releaseUrl;
  }

  changeReleaseType() {
    //this.groupFlag = !this.groupFlag;
  }

  get validateDisabled() {
    return (
      _.isEmpty(this.form.title) ||
      _.isEmpty(this.form.project) ||
      _.isEmpty(this.form.label) ||
      _.isEmpty(this.form.releaseUrl) ||
      _.isEmpty(this.form.type)
    );
  }

  // step1. check jira
  // step2. submit manifest
  // step3. check manifest generated
  onValidate() {
    const reg1 = new RegExp("[0-9]+");
    if (reg1.exec(this.form.label) === null ||
      typeof reg1.exec(this.form.label) === "undefined") {
      this.$message.info("Please check your release label typo");
    }

    let valid: boolean = false;
    (this.$refs["form"] as any).validate((valid_: boolean) => (valid = valid_));
    if (!valid) {
      return;
    }

    this.$store.dispatch("setReleaseSubmitLoading", true);
    // except production, skip check jira error for test
    if (process.env.VUE_APP_ENV == 'production') {
      this.checkJiraStatus()
    }else {
      this.jiraRemoteService.getReleaseCheckStatus(Util.getNt(), this.form.label, "DWRM-" + reg1.exec(this.form.label)).then(res => {
        console.debug("Call Api:check jira successed");
        if (res && res.data && res.data != null) {
          console.debug("check jira response: " + JSON.stringify(res.data));
        }
      }).catch(err => {
        console.error("Call Api:check jira failed: " + JSON.stringify(err));
      });
      let manifestInfo: any = this.$store.getters.getValidateJira || {};
      manifestInfo.ticketName = "DWRM-" + reg1.exec(this.form.label);
      this.$store.dispatch("setValidateJira", manifestInfo);
      this.submitManifest();
    }
  }

  checkJiraStatus() {
    const reg1 = new RegExp("[0-9]+");
    this.jiraRemoteService.getReleaseCheckStatus(Util.getNt(), this.form.label, "DWRM-" + reg1.exec(this.form.label)).then(res => {
      console.debug("Call Api:check jira successed");
      if (res && res.data && res.data != null &&
        _.lowerCase(res.data["isOkRelease"]).indexOf("success") > -1) {
        let manifestInfo: any = this.$store.getters.getValidateJira || {};
        manifestInfo.ticketName = res.data["ticketName"];
        this.$store.dispatch("setValidateJira", manifestInfo);
        this.submitManifest();
      } else {
        let validateMessage = "Check Jira Fail";
        if (!_.isUndefined(res.data["isOkRelease"])) {
          validateMessage = res.data["isOkRelease"];
        } else {
          try {
            validateMessage = JSON.parse(res.data.errorMessages)["errorMessages"][0];
          } catch (e) {
            console.error(e);
          }
        }

        this.validateResult = true;
        this.validateResultType = "danger";
        this.validateMessage = validateMessage;
        this.$store.dispatch("setJiraStatus", false);
        this.$store.dispatch("setReleaseSubmitLoading", false);
      }
    }, function (err : any) {
      err.resolve();
      throw err;
    })
    .catch(err => {
      let validateMessage = "Check Jira Fail";
      if (err && err.response && err.response.data && err.response.data.isOkRelease) {
        validateMessage = err.response.data["isOkRelease"];
      } else if (err && err.response && err.response.data && err.response.data.errorMessages){
        try {
          validateMessage = JSON.parse(err.response.data.errorMessages)["errorMessages"][0];
        } catch (e) {
          console.error(e);
        }
      }
      this.validateResult = true;
      this.validateResultType = "danger";
      this.validateMessage = validateMessage;
      this.$store.dispatch("setJiraStatus", false);
      this.$store.dispatch("setReleaseSubmitLoading", false);
      console.error("Call Api:check jira failed: " + JSON.stringify(err));
    });
  }

  submitManifest() {
    const manifest: any = this.$store.getters.getValidateJira || {};
    const param: any = { manifest: manifest, username: Util.getNt() };
    this.notebookRemoteService
      .submitManifest(param)
      .then(res => {
        console.debug("Call Api:submit manifest successed");
        if (res && res.data && res.data == "Generate manifest submit successfully.") {
          this.checkManifest();
        }else {
          this.validateResult = true;
          this.validateResultType = "danger";
          this.validateMessage = "Manifest Create Fail";
          this.$store.dispatch("setJiraStatus", false);
          this.$store.dispatch("setReleaseSubmitLoading", false);
        }
      }, function (err : any) {
        err.resolve();
        throw err;
      })
      .catch(err => {
        this.validateResult = true;
        this.validateResultType = "danger";
        this.validateMessage = !_.isUndefined(err.response) && !_.isUndefined(err.response.data) ? err.response.data : "Submit Manifest Fail";
        this.$store.dispatch("setJiraStatus", false);
        this.$store.dispatch("setReleaseSubmitLoading", false);
        console.error("Call Api:submit manifest failed: " + JSON.stringify(err));
      });
  }

  checkManifest() {
    const manifest: any = this.$store.getters.getValidateJira || {};
    this.jiraRemoteService.checkManifest(manifest.tag || "")
      .then(res => {
        console.debug("Call Api:check manifest generated successed");
        if (res && res.data && res.data != null &&
            res.data.status != undefined && (res.data.status.value == "done" || res.data.status.value == "manifest")) {
          this.validateResult = true;
          this.validateResultType = "success";
          this.validateMessage = "Success";
          this.$store.dispatch("setJiraStatus", true);
          this.$store.dispatch("setReleaseSubmitLoading", false);
        }else if (res.data.status.value == "failed") {
          this.validateResult = true;
          this.validateResultType = "danger";
          this.validateMessage = !_.isUndefined(res.data.failMsg) && !_.isEmpty(res.data.failMsg) ? res.data.failMsg : "Manifest Not Generated";
          this.$store.dispatch("setJiraStatus", false);
          this.$store.dispatch("setReleaseSubmitLoading", false);
        }else {
          this.checkManifest();
        }
      }, function (err : any) {
        err.resolve();
        throw err;
      })
      .catch(err => {
        this.validateResult = true;
        this.validateResultType = "danger";
        this.validateMessage = "Manifest Not Generated";
        this.$store.dispatch("setJiraStatus", false);
        this.$store.dispatch("setReleaseSubmitLoading", false);
        console.error(
          "Call Api:check manifest generated failed: " + JSON.stringify(err)
        );
      });
  }

  labelChange() {
    this.form.releaseUrl = this.initReleaseUrl(this.form.label);
    this.recordJiraInfo();
  }

  initReleaseAllStatus() {
    this.$store.dispatch("setJiraStatus", false);
    this.$store.dispatch("setSendManifestValidateStatus", false);
    this.$store.dispatch("setRolloutStatus", false);
  }

  recordJiraInfo() {
    let manifestInfo: any = this.$store.getters.getValidateJira || {};
    manifestInfo.title = this.form.title;
    manifestInfo.project = this.form.project;
    manifestInfo.tag = this.form.label;
    manifestInfo.ecrUrl = this.form.releaseUrl;
    manifestInfo.type = this.form.type;
    manifestInfo.prodTarget =
      this.form.type.indexOf("Production") > -1 || false;
    manifestInfo.qaTarget = this.form.type.indexOf("QA") > -1 || false;
    manifestInfo.mavenUrl = "";
    this.$store.dispatch("setValidateJira", manifestInfo);
    this.initReleaseAllStatus();
  }
}
</script>
<style lang="scss" scoped>
.el-button-group {
  .el-button {
    width: 70px;
    height: 30px;
  }
}
.el-input {
  width: 660px;
}
.validate-btn {
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
