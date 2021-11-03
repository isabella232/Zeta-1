<template>
    <div class="step-two-content">
        <div class="title">
            <span>Modify Column</span>
        </div>
		<div>
			<el-radio-group v-model="platform" size="small">
                <el-radio-button border v-for="item in platformArr" :key="item" :label="item">{{ item.replace("NuMozart", "Mozart") }}</el-radio-button>
            </el-radio-group>
		</div>
		<div class="table-div">
            <span class="table-label">Table</span>
			<span class="table-name">{{ tableName }}</span>
        </div>
		<el-table :data="columnsBySearch" height="calc(100% - 76px - 30px - 54px)" :default-sort = "{prop: 'column_id', order: 'descending'}">
			<el-table-column prop="column_name" label="Column Name">
				<template slot-scope="scope">
					<div class="col-div">
						<div class="content-center">{{scope.row.column_name ? scope.row.column_name.toUpperCase() : ""}}</div>
					</div>
				</template>
			</el-table-column>
			<el-table-column prop="column_desc" label="Description" min-width="300">
				<template slot-scope="scope">
					<div v-if="!scope.row.edit" class="col-div">
						<quill-editor class="ql-editor no-toolbar col-desc-content" placeholder="Please input..." v-model="scope.row.column_desc" :options="noToolbarEditorOptions" @change="onDescChange(scope.row.column_desc, scope.row)"/>
						<!--i class="zeta-icon-format_shapes edit-display" @click="colEditClick(scope.row)"></i-->
						<span class="edit-display" @click="colEditClick(scope.row)">Format</span>
					</div>
					<quill-editor class="ql-editor" v-model="scope.row.column_desc" :options="editorOptions" @change="onDescChange(scope.row.column_desc, scope.row)" v-else/>
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
			<el-table-column prop="data_type" label="Type" width="150">
				<template slot-scope="scope">
					<div class="col-div">
						<div class="content-center">{{scope.row.data_type ? scope.row.data_type.toUpperCase() : ""}}</div>
					</div>
				</template>
			</el-table-column>
		</el-table>
    </div>
</template>

<script lang="ts">
import { Component, Vue, Watch } from 'vue-property-decorator';
import { quillEditor } from 'vue-quill-editor';
import conf from "./../metadata-config";
import _ from 'lodash';
const EDITOR_OPTION =  {
	modules: {
		toolbar: [
			[{ 'size': ['small', false, 'large'] }],
			['bold', 'italic'],
			[{ 'list': 'ordered'}, { 'list': 'bullet' }],
			['link', 'image']
		]
	},
	placeholder: "Please input..."
}
const NO_TOOLBAR_EDITOR_OPTION =  {
	modules: {
		keyboard: {
			bindings: {
				tab: true
			}
		}
	},
	placeholder: "Please input..."
}

@Component({
  components: {
      quillEditor
  },
})
export default class RegisterStepTwo extends Vue {
	platformArr: Array<any> = [];
	platform: string = "";
	editorOptions = EDITOR_OPTION;
	noToolbarEditorOptions = NO_TOOLBAR_EDITOR_OPTION;
	tableName: string = "";
	update_columns: Array<any> = [];

    constructor() {
		super();
		const store: any =  this.$store.getters.getRegisterStepTwo || [];
        this.platformArr =  _.uniq(_.transform(_.map(store, "platform"), (rs: any, v: any) => { rs.push(_.upperFirst(_.toLower(v)).replace("mozart", "Mozart")) }, []));
		this.platform = !_.isEmpty(this.platformArr) ? this.platformArr[0] : "";
		const store_one: any = this.$store.getters.getRegisterStepOne || {};
		this.tableName = (store_one.database ? _.toUpper(store_one.database) : "") +
						 (store_one.database && store_one.table_name ? "." : "") +
					 	 (store_one.table_name ? _.toUpper(store_one.table_name) : "");
	}

	get columnsBySearch(): Array<any> {
		const store: any =  this.$store.getters.getRegisterStepTwo || {};
		const find: any = _.find(store, (v: any) => { return (_.toLower(v.platform) == _.toLower(this.platform)) });
        if (find) {
			_.forEach(find.column, (v: any) => {
				v.edit = v.edit ? true : false;
				v.bucket_flag = v.bucket_flag == 'Y' || v.index_flag == 'Y' ? "Y" : "N";
				const findColumn: any = _.find(this.update_columns, (sv: any) => { return _.trimEnd(_.toUpper(sv.column_name)) == _.trimEnd(_.toUpper(v.column_name)) });
				if (findColumn) {
					v.edit = findColumn.edit;
					v.column_desc = !_.isUndefined(findColumn.column_desc) ? findColumn.column_desc : v.column_desc;
				}
			});
            return _.sortBy(find.column, ["column_id"]);
        }else {
			return [];
		}
	}

	colEditClick(col: any) {
		_.forEach(this.columnsBySearch, (v: any) => {
			v.edit = false;
		})
		col.edit = true;
		this.onDescChange(null, col);
    }

	onDescChange(val: any, col: any) {
		_.remove(this.update_columns, (v: any) => {return _.toUpper(v.column_name) == _.toUpper(col.column_name)});
		col.column_name = _.toUpper(col.column_name);
		this.update_columns.push(col);
    }
	
	doNext() {
		this.$store.dispatch("setUpdateColumns", this.update_columns);
		this.$emit("ready-do-next");
	}
}
</script>

<style lang="scss" scoped>
.step-two-content {
	box-sizing: border-box;
	font-style: normal;
	height: 100%;
	overflow-y: hidden;
	padding: 0 25px;
	width: 100%;
}
.title {
	color: #1E1E1E;
	display: block;
	font-size: 18px;
	font-weight: 700;
	height: 36px;
	line-height: 36px;
	padding: 20px 0;
}
.table-div {
	color: #333333;
	font-size: 14px;
	line-height: 14px;
	padding: 20px 0;
	.table-label {
		font-weight: 700;
	}
	.table-name {
		padding-left: 10px;
	}
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
			width: 100%;
			display: flex;
        }
		.content-center {
			margin: auto 0;
		}
		.col-desc-content {
			width: 100%;
			margin: auto 0;
		}
		.edit-display {
			color: #569ce1;
			position: absolute;
			font-size: 12px;
			bottom: 35px;
			right: 36px;
			cursor: pointer;
			display: none;
		}
		.col-div:hover .edit-display {
			display: inline-block;
		}
		.edit-display:hover {
			color: #4D8CCA;
		}
		.ql-editor.no-toolbar {
			.ql-toolbar {
				display: none;
			}
			.ql-container {
				border: 1px solid #ccc;
			}
		}
		.ql-editor p {
			width: calc( 100% - 35px);
		}
    }
	.content-center {
		margin: auto 0;
	}
}
</style>