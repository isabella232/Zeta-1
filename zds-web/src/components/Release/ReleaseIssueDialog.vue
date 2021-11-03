<template>
  <el-dialog label-width="80px" title="" :visible.sync="visible_">
    <div v-for="o in issue" :key="o.id" style="margin-bottom: 10px;">
      <span class="issue-label" @click="forward(o.execTask)">{{o.releaseTag}}</span>
    </div>
    <div slot="footer" class="dialog-footer">
      <el-button id="close-btn" type="primary" @click="close()">Close</el-button>
    </div>
  </el-dialog>
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch } from "vue-property-decorator";
import _ from 'lodash';
@Component({
  components: {}
})
export default class ReleaseIssueDialog extends Vue {
  @Prop() visible: boolean;
  @Prop() issue: any;

  set visible_(e) {
    this.$emit("update:visible", e);
  }

  get visible_() {
    return this.visible;
  }

  forward(execTask: any) {
    let tasks: any = {};
    let execTaskArr: any = [];
    try {
      tasks = JSON.parse(execTask);
    } catch(err) {
      console.error("Convert to object error, content: " + execTask);
    }
    _.forEach(tasks, (v: string, k: string) => {
      if (parseInt(v) != 0) {
        const obj = {type: k, id: v};
        execTaskArr.push(obj);
      }
    })
    this.$store.dispatch("setExecTask", execTaskArr);
    this.$store.dispatch("setIfameIdx", 0);
    this.$emit("update:visible", false);
    this.$emit('forward-step-six');
  }

  close() {
    this.$emit("update:visible", false);
  }
}
</script>
<style lang="scss" scoped>
.el-dialog {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}
.issue-label {
  cursor: pointer;
}
</style>
