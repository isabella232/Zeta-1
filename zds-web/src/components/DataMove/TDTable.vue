<template>
  <div class="td-container">
    <div class="row">
      <div class="source-info-label">
        Source
      </div>
      <el-select
        v-model="tdtable.source"
        style="width: 150px"
        placeholder="platform"
      >
        <el-option
          v-for="item in srcPlatformOptions"
          :key="item.value"
          :label="item.key"
          :value="item.value"
        />
      </el-select>
      
      <span
        v-if="tdtable.source == 'numozart'"
        class="info"
      >
        You will need TD bridge access. To apply, click
        <a
          href="https://wiki.vip.corp.ebay.com/display/ND/Zeta+Data+Move+QA"
          target="_blank"
        >here</a>
      </span>
    </div>
    <div class="row">
      <div class="source-info-label">
        Source table
      </div>
      <el-input
        ref="searchSource"
        v-model="tdtable.fullTableName"
        class="source-input"
        size="medium"
        placeholder="DB Name.Table Name"
        @change="handleSourceSearch"
      />
      <el-button
        class="show-detail"
        type="default"
        size="small"
        plain
        :disabled="!(this.database && this.tableName)"
        :loading="columnLoading"
        @click="handleDetailSearch"
      >
        show detail
      </el-button>
      <br>
    </div>
    <div class="row">
      <div class="source-info-label">
        Filters
        <span class="optional">(optional)</span>
      </div>
      <el-input
        v-model="tdtable.filter"
        class="source-input"
        type="textarea"
        :autosize="{ minRows: 2}"
        placeholder="typein filters"
      />
    </div>

    <el-dialog
      title="Columns detail"
      :visible.sync="dialogTableVisible"
    >
      <div class="dialog-child">
        <el-table
          border
          :height="520"
          :data="columns"
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
</template>
<script lang="ts">
import { Component, Vue, Prop, Emit, Inject, Watch } from 'vue-property-decorator';
import _ from 'lodash';
import ScheduleContainer, { DataMoveSchedule, ScheduleConfig } from '@/components/common/schedule-container';
import DMRemoteService from '@/services/remote/DataMove';
import { TimePickerResult } from '@/components/common/time-picker';
import { Table as ElTable, Dialog as ElDialog} from 'element-ui';
import DataMove from './DataMove.vue';
import Util from '@/services/Util.service';
import { Mode } from './internal';
interface TableRow {
  name: string;
}

const DELIMETER = ';';
@Component({})
export default class TDTable extends Vue {
  @Inject('dmRemoteService') dmRemoteService: DMRemoteService;
  @Prop() tdtable!: DataMove.TDTable;

  srcPlatformOptions = [
    { key: 'mozart', value: 'numozart'},
    // { key: 'hopper', value: 'hopper' }
  ];
  /** SHOW DETAIL USE */
  columnLoading = false;
  columns = []; 
  dialogTableVisible = false;
  
  get database() {
    const tableObj = Util.separateTableDB(this.tdtable.fullTableName);
    return tableObj && tableObj.db ? tableObj.db.trim() : '';
  }

  get tableName() {
    const tableObj = Util.separateTableDB(this.tdtable.fullTableName);
    return tableObj && tableObj.table ? tableObj.table.trim() : '';
  }

  get sourceValid() {
    return this.tdtable.source != '' &&  this.database!.trim() != '' && this.tableName!.trim() != '';
  }

  handleSourceSearch(val: string) {
    if (this.sourceValid) {
      this.columns = [];
      this.$emit('td-table-change', {...this.tdtable});
    }
  }

  handleDetailSearch() {
    if (this.columns && this.columns.length > 0) {
      this.dialogTableVisible = true;
    } else {
      this.getColumnList();
    }
  }

  destroyed() {
    this.reset();
    this.$emit('td-table-change', {...this.tdtable});
  }

  reset() {
    this.tdtable.source = 'numozart';
    this.tdtable.fullTableName = '';
    this.tdtable.filter = '';
    this.columnLoading = false;
    this.columns = []; 
  }

  validateSource(tableInfo: DataMove.TDTable) {
    const source = tableInfo.source;
    if (!source) {
      return;
    }
    const hasSource = (_.map(this.srcPlatformOptions, option => option.value).indexOf(source) >= 0);
    if (!hasSource) {
      tableInfo.source = '';
    }
  }
  apply(tdTable: DataMove.TDTable) {
    if(!_.isEmpty(tdTable)) {
      this.validateSource(tdTable);
      this.tdtable.source = tdTable.source;
      this.tdtable.fullTableName = tdTable.fullTableName;
      this.tdtable.filter = tdTable.filter;
    }
  }
  
  getColumnList() {
    this.columnLoading = true;
    return this.dmRemoteService.getColumns(this.tdtable.source, this.database, this.tableName).then(
      res => {
        if (res && res.data && res.data != null) {
          const cols = res.data || [];
          this.columns = cols.columns;
          this.columnLoading = false;
          this.dialogTableVisible = true;
        }
      },
      err => {
        this.columnLoading = false;
      }
    );
  }

  @Watch('sourceValid')
  isSourceValid(val: boolean) {
    this.$emit('is-table-valid', {mode: Mode.TD, valid: val});
  }
  
}
</script>

<style lang="scss" scoped>
@import "@/styles/global.scss";
.td-container {
    .info {
            margin-left: 10px;
            color: #666;
            font-size: 12px;
    }
    .source-input {
        width: 350px;
    }
    .show-detail {
            margin-left: 5px;
    }
    .optional {
            font-style: italic;
            font-size: 12px;
            color: #999;
    }
    /deep/ .el-dialog__body {
        padding-bottom: 30px;
    }

    .dialog-child {
        margin: 15px;
        max-height: 520px;
        overflow-y: auto;
    }
}

</style>