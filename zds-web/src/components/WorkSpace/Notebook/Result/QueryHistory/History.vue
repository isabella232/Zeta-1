<template>
  <div
    v-loading="loading"
    class="history-panel"
  >
    <div class="history">
      <div class="tool-bar">
        <div class="tool-bar-group-left">
          <span class="max-hint">
            {{ pageSize }} records per page
            <span
              class="reload"
              @click="reload()"
            >
              <i class="el-icon-refresh" />
            </span>
          </span>
        </div>
        <div class="tool-bar-group-right">
          <el-input
            v-model="searchVal"
            type="textarea"
            :autosize="{ minRows: 1, maxRows: 1}"
            resize="none"
            class="statement-search"
            placeholder="input statement"
          >
            <i
              slot="prefix"
              class="el-input__icon el-icon-search"
            />
          </el-input>
          <el-pagination
            layout="prev, pager, next"
            :current-page.sync="currentPage"
            :page-size="pageSize"
            :total="queries.length"
          />
        </div>
      </div>
      <el-table
        :fit="true"
        border
        class="history-table"
        row-class-name="table-row"
        :data="pageData"
        size="mini"
      >
        <el-table-column
          :width="200"
          label="Run Time"
        >
          <template slot-scope="scope">
            <span>{{ getTimeStr(scope.row.update_dt) }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :width="90"
          label="Job Id"
        >
          <template slot-scope="scope">
            <a
              :href="scope.row.livy_job_url"
              class="job-id"
              target="_blank"
            >{{ scope.row.request_id }}</a>
          </template>
        </el-table-column>
        <el-table-column label="Query">
          <span
            slot-scope="scope"
            class="statement"
            @click="show(scope.row, 'code')"
          >{{ getQueryStr(scope.row.statement) }}</span>
        </el-table-column>
        <el-table-column
          :width="110"
          label="Status"
        >
          <span
            slot-scope="scope"
            :class="getStatusClass(scope.row.status)"
          >{{ getStatusStr(scope.row.status) }}</span>
        </el-table-column>
        <!-- <el-table-column label="Source">
              </el-table-column>-->
        <el-table-column
          :width="90"
          label="Duration"
        >
          <span
            slot-scope="scope"
          >{{ getDurationStr(scope.row.start_dt, scope.row.update_dt) }}</span>
        </el-table-column>
        <el-table-column
          :width="120"
          :filters="filters"
          label="Proxy User"
          prop="proxy_user"
        />
        <!-- <el-table-column label="Returned rows">
                  <span slot-scope="scope">{{ scope.row.result ? scope.row.result.result[0].rows.length : "Unknown" }}</span>
              </el-table-column>-->
        <el-table-column
          align="center"
          :width="110"
          header-align="center"
          label="Result Details"
        >
          <template slot-scope="scope">
            <el-button
              slot="reference"
              type="text"
              @click="show(scope.row, 'result')"
            >
              <i class="zeta-icon-result" />
            </el-button>
          </template>
        </el-table-column>
        <el-table-column
          align="center"
          :width="110"
          header-align="center"
          label="Favorite"
        >
          <template slot-scope="scope">
            <FavoriteIcon
              :id="scope.row.id"
              type="statement"
            />
          </template>
        </el-table-column>
      </el-table>
    </div>
    <el-dialog
      :visible.sync="dShow"
      :title="dName"
      width="60%"
      :fullscreen="false"
      :modal="false"
    >
      <history-pop
        v-if="dQuery"
        :type="dType"
        :notebook="notebook"
        :query="dQuery"
        @close="dShow=false"
      />
    </el-dialog>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch, Inject } from 'vue-property-decorator';
import { INotebook } from '@/types/workspace';
import { RestPacket } from '@/types/RestPacket';
import { StatusCodeMap } from '@/types/workspace';
import HistoryPop from './HistoryPop.vue';
import FavoriteIcon from '@/components/common/favorite-icon/favorite-icon-witn-store.vue';
import moment from 'moment';
import _ from 'lodash';
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';

@Component({
  components: {
    HistoryPop,
    FavoriteIcon,
  },
})
export default class History extends Vue {
  @Inject() notebookRemoteService: NotebookRemoteService;
  @Prop() notebook: INotebook;
  loading: boolean;
  queries: RestPacket.Query[];

  /* Display in dialog
     * Use only one dialog to save memory
     */
  dQuery: RestPacket.Query | null;
  dShow: boolean;
  dType: string;

  currentPage = 1;
  pageSize = 50;
  searchVal = '';
  originQueries: RestPacket.Query[] = [];
  constructor () {
    super();
    this.loading = true;
    this.queries = [];

    this.dQuery = null;
    this.dShow = false;
    this.dType = 'code';
  }
  get pageData (){
    if (!this.queries) return;
    const start = (this.currentPage >=1 && this.currentPage > 0) ? (this.currentPage - 1) * this.pageSize : 0;
    const end = (this.currentPage >=1 && this.pageSize > 0) ? (this.currentPage - 1) * this.pageSize + this.pageSize : this.queries.length;
    const data = this.queries.slice(start, end);
    return data;
  }

  get filters () {
    return _.chain(this.queries)
      .map(q => q.proxy_user)
      .uniq()
      .map(u => {
        return { text: u, value: u};
      })
      .value();
  }
  reload () {
    /* SHOULD NOT place loading logic in construct
         * Otherwise loading view will fail to update
         */
    this.searchVal = '';
    this.loading = true;
    this.loadHistory().then(hiss => {
      const histories = hiss.map(his => {
        let proxy_user = '';
        try {
          const pref = JSON.parse(his.preference);
          proxy_user = pref['notebook.connection'].realUser;
        } catch {

        }
        his.proxy_user = proxy_user;
        return his;
      });
      this.queries = histories;
      this.originQueries = histories;
      this.loading = false;
    });
  }

  mounted () {
    this.reload();
  }

  async loadHistory () {
    return this.notebookRemoteService.getHistory(this.notebook.notebookId).then( (res: any) => {
      const histories = res.data;
      return _.sortBy(histories, e => -e.id);
    });

  }

  getTimeStr (time: number): string {
    return moment(time).format('YYYY/M/D h:mm:ss a');
  }

  getQueryStr (sql: string): string {
    /* TODO: responsive overflow */
    const maxLength = 50;
    if (sql.length >= maxLength) return sql.slice(0, maxLength) + '...';
    else return sql;
  }

  getDurationStr (startDt: number, updateDt: number): string {
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

  show (query: RestPacket.Query, type: string) {
    this.dShow = true;
    this.dQuery = query;
    this.dType = type;
  }

  get dName (): string {
    return 'Hello';
  }

  getStatusClass (code: string): string {
    switch (StatusCodeMap[code]) {
      case 'Failed':
        return 'status error';
      case 'Finished':
        return 'status success';
      default:
        return 'status';
    }
  }
  focus = false;
  searchFocus (){
    this.focus = true;
  }
  searchBlur (){
    this.focus = false;
  }
  getStatusStr (code: string): string {
    return StatusCodeMap[code];
  }
  formatStr (str: string) {
    // let resultStr = str.replace(/\ +/g, ""); //remove space
    const resultStr = str.replace(/[\r\n]/g, '');  //remove \r\n
    return resultStr.trim().toLowerCase();
  }
  formatSql (sql: string): string {
    /** filter annotation `/* *\/` */
    sql = sql.replace(/\/\*{1,2}[\s\S]*?\*\//g, '');
    // filter annotation `-- multi line` //
    sql = sql.replace(/^-{2}\s{1,}\[[\s|\S]+-{2}\s{1,}].*/gm, '');
    // sql = sql.replace(/--.*[^\\n]*/g, "");
    // filter annotation `--` //
    sql = sql.replace(/-{2}\s{1,}.*/g, '');
    // filter annotation `# multi line`
    sql = sql.replace(/^#{1,}\s*\[[\s|\S]+#{1,}\s*].*/gm, '');
    // sql = sql.replace(/#.*[^\\n]*/g, "");
    //filter annotation `#`
    sql = sql.replace(/#{1,}.*/g, '');
    // handle `;`
    sql = sql.replace(/[\s]+;/g, ';');
    sql = sql.replace(/;+/g, ';');
    // remove last `;`
    sql = sql.replace(/^;+|;+$/g, '');
    sql = sql.replace(/;[\s]+$/g, '');

    return sql;
  }
  @Watch('searchVal')
  onSearchChange (val: string,){
    if (_.trim(val)){
      const str = this.formatStr(this.formatSql(val));
      const d = _.debounce(() => {
        this.queries = this.originQueries.filter(item => this.formatStr(item.statement).indexOf(str)>-1);
      }, 500, { leading: true});
      d();
    } else {
      this.queries = this.originQueries;
    }
  }
}
</script>

<style lang="scss" scoped>
@import "@/styles/global.scss";
.history-panel {
    height: 100%;
    display: flex;
    flex-direction: column;
    overflow: auto;
    .history{
      min-width: 600px;
    }
}

.history-table {
    width: calc(100% - 40px) !important;
    height: calc(100% - 10px);
    margin: 0 20px 10px;
    overflow: auto !important;

    thead tr,
    thead th {
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
        height: 630px;
        margin: 0 auto auto;
        .el-dialog__header {
            display: none;
        }
        .el-dialog__body {
            height: calc(100% - 40px);
            padding-top: 30px;
        }
    }
    // .error {
    //     color: $zeta-global-color-red;
    // }
    // .success {
    //     color: $zeta-global-color-green;
    // }
}

.max-hint {
    color: #aaa;
    margin: 5px 0 0 20px;
}

.reload {
  color: $zeta-global-color;
  cursor: pointer;
}
.tool-bar{
    display: flex;
    justify-content: space-between;
    align-content: center;
    padding: 5px 0;
    .tool-bar-group-left{
        line-height: 30px;
    }
    .tool-bar-group-right{
        display: flex;
        .statement-search{
            width: 280px;
        }
    }
}
</style>
