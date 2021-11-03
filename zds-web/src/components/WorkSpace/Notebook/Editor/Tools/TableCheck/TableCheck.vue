<template>
  <el-popover
    v-model="popoverVisible"
    v-click-outside="closePopover"
    placement="bottom-start"
    trigger="manual"
    :width="popoverWidth"
  >
    <div class="table-check notebook-tool">
      <div
        v-if="form.show"
        class="tool-bar"
        :class="{'form':form.show}"
      >
        <span v-show="form.show">
          <i
            class="el-icon-arrow-left"
            @click="() => form.show = false"
          />
          {{ form.table }}
        </span>
        <!-- <i @click="onCloseClick" class="el-icon-close"></i> -->
      </div>
      <!-- table status  -->
      <div
        v-if="!form.show"
        class="table-status"
      >
        <el-table
          v-if="tableData && tableData.length > 0"
          :data="tableData"
          :height="250"
          border
          style="width: 100%"
        >
          <el-table-column
            align="left"
            header-align="left"
            prop="tblName"
            label="Table"
          >
            <template slot-scope="scope">
              {{ getTableName(scope.row) }}
            </template>
          </el-table-column>
          <el-table-column
            align="center"
            header-align="center"
            prop="isMigrate"
            label="Migrated (Y/N)"
            :width="150"
          >
            <template slot-scope="scope">
              <span
                :class="{'notavailable':scope.row['isMigrate'] === null}"
              >{{ tableCellFormatter(scope.row,'isMigrate') }}</span>
            </template>
          </el-table-column>
          <el-table-column
            align="center"
            header-align="center"
            label="Existed (Y/N)"
            :width="125"
          >
            <template slot-scope="scope">
              <el-tooltip
                v-if="scope.row.isExist && scope.row.isExist.status === 1"
                class="item"
                effect="dark"
                :content="scope.row.isExist.hdfs"
                placement="top"
                :open-delay="200"
              >
                <span
                  style="cursor:pointer"
                >{{ tableCellFormatter(scope.row.isExist,'status') }}</span>
              </el-tooltip>
              <span
                v-else
                :class="{'notavailable':scope.row.isExist === null || (scope.row.isExist && scope.row.isExist.status)}"
              >{{ tableCellFormatter(scope.row.isExist,'status') }}</span>
            </template>
          </el-table-column>
          <el-table-column
            align="center"
            header-align="center"
            label="Match (Y/N)"
            :width="125"
          >
            <template slot-scope="scope">
              <el-tooltip
                v-if="scope.row.isMatch != undefined && (scope.row.isMatch.status === 1 || scope.row.isMatch.status === 0)"
                class="item"
                effect="dark"
                placement="top"
                :open-delay="200"
              >
                <div slot="content">
                  {{ `logic run date: ${scope.row.isMatch.logic_run_date}` }}
                  <br>
                  {{ `platforms: ${scope.row.isMatch.platforms}` }}
                </div>
                <span
                  style="cursor:pointer"
                >{{ tableCellFormatter(scope.row.isMatch,'status') }}</span>
              </el-tooltip>
              <span
                v-else
                :class="{'notavailable':scope.row.isMatch === null || (scope.row.isMatch && scope.row.isMatch.status)}"
              >{{ tableCellFormatter(scope.row.isMatch,'status') }}</span>
            </template>
          </el-table-column>
          <el-table-column
            align="center"
            header-align="center"
            prop="isDailyCopy"
            :width="150"
            label="Daily Copy (Y/N)"
          >
            <template slot-scope="scope">
              <span
                :class="{'notavailable':scope.row['isDailyCopy'] === null}"
              >{{ tableCellFormatter(scope.row,'isDailyCopy') }}</span>
            </template>
          </el-table-column>
          <el-table-column
            align="center"
            header-align="center"
            label="Action"
            :width="75"
          >
            <template slot-scope="scope">
              <el-tooltip
                v-if="scope.row['isDailyCopy'] === 0 && scope.row['jira']"
                content="Check JIRA ticket"
                effect="light"
              >
                <el-button type="text">
                  <i
                    type="text"
                    class="el-icon-check"
                    @click="()=> openJIRALink(scope.row['jira'])"
                  />
                </el-button>
                <!-- Check JIRA ticket -->
              </el-tooltip>
              <el-tooltip
                v-if="scope.row['isDailyCopy'] === 0 && !scope.row['jira']"
                content="Request Daily Copy"
                effect="light"
              >
                <el-button type="text">
                  <i
                    type="text"
                    class="el-icon-edit-outline"
                    @click="()=> dailyCopy(scope.row)"
                  />
                </el-button>
              </el-tooltip>
              <!-- <el-button
                                type="text"
                                v-if="scope.row['isDailyCopy'] === 0 && !scope.row['jira']"
                                @click="()=> dailyCopy(scope.row)"
                            >Request Daily Copy</el-button>-->
            </template>
          </el-table-column>
        </el-table>
        <div
          v-else
          class="no-data"
        >
          <h3>No Data Found</h3>
        </div>
      </div>
      <!-- daily copy form -->
      <div
        v-else
        v-loading="form.loading"
        class="request-info"
      >
        <div class="form-desc">
          Please provide general information to get start:
        </div>

        <el-form
          ref="copyForm"
          :model="form.model"
          :rules="form.modelRules"
          status-icon
        >
          <div class="items-left">
            <el-form-item
              label="Requester"
              prop="requester"
            >
              <el-input
                v-model="form.model.requester"
                type="text"
                auto-complete="off"
              />
            </el-form-item>
            <el-form-item
              label="DB Name"
              prop="dbName"
            >
              <el-input
                v-model="form.model.dbName"
                type="text"
                auto-complete="off"
              />
            </el-form-item>
            <el-form-item
              label="Table Name"
              prop="tblName"
            >
              <el-input
                v-model="form.model.tblName"
                type="text"
                auto-complete="off"
              />
            </el-form-item>
            <el-form-item
              label="Subject Area"
              prop="sa"
            >
              <el-input
                v-model="form.model.sa"
                type="text"
                auto-complete="off"
              />
            </el-form-item>
            <el-form-item
              label="Batch ID"
              prop="batchId"
            >
              <el-input
                v-model="form.model.batchId"
                type="text"
                auto-complete="off"
              />
            </el-form-item>
            <el-form-item
              label="Table Size"
              prop="tableSize"
            >
              <el-input
                v-model="form.model.tableSize"
                type="text"
                auto-complete="off"
              />
            </el-form-item>
            <el-form-item
              label="Table Owner"
              prop="owner"
            >
              <el-input
                v-model="form.model.owner"
                type="text"
                auto-complete="off"
              />
            </el-form-item>
          </div>
          <div class="items-right">
            <el-form-item
              label="Historical Data Range"
              prop="dataRange"
            >
              <el-input
                v-model="form.model.dataRange"
                type="text"
                auto-complete="off"
              />
            </el-form-item>
            <el-form-item
              label="Daily Logic"
              prop="logic"
            >
              <el-input
                v-model="form.model.logic"
                type="text"
                auto-complete="off"
                placeholder="Upsert / Fulltable / Insert Only / Delete & Insert"
              />
            </el-form-item>
            <el-form-item
              label="TD Touch File"
              prop="touchFile"
            >
              <el-input
                v-model="form.model.touchFile"
                type="text"
                auto-complete="off"
              />
            </el-form-item>
            <el-form-item
              label="Primary Key"
              prop="primaryKey"
            >
              <el-input
                v-model="form.model.primaryKey"
                type="text"
                auto-complete="off"
              />
            </el-form-item>
            <el-form-item
              label="PII"
              prop="PII"
            >
              <el-input
                v-model="form.model.PII"
                type="text"
                auto-complete="off"
              />
            </el-form-item>
            <el-form-item
              label="Audit Create Column"
              prop="auditCrtCol"
            >
              <el-input
                v-model="form.model.auditCrtCol"
                type="text"
                auto-complete="off"
                placeholder="e.g. CRE_DATE"
              />
            </el-form-item>
            <el-form-item
              label="Audit Update Column"
              prop="auditUdtCol"
            >
              <el-input
                v-model="form.model.auditUdtCol"
                type="text"
                auto-complete="off"
                placeholder="e.g. UDP_DATE"
              />
            </el-form-item>
            <el-form-item class="submit-btn">
              <el-button
                type="primary"
                @click="submit('copyForm')"
              >
                Submit
              </el-button>
            </el-form-item>
          </div>
        </el-form>
      </div>
    </div>
    <el-button
      slot="reference"
      type="text"
      class="notebook-tool-btn"
      :disabled="disable"
      @click="() => onInputChange()"
    >
      <i :class="statusIcon" />
      <span class="params-title">{{ title }}</span>
    </el-button>
  </el-popover>
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch, Inject } from 'vue-property-decorator';
import TableCheckRemoteService from '@/services/remote/SourceTableCheck';
import Util from '@/services/Util.service';
import config from '@/config/config';
import _ from 'lodash';
import vClickOutside from 'v-click-outside';

const formColsMap: Array<SourceTableCheck.FormCol> = [
  { key: 'requester', val: 'Requester' },
  { key: 'dbName', val: 'DB Name' },
  { key: 'tblName', val: 'Table Name' },
  { key: 'sa', val: 'Subject Area' },
  { key: 'batchId', val: 'Batch ID' },
  { key: 'tableSize', val: 'Table Size' },
  { key: 'owner', val: 'Table Owner' },
  { key: 'dataRange', val: 'Historical Data Range' },
  {
    key: 'logic',
    val: 'Daily Logic (Upsert / Fulltable / Insert Only / Delete & Insert)'
  },
  { key: 'touchFile', val: 'TD Touch File' },
  { key: 'primaryKey', val: 'Primary Key' },
  { key: 'PII', val: 'PII' },
  { key: 'auditCrtCol', val: 'Audit Create Column (e.g. CRE_DATE)' },
  { key: 'auditUdtCol', val: 'Audit Update Column (e.g. UPD_DATE)' }
];
class CopyModel implements SourceTableCheck.CopyModel {
  requester: string;
  dbName: string;
  tblName: string;
  sa: string;
  batchId: string;
  tableSize: string;
  owner: string;
  dataRange: string;
  logic: string;
  touchFile: string;
  primaryKey: string;
  PII: string;
  auditCrtCol: string;
  auditUdtCol: string;
  constructor (nt: string, dbName: string, tblName: string) {
    this.requester = nt;
    this.dbName = dbName;
    this.tblName = tblName;
  }
}
@Component({
  components: {},
  directives: {
    clickOutside: vClickOutside.directive
  }
})
export default class TableCheck extends Vue
  implements
        SourceTableCheck.TableCheckStatus,
        SourceTableCheck.TableCheckComponent {
  @Inject('tableCheckRemoteService')
  tableCheckRemoteService: TableCheckRemoteService;

  @Prop() sql: string;
  @Prop({ default: false, type: Boolean }) disable: boolean;
  @Prop() title: string;
  onInputChange () {
    if (this.popoverVisible) this.popoverVisible = false;
    else {
      if (! this.sql) {
        return;
      }
      this.loading = true;
      this.form.show = false;
      this.getTables(this.sql)
        .then(tables => {
          return this.tableCheckRemoteService.getSourceTableCheck(tables);
        })
        .then(({ data }) => {
          this.tableData = data;
          this.loading = false;
        })
        .catch(err => {
          this.tableData = [];
          this.loading = false;
        })
        .then(() => {
          this.popoverWidth =
                        this.tableData && this.tableData.length > 0 ? 950 : 222;
          this.popoverVisible = true;
        });
    }
  }
  closePopover () {
    this.popoverVisible = false;
  }
  loading = false;
  tableData: SourceTableCheck.TableInfo[] = [];
  popoverVisible = false;
  popoverWidth = 200;

  form = {
    show: false,
    loading: false,
    table: '',
    model: null as SourceTableCheck.CopyModel | null,
    modelRules: {
      requester: [
        {
          required: true,
          message: 'Please input requester',
          trigger: 'blur'
        }
      ],
      dbName: [
        {
          required: true,
          message: 'Please input DB name',
          trigger: 'blur'
        }
      ],
      tblName: [
        {
          required: true,
          message: 'Please input Table name',
          trigger: 'blur'
        }
      ]
    },
    close () {
      this.show = false;
      this.table = '';
      this.model = null;
    },
    open ({ dbName, tblName }: any) {
      this.show = true;
      this.table = tblName;
      this.model = new CopyModel(Util.getNt(), dbName, tblName);
    },
    generateTemplate () {
      let template = '';
      const colMap: Array<SourceTableCheck.FormCol> = formColsMap;
      _.forEach(this.model, (value, key) => {
        const formCol: SourceTableCheck.FormCol = colMap.find(
          col => col.key == key
        ) || { key: '', val: '' };

        template += `${formCol.val}:${value} \n`;
      });
      return template;
    }
  };
  directives = {
    'click-outside': {
      bind (el: HTMLElement, binding: Record<string, any>) {
      },
      unbind () {},
      stopProp () {}
    }
  };
  get statusIcon (): string {
    return this.loading ? 'el-icon-loading' : 'zeta-icon-tableCheck';
  }
  /** API */
  public open (sql: string) {
    this.sql = sql;
    this.loading = true;
    return this.getTables(this.sql)
      .then(tables => {
        return this.tableCheckRemoteService.getSourceTableCheck(tables);
      })
      .then(({ data }) => {
        this.tableData = data;
        this.loading = false;
        return this.tableData;
      });
  }
  public reset () {
    this.sql = '';
    this.tableData = [];
    this.form.show = false;
    this.loading = false;
  }
  protected submit (refName: string) {
    const form = this.$refs[refName] as any;
    form.validate((valid: boolean) => {
      if (valid && this.form.model != null) {
        const tmplt = this.form.generateTemplate();
        this.form.loading = true;
        this.tableCheckRemoteService.submitRequest(
          this.form.model.dbName,
          this.form.model.tblName,
          tmplt
        )
          .then(() => {
            this.form.loading = false;
            this.form.show = false;
            this.loading = true;
            return this.tableCheckRemoteService.getSourceTableCheck(
              this.tableData
            );
          })
          .then(({ data }: { data: any }) => {
            this.tableData = data;
            this.loading = false;
          });
      }
    });
  }

  onCloseClick () {
    this.$emit('update:show', false);
  }

  formatString = (str: string) => {
    const reg = /[^`]+(?=`)/g;
    const result = str.match(reg);
    return result && result[0] ? result[0] : '';
  };
  // table func
  private getTables (sql: string) {
    return this.tableCheckRemoteService.getTables(sql).then(res => {
      const tables: string[] = res.data;
      const outputTables: SourceTableCheck.TableInfoBase[] = [];
      tables.map(table => {
        const tableObj = Util.separateTableDB(table) || {
          db: '',
          table: ''
        };
        outputTables.push({
          dbName:
                        this.formatString(tableObj.db.trim()) || tableObj.db,
          tblName:
                        this.formatString(tableObj.table.trim()) ||
                        tableObj.table
        });
      });
      return outputTables;
    });
  }
  // table click event
  dailyCopy (row: any) {
    this.form.open({ tblName: row.tblName, dbName: row.dbName });
  }
  openJIRALink (jiraName: string) {
    const JIRALink = `https://jirap.corp.ebay.com/browse/${jiraName}`;
    window.open(JIRALink);
  }
  // table render func
  tableCellFormatter (row: any, col: any) {
    if (!row) {
      return 'N/A';
    }
    const value = row[col];
    if (value === 0) {
      return 'N';
    } else if (value === 1) {
      return 'Y';
    } else if (value === null) {
      return 'N/A';
    } else {
      return '-';
    }
  }
  getTableName (row: any) {
    const hasDB = Boolean(row.dbName);
    return hasDB ? row.dbName + '.' + row.tblName : row.tblName;
  }
  getADPOStatus (row: any) {
    if (row.isMigrate === 1) {
      return 'succeed';
    }
    if (row.isExist === 1 && row.isMatch === 1) {
      return 'succeed';
    }
    if (row.isExist === 1 && row.isMatch === 0) {
      return 'failed';
    }
    return 'progress';
  }

  // renderHeader(h: any, { column, $index }: any) {
  //     let label = column.label.split("<br>");
  //     return h("div", { class: { "table-header": true } }, [
  //         h("span", label[0]),
  //         h("br"),
  //         h("span", label[1])
  //     ]);
  // }
}
</script>

<style lang="scss" scoped>
@import "@/styles/global.scss";
$succeed: #5cb85c;
$progress: #f3af2b;
$failed: #e53917;
.notebook-tool {
    padding: 12px;
}
.table-check {
    // width: 800px;
    // min-height: 250px;
    // padding: 15px 20px;
    overflow-y: auto;
    .table-status {
        .no-data {
            text-align: center;
            font-size: 12px;
            margin: 10px;
            width: 200px;
        }
    }
    .tool-bar {
        width: 100%;
        display: inline-flex;
        justify-content: flex-end;
        margin: 5px 0;
        &.form {
            justify-content: flex-start;
        }
        i.el-icon-close,
        i.el-icon-arrow-left {
            margin: 5px;
            font-size: 18px;
            color: #999;
            cursor: pointer;
        }
    }
    .request-info {
        .form-desc {
            color: #666;
            margin-top: 5px;
            margin-bottom: 15px;
        }
        .submit-btn {
            text-align: right;
        }
    }
    /deep/ .el-table {
        th {
            // background-color: $zeta-label-lvl1;
            // padding: 6px 0;
            div.table-header {
                line-height: initial;
                white-space: normal;
                word-break: break-word;
            }
        }
        // tr {
            // td {
                // padding: 3px 0;
            // }
        // }
    }
}
.el-form {
    display: flex;
    height: 400px;
    /deep/ .items-left {
        width: 35%;
        .el-form-item__label {
            width: 100px;
        }
        .el-form-item__content {
            margin-left: 100px;
        }
    }
    /deep/ .items-right {
        width: 65%;
        .el-form-item__label {
            width: 180px;
        }
        .el-form-item__content {
            margin-left: 180px;
        }
    }
    // /deep/ .el-form-item{
    //     &.left{
    //       width:35%
    //     }
    //     &.right{
    //       width:65%
    //     }
    //   }
}
span.cycle {
    display: inline-block;
    height: 10px;
    width: 10px;
    border-radius: 5px;
    margin-right: 5px;
    &.progress {
        background-color: $progress;
    }
    &.succeed {
        background-color: $succeed;
    }
    &.failed {
        background-color: $failed;
    }
}
.notebook-tool-btn {
    position: relative;
    .zeta-icon-tableCheck,
    .el-icon-loading{
        transition: .3s;
        font-size: 20px;
    }
    .params-title{
        font-size: 12px;
        line-height: 20px;
        transition: .3s;
        position: absolute;
        display: none;
    }
    &:hover{
        .zeta-icon-tableCheck,
        .el-icon-loading{
            margin-right: 30px;
        }
        .params-title{
            transform: translateX(-30px);
            display: inline-block;
        }
    }
}
</style>
