<template>
  <div class="multi-user-selection">
    <el-select
      v-if="!disabled"
      :value="nt"
      :multiple="true"
      filterable
      remote
      :remote-method="userSearchHandler"
      placeholder="Enter name/NT"
      clearable
      style="width:100%"
      @change="onChange"
      @blur="onBlur"
    >
      <el-option
        v-for="item in options"
        :key="item.nt"
        :label="item.label"
        :value="item.nt"
      />
    </el-select>
      
    <div
      v-for="n in nt"
      v-else
      :key="n"
      style="display:inline-block;"
    >
      <span class="avatar-bg">
        <span
          :style="{backgroundImage:`url(https://ossserver6.dss.vip.ebay.com/userinfo/icon/${n})`}"
          class="avatar"
        />
      </span>
      <span style="margin-right:24px">{{ nameMap[n] || n }}</span>
    </div>
  </div>  
</template>


<script lang="ts">
import { Vue, Component, Prop, Emit, Watch } from 'vue-property-decorator';
import DoeRemoteService from '@/services/remote/DoeRemoteService';
import { attempt, isEmpty } from '@drewxiu/utils';
import { Option } from './UserSelection.vue';
import { matchString } from '../Metadata/utils';
import { uniqBy } from 'lodash';
const api = DoeRemoteService.instance;

@Component({})
export default class MultiUserSelection extends Vue {

  @Prop() nt: string[];
  @Prop() nameMap: { [nt: string]: string };
  @Prop({
    default: () => false,
  }) disabled: boolean;
  @Prop({
    default: () => [],
  }) customOptions: Option[];
  
  options: Option[] = [];

  get selectedOptions (): Option[] {
    return this.nt.map(nt => ({
      nt,
      label: `${this.nameMap[nt]} (${nt})`,
    }));
  }

  userSearchHandler (queryStr: string) {
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
    api.getUser(queryStr).then((res) => {
      this.options = [
        ...this.selectedOptions,
        ...attempt(() => {
          return res.data.response.docs.map(v => ({
            nt: v.nt,
            fullName: `${v.last_name}, ${v.preferred_name}`,
            label:
            (v.last_name ? v.last_name : '') +
            (v.last_name && v.preferred_name ? ',' : '') +
            (v.preferred_name ? v.preferred_name : '') +
            '(' +
            v.nt +
            ')',
          }));
        }, [])];
    });
  }
  resetOptions () {
    this.options = uniqBy([...this.selectedOptions, ...this.customOptions], 'nt');
  }

  @Watch('customOptions')
  initOptions () {
    this.resetOptions();
  }

  @Emit('change')
  onChange (val) {
    return val.map(nt => {
      return {
        nt,
        fullName: this.nameMap[nt] || this.options.find(i => i.nt === nt)!.fullName,
      };
    });
  }
  onBlur () {
    this.$parent.$emit('el.form.blur'); // simulate validate event on ElFromItem
  }
  created () {
    this.resetOptions();
  }
}

</script>

<style lang="scss" scoped>
.multi-user-selection {
  display: inline-block;
  width: 100%;
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



