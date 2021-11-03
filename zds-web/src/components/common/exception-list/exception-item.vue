<template>
  <div class="error-message-item" :class="messageType">
    <div class="item-header">
      <span
        class="title"
        :class="{'clickable': exceptionTitle.clickable}"
        :title="exceptionTitle.title"
        @click="exceptionTitle.clickable ? exceptionTitle.goto() : () => {}"
      >
        <i :class="exceptionTitle.icon" />
        {{ exceptionTitle.title }}
      </span>
      <span class="clickable" @click="resolve(exception)">
        <i class="el-icon-close" />
      </span>
    </div>
    <div class="item-detail">
      <div class="item-detail-message">
        <div v-if="!exception.useHtml" class="message-content">{{ exception.message }}</div>
        <div v-else class="message-content" v-html="exception.message" />
        <!--div v-if="extendable" class="extend-btn clickable" @click="toggleExtend">
          <i :class="extend? 'el-icon-arrow-down' : 'el-icon-arrow-right'" class="extend-btn-icon" />
        </div-->
      </div>
      <!--div v-if="extendable" v-show="extend" class="split-line">***</div>
      <div v-if="extendable" v-show="extend" class="item-detail-extend">
        <div v-if="hasMessage" class="detail-extend-message">
          <div class="detail-extend-message-name">Message</div>
          <div class="detail-extend-message-content">{{ exception.causeMessage }}</div>
        </div>
        <div v-if="hasStackTrace" class="detail-extend-message">
          <div class="detail-extend-message-name">StackTrace</div>
          <ul class="detail-extend-stacktrace-list detail-extend-message-content">
            <li
              v-for="(st,$i) in exception.stackTrace"
              :key="$i"
            >{{ ($i + 1) + '.' + formatStackTrace(st) }}</li>
          </ul>
        </div>
        <div v-if="hasCause" class="detail-extend-message">
          <div class="detail-extend-message-name">Cause</div>
          <div class="detail-extend-message-content">{{ exception.cause || exception.context }}</div>
        </div
      </div>-->
      <div class="copy-btn">
        <a href="#" @click="openLog">
          Copy Log
        </a>
        <!--i class="zeta-icon-export" @click="openLog"></i-->
      </div>
      <div class="item-tools-btn">
        <span v-if="hasRefresh" class="clickable" @click="handleClick(exception)">Refresh</span>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop } from "vue-property-decorator";
import _ from "lodash";
import { ZetaException, StackTrace } from "@/types/exception";
import { IWorkspace, WorkSpaceType } from "@/types/workspace";
import Util from "@/services/Util.service";
import copy from "copy-to-clipboard";

const workspaceIconMap = {
  [WorkSpaceType.NOTEBOOK]: "zeta-icon-notebook",
  [WorkSpaceType.NOTEBOOK_COLLECTION]: "zeta-icon-notebook",
  [WorkSpaceType.NOTEBOOK_ZEPPELIN]: "zeta-icon-notebook",
  [WorkSpaceType.DATAMOVE]: "zeta-icon-datamove",
  [WorkSpaceType.DATAVALIDATION]: "zeta-icon-DataValidation",
  [WorkSpaceType.SQLCONVERTER]: "zeta-icon-sqlConvert",
  [WorkSpaceType.METADATA]: "zeta-icon-table",
  [WorkSpaceType.DASHBOARD]: "zeta-icon-udf"
};
const pathIconMap: Dict<string> = {
  repository: "zeta-icon-fold1",
  metadata: "zeta-icon-medatada",
  schedule: "zeta-icon-workbook",
  release: "zeta-icon-send"
};
const routerPath: string[] = [
  "repository",
  "schedule",
  "settings",
  "da",
  "metadata",
  "registervdm"
];
@Component({
  components: {}
})
export default class ExceptionItem extends Vue {
  @Prop()
  exception: ZetaException;

  extend = false;
  get hasRefresh() {
    return this.exception.code === "Refresh" ? true : false;
  }
  get messageType() {
    return this.exception.type ? this.exception.type.toLowerCase() : "";
  }
  get hasMessage() {
    return Boolean(this.exception.causeMessage);
  }
  get hasStackTrace() {
    return !_.isEmpty(this.exception.stackTrace);
  }
  get hasCause() {
    return Boolean(this.exception.cause || this.exception.context);
  }
  get hasZetaServerReqId() {
    // return (
    //   this.exception.responseHeaders &&
    //   this.exception.responseHeaders["zds-server-req-id"]
    // );
    return this.exception.originalMessage;
  }
  get extendable() {
    return this.hasMessage || this.hasStackTrace || this.hasCause;
  }
  get exceptionTitle() {
    const props = this.exception.properties;
    const defaultIcon = "zeta-icon-info";
    const isWorkspace = props.path == "notebook" && props.workspaceId;
    const t: Dict<any> = {
      icon: defaultIcon,
      title: "",
      clickable: false
    };
    if (isWorkspace && props.workspaceId) {
      const workspace: IWorkspace = this.$store.state.workspace.workspaces[
        props.workspaceId
      ];
      if (!workspace) {
        return t;
      }
      const type = workspace.type;
      t.icon = workspaceIconMap[type] || t.icon;
      if (
        workspace &&
        (workspace.type == WorkSpaceType.NOTEBOOK ||
          workspace.type == WorkSpaceType.NOTEBOOK_ZEPPELIN)
      ) {
        t.title = workspace.name || type.toUpperCase();
      } else {
        t.title = props.path[0].toUpperCase() + props.path.substring(1);
      }
      t.clickable = true;
    } else {
      t.icon = pathIconMap[props.path] || t.icon;
      t.title = props.path[0].toUpperCase() + props.path.substring(1);
      if (_.includes(routerPath, props.path)) {
        t.clickable = true;
      }
    }
    if (t.clickable) {
      t.goto = () => {
        if (isWorkspace && props.workspaceId) {
          this.$router.push({ path: "/" });
          this.$store.dispatch("setActiveNotebookById", props.workspaceId);
        } else {
          if (_.includes(routerPath, props.path)) {
            this.$router.push({ path: "/" + props.path });
          }
        }
      };
    }
    return t;
  }
  resolve(e: ZetaException) {
    e.resolve();
  }
  formatStackTrace(st: StackTrace) {
    return `${st.methodName} method of \`${st.className}\` at ${st.fileName} line ${st.lineNumber}`;
  }
  toggleExtend() {
    if (this.extendable) {
      this.extend = !this.extend;
    }
  }
  openLog() {
    const copyFlg: any = copy(
      //this.exception.responseHeaders["cache-control"]
      JSON.stringify(this.exception, null, 4)
    );
    if (copyFlg) {
      this.$message.success("Copy to clipboard success!");
    }
    // const href = `${location.protocol}//${
    //   location.host
    // }/${Util.getPath()}share/#/errFullLog`;
    // window.localStorage.setItem(
    //   "error-full-log",
    //   JSON.stringify(this.exception)
    // );
    // window.open(href, "_blank");
  }

  handleClick(e: ZetaException) {
    e.resolve();
    if (e.properties.path === "zeta" && this.messageType === "warning") {
      Util.reloadPage();
    }
  }
}
</script>

<style lang="scss" scoped>
@import "@/styles/global.scss";

$errorColor: #e53917;
$warningColor: #e6a23c;
$white: #ffffff;
$error-header-color: mix($errorColor, $white, 70%);
$error-header-bg-color: mix($errorColor, $white, 20%);

$error-content-color: $errorColor;
$error-content-color-light: mix($errorColor, $white, 70%);
$error-content-bg-color: mix($errorColor, $white, 10%);
$error-border-color: $errorColor;

$warning-header-color: mix($warningColor, $white, 70%);
$warning-header-bg-color: mix($warningColor, $white, 20%);

$warning-content-color: $warningColor;
$warning-content-color-light: mix($warningColor, $white, 70%);
$warning-content-bg-color: mix($warningColor, $white, 10%);
$warning-border-color: $warningColor;
.clickable {
  cursor: pointer;
}
.error-message-item {
  margin-bottom: 5px;
  border: 1px solid $error-border-color;
  border-radius: 5px;
  background-color: $error-content-bg-color;
  width: 350px;
  .item-header {
    border-top-left-radius: 5px;
    border-top-right-radius: 5px;
    background-color: $error-header-bg-color;
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0 20px;
    font-size: 14px;
    line-height: 30px;
    .title,
    i {
      color: $error-header-color;
    }
    .title {
      min-width: 50px;
      text-overflow: ellipsis;
      white-space: nowrap;
      overflow: hidden;
    }
  }
  .item-detail {
    color: $error-content-color;
    font-size: 14px;
    padding: 10px 20px;
    > .item-detail-message {
      display: flex;
      justify-content: space-between;
      line-height: 30px;
      word-break: break-word;
      > .message-content {
        // overflow: hidden;
        // text-overflow: ellipsis;
        display: -webkit-box;
        // -webkit-box-orient: vertical;
        // -webkit-line-clamp: 4;
        overflow-y: auto;
        max-height: 150px;
      }
      i.extend-btn-icon {
        font-size: 10px;
        padding: 2px;
        border: 1px solid $error-content-color;
        border-radius: 1em;
      }
    }
    > .split-line {
      line-height: 30px;
    }
    > .item-detail-extend {
      font-size: 12px;
      margin: 0 0 10px 0;
      line-height: 20px;
      max-height: 250px;
      overflow: auto;
      .detail-extend-message {
        &:nth-child(1) {
          margin-top: 0;
        }
        margin: 10px 0;
        .detail-extend-message-name {
          margin-bottom: 5px;
        }
        .detail-extend-message-content {
          word-break: break-word;
          color: $error-content-color-light;
        }
        ul.detail-extend-stacktrace-list {
          > li {
            margin: 5px 0;
            list-style-type: none;
          }
        }
      }
    }
  }
}
.warning {
  border: 1px solid $warning-border-color;
  background-color: $warning-content-bg-color;
  .item-header {
    background-color: $warning-header-bg-color;
    .title,
    i {
      color: $warning-header-color;
    }
  }
  .item-detail {
    color: $warning-content-color;
    > .item-detail-message {
      i.extend-btn-icon {
        border: 1px solid $warning-content-color;
      }
    }
    > .item-detail-extend {
      .detail-extend-message {
        .detail-extend-message-content {
          color: $warning-content-color-light;
        }
      }
    }
    > .item-tools-btn {
      display: flex;
      justify-content: flex-end;
      .clickable {
        border: 1px solid $warning-border-color;
        font-size: 12px;
        padding: 0 6px;
        border-radius: 2px;
      }
    }
  }
}
.copy-btn {
  text-align: end;
  .zeta-icon-export {
    cursor: pointer;
    color: #569ce1;
  }
  .zeta-icon-export:hover {
    color: #4d8cca;
  }
}
</style>
