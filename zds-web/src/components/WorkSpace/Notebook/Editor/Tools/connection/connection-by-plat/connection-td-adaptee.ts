import { ConnectionAdaptee } from './connection-adaptee';
import { IConnection, Cluster, IPreference, CodeTypes, CodeType } from '@/types/workspace';
import Vue from 'vue';
import _ from 'lodash';
import { wsclient } from '@/net/ws';
import Util from '@/services/Util.service';
const TD_CONNECTION = Util.getDefaultConnectionByPlat(CodeType.TERADATA);
// config.zeta.notebook.tdConnection.clustersMap
export class TdConnection extends ConnectionAdaptee{
  $app: Vue;
  constructor(content: Vue, lastCnn?: IConnection){
    super(TD_CONNECTION.defaultConnection,lastCnn);
    this.$app = content;
  }
  getClusters(){
    const map: Dict<Cluster> = {};
    _.forEach(TD_CONNECTION.clustersMap,(cluster: string, alias: string) => {
      map[cluster] =  {
        access: true,
        clusterId: -1,
        clusterName: cluster,
        clusterAlias: alias,
        batchAccountOptions: [Util.getNt()]
      } as Cluster;
    });
    return map;
  }
  getDefaultConnection(): IConnection {
    const cnn = super.getDefaultConnection();
    if (cnn.alias === 'NuMozart') {
      cnn.alias = 'Mozart';
    }
    return cnn;
  }
  connect(notebookId: string, cnn: IConnection, preference: IPreference){
    return new Promise((resolve, reject) => {
      const host = TD_CONNECTION.hostMap[cnn.alias];
      const codeType = CodeTypes[CodeType.TERADATA];
      wsclient.jdbcNotebookConnect(notebookId, host, codeType.interpreter, codeType.jdbcType!, preference, undefined,undefined, undefined, undefined, undefined, undefined, {'jdbc.url.params.CHARSET': 'UTF8'} ).then(resolve).catch(e => reject(e));
      // resolve()
    });

  }
  disconnect(notebookId: string) {
    return wsclient.notebookDisconnect(notebookId);
  }
}
export type TdConnectionConstructor = typeof TdConnection;
