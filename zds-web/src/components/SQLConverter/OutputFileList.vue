<template>
  <div class="out-file">
    <div class="tool-bar">
      <el-button
        id="sv2Repo"
        type="primary"
        size="small"
        :disabled="isOutputFileEmpty"
        :loading="getSaveLoading"
        @click="saveToNotebook()"
      >
        Save to Repository
      </el-button>
    </div>
    <div
      v-loading="outputLoading"
      class="list-ctnr"
    >
      <el-tabs
        v-if="!isOutputFileEmpty"
        type="card"
      >
        <el-tab-pane
          v-for="(value, key) in outputFile"
          v-if="value.length > 0"
          :key="key"
          :label="key"
        >
          <div
            v-for="(item, $index) in value"
            :key="$index"
            class="list-item"
          >
            {{ item.fileName }}
            <span v-if="(outputFileStatus[key] && outputFileStatus[key][$index] && outputFileStatus[key][$index].status != null)|| saveLoading">
              <i
                v-if="saveLoading"
                class="el-icon-loading"
              />
              <i
                v-else-if="outputFileStatus[key][$index].status ==='Success'"
                class="el-icon-success status-success"
              />
              <!-- <i v-else-if="scripts.outputFileStatus[$index].status ==='Fail'" class="el-icon-error status-danger"></i> -->
              <el-tooltip
                v-else-if="outputFileStatus[key][$index].status ==='Fail'"
                :content="outputFileStatus[key][$index].errorMessage"
                placement="top"
                effect="light"
              >
                <i class="el-icon-error status-danger" />
              </el-tooltip>
            </span>
          </div>
        </el-tab-pane>
      </el-tabs>
      <div
        v-else
        class="no-data"
      >
        No Data
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Inject } from 'vue-property-decorator';
import { initRepository } from '@/services/workspace-init.service';
import Util from '@/services/Util.service';
import SqlConverterRemoteService from '@/services/remote/SQLConverter';
@Component({
  components: {},
})
export default class OutputFileList extends Vue {
  @Inject('sqlConverterRemoteService')
  sqlConverterRemoteService: SqlConverterRemoteService;
  @Prop({default:{}, type:Object}) outputFile: Dict<any[]> = {};
  @Prop({default:false, type:Boolean}) outputLoading = false;
  @Prop({default:'', type:String}) srcTableName = '';

  get isOutputFileEmpty () {
    for (const item in this.outputFile) {
      if (this.outputFile[item].length > 0) {
        return false;
      }
    }
    return true;
  }

  saveLoading = false;

  get getSaveLoading () {
    return this.saveLoading;
  }

  outputFileStatus: Record<string, any> = [];

  saveToNotebook () {
    this.saveLoading = true;
    this.sqlConverterRemoteService.saveToNotebook(
      Util.getNt(),
      this.srcTableName.toLowerCase(),
      this.outputFile
    ).then((res: any) => {
      // eslint-disable-next-line no-console
      console.debug('save to notebook', res);
      this.outputFileStatus = res.data;
      this.saveLoading = false;
      initRepository(this);
    });
  }
}
</script>
<style lang="scss" scoped>
.out-file {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.tool-bar {
  display: flex;
  justify-content: flex-end;
  padding: 5px;
  flex: 0 0 auto;
}

.list-ctnr {
  flex: 1 1 auto;
  height: calc(100% - 42px);
  /deep/ .el-tabs.el-tabs--card {
    height: 100%;
    .el-tabs__content {
      height: calc(100% - 56px);
      overflow: auto;
    }
  }
  .list-item {
    color: #606266;
    font-weight: 500;
    font-size: 14px;
    line-height: 20px;
  }
}

.status-success {
  color: #67c23a;
}
.status-danger {
  color: #f56c6c;
}
</style>
