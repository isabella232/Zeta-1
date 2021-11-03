<template>
  <div class="overview-panel">
    <div class="block">
      <el-timeline>
        <el-timeline-item
          v-for="(activity, index) in activities"
          :key="index"
          :icon="activity.icon"
          :type="activity.type"
          :color="activity.content == activeCatalog ? '#4d8cca' : ''"
          :size="activity.size">
          <span :id="activity.content + '_catalog'" class="timeline-item" @click="scrollTo(activity.content)">{{activity.content}}</span>
        </el-timeline-item>
      </el-timeline>
    </div>
    <div class="overview-content">
      <div class="base-info-div">
        <div class="center-panel">
          <div class="description content-div" id="Description">
            <div class="head-div">
              <p class="dataSet-label">Description</p>
            </div>
            <div v-if="!hasDesc && editDisabled" class="no-data-div">No content yet</div>
            <div :class="!editDisabled ? 'edit-mode': ''" v-else>
              <ckeditor id="desc" :editor="editor.InlineEditor" v-model="descModel" :disabled="editDisabled" :config="editorConfig"></ckeditor>
            </div>
          </div>
          <!--div class="common content-div" id="Common">
            <div class="head-div">
              <p class="dataSet-label">Common</p>
            </div>
            <div v-if="!hasCommon && editDisabled" class="no-data-div">No content yet</div>
            <div :class="!editDisabled ? 'edit-mode': ''" v-else>
              <ckeditor id="common" :editor="editor.InlineEditor" v-model="commonModel" :disabled="editDisabled" :config="editorConfig"></ckeditor>
            </div>
          </div-->
          <div class="custom content-div" style="width: 100%;">
            <div class="sections">
              <div v-for="panel in customerModel" :key="panel.title" class="section-div" :id="panel.title">
                <div class="head-div">
                  <p class="dataSet-label">{{ panel.title }}</p>
                  <p title="Move section down" class="cursor zeta-icon-caret" @click="moveDown(panel.title)" v-if="!editDisabled">Move down</p>
                  <p title="Move section up" class="cursor zeta-icon-caretup" @click="moveUp(panel.title)" v-if="!editDisabled">Move up</p>
                  <p title="Remove the current section" class="cursor zeta-icon-delet" @click="removeSection(panel.title)" v-if="!editDisabled">Remove</p>
                </div>
                  <!--p title="two column section" class="cursor" @click="secondSection(panel.title)" v-if="!editDisabled">Two Col</p>
                  <p title="single column section" class="cursor" @click="singleSection(panel.title)" v-if="!editDisabled">Single Col</p-->
                
                <div v-if="!hasContent(panel.content) && editDisabled"><div class="no-data-div">No content yet</div></div>
                <div class="content-panel" :class="[{ 'active-sections-2': panel.sections == 2 }, { 'active-sections-3': panel.sections == 3 }]" v-else>
                  <div class="edit-panel" :class="!editDisabled ? 'edit-mode': ''">
                    <ckeditor :id="panel.title.toLowerCase()" :editor="editor.InlineEditor" v-model="panel.content" :disabled="editDisabled" :config="editorConfig"></ckeditor>
                  </div>
                  <!--div class="edit-panel" :class="!editDisabled ? 'edit-mode': ''" v-if="panel.sections > 1">
                    <ckeditor :editor="editor.InlineEditor" v-model="panel.secondContent" :disabled="editDisabled" :config="editorConfig"></ckeditor>
                  </div-->
                </div>
              </div>
            </div>
            <el-button class="add-section-btn" type="primary" plain @click="centerDialogVisible = true" v-if="!editDisabled"><i class="zeta-icon-add">Add</i></el-button>
            <el-dialog title="Add New Section" :visible.sync="centerDialogVisible" width="30%">
              <table class="display">
                <!--tr>
                  <th width="80">Layout</th>
                  <td>
                    <el-radio-group v-model="newSectionCols">
                      <el-radio-button label="1">Single Column</el-radio-button>
                      <el-radio-button label="2">Two Columns</el-radio-button>
                      <el-radio-button label="3">Three Columns</el-radio-button>
                    </el-radio-group>
                  </td>
                </tr-->
                <tr>
                  <th>Title</th>
                  <td>
                    <el-input class="new-item" v-model="newItem" placeholder="First Section Title"></el-input>
                  </td>
                </tr>
                <!--tr v-if="newSectionCols != '1'">
                  <th>Title</th>
                  <td>
                    <el-input class="new-item" v-model="newItem2" placeholder="Second Section Title"></el-input>
                  </td>
                </tr>
                <tr v-if="newSectionCols == '3'">
                  <th>Title</th>
                  <td>
                    <el-input class="new-item" v-model="newItem3" placeholder="Third Section Title"></el-input>
                  </td>
                </tr-->
              </table>
              <span slot="footer" class="dialog-footer">
                <el-button @click="centerDialogVisible = false; newItem = '';">Cancel</el-button>
                <el-button type="primary" @click="centerDialogVisible = false; addPanel(); newItem = '';">Add</el-button>
              </span>
            </el-dialog>
          </div>
          <div class="reference content-div" id="Reference">
            <DatasetsReference :isOwner="isOwner" :domain="domain" :subDomain="subDomain" :reference="reference" :isEdit="!editDisabled" @submit-conf="updateDomainConf" @send-email="sendMail"/>
          </div>
          <div class="comments content-div" id="Comments">
            <h2 class="dataSet-label">Comments</h2>
            <div v-if="!hasComments"><div class="no-data-div">No content yet</div></div>
            <DatasetsComment ref="comment" :data="commentsModel"
                              @update-comment="updateDomainComment"
                              @delete-comment="deleteDomainComment"
                              @add-reply="addDomainComment"
                              @update-reply="updateDomainReply"
                              @delete-reply="deleteReplyComments"
                              @send-email="sendMail"/>
          </div>
        </div>
        <div class="right-side-panel">
          <DatasetsEnotify style="margin-bottom: 20px;" :isOwner="isOwner" :domain="domain" :subDomain="subDomain" :newArr="newArr" :isEdit="!editDisabled"/>
          <DatasetsOwner :isOwner="isOwner" :domain="domain" :subDomain="subDomain" :owner="owner" :isEdit="!editDisabled" @submit-conf="updateDomainConf" @send-email="sendMail"/>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch } from "vue-property-decorator";
import CKEDITOR from '@/components/common/ckeditor';
import DatasetsComment from './DatasetsComment.vue';
import DatasetsEnotify from './DatasetsEnotify.vue';
import DatasetsOwner from './DatasetsOwner.vue';
import DatasetsReference from './DatasetsReference.vue';
import Util from "@/services/Util.service";
import { MyUploadAdapter } from '../FAQ/utilties';
import _ from "lodash";
import $ from 'jquery';

function MyCustomUploadAdapterPlugin(editor: any) {
  editor.plugins.get('FileRepository').createUploadAdapter = (loader: any) => {
    return new MyUploadAdapter(loader);
  };
}

@Component({
  components: {
    DatasetsComment,
    DatasetsEnotify,
    DatasetsOwner,
    DatasetsReference
  }
})

export default class DatasetOverviewPanel extends Vue {
  @Prop() data: any;
  @Prop() desc: any;
  @Prop() reference: any;
  @Prop() comments: any;
  @Prop() isOwner: any;
  @Prop() domain: any;
  @Prop() subDomain: any;
  @Prop() newArr: any;
  @Prop() owner: any;
  @Prop() customerPanel: any;
  @Prop({default: false}) editDisabled: boolean;
  editor: any = CKEDITOR;
  commonModel: string = "";
  descModel: string = "";
  commentsModel: any = [];
  customerModel: any = [];
  editorConfig: any = {
    extraPlugins: [ MyCustomUploadAdapterPlugin ],
    removePlugins: ['MediaEmbed'],
    link: {
      addTargetToExternalLinks: true
    },
    image: {
      upload: {
        types: ['png', 'jpeg', 'jpg']
      },
      image: {
        toolbar: [
          'imageStyle:full',
          'imageStyle:side',
          '|',
          'imageTextAlternative'
        ]
      }
    }
  };

  descEdit: boolean = false;
  commonEdit: boolean = false;
  centerDialogVisible: boolean = false;
  newItem: string = "";
  newItem2: string = "";
  newItem3: string = "";
  activeCatalog: string = "";
  newSectionCols: string = "1";
  clkItem: string = "";

  constructor() {
    super();
    this.commonModel = this.data;
    this.descModel = this.desc;
    this.commentsModel = this.comments;
  }

  get activities() {
    let rs: any = [{ content: 'Description' }];
    _.forEach(this.customerModel, (v: any) => {
      rs.push({ content: _.upperFirst(_.toLower(v.title)) });
    });
    rs.push({ content: 'Reference' }, { content: 'Comments' });
    return rs;
  }

  get hasDesc() {
    return !_.isEmpty(this.descModel);
  }

  get hasCommon() {
    return !_.isEmpty(this.commonModel);
  }

  get hasComments() {
    return _.size(this.commentsModel) > 0;
  }

  mounted() {
    this.handleScroll();
    window.addEventListener('scroll', this.handleScroll, true);
  }

  // desc-div func
  updateDesc() {
    this.$emit("update-desc", this.descModel)
  }

  // customer-div func
  addPanel() {
    /*let section: any = { title: _.upperFirst(_.toLower(this.newItem)), content: "" };
    if (this.newSectionCols == "1") {
      section.sections = 1;
    }else if (this.newSectionCols == "2") {
      section.sections = 2;
    }*/
    this.customerModel.push({ title: _.upperFirst(_.toLower(this.newItem)), content: "", sections: 1 });
  }

  focusEditor(id: string) {
    setTimeout(() => {
      $("[id='" + _.toLower(id) + "']").focus();
    }, 100);
  }

  hasContent(content: string) {
    return !_.isEmpty(content);
  }

  // submit event
  updateDomainComment(params: any) {
    this.$emit("update-comment", params);
  }

  deleteDomainComment(params: any) {
    this.$emit("delete-comment", params);
  }

  addDomainComment(params: any) {
    this.$emit("add-reply", params);
  }

  updateDomainReply(params: any) {
    this.$emit("update-reply", params);
  }

  deleteReplyComments(params: any) {
    this.$emit("delete-reply", params);
  }

  sendMail(params: any) {
    this.$emit("send-email", params);
  }

  clearCommentEdit() {
    const child: any = this.$refs.comment;
    if (child && child.clearCommentEdit) child.clearCommentEdit();
  }

  cancelNewReply() {
    const child: any = this.$refs.comment;
    if (child && child.cancelNewReply) child.cancelNewReply();
  }

  cancelNewComment() {
    const child: any = this.$refs.comment;
    if (child && child.cancelNewComment) child.cancelNewComment();
  }

  clearReplyEdit() {
    const child: any = this.$refs.comment;
    if (child && child.clearReplyEdit) child.clearReplyEdit();
  }

  handleScroll() {
    const contentObj: any = $(".center-panel");
    this.activeCatalog = this.clkItem;
    if (this.activeCatalog == "") {
      $(".timeline-item").removeClass("isScroll");
      _.forEachRight(this.activities, (v: any) => {
        const directObj: any = $("[id='" + v.content + "']");
        if (directObj.offset() && contentObj.offset() && contentObj.scrollTop() >= directObj.offset().top - contentObj.offset().top) {
          $("[id='" + v.content + "_catalog']").addClass("isScroll");
          this.activeCatalog = v.content;
          return false;
        }
      });
    }
    this.clkItem = "";
  }

  scrollTo(content: string) {
    const contentObj: any = $(".center-panel");
    const directObj: any = $("[id='" + content + "']");
    this.activeCatalog = content;
    $(".timeline-item").removeClass("isScroll");
    $("[id='" + content + "_catalog']").addClass("isScroll");
    this.clkItem = content;
    if(!_.isUndefined(directObj)) contentObj.scrollTop(contentObj.scrollTop() + directObj.offset().top - contentObj.offset().top);
  }

  save() {
    const conf: any = { tabs: this.commonModel, sections: JSON.stringify(this.customerModel) };
    this.$emit("submit-conf", conf);
  }

  updateDomainConf(conf: any) {
    this.$emit("submit-conf", conf);
  }

  moveUp(title: any) {
    let tempList = _.cloneDeep(this.customerModel);
    const index = _.findIndex(this.customerModel, (v: any) => { return v.title == title });
    if (index > -1 && index > 0) {
      const currRow = tempList.splice(index, 1)[0];
      tempList.splice(index - 1, 0, currRow);
    }
    this.customerModel = tempList;
  }

  moveDown(title: any) {
    let tempList = _.cloneDeep(this.customerModel);
    const index = _.findIndex(this.customerModel, (v: any) => { return v.title == title });
    if (index > -1 && index < _.size(this.customerModel) - 1) {
      const currRow = tempList.splice(index, 1)[0];
      tempList.splice(index + 1, 0, currRow);
    }
    this.customerModel = tempList;
  }

  removeSection(title: any) {
    let tempList = _.cloneDeep(this.customerModel);
    _.remove(tempList, (v: any) => { return v.title == title });
    this.customerModel = tempList;
  }

  secondSection(title: any) {
    let tempList = _.cloneDeep(this.customerModel);
    const find = _.find(tempList, (v: any) => { return v.title == title });
    if (find) {
      find.sections = 2;
      find.secondContent = "";
    }
    this.customerModel = tempList;
  }

  singleSection(title: any) {
    let tempList = _.cloneDeep(this.customerModel);
    const find = _.find(tempList, (v: any) => { return v.title == title });
    if (find) {
      find.sections = 1;
      find.secondContent = undefined;
    }
    this.customerModel = tempList;
  }

  @Watch("data")
  onDataChange() {
    this.commonModel = this.data;
  }

  @Watch("desc")
  onDescChange() {
    this.descModel = this.desc;
  }

  @Watch("comments")
  onCommentsChange() {
    this.commentsModel = this.comments;
  }

  @Watch("customerPanel")
  onCustomerPanelChange() {
    this.customerModel = this.customerPanel;
    if (_.isEmpty(this.customerModel)) {
      this.customerModel.push({ title: "Content", content: "", sections: 1 });
    }
  }
}
</script>
<style lang="scss" scoped>
.overview-panel {
  height: 100%;
  width: 100%;
  display: flex;
}
.block {
  display: flex;
  padding: 0px 2px;
}
.el-timeline {
  /deep/ .el-timeline-item__node {
    width: 16px;
    height: 16px;
  }
  /deep/ .el-timeline-item__wrapper {
    top: 0px;
  }
}
.timeline-item {
  cursor: pointer;
  color: #E4E7ED;
}
.timeline-item:hover {
  color: #569CE1;
}
.isScroll:last-child {
  color: #569CE1;
}
.overview-content {
  height: 100%;
  width: 100%;
  overflow-y: hidden;
  margin-left: 20px;
  /deep/ .ck.ck-content {
    >p {
      margin-top: 10px;
    }
  }
  /deep/ .ck-content .table {
    margin: 1em 0em !important;
  }
}
.content-div {
  margin-bottom: 20px;
}
.dataSet-label {
  font-weight: 700;
  font-size: 16px;
  color: #999999;
  padding: 0 10px;
  height: 30px;
  line-height: 30px;
  display: inline-block;
}
.head-div {
  border-bottom: 1px solid #E4E7ED;
  width: 100%;
  padding-bottom: 10px;
}
.user-item-div {
  margin: 10px;
  margin-right: 2px;
}
.owner-btn {
  float: right;
  line-height: 30px;
}
.no-data-div {
  line-height: 60px;
  color: #CACBCF;
  padding-left: 10px;
}
.base-info-div {
  display: flex;
  height: 100%;
}
.center-panel {
  width: calc(100% - 460px);
  height: 100%;
  overflow-y: auto;
  margin: 0px 20px;
}
.center-panel::-webkit-scrollbar {
  width: 0;
}
.right-side-panel {
  width: 420px;
}
.submit-btn {
  margin-bottom: 10px;
  padding-left: 10px;
}
.content-panel {
  display: flex;
  justify-content: space-between;
}
.content-panel.active-sections-2 {
  .edit-panel {
    width: 49.5% !important;
  }
}
.edit-panel {
  width: 100%;
}
.edit-mode {
  border: 1px dashed #999;
  /deep/ .ck-focused {
    border: 0px !important;
    box-shadow: none !important;
  }
}
.cursor {
  cursor: pointer;
  float: right;
  color: #E4E7ED;
  font-family: ArialMT, Arial;
  font-size: 14px;
  padding: 0 5px;
  height: 30px;
  line-height: 30px;
}
.add-section-btn.el-button--primary.is-plain {
  margin-bottom: 20px;
  width: 100%;
  border: 1px dashed #E4E7ED;
  background: none;
  color: #CACBCF;
  [class^="zeta-icon-"],
  [class*=" zeta-icon-"] {
    font-family: "zeta-font", 'ArialMT', 'Arial' !important;
    color: #E4E7ED;
    font-size: 14px;
  }
}
.add-section-btn.el-button--primary.is-plain:hover {
  color: #FFF;
  background-color: #4d8cca;
}
.cursor:hover {
  color: #78b0e7;
}
.sections {
  .section-div:not(:last-child) {
    margin-bottom: 20px;
  }
}
table.display {
  border-spacing: 0 20px;
  width: 100%;
  > tr {
    min-height: 35px;
    padding: 5px 0;
    > th,
    > td {
      padding: 0;
    }
    > th {
      text-align: left;
    }
    > td {
      padding-left: 30px;
      color: #333;
    }
  }
}
.el-radio-button__inner {
  height: 35px;
  line-height: 35px;
  font-weight: 400;
  padding: 0px 20px;
}
.zeta-icon-add:before {
  padding-right: 5px;
}
</style>
