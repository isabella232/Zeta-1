<template>
  <div
    v-loading="loading"
    class="history-pop-ctnr"
  >
    <i
      class="el-icon-close close"
      @click="() => {activate('result');$emit('close') }"
    />
    <ul>
      <li
        :class="{ active: activeType === 'code' }"
        @click="activate('code')"
      >
        SQL
      </li>
      <li
        :class="{ active: activeType === 'result' }"
        @click="activate('result')"
      >
        Result
      </li>
      <li
        v-if="dagDom"
        :class="{ active: activeType === 'dag' }"
        @click="activate('dag')"
      >
        Execution Plan
      </li>
    </ul>
    <div class="history-pop">
      <code-display
        v-if="activeType === 'code'"
        v-model="code"
      />
      <DagRender
        v-else-if="activeType === 'dag'"
        :dom-src="dagDom"
      />
      <template v-else>
        <div
          v-if="resultType === &quot;TEXT&quot;"
          class="error-text"
        >
          <template v-if="errorMsg">
            <div v-if="errorMsg.main_cause">
              <div class="error-title">
                Error:
              </div>
              <div class="error-info">
                {{ errorMsg.main_cause }}
              </div>
              <template>
                <div
                  class="error-title"
                  style="margin-top:10px;"
                >
                  Details:
                </div>
                <div class="error-details">
                  {{ errorMsg.details }}
                </div>
                <div
                  class="error-title"
                  style="margin-top:10px;"
                >
                  Original Error Message:
                </div>

                <div class="error-details">
                  {{ errorMsg.original_message }}
                </div>
              </template>
            </div>
            <div v-else>
              {{ errorMsg }}
            </div>
          </template>
        </div>
        <template v-else-if="resultType === &quot;TABLE&quot; && resultLoaded">
          <div class="result-info">
            Retrieved <strong>{{ Object.keys(rows).length }}</strong> rows in {{ getDurationStr(duration) }}.
          </div>
          <div class="result-tools">
            <div class="result-download">
              <el-button
                size="mini"
                @click="exportCSVFile"
              >
                <i class="icon zeta-icon-download" />
              </el-button>
            </div>
            <div class="result-pagination">
              <el-pagination
                layout="sizes, prev, pager, next"
                :current-page.sync="currentPage"
                :page-size="pageSize"
                :total="rows.length"
                :page-sizes="[100,500,1000,2000,3000,5000]"
                @size-change="handleSizeChange"
              />
            </div>
          </div>
          <div class="result-table-ctnr">
            <table-display
              :headers="headers"
              :rows="rows"
              :page="currentPage"
              :page-size="pageSize"
            />
          </div>
        </template>
        <template v-else-if="!errMsg">
          No result available for this statement.
        </template>
        <el-card v-else>
          <el-alert
            type="error"
            :closable="false"
            :title="errMsg"
          />
        </el-card>
      </template>
    </div>
  </div>
</template>

<script lang="ts">
import { Vue, Component, Prop, Watch, Inject } from 'vue-property-decorator';
import { INotebook, StatusCodeMap } from '@/types/workspace';
import { RestPacket } from '@/types/RestPacket';
import CodeDisplay from '@/components/common/Visualization/CodeDisplay.vue';
import TableDisplay from '@/components/common/Visualization/TableDisplay.vue';
import DagRender from '@/components/common/dag-render-v2/';
import moment from 'moment';
import _ from 'lodash';
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import { ZetaException } from '@/types/exception';

const CHAR_CARRIAGE_RETURN = String.fromCharCode(13);
const CHAR_DOUBLE_QUOTES = String.fromCharCode(34);
const CHAR_LINE_FEED = String.fromCharCode(10);
const columnDelimiter = ',';
const rowDelimiter = '\n';
@Component({
  components: {
    CodeDisplay, TableDisplay, DagRender,
  },
})

export default class HistoryPop extends Vue {
  @Inject() notebookRemoteService: NotebookRemoteService;
  @Prop() type: string;
  @Prop() notebook: INotebook;
  @Prop() query: RestPacket.Query;
  @Prop() viewOnly: boolean;
  activeType: string;
  loading: boolean;
  /** undefined for not fetch */
  /** null for nothing */
  result: Dict<string>|string|null | undefined;
  schema: Array<string>|null;
  resultType: string|null;
  errMsg: string|null;
  duration: number|null;
  dag: string|null;
  resultLoaded = false;
  pageSize = 100;
  currentPage = 1;
  constructor () {
    super();
    this.activeType = this.type;
    this.loading = false;
    this.result = null;
    this.schema = null;
    this.dag = null;
    this.resultType = null;
    this.errMsg = null;
    this.load();
  }

  get code (): string {
    return this.query.statement.toString();
  }

  // get resultLoaded(): boolean {
  //     return this.result != undefined;
  // }

  // get parsedResult(): Object {
  //     if ()
  // }

  get headers (): string[] {
    if (!this.resultLoaded) return [];
    return this.schema as string[];
  }

  get rows (): string[][] {
    if (!this.resultLoaded) return [];
    const dict = this.result as Dict<string>;
    return Object.keys(dict).map(key => {
      return _.values(dict[key]).map(v =>{
        return _.isObject(v) ? JSON.stringify(v) : v;
      });
    });
  }

  get elapsedTime (): string {
    return 'null';
  }
  get dagDom (){
    if (!this.resultLoaded) return null;
    return this.dag;
  }

  load () {
    this.pageSize = 100;
    const queryStatusCode = this.query.status;
    const queryStatus = StatusCodeMap[queryStatusCode];
    // if(queryStatus != 'Finished'){
    //     this.resultType = "";
    //     return
    // }
    if (!this.resultLoaded) {
      this.loading = true;
      const getQuery = (this.$appName == 'zeta'&&!this.viewOnly) ? this.notebookRemoteService.getQuery : this.notebookRemoteService.getShareStatement;
      getQuery.call(this.notebookRemoteService, this.query.id.toString()).then( e => {
        try {
          this.loading = false;
          let dag = JSON.parse(e.data.result).dag || null;
          if (/<div>Waiting.../.test(dag) || /<div>Watting/.test(dag)) {
            dag = null;
          }
          this.dag =  dag;
          const result = JSON.parse(e.data.result).result;
          if (!result) {
            this.resultLoaded = true;
            return;
          }
          const pkg = result[0];
          this.resultType = pkg.type;
          if (this.resultType === 'TEXT') {
            this.result = pkg.content;
          }
          else if (this.resultType === 'TABLE') {
            this.result = pkg.rows;
            this.schema = pkg.schema;
          }
          this.duration = e.data.updateDt - e.data.startDt;
          this.resultLoaded = true;
        }
        catch (e) {
          // eslint-disable-next-line no-console
          console.warn('Unexpected packet from server', e);
          this.result = null;
          this.loading = false;
        }
      }).catch((e: ZetaException) => {
        if (e.message === 'Incorrect result size: expected 1, actual 0'){
          e.resolve();
          this.errMsg = 'The data has been cleaned up.';
        }
        this.loading = false;
      });
      return false;
    }
  }
  get errorMsg () {
    const msgString =this.result && (this.result as string);
    if (!msgString) {
      return;
    }
    try {
      const str = msgString.replace('traceback: []', '');
      const obj = JSON.parse(str);
      obj.main_cause = obj.main_cause
        .replace(/\\n/g, '\n')
        .replace(/\\t/g, '\t');
      obj.details = obj.details
        .replace(/\\n/g, '\n')
        .replace(/\\t/g, '\t');
      obj.original_message = obj.original_message
        .replace(/\\n/g, '\n')
        .replace(/\\t/g, '\t');
      return obj;
    } catch (e) {
      // eslint-disable-next-line no-console
      console.error('Result => errorMsg', e);
      return msgString;
    }
  }
  @Watch('type')
  onTypeChange (type: string, oldType: string) {
    this.activeType = type;
    if (type === 'result' && !this.resultLoaded)
      this.load();
  }

  @Watch('activeType')
  onActiveTypeChange (type: string) {
    // eslint-disable-next-line no-console
    console.debug('activeType change', type);
    if (type === 'result' && !this.resultLoaded)
      this.load();
  }

  @Watch('query')
  onQueryChange () {
    this.result = null;
    this.resultType = null;
    this.errMsg = null;
    this.schema = null;
    this.resultLoaded = false;
    if (this.activeType === 'result' && !this.resultLoaded)
      this.load();
  }

  activate (type: string) {
    this.activeType = type;
  }
  handleSizeChange (size: number){
    this.pageSize = size;
  }
  getDurationStr (durationTime: number): string {
    const duration = moment.duration(durationTime);
    if (duration.asHours() >= 1){
      return `${duration.hours()}h ${duration.minutes()}m ${duration.seconds()}s`;
    }
    if (duration.asMinutes() >= 1){
      return `${duration.minutes()}m ${duration.seconds()}s`;
    }
    if (duration.asSeconds() >= 1){
      return `${duration.seconds()}s`;
    }
    return '0s';
  }

  /* CSV export */
  exportCSVFile () {
    let arr: (string | number | null)[][] = [];
    arr.push(this.headers);
    arr = arr.concat(this.rows);
    // eslint-disable-next-line no-console
    console.debug('arr pop', arr);
    let str = '';
    for (let i = 0; i < arr.length; i++) {
      for (let j = 0; j < arr[i].length; j++) {
        let ceilValue = arr[i][j];
        if (ceilValue === null) {
          ceilValue = '';
        } else if (
          typeof ceilValue === 'number' ||
                    typeof ceilValue === 'boolean'
        ) {
          ceilValue = ceilValue.toString();
        }
        // console.log('pop',ceilValue);
        str += this.parseCSVCell(ceilValue);
        if (j < arr[i].length - 1) {
          str += columnDelimiter;
        }
      }
      str += rowDelimiter;
    }

    const blob = new Blob(['\ufeff' + str], { type: 'text/csv' });

    const aLink = document.createElement('a');
    aLink.href = URL.createObjectURL(blob);
    document.body.appendChild(aLink);

    aLink.download = `result_${new Date().getTime()}.csv`;
    document.body.appendChild(aLink);
    aLink.click();
    document.body.removeChild(aLink);
  }
  parseCSVCell (value: string) {
    if (
      value !== '' &&
            (value.indexOf(CHAR_CARRIAGE_RETURN) > -1 ||
                value.indexOf(CHAR_DOUBLE_QUOTES) > -1 ||
                value.indexOf(CHAR_LINE_FEED) > -1 ||
                value.indexOf(columnDelimiter) > -1)
    ) {
      value = value.replace(new RegExp('"', 'g'), '""');
      value = '"' + value + '"';
    }
    return value;
  }
}
</script>

<style lang="scss" scoped>
@import '@/styles/global.scss';
.history-pop-ctnr {
	height: 100%;
}

.history-pop {
  height: calc(100% - 36px);
}

ul {
    display: flex;
    list-style-type: none;

    li {
        padding: 0px 17px;
        margin: 0 10px 10px 0;
        cursor: pointer;

        &.active {
            border-bottom: 2px solid rgb(51, 110, 123);
        }
    }
}
.result-tools{
    display: flex;
    justify-content: space-between;
    margin:10px 0;
}
.result-pagination{
    display: flex;
    justify-content: flex-end;
}
.result-table-ctnr {
	height: calc(100% - 34px - 42px);
	overflow: auto;
	/deep/ .handsontable{
		height: 100%;
	}
	/deep/ thead {
		color: #555;
		tr, th {
			background-color: #eee;
		}
	}
}
.result-info {
    // width: 100%;
    background-color: $zeta-label-lvl1;
    border-radius: 3px;
    margin: 5px 0px;
    padding: 0 10px;
}

.close {
    position: absolute;
    right: 32px;
    position: absolute;
    font-size: 18px;
    cursor: pointer;
}

pre.error-text {
    word-wrap: break-word;
    white-space: pre-wrap;
    line-height: 25px;
}

.error-content,
.error-title {
    font-family: inherit;
    font-size: 13px;
    white-space: pre-wrap;
    word-break: break-word;
}
.error-title {
    padding: 5px;
    color: #f56c6c;
    background-color: #fef0f0;
}
.error-info {
    font-size: 12px;
    white-space: pre-line;
    padding: 5px;
    // color: #999;
    font-family: SFMono-Regular,Consolas,Liberation Mono,Menlo,Courier,monospace;
}
.error-details {
    font-size: 12px;
    white-space: pre-line;
    color: #999;
    font-family: SFMono-Regular,Consolas,Liberation Mono,Menlo,Courier,monospace;
}
</style>
