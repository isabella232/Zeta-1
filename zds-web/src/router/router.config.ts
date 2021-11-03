// import router cmppnts
import HomePage from '@/views/home-page-view.vue';
import WorkSpaceView from '@/views/WorkSpaceView.vue';
import Metadata from '@/views/share-views/MetadataSharedView.vue';
import MetadataSearchPage from '@/components/Metadata/SearchPage.vue';
import Notebook from '@/views/share-views/NotebookSharedView.vue';
import ZeppelinNotebook from '@/views/share-views/ZeppelinNotebookSharedView.vue';
import Dashboard from '@/views/share-views/DashboardSharedView.vue';
import Sample from '@/views/share-views/Sample.vue';
import SampleTableDisplay from '@/views/share-views/SampleTableDisplay.vue';
import ZetaSheetView from '@/views/share-views/zeta-sheet-view.vue';
import ResultView from '@/views/share-views/ResultView.vue';
import Repository from '@/views/Repository.vue';
import Release from '@/views/Release.vue';
import Settings from '@/views/Settings.vue';
import Schedule from '@/views/Schedule.vue';
import ScheduleDetail from '@/components/Schedule/schedule-detail.vue';
import ScheduleShareView from '@/views/share-views/ScheduleShareView.vue';
import DA from '@/views/DA.vue';
import Browse from '@/views/Browse.vue';
import UDFBrowse from '@/components/Metadata/udf/UDFBrowse.vue';
import UDFDetail from '@/components/Metadata/udf/UDFDetail.vue';
import SAList from '@/components/Metadata/SubjectArea/SAList.vue';
import SADetail from '@/components/Metadata/SubjectArea/SADetail.vue';
import TopicList from '@/components/Metadata/topic/List.vue';
import TopicDetail from '@/components/Metadata/topic/Detail.vue';
import DatasetsTableList from '@/components/Metadata/DatasetsTableList.vue';
import FAQ from '@/views/FAQ.vue';
import Detail from '@/components/FAQ/Detail.vue';
import Favorite from '@/views/favorite-view.vue';
import TableManagementView from '@/views/TableManagementView.vue';
import TableManagementReadOnly from '@/views/TableManagementReadOnly.vue';
import ErrFullLogView from '@/views/share-views/ErrFullLogView.vue';
/** toolkits */
import SqlConverter from '@/views/sql-converter-view.vue';
import DataMove from '@/views/data-move-view.vue';
import DataValidation from '@/views/data-validation-view.vue';
import HDFSManagement from '@/views/hdfs-management.vue';
import MetaSheet from '@/components/MetaSheet/meta-sheet.vue';
import { RouteConfig } from 'vue-router';

import { wsclient } from '@/net/ws';
import Util from '@/services/Util.service';
export const routerConfig: RouteConfig[] = [
  {
    name: 'HomePage',
    path: '/home',
    component: HomePage,
    meta: {
      displayName: 'Home',
    },
  },
  {
    name: 'HomePage2',
    path: '/',
    component: HomePage,
    meta: {
      displayName: 'Home',
    },
  },
  {
    name: 'Repository',
    path: '/repository',
    component: Repository,
    meta: {
      keepAlive: true,
    },
  },
  {
    name: 'Release',
    path: '/release',
    component: Release,
    meta: {
      reload: true,
    },
  },
  {
    name: 'Workspace',
    path: '/notebook',
    component: WorkSpaceView,
    beforeEnter: (to, from, next) => {
      Util.registerBeforeunload();
      wsclient.activeThisSession();
      next();
    },
    meta: {
      displayName: '',
      keepAlive: true,
    },
  },
  {
    name: 'Scheduler',
    path: '/schedule',
    component: Schedule,
    meta: {
      keepAlive: true,
    },
  },
  {
    name: 'SchedulerDetail',
    path: '/scheduleDetail/:id',
    component: ScheduleDetail,
    meta: {
      displayName: 'Scheduler',
    },
  },
  {
    name: 'Configuration',
    path: '/settings',
    component: Settings,
    meta: {
      keepAlive: true,
    },
  },
  {
    name: 'DA',
    path: '/da',
    component: DA,
  },
  {
    name: 'Metadata',
    path: '/metadata',
    component: () => import('@/components/Metadata/index/Index.vue'),
  },
  {
    name: 'Metadata Search',
    path: '/metadata/search',
    component: MetadataSearchPage,
    meta: {
      displayName: 'Metadata Search',
    },
  },
  {
    name: 'Register Metadata',
    path: '/metadata/register',
    component: () => import('@/views/RegisterEntry.vue'),
    meta: {
      displayName: 'Metadata',
    },
  },
  {
    name: 'Register VDM',
    path: '/metadata/register/vdm',
    component: () => import('@/views/RegisterVDM.vue'),
    meta: {
      displayName: 'Metadata',
    },
  },
  {
    name: 'Register UDF',
    path: '/metadata/register/udf',
    component: () => import('@/components/Metadata/register/UDF.vue'),
    meta: {
      displayName: 'Metadata',
    },
  },
  {
    name: 'My Collection',
    path: '/metadata/collection',
    component: () => import('@/components/Metadata/collection/index.vue'),
    meta: {
      displayName: 'Metadata',
    },
  },
  {
    name: 'Table Detail',
    path: '/metadata/tables/:name',
    component: () => import('@/components/Metadata/TableDetail/index.vue'),
    meta: {
      displayName: 'Metadata',
    },
  },
  {
    name: 'datasets',
    path: '/metadata/datasets',
    component: DatasetsTableList,
    meta: {
      displayName: 'Metadata',
    },
  },
  {
    name: 'UDF List',
    path: '/metadata/udf',
    component: UDFBrowse,
    meta: {
      displayName: 'Metadata',
    },
  },
  {
    name: 'UDF Detail',
    path: '/metadata/udf/:name',
    component: UDFDetail,
    meta: {
      displayName: 'Metadata',
    },
  },
  {
    name: 'SA List',
    path: '/metadata/sa',
    component: SAList,
    meta: {
      displayName: 'Metadata',
    },
  },
  {
    name: 'SA Detail',
    path: '/metadata/sa/:id',
    component: SADetail,
    meta: {
      displayName: 'Metadata',
    },
  },
  {
    name: 'Realtime PoC',
    path: '/metadata/topic',
    component: TopicList,
    meta: {
      displayName: 'Metadata',
    },
  },
  {
    name: 'Realtime PoC2',
    path: '/metadata/topic/:id',
    component: TopicDetail,
    meta: {
      displayName: 'Metadata',
    },
  },
  {
    name: 'Ask Question',
    path: '/FAQ',
    component: FAQ,
    meta: {
      keepAlive: true,
    },
  },
  {
    name: 'FAQ Detail',
    path: '/FAQDetail',
    component: Detail,
    meta: {
      displayName: 'Ask Question',
    },
  },
  {
    name: 'Sql Guide',
    path: '/sqlGuide',
    component: SqlConverter,
    meta: {
      keepAlive: true,
    },
  },
  {
    name: 'DataMove',
    path: '/dataMove',
    component: DataMove,
    meta: {
      keepAlive: true,
    },
  },
  {
    name: 'DataValidation',
    path: '/dataValidation',
    component: DataValidation,
    meta: {
      keepAlive: true,
    },
  },
  {
    name: 'HDFSManagement',
    path: '/hdfs',
    component: HDFSManagement,
    meta: {
      keepAlive: true,
    },
  },
  {
    name: 'Favorite',
    path: '/favorite',
    component: Favorite,
    meta: {
      keepAlive: true,
    },
  },
  {
    name: 'TableManagement',
    path: '/tablemanagement',
    component: TableManagementView,
  },
  {
    name: 'TableManagementReadOnly',
    path: '/tablemanagement_view',
    component: TableManagementReadOnly,
    meta: {
      displayName: 'TableManagement',
    },
  },
  {
    name: 'Zeta Sheets',
    path: '/zeta-sheet',
    component: MetaSheet,
    meta: {
      keepAlive: true,
      displayName: 'Sheet',
    },
  },
  {
    name: 'Auto EL',
    path: '/autoel',
    component: () => import('@/components/AutoEL/TaskList/index.vue'),
  },
  {
    name: 'Auto EL create',
    path: '/autoel/task',
    component: () => import('@/components/AutoEL/Create/index.vue'),
    children: [
      {
        name: 'Create New Task',
        path: '',
        component: () => import('@/components/AutoEL/Create/SelectTable/index.vue'),
      },
    ],
    meta: {
      displayName: 'Auto EL',
    },
  },
  {
    name: 'Auto EL detail',
    path: '/autoel/task/:id',
    component: () => import('@/components/AutoEL/Create/index.vue'),
    children: [
      {
        name: 'Select Table',
        path: '',
        component: () => import('@/components/AutoEL/Create/SelectTable/index.vue'),
      },
      {
        name: 'Data Model',
        path: 'datamodel',
        component: () => import('@/components/AutoEL/Create/DataModel/index.vue'),
      },
      {
        name: 'Release Task',
        path: 'release',
        component: () => import('@/components/AutoEL/Create/Release.vue'),
      },
      {
        name: 'Post Release',
        path: 'postrelease',
        component: () => import('@/components/AutoEL/Create/PostRelease.vue'),
      },
    ],
    meta: {
      displayName: 'Auto EL',
    },
  },
];
export const shareRouterConfig: RouteConfig[] = [
  {
    name: 'relocate',
    path: '/',
    component: Metadata,
  },
  {
    name:'Domain Browse',
    path:'/metadata/datasets',
    component: DatasetsTableList,
  },
  {
    name: 'notebook',
    path: '/notebook',
    redirect: (to) => {
      const notebookId = to.query.notebookId;
      const url = `//${location.host}/zeta/#/notebook?internal_action=OPEN_SHARED_NOTEBOOK&internal_action_params=${JSON.stringify({notebookId})}`;
      location.href = url;
      return '';
    },
  },
  {
    name: 'secnotebook',
    path: '/secnotebook',
    redirect: (to) => {
      const notebookId = to.query.notebookId;
      const url = `//${location.host}/zeta/#/notebook?internal_action=OPEN_SHARED_NOTEBOOK&internal_action_params=${JSON.stringify({notebookId})}`;
      location.href = url;
      return '';
    },
  },
  {
    name:'Multi-language Notebook',
    path:'/notebook/multilang',
    component: ZeppelinNotebook,
  },
  {
    name: 'dashboard',
    path: '/dashboard',
    component: Dashboard,
  },
  {
    name:'Sample',
    path:'/sample',
    component:Sample,
  },
  {
    name:'SampleTableDisplay',
    path:'/SampleTableDisplay',
    component:SampleTableDisplay,
  },
  {
    name:'ResultView',
    path:'/ResultView',
    component: ResultView,
  },
  {
    name:'ZetaSheet',
    path:'/zeta-sheet',
    component: ZetaSheetView,
  },
  {
    name: 'errFullLog',
    path: '/errFullLog',
    component: ErrFullLogView,
  },
  {
    name: 'My Collection',
    path: '/metadata/collection',
    component: () => import('@/components/Metadata/collection/indexSharedView.vue'),
  },
  {
    name:'SchedulerDetail',
    path:'/scheduleDetail/:id',
    component: ScheduleShareView,
  },
];
