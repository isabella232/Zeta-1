/* eslint-disable @typescript-eslint/no-unused-vars */
<template>
  <div>
    <div class="head-div">
      <p class="dataSet-label">
        Owner
      </p>
      <el-popover
        placement="bottom"
        width="400"
        trigger="click"
      >
        <div>
          <el-autocomplete
            v-model="newOwner"
            class="inline-input"
            style="width: 400px;"
            :fetch-suggestions="userSearchHandler"
            :trigger-on-focus="false"
            :debounce="debounce"
            placeholder="User Name / NT Login"
            @select="handleSelect"
            @input="handleInput"
          >
            <el-button
              slot="append"
              @click="addOwner"
            >
              Add
            </el-button>
          </el-autocomplete>
        </div>
        <el-button
          v-show="isOwner && isEdit"
          slot="reference"
          type="text"
          class="owner-btn"
          title="Input User Name or NT login"
        >
          <i class="zeta-icon-add" />
        </el-button>
      </el-popover>
    </div>
    <div style="max-height: 235px; overflow-y: auto;">
      <div
        v-for="user in ownerArr"
        :key="user.nt"
        class="user-item-div"
      >
        <div class="nav-item-icon">
          <!--span v-bind:style="{backgroundImage:'url(./img/icons/user_white.png)'}" class="avatar-bg">
                        <span v-bind:style="{backgroundImage:`url(//ihub.corp.ebay.com/images/ldap/${user.nt}.jpg)`}" class="avatar"></span>
                    </span>
                    <span style="margin-left: 5px;">{{ user.name }}</span-->
          <span class="avatar-bg">
            <dss-avatar
              inline
              size="small"
              cycle
              :nt="user.nt"
              class="avatar"
            />
          </span>
          {{ user.name }}
        </div>
        <span
          v-show="isOwner && isEdit"
          class="owner-btn"
        >
          <el-popover
            v-model="user.visible"
            placement="top"
            width="180"
          >
            <p>Are you sure to delete this?</p>
            <div style="text-align: right; margin: 0; margin-top:5px;">
              <el-button
                size="mini"
                type="text"
                @click="user.visible = false"
              >cancel</el-button>
              <el-button
                type="primary"
                size="mini"
                @click="removeOwner(user.nt)"
              >confirm</el-button>
            </div>
            <el-button
              slot="reference"
              type="text"
              class="owner-btn"
              title="Remove Owner"
              @click="user.visible = true"
            >
              <i class="el-icon-minus" />
            </el-button>
          </el-popover>
        </span>
      </div>
      <div
        v-if="!hasOwner"
        class="no-data-div"
      >
        No content yet
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Inject, Watch } from 'vue-property-decorator';
import DoeRemoteService from '@/services/remote/DoeRemoteService';
import _ from 'lodash';

@Component({
  components: {
    
  },
})
export default class DatasetsOwner extends Vue {
  @Inject('doeRemoteService')
  doeRemoteService: DoeRemoteService;

  @Prop() isOwner: any;
  @Prop() domain: any;
  @Prop() subDomain: any;
  @Prop() owner: any;
  @Prop() isEdit: any;

  debounce = 500;
  // search func
  userArr: any = [];

  // owner func
  newOwner = '';
  newOwnerNt = '';
  ownerArr: any = this.owner;

  get hasOwner () {
    return _.size(this.ownerArr) > 0;
  }

  userSearchHandler (queryStr: string, cb: any) {
    this.doeRemoteService.getUser(queryStr).then((res: any) => {
      const opntions: Array<any> = [];
      if (res && res.data && res.data.response && res.data.response.docs) {
        _.forEach(res.data.response.docs, (v: any) => {
          const option: any = {
            nt: v.nt,
            value: (v.last_name ? v.last_name : '') + 
                    (v.last_name && v.preferred_name ? ',' : '') + 
                    (v.preferred_name ? v.preferred_name : '') + 
                    '(' + v.nt + ')',
            name: (v.last_name ? v.last_name : '') + 
                    (v.last_name && v.preferred_name ? ',' : '') + 
                    (v.preferred_name ? v.preferred_name : ''),
          };

          opntions.push(option);
        });
      }

      this.userArr = opntions;
      cb(this.userArr);
    }).catch((err: any) => {
      console.error('user search failed: ' + JSON.stringify(err));
      cb([]);
    });
  }

  // owner func
  handleInput () {
    this.newOwnerNt = '';
  }
        
  handleSelect (item: any) {
    this.newOwnerNt = item.nt;
  }

  addOwner () {
    if (_.isEmpty(this.newOwnerNt)) {
      this.$message.error('Please select owner');
      return;
    }
    const findIndex = _.findIndex(this.ownerArr, (v: any) => { return v.nt == this.newOwnerNt; });
    if (findIndex < 0) {
      const user: any = {nt: this.newOwnerNt, name: this.newOwner};
      this.ownerArr.push(user);
      const conf: any = { owner: JSON.stringify(this.ownerArr) };
      this.$emit('submit-conf', conf);
      const params: any = {
        action: 'addowner',
        to: this.newOwner,
        nts: [this.newOwnerNt],
      };
      this.$emit('send-email', params);
    }
    this.newOwnerNt = '';
    this.newOwner = '';
  }

  removeOwner (nt: any) {
    const ownerArr = _.cloneDeep(this.ownerArr);
    const find = _.find(this.ownerArr, (v: any) => { return v.nt == nt; });
    if (find) {
      const removeNt = find.nt;
      const removeName = find.name;
      _.remove(ownerArr, (v: any) => { return v.nt == nt; });
      this.ownerArr = ownerArr;
      const conf: any = { owner: JSON.stringify(this.ownerArr) };
      this.$emit('submit-conf', conf);
      const params: any = {
        action: 'removeowner',
        to: removeName,
        nts: [removeNt],
      };
      this.$emit('send-email', params);
    }
  }

  @Watch('owner')
  onOwnerChange () {
    this.ownerArr = this.owner;
  }
}
</script>
<style lang="scss" scoped>
.head-div {
  border-bottom: 1px solid #E4E7ED;
  display: flex;
  padding-bottom: 10px;
}
.user-item-div {
  margin: 10px;
  margin-right: 2px;
}
.owner-btn {
  float: right;
  line-height: 30px;
}
.no-data-div {
  line-height: 60px;
  color: #CACBCF;
  padding-left: 10px;
}
.dataSet-label {
  font-weight: 700;
  font-size: 16px;
  color: #999999;
  width: 100%;
  padding: 0 10px;
  height: 30px;
  line-height: 30px;
}
.nav-item-icon {
  display: inline-flex;
  align-items: center;
  height: 33px;
  line-height: 33px;
  [class^="zeta-icon-"],
  [class*=" zeta-icon-"] {
      font-size: 24px;
      color: #94bfe1;
  }
  .avatar-bg {
      border-radius: 15px;
      display: block;
      background-position: center;
      width: 30px;
      height: 30px;
      background-size: 30px 30px;
  }
  .avatar {
      border-radius: 15px;
      display: block;
      background-position: center;
      width: 30px;
      height: 30px;
      background-size: 30px 40px;
  }
}
</style>
