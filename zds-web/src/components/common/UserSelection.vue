<template>
  <div class="user-selection">
    <span class="avatar-bg" :class="{ invisible: !nt }">
      <span
        :style="{ backgroundImage: `url(https://ossserver6.dss.vip.ebay.com/userinfo/icon/${nt})` }"
        class="avatar"
      />
    </span>
    <el-select
      v-if="!disabled"
      v-model="editNT"
      filterable
      remote
      :remote-method="userSearchHandler"
      reserve-keyword
      placeholder="Enter name/NT"
      clearable
      @change="onChange"
      @blur="onBlur"
    >
      <el-option v-for="item in options" :key="item.nt" :label="item.label" :value="item.nt" />
    </el-select>
    <span v-else>{{ name || displayName || nt }}</span>
  </div>
</template>

<script lang="ts">
import { Vue, Component, Prop, Emit, Watch } from 'vue-property-decorator';
import DoeRemoteService from '@/services/remote/DoeRemoteService';
import { attempt, isEmpty } from '@drewxiu/utils';
import { matchString } from '../Metadata/utils';
import { Action, State } from 'vuex-class';
import UserSelectionStore, { Actions } from '@/stores/modules/UserSelectionStore';
const api = DoeRemoteService.instance;

export interface Option {
  nt: string;
  fullName?: string;
  label: string;
}

@Component({})
export default class UserSelection extends Vue {
  @Prop() nt: string;
  @Prop() name: string;
  @Prop({
    default: () => false,
    type: Boolean
  })
  disabled: boolean;
  @Prop() customOptions: Option[];
  options: Option[] = [];
  editNT: string;

  @Action(Actions.getUserInfo) getUserInfo;
  @State(state => state.UserSelection.userInfo || {}) userInfo;

  @Watch('nt', { immediate: true })
  getName(nt) {
    if (!nt) {
      return;
    } else {
      if (!this.userInfo[nt]) {
        this.getUserInfo(nt);
      }
    }
  }

  get displayName() {
    let { nt, userInfo } = this;
    if (userInfo[nt]) {
      return `${userInfo[nt].preferred_name}, ${userInfo[nt].last_name}`;
    }
  }
  userSearchHandler(queryStr: string) {
    queryStr = queryStr.trim();
    if (!queryStr) {
      this.resetOptions();
      return;
    }
    if (!isEmpty(this.customOptions)) {
      this.options = this.customOptions.filter(o => {
        return matchString(queryStr, o.nt) || matchString(queryStr, o.fullName) || matchString(queryStr, o.label);
      });
      return;
    }
    api.getUser(queryStr).then(res => {
      this.options = attempt(() => {
        return res.data.response.docs.map(v => ({
          nt: v.nt,
          fullName: `${v.last_name}, ${v.preferred_name}`,
          label:
            (v.last_name ? v.last_name : '') +
            (v.last_name && v.preferred_name ? ',' : '') +
            (v.preferred_name ? v.preferred_name : '') +
            '(' +
            v.nt +
            ')'
        }));
      }, []);
    });
  }
  @Watch('customOptions')
  resetOptions() {
    this.options = this.customOptions || [];
  }
  @Emit('change')
  onChange(val) {
    return this.options.find(o => o.nt === val);
  }
  onBlur() {
    this.$parent.$emit('el.form.blur'); // simulate validate event on ElFromItem
  }
  created() {
    this.editNT = this.nt;
  }
  beforeCreate() {
    if (!this.$store.state[UserSelectionStore.namespace]) {
      this.$store.registerModule(UserSelectionStore.namespace, UserSelectionStore);
    }
  }
  updated() {
    this.editNT = this.nt;
  }
}
</script>

<style lang="scss" scoped>
.user-selection {
  display: inline-block;

  .avatar-bg {
    background-image: url(/zeta/img/icons/user_white.png);
    background-color: #ccc;
    vertical-align: middle;
    background-size: 30px 30px;
    display: inline-block;
    height: 30px;
    margin-right: 5px;
    width: 30px;
    background-position: 50% center;
    border-radius: 15px;
    .avatar {
      background-size: 40px 40px;
      display: block;
      height: 30px;
      width: 30px;
      background-position: 50% center;
      border-radius: 15px;
      background-color: #ccc;
    }

    &.invisible {
      display: none;
    }
  }
}
</style>
