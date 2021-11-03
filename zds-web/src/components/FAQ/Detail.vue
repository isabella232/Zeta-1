<template>
  <div
    ref="detail"
    class="faq-detail"
  >
    <div class="toolbar">
      <el-breadcrumb separator-class="el-icon-arrow-right">
        <el-breadcrumb-item>
          <span
            class="breadcrumb-clk"
            @click="back"
          >Ask Question</span>
        </el-breadcrumb-item>
        <el-breadcrumb-item> Question Detail</el-breadcrumb-item>
      </el-breadcrumb>
    </div>
    <div
      ref="scrollContent"
      v-loading="loading"
      class="scroll-content"
    >
      <div
        ref="questionContent"
        class="question-box"
      >
        <div class="question-title">
          <p>
            <span>{{ question.title }}</span>
            <span
              v-if="(question.submitter === User) || aceAdmin"
              class="edit-text"
              @click="updateQuestion"
            ><i class="icon zeta-icon-edit" />Edit</span>
          </p>
          <el-button
            class="create-button"
            type="primary"
            @click="showEdit"
          >
            Add Answer
          </el-button>
        </div>
        <div
          v-if="question.content"
          ref="questionBody"
          class="question-content"
          :class="isAlation?'alation-article':'zeta_faq'"
          v-html="question.content"
        />
        <div class="question-end">
          <div class="question-tags">
            <span
              v-for="tag in question.tags"
              :key="tag.id"
              class="tag"
            >
              <el-tag size="medium">{{ tag.name }}</el-tag>
            </span>
          </div>
          <div class="question-options">
            <div class="nav-item-icon button">
              <dss-avatar
                inline
                :nt="question.submitter"
                size="small"
                cycle
              />
              <span class="name">{{ question.submitter }}</span>
            </div>
            <span class="date button">{{ formatTime(question.createTime) }}</span>
          </div>
        </div>
      </div>
      <div class="question-post-box">
        <p class="answers-text">
          Answers:
        </p>
        <div
          v-if="showEditor"
          v-loading="postLoading"
          class="question-post-editor"
        >
          <ckeditor
            id="editor"
            v-model="answers"
            :editor="editor.InlineEditor"
            :config="editorConfig"
          />
          <el-button @click="showEditor = false">
            Cancel
          </el-button>
          <el-button
            type="primary"
            :disabled="disable"
            @click="addAnswer"
          >
            Submit
          </el-button>
        </div>
        <div v-if="question.answers&&question.answers.length>0">
          <div
            v-for="(post, index) in question.answers"
            :key="post.id"
          >
            <div
              v-if="question.answers.length>=4 && (index === question.answers.length-1) && !expand"
              class="brand-fold"
            >
              <span @click="toggle">Expand<i class="el-icon-arrow-down" /></span>
            </div>
            <div
              v-if="!post.replyTo && (expand || (index<2 || index === question.answers.length-1))"
              class="post-box"
            >
              <div class="post-left">
                <div class="like">
                  <i
                    :class="post.liked>0?'like-on':''"
                    class="zeta-icon-caretup"
                    @click="like(post.questionId,post.id,1)"
                  />
                  <div class="count">
                    {{ post.totalLike }}
                  </div>
                  <i
                    class="zeta-icon-caret"
                    @click="like(post.questionId,post.id,0)"
                  />
                </div>
              </div>
              <div class="post-right">
                <p
                  v-if="!post.edit"
                  class="post-content"
                  v-html="post.comment"
                />
                <div
                  v-if="post.edit"
                  v-loading="postLoading"
                  class="question-post-editor"
                >
                  <ckeditor
                    id="editor"
                    v-model="postAnswer"
                    :editor="editor.InlineEditor"
                    :config="editorConfig"
                  />
                  <el-button @click="showAnswerEditor(post)">
                    Cancel
                  </el-button>
                  <el-button
                    type="primary"
                    @click="updateAnswer(post)"
                  >
                    Submit
                  </el-button>
                </div>
                <div class="post-toolbar">
                  <div class="left">
                    <span
                      class="option-button comment"
                      @click="showTextArea(post.id)"
                    >Reply</span>
                    <span
                      v-if="(post.poster === User)||aceAdmin"
                      class="option-button edit"
                      @click="showAnswerEditor(post)"
                    >Edit</span>
                    <span
                      v-if="((post.poster === User) || aceAdmin)"
                      class="option-button delete"
                    >
                      <el-popover
                        v-model="post.visible"
                        placement="top"
                        width="180"
                      >
                        <p>Are you sure to delete this？</p>
                        <div style="text-align: right; margin: 0">
                          <el-button
                            size="mini"
                            type="text"
                            @click="post.visible = false"
                          >cancel</el-button>
                          <el-button
                            type="primary"
                            size="mini"
                            @click="deleteAnswer(post.questionId, post.id)"
                          >confirm</el-button>
                        </div>
                        <span
                          slot="reference"
                          class="delete-text"
                        >
                          Delete
                        </span>
                      </el-popover>
                    </span>
                    <span
                      v-if="aceAdmin"
                      class="option-button accept"
                      @click.prevent="handlePostAccepted(post)"
                    >
                      <el-checkbox
                        :value="post.accepted"
                        :disabled="getDisabled(post)"
                      >Accepted</el-checkbox>
                    </span>
                  </div>
                  <div class="right">
                    <span class="button">{{ formatTime(post.createTime) }}</span>
                    <span class="name">User: {{ post.poster }}</span>
                  </div>
                </div>
                <div
                  v-if="post.showTextarea"
                  class="post-editor"
                >
                  <el-input
                    v-model="post.reply"
                    type="textarea"
                    :autosize="{ minRows: 3, maxRows: 5 }"
                    resize="none"
                    :autofocus="true"
                    placeholder="Your comment to an answer would go here. Click the 'Reply' button to submit your response."
                  />
                  <el-button
                    type="primary"
                    :disabled="replyDisable(post.reply)"
                    @click="addComment(post.id, post.reply)"
                  >
                    Submit
                  </el-button>
                </div>
                <div class="comment-container">
                  <div
                    v-for="reply in post.replyDetail.list.slice(0, post.replyDetail.showMore?post.replyDetail.list.length:3)"
                    :key="reply.id"
                    class="comment-list"
                  >
                    <div class="comment-content">
                      <strong class="comment-name">{{ reply.poster }}:</strong>
                      <span>{{ reply.comment }} <em>{{ formatTime(reply.updateTime) }}</em></span>
                    </div>
                  </div>
                  <div
                    v-if="post.replyDetail.list.length>3 && !post.replyDetail.showMore"
                    class="show-more"
                    @click="handleShowMore(post.replyDetail)"
                  >
                    show more
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div
          v-else
          class="no-post"
        >
          <span v-if="!showEditor">None yet.</span>
        </div>
      </div>
    </div>
    <new-question-dialog
      :visible.sync="dialogVisible"
      :question="question"
      :update="update"
    />
    <CodeDisplayDialog
      ref="codeDisplayDialog"
      :value="alationQuery"
      :title="'Query'"
    />
  </div>
</template>
<script lang="ts">
import { Component, Vue, Watch, Ref, Provide } from 'vue-property-decorator';
import moment from 'moment';
import Util from '@/services/Util.service';
import _ from 'lodash';
import $ from 'jquery';
import ZetaAceRemoteService from '@/services/remote/ZetaAce.ts';
import CKEDITOR from '@/components/common/ckeditor';
import { MyUploadAdapter, editorConfig, generalMail } from './utilties';
import { FaqRes, Post } from '@/types/ace';
import NewQuestionDialog from './NewQuestionDialog.vue';
import CodeDisplayDialog from '@/components/common/Visualization/code-display-dialog.vue';
import { ZetaException } from '@/types/exception';
let uploadLoader: any = {};

function MyCustomUploadAdapterPlugin (editor: any) {
  editor.plugins.get('FileRepository').createUploadAdapter = (loader: any) => {
    uploadLoader = loader;
    return new MyUploadAdapter(loader);
  };
}

@Component({
  components: {
    NewQuestionDialog,
    CodeDisplayDialog,
  },
})
export default class Detail extends Vue {
  zetaAceRemoteService = new ZetaAceRemoteService();
  editor: any = CKEDITOR;
  editorConfig = editorConfig({placeholder: 'Add your answer to this question'}, [MyCustomUploadAdapterPlugin ]);
  loading = false;
  dialogVisible = false;
  update = false;
  question = {} as FaqRes;
  answers = '';
  postAnswer = '';
  showEditor = false;
  postLoading = false;
  expand = false;

  @Ref('codeDisplayDialog')
  dialog: CodeDisplayDialog;
  alationQuery = '';

  openCodeDialog (code: string) {
    this.alationQuery = code;
    this.dialog.open();
  }
  constructor () {
    super();
  }
  get aceAdmin (){
    return this.$store.state.user.user.aceAdmin;
  }
  get User () {
    return Util.getNt();
  }
  get id (){
    const query: any = this.$route.query;
    const id =  query.id ? query.id: 0;
    return id;
  }
  get disable (){
    return _.trim(this.answers).length === 0 ? true : false;
  }
  get isAlation (){
    const { tags } = this.question;
    if (tags){
      return tags.filter(tag=>tag.name === 'alation-article').length>0?true:false;
    }
    return false;
  }

  replyDisable (reply: string){
    return _.trim(reply).length === 0 ? true : false;
  }
  getQuestion (){
    this.loading = true;
    const getQuestion = this.zetaAceRemoteService.getQuestion(this.id);
    const getTagsId = this.zetaAceRemoteService.getTagsByQids(this.id);
    Promise.all([ getQuestion, getTagsId ]).then(result =>{
      const question = result[0].data;
      const tags = result[1].data;
      question.answers = _.chain(question.posts)
        .filter((p: Post) => {
          if (!p.replyTo){
            Vue.set(p, 'showTextarea', false);
            Vue.set(p, 'edit', false);
            p.replyDetail = {
              showMore: false,
              list: this.getComment(question.posts, p.id),
            };
          }
          return !p.replyTo;
        })
        .sort((p1: Post, p2: Post) => {
          if (p2.totalLike === p1.totalLike){
            return moment(p1.createTime).valueOf() - moment(p2.createTime).valueOf();
          }
          return p2.totalLike - p1.totalLike;
        })
        .value();
      question.tags = tags[this.id]?tags[this.id]:[];
      this.question = question;
      this.loading = false;
    }).catch(() =>{
      this.loading = false;
    });
  }
  formatTime (time: number) {
    return moment(time).format('YYYY-MM-DD HH:mm:ss');
  }
  back (){
    this.$router.push('/FAQ');
  }
  getComment (post: Array<Post>, id: number){
    return post ?
      _.chain(post)
        .filter((item) => {
          return item.replyTo === id;
        })
        .sort((p1, p2)=>{
          return moment(p2.createTime).valueOf() - moment(p1.createTime).valueOf();
        })
        .value()
      :
      [];
  }
  like (qid: number, pid: number, isLike: number) {
    const q = this.question;
    const post = q.answers.filter((item: any) => item.id === pid)[0];
    if ((post.liked === 1 && isLike === 1) || (post.liked <= 0 && isLike === 0))
      return;
    if (isLike === 1) {
      post.liked = 1;
      post.totalLike++;
      q.totalPostLike++;
    } else {
      post.liked = 0;
      post.totalLike--;
      q.totalPostLike--;
    }
    this.zetaAceRemoteService
      .postVote(qid, pid, isLike)
      .then(() => {
        this.$store.dispatch('updateFAQ', q);
      })
      .catch(err => {
        if (isLike === 1) {
          post.liked = 0;
          post.totalLike--;
          q.totalPostLike--;
        } else {
          post.liked = 1;
          post.totalLike++;
          q.totalPostLike++;
        }
      });
  }
  addAnswer () {
    const q = this.question;
    this.postLoading = true;
    this.zetaAceRemoteService
      .reply(q.id, this.answers)
      .then(res => {
        q.totalPost++;
        q.totalLevel1Post++;
        res.data.liked = 0;
        res.data.totalLike = 0;
        res.data.showTextArea = false;
        res.data.replyDetail = {
          showMore: false,
          list: [],
        };
        q.posts.push(res.data);
        q.answers.push(res.data);
        this.sendMail(this.answers, q, 'answer', 2);
        this.postLoading = false;
        this.showEditor = false;
        this.answers = '';
        this.$store.dispatch('updateFAQ', q);
        this.$nextTick(()=>{
          this.scrollToBottom();
        });
      })
      .catch(() => {
        this.postLoading = false;
      });
  }
  deleteAnswer (qid: number, pid: number) {
    const q = this.question;
    this.postLoading = true;
    this.zetaAceRemoteService
      .deletePost(qid, pid)
      .then(res => {
        this.postLoading = false;
        const post = q.answers.filter(item => {
          return item.id === pid;
        })[0];
        q.answers = q.answers.filter(item => {
          return item.id !== pid;
        });
        q.totalPost--;
        q.totalPostLike = q.totalPostLike - post.totalLike;
        q.totalLevel1Post--;
        this.$store.dispatch('updateFAQ', q);
      })
      .catch(err => {
        this.postLoading = false;
      });
  }
  showEdit (){
    this.showEditor = true;
    const top = (this.$refs['questionContent'] as HTMLElement).offsetHeight - (this.$refs['detail'] as HTMLElement).offsetHeight;
    const offset = 30 + 70+ 70 + 30; // paddingTop + toolbar area + answers area + paddingBottom
    const editorHeight = 125;
    $('.scroll-content').animate({scrollTop: top + offset + editorHeight});
  }
  scrollToBottom (){
    const content = this.$refs['scrollContent'] as HTMLElement;
    const postList = document.getElementsByClassName('post-box');
    const lastMessage = postList[postList.length - 1] as HTMLElement;
    $('.scroll-content').animate({scrollTop: lastMessage.offsetTop - 100});
  }
  createQuestion (){
    this.dialogVisible = true;
    this.update = false;
  }
  updateQuestion (){
    this.dialogVisible = true;
    this.update = true;
  }
  showTextArea (pid: number){
    const q = this.question;
    const post = q.answers.filter(p => p.id === pid)[0];
    Vue.set(post, 'reply', '');
    this.question.answers.map((p)=>{
      if (p.id === post.id){
        Vue.set(post, 'showTextarea', !post.showTextarea);
      } else {
        Vue.set(p, 'showTextarea', false);
      }
    });
  }
  showAnswerEditor (post: Post){
    this.postAnswer = post.comment;
    this.question.answers.map((p)=>{
      if (p.id === post.id){
        Vue.set(post, 'edit', !post.edit);
      } else {
        Vue.set(p, 'edit', false);
      }
    });
  }
  addComment (pid: number, comment: string){
    const q = this.question;
    this.zetaAceRemoteService
      .reply(q.id, comment, pid)
      .then(res => {
        const post = q.answers.filter(p => p.id === pid)[0];
        post.showTextarea = false;
        post.replyDetail.list.unshift(res.data);
      })
      .catch(() => {
        this.postLoading = false;
      });
  }
  updateAnswer (post: Post){
    const q = this.question;
    this.postLoading = true;
    this.zetaAceRemoteService
      .updateReply(q.id, post.id, this.postAnswer)
      .then(res => {
        this.postLoading = false;
        post.edit = false;
        post.comment = this.postAnswer;
      })
      .catch(() => {
        this.postLoading = false;
      });
  }
  async sendMail (message: string, questionObj: any, type: string, retryCount: number) {
    if (retryCount <= 0) return false;
    const params = generalMail(type, questionObj, message);
    if (!_.isEmpty(params.toAddr)) {
      this.zetaAceRemoteService.createEmail(params)
        .catch((err: ZetaException) => {
          err.resolve();
          // eslint-disable-next-line no-console
          console.error('Call Api:createEmail failed: ' + JSON.stringify(err));
          setTimeout(()=>{
            this.sendMail(message, questionObj, type, retryCount-1);
          }, 2000);
        });
    }
  }
  toggle (){
    this.expand = true;
  }
  handleShowMore (replyDetail: any){
    replyDetail.showMore = true;
  }
  @Watch('id', { immediate: true })
  handlePageLoad (){
    this.getQuestion();
  }
  @Watch('answers')
  handleAnswerChange (newVal: string){
    if (uploadLoader && uploadLoader.status === 'idle'){
      this.$nextTick(()=>{
        // const imgCount = newVal.split('img').length - 1;
        // const srcCount = newVal.split('src').length -1 ;
        // if(imgCount === srcCount){
        this.answers  = newVal + '<p><br data-cke-filler="true"></p>';
        uploadLoader.status = 'error';
        // }
      });
    }
  }
  @Watch('postAnswer')
  handlePostAnswerChange (){
    if (uploadLoader && uploadLoader.status === 'idle'){
      this.$nextTick(()=>{
        this.postAnswer  = this.postAnswer + '<p><br data-cke-filler="true"></p>';
        uploadLoader.status = 'error';
      });
    }
  }
  getDisabled (post: Post){
    const acceptedPost =  _.find(this.question.posts, (p)=>{return p.accepted == true;});
    if (acceptedPost === undefined){
      return false;
    } else {
      if (acceptedPost.id === post.id){
        return false;
      } else {
        return true;
      }
    }
    return false;
  }
  handlePostAccepted (post: Post){
    if (this.getDisabled(post)) return;
    const accepted = post.accepted;
    if (!accepted){
      this.zetaAceRemoteService.setAccepted(this.id, post.id).then(()=>{
        post.accepted = true;
      });
    } else {
      this.zetaAceRemoteService.cancelAccepted(this.id, post.id).then(()=>{
        post.accepted = false;
      });
    }
  }
  mounted () {
    window.addEventListener('click', this.handleClick);
  }
  destroyed () {
    window.removeEventListener('click', this.handleClick);
  }
  get $questionBody (){
    return this.$refs['questionBody'];
  }
  async handleClick (e: MouseEvent){
    if (!this.isAlation) return;
    if (!this.$questionBody) return;
    if ((this.$questionBody as HTMLElement).contains(e.target as any)){
      e.preventDefault();
      const target = e.target as any;
      const dataset = target.dataset;
      if (dataset && dataset.otype === 'article'){
        const index = target.href.indexOf('/article/')+9;
        const str = target.href.substr(index);
        const result = await this.zetaAceRemoteService.getFaqId(str);
        if (result.data && result.data.questionId){
          this.$router.push(`/FAQDetail?id=${result.data.questionId}`);
        }
      }
      if (dataset && dataset.otype === 'query'){
        const index = target.href.indexOf('/query/')+7;
        const str = target.href.substr(index);
        const result = await this.zetaAceRemoteService.getQuery(str);
        if (result.data){
          this.openCodeDialog(result.data.content);
        }
      }
    }
  }
}
</script>
<style lang="scss" scoped>
@import "@/styles/global.scss";
$font-color: #8b8b8b;
.faq-detail{
  height: 100%;
  padding: 30px;
  box-sizing: border-box;
  overflow: hidden;
  .toolbar{
    padding-bottom: 20px;
    .el-breadcrumb {
      margin-bottom: 10px;
      .breadcrumb-clk {
        cursor: pointer;
        color: #4D8CCA;
        font-weight: bold;
      }
      .breadcrumb-clk:hover {
        color: #569CE1;
        text-decoration: underline;
      }
    }
  }
  .scroll-content{
    height: calc(100% - 45px);
    overflow: auto;
  }
  .question-box{
    .question-title {
      font-weight: bold;
      font-size: 22px;
      line-height: 26px;
      padding-bottom: 10px;
      margin-bottom: 10px;
      color: #333333;
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      word-break: break-word;
      border-bottom: 1px solid #dcdfe6;
      p{
        flex: 1;
        padding-right: 20px;
      }
      .create-button{
        display: inline-block;
      }
      .edit-text{
        color: $zeta-global-color;
        font-size: 14px;
        font-weight: normal;
        margin-left: 10px;
        cursor: pointer;
        .zeta-icon-edit{
          color: $zeta-global-color;
        }
      }
    }
    .question-content {
      font-size: 16px;
      line-height: 20px;
      margin-bottom: 10px;
      white-space: pre-wrap;
      word-break: break-word;
      /deep/ img{
        max-width: 100%;
      }
    }
    .question-end{
      margin-top: 30px;
      display: flex;
      justify-content: space-between;
      align-items: center;

      .question-options {
        color: $zeta-global-color-disable;
        font-size: 12px;
        .nav-item-icon {
            display: inline-block;
            .name {
              line-height: 20px;
              display: inline-block;
              margin-left: 10px;
            }
          }
        .button {
          margin-right: 20px;
          vertical-align: middle;
        }
      }
      .question-tags{
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
  .question-post-box{
    margin-top: 20px;
    padding: 20px 30px;
    background-color: #f7f7f7;
    .answers-text{
      font-size: 18px;
      font-weight: bold;
      color: $font-color;
      margin-bottom: 10px;
    }
    .post-box{
      display: flex;
      background-color: #ffffff;
      border-radius: 2px;
      margin: 20px 0;
      padding: 20px;
      .post-left{
        width: 70px;
        display: flex;
        color: $font-color;
        font-size: 0;
        .count{
          text-align: center;
          font-size: 24px;
          padding: 5px 0;
        }
        /deep/ i {
        position: relative;
        cursor: pointer;
        color: $font-color;
        vertical-align: middle;
        font-size: 14px;
        &:hover {
          color: $zeta-global-color;
        }
        &.like-on{
          color: $zeta-global-color;
        }
      }
      }
      .post-right{
        flex: 1;
        // padding: 10px;
        .post-content{
          color: #606266;
          /deep/ img{
            max-width: 100%;
          }
        }
        .post-toolbar{
          display: flex;
          justify-content: space-between;
          padding: 10px 0;
          color: $zeta-global-color-disable;
          .option-button{
            margin-right: 20px;
            &.accept{
              /deep/ .el-checkbox{
                color: $zeta-global-color-disable;
                &.is-checked{
                  .el-checkbox__label{
                    color: $zeta-global-color-green;
                  }
                  .el-checkbox__inner{
                    background-color: $zeta-global-color-green;
                    border-color: $zeta-global-color-green;
                  }
                }
                .el-checkbox__inner{
                  border-radius: 50%;
                  &:hover{
                    border-color: $zeta-global-color-green;
                  }
                }
                .el-checkbox__input.is-focus{
                  .el-checkbox__inner{
                    border-color: $zeta-global-color-green;
                  }
                }
              }
            }
          }
          .name{
            // color: $font-color;
          }
          .comment,.edit{
            cursor: pointer;
            &:hover{
              color: $zeta-global-color
            }
          }
          .delete-text {
            cursor: pointer;
            &:hover {
              color: $zeta-global-color;
            }
          }
        }
        .comment-list{
          padding: 15px;
          &:first-child{
            border-top: 1px dashed #ffffff;
          }
          &:nth-of-type(odd){
            background-color: #f7f7f7;
          }
          .comment-content{
            color: $font-color;
            display: flex;
            span{
              margin-left: 5px;
            }
            .comment-name{
              // font-weight: normal;
              // color: #569CE1;
            }
            em{
              font-style: normal;
              color: $zeta-global-color-disable;
              margin-left: 10px;
              font-size: 12px;
            }
          }
        }
        .show-more{
          line-height: 20px;
          color: $zeta-global-color-heighlight;
          cursor: pointer;
          &:hover{
            color: $zeta-global-color;
          }
        }
      }
    }
    .post-editor{
      margin-bottom: 10px;
      overflow: hidden;
      .el-button{
        margin-top: 2px;
        float: right;
      }
    }
  }
  .brand-fold{
    color: #4D8CCA;
    text-align: center;
    margin-bottom: 20px;
    font-size: 15px;
    span{
      cursor: pointer;
      &:hover{
        color: #569CE1;
      }
      i{
        margin-left: 2px;
      }
    }
  }
  .no-post{
    color: $font-color;
    font-size: 14px;
  }
  .question-post-editor{
    margin-bottom: 10px;
    overflow: hidden;
    .el-button{
      margin-top: 2px;
      margin-left: 10px;
      float: right;
    }
  }
}
.question-content, .post-content{
  overflow: hidden;
  /deep/ .image{
    position: relative;
    display: block;
    max-width: 100%;
    min-width: 50px;
    clear: both;
    text-align: center;
    margin: 1em auto;
    &.image_resized{
      max-width: 100%;
      display: block;
      box-sizing: border-box;
    }
    &.image-style-side{
      float: right;
      margin-left: var(--ck-image-style-spacing);
      max-width: 50%;
    }
    &.image-style-align-left{
      float: left;
      margin-right: var(--ck-image-style-spacing);
      max-width: 50%;
    }
    img{
      display: block;
      margin: 0 auto;
      max-width: 100%;
      min-width: 50px;
    }
    figcaption{
      // display: table-caption;
      caption-side: bottom;
      word-break: break-word;
      color: #333;
      background-color: #f7f7f7;
      padding: .6em;
      font-size: .75em;
      outline-offset: -1px;
      text-align: center;
    }
  }
  /deep/ pre{
      padding: 1em;
      color: #353535;
      background: hsla(0,0%,78%,.3);
      border: 1px solid #c4c4c4;
      border-radius: 2px;
      text-align: left;
      direction: ltr;
      tab-size: 4;
      white-space: pre-wrap;
      font-style: normal;
      min-width: 200px;
      code{
        background: unset;
        padding: 0;
        border-radius: 0;
      }
    }
}

.blank-col {
  height: 1px;
}
</style>
<style lang="scss" scoped>
.alation-article{
  font-size: 14px !important;
  word-break: break-word !important;
  white-space: normal !important;
  /deep/ div{
    h1, h2, h3, h4, h5, h6{
      color: inherit;
      margin: 10px 0;
      font-family: inherit;
      line-height: 20px;
      text-rendering: optimizelegibility;
      line-height: normal;
    }
    h1{
      font-size: 33px;
      font-weight: 500;
    }
    h2{
      font-size: 25px;
      font-weight: 500;
    }
    h3{
      font-size: 19px;
      font-weight: 500;
    }
    ul,ol{
      padding: 0;
      margin: 0 0 10px 25px;
    }
    li{
      line-height: 20px;
    }
    p{
      margin: 0 0 10px;
    }
    table{
      width: 100%;
      max-width: 100%;
      background-color: transparent;
      border-collapse: collapse;
      border-spacing: 0;
      border: none;
      empty-cells: show;
    }
    pre{
      display: block;
      padding: 9.5px;
      margin: 0 0 10px;
      font-size: 13px;
      line-height: 20px;
      word-break: break-all;
      word-wrap: break-word;
      white-space: pre;
      white-space: pre-wrap;
      background-color: #f5f5f5;
      border: 1px solid #ccc;
      border: 1px solid rgba(0,0,0,0.15);
      -webkit-border-radius: 4px;
      -moz-border-radius: 4px;
      border-radius: 4px;
      white-space: pre-wrap;
      word-wrap: break-word;
      overflow: visible;
    }
  }
}
</style>
