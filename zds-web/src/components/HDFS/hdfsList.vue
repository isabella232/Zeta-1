<template>
  <div class="hdfs-management">
    <div class="toolbar">
      <el-button
        plain
        @click="createFolder"
      >
        Create Folder
      </el-button>
      <el-button
        plain
        :disabled="numSelectedFiles !== 1"
        @click="renamePackage"
      >
        Rename
      </el-button>
      <el-button
        plain
        :disabled="(numSelectedFiles) !== 1"
        class="delete"
        :loading="downloadLoading"
        @click="downLoadHDFS(selectedFiles)"
      >
        Download
      </el-button>
      <el-button
        type="danger"
        :disabled="(numSelectedFiles) <= 0"
        class="delete"
        @click="delPackage(selectedFiles)"
      >
        Delete
      </el-button>
      <upload-button @show-upload-diaolog="showUploadPackageDialog" />
      <el-select
        v-model="cluster"
        placeholder="select"
        class="select-cluster"
      >
        <el-option
          v-for="(val, $key, $index) in platform"
          :key="$index"
          :label="$key"
          :value="val"
        />
      </el-select>
    </div>
    <div class="hdfs-list">
      <el-table
        ref="hdfsTable"
        v-loading="loading"
        height="100%"
        :data="hdfsList"
        row-key="filePath"
        :default-sort="{prop: 'updateTime', order: 'descending'}"
        @sort-change="sortChange"
        @selection-change="handleSelectionChange"
      >
        <el-table-column
          type="selection"
          width="55"
          :selectable="rowSelectAble"
          :reserve-selection="true"
        />
        <el-table-column
          prop="fileName"
          label="Name"
          :render-header="titleHeaderRender"
          sortable="custom"
        >
          <template slot-scope="scope">
            <template v-if="scope.row.isFile">
              <span class="f-link">
                <i class="zeta-icon-udf" />
                {{ scope.row.fileName }}
              </span>
            </template>
            <template v-else>
              <span
                class="f-link folder"
                @click="onclickFolder(scope.row.fileName)"
              >
                <i class="icon-folder-empty" />
                {{ scope.row.fileName }}
              </span>
            </template>
          </template>
        </el-table-column>
        <el-table-column
          prop="type"
          label="File Type"
          width="140px"
          sortable="custom"
        >
          <template slot-scope="scope">
            {{ scope.row.type }}
          </template>
        </el-table-column>
        <el-table-column
          prop="updateTime"
          label="Update Time"
          width="180px"
          sortable="custom"
        >
          <template slot-scope="scope">
            {{ formatDateFromTimestamps(scope.row.updateTime) }}
          </template>
        </el-table-column>
      </el-table>
    </div>
    <upload-package-dialog
      :visible.sync="uploadPackageDialogVisible"
      :file-list="fileList"
      @remove-file="removeFile"
      @upload-success="uploadSuccess"
    />
    <rename-package-dialog
      :visible.sync="renamePackageDialogVisible"
      :current-package="currentPackage"
      @rename-success="handleRenameSuccess"
    />
    <create-folder-dialog :visible.sync="createFolderDialogVisible" />
  </div>
</template>
<script lang="ts">
import {
  Component,
  Vue,
  Watch,
  Inject,
  Ref,
} from 'vue-property-decorator';
import Util from '@/services/Util.service';
import _ from 'lodash';
import Moment from 'moment';
import { IHDFSFile, IFile } from '@/types/workspace';
import { Table as ElTable } from 'element-ui';
import HDFSRemoteService from '@/services/remote/HDFSRemoteService';
import UploadButton from '@/components/common/workspace/upload-package-dialog/upload-button.vue';
import UploadPackageDialog from '@/components/common/workspace/upload-package-dialog/upload-package-dialog.vue';
import RenamePackageDialog from '@/components/common/workspace/rename-package-dialog/rename-package-dialog.vue';
import CreateFolderDialog from '@/components/common/workspace/create-folder-dialog/create-folder-dialog.vue';
type Folder = string;
type SortProp = {
  prop: 'fileName' | 'type' | 'updateTime';
  order: 'descending' | 'ascending' | null;
};
@Component({
  components: {
    UploadButton,
    UploadPackageDialog,
    RenamePackageDialog,
    CreateFolderDialog,
  },
})
export default class HDFSList extends Vue {
  @Inject('HDFSRemoteService')
  HDFSRemoteService: HDFSRemoteService;
  @Ref('hdfsTable')
  readonly hdfsTable!: ElTable;
  loading = false;
  files: Dict<IFile> = {};
  rootPath: string = '/user/' + Util.getNt() + '/';
  pwd: string;
  selectedFolders: IHDFSFile[] = [];
  selectedFiles: IHDFSFile[] = [];
  cluster = 14;
  uploadPackageDialogVisible = false;
  renamePackageDialogVisible = false;
  createFolderDialogVisible = false;
  downloadLoading = false;
  fileList: Array<any> = [];
  constructor () {
    super();
    this.pwd = this.rootPath;
  }
  mounted () {
    this.getFile();
  }
  getPath (filePath: string) {
    const subP = filePath.slice(this.pwd.length).split('/');
    return subP[0];
  }
  getName (name: string) {
    const sub = name.slice(this.pwd.length).split('/');
    return sub[sub.length - 1];
  }
  getType (name: string) {
    const type = name.substring(name.lastIndexOf('.') + 1, name.length);
    return type;
  }
  get platform () {
    return {
      Ares: 2,
      ApolloReno: 14,
      Hercules: 10,
    };
  }
  get numSelectedFiles (): number {
    return this.selectedFiles.length;
  }
  get numSelectedFolders () {
    return this.selectedFolders.length;
  }

  get currentPackage () {
    return this.selectedFiles[0];
  }
  get hdfsList () {
    const list: Array<IHDFSFile> = [];
    if (this.pwd !== this.rootPath) {
      list.push({
        filePath: '..',
        fileName: '..',
        isFile: false,
        selectable: false,
        cluster: this.cluster,
      });
    }
    for (const item in this.files) {
      list.push({
        filePath: item,
        fileName: this.getName(item),
        isFile: this.files[item].isFile,
        type: this.files[item].isFile ? this.getType(item) : null,
        selectable: this.files[item].isFile,
        updateTime: this.files[item].modifyTime,
        cluster: this.cluster,
      });
    }
    return list.sort(this.sortByProp(this.sortProp));
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
    return (a: any, b: any) => {
      const key = sortProp.prop;
      const scoreA = a.isFile ? 0 : a.filePath === '..' ? 1000 : 10;
      const scoreB = b.isFile ? 0 : b.filePath === '..' ? 1000 : 10;
      let scoreSort = (a[key] || 0) >= (b[key] || 0) ? -1 : 1;
      if (sortProp.order === null) {
        scoreSort = 0;
      } else if (sortProp.order === 'ascending') {
        scoreSort = -scoreSort;
      }
      return scoreB - scoreA + scoreSort;
    };
  }
  rowSelectAble (row: IHDFSFile) {
    if (!row.selectable) return false;
    return true;
  }
  handleSelectionChange (val: any) {
    this.selectedFiles = _.chain(val)
      .filter(v => v.isFile)
      .map((item: any) => {
        return item as IHDFSFile;
      })
      .value();
    this.selectedFolders = _.chain(val)
      .filter(v => !v.isFile)
      .map((item: any) => {
        return item as IHDFSFile;
      })
      .value();
  }

  titleHeaderRender (h: any) {
    const selectedCnt = this.numSelectedFiles + this.numSelectedFolders;
    return [
      h(
        'span',
        {
          style: {
            margin: '0 20px 0 0',
          },
        },
        'Current Path: ' + this.pwd
      ),
      h('span', selectedCnt > 0 ? selectedCnt + ' selected' : ''),
    ];
  }
  onclickFolder (folder: Folder) {
    if (folder === '.') return;
    this.hdfsTable.clearSelection();
    if (folder === '..') {
      if (this.pwd === this.rootPath) return;
      else {
        const pwd = this.pwd.slice(0, -1);
        this.pwd = pwd.slice(0, pwd.lastIndexOf('/') + 1);
      }
    } else {
      this.pwd += folder + '/';
    }
    this.getFile();
  }
  formatDateFromTimestamps (timestamps: number) {
    return timestamps ? Moment(timestamps).format('YYYY/MM/DD HH:mm') : '';
  }
  getFile () {
    this.loading = true;
    this.HDFSRemoteService.getHDFSFile(this.cluster, this.pwd)
      .then(res => {
        this.loading = false;
        if (res.data) {
          this.files = res.data;
          this.$store.dispatch('initPackage', { packages: this.files });
        }
      })
      .catch(e => {
        this.loading = false;
        this.files = {};
      });
  }
  private toogleSelection (path: string) {
    const item = this.hdfsList.find(r => r.filePath == path);
    if (item) {
      this.hdfsTable.toggleRowSelection(item, false);
    }
  }
  showUploadPackageDialog (fileList: Array<any>){
    this.uploadPackageDialogVisible = true;
    this.fileList = fileList;
  }
  removeFile (){
    this.fileList = [];
  }
  uploadSuccess (){
    this.getFile();
  }
  renamePackage (){
    this.renamePackageDialogVisible = true;
  }
  createFolder (){
    this.createFolderDialogVisible = true;
  }
  handleRenameSuccess (){
    this.hdfsTable.clearSelection();
  }
  async downLoadHDFS (selectedFiles: any){
    const pg = selectedFiles[0];
    try {
      this.downloadLoading = true;
      const res = await this.HDFSRemoteService.download(pg.cluster, pg.filePath);
      this.downloadLoading = false;
      if (res.status === 200) {
        const aLink = document.createElement('a');
        aLink.href = URL.createObjectURL(res.data);
        aLink.download = pg.filePath;
        /** resolve the a tag click method cannot trigger the download event in firefox */
        document.body.appendChild(aLink);
        aLink.click();
        document.body.removeChild(aLink);
      }
    } catch (e) {
      this.downloadLoading = false;
      // eslint-disable-next-line no-console
      console.error(e);
    }
  }
  async delPackage (selectedFiles: any) {
    const selectedPackage: IHDFSFile[] = _.clone(selectedFiles);
    for (const index in selectedPackage) {
      const pg: IHDFSFile = selectedPackage[index];
      try {
        const message = `Are you sure to delete <br><u><b>${pg.fileName}</b></u><br> from HDFS management?`;

        await this.$confirm(message, 'Confirm Delete', {
          confirmButtonText: 'Delete',
          cancelButtonText: 'Cancel',
          customClass: 'del-file-message',
          type: 'warning',
          dangerouslyUseHTMLString: true,
        });
        this.HDFSRemoteService.del(pg.cluster!, pg.filePath).then((res) => {
          if (res.data && (res.data.code === 200)) {
            this.toogleSelection(pg.filePath);
            this.$store.dispatch('deletePackage', pg);
            this.$message({
              type: 'success',
              message: 'Successfully delete',
              customClass: 'delete-success-message',
            });
          } else {
            this.$message({
              type: 'warning',
              message: 'Fail delete',
              customClass: 'delete-fail-message',
            });
          }
        });
      } catch {
        /* Blank */
      }
    }
  }
  get storePackageFiles () {
    return this.$store.getters.packageFiles;
  }
  @Watch('storePackageFiles')
  handleFilesChange (newVal: Dict<IFile>) {
    this.files = newVal;
  }
  @Watch('pwd')
  handlePwd (newVal: boolean) {
    if (newVal) {
      this.$store.dispatch('setFolder', this.pwd);
    }
  }
  @Watch('cluster')
  handleClusterChange (newVal: number, oldVal: number) {
    if (newVal !== oldVal) {
      this.hdfsTable.clearSelection();
      this.$store.dispatch('updateCluster', newVal);
      this.getFile();
    }
  }
}
</script>
<style lang="scss" scoped>
@import '@/styles/global.scss';
.el-table__row {
  .folder {
    cursor: pointer;
  }
}
.hdfs-management{
  height: 100%;
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
  .select-cluster{
    float: right;
  }
}
.hdfs-list {
  height: calc(100% - 50px);
  /deep/ .el-table {
    td,
    tr {
        font-size: 14px;
        padding: 12px 0;
    }
    .cell {
        line-height: 23px;
        font-size: 14px;
    }
    .el-table__header-wrapper {
      min-height: 55px;
    }
  }
}
</style>
