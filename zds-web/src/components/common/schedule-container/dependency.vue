<template>
  <div class="dependency-container">
    <div class="row">
      <div class="row-name swatch">
        Setup dependency on Source tables
        <el-tooltip
          content="Bottom center"
          placement="bottom"
          effect="light"
          popper-class="data-move-info-popup"
        >
          <div slot="content">
            Add Dependency feature is only avaliable for ApolloReno, Hermes and Hercules.
          </div>
          <i class="el-icon-question" />
        </el-tooltip>
      </div>
      <div class="row-info">
        <el-switch
          v-model="dependency.enabled"
          :disabled="isShared"
        />
      </div>
    </div>
    <div v-if="dependency.enabled">
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
            popper-class="my-dependency-search"
            @select="handleSelect"
          >
            <template slot-scope="{ item }">
              <div :class="getStatus(item)?'unknown':'list'">
                <span
                  class="name"
                  :title="item.value"
                >{{ item.value }}</span>
                <span
                  v-if="getStatus(item)"
                  class="request-row"
                >
                  <span
                    class="request"
                    @click.stop="goRequest(item)"
                  >request</span>
                  <el-tooltip
                    content="Bottom center"
                    placement="bottom"
                    effect="light"
                    popper-class="dependency-info-popup"
                  >
                    <div slot="content">
                      Click to request table owner provide dependency signal.
                    </div>
                    <i class="el-icon-question" />
                  </el-tooltip>
                </span>
              </div>
            </template>
          </el-autocomplete>
        </div>
      </div>
      <el-table
        v-loading="loading"
        :data="dependency.dependencyTables"
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
              {{ scope.row.tableName }}
            </div>
          </template>
        </el-table-column>
        <el-table-column
          prop="wait"
          label="Last Update Time"
          :width="180"
        >
          <template slot-scope="scope">
            <div class="col-div" v-if="scope.row.lastUpdateTime">
              {{ scope.row.lastUpdateTime }} PDT
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
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import ZetaAceRemoteService from '@/services/remote/ZetaAce';
import { NoteScheduleDependency, NoteScheduleDependencyItems  } from '@/components/common/schedule-container';
import _ from 'lodash';
import Util from '@/services/Util.service';
import Config from '@/config/config';
import { ZetaException } from '@/types/exception';

interface TableItem {
  value: string;
  platform: string;
  table: string;
  db: string;
  info: {
    type: string;
    [key: string]: string;
  };
}
@Component({
  components: {
  },
})
export default class Dependency extends Vue {
  @Inject()
  notebookRemoteService: NotebookRemoteService;
  zetaAceRemoteService= new ZetaAceRemoteService();
  @Prop()
  dependency: NoteScheduleDependency;
  @Prop()
  notebookId: string;
  @Prop()
  clusterId: string;

  loading = false;

  searchStr = '';

  originData: any = [];

  get isNotebookPath(){
    return this.$route.path.indexOf('notebook')>-1?true:false;
  }
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
      //   return 'Apollo_Rno';
      // }
      return alias.toLowerCase().indexOf('apollo')>-1?'Apollo_Rno': alias;
    }
    return alias;
  }
  get hourOptions() {
    const keys = Array.from(new Array(13).keys()).slice(1); // [1..12]
    return _.map(keys, (key: number) => {
      return {
        key,
        value: key,
      };
    });
  }
  get scheduleInfo() {
    return this.$store.getters.taskByNotebookId(this.notebookId);
  }
  async getDependencyItems() {
    try {
      const params = _.map(this.dependency.dependencyTables,(d: NoteScheduleDependencyItems)=>{
        return  {
          platform: this.platform,
          db: d.tableName.split('.')[0],
          table:d.tableName.split('.')[1],
        };
      });
      if(params.length===0) return;
      this.loading = true;
      const result = await this.notebookRemoteService.getDependencyTables(params);
      const returnTables = result.data.data;
      this.dependency.dependencyTables = _.map(returnTables, (option: any) => {
        const fullTableName = `${option.db+'.'+option.table}`;
        return {
          timeLag: 0,
          wait: true,
          tableName: fullTableName,
          lastUpdateTime: option.finish_time,
        };
      });
      this.originData = this.dependency.dependencyTables.slice();
      this.loading = false;
    }
    catch(e) {
      this.loading = false;
    }
  }
  getStatus(item: TableItem){
    return item.info.type === 'unknown'? true: false;
  }
  querySearch(searchStr: string, cb: Function){

    this.notebookRemoteService.queryTable(searchStr, this.platform).then(res => {
      let options: Array<any> = [];
      if (res && res.data && res.data.value) {
        _.forEach(res.data.value, (option: any) => {
          const fullTableName = `${option.db+'.'+option.table}`;
          options.push(
            {
              value: fullTableName,
              platform: option.platform,
              table: option.table,
              db: option.db,
              info: option.info,
            }
          );
        });
      }
      if (this.platform === 'Hermes'){
        options = this.uniqData(options);
      }
      // console.log(options);

      options.sort(this.sortByType());
      cb(options);
    });
  }
  uniqData(data: Array<any>){
    const hermesDate = data.filter((item)=>{
      return item.platform.toLowerCase() === 'hermes';
    });
    const rnoData = data.filter((item)=>{
      return item.platform.toLowerCase() === 'apollo_rno';
    });

    rnoData.forEach((rno)=>{
      const index = _.findIndex(hermesDate, (hermes) => hermes.value === rno.value );
      if(index===-1){
        hermesDate.push(rno);
      }else{
        if(hermesDate[index].info.type !== 'ok' && rno.info.type === 'ok'){
          hermesDate.splice(index, 1);
          hermesDate.push(rno);
        }
      }
    });
    return hermesDate;
  }
  sortByType(){
    return (a: TableItem, b: TableItem) => {
      const score = 50;
      return this.getScore(b.info.type, score) - this.getScore(a.info.type,score);
    };
  }
  getScore(value: string, score: number){
    switch (value){
      case 'ok':
        score = 100;
      case 'static':
        score = 100;
        break;
      case 'unknown':
        score = 10;
        break;
      default:
        score = 50;
        break;
    }
    return score;
  }
  handleSelect(item: any){
    if(this.getStatus(item)){
      setTimeout(()=>{
        this.searchStr = '';
      },0);
      return;
    }
    this.searchStr = '';
    const tableItem = {  wait: true, timeLag: 0 ,tableName: item.value, lastUpdateTime: item.info.last_finish_time};
    const has = _.find(this.dependency.dependencyTables, table => {
      if(table.tableName === tableItem.tableName)
        return true;
    });
    if(has)return;
    this.dependency.dependencyTables.unshift(tableItem);
  }

  handleRemove(index: number){
    this.dependency.dependencyTables.splice(index, 1);
  }
  goRequest(item: TableItem){
    const content ={
      name: 'All',
      msg: `[${this.User}] requests for dependency signal of table [${item.value}] on platform [${item.platform}]
        <br/>
        <br/>
        Table Owner - ${item.info.prdct_owner_nt}
        <br/>
        <br/>
        Guide – to enable dependency signal for table, please contact Bin Song with done file provided.
      `,
    };
    const param = {
      subject: `Request Dependency Signal of table [${item.value}]`,
      content: content,
      ccAddr: '',
      sae: item.info.prmry_dev_sae_nt,
    };
    this.sendMail(param);
  }
  async sendMail(param: any) {
    const toAddr = ['stachen@ebay.com','gzhu@ebay.com','binsong@ebay.com'];
    toAddr.push(`${this.User}@ebay.com`);
    if(param.sae){
      toAddr.push(`${param.sae}@ebay.com`);
    }

    const params: any = {
      fromAddr: 'DL-eBay-ZETA@ebay.com',
      toAddr: toAddr.join(';'),
      subject: param.subject,
      content: JSON.stringify(param.content),
      template: 'ZetaNotification',
      ccAddr: '',
      type: 3, //1: html; 2: txt
    };
    if (!_.isEmpty(params.toAddr)) {
      this.zetaAceRemoteService
        .createEmail(params)
        .then(res => {
          console.error('Call Api:createEmail successed');
          if (res && res.status == 200) {
            this.$message.success('Request success');
          }
        })
        .catch((err: ZetaException) => {
          err.resolve();
          console.error('Call Api:createEmail failed: ' + JSON.stringify(err));
          this.$message.info('Request failed');
        });
    } else {
      this.$message.info('empty recipient mailbox');
    }
  }
  created () {
    if(this.isShared || this.isNotebookPath){
      if(this.dependency.enabled){
        this.getDependencyItems();
      }
    }
  }
  @Watch('dependency')
  handleDependency(){
    if(this.dependency.enabled){
      this.getDependencyItems();
    }
  }
  @Watch('dependency.enabled')
  handleNotebookDependencyEnabled(){
    if(this.dependency.enabled){
      this.getDependencyItems();
    }
  }
  @Watch('clusterId', {deep: true})
  clusterIdChange(newVal: any) {
    console.log('clusterIdChange:' + JSON.stringify(newVal));
    if(newVal && this.dependency.enabled) {
      this.searchStr = '';
      (this.$refs['autocomplete'] as any).suggestions = [];
      if(this.scheduleInfo && (this.scheduleInfo.task.clusterId == newVal)){
        this.dependency.dependencyTables = JSON.parse(JSON.stringify(this.originData));
      }else{
        this.dependency.dependencyTables = [];
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
      width: 290px !important;
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
.my-dependency-search{
  .el-autocomplete-suggestion li{
    padding: 0 10px;
  }
}
</style>
