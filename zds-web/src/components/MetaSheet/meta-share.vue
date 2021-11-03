<template>
  <div class="share-container">
    <div class="category addpeople">
      <div class="icon-section">
        <i class="zeta-icon-addpeople"></i>
      </div>
      <div class="title-text">
        Share with people
      </div>
    </div>
    <div class="header">
      <div class="search" v-if="isAddable">
        <el-autocomplete
        v-model.trim="username"
        class="input-auto"
        :select-when-unmatched="true"
        :trigger-on-focus="false"
        :fetch-suggestions="debounceSearch"
        placeholder="Add email here"
        />
        <el-button type="primary" class="add-btn" :loading="flag.isAdding" @click="addToList()" v-click-metric:ZS_CLICK="{name: 'GRANT_ACCESS'}">Add</el-button>
      </div>
      <div class="mode">
        <el-radio v-model="access" label="Reader">Read Only</el-radio>
        <el-radio v-model="access" label="Writer">Read &amp; Write</el-radio>
        <el-radio v-model="access" label="Admin">Admin</el-radio>
      </div>
    </div>
    <div class="body">
      <div class="row" v-for="user in authInfo" :key="user.email">
        <div class="avatar-info">
          <dss-avatar
            inline
            :nt="user.nt"
            cycle
          />
        </div>
        <div class="user-info">
          <div class="name">{{user.name}}</div>
          <div class="email">{{user.email}}</div>
        </div>
        <div class="mode" :class="{addable: isUserAddable(user.nt)}">
          <el-dropdown v-if="isUserAddable(user.nt)" @command="handleChangeType" trigger="click">
            <span class="el-dropdown-link">
              {{user.access}}<i class="el-icon-arrow-down el-icon--right"></i>
            </span>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item v-for="(option) in accessOptions"
                :command="{user:user,command:option.value}"
                :key="option.value">{{option.name}}
              </el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
          <span v-else>{{user.access}}</span>
        </div>
        <div class="operator" v-if="isUserAddable(user.nt) && user.nt" @click="removeUser(user.nt)">
          <i class="el-icon-close"></i>
        </div>
      </div>
    </div>
    <div class="divider"></div>
    <div class="category share">
      <div class="icon-section">
            <i class="el-icon-link"></i>
      </div>
      <div class="title-text">
          Read-only
      </div>
    </div>
    <div class="share-link">
        <el-input
        ref="input"
        v-model="shareUrl"
        readonly
        class="link">
        <el-button
        slot="append"
        v-click-metric:ZS_CLICK="{name: 'shareLinkcopyClick'}"
        @click="copy"
        >
        Copy link
        </el-button>
        </el-input>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Inject, Prop, Watch } from "vue-property-decorator";
import { AuthorizationUsers, Mode, MetaSheetAccess ,ACCESS, MODE, MSG_TYPE, MetaSheetTableRow } from '@/types/meta-sheet';
import _ from "lodash";
import DoeRemoteService from '@/services/remote/DoeRemoteService';
import MetaSheetService from '@/services/remote/MetaSheetService';
import MetaSheetUtil from './util';
import Util from "../../services/Util.service";
@Component({
  components: {
  }
})
export default class MetaTableShare extends Vue {
  @Prop() mode: Mode; // VIEW or EDIT
  @Prop() sheetId: string;
  @Inject('metaSheetService') metaSheetService: MetaSheetService;
  debounceSearch: Function;
  username = '';
  access: MetaSheetAccess = ACCESS.READER;
  userInfoDic: any = {};
  doeRemoteService = new DoeRemoteService();
  authInfo:any = [];
  flag = {
    isAdding: false
  }

  constructor() {
    super();
    this.debounceSearch = _.debounce(this.querySearchAsync, 300);
  }

  mounted() {
    this.setAuthInfo();
  }

  get shareUrl() {
    return `${location.protocol}//${location.host}${location.pathname}share/#/zeta-sheet?dashboardId=${this.sheetId}`;
  }

  get users() {
    let metaSheetRow:MetaSheetTableRow= this.$store.getters.metasheetById(this.sheetId);
    if(metaSheetRow) {
        return metaSheetRow.authInfo || {};
    }
    return {};
  }

  get isAddable() {
    return this.mode != MODE.VIEW;
  }

  get nt() {
    return Util.getNt();
  }

  // get userList() {
  //     return _.filter(this.authInfo, (user:any) => user.access == this.access);
  // }

  get accessOptions() {
    return [
      {value: 'Reader', name: 'Reader'},
      {value: 'Writer', name: 'Read & Write'},
      {value: 'Admin', name: 'Admin'}
    ]
  }

  copy() {
    const $input = this.$refs.input as any;
    $input.select();
    const result = document.execCommand("copy");
    result && MetaSheetUtil.showMessage(this, MSG_TYPE.SUCCESS, 'Link copied');
  }

  isUserAddable(userNt) {
    var nt = Util.getNt();
    return this.isAddable && userNt != nt;
  }

  setAuthInfo() {
    let authInfo:any = [];
      _.forOwn(this.users, (value, key) => {
        if(key == "OWNERS") {
          let data = _.map(value, v => {
              return {
                ...v,
                access: ACCESS.OWNER
              }
          });
          authInfo = authInfo.concat(data);
        } else if(key == 'WRITERS') {
          let data = _.map(value, v => {
              return {
                ...v,
                access: ACCESS.WRITER
              }
          });
          authInfo = authInfo.concat(data);
        } else if(key == 'READERS') {
          let data = _.map(value, v => {
              return {
                ...v,
                access: ACCESS.READER
              }
          });
          authInfo = authInfo.concat(data);
        }
    });
    this.authInfo = _.orderBy(authInfo, d => d['nt']);
  }

  async handleChangeType(val) {
    if(this.isAddable) {
      let users = _.cloneDeep(this.authInfo);
      let index = _.findIndex(users, (user:any) => user.nt == val.user.nt);
      if(index != -1) {
        users[index] = {
            ...users[index],
            access: val.command
        }
        await this.updateAuthInfo(users, false);
      }
    }
  }

  async querySearchAsync(queryString: string, cb: Function) {
    const str = _.trim(queryString);
    if(str.length === 0) {
      cb([]);
      return;
    }
    const searchList = await this.getEmail();
    const results = queryString ? searchList.filter(this.createStateFilter(str)) : searchList;
    cb(searchList);
  }

  async getEmail(){
    if(_.trim(this.username).length === 0) return [];
    const result = await this.doeRemoteService.getEmail(this.username.split("@")[0]);
    if(result && result.data && result.data.response && result.data.response.docs){
      this.userInfoDic = result.data.response.docs;
      return _.chain(result.data.response.docs).map(item => { return {value: `${item.mail}`};}).value();
    }
    return [];
  }

  getUserOption(users) {
    let param = {
        OWNERS: [],
        WRITERS: [],
        READERS: []
    }
    param.OWNERS = <any>(_.filter(users, (d:any) => d.access == ACCESS.OWNER));
    param.WRITERS = <any>(_.filter(users, (d:any) => d.access == ACCESS.WRITER));
    param.READERS = <any>(_.filter(users, (d:any) => d.access == ACCESS.READER));
    return param;
  }

  createStateFilter(queryString: string) {
    return (searchList: any) => {
      return (searchList.value.toLowerCase().indexOf(queryString.toLowerCase())>-1);
    };
  }

  async addToList() {
    if(!this.flag.isAdding) {
      const email = this.username;
      if(email) {
        let users = _.cloneDeep(this.authInfo);
        let existingUser = _.find(users, (d:any) => d.email === email);
        if(!existingUser) {
          let metasheetRow = this.$store.getters.metasheetById(this.sheetId);
          let selectedUser = _.find(this.userInfoDic, d => d.mail === email);
          if(selectedUser) {
            let {mail, nt, givenName, last_name} = selectedUser;
            let creator = _.find(this.userInfoDic, d => d.nt === (metasheetRow && metasheetRow.nt));
            if(!creator) {
              this.flag.isAdding = true;
              let option: any = {
                email: mail as string,
                name: `${givenName} ${last_name}`,
                nt: nt as string,
                access: this.access
              }
              users.push(option);
              await this.updateAuthInfo(users, true);
              this.flag.isAdding = false;
            } else {
              MetaSheetUtil.showMessage(this, MSG_TYPE.ERROR, `${creator.mail} is already creator`, 3000);
            }
          }
        } else {
          this.flag.isAdding = false;
          MetaSheetUtil.showMessage(this, MSG_TYPE.ERROR, `${existingUser.email} already has ${existingUser.access} role assigned`, 3000);
        }
      }
    }
  }

  async removeUser(nt: string) {
    await this.$confirm('Are you sure to delete?', 'Alert', {
      confirmButtonText: 'Confirm',
      cancelButtonText: 'Cancel',
      type: MSG_TYPE.WARNING
    });
    let users = _.cloneDeep(this.authInfo);
    let index = _.findIndex(users, (user:any) => user.nt == nt);
    if(index != -1) {
      users.splice(index, 1);
    }
    await this.updateAuthInfo(users, false);
  }

  async updateAuthInfo(users, emptyInput = true) {
    let param = this.getUserOption(users);
    let response = await this.metaSheetService.grantUserAccess(this.sheetId, param);
    if(MetaSheetUtil.isOk(response)) {
      this.authInfo = users;
      let newParam = {};
      _.forOwn(param, (v, k) => {
        newParam[k] = [];
        _.each(v, (d:any) => {
            newParam[k].push({
                nt: d.nt,
                email: d.email,
                name: d.name
            })
        });
      });
      this.$store.dispatch('updateMetaSheetUsers', {id: this.sheetId, authInfo: newParam});
      if(emptyInput) {
        this.username = '';
      }
    } else {
      MetaSheetUtil.showMessage(this, MSG_TYPE.ERROR, response.data.msg);
    }
  }

  @Watch('users')
  usersChange(newVal, oldVal) {
    this.setAuthInfo();
  }
}
</script>

<style lang="scss" scoped>
$grey: #C4C4C4;
.divider {
  margin: 10px -12px;
  border-top: 1px solid $grey;
}

.header {
  margin: 10px 0;
  .search {
    display: flex;
    margin-bottom: 10px;
    .add-btn {
      margin-left: 20px;
    }
    .input-auto {
      flex: 1;
    }
  }
}
.title-text {
  font-weight: bold;
  font-size: 14px;
  line-height: 24px;
  letter-spacing: 0.005em;
  color: #000000;
}

.body {
  max-height: 175px;
  overflow-y: auto;
  overflow-x: hidden;
}
.category {
  display: flex;
  // padding: 10px;
  margin-bottom: 15px;
  .icon-section {
    display: flex;
    justify-content: center;
    align-items: center;
    margin-right: 10px;
    width: 35px;
    height: 35px;
    border-radius: 50%;
    background: #569ce1;
    i {
      color: white;
      font-size: 20px;
    }
  }
  .title-text {
    flex: 1;
    align-self: center;
  }

  &.share {
    .icon-section {
      background: rgb(0, 165, 197);
      i {
        transform: rotate(45deg);
        margin-left: -1px;
      }
    }
    .title-text .sub-text {
      // margin-left: 10px;
      color: #B8B8B8;
      font-weight: 400;
    }
  }
}
.row {
  display: flex;
  align-items: center;
  border-top: 1px solid #eee;
  padding: 10px;
  margin-right: -10px;
  .avatar-info {
    display: flex;
    align-items: center;
    margin-right: 15px;
  }
  .user-info {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: center;

    .name {
      color: #333;
      font-weight: 700;
    }
    .email {
      color: #606266;
      font-size: 12px;

    }
  }
  .mode {
    width: 70px;
    text-align: center;
    align-self: center;
    &.addable {
      cursor: pointer;
    }
  }
  .operator {
    width: 70px;
    display: flex;
    justify-content: center;
    align-items: center;
  }

}

</style>
