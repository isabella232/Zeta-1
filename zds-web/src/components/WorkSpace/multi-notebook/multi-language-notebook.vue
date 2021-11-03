<template>
<div class="multi-language-notebook workspace-container" >
    <div class="control">
        <div class="tint fluid-row">
            <div class="left">
                <connection :codeType="currentCodeType" :notebookId="notebook.notebookId" :isPublic="false"
                :capacity="queueCapacity"
                @onConnectionChange="onChange"/>
                <div class="fence"></div>
                <!-- tools -->
                    <el-button
                        class="edit-button"
                        type="text"
                        @click="createSubNotehook"
                        title="add new cell"
                        :disabled="false">
                        <i class="el-icon-plus" style="font-size: 18px"/>
                    </el-button>
                    <el-button
                        class="edit-button"
                        type="text"
                        @click="delSubNotebook"
                        title="remove cell"
                        :disabled="!activeIndex">
                        <i class="el-icon-minus" style="font-size: 18px"/>
                    </el-button>
                <div class="fence"></div>
                <div>
                    <el-button
						type="primary"
						class="run-button"
						:class="canRun() ? '' : 'disabled'"
                        :disabled="!canRun()"
						size="small"
						@click="onRun">
						<template>
							<!-- :class='{ disable: !canRun(notebook) }' -->
							<i class="zeta-icon-run"/>
							<template v-if="running">Running</template>
							<template v-else-if="submitting">Submitting</template>
							<template v-else>Run</template>
							<i class='el-icon-loading' v-if='submitting'></i>
						</template>
					</el-button>
                </div>
            </div>
        </div>
        <ConnectProgress :progress="notebook.progress" v-if="connecting"/>
    </div>
    <div class="editor-container" v-loading="addLoading || loading || hisLoading" :element-loading-text="loadingText">
        <!-- <div v-for="(nb) in subNbsArray" :key="nb.notebookId" @click="itemClick(nb.notebookId)" :class="{'active': nb.notebookId === activeIndex}" class="sub-editor">
            <SubEditor
                :ref='nb.notebookId'
                v-if="nb"
                :value="nb"
                :jobManager="jobManager"
                @input="$nb => nb = $nb"
                @onEditorFocus="() => itemClick(nb.notebookId)"/>
        </div> -->
        <draggable tag="div" :list="subNbsArray" class="sub-editor-list" :options="{handle:'.sub-editor-header'}" @change="onDragChange" @start="dragging = true" @end="dragging= false">
            <div v-for="(nb) in subNbsArray" :key="nb.notebookId" @click="itemClick(nb.notebookId)" :class="{'active': nb.notebookId === activeIndex}" class="sub-editor">
                <SubEditor
                    :ref='nb.notebookId'
                    v-if="nb"
                    :value="nb"
                    :jobManager="jobManager"
                    :dragging="dragging"
                    @input="$nb => nb = $nb"
                    @onEditorFocus="() => itemClick(nb.notebookId)"/>
            </div>
        </draggable>
        <div v-if="!hasSubNBs" class="editor-container-nodata">
            No Data
        </div>

    </div>

</div>
</template>

<script lang="ts">
import { Vue, Component, Prop, Provide, Watch } from 'vue-property-decorator'
import SubEditor from './editor/sub-editor.vue';
import { WorkspaceComponentBase, INotebook, IPreference, CodeType, IConnection, NotebookStatus, MultiNotebook, Notebook, IJob, JobStatus, IQueryContent, WorkSpaceType} from '@/types/workspace';
import ConnectProgress from "@/components/WorkSpace/Notebook/Editor/Tools/Progress.vue";
import { RestPacket } from '@/types/RestPacket';
import _ from 'lodash';
import uuid from 'uuid/v4';
import moment from 'moment';
import Util from '@/services/Util.service';
import { WorkspaceSrv as NBSrv } from '@/services/Workspace.service';
import Connection from '@/components/WorkSpace/Notebook/Editor/Tools/connection/connection.vue';
import { wsclient } from '@/net/ws';
import { langMap } from '.';
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import { ZetaExceptionProps } from '@/types/exception';
import { JobManager, SubNoteJob } from './job-queue-manager';
import draggable from 'vuedraggable';
let pretty = require('js-object-pretty-print').pretty;

function getNewCellName(snbs: Dict<INotebook> | undefined){
    const namePrefix = 'Cell '
    const reg = /Cell (.*\d)?/
    let index = _.chain(snbs)
    .map((snb) => {
        if(reg.test(snb.name)){
            let result = reg.exec(snb.name);
            let val = result && result[1] ? result[1] : '-1'
            return parseInt(val || '-1')
        }
        else{
            return 0
        }
    })
    .max().value()
    if(index === undefined){
        index = 0
    }
    return `${namePrefix}${index + 1}`

}
function getSeq(snbs: Dict<Notebook>, notebookId: string) {
    let seq = Object.keys(snbs).length - .5;
    _.chain(snbs).map().sortBy('seq').forEach((snb, $i) => {
        if(snb.notebookId === notebookId) {
            seq = $i + .5
            return seq
        }
    }).value()
    return seq
}
@Component({
  components: {
      SubEditor,
      Connection,
      ConnectProgress,
      draggable
  },
})
export default class MultiLanguageNotebook extends WorkspaceComponentBase {
    @Provide()
    notebookRemoteService = new NotebookRemoteService()
    @Prop()
    data: MultiNotebook
    get notebook():MultiNotebook{
        return this.data
    }
    get subNbsArray() {
        return _.chain(this.subNbs).map().sortBy('seq').value()
    }
    get subNbs(){
        return this.notebook.subNotebooks
    }
    get hasSubNBs(){
        return !_.isEmpty(this.subNbs)
    }
    get running() {
        // return this.notebook.status === NotebookStatus.RUNNING
        return this.jobManager.isRunning;
    }
    get submitting() {
        return this.notebook.status === NotebookStatus.SUBMITTING;
    }
    get connecting(){
        return this.notebook.status === NotebookStatus.CONNECTING;
    }
    get currentCodeType () {
        const defaultVal = CodeType.SQL;
        if(this.notebook.preference && this.notebook.preference['notebook.connection']){
            const connection = this.notebook.preference['notebook.connection'] as IConnection
            if(connection && connection.codeType){
                return connection.codeType
            }
        };
        return defaultVal
    }
    activeIndex: string = '';
    loading = false;
    hisLoading = false;
    addLoading = false;
    get loadingText() {
        if (this.loading) {
            return 'Init Notebook...';
        }
        if (this.hisLoading) {
            return 'Fecthing History...';
        }
        if (this.addLoading) {
            return 'Creating Cell...';
        }
    }
    queueCapacity: number | null | undefined = -1;
    jobManager: JobManager = new JobManager()
    dragging = false;
    canRun() {
        switch(this.notebook.status) {
            case NotebookStatus.OFFLINE:
            case NotebookStatus.CONNECTING:
            case NotebookStatus.DISCONNECTING:
            case NotebookStatus.SUBMITTING:
            case NotebookStatus.RUNNING:
                return false
        }
        if(this.running) return false;
        if(!this.hasSubNBs) return false;
        return true
    }
    onRun(){
        // run all
        this.runAll();
    }
    mounted(){
        const props: ZetaExceptionProps = {
            path: 'notebook',
            workspaceId: this.workspaceId
        }
        this.notebookRemoteService.props(props);
        this.jobManager.setNotebook(this.notebook);
        this.onReload()
        this.injectHistory()
    }
    activated() {
        this.onReload();
    }
    private injectHistory() {
        this.hisLoading = true;
        this.notebookRemoteService.getMultiNotebookHistory(this.notebook.notebookId).then(({data}) => {
            _.forEach(data, (job: any) => {
                const nid = job.notebookId;
                const jobId = job.jobId;
                this.$store.dispatch('addJob', { nid, jobId });
                const reqId = uuid()
                const codes = job.history.map((statement: any, seq: number) => {
                    return {
                        code: statement.statement,
                        seq
                    }
                })
                this.$store.dispatch("addQueries", { nid, jobId, reqId: reqId, codes });
                _.forEach(job.history, statement => this.injectStatement(statement, nid, reqId, jobId))
            })
            this.hisLoading = false;
        }).catch(() => {
            this.hisLoading = false;
        });
    }
    private injectStatement(statement: any, notebookId: string, reqId: string, jobId: string) {
        let result = JSON.parse(statement.result);
        let resultList = result.result;

        if (!resultList || resultList.length === 0) {
            console.warn("ResultList is empty", result);
            return;
        }
        else if (resultList.length > 1) {
            console.warn("ResultList.length > 1", result);
        }

		let resultIndex: number = -1;
        for (let i = 0; i < resultList.length; i++) {
			if (resultList[i].type === "TABLE") {
                resultIndex = i;
                break;
            }
		}

		if (resultIndex !== -1) {
			if (resultIndex >= resultList.length) {
				console.warn("Result Type is not table but OP is NB_CODE_JOB_DONE", result);
				return;
			}

			let resultTable = resultList[resultIndex];
			if (!resultTable) {
				console.warn("ResultTable not exist", result);
				return;
			}

			let headers = resultTable.schema;
			/* transform resultTable.rows from (key, value) style
			 * to array style
			 */
			// let indices: { [K: string]: number } = headers.reduce((pre, col, index) => ({ ...pre, [col]: index }), {})
			let rows = resultTable.rows.map((row: any) => {
				let cols = Object.keys(row)
                let arr = cols.map(col => row[col]);
				return arr
			})

			this.$store.dispatch("updateQuery", { jobId: statement.requestId, seqId: statement.seq, query: { endTime: statement.updateDt }});
			this.$store.dispatch("addQueryResult", { nid: notebookId, jobId: statement.requestId, reqId: uuid(),seqId: statement.seq, headers: headers, rows: rows });
		} else {
			this.$store.dispatch("updateQuery", { jobId: statement.requestId, seqId: statement.seq, query: { endTime: statement.updateDt }});
			let content: Array<IQueryContent> = [];
			for (let resultContent of resultList) {
				content.push(resultContent  as IQueryContent);
            }
            // error

            if(result.header.code === 'ERROR') {

                this.$store.dispatch("haltJob", { nid: notebookId, jobId, reqId, seqId: statement.seq, status: JobStatus.ERROR });
                // mock error message
                const error = content.find(c => c.type === 'TEXT')
                const info = "[" + pretty(error && error.content ? error.content : content) + "]"
                this.$store.dispatch("setJobStatus", { jobId, reqId, status: JobStatus.ERROR, info });
            } else {
                this.$store.dispatch("addQueryResult", { nid: notebookId, jobId: statement.requestId,reqId, seqId: statement.seq, content });
            }

        }
    }
    async onReload(){
        if (!this.notebook.loaded) {
            this.loading = true;
            try {
                let { data: file }: { data: RestPacket.File } = await this.notebookRemoteService.getById(this.notebook.notebookId);
                let nb: INotebook = NBSrv.file2nb(file);
                this.$store.dispatch("updateNotebook", nb)
                this.loading = false;
            }
            catch(e) {
                console.error("Fail to load notebook", e);
                this.$message.error("Fail to retrieve notebook. See log for detail.");
                this.loading = false;
            }

        }
        if(!this.hasSubNBs) {
            this.createSubNotehook()
        }
        this.updateSequences()
    }
    onChange(){

    }
    itemClick(index: string){
        this.activeIndex = index;
    }
    createSubNotehook(){
        this.addLoading = true;
        let preference = <IPreference> {
            'subNotebook.lang': 'Python'
        }
        let file = this.$store.getters.fileByNId(this.notebook.notebookId);
        const path = (file.path || '') + '/' +this.notebook.name
        const seq = getSeq(this.subNbs, this.activeIndex);
        let nb_rest: RestPacket.File = {
            id: uuid(),
            nt: Util.getNt(),
            content: "",
            path: path,
            title: getNewCellName(this.subNbs),
            createDt: moment().valueOf(),
            updateDt: moment().valueOf(),
            status: "",
            preference: JSON.stringify(preference),
            opened: 1,
            seq,
            nbType: 'sub_nb',
            collectionId: this.notebook.notebookId
        };

        this.notebookRemoteService.add(nb_rest).then(({data}) => {
            let snb: INotebook = NBSrv.file2nb(data as RestPacket.File)
            snb.seq = seq;
            this.$store.dispatch("addSubNotebook", snb)
            this.addLoading = false;
            this.updateSequences()
        }).catch(() => {
            this.addLoading = false;
        })

    }
    delSubNotebook() {
        if(this.activeIndex) {
            const subEditor = (this.$refs[this.activeIndex] as Vue[])[0] as SubEditor;
            if(subEditor && subEditor.delSubNotebook) {
                subEditor.delSubNotebook()
                this.updateSequences();
            }
        }
    }
    updateSequences() {
        let seqMap: Dict<number> = {}
        _.forEach(this.subNbsArray, (snb, seq) => {
            if(snb.seq != seq) {
                snb.seq = seq;
                this.$store.dispatch('updateSubNotebook', snb);
                seqMap[snb.notebookId] = seq
            }
        })
        if(!_.isEmpty(seqMap)) {
            this.notebookRemoteService.updateNotebooksSeq(seqMap);
        }

    }
    /** @deprecated */
    runCell(index:string){

        const notebook = (this.subNbs as Dict<INotebook>)[index]
        const code = notebook.code;
        const language = notebook.preference && notebook.preference["subNotebook.lang"] ? notebook.preference["subNotebook.lang"] || "SPARK SQL" : "SPARK SQL"
        const interpreter = langMap[language as 'SPARK SQL' | 'R' | 'Python'].interpreter
        wsclient.jobSubmitInMulti(notebook.notebookId, code, interpreter);
    }
    runAll(){
        let jobs: SubNoteJob[] = _.chain(this.subNbsArray).map( snb => {
            const language = snb.preference && snb.preference["subNotebook.lang"] ? snb.preference["subNotebook.lang"] || "SPARK SQL" : "SPARK SQL"
            const interpreter = langMap[language].interpreter
            const codeType = langMap[language].codeType
            const codeEmpty = _.isEmpty(snb.code.trim());
            if(codeEmpty) return undefined
            return {
                notebookId: snb.notebookId,
                code: snb.code,
                interpreter,
                codeType
            }
        }).compact().value()
        if(this.jobManager)
            this.jobManager.appendJobs(jobs)

    }

    get jobIds() {
        let jobIds: string[] = [];
        _.forEach(this.subNbs, snb => {
            jobIds = jobIds.concat(snb.jobs)
        })
        return jobIds;
    }
    get jobsStatus() {
        const allJobs = this.$store.state.workspace.jobs;
        let jobStatusDict:Dict<{status: JobStatus}> = {};
        _.chain(allJobs).pick(this.jobIds).forEach((job,jId) => {
            if(job.status != JobStatus.SUCCESS && job.status != JobStatus.ERROR){
                jobStatusDict[jId] = _.pick(job, 'status')
            }
        }).value()
        return jobStatusDict
    }

    @Watch('jobsStatus',{ deep : true})
    onJobStatusChange(jobs: Dict<{ status: JobStatus}>, oldJobs: Dict<{status: JobStatus}>) {
        let changeFlag = false;
        _.forEach(oldJobs, (job, jId) => {
            let status = job.status;
            if(!jobs[jId] || jobs[jId].status != status) {
                console.debug('onJobStatusChange', `jobId: ${jId} status from ${status} => ${jobs[jId] ? jobs[jId].status : 'success/error'}`)
                changeFlag = true;
            }
        })
        if(!changeFlag) return
        if(this.jobManager)
            this.jobManager.onJobDone()
    }
    @Watch('notebook.status')
    onNotebookStatusChange(status: NotebookStatus, oldVal: NotebookStatus) {
        if(oldVal != status && oldVal != NotebookStatus.INITIAL) {
            if(status == NotebookStatus.OFFLINE) {
                this.$store.dispatch('haltSubNotebookJobsByNId', {nid: this.notebook.notebookId})
                this.jobManager.haltAll()
            }
        }
    }
    onDragChange({ moved }: { moved: any}) {
        if(moved) {
            const snb = moved.element as Notebook;
            const newSeq = moved.newIndex;
            const dragDown = (moved.newIndex - moved.oldIndex) > 0
            snb.seq = newSeq + (dragDown ? .5: -0.5);
            this.updateSequences()
        }

    }
    // async onClose() {
    //     const nId = this.notebook.notebookId;
    //     try {
    //         if (this.notebook.status === NotebookStatus.RUNNING) {
    //             await this.$confirm(
    //                 'This notebook is running. Are you sure to close it?',
    //                 'Close',
    //                 {
    //                 confirmButtonText: 'Close anyway',
    //                 cancelButtonText: 'Cancel',
    //                 type: 'warning'
    //                 }
    //             );
    //         }
    //         // reset notebook connection status

    //         this.$store.dispatch('setNotebookStatus', {
    //             nid: this.notebook.notebookId,
    //             status: NotebookStatus.IDLE
    //         });
    //         this.$store.dispatch('setNotebookStatus', {
    //             nid: this.notebook.notebookId,
    //             status: NotebookStatus.DISCONNECTING
    //         });
    //         // close livy session
    //         wsclient.notebookDisconnect(nId);
    //         this.notebookRemoteService.closeNotebook(nId);
    //     }
    //     catch(e) {}
    // }
}


</script>
<style lang="scss" scoped>
@import '@/styles/global.scss';
$control-height: 40px;
$control-font-color: $zeta-font-color;
.multi-language-notebook{
    display: flex;
    flex-direction: column;
}
.control {
    box-sizing: border-box;
    position: relative;
    span {
        line-height: 20px;
    }
    .fluid-row {
        display: flex;
        padding: 5px 15px 0 15px;
        height: $control-height;
        align-items: center;
        &.tint {
            background-color: $zeta-label-lvl1;
            border-radius: 3px;
            border: 1px solid #ddd;
        }
        .left {
            .sv-btn{
                margin-left: 5px;
                margin-right: 5px;
                color: #FFF;
                // border-radius: 5px;
                &[disabled]{
                    border:1px solid #DDD;
                    background-color: #DDD;
                    cursor: not-allowed;
                }
            }
			.run-button {
				margin-right: 10px;
				&.el-button {
					padding: 3px 6px;
				}
				&.disabled {
					/deep/ .el-button {
						border-color: #ebeef5;
						background-color: #CACBCF;
						cursor: not-allowed;
					}
				}
				i.zeta-icon-run {
					font-size: 14px;
				}
				/deep/ .el-button {
					padding: 3px 6px;
				}
				/deep/ .el-dropdown__caret-button {
					padding: 3px 2px;
				}
			}
			.stop-button {
				padding: 3px 6px;
				i.zeta-icon-stop {
					font-size: 14px;
				}
			}
        }
        .right {
            margin-left: auto;
			display: flex;
			.tool-right {
				margin-left: 10px;
			}
        }
    }

    .fence {
        border-right: 1px solid $zeta-global-color-disable;
        margin: 0 10px;
    }

    .inline {
        display: inline-flex;
        padding: 0 8px;
    }
    button{
        display: inline-flex;
        padding: 0 3px;
        cursor: pointer;
        outline: none;

        /deep/ img {
            height: 1.5em;
            position: relative;
            // top: em;
        }
        &.light {
            font-size: 0.9em;
            color: #8b8b8b;

            img {
                height: 1.5em;
                top: 0;
                left: -2px;
            }
        }

        &.disable {
            color: #8b8b8b;
            cursor: not-allowed;
            img {
                filter: grayscale(100%);
            }
        }
    }

    .conn-status {
        display: inline-flex;
        font-size: 0.9em;
    }

    .left {
        display: flex;
    }
    .el-button + .el-button {
        margin-left: 0;
    }
}
.editor-container{
    flex-grow:1;
    overflow-y: auto;
    .sub-editor{
        &:not(:nth-child(-1)){
            margin-bottom: 15px;
        }
        &.active {
            /deep/ .sub-editor-item {
                border-color: #569ce1;
            }
        }
    }
}
.edit-button, /deep/ .edit-button {
        color: $zeta-font-color;
    }
</style>
