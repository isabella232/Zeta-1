<template>
  <div class="favorite-notebooks">
    <el-table
      ref="repoListTable"
      :data="repoList | favoriteFilter "
      row-key="notebookId"
      :default-sort="{prop: 'updateDt', order: 'descending'}"
      @sort-change="sortChange"
    >
      <el-table-column
        prop="title"
        label="Name"
        sortable="custom"
      >
        <template slot-scope="scope">
          <span
            class="f-link"
            @click.stop="noteClickHandler(scope.row)"
          >
            <i :class="getIconClass(scope.row)" />
            {{ scope.row.title }}
          </span>
        </template>
      </el-table-column>
      <el-table-column
        prop="favorite"
        label="Action"
        width="120px"
        align="center"
      >
        <template slot-scope="scope">
          <favorite-icon
            :id="scope.row.id"
            :type="'pub_nb'"
          />
        </template>
      </el-table-column>
      <el-table-column
        prop="platform"
        label="Platform"
        width="120px"
        sortable="custom"
        align="center"
      >
        <template slot-scope="scope">
          <i
            :class="scope.row.platform"
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
          slot-scope="scope"
        >
          <span>{{ scope.row.interpreter | noteType }}
          </span>
        </template>
      </el-table-column>
      <el-table-column
        prop="createDt"
        label="Create Date"
        width="160px"
        sortable="custom"
      >
        <template slot-scope="scope">
          {{ scope.row.createDt| formatDateFromTimestamps }}
        </template>
      </el-table-column>
      <el-table-column
        prop="updateDt"
        label="Last Update Date"
        width="160px"
        sortable="custom"
      >
        <template slot-scope="scope">
          {{ scope.row.updateDt | formatDateFromTimestamps }}
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Inject, Ref } from 'vue-property-decorator';
import moment from 'moment';
import { PublicNotebookItem, WorkSpaceType } from '@/types/workspace';
import { Table as ElTable } from 'element-ui';
import { Getter } from 'vuex-class';
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import { IFileMapper } from '@/services/mapper';
import { WorkspaceSrv as NBSrv, WorkspaceSrv } from '@/services/Workspace.service';
import ZeppelinApi from '@/services/remote/ZeppelinApi';
import { ZetaException } from '@/types/exception';
import Util from '@/services/Util.service';
import FavoriteIcon from '@/components/common/favorite-icon/favorite-icon-witn-store.vue';
import { WorkspaceManager } from '@/types/workspace/workspace-manager';
import { noteType } from '@/components/Repository/note-type-filter';
type Folder = string;
type SortProp = { prop: 'title' | 'platform' | 'interpretor' | 'createDt' | 'updateDt'; order: 'descending' | 'ascending' | null};

type FavoriteItem = PublicNotebookItem & {
  favorite: boolean;
};
@Component({
  components: {
    FavoriteIcon,
  },
  filters: {
    favoriteFilter: (list: Array<FavoriteItem>) => {
      return list.filter(item => item.favorite);
    },
    formatDateFromTimestamps(timestamps: number) {
      return timestamps ? moment(timestamps).format('YYYY/MM/DD HH:mm') : '';
    },
    noteType,
  }
})
export default class FavoritePublicNotebookList extends Vue {

  @Inject()
  notebookRemoteService: NotebookRemoteService;
  @Inject('zeppelinApi')
  zeppelinApi: ZeppelinApi;
  @Ref('repoListTable')
  readonly repoTable!: ElTable;
  @Getter('favoritePublicNotebooks')
  favoriteList: FavoriteItem[];

  get repoList(): Array<FavoriteItem> {
    const nbs = this.$store.state.publicNotebook.publicNotebooks;
    if (!nbs) return [];
    return nbs.map((nb: PublicNotebookItem) => {
      const favoriteItem = this.favoriteList.find(f => f.id === nb.id);
      return {
        ...nb,
        favorite: favoriteItem ? favoriteItem.favorite : false,
      };
    }).sort(this.sortByProp(this.sortProp));
  }


  sortProp: SortProp = {
    prop: 'updateDt',
    order: 'descending'
  };
  sortChange(arg: SortProp) {
    this.sortProp.prop = arg.prop;
    this.sortProp.order = arg.order;
  }
  private sortByProp(sortProp: SortProp) {
    return (a: FavoriteItem, b: FavoriteItem) => {
      const key = sortProp.prop;
      let scoreSort = (a[key] || 0) >= (b[key] || 0) ? -1: 1;
      if (sortProp.order === null) {
        scoreSort = 0;
      } else if (sortProp.order === 'ascending') {
        scoreSort = -scoreSort;
      }
      return scoreSort;
    };
  }
  noteClickHandler(srcData: PublicNotebookItem) {
    if (srcData.isZeppelin) {
      this.openZplPublicNote(srcData);
    } else {
      this.openPublicReports(srcData);
    }
  }
  openPublicReports(srcData: PublicNotebookItem) {
    if (srcData.nt == Util.getNt()) {
      const nb = NBSrv.notebook({
        notebookId: srcData.id,
        name: srcData.title,
        code:  '',
        nbType: 'single'
      });
      WorkspaceManager.getInstance(this).addActiveTabAndOpen(nb);
      return;
    }
    this.notebookRemoteService
      .getPublicNotebook(srcData.id)
      .then(({ data }) => {
        const nb = WorkspaceSrv.file2nb(data);
        WorkspaceManager.getInstance(this).addActiveTabAndOpen(nb);
      })
      .catch((err: ZetaException) => {
        if (err.code === 'ENTITY_IS_NULL') {
          err.resolve();
          this.createNewPublic(srcData.id).then(({ data }) => {
            const file = IFileMapper.packetMapper(data);
            this.$store.dispatch('addFile', file);

            const nb = WorkspaceSrv.file2nb(data);
            WorkspaceManager.getInstance(this).addActiveTabAndOpen(nb);
          });
        } else {
        }
      });
  }
  createNewPublic(id: string) {
    return this.notebookRemoteService.createPublicNotebook(id);
  }
  openZplPublicNote(srcData: PublicNotebookItem): any {
    const cloneName = '/public_notebooks/'+ srcData.title + '_Clone';
    this.zeppelinApi.publish(srcData.id, {name: cloneName}).then((res) => {
      const notebookId = res.data.body;
      const ws = WorkspaceSrv.notebook({
        notebookId,
        name: srcData.title + '_Clone',
        type: WorkSpaceType.NOTEBOOK_ZEPPELIN
      });
      WorkspaceManager.getInstance(this).addActiveTabAndOpen(ws);
      const file = IFileMapper.zeppelinNoteMapper({
        name: cloneName,
        id: notebookId,
        opened: 1,
        interpreterSettings: [],
        seq: -1,
        createDt: moment().valueOf()
      });
      this.$store.dispatch('addFile', file);
    });
  }

  getIconClass(item: PublicNotebookItem): string {
    const nb = this.$store.getters.nbByFile({ notebookId: item.id });
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
