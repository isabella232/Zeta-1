<template>
  <pre
    class="json-highlighter"
    v-html="highlightedJson"
  />
</template>

<script lang="ts">
import { Component, Vue, Prop } from 'vue-property-decorator';
// eslint-disable-next-line @typescript-eslint/ban-ts-ignore
// @ts-ignore;
import jsonHighlight from 'json-highlight';

@Component({
  components: {},
})
export default class JsonHighlight extends Vue {

  @Prop() json: string | object;

  get highlightedJson() {
    const json = typeof(this.json) === 'string' ? JSON.parse(this.json) : this.json;
    return jsonHighlight(json);
  }
}
</script>
<style lang="scss">
@import "@/styles/global.scss";
.json-highlighter {
  .json-key {
    color: green;
  }
  .json-number,
  .json-string {
    color: blue;
  }
}
</style>
