<template>
  <div
    v-loading="loading"
    class="table-managerment-container"
  >
    <div class="header">
      <el-breadcrumb
        v-if="backSa !== ''"
        separator-class="el-icon-arrow-right"
      >
        <el-breadcrumb-item>
          <span
            class="breadcrumb-clk"
            @click="back"
          >Return SA Management</span>
        </el-breadcrumb-item>
      </el-breadcrumb>
      <el-button
        v-if="!edit && !readOnly"
        type="primary"
        class="edit-btn"
        :disabled="!allowDo"
        @click="edit = true"
      >
        Edit
      </el-button>
      <el-button
        v-if="edit"
        type="default"
        plain
        class="cancel-btn"
        @click="edit = false; getPageTables();"
      >
        Cancel
      </el-button>
      <el-button
        v-if="edit"
        type="primary"
        class="save-btn"
        @click="preUpdate(); saveDialogVis = true;"
      >
        Save
      </el-button>
      <el-dropdown
        class="float-right"
        :hide-on-click="false"
      >
        <el-button
          type="default"
          plain
        >
          View Columns
          <i class="el-icon-arrow-down el-icon--right" />
        </el-button>
        <el-dropdown-menu slot="dropdown">
          <el-checkbox-group
            v-model="ckColumnValue"
            @change="columnChange"
          >
            <el-dropdown-item
              v-for="cloumn in columnOptions"
              :key="cloumn.column_label"
              :command="cloumn.column_label"
            >
              <el-checkbox
                :key="cloumn.column_label"
                :label="cloumn.column_label"
              />
            </el-dropdown-item>
          </el-checkbox-group>
        </el-dropdown-menu>
      </el-dropdown>
      <el-button
        class="float-right"
        type="default"
        plain
        style="margin-right: 10px;"
        :disabled="!allowDo"
        @click="initCheckedColumn();downloadDialogVis = true"
      >
        Download
      </el-button>
      <el-button
        class="float-right"
        type="default"
        plain
        style="margin-right: 10px;"
        @click="resetFilter()"
      >
        Reset Filter
      </el-button>
      <el-button
        class="float-right"
        type="default"
        plain
        @click="copytext"
      >
        Share
      </el-button>
    </div>
    <div class="tab-div">
      <div class="filter-bars">
        <ul>
          <li
            v-for="tab in tabInfo"
            :key="tab.tab_id"
            :class="{'active':activeName === tab.tab_id}"
            @click="()=> {activeName = tab.tab_id;handleClick();}"
          >
            {{ tab.tab_label }}
          </li>
        </ul>
      </div>
      <div class="content">
        <el-table
          v-if="columns.length > 0"
          ref="multipleTable"
          class="data-table"
          :data="tableData"
          border
          height="calc(100% - 130px)"
          @selection-change="handleSelectionChange"
          @select="handleSelect"
        >
          <el-table-column
            v-if="edit"
            type="selection"
            width="40"
            align="center"
          />
          <el-table-column
            v-for="column in columns"
            :key="column.property"
            :property="column.property"
            :label="column.column_label"
            :align="column.align"
            header-align="left"
            :fixed="column.fixed"
            :width="column.width"
            :min-width="column.minWidth"
          >
            <template
              slot="header"
              slot-scope="scope"
            >
              <FilterComponent
                v-if="column.type == 'filter'"
                ref="filter"
                :options-data="getFilterOptions(JSON.parse(filterMapStr)[column.property], column.property, column.format)"
                :title="column.column_label"
                :property="column.property"
                :sortable="column.sortable"
                :default-selected="getDefaultSelected(column.property, column.default_selected)"
                @sort-clk="sortCol"
                @call-back="updateFilter"
              />
              <SearchComponent
                v-else-if="column.type == 'search-filter'"
                ref="searchFilter"
                :title="column.column_label"
                :property="column.property"
                :sortable="column.sortable"
                @sort-clk="sortCol"
                @call-back="updateFilter"
              />
              <DatepickerFilterComponent
                v-else-if="column.type == 'date-filter'"
                ref="dateFilter"
                :title="column.column_label"
                :property="column.property"
                :sortable="column.sortable"
                @sort-clk="sortCol"
                @call-back="updateFilter"
              />
              <div v-else>
                <span>{{ column.column_label }}</span>
                <span
                  v-if="column.sortable"
                  class="caret-wrapper"
                >
                  <i
                    class="sort-caret ascending"
                    @click="sortCol('asc', column.property)"
                  />
                  <i
                    class="sort-caret descending"
                    @click="sortCol('desc', column.property)"
                  />
                </span>
              </div>
            </template>
            <template slot-scope="scope">
              <div v-if="edit && column.edit">
                <el-select
                  v-if="column.edit_type == 'filter'"
                  v-model="scope.row[column.property]"
                  @change="valueChange(scope.row[column.property], column.property, scope.row)"
                >
                  <el-option
                    v-for="item in getEditOptions(column.property)"
                    :key="item.label + scope.$index"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
                <el-date-picker
                  v-else-if="column.edit_type == 'date'"
                  v-model="scope.row[column.property]"
                  type="date"
                  :value-format="column.value_format"
                  @change="valueChange(scope.row[column.property], column.property, scope.row)"
                />
                <el-input
                  v-else-if="column.edit_type == 'textarea'"
                  v-model="scope.row[column.property]"
                  type="textarea"
                  :rows="1"
                  @change="valueChange(scope.row[column.property], column.property, scope.row)"
                />
                <el-input
                  v-else
                  v-model="scope.row[column.property]"
                  :type="'text'"
                  @change="valueChange(scope.row[column.property], column.property, scope.row)"
                />
              </div>
              <div v-else>
                <div
                  v-if="column.property == 'table_name'"
                  style="display: flex;"
                >
                  <span
                    class="table-name-label"
                    style="width: 100%; display: block;"
                    @click="jumpMetadata(scope.row.table_name)"
                  >{{ scope.row[column.property].toUpperCase() }}</span>
                </div>
                <span
                  v-else-if="column.property == 'db_name'"
                >{{ transformDBName(scope.row[column.property]) }}</span>
                <i
                  v-for="obj in platformArr(scope.row[column.property])"
                  v-else-if="column.property == 'pltfrm_name'"
                  :key="obj"
                  :class="obj"
                  class="platform-icon"
                />
                <ul
                  v-else-if="column.format == 'html'"
                  class="change-log-ul"
                >
                  <li
                    v-for="(data, $i) in filterChangeLog(scope.row.table_name)"
                    :key="$i"
                  >
                    <div
                      style="margin: 10px 0;"
                      v-html="createHtml(data)"
                    />
                  </li>
                </ul>
                <el-tag
                  v-for="tag in splitStringToList(scope.row[column.property])"
                  v-else-if="column.format == 'group'"
                  :key="tag"
                >
                  {{ tag }}
                </el-tag>
                <span v-else>{{ formatTableContent(scope.row[column.property], column.format) }}</span>
              </div>
            </template>
          </el-table-column>
        </el-table>
        <div class="footer">
          <el-pagination
            layout="sizes, prev, pager, next"
            :current-page.sync="currentPage"
            :page-sizes="[20, 50, 100, 200]"
            :total="total"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
          <span class="total-panel">
            Total
            <span style="font-weight: bold;">{{ total }}</span> tables retained
          </span>
        </div>
      </div>
    </div>
    <el-dialog
      v-loading="loading"
      title="Choose Download Fields"
      :visible.sync="downloadDialogVis"
      width="500"
      center
    >
      <el-checkbox
        v-model="checkAll"
        :indeterminate="isIndeterminate"
        @change="handleCheckAllChange"
      >
        Select All
      </el-checkbox>
      <div style="margin: 15px 0;" />
      <el-checkbox-group
        v-model="checkedColumn"
        @change="handleCheckedChange"
      >
        <el-checkbox
          v-for="column in columnOptions"
          :key="column.property"
          :label="column.property"
        >
          {{ column.column_label }}
        </el-checkbox>
      </el-checkbox-group>
      <span
        slot="footer"
        class="dialog-footer"
      >
        <el-button @click="downloadDialogVis = false">Cancel</el-button>
        <el-button
          type="primary"
          @click="download();"
        >Download</el-button>
      </span>
    </el-dialog>
    <el-dialog
      v-loading="loading"
      :title="commitUpdateColArr.length + ' tables will be updated'"
      :visible.sync="saveDialogVis"
      width="500"
      height="500"
    >
      <el-table
        :data="commitUpdateColArr"
        border
        style="width: 100%"
        max-height="500"
      >
        <el-table-column
          prop="table_name"
          label="Table"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.table_name.toUpperCase() }}</span>
          </template>
        </el-table-column>
        <el-table-column
          prop="db_name"
          label="Database"
        >
          <template slot-scope="scope">
            <span>{{ transformDBName(scope.row.db_name) }}</span>
          </template>
        </el-table-column>
        <el-table-column
          prop="pltfrm_name"
          label="Platform"
        >
          <template slot-scope="scope">
            <i
              v-for="obj in platformArr(scope.row.pltfrm_name)"
              :key="obj"
              :class="obj"
              class="platform-icon"
            />
          </template>
        </el-table-column>
        <el-table-column label="Change">
          <template slot-scope="scope">
            <div v-html="parseLog(scope.row.change)" />
          </template>
        </el-table-column>
      </el-table>
      <span
        slot="footer"
        class="dialog-footer"
      >
        <el-button @click="saveDialogVis = false">Cancel</el-button>
        <el-button
          type="primary"
          @click="updateTableManagement();"
        >Confirm</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script lang="ts">
import {
  Component,
  Vue,
  Inject,
  Prop,
} from 'vue-property-decorator';
import _ from 'lodash';
import FilterComponent from '@/components/DA/filter-component.vue';
import SearchComponent from '@/components/DA/search-component.vue';
import DoeRemoteService from '@/services/remote/DoeRemoteService';
import DatepickerFilterComponent from '@/components/DA/datepicker-filter-component.vue';
import {
  exportExcel,
} from '@/components/Metadata/DownloadUsage.ts';
import * as Logger from '@/plugins/logs/zds-logger';
import Util from '@/services/Util.service';
import $ from 'jquery';
import copy from 'copy-to-clipboard';

@Component({
  components: {
    FilterComponent,
    SearchComponent,
    DatepickerFilterComponent,
  },
})
export default class TableManagement extends Vue {
  @Inject('doeRemoteService') doeRemoteService: DoeRemoteService;
  @Prop({
    default: false,
  }) readOnly: boolean;
  @Prop() tabId: string;
  @Prop() query: any;
  loading = false;
  edit = false;
  downloadDialogVis = false;
  saveDialogVis = false;
  activeName = '';
  tabInfo: any = [];
  columnInfo: any = [];
  columns: any = [];
  columnOptions: any = [];
  optionInfo: any = [];
  editableColumns: any = [];
  ckColumnValue: any = {};
  filterMapStr: string = JSON.stringify({});
  queryObj: any = {};
  col = '';
  sort = 'asc';
  tableData: any = [];
  preTableData: any = [];
  changeLogData: any = [];
  multipleSelection: any = [];
  currentPage = 1;
  pageSize: any = 20;
  total = 1;
  updateArr: any = [];
  updateJSON: any = [];
  checkAll = false;
  checkedColumn: any = [];
  isIndeterminate = true;
  gridData: any = [];
  commitUpdateArr: any = [];
  commitUpdateColArr: any = [];
  backSa = '';
  permissions: any = [];

  get allowDo () {
    const find: any = _.find(this.tabInfo, (v: any) => {
      return v.tab_id == this.activeName;
    });
    return find && find.tab_edit;
  }

  get getTab () {
    return this.tabInfo;
  }

  get isAdmin () {
    return true;
  }

  copytext () {
    let query = `${location.protocol}//${location.host}/${Util.getPath()}#/tablemanagement?tab_id=${this.activeName}`;
    _.forEach(this.queryObj, (v: any, k: any) => {
      query += '&' + k + '=' + JSON.stringify(v.value);
    });
    const copyFlg: any = copy(query);
    if (copyFlg) {
      this.$message.success('Copy link to clipboard success!');
    }
  }

  isOwner (row: any) {
    return row.migration_nt == Util.getNt();
  }

  back () {
    this.$router.push(`/metadata/sa/${this.backSa}`);
  }

  created () {
    this.loading = true;
    this.doeRemoteService.getTbMngrTab().then((res: any) => {
      if (res) {
        this.tabInfo = res.tabInfo || [];
        this.columnInfo = res.columnInfo || [];
        this.optionInfo = res.optionInfo || [];
        this.activeName = this.tabId ? this.tabId : _.find(this.tabInfo, (v: any) => { return v.default_active == 1; })['tab_id'];
        this.columnOptions = this.activeName != '' ? _.filter(this.columnInfo, (v: any) => { return v.tab_id == this.activeName; }) : [];
        this.ckColumnValue = this.activeName != '' ? _.map(_.filter(this.columnInfo, (v: any) => { return v.tab_id == this.activeName && v.display == 1; }), 'column_label') : [];
        this.getAllFilter();
        const store: any = this.$store.getters.getTableManagementRoute;
        if (store.data && store.data.sbjct_area) {
          this.backSa = store.data.sbjct_area;
          this.$store.dispatch('setBrowseRoute', { data: false });
          this.$store.dispatch('setBrowseSearchRoute', { url: false });
          this.$store.dispatch('setTableManagementRoute', { data: false });
        }
        this.getTableManagement();
      }
    });
  }

  handleClick () {
    this.tableData = [];
    this.updateArr = [];
    this.columns = [];
    this.edit = false;
    this.columnOptions = this.activeName != '' ? _.filter(this.columnInfo, (v: any) => { return v.tab_id == this.activeName; }) : [];
    this.ckColumnValue = this.activeName != '' ? _.map(_.filter(this.columnInfo, (v: any) => { return v.tab_id == this.activeName && v.display == 1; }), 'column_label') : [];
    this.resetFilter();
  }

  columnChange (val: any) {
    this.columns = _.filter(this.columnOptions, (v: any) => _.indexOf(val, v.column_label) > -1);
    this.resetFilter();
  }

  handleSizeChange (val: any) {
    this.pageSize = val;
    this.currentPage = 1;
    this.getPageTables();
  }

  handleCurrentChange (val: any) {
    this.currentPage = val;
    this.getPageTables();
  }

  handleSelectionChange (val: any) {
    this.multipleSelection = val;
  }

  handleSelect (val: any, row: any) {
    const find: any = _.find(val, (v: any) => {
      return v.table_name == row.table_name &&
              v.db_name == row.db_name &&
              v.pltfrm_name == row.pltfrm_name &&
              v.sbjct_area == row.sbjct_area;
    });

    const findUpdate: any = _.find(this.updateArr, (v: any) => {
      return v.table_name == row.table_name &&
              v.db_name == row.db_name &&
              v.pltfrm_name == row.pltfrm_name &&
              v.sbjct_area == row.sbjct_area;
    });
    if (!find) {
      _.remove(this.updateArr, (v: any) => {
        return v.table_name == row.table_name &&
                v.db_name == row.db_name &&
                v.pltfrm_name == row.pltfrm_name &&
                v.sbjct_area == row.sbjct_area;
      });
    } else if (find && !findUpdate) {
      this.updateArr.push(row);
    }
  }

  formatTableContent (val: any, format: any) {
    switch (format) {
      case 'uppercase':
        return _.toUpper(val);
      case 'lowcase':
        return _.toLower(val);
      case 'upperfirst':
        return _.upperFirst(_.toLower(val));
      case 'group-platform':
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
      case 'group-uppercase':
        return _.toUpper(val);
      default:
        return val;
    }
  }

  getFilterOptions (filter: any, property: any, format?: any) {
    const arr: any = _.map(filter, property);
    switch (format) {
      case 'uppercase':
        return _.transform(
          arr,
          (rs: any, v: any) => {
            rs.push(_.toUpper(v));
          },
          []
        );
      case 'lowcase':
        return _.transform(
          arr,
          (rs: any, v: any) => {
            rs.push(_.toLower(v));
          },
          []
        );
      case 'upperfirst':
        return _.transform(
          arr,
          (rs: any, v: any) => {
            rs.push(_.upperFirst(_.toLower(v)));
          },
          []
        );
      case 'group':
        return _.sortBy(_.uniq(_.split(_.join(arr, ','), ',')));
      case 'group-uppercase':
        return _.sortBy(_.uniq(_.split(_.join(arr, ','), ',')));
      case 'group-platform':
        return _.transform(
          _.sortBy(_.uniq(_.split(_.join(arr, ','), ','))),
          (rs: any, v: any) => {
            rs.push(_.upperFirst(_.toLower(v).replace('numozart', 'mozart').replace('apollo_rno', 'apollo RNO')));
          },
          []
        );
      default:
        return _.uniq(arr);
    }
  }

  transformDBName (dbNames: any) {
    return _.join(
      _.uniq(
        _.transform(
          _.split(_.toUpper(dbNames), ','),
          (rs: any, v: any) => {
            rs.push(_.isEmpty(v) ? 'DEFAULT' : v);
          },
          []
        )
      ),
      ','
    );
  }

  platformArr (platforms: string): Array<any> {
    return _.sortBy(_.uniq(_.split(_.toLower(platforms), ',')), v => v);
  }

  jumpMetadata (tableName: any) {
    let url = `/tablemanagement?tab_id=${this.activeName}`;
    _.forEach(this.queryObj, (v: any, k: any) => {
      url += '&' + k + '=' + JSON.stringify(v.value);
    });
    this.$store.dispatch('setTableManagementRoute', { data: url });
    sessionStorage.setItem('metadata_table', _.toUpper(tableName));
    sessionStorage.setItem('metadata_table_type', 'table');
    Logger.counter('METADATA_TABLE_DETAIL', 1, {
      name: 'overview',
      trigger: 'table_management_click',
      table: _.toUpper(tableName),
    });
    this.$router.push('/metadata');
  }

  updateFilter (val: any, property: any) {
    this.setQueryObj(val, property);
    this.getTableManagement();
  }

  setQueryObj (val: any, property: string) {
    const find: any = _.find(this.columnOptions, (v: any) => {
      return v.property == property && v.tab_id == this.activeName;
    });

    if (find) {
      if (find.format == 'group-platform') {
        val = _.transform(
          val,
          (rs: any, sv: any) => {
            let value = sv;
            if (_.toLower(value) == 'apollo rno') value = 'Apollo_rno';
            if (_.toLower(value) == 'mozart') value = 'NuMozart';
            rs.push(value);
          },
          []
        );
      }
      
      this.queryObj[property] = { type: find.type, group: find.group, value: val };
    }
  }

  sortCol (sort: any, property: any) {
    const heads: any = $('.el-table__header th');
    heads.removeClass('ascending');
    heads.removeClass('descending');
    const index: any = _.findIndex(this.columns, (v: any) => {
      return v.property == property;
    });
    $(heads[index]).addClass(sort == 'asc' ? 'ascending' : 'descending');
    this.sort = sort;
    this.col = property;
    this.getSortTables();
  }

  valueChange (val: any, property: any, row: any) {
    const find: any = _.find(this.multipleSelection, (v: any) => {
      return (
        v.table_name == row.table_name &&
        v.db_name == row.db_name &&
        v.pltfrm_name == row.pltfrm_name &&
        v.sbjct_area == row.sbjct_area
      );
    });
    const elTable: any = this.$refs.multipleTable;
    if (!find) elTable.toggleRowSelection(row);
    _.forEach(this.multipleSelection, (v: any) => {
      // property !== 'comment' && (v[property] = val);
      const findIndex: any = _.findIndex(this.updateArr, (sv: any) => {
        return (
          v.table_name == sv.table_name &&
          v.db_name == sv.db_name &&
          v.pltfrm_name == sv.pltfrm_name &&
          v.sbjct_area == sv.sbjct_area
        );
      });
      if (findIndex > -1) {
        this.updateArr[findIndex] = v;
      } else {
        this.updateArr.push(v);
      }
    });
  }

  resetQuery () {
    this.queryObj = {};
    // this.queryStr = '';
    this.currentPage = 1;
    _.forEach(
      _.filter(this.columnOptions, (v: any) => v.type == 'filter'),
      (v: any) => {
        if (v.default_selected) {
          this.setQueryObj(_.split(v.default_selected, ','), v.property);
        }
      }
    );
  }

  resetSort () {
    const heads: any = $('.el-table__header th');
    heads.removeClass('ascending');
    heads.removeClass('descending');
    this.col = '';
  }

  resetFilter () {
    this.resetQuery();
    this.resetSort();
    // reset filter default selected
    const filters: any = this.$refs.filter;
    const searchfilters: any = this.$refs.searchFilter;
    const datefilters: any = this.$refs.dateFilter;
    _.forEach(filters, (v: any) => {
      const find: any = _.find(this.columnOptions, (sv: any) => {
        return sv.property == v.property && sv.tab_id == this.activeName;
      });
      v.ckValue =
        find && find.default_selected ? find.default_selected.split(',') : [];
    });
    _.forEach(datefilters, (v: any) => {
      v.dateValue = [];
    });
    let reGetFlg = true;
    _.forEach(searchfilters, (v: any) => {
      reGetFlg = reGetFlg && _.isEmpty(v.searchTxt);
      v.searchTxt = '';
    });
    if (reGetFlg) this.getTableManagement();
  }

  getPageTables () {
    if (!this.edit) this.updateArr = [];
    this.changeLogData = [];
    const params: any = {
      col: this.col,
      itemsPerPage: this.pageSize,
      page: this.currentPage,
      queryObj: this.queryObj,
      sort_ind: this.sort,
      tab_id: this.activeName,
    };
    this.loading = true;
    this.requestData(params, true, false);
  }

  getAllFilter () {
    this.queryObj = {};
    _.forEach(
      _.filter(this.columnInfo, (v: any) => v.type == 'filter'),
      (v: any) => {
        if (this.query && this.query[v.property] && v.tab_id == this.activeName) {
          this.setQueryObj(this.query[v.property], v.property);
        } else if (v.default_selected && v.tab_id == this.activeName) {
          this.setQueryObj(_.split(v.default_selected, ','), v.property);
        }
        const options = _.filter(this.optionInfo, (sv: any) => { return sv.tab_id == this.activeName && sv.property == v.property; });
        if (options && !_.isEmpty(options)) {
          const tmp: any = JSON.parse(this.filterMapStr);
          tmp[v.property] = _.transform(options, (rs: any, sv: any) => { const obj = {}; obj[v.property] = sv.value; rs.push(obj); }, []);
          this.filterMapStr = JSON.stringify(tmp);
        } else {
          this.getTableManagementColumnValues(v.property);
        }
      }
    );
  }

  getTableManagementColumnValues (columnName: any) {
    let params: any = 'col=' + columnName;
    if (columnName == 'mngr_name') params = params + '&col=mngr_nt';
    this.doeRemoteService
      .getTableManagementColumnValues(params)
      .then((res: any) => {
        if (res && res.data && res.data.data) {
          const tmp: any = JSON.parse(this.filterMapStr);
          tmp[columnName] = res.data.data;
          this.filterMapStr = JSON.stringify(tmp);
        }
      })
      .catch(err => {
        console.error(
          'Call Api:getTableManagementColumnValues ' +
            columnName +
            ' failed: ' +
            err
        );
      });
  }

  getTableManagement () {
    this.loading = true;
    if (!this.edit) this.updateArr = [];
    if (!this.edit) this.preTableData = [];
    this.tableData = [];
    this.changeLogData = [];
    const params: any = {
      col: this.col,
      itemsPerPage: this.pageSize,
      sort_ind: this.sort,
      queryObj: this.queryObj,
      tab_id: this.activeName,
    };
    this.requestData(params, false, true);
  }

  getSortTables () {
    this.changeLogData = [];
    if (!this.edit) this.updateArr = [];
    const params: any = {
      col: this.col,
      itemsPerPage: this.pageSize,
      sort_ind: this.sort,
      queryObj: this.queryObj,
      tab_id: this.activeName,
    };
    this.loading = true;
    this.requestData(params, true, true);
  }

  preUpdate () {
    this.commitUpdateArr = [];
    this.commitUpdateColArr = [];
    const editableCols: any = _.map(
      _.filter(this.columnOptions, (v: any) => {
        return v.edit;
      }),
      'property'
    );
    _.forEach(this.updateArr, (v: any) => {
      const findPreUpdate: any = _.find(this.preTableData, (sv: any) => {
        return (
          v.table_name == sv.table_name &&
          v.db_name == sv.db_name &&
          v.pltfrm_name == sv.pltfrm_name
        );
      });
      const update: any = {};
      if (findPreUpdate) {
        _.forEach(editableCols, (k: any) => {
          if (findPreUpdate[k] != v[k]) {
            update[k] = {
              oldVal: findPreUpdate[k],
              newVal: v[k],
            };
          }
        });
      }
      if (!_.isEmpty(update)) {
        const commitUpdateColData: any = {
          table_name: v.table_name,
          db_name: v.db_name,
          pltfrm_name: v.pltfrm_name,
          change: JSON.stringify(update),
          nt: Util.getNt(),
        };
        this.commitUpdateArr.push(v);
        this.commitUpdateColArr.push(commitUpdateColData);
      }
    });
  }

  parseLog (changeLog: any) {
    let html = '';
    if (!_.isEmpty(changeLog)) {
      _.forEach(JSON.parse(changeLog), (v: any, k: any) => {
        const find: any = _.find(this.columnOptions, (sv: any) => {
          return sv.property == k && sv.tab_id == this.activeName;
        });
        if (find) {
          html = html + '<div>' + find.column_label + ': ' + v.newVal + '</div>';
        }
      });
    }
    return html;
  }

  initCheckedColumn () {
    this.checkedColumn = _.map(_.filter(this.columnInfo, (v: any) => { return v.tab_id == this.activeName; }), 'property');
    this.checkAll = this.checkedColumn.length === this.columnOptions.length;
    this.isIndeterminate =
      this.checkedColumn.length > 0 &&
      this.checkedColumn.length < this.columnOptions.length;
  }

  handleCheckAllChange (val: boolean) {
    this.checkedColumn = val ? _.map(this.columnOptions, 'property') : [];
    this.isIndeterminate = false;
  }

  handleCheckedChange (value: any) {
    const checkedCount = value.length;
    this.checkAll = checkedCount === this.columnOptions.length;
    this.isIndeterminate =
      checkedCount > 0 && checkedCount < this.columnOptions.length;
  }

  download () {
    const params: any = {
      columns: 'concat(' + _.join(_.transform(this.checkedColumn, (rs: any, v: any) => {rs.push('ifnull(' + v + ', \'\')');}, []), ',\';;\',') + ') as result',
      queryObj: this.queryObj,
      tab_id: this.activeName,
    };
    this.loading = true;
    this.doeRemoteService
      .getAllTableManagement(params)
      .then((res: any) => {
        console.debug('Call Api:getAllTableManagement successed');
        if (res && res.data) {
          const head: any = [];
          _.forEach(this.checkedColumn, (v: any) => {
            const find: any = _.find(this.columnOptions, (sv: any) => {
              return sv.property == v;
            });
            if (find) head.push(find.column_label);
          });
          const result: any = [];
          result.push(head);
          _.forEach(res.data, (v: any) => {
            result.push(_.split(v.result, ';;'));
          });
          exportExcel(result);
        }
        this.downloadDialogVis = false;
        this.loading = false;
      })
      .catch((err: any) => {
        console.error('Call Api:getAllTableManagement failed: ' + err);
        this.loading = false;
      });
  }

  requestData (params: any, queryBySort: boolean, resetCurrentPage?: boolean) {
    this.loading = true;
    if (queryBySort) {
      this.doeRemoteService
        .getTableManagementPageSort(params)
        .then((res: any) => {
          console.debug('Call Api:getTableManagementPageSort successed');
          if (res && res.data) {
            if (resetCurrentPage) this.currentPage = 1;
            this.tableData =_.cloneDeep(res.data);
            this.preTableData = _.unionWith(this.preTableData, _.cloneDeep(res.data), _.isEqual);
            
          }
          this.loading = false;
        })
        .catch((err: any) => {
          console.error('Call Api:getTableManagementPageSort failed: ' + err);
          this.loading = false;
        })
        .finally(() => {
          this.updateTableData();
        });
    } else {
      this.doeRemoteService
        .getTableManagement(params)
        .then((res: any) => {
          console.debug('Call Api:getTableManagement successed');
          if (res && res.data) {
            if (resetCurrentPage) this.currentPage = 1;
            this.tableData = _.cloneDeep(res.data.tables);
            this.preTableData = _.unionWith(this.preTableData, _.cloneDeep(res.data.tables), _.isEqual);
            this.total = res.data.num[0].num;
          }
          this.columns = this.activeName != '' ? _.filter(this.columnOptions, (v: any) => { return _.indexOf(this.ckColumnValue, v.column_label) > -1; }) : [];
          this.loading = false;
        })
        .catch((err: any) => {
          console.error('Call Api:getTableManagement failed: ' + err);
          this.loading = false;
        })
        .finally(() => {
          this.updateTableData();
        });
    }
  }

  getEditOptions (property: string) {
    return _.filter(this.optionInfo, (v: any) => { return v.tab_id == this.activeName && v.property == property && v.type == 'editfilter'; });
  }

  updateTableManagement () {
    if (_.isEmpty(this.commitUpdateArr)) {
      this.$message.info('No change');
      this.saveDialogVis = false;
      this.edit = false;
    } else {
      this.loading = true;
      const params: any = {
        nt: Util.getNt(),
        list: this.commitUpdateArr,
        changeList: this.commitUpdateColArr,
        tab_id: this.activeName,
      };
      this.doeRemoteService
        .updateTableManagement(params)
        .then((res: any) => {
          console.debug('Call Api:updateTableManagement successed');
          if (res && res.status == 200) {
            this.$message.success('Save success');
            this.getPageTables();
          }
          this.loading = false;
          this.saveDialogVis = false;
          this.edit = false;
          this.updateArr = [];
        })
        .catch((err: any) => {
          console.error('Call Api:updateTableManagement failed: ' + err);
          this.loading = false;
          this.updateArr = [];
        });
    }
  }

  updateTableData () {
    if (this.edit) {
      const elTable: any = this.$refs.multipleTable;
      for (let i = 0; i < _.size(this.tableData); i++) {
        const find: any = _.find(this.updateArr, (v: any) => {
          return v.table_name == this.tableData[i].table_name &&
                  v.db_name == this.tableData[i].db_name &&
                  v.pltfrm_name == this.tableData[i].pltfrm_name &&
                  v.sbjct_area == this.tableData[i].sbjct_area;
        });
        if (find) {
          this.tableData[i] = _.cloneDeep(find);
          elTable.toggleRowSelection(this.tableData[i]);
        }
      }
    }
  }

  splitStringToList (content: string, separateCharacter?: string) {
    return content && _.size(content) > 0 ? _.split(content, separateCharacter ? separateCharacter : ',') : undefined;
  }

  getDefaultSelected (property: string, defaultSelected: any) {
    return this.query[property] ? _.join(this.query[property], ',') : defaultSelected;
  }
}

</script>
<style lang="scss" scoped>
  @import '@/styles/global.scss';
  @import '@/styles/metadata.scss';
  $bc-height: $workspace-tab-height+$workspace-tab-margin-bottom;

  .table-managerment-container {
    height: 100%;
    overflow-y: hidden;
  }

  .float-right {
    float: right;
  }

  .el-breadcrumb {
    margin-bottom: 16px;

    .breadcrumb-clk {
      cursor: pointer;
      color: #4d8cca;
    }

    .breadcrumb-clk:hover {
      color: #569ce1;
      text-decoration: underline;
    }
  }

  .data-table {
    margin: 10px 0;
  }

  .tab-div {
    width: 100%;
    display: flex;
    height: calc(100% - 60px);
    overflow: hidden;
    flex-wrap: wrap;

    .filter-bars {
      align-items: center;
      display: flex;
      height: 40px;
      justify-content: space-between;
      border-bottom: 1px solid #cacbcf;
      width: 100%;
      margin-bottom: 20px;

      ul {
        display: flex;
        height: 100%;
        list-style-type: none;
        width: 100%;

        >li {
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

    .content {
      width: 100%;
      height: 100%;
    }
  }

  .el-table {
    /deep/ .el-table__body-wrapper {
      height: calc(100% - 68px) !important;
    }

    /deep/ .el-table__header-wrapper  .el-checkbox {
      display: none;
    }

    .icon {
      text-align: center;
      display: none;
      font-weight: bolder;
    }

    /deep/ .warning-color {
      color: #ce3315;
      display: block !important;
    }

    /deep/ .pass-color {
      color: #52a552;
      display: block !important;
    }

    /deep/ .ignore-color {
      color: #db9d26;
      display: block !important;
    }

    /deep/ .el-date-editor {
      width: 135px !important;
    }
  }

  .change-log-ul {
    padding: 0 20px;
  }

  .el-dialog__body {
    .el-checkbox {
      width: 150px;
    }
  }

  .el-pagination {
    float: right;
    margin: 10px 0;
  }

  .total-panel {
    display: inline-block;
    float: right;
    margin: 10px 0;
    padding: 2px 5px;
    height: 28px;
    line-height: 28px;
  }

  .table-name-label {
    cursor: pointer;
  }

  .table-name-label:hover {
    color: #4d8cca !important;
  }

  .label-span {
    display: inline-block;
    color: #333;
    padding: 10px;
    font-weight: bold;
    font-size: 13px;
  }

  .content-span {
    display: block;
    font-size: 13px;
    padding: 0 10px;
  }

  .popover-icon {
    cursor: pointer;
    color: #569ce1;
  }

  .popover-icon:hover {
    color: #4d8cca;
  }

  /deep/ .el-table__body-wrapper::-webkit-scrollbar {
    width: 16px;
    height: 16px;
  }

  /deep/ .el-table__body-wrapper::-webkit-scrollbar-thumb {
    background-color: #ccc;
  }

  /deep/ .el-table__body-wrapper::-webkit-scrollbar-track {
    background-color: #eee;
  }

</style>