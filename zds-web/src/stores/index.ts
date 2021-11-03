import Vue from 'vue';
import Vuex from 'vuex';
import dataMove from '@/stores/modules/DataMoveStore';
import user from '@/stores/modules/UserStore';
import GroupManagement from '@/stores/modules/GroupStore';
import workspace from '@/stores/modules/WorkspaceStore';
import repository from '@/stores/modules/RepositoryStore';
import dataValidation from '@/stores/modules/DataValidationStore';
import notificationStore from '@/stores/modules/NotificationStore';
import zetaAceStore from '@/stores/modules/ZetaAceStore';
import DashboardStore from '@/stores/modules/DashboardStore';
import PackageStore from '@/stores/modules/PackageStore';
import { IWorkspaceStore } from '@/types/workspace';
import { IRepositoryStore } from '@/types/repository';
import doeSearch from '@/stores/modules/DoeSearchStore';
import ScheduleStore from '@/stores/modules/ScheduleStore';
import Release from '@/stores/modules/ReleaseStore';
import Metadata from '@/stores/modules/MetadataStore';
import ZetaExceptionStore from '@/stores/modules/ZetaExceptionStore';
import FavoriteStore from '@/stores/modules/favorite-store';
import publicNotebook from '@/stores/modules/public-notebook-store';
import MetasheetStore from '@/stores/modules/MetasheetsStore';
import VuexLoading from 'vuex-loading-plugin';
import SAStore from '@/stores/modules/SaStore';

Vue.use(Vuex);

export interface IStore {
  workspace: IWorkspaceStore;
  repository: IRepositoryStore;
}

const store = new Vuex.Store<IStore>({
  modules: {
    workspace,
    dataMove,
    user,
    GroupManagement,
    repository,
    dataValidation,
    doeSearch,
    notificationStore,
    zetaAceStore,
    DashboardStore,
    PackageStore,
    ScheduleStore,
    Release,
    Metadata,
    FavoriteStore,
    ZetaExceptionStore,
    publicNotebook,
    MetasheetStore,
    SAStore,
  },
  strict: false,
  plugins: [VuexLoading.create()],
});
const shareStore = new Vuex.Store<IStore>({
  modules: {
    user,
    FavoriteStore,
    ScheduleStore,
  },
  strict: false,
  plugins: [VuexLoading.create()],
});
export { store, shareStore };
