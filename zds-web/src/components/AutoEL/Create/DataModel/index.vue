<template>
  <div class="data-model">
    <div class="overview">
      <labeled label="Source Table">
        {{ task.src_table }}
      </labeled>
      <labeled label="Target DB">
        {{ task.tgt_db }}
      </labeled>
      <labeled label="Source Host">
        <span style="display:inline-block;max-height:112px;overflow:auto;">
          {{ host }}
        </span>
      </labeled>
      <labeled label="Target Table">
        {{ task.tgt_table }}
        <span v-if="task.upd_freq === 'one_time'">_w</span>
      </labeled>
      <div />
      <labeled label="Data Location">
        {{ hdLocation }}
      </labeled>
      <labeled label="Source Table Description">
        <div style="white-space:pre-wrap;">
          <span>{{ sourceTableDetail.table_desc }}</span>
        </div>
      </labeled>
      <labeled label="Target Table Description">
        <el-input
          v-model="form.targetDesc"
          type="textarea"
          autosize
          :disabled="disabled"
        />
      </labeled>
    </div>
    <el-divider />

    <el-table
      :data="form.columns"
      class="data-model-table"
      height="1000"
      style="width: 100%"
    >
      <div slot="empty">
        <i class="el-icon-loading" />
        Loading
      </div>
      <el-table-column label="Source Table">
        <el-table-column
          label="Column Name"
          prop="s_name"
        />
        <el-table-column
          label="Column Type"
          prop="s_type"
          width="120"
        />
        <el-table-column
          label="Column Desc"
          prop="s_desc"
        />
        <el-table-column
          label="EIC"
          prop="s_isEic"
          width="90"
        />
        <el-table-column
          label="PCI"
          width="50"
        >
          <template slot-scope="scope">
            <el-checkbox
              v-if="scope.row.s_name"
              v-model="scope.row.s_isPCI"
              disabled
            />
          </template>
        </el-table-column>
        <el-table-column
          label="PI" 
          width="40"
        >
          <template slot-scope="scope">
            <el-checkbox
              v-if="scope.row.s_name"
              v-model="scope.row.s_isPI"
              disabled
            />
          </template>
        </el-table-column>
        <el-table-column
          label="Mandatory"
          width="90"
        >
          <template slot-scope="scope">
            <el-checkbox
              v-if="scope.row.s_name"
              v-model="scope.row.s_isMandatroy"
              disabled
            />
          </template>
        </el-table-column>
      </el-table-column>
      <el-table-column label="Target Table">
        <el-table-column
          label="Column Name"
          width="160"
        >
          <template slot-scope="scope">
            <div :class="scope.row.suggestedName && scope.row.t_name !== scope.row.suggestedName ? 'warning' : 'undefined'">
              <span v-if="scope.row.readonly || disabled">{{ scope.row.t_name }}</span>
              <el-input
                v-else
                v-model="scope.row.t_name"
                :placeholder="scope.row.suggestedName"
              />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="Column Type">
          <template slot-scope="scope">
            <div :class="scope.row.suggestedType && scope.row.t_type !== scope.row.suggestedType ? 'warning' : 'undefined'">
              <span v-if="scope.row.readonly || disabled">{{ scope.row.t_type }}</span>
              <el-input
                v-else
                v-model="scope.row.t_type"
                :placeholder="scope.row.suggestedType"
              />
            </div>
          </template>
        </el-table-column>
        <el-table-column
          label="Column Desc"
          width="250"
        >
          <template slot-scope="scope">
            <div :class="scope.row.t_desc === '' ? 'warning' : 'undefined'">
              <span v-if="scope.row.readonly || disabled">{{ scope.row.t_desc }}</span>
              <el-input
                v-else
                v-model="scope.row.t_desc"
                type="textarea"
              />
            </div>
          </template>
        </el-table-column>
        <el-table-column
          label="PK"
          width="40"
        >
          <template slot-scope="scope">
            <el-checkbox
              v-model="scope.row.t_isPK" 
              :disabled="disabled || scope.row.isAuditCol"
            />
          </template>
        </el-table-column>
        <el-table-column
          label="PII"
          width="40"
        >
          <template slot-scope="scope">
            <el-checkbox
              v-model="scope.row.t_isPII"
              :disabled="disabled || scope.row.isAuditCol"
            />
          </template>
        </el-table-column>
        <el-table-column
          v-if="task.upd_strategy !== 'Full Table'"
          label="Incremental Index"
          width="80"
        >
          <template slot-scope="scope">
            <el-checkbox
              v-model="scope.row.t_isIncremental" 
              :disabled="disabled || scope.row.s_type !== 'DATE' "
            />
          </template>
        </el-table-column>
        <el-table-column
          label="Needed in DDL"
          width="120"
        >
          <template slot-scope="scope">
            <el-checkbox
              v-model="scope.row.t_inDDL" 
              :disabled="disabled"
            />
          </template>
        </el-table-column>
      </el-table-column>
    </el-table>

    <!-- DOE-466: hide this field  -->
    <div
      v-show="false"
      class="custom-condition"
    >
      <h1>Custom Condition</h1>
      <el-input
        v-model="form.customCondition"
        type="textarea"
        placeholder="Example: LAST_MODIFIED_DATE >= TO_DATE('2020-12-01 00:00:00','YYYY-MM-DD HH24:MI:SS')"
        autosize
        :disabled="disabled"
      />
    </div>

    <div
      v-if="disabled"
      class="script-wrapper"
    >
      <h1>Generated scripts</h1>
      <el-row :gutter="20">
        <el-col :span="12">
          <h2>DDL</h2>
          <div v-loading="loading">
            <SqlEditor
              v-if="ddl"
              :value="ddl"
              :options="{ lineNumbers: false }"
              read-only
            />
          </div>
        </el-col>
        <el-col :span="12">
          <h2>Incremental Job Scripts</h2>
          <div v-loading="loading">
            <SqlEditor
              v-if="incrementalScript"
              :value="incrementalScript"
              :options="{ lineNumbers: false }"
              read-only
            />
          </div>
        </el-col>
      </el-row>
    </div>

    <div
      class="btn-wrapper"
    >
      <el-button @click="onPrev">
        Previous
      </el-button>
      <div>
        <el-button
          v-if="!disabled"
          @click="onSave"
        >
          Save
        </el-button>
        <el-button
          type="primary"
          :loading="loading"
          @click="onNext"
        >
          Next
        </el-button>
        <template v-if="task.status === 'PENDING' && isDA && needDAApproval">
          <el-button
            type="danger"
            @click="onReject('DA')"
          >
            Reject
          </el-button>
          <el-button
            type="success"
            @click="onApprove('DA')"
          >
            Approve
          </el-button>
        </template>
        <template v-else-if="task.status === 'PENDING' && isSAOwner && needSAOwnerApproval && !needDAApproval">
          <el-button
            type="danger"
            @click="onReject('SA_OWNER')"
          >
            Reject
          </el-button>
          <el-button
            type="success"
            @click="onApprove('SA_OWNER')"
          >
            Approve
          </el-button>
        </template>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Watch, Mixins } from 'vue-property-decorator';
import Labeled from '../components/Labeled.vue';
import { Action, State, Mutation, Getter } from 'vuex-class';
import AutoELService from '@/services/remote/AutoELService';
import { Actions, Mutations } from '../../store';
import { attempt, isEmpty } from '@drewxiu/utils';
import SqlEditor from '@/components/common/Visualization/CodeDisplay.vue';
import { buildInitColumns, buildColumnsByDataModel, Column } from './helper';
import Util from '@/services/Util.service';
import { FormStatus } from '../mixins/Form';
import { sentToSAOwnerForApproval } from '../emailHelper';
import GroupService from '@/services/remote/GroupService';
import { GroupIdTemplate } from '@/components/common/AccessControl.vue';
const API = AutoELService.instance;

@Component({
  components: {
    Labeled,
    SqlEditor,
  },
})
export default class DataModel extends Mixins(FormStatus) {

  @Action(Actions.GetTask) getTask;
  @Action(Actions.GetDataModel) getDataModel;
  @Action(Actions.GetHadoopLocation) getLocation;
  @Action(Actions.GetSourceTableInfo) getSourceTableInfo;
  @Action(Actions.GetDDL) getDDL;
  @Action(Actions.GetUpsertScript) getUpsertScript;
  @Action(Actions.SaveDataModel) saveModel;
  @Mutation(Mutations.Clear) clear;
  @State(state => state.AutoEL.task || {}) task;
  @State(state => state.AutoEL.dataModel) dataModel;
  @State(state => state.AutoEL.hadoopLocation) hdLocation;
  @State(state => state.AutoEL.sourceTableInfo) sourceTableInfo;
  @State(state => state.AutoEL.ddl) ddl;
  @State(state => state.AutoEL.incrementalScript) incrementalScript;
  @Getter('$globalLoading') loading;

  form = {
    targetDesc: '',
    columns: [] as Column[],
    customCondition: '',
  };
  get host (){
    return this.task && this.task.source_list && this.task.source_list.map(h => `${h.table_name},${h.dbc_name}`).join('\n');
  }
  get id () {
    return this.$route.params.id;
  }
  get sourceTableDetail () {
    return attempt(() => this.sourceTableInfo[this.task.src_table] || {}, {});
  }
  get tableSize () {
    const tableSize = this.sourceTableDetail.table_size || 0;
    return (tableSize / (1024 * 1024 * 1024)).toFixed(1);
  }
  get model () {
    const model = {
      pltfrm_name: this.task.tgt_platform,
      db_name: this.task.tgt_db,
      table_name: this.task.tgt_table,
      sbjct_area: this.task.sa_code,
      table_desc: this.form.targetDesc,
      custom_condition: this.form.customCondition,
      hdfs_lctn: this.hdLocation,
      user: Util.getNt(),
      columns: this.form.columns.map((col, i) => {
        return {
          order_index: i + 1,
          column_name: col.t_name,
          data_type: col.t_type,
          column_desc: col.t_desc,
          src_type: col.s_type,
          src_column: col.s_name,
          pk_flag: col.t_isPK ? 'Y' : 'N',
          pi_flag: col.s_isPI ? 'Y' : 'N',
          pii_flag: col.t_isPII ? 'Y' : 'N',
          ppi_flag: col.s_isPCI ? 'Y' : 'N',
          mndtry_flag: col.s_isMandatory ? 'Y' : 'N',
          incr_index_flag: col.t_isIncremental ? 'Y' : 'N',
          prtn_flag: col.t_partition ? 'Y' : 'N',
          needed_flag: col.t_inDDL ? 'Y' : 'N',
        };
      }),
    };
    return model;
  }
  get hasPK () {
    return !!this.model.columns.find(col => col.pk_flag === 'Y');
  }
  get hasIncrementalIndex () {
    return !!this.model.columns.find(col => col.incr_index_flag === 'Y');
  }
  get emailList () {
    return Array.from(new Set([this.task.cre_user, this.task.upd_user]))
      .filter(Boolean)
      .map((nt) => nt + '@ebay.com');
  }
  @Watch('task', { immediate: true })
  getHDLocation (t) {
    if (!isEmpty(t)) {
      this.getLocation();
      this.getSourceTableInfo(t.src_table);
    } else {
      this.clear();
    }
  }
  @Watch('dataModel')
  @Watch('sourceTableDetail')
  async setDefaultTargetDesc () {
    const { dataModel, sourceTableDetail, task } = this;
    if (isEmpty(sourceTableDetail) && !isEmpty(task)) {
      this.getSourceTableInfo(task.src_table);
      return;
    }
    if (dataModel) {
      this.form.targetDesc = dataModel.table_desc;
      this.form.customCondition = dataModel.custom_condition;
      this.form.columns = await buildColumnsByDataModel(dataModel.columns, sourceTableDetail.columns);
    } else {
      if (!this.form.targetDesc) {
        this.form.targetDesc = sourceTableDetail.table_desc;
      }
      this.form.columns = await buildInitColumns(sourceTableDetail.columns);
    }
  }
  @Watch('dataModel', { immediate: true })
  getScripts (dataModel) {
    if (this.disabled && dataModel) {
      this.getDDL(this.id);
      this.getUpsertScript(this.id);
    }
  }
  mounted () {
    const { id } = this.$route.params;
    const { clone } = this.$route.query;
    if (id) {
      !this.task && this.task.id !== id && this.getTask(id);
      if (clone) {
        this.getDataModel(clone);
      } else {
        this.getDataModel(id);
      }
    } else {
      this.$router.push('/autoel/task');
    }
  }
  onPrev () {
    if (this.preview) {
      this.$router.push(`/autoel/task/${this.id}/datamodel`);
    } else {
      this.$router.push(`/autoel/task/${this.id}`);
    }
  }
  async onSave () {
    const model = this.model;
    const id = this.id;
    if (this.task.upd_strategy === 'Append' && !this.hasIncrementalIndex) {
      const msg = 'For task with "Append" as update strategy, Incremental Index must be selected';
      this.$alert(msg);
      throw msg;
    }
    if (this.task.upd_strategy === 'Upsert' && (!this.hasIncrementalIndex || !this.hasPK)) {
      const msg = 'For task with "Upsert" as update strategy, Incremental Index and PK(primary key) must be selected';
      this.$alert(msg);
      throw msg;
    }
    if (!model.table_desc) {
      const msg = 'Table description cannot be empty';
      this.$alert(msg);
      throw msg;
    }
    this.saveModel({ id, model });
    this.$message.success('Data Model successfully saved.');
  }
  async onNext () {
    const id = this.id;
    if (!(this.preview || this.disabled)) {
      await this.onSave();
      this.$router.push(`/autoel/task/${id}/datamodel?preview=true`);
    } else {
      this.$router.push(`/autoel/task/${id}/release`);
    }
  }
  async sendToSAOwner () {
    const group = await GroupService.instance.getGroup(GroupIdTemplate.SA(this.task.sa_name));
    const toAddr = group.items.map(i => `${i.id}@ebay.com`);
    await sentToSAOwnerForApproval(toAddr, this.emailList, this.task);
    this.getTask(this.id);
  }
  async onReject (role: 'DA' | 'SA_OWNER') {
    await API.approve({
      id: this.task.id,
      role,
      approved: false,
    });
    this.$message.success('This task has been rejected by ' + role);
    this.$router.push('/autoel');
  }
  async onApprove (role: 'DA' | 'SA_OWNER') {
    await API.approve({
      id: this.task.id,
      role,
    });
    this.$message.success('This task has been approved by ' + role);
    if (role === 'DA' && this.needSAOwnerApproval) {
      await this.sendToSAOwner();
    }
    this.$router.push('/autoel');
  }
}

</script>

<style lang="scss" scoped>
@import '@/styles/global.scss';

.data-model {
  padding-top: 24px;
  padding-bottom: 32px;
  margin: auto;
  width: 95%;
  > div.overview {
    display: flex;
    justify-content: space-between;
    flex-wrap: wrap;
    > * {
      width: 48%;
    }
  }

  & /deep/ .el-checkbox__input.is-disabled.is-checked .el-checkbox__inner::after {
    border-color: #333;
  }
  .custom-condition {
    padding-top: 32px;
    h1 {
      font-size: 16px;
      padding-bottom: 16px;
    }
  }

  .script-wrapper {
    padding-top: 32px;

    h1 {
      font-size: 16px;
    }
    h2 {
      margin-top: 8px;
      margin-bottom: 16px;
      font-size: 14px;
    }

    & ::v-deep .code-display {
      font-size: 12px;
      padding: 8px 16px;
      border: 1px solid #ddd;
      border-radius: 6px;
      overflow: scroll;
      background: #eee;
      max-height: 700px;
      .CodeMirror.cm-s-default {
        background: transparent;
      }
    }
  }
  .data-model-table {
    .warning {
      color: orange;
      & /deep/ .el-input__inner {
        color: orange;
      }
      & /deep/ .el-textarea__inner {
        border-color: orange;
      }
    }
  }
  .btn-wrapper {
    display: flex;
    margin-top: 32px;
    justify-content: space-between;
  }
}
</style>