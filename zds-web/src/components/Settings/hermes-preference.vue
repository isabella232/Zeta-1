<template>
  <div>
    <div class="cfg-list">
      <el-row>
        <el-col :span="6">
          Session idle timeout(hours)
        </el-col>
        <el-col :span="12">
          <el-input-number
            v-model="idleTimeout"
            type="number"
            :min="0"
            :max="4"
          />
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="6">
          Row Result
          <span>(Maximun 5000)</span>
        </el-col>
        <el-col :span="12">
          <el-input-number
            v-model="resultRow"
            type="number"
            :step="1000"
            :min="0"
            :max="5000"
          />
        </el-col>
      </el-row>
      <el-row>
        <el-col
          :span="24"
          class="confirm-col"
        >
          <el-button
            type="primary"
            :loading="loading"
            @click="save"
          >
            Update
          </el-button>
        </el-col>
      </el-row>
    </div>
  </div>
</template>
<script lang="ts">
import { Vue } from 'vue-property-decorator';
import Component from 'vue-class-component';
import { Getter, Action } from 'vuex-class';
import _ from 'lodash';
@Component({
  components: {},
})
export default class HermesPreference extends Vue {
  @Getter('userPreferenceByKey') userPreference: (key: string) => any;
  @Action updateUserPreference: (preference: Dict<any>) => Promise<any>;
  loading = false;
  idleTimeout = 1;
  resultRow = 1000;
  mounted () {
    const idleTimeout = (parseInt(this.userPreference('session_idle_timeout'))) / 3600;
    const resultRow = parseInt(this.userPreference('hermes_max_result'));
    if (idleTimeout) {
      this.idleTimeout = idleTimeout;
    }
    if (resultRow) {
      this.resultRow = resultRow;
    }
  }
  save () {
    this.loading = true;
    this.updateUserPreference({
      'session_idle_timeout': this.idleTimeout * 3600,
      'hermes_max_result': this.resultRow,
    }).then(() => {
      this.loading = false;
      this.$message.success('Update succeed');
    }).catch(() => {
      this.loading = false;
    });
  }
}
</script>
<style lang="scss" scoped>
  /deep/ .el-row {
    height: 40px;
    line-height: 40px;
    margin-top: 15px;
    margin-bottom: 15px;
    .confirm-col {
      text-align: right;
    }
    .el-input.error {
      /deep/ .el-input__inner {
        border-color: #e53917;
        color: #e53917;
      }
    }
    .el-input__inner {
      height: 40px;
    }
    span.error.error-alert {
      margin-left: 10px;
      color: #e53917;
    }
  }
  /deep/ .el-input__suffix{
      cursor: pointer;
  }
  .tip{
      font-size: 12px;
      color: #999999;
  }
</style>
