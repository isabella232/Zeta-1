import Vue from "vue";
import RepoApiHelper from "@/services/repo-api";
import { Component, Emit } from "vue-property-decorator";
import { ZetaException } from "@/types/exception";
import { IFile } from "@/types/repository";

@Component
export default class RepoPluginBase extends Vue {
    /** will be inject by plugin */
    repoApi: RepoApiHelper;

    @Emit('onSuccess')
    onSuccess(file?: IFile) {
        
    }
    @Emit('onCancel')
    onCancel(){

    }
    @Emit('onError')
    onError(ex: ZetaException){

    }
}