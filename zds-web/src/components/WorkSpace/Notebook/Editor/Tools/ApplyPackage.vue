<template>
    <el-popover placement="bottom-start" :width="600" trigger="click" @show="isApolloRno&&init()">
        <div class="packageList notebook-tool" v-loading="updating">
            <h2>Select File <p v-if="isApolloRno">(Currently import {{currentNBPackages.length}})</p></h2>
            <div v-if="isApolloRno">
                <div class="package-list" v-loading="loading">
                    <el-table max-height="300" ref="hdfsTable" :data="hdfsList" @selection-change="handleSelectionChange" row-key="filePath"
                    :default-sort = "{prop: 'updateTime', order: 'descending'}"  @sort-change="sortChange" >
                        <el-table-column type="selection" width="50" :selectable="rowSelectAble" :reserve-selection="true"></el-table-column>
                        <el-table-column prop="fileName" label="Name" :render-header="titleHeaderRender" sortable="custom">
                            <template slot-scope="scope">
                                <template v-if="scope.row.isFile">
                                    <span class="f-link">
                                        <i class="zeta-icon-udf"></i>
                                        {{ scope.row.fileName }}
                                    </span>
                                </template>
                                <template v-else>
                                    <span class="f-link folder" @click='onclickFolder(scope.row.fileName)'>
                                        <i class="icon-folder-empty"></i>
                                        {{ scope.row.fileName }}
                                    </span>
                                </template>
                            </template>
                        </el-table-column>
                        <el-table-column prop="type" label="File Type" width="100px" sortable="custom">
                            <template slot-scope="scope">
                                {{ scope.row.type }}
                            </template>
                        </el-table-column>
                        <el-table-column prop="updateTime" label="Update Time" width="140px" sortable="custom">
                            <template slot-scope="scope">
                                {{ formatDateFromTimestamps(scope.row.updateTime) }}
                            </template>
                        </el-table-column>
                    </el-table>
                </div>
                <div class="apply">
                    <el-button
                        v-show="!applyAlert"
                        type="primary"
                        @click="gotoHDFS"
                    >Manage</el-button>
                    <el-button
                        v-show="!applyAlert"
                        type="primary"
                        @click="applyEvent"
                        :disabled="notebookStatus ==='RUNNING' || applyDisabled"
                    >Apply</el-button>
                    <div
                        v-show="applyAlert"
                    >You’re attempting to disconnect from server. Are you sure you want to continue?</div>
                    <div v-show="applyAlert" class="apply-confirm">
                        <el-button type="primary" plain @click=" () => applyAlert = false">Cancel</el-button>
                        <el-button type="primary" @click="update">Continue</el-button>
                    </div>
                </div>
            </div>
            <div class="params notebook-tool-nodata" v-else>
                <span>This function is not support this platform for now.</span>
            </div>
        </div>
        <el-button type="text" class="notebook-tool-btn" slot="reference" :disabled="disable" v-click-metric:NB_TOOLBAR_CLICK="{name: 'applypackage'}">
            <i class="zeta-icon-upload"></i>
            <span class="params-title">{{title}}</span>
        </el-button>
    </el-popover>
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch, Inject, Ref } from "vue-property-decorator";
import {
  INotebook,
  NotebookStatus,
  CodeType
} from "@/types/workspace";
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import HDFSRemoteService from '@/services/remote/HDFSRemoteService';
import _ from "lodash";
import Util from "@/services/Util.service";
import { IHDFSFile, IFile } from '@/types/workspace';
import moment from "moment";
import { ZetaException } from "@/types/exception";
import { Table as ElTable } from "element-ui";
import { wsclient } from '@/net/ws';

type Folder = string;
type SortProp = { prop: 'fileName' | 'type' | 'updateTime', order: 'descending' | 'ascending' | null};
@Component({
    components: {  }
})
export default class ApplyPackage extends Vue {
    HDFSRemoteService = new HDFSRemoteService();
    @Inject()
    notebookRemoteService: NotebookRemoteService;
    @Prop() notebook: INotebook;
    @Prop() title: string;
    @Prop() currentCodeType: string;
    @Prop() disable: boolean
    @Ref('hdfsTable')
    readonly hdfsTable: ElTable;
    notebookId: string;
    pwd: string;
    loading: boolean = false;
    files: Dict<IFile> = {};
    // hdfsList: Array<IHDFSFile> = [];
    selectedFiles: IHDFSFile[] = [];
    currentPaths: string[] = [];
    constructor(){
        super();
        this.notebookId = this.notebook.notebookId;
        this.pwd = this.rootPath;
    }
    private rootPath: string = '/user/'+ Util.getNt()+ '/';
    private applyDisabled: boolean = false;
    private applyAlert: boolean = false;
    private updating: boolean = false;
    private visible: boolean = false;
    private cluster: number = 14;
    private flag: boolean = false;
    nbClusterPackage: any = [];
    currentNBPackages: Array<string> = [];
    sortProp: SortProp = {
        prop: 'updateTime',
        order: 'descending'
    }
    sortChange(arg: SortProp) {
        this.sortProp.prop = arg.prop;
        this.sortProp.order = arg.order;
    }
    private sortByProp(sortProp: SortProp) {
        return (a: any, b: any) => {
        const key = sortProp.prop;
        let score_a = a.isFile ? 0 : (a.filePath === '..') ? 1000 : 10;
        let score_b = b.isFile ? 0 : (b.filePath === '..') ? 1000 : 10;
        let score_sort = (a[key] || 0) >= (b[key] || 0) ? -1: 1;
        if (sortProp.order === null) {
            score_sort = 0;
        } else if (sortProp.order === 'ascending') {
            score_sort = -score_sort;
        }
        return score_b - score_a + score_sort
        }
    }
    get numSelectedFiles(): number {
        return this.selectedFiles.length;
    }

    get _() {
        return _;
    }
    get notebookStatus() {
        let notebook: INotebook = this.$store.state.workspace.workspaces[
            this.notebookId
        ];
        if (!(notebook && notebook.status)) {
            return "OFFLINE";
        }
        if (notebook.status === NotebookStatus.OFFLINE) {
            return "OFFLINE";
        } else if (notebook.status === NotebookStatus.CONNECTING) {
            return "CONNECTING";
        } else if (notebook.status === NotebookStatus.DISCONNECTING) {
            return "DISCONNECTING";
        } else if (notebook.status === NotebookStatus.RUNNING) {
            return "RUNNING";
        } else {
            return "ONLINE";
        }
    }
    get isApolloRno() {
        const connection = this.notebook.connection;
        return Boolean(
            this.currentCodeType == CodeType.SQL &&
                connection &&
                connection.clusterId === 14
        );
    }
    get hdfsList(){
        let list: Array<IHDFSFile> = [];
        if (this.pwd !== this.rootPath) {
            list.push({
                filePath: '..',
                fileName: '..',
                isFile: false,
                selectable: false
            });
        }
        for (let item in this.files) {
            let type = this.files[item].isFile ? this.getType(item) : null;
            if(type === null || type === 'jar'){
                list.push({
                    filePath: item,
                    fileName: this.getName(item),
                    isFile: this.files[item].isFile,
                    type: this.files[item].isFile ? this.getType(item) : null,
                    selectable: this.files[item].isFile,
                    updateTime: this.files[item].modifyTime
                });
            }
        }
        return list.sort(this.sortByProp(this.sortProp));
    }
    getPath(filePath: string){
        let subP = filePath.slice(this.pwd.length).split('/');
        return subP[0];
    }
    getName(name: string){
        let sub = name.slice(this.pwd.length).split('/');
        return sub[sub.length-1];
    }
    getType(name: string){
        let type = name.substring(name.lastIndexOf(".")+1,name.length);;
        return type;
    }
    formatDateFromTimestamps(timestamps: number) {
        return timestamps ? moment(timestamps).format('YYYY/MM/DD HH:mm') : '';
    }
    titleHeaderRender(h: any, item: Object) {
        const selectedCnt = this.numSelectedFiles;
        return [
        h(
            'span',
            {
            style: {
                margin: '0 20px 0 0'
            }
            },
            'Current Path: ' + this.pwd
        ),
        h(
            'span',
            selectedCnt > 0 ? selectedCnt + ' selected' : ''
        )
        ];
    }
    onclickFolder(folder: Folder) {
        if (folder === '.') return;
        if (folder === '..') {
            if (this.pwd === this.rootPath) return;
            else {
                let pwd = this.pwd.slice(0, -1);
                this.pwd = pwd.slice(0, pwd.lastIndexOf('/') + 1);
            }
        } else {
            this.pwd += folder + '/';
        }
        if(folder === '..'){
            this.flag = true;
        }
        this.getFile();
    }
    getFile(){
        this.loading = true;
        this.HDFSRemoteService.getHDFSFile(this.cluster, this.pwd).then(res => {
            this.loading = false;
            if(res.data){
                this.files = res.data;
                // this.getList(res.data);
            }
        }).catch(e => {
          this.loading = false;
          this.files = {};
          console.log(e);
        })
    }
    init(){
        // this.hdfsList = [];
        this.files = {};
        this.hdfsTable.clearSelection();
        this.updating = false;
        this.applyDisabled = true;
        this.flag = false;
        this.pwd = this.rootPath;
        this.getFile();
        let notebook = this.$store.state.workspace.workspaces[this.notebookId];
        this.currentNBPackages = notebook.packages ? (notebook.packages[this.cluster]||[]) : [];
        this.nbClusterPackage = _.cloneDeep(this.currentNBPackages);
    }
    rowSelectAble(row: IHDFSFile, index: number) {
        if(!row.selectable) return false;
        return true
    }
    toogleSelection(filePath: string) {
        const item = this.hdfsList.find((r: any) => r.filePath === filePath);
        if (item) {
            this.hdfsTable.toggleRowSelection(item, true);
        }
    }
    handleSelectionChange(val: any) {
        // console.log('handleSelectionChange', val);
        this.selectedFiles = _.chain(val).filter(v => v.isFile).map((item: any) => { return item as IHDFSFile }).value();
        let packages: any = _.clone(this.nbClusterPackage);
        let c_hd = _.remove(packages, (v: any) => { return _.find(this.getHdfsPaths(), (sv: any) => { return sv == v})});
        let sf_hd = _.intersection(this.getSelectedPaths(),this.getHdfsPaths());
        this.nbClusterPackage = _.concat(packages, sf_hd);
        let notebook: INotebook = this.$store.state.workspace.workspaces;
        // console.log('current notebook',notebook);
    }
    //selected notebook
    getSelectedPaths(){
        return this.selectedFiles.map(function (item) {
            return item.filePath;
        });
    }
    getHdfsPaths(){
        return this.hdfsList.map(function (item) {
            return item.filePath;
        });
    }
    applyEvent() {
        if (this.notebookStatus === "OFFLINE") {
            this.updateApplyPackage();
        } else if (this.notebookStatus === "ONLINE") {
            if (_.intersection(this.nbClusterPackage, this.currentNBPackages).length == this.currentNBPackages.length) {
                this.updateApplyPackage();
            }else{
                console.log('need disconnect')
                this.applyAlert = true;
            }
        }
    }

    update(){
            this.updating = true;
            // disconnect
            this.$store.dispatch("setNotebookStatus", {
                nid: this.notebookId,
                status: NotebookStatus.DISCONNECTING
            });
            return wsclient
                .notebookDisconnect(this.notebookId)
                .then(() => {
                    this.applyAlert = false;
                    this.$store.dispatch("setNotebookStatus", {
                        nid: this.notebookId,
                        status: NotebookStatus.OFFLINE
                    });
                    this.updating = false;
                    this.updateApplyPackage();
                })
                .catch((e: any) => {
                    console.error("save settings in apply package failed", e);
                    e.message = 'ail to apply settings.'
                });
    }
    updateApplyPackage(){
        this.updating = true;
        let newPaths = this.nbClusterPackage;
        let appendPaths = _.difference(newPaths, this.currentNBPackages);
        let isAppend = (this.notebookStatus === 'OFFLINE')? false:true;
        let packageList = (this.notebookStatus === 'OFFLINE')? newPaths:appendPaths;
        let params = {
            "isAppend": isAppend,
            "packageList": {
                [this.cluster]: packageList
            }
        }
        console.log('request params',params);
        const _this: any = this;
        this.notebookRemoteService.apply(this.notebookId, params).then(res =>{
            this.updating = false;
            this.applyDisabled = true;
            let nbPg = {
                [this.cluster]: (this.notebookStatus === 'OFFLINE')? newPaths:_.concat(appendPaths, this.currentNBPackages)
            }
            this.$store.dispatch("setPackages", {
                nid: this.notebookId,
                packages: nbPg
            });
            let notebook: any = this.$store.state.workspace.workspaces
            this.currentNBPackages = this.notebookId && notebook ? notebook[this.notebookId].packages[this.cluster] : [];
            this.$message({
                message: 'apply packages success!',
                type:'success'
            })

        }).catch((e: ZetaException) => {
            this.updating = false;
            console.error(e);
        })
    }
    gotoHDFS(){
        this.$router.push(`/hdfs`);
    }
    @Watch("selectedFiles")
    handleSelected(){
        this.applyAlert = false;
        let eq = _.isEqual(this.nbClusterPackage.sort(), this.currentNBPackages.sort());
        if(eq){
            this.applyDisabled = true;
        } else {
            this.applyDisabled = false;
        }
    }
    @Watch("files")
    handleHDFSFiles(newVal: Array<IHDFSFile>, oldVal: Array<IHDFSFile>){
        let notebook = this.$store.state.workspace.workspaces[this.notebookId];
        this.currentNBPackages = notebook.packages ? (notebook.packages[this.cluster]||[]) : [];
        if(this.hdfsList.length>0 && !this.flag){
            _.forEach(this.currentNBPackages, (paths)=>{
                this.toogleSelection(paths);
            })
        }
    }
}
</script>
<style lang="scss" scoped>
.packageList {
    h2{
        p{
            display: inline-block;
            font-size: 14px;
            font-weight: normal;
        }
    }
    .package-list{
        .folder{
            cursor: pointer;
        }
    }
    .advanced-btn-group {
        text-align: right;
    }
    .apply {
        text-align: right;
        .info {
            width: 400px;
            text-align: left;
        }
        .apply-confirm {
            text-align: right;
        }
    }
    .tip{
        font-size: 12px;
        color: #999999;
    }
}
.notebook-tool-btn {
    position: relative;
    .zeta-icon-upload{
        transition: .3s;
        font-size: 22px;
    }
    .params-title{
        font-size: 12px;
        line-height: 20px;
        transition: .3s;
        position: absolute;
        display: none;
    }
    &:hover{
        .zeta-icon-upload{
            margin-right: 30px;
        }
        .params-title{
            transform: translateX(-30px);
            display: inline-block;
        }
    }
}
</style>

