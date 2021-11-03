<template>
  <div class="dependency-container">
    <div class="row">
      <div class="row-name swatch">
        Generate dependency signal(s) for this job
        <el-tooltip
          content="Bottom center"
          placement="bottom"
          effect="light"
          popper-class="dependency-info-popup"
        >
          <div slot="content">
            This function is for manually adding <br/>dependency signals to tables updated<br/> by this job. When the scheduled job is <br/>finished, signal will be sent out for<br/> below added table(s) to indicate update <br/>to the table(s) has been completed.
          </div>
          <i class="el-icon-question" />
        </el-tooltip>
      </div>
      <div class="row-info">
        <el-switch
          v-model="dependencySignal.enabled"
          :disabled="isShared"
        />
      </div>
    </div>
    <div v-if="dependencySignal.enabled">
      <div class="row">
        <div class="row-name">
          SearchTable
        </div>
        <div class="row-info">
          <el-autocomplete
            ref="autocomplete"
            v-model="searchStr"
            class="my-autocomplete"
            :fetch-suggestions="querySearch"
            placeholder="search table"
            :trigger-on-focus="false"
            :clearable="true"
            :disabled="isShared"
            popper-class="my-dependency-signal-search"
            @select="handleSelect"
          >
            <template slot-scope="{ item }">
              <div
                v-if="item.value !== 'isEmpty'"
                :class="isOwner(item)?'list':'unknown'"
              >
                <span
                  class="name"
                  :title="item.value"
                >{{ item.value }}</span>
              </div>
              <p
                v-else
                class="is-empty"
              >
                Didn't find the table you want?
                <router-link to="/metadata/register/vdm">
                  Register new metadata
                </router-link>
              </p>
            </template>
          </el-autocomplete>
        </div>
      </div>
      <el-table
        v-loading="loading"
        :data="dependencySignal.signalTables"
        style="width: 100%"
        max-height="130"
        border
      >
        <el-table-column
          prop="tableName"
          label="Table"
        >
          <template slot-scope="scope">
            <div class="col-div">
              {{ scope.row }}
            </div>
          </template>
        </el-table-column>
        <el-table-column
          label="Action"
          :width="80"
        >
          <template slot-scope="scope">
            <div class="col-div">
              <div class="content-center">
                <i
                  v-if="!isShared"
                  class="icon el-icon-close"
                  @click="handleRemove(scope.$index)"
                />
              </div>
            </div>
          </template>
        </el-table-column>
        <template slot="empty">
          No Dependency table
        </template>
      </el-table>
    </div>
  </div>
</template>
<script lang="ts">
import { Component, Vue, Prop, Watch, Inject } from 'vue-property-decorator';
import { INotebook } from '@/types/workspace';
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import DoeRemoteService from '@/services/remote/DoeRemoteService';
import { NoteScheduleDependencySignal } from '@/components/common/schedule-container';
import _ from 'lodash';
import Util from '@/services/Util.service';
import Config from '@/config/config';

interface TableItem {
  pltfrm_name: string;
  db_name: string;
  table_name: string;
  is_owner: number;
}
@Component({
  components: {
  },
})
export default class DependencySignal extends Vue {
  @Inject()
  notebookRemoteService: NotebookRemoteService;
  doeRemoteService= new DoeRemoteService();
  @Prop()
  dependencySignal: NoteScheduleDependencySignal;
  @Prop()
  notebookId: string;
  @Prop()
  clusterId: string;

  loading = false;

  searchStr = '';

  get isShared(){
    return Util.isShareApp();
  }
  get User(){
    return Util.getNt();
  }

  get platform(){
    const allAlias = Config.zeta.notebook.connection.allAliasIdMap;
    const alias = _.findKey(allAlias, (v)=>{
      return v.toString() == this.clusterId;
    });
    if(alias){
      // if(alias === 'Hermes'){
      //   return 'hermes';
      // }
      return alias.toLowerCase().indexOf('apollo')>-1?'apollo_rno': alias;
    }
    return alias;
  }
  get scheduleInfo() {
    return this.$store.getters.taskByNotebookId(this.notebookId);
  }

  isOwner(item: TableItem){
    return item.is_owner === 1? true: false;
  }
  querySearch(searchStr: string, cb: Function){

    this.notebookRemoteService.isVDMOwner(this.User, this.platform, searchStr).then(res => {
      const options: Array<any> = [];
      if (res && res.data && res.data.data) {
        _.forEach(res.data.data, (option: any) => {
          const fullTableName = `${option.db_name+'.'+option.table_name}`;
          options.push(
            {
              value: fullTableName,
              platform: option.pltfrm_name,
              table: option.table_name,
              db: option.db_name,
              is_owner: option.is_owner,
            }
          );
        });
      }
      options.sort(this.sortByType());
      options.push({value: 'isEmpty'});
      cb(options);
    }).catch(()=>{
      cb([]);
    });
  }
  sortByType(){
    return (a: TableItem, b: TableItem) => {
      return b.is_owner - a.is_owner;
    };
  }
  handleSelect(item: any){
    if(item.value == 'isEmpty' || !this.isOwner(item) ){
      setTimeout(()=>{
        this.searchStr = '';
      },0);
      return;
    }
    this.searchStr = '';
    const tableName = item.value;
    const has = _.find(this.dependencySignal.signalTables, (table: string) => {
      if(table === tableName)
        return true;
    });
    if(has)return;
    this.dependencySignal.signalTables.unshift(tableName);
  }

  handleRemove(index: number){
    this.dependencySignal.signalTables.splice(index, 1);
  }

  @Watch('clusterId', {deep: true})
  clusterIdChange(newVal: any) {
    console.log('clusterIdChange:' + JSON.stringify(newVal));
    if(newVal && this.dependencySignal.enabled) {
      this.searchStr = '';
      (this.$refs['autocomplete'] as any).suggestions = [];
      if(this.scheduleInfo && (this.scheduleInfo.task.clusterId == newVal)){
        this.dependencySignal.signalTables = JSON.parse(JSON.stringify(this.scheduleInfo.dependencySignal.signalTables));
      }else{
        this.dependencySignal.signalTables = [];
      }
    }
  }
}
</script>
<style lang="scss" scoped>
$height: 30px;
.dependency-container {
    max-width: 590px;
    /deep/ .el-table {
        th, td {
            padding: 5px 0;
        }
    }
    /deep/ div.el-table__empty-block {
        min-height: $height;
        max-height: $height;
    }
    /deep/ .el-table__empty-text {
        line-height: $height;
    }
    .row-name.swatch{
      width: 310px !important;
    }
    .content-center {
      text-align: center;
      .icon{
        cursor: pointer;
        color: #569ce1;
        font-weight: bold;
        &:hover{
          color: #4d8cca;
        }
      }
    }
    .my-autocomplete{
      width: 100%;
      max-width: 450px;
    }
    .dependency-info-popup{
      color: red;
    }
}
</style>
<style lang="scss">
.my-dependency-signal-search{
  .el-autocomplete-suggestion__list{
    position: static !important;
    padding-bottom: 28px;
  }
  .el-autocomplete-suggestion li{
    padding: 0 10px;
  }
  .is-empty{
    position: absolute;
    bottom: 0;
    left: 0;
    width: 100%;
    padding: 0 10px;
    font-size: 12px;
    background-color: #e4e7ed;
    border-bottom-left-radius: 4px;
    border-bottom-right-radius: 4px;
    box-sizing: border-box;
  }
}
</style>
