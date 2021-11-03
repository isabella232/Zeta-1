/* init service */
import { loginWithToken, initStore, initKeyboardListener, excuteInternalAction } from '@/services/App-init.service';
import Util from '@/services/Util.service';

/* Components */
import Vue from 'vue';
import App from './App.vue';
import { router } from '@/router/';
import { store } from '@/stores/index';
// import './registerServiceWorker'

/* Middleware & framework */
import VueRouter from 'vue-router';
import ElementUI from 'element-ui';
import $ from 'jquery';
import locale from 'element-ui/lib/locale/lang/en';

/* Styles */
import '@/styles/element-variables.scss';
import '@/styles/include.scss';
import 'quill/dist/quill.core.css';
import 'quill/dist/quill.snow.css';
import 'quill/dist/quill.bubble.css';

import * as Logger from '@/plugins/logs/zds-logger';
import { ZetaException } from './types/exception';

Vue.use(ElementUI, { locale });
Vue.use(VueRouter);

/**
 * register local plugins
 */
import RenameFilePlugin from '@/plugins/repo/rename-file-dialog';
import CreareFilePlugin from '@/plugins/repo/create-file-dialog';
import ZdsMetricsPlugin from '@/plugins/logs/metric';
import ZetaLoadingPlugin from '@/plugins/zeta-loading';
import DumpFielPlugin from '@/plugins/dump';
import AvatarPlugin from '@/plugins/avatar';
import CustomElementUIPlugin from '@/plugins/custom-element-ui';
Vue.use(RenameFilePlugin);
Vue.use(CreareFilePlugin);
Vue.use(ZdsMetricsPlugin);
Vue.use(ZetaLoadingPlugin);
Vue.use(DumpFielPlugin);
Vue.use(CustomElementUIPlugin);
Vue.use(AvatarPlugin);

/**
 * ckeditor5, rich text editor
 */
import CKEditor from '@ckeditor/ckeditor5-vue';
Vue.use(CKEditor);

/**
 * elastic apm agent
 */
import { ApmVuePlugin } from '@elastic/apm-rum-vue';
Vue.use(ApmVuePlugin, {
  router,
  config: {
    serviceName: 'zeta',
    serverUrl: process.env.VUE_APP_APM_SERVICE_URL,
    environment: process.env.VUE_APP_ENV,
  },
});

Vue.prototype.$appName = 'zeta';
window.name = 'zeta';
/** default show loading */
const $loading = $('#loading');
const $app = $('#app');

// await store.dispatch('setUserBatchAcct', { nt })

const app: App = new Vue({
  router,
  store,
  render: (h) => h(App),
});
$loading.hide();
$app.show();
Util.register(app);

/**
 * init store
 */
// const loading = app.$loading({
// 	lock: true,
// 	text: 'Loading',
// 	spinner: 'el-icon-loading',
// 	background: 'rgba(0, 0, 0, 0.7)'
// });
app.$zetaLoading('loading user info', 0);
// app.$mount('#app');

loginWithToken().then(async () => {
  Logger.counter('LOGIN');
  const promise = initStore(app);
  return promise.catch(e => {
    throw e;
  });
}).catch((err) => {
  const ex = new ZetaException({ code: '', errorDetail: { message: 'Fail to login' } });
  store.dispatch('addException', {exception: ex});
  console.log(err);
}).then(() => {
  app.$mount('#app');
  app.$zetaLoadingClose();
  excuteInternalAction(app);
});
initKeyboardListener();
