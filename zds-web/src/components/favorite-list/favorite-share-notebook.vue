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
            <i class="zeta-icon-notebook" />
            {{ scope.row.title }}
          </span>
        </template>
      </el-table-column>
      <el-table-column
        prop="favorite"
        label="Action"
        align="center"
      >
        <template slot-scope="scope">
          <favorite-icon
            :id="scope.row.id"
            :type="scope.row.favoriteType"
            :value="scope.row.favorite"
            @input="f => onFavoriteChange(scope.row.id, f)"
          />
        </template>
      </el-table-column>
      <el-table-column
        prop="nt"
        label="Owner"
        sortable="custom"
      />
      <el-table-column
        prop="icon"
        label="Platform"
        width="120px"
        sortable="custom"
        align="center"
      >
        <template slot-scope="scope">
          <i
            :class="scope.row.icon"
            class="workspace-icon"
          />
        </template>
      </el-table-column>
      <el-table-column
        prop="interpreter"
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
        prop="lastRunDt"
        label="Last Run Date"
        sortable="custom"
      >
        <template slot-scope="scope">
          {{ scope.row.lastRunDt | formatDateFromTimestamps }}
        </template>
      </el-table-column>
      <el-table-column
        prop="updateDt"
        label="Last Update Date"
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
import Util from '@/services/Util.service';
import { RestPacket } from '@/types/RestPacket';
import { Table as ElTable } from 'element-ui';
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import FavoriteIcon from '@/components/common/favorite-icon/favorite-icon-with-value.vue';
import ZeppelinApi from '@/services/remote/ZeppelinApi';
import { parseName, parseInterpreters } from '@/services/zeppelin/Zeppelin.service';
import { IFileMapper } from '@/services/mapper';
import { WorkspaceSrv } from '@/services/Workspace.service';
import { CodeType, IConnection } from '@/types/workspace';
import { IFile } from '@/types/repository';
import { noteType } from '@/components/Repository/note-type-filter';
type Folder = string;
type SortProp = { prop: 'title' | 'nt' | 'platform' | 'interpreter' | 'lastRunDt' | 'updateDt'; order: 'descending' | 'ascending' | null};

type FavoriteItem = {
  id: string;
  updateDt: number;
  lastRunDt: number;
  interpreter: string;
  title: string;
  favorite: boolean;
  nt: string;
  icon: string;
  favoriteType: 'share_nb' | 'share_zpl_nb';
};
function parseCodeType(file: IFile){
  if(file.nbType === 'zeppelin') return 'Stacked';
  if(file.preference && file.preference && file.preference['notebook.connection'] && file.preference['notebook.connection']){
    const connection = file.preference['notebook.connection'] as IConnection;
    let codeTypeDisplay = '';
    switch(connection.codeType) {
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
function mapper(packet: RestPacket.File): FavoriteItem {
  const file = IFileMapper.packetMapper(packet);
  const icon = WorkspaceSrv.getPlatformIconByFile(file);
  return {
    id: packet.id,
    updateDt: packet.updateDt,
    lastRunDt: packet.lastRunDt || 0,
    nt: packet.nt,
    title: packet.title,
    favorite: true,
    icon,
    interpreter: parseCodeType(file),
    favoriteType: 'share_nb',
  };
}
function zplMapper(data: any): FavoriteItem {
  const note = parseName(data.name);
  const cnn = parseInterpreters(data.interpreterSettings);
  const icon = WorkspaceSrv.getPlatformIconByConnection(cnn, 'zeppelin');
  const nt = data.nt && data.nt[0] ? data.nt[0] : '';
  return {
    id: data.id,
    updateDt: data.updateDt,
    lastRunDt: data.lastRunDt,
    title: note.name,
    favorite: true,
    nt,
    icon,
    interpreter: 'Stacked',
    favoriteType: 'share_zpl_nb',
  };
}

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
    noteType
  }
})
export default class FavoriteShareNotebook extends Vue {

  @Inject()
  notebookRemoteService: NotebookRemoteService;
  @Inject()
  zeppelinApi: ZeppelinApi;

  @Ref('repoListTable')
  readonly repoTable!: ElTable;

  repoList: Array<FavoriteItem>  = [];

  mounted() {
    this.init();
  }
  // activated() {
  //   this.i
  // }
  onFavoriteChange(id: string, favorite: boolean) {
    const item = this.repoList.find(item => item.id === id);
    if (item) {
      item.favorite = favorite;
    }
  }

  init() {
    this.notebookRemoteService.favoriteShareNotebooks().then(data => {
      const list = data.data.zetaFavorite;
      list.map(item => {
        this.repoList.push(mapper(item));
      });
      return data.data.zeppelinFavorite;
    }).then((zplList: any) => {
      const ids = zplList.map(item => item.id);
      const idsStr = ids.join(',');
      if (!idsStr) {
        return {
          data: {
            body: [] as any[]
          }
        };
      } else {
        return this.zeppelinApi.getNotesBrief(idsStr) as Promise<any>;
      }
    }).then(({data}) => {
      data.body.map(item => {
        this.repoList.push(zplMapper(item));
      });
      this.repoList.sort(this.sortByProp(this.sortProp));
    });
  }
  sortProp: SortProp = {
    prop: 'updateDt',
    order: 'descending'
  };
  sortChange(arg: SortProp) {
    this.sortProp.prop = arg.prop;
    this.sortProp.order = arg.order;
    this.repoList.sort(this.sortByProp(this.sortProp));
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

  noteClickHandler(item: FavoriteItem) {
    if (item.favoriteType === 'share_zpl_nb') {
      const url = `${location.protocol}//${location.host}/${Util.getPath()}share/#/notebook/multilang?notebookId=${item.id}`;
      window.open(url,'_blank');
    } else {
      const url = `${location.protocol}//${location.host}/${Util.getPath()}share/#/notebook?notebookId=${item.id}`;
      window.open(url,'_blank');
    }
  }

}
</script>

<style lang="scss" scoped>
@import '@/styles/global.scss';
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
.workspace-icon {
  opacity: 1 !important;
}
</style>
