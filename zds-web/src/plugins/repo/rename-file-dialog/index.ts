import RenameFileDialog from './rename-file-dialog.vue';
import Vue, { PluginObject} from 'vue'
import { IFile } from '@/types/repository';
import { ZetaException } from '@/types/exception';
import RepoApiHelper from '@/services/repo-api';
import { removeComponent, mountComponent } from '../utils';
export interface IRenameOption {
    id: string,
    name: string,
    path: string
}
const RenameFilePlugin: PluginObject<any> =  {
    install(Vue) {
        Vue.prototype.$renameFile = function(file: IFile){
            console.log('RenameFilePlugin install')
            const repoApi = new RepoApiHelper(this.notebookRemoteService, this.zeppelinApi);
            const form = {id: file.notebookId, name: file.title, path: file.path}
            const $instance = mountComponent(RenameFileDialog, repoApi, { file, form })

            let $resolve: Function, $reject: Function;
            let promise = new Promise((resolve, reject) => {
                $resolve = resolve;
                $reject = reject;
            })

            $instance.$on('onCancel', () => {
                removeComponent($instance);
                const e = <ZetaException> {
                    message: 'Canceled by user'
                }
                $reject(e);
            })
            $instance.$on('onError', (e: ZetaException) => {
                // removeComponent($instance);
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
        $renameFile: (option: IFile) => Promise<IFile>
    }
}
export default RenameFilePlugin;