<template>
  <div id="dataMove">
    <div class="migration">
      <div class="source">
        <div class="label label-default">
          1.Select source
        </div>
        <div class="info">
          Zeta data move not available for Teradata to hadoop.
        </div>
        <div style="margin-top: 15px; margin-bottom: 15px; padding: 0 5px;">
          <span style="margin-right: 15px">Mode</span>
          <el-radio-group v-model="mode">
            <el-radio :label="enumMode.VDM">
              VDM Move
            </el-radio>
            <!-- <el-radio :label="enumMode.TD">
              TD Move
            </el-radio> -->
            <el-radio :label="enumMode.UPLOAD">
              Upload File
            </el-radio>
          </el-radio-group>
        </div>
        <source-table
          ref="src"
          :mode="mode"
          @td-table-change="tdTableChange"
          @is-table-valid="setSourceTableValid"
          @file-change="fileChange"
          @vdm-change="vdmChange"
        />
      </div>
      <div class="target">
        <div class="label label-default">
          2.Select destination
        </div>
        <target-table
          ref="tgt"
          :has-file="hasFile"
          :source-table="tdFullTableName"
          :vdm-table="vdmTable"
          :mode="mode"
          @is-table-valid="setTargetTableValid"
        />
      </div>
    </div>
    <div class="action">
      <el-button
        type="primary"
        :loading="migrationLoading"
        :disabled="migrationDisabled"
        @click="migration"
      >
        Confirm
      </el-button>
      <el-button
        type="default"
        plain
        @click="reset"
      >
        Reset
      </el-button>
    </div>
    <div class="history">
      <div class="label">
        Data Move History
        <i
          :class="{'el-icon-refresh':!hisLoading,'el-icon-loading':hisLoading}"
          @click="()=> refreshHis()"
        />
      </div>
      <history
        ref="history"
        @moveAgain="applyHistory"
      />
    </div>
    <el-dialog
      ref="dialog"
      title="Table(s) already exist in Hermes LVS"
      :visible.sync="dialogVisible"
      custom-class="table-dialog"
      width="500"
      :show-close="false"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
    >
      <div class="content">
        <div class="alert">
          <span style="color: red; font-weight: bold">Warning:</span> You are about to overwrite the below list of HDM tables in Hermes LVS. <strong>Please be aware that overwritten data cannot be recovered</strong>.
        </div>
        <div class="section">
          <el-table
            :data="existingTableData"
            border
            style="width: 100%"
            max-height="250"
          >
            <el-table-column
              prop="source"
              label="Source table(s) on Hermes RNO"
            />
            <el-table-column
              prop="target"
              label="Target table(s) on Hermes LVS"
            />
          </el-table>
        </div>
        <div class="dialog-footer">
          <el-button type="default" plain @click="dialogVisible = false">Cancel</el-button>
          <el-button type="primary" @click="confirmMove(true)">Confirm</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script lang="ts">
import { Component, Provide} from 'vue-property-decorator';
import Util from '@/services/Util.service';
import SourceTable from '@/components/DataMove/SourceTable.vue';
import TargetTable from '@/components/DataMove/TargetTable.vue';
import History from '@/components/DataMove/History.vue';
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import DMRemoteService from '@/services/remote/DataMove';
import { WorkspaceComponentBase } from '@/types/workspace';
import { Mode } from './internal';
import _ from 'lodash';

@Component({
  components: {
    SourceTable,
    TargetTable,
    History,
  },
})
export default class DataMoveComponent extends WorkspaceComponentBase {
  @Provide('notebookRemoteService')
  notebookRemoteService = new NotebookRemoteService()
  @Provide('dmRemoteService')
  dmRemoteService: DMRemoteService = new DMRemoteService()

  nt = '';
  migrationDisabled = true;
  isTargetTableValid = false;
  isSourceTableValid = false;
  mode = Mode.VDM;
  srcTblRef = this.$refs.src as SourceTable;
  tgtTblRef = this.$refs.tgt as TargetTable;
  tdFullTableName = '';
  migrationLoading = false;
  hasFile = false;
  enumMode = Mode;
  dialogVisible = false;
  existingTableData: Array<any> = [];
  overrideTables = [];
  vdmTable = {
    source: 'hopper',
    database: '',
    tableName: '',
    viewName: '',
  };

  get hisLoading () {
    return this.$store.getters.dataMove.historyLoading;
  }
  vdmChange (val: DataMove.VDMTable){
    this.vdmTable = val;
  }

  setSourceTableValid (val: {mode: string; valid: boolean}) {
    if (this.mode == val.mode) {
      this.isSourceTableValid = val.valid;
      this.shouldEnableConfirm();
    }
  }

  setTargetTableValid (isValid: boolean) {
    this.isTargetTableValid = isValid;
    this.shouldEnableConfirm();
  }

  shouldEnableConfirm (): void {
    this.migrationDisabled = !this.isTargetTableValid || !this.isSourceTableValid;
  }

  fileChange (val: boolean){
    this.hasFile = val;
  }

  mounted () {
    this.dmRemoteService.props({
      path: 'datamove',
      workspaceId: this.workspaceId,
    });
    this.notebookRemoteService.props({
      path: 'datamove',
      workspaceId: this.workspaceId,
    });
    this.nt = Util.getNt();
    (this.$refs.history as History).reloadHis();
    this.srcTblRef = this.$refs.src as SourceTable;
    this.tgtTblRef = this.$refs.tgt as TargetTable;
    this.reset();
  }

  reset () {
    this.srcTblRef.reset();
    this.tgtTblRef.reset();
    this.migrationDisabled = true;
    this.migrationLoading = false;
  }

  migration () {
    if (this.isTargetTableValid && this.isSourceTableValid) {
      const params = this.srcTblRef.getParams();
      const targetTable: DataMove.HerculesTable = this.tgtTblRef.migration() as DataMove.HerculesTable;
      if (this.mode == Mode.UPLOAD) {
        this.moveFromFile(params as DataMove.UploadParam,targetTable);
        return;
      } else if (this.mode == Mode.VDM){
        const sourceTable = params as DataMove.VDMTable;
        const override = targetTable.override;
        if (sourceTable.source === 'hermesrno' && sourceTable.tableName != '' && override){
          this.validateVDM(params as DataMove.VDMTable, targetTable);
        } else {
          this.moveFromVDM(params as DataMove.VDMTable, targetTable);
        }
        return;
      } else if (this.mode == Mode.TD) {
        this.moveFromTD(params as DataMove.TDTable, targetTable);
      }
    }
  }
  moveFromTD (sourceTable: DataMove.TDTable, targetTable: DataMove.HerculesTable) {
    this.migrationLoading = true;
    this.dmRemoteService.move(
      this.nt,
      sourceTable,
      targetTable,
      targetTable.override ? 1 : 0,
      targetTable.convert ? 1 : 0
    ).then(
      () => {
        this.reset();
        this.refreshHis();
        this.migrationLoading = false;
      },
      () => {
        this.migrationLoading = false;
      }
    );
  }
  moveFromFile (params: any, targetTable: DataMove.HerculesTable){
    this.migrationLoading = true;
    this.dmRemoteService.moveFromFile(
      this.nt,
      params.path,
      targetTable,
      targetTable.override ? 1 : 0,
      targetTable.convert ? 1 : 0,
      JSON.stringify(params.schema)
    ).then(
      () => {
        this.reset();
        this.refreshHis();
        this.migrationLoading = false;
      },
      () => {
        this.migrationLoading = false;
      }
    );
  }
  moveFromVDM (sourceTable: DataMove.VDMTable, targetTable: DataMove.HerculesTable, overrideTables = []){
    this.migrationLoading = true;
    this.dmRemoteService.movePlus(
      this.nt,
      sourceTable,
      targetTable,
      overrideTables,
    ).then(
      () => {
        this.reset();
        this.refreshHis();
        this.migrationLoading = false;
      },
      () => {
        this.migrationLoading = false;
      }
    );
  }
  applyHistory (srcTable: DataMove.SourceTable, tgtTable: DataMove.HerculesTable, mode: Mode) {
    this.mode = mode;
    this.$nextTick(() => {
      this.srcTblRef.apply(srcTable);
      setTimeout(() => {
        this.tgtTblRef.apply(tgtTable);
      }, 100);
    });
  }
  refreshHis () {
    (this.$refs.history as History).reloadHis();
  }
  tdTableChange (val: DataMove.TDTable) {
    this.tdFullTableName = val.fullTableName;
  }
  confirmMove (){
    this.dialogVisible = false;
    const params = this.srcTblRef.getParams();
    const targetTable: DataMove.HerculesTable = this.tgtTblRef.migration() as DataMove.HerculesTable;
    this.moveFromVDM(params as DataMove.VDMTable, targetTable, this.overrideTables);
  }
  validateVDM (sourceTable: DataMove.VDMTable, targetTable: DataMove.HerculesTable){
    this.migrationLoading = true;
    this.overrideTables = [];
    this.dmRemoteService.validateVDM(
      this.nt,
      sourceTable,
      targetTable
    ).then(
      (res) => {
        this.migrationLoading = false;
        if (res.data.length === 0){
          this.confirmMove();
        } else {
          this.dialogVisible = true;
          this.overrideTables = res.data;
          this.existingTableData = _.chain(res.data).map(tbName => {
            return {
              source: `${sourceTable.database}.${tbName}`,
              target: `${targetTable.srcDB}.${tbName}`,
            };
          }).value();
        }
      },
      () => {
        this.migrationLoading = false;
      }
    );
  }
}
</script>
<style lang="scss" scoped>
#dataMove {
    overflow-y: auto;
    .action,
    .migration {
        min-width: 900px;
        max-width: 1200px;
        margin: 35px auto;
        padding: 0 75px;
        display: flex;
        flex-direction: row;
        justify-content: space-between;
    }
    .action {
        display: flex;
        justify-content: flex-end;
    }
    .history {
        min-width: 1000px;
    }
    .info{
      margin-left: 5px;
		  color: #666;
		  font-size: 14px;
      font-weight: normal;
    }
}
.table-dialog{
  .content{
    padding-bottom: 20px;
    .alert{
      // color: #F3AF2B
      word-break: normal;
    }
    .section{
      margin: 20px 0;
      max-height: 300px;
      overflow-y: auto;
    }
    .dialog-footer{
      display: flex;
      justify-content: flex-end;
    }
  }
}
</style>

