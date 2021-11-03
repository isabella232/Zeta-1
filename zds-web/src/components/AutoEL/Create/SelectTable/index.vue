<template>
  <div class="step-0">
    <el-form
      ref="form"
      :model="form"
      label-position="top"
    >
      <el-row :gutter="48">
        <el-col :span="12">
          <el-form-item label="Data Landing Type">
            <el-select
              v-model="form.landingType"
              :disabled="disabled"
            >
              <el-option
                label="Oracle -> Hadoop"
                value="Oracle"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item
            label="Subject Area"
            prop="sa"
            :rules="[
              { validator: saValidator, trigger: ['blur', 'change'] },
              {
                required: true,
                message: 'Please choose a Subject Area',
                trigger: ['blur', 'change'],
              },
            ]"
          >
            <el-select
              v-model="form.sa"
              filterable
              :disabled="disabled"
            >
              <el-option
                v-for="sa in saListWithGroups"
                :key="sa.sa_code + sa.sbjct_area"
                :label="`${sa.sbjct_area}(${sa.sa_code})`"
                :value="[sa.sbjct_area, sa.sa_code].join()"
                :disabled="sa.disabled"
              >
                {{ sa.sbjct_area }}({{ sa.sa_code }})
                <div
                  v-if="sa.disabled"
                  style="display: inline-block; float: right; cursor: pointer"
                >
                  <a
                    style="padding-right: 5px"
                    @click.stop="saRequest(sa)"
                  >request</a>
                  <el-tooltip
                    content="Bottom center"
                    placement="bottom"
                    effect="light"
                  >
                    <div slot="content">
                      Click to request access to this subject area
                    </div>
                    <i class="el-icon-question" />
                  </el-tooltip>
                </div>
              </el-option>
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="48">
        <el-col :span="12">
          <el-form-item label="Team">
            <div>
              <a
                :href="`mailto:${saDetail.team_dl}`"
                target="_blank"
              >
                {{ saDetail.team_name }}
              </a>
            </div>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="HD Batch Account">
            <span class="text">
              {{ saDetail.batch_acct }}
            </span>
          </el-form-item>
        </el-col>
      </el-row>
      <el-divider />
      <el-row :gutter="48">
        <el-col :span="12">
          <el-form-item
            prop="sourceTable"
            label="Source Table"
            :rules="[
              {
                required: true,
                message: 'Please choose Source Table',
                trigger: ['blur', 'change'],
              },
            ]"
          >
            <el-select
              v-model="form.sourceTable"
              remote
              filterable
              :remote-method="fetchSourceTableOptions"
              :disabled="disabled"
            >
              <el-option
                v-for="t in sourceTableList"
                :key="t"
                :label="t"
                :value="t"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="Table Size">
            <span class="text">
              {{ tableSize }}
            </span>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="48">
        <el-col :span="12">
          <el-form-item
            prop="hostStr"
            label="Source Host"
            :rules="[
              { validator: dbcValidator, trigger: ['blur'] },
              {
                required: true,
                message: 'Please input source host',
                trigger: ['blur', 'change'],
              },
            ]"
          >
            <div
              v-if="!disabled"
              class="dbc-controls"
            >
              <el-tooltip placement="top">
                <div slot="content">
                  Please update this field if table and dbc connection provided
                  here is not right or empty.
                  <br>
                  Format for multi-host table should be like this (table and dbc
                  separated by comma):
                  <br>
                  table1,test.dbc
                  <br>
                  table2,test.dbc
                </div>
                <i
                  class="el-icon-warning"
                  style="cursor: pointer; margin-left: 6px; float: left"
                />
              </el-tooltip>
              <div
                v-if="dbcValidating"
                style="padding-left: 8px; color: #569ce1"
              >
                <i class="el-icon-loading" />Validating, it may take a few
                minutes...
              </div>
              <template v-else>
                <el-switch
                  v-model="manualHostInput"
                  active-text="Manual"
                  style="top: -8px; margin-right: 16px"
                />
                <el-button
                  type="primary"
                  size="mini"
                  round
                  style="position: relative; top: -8px"
                  icon="el-icon-download"
                  :disabled="!sourceTableDetail.host"
                  @click="autoMap"
                >
                  Reset
                </el-button>
              </template>
            </div>
            <el-input
              v-if="manualHostInput || disabled"
              v-model="form.hostStr"
              type="textarea"
              :rows="host.length + 1"
              style="margin: 18px 0 8px"
              :disabled="disabled"
              :autosize="{ minRows: 4 }"
            />
            <source-host-table
              v-else
              v-loading="loading"
              :source-table-detail="sourceTableDetail.host"
              :hosts="host"
              @change="(val) => (form.hostStr = val)"
            />
            <a
              v-if="dbcValidationResult.invalid.length > 0"
              @click="() => (showDBCErrors = true)"
            >
              DBC Validation Errors
            </a>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="Schema">
            <a
              v-if="form.sourceTable"
              @click="showSchema = true"
            >View Detail</a>
            <span
              v-else
              class="text"
            >Please select a Source Table</span>
          </el-form-item>
          <el-form-item label="Sample Data">
            <a
              v-if="dbcValidationResult.valid.length > 0"
              @click="showSample = true"
            >View Detail</a>
            <span
              v-else
              class="text"
            >Please select a subject area and a source table with valid source
              hosts</span>
          </el-form-item>
        </el-col>
      </el-row>
      <el-divider />
      <el-row :gutter="48">
        <el-col :span="12">
          <el-form-item
            label="Target Platform"
            size="small"
          >
            <el-checkbox-group
              v-model="form.platform"
              style="margin-top: 8px"
              :disabled="disabled"
              :min="1"
            >
              <el-checkbox-button label="hercules">
                Hercules
              </el-checkbox-button>
              <el-checkbox-button label="apollo_rno">
                Apollo RNO
              </el-checkbox-button>
              <el-checkbox-button label="ares">
                Ares
              </el-checkbox-button>
            </el-checkbox-group>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="Target DB">
            <span class="text">
              {{ form.targetDB }}
            </span>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="48">
        <el-col :span="12">
          <el-form-item
            prop="targetTable"
            label="Target Table"
            :rules="[
              {
                required: true,
                message: 'Please choose Target Table',
                trigger: ['blur', 'change'],
              },
            ]"
          >
            <el-input
              v-model="form.targetTable"
              :disabled="disabled"
            >
              <template
                v-if="form.frequency === 'one_time'"
                slot="append"
              >
                _w
              </template>
            </el-input>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="Update Frequency">
            <div>
              <el-select
                v-model="form.frequency"
                :disabled="disabled"
              >
                <el-option
                  label="Frequent"
                  value="Frequent"
                />
                <el-option
                  label="Daily"
                  value="Daily"
                />
                <el-option
                  label="Weekly"
                  value="Weekly"
                />
                <el-option
                  label="Monthly"
                  value="Monthly"
                />
                <el-option
                  label="One Time"
                  value="one_time"
                />
              </el-select>
              <span
                v-if="form.frequency === 'Frequent'"
                style="padding-left: 1em"
              >
                Every
                <el-select
                  v-model="form.frequencyByHour"
                  :disabled="disabled"
                  style="width: 64px"
                >
                  <el-option
                    v-for="i in [[1, 'ONE'], [2, 'TWO'], [3, 'THREE'], [4, 'FOUR'], [6, 'SIX'], [8, 'EIGHT'], [12, 'TWELVE']]"
                    :key="i[0]"
                    :label="i[0]"
                    :value="i[1]"
                  />
                </el-select>
                Hour(s)
              </span>
            </div>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="48">
        <el-col :span="12">
          <el-form-item label="Update Strategy">
            <el-select
              v-model="form.strategy"
              :disabled="disabled"
            >
              <el-option
                label="Upsert"
                value="Upsert"
              />
              <el-option
                label="Full Table"
                value="Full Table"
              />
              <el-option
                label="Append"
                value="Append"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row
        type="flex"
        justify="space-between"
        style="flex-direction: row-reverse"
      >
        <el-col :span="3">
          <el-form-item>
            <el-button
              type="primary"
              :loading="loading || dbcValidating"
              @click="onNext"
            >
              Next
            </el-button>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <DBCErrors
      :show="showDBCErrors"
      :data="dbcValidationResult.invalid"
      :loading="loading"
      @close="showDBCErrors = false"
    />
    <schema-dialog
      :show="showSchema"
      :table-name="form.sourceTable"
      @close="showSchema = false"
    />
    <sample-dialog
      :show="showSample"
      :table-name="form.sourceTable"
      :dbc="firstValidDBC"
      :sa-code="saDetail.sa_code"
      @close="showSample = false"
    />
  </div>
</template>

<script lang="ts">
import { Component, Mixins, Vue, Watch } from 'vue-property-decorator';
import { Actions, Mutations } from '@/components/AutoEL/store';
import { Action, State, Mutation, Getter } from 'vuex-class';
import AutoELService from '@/services/remote/AutoELService';
import {
  Actions as GroupActions,
  Mutations as GroupMutations,
} from '@/stores/modules/GroupStore';
import { Task } from '../../types';
import SourceHostTable from '@/components/AutoEL/Create/SelectTable/SourceHostTable.vue';
import Util from '@/services/Util.service';
import { attempt, isNullOrEmpty } from '@drewxiu/utils';
import DBCInfoService from './DBCInfoService';
import SampleDialog from './dialogs/Sample.vue';
import SchemaDialog from './dialogs/Schema.vue';
import DBCErrors from './dialogs/DBCErrors.vue';
import { groupBy, keyBy } from 'lodash';
import GroupService from '@/services/remote/GroupService';
import DoeRemoteService from '@/services/remote/DoeRemoteService';
import { FormStatus } from '../mixins/Form';
import { GroupIdTemplate } from '@/components/common/AccessControl.vue';

const API = AutoELService.instance;

@Component({
  components: {
    SchemaDialog,
    SampleDialog,
    DBCErrors,
    SourceHostTable,
  },
})
export default class SelectTable extends Mixins(FormStatus) {
  @Action(Actions.GetTask) getTask;
  @Action(Actions.CloneTask) cloneTask;
  @Action(Actions.GetSAList) getSAList;
  @Action(Actions.GetSourceTableList) getSourceTableList;
  @Action(Actions.GetSourceTableInfo) getSourceTableInfo;
  @Action(GroupActions.GetAdminGroup) getAdminGroup;
  @Mutation(Mutations.Clear) clear;
  @Mutation(GroupMutations.SetGroup) setGroup;

  @State((state) => state.GroupManagement.groups) groups;
  @State((state) => state.AutoEL.task)
  task;
  @State((state) => state.AutoEL.saList)
  saList;
  @State((state) => state.AutoEL.sourceTables)
  sourceTableList;
  @State((state) => state.AutoEL.sourceTableInfo)
  sourceTableInfo;
  @State((state) => state.AutoEL.tableOnPlatform)
  tableOnPlatform;
  @Getter('$globalLoading') loading;
  @State((state) => state.user.user.profile) userProfile;
  form: any = {
    landingType: 'Oracle',
    sa: null,
    sourceTable: null,
    targetTable: null,
    targetDB: null,
    platform: ['hercules'],
    frequency: 'Daily',
    frequencyByHour: 1,
    strategy: 'Upsert',
    hostStr: '',
  };

  manualHostInput = false;
  showSchema = false;
  showSample = false;
  showDBCErrors = false;
  clone: string | null = null;
  tableSize = 'Please select a subject area and a source table with valid source hosts';
  dbcValidationResult = {
    invalid: [] as any,
    valid: [] as any,
  };
  dbcValidating = false;
  get userInfo () {
    return this.userProfile.firstName + ', ' + this.userProfile.lastName;
  }
  nt = Util.getNt();

  myGroups: any = null;

  get saListWithGroups () {
    if (isNullOrEmpty(this.saList) || isNullOrEmpty(this.myGroups)) {
      return [];
    }
    return this.saList
      .map((sa) => {
        return {
          ...sa,
          disabled: !this.myGroups[GroupIdTemplate.SA(sa.sbjct_area)],
        };
      })
      .sort((a) => (a.disabled ? 1 : -1));
  }

  get saDetail (): any {
    return attempt(
      () =>
        this.saList.find(
          (sa) => `${sa.sbjct_area},${sa.sa_code}` === this.form.sa
        ) || {},
      {}
    );
  }
  get sourceTableDetail () {
    return this.sourceTableInfo[this.form.sourceTable] || {};
  }
  get host () {
    if (!this.form.hostStr) return [];
    return this.form.hostStr.split('\n').map((h) => {
      const [table_name, dbc_name] = h.split(',');
      return {
        table_name,
        dbc_name,
      };
    });
  }
  get firstValidDBC () {
    return attempt(() => this.dbcValidationResult.valid[0]);
  }

  @Watch('manualHostInput')
  @Watch('form.hostStr')
  clearValidation () {
    this.dbcValidationResult.invalid.length = 0;
    this.dbcValidationResult.valid.length = 0;
    this.formRef.clearValidate();
  }
  @Watch('task', { immediate: true })
  setForm (task: Task) {
    if (!task) return;
    let frequency = task.upd_freq;
    let hour = '1';
    if (frequency.startsWith('HOURLY_')) {
      frequency = 'Frequent';
      hour = task.upd_freq.split('_')[1];
    }
    this.form = {
      ...this.form,
      landingType: task.src_type,
      sa: [task.sa_name, task.sa_code].join(),
      sourceTable: task.src_table,
      targetTable: task.tgt_table,
      platform: task.tgt_platform.split(','),
      frequency: frequency,
      frequencyByHour: hour,
      strategy: task.upd_strategy,
      hostStr: task.source_list
        .map((h) => `${h.table_name},${h.dbc_name}`)
        .join('\n'),
    };
    if (task.source_list.length > 0) {
      Vue.nextTick(() => this.formRef.validate());
    }
  }
  @Watch('host')
  @Watch('saDetail.sa_code')
  async computeTableSize () {
    const {
      host,
      saDetail: { sa_code },
    } = this;
    let msg = '';
    if (host.length && sa_code) {
      this.tableSize = 'Loading...';
      const { total_table_size } = await API.getTableSize(sa_code, host);
      msg = total_table_size < 0 ? 'Unknown' : total_table_size + ' records';
    } else {
      msg =
        'Please select a subject area and a source table with valid source hosts';
    }
    this.tableSize = msg;
  }
  @Watch('form.sourceTable')
  getSourceTableDetail (val) {
    if (!this.sourceTableInfo[val]) {
      this.getSourceTableInfo(val);
    }
    if (!this.disabled) {
      this.form.targetTable = val;
      this.form.hostStr = '';
    }
  }
  @Watch('form.sourceTable')
  fetchSourceTableOptions (query: string = this.form.sourceTable) {
    this.getSourceTableList(query);
  }
  @Watch('sourceTableDetail.host')
  initSourceHost (host) {
    if (host && !this.disabled && !this.form.hostStr) {
      this.autoMap();
      Vue.nextTick(() => this.formRef.validate());
    }
  }
  @Watch('saDetail.target_db', { immediate: true })
  @Watch('form.frequency')
  setTargetDB () {
    if (this.form.frequency === 'one_time') {
      this.form.targetDB = 'hadoop_test_w';
    } else {
      this.saDetail && (this.form.targetDB = this.saDetail.target_db);
    }
  }
  async onNext () {
    if (this.disabled) {
      return this.$router.push(`/autoel/task/${this.task.id}/datamodel`);
    }
    this.formRef.validate(async (isValid) => {
      if (!isValid) {
        return false;
      }
      const { form, saDetail, task } = this;
      if (!form.hostStr || !saDetail.batch_acct || !saDetail.target_db) {
        this.$alert(
          'HD Batch Account, Source Host, Target DB cannot be empty, please contact support <a href="mailto:DL-eBay-Metadata@ebay.com">DL-eBay-Metadata@ebay.com</a> for help.',
          'Oooops...',
          { dangerouslyUseHTMLString: true }
        );
        return;
      }
      const tableOnPlatform = await API.getTableOnPlatform(
        form.targetTable,
        saDetail.target_db
      );
      if (tableOnPlatform.length > 0) {
        await this.$confirm(
          'This table already exists in Hercules and Apollo_rno. This task will drop exsiting table and create a new one. It will require manual approval. Do you want to proceed?',
          { title: 'This task may cause an overwrite!' }
        );
      }
      const taskForm: Task = {
        id: task && task.id,
        src_type: form.landingType,
        src_platform: '',
        src_db: '',
        src_table: form.sourceTable,
        tgt_type: 'HD',
        tgt_platform: form.platform.join(','),
        tgt_db: form.targetDB,
        tgt_table: form.targetTable,
        sa_code: saDetail.sa_code,
        sa_name: saDetail.sbjct_area,
        upd_freq:
          form.frequency === 'Frequent'
            ? `HOURLY_${form.frequencyByHour}`
            : form.frequency,
        upd_strategy: form.strategy,
        user: Util.getNt(),
        source_list: this.host,
        user_full_name: this.userInfo,
      };
      const resp = taskForm.id
        ? await API.updateTask(taskForm)
        : await API.createTask(taskForm);
      const id = resp.id;
      this.getTask(id);
      this.$router.push(
        `/autoel/task/${id}/datamodel${
          this.clone ? `?clone=${this.clone}` : ''
        }`
      );
    });
  }
  mounted () {
    this.getSAList();
    this.getSourceTableList();
    GroupService.instance
      .getGroups({ iq: this.nt })
      .then((groups) => (this.myGroups = keyBy(groups, 'id')));
    const { id } = this.$route.params;
    const { clone } = this.$route.query;
    if (id) {
      this.getTask(id);
    } else {
      this.clear();
    }
    if (clone) {
      this.clone = clone as string;
      this.cloneTask(clone);
    }
  }
  autoMap () {
    this.form.hostStr = this.sourceTableDetail.host
      .map((host) => {
        return `${host.table_name},${host.list[0].dbc_name}`;
      })
      .join('\n');
  }
  saValidator (rule, value = '', callback) {
    if (!value) {
      callback();
      return;
    }
    const [sbjct_area, sa_code] = value.split(',');
    const sa = this.saListWithGroups.find(
      (sa) => sa.sbjct_area === sbjct_area && sa.sa_code === sa_code
    );
    if (sa && sa.disabled) {
      callback(new Error('Permission denied, please request access first.'));
      return;
    }
    callback();
  }
  dbcValidator (rule, value, callback) {
    for (const row of this.host) {
      if (!row.dbc_name || !row.table_name) {
        callback(new Error('Incorrect format'));
        return;
      }
    }
    this.dbcValidating = true;
    return DBCInfoService.validate(this.host, this.saDetail.sa_code)
      .then((res) => {
        const groupedResult = groupBy(res, 'isValid');
        this.dbcValidationResult.valid = groupedResult['true'] || [];
        this.dbcValidationResult.invalid = groupedResult['false'] || [];
      })
      .finally(() => {
        callback(
          this.dbcValidationResult.invalid.length > 0
            ? new Error('Invalid DBC, please check the link above')
            : undefined
        );
        this.dbcValidating = false;
      });
  }
  async saRequest (sa) {
    const groupId = GroupIdTemplate.SA(sa.sbjct_area);
    if (!this.groups[groupId]) {
      this.setGroup(await GroupService.instance.getGroup(groupId));
    }
    const group = this.groups[groupId];
    const requestor = `${this.userInfo}(${this.nt})`;
    const content = {
      name: 'Subject Area Owners',
      msg: `
        ${requestor} requests access to subject area ${sa.sbjct_area}. Please visit <a href="${location.origin}/${Util.getPath()}/#/metadata/sa/${sa.sbjct_area}">SA Management</a> page to approve this request by adding ${requestor} to the Access Control Group. 
        This access control is used by DOE applications including metadata update, Auto EL oracle table landing, etc.
      `,
    };
    const ccAddr = [
      `${this.nt}@ebay.com`,
      'zxiu@ebay.com',
      'binsong@ebay.com',
      'qxu1@ebay.com',
    ].join(';');
    const toAddr =
      process.env.VUE_APP_ENV == 'production'
        ? group.items.map((i) => `${i.id}@ebay.com`).join(';')
        : ccAddr;
    const param = {
      fromAddr: 'DL-eBay-Metadata@ebay.com',
      subject: `Request Subject Area [${sa.sbjct_area}] Access for ${requestor}`,
      content: JSON.stringify(content),
      ccAddr,
      toAddr,
      template: 'ZetaNotification',
      type: 3,
    };
    await DoeRemoteService.instance.createEmail(param);
    this.$message.success('Request was sent via Email.');
  }
}
</script>

<style lang="scss" scoped>
@import '@/styles/global.scss';

.step-0 {
  padding-top: 24px;
  display: flex;
  justify-content: space-around;
  a {
    cursor: pointer;
  }
  .dbc-controls {
    position: absolute;
    z-index: 1;
    right: 0;
    top: -30px;
    text-align: right;
    width: calc(100% - 80px) !important;
  }
  /deep/ form {
    min-width: 85%;
    display: grid;
  }

  /deep/ .el-checkbox-button.is-disabled.is-checked {
    .el-checkbox-button__inner {
      background-color: darken(#569ce1, 10%);
      color: white;
    }
  }

  span.text {
    color: #999;
    word-break: break-word;
    display: inline-block;
    max-height: 112px;
    overflow-y: auto;
  }
  /deep/ label.el-form-item__label {
    padding-bottom: 0;
  }
  /deep/ div.el-form-item__content > * {
    min-width: 50%;
    width: 100%;
  }
}
</style>
