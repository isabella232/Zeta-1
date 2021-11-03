<template>
  <div
    ref="wrapper"
    class="tree-chart-tooltip"
    :style="{top: top + 'px', left: left + 'px', visibility: data ? 'visible' : 'hidden' }"
    @mouseleave="mouseleave"
    @mouseenter="mouseenter"
  >
    <el-alert
      v-if="jobInfo.status === 'unknown'"
      type="warning"
      :closable="false"
    >
      <span>Missing done file.  
        <a @click="onRequest">
          Request <i class="el-icon-message" />
        </a>
      </span>
    </el-alert>
    <el-alert
      v-if="jobInfo.status === 'warning'"
      title="This table may have potential delay than usual"
      effect="dark"
      type="warning"
      :closable="false"
    />
    <div
      class="detail-row"
      style="margin-top: 8px"
    >
      <label>Platform</label>
      <span>{{ tableInfo.platform }}</span>
    </div>
    <div class="detail-row">
      <label>Database</label>
      <span>{{ tableInfo.db_name || 'default' }}</span>
    </div>
    <div class="detail-row">
      <label>Table name</label>
      <span>{{ tableInfo.table_name }}</span>
    </div>
    <div
      v-if="jobInfo.finish_time"
      class="detail-row"
    >
      <label>Last finish Time</label>
      <span>{{ (jobInfo.finish_time || '').substr(0, 19) }}</span>
    </div>
  </div>
</template>

<script lang="ts">
import { Vue, Component, Prop } from 'vue-property-decorator';
import { attempt } from '@drewxiu/utils';

@Component({
  components: {
  }
})
export default class Tooltip extends Vue {

  @Prop() data;

  selfRef: any;
  mounted() {
    this.selfRef = this.$refs.wrapper;
  }

  get top() {
    return attempt(() => {
      const y = this.data.y;
      const heightLimit = this.selfRef.parentElement.offsetHeight;
      const selfHeight = this.selfRef.offsetHeight;
      if (heightLimit - y < selfHeight) {
        return y - selfHeight;
      }
      return y;
      
    }, -2000);
  }
  get left() {
    return attempt(() => this.data.x, 4000) + 10;
  }
  get tableInfo() {
    return attempt(() => this.data.data || {}, {});
  }
  get jobInfo() {
    return  this.tableInfo.info || {};
  }

  mouseenter() {
    this.$emit('mouseenter');
  }
  mouseleave() {
    this.$emit('mouseleave');
  }

  onRequest() {
    this.$emit('request', this.data);
  }
}
</script>
<style lang="scss" scoped>

.tree-chart-tooltip {
  position: absolute;
  transition: all .2s ease-in-out;

  background: white;
  padding: 12px 16px;
  border: 1px solid #ccc;
  border-radius: 6px;

  a {
    cursor: pointer;
  }

  .detail-row {
    padding: 0 0 4px;

    label {
      display: inline-block;
      width: 120px;
      font-weight: bold;
    }
    > span {
      color: #666;
      display: inline-block;
      vertical-align: text-top;
      word-break: break-word;
    }
  }
}
</style>
