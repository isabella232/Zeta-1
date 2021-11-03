<template>
  <div class="result">
    <ul class="query-tabs-nav-bar left">
      <li
        v-for="(query, index) in queries"
        :key="index"
        :title="'Query '+index"
        :class="{ active: index === activeIndex }"
      >
        <button
          :class="queryIcon(query)"
          @click="activeIndex = index"
        />
      </li>
    </ul>
    <div class="right">
      <template v-if="activeQuery">
        <feedback
          v-if="activeQuery.statementId"
          title="Feedback to this run"
          class="feedback"
          fd-type="query"
          :statement-id="activeQuery.statementId"
        />
        <QueryBrief
          v-if="!(activeQuery.status.status == 'SUCCESS' && activeQuery.headers)"
          :query="activeQuery.query"
        />
        <PlotDisplay
          v-if=" activeQuery.status.status == 'SUCCESS' && activeQuery.headers"
          class="result-table"
          :statement-id="activeQuery.statementId"
          :elapsed-time="elapsedTime"
          :headers="activeQuery.headers"
          :rows="activeQuery.rows"
          :updated-count="activeQuery.updatedCount"
          :query="activeQuery.query"
          :notebook="notebook"
        />

        <ContentDefaultDisplay
          v-else-if="activeQuery.status.status == 'SUCCESS' && activeQuery.content"
          class="info-overflow"
          :contents="activeQuery.content"
        />
        <el-card
          v-else-if="activeQuery.status.status == 'WAITING' || activeQuery.status.status == 'SUBMMITED' || activeQuery.status.status == 'RUNNING'"
          class="running-status"
        >
          <div
            style="height:100%;display:flex; flex-direction: column;"
            class="progress-card"
          >
            <div>
              <span>Progress:{{ activeQuery.status.status.valueOf() }}</span>
              <a
                v-if="notebook.monitorUrl"
                :href="notebook.monitorUrl"
                target="_blank"
                class="monitor-link"
              >Monitor Spark Job</a>
            </div>
            <el-progress
              class="progress-line"
              :text-inside="true"
              :stroke-width="20"
              :percentage="queryProgress(activeQuery)"
            />
            <div class="progress-info">
              <div
                v-if="subStatus"
                class="progress-info-item"
              >
                <span
                  v-if="parseJobIds(subStatus.activeJobIds)"
                  class="progress-id"
                >Subjob Id:{{ parseJobIds(subStatus.activeJobIds) }}</span>
                <br v-if="parseJobIds(subStatus.activeJobIds)">
                <span
                  class="progress-active-task"
                >Active Tasks: {{ subStatus.numActiveTasks }} / {{ subStatus.totalNumTasks }}</span>
                <span
                  class="progress-completed-task"
                >Completed Tasks: {{ subStatus.numCompletedCurrentStageTasks }} / {{ subStatus.totalNumTasks }}</span>
              </div>
            </div>
            <div
              v-if="subStatus.dag"
              style="flex-grow:1;height:100%"
              class="progress-dag"
            >
              <!-- <DAGContainer :dagSrc="subStatus.dag" /> -->
              <DAGPopup
                :value="subStatus.dag"
                :loading="dagLoading"
              />
            </div>
          </div>
        </el-card>
        <el-card v-else-if="activeQuery.status.status == 'CANCELED'">
          Query Canceled
        </el-card>
        <el-card
          v-else-if="activeQuery.status.status == 'ERROR'"
          class="info-overflow"
        >
          <template v-if="errorMsg">
            <!-- <el-alert
                  type='error'
                  :closable='false'
                  title="">
                  <pre class="error-content">Main Cause: </pre>
                        </el-alert>-->
            <div class="error-title">
              Error:
            </div>
            <div class="error-info">
              {{ errorMsg.main_cause }}
            </div>
            <span
              style="padding-left:5px;"
              @click="msgShow = !msgShow"
            >
              <el-button
                type="text"
                style="text-decoration: underline"
              >{{ msgShow ? 'hide details' : 'show details' }}</el-button>
            </span>
            <template v-if="msgShow">
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
          </template>
          <el-alert
            v-else
            type="error"
            :closable="false"
            title
          >
            <pre class="error-content">{{ queryErrorMessage(activeQuery) }}</pre>
          </el-alert>
        </el-card>
      </template>
    </div>
    <CodeDisplayDialog
      ref="codeDisplayDialog"
      :value="displayCode"
      :title="'SQL'"
    />
  </div>
</template>

<script lang="ts">
/**
 * Component <Result>. Versatile result displayer.
 * This component displays result of a job batch using <ResultTable>.
 * A job batch contains multiple queries.
 * <Result> creates a vertical nav-tabs at the left side for navigation between queries.
 * It also prints out error message and other excution information.
 *
 * @prop notebook:INotebook Current notebook
 * @prop result:IJob Result of a single job batch. May contains several queries.
 * @prop activeIndex:number Index of active query.
 */
import { Component, Vue, Prop, Ref, Provide, Watch } from 'vue-property-decorator';
import { INotebook } from '@/types/workspace';
// import TableDisplay from "@/components/common/Visualization/TableDisplay.vue";
import ContentDefaultDisplay from '@/components/common/Visualization/content-default-display.vue';
import PlotDisplay from './PlotDisplay.vue';
import QueryBrief from '@/components/common/Visualization/query-brief.vue';
import DAGPopup from '@/components/WorkSpace/Notebook/Result/QueryResult/dag-popup.vue';
import CodeDisplayDialog from '@/components/common/Visualization/code-display-dialog.vue';
import Feedback from '@/components/common/Feedback.vue';
import moment from 'moment';
import _ from 'lodash';
import { Query, NotebookJob, QueryInfo } from '@/types/workspace/notebook-job';
@Component({
  components: {
    PlotDisplay,
    ContentDefaultDisplay,
    DAGPopup,
    QueryBrief,
    CodeDisplayDialog,
    Feedback,
  },
})
export default class Result extends Vue {
  @Prop() notebook: INotebook;
  @Prop() result: NotebookJob;
  activeIndex?: number;
  showEditor = false;
  msgShow = false;

  @Ref('codeDisplayDialog')
  dialog: CodeDisplayDialog;
  displayCode = '';

  @Provide()
  openCodeDialog (code: string) {
    this.displayCode = code;
    this.dialog.open();
  }
  constructor () {
    super();
    if (this.queries) this.activeIndex = 0;
  }
  get queries (): Query[] {
    return this.result.queries;
  }

  get elapsedTime () {
    if (!this.activeQuery) return '0';
    return moment(this.activeQuery.endTime).diff(
      moment(this.activeQuery.startTime),
      'seconds'
    );
  }

  get activeQuery () {
    if (
      this.activeIndex != null &&
            this.activeIndex < this.queries.length
    ) {
      const query = this.queries[this.activeIndex];
      const rows = _.map(query.rows, row => {
        return _.map(row, col => {
          return _.isObject(col) ? JSON.stringify(col) : col;
        });
      });
      query.rows = rows;
      return query;
    } else {
      return null;
    }
  }
  get subStatus (): QueryInfo | null {
    if (this.activeQuery != null) {
      console.log('dag -> parent',this.activeQuery.status)
      return this.activeQuery.status;
    } else {
      return null;
    }
  }
  dagLoading = false;
  @Watch('subStatus', {deep:true})
  onStatusChange (subStatus: any){
    if (subStatus && /Waiting/.test(subStatus.dag || '')) {
      this.dagLoading = true;
    } else if (subStatus && /TimeoutException/.test(subStatus.dag || '')) {
      this.dagLoading = true;
    } else {
      this.dagLoading = false;
    }
  }

  queryIcon (query: Query): string {
    switch (query.status.status.valueOf()) {
      case 'SUBMITTED':
      case 'RUNNING':
      case 'WAITING':
        return 'el-icon-loading';
      case 'SUCCESS':
        return 'el-icon-success';
      case 'CANCELED':
        return 'el-icon-remove';
      case 'ERROR':
        return 'el-icon-warning';
      default:
        return 'el-icon-question';
    }
  }

  queryProgress (query: Query): number {
    return Math.ceil(query.status.progress * 100) || 0;
  }

  queryErrorMessage (query: Query): string {
    let errorMessage =
            query.errorMessage || this.result.info || 'Unknown error';
    errorMessage = errorMessage.replace(/\\n/g, '\n').replace(/\\t/g, '\t');
    console.debug('queryErrorMessage', errorMessage);
    return errorMessage;
  }
  get errorMsg () {
    if (!this.activeQuery) {
      return;
    }
    const msgString =
            this.activeQuery.errorMessage || (this.result.info as string);
    if (!msgString) {
      return;
    }
    try {
      const obj = JSON.parse(
        JSON.parse(msgString.replace('traceback: []', ''))[0]
      );
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
      console.error('Result => errorMsg', e);
      return;
    }
  }
  parseJobIds (ids: null | number[]) {
    if (!ids) {
      return null;
    } else {
      return ids.join(',');
    }
  }
}
</script>

<style lang="scss" scoped>
@import "@/styles/global.scss";
.result {
    min-height: 0;
}
.query-tabs-nav-bar {
    height: 100%;
    overflow-y: auto;

    button {
        &.el-icon-success {
            color: $zeta-global-color;
        }
        &.el-icon-warning {
            color: orange;
        }
        &.el-icon-remove {
            color: gainsboro;
        }
    }
}

.right {
    // height: 100%;
    overflow: auto;
    position: relative;
    max-width: 100%;
    .export-csv-btn {
        margin-left: 20px;
        // color: #336e7b;
        cursor: pointer;
        text-decoration: underline;
    }

    .result-table {
        overflow: hidden;
        height: 100%;
        width: 100%;
    }
    .feedback{
      position: absolute;
      right: 10px;
      top: 10px;
    }
    .query-brief{
      margin-top: 30px;
    }
}

.monitor-link {
    margin-left: 3em;
    color: $zeta-global-color;
}

.error-content,
.error-title {
    font-family: inherit;
    font-size: 13px;
    white-space: pre-line;
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
.el-card{
    height: 100%;
}
.info-overflow {
    overflow: auto;
}
.progress-line {
    margin-top: 5px;
    margin-bottom: 5px;
}
.progress-info-item {
    // span.progress-active-task{
    //     margin
    // }
    span.progress-active-task,
    span.progress-completed-task {
        font-size: 12px;
        margin-left: 15px;
    }
}
.running-status {
    height: 100%;
    /deep/ .el-card__body {
        height: 100%;
    }
}
</style>

