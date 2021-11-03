import CreateFileDialog from './create-file-dialog.vue';
import { NotebookType } from "@/types/workspace";
import { IFile } from "@/types/repository";
import { removeComponent, mountComponent } from "../utils";
import { ZetaException } from "@/types/exception";
import RepoApiHelper from "@/services/repo-api";
import Vue, { PluginObject } from "vue";

export interface ICreateOption {
    path?: string
    seq?: number
}
export interface InterpreterOptions {
    name: string,
    val: string,
    clusters: Dict<string>
}
export type FileReaderCallback = ((this: FileReader, ev: ProgressEvent) => any) | null
export interface NotebookUploadFormBase {
    folder: string;
    interpreter: string;
    nbType: NotebookType;
}
export interface ISingleNotebookForm extends NotebookUploadFormBase {
    name: string;
    seq: number
}

const CreateFilePlugin: PluginObject<any> =  {
    install(Vue) {
        Vue.prototype.$createFile = function(options?: ICreateOption){
            console.log('RenameFilePlugin install')
            const repoApi = new RepoApiHelper(this.notebookRemoteService, this.zeppelinApi);
            const $instance = mountComponent(CreateFileDialog, repoApi, { form: options})

            let $resolve: Function, $reject: Function;
            let promise = new Promise((resolve, reject) => {
                $resolve = resolve;
                $reject = reject;
            })

            $instance.$on('onCancel', () => {
                removeComponent($instance);
                $reject();
            })
            $instance.$on('onError', (e: ZetaException) => {
                removeComponent($instance);
                $reject(e);
            })
            $instance.$on('onSuccess', (file: IFile) => {
                removeComponent($instance);
                $resolve(file);
            })

            return promise;
        }
    }
}
declare module 'vue/types/vue' {
    interface Vue {
        $createFile: (option: ICreateOption) => Promise<IFile>
    }
}
export default CreateFilePlugin;