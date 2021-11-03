<template>
<div class="sub-editor-item"
    v-loading="delLoading"
    :element-loading-text="loadingTxt"
>
    <div class="sub-editor-header">
        <div class="sub-editor-header-left">
            <!-- <div class="snb-name">{{notebook.name}}</div> -->
            <!-- <div class="sun-status">{{status}}</div> -->
            <SubEditorStatus :status="status" />
            <InputSwitch class="snb-name" v-model="notebook.name" size="mini" :disabled="true"/>

            <div class="fence"></div>
            <!-- :disabled="history.undo === 0" -->
            <el-button
                class="edit-button"
                type="text"
                title="undo"
                @click="onUndo">
                <i class="zeta-icon-undo1" style="font-size: 20px;"/>
            </el-button>
            <!-- :disabled="history.redo === 0" -->
            <el-button
                class="edit-button"
                type="text"
                title="redo"
                @click="onRedo">
                <i class="zeta-icon-redo1" style="font-size: 20px;"/>
            </el-button>
            <div class="fence"></div>
            <el-button
                class="edit-button"
                type="text"
                title="redo"
                :disabled="status !== 'IDLE'"
                @click="runCell">
                    <template v-if="running">
                        <i class='el-icon-loading'/>
                        Running
                    </template>
                    <template v-else>
                        <i class="zeta-icon-run"/>
                        Run
                    </template>
            </el-button>
        </div>
        <div class="sub-editor-header-right">
            <el-dropdown trigger="click" @command="onModeChange">
                <span class="el-dropdown-link">
                    {{mode}}<i class="el-icon-arrow-down el-icon--right"></i>
                </span>
                <el-dropdown-menu slot="dropdown">
                    <el-dropdown-item v-for="(lang, name) in modeOptions" :key="name" :command="name">{{name}}</el-dropdown-item>
                </el-dropdown-menu>
            </el-dropdown>
            <!-- <template v-if="saving">
                Auto Saving
                <i class="el-icon-loading"/>
            </template> -->
        </div>

    </div>
    <div class="sub-editor-code">
        <EditorBase
        ref="editor"
        class="editor-base"
        :value="notebook.code"
        @input="onCodeChange"
        :mode="mimeOptions[mode].mime"
        :viewOnly="dragging"
        @onFocus="onEditorFocus"/>
        <!--  -->
    </div>
    <ResultTabs v-show="showResult" class="sub-editor-result" :notebook="notebook"/>
</div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch, Inject, Emit } from 'vue-property-decorator'
import EditorBase from '@/components/common/editor/editor-base.vue';
import ResultTabs from '@/components/WorkSpace/Notebook/Result/QueryResult/ResultTabs.vue'
import { INotebook, INotebookConfig, NotebookStatus } from '@/types/workspace';
import { RestPacket } from '@/types/RestPacket';
import Util from '@/services/Util.service';
import moment from 'moment';
import _ from 'lodash';
import { langMap } from '../';
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import { JobManager, SubNoteJob } from '../job-queue-manager';
import InputSwitch from '@/components/common/input-switch.vue'
import SubEditorStatus from './sub-status.vue'

@Component({
  components: {
      EditorBase, ResultTabs, InputSwitch, SubEditorStatus
  },
})
export default class SubEditor extends Vue {
    @Inject()
    notebookRemoteService: NotebookRemoteService
    @Prop()
    jobManager: JobManager
    @Prop({ default: false })
    dragging: boolean
    @Prop()
    value: INotebook
    get notebook(){
        return this.value
    }
    get showResult() {
        const res = _.pick(this.$store.state.workspace.jobs, this.notebook.jobs);
        if(!_.isEmpty(res)) return true
        return false;
    }
    delLoading = false;
    loadingTxt = '';
    saving = false;
    debounceSave: Function
    mode: string = 'Python'
    constructor(){
        super()
        this.debounceSave = _.debounce(this.saveNb,3000, {
            trailing: true
        })
    }
    get mimeOptions() {
        return langMap
    }
    get status() {
        const nId = this.notebook.notebookId;
        const waitingExecute = this.jobManager ? this.jobManager.waitingJobs.find((sJob: SubNoteJob) => sJob.notebookId === nId) : undefined;

        if(waitingExecute) return "PENDING"
        return this.notebook.status
    }
    get running () {
        return this.notebook.status == NotebookStatus.RUNNING;
    }
    mounted(){
        if(this.notebook.preference && this.notebook.preference['subNotebook.lang']){
            this.mode = this.notebook.preference['subNotebook.lang'] as string
        }
        else{
            this.mode = 'Python'
        }

    }
    onCodeChange (code: string) {
        this.notebook.code = code;
        this.$applyNotebook(this.notebook)
    }
    onModeChange(mode: string) {
        this.mode = mode;
        if(this.notebook.preference) {
            this.notebook.preference['subNotebook.lang'] = mode;
        } else {
            this.$set(this.notebook, 'preference', {
                'subNotebook.lang': mode
            })
        }
        this.$applyNotebook(this.notebook)
    }
    @Watch('notebook.seq')
    onSeqChange(){
    }
    $applyNotebook(nb: INotebook){
        this.$store.dispatch("updateSubNotebook", nb)
        this.saving = true;
        this.debounceSave(nb);
        this.onNotebookChange(nb);
    }


    @Emit('input')
    onNotebookChange($nb: INotebook){

    }
    @Emit("onEditorFocus")
    onEditorFocus() {

    }
    get modeOptions() {
        return langMap
    }

    onUndo() {
        (this.$refs.editor as EditorBase).undo();
    }
    onRedo() {
        (this.$refs.editor as EditorBase).redo();
    }
    runCell() {
        const language = this.notebook.preference && this.notebook.preference["subNotebook.lang"] ? this.notebook.preference["subNotebook.lang"] || "Python" : "Python"
        const interpreter = langMap[language].interpreter
        const codeType = langMap[language].codeType
        const codeEmpty = _.isEmpty(this.notebook.code.trim());
        if(codeEmpty) return undefined
        const job: SubNoteJob = {
            notebookId: this.notebook.notebookId,
            code: this.notebook.code,
            interpreter,
            codeType
        }
        this.jobManager.appendJobs([job])
    }
    delSubNotebook(){
        this.delLoading = true
        this.loadingTxt = 'removing sub notebook'
        this.notebookRemoteService.del(this.notebook.notebookId).then(() => {
            this.$store.dispatch('deleteSubNotebook',this.notebook)
            this.delLoading = false;
        }).catch(() => {
            this.delLoading = false;
        })
    }
    saveNb(nb: INotebook){
        let file = this.$store.getters.fileByNId(this.notebook.collectionId);
        const path = (file.path || '') + '/' +file.title
        let nb_rest: RestPacket.File = {
            id: nb.notebookId,
            nt: Util.getNt(),
            content: nb.code,
            title: nb.name,
            createDt: nb.createTime,
            updateDt: moment().valueOf(),
            preference: JSON.stringify(nb.preference),
            path: path,
            nbType: 'sub_nb',
            status: ""
        }
        return this.notebookRemoteService.save(nb_rest)
            .then(() => {
                this.saving = false;
                this.$store.dispatch("updateSubNotebook", nb)
            })
            .catch((save) => {
                this.saving = false;
                this.$message.error("Fail to save to server");
            });
    }
}


</script>
<style lang="scss" scoped>
@import '@/styles/global.scss';
.fence {
    border-right: 1px solid $zeta-global-color-disable;
    margin: 0 10px;
}
.sub-editor-item{
    border:1px solid #ddd;
    .sub-editor-header{
        padding: 0 15px;
        background-color: $zeta-label-lvl1;
        border-bottom:1px solid #ddd;
        display: flex;
        justify-content: space-between;
        height: 30px;
        align-items: center;
        > .sub-editor-header-left{
            display: flex;
            > .snb-name{
                margin-left: 5px;
            }
            > .snb-name{
                width: 100px;
                font-weight: 800;
                display: flex;
                align-items: center;
                &.input-switch /deep/ .input-switch-modify{
                    .el-input__inner{
                        height: 24px;
                        line-height: 24px;
                        font-size: 12px;
                    }
                }
            }
        }
    }
    .sub-editor-code{
        .editor-base{
            height: 150px;

        }
    }
    .sub-editor-result{
        max-height: 480px;
    }
}
</style>
