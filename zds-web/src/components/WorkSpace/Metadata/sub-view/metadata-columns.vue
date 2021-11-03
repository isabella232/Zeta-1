<template>
    <div class="columns-display">
        <div class="table-container"> 
            <!-- todo v-for -->
            <el-radio-group v-model="platform" size="small">
                <el-radio-button border v-for="item in platformArr" :key="item" :label="item">{{ item.replace("NuMozart", "Mozart") }}</el-radio-button>
            </el-radio-group>
            <div class="db-sel-div">
                <span class="db-label">Database</span>
                <el-radio v-model="db" v-for="(item, $i) in dbArr" :key="item.db" :label="item.db">{{ item.db }}<span v-if="$i < (dbArr.length - 1)" class="separator-line"></span></el-radio>
            </div>
            <el-table :data="columnsBySearch" height="calc(100% - 40px - 56px)" :default-sort = "{prop: 'column_id', order: 'descending'}">
                <el-table-column prop="column_name" label="Column Name">
                    <template slot-scope="scope">
                        <div class="col-div">
                            <div class="content-center">{{scope.row.column_name ? scope.row.column_name.toUpperCase() : ""}}</div>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column prop="column_desc" label="Description" min-width="300">
                    <template slot-scope="scope">
                        <div class="edit-enable" v-if="edit">
                            <div v-if="!scope.row.edit" class="col-div" @click="colEditClick(scope.row)">
                                <div class="col-desc-content" v-html="scope.row.column_desc"></div>
                                <i class="zeta-icon-edit edit-display"></i>
                            </div>
                            <quill-editor class="ql-editor" v-model="scope.row.column_desc" :options="editorOptions" @change="onDescChange(scope.row.column_desc, scope.row)" v-else/>
                        </div>
                        <div v-else>
                            <div class="col-div">
                                <div class="col-desc-content" v-html="scope.row.column_desc"></div>
                            </div>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column prop="ppi_flag" label="Partition" width="110" align="center" sortable>
                    <template slot-scope="scope">
                        <div style="text-align: center; font-size: 25px;">
                            <i class="zeta-icon-finish" style="color: #569ce1;" v-if="scope.row.ppi_flag == 'Y'"></i>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column prop="bucket_flag" label="Bucket" width="100" align="center" sortable>
                    <template slot-scope="scope">
                        <div style="text-align: center; font-size: 25px;">
                            <i class="zeta-icon-finish" style="color: #569ce1;" v-if="scope.row.bucket_flag == 'Y'"></i>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column prop="pii_flag" label="PII" width="100" align="center" sortable>
                    <template slot-scope="scope">
                        <div style="text-align: center; font-size: 25px;">
                            <i class="zeta-icon-finish" style="color: #569ce1;" v-if="scope.row.pii_flag == 'Y'"></i>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column prop="data_type" label="Type" width="150">
                    <template slot-scope="scope">
                        <div class="col-div">
                            <div class="content-center">{{scope.row.data_type ? scope.row.data_type.toUpperCase() : ""}}</div>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column prop="sampleData" label="Sample Data" width="120">
                    <template slot-scope="scope">
                        <div class="ceil sample-data-div">
                            <i class="zeta-icon-result" @click="sampleDataEditClick(scope.row)" title="Detail"></i>
                            <span v-html="scope.row.sampleData"></span>
                        </div>
                    </template>
                </el-table-column>
            </el-table>
        </div>
        <MetadataSampleDataDialog ref="dialog" :loading="loading" v-if="sampleDataDialogVisible" :visible.sync="sampleDataDialogVisible" :data="sampleData" :edit="true" :column="sampleDataColumnName" @dialog-comfirm="addSampleData" @close="dialogClose"/>
    </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch } from 'vue-property-decorator';
import MetadataSampleDataDialog from './metadata-sample-data-dialog.vue';
import _ from 'lodash';
import { quillEditor } from 'vue-quill-editor';
import conf from "./../metadata-config";

const EDITOR_OPTION =  {
          modules: {
            toolbar: [
              [{ 'size': ['small', false, 'large'] }],
              ['bold', 'italic'],
              [{ 'list': 'ordered'}, { 'list': 'bullet' }],
              ['link', 'image']
            ]
          }
}
@Component({
  components: {
      MetadataSampleDataDialog,
      quillEditor
  },
})
export default class MetadataColumns extends Vue {
    @Prop() metadataTableDict: any;
    @Prop() metadataViewDict: any;
    @Prop() sampleDataDict: any;
    @Prop() edit: boolean;
    @Prop() loading: boolean;
    editorOptions = EDITOR_OPTION;
    platform: string = "";
    platformArr: Array<any> = [];
    db: string = "";
    selDb: any = {};
    temp_columns: Array<any> = [];
    update_columns: Array<any> = [];
    sampleDataDialogVisible: boolean = false;
    sampleData: Array<any> = [];
    sampleDataColumnName: string = "";

    get dbArr(): Array<any> {
        const pickTable: any = _.pickBy(this.metadataTableDict, (v: any) => { return _.toLower(v.platform) == _.toLower(this.platform) });
        const pickView: any = _.pickBy(this.metadataViewDict, (v: any) => { return _.toLower(v.platform) == _.toLower(this.platform) });
        let rs: any = [];
        _.forEach(pickTable, (v: any) => {
            rs.push({db: v.db_name == "" ? "Default" : _.toUpper(v.db_name), type: "table"})
        })
        _.forEach(pickView, (v: any) => {
            rs.push({db: v.view_db == "" ? "Default" : _.toUpper(v.view_db), type: "view"})
        })
        const accessIdx = _.findIndex(rs, (v: any) => {return v.db == "ACCESS_VIEWS"});
        const accessDb = _.find(rs, (v: any) => {return v.db == "ACCESS_VIEWS"});
        if (accessIdx > -1 && accessDb) {
            rs.splice(accessIdx, 1);
            rs.splice(0, 0, accessDb);
        }
        this.db = !_.isEmpty(rs) ? rs[0]["db"] || "" : "";
        return rs;
    }

    get columnsBySearch(): Array<any> {
        const findTable: any = _.find(this.metadataTableDict, (v: any) => { return (_.toLower(v.platform) == _.toLower(this.platform) &&  _.toUpper(v.db_name) == (this.db == "Default" ? "" : this.db)) });
        const findView: any = _.find(this.metadataViewDict, (v: any) => { return (_.toLower(v.platform) == _.toLower(this.platform) &&  _.toUpper(v.view_db) == (this.db == "Default" ? "" : this.db)) });
        if (findTable || findView) {
            let rs: any = [];
            _.forEach(_.cloneDeep(findTable ? findTable.column : findView.column), (v: any) => {
                v.edit = false;
                v.bucket_flag = v.bucket_flag == 'Y' || v.index_flag == 'Y' ? "Y" : "N";
                const find: any = _.find(this.update_columns, (sv: any) => { return _.trimEnd(_.toUpper(sv.column_name)) == _.trimEnd(_.toUpper(v.column_name)) });
                if (find) {
                    v.edit = true;
                    v.column_desc = !_.isUndefined(find.column_desc) ? find.column_desc : v.column_desc;
                    v.sampleData = find.sampleData || v.sampleData;
                }

                const findSampleQuery: any = _.find(this.sampleDataDict, (sv: any) => { return _.trimEnd(_.toUpper(sv.column_name)) == _.trimEnd(_.toUpper(v.column_name)) });
                if (findSampleQuery) {
                    v.sampleDataArr = findSampleQuery.sample_value ? JSON.parse(findSampleQuery.sample_value) : [];
                    v.sampleData = v.sampleDataArr.length > 0 ? v.sampleDataArr[0] : "";
                }else {
                    v.sampleData = "";
                    v.sampleDataArr = [];
                }
                rs.push(v);
            });
            this.temp_columns = _.sortBy(rs, ["column_id"]);
            return this.temp_columns;
        }
        return [];
    }

    constructor() {
        super();

        let rs: any = [];
        if (this.metadataTableDict && !_.isEmpty(this.metadataTableDict)) {
        const arr = _.uniq(_.map(this.metadataTableDict, "platform"));
        _.forEach(arr, (v: any) => {
            rs.push(v.toLowerCase().replace(/( |^)[a-z]/g, (L: any) => L.toUpperCase()).replace("mozart", "Mozart"));
        })
        }
        
        if (this.metadataViewDict && !_.isEmpty(this.metadataViewDict)) {
        const arr = _.uniq(_.map(this.metadataViewDict, "platform"));
        _.forEach(arr, (v: any) => {
            rs.push(v.toLowerCase().replace(/( |^)[a-z]/g, (L: any) => L.toUpperCase()).replace("mozart", "Mozart"));
        })
        }
        this.platformArr = _.intersection(conf.allPlatform, _.uniq(rs));
        this.platform = !_.isEmpty(this.platformArr) ? this.platformArr[0] : "";
    }

    selectDb(item: any) {
        this.selDb = item;
    }

    colEditClick(col: any) {
        col.edit = true;
        this.onDescChange(undefined, col);
        this.$emit("edit-model");
    }

    sampleDataEditClick(col: any) {
        let rs: any = [];
        _.forEach(col.sampleDataArr, (v: any) => {
            rs.push({text: v})
        })
        this.sampleData = rs;
        this.sampleDataColumnName = col.column_name;
        this.sampleDataDialogVisible = true;
    }

    revert() {
        this.cleanCache();
        const findTable = _.find(this.metadataTableDict, (v: any) => { return (_.toLower(v.platform) == _.toLower(this.platform) &&  _.toUpper(v.db_name) == (this.db == "Default" ? "" : this.db)) });
        const findView = _.find(this.metadataViewDict, (v: any) => { return (_.toLower(v.platform) == _.toLower(this.platform) &&  _.toUpper(v.view_db) == (this.db == "Default" ? "" : this.db)) });
        if (findTable || findView) {
            let rs: any = [];
            _.forEach(_.cloneDeep(findTable ? findTable.column : findView.column), (v: any) => {
                v.edit = false;
                rs.push(v)
            });
            this.temp_columns = rs;
        }
    }

    cleanCache() {
        this.update_columns = [];
    }

    dialogClose() {
        this.sampleDataDialogVisible = false;
    }

    onDescChange(val: any, col: any) {
        _.remove(this.update_columns, (v: any) => {return _.toUpper(v.column_name) == _.toUpper(col.column_name)});
        col.db = this.db;
        col.column_name = _.toUpper(col.column_name);
        this.update_columns.push(col);
    }

    addSampleData(params: any) {
        this.$emit("submit-sample-data", params);
    }

    @Watch("edit")
    onEditChange(val: any) {
        if (!val) {
            _.forEach(this.columnsBySearch, (v: any) => {
                v.edit = false;
            })
        }
    }
}
</script>
<style lang="scss" scoped>
.ql-editor {
    width: 100%;
}
/deep/ .ql-snow {
    .ql-formats,
    .ql-toolbar, &.ql-toolbar {
        &:after {
            display: block !important;
        }
    }
}
/deep/ .ql-container .ql-snow {
    height: calc(100% - 50px);
}
.columns-display {
    height: 100%;
    .table-container {
        height: 100%;
    }
}
.el-radio-group {
    margin-top: 10px;
}
.edit-display {
    line-height: 18px;
    display: none;
    color: #569ce1;
    margin: auto 0;
}
.edit-enable:hover .edit-display {
    display: inline-block;
}
.edit-enable {
    display: flex;
    cursor: pointer !important;
}
.sample-data-div {
    display: flex;
    > span {
        display: block;
        line-height: 60px;
        margin-left: 5px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }
    > i {
        cursor: pointer;
        line-height: 60px;
    }
}
.col-desc-content {
    width: calc(100% - 25px);
    margin: auto 0;
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
.el-radio-button {
    box-shadow: none !important;
    /deep/ .el-radio-button__inner {
        background: inherit;
        background-color: #fff;
        border: 1px solid #569ce1 !important;
        border-radius: 4px !important;
        box-shadow: none !important;
        color: #569ce1;
        font-size: 14px;
        height: 30px;
        line-height: 10px;
        margin-right: 10px;
        min-width: 90px;
    }
    /deep/ .el-radio-button__orig-radio:checked + .el-radio-button__inner:hover {
      background-color:#4d8cca;
      border: 1px solid #4d8cca !important;
      color: #fff;
    }

    /deep/ .el-radio-button__inner:hover {
      border: 1px solid #4d8cca !important;
      color: #4d8cca;
    }
}

.db-sel-div {
    margin: 20px 0;
}
.db-label {
    font-weight: 700;
    font-style: normal;
    font-size: 14px;
    text-align: left;
    color: #333333;
    line-height: normal;
    margin-right: 30px;
}
.separator-line {
    width: 1px;
    border-left: 1px solid #cacbcf;
    margin-left: 10px;
}
.el-radio {
    margin: 0px !important;
    /deep/ .el-radio__inner {
        display: none;
    }
    /deep/ .el-radio__label {
        color: #cacbcf;
    }
    /deep/ .el-radio__label:hover {
        color: #4d8cca;
    }
}
.el-table {
    border-top: 2px solid #cacbcf;
    border-bottom: 2px solid #cacbcf;
    color: #333333;
    /deep/ .el-table__body-wrapper {
        .cell {
            line-height: 60px;
            min-height: 60px;
            word-break: break-word;
            white-space: pre-line;
            text-align: left;
        }
        .col-div {
            height: 100%;
            line-height: 18px !important;
            min-height: 60px;
        }
    }
    /deep/ .el-table__body-wrapper::-webkit-scrollbar {
		width: 0;
	}
}
.zeta-icon-result {
    color: #cacbcf;
}
[class^="zeta-icon-"], [class*=" zeta-icon-"] {
    font-size: 25px !important;
}
</style>
