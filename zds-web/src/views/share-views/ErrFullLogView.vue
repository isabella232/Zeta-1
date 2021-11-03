<template>
  <div>
    <div class="json-wrapper">
      <json-highlight :json="logJson" />
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Watch } from "vue-property-decorator";
import JsonHighlight from "@/components/common/JsonHighlight.vue";
@Component({
  components: {
    JsonHighlight
  }
})
export default class ErrFullLogView extends Vue {
  get logJson() {
    //const logJson = this.$route.query.logJson;
    const logJson = localStorage.getItem("error-full-log");
    return logJson;
  }
}
</script>
<style lang="scss" scoped>
.json-wrapper {
  width: 100%;
  height: 100%;
  overflow: auto;
}
</style>

