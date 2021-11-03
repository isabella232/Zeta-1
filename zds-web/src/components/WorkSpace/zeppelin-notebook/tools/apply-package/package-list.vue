<template>
    <div class="packageList notebook-tool" v-loading="updating">
        <h2>Select File <p>(Currently import {{notebookPackages.length}})</p></h2>
        <div>
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
                <el-button type="primary" @click="gotoRepo">Manage</el-button>
                <el-button type="primary" :disabled="applyDisabled" @click="update">Apply</el-button>
            </div>
        </div>
    </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch, Emit, Provide, Inject, Ref, } from "vue-property-decorator";
import { INotebook, NotebookStatus, } from "@/types/workspace";
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import _ from "lodash";
import Util from "@/services/Util.service";
import { IHDFSFile, IFile } from '@/types/workspace';
import moment from "moment";
import { Table as ElTable } from "element-ui";
import { wsclient } from '@/net/ws';
import { Getter, Action } from "vuex-class";
import { DefaultSortOptions } from "element-ui/types/table";
import ZeppelinApi from "@/services/remote/ZeppelinApi";
interface Selectable {
    selected: boolean;
}
@Component
export default class PackageList extends Vue {
    /** constant */
    readonly CLUSTER_ID: number = 14;
    readonly ROOT_PATH: string = '/user/'+ Util.getNt()+ '/';

    /** Inject remote service */
    @Inject()
    zeppelinApi: ZeppelinApi;

    @Prop()
    notebookId: string;

    @Ref('hdfsTable')
    readonly hdfsTable: ElTable;

    @Getter('nbByNId')
    getterNB: (nId: string) => INotebook;

    @Action
    setPackages: Function

    get notebook() {
        return this.getterNB(this.notebookId);
    }
    get notebookPackages() {
        const packages =  this.notebook.packages ? this.notebook.packages[this.CLUSTER_ID] : [];
        return packages ? packages : []
    }
    get numSelectedFiles(): number {
        return this.selectedFiles.length;
    }


    get hdfsList(){
        let list: Array<IHDFSFile & Selectable> = [];
        if (this.pwd !== this.ROOT_PATH) {
            list.push({
                filePath: '..',
                fileName: '..',
                isFile: false,
                selectable: false,
                selected: false,
            });
        }
        for (let item in this.files) {
            let type = this.files[item].isFile ? this.getType(item) : null;
            if(type === null || type === 'jar' || type === 'py' || type === 'zip' || type === 'egg'){
                list.push({
                    filePath: item,
                    fileName: this.getName(item),
                    isFile: this.files[item].isFile,
                    type: this.files[item].isFile ? this.getType(item) : null,
                    selectable: this.files[item].isFile,
                    updateTime: this.files[item].modifyTime,
                    selected: Boolean(this.files[item].selected)
                });
            }
        }
        return list.sort(this.sortByProp(this.sortProp));
    }

    /** ajax status */
    loading: boolean = false;
    updating: boolean = false;

    pwd: string = this.ROOT_PATH;
    files: Dict<IFile & Selectable> = {};
    selectedFiles: IHDFSFile[] = [];
    currentPaths: string[] = [];

    applyDisabled: boolean = false;

    // TODO rename field `flag`, flag means?
    flag: boolean = false;
    nbClusterPackage: any = [];
    sortProp: DefaultSortOptions = {
        prop: 'updateTime',
        order: 'descending'
    }
    sortChange(arg: DefaultSortOptions) {
        this.sortProp.prop = arg.prop;
        this.sortProp.order = arg.order;
    }
    sortByProp(sortProp: DefaultSortOptions) {
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
    onclickFolder(folder: string) {
        if (folder === '.') return;
        if (folder === '..') {
            if (this.pwd === this.ROOT_PATH) return;
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
        this.getFile().then(this.handleHDFSFiles);
    }
    getFile(){
        this.loading = true;
        return this.zeppelinApi.getPackages(this.notebookId, this.CLUSTER_ID, this.pwd).then(res => {
            this.loading = false;
            if (res.data && res.data.body) {
                const files = res.data.body;
                _.forEach(files, (f) => {
                    f.selected = Boolean(f.applied)
                });
                this.files = files;
            }
        }).catch(e => {
            this.loading = false;
            throw e;
        })
    }
    public init(){
        this.files = {};
        this.hdfsTable.clearSelection();
        this.updating = false;
        this.applyDisabled = true;
        this.flag = false;
        this.pwd = this.ROOT_PATH;
        this.getFile().then(() => {
            this.nbClusterPackage = _.chain(this.hdfsList).filter(file => file.selected).map(file => file.filePath).value();
            // this.setPackages({
            //     nid: this.notebookId,
            //     packages: {
            //         [this.CLUSTER_ID]: this.nbClusterPackage,
            //     },
            // });
            this.handleHDFSFiles()
        });

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
        this.selectedFiles = _.chain(val).filter(v => v.isFile).map((item: any) => { return item as IHDFSFile }).value();
        let packages: any = _.clone(this.nbClusterPackage);
        let c_hd = _.remove(packages, (v: any) => { return _.find(this.getHdfsPaths(), (sv: any) => { return sv == v})});
        let sf_hd = _.intersection(this.getSelectedPaths(),this.getHdfsPaths());
        this.nbClusterPackage = _.concat(packages, sf_hd);
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

    update(){
        this.updating = true;
        let params = {
            "isAppend": false,
            "packageList": {
                [this.CLUSTER_ID]: this.nbClusterPackage,
            },
        }
        this.zeppelinApi.updatePackages(this.notebookId, params).then(res =>{
            this.updating = false;
            this.applyDisabled = true;
            let nbPg = {
                [this.CLUSTER_ID]: this.nbClusterPackage
            }
            this.setPackages({
                nid: this.notebookId,
                packages: nbPg
            });
            this.$message({
                message: 'apply packages success!',
                type:'success'
            })

        }).catch((e) => {
            this.updating = false;
        })
    }
    gotoRepo(){
        this.$emit('close');
        this.$nextTick(() => {
            this.$router.push(`/hdfs`);
        });

    }
    @Watch("selectedFiles")
    handleSelected(){
        let eq = _.isEqual(this.nbClusterPackage.sort(), this.notebookPackages.sort());
        if(eq){
            this.applyDisabled = true;
        } else {
            this.applyDisabled = false;
        }
    }
    handleHDFSFiles(){
        if(this.hdfsList.length>0 && !this.flag){
            _.forEach(this.notebookPackages, (paths)=>{
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
</style>

