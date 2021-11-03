<template>
    <div class="ntbk-cnn">
        <div class="ntbk-cnn-codeType">
            <span>{{currentCodeTypeInfo.name}}</span>
        </div>
        <el-tooltip v-if="errorMessage" placement="top" effect="light">
            <div slot="content">{{errorMessage}}</div>
            <i class="el-icon-warning"></i>
        </el-tooltip>
        <div class="ntbk-cnn-clstr">
            <el-dropdown placement="bottom" trigger="click" @command="handleClusterDropdownCommand">
                <el-button
                    v-if="clusterOptions && _.keys(clusterOptions) && _.keys(clusterOptions).length > 0"
                    type="text"
                    class="el-dropdown-link acct-dropdown"
                    v-bind:class="{'enable':connectEnable && modifyEnable}"
                    :disabled="!modifyEnable"
                >
                    <span>{{currentConnection.alias || "Cluster"}}</span>
                    <i class="el-icon-arrow-down el-icon--right"></i>
                </el-button>
                <el-button
                    v-else
                    type="text"
                    class="el-dropdown-link acct-dropdown"
                    :disabled="true"
                >
                    <span>{{currentConnection.alias || "Cluster"}}</span>
                    <i class="el-icon-arrow-down el-icon--right"></i>
                </el-button>
                <el-dropdown-menu slot="dropdown" class="batch-list">
                    <el-dropdown-item
                        :class="{'connect-disabled':!cluster.access}"
                        :key="clusterAlias"
                        v-for="(cluster,clusterAlias) in clusterOptions"
                        :command="clusterAlias"
                    >
                        {{clusterAlias}}
                        <el-tooltip
                            v-if="!cluster.access"
                            content="Bottom center"
                            placement="bottom"
                            effect="light"
                        >
                            <div slot="content">
                                please follow
                                <a
                                    target="_blank"
                                    href="https://wiki.vip.corp.ebay.com/x/l00VIg"
                                >wiki</a>
                                to access {{clusterAlias}}
                            </div>
                            <i class="el-icon-question"></i>
                        </el-tooltip>
                    </el-dropdown-item>
                </el-dropdown-menu>
            </el-dropdown>
            <el-tooltip
                v-if="!accessable(currentConnection) && !errorMessage"
                content="Bottom center"
                placement="bottom"
                effect="light"
            >
                <div slot="content">
                    <div v-if="isClusterExist">
                        please follow
                        <a
                            target="_blank"
                            href="https://wiki.vip.corp.ebay.com/x/l00VIg"
                        >wiki</a>
                        to access {{currentConnection && currentConnection.alias ? currentConnection.alias : ''}}
                    </div>
                    <div v-else>
                        {{currentConnection && currentConnection.alias ? currentConnection.alias : ''}} already deprecated
                    </div>
                </div>
                <i class="el-icon-question"></i>
            </el-tooltip>
        </div>
        <div class="ntbk-cnn-acct">
            <el-dropdown
                trigger="click"
                placement="bottom"
                @command="handleDropdownCommand"
                v-if="accountEnable"
            >
                <el-button
                    type="text"
                    class="el-dropdown-link acct-dropdown"
                    :class="{'enable':connectEnable && modifyEnable}"
                    :disabled="!modifyEnable || !clusterConnectEnable"
                >
                    <span>{{currentConnection.batchAccount || nt}}</span>
                    <i class="el-icon-arrow-down el-icon--right"></i>
                </el-button>
                <el-dropdown-menu slot="dropdown" class="batch-list">
                    <el-dropdown-item
                        :key="$index"
                        v-for="(batchId,$index) in accountOptions"
                        :command="batchId"
                    >{{batchId}}</el-dropdown-item>
                </el-dropdown-menu>
            </el-dropdown>
            <el-button v-else type="text" class="el-dropdown-link acct-dropdown" :disabled="true">
                N/A
                <i class="el-icon-arrow-down el-icon--right"></i>
            </el-button>
        </div>
        <div class="ntbk-cnn-db" v-if="codeType === 'KYLIN'">
            <span>{{notebookStatus ==='ONLINE'?currentConnection.database:databaseName}}</span>
        </div>
        <div class="ntbk-cnn-actn">
            <el-button
                plain
                v-if="notebookStatus ==='OFFLINE'"
                @click="login"
                :disabled="!connectEnable || !clusterConnectEnable"

                v-click-metric:NB_TOOLBAR_CLICK="{name: 'connect'}"
            >Connect</el-button>
            <el-button
                plain
                :loading="true"
                v-else-if="notebookStatus ==='CONNECTING'"
                class="connecting"
            >Connecting</el-button>
            <el-button
                plain
                :loading="true"
                v-else-if="notebookStatus ==='DISCONNECTING'"
            >Disconnecting</el-button>
            <el-button plain v-if="notebookStatus ==='ONLINE'" @click="logoutPopup" v-click-metric:NB_TOOLBAR_CLICK="{name: 'disconnect'}">Disconnect</el-button>
            <el-button
                plain
                v-if="notebookStatus ==='CONNECTING'"
                class="cancel"
                @click="logout"
                v-click-metric:NB_TOOLBAR_CLICK="{name: 'connectCancel'}"
            >Cancel</el-button>
        </div>
        <DataBaseDialog v-if="codeType === 'KYLIN'" :visible.sync="databaseVisible" :connection="currentConnection" @loginKylin="loginKylin"/>
    </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Emit, Watch, Inject } from "vue-property-decorator";
import {
    INotebook,
    IConnection,
    NotebookStatus,
    CodeType,
    CodeTypeInfo,
    CodeTypes,
    IPreference
} from "@/types/workspace";
import Util from "@/services/Util.service";
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import _ from "lodash";
import { throws } from "assert";
import config from "@/config/config";
import { Cluster, ClusterDict } from "@/types/workspace";
import { IFile } from "@/types/repository";
import { BDP_STATUS } from "@/types/workspace";
import { wsclient } from '@/net/ws';
import { ConnectionAdaptee, getConnectionAdaptee } from "./connection-by-plat";
import DataBaseDialog from "./databaseDialog.vue";
const DEFAULT_CONNECTION = config.zeta.notebook.connection.defaultConnection;
function concat<T>(arr: T[], newArr: T[]) {
    _.forEach(newArr, item => {
        arr.push(item);
    });
    _.union(arr)
}
@Component({
    components: {
        DataBaseDialog
    }
})
export default class Connection extends Vue {
    @Prop() notebookId: string;
    @Inject()
    notebookRemoteService: NotebookRemoteService;
    @Inject() viewOnly: boolean;
    @Inject() runAccess: boolean;

    @Prop({ default: false, type: Boolean }) isPublic: boolean;
    @Prop({ default: false, type: Boolean }) isPublicOwner: boolean;
    @Prop() capacity: number;
    @Prop() codeType: CodeType;
    readonly nt: string = Util.getNt();
    currentConnection: IConnection = <IConnection>{};
    // adaptee: ConnectionAdaptee
    clusterOptions: Dict<Cluster> = {};
    accountOptions: string[] = [];
    errorMessage = "";
    private dbName: string="";
    private databaseVisible:Boolean = false;

    get _() {
      return _;
    }
    get notebook(): INotebook {
        return this.$store.state.workspace.workspaces[this.notebookId];
    }
    get notebookLoaded() {
        if (this.notebook) {
            return this.notebook.loaded;
        } else {
            return false;
        }
    }
    get modifyEnable(): Boolean {
        if (!this.notebook) {
            return false;
        }
        if (this.isPublic) {
            return false;
        }
        if (this.isPublicOwner){
            return false;
        }
        return Boolean(this.notebook.status === NotebookStatus.OFFLINE);
    }
    get connectEnable(): Boolean {
        if (!this.$data.currentConnection) return false;
        return Boolean(
            this.$data.currentConnection.source &&
                this.$data.currentConnection.clusterId &&
                this.$data.currentConnection.batchAccount
        );
    }

    get isClusterExist() {
        if (_.isEmpty(this.clusterOptions)) return false;
        if (!this.clusterOptions[this.$data.currentConnection.alias]) {
            return false;
        }
        return true;
    }
    get clusterConnectEnable() {
        if (!this.isClusterExist) {
            return false;
        }
        /**
         * add by tianrsun
         */
        if (this.isPublic) {
            return true
        } else {
            return this.clusterOptions[this.$data.currentConnection.alias].access;
        }
        /**
         * end
         */
    }
    get accountEnable() {
        if (this.isPublic) {
            return Boolean(this.currentConnection.batchAccount)
        }
        return Boolean(this.accountOptions && this.accountOptions.length && this.accountOptions.length > 0)
    }
    get notebookStatus() {
        if (!this.notebook) {
            return "OFFLINE";
        }
        if (this.notebook.status === NotebookStatus.OFFLINE) {
            return "OFFLINE";
        } else if (this.notebook.status === NotebookStatus.CONNECTING) {
            return "CONNECTING";
        } else if (this.notebook.status === NotebookStatus.DISCONNECTING) {
            return "DISCONNECTING";
        } else {
            return "ONLINE";
        }
    }
    get databaseName(){
        if(this.notebookStatus !== "OFFLINE"){
        return this.dbName;
        }
        return "";
    }
    constructor() {
        super();
    }
    /**
     * init func will be executed when notebook loaded
     */
    private init() {
        let CnnAdaptee = getConnectionAdaptee(this.codeType);
        const adaptee = (this.$data.adaptee = new CnnAdaptee(
            this,
            this.notebook.connection
        ));
        const currentConnection = adaptee.getDefaultConnection();
        this.currentConnection = Object.assign(
            {},
            this.currentConnection,
            currentConnection
        );
        this.clusterOptions = Object.assign(
            {},
            adaptee.getClusters()
        );
        const accounts = adaptee.getAccounts(currentConnection.alias);
        this.accountOptions = [];
        concat(this.accountOptions, accounts);
        // set default account to user's nt
        if (this.clusterConnectEnable && !this.currentConnection.batchAccount) {
            if (this.accountOptions.indexOf(Util.getNt()) >= 0) {
                this.currentConnection.batchAccount = Util.getNt();
            } else if (this.accountOptions.length > 0) {
                this.currentConnection.batchAccount = this.accountOptions[0];
            }
        }
        this.savePreference(this.notebookId, this.$data.currentConnection, false);
        // end
        this.errorMessage = adaptee.errorMessage();
    }
    handleDropdownCommand(command: string) {
        this.$data.currentConnection.batchAccount = command;
        this.savePreference(this.notebookId, this.$data.currentConnection);
    }
    handleClusterDropdownCommand(clusterAlias: string) {
        if (!this.clusterOptions) return;
        let cluster: Cluster = this.clusterOptions[clusterAlias];
        this.currentConnection = {
            ...this.$data.currentConnection,
            alias: clusterAlias,
            clusterId: cluster.clusterId,
            source: cluster.clusterName
        };


        // get account list
        const adaptee = this.$data.adaptee;
        this.accountOptions.splice(0, this.accountOptions.length);
        const accounts = adaptee.getAccounts(this.currentConnection.alias);
        concat(this.accountOptions, accounts);

        // set default account
        if(!_.includes(this.accountOptions, this.$data.currentConnection.batchAccount)) {
            const account = accounts[0] || Util.getNt();
            this.$data.batchAccount = account;
            this.$data.currentConnection.batchAccount = account;
        }
        this.savePreference(this.notebookId, this.$data.currentConnection);
    }
    accessable(cnn: IConnection) {
        /**
         * add by tianrsun @2019-11-13
         * enhance public notebook
         */
        if (this.isPublic) {
            return true
        }
        /**
         * end
         */
        const alias = cnn.alias;
        const cluster = this.clusterOptions[alias];
        return cluster && cluster.access ? cluster.access : false;
    }

    //action

    login() {
        if(this.codeType === 'KYLIN' && (!this.isPublicOwner && !this.isPublic)){
            this.databaseVisible = true;
            return;
        }
        // preference would be auto save when any propety change
        // //let preference = this.savePreference(this.notebookId,this.connection);
        let preference = this.notebook.preference;
        this.$store.dispatch("setNotebookStatus", {
            nid: this.notebookId,
            status: NotebookStatus.CONNECTING
        });
        // alert when capacity > 95
        if (this.capacity && this.capacity >= 95) {
            this.$message({
                showClose: true,
                duration: 0,
                message:
                    "Currently Hadoop Queue is 90% occupied,  connection may take longer or fail. Please try later or use another queue….",
                type: "warning"
            });
        }
        //init progress
        this.$store.dispatch('setConnectProgress', {nid: this.notebookId, progress: 0});
        this.$data.adaptee.connect(
            this.notebookId,
            this.$data.currentConnection,
            preference!
        ).then(() => {
          //updateFile status
          let file: IFile = this.$store.getters.fileByNb(this.notebook);
          let file_state = this.notebook.status==='IDLE'?'Connected':this.notebook.status;
          this.$store.dispatch(
                    'updateFile',
                    Object.assign(file, { ...file, state: file_state})
                );
            }).catch((e: any) => {
                console.debug("connect cancel", e)
                this.$store.dispatch("setNotebookStatus", {
                    nid: this.notebookId,
                    status: NotebookStatus.OFFLINE
                });
            });
    }
    logoutPopup() {
        this.$confirm("Current connection would be closed", "Warning", {
            confirmButtonText: "Disconnect",
            cancelButtonText: "Cancel",
            type: "warning"
        })
        .then(() => {
            this.logout();
        })
        .catch(() => {});
    }
    private logout() {
        this.$store.dispatch("setNotebookStatus", {
            nid: this.notebookId,
            status: NotebookStatus.DISCONNECTING
        });
        try {

            /** comments by tianrsun @2019-06-11
             * // notebook status change will be executed in websocket client
             *! comments by tianrsun @2019-06-28
             *! set notebook status to `DISCONNECT` immediately
             *
             */
            this.$store.dispatch("setNotebookStatus", {
                nid: this.notebookId,
                status: NotebookStatus.OFFLINE
            });
            this.$message.success(
                `\`${this.notebook.name}\` Disconnect succeed`
            );

            this.$data.adaptee
            .disconnect(this.notebookId)
            .then(() => {

            })
            .catch((err: any) => {
            });
        } catch(e) {
            console.error("disconnect error",e)
        }

    }
    savePreference(notebookId: string, connection: IConnection, notifyChange: boolean = true): IPreference {
        let notebook: INotebook = this.$store.state.workspace.workspaces[
            notebookId
        ];
        let preference = _.cloneDeep(notebook.preference) || <IPreference>{};
        preference = <IPreference>{
            ...preference,
            "notebook.connection": connection
        };
        this.$store.dispatch("setConnection", {
            notebookId: this.notebook.notebookId,
            connection: connection
        });
        this.$store.dispatch("updateNotebook", {
            notebookId: this.notebookId,
            preference
		    });
        // will not update file if it's a shared notebook
        if (this.viewOnly) {
          return preference;
        }
        const file: IFile = this.$store.getters.fileByNb(this.notebook);
        this.$set(file, 'preference', preference)
        this.$store.dispatch('updateFile', file);
        if(notifyChange){
            this.onConnectionChange();
        }
        return preference;
    }
    @Emit("onConnectionChange")
    onConnectionChange() {}

    @Watch("notebookLoaded", { immediate: true })
    onNotebookLoaded(loaded: boolean, oldVal: boolean) {
        if (!oldVal && loaded) {
            this.init();
        }
    }
    @Watch("codeType")
    onNotebookCodeTypechanged(newVal: boolean, oldVal: boolean) {
        console.debug("codeType===",`${oldVal} => ${newVal}`)
        this.init();

    }
    loginKylin(dbName: string, password:string){
        this.dbName = dbName;
        this.currentConnection = {
            ...this.$data.currentConnection,
            database: dbName,
            password: password,
            user: Util.getNt()
        };
        this.savePreference(this.notebookId, this.$data.currentConnection);

        let preference = this.notebook.preference;
        // alert when capacity > 95
        if (this.capacity && this.capacity >= 95) {
            this.$message({
                showClose: true,
                duration: 0,
                message:
                    "Currently Hadoop Queue is 90% occupied,  connection may take longer or fail. Please try later or use another queue….",
                type: "warning"
            });
        }
        this.$store.dispatch("setNotebookStatus", {
            nid: this.notebookId,
            status: NotebookStatus.CONNECTING
        });
        this.$data.adaptee.connect(
            this.notebookId,
            this.$data.currentConnection,
            preference!
        ).then(() => {

        }).catch(() => {
            console.debug("connect cancel")
        });
    }
    get currentCodeTypeInfo() {
        return CodeTypes[this.codeType];
    }
}
</script>
<style lang="scss" scoped>
@import "@/styles/global.scss";
.ntbk-cnn {
    display: inline-flex;
    align-items: center;
    .ntbk-cnn-clstr,
    .ntbk-cnn-acct,
    .cnn-code,
    .ntbk-cnn-actn,
    .ntbk-cnn-db{
        margin-left: 20px;
        /deep/ .el-button.el-button--text {
            color: $zeta-font-color;
            &:hover {
                color: $zeta-global-color;
            }
            &.is-disabled,
            &.is-disabled:hover {
                color: $zeta-global-color-disable;
            }
            padding: 0;
            text-align: right;
            // text-decoration: underline;
        }
        /deep/ .el-button.el-button--default {
            padding: 3px 11px;
        }
    }
    .ntbk-cnn-actn {
        text-align: center;
        .connecting {
            min-width: 95px;
            display: inline-block;
        }
        .cancel {
            min-width: 95px;
            display: none;
        }
        &:hover {
            .connecting {
                display: none;
            }
            .cancel {
                display: inline-block;
                margin-left: 0;
            }
        }
    }
}
.batch-list {
    max-height: 350px;
    overflow-y: auto;
}
.el-icon-warning:before {
    color: $zeta-font-color;
}
</style>

