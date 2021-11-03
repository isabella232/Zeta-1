<template>
  <div class="table-config-container">
    <div class="body" :class="{'edit-body': isEditMode}">
      <div class="title-container">
        <span @click="editSheetName()" v-show="flag.showSheetName" :class="{'s-container': isInvalidName}">
          <span class="sheet-title" >{{metaSheetName}}</span>
          <i class="el-icon-edit"></i>
        </span>
          <el-input class="edit-input" ref="sheetNameInputRef" v-show="!flag.showSheetName" v-model="metaConfig.name" placeholder="Please enter sheet name" @blur="changeSheetName()"/>
      </div>
      <meta-scheme-config
      :mode="mode"
      :data="tableData"
      ref="schemaConfigRef"
      @validate="setValidateFlag"
      @add-row="addNewRow"
      @delete-row="deleteRow" />

    </div>
    <div class="tip" v-if="isEditMode">
      <el-alert title="Note: Please save the change you enter in the sheet before you make any changes on the table schema here otherwise the sheet content you edit will loose." type="info"></el-alert>
    </div>
    <div class="footer">
      <el-button type="primary" @click="create()" :loading="flag.loading">{{toolBtnText}}</el-button>
      <el-button plain @click="close()">Cancel</el-button>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Inject, Prop, Watch } from "vue-property-decorator";
import { MetaConfigTableRow, MetaSheetConfig, Mode, MODE, MetaSheetTableItem, MSG_TYPE, MetaSheetTableRow } from '@/types/meta-sheet';
import _ from "lodash";
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import { ElInput } from "element-ui/types/input";
import MetaSheetService from '@/services/remote/MetaSheetService';
import MetaSchemeConfig from './schema-config.vue';
import MetaSheetUtil from './util';
import Util from "@/services/Util.service";
@Component({
  components: {
    MetaSchemeConfig
  }
})
export default class MetaConfigEdit extends Vue {
  @Prop() mode: Mode; // ADD or EDIT
  @Prop() config: MetaSheetConfig;
  @Inject('metaSheetService') metaSheetService: MetaSheetService;

  metaRow:MetaConfigTableRow = {
    column: '',
    type: '',
    length: '',
    desc: '',
    nullValue: true,
    primaryKey: false,
    editable: true,
    validate: false
  };
  flag = {
    showSheetName: true,
    isCreating: false,
    loading: false
  }
  metaConfig:MetaSheetConfig = _.cloneDeep(this.config);

  get isEditMode() {
    return this.mode == MODE.EDIT;
  }

  get metaSheetName() {
    if(this.metaConfig.name) {
      return this.metaConfig.name.trim();
    } else {
      return 'Title zeta sheet name';
    }
  }

  get schemaConfigRef() {
    return this.$refs.schemaConfigRef as MetaSchemeConfig;
  }

  get sheetNameInputRef():ElInput{
    return this.$refs.sheetNameInputRef as ElInput;
  }

  get schemaConfig() {
    return this.metaConfig.schemeConfig;
  }

  get toolBtnText() {
    if(this.mode == MODE.ADD) {
      return 'Create';
    } else if(this.mode == MODE.EDIT){
      return 'Save';
    }
  }

  get assertValid() {
    let tableData = this.schemaConfig!.tableData;
    let emptyRow = _.find(this.tableData, row => MetaSheetUtil.isInvalidRow(row)) ;
    return !emptyRow;
  }

  get isInvalidName() {
    return this.flag.isCreating && !this.metaConfig.name;
  }

  get tableData() {
    return this.metaConfig.schemeConfig!.tableData;
  }

  deleteRow(index: number) {
    this.tableData.splice(index, 1);
  }

  addNewRow() {
    this.tableData.push(_.cloneDeep(this.metaRow));
  }

  changeSheetName() {
    this.flag.showSheetName = true;
  }

  editSheetName() {
    this.flag.showSheetName = false;
    this.$nextTick(() => {
      this.sheetNameInputRef.focus();
    })
  }

  async create() {
    this.flag.isCreating = true;
    let result = this.schemaConfigRef.validate();
    if(result && !this.isInvalidName) {
      this.metaConfig.name = this.metaSheetName;
      if(this.mode == MODE.ADD) {
        await this.addNewMetaSheet(this.metaConfig);
      } else if(this.mode == MODE.EDIT) {
        await this.updateMetaSheet(this.metaConfig);
      }
    }
  }

  async addNewMetaSheet(config: MetaSheetConfig) {
    this.flag.loading = true;
    const nt = Util.getNt();
    let tableData = config.schemeConfig!.tableData;
    let newItem: MetaSheetTableItem = {
      metaTableName: config.name,
      schemaInfo: JSON.stringify(tableData)
    }
    try {
      let response = await this.metaSheetService.createMetaSheet(newItem);
      if(MetaSheetUtil.isOk(response)) {
        const id = response.data.msg;
        this.$emit('created', id);
      } else {
        MetaSheetUtil.showMessage(this, MSG_TYPE.ERROR, response.data.msg);
        this.flag.loading = false;
      }
    }
    catch(e) {
      this.flag.loading = false;
    }
  }

  async updateMetaSheet(config: MetaSheetConfig) {
    this.flag.loading = true;
    // this.flag.metaSheetLoading = true;
    let newSchemaConfig = config.schemeConfig!;
    let tableData = this.$store.getters.metasheets;
    let target = _.find(tableData, (row:MetaSheetTableRow) => row.id === config.id)!;
    let oldSchemaConfig = target.schemeConfig!;
    let oldColumns = _.map(oldSchemaConfig.tableData, row => row.column);
    let newColumnsEditable =  _.chain(newSchemaConfig.tableData).filter(row => row.editable).value();
    let newColumnsNotEditable = _.chain(newSchemaConfig.tableData).filter(row => !row.editable).map((row:MetaConfigTableRow) => row.column).value();

    let changedColumns = _.difference(oldColumns, newColumnsNotEditable);
    let param = {operations: {}, metaTableInfo: {}};
    if(target.metaTableName != config.name) {
      (<any>param.metaTableInfo).metaTableName = config.name;
    }
    if(changedColumns.length > 0) {
      let dropedColumns = _.filter(oldSchemaConfig.tableData, row => changedColumns.indexOf(row.column) != -1);
      param.operations['DROP'] = dropedColumns;
    }
    if(newColumnsEditable.length > 0) {
      param.operations['ADD'] = newColumnsEditable;
    }
    // compare if drop table and add same field together
    if(param.operations['DROP'] && param.operations['ADD']) {
      let processAddRows:Array<MetaConfigTableRow> = [];
      _.each(param.operations['ADD'], (addRow:MetaConfigTableRow) => {
        let removed = _.remove(param.operations['DROP'], (dropRow:MetaConfigTableRow ) => {
          return addRow.column == dropRow.column && addRow.desc == dropRow.desc
          && addRow.length == dropRow.length && addRow.nullValue === dropRow.nullValue
          && addRow.primaryKey === dropRow.primaryKey
          && addRow.type == dropRow.type
        });
        if(removed.length == 0) {
          processAddRows.push(addRow);
        }
      });
      param.operations['ADD'] = processAddRows;
    }
    if(_.isEmpty(param.operations)) {
      delete param.operations;
    }
    if(_.isEmpty(param.metaTableInfo)) {
      delete param.metaTableInfo;
    }
    if(!_.isEmpty(param)) {
      let response = await this.metaSheetService.updateMetaSheetById(config.id!, param);
      if(MetaSheetUtil.isOk(response)) {
        // this.flag.showMetaConfigTable = false;
        // await this.getMetaSheets();
        let tableData = config.schemeConfig!.tableData;
        _.each(tableData, (d)=>  {
            d.editable = false;
            d.validate = false;
        })
        let newTarget:MetaSheetTableRow = {
          ...target,
          metaTableName: config.name,
          schemeConfig: config.schemeConfig,
        }
        this.$store.dispatch('updateMetaSheet',newTarget);
        this.$store.dispatch('updateWSMetaSheet',newTarget);
        this.$emit('updated');
      } else {
        MetaSheetUtil.showMessage(this, MSG_TYPE.ERROR, response.data.msg);
        this.flag.loading = false;
      }
    } else {
      this.$emit('updated');
    }
  }

  close() {
    this.$emit("cancel");
  }

  setValidateFlag() {
    let tableData = _.cloneDeep(this.tableData);
    tableData.forEach(d => d.validate = true);
    this.metaConfig.schemeConfig!.tableData = tableData;
  }

}
</script>

<style lang="scss" scoped>
$blue: #569ce1;
$p10: 10px;
$grey: #B1B3B8;
$errorColor: #f56c6c;
.table-config-container {

    .edit-input {
        width: 250px;
    }
    // min-height: 200px;
    padding-bottom: 50px;

    .add-new {
        margin-top: $p10;
        color: $blue;
        i {
            margin-right: $p10;
        }
    }

    .body {
        margin-bottom: 70px;
        .title-container {
            // width: 250px;
            margin: 10px auto 40px;
            text-align: center;
            font-size: 16px;
            cursor: pointer;
            color: $grey;

            .s-container {
                position: relative;

                &::after {
                    content: 'This field is required';
                    position: absolute;
                    top: 28px;
                    left: 1px;
                    font-size: 13px;
                    color: $errorColor;
                    white-space: nowrap;
                }
            }

            .sheet-title {
                display: inline-block;
                padding-bottom: 5px;
                border-bottom: 1px solid $grey;
                color: $grey;
                text-align: initial;
            }
            i {
                margin-left: 5px;
                vertical-align: text-bottom;
            }
        }
    }

    .edit-body {
        margin-bottom: 30px;
    }

    .tip {
        margin-bottom: 20px;
    }

    .footer {
        float: right;
    }

}
</style>
