<template>
  <div class="operation-form">
    <div class="done-file-wrapper">
      <h1>Register Done File</h1>
      <el-form :model="doneFileForm">
        <el-form-item>
          <el-select
            v-model="doneFileForm.db"
            placeholder="Database"
          >
            <el-option
              label="DEFAULT"
              value=""
            />
            <el-option
              v-for="db in databases"
              :key="db"
              :label="db"
              :value="db"
            />
          </el-select>
          <span>.{{ tableName }}</span>
        </el-form-item>
        <el-form-item>
          <el-radio-group v-model="doneFileForm.type">
            <el-radio label="static">
              Static
            </el-radio>
            <el-radio label="uow">
              Unit of Work (UOW)
            </el-radio>
            <el-radio label="non-uow">
              Non-UOW
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-checkbox-group
          v-show="doneFileForm.type == 'static'"
          v-model="doneFileForm.platforms"
          :min="1"
        >
          <el-checkbox
            v-for="p in platformOptions"
            :key="p.value"
            :label="p.value"
          >
            {{ p.label }}
          </el-checkbox>
        </el-checkbox-group>
        <el-form-item v-show="doneFileForm.type != 'static'">
          <el-table
            :data="doneFileForm.list"
            stripe
            style="width: 100%"
            max-height="400"
          >
            <el-table-column
              prop="platforms"
              label="Platform"
              width="325"
            >
              <template slot-scope="scope">
                <el-checkbox-group
                  v-model="scope.row.platforms"
                  :min="1"
                >
                  <el-checkbox
                    v-for="p in platformOptions"
                    :key="p.value"
                    :label="p.value"
                  >
                    {{ p.label }}
                  </el-checkbox>
                </el-checkbox-group>
              </template>
            </el-table-column>
            <el-table-column
              prop="donefile"
              label="Done File Name"
            >
              <template slot-scope="scope">
                <el-input
                  v-model="scope.row.donefile"
                  style="width: calc(100% - 30px)"
                  :placeholder="doneFileForm.type == 'uow' ? 'dw_lstg.dw_lstg_item.done' : 'dw_lstg.dw_lstg_item.done.YYYYMMDD000000'"
                />
                <el-tooltip
                  content="Bottom center"
                  placement="bottom"
                  effect="light"
                  popper-class="dependency-info-popup"
                >
                  <div slot="content">
                    <p>We support done files under folder /dw/etl/home/prod/watch on ETL Servers.</p>
                    <p v-if="doneFileForm.type == 'uow'">
                      Standard done file format should be ${sa_code}.${table_name}.done.${uow_to}.
                    </p>
                    <p v-if="doneFileForm.type == 'non-uow'">
                      Standard done file format should be ${sa_code}.${table_name}.done.
                    </p>
                    <p>Currently we don't support done file on Hadoop HDFS.</p>
                  </div>
                  <i class="el-icon-warning" />
                </el-tooltip>
              </template>
            </el-table-column>
            <el-table-column
              v-if="doneFileForm.list.length > 1"
              width="70"
            >
              <template slot-scope="scope">
                <i
                  class="el-icon-close"
                  @click="onDelete(scope.$index)"
                />
              </template>
            </el-table-column>
          </el-table>
          <el-button
            icon="el-icon-circle-plus-outline"
            style="border:none;"
            @click="onAdd"
          >
            Add
          </el-button>
        </el-form-item>
      </el-form>

      <el-alert
        v-for="e in errors"
        :key="e"
        :title="e"
        :closable="false"
        type="error"
        style="margin-top:6px"
        show-icon
      />
    </div>

    <el-divider />
    <footer>
      <el-button @click="() => $emit('cancel')">
        Cancel
      </el-button>
      <el-button
        type="primary"
        icon="el-icon-circle-check"
        @click="onSave"
      >
        Save
      </el-button>
    </footer>
  </div>
</template>
<script lang="ts">
import { Component, Mixins, Watch } from 'vue-property-decorator';
import { Common } from '../mixins/Common';
import { TableInfoMixin } from '../mixins/TableInfo';
import DoeRemoteService from '@/services/remote/DoeRemoteService';
import { isEmpty } from '@drewxiu/utils';
import Util from '@/services/Util.service';
const API = DoeRemoteService.instance;

@Component({
  components: {
  },
})
export default class OperationForm extends Mixins(Common, TableInfoMixin) {
  doneFileForm = {
    db: '',
    type: 'uow',
    platforms: ['apollo_rno', 'hercules'],
    list: [{platforms: ['apollo_rno', 'hercules'], donefile:''}],
  };
  platformOptions = [
    { label: 'Apollo Rno', value: 'apollo_rno' }, 
    { label: 'Hercules', value: 'hercules' },
  ];
  errors: string[] = [];

  @Watch('databases')
  initDB () {
    if (!isEmpty(this.databases)) {
      this.doneFileForm.db = this.databases[0];
    } else {
      this.doneFileForm.db = '';
    }
  }
  onAdd () {
    this.doneFileForm.list.push({platforms: ['apollo_rno', 'hercules', 'numozart'], donefile:''});
    
  }
  onDelete (index) {
    this.doneFileForm.list.splice(index, 1);
  }
  async onSave () {
    this.errors.length = 0;
    const user = Util.getNt();
    const table = this.tableName;
    const { db, type } = this.doneFileForm;
    try {
      await API.registerDonefile(this.doneFileForm.list.map(i => ({
        db,
        type,
        user,
        table,
        platforms: i.platforms.join(','),
        donefile: i.donefile,
      })));
    } catch (e) {
      e.resolve();
      if (e.message.data) {
        this.errors = (e.message.data.map(i => `${i.donefile} not found on: ${i.missingAt}`));
      } else {
        this.errors.push(e.message.msg);
      }
    }
  }
}
</script>
<style lang="scss" scoped>

.operation-form {
  padding: 6px 0 36px;

  ::v-deep {
    thead th {
      background-color: #F5F5F5;
      padding: 0;
    }
  }
  .done-file-wrapper {
    h1 {
      font-size: 15px;
      padding-bottom: 6px;
    }
  }
  footer {
    text-align: right;
  }
}
</style>