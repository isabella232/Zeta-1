<template>
  <div class="columns-display">
    <div class="table-container">
      <div class="platform">
        <em>Platform:</em>
        <el-radio-group v-model="platform" size="small">
          <el-radio-button border v-for="item in platformArr" :key="item" :label="item">{{ item }}</el-radio-button>
        </el-radio-group>
      </div>
      <div class="db-sel-div">
        <em>Database:</em>
        <el-radio v-model="db" v-for="(item, $i) in dbArr" :key="item.db" :label="item.db">
          {{ item.db }}<span v-if="$i < (dbArr.length - 1)" class="separator-line" />
        </el-radio>
      </div>
      <el-table
        :data="columnsBySearch"
        :height="tableHeight"
        :default-sort="{prop: 'column_id', order: 'descending'}"
      >
        <el-table-column prop="column_name" label="Column" width="150" sortable>
          <template slot-scope="scope">
            <div class="col-div">
              <div class="content-center">{{ scope.row.column_name }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="data_type" label="Type" width="100" sortable>
          <template slot-scope="scope">
            <div class="col-div">
              <div class="content-center">{{ scope.row.data_type }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="ppi_flag" label="Partition" width="120" align="center" sortable>
          <template slot-scope="scope">
            <div style="text-align: center; font-size: 25px;">
              <i v-if="scope.row.ppi_flag == 'Y'" class="zeta-icon-finish" style="color: #569ce1;"  />
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="bucket_flag" label="Bucket" width="120" align="center" sortable>
          <template slot-scope="scope">
            <div style="text-align: center; font-size: 25px;">
              <i
                v-if="scope.row.bucket_flag == 'Y'"
                class="zeta-icon-finish"
                style="color: #569ce1;"
              />
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch } from "vue-property-decorator";
import _ from "lodash";
import { allPlatform } from '../utilties';
@Component({
  components: {}
})
export default class MetadataColumns extends Vue {
  @Prop() metadataTableDict: any;
  @Prop() metadataViewDict: any;
  platform = "";
  platformArr: Array<any> = [];
  db = "";
  selDb: any = {};
  temp_columns: Array<any> = [];
  tableHeight = "240px";
  get dbArr(): Array<any> {
    const pickTable: any = _.pickBy(this.metadataTableDict, (v: any) => {
      return _.toLower(v.platform) == _.toLower(this.platform);
    });
    const pickView: any = _.pickBy(this.metadataViewDict, (v: any) => {
      return _.toLower(v.platform) == _.toLower(this.platform);
    });
    const rs: any = [];
    _.forEach(pickTable, (v: any) => {
      rs.push({
        db: v.db_name == "" ? "Default" : _.toUpper(v.db_name),
        type: "table"
      });
    });
    _.forEach(pickView, (v: any) => {
      rs.push({
        db: v.view_db == "" ? "Default" : _.toUpper(v.view_db),
        type: "view"
      });
    });
    this.db = !_.isEmpty(rs) ? rs[0]["db"] || "" : "";
    return rs;
  }

  get columnsBySearch(): Array<any> {
    const findTable: any = _.find(this.metadataTableDict, (v: any) => {
      return (
        _.toLower(v.platform) == _.toLower(this.platform) &&
        _.toUpper(v.db_name) == (this.db == "Default" ? "" : this.db)
      );
    });
    const findView: any = _.find(this.metadataViewDict, (v: any) => {
      return (
        _.toLower(v.platform) == _.toLower(this.platform) &&
        _.toUpper(v.view_db) == (this.db == "Default" ? "" : this.db)
      );
    });
    if (findTable || findView) {
      const rs: any = [];
      _.forEach(
        _.cloneDeep(findTable ? findTable.column : findView.column),
        (v: any) => {
          v.edit = false;
          v.bucket_flag =
            v.bucket_flag == "Y" || v.index_flag == "Y" ? "Y" : "N";
          rs.push(v);
        }
      );
      this.temp_columns = _.sortBy(rs, ["column_id"]);
      return this.temp_columns;
    }
    return [];
  }

  constructor() {
    super();

    const rs: any = [];
    if (this.metadataTableDict && !_.isEmpty(this.metadataTableDict)) {
      const arr = _.uniq(_.map(this.metadataTableDict, "platform"));
      _.forEach(arr, (v: any) => {
        rs.push(
          v.toLowerCase().replace(/( |^)[a-z]/g, (L: any) => L.toUpperCase()).replace("mozart", "Mozart")
        );
      });
    }

    if (this.metadataViewDict && !_.isEmpty(this.metadataViewDict)) {
      const arr = _.uniq(_.map(this.metadataViewDict, "platform"));
      _.forEach(arr, (v: any) => {
        rs.push(
          v.toLowerCase().replace(/( |^)[a-z]/g, (L: any) => L.toUpperCase()).replace("mozart", "Mozart")
        );
      });
    }
    this.platformArr = _.intersection(allPlatform, _.uniq(rs));
    this.platform = !_.isEmpty(this.platformArr) ? this.platformArr[0] : "";
  }
  selectDb(item: any) {
    this.selDb = item;
  }
}
</script>
<style lang="scss" scoped>
.el-radio-button {
  box-shadow: none !important;
  /deep/ .el-radio-button__inner {
    display: inline-block;
    background: inherit;
    background-color: #fff;
    border: 1px solid #569ce1 !important;
    border-radius: 4px !important;
    box-shadow: none !important;
    color: #569ce1;
    font-size: 11px;
    margin-right: 5px;
    margin-bottom: 5px;
    padding: 0 5px;
    height: 20px;
    line-height: 18px;
  }
  /deep/ .el-radio-button__orig-radio:checked + .el-radio-button__inner {
    background-color: #569ce1;
    border: 1px solid #569ce1 !important;
    color: #fff;
  }

  /deep/ .el-radio-button__inner:hover {
    border: 1px solid #569ce1 !important;
    color: #569ce1;
  }
}
.columns-display {
  height: 100%;
  .table-container {
    height: 100%;
  }
}
p {
  em {
    font-style: normal;
    font-weight: 700;
    font-size: 13px;
  }
}

.content-center {
  margin: auto 0;
}
.no-drop {
  cursor: no-drop !important;
}
.col-div {
  width: 100%;
  display: flex;
}

.db-sel-div {
  margin: 5px 0 10px;
  color: #999999;
}
.db-label {
  font-weight: 700;
  font-style: normal;
  font-size: 14px;
  text-align: left;
  color: #333333;
  line-height: normal;
  margin-right: 10px;
}
.separator-line {
  width: 1px;
  border-left: 1px solid #cacbcf;
  margin-left: 10px;
}
.el-radio {
  margin-left: 0px !important;
  margin-right: 10px;
  /deep/ .el-radio__inner {
    display: none;
  }
  /deep/ .el-radio__label {
    font-size: 13px;
    color: #cacbcf;
    padding-left: 0;
  }
  /deep/ .el-radio__label:hover {
    color: #4d8cca;
  }
  /deep/ .el-radio__input.is-checked + .el-radio__label {
    color: #569ce1;
  }
}
.el-table {
  border-top: 2px solid #cacbcf;
  border-bottom: 2px solid #cacbcf;
  color: #333333;
  /deep/ .el-table__body-wrapper {
    .cell {
      line-height: 24px;
      min-height: 24px;
      word-break: break-word;
      white-space: pre-line;
      text-align: left;
    }
    .col-div {
      height: 100%;
      line-height: 18px !important;
      min-height: 24px;
    }
  }
  /deep/ .el-table__body-wrapper::-webkit-scrollbar {
    width: 0;
  }
}
.zeta-icon-result {
  color: #cacbcf;
}
[class^="zeta-icon-"],
[class*=" zeta-icon-"] {
  font-size: 25px !important;
}
</style>