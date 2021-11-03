<template>
  <div v-loading="loading" class="history-panel">
    <span
      v-if="queries.length>0"
      class="max-hint"
    >Maximum show 50 records <a class="reload" @click="reload()">Reload</a></span>
    <el-table
      v-if="queries.length>0"
      :fit="true"
      border
      class="history-table"
      row-class-name="table-row"
      :data="queries.slice(0, 50)"
      size="mini"
    >
      <el-table-column
        label="Run Time">
        <template slot-scope="scope">
          <span>{{ getTimeStr(scope.row.update_dt)}} </span>
        </template>
      </el-table-column>
      <el-table-column label="Job Id">
        <template slot-scope="scope">
          <a :href="scope.row.livy_job_url" class="job-id" target="_blank"> {{ scope.row.request_id }} </a>
        </template>
      </el-table-column>
      <el-table-column label="Query">
        <span slot-scope="scope" class="statement" @click='show(scope.row, "code")'>
          {{ getQueryStr(scope.row.statement) }}
        </span>
      </el-table-column>
      <el-table-column label="Status">
        <span slot-scope="scope" class="status">
          {{ getStatusStr(scope.row.status) }}
        </span>
      </el-table-column>
      <el-table-column label="Duration">
        <span slot-scope="scope">
          {{ getDurationStr(scope.row.start_dt, scope.row.update_dt) }}
        </span>
      </el-table-column>
      <el-table-column align="center" header-align="center" label="Result Details">
        <template slot-scope="scope">
          <el-button slot="reference" type="text" @click="show(scope.row, 'result')">
            <i class="zeta-icon-result" />
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <span v-else class="error_info">{{ error_info }}  </span>
    <el-dialog
      :visible.sync="dShow"
      :title="dName"
      width="55%"
      :fullscreen="false"
      :modal="false"
    >
      <history-pop
        v-if="dQuery"
        :view-only="true"
        :type="dType"
        :query="dQuery"
        @close="dShow=false"
      />
    </el-dialog>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Inject } from 'vue-property-decorator';
import { RestPacket } from '@/types/RestPacket';
import { StatusCodeMap } from '@/types/workspace';
import moment from 'moment';
import _ from 'lodash';
import HistoryPop from '@/components/WorkSpace/Notebook/Result/QueryHistory/HistoryPop.vue';
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';

@Component({
  components: {
    HistoryPop,
  },
})

export default class NotebookHistory extends Vue {
  @Inject()
  notebookRemoteService: NotebookRemoteService;

  @Prop()
  reqId: number;
  loading: boolean;
  queries: RestPacket.Query[];

  /* Display in dialog
     * Use only one dialog to save memory
     */
  dQuery: RestPacket.Query|null;
  dShow: boolean;
  dType: string;
  error_info = '';

  constructor () {
    super();
    this.loading = true;
    this.queries = [];

    this.dQuery = null;
    this.dShow = false;
    this.dType = 'code';
  }

  reload() {
    /* SHOULD NOT place loading logic in construct
         * Otherwise loading view will fail to update
         */
    this.loading = true;
    this.error_info = '';
    this.loadHistory().then(histories => {
      this.queries = histories;
      this.loading = false;
      if (this.queries.length===0){
        this.error_info = 'The data has been cleaned up.';
      }
    })
      .catch(e => {
        e.message = 'Fail to load history.';
      });
  }

  mounted() {
    this.reload();
  }
  async loadHistory() {
    return this.notebookRemoteService.getScheduleNotebookHistory(this.reqId.toString()).then( (res: any) => {
      const histories = res.data;
      return _.sortBy(histories, e => -e.id);
    });

  }

  getTimeStr(time: number): string {
    return moment(time).utcOffset(-7).format('YYYY/M/D h:mm:ss a');
  }

  getQueryStr(sql: string): string {
    /* TODO: responsive overflow */
    const maxLength = 50;
    if (sql.length >= maxLength)
      return sql.slice(0, maxLength) + '...';
    else
      return sql;
  }

  getDurationStr(startDt: number, updateDt: number): string {
    const duration = moment.duration(updateDt - startDt);
    if (duration.asHours() >= 1) {
      return `${duration.hours()}h ${duration.minutes()}m ${duration.seconds()}s`;
    }
    if (duration.asMinutes() >= 1) {
      return `${duration.minutes()}m ${duration.seconds()}s`;
    }
    if (duration.asSeconds() >= 1) {
      return `${duration.seconds()}s`;
    }
    return '0s';
  }

  show(query: RestPacket.Query, type: string) {
    this.dShow = true;
    this.dQuery = query;
    this.dType = type;
  }

  get dName(): string {
    return 'Hello';
  }

  getStatusStr(code: string): string {
    return StatusCodeMap[code];
  }
}
</script>

<style lang="scss" scoped>
@import "@/styles/global.scss";
.history-panel {
    height: 100%;
    display: flex;
    flex-direction: column;
}

.history-table {
    width: 100% !important;
    height: calc(100% - 20px);
    margin: 10px 0;
    overflow: auto !important;

    thead tr, thead th {
        background-color: #f2f2f2;
    }

    .statement {
        cursor: pointer;
    }

    .job-id {
        color: $zeta-global-color;
        text-decoration: underline;
    }
}
.history-panel {
	/deep/ .el-dialog {
		height: 80%;
		margin: 0 auto auto;
		.el-dialog__header {
			display: none;
		}
		.el-dialog__body {
			height: calc(100% - 60px);
		}
	}
}

.max-hint {
    color: #AAA;
    margin: 5px 0 0;
}
.error_info{
  margin: 10px 0 0;
}

a.reload {
    margin-left: 10px;
    cursor: pointer;
}
</style>
