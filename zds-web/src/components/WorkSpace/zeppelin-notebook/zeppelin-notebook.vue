<template>
  <div class="zeppelin-notebook workspace-container" v-if="initialShowFlag">
    <UsedCapacity
      ref="usage"
      :show="show"
      @percentage-change="percentageChange"
    />
    <Configuration
      ref="configDialog"
      :notebook-id="workspaceId"
      @apply-config="applyConfig"
    />
    <ApplyPackageWrapper
      ref="package"
      :notebook-id="workspaceId"
    />
    <ShareLinkDialog
      ref="shareDialog"
      v-model="shareUrl"
      title="Share Notebook"
    />
    <iframe
      v-if="zeppelinUrl"
      ref="zeppelinIFrame"
      :src="zeppelinUrl"
      style="height: 100%;width:100%;border: 0;"
    />
  </div>
</template>

<script lang="ts">
import { Component, Prop, Provide, Ref, Watch } from 'vue-property-decorator';
import { WorkspaceComponentBase, INotebook } from '@/types/workspace';
import config from '@/config/config';
import ZeppelinApi from '@/services/remote/ZeppelinApi';
import Configuration from './tools/configuration.vue';
import ApplyPackageWrapper from './tools/apply-package/apply-package-wrapper.vue';
import ShareLinkDialog from '@/components/common/share-link-dialog';
import UsedCapacity  from './tools/UsedCapacity.vue';
import Util from '@/services/Util.service';

@Component({
  components: {
    Configuration,
    ApplyPackageWrapper,
    ShareLinkDialog,
    UsedCapacity,
  }
})
export default class ZeppelinNotebook extends WorkspaceComponentBase{

  @Provide('zeppelinApi')
  zeppelinApi = new ZeppelinApi();

  @Prop() data: INotebook;

  @Ref()
  usage: UsedCapacity;

  @Ref()
  configDialog: Configuration;

  @Ref()
  package: ApplyPackageWrapper;

  @Ref()
  shareDialog: ShareLinkDialog;

  @Ref()
  zeppelinIFrame: HTMLIFrameElement;

  @Prop()
  show: boolean;

  initialShowFlag = false;

  created () {
	  if (this.$route.path == 'notebook/multilang') {
		  this.initialShowFlag = true;
	  }
	  if (!this.show) {
		  this.initialShowFlag = false;
	  } else {
		  this.initialShowFlag = true;
	  }
  }

  get zeppelinUrl () {
    const ts = new Date().getTime();
    return `${config.zeta.zeppelin.domain}?zetaIframe&t=${ts}&nt_login=${Util.getNt()}#/notebook/${this.workspaceId}`;
  }

  get shareUrl () {
    return `${location.protocol}//${location.host}/${Util.getPath()}share/#/notebook/multilang?notebookId=${this.workspaceId}`;
  }
  get noteName () {
    return this.data.name;
  }
  mounted () {
    // this.wsClinet = new ZeppelinNoteWebSocketClient('wss://zeta-tools.staging.corp.ebay.com/zeppelin/ws',
    //   this.workspaceId,
    //   'bab310fc-633f-455f-9c75-726decca603a',
    //   'tianrsun',
    // );
    // this.wsClinet.$on('CONNECTION_SUCCESS',() => {
    //   this.$message.success(` \`${this.noteName}\` connect succeed!`);
    // }).$on('DISCONNECTION_SUCCESS',() => {
    //   this.$message.success(` \`${this.noteName}\` disconnect succeed!`);
    // }).$on('CONNECTION_ABORT',() => {
    //   this.$message.success(` \`${this.noteName}\` disconnect succeed!`);
    // }).$on('CONNECTION_ERROR',(msg: any) => {
    //   console.warn(`${this.noteName} connect error`, msg);
    //   const ex = new ZetaException({ code: '', errorDetail: { message: 'Connect error' } }).props({
    //     path: 'notebook',
    //     workspaceId: this.workspaceId,
    //   });
	  //   this.$store.dispatch('addException', {exception: ex});
    // }).$on('DISCONNECTION_ERROR',(msg: any) => {
    //   console.warn(`${this.noteName} connect error`, msg);
    //   const ex = new ZetaException({ code: '', errorDetail: { message: 'Disconnect error' } }).props({
    //     path: 'notebook',
    //     workspaceId: this.workspaceId,
    //   });
	  //   this.$store.dispatch('addException', {exception: ex});
    // });
    // // CONNECTION_ERROR
    // this.wsClinet.open();
	  this.zeppelinApi.props({
	    path: 'notebook',
	    workspaceId: this.workspaceId
	  });
  }
  percentageChange(percentage: number){
    const packet = {
      action: 'ZETA_PERCENTAGE_CHANGE',
      params: { percentage }
    };
    this.postMessage(packet);
  }
  applyConfig(config: any){
    const packet = {
      action: 'ZETA_APPLY_CONFIG',
      params: { config }
    };
    this.postMessage(packet);
  }
  @Watch('show')
  isNoteBookShow (val: boolean) {
	  if (val) {
		 this.initialShowFlag = true;
		 const packet = {
			 action: 'ZETA_RESIZE_EDITOR'
		 };
		 this.postMessage(packet);
	  }
  }
  usedCapacity(params) {
    this.usage.setParams(params);
  }
  openUsedCapacityDialog(){
    this.usage.open();
  }
  configuration(params) {
    this.configDialog.open(params);
  }
  uploadFile() {
	  this.package.open();
  }
  openShareUrl () {
    this.shareDialog.open();
  }
  postMessage (packet: any) {
    this.$nextTick(() => {
      const subWindow = this.zeppelinIFrame.contentWindow;
      if (subWindow) {
        subWindow.postMessage(packet, '*');
      }
    });
  }
}
</script>

<style lang="scss" scoped>
@import '@/styles/global.scss';
$bc-height: $workspace-tab-height + $workspace-tab-margin-bottom;
.workspace-container {
  border: 1px solid #dcdfe6;
  -webkit-box-shadow: 0 2px 4px 0 rgba(0, 0, 0, 0.12), 0 0 6px 0 rgba(0, 0, 0, 0.04);
  box-shadow: 0 2px 4px 0 rgba(0, 0, 0, 0.12), 0 0 6px 0 rgba(0, 0, 0, 0.04);
}
.metadata {
	box-sizing: border-box;
	font-style: normal;
	height: 100%;
	overflow: hidden;
	padding: 0 25px;
	width: 100%;
	> div {
		height: 100%;
	}
}
.title {
	color: #1E1E1E;
	display: block;
	font-size: 18px;
	font-weight: 700;
	height: 36px;
	> span {
		margin-right: 5px;
	}
	/deep/ .zeta-icon-share {
		color: #cacbcf;
	}
	/deep/ .zeta-icon-share:hover {
		color: #569ce1;
	}
}
.metadata-tools-bar {
	align-items: center;
	display: flex;
	height: 40px;
	justify-content: space-between;
	ul.metadata-tabs {
		display: flex;
		height: 100%;
		list-style-type: none;
		width: 334px;
		> li {
			align-items: center;
			border-bottom: 1px solid #cacbcf;
			color:#999;
			cursor: pointer;
			display: flex;
			font-size: 14px;
			padding: 0 20px;
			&.active {
				border-bottom: 1px solid #569ce1;
				background-color: #fff;
				color:$zeta-global-color;
			}
			&:hover{
				color:#569CE1;
			}
		}
	}
}
.metadata-display {
	display: block;
	height: calc(100% - #{$bc-height} - 40px);
	> div {
		height: 100%;
	}
	.overview {
		overflow-y: auto;
	}
	.overview::-webkit-scrollbar {
		width: 0;
	}
	.columns {
		padding: 10px 0;
		div.el-table{
			/deep/ div.el-table__body-wrapper{
				tr > td > .cell{
					word-break: break-word;
					white-space: pre-line;
				}
			}
		}
	}
	.sample {
		padding: 10px 0;
	}
}
.btn-group {
	border-bottom: 1px solid #cacbcf;
	display: inline-block;
	height: 39px;
	padding: 0 10px;
	text-align: right;
	width: calc(100% - 354px);
}
.comment-icon-div {
	bottom: 20px;
    position: fixed;
	right: 20px;
	width: 73px;
	/deep/ .el-badge__content {
		top: 10px;
		right: 35px;
	}
	.el-button {
		border-color: #fff;
		box-shadow: 0 0 10px #cacbcf;
		color: #569ce1;
		min-width: 0;
	}
	.el-button:hover {
		background-color: #fff;
		color: #4d8cca;
	}
	/deep/ [class^="zeta-icon-"], [class*=" zeta-icon-"] {
		font-size: 25px !important;
	}
}
.cancel-btn {
  background: inherit;
  background-color: #fff;
  border: 1px solid #569ce1;
  border-radius: 4px;
  box-shadow: none;
  color: #569ce1;
}
</style>
