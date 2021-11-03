<template>
  <div class="source-config-container">
    <div v-if="!isInViewMode">
      <div class="row" v-if="isWhitelist">
          <div class="row-name">
            Target System
          </div>
          <div class="row-body" style="line-height: 30px;">
            <el-radio :disabled="!fieldEditable" v-model="config.metaTableType" label="HDM" @change="systemChange()">HDM</el-radio>
            <el-radio :disabled="!fieldEditable" v-model="config.metaTableType" label="PROD" @change="systemChange()">Production</el-radio>
          </div>
      </div>
      <div class="row" v-show="this.isPROD">
        <div class="row-name">
          Batch Account
        </div>
        <div class="row-body">
            <!-- <el-select v-if="this.isPROD" v-model="config.account" :disabled="!fieldEditable" placeholder="Please Select">
                <el-option
                v-for="(option) in dataBaseItems"
                :key="option.value"
                :label="option.value"
                :value="option.value"
                ></el-option>
            </el-select> -->
          <el-input v-model.trim="config.account" placeholder="" :disabled="flag.disabledPRODEdit"></el-input>
        </div>
      </div>
      <div class="row">
        <div class="row-name">
          Database
        </div>
        <div class="row-body database-row">
          <el-input v-if="this.isPROD" v-model.trim="config.db" placeholder="" :disabled="flag.disabledPRODEdit"></el-input>
          <el-select v-if="this.isHDM" v-model="config.db" placeholder="Please Select" :disabled="!fieldEditable">
              <el-option
              v-for="(option) in dataBaseItems"
              :key="option.value"
              :label="option.value"
              :value="option.value"
              ></el-option>
          </el-select>
        </div>
      </div>
      <div class="row" v-show="this.isPROD">
        <div class="row-name">
            Platform
        </div>
        <div class="row-body">
          <el-select v-model="config.platform" placeholder="Please Select" :disabled="!fieldEditable">
              <el-option
              v-for="(option) in platformItems"
              :key="option.value"
              :label="option.label"
              :value="option.value"
              ></el-option>
          </el-select>
        </div>
      </div>
      <div class="row">
        <div class="row-name">
            Table
        </div>
        <div class="row-body">
            <el-input v-model.trim="config.tbl" placeholder="table name" :disabled="!fieldEditable"></el-input>
        </div>
      </div>
      <div class="row" v-if="isHDM && flag.hasNoAccess">
          <span class="error-msg" >
  No write access to any hermes database. More info check <i class="link" @click="openAce">here</i>.
        </span>
      </div>
    </div>
    <div v-else>
      <div class="row info">
        <div class="config">
          <span>Target System:</span>
          <span class="name">{{config.metaTableType}}</span>
        </div>
        <div class="config">
          <span>Database:</span>
          <span class="name">{{config.db}}</span>
        </div>
      </div>
      <div class="row info">
        <div class="config">
          <span>Table:</span>
          <span class="name">{{config.tbl}}</span>
        </div>
        <div class="config">
          <span>Platform:</span>
          <span class="name">{{config.platform}}</span>
        </div>
      </div>
      <div class="row info">
        <div class="config">
          <span>Account:</span>
          <span class="name">{{config.account}}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Inject, Prop } from "vue-property-decorator";
import {  SourceConfig, Mode, Frequency, freq, MODE, FREQ, MS_SYS } from '@/types/meta-sheet';
import _ from "lodash";
import MetaSheetService from '@/services/remote/MetaSheetService';
import DMRemoteService from '@/services/remote/DataMove';
import { defaultDataMoveQuestion } from "@/components/common/zeta-ace/utilties";
import moment from 'moment';
import Util from "@/services/Util.service";
@Component({
  components: {
  }
})
export default class MetaSourceConfigVue extends Vue {
  @Prop() config: SourceConfig;
  @Prop() mode: Mode; // VIEW or EDIT
  @Prop() fieldEditable: boolean; // this is indicate whether user first click the sync btn, if first time click, then this field is true, otherwise false.
  @Prop() isCreator: boolean; // only creator first time need to get whitelist and vdm database.
  @Prop() isWhitelist: boolean; // only whitelist person can see the system option
  @Inject('metaSheetService') metaSheetService: MetaSheetService;
  @Inject('dmRemoteService') dmRemoteService: DMRemoteService;

  dataBaseItems: Array<any> = [];
  platformItems = [
    {value: 'ares', label: 'Ares'},
    {value: 'apollorno', label: 'ApolloReno'},
    {value: 'hercules', label: 'Hercules'}
  ];
    // batchAccountItems = [
    //     {value: 'choli'},
    //     {value: 'b_mkth'},
    // ];

  flag = {
    hasNoAccess: false,
    disabledPRODEdit: true
  }

  get isInViewMode() {
    return this.mode == MODE.VIEW;
  }

  get isPROD() {
    return this.config.metaTableType == MS_SYS.PROD;
  }

  get isHDM() {
      return this.config.metaTableType == MS_SYS.HDM;
  }

  async created() {
    try {
      if(this.isCreator && this.fieldEditable) {
        let response = await this.getDataBases();
        if(response.status == 200 && response.data) {
          this.dataBaseItems = _.map(response.data, (item: string) => {
            return {
              value: item
            }
          });
        } else {
          this.dataBaseItems = [];
        }
        if(this.dataBaseItems.length == 0) {
          this.flag.hasNoAccess = true;
        }
      }
    }
    catch(e) {}
  }

  getWhiteList() {
    return this.metaSheetService.getWhiteList();
  }

  getDataBases() {
    return this.dmRemoteService.getVDMDataBases('hermesrno', true);
  }

  systemChange() {
    let metaTableType = this.config.metaTableType;
    if(metaTableType == MS_SYS.PROD) {
      this.config.db = 'prs_w';
      this.config.account = 'b_mrktng_hlth';
      this.config.tbl = '';
      this.config.platform = '';
    } else {
      this.config.db = '';
      this.config.tbl = '';
      this.config.platform = '';
    }
  }

  getAcctontName() {

  }

  openAce() {
    this.$emit("close");
    this.$store.dispatch('setAceVisible', true);
    let q = {
      question: "No write access to HDM(Hadoop data mart) on Hermes",
      answer: {
      question: "Two potential reasons for showing this issue.\n 1.User don’t have Hermes access\n&nbsp;&nbsp;&nbsp;&nbsp;Please go this like to request Hermes access on <a href='https://bdp.vip.ebay.com' target='_blank'>https://bdp.vip.ebay.com</a>.\n 2.You have no Hermes database created.\n &nbsp;&nbsp;&nbsp;&nbsp;Create & Manage your workspace on <a href='https://bdp.vip.ebay.com' target='_blank'>https://bdp.vip.ebay.com.</a>.",
      },
      time: moment().format('h:mm:ss a')
    }
    this.$store.dispatch('setAceQuestion', q);
  }

}
</script>

<style lang="scss" scoped>
$blue: #569ce1;
$grey: #606266;
$red: #f56c6c;
$margin: 10px;
$margin2x: 20px;
.source-config-container {
  .row {
    display: flex;
    margin-bottom: $margin2x;

    .row-name {
      width: 120px;
      line-height: 30px;
      flex-shrink: 0;
    }

    .row-body {
      flex: 1;
      display: flex;

      span.info {
        white-space: nowrap;
        margin: 0 5px;
        align-self: center;
      }

      .el-radio {
        align-self: center;
      }

      .el-input, .el-select {
        margin-right: 5px;
      }
    }
    /deep/ .el-select {
        width: 100%;
    }
  }

  .row.info {
    div:first-child {
      flex: 1;
    }

    div:last-child {
      flex: 1;
    }

    .config {
      display: flex;
      span:first-child {
        width: 150px;
      }
      span:last-child {
        flex: 1;
      }

    }

    .name {
      margin-left: $margin;
      color: $grey;
    }
  }

  span.error-msg{
    font-size: 12px;
    color: $red;
  }

  .link{
    font-style: normal;
    color: $blue;
    cursor: pointer;
    text-decoration: underline;
  }
}
</style>
