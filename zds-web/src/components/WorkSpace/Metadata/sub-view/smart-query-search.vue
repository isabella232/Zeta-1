<template>
	<div class="smart-query">
        <span class="smart-query-title">Most Popular Join</span>
        <i class="el-icon-question" title="Search or find the table you want to join. Zeta will auto generate the sample query for you."></i>
        <el-input placeholder="Search the table you want to join..." v-model="searchContent" class="search-input" clearable><i slot="prefix" class="el-input__icon el-icon-search"></i></el-input>
        <div class="search-items-div">
            <span class="search-item" @click="openSearch(item)" v-for="(item, index) in filterTables" :key="index">{{ item.join_table.toUpperCase() }}</span>
        </div>
        <SmartQueryDialog :visible.sync="smartQueryDialogVisible" :joinTable="selectItem" :mainTable="mainTable" :smartQueryTableArr="smartQueryTableArr"/>
	</div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch } from 'vue-property-decorator';
import SmartQueryDialog from './smart-query-dialog.vue';
import _ from 'lodash';
@Component({
  components: {
    SmartQueryDialog
  }
})
export default class SmartQuerySearch extends Vue {
    @Prop() smartQueryTableArr: any;

    searchContent: string = "";
    smartQueryDialogVisible: boolean = false;
    selectItem: string = "";
    mainTable: string = "";

    get filterTables() {
        if (_.isEmpty(this.searchContent)) {
            return _.slice(this.smartQueryTableArr, 0, 10);
        }
        return _.sortBy(_.filter(this.smartQueryTableArr, (v: any) => {return _.toUpper(v.join_table).indexOf(_.toUpper(this.searchContent)) > -1;}), 'join_table');
    }

    openSearch(item: any) {
        this.selectItem = item.join_table;
        this.mainTable = item.main_table;
        this.smartQueryDialogVisible = true;
    }
}
</script>

<style lang="scss" scoped>
.smart-query {
    width: 100%;
    height: 100%;
    padding: 20px 20px;
    box-sizing: border-box;
}
.smart-query-title {
    font-weight: 700;
    font-size: 16px;
    line-height: 20px;
}
.search-input {
    padding: 20px 0;
}
.search-items-div {
    padding: 20px 0;
    height: calc(100% - 130px);
    overflow-y: auto;
}
.search-item {
    cursor: pointer;
    display: block;
    font-size: 14px;
    line-height: 20px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    width: 100%;
    padding-bottom: 10px;
}
.el-icon-question {
    padding-left: 5px;
    color: #cacbcf;
}
</style>
