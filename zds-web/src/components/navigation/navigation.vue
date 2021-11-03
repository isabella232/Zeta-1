<template>
  <div
    ref="nav"
    class="nav"
    @mouseover="open"
    @mouseleave="close"
  >
    <div
      class="nav-list"
      :class="{'closed':!opened}"
    >
      <div class="nav-items-top">
        <div
          v-click-metric:NAV_CLICK="{name: 'Logo'}"
          class="nav-logo"
          @click="pathTo('/')"
        >
          <div class="nav-item-icon">
            <img :src="'./img/logo.png'">
          </div>

          <div class="nav-item-name">
            <img :src="'./img/zeta_logo_text.svg'">
          </div>
        </div>

        <div class="nav-items-list">
          <navigation-workspace-list
            ref="wss"
            :class="{'active': activeItem ==='Workspace'}"
          />
          <navigation-item
            :name="'Repository'"
            :icon="'zeta-icon-fold1'"
            :path="'/repository'"
            :class="{'active': activeItem ==='Repository'}"
          />
          <navigation-item
            v-click-metric:NAV_CLICK="{name: 'Metadata'}"
            :name="'Metadata'"
            :icon="'zeta-icon-medatada'"
            :path="'/metadata'"
            :class="{'active': activeItem ==='Metadata'}"
          />
          <navigation-item
            v-click-metric:NAV_CLICK="{name: 'Scheduler'}"
            :name="'Scheduler'"
            :icon="'zeta-icon-workbook'"
            :path="'/schedule'"
            :class="{'active': activeItem ==='Scheduler'}"
          />
          <navigation-tools-list ref="tools" />
          <!-- <navigation-item
            v-click-metric:NAV_CLICK="{name: 'Realtime'}"
            :name="'Realtime(alpha)'"
            :icon="'zeta-icon-monitor'"
            :class="{'active': activeItem ==='Realtime'}"
            @click="openRealTime()"
          /> -->
        </div>
      </div>
      <div class="nav-items-bottom">
        <!-- TODO  favorites-->
        <div class="nav-items-list">
          <navigation-item
            v-click-metric:NAV_CLICK="{name: 'Favorite'}"
            :name="'Favorite'"
            :icon="'el-icon-star-off default'"
            :path="'/favorite'"
            :class="{'active': activeItem ==='Favorite'}"
          />
          <navigation-item
            v-click-metric:NAV_CLICK="{name: 'Ask Question'}"
            :name="'Ask Question'"
            :icon="'zeta-icon-faq'"
            :path="'/FAQ'"
            :class="{'active': activeItem ==='Ask Question'}"
          />
          <navigation-item
            v-click-metric:NAV_CLICK="{name: 'Configuration'}"
            :name="'Configuration'"
            :icon="'zeta-icon-set'"
            :path="'/settings'"
            :class="{'active': activeItem ==='Configuration'}"
          />
          <navigation-item
            :name="user.name"
            :img="user.img"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Ref, Provide } from 'vue-property-decorator';
import NavigationItem from './navigation-item.vue';
import NavigationToolsList from './navigation-tools-list.vue';
import NavigationWorkspaceList from './navigation-workspace-list.vue';
import { routerConfig } from '@/router/router.config';
import './style.scss';
import _ from 'lodash';
import Util from '@/services/Util.service';
import { ElMessageBoxOptions } from 'element-ui/types/message-box';
interface MouseEvent {
  toElement: HTMLElement;
  relatedTarget: HTMLElement;

}
@Component({
  components: {
    NavigationItem,
    NavigationToolsList,
    NavigationWorkspaceList,
  },
})
export default class Navigation extends Vue {
  @Ref('tools')
  tools: NavigationToolsList;
  @Ref('wss')
  wss: NavigationWorkspaceList;
  @Ref('nav')
  nav: HTMLElement;

  @Provide()
  pathTo (path: string) {
    if (Util.isShareApp()) {
      const url = `${location.protocol}//${location.host}/${Util.getPath()}#${path}`;
      window.open(url, 'zeta');
    } else {
      this.$router.push(path);
    }
  }
  debounceClose: () => void;
  opened = false;

  get activeItem () {
    const path = this.$route.path;
    const cfg = routerConfig.find(cfg => cfg.path === path);
    return cfg ? cfg.name: '';
  }
  mounted () {
    this.debounceClose = _.debounce(() => {
      this.opened = false;
      this.wss.close();
    }, 100, {
      trailing: true
    });
  }
  open () {
    this.opened = true;
    this.wss.open();
  }
  close (e: MouseEvent) {
    if (this.nav.contains(e.toElement || e.relatedTarget)) {
      return;
    }
    this.debounceClose();
  }
  get user () {
    const userName = this.$store.getters.user.profile.firstName + ' ' +this.$store.getters.user.profile.lastName;
    return {
      name:userName,
      img: '1',
    };
  }
  // openRealTime () {
  //   console.log('openRealTime');
  //   const html =
  //   '<div>' +
  //     '<div style="line-height: 40px;">' +
  //       '<span>Quick Start Guide</span>' +
  //       '<a style="margin-left:10px" target="_blank" href="https://wiki.vip.corp.ebay.com/x/kBQWLg">here</a>' +
  //     '</div>' +
  //   '</div>';
  //   this.$confirm(html, 'Pre-requirement:C2S proxy', {
  //     showCancelButton: false,
  //     confirmButtonText: 'Continue',
  //     showClose:true,
  //     dangerouslyUseHTMLString: true,
  //   } as ElMessageBoxOptions).then(() => {
  //     window.open('http://zeta.dss.vip.ebay.com/zpl-realtime/#/');
  //   });
  // }
}
</script>

