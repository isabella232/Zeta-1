<template>
	<div class="sample-table-display">
        <el-pagination
            layout="prev, pager, next"
            :current-page.sync="currentPage"
            :page-size="pageSize"
            :total="rows.length">
        </el-pagination>
        <table-display
        class="display"
            :page="currentPage"
            :page-size="pageSize"
            :headers="headers"
            :rows="rows">
        </table-display>
	</div>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import { headers, rows } from './table-sample'
import TableDisplay from "@/components/common/Visualization/TableDisplay.vue";
@Component({
  components: {
      TableDisplay
  },
})
export default class SampleTableDisplay extends Vue {
    pageSize = 20;
    currentPage = 1;
    headers = headers;
    rows = rows;
}
</script>
<style lang="scss" scoped>
.sample-table-display{
    overflow: auto;

    height: 500px;
    width: 1000px;
    .display{
        height: 100%;
    }

}
</style>
