import { ConnectionAdaptee } from './connection-adaptee';
import { IConnection, Cluster, IPreference, CodeTypes, CodeType, BDP_STATUS } from '@/types/workspace';
import config from '@/config/config';
import Vue from 'vue';
import _ from 'lodash';
import { wsclient } from '@/net/ws';
import Util from '@/services/Util.service';
const PYSPARK_CONNECTION = Util.getDefaultConnectionByPlat(CodeType.SPARK_PYTHON)
const ENABLE_CLUSTER_IDS = [14]
export class PySparkConnection extends ConnectionAdaptee{
    $app: Vue
    constructor(content: Vue,lastCnn?: IConnection){
        super(PYSPARK_CONNECTION.defaultConnection,lastCnn)
        this.$app = content;
    }
    getClusters(){
        return _.pickBy(this.$app.$store.getters.user.clusterOption, option => _.includes(ENABLE_CLUSTER_IDS, option.clusterId))
    }

    getAccounts(alias: string){
        const cluster =  this.$app.$store.getters.user.clusterOption[alias] as Cluster
        if(!cluster || !cluster.batchAccountOptions) return []
        return cluster.batchAccountOptions
    }
    connect(notebookId: string, cnn: IConnection, preference: IPreference){
        return new Promise((resolve, reject) => {
            /** hard code notebook profile */
            preference["notebook.profile"] = '93b9ed8a-2af2-4537-aeed-67c011f1facc'
            /** end */

            wsclient.multiNotebookConnect(notebookId, cnn.clusterId, cnn.source, cnn.batchAccount, CodeTypes[CodeType.SPARK_PYTHON].interpreter, preference)
            resolve()
        })
    }
    disconnect(notebookId: string) {
        return wsclient.notebookDisconnect(notebookId)
    }
    errorMessage(){
        const status:BDP_STATUS =  this.$app.$store.getters.user.bdpStatus;
        if(status == BDP_STATUS.failed) {
            return this.$app.$store.getters.user.bdpMsg;
        }
        else if (status == BDP_STATUS.error){
            return 'Fetch DBP service failed'
        }
        else {
            return ''
        }
    }
}
export type PySparkConnectionConstructor = typeof PySparkConnection;
