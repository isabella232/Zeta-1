<template>
  <div>
    <el-dialog
      :title="update?'Update Question':'Ask Question'"
      :visible.sync="visible_"
      :close-on-click-modal="false"
    >
      <el-form
        ref="form"
        v-loading="createLoading"
        :model="form"
      >
        <el-form-item
          label="Title"
          prop="title"
          :rules="rules[1]"
        >
          <el-input
            v-model="form.title"
            type="text"
            maxlength="150"
            show-word-limit
            placeholder="e.g. How to make a new SQL table?"
            class="title"
          />
        </el-form-item>
        <el-form-item
          label="Content"
          prop="content"
        >
          <ckeditor
            id="editor"
            v-model="form.content"
            :editor="editor.InlineEditor"
            :config="editorConfig"
          />
        </el-form-item>
        <el-form-item
          label="Tags"
          prop="inputTag"
          :rules="rules[0]"
        >
          <div class="tag-box">
            <el-tag
              v-for="tag in form.tags"
              :key="tag.id"
              size="medium"
              closable
              :disable-transitions="false"
              @close="handleClose(tag)"
            >
              {{ tag.name }}
            </el-tag>
            <el-input
              v-if="inputVisible"
              ref="saveTagInput"
              v-model.trim="form.inputTag"
              class="input-new-tag"
              size="small"
              placeholder="tag"
              @keyup.enter.native="handleInputConfirm"
              @blur="handleInputConfirm"
            />
            <el-button
              v-else
              class="button-new-tag"
              size="small"
              @click="showInput"
            >
              + New Tag
            </el-button>
          </div>
        </el-form-item>
      </el-form>
      <div
        slot="footer"
        class="dialog-footer"
      >
        <el-button @click="visible_ = false">
          Cancel
        </el-button>
        <el-button
          type="primary"
          @click="addQuestion"
        >
          {{ update ? 'Update': 'Submit' }}
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>
<script lang="ts">
import { Vue, Component, Prop, Watch } from 'vue-property-decorator';
import ZetaAceRemoteService from '@/services/remote/ZetaAce';
import Util from '@/services/Util.service';
import _ from 'lodash';
import CKEDITOR from '@/components/common/ckeditor';
import { MyUploadAdapter, editorConfig, generalMail } from './utilties';
import { Form } from 'element-ui';
import { FaqRes, Tag } from '@/types/ace';
import { ZetaException } from '@/types/exception';

let uploadLoader: any = {};
function MyCustomUploadAdapterPlugin (editor: any) {
  editor.plugins.get('FileRepository').createUploadAdapter = (loader: any) => {
    uploadLoader = loader;
    return new MyUploadAdapter(loader);
  };
}


@Component({})
export default class NewQuestionDialog extends Vue {
  zetaAceRemoteService = new ZetaAceRemoteService();
  @Prop() visible: boolean;
  @Prop() question: FaqRes;
  @Prop({default: false}) update: boolean;
  createLoading = false;
  originQ: FaqRes;
  editor: any = CKEDITOR;
  editorConfig = editorConfig({placeholder: 'Here are some more details.'}, [ MyCustomUploadAdapterPlugin ]);
  form: {
    title: string;
    content: string;
    tags: Array<Tag>;
    inputTag: string;
  };
  inputVisible = false;
  public rules = [
    {
      validator: (r: any, f: any, cb: Function) => {
        if (/^[0-9\+\#\-\.]/.test(f))cb(new Error('Invalid tag name, start with alphabet'));
        else if (/[^a-zA-Z0-9\+\#\-\.]/.test(f))
          cb(
            new Error(
              'Invalid tag name, only [a-zA-Z] [0-9] + - # . is allowed, and start with alphabet, length min 3 and max 35.'
            )
          );
        else if (f.length!=0&&(f.length<3||f.length>35)) cb(new Error('length min 3 and max 35.'));
        else cb();
      },
      trigger: 'change',
    }, {
      validator: (r: any, f: string, cb: Function) => {
        if (f.trim() === '') {
          cb(
            new Error('Title cannot be blank!')
          );
        }
        else cb();
      },
    },
  ];
  set visible_ (e) {
    this.$emit('update:visible', e);
  }
  get visible_ (): boolean {
    return this.visible;
  }
  get storeTags (){
    return this.$store.state.zetaAceStore.tags;
  }
  get tags (): Array<any> {
    return this.update ? (this.originQ.tags?this.originQ.tags:[]):[];
  }
  constructor () {
    super();
    this.form = {
      title: '',
      content: '',
      tags: [],
      inputTag: '',
    };
  }
  addQuestion () {
    (this.$refs['form'] as Form).validate(valid => {
      if (valid) {
        this.createLoading = true;
        if (this.update){
          // update question
          this.zetaAceRemoteService.updateQuestion(this.question.id, this.form.title, this.form.content)
            .then((res: any) => {
              const newQ = _.assign(this.question, res.data);
              newQ.tags = this.form.tags;
              const removeTagList  = _.pullAllBy(this.tags, newQ.tags, 'id');
              if (removeTagList.length>0){
                const tids = _.chain(removeTagList).map(tag => {return tag.id; }).value().toString();
                this.removeTagsToQuestion(newQ, tids);
              }
              if (this.form.tags.length>0){
                const tids = _.chain(newQ.tags).map(tag => {return tag.id; }).value().toString();
                this.addTagsToQuestion(newQ, tids);
              } else {
                this.visible_ = false;
                this.createLoading = false;
                this.$message.success('Update question Success');
                this.$store.dispatch('updateFAQ', newQ);
              }
            }).catch( () => {
              this.createLoading = false;
            });
        } else {
          //add question
          this.zetaAceRemoteService.addQuestion(this.form.title, this.form.content)
            .then((res: any) => {
              const newQ = res.data;
              newQ.totalLike = 0;
              newQ.totalPost = 0;
              newQ.totalPostLike = 0;
              newQ.liked = 0;
              newQ.totalLevel1Post = 0;
              newQ.tags = this.form.tags;
              if (this.form.tags.length>0){
                const tids = _.chain(newQ.tags).map(tag => {return tag.id; }).value().toString();
                this.addTagsToQuestion(newQ, tids);
              } else {
                this.visible_ = false;
                this.createLoading = false;
                this.sendMail(this.form.title, newQ, 'question', 2);
                this.$message.success('Add question Success');
                this.$store.dispatch('addFAQ', newQ);
              }
            }).catch( () => {
              this.createLoading = false;
            });
        }
      } else {
        return false;
      }
    });
  }
  handleInputConfirm (){
    const reg2 = /[^a-zA-Z0-9\+\#\-\.]/;
    const reg1 = /^[0-9\+\#\-\.]/;
    if (reg1.test(this.form.inputTag) || reg2.test(this.form.inputTag) || this.form.inputTag.length<3 || this.form.inputTag.length>35){
      return;
    }
    if (this.form.tags.filter(tag=>tag.name === this.form.inputTag).length>0){
      this.$message.warning('exist tag');
      return;
    }
    this.addTag(this.form.inputTag);
  }
  handleClose (tag: Tag) {
    if (this.update){
      // this.zetaAceRemoteService.removeTagsFromQuestion(this.question.id, tag.id.toString()).then(()=>{
      this.form.tags.splice(this.form.tags.indexOf(tag), 1);
      // });
    } else {
      this.form.tags.splice(this.form.tags.indexOf(tag), 1);
    }
  }
  addTag (tag: string){
    const tagValue = tag.toLowerCase();
    this.zetaAceRemoteService.addTag(tagValue).then(res => {
      if (res && res.data){
        this.form.tags.push(res.data);
        this.form.inputTag = '';
        this.inputVisible = false;
        if (_.find(this.storeTags, res.data) === undefined){
          this.$store.dispatch('addTags', res.data);
        }
      } else {
        this.$message.warning('Tag addition failed');
      }
    }).catch(err =>{
      // eslint-disable-next-line no-console
      console.error(err);
    });
  }
  addTagsToQuestion (q: FaqRes, tids: string){
    this.zetaAceRemoteService.addTagsToQuestion(q.id, tids).then(()=>{
      this.createLoading = false;
      this.visible_ = false;
      if (this.update){
        this.$message.success('Update question Success');
        this.$store.dispatch('updateFAQ', q);
      } else {
        this.sendMail(this.form.title, q, 'question', 2);
        this.$message.success('Add question Success');
        this.$store.dispatch('addFAQ', q);
      }
    }).catch(err =>{
      // eslint-disable-next-line no-console
      console.log(err, 'Fail add tags to question');
    });
  }
  removeTagsToQuestion (q: FaqRes, tids: string){
    this.zetaAceRemoteService.removeTagsFromQuestion(q.id, tids).catch(err =>{
      // eslint-disable-next-line no-console
      console.log(err, 'Fail add tags to question');
    });
  }
  showInput () {
    this.inputVisible = true;
    this.$nextTick(() => {
      (this.$refs.saveTagInput as any).$refs.input.focus();
    });
  }
  async sendMail (message: string, questionObj: any, type: string, retryCount:  number) {
    if (retryCount <= 0) return false;
    const params  =  generalMail(type, questionObj, message);
    if (!_.isEmpty(params.toAddr)) {
      this.zetaAceRemoteService.createEmail(params)
        .catch((err: ZetaException) => {
          err.resolve();
          setTimeout(()=>{
            this.sendMail(message, questionObj, type, retryCount-1);
          }, 2000);
        });
    }
  }
  @Watch('visible_')
  handleVisible (newVal: boolean) {
    if (newVal) {
      if (this.update){
        this.originQ = _.cloneDeep(this.question);
        this.form = {
          title: this.question.title ? this.question.title : '',
          content: this.question.content ? this.question.content : '',
          tags: this.question.tags ? _.cloneDeep(this.question.tags) : [],
          inputTag: '',
        };
      } else {
        this.form = {
          title: '',
          content: '',
          tags: [],
          inputTag: '',
        };
      }
    }
  }
  @Watch('form.content')
  handleContentChange (newVal: string){
    if (uploadLoader && uploadLoader.status=== 'idle'){
      this.$nextTick(()=>{
        // const imgCount = newVal.split('img').length - 1;
        // const srcCount = newVal.split('src').length -1 ;
        // if(imgCount === srcCount){
        this.form.content  = newVal + '<p><br data-cke-filler="true"></p>';
        uploadLoader.status = 'error';
        // }
      });
    }
  }
}
</script>
<style lang="scss">
  #editor{
    position: relative;
    font-size: 14px;
    display: inline-block;
    width: 100%;
    min-height: 80px;
    border: 1px solid #DCDFE6;
    box-sizing: border-box;
    background-color: #ffffff;
    outline: none;
    word-break: break-word;
    &:focus{
      border: 1px solid #569ce1;
    }
    line-height: normal;
  }
  .ck-balloon-panel{
    z-index: 3000 !important;
  }
  .ck.ck-placeholder:before, .ck .ck-placeholder:before{
    color: #c0c4cc !important;
  }
  .ck.ck-editor__editable .image .ck-progress-bar{
    height: 3px !important;
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
      margin-left: 10px;
      vertical-align: bottom;
    }
  }
  .title{
    .el-input__inner{
      padding-right: 60px;
    }
  }

</style>

