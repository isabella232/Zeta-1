<template>
  <div>
    <div
      class="view-details"
      @click="showDetails()"
    >
      View Result
    </div>
    <el-dialog
      title="Error Log"
      :visible.sync="errorDialogVisible"
      custom-class="log-dialog"
      :append-to-body="true"
    >
      <div v-if="errorLog && errorLog !==''">
        {{ errorLog }}
      </div>
      <div v-else>
        Did not find Error Log
      </div>
    </el-dialog>
    <el-dialog
      :visible.sync="detailVisible"
      :title="title"
      width="60%"
      custom-class="result-dialog"
      :append-to-body="true"
    >
      <div
        v-if="type == 'Notebook'"
        class="notebook-detail"
      >
        <notebook-history
          v-if="reqId !== null"
          :req-id="reqId"
        />
      </div>
      <div
        v-if="type == 'DataMove'"
        class="data-move-sql"
      >
        <CodeDisplay
          v-if="dmSql && dmSql !==''"
          v-model="dmSql"
          disabled
          :options="editorOptions"
        />
        <div v-else>
          Did not find SQL
        </div>
      </div>
      <div
        v-if="type == 'DataValidate'"
        class="data-validate-result"
      >
        <div class="dialog-table-title">
          Row Count
        </div>
        <el-table
          v-if="validateResult.rowCnt"
          :data="[validateResult.rowCnt]"
          :row-class-name="checkColClassName"
          border
          class="dv-result-table"
        >
          <el-table-column
            :label="validateResult.srcPlatform"
            prop="src"
          />
          <el-table-column
            :label="validateResult.targetPlatform"
            prop="target"
          />
        </el-table>
        <div
          class="dialog-table-title"
          style="margin-top: 20px; "
        >
          Sum Check
        </div>
        <el-table
          v-if="validateResult.sumCheck"
          :data="validateResult.sumCheck"
          :default-sort="{prop: 'columnName', order: 'ascending'}"
          :row-class-name="checkColClassName"
          border
          class="dv-result-table"
        >
          <el-table-column
            label="Column Name"
            prop="columnName"
            sortable
          />
          <el-table-column
            :formatter="colValueformatter"
            :label="validateResult.srcPlatform"
            prop="src"
            width="160px"
          />
          <el-table-column
            :formatter="colValueformatter"
            :label="validateResult.targetPlatform"
            prop="target"
            width="160px"
          />
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch } from 'vue-property-decorator';
import NotebookHistory from '@/components/Schedule/components/components/notebook-history.vue';
import CodeDisplay from '@/components/common/Visualization/CodeDisplay.vue';

import { ScheduleHistory } from '@/components/common/schedule-container';
import {
  ValidationResult,
  ValidationResultRow,
} from '@/components/DataValidation/History.vue';
import _  from 'lodash';
import Util from '@/services/Util.service';
import DVRemoteService from  '@/services/remote/DataValidation';
import DMRemoteService from '@/services/remote/DataMove';

@Component({
  components:{
    NotebookHistory,
    CodeDisplay,
  },
})
export default class ResultDetail extends Vue {
  dvRemoteService = new DVRemoteService();
  dmRemoteService = new DMRemoteService();
  @Prop() task: ScheduleHistory;
  @Prop() notebookId: string;

  errorDialogVisible = false;
  errorLog = '';
  detailVisible = false;
  reqId: number | null = null;
  dmSql = '';

  editorOptions = {
    // codemirror options
    tabSize: 4,
    mode: 'text/x-mysql',
    lineNumbers: true,
    smartIndent: false,
    matchBrackets: true,
    autofocus: true,
    indentWithTabs: true
  };
  validateResult: ValidationResult = {
    rowCnt: undefined,
    sumCheck: [],
    srcPlatform: '',
    targetPlatform: ''
  };

  get type(){
    return this.task? this.task.type: 'Notebook';
  }
  get title(): string {
    let title = 'Notebook Result';
    switch (this.type) {
      case 'Notebook':
        break;
      case 'DataMove':
        title = 'Data Move SQL';
        break;
      case 'DataValidate':
        title = 'Data Validation Result';
      default:
        break;
    }
    return title;
  }
  showError() {
    this.errorDialogVisible = true;
    this.errorLog = this.task.log?this.task.log.replace('\\n', '\n'):'';
  }
  showDetails() {
    // system error
    if(!this.task.jobHistoryId){
      this.showError();
      return;
    }
    // run error
    switch (this.type) {
      case 'Notebook':
        this.show();
        break;
      case 'DataMove':
        this.showDataMoveSql();
        break;
      case 'DataValidate':
        this.showResult();
        break;
      default:
        break;
    }
  }

  show() {
    this.reqId = null;
    this.detailVisible = true;
    this.reqId = this.task.jobHistoryId;
  }

  showDataMoveSql() {
    this.detailVisible = true;
    const id = this.task.jobHistoryId as number;
    this.dmRemoteService.getDetail(id).then((res) => {
      if(res && res.data && res.data != null){
        this.dmSql = Util.SQLFormatter(res.data.dataMoveDetail.query);
      }
    }).catch(e => {
      throw e;
    });
  }

  showResult() {
    this.detailVisible = true;
    const id = this.task.jobHistoryId as number;
    this.dvRemoteService.getDetail(id).then((res) => {
      if(res && res.data && res.data != null){
        this.parseResult(res.data.dataValidateDetail.result);
      }
    }).catch(e => {
      throw e;
    });
  }
  parseResult(dvResult) {
    let result: any = dvResult;
    try {
      result = JSON.parse(result);
      const platforms: Array<any> = [];
      for (let i = 0; i < result.length; i++) {
        if (platforms.indexOf(result[i].platform) === -1) {
          platforms.push(result[i].platform);
        }
      }
      if (platforms.length === 1) {
        platforms.push(platforms[0]);
      }
      const plat0 = platforms[1],
        plat1 = platforms[0];
      const rowCount: ValidationResultRow = {
        columnName: 'ROWCOUNT',
        target: 0,
        src: 0,
      };
      const resultArr: Array<ValidationResultRow> = [];
      _.chain(result)
        .groupBy('metric_column')
        .forEach((value: any, key: string) => {
          if (key === 'ROWCOUNT') {
            rowCount.src = value.find(
              (v: any) =>
                v.platform.toLowerCase() == plat0.toLowerCase()
            ).metric_value;
            rowCount.target = value.find(
              (v: any) =>
                v.platform.toLowerCase() == plat1.toLowerCase()
            ).metric_value;
          } else {
            const src = value.find(
              (v: any) =>
                v.platform.toLowerCase() == plat0.toLowerCase()
            );
            const target = value.find(
              (v: any) =>
                v.platform.toLowerCase() == plat1.toLowerCase()
            );
            resultArr.push({
              columnName: key,
              src:
                  src && src.metric_value
                    ? src.metric_value
                    : null,
              target:
                    target && target.metric_value
                      ? target.metric_value
                      : null
            });
          }
        })
        .value();
      this.validateResult.rowCnt = rowCount;
      this.validateResult.sumCheck = resultArr;
      this.validateResult.srcPlatform = this.convertPlatform(plat0);
      this.validateResult.targetPlatform = this.convertPlatform(plat1);
    } catch (e) {
      console.error(e);
    }
  }
  colValueformatter(row: any, column: string, val: string) {
    if (val === null) {
      return 'N/A';
    } else {
      return val;
    }
  }
  checkColClassName({ row }: any) {
    if (row.src !== row.target) {
      return 'failed-row';
    }
    return '';
  }
  convertPlatform(platform: string): string {
    if(platform.toLowerCase().indexOf('numozart')>-1 || platform.toLowerCase().indexOf('mozart_lvs')>-1){
      return 'mozart';
    }
    return platform;
  }
}
</script>

<style lang="scss" scoped>
@import "@/styles/global.scss";

.view-details{
  cursor: pointer;
  color: $zeta-global-color;
  text-decoration: underline;
  outline: none;
}
.el-dialog__wrapper /deep/ .result-dialog {
    padding-bottom: 30px;
    .el-dialog__header {
        padding: 30px 30px 0;
    }
    .el-dialog__body {
        padding: 0 30px;
        overflow-y: auto;
        word-break: break-word;
    }
  .dialog-table-title {
        font-size: 14px;
        font-weight: 600;
        margin-bottom: 10px;
    }
    /deep/ .dv-result-table {
        tr td,
        tr th {
            padding: 6px 0;
        }
    }
    .el-dialog__wrapper{
      .el-dialog__body{
        padding: 20px;
      }
    }
}
.el-dialog__wrapper /deep/ .log-dialog {
    padding-bottom: 30px;
    .el-dialog__header {
        padding: 30px 30px 0;
    }
    .el-dialog__body {
        padding: 0 30px;
        max-height: 500px;
        overflow-y: auto;
        word-break: break-word;
        white-space: pre-line;
        line-height: 25px;
    }
}
</style>
