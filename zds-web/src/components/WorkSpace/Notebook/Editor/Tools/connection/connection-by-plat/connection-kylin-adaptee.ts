import { ConnectionAdaptee } from './connection-adaptee';
import { IConnection, Cluster, IPreference, CodeTypes, CodeType, INotebook } from '@/types/workspace';
import config from '@/config/config';
import Vue from 'vue';
import _ from 'lodash';
import { wsclient } from '@/net/ws';
import Util from '@/services/Util.service';
const KYLIN_CONNECTION = Util.getDefaultConnectionByPlat(CodeType.KYLIN);

const zetaBatchAccount: string = 'b_zeta_devsuite'

export class KylinConnection extends ConnectionAdaptee {
    $app: Vue
    constructor(content: Vue, lastCnn?: IConnection) {
        super(KYLIN_CONNECTION.defaultConnection, lastCnn)
        this.$app = content;
    }
    getClusters() {
        let map: Dict<Cluster> = {}
        _.forEach(KYLIN_CONNECTION.clustersMap, (cluster: string, alias: string) => {
            map[cluster] = <Cluster>{
                access: true,
                clusterId: -1,
                clusterName: cluster,
                clusterAlias: alias,
                batchAccountOptions: [Util.getNt()]
            }
        })
        return map
    }

    getAccounts(alias: string) {
        return [Util.getNt()]
    }
    connect(notebookId: string, cnn: IConnection, preference: IPreference) {
        return new Promise((resolve, reject) => {
            const host = KYLIN_CONNECTION.hostMap[cnn.alias];
            const codeType = CodeTypes[CodeType.KYLIN]
            const ssl = /rno/.test(cnn.alias) || false;
            wsclient.jdbcNotebookConnect(notebookId, host, codeType.interpreter, codeType.jdbcType!, preference, cnn.user, cnn.password, cnn.database, ssl).then(resolve).catch(e => reject(e));
        })
    }
    disconnect(notebookId: string) {
        return wsclient.notebookDisconnect(notebookId)
    }
}
export type KylinConnectionConstructor = typeof KylinConnection;
