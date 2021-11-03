<template>
  <div class="my-collection">
    <Header
      :show-back-button="true"
      :breadcrumb="[{ key: 'Metadata', label: 'Metadata' }, {key: 'Collection', label: 'Collection'}]"
      @breadcrumb-click="back"
    />
    <el-tabs
      v-model="activeTab"
      class="tabs-css"
    >
      <el-tab-pane name="status">
        <span slot="label">
          <span>Table Status</span>
          <ShareLink
            v-model="shareUrl"
            style="position: relative; top: 1px; margin-left: 5px;"
          />
        </span>
        <table-status
          :collection-data="collectionData"
          @jump-collection="getCollectionList"
        />
      </el-tab-pane>
      <el-tab-pane
        label="Customized Table Collection"
        name="collection"
      >
        <my-collection
          :collection-data="collectionData"
          @query-collection="getCollectionList"
        />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script lang="ts">
/* eslint-disable @typescript-eslint/camelcase */
import { Vue, Component, Provide } from 'vue-property-decorator';
import DoeRemoteService from '@/services/remote/DoeRemoteService';
import TableStatus from './TableStatus.vue';
import MyCollection from './MyCollection.vue';
import ShareLink from '@/components/common/share-link';
import Util from '@/services/Util.service';
import Header from '@/components/Metadata/index/components/Header.vue';

@Component({
  components: {
    TableStatus,
    ShareLink,
    MyCollection,
    Header,
  },
})
export default class TeamBrowse extends Vue {
  doeRemoteService: DoeRemoteService = DoeRemoteService.instance;
  collectionData: any = [];

  activeTab: 'status' | 'collection' = 'status';

  mounted () {
	  this.getCollectionList();
  }

  get shareUrl () {
	  return (
	    `${location.protocol}//${location.host}/${Util.getPath()}share/#/metadata/collection?nt=${Util.getNt()}`
	  );
  }

  getCollectionList () {
	  this.collectionData = [];
	  this.doeRemoteService.getMyCollectionList().then(res => {
	    if (res && res.data) {
	      this.collectionData = res.data;
	    }
	  });
  }
  back () {
    this.$router.push('/metadata');
  }

  @Provide('jumpCollectionTab')
  jumpCollectionTab () {
	  this.activeTab = 'collection';
  }
}
</script>
<style lang="scss" scoped>
@import "@/styles/metadata.scss";

.my-collection {
	padding: 8px 16px ;
  height: 100%;
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
</style>
