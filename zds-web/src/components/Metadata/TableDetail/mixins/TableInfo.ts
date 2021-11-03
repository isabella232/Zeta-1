import { uniq } from 'lodash';
import { Component, Vue, Watch } from 'vue-property-decorator';
import { Action, State, Getter } from 'vuex-class';
import { Getters, Actions, State as IState } from '../store';

@Component
export class TableInfoMixin extends Vue {
  @State((state: { TableDetail: IState }) => state.TableDetail.tableInfo) tables;
  @State((state: { TableDetail: IState }) => state.TableDetail.viewInfo) views;
  @Getter(Getters.AllPlatforms) platforms;
  @Getter(Getters.TableSample) tableSample;
  @Getter(Getters.TableSubjectArea) sa;
  @Action(Actions.GetTableInfo) getTableInfo;
  @Action(Actions.GetViewInfo) getViewInfo;

  platform = '';
  db = '';

  get tableFromSamePlatform () {
    return this.tables.filter(t => t.platform === this.platform);
  }
  get databases (): string[] {
    return uniq(this.tables.map(t => t.db_name.toUpperCase()).filter(Boolean));
  }
  get viewFromSamePlatform () {
    return this.views.filter(v => v.platform.toLowerCase() === this.platform.toLowerCase());
  }
  get columnInfoFromSamePlatform () {
    return [...this.tableFromSamePlatform.map(t => ({ db: t.db_name, columns: t.column })), ...this.viewFromSamePlatform.map(t => ({ db: t.view_db, columns: t.column }))];
  }
  get selectedColumns () {
    const info = this.columnInfoFromSamePlatform.find(c => c.db === this.db);
    return info ? info.columns : [];
  }
  get columnDBOptions () {
    return this.columnInfoFromSamePlatform.map(c => c.db);
  }

  @Watch('platforms', { immediate: true })
  setDefaultPlatform () {
    if (!this.platform) {
      this.platform = this.platforms[0];
    }
  }
  @Watch('columnDBOptions')
  setDefaultColumnDB () {
    if (!this.db) {
      this.db = this.columnDBOptions[0];
    }
  }
  reloadTableInfo (table: string = this.tableSample.table_name) {
    return Promise.all([this.getTableInfo({ table }), this.getViewInfo(table)]);
  }

}