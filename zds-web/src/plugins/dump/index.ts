import DumpFileDialog from './dump-dialog.vue';

import Vue, { PluginObject } from 'vue';
interface DialogAPI {
  showDialog: (url: string) => void;
  close: () => void;
  getVisible: () => void;
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
function showNext($instance: Vue, dumpQueue: string[]){
  const visible = ($instance as any).getVisible();
  if  (!visible) {
    if  (dumpQueue.length > 0) {
      const currentUrl = dumpQueue.shift();
      Vue.nextTick(()=>{
        ($instance as any).showDialog(currentUrl);
      });
    } else {
      ($instance as any).close();
    }
  }
}

const DumpFilePlugin: PluginObject<any> =  {
  install(Vue) {
    // const dumpQueue: string[] = [];
    // let $instance: Vue = null;
    
    Vue.prototype.$dumpDialog = function(url: string){
      
      const $instance = mountComponent(DumpFileDialog);
      $instance.$nextTick(() => {
        ($instance as any).showDialog(url);
      });
      return $instance;
      // dumpQueue.push(url);

      // showNext($instance,dumpQueue);
    };
    
    // Vue.prototype.$dumpDialogClose = function(){
    //   showNext($instance,dumpQueue);
    // };
  }
};
declare module 'vue/types/vue' {
  interface Vue {
    $dumpDialog: (url: string) => void;
    $dumpDialogClose: () => void;
  }
}
export default DumpFilePlugin;