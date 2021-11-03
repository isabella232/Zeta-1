<template>
  <div v-loading.lock="loading" class="faq">
    <div class="toolbar">
      <div class="left">
        <div class="search">
          <div class="filters">
            <span>Submitted by</span>
            <el-select v-model="scopeValue">
              <el-option
                v-for="item in scoped"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
            <div class="tags">
              <span>Tag</span>
              <el-autocomplete
                ref="tag-input"
                v-model="tagValue"
                :popper-class="tagValue == '' ? 'recommend-css' : ''"
                placeholder="search tag"
                clearable
                :fetch-suggestions="searchTags"
                :debounce="debounce"
                @select="handleSelectedTag"
              />
            </div>
            <div class="date">
              <span>Order by</span>
              <el-select v-model="sortType">
                <el-option
                  v-for="item in SortType"
                  :key="item"
                  :label="item"
                  :value="item">
                </el-option>
              </el-select>
            </div>
          </div>
          <div class="input-box">
            <i class="el-icon-search" />
            <input
              v-model.trim="searchKey"
              class="search-input"
              placeholder="search question"
              type="text"
              maxlength="200"
              @keyup.enter="search"
            />
          </div>
        </div>
      </div>
      <div class="right">
        <el-button class="create-button" type="primary" @click="visible = true">Ask Question</el-button>
      </div>
    </div>
    <div v-if="questionList.length>0" class="list">
      <div ref="list" class="list-scroll" style="height: calc(100vh - 135px)">
        <div class="content" v-for="item in questionList" :key="item.id">
          <el-row>
            <el-col :span="2" class="left">
              <div class="statscontainer">
                <div class="stats">
                  <div class="vote">
                    <div class="votes">
                      <div class="vote-count-post viewcount">{{ item.totalPostLike }}</div>
                      <div>upvotes</div>
                    </div>
                  </div>
                  <div class="status answered-accepted">
                    <div class="answers-count-post viewcount">{{ item.totalLevel1Post }}</div>
                    <div>answers</div>
                  </div>
                </div>
              </div>
            </el-col>
            <el-col :span="22">
              <p class="question-title">
                <span @click="goDetail(item.id)">{{ item.title }}</span>
              </p>
              <div v-if="replaceHTML(item.content).length>0" class="question-content" v-html="replaceHTML(item.content)" />
              <div v-else class="question-content tooltip">No additional details.</div>
              <div class="question-end">
                <div class="nav-item-icon button">
                  <dss-avatar
                    inline
                    size="small"
                    cycle
                    :nt="item.submitter"
                  />
                  <span class="name">{{ item.submitter }}</span>
                </div>
                <span class="date button">{{ formatTime(item.createTime) }}</span>
                <span
                  v-if="(item.submitter === User) || aceAdmin"
                  class="delete button"
                >
                  <el-popover
                    v-model="item.visible"
                    placement="top"
                    width="180"
                  >
                    <p>Are you sure to delete this？</p>
                    <div style="text-align: right; margin: 0">
                      <el-button size="mini" type="text" @click="item.visible = false">cancel</el-button>
                      <el-button type="primary" size="mini" @click="deleteQuestion(item.id)">confirm</el-button>
                    </div>
                    <span slot="reference" class="delete-text">Delete</span>
                  </el-popover>
                </span>
              </div>
              <div v-if="item.tags.length>0" class="question-tags">
                <span v-for="tag in item.tags" :key="tag.id" class="tag">
                  <el-tag size="medium">{{ tag.name }}</el-tag>
                </span>
              </div>
            </el-col>
          </el-row>
        </div>
      </div>
      <div v-if="!tagValue" class="pagination">
        <span>{{ size }} records per page
          <i
            class="refresh"
            :class="{'el-icon-refresh':!loading,'el-icon-loading':loading}"
            @click="()=> refresh()"
          /></span>
        <el-pagination
          :current-page.sync="currentPage"
          :page-size="size"
          layout="prev, pager, next"
          :total="total"
          @current-change="changePage"
        />
      </div>
      <div v-else class="pagination">
        <span>{{ size }} records per page</span>
        <el-pagination
          :current-page.sync="currentTagPage"
          :page-size="size"
          layout="prev, pager, next"
          :total="totalTag"
          @current-change="changeTagPage"
        />
      </div>
    </div>
    <div v-else class="no-data">No Data!</div>
    <new-question-dialog :visible.sync="visible" />
  </div>
</template>
<script lang="ts">
import {
  Component,
  Vue,
  Watch,
} from 'vue-property-decorator';
import _ from 'lodash';
import moment from 'moment';
import Util from '@/services/Util.service';
import ZetaAceRemoteService from '@/services/remote/ZetaAce.ts';
import NewQuestionDialog from './NewQuestionDialog.vue';
import { FaqRes } from '@/types/ace';

enum SortType {
  createTime = 'createTime',
  updateTime ='updateTime'
}
@Component({
  components: {
    NewQuestionDialog,
  },
})
export default class FAQList extends Vue {
  zetaAceRemoteService = new ZetaAceRemoteService();
  questionList: Array<FaqRes> = [];
  loading = false;
  searchKey = '';
  searchList: Array<any> = [];
  debouncedSearch: Function;
  createLoading = false;
  visible = false;
  scoped: Array<any> = [{label:'All questions', value: 'all'},{label:'My questions', value: 'self'}];
  scopeValue = 'all';
  tagValue = '';
  currentTag: any= {};
  debounce = 500;
  total = 0;
  currentPage = 1;
  totalTag = 0;
  currentTagPage = 1;
  readonly size: number = 20;
  private SortType = SortType;
  private sortType: SortType = SortType.updateTime;
  constructor (){
    super();
    this.debouncedSearch = _.debounce(this.search, 600, {
      trailing: true,
    });
  }
  get aceAdmin (){
    return this.$store.state.user.user.aceAdmin;
  }
  get User (){
    return Util.getNt();
  }
  get faqList (){
    return  this.$store.state.zetaAceStore.faqList;
  }
  get tags (){
    return this.$store.state.zetaAceStore.tags;
  }
  setScrollTop (){
    const scrollTop = this.$store.state.zetaAceStore.faqScrollTop;
    this.$nextTick(() => {
      const dom = (this.$refs.list as HTMLElement);
      if (dom){
        dom.scrollTop = scrollTop;
      }
    });
  }
  refresh (){
    this.getAllQuestion();
  }
  activated () {
    this.setScrollTop();
  }
  mounted () {
    this.getAllQuestion();
  }
  getAllQuestion (){
    this.$store.dispatch('setFaqScrollTop', 0);
    this.setScrollTop();
    this.tagValue = '';
    this.loading = true;
    this.zetaAceRemoteService.getAllQuestion(this.scopeValue, this.sortType, this.size, this.currentPage-1)
      .then(result => {
        if (result && result.data && result.data.questions && result.data.questions.length>0){
          const questions = result.data.questions;
          const total = result.data.totalSize;
          // const tags = result[1].data.tags;
          // this.$store.dispatch('initTags', tags);
          this.getTagsByQuestions(questions, total);
        } else {
          this.loading = false;
          this.questionList = [];
          this.total = 0;
          this.$store.dispatch('initFAQ', {list:this.questionList,totalCount:this.total});
        }
      }).catch(() => {
        this.loading = false;
      });
  }
  async getTagsByQuestions (questions: Array<FaqRes>, total: number){
    const qids = _.chain(questions).map(q => {return q.id; }).value().toString();
    const result = await this.zetaAceRemoteService.getTagsByQids(qids);
    const tags = result.data || {};
    this.loading = false;
    questions.map((item: FaqRes)=>{
      item.tags = tags[item.id]?tags[item.id]:[];
    });
    this.questionList = questions;
    this.total = total;
    this.handleFilter();
    this.$store.dispatch('initFAQ', {list:this.questionList,totalCount:this.total});
  }
  replaceHTML (str: string){
    return str?str.replace(/<[^>]*>|/g, ''):'';
  }
  formatTime (time: number){
    return moment(time).format('YYYY-MM-DD HH:mm:ss');
  }
  searchTags (tag: string, cb: any){
    if (this.tags.length>0 && tag.length === 0){
      const opntions: Array<any> = [];
      _.forEach(this.tags, (v: any) => {
        opntions.push({value: v.name, id: v.id});
      });
      cb(opntions);
      return;
    }
    this.zetaAceRemoteService.getAllTags(tag).then(res => {
      const opntions: Array<any> = [];
      if (res && res.data && res.data.tags) {
        _.forEach(res.data.tags, (v: any) => {
          opntions.push({value: v.name, id: v.id});
        });
        if (tag.length === 0){
          const tags = res.data.tags;
          this.$store.dispatch('initTags', tags);
        }
      }
      cb(opntions);
    }).catch(() => {
      cb([]);
    });
  }
  handleSelectedTag (tag: any){
    this.currentTag = tag;
    this.getQuestionsByTags(tag.id);
  }
  search (){
    if (_.trim(this.searchKey).length===0){
      this.getAllQuestion();
    } else {
      this.loading = true;
      const scopes='faq';
      this.zetaAceRemoteService.aceSearch(this.searchKey, scopes)
        .then(res=>{
          const faq = res.data.faq?res.data.faq:[];
          const ids: any[] = [];
          faq.map(function (item: any) {
            ids.push(item.questionId);
          });
          if (faq.length>0){
            this.getSearchQuestion(ids);
          } else {
            this.loading = false;
            this.questionList = [];
            this.total = 0;
            this.$store.dispatch('initFAQ', {list:this.questionList, totalCount:this.total});
          }
        }).catch(err=>{
          this.loading = false;
        });
    }
  }
  getSearchQuestion (ids: any[]){
    this.zetaAceRemoteService.getQuestionsByIds(ids.toString()).then(res =>{
      if (res && res.data){
        if (res.data.length > 0){
          this.total = res.data.length;
          const data = _.chain(ids).map(id => {
            return _.find(res.data, (q) => {
              return q.id === id;
            });
          })
            .compact()
            .value();
          this.getTagsByQuestions(data, this.total);
        } else {
          this.loading = false;
          this.questionList = [];
          this.total = 0;
          this.$store.dispatch('initFAQ', {list:this.questionList,totalCount:this.total});
        }
      }
    }).catch(() => {
      this.loading = false;
    });
  }
  getQuestionsByTags (tid: number){
    this.loading = true;
    this.zetaAceRemoteService.getQuestionIdsByTid(tid, this.currentTagPage-1, this.size).then(res => {
      const qids = res.data && res.data.questionIds ? res.data && res.data.questionIds : 0;
      this.totalTag = res.data.totalSize;
      if (qids){
        this.getSearchQuestion(qids);
      } else {
        this.loading = false;
        this.questionList = [];
        this.total = 0;
        this.$store.dispatch('initFAQ', {list:this.questionList,totalCount:this.total});
      }
    }).catch(() =>{
      this.loading = false;
    });
  }
  changeTagPage (){
    this.getQuestionsByTags(this.currentTag.id);
  }
  changePage (){
    this.getAllQuestion();
  }
  deleteQuestion (qid: number){
    this.zetaAceRemoteService.deleteQuestion(qid)
      .then( () =>{
        this.$message.success('Delete Success');
        this.$store.dispatch('deleteFAQ', qid);
      });
  }
  goDetail (id: number){
    this.$store.dispatch('setFaqScrollTop', (this.$refs.list as HTMLElement).scrollTop);
    this.$router.push(`/FAQDetail?id=${id}`);
  }
  handleFilter (){
    const scopeList = this.questionList.filter(q => {
      if (this.scopeValue === 'self'){
        return q.submitter === this.User;
      }
      return true;
    });
    let list: any = scopeList;
    if (this.tagValue){
      list = scopeList.filter(q => {
        if (q.tags && q.tags.length>0){
          const has = q.tags!.filter(t=>t.name === this.tagValue);
          if (has.length>0){
            return true;
          }
        }
      });
    }
    // has search text not need sort
    if (_.trim(this.searchKey).length===0){
      this.questionList = _.sortBy(
        list,
        (q)=>{
          return -moment(q[this.sortType]).valueOf();
        }
      );
    } else {
      this.questionList  = list;
    }
  }
  @Watch('faqList')
  handleChangeFaq (){
    this.questionList = this.faqList;
    this.total = this.$store.state.zetaAceStore.faqTotal;
  }
  @Watch('scopeValue')
  changeScope (){
    this.currentPage = 1;
    if (this.tagValue === ''){
      if (_.trim(this.searchKey).length===0){
        this.getAllQuestion();
      } else {
        this.search();
      }
    } else {
      this.getQuestionsByTags(this.currentTag.id);
    }
  }
  @Watch('sortType')
  changeSortType (){
    this.currentPage = 1;
    if (this.tagValue === ''){
      if (_.trim(this.searchKey).length===0){
        this.getAllQuestion();
      } else {
        this.search();
      }
    } else {
      this.getQuestionsByTags(this.currentTag.id);
    }
  }
  @Watch('tagValue')
  handleTagChange (){
    this.currentPage = 1;
    if (this.tagValue === ''){
      if (_.trim(this.searchKey).length===0){
        this.getAllQuestion();
      } else {
        this.search();
      }
    }
  }
  @Watch('searchKey')
  handleSearch (){
    this.debouncedSearch();
  }
}
</script>
<style lang="scss" scoped>
@import "@/styles/global.scss";
.faq{
  height: 100%;
  overflow: hidden;
  padding-bottom: 10px;
  box-sizing: border-box;
}
.toolbar{
  padding-bottom: 30px;
  margin: 0 30px;
  display: flex;
  justify-content: space-between;
  height: 60px;
  line-height: 30px;
  border-bottom:  1px solid #dcdfe6;
  box-sizing: border-box;
  .left{
    display: flex;
    justify-content: space-between;
    .title{
      display: inline-block;
      font-size: 26px;
      font-weight: bold;
      margin-right: 30px;
    }
    .search{
      display: flex;
      justify-content: space-between;
      .filters{
        display: flex;
        line-height: 30px;
        .el-select,
        .el-autocomplete {
          margin: 0 20px 0 10px;
          width: 140px;
        }
      }
      .input-box{
        position: relative;
        i{
          position: absolute;
          left: 7px;
          top: 7px;
          color: #cacbcf;
          font-size: 16px;
        }
        .search-input{
          line-height: 28px;
          box-sizing: border-box;
          width: 220px;
          padding-left: 30px;
          border: 1px solid #dcdfe6;
          border-radius: 4px;
          outline: none;
          &:focus{
            border-color: $zeta-global-color;
          }
          &::placeholder{
            color: #cacbcf;
          }
        }
      }
      /deep/ .el-input-group__prepend{
        border-color: $zeta-global-color;
        border-right:1px solid $zeta-global-color;
        background-color: $zeta-global-color;
        .el-icon-search{
          color: #ffffff;
        }
      }
    }
  }
  .right{
    display: flex;
    justify-content: space-between;
  }
}

.list-scroll {
  height: 100%;
  overflow-y: auto;
  width: 100%;
  padding: 10px 30px 0;
  box-sizing: border-box;
  .content {
    padding: 20px 0;
    border-bottom: 1px solid #f2f2f2;
    word-break: break-word;
    &:last-child {
      border-bottom: none;
    }
    .el-row {
      .left{
        display: flex;
        max-width: 70px;
        .statscontainer{
          display: flex;
          flex-direction: column;
          text-align: center;
          color: #8b8b8b;
          font-size: 12px;
          .vote{
            margin-bottom: 10px;
          }
          .viewcount{
            font-size: 24px;
            // font-weight: bold;
            color: #8b8b8b;
          }
        }
      }
      .question-title {
        font-weight: bold;
        font-size: 18px;
        line-height: 20px;
        margin-bottom: 10px;
        color: $zeta-global-color;
        // white-space: nowrap;
        >span{
          cursor: pointer;
          overflow: hidden;
          text-overflow: ellipsis;
          display: -webkit-box;
          -webkit-line-clamp: 1;
          -webkit-box-orient: vertical;
          width: fit-content;
          &:hover{
            color: $zeta-global-color-heighlight;
          }
        }
      }
      .question-content{
        font-size: 14px;
        line-height: 16px;
        height: 32px;
        overflow: hidden;
        text-overflow: ellipsis;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        &.tooltip{
          color: #cacbcf;
        }
      }
      .question-end {
        color: $zeta-global-color-disable;
        font-size: 13px;
        margin-top: 8px;
        .nav-item-icon {
          display: inline-block;
          .name {
            vertical-align: middle;
            display: inline-block;
            margin-left: 10px;
          }
        }
        .button{
          margin-right: 20px;
          vertical-align: middle;
        }
        .delete{
          .delete-text{
            cursor: pointer;
            &:hover{
              color: $zeta-global-color
            }
          }
        }
        /deep/ i {
          position: relative;
          cursor: pointer;
          color: #999;
          font-size: 20px;
          padding-right: 5px;
          position: relative;
          vertical-align: middle;
          &:hover{
            color: $zeta-global-color;
          }
          &.zeta-icon-like-on{
            color: #e44336;
            >span{
              color:#ffffff;
            }
          }
        }
      }
      .question-tags{
        margin-top: 10px;
        .tag{
          margin-left: 8px;
          margin-bottom: 5px;
          &:first-child{
            margin-left: 0;
          }
          .el-tag{
            border-width: 0;
            border-radius: 2px;
          }
        }
      }
    }
  }
}
.pagination{
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid #eeeeee;
  line-height: 30px;
  >span{
    color: #999;
  }
  .refresh{
    font-size: 16px;
    margin-left: 3px;
    cursor: pointer;
    &:hover{
      color: $zeta-global-color;
    }
  }
}
.fade-enter-active,
.fade-leave-active {
  transition: all 0.3s linear;
}
.fade-enter,
.fade-leave-to {
  transform: translateY(-10px);
  opacity: 0;
}
/deep/ .ql-snow {
  .ql-formats,
  .ql-toolbar,
  &.ql-toolbar {
    &:after {
      display: block !important;
    }
  }
}
.no-data{
  font-size: 18px;
  text-align: center;
  padding: 50px 0;
}
.question-content{
  td{
    border: 1px solid #999;
  }
  /deep/ table{
    border-collapse: collapse;
    border-spacing: 0;
    border: 1px double #b3b3b3;
    td{
      min-width: 2em;
      padding: .4em;
      border: 1px solid #d9d9d9;
    }
  }
}
</style>


