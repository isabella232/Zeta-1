<template>
	<div class="metadata-enotify">
		<div class="lastest-div">
			<span class="title">Latest Update</span>
			<table class="display">
				<tr>
					<th width="150">{{lastestArr.create_time}}</th>
					<td>
						<el-popover placement="bottom-start" trigger="click" width="800">
							<iframe :src="lastestArr.link" style="width: 100%; height: 700px; border: 0;"></iframe>
							<div slot="reference">
								<span class="content">{{lastestArr.enotify_title}}</span>
							</div>
						</el-popover>
					</td>
				</tr>
			</table>
		</div>
		<div class="maintenance-div">
			<span class="title">Maintenance Notifications</span>
			<table class="display">
				<tr v-for="item in getMaintenanceArr">
					<th width="150">{{item.create_time}}</th>
					<td>
						<el-popover placement="bottom-start" trigger="click" width="800">
							<iframe :src="item.link" style="width: 100%; height: 700px; border: 0;"></iframe>
							<div slot="reference">
								<span class="content">{{item.enotify_title}}</span>
							</div>
						</el-popover>
					</td>
				</tr>
			</table>
			<tr v-if="!showMoreMaintenanceFlag && maintenanceArr.length > 5">
				<th class="show-more" @click="showMore('maintenance')">Show More</th>
			</tr>
			<tr v-if="showMoreMaintenanceFlag">
				<th class="show-more" @click="showMore('maintenance')">Show Less</th>
			</tr>
		</div>
		<div class="product-div">
			<span class="title">Product Notifications</span>
			<table class="display">
				<tr v-for="item in getProductArr">
					<th width="150">{{item.create_time}}</th>
					<td>
						<el-popover placement="bottom-start" trigger="click" width="800">
							<iframe :src="item.link" style="width: 100%; height: 700px; border: 0;"></iframe>
							<div slot="reference">
								<span class="content">{{item.enotify_title}}</span>
							</div>
						</el-popover>
					</td>
				</tr>
			</table>
			<tr v-if="!showMoreProductFlag && productArr.length > 5">
				<th class="show-more" @click="showMore('product')">Show More</th>
			</tr>
			<tr v-if="showMoreProductFlag">
				<th class="show-more" @click="showMore('product')">Show Less</th>
			</tr>
		</div>
		<div class="general-div">
			<span class="title">General Notifications</span>
			<table class="display">
				<tr v-for="item in getGeneralArr">
					<th width="150">{{item.create_time}}</th>
					<td>
						<el-popover placement="bottom-start" trigger="click" width="800">
							<iframe :src="item.link" style="width: 100%; height: 700px; border: 0;"></iframe>
							<div slot="reference">
								<span class="content">{{item.enotify_title}}</span>
							</div>
						</el-popover>
					</td>
				</tr>
			</table>
			<tr v-if="!showMoreGeneralFlag && generalArr.length > 5">
				<th class="show-more" @click="showMore('general')">Show More</th>
			</tr>
			<tr v-if="showMoreGeneralFlag">
				<th class="show-more" @click="showMore('general')">Show Less</th>
			</tr>
		</div>
	</div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Provide, Watch } from 'vue-property-decorator';
import _ from 'lodash';
@Component({
	components: {
		
	}
})
export default class MetadataEnotify extends Vue {
	@Prop() data: any;
	lastestArr: any = [];
	maintenanceArr: any = [];
	productArr: any = [];
	generalArr: any = [];
	showMoreMaintenanceFlag: boolean = false;
	showMoreProductFlag: boolean = false;
	showMoreGeneralFlag: boolean = false;
	newFlag: boolean = false;

	mounted() {
		if (this.data && this.data.length > 0) {
			this.lastestArr = this.data[0];
			_.forEach(this.data, (v: any) => {
				if (_.toUpper(v.enotify_type) == "MAINTENANCE") this.maintenanceArr.push(v);
				else if (_.toUpper(v.enotify_type) == "PRODUCT") this.productArr.push(v);
				else if (_.toUpper(v.enotify_type) == "GENERAL") this.generalArr.push(v);
			})
		}
	}

	get getMaintenanceArr() {
		return (this.showMoreMaintenanceFlag ? this.maintenanceArr : _.slice(this.maintenanceArr, 0, 5)) || this.maintenanceArr;
	}

	get getProductArr() {
		return (this.showMoreProductFlag ? this.productArr : _.slice(this.productArr, 0, 5)) || this.productArr;
	}

	get getGeneralArr() {
		return (this.showMoreGeneralFlag ? this.generalArr : _.slice(this.generalArr, 0, 5)) || this.generalArr;
	}

	showMore(type: any) {
		if (type == "maintenance") this.showMoreMaintenanceFlag = !this.showMoreMaintenanceFlag;
		else if (type == "product") this.showMoreProductFlag = !this.showMoreProductFlag;
		else if (type == "general") this.showMoreGeneralFlag = !this.showMoreGeneralFlag;
	}
}
</script>
<style lang="scss" scoped>
@import '@/styles/global.scss';
.metadata-enotify {
	width: 100%;
	height: 100%;
	overflow-y: auto;
	background-color: #ffffff;
}
.lastest-div, .maintenance-div, .product-div, .general-div {
	margin-top: 10px;
	margin-bottom: 20px;
}
.title {
	font-weight: bold;
	font-size: 14px;
}
.new {
	color: #E53917;
	font-style: italic;
	font-size: 12px;
	padding-left: 5px;
}
.display {
	> tr {
		min-height: 30px;
		> th,
		> td {
			padding: 0;
		}
		> th {
			text-align: left;
			font-weight: normal;
			height: 30px;
		}
		> td {
			color: #333;
		}
	}
}
.show-more {
  color: #569ce1;
  cursor: pointer;
  line-height: normal;
  font-size: 13px;
  font-weight: 400;
  text-decoration: underline;
}
.content {
	cursor: pointer;
}
.content:hover {
	color: #569ce1;
}
</style>

