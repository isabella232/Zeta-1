import Vue, { PluginObject, VNodeDirective } from 'vue';
import * as Logger from '@/plugins/logs/zds-logger';
import _ from 'lodash';
const clickListener = _.debounce((name: string, tags: Dict<string>) => {
  console.debug('tracking by vue dirctive', name , tags);
  Logger.counter(name, 1, tags);
}, 500, { leading: true, trailing: false });
const getTrackingParams = (el: HTMLElement) => {
  const name = el.getAttribute('zeta-tracking-name') || '';
  if (!name) {
    return;
  }
  const tagsStr = el.getAttribute('zeta-tracking-tags') || '{}';
  let tags = {};
  try {
    tags = JSON.parse(tagsStr);
  } catch {
    tags = {};
  }
  return {
    name,
    tags,
  };
};
const ZdsMetricsPlugin: PluginObject<any> =  {
  install(Vue) {
    let listener: any = null; 
    Vue.directive('click-metric', {
      bind: function(el: HTMLElement, binding: VNodeDirective){
        const name = binding.arg;
        const tags = binding.value;
                
        el.setAttribute('zeta-tracking-name', name);
        el.setAttribute('zeta-tracking-tags', JSON.stringify(tags));
        listener = function(this: HTMLElement, event: MouseEvent) {
          const trackingParams = getTrackingParams(this);
          if (!trackingParams) {
            return;
          } else {
            clickListener(trackingParams.name, trackingParams.tags);
          }
        };
        el.addEventListener('click', listener);
      },
      update: function(el: HTMLElement, binding: VNodeDirective) {
        const name = binding.arg;
        const tags = binding.value;
        el.setAttribute('zeta-tracking-name', name);
        el.setAttribute('zeta-tracking-tags', JSON.stringify(tags));
      },
      unbind: function(el: HTMLElement) {
        el.removeEventListener('click', listener);
      }
    });
  }
};
export default ZdsMetricsPlugin;