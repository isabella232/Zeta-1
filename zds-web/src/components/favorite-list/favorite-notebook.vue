<template>
  <div class="favorite-notebooks">
    <el-table
      ref="repoListTable"
      :data="repoList | favoriteFilter "
      row-key="notebookId"
      :default-sort="{prop: 'updateTime', order: 'descending'}"
      @sort-change="sortChange"
    >
      <el-table-column
        prop="title"
        label="Name"
        sortable="custom"
      >
        <template slot-scope="scope">
          <template v-if="scope.row.isFile">
            <span
              v-click-metric:NOTEBOOK_CLICK="{name: 'open'}"
              class="f-link"
              @click.stop="onclickFile(scope.row.notebook)"
            >
              <i :class="getIconClass(scope.row.notebook)" />
              {{ scope.row.title }}
            </span>
          </template>
        </template>
      </el-table-column>
      <el-table-column
        prop="path"
        label="Path"
        width="120px"
        sortable="custom"
      />
      <el-table-column
        prop="favorite"
        label="Action"
        width="120px"
        sortable="custom"
        align="center"
      >
        <template slot-scope="scope">
          <favorite-icon
            v-if="scope.row.isFile"
            :id="scope.row.notebookId"
          />
        </template>
      </el-table-column>
      <el-table-column
        label="Platform"
        width="120px"
        sortable="custom"
        align="center"
      >
        <template slot-scope="scope">
          <i
            v-if="scope.row.isFile"
            :class="getPlatformIcon(scope.row.notebook)"
            class="workspace-icon"
          />
        </template>
      </el-table-column>
      <el-table-column
        prop="interpretor"
        label="Notebook Type"
        width="230px"
        sortable="custom"
        class-name="nb-type"
      >
        <template
          v-if="scope.row.isFile"
          slot-scope="scope"
        >
          <span>{{ scope.row.interpretor | noteType }}
          </span>
        </template>
      </el-table-column>
      <el-table-column
        prop="runTime"
        label="Last Run Date"
        width="160px"
        sortable="custom"
      >
        <template slot-scope="scope">
          {{ scope.row.runTime | formatDateFromTimestamps }}
        </template>
      </el-table-column>
      <el-table-column
        prop="updateTime"
        label="Last Update Date"
        width="160px"
        sortable="custom"
      >
        <template slot-scope="scope">
          {{ scope.row.updateTime | formatDateFromTimestamps }}
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Inject, Ref, Watch } from 'vue-property-decorator';
import { IFile } from '@/types/repository';
import moment from 'moment';
import _ from 'lodash';
import { INotebook, IConnection, CodeType } from '@/types/workspace';
import { Table as ElTable } from 'element-ui';
import { State } from 'vuex-class';
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import { INotebookMapper } from '@/services/mapper';
import { WorkspaceSrv as NBSrv, WorkspaceSrv } from '@/services/Workspace.service';
import ZeppelinApi from '@/services/remote/ZeppelinApi';
import FavoriteIcon from '@/components/common/favorite-icon/favorite-icon-witn-store.vue';
import { WorkspaceManager } from '@/types/workspace/workspace-manager';
import { noteType } from '@/components/Repository/note-type-filter';
type Folder = string;
type SortProp = { prop: 'title' | 'interpretor' | 'runTime' | 'updateTime'; order: 'descending' | 'ascending' | null};

function getAllFilesByPath (files: Dict<IFile>, path: string): IFile[] {
  return _.chain(files).filter(f => _.startsWith(f.path, path)).value();
}
interface FavoriteItem {
  title: string;
  path: string;
  isFile: boolean;
  notebook?: IFile;
  notebookId?: string;
  runTime?: number;
  updateTime?: number;
  interpretor?: string;
  state: string;
  selectable: boolean;
  favorite: boolean;
}
@Component({
  components: {
    FavoriteIcon,
  },
  filters: {
    favoriteFilter: (list: Array<FavoriteItem>) => {
      return list.filter(item => item.favorite);
    },
    searchFilter: (list: Array<FavoriteItem>, keyword: string) => {
      if (!keyword.trim()) {
        return list;
      }
      return list.filter(item => item.title.toLowerCase().indexOf(keyword.toLowerCase()) >= 0);
    },
    formatDateFromTimestamps (timestamps: number) {
      return timestamps ? moment(timestamps).format('YYYY/MM/DD HH:mm') : '';
    },
    noteType,
  },
})
export default class FavoriteNotebookRepo extends Vue {

  @Inject()
  notebookRemoteService: NotebookRemoteService;
  @Inject('zeppelinApi')
  zeppelinApi: ZeppelinApi;
  @Ref('repoListTable')
  readonly repoTable!: ElTable;
  activeName = 'notebook';
  debouncedSearch: Function;
  searchKey = '';
  @State(state => state.repository.files)
  originFiles: Dict<IFile>;


  get repoList (): Array<FavoriteItem> {

    return _.map(this.originFiles, (file: IFile) => {
      const interpretor = this.parseCodeType(file);
      const favorite = this.$store.getters.getFavoriteById(file.notebookId, 'nb');
      console.log('fav nId:[' + file.notebookId + ']', favorite);
      return {
        title: file.title,
        path: file.path,
        isFile: true,
        notebook: file,
        notebookId: file.notebookId,
        runTime: file.lastRunTime,
        updateTime: file.updateTime,
        interpretor,
        state: file.state,
        selectable: true,
        favorite: Boolean(favorite),
      } as FavoriteItem;
    }).sort(this.sortByProp(this.sortProp));
  }

  sortProp: SortProp = {
    prop: 'updateTime',
    order: 'descending',
  };
  sortChange (arg: SortProp) {
    this.sortProp.prop = arg.prop;
    this.sortProp.order = arg.order;
  }
  private sortByProp (sortProp: SortProp) {
    return (a: FavoriteItem, b: FavoriteItem) => {
      const key = sortProp.prop;
      const scoreA = a.isFile ? 0 : (a.notebookId === '..') ? 100 : 10;
      const scoreB = b.isFile ? 0 : (b.notebookId === '..') ? 100 : 10;
      let scoreSort = (a[key] || 0) >= (b[key] || 0) ? -1: 1;
      if (sortProp.order === null) {
        scoreSort = 0;
      } else if (sortProp.order === 'ascending') {
        scoreSort = -scoreSort;
      }
      return scoreB - scoreA + scoreSort;
    };
  }

  parseCodeType (file: IFile){
    if (file.nbType === 'zeppelin') return 'Stacked';
    if (file.preference && file.preference && file.preference['notebook.connection'] && file.preference['notebook.connection']){
      const connection = file.preference['notebook.connection'] as IConnection;
      let codeTypeDisplay = '';
      switch (connection.codeType) {
        case CodeType.TERADATA:
          codeTypeDisplay = 'TD SQL';
          break;
        case CodeType.SQL:
          codeTypeDisplay = 'Spark SQL';
          break;
        case CodeType.HIVE:
          codeTypeDisplay = 'Hive SQL';
          break;
        case CodeType.KYLIN:
          codeTypeDisplay = 'Kylin SQL';
          break;
        case CodeType.TEXT:
          codeTypeDisplay = 'Text';
          break;
        case CodeType.SPARK_PYTHON:
          codeTypeDisplay = 'Spark Python';
          break;
      }
      return codeTypeDisplay;
    }
    return '';
  }
  getPlatformIcon (note: IFile) {
    // let cnn: IConnection | undefined = undefined;
    // if(note.preference && note.preference && note.preference['notebook.connection'] && note.preference['notebook.connection']){
    //   cnn = note.preference['notebook.connection'] as IConnection;

    // }
    return WorkspaceSrv.getPlatformIconByFile(note);
  }


  onclickFile (nb: IFile) {
    let notebook: INotebook;
    if (nb.nbType === 'zeppelin') {
      notebook = INotebookMapper.zplFileMapper(nb);
      notebook = NBSrv.zeppelinNotebook(notebook);
    } else {
      notebook = this.$store.getters.nbByFile(nb);
    }

    if (!notebook) {
      notebook = NBSrv.notebook({
        notebookId: nb.notebookId,
        name: nb.title,
        code: nb.content || '',
        createTime: nb.createTime,
        updateTime: nb.updateTime,
        preference: nb.preference,
        nbType: nb.nbType,
      });
    }
    WorkspaceManager.getInstance(this).addActiveTabAndOpen(notebook);

    // this.$nextTick(() => {
    //   if(nb.nbType === 'zeppelin') {
    //     this.zeppelinApi.openNote(nb.notebookId);
    //   }
    // });
  }

  getIconClass (file: IFile): string {
    const nb: INotebook | null = this.$store.getters.nbByFile(file);
    return nb ? 'zeta-icon-notebook2 opened' : 'zeta-icon-notebook';
  }

}
</script>

<style lang="scss" scoped>
@import '@/styles/global.scss';
.workspace-icon {
  opacity: 1 !important;
}
.toolbar {
  padding: 8px 0;
  min-height: 26px;
  .fence {
    border-right: 1px solid $zeta-global-color-disable;
    margin: 0 10px;
  }
  .el-button + .el-button-holder,
  .el-button-holder + .el-button-holder,
  .el-button-holder + .el-button {
    margin-left: 10px;
  }
}

.f-link {
  cursor: pointer;
  text-decoration: none;
  padding: 0 20px 0 0;
  // color: rgb(51, 110, 123);
  &:hover {
    color: $zeta-global-color-heighlight;
    > i {
      color: $zeta-global-color-heighlight;
    }
  }
}
.repo {
  display: flex;
  flex-direction: column;
  .content {
    height: 0;
    flex-grow: 1;
    display: flex;
    flex-direction: column;
    /deep/ .el-tabs__item {
      width: 120px;
      text-align: center;
    }
    /deep/ .el-tabs__content {
      flex-grow: 1;
      display: flex;
      flex-direction: column;
      .el-tab-pane {
        flex-grow: 1;
        height: 100%;
        display: flex;
        flex-direction: column;
      }
      .el-table {
        display: flex;
        flex-direction: column;
        td,tr {
          font-size: 14px;
          padding: 12px 0;
        }
        .cell {
          line-height: 23px;
          font-size: 14px;
        }
        .el-table-column--selection .cell {
          text-overflow: clip;
        }
        .el-table__header-wrapper {
          min-height: 55px;
        }
        .el-table__body-wrapper {
          overflow: auto;
        }
      }
    }
  }
  .el-dropdown-link {
    cursor: pointer;
    color: $zeta-global-color;
  }
  .notebook-search{
    width: 200px;
    float: right;
  }
}
</style>
