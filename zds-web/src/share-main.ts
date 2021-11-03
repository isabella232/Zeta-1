/* init service */
import Util from '@/services/Util.service';
import { loginWithoutToken, initSharedFavoriteStore} from '@/services/App-init.service';
/* Components */
import Vue from 'vue';
import App from './AppShare.vue';
import {shareRouter} from '@/router';
import {shareStore} from '@/stores/index';
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

import AvatarPlugin from '@/plugins/avatar';
Vue.use(AvatarPlugin);
/**
 * ckeditor5, rich text editor
 */
import CKEditor from '@ckeditor/ckeditor5-vue';
Vue.use( CKEditor );

/** sso */
loginWithoutToken();

Vue.use(ElementUI, { locale });
Vue.use(VueRouter);
Vue.prototype.$appName = 'zeta-share';

import ZdsMetricsPlugin from '@/plugins/logs/metric';
Vue.use(ZdsMetricsPlugin);

/** default show loading */
const $loading = $('#loading');
const $app = $('#app');
const app = new Vue({
  router: shareRouter,
  store: shareStore,
  render: h => h(App)
}).$mount('#app');
Util.register(app);
/** init store */
const store = app.$store;
const nt = Util.getNt();
store.dispatch('initUserInfo', { nt:Util.getNt() }).then(() => {
  return initSharedFavoriteStore(app);
});
// initKeyboardListener();

