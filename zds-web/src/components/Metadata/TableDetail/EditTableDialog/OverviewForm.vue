<template>
  <div class="overview-form">
    <el-form
      label-width="100px"
      :model="form"
    >
      <el-form-item label="Description">
        <quill-editor
          v-model="form.desc"
          :options="editorOpt"
        />
      </el-form-item>
      <el-form-item label="Labels">
        <el-select
          v-model="form.labels"
          multiple
          filterable
          allow-create
          default-first-option
          placeholder="Create tags for this table"
          style="width:100%;"
        >
          <el-option
            v-for="l in form.labels"
            :key="l"
            :label="l"
            :value="l"
          />
        </el-select>
      </el-form-item>
    </el-form>
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
import { Getter, Action, State } from 'vuex-class';
import { Getters, Actions } from '../store';
import { quillEditor } from 'vue-quill-editor';
import { EditorOptions } from '@/components/Metadata/udf/UDFDetail.vue';
import DoeRemoteService from '@/services/remote/DoeRemoteService';
const API = DoeRemoteService.instance;

@Component({
  components: {
    quillEditor,
  },
})
export default class OverviewForm extends Mixins(Common) {
  @Action(Actions.GetTableInfo) getTableInfo;
  @Getter(Getters.TableSample) tableSample;
  @State(state => state.TableDetail.labels) labels;
  editorOpt = EditorOptions;
  form = {
    desc: '',
    sa: '',
    sae: [],
    team: '',
    labels: [],
  };

  @Watch('labels', { immediate: true })
  @Watch('tableSample', { immediate: true })
  initForm () {
    if (this.tableSample) {
      this.form.desc = this.tableSample.table_desc;
    }
    if (this.labels) {
      this.form.labels = this.labels;
    }
  }
  async onSave () {
    await Promise.all([API.addTblDesc(this.tableName, this.form.desc), API.replaceTableLabels(this.tableName, this.form.labels)]);
    this.getTableInfo({ table: this.tableName });
    this.$emit('saved');
  }
}
</script>
<style lang="scss" scoped>

.overview-form {
  padding: 24px 0 36px;
  ::v-deep {
    label {
      font-weight: bold;
    }
  }
  footer {
    text-align: right;
  }
}
</style>