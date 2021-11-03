<template>
  <div>
    <div class="cfg-list">
      <el-row>
        <el-col :span="6">
          Editor Font Size
        </el-col>
        <el-col :span="12">
          <el-slider
            v-model="fontSize"
            :min="10"
            :max="30"
            :marks="fontSizeMarks"
            :format-tooltip="formatTooltip"
          />
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="6">
          Auto Complete
        </el-col>
        <el-col :span="12">
          <el-switch v-model="autoComplete" />
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
export default class UserPreference extends Vue {
  @Getter('userPreferenceByKey') userPreference: (key: string) => any;
  @Action updateUserPreference: (preference: Dict<any>) => Promise<any>;
  loading = false;
  fontSize = 14;
  autoComplete = false;
  readonly fontSizeMarks = {
    10: '10px',
    14: '14px',
    20: '20px',
    30: '30px',
  };
  mounted () {
    const fontSize = this.userPreference('editor-font-size');
    const autoComplete = this.userPreference('auto-complete');
    if (fontSize) {
      this.fontSize = fontSize;
    }
    if (autoComplete || _.isUndefined(autoComplete)) {
      this.autoComplete = true;
    }
  }
  formatTooltip (v: any) {
    return v + 'ps';
  }
  save () {
    this.loading = true;
    this.updateUserPreference({
      'editor-font-size': this.fontSize,
      'auto-complete': this.autoComplete,
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
    span.error.error-alert {
      margin-left: 10px;
      color: #e53917;
    }
  }
  /deep/ .el-input__suffix{
      cursor: pointer;
  }
</style>
