<template>
  <div class="target-container">
    <div class="data-source row">
      <div class="target-info-label">
        Target
      </div>
      <el-select
        v-model="targetTable.source"
        placeholder="platform"
        @change="targetSourceChange"
      >
        <el-option
          v-for="item in targetPlatformOptions"
          :key="item.key"
          :value="item.value"
          :disabled="disabledTarget(item)"
          :label="item.key"
        />
      </el-select>
    </div>
    <div class="table-name row">
      <div
        v-if="!isVDMAvaliable"
        class="target-info-label"
      >
        Target table
        <span class="info">
          Datamove only support temporary table.
          <el-tooltip
            content="Bottom center"
            placement="bottom"
            effect="light"
            popper-class="data-move-info-popup"
          >
            <div slot="content">
              Datamove is using personal account to execute. The target table will be created in hdfs path “hdfs://hercules/sys/edw/zeta_dev/datamove/”.
              <br>
              If you choose override, please make sure you have access right to the target table.
              <br>
              If you want to copy data to Prod table, below are steps.
              <br>
              1.Using datamove to copy data to a temporary table
              <br>
              2.Go to Notebook. Using batch account to run an insert select to insert data from temp to production table.
            </div>
            <i class="el-icon-question" />
          </el-tooltip>
        </span>
      </div>
      <div
        v-if="isVDMAvaliable"
        class="target-info-label"
      >
        HDM(Hadoop Data Mart)
      </div>
      <el-form
        :rules="tableInputRule"
        :model="targetTable"
      >
        <el-row>
          <el-col
            :span="10"
            style="width:200px"
          >
            <el-form-item prop="srcDB">
              <el-select
                v-if="isHermes"
                v-model="targetTable.srcDB"
                class="table-input"
                placeholder="DB Name"
              >
                <el-option
                  v-for="item in workspaceOptions"
                  :key="item"
                  :label="item"
                  :value="item"
                />
              </el-select>
              <el-input
                v-else
                v-model="targetTable.srcDB"
                class="table-input"
                placeholder="DB Name"
                :maxlength="50"
              />
            </el-form-item>
          </el-col>
          <el-col
            :span="1"
            style="width:10px"
          >
            <em class="point">.</em>
          </el-col>
          <el-col :span="11">
            <el-form-item prop="table">
              <el-input
                v-model="targetTable.table"
                class="table-input"
                placeholder="Table Name"
                :maxlength="50"
                :disabled="isVDMAvaliable"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </div>
    <div
      v-show="overrideValid"
      class="row"
    >
      <el-checkbox
        v-model="targetTable.override"
        class="override"
      >
        Overwrite existing table(s)
      </el-checkbox>
    </div>
    <!-- <div
      v-if="!isHermes"
      class="row"
    >
      <el-checkbox
        v-model="targetTable.convert"
        class="convert"
        :disabled="hasFile"
      >
        Column Type Not Change
      </el-checkbox>
    </div> -->
    <div class="row">
      <div
        v-if="showOverrideWarning"
        class="warning-msg"
      >
        <span style="color: red; font-weight: bold">WARNING:</span> By selecting this option you may be overwriting your previously migrated Hopper data if you had a VDM with the same name in both Teradata clusters.  To avoid potential loss of Hopper data, uncheck this option and change the target database to one that contains the string, “_MZ_T” (for Mozart).
      </div>
    </div>
    <span
      v-if="!isFirstLoad && isHermes && workspaceOptions.length===0"
      class="error-msg is-hermes"
    >
      No databases found where this account has write permissions. <i
        class="link"
        @click="openAce"
      >Ask Zeta Ace</i><span> about this error.</span>
    </span>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch, Inject } from 'vue-property-decorator';
import Util from '@/services/Util.service';
import DMRemoteService from '@/services/remote/DataMove';
import _ from 'lodash';
import moment from 'moment';
import { defaultDataMoveQuestion } from '@/components/common/zeta-ace/utilties';
import { Mode } from './internal';
@Component({
  components: {},
})
export default class TargetTable extends Vue implements DataMove.TableAPI{
  @Inject('dmRemoteService') dmRemoteService: DMRemoteService;
  @Prop() hasFile: boolean;
  @Prop() sourceTable: string;
  @Prop() mode: string;
  @Prop() vdmTable: DataMove.VDMTable;
  isFirstLoad = true;
  targetPlatformOptions = [
    { key: 'hermes', value: 'hermesrno'},
    { key: 'hercules', value: 'hercules'},
	  { key: 'ares', value: 'ares'},
	  { key: 'apollorno', value: 'apollorno'},
  ];
  targetTable: DataMove.HerculesTable = {
    $self: this,
	  loading:false,
    source: 'apollorno',
    override:false,
	  convert:false,
	  table: '',
	  srcDB: '',
    get srcTable () {
      const tableObj = Util.separateTableDB(this.table.trim());
	    return tableObj && tableObj.table ? tableObj.table.trim() : '';
    },
  };
  workspaceOptions: Array<string> = [];
  isDBValid = true;
  isTableValid = true;
  tableInputRule: object = {
    table: [{
      trigger: 'blur',
    }, {
	    validator: this.validateTableName,
    },
	  ], srcDB: [{
	    trigger: 'blur',
    }, {
	    validator: this.validateDBName,
	  },
	  ]};

  get overrideValid (){
	  return  this.vdmTable.viewName!=''?false:true;
  }
  get showOverrideWarning (){
	  return this.vdmTable.source === 'numozart' && this.vdmTable.tableName != '' && (this.targetTable.override === true) ? true: false;
  }
  get isHermes (){
    return this.targetTable.source === 'hermesrno' || this.targetTable.source === 'hermeslvs';
  }

  get targetInputValid () {
    const valid = this.isDBValid && this.isTableValid && this.targetTable.srcDB!.trim() != '';
    if (this.mode != Mode.VDM) {
	    return valid && this.targetTable.table.trim() != '';
	  } else {
      return valid;
	  }
  }

  validateDBName (r: any, f: string, cb: Function) {
    if (/[^a-zA-Z0-9_]/.test(f)) {
      this.isDBValid = false;
      cb(new Error('Name not valid. Only alphanumeric, _, are allowed.'));
    } else {
	    this.isDBValid = true;
      cb();
	  }
  }

  validateTableName (r: any, f: string, cb: Function) {
    if (/[^a-zA-Z0-9_]/.test(f)) {
	    this.isTableValid = false;
	    cb(new Error('Name not valid. Only alphanumeric, _, are allowed.'));
    } else {
      this.isTableValid = true;
      cb();
    }
  }

  @Watch('sourceTable')
  onSourceTableNameChange (val: string) {
    if (val){
	    const tableObj = Util.separateTableDB(val);
	    if (!this.isHermes){
	      this.targetTable.srcDB = 'zeta_dev_working';
	    } else {
        this.targetTable.srcDB = this.workspaceOptions[0] || '';
      }
      this.targetTable.table = `${tableObj && tableObj.table ? tableObj.table : ''}`;
    }
  }

  @Watch('mode', {immediate: true})
  async onModeChange () {
    if (this.isVDMAvaliable) {
      this.targetPlatformOptions[0] = {key: 'hermeslvs', value:'hermeslvs'};
      this.targetTable.source = 'hermeslvs';
      await this.getWorkspace('hermeslvs');
	    if (this.workspaceOptions.length) {
        this.targetTable.srcDB = this.workspaceOptions[0];
      } else {
	      this.targetTable.srcDB = '';
      }
      this.targetTable.table = '';
      this.targetTable.override = true;
    } else {
      this.targetPlatformOptions[0] = {key: 'hermes', value:'hermesrno'};
      this.targetTable.source = 'apollorno';
	    this.targetTable.srcDB = '';
      this.targetTable.table = '';
	    this.targetTable.override = false;
      this.targetTable.convert = false;
	  }
  }

  @Watch('hasFile')
  onSourceHasFile (val: boolean){
    this.targetTable.override = val;
    this.targetTable.convert = false;
    if (!val && this.isHermes){
	    this.reset();
	  }
  }

  getSrcDB (table: string){
	  const tableObj = Util.separateTableDB(table);
    return tableObj && tableObj.db ? tableObj.db.trim() : '';
  }

  sliceTableName (tableName: string) {
	  const tableObj = Util.separateTableDB(tableName);
    const table = tableObj && tableObj.table ? tableObj.table.trim() : '';
	  return table.length >= 50 ? table.substring(0, 50) : table;
  }

  disabledTarget (item: {value: string}) {
	  if (this.isVDMAvaliable) {
      return item.value != 'hermeslvs';
	  } else if (this.mode === Mode.UPLOAD) {
      return false;
	  } else {
	    return item.value == 'hermesrno';
	  }
  }

  get isVDMAvaliable () {
	  return this.mode == Mode.VDM;
  }

  reset () {
    if (this.isVDMAvaliable) {
	    this.targetTable.source= 'hermeslvs';
	    this.targetTable.table= '';
	    this.targetTable.override = true;
      this.targetTable.srcDB = this.workspaceOptions[0] ? this.workspaceOptions[0] : '';
    } else {
	    this.targetTable.loading = false;
	    this.targetTable.source= 'apollorno';
	    this.targetTable.table= '';
	    this.targetTable.srcDB= '';
      this.targetTable.override = false;
	    this.targetTable.convert = false;
	  }
  }

  async apply (tableInfo: DataMove.HerculesTable) {
	  this.reset();
    let src = tableInfo.source;
    const options = _.map(this.targetPlatformOptions, p => p.value);
    if (!_.includes(options, src)) {
	    src = options[0];
	  }
	  this.targetTable.source= src;

    if (tableInfo.source == 'hermeslvs') {
      await this.getWorkspace('hermeslvs');
	    // check database
	    if (_.includes(this.workspaceOptions, tableInfo.table)) {
	      this.targetTable.srcDB = tableInfo.table;
        this.targetTable.override = tableInfo.override;
      }

	  } else {
	    const db = this.getSrcDB(tableInfo.table);
      this.targetTable.srcDB= db;
	    this.targetTable.table= this.sliceTableName(tableInfo.table);
      this.targetTable.override = tableInfo.override;
      this.targetTable.convert = tableInfo.convert;

	  }
  }
  checkParams (){
	  return this.targetInputValid;
  }
  migration (){
    this.targetTable.table = this.targetTable.table.trim();
    return this.targetTable;
  }
  getTableName (name: string): string{
    const firstChar = name[0];
	  return firstChar.toUpperCase() + name.substring(1, name.length);
  }
  getParams (){
    return null;
  }
  openAce (){
	  this.$store.dispatch('setAceVisible', true);
	  const q = defaultDataMoveQuestion[0];
	  q.time = moment().format('h:mm:ss a');
	  this.$store.dispatch('setAceQuestion', q);
  }
  async getWorkspace (platform: string){
    // const plt = this.isVDMAvaliable?'hermeslvs':platform;
    this.isFirstLoad = true;
    const plat = platform == 'hermes'?'hermesrno': platform;
    try {
      const res: any = await this.dmRemoteService.getVDMDataBases(plat, true);
      if (res && res.data) {
        this.workspaceOptions = res.data;
        if (this.isHermes && !this.targetTable.srcDB && this.workspaceOptions.length > 0) {
          this.targetTable.srcDB = this.workspaceOptions[0];
	      }
	    } else {
	      this.workspaceOptions = [];
	    }
      this.isFirstLoad = false;
    }
    catch (e) {
	    this.isFirstLoad = false;
	  }
  }
  async targetSourceChange (){
    if (this.isHermes){
      await this.getWorkspace('hermesrno');
      this.targetTable.srcDB = this.workspaceOptions[0]||'';
	  } else {
	    if (this.sourceTable){
	      this.targetTable.srcDB = 'zeta_dev_working';
      } else {
        this.targetTable.srcDB = '';
      }
    }
  }

  @Watch('targetInputValid')
  isTagetTableValid (val: boolean) {
    this.$emit('is-table-valid', val);
  }
  @Watch('vdmTable', {deep:true})
  handleVDMTableChange (){
	  if (this.vdmTable.viewName!='' || this.vdmTable.source === 'numozart'){
	    this.targetTable.override = false;
	  } else {
	    this.targetTable.override = true;
	  }
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
		.el-form-item{
			margin-bottom: 0;
		}
	}
	.table-name /deep/ .el-input-group,
	.path-input,
	.table-input{
		width: 200px;
	}
	.point{
		font-style: normal;
		font-size: 24px;
		line-height: 30px;
		height: 30px;
		padding: 0 2px;
		vertical-align: top;
		display: inline-block;
	}
	.table-input.error {
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

			// font-style: italic;
			font-size: 12px;
			color: red;
      >span{
        color: #333333;
      }
  }
  .warning-msg{
    font-size: 12px;
    max-width: 500px;
  }
	span.is-hermes{
		display: block;
		margin-top: 20px;
	}
	.link{
		font-style: normal;
		color: #569ce1;
		cursor: pointer;
		text-decoration: underline;
	}
	.info{
		color: #666;
		font-size: 12px;

	}
}
</style>
<style lang="scss">
	.data-move-info-popup{
		width: 400px;
	}
	.el-select-dropdown__item{
		.tip{
			font-size: 12px;
			color: #cacbcf;
			margin-left: 5px
		}
	}
</style>



