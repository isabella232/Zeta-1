<template>
  <div
    v-loading="loading"
    class="metadata-comment"
  >
    <div class="metadata-comment-div">
      <div class="metadata-comment-title">
        <div class="comment-title">
          {{ title }}
        </div>
        <button
          class="close-btn"
          @click="() => close()"
        >
          <i class="zeta-icon-close" />
        </button>
      </div>
      <div class="metadata-comment-input">
        <el-input
          v-if="!edit"
          placeholder="Write comments"
          @focus="openEdit"
        />
        <div v-else>
          <el-input
            v-model="newComment"
            type="textarea"
            :autosize="{ minRows: 8, maxRows: 10 }"
            placeholder="Write comments"
            :autofocus="autofocus"
          />
          <el-button
            class="cancel-btn"
            type="default"
            plain
            @click="cancel"
          >
            Cancel
          </el-button>
          <el-button
            class="submit-btn"
            type="primary"
            :disabled="submitDisabled"
            @click="submit()"
          >
            Submit
          </el-button>
        </div>
      </div>
      <div class="metadata-comment-content">
        <el-card
          v-for="(n) in getComments"
          :id="n.comment_id"
          :key="n.comment_id"
          shadow="never"
        >
          <el-row>
            <el-col :span="3">
              <div class="nav-item-icon">
                <span class="avatar-bg">
                  <dss-avatar
                    inline
                    size="small"
                    cycle
                    :nt="n.cre_user"
                    class="avatar"
                  />
                </span>
              </div>
            </el-col>
            <el-col :span="21">
              <p class="card-name">
                {{ n.cre_user }}
              </p>
              <p
                class="card-content"
                v-html="repalceBr(n.comment)"
              />
              <p class="card-end">
                {{ n.cre_date }}							
                <el-button
                  v-if="vueRef == 'MetaData'"
                  type="info"
                  icon="el-icon-edit-outline"
                  @click="reply(n.comment_id)"
                />
                <i
                  v-if="isOwner(n.cre_user) && vueRef != 'DA'"
                  class="zeta-icon-delet"
                  @click="delComment(n)"
                />
              </p>
            </el-col>
          </el-row>
          <el-row
            v-for="(sn, index) in n.replys"
            :id="sn.comment_id"
            :key="index"
            class="reply-row"
          >
            <el-col
              :span="3"
              class="blank-col"
            />
            <el-col :span="3">
              <div class="nav-item-icon">
                <span class="avatar-bg">
                  <dss-avatar
                    inline
                    size="small"
                    cycle
                    :nt="sn.cre_user"
                    class="avatar"
                  />
                </span>
              </div>
            </el-col>
            <el-col :span="18">
              <p class="card-name">
                {{ sn.cre_user }}
              </p>
              <p
                class="card-content"
                v-html="repalceBr(sn.comment)"
              />
              <p class="card-end">
                {{ sn.cre_date }}
              </p>
            </el-col>
          </el-row>
          <div v-if="n.edit">
            <el-col
              :span="3"
              class="blank-col"
            />
            <el-col
              :span="21"
              style="padding: 10px 0;"
            >
              <el-input
                v-model="replyComment"
                type="textarea"
                :autosize="{ minRows: 3, maxRows: 6 }"
                placeholder="Write comments"
                :autofocus="autofocus"
                @input="updateReply(n.comment_id)"
              />
              <el-button
                class="cancel-btn"
                type="default"
                plain
                @click="replyCancel(n.comment_id)"
              >
                Cancel
              </el-button>
              <el-button
                class="submit-btn"
                type="primary"
                @click="submit(n.comment_id)"
              >
                Submit
              </el-button>
            </el-col>
          </div>
        </el-card>
      </div>	
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Provide, Watch } from 'vue-property-decorator';
import Util from '@/services/Util.service';
import uuid from 'uuid/v4';
import _ from 'lodash';
import $ from 'jquery';
@Component({
  components: {
		
  },
})
export default class MetadataComment extends Vue {
  @Prop() data: any;
  @Prop() vueRef: any;
  @Prop() loading: boolean;
  @Prop() scrollId: string;
  edit = false;
  newComment = '';
  autofocus = true;
  commentArr: any = [];
  replyMap: any = {};
  replyEditMap: any = {};
  replyComment: any = '';
  replyId: any = '';

  get title (): string {
    return !_.isUndefined(this.data.comments) ? 'Comments (' + this.getComments.length + ')' : 'Comments (0)';
  }

  get getComments (): Array<any> {
    if (this.vueRef == 'MetaData') {
      const allComments = _.sortBy(this.data.comments, ['cre_date']);
	    const commentIds = _.uniq(_.map(allComments, 'comment_id'));
      const comments: any = [];
      _.forEach(commentIds, (v: any) => {
        const pick = _.pickBy(allComments, (sv: any) => { return v == sv.comment_id; }) || [];
        const replys: any = [];
	      let i = 0;
	      _.forEach(pick, (v: any, k: any) => {
	        if (i > 0) replys.push(v);
	        i++;
        });
	      const find = _.find(allComments, (sv: any) => { return v == sv.comment_id; }) || [];
	      find.replys = replys;
        find.edit = this.replyEditMap[v] ? this.replyEditMap[v] : false;
        comments.push(find);
	    });
      this.commentArr = _.reverse(comments);
    } else {
      this.commentArr = _.reverse(_.sortBy(this.data.comments, ['cre_date'])) || [];
    }
    return this.commentArr;
  }

  get submitDisabled (): boolean {
    return _.isEmpty(this.newComment);
  }

  constructor () {
	  super();
  }

  repalceBr (comments: any) {
	  return comments.replace(new RegExp('\n', 'gm'), '<br/>');
  }

  openEdit () {
	  this.edit = true;
	  this.newComment = '';
  }

  scrollToCard () {
    const contentObj: any = $('.metadata-comment-content');
    const directObj: any = $('#' + this.scrollId);
    if (!_.isUndefined(directObj)) contentObj.scrollTop(contentObj.scrollTop() + directObj.offset().top - contentObj.offset().top);
  }

  cancel () {
    this.edit = false;
  }

  allReplyCancel () {
	  this.replyEditMap = {};
  }

  reply (id: any) {
	  this.replyEditMap[this.replyId] = false;
	  this.replyId = id;
    this.replyMap[id] = 'Re:';
    this.replyEditMap[id] = true;
    this.replyComment = 'Re:';
	  this.data.comments = _.cloneDeep(this.data.comments);
  }

  replyCancel (id: any) {
	  this.replyMap[id] = '';
    this.replyEditMap[id] = false;
	  this.replyComment = 'Re:';
	  this.data.comments = _.cloneDeep(this.data.comments);
  }

  updateReply (id: any) {
	  this.replyMap[id] = this.replyComment;
  }

  submit (id: any) {
	  if (this.vueRef == 'DA') {
	    const params: any = {
	      platform: this.data.pltfrm_name,
	      db: this.data.db_name,
        table: this.data.table_name,
	      comment: this.newComment,
	      cre_user: Util.getNt(),
      };

      this.$emit('submit-comment', params);
	  } else if (this.vueRef == 'MetaData') {
      const find = _.find(this.commentArr, v => v.comment_id == id);
      const nts: any = find ? _.map(find.replys, 'cre_user') : [];
	    if (find) nts.push(find.cre_user); // add comment user
	    nts.push(Util.getNt()); // add current reply user
      const params: any = {
	      comment_id: id ? id : uuid(),
	      comment: id ? this.replyMap[id] : this.newComment,
        nts: nts,
      };

	    this.$emit('submit-comment', params);
    }
  }

  open () {
	  $('.metadata-comment').animate({width: '450px'});
    if (!_.isEmpty(this.scrollId)) this.scrollToCard();
  }

  close () {
    $('.metadata-comment').animate({width: '0px'});
  }

  isOwner (userNt: any): boolean {
    const nt: any = Util.getNt();
	  return _.isEqual(nt, userNt);
  }

  delComment (params: any) {
    this.$emit('delete-comment', params);
  }
}
</script>
<style lang="scss" scoped>
@import '@/styles/global.scss';
.metadata-comment {
	background-color: #F4F7F9;
	box-shadow: 2px 0px 6px 0px rgba(0, 0, 0, 0.2);
	display: flex;
	flex-direction: column;
	height: 100%;
	width: 0px;
	opacity: .98;
	overflow: hidden;
	position: fixed;
	right: 0;
    top: 0;
	z-index: 4;
}

.metadata-comment-title {
	display: flex;
	height: 43px;
	justify-content: space-between;
	line-height: 43px;
	width: 100%;
	> div {
		font-size: 20px;
	}
	> .close-btn {
		color: $zeta-font-color;
		cursor: pointer;
		&:hover {
			color: $zeta-font-light-color;
		}
		> i {
			font-size: 18px;
			color: inherit;
		}
	}
}

.comment-title {
	font-size: 18px;
    font-style: normal;
	font-weight: 700;
    color: #1E1E1E;
}

.metadata-comment-input {
	min-height: 30px;
	width: calc(100% - 18px);
	.el-input {
		width: 100%;
	}
}

.metadata-comment-content {
	height: calc(100% - 108px);
	overflow-y: auto;
	padding: 10px 0;
	width: 100%;
}

.metadata-comment-div{
	height: 100%;
	padding: 15px 15px;
	width: 420px;
}

.el-card {
	background-color: #F4F7F9;
	border: 0px;
	border-bottom: 1px solid #ccc;
	opacity: .98;
	/deep/ .el-card__body {
		padding-bottom: 10px;
	}
	.reply-row {
		padding-top: 10px;
	}
}

.separator-line {
	border-bottom: 1px solid #ccc;
}

.nav-item-icon {
  align-items: center;
  display: flex;
  justify-content: left;
  width: 200px;
  [class^="zeta-icon-"],
  [class*=" zeta-icon-"] {
    font-size: 24px;
    color: #94bfe1;
  }
  .avatar-bg {
	background-position: center;
	background-size: 30px 30px;
    border-radius: 15px;
    display: block;
    height: 30px;
    width: 30px;
  }
  .avatar {
	background-position: center;
	background-size: 30px 40px;
    border-radius: 15px;
    display: block;
    height: 30px;
    width: 30px;
  }
}

.card-name {
	color: #569CE1;
	font-size: 16px;
	font-style: normal;
	font-weight: 700;
    line-height: 20px;
}

.card-content {
	font-size: 14px;
	font-style: normal;
	font-weight: 400;
	line-height: 20px;
	margin: 5px 0px;
	white-space: pre-wrap;
}

.card-end {
	color: #999999;
	font-size: 14px;
	font-style: normal;
	font-weight: 400;
	line-height: normal;
	/deep/ .el-button {
		background-color: #F4F7F9;
		border: 0;
		color: #999999;
		float: right;
		font-size: 20px;
		min-width: 20px;
		opacity: .98;
		padding: 0;
		width: 20px;
	}
}

.submit-btn {
  float: right;
  margin-top: 22px;
  margin-right: 5px;
}

.cancel-btn {
  background: inherit;
  background-color: rgba(255, 255, 255, 0);
  border-color: rgba(86, 156, 225, 1);
  border-radius: 4px;
  border-style: solid;
  border-width: 1px;
  box-shadow: none;
  color: #569ce1;
  float: right;
  margin-top: 22px;
}

.blank-col {
	height: 1px;
}
.zeta-icon-delet {
	float: right;
	line-height: 17px;
	margin-right: 5px;
	color: #999;
	cursor: pointer;
}
</style>

