import { ConnectionAdaptee } from './connection-adaptee';
import { INotebook, IConnection, Cluster, IPreference, CodeTypes, CodeType, BDP_STATUS, NotebookStatus } from '@/types/workspace';
import Vue from 'vue';
import _ from 'lodash';
import { wsclient } from '@/net/ws';
import Util from '@/services/Util.service';
import { ZetaException } from '@/types/exception';
import { isHermes, getHermesCnnConfig } from '@/services/connection.service';
const SPARK_CONNECTION = Util.getDefaultConnectionByPlat(CodeType.SQL);
export class SparkConnection extends ConnectionAdaptee{
  // readonly HERMES_CLUSTER_ID = 16;
  // readonly HERMES_LVS_CLUSTER_ID = 21;
  $app: Vue;
  constructor (content: Vue, lastCnn?: IConnection){
    super(SPARK_CONNECTION.defaultConnection, lastCnn);
    this.$app = content;
  }
  getClusters (){
    return this.$app.$store.getters.user.clusterOption;
  }

  getAccounts (alias: string){

    const cluster =  this.$app.$store.getters.user.clusterOption[alias] as Cluster;
    if (cluster && isHermes(cluster.clusterId) && cluster.access) {
      return [Util.getNt()];
    }
    if (!cluster || !cluster.batchAccountOptions) return [];
    return cluster.batchAccountOptions;
  }
  /**
     * @override getDefaultConnection
     * when Cluster == `Hermes` set default connection's account to NT_LOGIN
     */
  getDefaultConnection () {
    const defaultCnn = super.getDefaultConnection();
    if (defaultCnn.clusterId && isHermes(defaultCnn.clusterId)) {
      defaultCnn.batchAccount = Util.getNt();
      return defaultCnn;
    } else {
      return defaultCnn;
    }
  }
  hermesErrorHandler = (ex: ZetaException, notebookId: string, cnn: IConnection, preference: IPreference, resolve: any, reject: any, pupop=true) => {
    const statusCode = ex.context.zetaStatus.statusCode;
    const notebook: INotebook = this.$app.$store.state.workspace.workspaces[notebookId];
    if (statusCode && statusCode == '9003'){
      const url = 'https://wiki.vip.corp.ebay.com/x/4J6sJ';
      // const template = `\`${notebook.name}\` Connect failed. ` +
      const template = 'Connect failed. ' +
            `Windows PET password expired. Click <a href=${url} target="_blank">here</a> to renew password`;
      ex.template(template);
    } else if (statusCode && statusCode == '9002') {
      const url = 'https://wiki.vip.corp.ebay.com/x/4J6sJ';
      // ex.template(`\`${notebook.name}\` Connect failed. ` +
      ex.template('Connect failed. ' +
            `Windows PET account locked. Click <a href=${url} target="_blank">here</a> to unlock.`);
    } else if (statusCode && statusCode == '9004') {
      const url = 'https://wiki.vip.corp.ebay.com/x/4J6sJ';
      // ex.template(`\`${notebook.name}\` Connect failed. ` +
      ex.template('Connect failed. ' +
            `Windows PET account is not activated. Click <a href=${url} target="_blank">here</a> to unlock.`);
    } else if (statusCode && statusCode == '9001'){
      // pass error
      // // let url = `${location.protocol}//${location.host}/${process.env.NODE_ENV === 'production' ? 'zeta/' : ''}#/settings`;
      // // ex.template(`\`${notebook.name}\` Connect failed. ` +
      // // `Windows PET password incorrect, please <a href=${url}>change your password</a>`)
      ex.resolve();
      if (pupop){
        this.promptPopup(notebookId, cnn, preference, resolve, reject, true);
      }
    } else {
      // ex.template(`\`${notebook.name}\` Connect failed.`);
      ex.template('Connect failed.');
      ex.causeMessage = 'Unknown Error';
    }
    return ex;
  };
  connect (notebookId: string, cnn: IConnection, preference: IPreference){
    return new Promise((resolve, reject) => {

      const clusterId = cnn.clusterId;
      /** hermes use jdbc connection */
      if (clusterId && isHermes(clusterId)) {
        this.connectToHermes(notebookId, cnn, preference).then(resolve).catch(e => reject(e));
        // if(this.$app.$store.getters.hasWinPass) {
        //   this.connectToHermes(notebookId, cnn, preference).catch((ex)=>{
        //     return this.hermesErrorHandler(ex, notebookId, cnn, preference, resolve, reject, true);
        //   }).then(()=>resolve());
        // } else {
        //   this.promptPopup(notebookId, cnn, preference, resolve, reject);
        // }
      } else {
        wsclient.notebookConnect(notebookId, cnn.clusterId, cnn.source, cnn.batchAccount, CodeTypes[CodeType.SQL].interpreter, preference).then(resolve).catch(e => reject(e));
      }

    });

  }
  private connectToHermes (notebookId: string, cnn: IConnection, preference: IPreference){
    // const host = 'hermes.prod.vip.ebay.com';
    // const interpreter = 'carmel';
    // const jdbcType = 'carmel';
    // const pricipal = 'b_carmel/hermes.prod.vip.ebay.com@PROD.EBAY.COM';
    const { host, interpreter, jdbcType, pricipal } = getHermesCnnConfig(cnn.clusterId);
    return wsclient.jdbcNotebookConnect(notebookId, host, interpreter, jdbcType, preference, Util.getNt(), '', 'access_views', undefined, 10000, pricipal, { clusterId: cnn.clusterId });
  }
  disconnect (notebookId: string) {
    return wsclient.notebookDisconnect(notebookId);
  }
  errorMessage (){
    const status: BDP_STATUS =  this.$app.$store.getters.user.bdpStatus;
    if (status == BDP_STATUS.failed) {
      return this.$app.$store.getters.user.bdpMsg;
    }
    else if (status == BDP_STATUS.error){
      return 'Fetch DBP service failed';
    }
    else {
      return '';
    }
  }
  promptPopup (notebookId: string, cnn: IConnection, preference: IPreference, resolve: any, reject: any, errorPass?: boolean){
    this.$app.$prompt(`
            <div style="display: flex; justify-content: space-between;">
                <span>Windows PET Password </span>
                <a href="https://wiki.vip.corp.ebay.com/display/GDI/Apply+Prod+AD+account+in+PET" target="_blank"> How to?</a>
            </div>
            `, 'Set Password', {
      dangerouslyUseHTMLString: true,
      inputValidator:(val) => {
        if (_.isEmpty(val)){
          return 'cannot be empty';
        }
        if (val === '***'){
          return 'Your PET Production NT password is not correct';
        }
        return true;
      },
      inputValue: errorPass?'***':undefined,
      inputType: 'password',
      beforeClose: (action, instance, done) => {
        if (action === 'confirm') {
          instance.confirmButtonLoading = true;
          const value = instance.inputValue;
          const res =  this.$app.$store.dispatch('setWinPass', {winPass:value});
          res.then(()=>{
            this.$app.$store.dispatch('setNotebookStatus', {
              nid: notebookId,
              status: NotebookStatus.CONNECTING,
            });
            this.connectToHermes(notebookId, cnn, preference)
              .then(() => {
                instance.confirmButtonLoading = false;
                resolve();
                done();
              })
              .catch((ex) => {
                const result = this.hermesErrorHandler(ex, notebookId, cnn, preference, resolve, reject, false);
                const statusCode = result.context.zetaStatus.statusCode;
                const status = result.context.zetaStatus.status;
                instance.confirmButtonLoading = false;
                instance.inputValue = '';
                if (statusCode === '9001'){
                  instance.inputValidator = (val) => {
                    if (_.isEmpty(val)) {
                      return status;
                    }
                    return true;
                  };
                } else {
                  done();
                }
              });
          });
        } else {
          done();
        }
      },
    }).catch((e) => {
      reject(e);
    });
  }
}
export type SparkConnectionConstructor = typeof SparkConnection;
