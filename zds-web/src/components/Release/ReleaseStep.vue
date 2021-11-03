<template>
  <div
    id="releasePage"
    class="content"
    style="height: 100%"
    v-loading="loading"
  >
    <div class="stepsBar">
      <el-steps :active="this.active" finish-status="success" simple>
        <el-step title="STEP 1"></el-step>
        <el-step title="STEP 2"></el-step>
        <el-step title="STEP 3"></el-step>
        <el-step title="STEP 4"></el-step>
        <el-step title="STEP 5"></el-step>
        <el-step title="STEP 6"></el-step>
      </el-steps>
      <el-badge :value="issueCount" class="item">
        <el-button id="message-btn" size="small" icon="el-icon-message" @click="openReleaseIssueDialog"></el-button>
      </el-badge>
    </div>
    <div class="form">
      <step-one id="step-one" v-if="this.active == 0"/>
      <step-two id="step-two" v-if="this.active == 1"/>
      <step-three id="step-three" v-if="this.active == 2"/>
      <step-four id="step-four" v-if="this.active == 3"/>
      <step-five id="step-five" v-if="this.active == 4" @refreash-dialog="getReleaseHistory" @forward-step-three="forwardStepThree"/>
      <step-six id="step-six" v-if="this.active == 5"/>
      <div class="buttonGroup">
        <el-button id="pre-btn" v-if="this.active > 0" type="primary" @click="lastStep" :disabled="loading">Previous</el-button>
        <el-button id="next-btn" v-if="nextBtnDisplay" type="primary" @click="nextStep" :disabled="nextDisabled || loading">Next</el-button>
        <span id="skip-span" class="skip" v-if="this.active == 0" @click="forwardStepThree">Skip</span>
        <el-button id="done-btn" v-if="iframeDone" type="primary" disabled>Done</el-button>
      </div>
    </div>
    <release-issue-dialog  :visible.sync="releaseIssueDialogVisible" :issue="issue" @forward-step-six="forwardStepSix"/>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Inject } from "vue-property-decorator";
import StepOne from "@/components/Release/StepOne.vue";
import StepTwo from "@/components/Release/StepTwo.vue";
import StepThree from "@/components/Release/StepThree.vue";
import StepFour from "@/components/Release/StepFour.vue";
import StepFive from "@/components/Release/StepFive.vue";
import StepSix from "@/components/Release/StepSix.vue";
import ReleaseIssueDialog from "@/components/Release/ReleaseIssueDialog.vue";
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import _ from "lodash";


@Component({
  components: {
    StepOne,
    StepTwo,
    StepThree,
    StepFour,
    StepFive,
    StepSix,
    ReleaseIssueDialog
  }
})
export default class ReleaseStep extends Vue {
  @Inject()
  notebookRemoteService: NotebookRemoteService
  active: number = 0;
  releaseIssueDialogVisible: boolean = false;
  issueCount: number = 0;
  issue: any = [];
  stepAction: any = [];

  get loading(): boolean {
    return this.$store.getters.getReleaseSubmitLoading || false;
  }

  mounted() {
    this.getReleaseHistory();
  }

  get nextDisabled(): boolean {
    if (this.active == 0) {
      const releaseFile = this.$store.getters.getReleaseFile;
      return (
        _.isUndefined(releaseFile) ||
        _.isNull(releaseFile) ||
        _.isEmpty(releaseFile)
      );
    } else if (this.active == 1) {
      return !this.$store.getters.getGithubPushStatus;
    } else if (this.active == 2) {
      return !this.$store.getters.getJiraStatus;
    } else if (this.active == 3) {
      return !this.$store.getters.getSendManifestValidateStatus;
    } else if (this.active == 4) {
      return !(this.$store.getters.getRolloutStatus && this.$store.getters.getExecTaskStatus && this.$store.getters.getExecTask.length > 0);
    }
    return false;
  }
 
  get nextBtnDisplay(): boolean {
    // page 5 when rollout successed and no iframe need be shown, next-button should be invisiabled.
    if (this.active == 4 && this.$store.getters.getRolloutStatus && this.$store.getters.getExecTaskStatus && this.$store.getters.getExecTask.length == 0) {
      return false;
    }else if (this.active == 5 && this.$store.getters.getIframeIdx >= this.$store.getters.getExecTask.length - 1) {
      return false;
    }else if (this.active < 5) {
      return true;
    }
    return false;
  }

  get iframeDone(): boolean {
    if (this.active == 5 && this.$store.getters.getIframeIdx >= this.$store.getters.getExecTask.length - 1) {
      return true;
    }else if (this.active == 4 && this.$store.getters.getRolloutStatus && this.$store.getters.getExecTaskStatus && this.$store.getters.getExecTask.length == 0) {
      return true;
    }
    return false;
  }

  nextStep() {
    if (this.active == 5) {
      let index = this.$store.getters.getIframeIdx + 1;
      this.$store.dispatch("setIfameIdx", index <= this.$store.getters.getExecTask.length - 1 ? index : this.$store.getters.getExecTask.length - 1);
    }else {
      this.stepAction.push(this.active);
      this.active++;
    }
  }

  lastStep() {
    if (this.active == 5) {
      let index = this.$store.getters.getIframeIdx - 1;
      this.$store.dispatch("setIfameIdx", index < 0 ? 0 : index);
      if (index < 0) {
        this.active = this.stepAction.pop() || 0
      }
    }else {
      this.active = this.stepAction.pop() || 0;
    }
  }

  forwardStepThree() {
    this.stepAction.push(this.active);
    this.active = 2;
  }

  openReleaseIssueDialog() {
    if (this.issueCount > 0) {
      this.releaseIssueDialogVisible = true;
    }
  }

  forwardStepSix() {
    this.stepAction.push(this.active);
    this.active = 5;
  }

  getReleaseHistory() {
    this.notebookRemoteService.getReleaseHistory().then((res: any) => {
      if (res && res.data && res.data != null) {
        this.issue = res.data;
        this.issueCount = this.issue.length;
        console.debug("Call Api:getReleaseHistory successed");
      }
    }).catch(err => {
      console.error("Call Api:getReleaseHistory failed: " + JSON.stringify(err));
    });
  }
}
</script>
<style lang="scss" scoped>
@import '@/styles/global.scss';
$bc-height: $workspace-tab-height + $workspace-tab-margin-bottom;
.content {
  width: 100%;
}
.el-steps {
  width: 100%;
  height: 20px;
}
.el-badge {
  #message-btn {
    height: 46px;
  }
}
.form {
  width: 100%;
  overflow-y: auto;
  height: calc( 100% - #{$bc-height}) ;
}
.buttonGroup {
  width: 180px;
  margin: 0 auto;
  display: flex;
  align-items: flex-end;
}
.buttonGroup {
  /deep/ .el-button {
    min-width: 90px;
  }
}
.skip {
  text-decoration: underline;
  font-weight: 400;
  font-style: normal;
  font-size: 14px;
  color: #569ce1;
  margin-left: 10px;
  cursor: pointer;
}
.stepsBar {
	display: flex;
	flex-direction: row;
	height: 46px;
	.el-steps {
		width: 100%;
		height: 20px;
	}
	.el-step.is-simple {
		/deep/ .el-step__icon.is-text {
			border: 1px solid;
		}
		/deep/ .el-step__title {
			font-size: 14px;
		}
		/deep/ .el-step__head.is-wait {
			.el-step__icon.is-text {
				border-color: #999999;
			}
		}
		/deep/ .el-step__title.is-wait {
			color: #999999;
		}
		/deep/ .el-step__head.is-process {
			.el-step__icon.is-text {
				border-color: #569CE1;
			}
		}
		/deep/ .el-step__title.is-process {
			font-weight: 400;
			color: #569CE1;
		}
		/deep/ .el-step__head.is-success {
			.el-step__icon.is-text {
				border-color: #5CB85C;
			}
		}
		/deep/ .el-step__title.is-success {
			color: #5CB85C;
		}
	}
}
</style>
