<template>
  <div class="target-container">
    <!-- table info -->
    <div class="data-source row">
      <div class="target-info-label">
        Destination
      </div>
      <el-select
        v-model="targetTable.source"
        placeholder="platform"
      >
        <el-option
          v-for="item in targetPlatformOptions"
          :key="item.value"
          :label="item.key"
          :value="item.value"
        />
      </el-select>
    </div>
    <div class="table-name row">
      <div class="target-info-label">
        Target table
      </div>
      <el-input
        v-model="targetTable.table"
        :class="{'error': targetTable.isExist === false || errorAlert.HTable}"
        class="table-input"
        placeholder="DB Name.Table Name"
      />
      <div v-if="errorAlert.HTable">
        <span class="error-msg">{{ errorMsg.HTable }}</span>
      </div>
    </div>
    <div class="account row">
      <div class="target-info-label">
        {{ platformAccountWord }}
        <span class="optional">(optional)</span>
      </div>
      <el-input
        v-model="targetTable.batchAccount"
        class="account-input"
        type="text"
      />
    </div>
    <div class="filter row">
      <div class="target-info-label">
        Filters
        <span class="optional">(optional)</span>
      </div>
      <el-input
        v-model="targetTable.filter"
        class="filter-input"
        type="textarea"
        :autosize="{ minRows: 2}"
        placeholder="Type In Filters"
      />
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch } from 'vue-property-decorator';
import Util from '@/services/Util.service';
// import { TDTable, TableAPI, HerculesTable } from "./DataValidation";

@Component({
  components: {},
})
export default class TargetTable extends Vue implements DataValidation.TableAPI{
  @Prop() sourceTable = '';
  @Watch('sourceTable')
  onSourceTableChange (val: string, oldVal: string) {
    if (val){
      this.targetTable.table = val;
    }
  }
  private targetPlatformOptions = [
    // { key: "Vivaldi", value: "vivaldi"},
    // { key: "Mozart", value: "mozart" },
    // {key: 'Mozart', value: 'mozart_lvs'},
    // { key: "Hopper", value: "hopper" },
    { key: 'Hercules', value: 'hercules' },
    { key: 'Ares', value: 'ares' },
    // { key: "Apollo", value: "apollo" },
    { key: 'Apollo Reno', value: 'APOLLO_RNO'},
  ];
  private targetTable: DataValidation.HerculesTable = {
    $self: this,
    loading:false,
    source: 'hercules',
    table: '',
    filter:'',
    batchAccount:'',
    get srcDB () {
      const tableObj = Util.separateTableDB(this.table);
      return tableObj && tableObj.db ? tableObj.db.trim() : '';
    },
    get srcTable () {
      const tableObj = Util.separateTableDB(this.table);
      return tableObj && tableObj.table ? tableObj.table.trim() : '';
    },
  };
  errorAlert = {
    HTable:false,
  };
  errorMsg = {
    HTable:'',
  };
  get platformAccountWord (){
    if (this.targetTable.source == 'vivaldi' ||
      this.targetTable.source == 'mozart' ||
      this.targetTable.source == 'hopper'){
      return 'ETL_ID or DW_SA';
    }
    else if (this.targetTable.source == 'hercules' ||
      this.targetTable.source == 'ares' ||
      this.targetTable.source == 'apollo' ){
      return 'Batch Account';
    }
    else {
      return 'Platform Account';
    }
  }

  reset () {
    this.targetTable.loading = false;
    this.targetTable.source= 'hercules';
    this.targetTable.table= '';
    this.targetTable.batchAccount = '';
    this.targetTable.filter = '';
    this.errorAlert = {HTable:false};
    this.errorMsg = {HTable:''};
  }


  validateSource (tableInfo: DataValidation.TDTable) {
    const source = tableInfo.source;
    if (!source) {
      return;
    }
    const hasSource = (this.targetPlatformOptions.map(option => option.value).indexOf(source) >= 0);
    if (!hasSource) {
      tableInfo.source = '';
    }
  }
  apply (tableInfo: DataValidation.HerculesTable) {
    this.reset();
    this.validateSource(tableInfo);
    this.targetTable.source= tableInfo.source;
    this.targetTable.table= tableInfo.table;
    this.targetTable.filter = tableInfo.filter;
    this.targetTable.batchAccount = tableInfo.batchAccount;
  }
  checkParams (){
    /**
     * disable check table name
     */
    // let checkTable = Boolean(this.targetTable.srcDB && this.targetTable.srcTable);
    // if(!checkTable){
    //   this.errorAlert.HTable = true;
    //   this.errorMsg.HTable= "Incorrect table name";
    // }

    return true;
  }
  validate (){
    this.targetTable.table = this.targetTable.table.trim();
    return this.targetTable;
  }


}
</script>
<style lang="scss" scoped>
.target-container {
  padding: 0 5px;
  .row {
    margin-top: 15px;
    margin-bottom: 15px;
    span {
      margin-right: 5px;
    }
    div.target-info-label {
      margin-bottom: 10px;
    }
  }
  .table-name /deep/ .el-input-group,
  .account-input,
  .table-input,
  .filter-input{
    width: 350px;
  }
  .el-input.error {
    /deep/ .el-input__inner {
      border-color: #e53917;
      color: #e53917;
    }
  }
  .not-exist {
    span {
      font-style: italic;
      font-size: 12px;
      color: #ccc;
    }
  }
  span.error-msg{

      font-style: italic;
      font-size: 12px;
      color: red;
  }
  .optional{
    font-style: italic;
    font-size: 12px;
    color: #999;
  }
}
</style>


