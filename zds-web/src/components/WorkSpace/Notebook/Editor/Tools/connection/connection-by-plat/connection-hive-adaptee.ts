import { ConnectionAdaptee } from './connection-adaptee';
import { IConnection, Cluster, IPreference, CodeTypes, CodeType, BDP_STATUS } from '@/types/workspace';
import config from '@/config/config';
import Vue from 'vue';
import _ from 'lodash';
import { wsclient } from '@/net/ws';
import Util from '@/services/Util.service';
const HIVE_CONNECTION = Util.getDefaultConnectionByPlat(CodeType.HIVE)
export class HiveConnection extends ConnectionAdaptee{
    $app: Vue
    constructor(content: Vue,lastCnn?: IConnection){
        super(HIVE_CONNECTION.defaultConnection,lastCnn)
        this.$app = content;
    }
    getClusters(){
		let clusters: Dict<Cluster> = {};
		for (let clusterKey in HIVE_CONNECTION.clustersMap) {
            let clusterValue = HIVE_CONNECTION.clustersMap[clusterKey];
            let clusterObject = this.$app.$store.getters.user.clusterOption[clusterValue];
            if(!_.isEmpty(clusterObject)) {
                clusters[clusterValue] = this.$app.$store.getters.user.clusterOption[clusterValue];
            }
        }
		return clusters;
    }
    connect(notebookId: string, cnn: IConnection, preference: IPreference){
        return new Promise((resolve, reject) => {
            const host = HIVE_CONNECTION.hostMap[cnn.alias];
            const hiveCodeInfo = CodeTypes[CodeType.HIVE];
            const pricipal = HIVE_CONNECTION.principalMap[cnn.alias];
            console.log(pricipal)
            wsclient.jdbcNotebookConnect(notebookId, host, hiveCodeInfo.interpreter, hiveCodeInfo.jdbcType!, preference, Util.getNt(), '', 'default', undefined, 10000, pricipal);
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
export type HiveConnectionConstructor = typeof HiveConnection;
