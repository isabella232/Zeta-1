<template>
  <el-form label-position="top" :model="form" ref="form">
    <div class="step-title"><p><span>Release - Rollout</span></p></div>
    <el-form-item label="Project:" prop="project">
      <el-input v-model="form.project" readonly></el-input>
    </el-form-item>
    <el-form-item label="Label:" prop="label">
      <el-input v-model="form.label" readonly></el-input>
    </el-form-item>
    <el-form-item label="Developer:" prop="developer">
      <el-input v-model="form.developer" readonly></el-input>
    </el-form-item>
    <el-form-item label="Release Ticket URL:" prop="releaseTicketUrl">
      <el-input v-model="form.releaseTicketUrl" readonly></el-input>
    </el-form-item>
    <el-form-item label="Release Target:" prop="releaseTarget">
      <el-input v-model="form.releaseTarget" readonly></el-input>
    </el-form-item>
    <el-form-item label="Release File:">
      <el-table :data="tableData" border>
        <el-table-column  property="row" label="#" width="50"></el-table-column>
        <el-table-column  property="path" label="Path"></el-table-column>
        <el-table-column  property="developer" label="Developer" width="150"></el-table-column>
        <el-table-column  property="commit" label="Commit" width="150"></el-table-column>
        <el-table-column  property="date" label="Date" width="250"></el-table-column>
      </el-table>
    </el-form-item>
    <el-button type="primary" @click="submit" :disabled="submitDisabled">submit</el-button>
    <el-tag v-show="submitResult" :type="submitResultType">{{submitMessage}}
      <span v-if="submitResultType == 'danger'">go to</span>
      <span class="step_link" @click="jumpStepThree" v-if="submitResultType == 'danger'"> Step 3 </span>
      <span v-if="submitResultType == 'danger'">check.</span>
    </el-tag>
  </el-form>
</template>

<script lang="ts">
import { Component, Vue, Inject } from "vue-property-decorator";
import JIRARemoteService from "@/services/remote/JIRARemoteService";
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import Util from "@/services/Util.service";
import _ from "lodash";
@Component({
  components: {}
})
export default class StepFive extends Vue {
  @Inject()
  notebookRemoteService: NotebookRemoteService
  @Inject('jiraRemoteService')
  jiraRemoteService: JIRARemoteService

  releaseTagName: string = "";
  ticketName: string = "";
  submitResult: boolean = false;
  submitResultType: string = "";
  submitMessage: string = "";
  timer: any;
  submitDisabled: boolean = false;
  form: {
    project: string;
    label: string;
    developer: string;
    releaseTicketUrl: string;
    releaseTarget: string;
  };

  tableData: any = this.$store.getters.getCommitData || [];

  constructor() {
    super();
    const jiraInfo: any = this.$store.getters.getValidateJira;
    this.form = {
      project: jiraInfo["project"] || "",
      label: jiraInfo["tag"] || "",
      developer: Util.getNt() || "",
      releaseTicketUrl: jiraInfo["ecrUrl"] || "",
      releaseTarget: _.join(jiraInfo["type"], ',') || ""
    };
    this.releaseTagName = jiraInfo["tag"];
    this.ticketName = jiraInfo["ticketName"];
  }

  submit() {
    this.$store.dispatch("setReleaseSubmitLoading", true);
    const params: any = {tag: this.releaseTagName};
    this.notebookRemoteService.rollout(params).then(res => {
      console.debug("Call Api:rollout successed");
      if (res && res.status == 200) {
        this.getOnetimeExecTask();
      }else {
        this.submitResult = true;
        this.submitResultType = "danger";
        this.submitMessage = "Rollout Manifest Submit Failed";
        this.$store.dispatch("setRolloutStatus", false);
        this.$store.dispatch("setReleaseSubmitLoading", false);
        console.debug("rollout manifest submit failed");
      }
    }, function (err : any) {
      err.resolve();
      throw err;
    }).catch(err => {
      this.submitResult = true;
      this.submitResultType = "danger";
      this.submitMessage = "Rollout Manifest Submit Failed";
      this.$store.dispatch("setRolloutStatus", false);
      this.$store.dispatch("setReleaseSubmitLoading", false);
      console.error("Call Api:rollout failed: " + JSON.stringify(err));
    });
  }

  getOnetimeExecTask() {
    this.timer = setInterval(() => {
      this.notebookRemoteService.getReleaseStatus(this.releaseTagName).then(res => {
        console.debug("Call Api:getReleaseStatus successed");
        if (res && res.data && res.data != null) {
          if (res.data["status"]["value"] == "done") {
            let tasks: any = res.data.execTask || {};
            let execTaskArr: any = [];
            _.forEach(tasks, (v: string, k: string) => {
              if (parseInt(v) != 0) {
                const obj = {type: k, id: v};
                execTaskArr.push(obj);
              }
            })
            this.$store.dispatch("setExecTask", execTaskArr);
            this.$store.dispatch("setIfameIdx", 0);
            this.$store.dispatch("setExecTaskStatus", true);
            this.changeResolution();
            this.submitResult = true;
            this.submitResultType = "success";
            this.submitMessage = "Release Success";
            this.$store.dispatch("setRolloutStatus", true);
            this.$store.dispatch("setReleaseSubmitLoading", false);
            this.changeResolution();
            this.$emit("refreash-dialog");
            window.clearInterval(this.timer);
          }else if (res.data["status"]["value"] == "failed") {
            this.submitResult = true;
            this.submitResultType = "danger";
            this.submitMessage = !_.isUndefined(res.data.failMsg) && !_.isEmpty(res.data.failMsg) ? res.data.failMsg : "Release failed";
            this.$store.dispatch("setRolloutStatus", false);
            this.$store.dispatch("setReleaseSubmitLoading", false);
            this.submitDisabled = true;
            window.clearInterval(this.timer);
          } else {
            console.debug("Release execution task not ready, and call again");
          }
        }
      }, function (err : any) {
        err.resolve();
        throw err;
      }).catch(err => {
        this.submitResult = true;
        this.submitResultType = "danger";
        this.submitMessage = "Release failed";
        this.$store.dispatch("setRolloutStatus", false);
        this.$store.dispatch("setReleaseSubmitLoading", false);
        this.submitDisabled = true;
        window.clearInterval(this.timer)
        console.error("Call Api:getReleaseStatus failed: " + JSON.stringify(err));
      })
    }, 1000);
  }

  changeResolution() {
    this.jiraRemoteService.editResolution(this.ticketName || "").then(res => {
      console.debug("Call Api:edit resolution successed");
    }, function (err : any) {
      err.resolve();
      throw err;
    }).catch(err => {
      console.error("Call Api:edit resolution failed: " + JSON.stringify(err));
    });
  }

  jumpStepThree() {
    this.$emit("forward-step-three");
  }
}
</script>
<style lang="scss" scoped>
.el-input {
  width: 660px;
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
  color: #569CE1;
  margin-bottom: 22px;
}
.step-title{
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
.step_link {
  text-decoration: underline;
  cursor: pointer;
}
</style>
