import { NotebookStatus, MultiNotebook } from "@/types/workspace";
import _ from 'lodash';
import { wsclient } from '@/net/ws';
export interface SubNoteJob {
    notebookId: string
    code: string
    interpreter: string
    codeType: 'sql' | 'pyspark' | 'sparkr'
}

export class JobManager {
    multiNotebook: MultiNotebook
    jobs: SubNoteJob[]
    runningIndex: number = -1;
    waitingJobs: SubNoteJob[]
    callback: undefined | (() => void) = undefined
    constructor(cb?: () => void) {

        this.jobs = [];
        this.waitingJobs = [];
        this.callback = cb;
    }
    setNotebook(nb: MultiNotebook) {
        if(_.isEmpty(this.multiNotebook)) {
            this.multiNotebook = nb;
        } else {
            Object.assign(this.multiNotebook, nb)
        }
    }
    get hasNext() {
        return !_.isEmpty(this.waitingJobs)
    }
    get isRunning() {
        return this.runningIndex >= 0
    }
    public appendJobs(js: SubNoteJob[]) {
        if(js.length < 1) {
            console.warn('====JobManager====', 'cannot appendJobs jobs.length < 1');
            return
        }
        const jobsExist = this.isRunning             // current running
            || !_.isEmpty(this.waitingJobs)          // waiting to execute
        if(jobsExist) {
            // this.waitingJobs.concat(js)
            this.concatSubNoteJob(js)
        } else {
            this.submitJobs(js)
        }
    }
    private concatSubNoteJob(target: SubNoteJob[]) {
        _.forEach(target, job => {
            let planedJob = _.find(this.waitingJobs, subJob => subJob.notebookId == job.notebookId);
            if(planedJob) {
                planedJob.code = job.code;
                planedJob.interpreter = job.interpreter;
                planedJob.codeType = job.codeType;
            } else {
                this.waitingJobs.push(job);
                this.jobs.push(job)
            }
        })
    }
    private submitJobs(js: SubNoteJob[]) {
        if(this.isRunning) {
            console.warn('====JobManager====', 'cannot submit jobs because current jobs still running');
            return
        }

        this.jobs = js;
        this.waitingJobs = js.slice()
        this.submit()
    }
    public onJobDone() {
        const status = this.multiNotebook.status
        if(status !== NotebookStatus.IDLE) {
            console.warn('Multi notebook\'s status wrong, status: ', status)
            return
        }
        if(!this.hasNext) {
            this.runningIndex = -1;
            if(this.callback) this.callback()
        } else {
            const next = this.runningIndex + 1;
            this.submit(next)
        }
    }
    public haltAll() {
        this.clearJobs();
    }
    private submit(index: number = 0) {
        if(!this.jobs[index]) {
            console.warn('====JobManager====', `cannot find job by index: \`${index}\``);
        }
        this.runningIndex = index;
        this.waitingJobs = this.jobs.slice(index + 1)
        // submit in websocket
        const job = this.jobs[this.runningIndex];
        wsclient.jobSubmitInMulti(job.notebookId, job.code, job.interpreter,job.codeType)
    }
    private clearJobs() {
        this.jobs.splice(0,this.jobs.length);
        this.waitingJobs.splice(0,this.waitingJobs.length);
        this.runningIndex = -1;
    }
}
