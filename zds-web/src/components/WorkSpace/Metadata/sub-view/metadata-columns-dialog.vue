<template>
  <el-dialog label-width="80px" title="Review the change" :visible.sync="visible_" @close="close">
    <span class="content">Please confirm the columns changed as below. The change will be deployed to the table/view on all platforms.</span>
    <el-table :data="tableData" border height="420" empty-text="No change">
        <el-table-column property="table" label="Table"></el-table-column>
        <el-table-column width="200" property="column" label="Column"></el-table-column>
        <el-table-column property="impact" label="Impact">
          <template slot-scope="scope">
            <div v-for="obj in scope.row.impact" :key="obj.platform" class="impact-div">
              <i :class="obj.platform.toLowerCase()" class="platform-icon"/>
              <ul class="sub-item-list">
                <li v-for="item in obj.db" :key="item" >{{ ((item != "" && item ? item : "DEFAULT") + "." + tableName).toUpperCase() }}</li>
              </ul>
            </div>
          </template>
        </el-table-column>
    </el-table>
  
    <div slot="footer" class="dialog-footer">
      <el-button type="primary" @click="confirm">Confirm</el-button>
      <el-button type="default" plain @click="close">Cancel</el-button>
    </div>
  </el-dialog>
</template>

<script lang="ts">

import { Component, Vue, Prop, Watch } from "vue-property-decorator";
import Util from '@/services/Util.service';
import _ from 'lodash';
@Component({
  components: {}
})
export default class MetadataColumnsDialog extends Vue {
  @Prop() visible: boolean;
  @Prop() metadataTableDict: any;
  @Prop() metadataViewDict: any;
  @Prop() data: any;
  @Prop() tableName: any;

  get tableData() {
    let rs: any = [];
    _.forEach(this.data, (v: any) => {
      let col: any = {
        table: _.toUpper(v.db + "." + this.tableName),
        column: v.column_name,
        impact: this.findImpact(v.column_name)
      }
      rs.push(col)
    })
    return rs;
  }

  set visible_(e) {
    this.$emit("update:visible", e);
  }

  get visible_(): boolean {
    return this.visible;
  }

  findImpact(column: any) {
    let rs: any = [];
    let platformArr: any = [];
    if (this.metadataTableDict && !_.isEmpty(this.metadataTableDict)) {
      const arr = _.uniq(_.map(this.metadataTableDict, "platform"));
      _.forEach(arr, (v: any) => {
        platformArr.push(v.toLowerCase().replace(/( |^)[a-z]/g, (L: any) => L.toUpperCase()));
      })
    }
    
    if (this.metadataViewDict && !_.isEmpty(this.metadataViewDict)) {
      const arr = _.uniq(_.map(this.metadataViewDict, "platform"));
      _.forEach(arr, (v: any) => {
        platformArr.push(v.toLowerCase().replace(/( |^)[a-z]/g, (L: any) => L.toUpperCase()));
      })
    }
    platformArr = _.uniq(platformArr);

    _.forEach(platformArr, (v: any) => {
      let db: any = [];
      const pickTable = _.pickBy(this.metadataTableDict, (sv: any) => {
        const platFlg: boolean = _.trimEnd(_.toUpper(sv.platform)) == _.trimEnd(_.toUpper(v));
        const findCol = _.find(sv.column, (ssv: any) => { return _.trimEnd(_.toUpper(ssv.column_name)) == _.trimEnd(_.toUpper(column))});
        return platFlg && findCol;
      });
      const pickView = _.pickBy(this.metadataViewDict, (sv: any) => {
        const platFlg: boolean = _.trimEnd(_.toUpper(sv.platform)) == _.trimEnd(_.toUpper(v));
        const findCol = _.find(sv.column, (ssv: any) => { return _.trimEnd(_.toUpper(ssv.column_name)) == _.trimEnd(_.toUpper(column))});
        return platFlg && findCol;
      });
      db = _.union(_.uniq(_.map(pickTable, "db_name")), _.uniq(_.map(pickView, "view_db")));
      if (!_.isEmpty(db)) rs.push({platform: _.upperFirst(v), db: db})
    });
    return rs;
  }

  confirm() {
    this.$emit("submit-columns");
    this.$emit("update:visible", false);
  }

  close() {
    this.$emit("update:visible", false);
  }
}
</script>

<style lang="scss" scoped>
@import '@/styles/metadata.scss';
span.content {
  margin-bottom: 10px;
  display: block;
}
ul.sub-item-list {
  list-style-type: none;
  display: inline-block;
  width: 100%;
  font-weight: 400;
  font-style: normal;
  text-align: left;
  line-height: 20px;
  font-size: 13px;
}
.impact-div {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 5px;
}
.platform-icon {
  width: 80px;
  height: 20px;
  text-align: center;
  margin-right: 10px !important;
}
</style>
