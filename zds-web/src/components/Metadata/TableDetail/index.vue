<template>
  <div class="table-detail">
    <Header
      :show-back-button="true"
      :breadcrumb="[{ key: 'Metadata', label: 'Metadata' }, { key: 'tableName', label: tableName }]"
      @breadcrumb-click="backTo"
    />
    <div
      ref="scrollParent"
      class="scroll-content"
      @scroll="debouncedSync"
    >
      <div class="table-of-contents-wrapper">
        <table-of-contents
          :contents="contents"
          :actived="actived"
          @select="onAnchorChange"
        />
      </div>
      <headline
        :name="tableName"
        :rate="rate"
        @edit="() => showEditDialog = true"
      />
      <section>
        <overview />
      </section>
      <section>
        <platform />
      </section>
      <section>
        <operation />
      </section>
      <section>
        <usage />
      </section>
    </div>
    <edit-table-dialog
      :show.sync="showEditDialog"
      :table="tableName"
    />
  </div>
</template>

<script lang="ts">
import { Component, Mixins } from 'vue-property-decorator';
import { Action } from 'vuex-class';
import TableDetailModule, { Actions } from './store';
import TableOfContents, { Contents } from './TableOfContents.vue';
import Headline from './Headline.vue';
import Overview from './Overview.vue';
import Platform from './Platform/index.vue';
import Operation from './Operation.vue';
import Usage from './Usage.vue';
import EditTableDialog from './EditTableDialog/index.vue';
import { Common } from './mixins/Common';
import DoeRemoteService from '@/services/remote/DoeRemoteService';
import { debounce, get } from 'lodash';
import { blink } from './helper';
import { flattenDeepBy } from '@drewxiu/utils';
import Header from '@/components/Metadata/index/components/Header.vue';

const API = DoeRemoteService.instance;

@Component({
  components: {
    TableOfContents,
    Headline,
    Overview,
    Platform,
    Operation,
    Usage,
    EditTableDialog,
    Header,
  },
})
export default class TableDetail extends Mixins(Common) {
  @Action(Actions.GetTableInfo) getTableInfo;
  contents: Contents = [
    {
      id: 'overview',
      label: 'Overview',
    },
    {
      id: 'platform',
      label: 'Platform',
      children: [
        {
          id: 'summary',
          label: 'Summary',
        },
        {
          id: 'columns',
          label: 'Columns',
        },
        {
          id: 'sample',
          label: 'Sample Queries',
        },
      ],
    },
    {
      id: 'operation',
      label: 'Operation Info',
      children: [
        {
          id: 'general',
          label: 'General Info',
        },
        {
          id: 'job',
          label: 'Job Details',
        },
      ],
    },
    {
      id: 'usage',
      label: 'Usage Persona',
    },
  ];
  actived = this.contents[0].id;
  rate = 0;
  showEditDialog = false;

  get contentIdList () {
    return flattenDeepBy([...this.contents], c => c.children).map(i => i.id);
  }

  get scrollParent () {
    return this.$refs.scrollParent as Element;
  }
  onAnchorChange (id, isSub = false) {
    this.actived = id;
    const element = document.getElementById(id)!;
    const stickyOffset = 80 + (isSub ? 40 : 0);
    this.scrollParent.scrollTo({ left: 0, top: element.offsetTop - stickyOffset, behavior: 'smooth' });
    blink(element);
  }
  backTo () {
    this.$router.push('/metadata');
  }
  created () {
    if (!this.$store.state[TableDetailModule.namespace]) {
      this.$store.registerModule(TableDetailModule.namespace, TableDetailModule);
    }
    this.getTableInfo({ table: this.tableName });
    API.getConfidenceScore(this.tableName).then((res) => this.rate = get(res, 'data.data.value[0].score', 0));
  }
  syncTableOfContent () {
    const firstVisibleEle = this.contentIdList.find(id => {
      const ele = document.getElementById(id);
      return !!ele && ele.offsetTop > this.scrollParent.scrollTop;
    });
    if (firstVisibleEle) {
      this.actived = firstVisibleEle;
    }
  }
  debouncedSync = debounce(this.syncTableOfContent, 100);
}
</script>
<style lang="scss" scoped>
@import '@/styles/metadata.scss';
.table-detail {
  padding: 8px;
  .scroll-content {
    height: calc(100vh - 70px);
    overflow: scroll;
    position: relative;
    .table-of-contents-wrapper {
      position: fixed;
      right: 120px;
      top: 135px;
    }
  }
  section {
    max-width: calc(100% - 320px);
    ::v-deep .sticky-header {
      position: sticky;
      top: 75px;
      font-weight: bold;
      font-size: 17px;
      background: #F5F5F5;
      padding: 8px 12px;
      margin-bottom: 12px;
      z-index: 9;
    }
  }
}
</style>
