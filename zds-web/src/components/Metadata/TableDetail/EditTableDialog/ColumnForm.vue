<template>
  <div class="column-form">
    <header>
      <el-select v-model="platform">
        <el-option
          v-for="p in platformOptions"
          :key="p.value"
          :label="p.label"
          :value="p.value"
        />
      </el-select>
      <el-radio-group
        v-model="db"
        class="light"
        style="margin-left:12px;"
      >
        <el-radio
          v-for="d in columnDBOptions"
          :key="d"
          :label="d"
        >
          {{ d || 'DEFAULT' }}
        </el-radio>
      </el-radio-group>
    </header>
    <el-table
      :data="selectedColumns"
      :default-sort="{prop: 'column_id', order: 'descending'}"
    >
      <el-table-column
        prop="column_name"
        label="Column Name"
      >
        <template slot-scope="scope">
          {{ scope.row.column_name.toUpperCase() }}
        </template>
      </el-table-column>
      <el-table-column
        prop="column_desc"
        label="Description"
        min-width="300"
      >
        <template slot-scope="scope">
          <el-input
            v-model="columnForm[scope.$index].column_desc"
            :class="changedColumnsKeyByIndex[scope.$index] ? 'changed-row' : ''"
          />
        </template>
      </el-table-column>
      <el-table-column
        prop="ppi_flag"
        label="Partition"
        width="110"
        align="center"
        sortable
      >
        <template slot-scope="scope">
          <div style="text-align: center; font-size: 25px;">
            <i
              v-if="scope.row.ppi_flag == 'Y'"
              class="zeta-icon-finish"
              style="color: #569ce1;"
            />
          </div>
        </template>
      </el-table-column>
      <el-table-column
        prop="bucket_flag"
        label="Bucket"
        width="100"
        align="center"
        sortable
      >
        <template slot-scope="scope">
          <div style="text-align: center; font-size: 25px;">
            <i
              v-if="scope.row.bucket_flag == 'Y'"
              class="zeta-icon-finish"
              style="color: #569ce1;"
            />
          </div>
        </template>
      </el-table-column>
      <el-table-column
        prop="pii_flag"
        label="PII"
        width="100"
        align="center"
        sortable
      >
        <template slot-scope="scope">
          <div style="text-align: center; font-size: 25px;">
            <i
              v-if="scope.row.pii_flag == 'Y'"
              class="zeta-icon-finish"
              style="color: #569ce1;"
            />
          </div>
        </template>
      </el-table-column>
      <el-table-column
        prop="data_type"
        label="Type"
        width="150"
      >
        <template slot-scope="scope">
          <div class="col-div">
            <div class="content-center">
              {{ scope.row.data_type ? scope.row.data_type.toUpperCase() : "" }}
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column
        prop="sampleData"
        label="Sample Data"
        width="120"
      >
        <template slot-scope="scope">
          <div class="ceil sample-data-div">
            <i class="zeta-icon-result" />
            <span v-html="scope.row.sampleData" />
          </div>
        </template>
      </el-table-column>
    </el-table>
    <el-divider />
    <footer>
      <el-button @click="() => $emit('cancel')">
        Cancel
      </el-button>
      <el-button
        type="primary"
        icon="el-icon-circle-check"
        :disabled="changedColumns.length === 0"
        @click="onSave"
      >
        Save
      </el-button>
    </footer>
  </div>
</template>
<script lang="ts">
import { keyBy } from 'lodash';
import { Component, Mixins, Watch } from 'vue-property-decorator';
import { Common } from '../mixins/Common';
import { TableInfoMixin } from '../mixins/TableInfo';
import moment from 'moment';
import DoeRemoteService from '@/services/remote/DoeRemoteService';
import Util from '@/services/Util.service';
import { isEmpty } from '@drewxiu/utils';
const API = DoeRemoteService.instance;

const Labels = {
  Apollo_rno: 'Apollo',
  Hermes: 'Hermes',
  Hercules: 'Hercules',
  Ares: 'Ares',
  NuMozart: 'Mozart',
};

@Component({
  components: {
  },
})
export default class ColumnForm extends Mixins(Common, TableInfoMixin) {

  columnForm: any[] = [];
  showChanges = false;
  get platformOptions () {
    return this.platforms.map(p => ({ label: Labels[p.toLowerCase()], value: p }));
  }

  get changedColumns () {
    const changed: any[] = [];
    for (const i in this.columnForm) {
      const formItem = this.columnForm[i];
      const original = this.selectedColumns[i];
      if (formItem.column_desc !== original.column_desc) {
        changed.push({
          index: i,
          original,
          changed: formItem,
        });
      }
    }
    return changed;
  }
  get changedColumnsKeyByIndex () {
    return keyBy(this.changedColumns, 'index');
  }
  @Watch('selectedColumns')
  initForm () {
    this.columnForm = this.selectedColumns.map(c => ({
      column_desc: c.column_desc,
    }));
  }
  async onSave () {
    const upd_date = moment().utcOffset(-7).format('');
    const upd_user = Util.getNt();
    const body = {
      columns: this.changedColumns.map(c => ({
        table: this.tableName,
        name: c.original.column_name.toUpperCase(),
        upd_date,
        upd_user,
        desc: c.changed.column_desc,
        desc_html: `<p>${c.changed.column_desc}</p>`,
      })),
    };
    await API.addColDesc(body);
    this.reloadTableInfo(this.tableName);
  }
  mounted () {
    if (isEmpty(this.tables)) {
      this.reloadTableInfo(this.tableName);
    }
  }
}
</script>
<style lang="scss" scoped>

.column-form {
  padding: 0 0 36px;
  header {
    padding: 12px 0 18px;
    ::v-deep {
      .light.el-radio-group {
        label {
          margin-right: 0;
          &:not(:last-of-type)::after {
            content: '';
            display: inline-block;
            width: 1px;
            background: #ccc;
            height: 18px;
            vertical-align: sub;
            margin-left: 18px;
          }
          .el-radio__input {
            display: none;
          }
          .el-radio__label {
            font-weight: normal;
          }
        }
      }
    }
  }
  ::v-deep {
    thead {
      th {
        background-color: #F5F5F5;
        padding: 2px 0;
      }
    }
    .el-table .changed-row input{
      color: orange;
    }
  }
  footer {
    text-align: right;
  }
}
</style>