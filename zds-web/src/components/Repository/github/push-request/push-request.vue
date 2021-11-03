<template>
    <span class="push-request">
        <el-button plain  @click="onDialogOpen" :disabled="btnDisabled" v-click-metric:REPO_TOOLBAR_CLICK="{name: 'newPushRequest'}">New Push Request</el-button>
        
        <!-- file select dialog -->
        <el-dialog width="560px" title="New Push Request" :visible.sync="visible" :close-on-click-modal="false">
            <div v-loading="loading">   
                <div class="form">
                    <el-form ref="form" label-width="90px" label-position="left">
                        <el-form-item label="Url" :required="true">
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
                            <!-- <el-input 
                                v-model="form.url" 
                                clearable 
                                :disabled="fecthBranchLoading" 
                                placeholder="example: https://github.corp.ebay.com/nt/repo"
                                :class="{'url-error':alertShow}"></el-input> -->
                            <div class="url-error-message" v-if="alertShow"><i class="el-icon-warning"/>{{alertMessage}}</div>
                        </el-form-item>
                        <el-form-item label="Branch"  :required="true">
                            <!-- <el-select v-model="form.branch" :disabled="!form.branch">
                                <el-option v-for="(b,i) in form.branches" :key="i" :label="b" :value="b"></el-option>
                            </el-select> -->
                            <BranchSelector v-model="form.branch" :branches="form.branches" 
                                :disabled="!form.branch || form.branches.length == 0"
                                :newBranch="form.newBranch" @emitNewBranch="val => form.newBranch = val"
                            />
                        </el-form-item>
                        <el-form-item label="Tag">
                            <el-input v-model="form.tag"></el-input>
                        </el-form-item>
                        <el-form-item label="Comment" class="commit-message" :required="true">
                            <el-input v-model="form.commit" type="textarea"></el-input>
                        </el-form-item>
                    <!-- <el-form-item class="actions">
                        <el-button type="primary" >list</el-button>
                    </el-form-item> -->
                    </el-form>
                </div>
                <hr class="line">
                <div class="files">
                    <span style="font-size: 14px; font-weight: bold;line-height: 30px">Push List</span>
                    <ul class="files-list">
                        <li v-for="(f,$i) in seletedFiles" :key="$i">
                            {{f.fullPath}}
                        </li>
                    </ul>
                </div>
                <div class="actions">
                    <el-button type="primary" :disabled="pushDisable"  @click="checkFile" v-click-metric:GITHUB_CLICK="{name: 'push'}">Push</el-button>
                    <el-button plain @click="visible = false">Cancel</el-button>
                </div>
            </div>
        </el-dialog>
        <!-- resolve exist dialog -->
        <el-dialog width="360px" :visible.sync="overwriteVisible" :close-on-click-modal="false">
            <template slot="title">
                <div>
                    <div class="title">
                        <i class="el-icon-warning"/>
                        <span>Error</span>
                    </div>
                    <span> An error occurred while pushing from Github</span>
                    <br/>
                    <span>Do you want to override it?</span>
                </div>
            </template>
            <div class="override" v-loading="overwriteLoading">
                <div class="files file-exist">
                <FilesListSelector :selectable="false" v-model="errorFiles">
                    <!-- <template slot="item" slot-scope="scope">{{scope.item.fullPath}}:  <span class="override-error-msg">{{scope.item.errorMsg}}</span> </template> -->
                    <template slot="item" slot-scope="scope"><div class="override-item"><span>{{scope.item.fullPath}}</span> <span style="text-align: left" class="override-error-msg">{{scope.item.errorMsg}}</span></div></template>
                </FilesListSelector>            
                </div>
                <div class="actions">
                    <el-button type="primary" @click="push">Override</el-button>
                    <el-button plain @click="overwriteVisible = false">Cancel</el-button>
                </div>
            </div>
        </el-dialog>
    </span>    
</template>
<script lang="ts">
import { Component, Vue, Prop, Watch, Emit, Inject} from "vue-property-decorator";
import GitRemoteService from '@/services/remote/GithubService'
import Util from "@/services/Util.service";
import { GithubFile} from "./";
import _ from 'lodash';
import BranchSelector  from './branch-selector.vue'
import FilesListSelector from '../pull-request/files-list-selector.vue'
import AlertDialog from '../alert-dialog.vue'
import { ZetaFile } from "@/components/Repository/github/types";
import { errprHandler, ERROR_MAP, getStorageSearchUrl, saveStorageSearchUrl, createFilter} from "@/components/Repository/github/utilities";
import { AxiosError } from "axios";
interface storageUrl{
    value: string
}
interface PushRequestForm{
    url: string
    branch: string | undefined,
    newBranch?: string
    branches?: string[]
    tag?: string
    commit: string
}
@Component({
  components: {
      BranchSelector,
      FilesListSelector,
      AlertDialog
  }
})
export default class PushRequest extends Vue {
    @Inject('gitRemoteService')
    gitRemoteService: GitRemoteService

    @Prop({type:String, default:""})
    defaultLink:string
    @Prop({type:String, default:"/"})
    zetaPath: string
    @Prop({type:Boolean, default: true})
    disabled: boolean
    get btnDisabled(){
        return this.disabled
    }
    @Prop()
    files: ZetaFile[]
    get seletedFiles (){
        return this.files
    }

    errorFiles: ZetaFile[] = []

    formModal = {
        url:"",
        branch:""
    }
    form:PushRequestForm = {
        url: '',
        branch: '',
        newBranch: '',
        branches: [],
        tag: '',
        commit: ''
    }
    visible = false;
    loading = false;
    
    overwriteVisible = false;
    overwriteLoading = false;

    fecthBranchLoading: boolean = false;
    storageKey: string = "gitPushUrl";
    storageSearchUrl: Array<storageUrl> = [];
    debounceFetchGithubUrl: (url: string) => void
    alertShow = false;
    alertMessage = '';
    get pushDisable(){
        return !this.form.url || !this.form.branch || !this.form.commit
    }
    mounted(){
        this.debounceFetchGithubUrl = _.debounce(this.loadGithubUrl, 1500);
    }
    onDialogOpen(){
        this.visible = true;
        this.form.url = this.defaultLink;
        if(this.form.url){
            this.onUrlChange(this.form.url)
        }
        this.errorFiles = []
        this.storageSearchUrl = getStorageSearchUrl(this.storageKey);
    }
    querySearch(queryString: string, cb: Function) {
        let url = this.storageSearchUrl;
        let results = queryString ? url.filter(createFilter(queryString)) : url;
        // call callback 
        cb(results);
    }
    async loadGithubUrl(url: string) {
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
        },(err: AxiosError) => {
            this.form.branch = ''
            this.form.branches = []
            errprHandler(err).then((msg: string) => {
                this.alertShow = true;
                this.alertMessage = msg;
            })
            this.fecthBranchLoading = false;
        });
    }
    @Watch('form.url')
    onUrlChange(url:string){
        if(!url || !this.visible){
            return 
        }
       this.debounceFetchGithubUrl(url)
    }
    push(){
        if(this.seletedFiles.length == 0){
            return new Promise((r,reject) => {
                reject('no files selected')
            })
        }
        else{
            this.overwriteLoading = true;
            return this.gitRemoteService.push(Util.getNt(), this.form.url, this.form.commit, this.seletedFiles, this.form.branch as string, this.form.newBranch, this.form.tag).then(() => {
                this.overwriteVisible = false;
                this.overwriteLoading = false;
                this.pushSuccess()
            }).catch((error) => {
                this.overwriteLoading = false;
                // this.pushFail(`push error: ${error}`)
            })
        }
        
    }
    checkFile(){
        /**
         * step 0: create branch if choose a new branch
         * step 1: check file exist
         * step 2: git push
         */
        const createBranch = () => {
            return this.gitRemoteService.createBranch(Util.getNt(), this.form.url, this.form.branch as string , this.form.newBranch as string).catch(error => {
                this.loading = false;
                console.warn(`create branch error`, error)
                // this.pushFail(`create branch error`)
            })
        }
        const checkFile = () => {
            const promise: Promise<any> = this.gitRemoteService.checkGitFileExists(Util.getNt(), this.form.url, this.form.branch as string, this.files) as Promise<any>
            return promise.then(res => {
                const data = res.data
                if(Object.keys(data).length > 0){
                    _.forEach(data, (msg: string, p: string) => {
                        const file = _.clone(_.find(this.files, f => f.fullPath == p))
                        if(file){
                            file.errorMsg = msg;
                            this.errorFiles.push(file);
                        }
                    })
                    this.visible = false;
                    this.loading = false;
                    this.overwriteVisible = true;
                }
                else{
                    this.gitRemoteService.push(Util.getNt(), this.form.url, this.form.commit, this.seletedFiles, this.form.branch as string, this.form.newBranch, this.form.tag).then(() => {
                        this.visible = false;
                        this.loading = false;
                        this.pushSuccess()
                    }).catch(error => {
                        console.warn(`push error`, error)
                        // this.pushFail(`push error`)
                    })
                }
            }).catch(error => {
                this.loading = false;
                console.warn(`check file error`, error)
                // this.pushFail(`check file error`)
            })
        }
        if(this.files.length ===0){
            return    
        }

        this.loading = true;
        // let promise = this.form.newBranch ? createBranch().then(() => checkFile()) : checkFile()
        if(this.form.newBranch){
            const newBranch = this.form.newBranch;
            return createBranch().then(() => {
                if(this.form.branches) this.form.branches.push(newBranch)
                this.form.branch = newBranch;
                this.form.newBranch = '';
                return checkFile()
            })
        }
        else{
            return checkFile()
        }
        
    }

    @Emit('pushSuccess')
    pushSuccess(){

    }
    @Emit('pushFail')
    pushFail(msg: string){

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
/deep/ .form {
    margin-bottom: 20px;
    /deep/ .el-form-item {
        margin: 0;
    }
    /deep/ .commit-message {
        margin: 5px 0;
    }
}
/deep/ .el-dialog__body {
    padding: 10px 30px 30px 30px;
}
.push-request {
    .el-button{
        margin-left: 10px;
    }
}
.actions{
    text-align: right;
    .el-button{
        min-width: 90px;
        & + .el-button{
            margin-left: 10px;
        }
    }
}
.files{
    padding: 10px 5px;

    .files-list{
        li{
            margin: 0;
            line-height: 30px;
            list-style-type: none;
        }
    }
}

/deep/ .selector{
    max-height: 300px;
    overflow-y: auto;
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
/deep/ .commit-message {
    > label {
        line-height: 20px;
    }
}
/deep/ .override {
    > .files.file-exist {
        margin: 0px;
        margin-bottom: 20px;
        padding: 0px;
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
