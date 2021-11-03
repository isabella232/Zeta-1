<template>
  <el-popover
    placement="bottom"
    width="380"
    trigger="click"
    v-model="visible"
    @show="init"
  >
    <div
      class="feedback-container"
      v-loading="loading"
    >
      <div class="score">
        <span><span style="color:red">*</span>{{ title }}:</span>
        <div class="score-flex">
          <el-rate
            v-model="feedbackInfo.score"
            allow-half
            :disabled="isSubmit"
          /><span v-if="feedbackInfo.score" class="score-template">{{ score.toFixed(1) }}</span>
        </div>
        <span v-if="error" class="error-info">Please pick a score!</span>
      </div>
      <div class="tag" v-if="!isSubmit">
        <div class="tag-box">
          <el-checkbox-group v-model="feedbackInfo.currentTag" size="small">
            <el-checkbox-button v-for="tag in tags" :label="tag" :key="tag">{{tag}}</el-checkbox-button>
          </el-checkbox-group>
        </div>
      </div>
      <div class="comment">
        <span>Comments:</span>
        <el-input
          v-model="feedbackInfo.comments"
          type="textarea"
          :rows="4"
          resize="none"
          maxlength="500"
          show-word-limit
          :disabled="isSubmit"
          placeholder="Detail about your feedback..."
        />
      </div>
      <div class="footer">
        <el-button
          plain
          size="mini"
          @click="hide"
        >
          Cancel
        </el-button>
        <el-button
          v-if="!isSubmit"
          type="primary"
          size="mini"
          @click="submit"
        >
          Submit
        </el-button>
      </div>
    </div>
    <el-button
      slot="reference"
      v-click-metric:NB_TOOLBAR_CLICK="{name: 'feedback'}"
      type="text"
      class="notebook-tool-btn"
    >
      <i class="zeta-icon-comment" :title="title" />
    </el-button>
  </el-popover>
</template>
<script lang="ts">
import { Vue, Component, Prop } from 'vue-property-decorator';
import FeedbackService from '@/services/remote/FeedbackService';
import Util from '@/services/Util.service';
import { Feedback, FDType } from '@/types/workspace/feedback.internal';
import { ZetaException } from '@/types/exception';

@Component({

})
export default class FeedbackView extends Vue{
  feedbackService = new FeedbackService();
  @Prop() statementId: number;
  @Prop() fdType: FDType;
  @Prop() title: string;
  visible = false;
  error = false;
  isSubmit = false;
  loading = false;
  feedbackInfo = {
    comments: '',
    score: 0,
    currentTag: [],
  };

  get tags (){
    let tag: Array<string> = [];
    switch (this.score){
      case 1:
      case 2:
        tag = ['Execution too slow', 'Not useful error message'];
        break;
      case 3:
      case 4:
        tag = ['Execution slow', 'Confusing error message'];
        break;
      case 5:
      case 6:
        tag = ['Could take less time', 'Not relevant error message'];
        break;
      case 7:
      case 8:
        tag = ['Acceptable waiting time', 'Useful error message'];
        break;
      case 9:
      case 10:
        tag = ['Fast execution', 'Helpful error message'];
        break;
      default:
        break;
    }
    return tag;
  }
  get nt (){
    return Util.getNt();
  }

  get score (){
    this.error = false;
    return 2 * this.feedbackInfo.score;
  }
  mounted () {
    this.feedbackService.props({
      path: 'notebook',
    });
  }
  init (){
    this.visible = true;
    this.error = false;
    this.feedbackInfo = {
      comments: '',
      score: 0,
      currentTag: [],
    };
    this.getFeedback();
  }
  hide (){
    this.visible = false;
  }
  submit (){
    this.error = false;
    if (this.feedbackInfo.score === 0){
      this.error = true;
      return;
    }
    const comments = this.feedbackInfo.comments +';'+ this.feedbackInfo.currentTag.toString();
    const params: Feedback = {
      usreId: this.nt,
      comments: comments,
      feedbackScore: this.score,
      feedbackType: this.fdType,
      context: this.statementId,
    };
    this.loading = true;
    this.feedbackService.addFeedback(this.statementId, params).then(({ data })=>{
      this.loading = false;
      this.visible = false;
      this.$message({
        type:'success',
        message:'Add feedback success!',
      });
    }).catch((e: ZetaException)=>{
      this.loading = false;
      console.log(e);
    });
  }
  async getFeedback (){
    this.isSubmit = false;
    this.loading = true;
    const result  = await this.feedbackService.getFeedback(this.fdType, this.statementId);
    this.loading = false;
    try {
      if (result && result.data.body.hits.hits.length>0){
        this.isSubmit = true;
        const source = result.data.body.hits.hits[0]._source;
        this.feedbackInfo = {
          comments: source.comments,
          score: source.feedbackScore?source.feedbackScore/2:0,
          currentTag: [],
        };
      }
    } catch (error) {
      this.loading = false;
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }
}
</script>
<style lang="scss">
  .feedback-container{
    .title{
      font-size: 16px;
      font-weight: bold;
      display: inline-block;
    }
    .score{
      >span{
        display: inline-block;
        margin-bottom: 2px;
      }
      .score-flex{
        display: flex;
        align-items: center;
        .score-template{
          color: #E2B258
        }
      }
      .error-info{
        font-size: 12px;
        color: red;
      }
    }
    .tag{
      margin: 5px 0;
      >p{
        margin-bottom: 10px;
      }
      /deep/ .el-checkbox-button {
        box-shadow: none !important;
        /deep/ .el-checkbox-button__inner {
          display: inline-block;
          background: inherit;
          background-color: #fff;
          border: 1px solid #569ce1 !important;
          border-radius: 4px !important;
          box-shadow: none !important;
          color: #569ce1;
          font-size: 12px;
          margin-right: 5px;
          margin-bottom: 5px;
          padding: 0 10px;
          height: 30px;
          line-height: 28px;
        }
        /deep/ .el-checkbox-button__original:checked + .el-checkbox-button__inner {
          background-color:#569ce1;
          border: 1px solid #569ce1 !important;
          color: #fff;
        }
      }
    }
    .comment{
      span{
        display: inline-block;
        margin-bottom: 10px;
      }
      /deep/ .el-textarea.is-disabled .el-textarea__inner{
        border: none;
        color: #606266;
      }
    }
    .footer{
      display: flex;
      justify-content: flex-end;
      margin-top: 10px;
    }
    /deep/ .el-icon-star-off {
      font-size: 20px !important;
    }
    /deep/ .el-icon-star-on {
      font-size: 20px !important;
    }
  }
</style>
