import ZetaLoading from '@/components/common/zeta-loading.vue';

import Vue, { PluginObject } from 'vue';
interface LoadingAPI {
  loading: (message?: string, pctg?: number) => void;
  close: () => void;
}
function removeComponent($instance: Vue) {
  if ($instance.$el && $instance.$el.parentNode) {
    $instance.$el.parentNode.removeChild($instance.$el);
  }
  $instance.$destroy();
}
function mountComponent<T>(component: typeof Vue){
  const componentConstructor = Vue.extend(component);
  const $instance = new componentConstructor();
  $instance.$mount();
  document.body.appendChild($instance.$el);
  return $instance;
}
const ZetaLoadingPlugin: PluginObject<any> =  {
  install(Vue) {
    const $instance = mountComponent(ZetaLoading);
    Vue.prototype.$zetaLoading = function(message?: string, pctg?: number){
      Vue.nextTick(() => {
        ($instance as any).loading(message, pctg);
      });
      
    };

    Vue.prototype.$zetaLoadingClose = function(){
      setTimeout(() => {
        ($instance as any).close();
        $instance.$nextTick(() => {
          removeComponent($instance);
        });
      }, 100);
      
    };
  }
};
declare module 'vue/types/vue' {
  interface Vue {
    $zetaLoading: (message?: string, pctg?: number) => void;
    $zetaLoadingClose: () => void;
  }
}
export default ZetaLoadingPlugin;