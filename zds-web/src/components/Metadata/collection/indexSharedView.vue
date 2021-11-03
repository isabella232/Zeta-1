<template>
	<div class="my-collection">
		<div class="readonly-popup">
			<span class="left info">
				<i class="zeta-icon-info" />
				<template> This page is READ ONLY</template>
			</span>
		</div>
		<el-breadcrumb separator-class="el-icon-arrow-right" style="margin-bottom: 24px">
			<el-breadcrumb-item>
				<span @click="back">Metadata Browse</span>
			</el-breadcrumb-item>
			<el-breadcrumb-item>Collection</el-breadcrumb-item>
		</el-breadcrumb>

		<el-tabs v-model="activeTab" class="tabs-css">
			<el-tab-pane label="Table Status" name="status">
				<table-status :collectionData="collectionData"/>
			</el-tab-pane>
		</el-tabs>
	</div>
</template>

<script lang="ts">
/* eslint-disable @typescript-eslint/camelcase */
import { Vue, Component } from "vue-property-decorator";
import DoeRemoteService from "@/services/remote/DoeRemoteService";
import TableStatus from "./TableStatus.vue";
import MyCollection from "./MyCollection.vue";
import Util from '@/services/Util.service';

@Component({
	components: {
		TableStatus,
		MyCollection
	}
})
export default class indexSharedView extends Vue {
	doeRemoteService: DoeRemoteService = DoeRemoteService.instance;
	collectionData: any = [];

	activeTab: "status" | "collection" = "status";

	mounted() {
		this.getCollectionList(this.$route.query.nt);
	}

	back() {
		this.$router.push("/metadata");
	}

	getCollectionList(nt: any) {
		this.collectionData = [];
		this.doeRemoteService.getMyCollectionList(nt).then(res => {
			if (res && res.data) {
				this.collectionData = res.data;
			}
		});
	}
}
</script>
<style lang="scss" scoped>
@import "@/styles/metadata.scss";

.my-collection {
	height: calc(100% - 32px);
	padding: 0 16px 0;
	.el-breadcrumb__item {
		&:not(:last-of-type) {
			cursor: pointer;
			span {
				color: #4d8cca;
				&:hover {
					text-decoration: underline;
				}
			}
		}
	}
	.tabs-css {
		height: calc(100% - 38px);
		/deep/ .el-tabs__content {
			height: calc(100% - 55px);
		}
		/deep/ .el-tab-pane {
			height: 100%;
		}
	}
}
$color: #ebb563;
.readonly-popup {
	background-color: #fdf7e9;
	display: flex;
	justify-content: space-between;
	padding: 5px 10px;
	width: 100%;
	span.info {
		&,
		i {
			color: $color;
		}
	}
}
</style>
