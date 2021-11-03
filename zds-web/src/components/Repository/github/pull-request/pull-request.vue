<template>
    <span class="pull-request">
        <el-button plain  @click="onDialogOpen" v-click-metric:REPO_TOOLBAR_CLICK="{name: 'newPullRequest'}">New Pull Request</el-button>
        
        <!-- file select dialog -->
        <el-dialog title="New Pull Request" width="560px" :visible.sync="visible" :close-on-click-modal="false">
            <div class="pull" v-loading="loading">
                <div class="repo">
                    <!-- <div class="repo-url">
                        url <el-input v-model="url" @change="onUrlChange"/>
                        
                    </div> -->
                    <el-form ref="form" label-width="60px">
                        <el-form-item label="Url">
                            <template slot="label">
                                <i class="el-icon-loading" v-if="fecthBranchLoading"/>URL
                            </template>
                            <el-autocomplete
                                :class="{'url-error':alertShow}"
                                class="inline-input"
                                :fetch-suggestions="querySearch"
                                v-model="form.url"
                                clearable 
                                :disabled="fecthBranchLoading" 
                                placeholder="example: https://github.corp.ebay.com/nt/repo"
                            ></el-autocomplete>
                            <div class="url-error-message" v-if="alertShow"><i class="el-icon-warning"/>{{alertMessage}}</div>
                        </el-form-item>
                        <el-form-item label="Branch">
                            <el-select v-model="form.branch" :disabled="!form.branch || form.branches.length == 0" filterable>
                                <el-option v-for="(b,i) in form.branches" :key="i" :label="b" :value="b"></el-option>
                            </el-select>
                        </el-form-item>
                        <el-form-item class="actions">
                            <el-button type="primary" @click="listFiles" :disabled="!(form.url && form.branch) || !(selector.url === '' || selector.branch === '')" v-click-metric:GITHUB_CLICK="{name: 'getList'}">List</el-button>
                        </el-form-item>
                    </el-form>

                </div>
                <hr v-if="selector.url && selector.branch" class="line"/>
                <div class="files" v-if="selector.url && selector.branch">
                    <span style="font-size: 14px; font-weight: bold">Pull List</span>
                    <el-input v-model="selector_search.words" clearable :disabled="selector_search.loading" placeholder="search files"/>
                    <FilesTreeSelector v-if="!selector_search.words" ref="treeSelector" :url="selector.url" :branch="selector.branch" @onSelected="c => selector.selected = c"/>
                    <FilesListSelector v-else ref="listSelector" v-model="selector_search.searchFiles" :loading="selector_search.loading" />
                    <!-- <el-button @click="clear">clear</el-button>  -->
                </div>
                <div class="actions" v-if="selector.url && selector.branch">
                   <el-button type="primary" @click="checkExist" :disabled="!selected" v-click-metric:GITHUB_CLICK="{name: 'pull'}">Pull {{selected > 0 ? ( '(' + selected + ')' ) : ''}}</el-button>
                </div>
            </div>
        </el-dialog>
        <!-- resolve exist dialog -->
        <el-dialog width="360px" :visible.sync="overrideVisible" :close-on-click-modal="false">
            <template slot="title">
                <div>
                    <div class="title">
                        <i class="el-icon-warning"/>
                        <span>Error</span>
                    </div>
                    <span> An error occurred while pulling from Github </span>
                    <br/>
                    <span>Do you want to override it?</span>
                </div>
            </template>
            <div class="override" v-loading="overrideLoading">
                <div class="files file-exist">
                    <FilesListSelector :selectable="false" v-model="existFiles" >
                        <template slot="item" slot-scope="scope"><div class="override-item"><span>{{scope.item.path}}</span> <span style="text-align: left" class="override-error-msg">{{scope.item.errorMsg}}</span></div></template>
                    </FilesListSelector>
                </div>
                <div class="actions">
                    <el-button type="primary" @click="pull">Override</el-button>
                    <el-button plain @click="overrideVisible = false">Cancel</el-button>
                </div>
            </div>
        </el-dialog>
        <!-- <AlertDialog v-model="alertDialogShow" :message="alertMessage"/> -->
    </span>    
</template>
<script lang="ts">
import { Component, Vue, Prop, Watch, Emit, Inject } from "vue-property-decorator";
import GitRemoteService from '@/services/remote/GithubService'
import FilesTreeSelector from './files-tree-selector.vue'
import FilesListSelector from './files-list-selector.vue'
import AlertDialog from '../alert-dialog.vue'
import Util from "@/services/Util.service";
import { FileSelector, GithubFile} from "@/components/Repository/github/pull-request";
import { errprHandler, ERROR_MAP, getStorageSearchUrl, saveStorageSearchUrl, createFilter} from "@/components/Repository/github/utilities";
import _ from 'lodash';
import { AxiosError } from "axios";
interface storageUrl{
    value: string
}
interface PullRequestForm{
    url: string
    branch: string | undefined
    branches?: string[]
}
function parseObject2File(obj:Dict<string>){
    return _.map(obj,(sha:string,path:string) => {
        return <GithubFile>{
            fullPath: path,
            path,sha,type: 'FILE_NORMAL',selected:false
        }
    })
}
function getPath(files:GithubFile[]){
    return _.map(files, file => {
        return {path: file.fullPath}
    })
}
@Component({
  components: {
      FilesTreeSelector,
      FilesListSelector,
      AlertDialog
  }
})
export default class PullRequest extends Vue {
    @Inject('gitRemoteService')
    gitRemoteService: GitRemoteService

    @Prop({type:String, default:""})
    defaultLink:string
    @Prop({type:String, default:"/"})
    zetaPath: string
    fecthBranchLoading: boolean = false;
    loading = false;
    overrideLoading = false;
    visible = false;
    overrideVisible = false;
    pullError = false;
    storageKey: string = "gitPullUrl";
    storageSearchUrl: Array<storageUrl> = [];
    debounceFetchGithubUrl: (url: string) => void
    debounceSearch: (fileName: string) => void
    form:PullRequestForm = {
        url: '',
        branch: undefined
    }
    selector = {
        url: '',
        branch: '',
        selected: 0
    }
    selector_search = {
        words: '',
        loading: false,
        searchFiles: [] as GithubFile[]
    }
    existFiles: any[] = [];
    alertShow = false;
    alertMessage = '';
    onDialogOpen(){
        this.visible = true;
        this.overrideVisible = false;
        this.pullError = false;
        this.form.url = this.defaultLink || ''
        this.form.branch = ''
        this.onUrlChange(this.form.url);
        this.clearFileResult();
        this.storageSearchUrl = getStorageSearchUrl(this.storageKey);
    }
    clearFileResult(){
        this.selector.url = '';
        this.selector.branch = '';
        this.selector.selected = 0;
        this.selector_search.words = '';
        this.selector_search.loading = false;
        this.selector_search.searchFiles = [];
        this.existFiles = [];
    }
    mounted(){
        if(this.defaultLink){
            this.form.url = this.defaultLink;
            this.onUrlChange(this.form.url)
        }
        this.debounceFetchGithubUrl = _.debounce(this.loadGithubUrl, 1500);
        this.debounceSearch = _.debounce(this.fetchFiles, 1500);
    }
    querySearch(queryString: string, cb: Function) {
        let url = this.storageSearchUrl;
        let results = queryString ? url.filter(createFilter(queryString)) : url;
        // call callback 
        cb(results);
    }
    async loadGithubUrl(url: string){
        this.fecthBranchLoading = true;
        this.gitRemoteService.fetchBranches(Util.getNt(), url).then(({data: branches}) => {
            this.form.branches = branches;
            if(branches && branches[0]){
                this.form.branch = branches[0]
            }
            this.alertShow = false;
            this.alertMessage = '';
            this.fecthBranchLoading = false;
            saveStorageSearchUrl(this.storageKey, url);
            this.storageSearchUrl = getStorageSearchUrl(this.storageKey);
        }).catch((err: AxiosError) => {
            this.form.branch = ''
            this.form.branches = []
            errprHandler(err).then((msg: string) => {
                // this.visible = false;
                this.alertShow = true;
                this.alertMessage = msg;
                this.fecthBranchLoading = false;

                this.selector.url = '';
                this.selector.branch = '';
            })
        });
    }
    async fetchFiles(word: string){
        this.selector_search.loading = true;
        this.gitRemoteService.searchFiles(Util.getNt(), this.form.url,this.form.branch || '', word).then(({data}) => {
            const fileDict = data as Dict<string>
            this.selector_search.searchFiles = parseObject2File(fileDict)
            this.selector_search.loading = false;
        }).catch(() => {
            this.selector_search.loading = false;
        })
    
    }
    @Watch('form.url')
    onUrlChange(url:string){
        if(!url || !this.visible){

            return 
        }
        this.debounceFetchGithubUrl(url)
        this.clearFileResult()
    }
    @Watch('form.branch')
    onBranchChange(branch: string){
        this.clearFileResult()
    }
    @Watch('selector_search.words')
    searchFiles(word:string){
        if(!word){
            this.selector_search.searchFiles = []
            return;
        }
        this.debounceSearch(word)
    }
    listFiles() {
        this.selector.url = this.form.url
        this.selector.branch = this.form.branch || ''
    }
    getSelected() :null | GithubFile[]{
        if(this.selector_search.words){
            // let selector: FileSelector = this.$refs.listSelector as FilesListSelector as FileSelector;
            // if(!selector)
            return _.filter(this.selector_search.searchFiles, file => file.selected)
        }
        else{

            let selector: FileSelector = this.$refs.treeSelector as FilesTreeSelector as FileSelector;
            if(!selector || !selector.getSelectedNodes){
                return null
            }
            let nodes = selector.getSelectedNodes()
            return nodes;
        }

    }
    get selected(){
        if(this.selector_search.words){
            // let selector: FileSelector = this.$refs.listSelector as FilesListSelector as FileSelector;
            // if(!selector)
            const selected = _.filter(this.selector_search.searchFiles, file => file.selected)
            return selected.length
        }
        else{

            return this.selector.selected
        }
    }
    checkExist(){
        let files = this.getSelected()
        if(files && files.length > 0){
            this.loading = true;
            this.gitRemoteService.checkNotebookExists(Util.getNt(),this.form.url, this.form.branch as string, this.zetaPath,getPath(files)).then(({data}) => {
                // success
                if(_.keys(data).length == 0){
                    this.gitRemoteService.pull(Util.getNt(),this.form.url, this.form.branch as string, this.zetaPath ,getPath(files as GithubFile[])).then(() => {
                        this.loading = false;
                        this.visible = false;
                        this.pullSuccess()
                    })
                } else {
                    const existFiles = data;
                    this.loading = false;
                    this.existFiles = _.map(existFiles, (msg, path) => {
                        return {
                            path,
                            errorMsg: msg
                        }
                    })
                    this.visible = false;
                    this.overrideVisible = true;

                }
            })
        } else {
            // TODO @Exception need to mock a exception object
            this.pullFail("no files selected");
        }
        
    }

    pull(){
        const files = this.getSelected();
        if(files && files.length > 0){
            this.overrideLoading = true;
            return this.gitRemoteService.pull(Util.getNt(),this.form.url, this.form.branch as string, this.zetaPath ,getPath(files))
            .then(() => {
                this.overrideLoading = false;
                this.overrideVisible = false;
                this.pullSuccess()
            }).catch((msg) => {
                this.overrideLoading = false;
                this.overrideVisible = false;
                // this.pullFail(msg)
            })
        }   
    }
    @Emit('pullSuccess')
    pullSuccess(){

    }
    @Emit('pullFail')
    pullFail(msg: string){

    }
}
</script>
<style lang="scss" scoped>
@import "@/styles/global.scss";
.url-error-message, .override-error-msg{
    line-height: 16px;
    font-size: 14px;
    color: $zeta-global-color-red;
    display: flex;
    align-items: center;
}
.inline-input{
    width: 100%;
}
.el-input.url-error{
    /deep/ .el-input__inner{
        border:1px solid $zeta-global-color-red;
    }
}
.pull-request {
    .el-button{
        margin-left: 10px;
    }
}
.files{
    margin: 10px 0;
    > .el-input {
        margin-top: 10px;
    }
}
.el-form {
    margin: 0;
    /deep/ .el-form-item__label {
        text-align: left;
    }
    /deep/ .el-input {
        height: 30px;
        > input {
            height: 30px !important;
        }
    }
    /deep/ .el-form-item {
        margin: 0;
    }
}
/deep/ .el-dialog{
    .el-dialog__body {
        padding: 10px 30px 30px 30px;
    }
}
.actions{
    padding-top: 10px;
    text-align: right;
    .el-button{
        min-width: 90px;
    }
}
/deep/ .selector{
    margin: 10px 0;
    max-height: 300px;
    overflow-y: auto;
    border: 1px solid $zeta-global-color-disable;
    border-radius: 4px;
}
.title{
    display: flex;
    align-items: center;
    margin-bottom: 10px;
    i{
        color:red;
        align-self: center;
        margin: 0px 10px 0 0;
    }
    i:before {
        font-size: 25px;
    }
    span{
        font-weight: bold;
        font-size: 18px;
    }
}
/deep/ .override {
    > .files.file-exist {
        margin: 5px 0;
        margin-bottom: 20px;
        >.file-selector.selector {
            margin: 0;
            border: none;
            .file-item {
                padding-left: 0;
            .override-item {
                margin-right: 10px;
                display:flex;
                justify-content: space-between;
            }
            }
        }
    }
}
</style>
