<template>
  <div class="card">
    <div class="header">
      <div class="el-divider el-divider--horizontal">
        <span
          class="initials"
          @click="openDomain()"
        >{{ getInitials }}</span>
        <div
          class="el-divider__text is-left title"
          @click="openDomain()"
        >
          {{ getTitle }}
        </div>
      </div>
    </div>
    <div class="sub-div">
      <span
        class="sub sub-all-div"
        @click="openDomain()"
      >All</span>
      <span
        v-for="item in getSubDomain"
        :key="item"
        class="sub"
        @click="openDomain(item)"
      >{{ item }}</span>
      <el-dropdown
        v-if="dropdownVisible()"
        @command="openDomain"
      >
        <span class="el-dropdown-link">
          <i class="zeta-icon-simpleList" />
        </span>
        <el-dropdown-menu slot="dropdown">
          <el-dropdown-item
            v-for="item in getSubDomainDropItem"
            :key="item"
            :command="item"
          >
            {{ item }}
          </el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>
    </div>
    <div class="desc-div">
      <span
        class="desc"
        :title="replaceHtml(data.domain_desc)"
      >{{ replaceHtml(data.domain_desc) }}</span>
    </div>
    <div class="statistics-div">
      <div class="statistics-item">
        <span class="count eq-color">{{ formatThousandth(data.cnt) }}</span>
        <span class="item-name">Total Tables</span>
      </div>
      <div class="statistics-item">
        <span class="count eq-color">{{ formatThousandth(data.user_cnt) }}</span>
        <span class="item-name">Unique Users</span>
      </div>
      <div class="statistics-item">
        <span class="count eq-color">{{ formatThousandth(data.batch_cnt) }}</span>
        <span class="item-name">Unique Batches</span>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop } from 'vue-property-decorator';
import _ from 'lodash';
import $ from 'jquery';
@Component({
  components: {}
})
export default class DomainCard extends Vue {
  @Prop() data: any;

  get getInitials(): string {
    return _.isEmpty(this.data.domain) ? '' : this.data.domain.substring(0, 1);
  }

  get getTitle(): string {
    return _.isEmpty(this.data.domain) ? '' : this.data.domain.substring(1);
  }

  formatThousandth(number: any) {
    if (_.isNumber(number)) {
      const reg = /\d{1,3}(?=(\d{3})+$)/g;
      return (number + '').replace(reg, '$&,');
    }

    return 'NA';
  }

  openDomain(subDomain: any) {
    const params: any = {
      name: subDomain ? subDomain : this.data.domain,
      isDomain: !subDomain,
      subDomain: this.data.sub_domain ? _.split(this.data.sub_domain, ',') : [],
      domain: this.data.domain
    };
    this.$emit('open-sub-domain', params);
  }

  get getSubDomain() {
    const subDomainArr: any = this.data.sub_domain ? _.split(this.data.sub_domain, ',') : [];
    let subDivWidth: any = (window.innerWidth - 540) * 0.49 - 90;
    let size: any = _.size(subDomainArr);
    for (let i = 0; i < size; i++) {
      subDivWidth = subDivWidth - (_.size(subDomainArr[i]) * 10 + 20);
      if (subDivWidth < 0) {
        size = i;
      }
    }
    return this.data.sub_domain ? _.slice(_.split(this.data.sub_domain, ','), 0 , size) : undefined;
  }

  get getSubDomainDropItem() {
    const subDomainArr: any = this.data.sub_domain ? _.split(this.data.sub_domain, ',') : [];
    let subDivWidth: any = (window.innerWidth - 540) * 0.49 - 90;
    let size: any = _.size(subDomainArr);
    for (let i = 0; i < size; i++) {
      subDivWidth = subDivWidth - (_.size(subDomainArr[i]) * 10 + 20);
      if (subDivWidth < 0) {
        size = i;
      }
    }
    return this.data.sub_domain ? _.slice(_.split(this.data.sub_domain, ','), size , _.size(subDomainArr)) : undefined;
  }

  dropdownVisible() {
    const subDomainArr: any = this.data.sub_domain ? _.split(this.data.sub_domain, ',') : [];
    const subDivWidth: any = (window.innerWidth - 540) * 0.49 - 90;
    let totalWidth: any = 0;
    let visibleFlg = false;
    _.forEach(subDomainArr, (v: any, i: any) => {
      totalWidth = totalWidth + _.size(v) * 10 + 20;
      visibleFlg = subDivWidth - totalWidth < 0;
      if (visibleFlg) return visibleFlg;
    });

    return visibleFlg;
  }

  replaceHtml(desc: any) {
    return desc.replace(/<[^>]*>|/g,'').replace(/&nbsp;/gi, '');
  }

  handleCommand(command: any) {
    this.openDomain(command);
  }
}
</script>
<style lang="scss" scoped>
.card {
  height: 100%;
  width: calc(100% - 30px);
  margin: 30px 0;
  border-bottom: 2px solid #569CE1;
  .header {
    .el-divider {
      .title {
        color: #FFF;
        font-weight: 700;
        font-size: 20px;
        left: 0 !important;
      }
      .initials {
        position: absolute;
        color: #FFF;
        cursor: pointer;
        font-weight: 700;
        font-size: 36px;
        transform: translateY(-63%);
        z-index: 1;
        padding-left: 10px;
        text-shadow: 3px -3px 0px #569ce1;
      }
    }
  }
  .sub-div {
    padding-left: 5px;
    padding-top: 13px;
    .sub {
      color: #569ce1;
      cursor: pointer;
      font-size: 16px;
      padding: 0 10px;
    }
    .sub:first-child {
      padding-left: 5px;
    }
    .sub:not(:first-child) {
      border-left: 1px solid #333;
    }
    .na_sub {
      color: #CACBCF !important;
      cursor: auto !important;
    }
  }
  .desc-div {
    height: 50px;
    padding-top: 20px;
    padding-left: 10px;
    color: #CACBCF;
    .desc {
      overflow: hidden;
      -webkit-line-clamp: 3;
      -webkit-box-orient: vertical;
      display: -webkit-box;
    }
    .show-details {
      color: #569ce1;
      cursor: pointer;
      display: block;
      padding-top: 5px;
    }
  }
  .statistics-div {
    display: flex;
    flex-direction: row;
    justify-content: center;
    padding-bottom: 20px;
    padding-top: 30px;
    .statistics-item {
      width: 33%;
      text-align: center;
      font-size: 28px;
      .zeta-icon-arrow {
        font-size: 20px;
      }
      .eq-color {
        color: #CACBCF;
      }
      .up-color {
        color: #5CB85C;
      }
      .down-color {
        color: #E53917;
      }
      .eq-icon {
        color: #CACBCF;
        display: inline-block;
        transform: rotate(-90deg);
      }
      .up-icon {
        color: #5CB85C;
        display: inline-block;
        transform: rotate(180deg);
      }
      .down-icon {
        color: #E53917;
        display: inline-block;
      }
      .item-name {
        display: block;
        font-size: 14px;
        padding: 5px 0;
      }
    }
  }
  .content {
    border-bottom: 2px solid #569CE1;
    padding: 20px 0;
    height: 160px;
  }
}
.el-divider--horizontal {
  display: block;
  height: 2px;
  width: 100%;
  margin: 24px 0;
}
.el-divider {
  background-color: #569CE1;
  position: relative;
}
.el-divider__text.is-left {
  cursor: pointer;
  transform: translateY(-50%);
}
.el-divider__text {
  position: absolute;
  background-color: #569CE1;
  box-shadow: 15px 0px 0px #FFF;
  padding-left: 38px;
  padding-right: 20px;
}
.el-dropdown-link {
  margin-left: 10px;
  font-size: 16px;
  .zeta-icon-simpleList {
    cursor: pointer;
    color: #569ce1;
  }
}
</style>
