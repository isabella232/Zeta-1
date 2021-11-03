<template>
  <div
    v-loading="inputLoading"
    class="input-script"
  >
    <div
      v-if="tableInfo && tableInfo.SA"
      class="table-info"
    >
      <div class="info">
        SA:<div class="info-content">
          {{ tableInfo.SA }}
        </div>
      </div>
    <!--div class=info-status>Status:{{tableInfo.status}}</div-->
    </div>
    <div
      v-if="tableInfo && tableInfo.owner"
      class="table-info"
    >
      <div class="info">
        Owner:<div class="info-content">
          {{ tableInfo.owner }}
        </div>
      </div>
    </div>
    <div
      v-if="tableInfo && tableInfo.topJob && tableInfo.topJob.length > 0"
      class="table-info"
    >
      <div class="info">
        Top Job:<div class="info-content">
          {{ tableInfo.topJob[0] }}
        </div>
        <el-popover
          v-if="tableInfo.topJob.length > 1"
          placement="bottom"
          trigger="click"
        >
          <div
            v-for="job in tableInfo.topJob"
            :key="'job'+job"
          >
            {{ job }}
          </div>
          <el-button
            slot="reference"
            size="mini"
          >
            <i class="el-icon-more" />
          </el-button>
        </el-popover>
      </div>
    </div>
    <div
      v-if="inputScriptList && inputScriptList.length > 0"
      class="script-list"
    >
      <el-tree
        :props="defaultProps"
        :data="inputScriptList"
      >
        <span
          slot-scope="{ node, data }"
          class="custom-tree-node"
        >
          <a
            v-if="node.level === 1"
            :href="data.path"
            target="_blank"
          >{{ node.label }}</a>
          <span v-else>{{ node.label }}</span>
        </span>
      </el-tree>
    </div>
    <div
      v-else
      class="no-data"
    >
      No SQL file found
    </div>
  </div>
</template>

<script lang="ts">
import {
  Component,
  Vue,
  Prop,
} from 'vue-property-decorator';
import config from '@/config/config';
import _ from 'lodash';

@Component({
  components: {},
})
export default class InputScriptList extends Vue {
  @Prop({default:false, type:Boolean}) inputLoading = false;
  @Prop({default:{}}) tableInfo: any;
  @Prop({default:[]}) scriptList: any[];
  /**
   * 0 sqlScripts
   * 1 DDL
   */
  mode = 'scripts';
  get config () {
    return config;
  }

  defaultProps = {
    children: 'children',
    label: 'label',
  };
  /**
   * store state
   */
  // get inputLoading() {
  //   return this.$store.getters.inputScriptLoading;
  // }
  get inputScriptList () {
    const list: any[] = [];
    const inputList = _.sortBy(this.scriptList || [], ['seqNum']).reverse();
    _.forEach(inputList, (value: any, key: string)=>{
      const item: any = {label:value.sqlFileName, children:[], path: `${config.githubUrl}/${value.saCode}/${value.sqlFileName}`};
      if (value.source){
        _.forEach(value.source, (src: string) =>{
          item.children.push({label:src});
        });
      }
      list.push(item);
    });
    return list;
  }

  // get tableInfo() {
  //   return this.$store.getters.inputTableInfo;
  // }

}
</script>
<style lang="scss" scoped>
.input-script {
  min-height: 400px;
  margin-top: 15px;
  display: flex;
  flex-direction: column;
  .table-info {
    display: flex;
    justify-content: flex-start;
    margin-bottom: 15px;
    .info {
      // min-width: 120px;
      margin: auto 30px auto 5px;
      font-weight: 700;
      display: flex;
    }
    .info-content {
      font-weight: 400;
      margin-left: 5px;
    }
  }
  .script-list {
    // margin-top: 15px;
    overflow-y: auto;
  }
}
</style>
