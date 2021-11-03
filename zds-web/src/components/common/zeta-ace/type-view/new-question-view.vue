<template>
  <div v-loading="newQuestion.loading">
    <div v-if="!newQuestion.submit" class="question-box">
      <span class="title">Submit a question</span>
      <div class="input-editor">
        <span>Title:</span>
        <el-input
          v-model="newQuestion.question"
          type="text"
          :autofocus="true"
          maxlength="150"
          show-word-limit
          @input="(str)=>handleInputChange(newQuestion,str)"
        />
        <i class="error-info">{{ newQuestion.error }}</i>
      </div>
      <div class="input-editor">
        <span>Content:</span>
        <el-input
          v-model="content"
          type="textarea"
          :rows="3"
          resize="none"
          maxlength="10000"
          show-word-limit
          placeholder="Here are some more details."
        />
      </div>
      <el-upload
        :action="uploadUrl"
        :headers="headers"
        :on-success="(...e)=>uploadSuccess(newQuestion, ...e)"
        :on-remove="(...e)=>handleRemove(newQuestion, ...e)"
        list-type="picture"
        accept=".png, .jpg, .jpeg"
      >
        <el-button size="small" type="primary" plain>Image upload</el-button>
        <div slot="tip" class="el-upload__tip">jpg/png files with a size less than 4M</div>
      </el-upload>
      <div class="tag-input-editor">
        <p>Tags:</p>
        <div class="tag-box">
          <el-tag
            v-for="tag in tags"
            :key="tag.id"
            size="medium"
            closable
            :disable-transitions="false"
            @close="handleClose(tag)"
          >
            {{ tag.name }}
          </el-tag>
          <el-input
            :loading="tagLoading"
            v-if="inputVisible"
            ref="saveTagInput"
            v-model.trim="inputTag"
            class="input-new-tag"
            size="small"
            placeholder="tag"
            @keyup.enter.native="handleInputConfirm"
            @blur="handleInputConfirm"
          />
          <el-button v-else class="button-new-tag" size="small" @click="showInput">+ New Tag</el-button>
        </div>
        <p class="error-info">{{tagError}}</p>
      </div>
      <el-button type="primary" class="submit" @click="submitQuestion">Submit</el-button>
    </div>
    <div v-else>
      <p v-if="newQuestion.title" class="title" >{{ newQuestion.title }}</p>
      <p v-if="newQuestion.path" class="router"  @click="goFAQ(newQuestion.path)">View it Here</p>
    </div>
  </div>
</template>
<script lang="ts">
import { Vue, Component, Prop, Inject, Watch } from 'vue-property-decorator';
import ZetaAceRemoteService from '@/services/remote/ZetaAce.ts';
import config from '@/config/config';
import _ from 'lodash';
import Util from '@/services/Util.service';
import {FaqRes, Message, Tag } from '@/types/ace';
import { ZetaException } from '@/types/exception';
import { generalMail } from '@/components/FAQ/utilties';
const baseUrl = config.zeta.base;
@Component({

})
export default class NewQuestionView extends Vue{
  @Inject('zetaAceRemoteService')
  zetaAceRemoteService: ZetaAceRemoteService;
  @Prop() question: any;
  uploadUrl = `${baseUrl}attachments`;
  inputVisible = false;
  inputTag = '';
  tagError = '';
  tagLoading = false;
  content = '';
  tags: Array<Tag> = [];
  get user() {
    return Util.getNt();
  }
  get headers(){
    return Util.getResuestHeader();
  }
  get newQuestion(){
    return this.question;
  }
  uploadSuccess(newQuestion: any, res: any){
    const url = `${baseUrl}attachments/${res.fileName}.${res.fileExtension}`;
    newQuestion.imgURL.push(url);
  }
  handleRemove(newQuestion: any, file: any){
    const src = `${baseUrl}attachments/${file.response.fileName}.${file.response.fileExtension}`;
    newQuestion.imgURL = newQuestion.imgURL.filter((url: any) => {
      return url !== src;
    });
  }
  handleInputChange(newQuestion: any, str: string){
    newQuestion.error = '';
    if(_.trim(str).length===0){
      newQuestion.error = 'Cannot be empty';
    }
  }
  formatImg(imgURL: Array<string>){
    const imgDom = _.chain(imgURL).map(str =>{
      return `<p><img src='${str}' /></p>`;
    }).value().join('');
    return imgDom;
  }
  showInput() {
    this.inputVisible = true;
    this.$nextTick(() => {
      (this.$refs.saveTagInput as any).$refs.input.focus();
    });
  }
  handleInputConfirm(){
    const reg2 = /[^a-zA-Z0-9\+\#\-\.]/;
    const reg1 = /^[0-9\+\#\-\.]/;
    if(reg1.test(this.inputTag) || reg2.test(this.inputTag) || this.inputTag.length<3 || this.inputTag.length>35){
      return;
    }
    if(this.tags.filter((tag: Tag)=>tag.name === this.inputTag).length>0){
      this.tagError = 'exist tag';
      return;
    }
    this.tagError = '';
    this.addTag(this.inputTag);
  }
  handleClose(tag: Tag) {
    this.tags.splice(this.tags.indexOf(tag), 1);
  }
  addTag(tag: string){
    this.tagLoading = true;
    const tagValue = tag.toLowerCase();
    this.zetaAceRemoteService.addTag(tagValue).then(res => {
      if(res && res.data){
        this.tags.push(res.data);
        this.inputTag = '';
        this.inputVisible = false;
        this.tagLoading = false;
      }else{
        console.log(res);
        this.tagError = 'Tag addition failed';
      }
    }).catch(err =>{
      console.error(err);
    });
  }

  submitQuestion() {
    let newQuestion = this.newQuestion;
    if(_.trim(newQuestion.question).length === 0) {
      newQuestion.error = 'Cannot be empty';
      return;
    }
    newQuestion.loading = true;
    const newMsg = {
      title: 'Your question has been successfully submited to the FAQ.',
      path: '/FAQDetail'
    } as Message;
    const content = this.content + this.formatImg(newQuestion.imgURL);
    const result = this.zetaAceRemoteService.addQuestion(newQuestion.question, content);
    result
      .then((res: any) => {
        const q = res.data;
        q.totalLike = 0;
        q.totalPost = 0;
        q.totalPostLike = 0;
        q.liked = 0;
        q.totalLevel1Post = 0;
        q.tags = this.tags;
        this.$store.dispatch('addFAQ', q);
        this.sendMail(newQuestion.question, q.id, 2);
        if(this.tags.length>0){
          const tids = _.chain(this.tags).map(tag => {return tag.id; }).value().toString();
          this.zetaAceRemoteService.addTagsToQuestion(q.id, tids).then(()=>{
            newMsg.path = `/FAQDetail?id=${q.id}`;
            newQuestion = Object.assign(newQuestion, newMsg);
            newQuestion.submit = true;
            newQuestion.loading = false;
          }).catch(err =>{
            console.log(err,'Fail add tags to question');
          });
        }else{
          newMsg.path = `/FAQDetail?id=${q.id}`;
          newQuestion = Object.assign(newQuestion, newMsg);
          newQuestion.submit = true;
          newQuestion.loading = false;
        }
      })
      .catch(() => {
        newQuestion.loading = false;
      });
  }

  goFAQ(path: string) {
    this.$router.push(path);
  }

  async sendMail(message: string, qid: number, retryCount: number) {
    if (retryCount <= 0) return false;
    const params = generalMail('question', {id: qid, title: message});
    if (!_.isEmpty(params.toAddr)) {
      this.zetaAceRemoteService
        .createEmail(params)
        .catch((err: ZetaException) => {
          err.resolve();
          console.error('Call Api:createEmail failed: ' + JSON.stringify(err));
          setTimeout(()=>{
            this.sendMail(message, qid, retryCount-1);
          }, 2000);
        });
    }
  }
  getEmailLink(qid: number): string {
    return `${location.protocol}//${location.host}/${Util.getPath()}#/FAQDetail?id=${qid}`;
  }
  @Watch('inputTag')
  handleTag(val: string){
    if (/^[0-9\+\#\-\.]/.test(val)){
      this.tagError = 'Invalid tag name, start with alphabet';
    } else if (/[^a-zA-Z0-9\+\#\-\.]/.test(val)){
      this.tagError = 'Invalid tag name, only [a-zA-Z] [0-9] + - # . is allowed, and start with alphabet, length min 3 and max 35.' ;
    } else if (val.length!=0&&(val.length<3||val.length>35)){
      this.tagError = 'length min 3 and max 35.';
    } else {
      this.tagError = '';
    }
  }
}
</script>
<style lang="scss" scoped>
  .question-box{
    .title{
      display: inline-block;
      font-size: 16px;
      margin-bottom: 10px;
      padding-bottom: 5px;
      border-bottom: 1px solid #569ce1;
    }
    .input-editor{
      margin-bottom: 10px;
      span{
        display: inline-block;
        font-weight: bold;
        margin-bottom: 10px;
      }
      .el-textarea{
        /deep/ textarea{
          // background-color: #F9F9FA;
        }
      }
    }
    .tag-input-editor{
      margin-top: 10px;
      >p{
        margin-bottom: 10px;
      }
    }
    .tag-box{
      display: flex;
      width: 100%;
      line-height: 30px;
      .el-tag{
        margin-right: 10px;
        line-height: 28px;
        &:last-child{
          margin-right: 0;
        }
      }
      .button-new-tag {
        height: 28px;
        line-height: 1;
        box-sizing: border-box;
        padding-top: 0;
        padding-bottom: 0;
        font-size: 12px !important;
      }
      .input-new-tag {
        width: 90px;
        margin-left: 0;
        vertical-align: bottom;
      }
    }
    .submit{
      margin-top: 10px;
      float: right;
    }
  }
  .error-info{
    font-size: 12px;
    color: #f00;
    font-style: normal;
  }
  /deep/ .el-upload{
    .el-button{
      margin-right: 5px;
      padding: 6px;
    }
  }
  /deep/ .el-upload__tip{
    display: inline-block;
  }
  /deep/ .el-upload-list{
    display: flex;
    flex-wrap: wrap;
    li{
      width: 92px;
      padding: 5px;
      margin-right: 5px;
    }
    .el-upload-list__item-thumbnail{
      margin-left: 0;
      float: none;
      width: 80px;
      height: 80px;
    }
    .el-upload-list__item-name{
      display: none;
    }
    .el-upload-list__item-status-label,
    .el-icon-close{
      z-index: 2;
    }
  }
</style>
