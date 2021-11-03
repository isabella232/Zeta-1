<template>
  <el-dialog
    title="Your download file is ready"
    :visible.sync="visible"
    width="480px"
    top="calc(50vh - 75px)"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :append-to-body="true"
  >
    <p>Download file is ready. Download process got blocked by browser.</p>
    Click <a :href="url" target="_blank" @click="close">here</a> to unblock the proceed.
    <span slot="footer" class="dialog-footer" />
  </el-dialog>
</template>

<script lang="ts">
import { Vue, Component, Prop, Watch} from 'vue-property-decorator';

@Component
export default class DumpDialog extends Vue {
  url = '';
  visible = false;
 
  getVisible(){
    return this.visible;
  }
  showDialog(url: string){
    this.url = url;
    this.visible = true;
  }
  close(){
    this.visible = false;
  }
  @Watch('visible')
  change(){
    if(!this.visible){
      this.$destroy();
    }
  }
}
</script>

<style scoped>

</style>
