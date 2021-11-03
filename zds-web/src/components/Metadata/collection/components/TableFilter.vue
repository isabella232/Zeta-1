<template>
	<div class="table-filter">
		<el-select
			v-model="selectedTable"
			class="search-box"
			multiple
			collapse-tags
			placeholder="Select"
			@change="onSelTbChange"
			ref="selectedTable"
		>
			<el-option label="All Tables" value="all"></el-option>
			<a class="add-link" href="#" @click="jumpCollection" v-if="getCollectionSize < 1">add table to your collection</a>
			<el-option
				v-for="item in collectionData"
				:key="item.collection_name"
				:label="item.collection_name"
				:value="item.collection_name"
			/>
		</el-select>
		<el-checkbox-group
			v-model="selectedPlatforms"
			size="mini"
			class="options"
			:min="1"
			@change="onChkPltformChange"
		>
			<el-checkbox
				v-for="p in platforms"
				:key="p.key"
				:class="'opt-' + p.key"
				border
				:label="p.label"
				:value="p.key"
				@change="queryLine"
			/>
		</el-checkbox-group>
		<div
			style="border: 1px solid; padding: 0 5px; display: inherit; height: 26px; margin-left: 10px; border-radius: 3px;"
		>
			<div>
				<hr style="border: 1px solid #569ce1; width: 30px; margin: 12px 0;" />
			</div>
			<div>
				<span style="line-height: 28px; font-size: 12px; padding: 0 5px;">Data Availability</span>
			</div>
			<!--div>
				<hr style="border: 1px dashed #569ce1; width: 30px; margin: 12px 0;" />
			</div>
			<div>
				<span style="line-height: 28px; font-size: 12px; padding-left: 5px;">DQ Failure Rate</span>
			</div-->
		</div>
	</div>
</template>

<script lang="ts">
/* eslint-disable @typescript-eslint/camelcase */
import { Vue, Component, Prop, Watch, Inject } from "vue-property-decorator";
import _ from "lodash";

@Component({
	components: {}
})
export default class TableFilter extends Vue {
	@Prop() collectionData: any;
	@Inject() jumpCollectionTab: any;
	selectedPlatforms = ["Apollo RNO/Hermes"];
	selectedTable = ["all"];
	platforms = [
		{ key: "apollo_rno", label: "Apollo RNO/Hermes" },
		{ key: "hercules", label: "Hercules" },
		{ key: "numozart", label: "Mozart" }
	];

	get getCollectionSize() {
		return _.size(this.collectionData);
	}

	queryLine() {
		const tableList =
			_.indexOf(this.selectedTable, "all") > -1
				? _.map(this.collectionData, "collection_name")
				: this.selectedTable;
		const platforms = _.replace(
			_.replace(_.join(this.selectedPlatforms, ","), "Mozart", "numozart"),
			"Apollo RNO/Hermes",
			"apollo_rno"
		);
		if (!_.isEmpty(tableList)) this.$emit("query-chart", platforms, tableList);
	}

	onSelTbChange() {
		if (_.isEmpty(this.selectedTable) || _.last(this.selectedTable) == "all") {
			this.selectedTable = ["all"];
		} else {
			_.remove(this.selectedTable, (v: any) => {
				return v == "all";
			});
		}
		this.queryLine();
	}

	onChkPltformChange() {
		this.queryLine();
	}

	jumpCollection() {
		this.jumpCollectionTab();
		const selectedTable: any = this.$refs.selectedTable;
		if (selectedTable && selectedTable.blur) {
			selectedTable.blur();
		}
	}

	@Watch("collectionData")
	onCollectionDataChange() {
		this.queryLine();
	}
}
</script>
<style lang="scss" scoped>
@import "@/styles/metadata.scss";

@mixin checkbox($color) {
	color: $color;
	border-color: $color;
	/deep/ .el-checkbox__inner {
		border-color: $color;
	}
	&.is-checked {
		/deep/ .el-checkbox__label {
			color: $color;
		}
		/deep/ .el-checkbox__inner {
			background-color: $color;
		}
	}
}

.table-filter {
	display: flex;
	.search-box {
		width: 400px;
		margin-right: 20px;
	}

	.options {
		> * {
			margin-left: 0;
			margin-right: 4px;
			padding-top: 4px;
		}
		.opt-numozart {
			@include checkbox($mozart);
		}
		.opt-hermes {
			@include checkbox($hermes);
		}
		.opt-hopper {
			@include checkbox($hopper);
		}
		.opt-hercules {
			@include checkbox($hercules);
		}
		.opt-apollo_rno {
			@include checkbox($apollo);
		}
	}
}
.add-link {
	cursor: pointer;
	height: 30px !important;
	line-height: 30px !important;
	padding: 0 20px;
	font-size: 14px;
}
</style>
