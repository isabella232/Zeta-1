<template>
  <el-popover
    placement="bottom-start"
    trigger="click"
    :value="popoverVisible"
  >
    <div
      v-if="paramInSql.length > 0"
      class="params notebook-tool"
    >
      <el-row
        type="flex"
        class="el-row"
        :gutter="20"
        justify="space-between"
      >
        <el-col
          :span="6"
          class="title"
        >
          <div>Variables</div>
        </el-col>
        <el-col
          :span="12"
          class="title"
        >
          <div>
            Generator
            <el-popover
              placement="bottom"
              trigger="hover"
            >
              <div style="margin:0px 24px">
                <a
                  target="_blank"
                  href="https://wiki.vip.corp.ebay.com/display/ND/Dynamic+Variables"
                >Quick start</a>
              </div>
              <i
                slot="reference"
                class="el-icon-info"
              />
            </el-popover>
          </div>
        </el-col>
        <el-col
          :span="6"
          class="title"
        >
          <div>Value</div>
        </el-col>
      </el-row>

      <el-row
        v-for="paramName in paramInSql"
        :key="paramName"
        class="params-item el-row"
        type="flex"
        :gutter="20"
        justify="space-between"
      >
        <el-col :span="6">
          <span>{{ paramName }}:</span>
        </el-col>
        <el-col :span="12">
          <el-input
            type="textarea"
            resize="none"
            :autosize="{ maxRows: 4}"
            :disabled="isPreset(paramName)"
            class="option-input"
            :value="paramsGen[paramName]"
            @input="val => onParamGenChange(val,paramName)"
          />
        </el-col>
        <el-col :span="6">
          <el-input
            class="option-input"
            :value="paramsVar[paramName]"
            @input="val => onParamValChange(val,paramName)"
          />
        </el-col>
      </el-row>
    </div>
    <div
      v-else
      class="params notebook-tool-nodata"
    >
      <h3>No variables found</h3>
      <span>Only support Spark SQL and TD Sql</span>
    </div>
    <el-button
      slot="reference"
      v-click-metric:NB_TOOLBAR_CLICK="{name: 'params'}"
      type="text"
      class="notebook-tool-btn"
    >
      <i class="zeta-icon-sql" />
      <span class="params-title">{{ title }}</span>
    </el-button>
  </el-popover>
</template>

<script lang="ts">

import { Component, Vue, Emit, Prop, Watch } from 'vue-property-decorator';
import { IPreference, INotebook, variables } from '@/types/workspace';
import _ from 'lodash';

export interface Parameter {
  vargenerator: string | null;
  value: string;
}
@Component
export default class Params extends Vue {
  @Prop() isPublic: boolean;
  @Prop() notebookId: string;
  @Prop() sql: string;
  @Prop() popoverVisible: boolean;
  @Prop() title: string;
  paramsVar: Dict<string> = {};
  paramsGen: Dict<string> = {};
  paramInSql: Array<string> = [];
  debounceUpdate = _.debounce(this.updatePreference, 1000, {
    trailing: true,
  });

  get preference(): IPreference | undefined {
    const notebook: INotebook = this.$store.state.workspace.workspaces[
      this.notebookId
    ];
    if (notebook) {
      return notebook.preference;
    } else {
      return undefined;
    }
  }
  get allParams() {
    return this.uniqColumns(this.paramsVar, this.paramsGen);
  }

  @Watch('sql')
  onSqlChange(newVal: string) {
    this.loadParams(newVal);
  }

  @Watch('preference', { deep: true })
  onPreferenceChange() {
    this.loadParams();
  }

  @Emit('onParamsChange')
  // eslint-disable-next-line @typescript-eslint/no-empty-function
  onParamsChange() {}

  mounted() {
    this.loadParams();
  }

  quickStart() {
    location.href = '';
  }

  loadParams(sql: string = this.sql){
    const params = _.uniq(this.findParams(sql));
    this.$set(this, 'paramInSql', params);
    const vari = this.varFromPreference(params);
    const gen = this.genFromPreference(params);
    // diff
    const varEqual = _.isEqual(vari, this.paramsVar);
    const genEqual = _.isEqual(gen, this.paramsGen);
    if (varEqual && genEqual) {
      return;
    }
    if (!varEqual) {
      this.$set(this, 'paramsVar', vari);
    }
    if (!genEqual) {
      this.$set(this, 'paramsGen', gen);
    }
    this.debounceUpdate();
  }

  /**
   * @param params all parameters from Notebook content
   */
  varFromPreference(params: string[]) {
    const vari = this.preference &&
      this.preference['notebook.variables'] != undefined
      ? _.clone(this.preference['notebook.variables'] as Dict<string>)
      : {};
    // add new col if it not exist in preference
    _.forEach(params, key => {
      if (!vari[key]) {
        vari[key] = '';
      }
    });
    _.forEach(vari, (val, col) => {
      if (variables[col] && !val) {
        vari[col] = variables[col];
      }
      // rm dead col
      if (params.indexOf(col) < 0 && !vari[col]) {
        delete vari[col];
      }
    });
    return vari;
  }

  /**
   * @param params all parameters from Notebook content
   */
  genFromPreference(params: string[]) {
    const gen = this.preference &&
      this.preference['notebook.vargenerators'] != undefined
      ? _.clone(this.preference['notebook.vargenerators'] as Dict<string>)
      : {};

    // add new col if it not exist in preference
    _.forEach(params, key => {
      if (!gen[key]) {
        gen[key] = '';
      }
    });
    // rm dead col
    _.forEach(gen, (val, col) => {
      if (params.indexOf(col) < 0 && !gen[col]) {
        delete gen[col];
      }
    });
    return gen;
  }

  findParams(sql: string) {
    const reg = /\$\{(.+?)\}/g;
    const $params: string[] = [];
    _.chain(sql.match(reg) || [])
      .uniq()
      .forEach(p => $params.push(p.substr(2, p.length - 3)))
      .value();

    const reg2 = /\$[a-zA-Z]([\w-]*)(?![\w-}])/g;
    _.chain(sql.match(reg2) || [])
      .uniq()
      .forEach(p => $params.push(p.substr(1, p.length - 1)))
      .value();
    return $params;
  }

  getParamsMap(): Dict<Parameter> {
    return _.reduce(this.allParams, (result, paramName) => {
      result[paramName] = {
        vargenerator: this.paramsGen[paramName],
        value: this.paramsVar[paramName],
      };
      return result;
    }, {} as Dict<Parameter>);
  }

  //action
  updatePreference() {
    const notebookPreference: IPreference = this.preference? this.preference :{};
    notebookPreference['notebook.variables'] = this.paramsVar;
    notebookPreference['notebook.vargenerators'] = this.paramsGen;

    this.$store.dispatch('updateNotebook', {
      notebookId: this.notebookId,
      preference: notebookPreference,
    });
    this.onParamsChange();
  }
  onParamValChange(val: any, col: string) {
    this.paramsVar[col] = val;
    this.debounceUpdate();
  }
  onParamGenChange(val: any, col: string) {
    this.paramsGen[col] = val;
    this.debounceUpdate();
  }

  uniqColumns(vari: Dict<string>, gen: Dict<string> ) {
    return _.chain(vari).keys()
      .concat(_.keys(gen))
      .uniq()
      .value();
  }
  isPreset(col: string) {
    return _.keys(variables).indexOf(col) >= 0;
  }
}
</script>

<style lang="scss" scoped>
.notebook-tool {
    margin: 0 12px;
}
.params {
    &.notebook-tool-nodata {
        width: 200px;
        text-align: center;
        margin: 10px;
        font-size: 12px;
    }
    .el-row {
      // height:35px;
      &:last-child {
        margin-bottom: 0;
      }
      .title{
        color:black;
        font-weight:bold;
      }
    }
    .el-col {
    border-radius: 4px;
    }
    .row-bg {
    padding: 10px 0;
    align-items:stretch;
    background-color: #f9fafc;
  }

  .params-item {
    display: flex;
    margin: 5px 0;
    justify-content: space-between;
  }
}
.notebook-tool-btn {
    position: relative;
    .zeta-icon-sql{
        transition: .3s;
        font-size: 20px;
    }
    .params-title{
        font-size: 12px;
        line-height: 20px;
        transition: .3s;
        position: absolute;
        display: none;
    }
    &:hover{
        .zeta-icon-sql{
            margin-right: 40px;
        }
        .params-title{
            transform: translateX(-40px);
            display: inline-block;
        }
    }
}
</style>
