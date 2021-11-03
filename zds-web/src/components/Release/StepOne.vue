<template>
  <el-form label-position="top" :model="form" ref="form">
    <div class="step-title">
      <p>
        <span>Release - Choose Files</span>
      </p>
    </div>
    <el-form-item label="SA:" prop="sa">
      <el-input id="sa-ipt" v-model="form.sa" placeholder="Enter SA" :disabled="!existEtlFile" @blur="setSA"></el-input>
    </el-form-item>
    <el-form-item label="Type:">
      <el-button id="etl-btn" type="primary" @click="addType('ETL')" :disabled="existEtl || this.form.sa.length == 0">ETL</el-button>
      <el-button id="uc4-btn" type="primary" @click="addType('UC4')" :disabled="existUC4">UC4</el-button>
      <el-button id="onetime-btn" type="primary" @click="addType('Onetime')" :disabled="existOnetime">Onetime</el-button>
    </el-form-item>
    <el-form-item label="File:">
      <el-table :data="tableData" border height="420">
        <el-table-column property="type" label="Type" width="150"></el-table-column>
        <el-table-column property="path" label="Path">
          <template slot-scope="scope">
            <span v-if="scope.row.isSet">
              <el-input style="width: 100%;" v-model="scope.row.fullPath"></el-input>
            </span>
            <span v-else>
              {{scope.row.fullPath}}
              <el-tag
                v-show="scope.row.tagVisible"
                :type="scope.row.tagType"
              >{{scope.row.tagMessage}}<span class="repo_link" @click="jumpRepository" v-if="scope.row.tagType == 'danger'">Repository.</span></el-tag>
            </span>
          </template>
        </el-table-column>
        <el-table-column fixed="right" width="230">
          <template slot-scope="scope">
            <el-button
              v-show="!scope.row.isSet"
              @click="edit(scope.row, scope.$index)"
              type="primary"
              icon="el-icon-edit"
            >Edit</el-button>
            <el-button
              v-show="scope.row.isSet"
              @click="save(scope.row)"
              type="primary"
            >Save</el-button>
            <el-button
              v-show="!scope.row.isSet"
              @click.native.prevent="remove(scope.$index, tableData)"
              type="primary"
              icon="el-icon-delete"
            >Remove</el-button>
            <el-button
              v-show="scope.row.isSet"
              @click="cancel(scope.row)"
              type="primary"
            >Cancel</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-form-item>
    <add-type-dialog
      :visible.sync="addTypeDialogVisible"
      :data="typeArr"
      :type="type"
      :checked="checkedOptions"
      :disabledOptions="disabledOptions"
      @add-file="addFileData"
    />
  </el-form>
</template>

<script lang="ts">
import { Component, Vue, Inject } from "vue-property-decorator";
import AddTypeDialog from "@/components/Release/AddTypeDialog.vue";
import _ from "lodash";
import GitRemoteService from '@/services/remote/GithubService'
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import Util from "@/services/Util.service";

@Component({
  components: {
    AddTypeDialog
  }
})
export default class StepOne extends Vue {
  @Inject('gitRemoteService')
  gitRemoteService: GitRemoteService
  @Inject()
  notebookRemoteService: NotebookRemoteService
  form: {
    sa: string;
  };

  rule: {
    sa: Array<any>;
  };

  tableData: Array<any> = this.$store.getters.getReleaseFile || [];
  addTypeDialogVisible: boolean = false;
  bfChangePath: string = "";
  type: string = "";
  typeArr: any = [];
  allTypeArr: any = [];
  uc4TypeArr: any = [];
  checkedOptions: Array<any> = [];
  disabledOptions: Array<any> = [];
  fileTypeMap: any = {
    sql: "sql",
    cfg: "cfg",
    lis: "cfg",
    dml: "dml",
    ksh: "bin",
    ddl: "sql"
  };
  mounted() {
    // call api to gain all info by nt, then set all-type items collection and UC4-type items collection
    const vm = this;
    this.notebookRemoteService
      .getAll()
      .then((res: any) => {
        if (res && res.data && res.data != null) {
          _.forEach(_.cloneDeep(res.data), o => {
            if (!_.isEmpty(o.path) && o.path[0] != "/") {
              o.path = "/" + o.path;
            }
            if (!_.isEmpty(o.path) && o.path[o.path.length - 1] != "/") {
              o.path = o.path + "/";
            }
            if (_.isEmpty(o.path)) {
              o.path = "/";
            }
            vm.allTypeArr.push(o);
          })
          _.forEach(_.pickBy(_.cloneDeep(vm.allTypeArr), o => {
              return o.title.indexOf(".xml") > -1;
            }),
            o => vm.uc4TypeArr.push(o)
          );
        }
        console.debug("Call Api:getAll successed");
      })
      .catch(err => {
        console.error("Call Api:getAll failed: " + JSON.stringify(err));
      });
  }

  constructor() {
    super();
    this.form = {
      sa: this.$store.getters.getSA || ""
    };
  }

  get existEtl(): boolean {
    return _.isEmpty(this.allTypeArr);
  }

  get existUC4(): boolean {
    return _.isEmpty(this.uc4TypeArr);
  }

  get existOnetime(): boolean {
    return _.isEmpty(this.allTypeArr);
  }

  get existEtlFile(): boolean {
    const find = _.find(this.tableData, o => o["type"] == "ETL");
    return _.isUndefined(find) || _.isNull(find) || _.isEmpty(find);
  }

  setSA() {
    this.$store.dispatch("setSA", this.form.sa);
  }

  addType(type: string) {
    // gain dialog checkbox-options
    this.typeArr = _.isEqual(type, "UC4")
      ? _.cloneDeep(this.uc4TypeArr)
      : _.cloneDeep(this.allTypeArr);
    // set dialog visible
    this.addTypeDialogVisible = true;
    // set file type
    this.type = type;
    // set selected-items
    this.checkedOptions = [];
    const pickChecked = _.pickBy(_.cloneDeep(this.tableData), o => {
      return o.type == this.type;
    });
    if (!_.isUndefined(pickChecked)) {
      _.forEach(pickChecked, o => {
        const find = _.find(this.typeArr, v => v.id == o.id);
        if (!_.isUndefined(find)) this.checkedOptions.push(o);
      });
    }

    // set disabled-items, if it was seleced by ETL type, you can`t select it from Onetime
    this.disabledOptions = [];
    const pickDisabled = _.pickBy(_.cloneDeep(this.tableData), o => {
      return o.type != this.type;
    });
    if (!_.isUndefined(pickDisabled)) {
      _.forEach(pickDisabled, o => {
        const find = _.find(this.typeArr, v => v.id == o.id);
        if (!_.isUndefined(find)) this.disabledOptions.push(o);
      });
    }
  }

  addFileData(data: Array<any>, type: string) {
    let addData = _.cloneDeep(data);
    const vm = this;
    _.forEach(addData, o => {
      o["notebookId"] = o["id"];
      o["type"] = type;
      o["isSet"] = false;
      o["fullPath"] = vm.createFullPath(o, type);
      o["tagVisible"] = false;
      o["tagType"] = "";
      o["tagMessage"] = "";
    });
    _.remove(this.tableData, o => {return o.type == type && !_.find(data, so => {return so.id == o.id})});
    this.tableData = _.unionBy(this.tableData, addData, "id");
    this.$store.dispatch("setReleaseFile", this.tableData);
    this.pickGitFileWaitCheck();
  }

  // /type/postfix/sa/title
  createFullPath(row: any, type: string) {
    let fullPath = "";
    if (_.isEqual(type, "ETL")) {
    fullPath = "/" + type.toLowerCase();
    const split = _.isUndefined(row["title"]) ? [] : row["title"].split(".");
    const postfix: string = _.isEmpty(split)
      ? undefined
      : this.fileTypeMap[_.lowerCase(split[split.length - 1])];
    if (!_.isUndefined(postfix)) fullPath = fullPath + "/" + postfix;
      fullPath = fullPath + "/" + this.form.sa + "/" + row["title"];
    }else {
      if (row["path"].indexOf("/uc4") == 0 || row["path"].indexOf("/onetime") == 0) {
        fullPath = row["path"] + row["title"];
      }else {
        fullPath = "/" + type.toLowerCase() + row["path"] + row["title"];
      }
    }
    
    return fullPath;
  }

  pickGitFileWaitCheck() {
    let fileArr: any = [];
    let project: string = "";
    let checkMap: any = new Map();
    _.forEach(_.pickBy(this.tableData, o => o["gitRepo"] != null), o => {
      if (_.isUndefined(checkMap[o["gitRepo"]])) {
        fileArr = [];
        fileArr.push({ fullPath: o["fullPath"], sha: o["sha"] });
      }else {
        fileArr = checkMap[o["gitRepo"]];
        fileArr.push({ fullPath: o["fullPath"], sha: o["sha"] });
      }

      checkMap[o["gitRepo"]] = fileArr;
    });

    _.forEach(checkMap, (v: any, k: string) => {
      this.checkGitFileExists(v, undefined, k);
    })
  }

  checkGitFileExists(fileArr: any, row: any, project: string) {
    const nt: string = Util.getNt();
    const url: string = "https://github.corp.ebay.com/" + project;
    const branch: string = "master";
    let files = !_.isUndefined(fileArr)
      ? fileArr
      : !_.isUndefined(row)
      ? [{ fullPath: row["fullPath"], sha: row["sha"] }]
      : [];

    const promise: Promise<any> = this.gitRemoteService.checkGitFileExists(
      nt,
      url,
      branch,
      files
    ) as Promise<any>;
    const vm = this;
    promise
      .then(res => {
        if (res && res.data && res.data != null) {
          if (!_.isUndefined(fileArr)) {
            _.forIn(res.data, (v: string, k: string) => {
              let find = _.find(vm.tableData, o => o["fullPath"] == k);
              vm.setRowTagInfo(vm.tableData[0], v);
              //if (!_.isUndefined(find)) vm.setRowTagInfo(find, v);
            });
          } else if (!_.isUndefined(row)) {
            vm.setRowTagInfo(row, res.data[row["fullPath"]]);
          }
        }
        console.debug("Call Api:checkGitFileExists successed");
      })
      .catch(err => {
        console.error(
          "Call Api:checkGitFileExists failed: " + JSON.stringify(err)
        );
      });
  }

  setRowTagInfo(row: any, gitMess: string) {
    if (_.isEqual(gitMess, "existed")) {
      row["tagVisible"] = true;
      row["tagType"] = "warning";
      row["tagMessage"] =
        "The file exists. Clicking next will automatically Override.";
    } else if (_.isEqual(gitMess, "conflicted")) {
      row["tagVisible"] = true;
      row["tagType"] = "danger";
      row["tagMessage"] =
        "The file version is lower than github. It maybe occurs conflict. Please pull the latest file version from ";
    }
  }

  edit(row: any, index: number) {
    let isSet: boolean = true;
    _.forEach(this.tableData, (v: any, k: any) => {
      if (v.isSet && k != index) {
        this.$message.info("Please save the current edit item first.");
        isSet = false;
      }
    });
    if (isSet) {
      row["isSet"] = isSet;
      this.bfChangePath = row["fullPath"];
    }
  }

  remove(index: number, rows: Array<any>) {
    let isSet: boolean = true;
    _.forEach(this.tableData, (v: any, k: any) => {
      if (v.isSet && k != index) {
        this.$message.info("Please save the current edit item first.");
        isSet = false;
      }
    });
    if (isSet) {
      rows.splice(index, 1);
      this.$store.dispatch("setReleaseFile", this.tableData);
    }
  }

  save(row: any) {
    row["isSet"] = false;
    row["tagVisible"] = false;
    row["tagType"] = "";
    row["tagMessage"] = "";
    this.$store.dispatch("setReleaseFile", this.tableData);
    if (
      !_.isUndefined(row["gitRepo"]) &&
      !_.isNull(row["gitRepo"]) &&
      !_.isEmpty(row["gitRepo"])
    ) {
      this.checkGitFileExists(undefined, row, row["gitRepo"]);
    }
  }

  cancel(row: any) {
    row["isSet"] = false;
    row["fullPath"] = this.bfChangePath;
  }

  jumpRepository() {
    this.$router.push("/repository");
  }
}
</script>
<style lang="scss" scoped>
.el-button {
  min-width: 90px;
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
}
.el-button--primary:not(.is-plain):hover {
  color: #4d8cca;
  background-color: rgba(255, 255, 255, 0);
  border-color: #4d8cca;
}
.el-button--primary:not(.is-plain):focus {
  color: #4d8cca;
  background-color: rgba(255, 255, 255, 0);
  border-color: #4d8cca;
}
.el-button--primary.is-disabled:not(.is-plain) {
  color: #cacbcf;
  background-color: rgba(255, 255, 255, 0) !important;
  border: 1px solid #cacbcf !important;
}
.el-button--primary.is-disabled:not(.is-plain):hover {
  color: #cacbcf;
  background-color: rgba(255, 255, 255, 0) !important;
  border: 1px solid #cacbcf !important;
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
  /deep/ .el-input {
    width: 188px;
  }
}
.tabel {
  height: 420px;
  background: inherit;
  background-color: rgba(255, 255, 255, 1);
  box-sizing: border-box;
  border-width: 1px;
  border-style: solid;
  border-color: rgba(221, 221, 221, 1);
  border-radius: 4px;
  box-shadow: none;
}
.repo_link {
  text-decoration: underline;
  cursor: pointer;
}
</style>
