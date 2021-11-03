<template>
  <div class="comment-div">
    <el-card
      v-for="n in getComments"
      :id="n.comment_id"
      :key="n.comment_id"
      shadow="never"
    >
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
      <div class="comment-div">
        <div class="comment-header">
          {{ n.cre_user_name || n.cre_user }}
        </div>
        <div class="comment-body">
          <div class="comment-content">
            <p
              v-if="editCommentId != n.comment_id"
              class="comment"
              v-html="n.comment"
            />
            <quill-editor
              v-else
              v-model="n.comment"
              :options="editorOptions"
            />
          </div>
          <div
            v-if="editCommentId != n.comment_id"
            class="comment-actions"
          >
            <span
              v-if="isOwner(n.cre_user)"
              class="btn"
              @click="edit(n)"
            >Edit</span>
            <span
              v-if="isOwner(n.cre_user)"
              class="btn"
            >
              <el-popover
                v-model="n.visible"
                placement="top"
                width="180"
              >
                <p>Are you sure to delete this?</p>
                <div style="text-align: right; margin: 0; margin-top:5px;">
                  <el-button
                    size="mini"
                    type="text"
                    @click="n.visible = false"
                  >cancel</el-button>
                  <el-button
                    type="primary"
                    size="mini"
                    @click="deleteComment(n)"
                  >confirm</el-button>
                </div>
                <span
                  slot="reference"
                  class="delete-text"
                >Delete</span> 
              </el-popover>
            </span>
            <span
              class="btn"
              @click="addReply(n)"
            >Reply</span>
            <span class="comment-date">{{ n.cre_date }}</span>
          </div>
          <div
            v-else
            class="comment-actions"
          >
            <span
              class="btn"
              @click="saveComment(n)"
            >Save</span>
            <span
              class="btn"
              @click="cancelEdit(n)"
            >Cancel</span>
          </div>
          <div class="comment-reply">
            <el-row
              v-for="(sn, index) in n.replys"
              :id="sn.reply_id"
              :key="index"
              class="reply-row"
            >
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
              <div class="reply-div">
                <div class="comment-header">
                  {{ sn.cre_user_name || sn.cre_user }}
                </div>
                <div class="comment-body">
                  <div class="comment-content">
                    <p
                      v-if="editReplyId != sn.reply_id"
                      class="comment"
                      v-html="sn.comment"
                    />
                    <quill-editor
                      v-else
                      v-model="sn.comment"
                      :options="editorOptions"
                    />
                  </div>
                  <div class="comment-actions">
                    <div
                      v-if="editReplyId != sn.reply_id"
                      class="comment-actions"
                    >
                      <span
                        v-if="isOwner(sn.cre_user)"
                        class="btn"
                        @click="editReply(sn)"
                      >Edit</span>
                      <span
                        v-if="isOwner(sn.cre_user)"
                        class="btn"
                      >
                        <el-popover
                          v-model="sn.visible"
                          placement="top"
                          width="180"
                        >
                          <p>Are you sure to delete this reply?</p>
                          <div style="text-align: right; margin: 0; margin-top:5px;">
                            <el-button
                              size="mini"
                              type="text"
                              @click="sn.visible = false"
                            >cancel</el-button>
                            <el-button
                              type="primary"
                              size="mini"
                              @click="deleteReply(sn)"
                            >confirm</el-button>
                          </div>
                          <span
                            slot="reference"
                            class="delete-text"
                          >Delete</span> 
                        </el-popover>
                      </span>
                      <span class="comment-date">{{ sn.cre_date }}</span>
                    </div>
                    <div
                      v-else
                      class="comment-actions"
                    >
                      <span
                        class="btn"
                        @click="saveReply(sn)"
                      >Save</span>
                      <span
                        class="btn"
                        @click="cancelEditReply(sn)"
                      >Cancel</span>
                    </div>
                  </div>
                </div>
              </div>
            </el-row>
            <el-row v-if="replyCommentId == n.comment_id">
              <div class="add-reply">
                <div class="nav-item-icon">
                  <span class="avatar-bg">
                    <dss-avatar
                      inline
                      size="small"
                      cycle
                      :nt="currentNt"
                      class="avatar"
                    />
                  </span>
                </div>
                <quill-editor
                  v-model="newReply"
                  :options="editorOptions"
                  style="padding-left: 10px; width: 100%;"
                />
              </div>
              <div class="comment-actions reply">
                <span
                  class="btn"
                  @click="saveNewReply(n)"
                >Save</span>
                <span
                  class="btn"
                  @click="cancelNewReply(n)"
                >Cancel</span>
              </div>
            </el-row>
          </div>
        </div>
      </div>
    </el-card>
    <div class="add-comment">
      <!--div class="nav-item-icon">
              <span v-bind:style="{backgroundImage:'url(./img/icons/user_white.png)'}" class="avatar-bg">
                  <span v-bind:style="{backgroundImage:`url(//ihub.corp.ebay.com/images/ldap/${currentNt}.jpg)`}" class="avatar"></span>
              </span>
          </div-->
      <quill-editor
        v-model="newComment"
        :options="editorOptions"
      />
    </div>
    <el-button
      class="submit-btn"
      type="primary"
      :disabled="newComment.length <= 0"
      @click="submitNewComment()"
    >
      Submit
    </el-button>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Inject, Watch } from 'vue-property-decorator';
import { quillEditor } from 'vue-quill-editor';
import Util from '@/services/Util.service';
import uuid from 'uuid/v4';
import _ from 'lodash';
import $ from 'jquery';


const EDITOR_OPTION = {
  modules: {
  },
};

@Component({
  components: {
    quillEditor,
  },
})
export default class DatasetsComment extends Vue {
  @Prop() data: any;
  editorOptions = EDITOR_OPTION;
  activeNames: any = [];
  commentArr: any = [];
  replyMap: any = {};
  replyEditMap: any = {};
  replyComment = '';
  replyId = '';
  editCommentId = '';
  editComment = '';
  replyCommentId = '';
  newReply = '';
  editReplyId = '';
  editReplyComment = '';
  currentNt: any = Util.getNt();
  newComment: any = '';
    
  get getComments (): Array<any> {
    const allComments = _.sortBy(this.data, ['cre_date']);
    const commentIds = _.uniq(_.map(allComments, 'comment_id'));
    this.activeNames = commentIds;
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
    return this.commentArr;
  }

  handleChange (val: any) {
    console.log(val);
  }

  isExpand (commentId: string): boolean {
    const find: any = _.find(this.activeNames, (v: string) => { return v == commentId; });
    return find ? find : false;
  }

  isOwner (nt: string) {
    return Util.getNt() == nt;
  }

  edit (obj: any) {
    const find: any = _.find(this.data, (v: any) => { return v.comment_id == this.editCommentId && !v.reply_id; });
    if (find) this.cancelEdit(find);
    this.editCommentId = obj.comment_id;
    this.editComment = obj.comment;
  }

  deleteComment (obj: any) {
    this.$emit('delete-comment', obj);
    const find = _.find(this.commentArr, v => v.comment_id == obj.comment_id);
    const nts: any = find ? _.map(find.replys, 'cre_user') : [];
    nts.push(Util.getNt());
    const params: any = {
      action: 'removecomment',
      to: 'All',
      nts: nts,
      content: obj.comment,
    };

    this.$emit('send-email', params);
  }

  saveComment (obj: any) {
    this.$emit('update-comment', obj);
    const find = _.find(this.commentArr, v => v.comment_id == obj.comment_id);
    const nts: any = find ? _.map(find.replys, 'cre_user') : [];
    nts.push(Util.getNt());
    const params: any = {
      action: 'updatecomment',
      to: 'All',
      nts: nts,
      content: obj.comment,
    };

    this.$emit('send-email', params);
  }

  addReply (obj: any) {
    this.replyCommentId = obj.comment_id;
    this.newReply = '';
  }

  cancelEdit (obj: any) {
    obj.comment = this.editComment;
    this.clearCommentEdit();
  }

  editReply (obj: any) {
    const find: any = _.find(this.data, (v: any) => { return v.reply_id == this.editReplyId; });
    if (find) this.cancelEditReply(find);
    this.editReplyId = obj.reply_id;
    this.editReplyComment = obj.comment;
  }

  saveReply (obj: any) {
    this.$emit('update-reply', obj);
    const find = _.find(this.commentArr, v => v.comment_id == obj.comment_id);
    const nts: any = find ? _.map(find.replys, 'cre_user') : [];
    nts.push(Util.getNt());
    const params: any = {
      action: 'updatereply',
      to: 'All',
      nts: nts,
      content: obj.comment,
    };

    this.$emit('send-email', params);
  }

  deleteReply (obj: any) {
    this.$emit('delete-reply', obj);
    const find = _.find(this.commentArr, v => v.comment_id == obj.comment_id);
    const nts: any = find ? _.map(find.replys, 'cre_user') : [];
    nts.push(Util.getNt());
    const params: any = {
      action: 'removereply',
      to: 'All',
      nts: nts,
      content: obj.comment,
    };

    this.$emit('send-email', params);
  }

  cancelEditReply (obj: any) {
    obj.comment = this.editReplyComment;
    this.clearReplyEdit();
  }

  clearCommentEdit () {
    this.editCommentId = '';
    this.editComment = '';
  }

  clearReplyEdit () {
    this.editReplyId = '';
    this.editReplyComment = '';
  }

  saveNewReply (obj: any) {
    const params: any = {};
    params.comment_id = obj.comment_id;
    params.reply_id = uuid();
    params.comment = this.newReply;
    this.$emit('add-reply', params);
    const find = _.find(this.commentArr, v => v.comment_id == obj.comment_id);
    const nts: any = find ? _.map(find.replys, 'cre_user') : [];
    nts.push(Util.getNt());
    const paramsMail: any = {
      action: 'addreply',
      to: 'All',
      nts: nts,
      content: this.newReply,
    };

    this.$emit('send-email', paramsMail);
  }

  cancelNewReply (obj: any) {
    this.replyCommentId = '';
    this.newReply = '';
  }

  cancelNewComment () {
    this.newComment = '';
  }

  submitNewComment () {
    const params: any = {};
    params.comment_id = uuid();
    params.comment = this.newComment;
    this.$emit('add-reply', params);
        
    const paramsMail: any = {
      action: 'addcomment',
      to: 'All',
      nts: [Util.getNt()],
      content: params.newComment,
    };

    this.$emit('send-email', paramsMail);
  }
}
</script>
<style lang="scss" scoped>
.comment-div {
    width: 100%;
}
.el-card {
	border: 0px;
	border-bottom: 1px solid #ccc;
	/deep/ .el-card__body {
        display: flex;
		padding-bottom: 10px;
        padding-left: 0px;
	}
	.reply-row {
		padding-top: 10px;
        display: flex;
	}
}
.nav-item-icon {
    width: 35px;
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
.reply-div {
    width: calc(100% - 40px);
    padding-left: 10px;
}
.comment-header {
    margin-bottom: 10px;
}
.comment-actions {
    margin-top: 10px;
    font-size: 12px;
    clear: both;
    >span {
        margin-right: 10px;
    }
    .btn {
        color: #569ce1;
        cursor: pointer;
    }
}
.add-reply {
    display: flex;
    padding-top: 10px;
}
.reply {
    margin-top: 45px;
    margin-left: 45px;
}
.add-comment {
  padding-top: 20px;
}
.submit-btn {
  float: right;
  margin: 10px 0;
}
</style>
