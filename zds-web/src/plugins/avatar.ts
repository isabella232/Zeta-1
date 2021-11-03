import { PluginObject } from 'vue';
import DssAvatar from '@/components/common/avatar.vue';
import config from '@/config/config'
declare module 'vue/types/vue' {
  interface Vue {
    $avatarUrl: (nt: string) => string;
  }
}
export default {
  install (Vue) {
    Vue.prototype.$avatarUrl = function (nt: string){
      return `${config.oss.avatarUrl}${nt}`;
    };
    Vue.component('dss-avatar', DssAvatar);
  }
} as PluginObject<any>;
