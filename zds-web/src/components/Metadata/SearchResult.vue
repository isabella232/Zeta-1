<template>
  <div class="search-result">
    <div class="content-div">
      <div
        v-if="dataArr.length == 0 && !loading"
        class="no-result-div"
      >
        No Result
      </div>
      <div
        v-for="(item, $i) in dataArr"
        :key="$i"
        class="row-div"
      >
        <table-item
          v-if="item.type === 'table'"
          :data="item"
          :pos="getPos(item, $i)"
        />
        <tableau
          v-if="item.type === 'tableau'"
          :data="item"
          :pos="getPos(item, $i)"
        />
        <udf
          v-if="item.type === 'udf'"
          :data="item"
          :pos="getPos(item, $i)"
        />
        <notebook
          v-if="item.type === 'query'"
          :data="item"
          :extra="notebookExtra[item.parsedContent._id]"
          :pos="getPos(item, $i)"
        />
      </div>
      <div
        v-show="data.length > 0"
        class="pagination-div"
      >
        <el-pagination
          layout="prev, pager, next"
          :current-page.sync="currentPage"
          :page-size="pageSize"
          :total="total"
          @current-change="onCurrentChange"
        />
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Inject, Watch } from 'vue-property-decorator';
import Notebook from './components/SearchResult/Notebook.vue';
import Table from './components/SearchResult/Table.vue';
import Tableau from './components/SearchResult/Tableau.vue';
import Udf from './components/SearchResult/UDF.vue';
import DoeRemoteService from '@/services/remote/DoeRemoteService';
import { attempt, stripHtmlTags } from '@drewxiu/utils';
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';

@Component({
  components: {
    TableItem: Table,
    Tableau,
    Udf,
    Notebook,
  },
})
export default class MetadataSearchResult extends Vue {
  @Inject() doeRemoteService: DoeRemoteService;
  notebookService: NotebookRemoteService = new NotebookRemoteService();

  @Prop() data: any;
  @Prop() total: any;
  @Prop() offset: any;
  @Prop() loading: any;
  currentPage = 1;
  pageSize = 10;
  notebookExtra = {};

  constructor () {
    super();
    this.currentPage = this.offset / this.pageSize + 1;
  }

  get dataArr () {
    const rs = this.data.map((v: any) => {
      const parsedContent = attempt(JSON.parse.bind(null, v.content), {});
      const item = {
        ...v,
        parsedContent,
        desc: attempt(() => stripHtmlTags(parsedContent._source.desc || parsedContent._source.description), ''),
        example: attempt(() => stripHtmlTags(v.parsedContent._source.example)),
      };
      return item;
    });
    return rs;
  }

  get notebooks () {
    return this.dataArr.filter(i => i.type === 'query');
  }

  getPos (item: any, index: any) {
    return {
      currentPage: this.currentPage,
      pageSize: this.pageSize,
      index: index,
      target_name: item.name,
      target_type: item.type,
    };
  }

  @Watch('notebooks')
  getExtra () {
    if (this.notebooks.length === 0) return;
    this.notebookService.getSummaryByIds(this.notebooks.map(i => i.parsedContent._id).join())
      .then(({ data }) => {
        this.notebookExtra = data.reduce((prev, next) => {
          prev[next.id] = next;
          return prev;
        }, {});
      });
  }

  onCurrentChange () {
    this.$emit('search', false, (this.currentPage - 1) * this.pageSize);
  }
}
</script>
<style lang="scss" scoped>
@import '@/styles/metadata.scss';
.search-result {
  width: 100%;
  height: 100%;
}
.row-div {
  display: flex;
  flex-direction: column;
  padding: 0 20px 40px 20px;
}

.content-div {
  height: 100%;
  overflow-y: auto;
}
.content-div::-webkit-scrollbar {
  width: 0;
}
.pagination-div {
  display: flex;
  justify-content: center;
}
/deep/ .content-name {
  color: #569ce1;
  font-size: 14px;
  cursor: pointer;
  margin-right: 5px;
  padding: 2px 0;
  display: inline-block;
  text-transform: uppercase;
}
/deep/ .content-name:hover {
  color: #4d8cca;
  text-decoration: underline;
}
/deep/ .content-desc {
  margin-bottom: 5px;
  color: #cacbcf;
  overflow: hidden;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  display: -webkit-box;
}

.no-result-div {
  text-align: center;
}
</style>
