import { IConnection, Cluster, IPreference } from "@/types/workspace";
import Vue from "vue";
import Util from "@/services/Util.service";
import _ from 'lodash';
export abstract class ConnectionAdaptee {
    /** default connection by platform */
    defaultConnection: IConnection 
    /** last connection preference */
    lastConnection: IConnection 

    constructor( defaultCnn?:IConnection, lastCnn?: IConnection) {
        this.defaultConnection = !defaultCnn ? <IConnection> {} : defaultCnn;
        this.lastConnection = !lastCnn ? <IConnection> {} : lastCnn;;
    }

    /**
     * merge preset connection & last connection info
     */
    getDefaultConnection(): IConnection {
        if(_.isEmpty(this.defaultConnection) && _.isEmpty(this.lastConnection)){
            return  <IConnection> {};
        }
        else if(_.isEmpty(this.defaultConnection)){
            return this.lastConnection
        }
        else if(_.isEmpty(this.lastConnection)) {
            return this.defaultConnection
        }
        else {
            // merge default & last connection
            if(this.defaultConnection.codeType === this.lastConnection.codeType) {
                Object.assign(this.defaultConnection, this.lastConnection)
            }
            return this.defaultConnection
        }
    }

    /**
     * get cluster list
     */
    getClusters(): Dict<Cluster>{
        return {}
    }

    /**
     * connection's account
     * @param alias cluster's alias
     */
    getAccounts(alias: string): string[]{
        return [Util.getNt()]
    }

    /**
     * 
     * @param notebookId 
     * @param connection 
     * @param preference 
     */
    protected connect(notebookId: string, connection: IConnection, preference: IPreference) : Promise<any>{
        return new Promise((resolve, reject) => {
            resolve()
        })
    }
    connectAsync(notebookId: string, connection: IConnection, preference: IPreference) : Promise<any>{
        return this.connect(notebookId, connection, preference).then(this.connectSuccess).catch(this.connectFailed)
    }
    /**
     * callback when connection failed
     * @param msg 
     */
    connectFailed(msg: any) {
    }

    /**
     * callback when connection success
     * @param msg 
     */
    connectSuccess(msg: any) {
    }
    /**
     * 
     * @param nId 
     */
    protected disconnect(nId: string){
        return new Promise(() => {})
    }
    disconnectAsync(nId: string) {
        return this.disconnect(nId).then(this.disconnectSuccess).catch(this.disconnectFailed)
    }
    /**
     * callback when connection failed
     * @param msg 
     */
    disconnectFailed(msg: any) {
    }

    /**
     * callback when connection success
     * @param msg 
     */
    disconnectSuccess(msg: any) {
    }
    loading(){
        return false;  
    }

    /**
     * error message about loading connection info
     */
    errorMessage(){
        return '';
    }
    /**
     * 
     * @param currentCnn 
     */
    toolTipsTemplate(currentCnn?: IConnection): string {
        return ''
    }


}