<template>
  <div class="sa-list-wrapper">
    <Header
      current-page="SA List"
      :show-back-button="true"
      :breadcrumb="[{ key: 'Metadata', label: 'Metadata'}, { key: 'SAList', label: 'SA List' }]"
      @breadcrumb-click="back"
    />
    <div class="btn-collection">
      <div class="collection-left"> 
        <h4> Subject Areas </h4>
      </div>
      <div class="collection-right"> 
        <el-button
          circle
          size="mini"
          icon="el-icon-document"
          style="min-width:unset;padding:7px;"
          @click="downloadExcel"
        />
        <el-button
          v-if="isDA"
          type="primary"
          icon="el-icon-circle-plus-outline"
          @click="createSA"
        >
          Create
        </el-button>
      </div>
    </div>
    <el-table
      :data="currentPageData"
      border
      height="calc(100vh - 160px)"
    >
      <el-table-column 
        v-for="type in cols"
        :key="type"
      >
        <template
          slot="header"
          slot-scope="scope"
        >
          <!-- Subject Area -->
          <template v-if="type==='sbjct_area'">
            <search-component
              v-if="type==='sbjct_area'"
              :title="colNames[type]"
              @call-back="(val) => filterChange(val, 'sbjct_area')"
            />
          </template>
          <template v-else-if="type === 'prmry_da' || type === 'prdct_owner' || type === 'dev_mngr' || type === 'dev' || type === 'bsa'">
            <filter-component
              :title="colNames[type]"
              :property="type"
              :options-data="getOptions(type)"
              @call-back="filterChange"
            />
          </template>
          <template v-else>
            {{ colNames[type] }}
          </template>
        </template>

        <template slot-scope="scope">
          <template v-if="type==='sbjct_area'">
            <span
              class="sa-name-cell"
              @click="goDetail(scope.row.sbjct_area)"
            >
              {{ scope.row.sbjct_area }}
            </span>
          </template>
          <template v-else-if="type==='table_cnt'">
            {{ scope.row.table_cnt }}
          </template>
          <template v-else>
            <div
              v-if="scope.row[`${type}_nt`]"
              @mouseenter="(evt) => showUserCard(evt, scope.row[`${type}_nt`])"
              @mouseleave="mouseLeave"
            >
              <i class="zeta-icon-user" />
              {{ scope.row[type] }}
            </div>
            <div v-else> 
              <div
                v-for="info in scope.row[`${type}`]"
                :key="info.nt"
                @mouseenter="(evt) => showUserCard(evt, info.nt)"
                @mouseleave="mouseLeave"
              >
                <i class="zeta-icon-user" />
                {{ info.name }}
              </div>
            </div>
          </template>
        </template>
      </el-table-column>
    </el-table>
    <div
      v-if="showCard"
      class="user-popover"
      :style="{left: hoveredUser.x + 'px', top: hoveredUser.y + 'px'}"
      @mouseenter="() => showCard += 1"
      @mouseleave="mouseLeave"
    >
      <user-card :nt="hoveredUser.nt" />
    </div>
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
import { Vue, Component } from 'vue-property-decorator';
import { namespace } from 'vuex-class';
import SearchComponent from '@/components/DA/search-component.vue';
import FilterComponent from '@/components/DA/filter-component.vue';
import UserCard from '@/components/common/UserCard.vue';
import { matchString } from '../utils';
import XLSX from 'xlsx';
import Util from '@/services/Util.service';
import { intersectionWith } from 'lodash';
import Header from '@/components/Metadata/index/components/Header.vue';

const SaStore = namespace('SAStore');

@Component({
  components: {
    SearchComponent,
    FilterComponent,
    UserCard,
    Header,
  },
})
export default class SAList extends Vue {
  @SaStore.Action('getSAList') getSAList;
  @SaStore.Action('getDAList') getDAList;
  @SaStore.State('saList') saList;
  @SaStore.State('daList') daList;

  pageSize = 20;
  currentPage = 0;
  showCard = 0;
  filters = {
    sbjct_area: '',
    prdct_owner: [] as string[],
    dev_mngr: [] as string[],
    prmry_da: [] as string[],
    dev: [] as string[],
    bsa: [] as string[],
  };
  hoveredUser = {
    nt: '',
    x: 0,
    y: 0,
  };

  colNames = {
    sbjct_area: 'Subject Area',
    prdct_owner: 'Product Owner',
    dev_mngr: 'Dev Manager',
    dev: 'DEV SAE',
    bsa: 'BSA SAE',
    prmry_da: 'DA',
    table_cnt: 'Table Count',
  };
  cols = Object.keys(this.colNames);

  created () {
    this.getSAList();
    this.getDAList();
  }

  get isDA () {
    const nt = Util.getNt();
    return !!this.daList.find(i => i.nt === nt);
  }

  get filteredData () {
    const { filters: { sbjct_area, prdct_owner, dev_mngr, prmry_da, dev, bsa } } = this;
    return this.saList.filter(sa => {
      const saFilter = () => matchString(sbjct_area, sa.sbjct_area);
      const po = () => prdct_owner.length === 0 || prdct_owner.indexOf(sa.prdct_owner) > -1;
      const dm = () => dev_mngr.length === 0 || dev_mngr.indexOf(sa.dev_mngr) > -1;
      const da = () => prmry_da.length === 0 || prmry_da.indexOf(sa.prmry_da) > -1;
      const de = () => dev.length === 0 || intersectionWith(dev, sa.dev, (a, b: any) => a === b.name).length > 0;
      const bs = () => bsa.length === 0 || intersectionWith(bsa, sa.bsa, (a, b: any) => a === b.name).length > 0;
      return saFilter() && po() && dm() && da() && de() && bs();
    }).sort((a, b) => b.table_cnt - a.table_cnt);
  }

  get currentPageData () {
    const { pageSize, currentPage } = this;
    const start = pageSize * currentPage;
    return this.filteredData.slice(start, start + pageSize);
  }

  getOptions (type: string) {
    const opts = new Set();
    this.saList.forEach(sa => {
      const vals = type === 'dev' || type === 'bsa' ? (sa[type] || []).map(i => i.name) : [sa[type]];
      vals.forEach(str => {
        opts.add(str);
      });
    });
    return Array.from(opts).filter(Boolean);
  }

  showUserCard (evt, nt) {
    this.showCard += 1;
    const bound = evt.target.getBoundingClientRect();
    Object.assign(this.hoveredUser, {
      nt,
      x: bound.x - 25,
      y: bound.y + 30,
    });
  }

  mouseLeave () {
    setTimeout(() => {
      this.showCard -= 1;
    }, 300);
  }

  onSizeChange (size) {
    this.pageSize = size;
    this.currentPage = 0;
  }

  filterChange (checkedValues = [], property) {
    this.currentPage = 0;
    this.filters[property] = checkedValues;
  }

  downloadExcel () {
    const transformed = this.saList.map(sa => {
      return {
        'Subject Area': sa.sbjct_area,
        Description: sa.sa_desc,
        'Dev Manager': sa.dev_mngr,
        'Product Owner': sa.prdct_owner,
        'Data Architect': sa.prmry_da,
        'Data Owner': sa.data_owner,
        'BSA SAE': (sa.bsa || []).map(i => `${i.name}(${i.nt})`).join(';'),
        'DEV SAE': (sa.dev || []).map(i => `${i.name}(${i.nt})`).join(';'),
        'Table Count': sa.table_cnt,
        Domain: sa.domain,
        'Sub Domain': sa.sub_domain,
        'SA Code': sa.sa_code,
        'Batch Account': sa.batch_acct,
        'Target Database': sa.target_db,
        'Working Database': sa.target_working_db,
      };
    });
    const ws: any = XLSX.utils.json_to_sheet(transformed);
    const wb: any = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, 'Subject Areas');
    XLSX.writeFile(wb, 'subject_areas.xlsx');
  }

  createSA () {
    this.$router.push('/metadata/sa/new');
  }
  back () {
    this.$router.push('/metadata');
  }
  goDetail (name) {
    this.$router.push(`/metadata/sa/${name}`);
  }
}
</script>
<style lang="scss" scoped>
@import '@/styles/metadata.scss';

.sa-list-wrapper {
  padding: 8px 16px 0;
  .btn-collection {
    display: flex;
    justify-content: space-between;
    padding: 15px 0;
    h4 {    
        display: inline-block;
        padding: 3px;
    }
  }
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

  .sa-name-cell {
    color: #4d8cca;
    text-transform: uppercase;
    cursor: pointer;
    &:hover {
      color: darken(#4d8cca, 20);
    }
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
  .user-popover {
    position: fixed;
    z-index: 2000;
    background: white;
    border: 1px solid #999;
    padding: 1em 1.5em;
    border-radius: 8px;
    min-width: 200px;
    &::before {
      content: '';
      display: block;
      height: 12px;
      width: 12px;
      transform: rotate(-45deg);
      border: 1px solid #999;
      border-left: transparent;
      border-bottom: transparent;
      position: absolute;
      top: -7px;
      left: 80px;
      background: white;
    }
  }
}
</style>
