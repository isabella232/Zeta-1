<template>
  <div id="platform" class="platform">
    <header class="sticky-header">
      <span style="padding-right:24px;">Platform</span>
      <el-radio-group v-model="platform">
        <el-radio v-for="p in platforms" :key="p" :label="p">
          {{ p }}
        </el-radio>
      </el-radio-group>
    </header>
    <el-collapse v-model="activeSection">
      <el-collapse-item name="summary">
        <template slot="title">
          <h1 id="summary">
            Summary
          </h1>
        </template>
        <section>
          <el-row>
            <el-col :span="12">
              <labeled>
                <label slot="label">View</label>
                <div>
                  <p v-for="v in viewFromSamePlatform" :key="v.view_db + v.view_name">
                    <span> {{ v.view_db }}.{{ v.view_name }} </span>
                    <i
                      v-if="v.view_db.toUpperCase() == 'BATCH_VIEWS'"
                      style="font-size:1em;color:red"
                      class="zeta-icon-info"
                      title="Batch view only serve for data development purpose, no analytics usage allowed."
                    />
                    <i class="zeta-icon-hot" style="font-size:1em;color:#cacbcf">{{
                      v.distinct_batch_cnt + v.distinct_user_cnt
                    }}</i>
                  </p>
                </div>
              </labeled>
            </el-col>
            <el-col :span="12">
              <labeled>
                <label slot="label">Table</label>
                <div>
                  <p v-for="t in tableFromSamePlatform" :key="t.db_name + t.table_name">
                    <span> {{ t.db_name || 'DEFAULT' }}.{{ t.table_name }} </span>
                    <i class="zeta-icon-hot" style="font-size:1em;color:#cacbcf">{{
                      t.distinct_batch_cnt + t.distinct_user_cnt
                    }}</i>
                  </p>
                </div>
              </labeled>
            </el-col>
          </el-row>
        </section>
      </el-collapse-item>
      <el-collapse-item name="columns">
        <template slot="title">
          <h1 id="columns">
            Columns
          </h1>
        </template>
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
        <columns :data="selectedColumns" />
      </el-collapse-item>
      <el-collapse-item name="sample">
        <template slot="title">
          <div id="sample" style="display:flex;width:100%;justify-content:space-between;">
            <h1 style="width:auto;">
              Sample Queries
            </h1>
            <join-table />
          </div>
        </template>
        <samples :table="tableName" :platform="platform" />
      </el-collapse-item>
    </el-collapse>
  </div>
</template>
<script lang="ts">
import { Component, Mixins, Watch } from 'vue-property-decorator';
import Labeled from '@/components/AutoEL/Create/components/Labeled.vue';
import Columns from './Columns.vue';
import Samples from './Samples.vue';
import JoinTable from './JoinTable.vue';
import { Common } from '../mixins/Common';
import { TableInfoMixin } from '../mixins/TableInfo';

@Component({
  components: {
    Labeled,
    Columns,
    Samples,
    JoinTable
  }
})
export default class Platform extends Mixins(Common, TableInfoMixin) {
  activeSection = ['summary', 'columns', 'sample'];

  @Watch('tableName', { immediate: true })
  getView(table) {
    this.getViewInfo(table);
  }
}
</script>
<style lang="scss" scoped>
.platform {
  padding: 0 12px;
  ::v-deep {
    .el-collapse-item__wrap,
    .el-collapse {
      border-top: none;
      border-bottom: none;
    }
    .el-collapse-item__header {
      flex-direction: row-reverse;
    }
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
  h1 {
    font-size: 15px;
    width: 100%;
  }
  label {
    font-weight: bold;
    text-align: right;
    padding-right: 12px;
    font-size: 14px;
  }
  section {
    overflow: hidden;
    transition: all ease-in-out 0.5s;
    max-height: 100%;
    .collapsed {
      max-height: 24px;
    }
  }
}
</style>
