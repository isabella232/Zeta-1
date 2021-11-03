import {routerConfig, shareRouterConfig} from './router.config';
import Router from 'vue-router';
const router = new Router({
  routes: routerConfig,
});
router.beforeEach((to, from, next) => {
  const alias = (to.meta && to.meta.displayName != undefined)? to.meta.displayName : to.name;
  const title = alias ? `Zeta ${alias}`: 'Zeta';
  document.title = title;
  next();
});

const shareRouter = new Router({
  routes: shareRouterConfig,
});

export {
  router,
  shareRouter,
};
