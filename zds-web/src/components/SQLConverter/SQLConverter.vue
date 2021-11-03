<template>
  <div
    id="editorContainer"
  >
    <div class="left-editor">
      <div class="label">
        Source
        <!--<el-select
          v-model="srcPlatform"
          placeholder="platform"
          @change="handleSourceChangeEvent"
        >
          <el-option
            v-for="item in srcPlatformOptions"
            :key="item.value"
            :label="item.key"
            :value="item.value"
          />
        </el-select>
      </div>
      <div class="table-info-input">
        <el-input
          ref="searchInput"
          v-model="searchContent"
          placeholder="DB Name.Table Name"
          :disabled="inputLoading"
          @change="handleSearchEvent"
        >
          <el-button
            v-if="!inputLoading"
            slot="append"
            icon="el-icon-search"
            @click="handleSearchEvent"
          />
          <el-button
            v-else
            slot="append"
            icon="el-icon-loading"
          />
        </el-input> -->
      </div>
      <!-- file mode -->
      <input-script-list
        v-show="convertMode == ConvertMode.file || convertMode == ConvertMode.ddl"
        :input-loading="inputLoading"
        :script-list="inputScriptList"
        :table-info="tableInfo"
      />
      <!-- sql mode -->
      <div
        v-show="convertMode == ConvertMode.sql"
        class="editor-container"
      >
        <CodeDisplay
          v-model="tdSQL"
          :options="editorOptions"
          :read-only="false"
        />
      </div>
    </div>
    <div class="mid-editor">
      <el-button
        type="primary"
        class="cvt-btn"
        :disabled="!convertBtnActive"
        @click="handleConvertEvent"
      >
        convert
      </el-button>
      <!-- <el-button
        type="default"
        class="rvt-btn"
        plain
        @click="handleRevertEvent"
      >
        revert
      </el-button> -->
    </div>
    <div class="right-editor">
      <div class="label">
        Target: Spark
      </div>
      <!-- file mode -->
      <output-file-list
        v-show="convertMode == ConvertMode.file"
        :output-loading="outputLoading"
        :output-file="outputFileList"
        :src-table-name="tableInfo.name"
        @outputModeChange="outputModeChange"
      />
      <!-- sql mode -->
      <output-sql
        v-show="convertMode == ConvertMode.sql"
        ref="outputSql"
        :spark-sql="sparkSQL"
        :convert-s-q-l-loading="convertSQLLoading"
      />
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Provide } from 'vue-property-decorator';
import InputScriptList from '@/components/SQLConverter/InputScriptList.vue';
import OutputFileList from '@/components/SQLConverter/OutputFileList.vue';
import OutputSql from '@/components/SQLConverter/OutputSql.vue';
import Util from '@/services/Util.service';
import CodeDisplay from '@/components/common/Visualization/CodeDisplay.vue';
import 'codemirror/mode/sql/sql';
// require styles
import 'codemirror/lib/codemirror.css';
import SqlConverterRemoteService from '@/services/remote/SQLConverter';
import config from '@/config/config';
import _ from 'lodash';
/** */
enum ConvertMode {
  file,
  sql
}
@Component({
  components: {
    CodeDisplay,
    InputScriptList,
    OutputSql,
    OutputFileList,
  },
})
export default class SQLConverter extends Vue {
  @Provide('sqlConverterRemoteService')
  sqlConverterRemoteService: SqlConverterRemoteService = new SqlConverterRemoteService();
  readonly editorOptions = {
    // codemirror options
    tabSize: 4,
    mode: 'text/x-mysql',
    lineNumbers: true,
    smartIndent: false,
    matchBrackets: true,
    autofocus: true,
    indentWithTabs: true,
  };
  readonly srcPlatformOptions = [
    // { key: 'vivaldi', value: 'vivaldi' },
    { key: 'mozart', value: 'MOZART' },
    // { key: 'hopper', value: 'HOPPER' }
  ];

  private ConvertMode = ConvertMode;
  private srcPlatform = 'MOZART';
  private searchContent = '';
  private tdSQL = '';
  private convertMode = ConvertMode.sql;
  private outputMode = 'scripts';
  private sourceTableInfo: any = [];
  /**
   * computed
   */
  get convertBtnActive () {
    let active = false;
    if (this.convertMode == ConvertMode.sql) {
      active = Boolean(this.tdSQL != '');
    } else {
      active = Boolean(this.inputScriptList && this.inputScriptList.length > 0);
    }
    return active;
  }
  get srcDB () {
    const tableObj = Util.separateTableDB(this.searchContent.trim());
    return tableObj && tableObj.db ? tableObj.db : '';
  }
  get srcTable () {
    const tableObj = Util.separateTableDB(this.searchContent.trim());
    return tableObj && tableObj.table ? tableObj.table : '';
  }
  /**  input variables */
  private inputLoading = false;
  private inputScriptList: any[] = [];
  private tableInfo: any = {};

  /** output files variables */
  private outputLoading = false;
  private outputFileList: Dict<any> = {};

  /** output scripts variables */
  private convertSQLLoading = false;
  private sparkSQL = '';
  mounted () {
    this.sqlConverterRemoteService.props({
      path: 'sqlGuide',
    });
  }
  handleSourceChangeEvent () {
    if (this.srcDB && this.srcTable) {
      this.handleSearchEvent();
    }
  }
  handleSearchEvent () {
    const searchInput: any = this.$refs.searchInput;
    if (!this.srcDB || this.srcDB == '') {
      this.$message({
        showClose: true,
        message: 'No DB Name',
        type: 'error',
      });
      searchInput.focus();
      return;
    }
    if (!this.srcTable || this.srcTable == '') {
      this.$message({
        showClose: true,
        message: 'No Table Name',
        type: 'error',
      });
      searchInput.focus();
      return;
    }
    this.convertMode = ConvertMode.file;
    //rever output
    this.tableInfo = {};
    this.sourceTableInfo = [];
    this.inputScriptList = [];
    this.outputFileList = {};
    this.getSQLList(this.srcPlatform, this.srcDB, this.srcTable).then(
      ({ status, msg }: any) => {
        if (status != 0) {
          this.$message({
            showClose: true,
            message: `"get SQL files failed" ${msg}`,
            type: 'error',
          });
        }
      }
    );
  }
  handleConvertEvent () {
    if (this.convertMode == ConvertMode.file) {
      this.convertMode = ConvertMode.file;
      if (this.inputScriptList && this.inputScriptList.length > 0) {
        const scriptNameList: any = [];
        this.inputScriptList.map((script: any) =>
          scriptNameList.push(script.sqlFileName)
        );
        this.convertFiles({
          sqlScripts: scriptNameList,
          tblInfos: Array.from(new Set(this.sourceTableInfo)),
          platform: this.srcPlatform,
          dbName: this.srcDB,
          tblName: this.srcTable,
        }).then(({ status, msg }: any) => {
          if (status != 0) {
            this.$message({
              showClose: true,
              message: `"convert Files failed" ${msg}`,
              type: 'error',
            });
          } else {
            this.outputMode = 'scripts';
          }
        });
      } else {
        this.$message({
          showClose: true,
          message: 'No file selected',
          type: 'warning',
        });
      }
    } else if (this.convertMode == ConvertMode.sql) {
      if (this.tdSQL) {
        this.convertSQL(this.tdSQL).then(({ status, sql, msg }: any) => {
          if (status != 0) {
            this.$message({
              showClose: true,
              message: `Convert failed : ${msg}`,
              type: 'error',
            });
          }
        });
      } else {
        this.$message({
          showClose: true,
          message: 'No SQL script found',
          type: 'warning',
        });
      }
    }
  }
  handleRevertEvent () {
    this.searchContent = '';
    this.convertMode = ConvertMode.sql;
    this.tdSQL = '';
    this.sparkSQL = '';

    this.tableInfo = {};
    this.sourceTableInfo = [];
    this.inputScriptList = [];
    this.outputFileList = {};
  }
  outputModeChange (mode: string) {
    this.outputMode = mode;
  }

  getSQLList (platform: string, dbName: string, tblName: string) {
    this.inputLoading = true;
    return this.sqlConverterRemoteService.findFDTWithSQLScripts(platform, dbName, tblName)
      .then(res => {
        if (res && res.data && res.data != null) {
          const sqlScripts = (res && res.data && res.data.sqlScripts) || [];
          const tableInfo: any =
            res && res.data
              ? {
                SA: res.data.saName || '',
                owner: res.data.ownerNt || '',
                status: res.data.controllerConvertStatus,
              }
              : {};
          tableInfo.name = `${dbName}.${tblName}`;
          if (res.data.uc4Jobs && res.data.uc4Jobs.length > 0) {
            tableInfo.topJob = [];
            _.forEach(
              res.data.uc4Jobs,
              ({ uc4TopContainerName }: any, key: any) => {
                tableInfo.topJob.push(uc4TopContainerName);
              }
            ); //[0].uc4TopContainerName
            tableInfo.topJob = _.uniq(tableInfo.topJob);
          }

          // commit(TYPE.CVT_SET_SRC_TABLE_LIST, { sqlScripts, upstreamTbls:[], tableInfo });
          return { sqlScripts, tableInfo };
        } else {
          throw 'no sql file found';
        }
      })
      .then(({ sqlScripts, tableInfo }: any) => {
        const url = `${config.zeta.base}SQLConvert/getTdTableSource`;
        const sqlList: any[] = [];
        _.forEach(sqlScripts, ({ sqlFileName }) => {
          sqlList.push(sqlFileName);
        });
        return this.sqlConverterRemoteService.getTdTableSource(sqlList).then(res => {
          const sourceTable: any[] = [];
          _.forEach(res.data, (value: any[], key) => {
            const script = sqlScripts.find(
              ({ sqlFileName }: any) => sqlFileName == key
            );
            if (script) script.source = value || [];
            sourceTable.push(...(value || []));
          });
          this.inputLoading = false;
          // commit(TYPE.CVT_SET_SRC_TABLE_LIST, { sqlScripts, upstreamTbls:[], tableInfo,sourceTable});
          this.inputScriptList = _.sortBy(sqlScripts, ['sqlFileName']);
          this.tableInfo = tableInfo;
          this.sourceTableInfo = sourceTable;
          return { status: 0 };
        });
      })
      .catch(error => {
        // eslint-disable-next-line no-console
        console.error('getSQLList error', error);
        this.inputLoading = false;
        return { status: 1, msg: error };
      });
  }
  convertFiles (paylod: any) {
    this.outputLoading = true;
    return this.sqlConverterRemoteService.convert(paylod).then(
      response => {
        const files = response && response.data ? response.data : {};

        this.outputFileList = files;
        this.outputLoading = false;
        return { status: 0 };
      },
      error => {
        // eslint-disable-next-line no-console
        console.error('convertFiles error', error);
        this.outputLoading = false;
        return { status: 1, msg: error };
      }
    );
  }
  convertSQL (SQL: string) {
    this.convertSQLLoading = true;
    return this.sqlConverterRemoteService.manualconvertsql(SQL)
      .then((response: any) => {
        if (response.data.errorMessage) {
          throw response.data.errorMessage;
        } else {
          this.convertSQLLoading = false;
          const sparkSQL = response.data.convertSql
            ? response.data.convertSql
            : '';
          this.sparkSQL = sparkSQL;
          return { status: 0, sql: response.data.convertSql };
        }
      })
      .catch(error => {
        // eslint-disable-next-line no-console
        console.error('convertSQL error', error);
        this.convertSQLLoading = false;
        return { status: 1, msg: error };
      });
  }
}
</script>
<style lang="scss" scoped>
@import '@/styles/global.scss';
#editorContainer {
  display: flex;
  justify-content: space-between;
  flex-grow: 1;
  width: 100%;
  .mid-editor {
    display: flex;
    flex-direction: column;
    justify-content: center;
    padding: auto 30px;
    /deep/ .el-button {
      & + .el-button {
        margin-left: 0;
        margin-top: 10px;
      }
      // &.is-plain,
      // .is-plain:hover {
      // 	background-color: #fff;
      // 	color: rgb(51, 110, 123);
      // 	border: 1px solid rgb(51, 110, 123);
      // }
    }
  }
  .left-editor,
  .right-editor {
    display: flex;
    flex-direction: column;
    .input-script {
      height: 100%;
    }
    .label {
      background-color: $zeta-label-lvl1;
      font-size: 13px;
      font-weight: 700;
      height: 43px;
      line-height: 43px;
      padding-left: 10px;
    }
    .table-info {
      display: flex;
      // min-height: 40px;
      /deep/ .el-input {
        width: auto;
        flex-grow: 1;
        &:nth-child(1) {
          .el-input-group__append {
            border-bottom-right-radius: 0;
            border-top-right-radius: 0;
          }
        }
        &:nth-child(2) {
          margin-left: -1px;
          .el-input__inner {
            border-bottom-left-radius: 0;
            border-top-left-radius: 0;
          }
        }
      }
    }
    /deep/ .el-input__inner {
      &:focus {
        border: 1px solid #999;
      }
    }
    /deep/ .editor-container {
      border-right: 1px solid #dcdfe6;
    }
    /deep/ .editor-container,
    /deep/ .editor-container .code-display {
      height:100%;
      flex-grow: 1;
      display: flex;
      flex-direction: column;
      .CodeMirror {
        flex-direction: column;
        display: flex;
        flex-grow: 1;
        .CodeMirror-scroll {
          flex-grow: 1;
        }
      }
    }
    /deep/ .no-data {
      text-align: center;
      font-size: 24px;
      font-weight: 600;
      margin-top: 70px;
    }
    width: 40%;
    min-width: 300px;
  }
}
</style>
<style lang="scss">
// border-bottom-color: rgb(220, 223, 230);
</style>

