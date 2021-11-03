<template>
  <div class="udf-browse-wrapper">
    <el-breadcrumb separator-class="el-icon-arrow-right">
      <el-breadcrumb-item>
        <span @click="back">Metadata Browse</span>
      </el-breadcrumb-item>
      <el-breadcrumb-item>UDF List</el-breadcrumb-item>
    </el-breadcrumb>

    <el-table
      :data="currentPageData"
      border
      height="calc(100vh - 160px)"
    >
      <el-table-column
        property="name"
        width="300"
      >
        <template
          slot="header"
          slot-scope="scope"
        >
          <search-component
            title="Name"
            @call-back="onNameFilterChange"
          />
        </template>
        <template slot-scope="scope">
          <span
            class="udf-name-cell"
            @click="goDetail(scope.row)"
          >{{ scope.row.name }}</span>
        </template>
      </el-table-column>
      <el-table-column
        property="db_name"
        width="120"
      >
        <template
          slot="header"
          slot-scope="scope"
        >
          <filter-component
            title="Database"
            property="database"
            :options-data="dbOptions"
            @change="(val) => dbStrFilter = val.toLowerCase()"
            @call-back="onFilterChange"
          />
        </template>
      </el-table-column>
      <el-table-column
        property="platform"
        label="Platform"
        width="300"
      >
        <template
          slot="header"
          slot-scope="scope"
        >
          <filter-component
            title="Platform"
            property="platform"
            :options-data="platformOptions"
            @change="(val) => platformStrFilter = val"
            @call-back="onFilterChange"
          />
        </template>
        <template slot-scope="scope">
          <i
            v-for="p in scope.row.platform.split(',')"
            
            :key="p"
            :title="p"
            :class="p.toLowerCase()"
            class="platform-icon"
          />
        </template>
      </el-table-column>
      <el-table-column
        property="description"
        label="Description"
      >
        <template slot-scope="scope">
          <span class="desc">{{ stripHtmlTags(scope.row.description) }}</span>
        </template>
      </el-table-column>
    </el-table>
    <footer>
      <span>Total 
        <strong>{{ filteredData.length }}</strong>
      </span>
      <el-pagination
        :page-size="pageSize"
        :current-page="currentPage + 1"
        layout="sizes, prev, pager, next"
        :total="filteredData.length"
        :page-sizes="[20, 50, 100]"
        @size-change="onSizeChange"
        @current-change="(val) => currentPage = val - 1"
      />
    </footer>
  </div>
</template>

<script lang="ts">
import { Vue, Component, Provide } from 'vue-property-decorator';
import _ from 'lodash';
import DoeRemoteService from '@/services/remote/DoeRemoteService';
import SearchComponent from '@/components/DA/search-component.vue';
import FilterComponent from '@/components/DA/filter-component.vue';
import { stripHtmlTags } from '@drewxiu/utils';
import { ZetaExceptionProps } from '@/types/exception';
import { matchString } from '../utils';

@Component({
  components: {
    SearchComponent,
    FilterComponent,
  },
})
export default class UDFBrowse extends Vue {

  doeRemoteService: DoeRemoteService = new DoeRemoteService();

  stripHtmlTags = stripHtmlTags;
  pageSize = 20;
  currentPage = 0;
  nameFilter = '';
  dbStrFilter = '';
  dbCheckedFilter = [];
  platformStrFilter = '';
  platformCheckedFilter = [];

  async mounted () {
    this.$store.dispatch('getUdfList');
    const props: ZetaExceptionProps = {
      path: 'metadata',
    };
    this.doeRemoteService.props(props);
  }

  get udfList () {
    return this.$store.state.Metadata.udfList;
  }

  get dbOptions () {
    const db = new Set();
    this.udfList.data.forEach(i => db.add(i.db_name));
    return Array.from(db);
  }

  get platformOptions () {
    const platforms = new Set();
    this.udfList.data.forEach(i => i.platform.split(',').forEach(p => platforms.add(_.capitalize(p))));
    return Array.from(platforms);
  }

  get filteredData () {
    const { nameFilter, dbStrFilter, dbCheckedFilter, platformStrFilter, platformCheckedFilter, udfList } = this;
    return udfList.data.filter(d => {
      const name = matchString(nameFilter, d.name);
      const dbFilters = [dbStrFilter, ...dbCheckedFilter].filter(Boolean);
      const db = matchString(dbFilters, d.db_name);
      const platformFilter = [platformStrFilter, ...platformCheckedFilter].filter(Boolean);
      const platform = matchString(platformFilter, d.platform);
      return name && db && platform;
    });
  }

  get currentPageData () {
    const { pageSize, currentPage } = this;
    const start = pageSize * currentPage;
    return this.filteredData.slice(start, start + pageSize);
  }

  onNameFilterChange (val) {
    this.currentPage = 0;
    this.nameFilter = val;
  }

  onSizeChange (size) {
    this.pageSize = size;
    this.currentPage = 0;
  }

  onFilterChange (checkedValues = [], property) {
    this.currentPage = 0;
    switch (property) {
      case 'platform':
        this.platformCheckedFilter = checkedValues;
        break;
      case 'database':
        this.dbCheckedFilter = checkedValues;
        break;
    }
  }

  back () {
    this.$router.push('/metadata');
  }

  goDetail (row) {
    this.$router.push(`/metadata/udf/${row.db_name}.${row.name}`);
  }
}
</script>
<style lang="scss" scoped>
@import '@/styles/metadata.scss';

.udf-browse-wrapper {
  padding: 32px 16px 0;
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
  .el-table {
    margin-top: 32px;
  }
  
  .udf-name-cell {
    color: #4d8cca;
    text-transform: uppercase;
    cursor: pointer;
    &:hover {
      color: darken(#4d8cca, 20);
    }
  }

  .desc {
    display: -webkit-box;
    overflow: hidden;
    -webkit-line-clamp: 3;
    -webkit-box-orient: vertical;
  }

  footer {
    text-align: right;
    > span {
      display: inline-block;
      transform: translateY(40%);
    }
    .el-pagination {
      display: inline-block;
      margin: 24px 0;
    }
  }
}
</style>
