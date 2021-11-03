import Vue, { VueConstructor } from 'vue';
import VueRouter, { RouteConfig } from 'vue-router';
import { Store } from 'vuex';
import { WorkSpaceType } from '@/types/workspace';
// import subview cmpnts
import NavMetadata from '@/components/common/Navigator/SubView/NavMetedata.vue';
import PublicNotebook from '@/components/common/Navigator/SubView/PublicNotebook.vue';
import { WorkspaceSrv } from '@/services/Workspace.service';
export enum NavType {
  PATH = 'PATH',
  ACTION = 'ACTION',
  LINK = 'LINK',
  SUB_VIEW = 'SUB_VIEW',
  CONTAINER = 'CONTAINER'
}
type NavBase = {
  id: string;
  name: string;
  iconClass: string;
  type: NavType;
  class?: string;
};
/**
 * path
 * go to other page
 */
type NavPathProp = {
  path: string;
  meta: any;
};
export type NavPath = NavBase & NavPathProp;

/**
 * action
 * doing
 */
export type ClickAction = (EventBus: Vue, router: VueRouter, store: Store<any>) => void;

type NavActionProp = {
  action: ClickAction;
  zetaActions: ZETA_ACTIONS;
};
export type NavAction = NavBase & NavActionProp;

type NavLinkProp = {
  path: string;
};
export type NavLink = NavBase & NavLinkProp;

type NavSubViewProp = {
  componentName: string;
  component: VueConstructor;
  zetaActions: ZETA_ACTIONS;
};
export type NavSubView = NavBase & NavSubViewProp;

type NavContainerProp = {
  children: NavItem[];
};
export type NavContainer = NavBase & NavContainerProp;

export type NavItem = NavPath | NavAction | NavLink | NavSubView | NavContainer;
export enum ZETA_ACTIONS {
  'OPEN_METADATA' = 'OPEN_METADATA',
  'OPEN_PUBLIC_REPORTS' = 'OPEN_PUBLIC_REPORTS',
  'ADD_DATA_MOVE' = 'ADD_DATA_MOVE',
  'ADD_DATA_VALIDATION' = 'ADD_DATA_VALIDATION',
  'ADD_SQL_CONVERT' = 'ADD_SQL_CONVERT',
  'CLONE_NOTEBOOK' = 'CLONE_NOTEBOOK',
  'CLONE_DASHBOARD' = 'CLONE_DASHBOARD',
  'CLONE_ZPL_PUB_NOTE' = 'CLONE_ZPL_PUB_NOTE',
  'CLONE_ZPL_NOTE' = 'CLONE_ZPL_NOTE',
  'OPEN_SHARED_NOTEBOOK' = 'OPEN_SHARED_NOTEBOOK',
}
export const config: NavItem[] = [
  {
    id: 'Metadata',
    name: 'Metadata',
    iconClass: 'zeta-icon-medatada',
    type: NavType.SUB_VIEW,
    component: NavMetadata,
    componentName: 'metadata',
    zetaActions: ZETA_ACTIONS.OPEN_METADATA
  },
  {
    id: 'Repository',
    name: 'Repository',
    iconClass: 'zeta-icon-fold1',
    type: NavType.PATH,
    path: '/repository',
    meta: {
      keepAlive: true
    }
  },
  {
    id: 'covert',
    name: 'SQL Convert',
    iconClass: 'zeta-icon-sqlConvert',
    type: NavType.ACTION,
    action: (eventBus: any, router: VueRouter, store) => {
      const ws = WorkspaceSrv.tool(
        'SQL Convert',
        WorkSpaceType.SQLCONVERTER
      );
      store.dispatch('addActiveWorkSpace', ws);
      // redirect to notebook
      router.push('/notebook');
    },
    zetaActions: ZETA_ACTIONS.ADD_SQL_CONVERT
  },
  {
    id: 'datamove',
    name: 'Data Move',
    iconClass: 'zeta-icon-dataMove',
    type: NavType.ACTION,
    action: (eventBus, router, store) => {
      const ws = WorkspaceSrv.tool('DataMove', WorkSpaceType.DATAMOVE);
      store.dispatch('addActiveWorkSpace', ws);
      // redirect to notebook
      router.push('/notebook');
    },
    zetaActions: ZETA_ACTIONS.ADD_DATA_MOVE
  },
  {
    id: 'datavalidation',
    name: 'Data Validation',
    iconClass: 'zeta-icon-DataValidation',
    type: NavType.ACTION,
    action: (eventBus, router, store) => {
      const ws = WorkspaceSrv.tool(
        'DataValidation',
        WorkSpaceType.DATAVALIDATION
      );
      store.dispatch('addActiveWorkSpace', ws);
      // redirect to notebook
      router.push('/notebook');
    },
    zetaActions: ZETA_ACTIONS.ADD_DATA_VALIDATION
  },
  {
    id: 'Schedule',
    name: 'Scheduler',
    iconClass: 'zeta-icon-workbook',
    type: NavType.PATH,
    path: '/schedule',
    meta: {
      keepAlive: true
    }
  },
  {
    id: 'PublicReport',
    name: 'Public Notebook',
    iconClass: 'zeta-icon-notebook',
    type: NavType.SUB_VIEW,
    component: PublicNotebook,
    componentName: 'publicNotebook',
    zetaActions: ZETA_ACTIONS.OPEN_PUBLIC_REPORTS
  },
  {
    id: 'Workspace',
    name: 'Workspace',
    iconClass: 'zeta-icon-workspace',
    type: NavType.PATH,
    path: '/notebook',
    meta: {
      keepAlive: true
    }
  },
  {
    id: 'Release',
    name: 'Release',
    iconClass: 'zeta-icon-send',
    type: NavType.PATH,
    path: '/release',
    meta: {
      reload: true
    }
  },
  {
    id: 'Da',
    name: 'Data Model',
    iconClass: 'zeta-icon-DA',
    type: NavType.PATH,
    path: '/da',
    meta: {
      keepAlive: true
    }
  }
];
export const toolsConfig: NavItem[] = [
  {
    id: 'FAQ',
    name: 'Ask Question',
    iconClass: 'zeta-icon-faq',
    type: NavType.PATH,
    path: '/FAQ',
    meta: {
      reload: true
    }
  },
  {
    id: 'wiki',
    name: 'User Guide',
    type: NavType.LINK,
    iconClass: 'zeta-icon-guidebook',
    path: 'https://wiki.vip.corp.ebay.com/display/ND/Zeta'
  },
  {
    id: 'email',
    name: 'Help',
    type: NavType.LINK,
    iconClass: 'zeta-icon-help-final',
    // path: 'mailto:DL-eBay-ZETA@ebay.com'
    path: 'https://wiki.vip.corp.ebay.com/x/-Nm4Ig'
  }
];
