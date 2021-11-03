import { PluginObject } from 'vue';
import Message from 'element-ui/packages/message/index';

const CustomElementUIPlugin: PluginObject<any> =  {
  install(Vue) {
    const $msg = function(option) {
      if(!option.hasOwnProperty('showClose')) {
        option.showClose = true;
      }
      return Message(option);
    };
    ['success', 'warning', 'info', 'error'].forEach(type => {
      $msg[type] = options => {
        if (typeof options === 'string') {
          options = {
            message: options,
          };
        }
        options.type = type;
        return $msg(options);
      };
    });
    Vue.prototype.$message = $msg;
  },
};

export default CustomElementUIPlugin;
