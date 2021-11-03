<template>
  <div class="source-container">
    <!-- table info -->
    <div class="data-source row">
      <div class="source-info-label">
        Source
      </div>
      <el-select
        v-model="sourceTable.source"
        placeholder="platform"
      >
        <el-option
          v-for="item in srcPlatformOptions"
          :key="item.value"
          :label="item.key"
          :value="item.value"
        />
      </el-select>
    </div>
    <div class="table-name row">
      <div class="source-info-label">
        Source table
      </div>
      <el-input
        ref="searchSource"
        v-model="sourceTable.table"
        :class="{'error':errorAlert.TDTable}"
        class="source-input"
        size="medium"
        placeholder="DB Name.Table Name"
        :disabled="sourceTable.columnLoading"
        @change="handleSourceSearch"
      >
        <!-- <el-button v-if="!sourceTable.columnLoading" slot="append" icon="el-icon-search" @click="handleSourceSearch"></el-button>
                <el-button v-else slot="append" icon="el-icon-loading"></el-button>-->
      </el-input>
      <!-- <el-button
        class="show-detail"
        type="default"
        size="small"
        plain
        :disabled="!(sourceTable.srcDB && sourceTable.srcTable)"
        :loading="sourceTable.columnLoading"
        @click="handleDetailSearch"
      >
        show detail
      </el-button> -->
      <br>
      <span
        v-show="errorAlert.TDTable"
        class="error-msg"
      >{{ errorMsg.TDTable }}</span>
      <el-dialog
        title="Columns detail"
        :visible.sync="dialogTableVisible"
      >
        <div class="dialog-child">
          <el-table
            border
            :height="520"
            :data="sourceTable.columns"
          >
            <el-table-column
              property="columnname"
              label="Name"
            />
            <el-table-column
              property="dataType"
              label="Type"
            />
          </el-table>
        </div>
      </el-dialog>
    </div>
    <div class="account row">
      <div class="source-info-label">
        {{ platformAccountWord }}
        <span class="optional">(optional)</span>
      </div>
      <el-input
        v-model="sourceTable.batchAccount"
        class="filter-input"
        type="text"
      />
    </div>
    <div class="filter row">
      <div class="source-info-label">
        Filters
        <span class="optional">(optional)</span>
      </div>
      <el-input
        v-model="sourceTable.filter"
        class="filter-input"
        type="textarea"
        :autosize="{ minRows: 2}"
        placeholder="Type In Filters"
      />
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Emit, Inject } from 'vue-property-decorator';
import Util from '@/services/Util.service';
// import { TDTable, TableAPI } from "./DataValidation";
import DVRemoteService from '@/services/remote/DataValidation';
interface TableDetailOption {
  [k: string]: Array<string>;
}
@Component({
  components: {},
})
export default class SourceTable extends Vue
  implements DataValidation.TableAPI {
  @Inject('dvRemoteService')
  dvRemoteService: DVRemoteService;
  private srcPlatformOptions = [
    // { key: "Vivaldi", value: "vivaldi" },
    // { key: "Mozart", value: "mozart" },
    // {key: 'Mozart', value: 'mozart_lvs'},
    // { key: "Hopper", value: "hopper" },
    { key: 'Hercules', value: 'hercules' },
    { key: 'Ares', value: 'ares' },
    // { key: "Apollo", value: "apollo" },
    { key: 'Apollo Reno', value: 'APOLLO_RNO' },
  ];
  dialogTableVisible = false;
  tableDetailOption: TableDetailOption = {};
  private sourceTable: DataValidation.TDTable = {
    $self: this,
    source: 'APOLLO_RNO',
    table: '',
    columnLoading: false,
    columns: [],
    get srcDB () {
      const tableObj = Util.separateTableDB(this.table);
      return tableObj && tableObj.db ? tableObj.db.trim() : '';
    },
    get srcTable () {
      const tableObj = Util.separateTableDB(this.table);
      return tableObj && tableObj.table ? tableObj.table.trim() : '';
    },
    batchAccount:'',
    filter: '',
  };
  errorMsg = {
    TDTable: '',
  };
  errorAlert = {
    TDTable: false,
  };
  separator = '||';
  get platformAccountWord () {
    if (
      this.sourceTable.source == 'vivaldi' ||
            this.sourceTable.source == 'mozart' ||
            this.sourceTable.source == 'hopper'
    ) {
      return 'ETL_ID or DW_SA';
    } else if (
      this.sourceTable.source == 'hercules' ||
            this.sourceTable.source == 'ares' ||
            this.sourceTable.source == 'apollo'
    ) {
      return 'Batch Account';
    } else {
      return 'Platform Account';
    }
  }
  handleSourceSearch (val: string) {
    if (this.sourceTable.srcDB && this.sourceTable.srcTable) {
      // this.getColumnList(this.sourceTable);
      this.sourceTable.columns = [];
      this.onSourceTableChange(this.sourceTable);
    }
  }
  handleDetailSearch () {
    const tableInfo = this.sourceTable.source + this.separator + this.sourceTable.table;
    if (this.tableDetailOption[tableInfo] && this.tableDetailOption[tableInfo].length > 0) {
      this.sourceTable.columns = this.tableDetailOption[tableInfo];
      this.dialogTableVisible = true;
    } else {
      this.getColumnList(this.sourceTable);
    }
  }
  @Emit('tableChange')
  // eslint-disable-next-line @typescript-eslint/no-empty-function
  onSourceTableChange ({ table, source, filter }: DataValidation.TDTable) {}

  getColumnList ({ source, srcDB, srcTable }: DataValidation.TDTable) {
    const tableInfo = source + this.separator + this.sourceTable.table;
    this.tableDetailOption[tableInfo] = [];
    source = source == 'mozart_lvs' ? 'numozart' : source;
    this.sourceTable.columnLoading = true;
    return this.dvRemoteService.getColumns(
      source,
      srcDB || '',
      srcTable || ''
    ).then(
      res => {
        if (res && res.data && res.data != null) {
          const cols = (res && res.data) || [];
          this.tableDetailOption[tableInfo] = cols.columns;
          this.sourceTable.columns = cols.columns;
          this.sourceTable.columnLoading = false;
          this.dialogTableVisible = true;
        }
      },
      () => {
        this.sourceTable.columnLoading = false;
      }
    );
  }
  reset () {
    this.sourceTable.source = 'APOLLO_RNO';
    this.sourceTable.table = '';
    this.sourceTable.batchAccount = '';
    this.sourceTable.columnLoading = false;
    this.sourceTable.columns = [];
    this.sourceTable.filter = '';
    this.errorMsg = { TDTable: '' };
    this.errorAlert = { TDTable: false };
  }

  validateSource (tableInfo: DataValidation.TDTable) {
    const source = tableInfo.source;
    if (!source) {
      return;
    }
    const hasSource = (this.srcPlatformOptions.map(option => option.value).indexOf(source) >= 0);
    if (!hasSource) {
      tableInfo.source = '';
    }
  }
  apply (tableInfo: DataValidation.TDTable) {
    this.reset();
    this.validateSource(tableInfo);
    this.sourceTable.source = tableInfo.source;
    this.sourceTable.table = tableInfo.table;
    this.sourceTable.filter = tableInfo.filter;
    this.sourceTable.batchAccount = tableInfo.batchAccount;
    // this.getColumnList(this.sourceTable);
  }
  checkParams () {
    const checkTDTable = Boolean(
      this.sourceTable.srcDB && this.sourceTable.srcTable
      // this.sourceTable.srcTable
    );
    if (!checkTDTable) {
      this.errorMsg.TDTable = 'Incorrect table name';
      this.errorAlert.TDTable = true;
    }
    return checkTDTable;
  }
  validate () {
    this.sourceTable.table = this.sourceTable.table.trim();
    return this.sourceTable;
  }
}
</script>
<style lang="scss" scoped>
@import "@/styles/global.scss";
.source-container {
    padding: 0 5px;
    .row {
        margin-top: 15px;
        margin-bottom: 15px;
        span {
            margin-right: 5px;
        }
        div.source-info-label {
            margin-bottom: 10px;
        }
    }
    .table-name /deep/ .el-input-group,
    .table-name .source-input,
    .filter-input {
        width: 350px;
    }
    .table-name {
        .show-detail {
            margin-left: 5px;
        }
    }
    .optional {
        font-style: italic;
        font-size: 12px;
        color: #999;
    }

    .el-button--text {
        &:hover,
        &:active,
        &:focus {
            border-color: transparent;
        }
    }
    span.error-msg {
        // font-style: italic;
        font-size: 12px;
        color: $zeta-global-color-red;
    }
    .el-input.error {
        /deep/ .el-input__inner {
            border-color: $zeta-global-color-red;
            color: $zeta-global-color-red;
        }
    }
    .el-dialog__wrapper {
        & /deep/ .el-dialog__header {
            padding: 30px 30px 0px 30px;
        }

        & /deep/ .el-dialog__body {
            margin: 15px 30px 0 30px;
            padding: 0 0 30px 0;
            max-height: 550px;
            .dialog-child {
                max-height: 520px;
                // overflow-y: auto;
            }
        }
    }
}
</style>
