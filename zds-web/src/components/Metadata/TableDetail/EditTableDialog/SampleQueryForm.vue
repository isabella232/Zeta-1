<template>
  <div class="sample-query-form">
    <el-form
      label-width="150px"
      :model="form"
    >
      <el-form-item
        prop="title"
        label="Title"
        required
      >
        <el-input
          v-model="form.title"
          placeholder="Describe this query"
        />
      </el-form-item>
      <el-form-item
        label="Platform"
        prop="platform"
        required
      >
        <el-select v-model="form.platform">
          <el-option
            label="All"
            value="all"
          />
          <el-option
            v-for="p in platforms"
            :key="p.value"
            :label="p.label"
            :value="p.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item
        label="Query"
        prop="content"
        required
      >
        <SqlEditor
          class="sql-editor"
          :read-only="false"
          :value="form.content"
          @input="val => form.content = val"
        />
      </el-form-item>
    </el-form>
    <div
      v-for="q in myQueries"
      :key="q.sample_query_id"
      class="my-query"
    >
      <el-divider />
      <header>
        <div>
          <h1>{{ q.title }}</h1>
          <i
            v-for="p in (q.platform || q.platforms).split(',')"
            :key="p"
            class="platform-icon"
            :class="p.toLowerCase()"
          />
        </div>
        <div>
          <!-- <i class="el-icon-edit" /> -->
          <el-popconfirm
            title="Are you sure to delete this query?"
            style="padding-left:8px;padding-top:2px;"
            @onConfirm="onDelete(q.sample_query_id)"
          >
            <i 
              slot="reference"
              class="el-icon-delete"
            />
          </el-popconfirm>
        </div>
      </header>
      <SqlEditor
        :value="q.title"
        :options="{ lineNumbers: false }"
        read-only
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
        @click="onSubmit"
      >
        Submit
      </el-button>
    </footer>
  </div>
</template>
<script lang="ts">
import { Component, Mixins } from 'vue-property-decorator';
import { Common } from '../mixins/Common';
import { Action, State } from 'vuex-class';
import { Actions } from '../store';
import { EditorOptions } from '@/components/Metadata/udf/UDFDetail.vue';
import SqlEditor from '@/components/common/Visualization/CodeDisplay.vue';
import DoeRemoteService from '@/services/remote/DoeRemoteService';
import Util from '@/services/Util.service';
import moment from 'moment';
const API = DoeRemoteService.instance;

@Component({
  components: {
    SqlEditor,
  },
})
export default class OverviewForm extends Mixins(Common) {
  @Action(Actions.GetSampleQueries) getSampleQueries;
  @State(state => state.TableDetail.queries) queries;
  editorOpt = EditorOptions;
  platforms = [
    { label: 'Apollo', value: 'apollo_rno' },
    { label: 'Hermes', value: 'hermes' },
    { label: 'Hercules', value: 'hercules' },
    { label: 'Ares', value: 'ares' },
    { label: 'Mozart', value: 'numozart' },
  ];
  form = {
    sample_query_id: null,
    title: '',
    platform: '',
    content: '',
  };

  get myQueries () {
    const nt = Util.getNt();
    return this.queries.filter(q => q.sample_query_id !== undefined && q.upd_user === nt);
  }

  async onSubmit () {
    const params = {
      title: this.form.title,
      platforms: this.form.platform === 'all' ? this.platforms.map(p => p.value).join(',') : this.form.platform,
      query_text: this.form.content,
      table: this.tableName,
      upd_user: Util.getNt(),
      upd_date: moment().utcOffset(-7).format('YYYY-MM-DD HH:mm:ss'),
    };
    await API.addSampleQuery(params);
    this.getSampleQueries(this.tableName);
  }
  async onDelete (id) {
    await API.deleteSampleQuery({ sample_query_id: id });
    this.getSampleQueries(this.tableName);
  }
}
</script>
<style lang="scss" scoped>
@import '@/styles/metadata.scss';

.sample-query-form {
  padding: 24px 0 36px;
  ::v-deep {
    label {
      font-weight: bold;
    }
  }
  .sql-editor {
    min-height: 200px;
    border: 1px solid #ddd;
    line-height: normal;
  }
  .my-query {
    header {
      display: flex;
      justify-content: space-between;
      h1 {
        font-size: 16px;
        display: inline-block;
        & ~ i {
          position: relative;
          top: -2px;
        }
      }
    }
  }
  footer {
    text-align: right;
  }
}
</style>