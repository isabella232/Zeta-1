<template>
  <div class="card-wrapper">
    <div
      v-if="loading"
      class="loading"
    >
      <i class="el-icon-loading" />
    </div>
    <div
      v-else-if="userInfo"
      class="user-card"
    >
      <header>
        <img
          :src="profileImg(nt)"
          :alt="displayName"
        >
        <div>
          <a
            :href="profileLink(nt)"
            target="__blank"
          >
            <h1>{{ displayName }}</h1>
          </a>
          <h2>{{ userInfo.eBayPositionText }}</h2>
          <h2>{{ userInfo.department }}</h2>
          <h2>{{ userInfo.eBaySite }}</h2>
        </div>
      </header>
      <div class="content">
        <div class="row">
          <strong>NT: </strong>
          <span>{{ nt }}</span>
        </div>
        <div class="row">
          <strong>Email: </strong>
          <a :href="'mailto:' + userInfo.mail">{{ userInfo.mail }}</a>
        </div>
      </div>
    </div>
    <div v-else>
      No Data
    </div>
  </div>
</template>

<script lang="ts">
import { Vue, Component, Prop, Watch } from 'vue-property-decorator';

@Component({})
export default class UserCard extends Vue {
  @Prop({ required: true }) nt: string;

  mounted () {
    this.getUser();
  }
  @Watch('nt')
  getUser () {
    if (!this.userInfo) {
      this.$store.dispatch('getUserByNT', this.nt);
    }
  }
  profileLink (nt: string) {
    return `https://hub.corp.ebay.com/profile/${nt}`;
  }
  profileImg (nt: string) {
    return `https://ossserver6.dss.vip.ebay.com/userinfo/icon/${nt}`;
  }
  get loading () {
    return this.$store.state.user.user.loading;
  }
  get userInfo () {
    return this.$store.state.user.generalUserInfo[this.nt];
  }
  get displayName () {
    return `${this.userInfo.last_name}, ${this.userInfo.preferred_name}`;
  }
}
</script>

<style lang="scss" scoped>
.card-wrapper {
  min-width: 200px;
  .loading {
    text-align: center;
    font-size: 2em;
  }
  .user-card {
    header {
      display: flex;
      margin-bottom: 8px;
      img {
        width: 60px;
        height: 70px;
        margin-right: 14px;
        display: block;
      }
      h1 {
        font-size: 16px;
      }
      h2 {
        font-size: 14px;
      }
    }
    .content {
      .row {
        margin-bottom: 4px;
        strong {
          display: inline-block;
          min-width: 70px;
          text-align: right;
          padding-right: 6px;
        }
      }
    }
  }
}
</style>
