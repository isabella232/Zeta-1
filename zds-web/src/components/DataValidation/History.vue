<template>
  <div
    v-loading="loading"
    class="history"
  >
    <el-table
      v-if="allHistory && allHistory.length > 0"
      class="history-table"
      :data="historyList"
      border
      style="width: 100%"
      :row-class-name="tableRowClassName"
    >
      <el-table-column
        prop="sourcePlatform"
        :width="100"
        label="Source"
      >
        <template slot-scope="scope">
          <span>{{ scope.row.sourcePlatform | numozartAdapter }}</span>
        </template>
      </el-table-column>
      <el-table-column
        prop="sourceTable"
        label="Source Table"
      />
      <el-table-column
        prop="targetPlatform"
        :width="100"
        label="Destination"
      >
        <template slot-scope="scope">
          <span>{{ scope.row.targetPlatform | numozartAdapter }}</span>
        </template>
      </el-table-column>
      <el-table-column
        prop="targetTable"
        label="Destination Table"
      >
        <template slot-scope="scope">
          {{ scope.row.targetTable }}
          <span
            v-if="scope.row.isNew"
            class="is-new"
          >(new)</span>
        </template>
      </el-table-column>
      <el-table-column
        :width="150"
        label="Runtime"
      >
        <template slot-scope="scope">
          <template v-if="scope.row.startTime">
            <div>
              {{ scope.row.startTime }}
              <br>
              to {{ scope.row.endTime ? scope.row.endTime : '--' }}
            </div>
          </template>
          <template v-else>
            <div>N/A</div>
          </template>
        </template>
      </el-table-column>
      <el-table-column
        prop="createDate"
        :width="150"
        label="Date"
      />
      <el-table-column
        label="Status"
        :width="150"
      >
        <template slot-scope="scope">
          <div class="flex-center">
            <!-- <span class="cycle" v-bind:class="scope.row.statusClass"></span> -->
            <span
              class="status"
              :class="scope.row.statusClass"
            >{{ scope.row.statusString }}</span>
            <el-button
              v-if="scope.row.status == 2"
              title="Error Log"
              type="text"
              @click="()=>{showLog(scope.row)}"
            >
              <i class="el-icon-error" />
            </el-button>
          </div>
        </template>
      </el-table-column>
      <el-table-column
        :width="90"
        header-align="center"
        align="center"
        label="SQL"
      >
        <template slot-scope="scope">
          <el-button
            title="Show SQL"
            type="text"
            @click="()=>{showSql(scope.row)}"
          >
            <i class="zeta-icon-sql zeta-icon" />
          </el-button>
        </template>
      </el-table-column>
      <el-table-column
        header-align="center"
        align="center"
        label="Result"
        :width="90"
      >
        <template slot-scope="scope">
          <el-button
            title="Show Result"
            type="text"
            @click="()=>{showResult(scope.row)}"
          >
            <!-- <div class="img-icon"> -->
            <i class="zeta-icon-result" />
            <!-- </div> -->
          </el-button>
        </template>
      </el-table-column>
      <el-table-column
        header-align="center"
        align="center"
        label="Redo"
        :width="90"
      >
        <template slot-scope="scope">
          <el-button
            title="Validate Again"
            type="text"
            :disabled="scope.row.sourcePlatform === 'mozart' || scope.row.sourcePlatform === 'mozart_lvs'"
            @click="()=>{moveClick(scope.row)}"
          >
            <i class="zeta-icon-DataValidation" />
          </el-button>
        </template>
      </el-table-column>
      <el-table-column
        header-align="center"
        align="center"
        label="Schedule"
        :width="90"
      >
        <template slot-scope="scope">
          <el-button
            title="Schedule"
            type="text"
            :disabled="scheduleValid(scope.row)"
            @click="()=>{showSchedule(scope.row)}"
          >
            <i class="zeta-icon-history" />
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-if="allHistory && allHistory.length > 0"
      class="pager"
      layout="prev, pager, next"
      :total="pageLength"
      :page-size="pageSize"
      :current-page.sync="pageIndex"
    />
    <!-- popup -->
    <el-dialog
      title="Data Validation SQL"
      :visible.sync="sqlDialogVisible"
      custom-class="sql-dialog"
      @closed="sqlDialogClose"
    >
      <div
        v-if="query && query !==''"
        class="dialog-child read-only-code-mirror"
      >
        <el-radio-group
          v-model="platform"
          size="small"
        >
          <el-radio-button
            v-for="item in query"
            :key="item.Platform"
            :label="item.Platform"
            border
          >
            {{ item.Platform }}
          </el-radio-button>
        </el-radio-group>
        <CodeDisplay
          :value="sql"
          :options="editorOptions"
        />
      </div>
      <div v-else>
        Did not find SQL
      </div>
    </el-dialog>
    <el-dialog
      title="Data Validation Result"
      :visible.sync="dialogVisible"
      custom-class="sql-dialog"
      @closed="resultDialogClose"
    >
      <div class="dialog-child">
        <div class="dialog-table-title">
          Row Count
        </div>
        <el-table
          v-if="validateResult.rowCnt"
          :data="[validateResult.rowCnt]"
          :row-class-name="checkColClassName"
          border
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
        <div class="dialog-table-title">
          Sum Check
        </div>
        <el-table
          v-if="validateResult.sumCheck"
          :data="validateResult.sumCheck"
          :default-sort="{prop: 'columnName', order: 'ascending'}"
          :row-class-name="checkColClassName"
          border
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
          />
          <el-table-column
            :formatter="colValueformatter"
            :label="validateResult.targetPlatform"
            prop="target"
          />
        </el-table>
      </div>
    </el-dialog>
    <el-dialog
      title="Data Validation Error Log"
      :visible.sync="errorDialogVisible"
      custom-class="log-dialog"
      @closed="logDialogClose"
    >
      <div
        v-if="log && log !==''"
        class="dialog-child"
      >
        {{ log }}
      </div>
      <div
        v-else
        class="dialog-child"
      >
        Data Validation Service is not working now. Please try later or contact
        <a
          target="_blank"
          href="mailto:DL-eBay-ZETA@ebay.com"
        >DL-eBay-ZETA@ebay.com</a>!
      </div>
    </el-dialog>
    <el-dialog
      title
      :visible.sync="scheduleDialogVisible"
      custom-class="schedule-dialog"
      width="540px"
      top="12vh"
      @close="scheduleDialogClose"
    >
      <schedule-container
        v-if="scheduleDialogVisible"
        type="DataValidate"
        :schedule-info="scheduleConfig"
        @visableChange="handleScheduleVisableChange"
      />
    </el-dialog>
    <div
      v-if="!(allHistory && allHistory.length > 0)"
      class="nodata"
    >
      No Data
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Emit, Watch, Inject, Provide } from 'vue-property-decorator';
import _ from 'lodash';
import moment from 'moment';
// import { TDTable, HerculesTable } from '@/components/DataValidation/DataValidation';
import Util from '@/services/Util.service';
import ScheduleContainer, {
  DataValidationSchedule, ScheduleConfig,
} from '@/components/common/schedule-container';
import DVRemoteService from '@/services/remote/DataValidation';
import CodeDisplay from '@/components/common/Visualization/CodeDisplay.vue';
import { TimePickerResult } from '@/components/common/time-picker';
import ScheduleUtil from '@/components/Schedule/util';
export interface ValidationResultRow {
  columnName: string;
  src: number | undefined;
  target: number | undefined;
}
export interface ValidationResult {
  rowCnt: ValidationResultRow | undefined;
  sumCheck: Array<ValidationResultRow>;
  srcPlatform: string;
  targetPlatform: string;
}
const PLATFORMMAP: Dict<string> = {'mozart_lvs': 'numozart'};
@Component({
  components: {
    CodeDisplay,
    ScheduleContainer,
  },
  filters: {
    numozartAdapter: (plat: string) => {
      if (plat === 'mozart_lvs') {
        return 'mozart';
      }
      return plat;
    },
  },
})
export default class History extends Vue {
  @Inject('dvRemoteService')
  dvRemoteService: DVRemoteService;

  validateResult: ValidationResult = {
    rowCnt: undefined,
    sumCheck: [],
    srcPlatform: '',
    targetPlatform: '',
  };
  editorOptions = {
    // codemirror options
    tabSize: 4,
    mode: 'text/x-mysql',
    lineNumbers: true,
    smartIndent: false,
    matchBrackets: true,
    autofocus: true,
    indentWithTabs: true,
    readOnly: true,
  };
  query: any= '';
  public sqlDialogVisible = false;
  platform='';
  sql = '';
  log = '';
  public errorDialogVisible = false;
  public dialogVisible = false;
  public pageSize = 5;
  public pageIndex = 1;
  scheduleDialogVisible = false;
  scheduleConfig: ScheduleConfig | null = null;

  get historyList () {
    const allHistory = _.cloneDeep(
      this.$store.getters.dataValidation.historyList
    );
    const list = allHistory.slice(
      (this.pageIndex-1) * this.pageSize,
      (this.pageIndex) * this.pageSize
    );
    const statusArr = ['In Progress', 'Succeed', 'Failed'];
    const statusClassArr = ['progress', 'succeed', 'failed'];
    list.map((item: any) => {
      item.createDate = moment(item.createDate).format(
        'YYYY-MM-DD HH:mm'
      );
      item.startTime = item.startTime
        ? moment(item.startTime).format('YYYY-MM-DD HH:mm')
        : null;
      item.endTime = item.endTime
        ? moment(item.endTime).format('YYYY-MM-DD HH:mm')
        : null;
      item.statusString = statusArr[item.status];
      item.statusClass = statusClassArr[item.status];
    });
    return list;
  }
  get allHistory () {
    return this.$store.getters.dataValidation.historyList;
  }
  get loading () {
    return this.$store.getters.dataValidation.historyLoading;
  }

  get pageLength () {
    return this.$store.getters.dataValidation.historyList.length;
  }

  tableRowClassName ({ row, rowIndex }: any) {
    if (row.status === 2) {
      return 'failed-row';
    }
    return '';
  }
  checkColClassName ({ row, rowIndex }: any) {
    if (row.src !== row.target) {
      return 'failed-row';
    }
    return '';
  }
  // show SQL function
  showSql (row: any) {
    this.sqlDialogVisible = true;
    if (row.dataValidateDetail){
      this.handleSql(row);
      return;
    }
    this.dvRemoteService.getDetail(row.historyId).then((res) => {
      if (res && res.data && res.data != null){
        row.dataValidateDetail = res.data.dataValidateDetail;
        this.handleSql(row);
      }
    }).catch(e => {
      throw e;
    });
  }
  handleSql (row: any){
    this.query = JSON.parse(row.dataValidateDetail.query);
    this.query.map((item: any) =>{
      item.sql = Util.SQLFormatter(item.SQL);
      if (item.Platform.toLowerCase() === 'mozart_lvs'){
        item.Platform  = PLATFORMMAP['mozart_lvs'].toUpperCase();
      }
    });
    this.platform = this.query[0].Platform;
  }
  sqlDialogClose () {
    this.query = '';
  }
  @Watch('platform')
  handlePlatform (newVal: string){
    this.sql = this.query.filter((item: any, index: number) =>item.Platform===newVal)[0].sql;
  }
  // show SQL function
  showResult (row: any) {
    this.dialogVisible = true;
    if (row.dataValidateDetail){
      this.handleResult(row);
      return;
    }
    this.dvRemoteService.getDetail(row.historyId).then((res) => {
      if (res && res.data && res.data != null){
        row.dataValidateDetail = res.data.dataValidateDetail;
        this.handleResult(row);
      }
    }).catch(e => {
      throw e;
    });
  }
  handleResult (row: any){
    let result = row.dataValidateDetail.result;
    try {
      result = JSON.parse(result);
      const platforms: any[] = [row.sourcePlatform, row.targetPlatform];
      const plat0 = platforms[0],
        plat1 = platforms[1];
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
                                  : null,
            });
          }
        })
        .value();
      this.validateResult.rowCnt = rowCount;
      this.validateResult.sumCheck = resultArr;
      this.validateResult.srcPlatform = plat0.toLowerCase()==='mozart_lvs'?'mozart':plat0;
      this.validateResult.targetPlatform = plat1.toLowerCase()==='mozart_lvs'?'mozart':plat1;
    } catch (e) {
      // eslint-disable-next-line no-console
      console.error(e);
    }
  }
  resultDialogClose () {
    this.validateResult = {
      rowCnt: {
        columnName: 'ROWCOUNT',
        src: 0,
        target: 0,
      },
      sumCheck: [],
      srcPlatform: '',
      targetPlatform: '',
    };
  }
  colValueformatter (row: any, column: string, val: string) {
    if (val === null) {
      return 'N/A';
    } else {
      return val;
    }
  }
  // move again function
  moveClick (row: any) {
    if (row.dataValidateDetail){
      this.handleRedo(row);
      return;
    }
    this.dvRemoteService.getDetail(row.historyId).then((res) => {
      if (res && res.data && res.data != null){
        row.dataValidateDetail = res.data.dataValidateDetail;
        this.handleRedo(row);
      }
    }).catch(e => {
      throw e;
    });
  }
  handleRedo (row: any){
    const srcTable: DataValidation.TDTable = {
      source: row.sourcePlatform,
      table: row.sourceTable,
      filter: row.dataValidateDetail.sourceFilter,
      batchAccount: row.dataValidateDetail.sourceBatchAccount,
    };
    const tgtTable: DataValidation.HerculesTable = {
      source: row.targetPlatform,
      table: row.targetTable,
      filter: row.dataValidateDetail.targetFilter,
      batchAccount: row.dataValidateDetail.targetBatchAccount,
    };
    this.moveAgain(srcTable, tgtTable);
  }
  showLog (row: any) {
    this.errorDialogVisible = true;
    this.log = row.log;
  }
  logDialogClose () {
    this.log = '';
  }
  @Emit('moveAgain')
  moveAgain (
    srcTable: DataValidation.TDTable,
    tgtTable: DataValidation.HerculesTable
  // eslint-disable-next-line @typescript-eslint/no-empty-function
  ) {}

  scheduleValid (row: any) {
    return row.statusClass === 'failed'
    || row.status != 1
    || row.sourcePlatform === 'mozart'
    || row.sourcePlatform === 'mozart_lvs'
    || row.sourcePlatform === 'hopper'
    || row.targetPlatform === 'hopper';
  }
  showSchedule (row: any) {
    if (row.statusClass !== 'succeed') {
      return;
    }
    if (row.dataValidateDetail){
      this.handleSchedule(row);
      return;
    }
    this.dvRemoteService.getDetail(row.historyId).then((res) => {
      if (res && res.data && res.data != null){
        row.dataValidateDetail = res.data.dataValidateDetail;
        this.handleSchedule(row);
      }
    }).catch(e => {
      throw e;
    });
  }
  handleSchedule (row: any){
    const task: DataValidationSchedule = {
      history: {
        nT: row.nT,
        sourceTable: row.sourceTable,
        targetTable: row.targetTable,
        sourcePlatform: row.sourcePlatform,
        targetPlatform: row.targetPlatform,
      },
      sourceFilter: row.dataValidateDetail.sourceFilter,
      targetFilter: row.dataValidateDetail.targetFilter,
      sourceBatchAccount: row.dataValidateDetail.sourceBatchAccount,
      targetBatchAccount: row.dataValidateDetail.targetBatchAccount,
    };
    this.scheduleConfig = ScheduleUtil.getDefaultConfig('DataValidate', task);
    this.scheduleDialogVisible = true;
  }

  scheduleDialogClose () {
    this.scheduleDialogVisible = false;
    this.scheduleConfig = null;
  }

  handleScheduleVisableChange (val: boolean) {
    this.scheduleDialogVisible = val;
  }
  @Watch('loading')
  handleLoadig (){
    this.pageIndex = 1;
  }
}
</script>

<style lang="scss" scoped>
@import "@/styles/global.scss";
$succeed: #5cb85c;
$progress: #f3af2b;
$failed: #e53917;
$failedBg: rgba(229, 57, 23, 0.0980392156862745);
div.flex-center {
    display: flex;
    align-items: center;
}
// span.cycle {
//     display: inline-block;
//     height: 10px;
//     width: 10px;
//     border-radius: 5px;
//     margin-right: 5px;
//     &.progress {
//         background-color: $progress;
//     }
//     &.succeed {
//         background-color: $succeed;
//     }
//     &.failed {
//         background-color: $failed;
//     }
// }
span.status {
    &.progress {
        color: $progress;
    }
    &.succeed {
        color: $succeed;
    }
    &.failed {
        color: $failed;
    }
}
span.is-new {
    color: red;
}

.history {
    // height: 300px;
    .pager {
        margin: 15px 0;
        text-align: right;
    }
}
.nodata {
    width: 100%;
    height: 100%;
    text-align: center;
    font-size: 30px;
    display: flex;
    align-items: center;
    justify-content: center;
    min-height: 300px;
    color: $zeta-global-no-data;
}
.el-dialog__wrapper {
    & /deep/ .el-dialog__header {
        padding: 30px 30px 0px 30px;
    }
    & /deep/ .el-dialog__body {
        margin: 15px 30px 0 30px;
        padding: 0 0 30px 0;
        max-height: 500px;
        .dialog-child {
            max-height: 470px;
            overflow-y: auto;
        }
    }
    & /deep/ .log-dialog .el-dialog__body {
        word-break: break-word;
        white-space: pre-line;
        line-height: 25px;
    }
    & /deep/ .schedule-dialog {
        .el-dialog__header {
            padding: 0;
        }
        .el-dialog__body {
            max-height: none;
            padding: 20px;
            margin: 0;
        }
        .row-info {
          .disable{
            max-width: 340px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }
        }
    }
}

.dialog-table-title {
    font-size: 14px;
    font-weight: 600;
    margin-bottom: 10px;
    // margin-left: 5px;
    margin-top: 15px;
}
.code-display{
    margin: 10px 0;
}
/deep/ .el-radio-group{
    .el-radio-button{
        margin-right: 10px;
        &.is-active{
            .el-radio-button__inner{
                border-left: 1px solid #569ce1;
            }
        }
        .el-radio-button__inner{
            border-left: 1px solid #dcdfe6;
            border-radius: 4px;
        }
    }
}
</style>
