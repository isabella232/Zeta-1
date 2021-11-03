<template>
  <div
    v-loading="loading"
    class="da-container"
  >
    <div
      v-if="!daSubmitVisible"
      style="height: 100%;"
    >
      <div class="metadata-tools-bar">
        <ul class="metadata-tabs">
          <li
            :class="{'active':activeTab ==='all'}"
            @click="()=> {activeTab = 'all';dateLabel='Date';currentPage = 1;clearFilterOption();}"
          >
            All
          </li>
          <li
            :class="{'active':activeTab ==='waiting'}"
            @click="()=> {activeTab = 'waiting';dateLabel='Submitted Date';currentPage = 1;clearFilterOption();}"
          >
            Pending
          </li>
          <li
            :class="{'active':activeTab ==='approved'}"
            @click="()=> {activeTab = 'approved';dateLabel='Aproved Date';currentPage = 1;clearFilterOption();}"
          >
            Approved
          </li>
        </ul>
        <div class="btn-group">
          <template>
            <el-button
              class="new-btn"
              type="primary"
              @click="jump({}, false, false)"
            >
              new
            </el-button>
          </template>
        </div>
      </div>
      <el-table
        class="data-table"
        :data="getData"
        border
        height="calc(100% - 30px - 32px - 40px)"
      >
        <el-table-column
          property="pltfrm_name"
          label="Platform"
        >
          <template
            slot="header"
            slot-scope="scope"
          >
            <FilterComponent
              ref="pltfrmFilter"
              :options-data="pltfrmFilter"
              type="platform"
              title="Platform"
            />
          </template>
          <template slot-scope="scope">
            <span>{{ formatPltfrm(scope.row.pltfrm_name) }}</span>
          </template>
        </el-table-column>
        <el-table-column
          property="db_name"
          label="DB Name"
        >
          <template
            slot="header"
            slot-scope="scope"
          >
            <FilterComponent
              ref="dbFilter"
              :options-data="dbFilter"
              type="db"
              title="DB Name"
            />
          </template>
          <template slot-scope="scope">
            <span>{{ scope.row.db_name.toUpperCase() }}</span>
          </template>
        </el-table-column>
        <el-table-column
          property="table_name"
          label="Table Name"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.table_name.toUpperCase() }}</span>
          </template>
        </el-table-column>
        <el-table-column
          property="jira_id"
          label="JIRA"
        >
          <template slot-scope="scope">
            <a
              :href="'https://jirap.corp.ebay.com/browse/' + scope.row.jira_id.toUpperCase()"
              target="_blank"
            >{{ scope.row.jira_id.toUpperCase() }}</a>
          </template>
        </el-table-column>
        <el-table-column
          property="sbjct_area"
          label="Subject Area"
        >
          <template
            slot="header"
            slot-scope="scope"
          >
            <FilterComponent
              ref="saFilter"
              :options-data="saFilter"
              title="Subject Area"
            />
          </template>
        </el-table-column>
        <el-table-column
          property="cre_user"
          label="Submitted by"
        >
          <template
            slot="header"
            slot-scope="scope"
          >
            <FilterComponent
              ref="creUserFilter"
              :options-data="creUserFilter"
              title="Submitted by"
            />
          </template>
        </el-table-column>
        <el-table-column
          property="prmry_da_nt"
          label="Asignee"
        >
          <template
            slot="header"
            slot-scope="scope"
          >
            <FilterComponent
              ref="asigneeFilter"
              :options-data="asigneeFilter"
              title="Asignee"
            />
          </template>
        </el-table-column>
        <el-table-column
          property="approver"
          label="Approver"
        >
          <template
            slot="header"
            slot-scope="scope"
          >
            <FilterComponent
              ref="approverFilter"
              :options-data="approverFilter"
              title="Approver"
            />
          </template>
        </el-table-column>
        <el-table-column
          property="cre_date"
          label="Date"
        >
          <template
            slot="header"
            slot-scope="scope"
          >
            <DatepickerFilterComponent
              ref="dateFilter"
              :title="dateLabel"
            />
          </template>
        </el-table-column>
        <el-table-column
          property
          label
          width="250"
        >
          <template
            slot="header"
            slot-scope="scope"
          >
            <el-input
              v-model="searchText"
              placeholder="Search"
            >
              <i
                slot="suffix"
                class="el-input__icon el-icon-search"
              />
            </el-input>
          </template>
          <template slot-scope="scope">
            <el-button
              v-if="!DAFlag"
              class="operate-btn clone-btn"
              type="primary"
              size="small"
              @click="jump(scope.row, true, false)"
            >
              Clone
            </el-button>
            <el-button
              class="operate-btn detail-btn"
              type="primary"
              size="small"
              @click="jump(scope.row, false, DAFlag)"
            >
              Detail
            </el-button>
            <el-button
              v-if="DAFlag || isDelete(scope.row.cre_user)"
              class="operate-btn delete-btn"
              type="danger"
              size="small"
              @click="onDelete(scope.row)"
            >
              Delete
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="footer">
        <el-pagination
          layout="prev, pager, next"
          :current-page.sync="currentPage"
          :page-size="pageSize"
          :total="total"
        />
      </div>
    </div>
    <DASubmit
      v-else
      :data="selectRow"
      :approve-flag="approveFlag"
      :clone-flg="cloneFlg"
      @back="back"
    />
  </div>
</template>

<script lang="ts">
import { Component, Vue, Inject } from 'vue-property-decorator';
import _ from 'lodash';
import DASubmit from '@/components/DA/DASubmit.vue';
import FilterComponent from '@/components/DA/filter-component.vue';
import DatepickerFilterComponent from '@/components/DA/datepicker-filter-component.vue';
import DoeRemoteService from '@/services/remote/DoeRemoteService';
import Util from '@/services/Util.service';
import moment from 'moment';

@Component({
  components: {
    DASubmit,
    FilterComponent,
    DatepickerFilterComponent
  }
})
export default class DAList extends Vue {
  @Inject('doeRemoteService') doeRemoteService: DoeRemoteService;

  loading = false;
  activeTab = 'all';
  tableData: Array<any> = [];
  selfTableData: Array<any> = [];
  searchText = '';
  daSubmitVisible = false;
  cloneFlg = false;
  selectRow: any = {};
  DAFlag = false;
  currentPage = 1;
  pageSize = 20;
  total = 1;
  approveFlag = false;
  dateLabel = 'Date';

  //filter
  pltfrmFilter: any = [];
  dbFilter: any = [];
  saFilter: any = [];
  creUserFilter: any = [];
  asigneeFilter: any = [];
  approverFilter: any = [];

  mounted() {
    this.checkDARole();
    this.getModelList();
  }

  checkDARole() {
    this.doeRemoteService
      .checkDARole(Util.getNt())
      .then(res => {
        if (res && res.data && res.data != null) {
          if (
            !_.isEmpty(res.data) &&
            !_.isUndefined(res.data.data) &&
            !_.isUndefined(res.data.data.value)
          ) {
            this.DAFlag = res.data.data.value.DAFlag === 'true';
            if (
              !_.isEmpty(this.$route.query.platform) &&
              !_.isEmpty(this.$route.query.db) &&
              !_.isEmpty(this.$route.query.table)
            ) {
              this.selectRow = {
                pltfrm_name: this.$route.query.platform,
                db_name: this.$route.query.db,
                table_name: this.$route.query.table
              };
              this.daSubmitVisible = true;
              this.approveFlag = this.DAFlag;
              this.cloneFlg = false;
            }
          }
        }
      });
  }

  get getData() {
    let arr: any = [];

    // tab
    if (this.activeTab == 'waiting') {
      if (this.DAFlag) {
        arr = _.filter(_.cloneDeep(this.tableData), (v: any) => {
          return v.status == 'new' || v.status == 'modified';
        });
      } else {
        arr = _.filter(_.cloneDeep(this.selfTableData), (v: any) => {
          return v.status == 'new' || v.status == 'modified';
        });
      }
    } else if (this.activeTab == 'approved') {
      if (this.DAFlag) {
        arr = _.filter(_.cloneDeep(this.tableData), (v: any) => {
          return v.status == 'approved';
        });
      } else {
        arr = _.filter(_.cloneDeep(this.selfTableData), (v: any) => {
          return v.status == 'approved';
        });
      }
    } else {
      arr = _.cloneDeep(this.tableData);
    }

    // filter
    const pltfrmFilter: any = this.$refs.pltfrmFilter;
    if (pltfrmFilter && !_.isEmpty(pltfrmFilter.ckValue)) {
      arr = _.filter(arr, (v: any) => {
        return !_.isEmpty(
          _.intersection(
            pltfrmFilter.ckValue,
            _.transform(
              _.toLower(v.pltfrm_name)
                .replace('numozart', 'mozart')
                .split(','),
              (rs: any, v: any) => {
                rs.push(_.upperFirst(v));
              },
              []
            )
          )
        );
      });
    }
    const dbFilter: any = this.$refs.dbFilter;
    if (dbFilter && !_.isEmpty(dbFilter.ckValue)) {
      arr = _.filter(arr, (v: any) => {
        return _.includes(dbFilter.ckValue, _.toUpper(v.db_name));
      });
    }
    const saFilter: any = this.$refs.saFilter;
    if (saFilter && !_.isEmpty(saFilter.ckValue)) {
      arr = _.filter(arr, (v: any) => {
        return _.includes(saFilter.ckValue, v.sbjct_area);
      });
    }
    const creUserFilter: any = this.$refs.creUserFilter;
    if (creUserFilter && !_.isEmpty(creUserFilter.ckValue)) {
      arr = _.filter(arr, (v: any) => {
        return _.includes(creUserFilter.ckValue, v.cre_user);
      });
    }
    const asigneeFilter: any = this.$refs.asigneeFilter;
    if (asigneeFilter && !_.isEmpty(asigneeFilter.ckValue)) {
      arr = _.filter(arr, (v: any) => {
        return _.includes(asigneeFilter.ckValue, v.prmry_da_nt);
      });
    }
    const approverFilter: any = this.$refs.approverFilter;
    if (
      approverFilter &&
      !_.isEmpty(approverFilter.ckValue) &&
      this.activeTab != 'waiting'
    ) {
      arr = _.filter(arr, (v: any) => {
        return _.includes(approverFilter.ckValue, v.approver);
      });
    }

    const dateFilter: any = this.$refs.dateFilter;
    if (dateFilter && !_.isEmpty(dateFilter.dateValue)) {
      arr = _.filter(arr, (v: any) => {
        return (
          moment(v.cre_date).format('YYYY-MM-DD') >=
            moment(dateFilter.dateValue[0]).format('YYYY-MM-DD') &&
          moment(v.cre_date).format('YYYY-MM-DD') <=
            moment(dateFilter.dateValue[1]).format('YYYY-MM-DD')
        );
      });
    }

    if (!_.isEmpty(this.searchText)) {
      arr = _.filter(arr, (v: any) => {
        return (
          v.pltfrm_name.toLowerCase().includes(this.searchText.toLowerCase()) ||
          v.db_name.toLowerCase().includes(this.searchText.toLowerCase()) ||
          v.table_name.toLowerCase().includes(this.searchText.toLowerCase()) ||
          v.jira_id.toLowerCase().includes(this.searchText.toLowerCase()) ||
          v.sbjct_area.toLowerCase().includes(this.searchText.toLowerCase())
        );
      });
    }

    this.total = _.size(arr);
    return _.slice(
      arr,
      (this.currentPage - 1) * this.pageSize,
      this.currentPage * this.pageSize - 1 > _.size(arr)
        ? _.size(arr)
        : this.currentPage * this.pageSize - 1
    );
  }

  getModelList() {
    this.loading = true;
    const params: any = {
      nt: Util.getNt()
    };
    this.tableData = [];
    this.selfTableData = [];
    this.initFilter();
    this.clearFilterOption();
    this.total = 0;
    this.doeRemoteService
      .getModelListByMultiPltfrm(params)
      .then(res => {
        console.debug('Call Api:getModelListByMultiPltfrm successed');
        if (res && res.data && res.data.data && res.data.data.value) {
          this.tableData = res.data.data.value.allModel;
          this.selfTableData = res.data.data.value.selfModel;
          this.total = _.size(this.tableData);
          this.pltfrmFilter = _.sortBy(
            _.uniq(
              _.split(
                _.toLower(
                  _.join(
                    _.map(res.data.data.value.allModel, 'pltfrm_name'),
                    ','
                  )
                ),
                ','
              )
            )
          );
          this.dbFilter = _.transform(
            _.sortBy(_.uniq(_.map(res.data.data.value.allModel, 'db_name'))),
            (rs: any, v: any) => {
              rs.push(_.toUpper(v));
            },
            []
          );
          this.saFilter = _.sortBy(
            _.uniq(_.map(res.data.data.value.allModel, 'sbjct_area'))
          );
          this.creUserFilter = _.sortBy(
            _.uniq(_.map(res.data.data.value.allModel, 'cre_user'))
          );
          this.asigneeFilter = _.sortBy(
            _.uniq(_.map(res.data.data.value.allModel, 'prmry_da_nt'))
          );
          this.approverFilter = _.sortBy(
            _.uniq(_.map(res.data.data.value.allModel, 'approver'))
          );
          _.remove(this.asigneeFilter, (v: any) => {
            return v == null;
          });
        }
        this.loading = false;
      })
      .catch(err => {
        console.error(
          'Call Api:getModelListByMultiPltfrm failed: ' + JSON.stringify(err)
        );
        this.loading = false;
      });
  }

  isDelete(submittedByNt: string) {
    return submittedByNt == Util.getNt();
  }

  search() {
    if (this.currentPage == 1) {
      this.getModelList();
    } else {
      this.currentPage = 1;
    }
  }

  jump(row: any, cloneFlg: boolean, approveFlag: boolean) {
    this.selectRow = row;
    this.daSubmitVisible = true;
    this.cloneFlg = cloneFlg;
    this.approveFlag = approveFlag;
  }

  back() {
    this.selectRow = {};
    this.daSubmitVisible = false;
    this.getModelList();
  }

  async onDelete(row: any, tableName?: string) {
    await this.$confirm(
      'Are you sure to delete table: ' + row.db_name + '.' + row.table_name,
      'Confirm Delete',
      {
        confirmButtonText: 'Delete',
        cancelButtonText: 'Cancel',
        customClass: 'del-file-message',
        type: 'warning',
        dangerouslyUseHTMLString: true
      }
    );

    this.loading = true;
    const params: any = {
      platform: row.pltfrm_name,
      db: row.db_name,
      table: tableName ? tableName : row.table_name
    };
    this.doeRemoteService
      .deleteModelByMultiPltfrm(params)
      .then(res => {
        console.debug('Call Api:deleteModelByMultiPltfrm successed');
        if (res && res.status == 200) {
          this.$message.success('Delete success');
          this.getModelList();
        }
      })
      .catch(err => {
        console.error(
          'Call Api:deleteModelByMultiPltfrm failed: ' + JSON.stringify(err)
        );
        this.loading = false;
      });
  }

  formatPltfrm(val: any) {
    return _.join(
      _.transform(
        _.split(_.toLower(val).replace('numozart', 'mozart'), ','),
        (rs: any, v: any) => {
          rs.push(_.upperFirst(v));
        },
        []
      ),
      ','
    );
  }

  initFilter() {
    this.pltfrmFilter = [];
    this.dbFilter = [];
    this.saFilter = [];
    this.creUserFilter = [];
    this.asigneeFilter = [];
    this.approverFilter = [];
  }

  clearFilterOption() {
    const pltfrmFilter: any = this.$refs.pltfrmFilter;
    if (pltfrmFilter && pltfrmFilter.ckValue) {
      pltfrmFilter.ckValue = [];
    }

    const dbFilter: any = this.$refs.dbFilter;
    if (dbFilter && dbFilter.ckValue) {
      dbFilter.ckValue = [];
    }

    const saFilter: any = this.$refs.saFilter;
    if (saFilter && saFilter.ckValue) {
      saFilter.ckValue = [];
    }

    const creUserFilter: any = this.$refs.creUserFilter;
    if (creUserFilter && creUserFilter.ckValue) {
      creUserFilter.ckValue = [];
    }

    const asigneeFilter: any = this.$refs.asigneeFilter;
    if (asigneeFilter && asigneeFilter.ckValue) {
      if (this.activeTab == 'waiting' && this.DAFlag) {
        asigneeFilter.ckValue = [Util.getNt()];
      } else {
        asigneeFilter.ckValue = [];
      }
    }

    const approverFilter: any = this.$refs.approverFilter;
    if (approverFilter && approverFilter.ckValue) {
      if (this.activeTab == 'approved' && this.DAFlag) {
        approverFilter.ckValue = [Util.getNt()];
      } else {
        approverFilter.ckValue = [];
      }
    }

    const dateFilter: any = this.$refs.dateFilter;
    if (dateFilter && dateFilter.dateValue) {
      dateFilter.dateValue = undefined;
    }
  }
}
</script>
<style lang="scss" scoped>
@import "@/styles/global.scss";
$bc-height: $workspace-tab-height + $workspace-tab-margin-bottom;
.da-container {
  height: 100%;
}
.el-form {
  /deep/ .el-textarea {
    width: 507px;
  }
  /deep/ .el-input {
    width: 507px;
  }
}
.el-table {
  /deep/ .cell {
    display: flex;
  }
  /deep/ .full-size-btn {
    width: 96% !important;
  }
  /deep/ .operate-btn {
    width: 32%;
    height: 30px;
    font-size: 14px;
    background: inherit;
    background-color: rgba(255, 255, 255, 0);
    border-width: 1px;
    border-style: solid;
    border-color: rgba(86, 156, 225, 1);
    border-radius: 4px;
    box-shadow: none;
    color: #569ce1;
  }
}
.new-btn {
  float: right;
  margin-bottom: 10px;
}
.el-icon-search {
  cursor: pointer;
  height: 30px;
  width: 50px;
}
.el-pagination {
  float: right;
}
.metadata-tools-bar {
  align-items: center;
  display: flex;
  height: 39px;
  justify-content: space-between;
  ul.metadata-tabs {
    display: flex;
    height: 100%;
    list-style-type: none;
    width: 100%;
    border-bottom: 1px solid #cacbcf;
    > li {
      align-items: center;
      color: #999;
      cursor: pointer;
      display: flex;
      font-size: 14px;
      padding: 0 20px;
      &.active {
        border-bottom: 1px solid #569ce1;
        background-color: #fff;
        color: $zeta-global-color;
      }
      &:hover {
        color: #569ce1;
      }
    }
  }
}
.btn-group {
  border-bottom: 1px solid #cacbcf;
  display: inline-block;
  height: 39px;
  padding-right: 10px;
  text-align: right;
  width: calc(100% - 571px);
}
.data-table {
  margin-top: 20px;
}
.el-dropdown-link {
  cursor: pointer;
  color: #333;
  font-size: 13px;
}
.el-dropdown-menu {
  .el-input {
    width: calc(100% - 40px);
    margin: 5px 20px;
  }
}
.zeta-icon-filter {
  float: right;
  line-height: 40px;
  color: #cacbcf;
}
.zeta-icon-filter:hover {
  color: #569ce1;
}
</style>
