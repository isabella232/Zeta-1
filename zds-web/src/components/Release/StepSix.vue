<template>
  <div v-loading="loading">
    <iframe :src="iframeURL" style="height: 800px; width:100%; border: 0; margin-bottom: 22px;" ref="iframe"></iframe>
  </div>
</template>

<script lang="ts">
import { Component, Vue } from "vue-property-decorator";
import { setInterval, clearInterval } from "timers";
import _ from "lodash";
@Component({
  components: {}
})
export default class StepSix extends Vue {
  type: string = "";
  id: string = "";
  timer: any;
  loading: boolean = process.env.VUE_APP_ENV == 'production' ? true : false;

  get iframeURL(): string {
    const taskArr = this.$store.getters.getExecTask;
    const idx = this.$store.getters.getIframeIdx;
    if (taskArr[idx] != undefined) {
      this.type = taskArr[idx]["type"];
      this.id = taskArr[idx]["id"];
    }
    console.debug(process.env.VUE_APP_DSSRM_URL + "onetime/" + this.type + (this.type == "uc4" ? "/view/" : "/edit/") + this.id);
    return process.env.VUE_APP_DSSRM_URL + "onetime/" + this.type + (this.type == "uc4" ? "/view/" : "/edit/") + this.id;
  }

mounted() {
  const head = document.getElementsByTagName('head');
  const meta = document.createElement('meta');
  const initChildCount: number = head[0].childElementCount;
  meta.httpEquiv = "Content-Security-Policy";
  meta.content = "upgrade-insecure-requests";
  head[0].appendChild(meta);

  this.timer = setInterval(() => {
    const oncallValidate: any = window.document.cookie.indexOf("otkn.oncall");
      if (oncallValidate > -1) {
        this.loading = false;
        if (head[0].childElementCount > initChildCount) {
          head[0].removeChild(meta);
        }
        window.clearInterval(this.timer);
      }
    }, 1000)
  }

  destory() {
    const head = document.getElementsByTagName('head');
  }
}

</script>
<style lang="scss" scoped>
</style>
