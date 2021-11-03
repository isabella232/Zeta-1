<template>
    <div class="history">
        <div class="table-container" v-loading="loading">
            <el-table
                class="history-table"
                v-if="allHistory && allHistory.length > 0"
                :data="historyList"
                border
                style="width: 100%"
                :row-class-name="tableRowClassName"
            >
                <el-table-column :width="100" label="Source">
                    <template slot-scope="scope">
                        <span>{{scope.row.sourcePlatform | hermesAdapter | numozartAdapter}}</span>
                    </template>
                </el-table-column>
                <el-table-column :width="200" prop="sourceTable" label="Source Table">
                    <template slot-scope="scope">
                        <el-popover
                            v-if='scope.row.type === 4 || scope.row.type === 5 '
                            placement="right"
                            trigger="click"
                            visible-arrow="false"
                            width="370"
                            @hide="hide">
                            <div>
                                <pl-table :data="scope.row.originSourceVdmTableData" border
                                max-height="200"
                                :row-height="rowHeight"
                                use-virtual>
                                    <pl-table-column  property="name" label="Table"></pl-table-column>
                                </pl-table >
                            </div>
                            <span slot="reference" class="vdm-table-name">{{scope.row.type === 4?'VDM mutil-tables':'VDM mutil-views'}}</span>
                        </el-popover>
                        <span v-if="scope.row.type !== 4 && scope.row.type !== 5">{{scope.row.sourceTable}}</span>
                    </template>
                </el-table-column>
                <el-table-column :width="150" prop="targetPlatform" label="Target">
                  <template slot-scope="scope">
                        <span>{{scope.row.targetPlatform | hermesAdapter }}</span>
                    </template>
                </el-table-column>
                <el-table-column :width="250" prop="targetTable" label="Target Table">
                    <template slot-scope="scope">
                        {{scope.row.targetTable}}
                        <span class="is-new" v-if="scope.row.isNew">(new)</span>
                    </template>
                </el-table-column>
                <el-table-column label="Runtime">
                    <template slot-scope="scope">
                        <template v-if="scope.row.startTime">
                            <div>
                                {{scope.row.startTime}}
                                - {{scope.row.endTime ? scope.row.endTime : ''}}
                            </div>
                        </template>
                        <template v-else>
                            <div>N/A</div>
                        </template>
                    </template>
                </el-table-column>
                <el-table-column prop="createDate" label="Create Date"></el-table-column>
                <el-table-column label="Status">
                    <template slot-scope="scope">
                        <div class="flex-center">
                            <span
                                class="status"
                                v-bind:class="scope.row.statusClass"
                            >{{scope.row.statusString}}</span>
                            <i
                                v-if="scope.row.status === 2"
                                title="Error Log"
                                @click="()=>{showLog(scope.row)}"
                                class="el-icon-error"
                            />
                        </div>
                    </template>
                </el-table-column>
                <el-table-column :width="90" header-align="center" align="center" label="SQL">
                    <template slot-scope="scope">
                        <el-button title="Show SQL" type="text"
                            @click="()=>{showSql(scope.row)}"
                            :disabled="scope.row.type!=1"
                        >
                            <i class="zeta-icon-sql zeta-icon"></i>
                        </el-button>
                    </template>
                </el-table-column>
                <el-table-column label="Redo" header-align="center" align="center" :width="90">
                    <template slot-scope="scope">
                        <el-button title="Move Again" type="text"
                          @click="()=>{moveClick(scope.row)}"
                          :disabled="scope.row.type!==4"
                        >
                          <i class="zeta-icon-dailyMove1 zeta-icon"></i>
                        </el-button>
                    </template>
                </el-table-column>
                <el-table-column label="Schedule" header-align="center" align="center" :width="90">
                    <template slot-scope="scope">
                        <el-button
                            type="text"
                            :disabled="true"
                            @click="()=>{showSchedule(scope.row)}"
                            title="Schedule"
                        >
                            <i class="zeta-icon-history zeta-icon"></i>
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
            ></el-pagination>
            <div v-if="!(allHistory && allHistory.length > 0)" class="nodata">No Data</div>
        </div>
        <!-- popup -->
        <el-dialog
            title="Data Move SQL"
            :visible.sync="dialogVisible"
            custom-class="sql-dialog"
            @closed="sqlDialogClose"
        >
            <div v-if="sql && sql !==''" class="dialog-child read-only-code-mirror">
                <CodeDisplay v-model="sql" :options="editorOptions"></CodeDisplay>
            </div>
            <div v-else>Did not find SQL</div>
        </el-dialog>

        <el-dialog
            title="Move Failure"
            :visible.sync="errorVDMDialogVisible"
            custom-class="vdm-log-dialog"
            @closed="vdmDialogCloseCallback">
            <div class="dialog-body">
                <div class="dialog-info">All or some of your move job failed, please check.</div>
                <div class="header">
                    <div class="btn-group">
                        <el-radio-group v-model="vmdDialogRadio" size="small">
                            <el-radio-button label="Succeed"></el-radio-button>
                            <el-radio-button label="Failed"></el-radio-button>
                        </el-radio-group>
                    </div>
                    <div class="link">
                        <p>Customer Support - Open ticket <a style="cursor: pointer" target="_blank" :href="jiraLink">here</a></p>
                        <div v-if="activeRow&&activeRow.type===4" class="retry"><i :class="{'el-icon-loading':retryLoading}" /><span class="retry-info" @click="retry"> Retry Failed Jobs</span></div>
                    </div>
                </div>
                <div class="table-section">
                    <el-table
                    :data="vdmTableData"
                    border
                    max-height="370"
                    style="width: 100%">
                        <el-table-column :label="activeRow&&activeRow.type===5?'View(s)':'Table (Job Title)'" prop="name"></el-table-column>
                        <el-table-column label="Reason" prop="reason"></el-table-column>
                        <template slot="empty">
                            {{activeRow&&activeRow.type===5?"No views found":"No tables found"}}
                        </template>
                    </el-table>
                    <el-pagination
                        v-if="vdmTableData && vdmTableData.length > 0"
                        class="pager"
                        layout="prev, pager, next"
                        :total="vdmTablePageLength"
                        :page-size="vdmTablePageSize"
                        :current-page.sync="vdmTablePageIndex"
                    ></el-pagination>
                </div>
            </div>

        </el-dialog>

        <el-dialog
            title="Data Move Error Log"
            :visible.sync="errorDialogVisible"
            custom-class="log-dialog"
            @closed="logDialogClose"
        >
            <div class="dialog-child" v-if="log && log !==''">{{log}}</div>
            <div class="dialog-child" v-else>Did not find Error Log</div>
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
                type="DataMove"
                :scheduleInfo="scheduleConfig"
                v-if="scheduleDialogVisible"
                @visableChange="handleScheduleVisableChange"
            />
        </el-dialog>

    </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Emit, Inject, Watch } from "vue-property-decorator";
import _ from "lodash";
import moment from "moment";
import Util from "@/services/Util.service";
import ScheduleContainer, {
    DataMoveSchedule, ScheduleConfig
} from "@/components/common/schedule-container";
import CodeDisplay from '@/components/common/Visualization/CodeDisplay.vue'
import DMRemoteService from '@/services/remote/DataMove'
import { TimePickerResult } from "@/components/common/time-picker";
import { Mode } from './internal';
import 'pl-table/themes/index.css';
import { PlTable, PlTableColumn, PlxTableGrid, PlxTableColumn } from 'pl-table';
import ScheduleUtil from '@/components/Schedule/util';

type VDMTableStatus = 'Succeed' | 'Failed';
interface VDMTableFailInfo {
    name: string;
    status: string,
    reason: string
}
@Component({
    components: {
        CodeDisplay,
        ScheduleContainer,
        PlTable,
        PlTableColumn
    },
    filters: {
      hermesAdapter: (plat: string) => {
        if(plat === 'hermesrno'){
          return 'hermes'
        }
        return plat;
      },
      numozartAdapter: (plat: string) => {
        if (plat === 'numozart') {
          return 'mozart';
        }
        return plat;
      }
    }
})
export default class History extends Vue {
    @Inject('dmRemoteService')
    dmRemoteService: DMRemoteService
    editorOptions = {
        // codemirror options
        tabSize: 4,
        mode: "text/x-mysql",
        lineNumbers: true,
        smartIndent: false,
        matchBrackets: true,
        autofocus: true,
        indentWithTabs: true,
        readOnly: true
    };
    sql = "";
    public dialogVisible = false;
    log = "";
    public errorDialogVisible = false;
    public errorVDMDialogVisible = false;
    public pageSize = 5;
    public pageIndex = 1;
    scheduleDialogVisible: boolean = false;
    scheduleConfig: ScheduleConfig | null = null;
    appendToBody = false;
    vmdDialogRadio = "Failed";
    retryLoading = false;
    activeRow: any = null;
    // Used in VDM fail dialog window
    vdmTableData: Array<VDMTableFailInfo> = [];
    originVdmTableData: Array<VDMTableFailInfo> = [];
    rowHeight = 40;
    vdmTablePageSize = 15;
    vdmTablePageIndex = 1;
    vdmTablePageLength = 0;

    get jiraLink() {
        return 'https://jirap.corp.ebay.com/secure/CreateIssue.jspa?pid=26775&issuetype=46&Create=Create';
    }
    get historyList() {
        let allHistory = _.cloneDeep(this.$store.getters.dataMove.historyList);
        let list = allHistory.slice(
            (this.pageIndex-1) * this.pageSize,
            (this.pageIndex) * this.pageSize
        );
        const statusArr = ["In Progress", "Succeed", "Failed"];
        const statusClassArr = ["progress", "succeed", "failed"];
        list.map((item: any) => {
            item.sourcePlatform = (item.type!==3) ? item.sourcePlatform : 'File';
            item.createDate = moment(item.createDate).format(
                "YYYY-MM-DD HH:mm"
            );
            item.startTime = item.startTime
                ? moment(item.startTime).format("HH:mm")
                : null;
            item.endTime = item.endTime
                ? moment(item.endTime).format("HH:mm")
                : null;
            item.statusString = statusArr[item.status];
            item.statusClass = statusClassArr[item.status];
        });
        return list;
    }
    get allHistory() {
        return this.$store.getters.dataMove.historyList;
    }
    get loading() {
        return this.$store.getters.dataMove.historyLoading;
    }
    get pageLength() {
        return this.$store.getters.dataMove.historyList.length;
    }
    scheduleValid(row){
      return row.statusClass === 'failed'
        || row.status != 1
        || row.type === 3
        || row.type === 5
        || row.sourcePlatform === 'mozart'
        || row.sourcePlatform === 'hopper';
    }
    tableRowClassName({ row, rowIndex }: any) {
        if (row.status === 2) {
            return "failed-row";
        }
        return "";
    }

    // show SQL function
    showSql(row: any) {
        this.dialogVisible = true;
        if(row.dataMoveDetail){
            this.sql = Util.SQLFormatter(row.dataMoveDetail.query);
            return;
        }
        this.dmRemoteService.getDetail(row.historyId).then((res) => {
            if(res && res.data && res.data != null){
                row.dataMoveDetail = res.data.dataMoveDetail;
                this.sql = Util.SQLFormatter(res.data.dataMoveDetail.query);
              }
        }).catch(e => {
            throw e;
        })
    }
    sqlDialogClose() {
        this.sql = "";
    }

    showLog(row: any) {
        this.activeRow = row;
        if(row.log && (row.type === 4 || row.type === 5) && typeof row.log === 'object') { //VDM table and log is a map value
            this.errorVDMDialogVisible = true;
            // populate dialog required data
            // TODO: error tables is missing. this is not correct
            let sourceTables = _.map(row.sourceTable,(table:string) => {
              return row.type === 4 ? table : table.split('.')[1];
            });
            let failTables = _.keys(row.log);
            let suceessTables = _.difference(sourceTables, failTables);
            let successTableData = _.map(suceessTables, (d:string) => {
                let name = d;
                if(row.type ===  5){
                  if(row.viewDb){
                    name = `${row.viewDb}.${this.parseView(d)}`
                  }else{
                    name = this.parseView(d);
                  }
                }
                return {
                    name: name,
                    status: 'Succeed',
                    reason: 'Job Complete Successfully'
                }
            });
            let failTableData = _.map(failTables, (d:any) => {
                let name = d;
                if(row.type === 5){
                  if(row.viewDb){
                    name = `${row.viewDb}.${d}`;
                  }
                }
                return {
                    name: name,
                    status: 'Failed',
                    reason: row.log[d]
                }
            });
            this.originVdmTableData = successTableData.concat(failTableData);
            this.setVDMTableData();
        } else {
            this.errorDialogVisible = true;
            this.log = row.log;
        }
    }
    logDialogClose() {
        this.log = "";
    }
    parseView(str: string){
      const arr = str.split('.');
      if ( arr.length > 1 ) {
        return arr[1];
      }
      return str;
    }
    // move again function
    moveClick(row: any) {
      console.log('dss');
        if(row.dataMoveDetail){
            this.handleMove(row);
            return;
        }
        this.dmRemoteService.getDetail(row.historyId).then((res) => {
            if(res && res.data && res.data != null){
                row.dataMoveDetail = res.data.dataMoveDetail;
                this.handleMove(row);
            }else{
                this.$message.error("no response data available");
            }
        }).catch(e => {
            throw e;
        })
    }
    handleMove(row: any){
        let table = row.sourceTable;
        let vdmDataBase = '';
        let vdmTableName = '';
        let vdmViewName = '';
        let mode = Mode.TD;
        if(row.type === 4 || row.type === 5 ) {
            table = '';
            let source = _.map(row.sourceTable, d => {
                return d.split(".")[1]
            })
            vdmDataBase = row.sourceTable[0].split(".")[0];
            if(row.type === 5){
              vdmViewName = source.join(";")
            }else{
              vdmTableName = source.join(";");
            }

            mode = Mode.VDM;
        }
        let srcTable: DataMove.SourceTable = {
            vdmTableOption: {
                source: row.sourcePlatform,
                database: vdmDataBase,
                tableName: vdmTableName,
                viewName: vdmViewName
            },
            tdTableOption: {
                source: row.sourcePlatform,
                fullTableName: table,
                filter: row.dataMoveDetail.filter,
            }
        }
        let override = Boolean(
            row.dataMoveDetail && row.dataMoveDetail.isDrop === 1 ? true : false
        );
        let convert = Boolean(
            row.dataMoveDetail && row.dataMoveDetail.isConvert === 1 ? true : false
        );
        let tgtTable: DataMove.HerculesTable = {
            source: row.targetPlatform,
            table: row.targetTable,
            override: override,
            convert: convert
        };
        this.moveAgain(srcTable, tgtTable, mode);
    }
    @Emit("moveAgain")
    moveAgain(srcTable: DataMove.SourceTable, tgtTable: DataMove.HerculesTable, mode: string) {}

    showSchedule(row: any) {
        if (row.statusClass !== "succeed") {
            return;
        }
        if(row.dataMoveDetail){
            this.handleSchedule(row);
            return;
        }
        this.dmRemoteService.getDetail(row.historyId).then((res) => {
            if(res && res.data && res.data != null){
                row.dataMoveDetail = res.data.dataMoveDetail;
                this.handleSchedule(row);
            }
        }).catch(e => {
            throw e;
        })
    }
    handleSchedule(row: any){
        let isConvert = (row.dataMoveDetail && row.dataMoveDetail.isConvert) ? row.dataMoveDetail.isConvert:0;
        const task: DataMoveSchedule = {
            filter: row.dataMoveDetail.filter,
            history: {
                nT: row.nT,
                sourceTable: _.isArray(row.sourceTable) ? row.sourceTable.join(";") : row.sourceTable,
                sourcePlatform: row.sourcePlatform,
                targetTable: row.targetTable,
                targetPlatform: row.targetPlatform,
                type: row.type
            },
            isDrop: 1,
            isConvert: isConvert
        };
        this.scheduleConfig = ScheduleUtil.getDefaultConfig('DataMove', task);
        this.scheduleDialogVisible = true;
    }

    scheduleDialogClose() {
        this.scheduleDialogVisible = false;
        this.scheduleConfig = null;
    }
    handleScheduleVisableChange(val: boolean) {
        this.scheduleDialogVisible = val;
    }
    reloadHis(option?:any) {
        this.$store.dispatch('setDMHistoryLoading');
        this.dmRemoteService.history(Util.getNt()).then((res) => {
            if(res && res.data && res.data != null){
                let historyList = (res && res.data ) || [];
                this.processHisList(historyList);
                // sort by createDate
                if(!(option && option.retryVDM))  this.pageIndex = 1;
                historyList = _.sortBy(historyList, [(o:any) => {return o.createDate} ]).reverse()
                this.$store.dispatch('setDMHistoryFinish', {historyList});

              }
        }).catch(e => {
            this.$store.dispatch('setDMHistoryFinish', {historyList: []});
            throw e;
        })

    }

    processHisList(historyList: Array<any>) {
        _.each(historyList, historyItem => {
            if(historyItem.type === 4 || historyItem.type === 5 ) { // VDM Table

                // handle dirty data
                try {
                    historyItem.sourceTable = JSON.parse(historyItem.sourceTable);
                }
                catch(e) {
                }
                // handle VDM log
                try {
                    historyItem.log = JSON.parse(historyItem.log);
                }
                catch(e) {
                }

                historyItem.originSourceVdmTableData = _.map(historyItem.sourceTable, d =>  {
                    return {name: d};
                });
            }
        });
    }

    retry() {
        this.retryLoading = true;
        this.dmRemoteService.retryVDMFailJobs(this.activeRow.historyId).then((res) => {
            if(res && res.data && res.data == 'Success'){
                this.retryLoading = false;
                this.errorVDMDialogVisible = false;
                this.reloadHis({retryVDM: true});
            }
        }).catch(e => {
            console.error(e);
        })
    }

    setVDMTableData() {
        let allVdmTableData = _.filter(this.originVdmTableData, (item:any) => {
                return item.status == this.vmdDialogRadio;
        });
        this.vdmTableData = allVdmTableData.slice(
        (this.vdmTablePageIndex-1) * this.vdmTablePageSize,
        (this.vdmTablePageIndex) * this.vdmTablePageSize);
        this.vdmTablePageLength = allVdmTableData.length;
    }

    @Watch("vmdDialogRadio")
    showVDMTableItems(newValue: string) {
        this.vdmTablePageIndex = 1;
        if(newValue != '') {
            this.setVDMTableData();
        }
    }

    @Watch('vdmTablePageIndex')
    vdmTablePageSizeChange(newVal, oldVal) {
        this.setVDMTableData();
    }

    vdmDialogCloseCallback() {
        this.vdmTablePageIndex = 1;
    }

    hide() {
        var lists = document.querySelectorAll(".el-table__virtual-wrapper");
        if(lists && lists.length) {
            [].forEach.call(lists, function(el: HTMLElement) {
                let child = <HTMLElement>el.firstElementChild;
                child && (child.style.transform = 'translateY(0px)');
            });
        }
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
    > span.status {
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
    > i.el-icon-error {
        cursor: pointer;
        line-height: 14px;
    }
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

span.is-new {
    color: red;
}
.el-table {
    // /deep/ .failed-row {
    // td:nth-child(-n + 6) {
    // background-color: $failedBg;
    // }
    // }
    // /deep/ th {
    //     padding: 6px 0;
    // }
    // /deep/ td {
    //     padding: 3px 0;
    // }
    /deep/ .invisible-col {
        display: none;
    }
}
.history {
    //   height: 300px;
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
    min-height: 200px;
    color: $zeta-global-no-data;
}
// .el-dialog__wrapper /deep/ .sql-dialog .el-dialog__body {
//     height: 500px;
//     overflow-y: auto;
// }
// .el-dialog__wrapper /deep/ .log-dialog .el-dialog__body {
//     height: 500px;
//     overflow-y: auto;
//     word-break: break-word;
//     white-space: pre-line;
// }
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
    & /deep/ .sql-dialog .el-dialog__body {
        .dialog-child {
            border: 1px solid $zeta-global-color-disable;
        }
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
    margin-left: 5px;
    margin-top: 15px;
}
i.zeta-icon {
    font-size: 20px;
}

.vdm-log-dialog {
    .dialog-body {
        // max-height: 470px;
        // overflow-y: auto;
        .dialog-info {
            margin-bottom: 10px;
        }
        .table-section {
            margin-top: 20px;
        }
    }
    .header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        .btn-group {
            width: 150px;
        }
        .link {
            margin-right: 5px;
            font-size: 12px;
            color: #666;

            .retry {
                margin-top: 5px;
                text-align: right;
                .retry-info {
                    color: #569ce1;
                    cursor: pointer;
                }

            }
        }
    }
}
.vdm-table-name {
    color:#569ce1;
    cursor: pointer;
}
/deep/ .el-popover {
    max-height: 300px;
    overflow-y: auto;
}
</style>
