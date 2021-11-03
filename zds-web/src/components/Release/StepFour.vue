<template>
  <el-form label-position="top">
    <div class="step-title"><p><span>Release - Validate</span></p></div>
    <el-progress :text-inside="true" :stroke-width="30" :percentage="percentage"></el-progress>
    <el-form-item label="File:">
      <el-table :data="tableData" border>
        <el-table-column  property="row" label="#" width="50"></el-table-column>
        <el-table-column  property="path" label="Path"></el-table-column>
        <el-table-column  property="developer" label="Developer" width="150"></el-table-column>
        <el-table-column  property="commit" label="Commit" width="150"></el-table-column>
        <el-table-column  property="date" label="Date" width="250"></el-table-column>
      </el-table>
    </el-form-item>
    <el-button class="submit-btn" type="primary" @click="onSubmit" :disabled="percentage < 100">Submit</el-button>
    <el-tag v-show="submitResult" :type="submitResultType">{{submitMessage}}</el-tag>
  </el-form>
</template>

<script lang="ts">
import { Component, Vue, Inject } from "vue-property-decorator";
import JIRARemoteService from "@/services/remote/JIRARemoteService";
import Util from "@/services/Util.service";
import _ from "lodash";
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';

@Component({
  components: {}
})
export default class StepFour extends Vue {
  @Inject()
  notebookRemoteService: NotebookRemoteService
  @Inject('jiraRemoteService')
  jiraRemoteService: JIRARemoteService

  tableData: Array<any> = [];
  percentage: number = 0;
  timer: any;
  errorTimes: number = 5;
  submitResult: boolean = false;
  submitResultType: string = "";
  submitMessage: string = "";

  mounted() {
    const manifest: any = this.$store.getters.getValidateJira || {};
    let tag: string = manifest.tag || "";
    this.timer = setInterval(() => {
      this.jiraRemoteService.getAbinitioManifest(tag).then(res => {
        if (res && res.data && res.data != null) {
          const commitFileMap = res.data.commitFileMap || [];
          let rowIndex: number = 1;
          let commitDataArr: Array<any> = [];
          _.forEach(commitFileMap, (v: any) => {
            _.forEach(v, (sv: any) => {
              const commitData: any = {row: rowIndex,
                path: sv.name,
                developer: sv.authorName,
                commit: sv.commit,
                date: sv.date
              };

              commitDataArr.push(commitData);
              rowIndex += 1;
            })
          })
          this.tableData = commitDataArr;
        }else {
          this.errorTimes--;
        }

        if (res.data.status["value"] == "done") {
          this.percentage = 100;
        }else {
          if (this.percentage <= 99) this.percentage += 1
        }
      }, function (err : any) {
        err.resolve();
        throw err;
      }).catch(err => {
        this.errorTimes--;
        console.error("Call Api:get manifest failed: " + JSON.stringify(err));
      });
      if (this.percentage >= 100 || this.errorTimes <= 0) {
        window.clearInterval(this.timer)
      }
    }, 1000);
  }

  getPercent() {
    this.percentage = this.percentage++;
  }

  onSubmit() {
    const manifest: any = this.$store.getters.getValidateJira || {};
    const params: any = {tag: manifest.tag || "", username: Util.getNt()};
    this.$store.dispatch("setReleaseSubmitLoading", true);
    this.notebookRemoteService.sendManifestValidate(params).then(res => {
      console.debug("Call Api:send manifest validate successed");
      if (res && res.data && res.data != null) {
        this.submitResult = true;
        this.submitResultType = "success";
        this.submitMessage = "Success";
        this.$store.dispatch("setSendManifestValidateStatus", true);
        this.$store.dispatch("setCommitData", this.tableData);
      }
      this.$store.dispatch("setReleaseSubmitLoading", false);
    }, function (err : any) {
      err.resolve();
      throw err;
    }).catch(err => {
      this.submitResult = true;
      this.submitResultType = "danger";
      this.submitMessage = "Fail";
      this.$store.dispatch("setSendManifestValidateStatus", false);
      this.$store.dispatch("setReleaseSubmitLoading", false);
      console.error("Call Api:send manifest validate failed: " + JSON.stringify(err));
    });
  }
}
</script>
<style lang="scss" scoped>
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
.submit-btn {
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
</style>
