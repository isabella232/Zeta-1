<template>
  <div id="validation">
    <div class="migration">
      <div class="source">
        <div class="label label-default">
          1.Select source
        </div>
        <source-table
          ref="srcTbl"
          @tableChange="sourceTableChange"
        />
      </div>
      <div class="target">
        <div class="label label-default">
          2.Select destination
        </div>
        <target-table
          ref="tgtTbl"
          :source-table="sourceTable"
        />
      </div>
    </div>
    <div class="action">
      <el-button
        type="primary"
        :loading="migrationLoading"
        @click="validate"
      >
        Compare
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
        Data Validation history
        <i
          :class="{'el-icon-refresh':!hisLoading,'el-icon-loading':hisLoading}"
          @click="()=> refreshHis()"
        />
      </div>
      <history @moveAgain="applyHistory" />
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Provide } from 'vue-property-decorator';
import Util from '@/services/Util.service';
import SourceTable from '@/components/DataValidation/SourceTable.vue';
import TargetTable from '@/components/DataValidation/TargetTable.vue';
import History from '@/components/DataValidation/History.vue';
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import DVRemoteService from '@/services/remote/DataValidation';
import { WorkspaceComponentBase } from '@/types/workspace';
import _ from 'lodash';

@Component({
  components: {
    SourceTable,
    TargetTable,
    History,
  },
})
export default class DataValidation extends WorkspaceComponentBase {

  @Provide('notebookRemoteService')
  notebookRemoteService = new NotebookRemoteService();
  @Provide('dvRemoteService')
  dvRemoteService: DVRemoteService = new DVRemoteService();

  nt = '';
  get hisLoading () {
    return this.$store.getters.dataValidation.historyLoading;
  }
  private srcTblRef: DataValidation.TableAPI = this.$refs.src as SourceTable;
  private tgtTblRef: DataValidation.TableAPI = this.$refs.tgt as TargetTable;
  private sourceTable = '';
  private migrationLoading = false;
  sourceTableChange ({ table, source, filter }: DataValidation.TDTable) {
    this.sourceTable = table;
  }
  mounted () {
    this.dvRemoteService.props({
      path:'datavalidation',
      workspaceId: this.workspaceId,
    });
    this.notebookRemoteService.props({
      path:'datavalidation',
      workspaceId: this.workspaceId,
    });
    this.nt = Util.getNt();
    this.refreshHis();
    this.srcTblRef = this.$refs.srcTbl as SourceTable;
    this.tgtTblRef = this.$refs.tgtTbl as TargetTable;
  }
  reset () {
    this.tgtTblRef.reset();
    this.srcTblRef.reset();
  }
  validate () {
    const vaild =
          this.srcTblRef.checkParams() && this.tgtTblRef.checkParams();
    const sourceTable: DataValidation.TDTable = this.srcTblRef.validate() as DataValidation.TDTable;
    const targetTable: DataValidation.HerculesTable = this.tgtTblRef.validate() as DataValidation.HerculesTable;
    if (vaild) {
      this.migrationLoading = true;
      this.dvRemoteService.validate(this.nt, sourceTable, targetTable)
        .then(() => {
          this.reset();
          this.refreshHis();
          this.migrationLoading = false;
        })
        .catch((e: any) => {
          let errMsg = 'request error';
          try {
            errMsg = e.response.data.errorDetail.message;
          } catch (ex) {}
          this.migrationLoading = false;
        });
    }
  }
  applyHistory (
    srcTable: DataValidation.TDTable,
    tgtTable: DataValidation.HerculesTable
  ) {
    this.tgtTblRef.apply(tgtTable);
    this.srcTblRef.apply(srcTable);
  }
  refreshHis () {
    this.$store.dispatch('setDVHistoryLoading');
    this.dvRemoteService.history(Util.getNt()).then((res)=>{
      if (res && res.data && res.data != null){
        let historyList = (res && res.data ) || [];
        // sort by createDate
        historyList = _.sortBy(historyList, [(o: any) => {return o.createDate;} ]).reverse();
        this.$store.dispatch('setDVHistoryFinish', {historyList});

      }
    }).catch(() => {
      this.$store.dispatch('setDVHistoryFinish', {historyList: []});
    });

  }
}
</script>
<style lang="scss" scoped>
#validation {
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
}
</style>

