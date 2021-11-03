<template>
	<div class="zeta-el-table-wrapper">
		<div class="zeta-el-table-wrapper-header">
			<div class="page-size">
				Page Size: 
				<el-select v-model="pageSize" placeholder="Page Size">
					<el-option
					v-for="item in pageOption"
					:key="item"
					:label="item"
					:value="item">
					</el-option>
				</el-select>
			</div>
			<div class="search-box" >
				<el-input v-model="search" placeholder="Search..."/>
			</div>
		</div>
		<div class="zeta-el-table-wrapper-table">
			<el-table v-if="tableData" :data="displayData" @sort-change="onSortChange">
				<template v-for="(cfg,col) in cfgs">
					<el-table-column 
						v-if="cfg.prop && !cfg.render"
						:class="cfg.className ? cfg.className: ''"
						:label="cfg.label" 
						:prop="cfg.prop" 
						sortable
						:key="col">
					</el-table-column>
					<el-table-column 
						v-else
						:class="cfg.className ? cfg.className: ''"
						:label="cfg.label" 
						sortable
						:sort-by="cfg.prop"
						:key="col">
						<template slot-scope="scope"
							v-html="cfg.render(scope)">
						</template>
					</el-table-column>
				</template>
			</el-table>
		</div>
		<div class="zeta-el-table-wrapper-footer">

			<el-pagination
				layout="prev, pager, next"
				:current-page.sync="currentPage"
				:page-size="pageSize"
				:total="tableData.length">
			</el-pagination>
		</div>
	</div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch } from 'vue-property-decorator';
import _ from 'lodash';
import { ColumnConfig } from './index';

@Component({
  components: {}
})
export default class ElTableWrapper extends Vue {
  //   @Prop() tableData: Dict<any>[];
  //   @Prop() cfgs: Dict<ColumnConfig>;
  tableData: Dict<any>[] = [
    {
      id: '12987122',
      name: '王小虎',
      amount1: '234',
      amount2: '3.2',
      amount3: 10
    },
    {
      id: '12987123',
      name: '王小虎',
      amount1: '165',
      amount2: '4.43',
      amount3: 12
    },
    {
      id: '12987124',
      name: '王小虎',
      amount1: '324',
      amount2: '1.9',
      amount3: 9
    },
    {
      id: '12987125',
      name: '王小虎',
      amount1: '621',
      amount2: '2.2',
      amount3: 17
    },
    {
      id: '12987126',
      name: '王小虎',
      amount1: '539',
      amount2: '4.1',
      amount3: 15
    },
    {
      id: '12987125',
      name: '王小虎',
      amount1: '621',
      amount2: '2.2',
      amount3: 17
    },
    {
      id: '12987126',
      name: '王小虎',
      amount1: '539',
      amount2: '4.1',
      amount3: 15
    }
  ];
  cfgs: Dict<ColumnConfig> = {
    id: {
      label: 'id',
      prop: 'id'
    },
    name: {
      label: 'name',
      prop: 'name'
    },
    amount1: {
      label: 'amount1',
      prop: 'amount1'
    },
    amount2: {
      label: 'amount2',
      prop: 'amount3'
    },
    amount3: {
      label: 'amount4',
      prop: 'amount3'
    }
  };

  search: string = '';
  currentPage = 1;
  pageOption = [5, 10, 20];
  pageSize = 5;
  sort: any = {};
  constructor() {
    super();
    this.pageSize = 5;
  }
  onSortChange(arg: any) {
    this.sort = arg;
  }
  get displayData() {
    let data: Dict<any>[] = this.tableData;
    // filter by search
    if (this.search) {
      data = _.chain(this.tableData)
        .filter((row: Dict<any>, index) => {
          const rows = _.chain(row)
            .filter(val => {
              const content = val + '';
              return content.includes(this.search);
            })
            .value();
          return rows && rows.length > 0;
        })
        .value();
    }
    // sort
    if (!_.isEmpty(this.sort)) {
      const column = this.sort.column;
      const descending = Boolean(this.sort.order === 'descending');
      data = _.chain(data)
        .sortBy(column)
        .value();
      data = descending ? data.reverse() : data;
    }
    const dataSlice = data.slice(
      (this.currentPage - 1) * this.pageSize,
      this.currentPage * this.pageSize
    );
    return dataSlice;
  }
}
</script>
<style lang="scss" scoped>
.zeta-el-table-wrapper-header{
  display:flex;
  justify-content: space-between;
  
}
</style>
