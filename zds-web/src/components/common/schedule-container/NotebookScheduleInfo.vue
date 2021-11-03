<template>
  <div class="dataMove-schedule">
    <h3 class="schedule-title">
      Notebook Details
    </h3>
    <div class="row">
      <div class="row-name">
        Notebook:
      </div>
      <div class="row-info">
        <el-button
          type="text"
          class="notebook-name"
          @click="openNotebookById(notebookId)"
        >
          {{ notebookName }}
        </el-button>
      </div>
    </div>
    <div class="row">
      <div class="row-name">
        Connect to:
      </div>
      <div
        v-if="isOwner&&!isShared"
        class="row-info"
      >
        <el-select
          v-if="codeType === 'SQL'"
          v-model="task.clusterId"
          :disabled="isShared||!isOwner"
          @change="(val) => valueChangeHandler('clusterId', val)"
        >
          <el-option
            v-for="item in clusterOption"
            :key="item.clusterAlias"
            :label="item.clusterAlias"
            :value="item.clusterId"
          />
        </el-select>
        <el-select
          v-else-if="codeType === 'TERADATA'"
          v-model="task.prop.host"
          :disabled="isShared||!isOwner"
          @change="(val) => valueChangeHandler('host', val)"
        >
          <el-option
            v-for="(host, clusterAlias) in tdClusterOption"
            :key="clusterAlias"
            :label="clusterAlias"
            :value="host"
          />
        </el-select>
        <el-select
          v-else-if="codeType === 'HIVE'"
          v-model="task.prop.host"
          :disabled="isShared||!isOwner"
          @change="(val) => valueChangeHandler('host', val)"
        >
          <el-option
            v-for="(host, clusterAlias) in hiveClusterOption"
            :key="clusterAlias"
            :label="clusterAlias"
            :value="host"
          />
        </el-select>
      </div>
      <div
        v-else
        class="row-info"
      >
        <span class="disable">{{ connectionAlias }}</span>
      </div>
      <div
        class="row-name"
        style="width: 90px; padding-left: 10px;"
      >
        Connect as:
      </div>
      <div class="row-info">
        <el-select
          v-if="codeType === 'SQL'"
          v-model="task.proxyUser"
          :disabled="isShared||!isOwner"
          @change="(val) => valueChangeHandler('batchAccount', val)"
        >
          <el-option
            v-for="(batchId, $index) in batchAccountOptions"
            :key="$index"
            :label="batchId"
            :value="batchId"
          />
        </el-select>
        <el-input
          v-else-if="codeType === 'TERADATA'"
          :value="task.proxyUser"
          :disabled="true"
        />
        <el-input
          v-else-if="codeType === 'HIVE'"
          :value="task.proxyUser"
          :disabled="true"
        />
      </div>
    </div>
    <dependency
      v-if="showDependency"
      :dependency="scheduleInfo.dependency"
      :cluster-id="task.clusterId"
      :notebook-id="notebookId"
    />
    <dependency-signal
      v-if="showDependency"
      :dependency-signal="scheduleInfo.dependencySignal"
      :cluster-id="clusterId"
      :notebook-id="notebookId"
    />
  </div>
</template>

<script lang="ts">
import { Component, Vue, Watch, Prop, Emit, Inject } from 'vue-property-decorator';
import {
  NotebookSchedule,
  ScheduleConfig,
  TDNotebookScheduleProp,
  getHermesConfigProps,
  getScheduleJDBCProp,
} from '@/components/common/schedule-container';
import { ClusterDict, CodeType, CodeTypes } from '@/types/workspace';
import { WorkspaceSrv } from '@/services/Workspace.service';
import config from '@/config/config';
import Util from '@/services/Util.service';
import _ from 'lodash';
import { WorkspaceManager } from '@/types/workspace/workspace-manager';
import Dependency from './dependency.vue';
import DependencySignal from './dependency-signal.vue';
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import { isHermes } from '@/services/connection.service';

function getCodeType(conf: NotebookSchedule): CodeType {
  if (conf.req.interpreter === 'livy-sparksql') return CodeType.SQL;
  if (conf.prop && conf.prop.jdbc_type) {
    if (conf.prop.jdbc_type === 'teradata') {
      return CodeType.TERADATA;
    } else if (conf.prop.jdbc_type === 'hive') {
      return CodeType.HIVE;
    } else if (conf.prop.jdbc_type === 'carmel') {
      return CodeType.SQL;
    }
  }
  return CodeType.SQL;
}
@Component({
  components: {
    Dependency,
    DependencySignal,
  },
})
export default class NotebookScheduleInfo extends Vue {
  @Inject('notebookRemoteService')
  notebookRemoteService: NotebookRemoteService;
  @Prop()
  value: NotebookSchedule;

  @Prop()
  scheduleInfo: ScheduleConfig;

  notebook: any = null;

  get isOwner() {
    if(this.scheduleInfo.nt === this.nt){
      return true;
    }
    return false;
  }

  get nt() {
    return Util.getNt();
  }

  get isShared(){
    return Util.isShareApp();
  }
  get task() {
    const prop = (this.value.prop as TDNotebookScheduleProp);
    if( 'prop' in this.value &&prop.jdbc_type === 'teradata'){
      const hostKey =  prop.host;
      const hostMap = config.zeta.notebook.tdConnection.hostMap;
      const host  = Object.values(hostMap).find(key=> key === hostKey);
      if(!host){
        prop.host = '';
      }
    }
	  return this.value;
  }
  get clusterId() {
    if(this.task.clusterId) {
      return this.task.clusterId;
    }
    const prop = this.task.prop;
    if(prop && prop.host) { // old data(TD and hive) does not have clusterId
      const hostMap = {
        ...config.zeta.notebook.tdConnection.hostMap,
        ...config.zeta.notebook.hiveConnection.hostMap,
      };
      const hostKey = Object.keys(hostMap).find(k => hostMap[k] == prop!.host);
      this.task.clusterId = config.zeta.notebook.connection.hiveOrTdClustersAliasIdMap[hostKey!];
      return this.task.clusterId;
    }
  }
  get isHermes() {
    return this.value.prop && this.value.prop.jdbc_type === 'carmel';
  }
  @Emit('input')
  onInputChange(result: NotebookSchedule){
  }

  valueChangeHandler(type: 'clusterId' | 'batchAccount' | 'host', val: any) {
	  if(type == 'clusterId' && isHermes(val)) {
	    const newTask: NotebookSchedule = _.clone(this.task);
	    const interpreter = 'carmel';
	    const prop = getHermesConfigProps(val);
	    newTask.proxyUser = Util.getNt();
	    newTask.req.interpreter = interpreter;
	    newTask.clusterId = val;
	    newTask.prop = prop;
	    this.onInputChange(newTask);
	  } else if(type == 'clusterId'){
	    const newTask: NotebookSchedule = _.clone(this.task);
      delete newTask.prop;
      const codeType = getCodeType(this.task);
      newTask.req.interpreter = CodeTypes[codeType].interpreter;
	    this.onInputChange(newTask);
	  } else if(type == 'host' && this.codeType === 'HIVE'){
      const newTask: NotebookSchedule = _.clone(this.task);
      newTask.req.interpreter = CodeTypes[this.codeType].interpreter;
      newTask.prop = getScheduleJDBCProp(this.codeType, val, this.hiveClusterAlias);
      newTask.clusterId = config.zeta.notebook.connection.hiveOrTdClustersAliasIdMap[this.hiveClusterAlias];
	    this.onInputChange(newTask);
	  } else {
      if (this.codeType === 'TERADATA') {
        const newTask: NotebookSchedule = _.clone(this.task);
        newTask.clusterId = config.zeta.notebook.connection.hiveOrTdClustersAliasIdMap[this.tdClusterAlias];
        this.onInputChange(newTask);
        return;
      }
      this.onInputChange(this.task);
    }

  }

  // get defaultCurrentCodeType(): CodeType {
  // 	const interpreter = this.task.req.interpreter;
  // 	const isHive = this.task.prop && this.task.prop.jdbc_type === 'hive';
  // 	const isHermes = this.task.prop && this.task.prop.jdbc_type === 'carmel';
  // 	if(isHive) return CodeType.HIVE;
  // 	if(isHermes) return CodeType.SQL;
  // 	for (let codeType in CodeTypes) {
  // 		if (CodeTypes[codeType].interpreter === interpreter) {
  // 			return codeType as CodeType;
  // 		}
  // 	}
  // 	return CodeType.SQL;
  // }
  get codeType (){
    return getCodeType(this.task);
  }

  get notebookId() {
	  if(!this.task) return '';
	  return this.task.req.notebookId;
  }
  get connectionAlias(){
    const allAlias = config.zeta.notebook.connection.allAliasIdMap;
    const alias = _.findKey(allAlias, (v)=>{
      return v == this.clusterId;
    });
    return alias;
  }
  get notebookName(): string {
    if(!this.isOwner || this.isShared){
      return this.notebook?this.notebook.name:'';
    }else{
      const file = this.$store.getters.fileByNId(this.notebookId);
      return file?file.title:'';

    }
  }

  get clusterOption(): ClusterDict {
    if(!this.isOwner || this.isShared){
      return {};
    }else{
      return this.$store.getters.user.clusterOption;
    }
  }

  get tdClusterOption() {
	  return config.zeta.notebook.tdConnection.hostMap;
  }
  get hiveClusterOption() {
	  return config.zeta.notebook.hiveConnection.hostMap;
  }
  get hiveClusterAlias() {
    const hiveClusterOption= this.hiveClusterOption;
    const host = this.task.prop ? this.task.prop.host: '';
    return Object.keys(hiveClusterOption).find((key: string) => hiveClusterOption[key] === host) || '';
  }
  get tdClusterAlias() {
    const tdClusterOption= this.tdClusterOption;
    const host = this.task.prop ? this.task.prop.host: '';
    return Object.keys(tdClusterOption).find((key: string) => tdClusterOption[key] === host) || '';
  }
  get batchAccountOptions() {
	  if(!this.clusterId) return [];
	  for (const index in this.clusterOption) {
	    if (this.clusterOption[index].clusterId === this.clusterId) {
	      return this.clusterOption[index].batchAccountOptions;
	    }
	  }
	  return [];
  }

  get showDependency() {
    return (this.scheduleInfo.type === 'Notebook')
    && (this.scheduleInfo.scheduleTime.jobType === 'DAILY')
    && (this.clusterId == 10 || this.clusterId == 14 || this.clusterId == 16); // only support Hercules/ApolloReno/Hermes
  }

  openNotebookById(nId: string){
    if(!nId) return;
    // share notebook
    if(this.isShared || !this.isOwner){
      const url = `${location.protocol}//${location.host}/${Util.getPath()}share/#/notebook?notebookId=${nId}`;
      window.open(url,'_blank');
      return;
    }
	  const ws = WorkspaceSrv.notebook({notebookId:nId});
    WorkspaceManager.getInstance(this).addActiveTabAndOpen(ws);
  }
  async getNotebook(){
    try {
      const result = await this.notebookRemoteService.getSharedNotebook(this.notebookId);
      if(result.data){
        this.notebook = WorkspaceSrv.file2nb(result.data);
      }
    }catch {
      this.$message.warning('Not found notebook!');
    }
    return '';
  }

  mounted () {
    if(this.isShared || !this.isOwner){
      this.getNotebook();
    }
  }
  @Watch('showDependency')
  handleDisableDependeency (newVal: boolean) {
    if (!newVal){
      this.scheduleInfo.dependency = {
        enabled: false,
        dependencyTables: [],
        waitingHrs: 1,
      };
      this.scheduleInfo.dependencySignal = {
        enabled: false,
        signalTables: [],
      };
    }
  }
}
</script>
<style lang="scss" scope>
.row{
  .notebook-name{
    max-width: 350px;
    white-space: nowrap;
    text-overflow: ellipsis;
    overflow: hidden;
  }
  .disable{
    background-color: #F5F7FA;
    border-color: #E4E7ED;
    color: #C0C4CC;
    cursor: not-allowed;
    line-height: 28px;
    height: 30px;
    background-image: none;
    border-radius: 4px;
    border: 1px solid #DCDFE6;
    display: inline-block;
    box-sizing: border-box;
    padding: 0 15px;
    width: 200px;
    outline: none;
  }
}
</style>
