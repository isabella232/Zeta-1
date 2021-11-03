<template>
  <el-popover
    placement="bottom"
    width="400"
    trigger="click"
  >
    <div>
      <el-row>
        <el-col :span="12">
          Notebook:
        </el-col>
        <el-col :span="12">
          {{ name }}
        </el-col>
        <el-col :span="12">
          Owner:
        </el-col>
        <el-col :span="12">
          {{ owner }}
        </el-col>
        <el-col :span="12">
          Last modifed:
        </el-col>
        <el-col :span="12">
          {{ lastModified }}
        </el-col>
      </el-row>
    </div>
    <el-button
      slot="reference"
      type="text"
      class="notebook-tool-btn"
    >
      <i class="zeta-icon-info" />
    </el-button>
  </el-popover>
</template>

<script lang="ts">
/**
 * Component <ShareLink>  Only use notebook share link
 * Other should use '@/components/common/share-link'
 */
import { Notebook } from '@/types/workspace';
import { Component, Vue, Prop } from 'vue-property-decorator';
import moment from 'moment';
@Component({
  components: {},
})
export default class NotebookInfo extends Vue {
  @Prop()
  notebook: Notebook;
  @Prop() title: string;

  get name () {
    return this.notebook.name;
  }
  get owner () {
    return this.notebook.nt;
  }
  get lastModified () {
    return moment(this.notebook.updateTime).format('YYYY-MM-DD HH:mm');
  }
}
</script>
<style lang="scss" scoped>
.notebook-tool-btn {
  margin-right: 10px;
}
.zeta-icon-info{
  font-size: 20px;
}
</style>
