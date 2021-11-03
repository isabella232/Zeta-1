<template>
	<div class="dag-popup">
		<!-- <el-popover
			placement="right"
			width="400"
			trigger="click">
			<DAGContainer ref="dag" :dagSrc="dagSrc" />
			<el-button slot="reference">Show Dag</el-button>
		</el-popover> -->
		<el-button type="primary" size="small" @click="dialogShow = true" >Execution Plan</el-button>

		<el-dialog title="" class="dag-popup-content" :visible.sync="dialogShow" :show-close="false">
			<DagRender ref="dag" :domSrc="dagSrc" :loading="loading" />
		</el-dialog>
	</div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch } from 'vue-property-decorator';
import DagRender from '@/components/common/dag-render-v2/'
@Component({
  components: {
	  DagRender
  },
})
export default class DAGPopup extends Vue {
	@Prop()
	value!:string
	@Prop()
	loading: boolean
	dialogShow = false;

	get dagSrc(){
		return this.value
	}

	get $DAGContainer():DagRender{
		return this.$refs.dag as DagRender
	}

	mounted(){
	}
	destroyed() {

	}

}
</script>
<style lang="scss" scoped>
.dag-popup{
	margin-top: 15px;
	// height: 100%;
	// position: relative;
}
	/deep/ .el-dialog {
		height: 80%;
		margin: 0 auto auto;
		.el-dialog__header {
			padding: 0;
		}
		.el-dialog__body {
			padding: 35px 20px;
			height: calc(100% - 70px);
		}
	}
</style>

