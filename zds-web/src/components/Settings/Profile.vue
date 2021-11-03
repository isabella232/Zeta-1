<template>
  <div class="td-acct" v-loading="loading">
    <!-- <h1>Account configuration</h1> -->
    <div class="create-acct">
      <el-row>
        <el-col :span="6">User Name</el-col>
        <el-col :span="12">{{UserName}}</el-col>
      </el-row>
      <el-row>
        <el-col :span="6">TD Username:</el-col>
        <el-col :span="12">{{TDAcct}}</el-col>
      </el-row>
      <el-row>
        <el-col :span="6">Teradata Password:</el-col>
        <el-col :span="12">
          <el-input
            v-model="inputInfo.tdPass"
            placeholder="password"
            :type="tdShow?'text':'password'"
            v-bind:class="{'error':alertInfo.password.alert}"
          ><i v-if="(inputInfo.tdPass && inputInfo.tdPass !== '***')" slot="suffix" @click="tdShow=!tdShow" class="el-input__icon el-icon-view"></i></el-input>
        </el-col>
        <el-col :span="6" v-show="alertInfo.password.alert">
          <span class="error error-alert">{{alertInfo.password.msg}}</span>
        </el-col>
      </el-row>

      <el-row>
        <el-col :span="6">Github Token :
          <el-tooltip content="Bottom center" placement="bottom" effect="light">
            <div slot="content">please follow
              <a
                target="_blank"
                href="https://wiki.vip.corp.ebay.com/display/ND/Zeta+Setup+Github+Token+Guide "
              >wiki</a> to setup token
            </div>
            <i class="el-icon-question"></i>
          </el-tooltip>
        </el-col>
        <el-col :span="12">
          <el-input v-model="inputInfo.githubToken" :type="gitShow?'text':'password'">
            <i v-if="(inputInfo.githubToken && inputInfo.githubToken !== '***')" slot="suffix" @click="gitShow=!gitShow" class="el-input__icon el-icon-view"></i>
          </el-input>
        </el-col>
      </el-row>
      <!-- <el-row>
        <el-col :span="6">Windows PET Password :
          <el-tooltip content="Bottom center" placement="bottom" effect="light">
            <div slot="content">please follow
              <a
                target="_blank"
                href="https://wiki.vip.corp.ebay.com/display/GDI/Apply+Prod+AD+account+in+PET"
              >wiki</a> to setup password
            </div>
            <i class="el-icon-question"></i>
          </el-tooltip>
        </el-col>
        <el-col :span="12">
          <el-input v-model="inputInfo.winPass" :type="winShow?'text':'password'">
            <i v-if="(inputInfo.winPass && inputInfo.winPass !== '***')" slot="suffix" @click="winShow=!winShow" class="el-input__icon el-icon-view"></i>
          </el-input>
        </el-col>
      </el-row> -->
      <el-row>
        <el-col class="confirm-col" :span="24">
          <el-button
            type="primary"
            @click="upload"
            :disabled="(!inputInfo.tdPass && !inputInfo.githubToken && !inputInfo.winPass ) || (inputInfo.tdPass === '***' && inputInfo.githubToken === '***' && inputInfo.winPass === '***')"
            v-click-metric:CONFIGURE_CLICK="{name: 'update'}"
          >Update</el-button>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Watch } from 'vue-property-decorator';
import Util from '@/services/Util.service';
import * as Settings from './Settings';

@Component({
  components: {}
})
export default class Profile extends Vue {
  private tdShow = false;
  private gitShow = false;
  private winShow = false;
  get loading() {
    return this.$store.getters.user.loading;
  }
  get hasAcct() {
    return this.inputInfo.tdPass;
  }
  get TDAcct() {
    return Util.getNt();
  }
  get UserName() {
    return (
      this.$store.getters.user.profile.firstName +
      ' ' +
      this.$store.getters.user.profile.lastName
    );
  }
  private isChange = false;

  get userInfo(): Settings.UserInfo {
    return this.$store.getters.user;
  }
  get profile(): Settings.Profile {
    return this.$store.getters.user.profile;
  }
  private inputInfo: Settings.UserInfo = {
    name: '',
    nt: '',
    tdPass: null,
    githubToken: null,
    winPass: null,
    preference: {
      sparkQueue: '',
      sparkQueryMaxResult: ''
    }
  };
  mounted() {
    const nt = Util.getNt();
    const userInfo = this.$store.getters.user as Settings.UserInfo;
    this.inputInfo.nt = nt;
    this.inputInfo.name = this.UserName;
    this.applyUserInfo(userInfo);
  }

  @Watch('userInfo', {immediate: true, deep: true})
  onUserProfileChange(newVal: Settings.UserInfo, oldVal: Settings.UserInfo){
    let passChange = false;
    if(newVal && !oldVal) {
      passChange = true;
    } else {
      passChange =  Boolean(newVal.githubToken || newVal.tdPass || newVal.winPass);
    }
    if(passChange) {
      this.applyUserInfo(newVal);
    }
  }
  private applyUserInfo(userInfo: Settings.UserInfo) {
    this.inputInfo.winPass = userInfo.winPass;
    this.inputInfo.tdPass = userInfo.tdPass;
    this.inputInfo.githubToken = userInfo.githubToken;
    this.inputInfo.preference = userInfo.preference;
  }

  private alertInfo = {
    password: {
      alert: false,
      msg: ''
    },
    reset() {
      this.password = {
        alert: false,
        msg: ''
      };
    }
  };
  upload() {
    const result = this.$store.dispatch('setUserInfo', this.inputInfo);
    result.then(()=>{
      this.$message.success('Update succeed');
    });
  }
}
</script>

<style lang="scss" scoped>
.td-acct {
  /deep/ .el-row {
    height: 40px;
    line-height: 40px;
    margin-top: 15px;
    margin-bottom: 15px;
    .confirm-col {
      text-align: right;
    }
    .el-input.error {
      /deep/ .el-input__inner {
        border-color: #e53917;
        color: #e53917;
      }
    }
    span.error.error-alert {
      margin-left: 10px;
      color: #e53917;
    }
  }
  /deep/ .el-input__suffix{
      cursor: pointer;
  }
}
</style>
