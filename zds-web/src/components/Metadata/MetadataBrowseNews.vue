<template>
  <div class="news">
    <div class="header">
      <div class="el-divider el-divider--horizontal">
        <div class="el-divider__text is-left title">{{ getTitle }}</div>
      </div>
    </div>
    <div class="content" v-for="(item, index) in getData" :keys="item.new_id ? item.new_id : item.enotify_id">
      <div class="index-div">
        <span class="index">{{ getIndex(index + 1) }}</span>
        </div>
      <div class="info-div">
        <div>
          <span class="name" @click="jump(item.new_url ? item.new_url : item.link)">{{ item.new ? item.new : item.enotify_title }}</span>
          <span class="status" v-if="item.status">New</span>
        </div>
        <div class="content-footer">
          <span class="date">{{ item.cre_date ? item.cre_date : item.create_time }}</span>
          <!--i class="zeta-icon-hot"></i>
          <span class="info">{{ item.popular }}</span>
          <i class="zeta-icon-comment-1"></i>
          <span class="info">{{ item.comment }}</span-->
        </div>
      </div>
    </div>
    <!--span class="more" v-if="moreFlag" @click="showMore">Show More...</span>
    <span class="more" v-if="!moreFlag" @click="showMore">Show Less...</span-->
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop } from "vue-property-decorator";
import _ from "lodash";
@Component({
  components: {}
})
export default class MetadataBrowseNews extends Vue {
  @Prop() data: any;
  @Prop() type: any;
  moreFlag: boolean = false;

  get getTitle() {
    if (this.type == 'news') return "News / Feed";
    else if (this.type == 'enotify') return "Recent Announcement";

    return this.type;
  }

  get getData() {
    return (!this.moreFlag ? this.data : _.slice(this.data, 0, 5)) || this.data;
  }

  getIndex(index: any): string {
    return _.padStart(index, 2, "0");
  }

  jump(url: string) {
    window.open(url);
  }

  showMore() {
    this.moreFlag = !this.moreFlag;
  }
}
</script>
<style lang="scss" scoped>
.news {
  border-bottom: 2px solid #CACBCF;
  display: inline-block;
  width: 100%;
  .header {
    .title {
      font-weight: 700;
      font-size: 20px;
      padding-right: 10px;
      left: 0;
    }
  }
  .content {
    padding: 20px 0;
    display: flex;
    .index-div {
      width: 80px;
      text-align: center;
      font-size: 28px;
      color: #CACBCF;
    }
    .info-div {
      width: 300px;
    }
    .name {
      cursor: pointer;
      font-size: 16px;
      font-weight: 700;
    }
    .name:hover {
      color: #569CE1;
    }
    .status {
      color: #E53917;
      font-style: italic;
      font-size: 12px;
      padding-left: 5px;
    }
    .content-footer {
      color: #CACBCF;
      font-size: 14px;
      padding-top: 5px;
      .date {
        padding-right: 20px;
      }
      .info {
        padding-left: 5px;
        padding-right: 10px;
      }
      [class^="zeta-icon-"],
      [class*=" zeta-icon-"] {
        color: #CACBCF;
        font-size: 18px;
      }
    }
  }
}
.el-divider--horizontal {
    display: block;
    height: 2px;
    width: 100%;
    margin: 24px 0;
}
.el-divider {
    background-color: #CACBCF;
    position: relative;
}
.el-divider__text.is-left {
    transform: translateY(-50%);
}
.el-divider__text {
    position: absolute;
    background-color: #FFF;
    box-shadow: 15px 0px 0px #FFF;
    padding: 20px 0;
}
.more {
  color: #5299DF;
  padding-left: 80px;
  margin-bottom: 10px;
  display: block;
  cursor: pointer;
}
</style>
