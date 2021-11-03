<template>
  <div class="columns">
    <el-table
      :data="data"
      :default-sort="{prop: 'column_id', order: 'descending'}"
    >
      <el-table-column
        prop="column_name"
        label="Column Name"
      >
        <template slot-scope="scope">
          <div class="col-div">
            <div class="content-center">
              {{ scope.row.column_name ? scope.row.column_name.toUpperCase() : "" }}
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column
        prop="column_desc"
        label="Description"
        min-width="300"
      >
        <template slot-scope="scope">
          <div v-html="scope.row.column_desc" />
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
            <span v-html="scope.row.sampleData" />
          </div>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>
<script lang="ts">
import { Component, Vue, Prop } from 'vue-property-decorator';

@Component({
  components: {
  },
})
export default class Columns extends Vue {
  @Prop({ default: () => []}) data;

}
</script>
<style lang="scss" scoped>

.columns {
  padding: 12px;
  ::v-deep {
    thead {
      th {
        background-color: #F5F5F5;
        padding: 2px 0;
      }
    }
  }
}
</style>