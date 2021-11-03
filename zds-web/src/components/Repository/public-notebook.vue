<template>
  <el-table
    :data="pwdAll"
    :default-sort="{prop: 'updateTime', order: 'descending'}"
    @sort-change="sortChange"
  >
    <el-table-column
      prop="title"
      label="Name"
      :render-header="titleHeaderRender"
      sortable="custom"
    >
      <template slot-scope="scope">
        <template
          v-if="scope.row.isFolder"
        >
          <span
            class="f-link"
            @click="onclickFolder(scope.row.title)"
          >
            <i :class="getIconClass(scope.row)" />
            {{ scope.row.title }}
          </span>
        </template>
        <template v-else>
          <span
            class="f-link"
            @click.stop="noteClickHandler(scope.row)"
          >
            <i :class="getIconClass(scope.row)" />
            {{ scope.row.title }}
          </span>
        </template>
      </template>
    </el-table-column>
    <el-table-column
      prop="favorite"
      label="Action"
      width="120px"
      sortable="custom"
      align="center"
    >
      <template slot-scope="scope">
        <template v-if="scope.row.isFolder" />
        <template v-else>
          <favorite-icon
            :id="scope.row.id"
            :type="'pub_nb'"
          />
        </template>
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
          :class="scope.row.platform"
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
      label="Owner"
      width="240px"
      sortable="custom"
    >
      <template slot-scope="scope">
        {{ scope.row.nt }}
      </template>
    </el-table-column>
    <el-table-column

      label="Create Date"
      width="240px"
      sortable="custom"
    >
      <template slot-scope="scope">
        {{ scope.row.createDt| formatDateFromTimestamps }}
      </template>
    </el-table-column>
    <el-table-column
      label="Last Update Date"
      width="240px"
      sortable="custom"
    >
      <template slot-scope="scope">
        {{ scope.row.updateDt | formatDateFromTimestamps }}
      </template>
    </el-table-column>
  </el-table>
</template>

<script lang="ts">
import { Component, Vue, Inject } from 'vue-property-decorator';
import {
  WorkSpaceType, IConnection, PublicNotebookItem
} from '@/types/workspace';
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import _ from 'lodash';
import { WorkspaceSrv } from '@/services/Workspace.service';
import Util from '@/services/Util.service';
import { ZetaException } from '@/types/exception';
import { IFileMapper } from '@/services/mapper';
import ZeppelinApi from '@/services/remote/ZeppelinApi';
import moment from 'moment';
import { Getter } from 'vuex-class';
import { FavoriteItem } from '@/stores/modules/favorite-store';
import { WorkspaceSrv as NBSrv } from '@/services/Workspace.service';
import FavoriteIcon from '@/components/common/favorite-icon/favorite-icon-witn-store.vue';
import { WorkspaceManager } from '@/types/workspace/workspace-manager';
import { noteType } from '@/components/Repository/note-type-filter';
type SortProp = { prop: 'title' | 'interpretor' | 'runTime' | 'updateTime'; order: 'descending' | 'ascending' | null};

@Component({
  components: {
    FavoriteIcon,
  },
  filters: {
    formatDateFromTimestamps(timestamps: number) {
      return timestamps ? moment(timestamps).format('YYYY/MM/DD HH:mm') : '';
    },
    noteType,
  }
})
export default class PublicNotebook extends Vue {
  sortProp: SortProp = {
    prop: 'updateTime',
    order: 'descending'
  };
  pwd: string;
  sortChange(arg: SortProp) {
    this.sortProp.prop = arg.prop;
    this.sortProp.order = arg.order;
  }
  private reportsLoading = true;

  @Inject('notebookRemoteService')
  notebookRemoteService: NotebookRemoteService;
  @Inject('zeppelinApi')
  zeppelinApi: ZeppelinApi;

  @Getter('favoritePublicNotebooks')
  favoriteList: FavoriteItem[];


  get notes() {
    const nbs = this.$store.state.publicNotebook.publicNotebooks;
    if (!nbs) return [];
    const nbRef = nbs.map((note: PublicNotebookItem) => {
      const favoriteItem = this.favoriteList.find(f => f.id === note.id);
      return {
        ...note,
        favorite: favoriteItem ? favoriteItem.favorite : false,
      };
    });
    return nbRef;
  }



  folderName (f: string): string{
    return f.substring(f.lastIndexOf('/',f.length-2)+1,f.length-1);
  }

  previousFolderName (i: string): string{
    return i.substring(0,i.lastIndexOf('/',i.length-2)+1);
  }

  get pwdfoldersisFolder (){
    const pwdfoldersisFolder = this.pwdfolders.map(f  =>
    { return {
      title:this.folderName(f),
      path:f,
      isFolder: true,
      previousPath: this.previousFolderName(f)
    };});
    const list: Array<any> = [];
    if (this.pwd !== '/') {
      list.push({
        title: '..',
        isFolder: true,
      });}
    return list.concat(pwdfoldersisFolder);
  }


  // fullpaths
  get fullpaths() {
    return _.chain(this.notes).map(note => note.path)
      .map(fullpath => {
        const splitPath = _.chain(fullpath).split('/').compact().uniq().value();
        const result: string[] = [];
        _.forEach(splitPath, subPath => {
          const lastPath = result.length >= 1 ? result[result.length-1] : '/';
          result.push(lastPath + subPath + '/');
        });
        return result;
      })
      .flatten().uniq().value() as string[];
  }
  get pwdfolders (){
    return _.chain(this.fullpaths)
      .filter(fp => _.startsWith(fp, this.pwd))
      .filter(fp => fp != this.pwd)
      .filter(fp => {
        const rp = fp.substr(this.pwd.length);
        const subFolders = _.chain(rp).split('/').filter(f => {
          return Boolean(f);
        }).value();
        return subFolders.length <= 1;
      })
      .value();
  }

  get pwdDirectFiles(){
    const direct = _.pickBy(this.notes, (file) =>
      this.directlyHas(this.pwd, file.path)
    );
    return _.chain(direct).map(note => note).value();
  }

  get pwdAll(){

    return this.pwdfoldersisFolder.concat(this.pwdDirectFiles);
  }

  directlyHas(root: string, path: string): boolean {
    const has = path.indexOf(root) === 0;
    if (!has) return false;
    const direct = path.slice(root.length).indexOf('/') === -1;
    return direct;
  }

  onclickFolder(folder: any) {
    if (folder === '..') {
      if (this.pwd === '/') return;
      else {
        this.pwd = this.previousFolderName(this.pwd);
      }
    } else {
      this.pwd += folder + '/';
    }
  }

  noteClickHandler(srcData: PublicNotebookItem) {
    if (srcData.isZeppelin) {
      this.openZplPublicNote(srcData);
    } else {
      this.openPublicReports(srcData);
    }
  }

  openPublicReports(srcData: PublicNotebookItem) {
    this.reportsLoading = true;
    if (srcData.nt == Util.getNt()) {
      this.reportsLoading = false;
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
            this.reportsLoading = false;
          });
        } else {
          this.reportsLoading = false;
        }
      });
  }

  titleHeaderRender(h: any) {
    return [
      h(
        'span',
        {
          style: {
            margin: '0 20px 0 0'
          }
        },
        'Current Path: ' + this.pwd
      )
    ];
  }
  createNewPublic(id: string) {
    return this.notebookRemoteService.createPublicNotebook(id);
  }
  openZplPublicNote(srcData: PublicNotebookItem): any {
    this.reportsLoading = true;
    const cloneName = '/public_notebooks/'+ srcData.title + '_Clone';
    this.zeppelinApi.publish(srcData.id, {name: cloneName}).then((res) => {
      const notebookId = res.data.body;
      const ws = WorkspaceSrv.notebook({
        notebookId,
        name: srcData.title + '_Clone',
        type: WorkSpaceType.NOTEBOOK_ZEPPELIN,
        nbType: 'zeppelin',
        preference: {
          'notebook.connection': {
            alias: srcData.platform
          } as IConnection,
        }
      });
      WorkspaceManager.getInstance(this).addActiveTabAndOpen(ws);
      const file = IFileMapper.zeppelinNoteMapper({
        name: cloneName,
        id: notebookId,
        opened: 1,
        interpreterSettings: [],
        seq: -1,
        createDt: moment().valueOf(),
      });
      this.$store.dispatch('addFile', file);
      this.reportsLoading = false;
    }).catch(() => {
      this.reportsLoading = false;
    });
  }
  getIconClass(item: any): string {
    return item.isFolder ? 'icon-folder-empty' : 'zeta-icon-notebook';
  }
  constructor() {
    super();
    this.pwd = '/';
  }

}
</script>
<style lang="scss" scoped>
@import "@/styles/global.scss";
.workspace-icon {
  opacity: 1 !important;
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
</style>

