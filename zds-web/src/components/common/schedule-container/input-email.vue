<template>
  <div class="input-emial">
    <div class="row">
      <div class="row-name">
        Additional Audience
      </div>
      <div class="row-info">
        <el-autocomplete
          v-model.trim="inputValue"
          class="input-auto"
          :select-when-unmatched="true"
          :trigger-on-focus="false"
          :fetch-suggestions="querySearchAsync"
          placeholder="Add Email address here"
          :disabled="!mailSwitch||isShared"
          @select="handleSelect"
        />
      </div>
    </div>
    <div v-if="mailSwitch" class="row row-tag">
      <div class="row-name" />
      <div class="row-info">
        <el-tag
          v-for="(tag,index) in emailList"
          :key="index"
          closable
          :disable-transitions="false"
          @close="handleClose(tag)"
        >
          {{ tag }}
        </el-tag>
      </div>
    </div>
  </div>
</template>
<script lang="ts">
import { Vue, Prop, Watch, Emit, Component } from 'vue-property-decorator';
import DoeRemoteService from '@/services/remote/DoeRemoteService';
import _ from 'lodash';
import Util from '@/services/Util.service';

@Component({
  components: {

  },
})
export default class InputEmail extends Vue {
  doeRemoteService = new DoeRemoteService();
  @Prop()
  value: string[] | null;
  @Prop()
  mailSwitch: boolean;

  inputValue = '';
  emailList: Array<string> = [];
  mounted () {
    this.emailList = this.value? this.value : [];
  }
  @Emit('input')
  // eslint-disable-next-line @typescript-eslint/no-empty-function
  onInputChange (result: string[]){
  }
  handleClose (email: string) {
    if (this.isShared)  return;
    this.emailList.splice(this.emailList.indexOf(email), 1);
  }
  handleSelect (item: any){
    this.inputValue = '';
    if (!item.value) return;
    this.emailList.push(item.value);
  }
  async querySearchAsync (queryString: string, cb: Function) {
    const str = _.trim(queryString);
    if (str.length === 0) {
      cb([]);
      return;
    }
    const searchList = await this.getEmail();
    const results = queryString ? searchList.filter(this.createStateFilter(str)) : searchList;

    cb(results);
  }
  createStateFilter (queryString: string) {
    return (searchList: any) => {
      return (searchList.value.toLowerCase().indexOf(queryString.toLowerCase())>-1);
    };
  }
  async getEmail (){
    if (_.trim(this.inputValue).length === 0) return [];
    const result = await this.doeRemoteService.getEmail(this.inputValue);
    if (result && result.data && result.data.response && result.data.response.docs){
      return _.chain(result.data.response.docs).map(item => { return {value: item.mail};}).value();
    }
    return [];
  }
  get isShared (){
    return Util.isShareApp();
  }
  @Watch('emailList')
  handleEmailChange (val: Array<string>){
    this.onInputChange(val);
  }
}
</script>
<style lang="scss" scoped>
    .input-auto{
        width: 100%;
    }
    .row-tag{
        height: auto !important;
        .el-tag{
            line-height: 20px;
            height: 22px;
            margin-right: 3px;
            margin-bottom: 3px;
            padding: 0 5px;
            /deep/ .el-icon-close{
                top: 0;
            }
        }
        .row-info{
            flex-wrap: wrap;
        }
    }
</style>
