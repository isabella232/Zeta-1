<template>
  <div class="suggested-tab-wrapper">
    <div class="tab-nav">
      <ul>
        <li 
          v-click-metric:ACE_CLICK="{name: 'tabName => Table'}"
          :class="{active: activeName === 'Table'}"
          @click="changeTab('Table')"
        >
          <el-badge
            :value="resultHTML.table.length"
            :max="10"
            :hidden="!resultHTML.table.length"
            class="item-count"
            type="primary"
          >Table</el-badge>
        </li>
        <li 
          v-click-metric:ACE_CLICK="{name: 'tabName => FAQ'}"
          :class="{active: activeName === 'FAQ'}"
          @click="changeTab('FAQ')"
        >
          <el-badge
            :value="resultHTML.faq.length"
            :max="10"
            :hidden="!resultHTML.faq.length"
            class="item-count"
            type="primary"
          >FAQ</el-badge>
        </li>
        <li 
          v-click-metric:ACE_CLICK="{name: 'tabName => UDF'}"
          :class="{active: activeName === 'UDF'}"
          @click="changeTab('UDF')"
        >
          <el-badge
            :value="resultHTML.udf.length"
            :max="10"
            :hidden="!resultHTML.udf.length"
            class="item-count"
            type="primary"
          >UDF</el-badge>
        </li>
        <li 
          v-click-metric:ACE_CLICK="{name: 'tabName => Notebook'}"
          :class="{active: activeName === 'Notebook'}"
          @click="changeTab('Notebook')"
        >
          <el-badge
            :value="resultHTML.notebook.length"
            :max="10"
            :hidden="!resultHTML.notebook.length"
            class="item-count"
            type="primary"
          >Notebook</el-badge>
        </li>
        <li 
          v-click-metric:ACE_CLICK="{name: 'tabName => SparkSQL'}"
          :class="{active: activeName === 'SparkSQL'}"
          @click="changeTab('SparkSQL')"
        >SparkSQL</li>
      </ul>
    </div>
    <div class="tab-content">
      <div v-show="activeName === 'Table' && resultHTML.table.length>0" class="tab-table">
        <li
          v-for="(item, index) in resultHTML.table"
          :key="index"
        >
          <div class="title-content">
            <span
              v-click-metric:ACE_CLICK="{name: 'getAnswer => '+item.searchType}"
              class="title" 
              @click="clickHint(item)"
            >{{ item.name }}</span>
            <i class="zeta-icon-export" @click="openNewTab(item)" />
          </div>
          <div class="platform">
            <em>Platform:</em>
            <p><i v-for="p in item.platform" :key="p" class="platform-icon workspace-icon " :class="getIconClass(p)" /></p>
          </div>
          <div class="desc"><em>Description:</em><p>{{ item.desc }}</p></div>
        </li>
      </div>
      <div v-show="activeName === 'FAQ' && resultHTML.faq.length>0" v-loading="faqLoading" class="faq">
        <li
          v-for="(item, index) in faq"
          :key="index"
        >
          <span 
            v-click-metric:ACE_CLICK="{name: 'getAnswer => '+item.searchType}"
            class="title"
            @click="clickHint(item)"
          >{{ item.title }}</span>
          <div class="answers">
            <em>Answer:</em> <p v-html="item.answer?item.answer.comment:''"></p>
          </div>
          <div v-if="item.tags.length>0" class="tags">
            <i v-for="tag in item.tags" :key="tag.id">#{{ tag.name }}</i>
          </div>
        </li>
      </div>
      <div v-show="activeName === 'UDF' && resultHTML.udf.length>0" class="udf">
        <li
          v-for="(item, index) in resultHTML.udf"
          :key="index"
        >
          <span 
            v-click-metric:ACE_CLICK="{name: 'getAnswer => '+item.searchType}"
            class="title"
            @click="clickHint(item)"
          >{{ item.name }}</span>
          <div class="platform">
            <em>dbName:</em>
            <p>{{ item.dbName?item.dbName.replace(/[\r\n]/g,""):'' }}</p>
          </div>
          <div class="desc udf-desc"><em>Description:</em><p>{{ item.desc }}</p></div>
          <div class="desc udf-desc"><em>Example:</em><p>{{ item.example }}</p></div>
        </li>
      </div>
      <div v-show="activeName === 'Notebook' && resultHTML.notebook.length>0" v-loading="notebookLoading" class="notebook">
        <li
          v-for="(item, index) in notebookResult"
          :key="index"
        >
          <div class="title-content">
            <span 
              v-click-metric:ACE_CLICK="{name: 'getAnswer => '+item.searchType}"
              class="title"
              @click="onClickHandler(item)"
            >{{ item.name }}</span><i class="zeta-icon-export" @click="onClickHandler(item)" />
          </div>
          <div class="item"><em>Owner:</em><p>{{ item.owner }}</p></div>
          <div class="item"><em>Updated:</em><p v-if="item.lastUpdateTime">{{ formatTime(item.lastUpdateTime) }}</p></div>
          <div class="item"><em>LastRun:</em><p v-if="item.lastRunTime">{{ formatTime(item.lastRunTime) }}</p></div>
          <div class="item"><em>Executed:</em><p>{{ item.executedCnt }} Times</p></div>
          <div class="item platform">
            <em>Platform:</em>
            <p><i class="platform-icon workspace-icon " :class="getIconClass(item.platform)" /></p>
          </div>
          <div class="desc query-desc"><em>Description:</em><p>{{ item.desc }}</p></div>
        </li>
      </div>
      <div v-show="activeName === 'SparkSQL'" class="spark" v-loading="loading">
        <ul>
          <li v-for="s in googleResult" :key="s.url"  v-click-metric:ACE_CLICK="{name: 'getAnswer => SparkSQL'}">
            <a :href="s.url" target="_blank"><p v-html="s.title" /></a>
            <p v-html="s.brief" />
          </li>
        </ul>
      </div>
      <div class="no-data" v-if="resultHTML[convertName(activeName)].length === 0">
        No results for your inquiry.
      </div>
    </div>
    <!-- <div class="action">
      <span>Was this session helpful?</span>
      <span
        v-click-metric:ACE_CLICK="{name: 'helpful => yes'}"
      >Yes<i class="zeta-icon-thumb-up" />
      </span>
      <span
        v-click-metric:ACE_CLICK="{name: 'helpful => no'}"
      >No<i class="zeta-icon-thumb-down" />
      </span>
    </div> -->
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Provide, Inject } from 'vue-property-decorator';
import ZetaAceRemoteService from '@/services/remote/ZetaAce';
import _ from 'lodash';
import { IFile } from '@/types/repository';
import { WorkspaceManager } from '@/types/workspace/workspace-manager';
import { INotebook, IPreference} from '@/types/workspace';
import { INotebookMapper } from '@/services/mapper';
import { WorkspaceSrv as NBSrv, WorkspaceSrv } from '@/services/Workspace.service';
import Util from '@/services/Util.service';
import moment from 'moment';
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import { getPlatformIconClass, openTab, openNotebookTab, parseAcceptedAnswer } from '../utilties';
import { ZetaException } from '@/types/exception';
@Component({
  components: {}
})
export default class SuggestedTabView extends Vue {
  @Provide('notebookRemoteService')
  notebookRemoteService = new NotebookRemoteService();
  @Inject('zetaAceRemoteService') 
  zetaAceRemoteService: ZetaAceRemoteService;
  @Prop() resultHTML: Dict<any>;
  activeName = 'Table';
  googleResult = [];
  notebookResult = [];
  notebookLoading = false;
  loading = false;
  faqLoading = false;
  faq: Array<any> = [];

  get tags(){
    return this.$store.state.zetaAceStore.tags;
  }
  clickHint(item: any){
    this.$emit('click-item', item);
  }
  onClickHandler(item: any) {
    openNotebookTab(item);
  }
  async changeTab(name: string){
    this.activeName = name;
    if(this.activeName === 'SparkSQL' && this.googleResult.length ===0){
      this.loading = true;
      this.googleResult = await this.googleSearch(this.resultHTML.sql[0].name);
      this.loading = false;
    }
    if(this.activeName === 'FAQ' && this.resultHTML.faq.length>0 && this.faq.length === 0){
      this.faqLoading = true;
      await this.getQuestions(this.resultHTML.faq);
      this.faqLoading = false;
    }
    if(this.activeName === 'Notebook' && this.notebookResult.length === 0){
      this.notebookLoading = true;
      const ids = _.chain(this.resultHTML.notebook).map(n => {return n.id; }).value().join(',');
      this.notebookResult = await this.getSummary(ids);
      this.notebookLoading = false;
    }
  }
  async getQuestions(faq: Array<any>){
    const qids = _.chain(faq).map(q => {return q.questionId; }).value().toString();
    const getQuestions = this.zetaAceRemoteService.getQuestionsByIds(qids);
    const getTags = this.zetaAceRemoteService.getTagsByQids(qids);
    const result = await Promise.all([getQuestions, getTags]);
    this.faq = result[0].data;
    const tags = result[1].data;
    this.faq.map((q: any) =>{
      q.searchType = 'FAQ',
      q.name = q.title,
      q.isActive = false;
      q.tags = tags[q.id]?tags[q.id]:[];
      q.answer = parseAcceptedAnswer(q);
    });
    this.faq.sort((a: any, b: any)=>{
      return qids.indexOf(a.id)-qids.indexOf(b.id);
    });
  }
  async googleSearch(query: string) {
    try {
      const res = await this.zetaAceRemoteService.googleSearch(query);
      return res.data.results;
    } catch (err) {
      console.error(err);
      return [];
    }
  }
  parsePlatform(preference: string ){
    let platform = '';
    const p = preference ? JSON.parse(preference) : null;
    const connection = p ? p['notebook.connection'] : null;
    if(connection){
      platform = connection['alias'] || ( Util.getDefaultConnectionByPlat(connection['codeType']).defaultConnection.alias );
    }
    return platform;
  }
  async getSummary(ids: string){
    try{
      const result = await this.notebookRemoteService.getSummaryByIds(ids);
      return _.chain(result.data).sortBy('executedCnt').map(nb => {
        return {
          id: nb.id,
          name: nb.title,
          owner: nb.nt,
          platform: this.parsePlatform(nb.preference), 
          lastRunTime: nb.lastRunDt,
          lastUpdateTime: nb.lastUpdateDt,
          executedCnt: nb.executedCnt,
          favoriteCnt: nb.favoriteCnt,
          desc: nb.desc,
          type: nb.type,
          searchType: 'Notebook'
        };
      }).value().reverse();
    } catch(err){
      err.resolve();
      console.error(err);
      return this.resultHTML.notebook;
    }
  }
  formatTime(time: string){
    return moment(time).format('YYYY-MM-DD HH:mm');
  }
  convertName(name: string){
    return name === 'SparkSQL'? 'sql': name.toLowerCase();
  }
  getIconClass(platform: string){
    return getPlatformIconClass(platform);
  }
  openNewTab(meta: any){
    openTab(meta);
  }
}
</script>
<style lang="scss" scoped>
@import '@/styles/global.scss';
  .suggested-tab-wrapper{
    margin-top: -5px;
  }
  .tab-nav{
    margin-bottom: 10px;
    ul {
      display: flex;
      height: 30px;
      line-height: 30px;
      list-style-type: none;
      width: 100%;
      border-bottom: 1px solid #cacbcf;
      box-sizing: border-box;
      > li {
        align-items: center;
        columns: #333333;
        cursor: pointer;
        display: flex;
        font-size: 13px;
        padding: 0 10px !important;
        box-sizing: border-box;
        &:first-child{
          padding-left: 0 !important;
        }
        &.active {
          border-bottom: 1px solid #569ce1;
          background-color: #fff;
          color:$zeta-global-color;
          margin-bottom: -1px;
          .el-badge{
            color:$zeta-global-color;
          }
        }
        &:hover{
          color:#569CE1;
        }
        /deep/ .el-badge{
            color: #333333;
          &.item-count{
            .el-badge__content{
              padding: 0 5px;
              height: 14px;
              line-height: 14px;
              font-size: 10px;
              top: 7px;
            }
          }
        }
      }
    }
  }
  .tab-content{
    height: 180px;
    overflow-y: auto;
    .spark{
      height: 100%;
      li{
        a{
          font-size: 14px;
          font-weight: bold;
        }
      }
    }
    .platform,.desc, .item{
      display: grid;
      grid-template-columns: 90px auto;
      line-height: 16px;
      padding-top: 5px;
      color: #999999;
      word-break: break-word;
      .platform-icon{
        margin-right: 2px;
        padding: 1px 2px;
        opacity: 1;
      }
    }
    .desc{
      >p{
        overflow: hidden;
        text-overflow: ellipsis;
        display: -webkit-box;
        -webkit-line-clamp: 3;
        -webkit-box-orient: vertical;
      }
    }
    .udf-desc{
      >p{
        display: inline-block;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }
    .faq{
      height: 100%;
      .title{
        overflow: hidden;
        text-overflow: ellipsis;
        display: inline-block;
        white-space: nowrap;
        max-width: 100%;
      }
      .answers{
        color: #999999;
        display: grid;
        grid-template-columns: 60px auto;
        >p{
          overflow: hidden;
          text-overflow: ellipsis;
          display: -webkit-box;
          -webkit-line-clamp: 3;
          -webkit-box-orient: vertical;
          max-height: 54px;
          /deep/ img{
            max-width: 100%;
          }
        }
      }
      .collapse-btn{
        cursor: pointer;
        margin-left: 5px;
        &:hover{
          color: #569ce1;
        }
      }
      .tags{
        margin-top: 5px;
        i{
          font-style: normal;
          color: #569ce1;
          margin-right: 10px;
        }
      }
    }
    .notebook{
      height: 100%;
      .title-content{
        display: flex;
        align-items: center;
        .title{
          display: inline-block;
          overflow: hidden;
          text-overflow: ellipsis;
          max-width: calc(100% - 30px);
          white-space: nowrap;
        }
      }
    }
    .tab-table{
      .title-content{
        display: flex;
        align-items: center;
      }
    }
    i.zeta-icon-export{
      color: #999999;
      margin-left: 10px;
      font-size: 14px;
      font-weight: normal;
      cursor: pointer;
      &:hover{
        color: #569ce1;
      }
    }
  }
  .action{
    display: flex;
    align-items: center;
    justify-content: flex-end;
    color: #999;
    padding-bottom: 10px;
    >span{
        padding-right: 10px;
        display: flex;
        align-items: center;
        cursor: pointer;
        &:first-child{
            margin-right: 10px;
        }
        &.like-on{
            i{
                color: #569ce1;
                animation: myfirst 0.3s;
            }
            @keyframes myfirst
            {
                0% {font-size: 18px;}
                100% {font-size: 20px;}
            }
        }
        i{
            font-size: 18px;
            color: #999;
            margin-left: 2px;
            &:hover{
              color: #569ce1;
            }
        }
    }
  }
  .no-data{
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #999999;
  }
</style>
