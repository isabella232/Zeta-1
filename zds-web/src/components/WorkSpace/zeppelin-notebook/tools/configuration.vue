<template>
  <el-dialog :visible.sync="visible" custom-class="config-dialog" @opened="opened" width="580px">
    <h2>Configuration</h2>
    <div class="optimization notebook-tool" v-loading="updating">
      <div class="default-setting" v-if="prflOption.selectedCfg && prflOption.selectedCfg.optmzt">
        <div class="setting-item-add">
          <span>
            <el-button
              class="circle-btn"
              type="primary"
              icon="el-icon-plus"
              size="mini"
              circle
              @click="addNewCol()"
              :disabled="!prflOption_add.colName || !prflOption_add.colName.trim()"
            />
            <el-input v-model="prflOption_add.colName" />
          </span>
          <el-select
            v-if="prflOption_add.colName === 'spark.yarn.queue'"
            v-model="prflOption_add.val"
          >
            <el-option
              v-for="(name,key) in queue"
              :key="key"
              :label="name===queueAuto?'auto':name"
              :value="name"
            />
          </el-select>
          <el-input v-else v-model="prflOption_add.val" @blur="setAddValue" />
        </div>
        <div
          class="setting-item-wrapper"
          :key="'dft_col_' + $index"
          v-for="(item,$index) in getOptionsColumns(prflOption.selectedCfg.optmzt[prefix])"
        >
          <div class="setting-item">
            <span>
              <el-button
                class="circle-btn"
                type="primary"
                icon="el-icon-minus"
                size="mini"
                circle
                @click="()=> removeCol(item)"
              />
              {{getDisplayName(item)}}
              <span
                class="tip"
              >{{(item === (prefix+'.'+maxResult) && !isPublic)?'(Maximun 5000)':''}}</span>
            </span>
            <div v-if="getDisplayName(item) === 'spark.yarn.queue'">
              <el-select
                v-model="prflOption.selectedCfg.optmzt[prefix][item]"
              >
                <el-option
                  v-for="(name,key) in queue"
                  :key="key"
                  :label="name===queueAuto?'auto':name"
                  :value="name"
                />
              </el-select>
            </div>
            <el-input
              v-else
              v-model="prflOption.selectedCfg.optmzt[prefix][item]"
              @blur="setValue"
              :data-colname="item"
            />
          </div>
        </div>
      </div>
      <div class="apply">
        <el-button
          v-show="!applyAlert"
          type="primary"
          :disabled="applyDisabled"
          v-click-metric:NB_TOOLBAR_CLICK="{name: 'config => apply config'}"
          @click="applyEvent"
        >Apply</el-button>
        <div
          v-show="applyAlert"
        >You’re attempting to disconnect from server. Are you sure you want to continue?</div>
        <div v-show="applyAlert" class="apply-confirm">
          <el-button type="primary" plain @click=" () => applyAlert = false">Cancel</el-button>
          <el-button
            type="primary"
            @click="update"
            v-click-metric:NB_TOOLBAR_CLICK="{name: 'config => apply config'}"
          >Continue</el-button>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script lang="ts">
import { Component, Vue, Prop } from 'vue-property-decorator';
import {
  Optimization,
  NotebookConfig,
} from '@/types/workspace';
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import _ from 'lodash';
import config from '@/config/config';
import { ZetaException } from '@/types/exception';

const DEFAULT_PRFL_NAME = config.zeta.notebook.optimization.defaultPrflName;
const MAXRESULT = 'zds.view.maxResult';
const MAXRESULTVALUE = 5000;

@Component({
  components: {}
})
export default class Configuration extends Vue {
  notebookRemoteService = new NotebookRemoteService();
  @Prop() notebookId: string;

  readonly defaultPrflName = DEFAULT_PRFL_NAME;
  readonly livyPrefix = 'livy';
  readonly jdbcPrefix = 'jdbc';
  readonly queueAuto = 'usingDynamicQueue';

  private applyAlert = false;
  private updating = false;
  private creating = false;

  private prflOption = {
    newPrflName: '',
    selected: '',
    selectedCfg: {} as NotebookConfig
  } as any;

  private prflOption_add = {
    colName: '',
    val: ''
  };
  private dropdownError = {
    msg: '',
    show: false,
    error: function(msg: string) {
      this.msg = msg;
      this.show = true;
    },
    clear: function() {
      this.msg = '';
      this.show = false;
    }
  };
  private queue: Array<string> = [this.queueAuto];
  private isPublic = false;

  maxResult: string = MAXRESULT;
  visible = false;
  currentBatchAccount = '';
  notebookStatus = -1;
  clusterName = '';
  profileId = 'zpl_config';
  config = {};

  //get params
  open(params) {
    this.applyAlert = false;
    this.visible = true;
    this.clusterName =  params.clusterName;
    this.currentBatchAccount = params.currentAccount;
    this.notebookStatus = params.connectionStatus;
    this.config[this.prefix] = this.initConfig(params.interpreterSettings);
  }
  opened() {
    this.init();
  }
  accessable(q: string) {
    if (this.prflOption.selected === this.defaultPrflName) {
      return true;
    }
    return this.queue.indexOf(q) > -1 ? true : false;
  }
  initConfig(config){
    const new_config = _.omit(config, ['zeppelin.proxyUser']);
    return Object.assign({}, this.defaultOptions, new_config);
  }
  get prefix() {
    if (this.isHermes || !this.currentCodeTypeIsSql) return this.jdbcPrefix;
    return this.livyPrefix;
  }
  get isHermes(): boolean{
    if(this.clusterName.indexOf('hermesrno')>-1){
      return true;
    }
    return false;
  }

  get currentCodeTypeIsSql() {
    return true;
  }

  get cfgDirty() {
    // console.log("check dirty")
    if (
      !(
        this.prflOption.selectedCfg &&
        this.prflOption.selectedCfg.optmzt &&
        this.prflOption.selectedCfg.optmzt[this.prefix]
      )
    ) {
      return false;
    }

    const selectedCfg = this.prflOption.selectedCfg.optmzt[this.prefix];
    const storeCfg = this.config[this.prefix];

    if (!_.isEmpty(this.prflOption_add.colName)) {
      // console.log("setDirty","add col")
      return true;
    }

    const keysEqual = Boolean(_.keys(selectedCfg).length === _.keys(storeCfg).length);
    if (!keysEqual) {
      // console.log("setDirty","keysEqual")
      return true;
    }

    let valEqual = true;
    const diffVal = _.chain(storeCfg)
      .forEach((val, col) => {
        if (val != selectedCfg[col] && valEqual == true) {
          valEqual = false;
        }
      })
      .value();
    if (!valEqual) {
      // console.log("setDirty","valEqual",diffVal)
      return true;
    }
    return false;
  }

  get applyDisabled() {
    return !this.cfgDirty;
  }
  get defaultOptions(): Optimization {
    const defaultCfg =
            this.$store.getters.cfgs()[DEFAULT_PRFL_NAME] ||
            {
              optmzt: {
                'livy': {},
              } as Optimization,
            };
    if(defaultCfg.optmzt['zds.livy']){
      const queue = defaultCfg.optmzt['zds.livy']['zds.livy.spark.yarn.queue'];
      if(queue && (queue==='auto')){
        defaultCfg.optmzt['zds.livy']['zds.livy.spark.yarn.queue'] = this.queueAuto;
      }
    }
    const cfg = {};
    for( const key in defaultCfg.optmzt['zds.livy']){
      const newKey = key.replace(/zds.livy/, this.prefix);
      if(newKey === 'livy.zds.view.maxResult'){
        const zplKey = `${this.prefix}.${MAXRESULT}`;
        cfg[zplKey] = defaultCfg.optmzt['zds.livy'][key];
      }else{
        cfg[newKey] = defaultCfg.optmzt['zds.livy'][key];
      }
    }
    const newConfig = _.omit(cfg, ['livy.hive.exec.dynamic.partition', 'livy.hive.exec.dynamic.partition.mode', 'livy.spark.sql.join.statisticOnExecution']);
    return newConfig;
  }
  getOptionsColumns(optmzt: Optimization) {
    return _.keys(optmzt);
  }
  getDisplayName(colName: string): string {
    const names: string = colName.replace(this.prefix + '.', '');
    if (names === MAXRESULT) {
      return 'Result Rows';
    }
    return names;
  }

  //action
  init() {
    const cfg = {
      optmzt: _.cloneDeep(this.config),
      title: this.defaultPrflName,
      notebookId: this.defaultPrflName
    } as NotebookConfig;

    this.prflOption = {
      ...this.prflOption,
      selected: cfg.title,
      selectedCfg: cfg
    };
    // get user queue
    if (this.currentCodeTypeIsSql) {
      this.getUserQueue();
    }
  }
  update() {
    //   add unresolve params
    if (this.prflOption_add.colName && this.prflOption_add.val) {
      this.addNewCol();
    }

    if (this.prflOption.selectedCfg) {
      // apply config
      this.$emit('apply-config', this.prflOption.selectedCfg.optmzt[this.prefix]);
      this.visible = false;
      this.applyAlert = false;
    }
  }

  applyEvent() {
    if (this.notebookStatus !== 1) {
      this.update();
    } else if (this.notebookStatus === 1) {
      this.applyAlert = true;
    }
  }

  addNewCol() {
    // column == ''
    if (!this.prflOption_add.colName || !this.prflOption_add.colName.trim()) {
      return;
    }
    // column exist
    if (
      this.prflOption.selectedCfg.optmzt[this.prefix] &&
      this.prflOption.selectedCfg.optmzt[this.prefix][
        `${this.prefix}.${this.prflOption_add.colName}`
      ]
    ) {
      return;
    }
    let colName = this.prflOption_add.colName.trim();
    const reg1 = /^Result\s{1,}rows$/gi;
    if (reg1.exec(colName)) {
      colName = MAXRESULT;
    }

    this.prflOption.selectedCfg.optmzt[this.prefix] = _.assign(
      {
        [`${this.prefix}.${colName}`]: this.prflOption_add.val
      },
      this.prflOption.selectedCfg.optmzt[this.prefix]
    );
    // clear
    this.prflOption_add = {
      colName: '',
      val: ''
    };
  }
  removeCol(item: any) {
    console.debug('rm item', item);
    const optmzt: Dict<any> = {};
    _.chain(this.prflOption.selectedCfg.optmzt[this.prefix])
      .forEach((val, col) => {
        if (col !== item) {
          optmzt[col] = val;
        }
      })
      .value();
    this.prflOption.selectedCfg.optmzt[this.prefix] = optmzt;
  }
  setValue(e: any) {
    if (this.isPublic) return;
    const colName = e.target.dataset.colname.trim();
    const value = e.target.value;
    if (
      colName === `${this.prefix + '.' + MAXRESULT}` &&
      value > MAXRESULTVALUE
    ) {
      this.prflOption.selectedCfg.optmzt[this.prefix][
        colName
      ] = MAXRESULTVALUE.toString();
    }
  }
  setAddValue() {
    if (this.isPublic) return;
    const colName = this.prflOption_add.colName.trim();
    const value = parseInt(this.prflOption_add.val);
    const reg1 = /^Result\s{1,}rows$/gi;
    if (reg1.exec(colName) && value > MAXRESULTVALUE) {
      this.prflOption_add.val = MAXRESULTVALUE.toString();
    }
  }
  getUserQueue() {
    this.updating = true;
    this.queue = [this.queueAuto];
    this.notebookRemoteService
      .getUserQueue(this.clusterName, this.currentBatchAccount)
      .then(({ data }) => {
        const q = data.filter((name: string) => name !== 'hdlq-data-zeta');
        this.queue = this.queue.concat(q);
        this.updating = false;
      }).catch((e: ZetaException)=>{
        this.updating = false;
      });
  }
}
</script>
<style lang="scss" scoped>
.circle-btn {
  padding: 0px !important;
  margin-left: 5px;
  margin-right: 5px;
  i {
    font-size: 10;
    font-weight: 900;
  }
}
.prfl-list-new {
  padding: 2px 5px 5px 5px;

  .el-input.new-prfl {
    width: 200px;
  }
  .err-msg {
    font-size: 10px;
    color: red;
  }
}

.optimization {
  padding: 15px 0;
  .default-setting,
  .advanced-setting {
    width: 510px;
    max-height: 300px;
    overflow-y: auto;
    margin: 15px 0;
    .setting-item,
    .setting-item-add {
      display: flex;
      align-items: center;
      margin: 5px 0;
      justify-content: space-between;
      .el-input {
        width: 180px;
        /deep/ .el-input__inner {
          height: 30px;
          line-height: 30px;
        }
      }
      .el-select {
        width: 180px;
      }
    }
  }
  .advanced-btn-group {
    text-align: right;
  }
  .apply {
    text-align: right;
    .info {
      width: 400px;
      text-align: left;
    }
    .apply-confirm {
      text-align: right;
    }
  }
  .tip {
    font-size: 12px;
    color: #999999;
  }
}
.notebook-tool-btn {
  position: relative;
  .zeta-icon-optimize {
    transition: 0.3s;
    font-size: 20px;
  }
  .params-title {
    font-size: 12px;
    line-height: 20px;
    transition: 0.3s;
    position: absolute;
    display: none;
  }
  &:hover {
    .zeta-icon-optimize {
      margin-right: 30px;
    }
    .params-title {
      transform: translateX(-30px);
      display: inline-block;
    }
  }
}
</style>
