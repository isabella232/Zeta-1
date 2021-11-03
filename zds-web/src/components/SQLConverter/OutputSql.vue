<template>
  <div
    v-loading="convertSQLLoading"
    class="output-sql editor-container read-only-code-mirror"
  >
    <!-- v-if="sparkSQL" -->
    <CodeDisplay
      v-model="sparkSql"
      disabled
      :options="editorOptions"
    />
    <!-- <div v-else class=no-data>Convert Failed</div> -->
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop } from 'vue-property-decorator';
import CodeDisplay from '@/components/common/Visualization/CodeDisplay.vue';
@Component({
  components: { CodeDisplay },
})
export default class OutputSql extends Vue {
  editorOptions = {
    // codemirror options
    tabSize: 4,
    mode: 'text/x-mysql',
    lineNumbers: true,
    smartIndent: false,
    matchBrackets: true,
    autofocus: true,
    indentWithTabs: true,
    readOnly: true,
  };

  /**
   * store state
   */
  // get sparkSQL() {
  //   return this.$store.getters.sparkSQL;
  // }

  // set sparkSQL(sql){
  //     // to do nothing
  // }
  // get convertLoading() {
  //   return this.$store.getters.convertSQLLoading;
  // }

  @Prop({default:'', type:String})
  sparkSql = '';
  @Prop({default:false, type:Boolean})
  convertSQLLoading = false;

  // public setSQL = (sql:string)=>{
  //   this.sparkSQL = sql;
  // }
  // public setLoading = (loading:boolean) => {
  //   this.convertLoading = loading
  // }


}
</script>
<style lang="scss" scoped>
</style>
