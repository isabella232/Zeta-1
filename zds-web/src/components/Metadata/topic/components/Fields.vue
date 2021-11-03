<template>
  <div>
    <el-table
      :data="fields"
      border
      height="calc(100vh - 240px)"
    >
      <el-table-column
        property="field"
        label="Field Name"
        width="300"
      />
      <el-table-column
        property="data_type"
        label="Type"
        width="200"
      />
      <el-table-column
        label="Mandatory"
        width="90"
        style="text-align:center;"
      >
        <template slot-scope="scope">
          <div style="text-align:center;">
            <i
              v-if="!scope.row.nullable"
              class="zeta-icon-finish"
              style="color:#4d8cca;font-size:1.5em;"
            />
          </div>
        </template>
      </el-table-column>
      <el-table-column
        property="description"
        label="Description"
      />
      <el-table-column
        property="sample_data"
        label="Sample Data"
        width="200"
      />
    </el-table>
  </div>
</template>

<script lang="ts">
/* eslint-disable @typescript-eslint/camelcase */
import { Vue, Component, Prop } from 'vue-property-decorator';
import DoeRemoteService from '../../../../services/remote/DoeRemoteService';

const api = DoeRemoteService.instance;

@Component({
})
export default class TopicDetail extends Vue {

  @Prop() topic;

  fields = [];

  async mounted() {
    this.fields = await api.getTopicFields(this.topic);
  }
}
</script>
<style lang="scss" scoped>
</style>
