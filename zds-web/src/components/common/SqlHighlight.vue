<template>
  <pre
    class="sql-highlighter"
    v-html="highlightedSql"
  />
</template>

<script lang="ts">
import { Component, Vue, Prop } from 'vue-property-decorator';
import { highlight } from 'sql-highlight';
import sqlFormatter from 'sql-formatter';
 
@Component({
  components: {},
})
export default class SqlHighlight extends Vue {

  @Prop() sql: string | object;

  get highlightedSql() {
    return highlight(sqlFormatter.format(this.sql || ''), { html: true });
  }
}
</script>
<style lang="scss">
@import "@/styles/global.scss";
.sql-highlighter {
  .sql-hl-keyword {
    color: green;
  }
  .sql-hl-function {
    color: orange;
  }
  .sql-hl-string,
  .sql-hl-number {
    color: blue;
  }
  .sql-hl-bracket,
  .sql-hl-special {
    color: gray;
  }
}
</style>


