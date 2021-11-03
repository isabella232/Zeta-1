<template>
  <div class="share-container">
    <div class="category addpeople">
      <div class="icon-section">
        <i class="zeta-icon-addpeople" />
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
        <el-radio v-model="access" label="Editor">Editor</el-radio>
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
          <div class="name">{{ user.name }}</div>
          <div class="email">{{ user.email }}</div>
        </div>
        <div class="mode">
          <span>{{ access }}</span>
        </div>
        <div class="operator" v-if="isAddable" @click="removeUser(user.nt)" >
          <i class="el-icon-close" />
        </div>
      </div>
    </div>
    <div class="divider" />
    <div class="category share">
      <div class="icon-section">
        <i class="el-icon-link" />
      </div>
      <div class="title-text">
        Share Link
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
import { Component, Vue, Inject, Prop, Watch } from 'vue-property-decorator';
import _ from 'lodash';
import DoeRemoteService from '@/services/remote/DoeRemoteService';
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import Util from '@/services/Util.service';
import { ScheduleConfig } from '@/components/common/schedule-container';

@Component({
  components: {

  }
})
export default class ScheduleShare extends Vue {
  @Prop() task: ScheduleConfig;
  @Inject('notebookRemoteService') notebookRemoteService: NotebookRemoteService;
  debounceSearch: Function;
  username = '';
  access = 'Editor';
  userInfoDic: any = {};
  doeRemoteService = new DoeRemoteService();
  authInfo: any = [];
  flag = {
    isAdding: false,
  };

  constructor() {
    super();
    this.debounceSearch = _.debounce(this.querySearchAsync, 300);
  }
  mounted() {
    this.setAuthInfo();
  }
  get isShared(){
    return Util.isShareApp();
  }
  get shareUrl() {
    return `${location.protocol}//${location.host}/${Util.getPath()}share/#/scheduleDetail/${this.task.id}`;
  }

  get users() {
    if(this.task) {
      return this.task.authInfo || {};
    }
    return {};
  }

  get isAddable() {
    if(this.isShared){
      return false;
    }
    if(this.task.nt === this.nt){
      return true;
    }
    const index = _.find(this.users.WRITERS,(item)=> item.nt === this.nt);
    return index ? true:false;
  }

  get nt() {
    return Util.getNt();
  }

  copy() {
    const $input = this.$refs.input as any;
    $input.select();
    const result = document.execCommand('copy');
    if(result){
      this.$message({
        message: 'Link copied',
        type: 'success',
      });
    }
  }

  setAuthInfo() {
    let authInfo: any = [];
    _.forOwn(this.users, (value, key) => {
      if(key == 'WRITERS') {
        const data = _.map(value, v => {
          return {
            ...v
          };
        });
        authInfo = authInfo.concat(data);
      }
    });
    this.authInfo = _.orderBy(authInfo, d => d['nt']);
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
    const result = await this.doeRemoteService.getEmail(this.username.split('@')[0]);
    if(result && result.data && result.data.response && result.data.response.docs){
      this.userInfoDic = result.data.response.docs;
      return _.chain(result.data.response.docs).map(item => { return {value: `${item.mail}`};}).value();
    }
    return [];
  }

  getUserOption(users) {
    const param = {
      OWNERS: [],
      READERS: [],
      WRITERS: users,
    };
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
        const users = _.cloneDeep(this.authInfo);
        const existingUser = _.find(users, (d: any) => d.email === email);
        if(!existingUser) {
          const taskRow = this.task;
          const selectedUser = _.find(this.userInfoDic, d => d.mail === email);
          if(selectedUser) {
            const {mail, nt, givenName, last_name} = selectedUser;
            const creator = _.find(this.userInfoDic, d => d.nt === (taskRow && taskRow.nt));
            if(!creator) {
              this.flag.isAdding = true;
              const option: any = {
                email: mail as string,
                name: `${givenName} ${last_name}`,
                nt: nt as string
              };
              users.push(option);
              await this.updateAuthInfo(users, true);
              this.flag.isAdding = false;
            } else {
              this.$message({
                message: `${creator.mail} is already creator`,
                type: 'error',
              });
            }
          }
        } else {
          this.flag.isAdding = false;
          this.$message({
            message: `${existingUser.email} already has ${this.access} role assigned`,
            type:'error',
          });
        }
      }
    }
  }

  async removeUser(nt: string) {
    await this.$confirm('Are you sure to delete?','Alert');
    const users = _.cloneDeep(this.authInfo);
    const index = _.findIndex(users, (user: any) => user.nt == nt);
    if(index != -1) {
      users.splice(index, 1);
    }
    await this.updateAuthInfo(users, false);
  }

  async updateAuthInfo(users, emptyInput = true) {
    const param = this.getUserOption(users);
    const id = this.task.id as number;
    const response = await this.notebookRemoteService.grantUserAccess(id, param);
    if(response && response.data) {
      this.authInfo = users;
      const newParam = {};
      _.forOwn(param, (v, k) => {
        newParam[k] = [];
        _.each(v, (d: any) => {
          newParam[k].push({
            nt: d.nt,
            email: d.email,
            name: d.name,
          })
        });
      });
      this.$store.dispatch('updateScheduleAccess', {id: id, authInfo: newParam});
      if(!this.has){
        this.$store.dispatch('deleteSchedule', id);
      }
      if(emptyInput) {
        this.username = '';
      }
    } else {
      this.$message({
        message: response.data.msg,
        type: 'error',
      });
    }
  }

  has(){
    if(this.task.nt === this.nt){
      return true;
    }
    const index = _.find(this.users.WRITERS,(item)=> item.nt === this.nt);
    return index ? true:false;
  }

  @Watch('users')
  usersChange() {
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
