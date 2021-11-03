import BaseRemoteService from '@/services/remote/BaseRemoteService';
import { ZetaException, ZetaExceptionProps, ZETA_EXCEPTION_TAG } from "@/types/exception";
import _ from 'lodash';
import * as Axios from 'axios';
import Util from "../Util.service";
export default class JIRARemoteService extends BaseRemoteService {
    defaultErrorHandler(error : any, url: string, props: ZetaExceptionProps = { path : 'unknown'}): ZetaException {
        console.error("REST ERR: ", error)
        // TODO LOG
        // window.$logger.error(["REST ERROR", url], error && error.response ? error.response.data : error)
        const data: any = {
            code: "",
            errorDetail: {
                message: error.response.data,
                cause: {
                    cause: error.stack,
                    errorCode: "",
                    message: "",
                    stackTrace: ""
                },
                rule: ""
            }
        }
        let exception = new ZetaException(data, props).tags([ZETA_EXCEPTION_TAG.REST_ERR])
        Util.getApp().$store.dispatch('addException', {exception})
        return exception;
    }

    get(url: string, config?: Axios.AxiosRequestConfig | undefined, canceler?: Axios.Canceler) {
        return super.get(url, config, canceler, true);
    }
    post(url: string, data?: any, config?: Axios.AxiosRequestConfig | undefined, canceler?: Axios.Canceler) {
        return super.post(url,data,config,canceler, true);
    }
    getReleaseCheckStatus = (nt: string, releaseTag: string, issueName: string) => {
        return this.get(process.env.VUE_APP_DSSRM_URL + `jira/checkStatus?nt=${nt}&releaseTag=${releaseTag}&issueName=${issueName}`);
    }
    
    submitManifest = (params: any) => {
        return this.post(process.env.VUE_APP_DSSRM_URL + `api/release/manifest`, params);
    }

    checkManifest = (tag: string) => {
        return this.get(process.env.VUE_APP_DSSRM_URL + `release/releaseRecord/${tag}`);
    }

    getAbinitioManifest = (tag: string) => {
        return this.get(process.env.VUE_APP_DSSRM_URL + `release/abinitioManifest/${tag}`);
    }

    sendManifestValidate = (params: any) => {
        return this.post(process.env.VUE_APP_DSSRM_URL + `api/release/sendValidateMail`, params);
    }

    rollout = (params: any) => {
        return this.post(process.env.VUE_APP_DSSRM_URL + `api/release/rollout`, params);
    }

    editResolution = (tag: string) => {
        return this.get(process.env.VUE_APP_DSSRM_URL + `jira/editResolution/${tag}`);
    }
}

