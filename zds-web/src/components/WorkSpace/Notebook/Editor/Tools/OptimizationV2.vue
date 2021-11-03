<template>
    <el-popover placement="bottom-start" :width="540" trigger="click" @show="()=> init()">
        <div class="optimization notebook-tool" v-loading="updating">
            <el-select
                v-model="prflOption.selected"
                ref="prflSelect"
                @change="(name)=> onPrflSelect(name)"
            >
                <div class="prfl-list-new">
                    <el-input
                        class="new-prfl"
                        v-model="prflOption.newPrflName"
                        placeholder="New Profile Title"
                        :disabled="creating"
                    >
                        <template slot="append">.cfg</template>
                    </el-input>
                    <el-button
                        class="add-prfl circle-btn"
                        size="mini"
                        circle
                        type="text"
                        v-click-metric:NB_TOOLBAR_CLICK="{name: 'config => create config'}"
                        @click="() => addNewProfile(prflOption.newPrflName)"
                        :loading="creating"
                    >
                        <i class="el-icon-plus"/>
                    </el-button>
                    <div class="err-msg" v-if="dropdownError.show">{{dropdownError.msg}}</div>
                </div>
                <el-option
                    v-for="(prfl,key) in storeProfiles"
                    :key="key"
                    :label="key"
                    :value="key"
                >{{(prfl && prfl.isNew)? '*':''}}{{key}}</el-option>
            </el-select>
            <div
                class="default-setting"
                v-if="prflOption.selectedCfg && prflOption.selectedCfg.optmzt"
            >
                <div class="setting-item-add" v-if="(prflOption.selected != defaultPrflName)">
                    <span>
                        <el-button
                            class="circle-btn"
                            type="primary"
                            icon="el-icon-plus"
                            size="mini"
                            circle
                            @click="addNewCol()"
                            :disabled="!prflOption_add.colName || !prflOption_add.colName.trim()"
                        />
                        <el-input v-model="prflOption_add.colName"/>
                    </span>
                    <el-select
                        v-if="prflOption_add.colName === 'spark.yarn.queue'"
                        v-model="prflOption_add.val"
                    >
                    <el-option
                            v-for="(name,key) in queue"
                            :key="key"
                            :label="name===queueAuto?'auto':name"
                            :value="name"
                    ></el-option>
                    </el-select>
                    <el-select
                        v-else-if="prflOption_add.colName === debugModeKey"
                        v-model="prflOption_add.val"
                    >
                        <el-option
                            v-for="(val, key) in DebugMode"
                            :key="key"
                            :label="key"
                            :value="val"
                        ></el-option>
                    </el-select>
                    <el-input
                        v-else
                        v-model="prflOption_add.val"
                        @blur="setAddValue"
                    />
                </div>
                <div
                    class="setting-item-wrapper"
                    :key="'dft_col_' + $index"
                    v-for="(item,$index) in getOptionsColumns(prflOption.selectedCfg.optmzt[prefix])"
                >
                    <div
                        class="setting-item"
                        v-if="defaultHiddenCol(item)">
                        <span>
                            <el-button
                                v-if="(prflOption.selected !== defaultPrflName)"
                                class="circle-btn"
                                type="primary"
                                icon="el-icon-minus"
                                size="mini"
                                circle
                                @click="()=> removeCol(item)"
                            />
                            {{getDisplayName(item)}}
                            <span class="tip">{{(item === (prefix+'.'+maxResult) && !isPublic)?'(Maximun 5000)':''}}</span>
                        </span>
                        <div v-if="getDisplayName(item) === 'spark.yarn.queue'">
                            <el-select
                                :disabled="prflOption.selected == defaultPrflName"
                                v-model="prflOption.selectedCfg.optmzt[prefix][item]"
                            >
                                <el-option
                                    v-for="(name,key) in queue"
                                    :key="key"
                                    :label="name===queueAuto?'auto':name"
                                    :value="name"
                                ></el-option>
                            </el-select>
                            <el-tooltip  content="Bottom center" placement="bottom" effect="light" v-if="!accessable(prflOption.selectedCfg.optmzt[prefix][item])">
                                <div slot="content">
                                    please follow
                                    <a
                                        target="_blank"
                                        href="https://wiki.vip.corp.ebay.com/pages/viewpage.action?spaceKey=ND&title=Zeta+-+User+Guide"
                                    >wiki</a>
                                    to access {{prflOption.selectedCfg.optmzt[prefix][item]}}.
                                </div>
                                <i class="el-icon-question"></i>
                            </el-tooltip>
                        </div>
                        <el-select
                            v-else-if="getDisplayName(item) === debugModeKey"
                            :disabled="prflOption.selected == defaultPrflName"
                            v-model="prflOption.selectedCfg.optmzt[prefix][item]"
                        >
                            <el-option
                                v-for="(val, key) in DebugMode"
                                :key="key"
                                :label="key"
                                :value="val"
                            ></el-option>
                        </el-select>
                        <el-button
                          v-else-if="isHermes && item === (prefix+'.'+maxResult)"
                          type="text"
                          @click="editInPreference">
                            Edit
                        </el-button>
                        <el-input
                            v-else
                            :disabled="prflOption.selected == defaultPrflName"
                            v-model="prflOption.selectedCfg.optmzt[prefix][item]"
                            @blur="setValue"
                            :data-colname="item"
                        />
                    </div>
                </div>
            </div>
            <div class="apply">
                <el-button
                    v-show="!applyAlert"
                    type="primary"
                    :disabled="notebookStatus ==='RUNNING' || applyDisabled"
                    v-click-metric:NB_TOOLBAR_CLICK="{name: 'config => apply config'}"
                    @click="applyEvent"
                >Apply</el-button>
                <div
                    v-show="applyAlert"
                >You’re attempting to disconnect from server. Are you sure you want to continue?</div>
                <div v-show="applyAlert" class="apply-confirm">
                    <el-button type="primary" plain @click=" () => applyAlert = false">Cancel</el-button>
                    <el-button type="primary" @click="update"
                    v-click-metric:NB_TOOLBAR_CLICK="{name: 'config => apply config'}">Continue</el-button>
                </div>
            </div>
        </div>
        <el-button type="text" class="notebook-tool-btn" slot="reference" :disabled="disable" v-click-metric:NB_TOOLBAR_CLICK="{name: 'config'}">
            <i class="zeta-icon-optimize"></i>
            <span class="params-title">{{title}}</span>
        </el-button>
    </el-popover>
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch, Emit, Inject } from "vue-property-decorator";
import {
    IPreference,
    IOptimization,
    INotebook,
    NotebookStatus,
    INotebookConfig,
    IWorkspaceStore,
    IConnection,
    CodeType,
    CodeTypes
} from "@/types/workspace";
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import _ from "lodash";
import Util from "@/services/Util.service";
import { IFile } from "@/types/repository";
import moment from "moment";
import { RestPacket } from "@/types/RestPacket";
import config from "@/config/config";
import { EventBus } from "@/components/EventBus";
import { wsclient } from '@/net/ws';
import { isHermes } from "@/services/connection.service";
const DEFAULT_PRFL_NAME = config.zeta.notebook.optimization.defaultPrflName;
enum DebugMode {
    off = 'false',
    on = 'true'
  }

const HERMESCONFIG = ['zds.jdbc.spark.yarn.queue','zds.jdbc.zds.view.maxResult'];
const NOTSQLCONFIG = ['zds.jdbc.zds.view.maxResult'];
const DEBUGMODE = 'spark.sql.join.statisticOnExecution';
const MAXRESULT = 'zds.view.maxResult';
const MAXRESULTVALUE = 5000;
const NEWCONFIG = ['zds.livy.zds.view.maxResult','zds.livy.spark.sql.join.statisticOnExecution'];
@Component({
    components: {}
})
export default class Optimization extends Vue {
    @Inject() notebookRemoteService: NotebookRemoteService;
    @Inject() viewOnly: boolean;
    readonly defaultPrflName = DEFAULT_PRFL_NAME;
    readonly livyPrefix = "zds.livy";
    readonly jdbcPrefix = "zds.jdbc";
    readonly queueAuto = 'usingDynamicQueue';
    readonly debugModeKey: string = 'Diagnose Mode';
    @Prop() notebook: INotebook;
    // @Prop() notebookId: string;
    @Prop({ default: false, type: Boolean }) disable: boolean;
    @Prop() title: string;
    @Prop() currentCodeType: string;
    notebookId: string;
    maxResult: string = MAXRESULT;
    constructor(){
        super();
        this.notebookId = this.notebook.notebookId;
    }
    accessable(q: string){
        if(this.prflOption.selected === this.defaultPrflName){
            return true;
        }
        return this.queue.indexOf(q)>-1?true: false;
    }
    get isPublic() {
        return this.notebook.publicReferred != null;
    }
    get prefix(){
        if(this.isHermes || !this.currentCodeTypeIsSql) return this.jdbcPrefix;
        return this.livyPrefix;
    }
    get isHermes() {
        const connection = this.notebook.connection;
        return Boolean(
            this.currentCodeType == CodeType.SQL &&
                connection &&
                // connection.clusterId === 16
                isHermes(connection.clusterId)
        );
    }
    get currentCodeTypeIsSql() {
        return this.currentCodeType === CodeType.SQL;
    }
    get currentBatchAccount(){
        return this.notebook.connection.batchAccount;
    }
    get cfgDirty() {
        // console.log("check dirty")
        if (
            !(
                this.prflOption.selectedCfg &&
                this.prflOption.selectedCfg.optmzt &&
                this.prflOption.selectedCfg.optmzt[this.prefix]
            )
        ) {
            return false;
        }
        if (
            !(
                this.storeProfiles &&
                this.storeProfiles[this.prflOption.selected] != undefined &&
                (this.storeProfiles[
                    this.prflOption.selected
                ] as INotebookConfig).optmzt &&
                (this.storeProfiles[
                    this.prflOption.selected
                ] as INotebookConfig).optmzt[this.prefix]
            )
        ) {
            return false;
        }
        let selectedCfg = this.prflOption.selectedCfg.optmzt[this.prefix];
        let storeCfg = (this.storeProfiles[
            this.prflOption.selected
        ] as INotebookConfig).optmzt[this.prefix];
        if (!_.isEmpty(this.prflOption_add.colName)) {
            // console.log("setDirty","add col")
            return true;
        }

        let keysEqual = Boolean(
            _.keys(selectedCfg).length === _.keys(storeCfg).length
        );
        if (!keysEqual) {
            // console.log("setDirty","keysEqual")
            return true;
        }

        let valEqual = true;
        let diffVal = _.chain(storeCfg)
            .forEach((val, col) => {
                if (val != selectedCfg[col] && valEqual == true) {
                    valEqual = false;
                }
            })
            .value();
        if (!valEqual) {
            // console.log("setDirty","valEqual",diffVal)
            return true;
        }
        return false;
    }
    get fileChanged(): boolean {
        if (!this.prflOption.selected) {
            return false;
        }
        let notebookProfileId = (this.preference &&
        this.preference["notebook.profile"]
            ? this.preference["notebook.profile"]
            : DEFAULT_PRFL_NAME) as string;
        let selectedProfileId = (this.storeProfiles[
            this.prflOption.selected
        ] as INotebookConfig).notebookId;
        return Boolean(notebookProfileId != selectedProfileId);
    }
    get applyDisabled() {
        if (this.fileChanged) {
            return false;
        } else {
            return !this.cfgDirty;
        }
    }

    get defaultOptions(): IOptimization {
        let defaultCfg: INotebookConfig =
            this.$store.getters.cfgs()[DEFAULT_PRFL_NAME] ||
            <INotebookConfig>{
                optmzt: <IOptimization>{
                    "zds.livy": {},
                    "zds.jdbc":{}
                }
            };
        for( const prefix in defaultCfg.optmzt){
          let queue = defaultCfg.optmzt[prefix][`${prefix}`+".spark.yarn.queue"];
            if(queue && (queue==='auto')){
                defaultCfg.optmzt[prefix][`${prefix}`+".spark.yarn.queue"] = this.queueAuto;
            }
        }
        return defaultCfg.optmzt;
    }
    private currentOptions: IOptimization = <IOptimization>{};

    private applyAlert: boolean = false;
    private updating: boolean = false;
    private creating: boolean = false;

    private prflOption = {
        popupShow: false,
        newPrflName: "",
        selected: "",
        selectedCfg: <INotebookConfig>{}
    };

    private prflOption_add = {
        colName: "",
        val: ""
    };
    private dropdownError = {
        msg: "",
        show: false,
        error: function(msg: string) {
            this.msg = msg;
            this.show = true;
        },
        clear: function() {
            this.msg = "";
            this.show = false;
        }
    };
    private queue: Array<string> = [this.queueAuto];
    private DebugMode = DebugMode;
    private debugMode: DebugMode = DebugMode.off;
    get notebookStatus() {
        let notebook: INotebook = this.$store.state.workspace.workspaces[
            this.notebookId
        ];
        if (!(notebook && notebook.status)) {
            return "OFFLINE";
        }
        if (notebook.status === NotebookStatus.OFFLINE) {
            return "OFFLINE";
        } else if (notebook.status === NotebookStatus.CONNECTING) {
            return "CONNECTING";
        } else if (notebook.status === NotebookStatus.DISCONNECTING) {
            return "DISCONNECTING";
        } else if (notebook.status === NotebookStatus.RUNNING) {
            return "RUNNING";
        } else {
            return "ONLINE";
        }
    }

    get preference(): IPreference | undefined {
        if (!this.notebookId) {
            return undefined;
        }
        let notebook: INotebook = this.$store.state.workspace.workspaces[
            this.notebookId
        ];
        return notebook.preference;
    }
    get storeProfiles(): Dict<INotebookConfig | undefined> {
        return this.$store.getters.cfgs();
    }
    getOptionsColumns(optmzt: IOptimization) {
        return _.keys(optmzt);
    }
    getDisplayName(colName: string): string {
        let names: string = colName.replace(this.prefix + ".", "");
        if(names === MAXRESULT){
            return "Result Rows"
        }
        if(names === DEBUGMODE){
            return "Diagnose Mode"
        }
        return names;
    }
    isHermesMaxResult(configName: string) {
      return configName === (this.prefix + '.' + this.maxResult);
    }
    editInPreference() {
      this.$router.push('/settings?preference=HermesPreference')
    }
    //mounted
    mounted() {}
    //action
    init() {
        let profileId = (this.preference && this.preference["notebook.profile"]
            ? this.preference["notebook.profile"]
            : DEFAULT_PRFL_NAME) as string;
        let profile: INotebookConfig | undefined; //= this.storeProfiles[profileId];
        _.forEach(
            this.storeProfiles,
            (pf: INotebookConfig | undefined, name) => {
                if (pf != undefined && pf.notebookId == profileId) {
                    profile = pf;
                }
            }
        );
        let profileName = profile && profile.title ? profile.title : DEFAULT_PRFL_NAME;
        this.onPrflSelect(profileName, true);
        // get user queue
        if(this.currentCodeTypeIsSql){
            this.getUserQueue();
        }
    }
    update() {
        //   add unresolve params
        if (this.prflOption_add.colName && this.prflOption_add.val) {
            this.addNewCol();
        }

        if (this.prflOption.selectedCfg) {
            this.updating = true;

            // disconnect
            this.$store.dispatch("setNotebookStatus", {
                nid: this.notebookId,
                status: NotebookStatus.DISCONNECTING
            });
            return wsclient
                .notebookDisconnect(this.notebookId)
                .then(() => {
                    this.applyAlert = false;
                    this.$store.dispatch("setNotebookStatus", {
                        nid: this.notebookId,
                        status: NotebookStatus.OFFLINE
                    });
                    if (
                        this.prflOption.selectedCfg.notebookId ===
                        DEFAULT_PRFL_NAME
                    ) {
                        return this.updateNotebookPreference();
                    } else {
                        return this.updateOptimization(
                            this.notebookId,
                            this.prflOption.selectedCfg
                        ).then(() => {
                            return this.updateNotebookPreference();
                        });
                    }
                })
                .catch((e: any) => {
                    console.error("save settings in optimization failed", e);
                    e.message = 'ail to apply settings.'
                });
        }
    }
    updateOptimization(notebookId: string, cfg: INotebookConfig) {
        let isNew = cfg.isNew;
        // huhan add by @2019-09-06
        this.handleExcludeSpecialConfig(cfg);
        //end
        let addOrUpdateCfgPromise = this.notebookRemoteService.save({
            id: cfg.notebookId,
            content: JSON.stringify(cfg.optmzt),
            updateDt: moment().valueOf()
        } as RestPacket.File);
        return addOrUpdateCfgPromise.then(({ data: file }) => {
            file.notebookId = file.id;
            this.$store.dispatch("updateFile", file);
            // if file opened
            // update notebook store
            let notebook: INotebook = this.$store.getters.nbByFile(file);
            if (notebook) {
                notebook.code = file.content;
                this.$store.dispatch("updateNotebook", notebook);
            }
            return;
        });
    }
    updateNotebookPreference() {
        // generate note preference
        let notebookPreference: IPreference =
            _.cloneDeep(this.preference) || <IPreference>{};
        notebookPreference[
            "notebook.profile"
        ] = this.prflOption.selectedCfg.notebookId;
        this.updating = true;
        return this.notebookRemoteService
            .savePreference(this.notebookId, notebookPreference)
            .then(() => {
              this.updating = false;
              this.$message.success("Successfully applied settings.");
              if (this.viewOnly) {
                return;
              }
              this.$store.dispatch("updateNotebook", {
                  notebookId: this.notebookId,
                  preference: notebookPreference
              });

              /** add emit event , rm EventBus */
              // EventBus.$emit("notebook-perference-change");
              if(this.isHermes || !this.currentCodeTypeIsSql) return;
              this.onProfileChange();
              return;
            });
    }
    applyEvent() {
        if (
            this.notebookStatus !== "ONLINE" &&
            this.notebookStatus !== "RUNNING"
        ) {
            this.update();
        } else if (this.notebookStatus !== "RUNNING") {
            this.applyAlert = true;
        }
    }

    addNewProfile(name: string) {
        this.dropdownError.clear();
        if (!name || !name.trim()) {
            this.dropdownError.error("profile name is null");
            this.prflOption.newPrflName = "";
            return;
        }
        name += ".cfg";
        if (this.storeProfiles[name]) {
            this.dropdownError.error("profile name already exist");
            this.prflOption.newPrflName = "";
            return;
        }
        this.creating = true;
        this.notebookRemoteService
            .add({
                id: "-1",
                nt: Util.getNt(),
                path: "/conf/",
                content: JSON.stringify(this.defaultOptions),
                title: name,
                createDt: moment().valueOf(),
                updateDt: moment().valueOf(),
                status: "",
                nbType: "single",
                preference: "{}"
            })
            .then(({ data: file }) => {
                let newCfg = <INotebookConfig>{
                    notebookId: file.id,
                    title: file.name,
                    optmzt: file.content
                };
                file.notebookId = file.id;
                this.$store.dispatch("updateFile", file);
                return;
            })
            .then(() => {
                this.prflOption.newPrflName = "";
                // select new prfl
                this.onPrflSelect(name);
                this.creating = false;
            });
    }
    onPrflSelect(name: string, init?: boolean) {
        let cfg: INotebookConfig = _.cloneDeep(
            this.storeProfiles[name]
        ) as INotebookConfig;
        if (_.isEmpty(cfg.optmzt)) {
            this.updating = true;
            this.notebookRemoteService.getCfgById(cfg.notebookId).then(({ data: file }) => {
                cfg.optmzt = JSON.parse(file.content as string);
                file.notebookId = file.id;
                this.$store.dispatch("updateFile", file);
                // huhan add by @2019-09-06
                this.handleExcludeSpecialConfig(cfg);
                //end
                this.prflOption = {
                    ...this.prflOption,
                    selected: cfg.title,
                    selectedCfg: cfg,
                    popupShow: false
                };
                this.updating = false;
            });
        } else {
            // huhan add by @2019-09-06
            this.handleExcludeSpecialConfig(cfg);
            //end
            this.prflOption = {
                ...this.prflOption,
                selected: cfg.title,
                selectedCfg: cfg,
                popupShow: false
            };
        }
    }
    addNewCol() {
        // column == ''
        if (
            !this.prflOption_add.colName ||
            !this.prflOption_add.colName.trim()
        ) {
            return;
        }
        // column exist
        if (
            this.prflOption.selectedCfg.optmzt[this.prefix]&&
            this.prflOption.selectedCfg.optmzt[this.prefix][
                `${this.prefix}.${this.prflOption_add.colName}`
            ]
        ) {
            return;
        }
        let colName = this.prflOption_add.colName.trim();
        let reg1 = /^Result\s{1,}rows$/gi;
        let reg2 = /^Diagnose\s{1,}Mode$/gi;
        if(reg1.exec(colName)){
            colName = 'zds.view.maxResult';
        }
        if(reg2.exec(colName)){
            colName = 'spark.sql.join.statisticOnExecution';
        }
        this.prflOption.selectedCfg.optmzt[this.prefix] = _.assign(
            {
                [`${this.prefix}.${colName}`]: this.prflOption_add.val
            },
            this.prflOption.selectedCfg.optmzt[this.prefix]
        );
        // clear
        this.prflOption_add = {
            colName: "",
            val: ""
        };
    }
    removeCol(item: any) {
        console.debug("rm item", item);
        let optmzt: Dict<any> = {};
        _.chain(this.prflOption.selectedCfg.optmzt[this.prefix])
            .forEach((val, col) => {
                if (col !== item) {
                    optmzt[col] = val;
                }
            })
            .value();
        this.prflOption.selectedCfg.optmzt[this.prefix] = optmzt;
    }
    setValue(e:any){
        if(this.isPublic) return;
        let colName = e.target.dataset.colname.trim();
        let value = e.target.value;
        if(colName === `${this.prefix+'.'+MAXRESULT}` && value>MAXRESULTVALUE){
            this.prflOption.selectedCfg.optmzt[this.prefix][colName] = MAXRESULTVALUE.toString();
        }
    }
    setAddValue(){
        if(this.isPublic) return;
        let colName = this.prflOption_add.colName.trim();
        let value = parseInt(this.prflOption_add.val);
        let reg1 = /^Result\s{1,}rows$/gi;
        if(reg1.exec(colName) && value>MAXRESULTVALUE){
            this.prflOption_add.val = MAXRESULTVALUE.toString();
        }
    }
    defaultHiddenCol(item: any){
        if((this.prflOption.selected === this.defaultPrflName) && (this.isHermes || !this.currentCodeTypeIsSql)){
            if( this.isHermes && HERMESCONFIG.indexOf(item)===-1){
                return false;
            }
            if(!this.currentCodeTypeIsSql && NOTSQLCONFIG.indexOf(item)===-1){
                return false;
            }
        }
        return true;
    }
    handleExcludeSpecialConfig(cfg: INotebookConfig){
        console.log('====',cfg);
        if(cfg.title === this.defaultPrflName) return;
        //only herme config(maxRow, queue)
        if(this.isHermes){
            cfg.optmzt = this.getHermesConfig(cfg);
        }
        //only maxRow config
        if(!this.currentCodeTypeIsSql){
            cfg.optmzt = this.getNotSqlConfig(cfg);
        }
        //is sql but not hermes add new config
        if(this.currentCodeTypeIsSql && !this.isHermes){
            cfg.optmzt = this.addNewConfig(cfg);
        }
    }
    addNewConfig(cfg: INotebookConfig){
        let optmzt: Dict<any> = {};
        let flag = false;
        if(cfg.optmzt[this.prefix]){
            _.chain(cfg.optmzt[this.prefix])
                .forEach((val, col) => {
                    if (NEWCONFIG.indexOf(col)>-1) {
                        flag = true;
                    }
                })
                .value();
        }
        if(!flag){
            _.chain(NEWCONFIG)
                .forEach((val, index) => {
                    optmzt[val] = (index===0?'1000':'false');
                })
                .value();
        }
        cfg.optmzt[this.prefix] = Object.assign(optmzt,cfg.optmzt[this.prefix]);
        return cfg.optmzt;
    }

    getHermesConfig(cfg: INotebookConfig){
        let optmzt: Dict<any> = {};
        if(cfg.optmzt[this.prefix]){
            _.chain(cfg.optmzt[this.prefix])
                .forEach((val, col) => {
                    // if (HERMESCONFIG.indexOf(col)>-1) {
                        optmzt[col] = val;
                    // }
                })
                .value();
        }else{
            _.chain(HERMESCONFIG)
                .forEach((val, index) => {
                    optmzt[val] = (index===0?this.queueAuto:'1000');
                })
                .value();
        }
        cfg.optmzt[this.prefix] = optmzt;
        return cfg.optmzt;
    }
    getNotSqlConfig(cfg: INotebookConfig){
        let optmzt: Dict<any> = {};
        if(cfg.optmzt[this.prefix]){
            _.chain(cfg.optmzt[this.prefix])
                .forEach((val, col) => {
                    if (NOTSQLCONFIG.indexOf(col)>-1) {
                        optmzt[col] = val;
                    }
                })
                .value();
        }else{
            _.chain(NOTSQLCONFIG)
                .forEach((val, col) => {
                    optmzt[val] = '1000';
                })
                .value();
        }

        cfg.optmzt[this.prefix] = optmzt;
        return cfg.optmzt;
    }
    getUserQueue(){
        let clusterName = (this.preference && this.preference['notebook.connection']
            ? (this.preference['notebook.connection'] as IConnection ).source: 'undefined' ) as string;
        this.updating = true;
        this.queue = [this.queueAuto];
        this.notebookRemoteService.getUserQueue(clusterName, this.currentBatchAccount).then(({ data }) => {
            let q = data.filter((name:string) => name !== 'hdlq-data-zeta');
            this.queue = this.queue.concat(q);
            this.updating = false;
        });
    }
    @Emit('onProfileChange')
    onProfileChange() {

    }
}
</script>
<style lang="scss" scoped>
.circle-btn {
    padding: 0px !important;
    margin-left: 5px;
    margin-right: 5px;
    i {
        font-size: 10;
        font-weight: 900;
    }
}
.prfl-list-new {
    padding: 2px 5px 5px 5px;
    // .add-prfl {
    // }
    .el-input.new-prfl {
        width: 200px;
    }
    .err-msg {
        font-size: 10px;
        color: red;
    }
}

.optimization {
    padding: 15px;
    .default-setting,
    .advanced-setting {
        width: 510px;
        max-height: 300px;
        overflow-y: auto;
        margin: 15px 0;
        .setting-item,
        .setting-item-add {
            display: flex;
            align-items: center;
            margin: 5px 0;
            justify-content: space-between;
            .el-input {
                width: 180px;
                /deep/ .el-input__inner {
                    height: 30px;
                    line-height: 30px;
                }
            }
            .el-select {
                width: 180px;
            }
        }
    }
    .advanced-btn-group {
        text-align: right;
    }
    .apply {
        text-align: right;
        .info {
            width: 400px;
            text-align: left;
        }
        .apply-confirm {
            text-align: right;
        }
    }
    .tip{
        font-size: 12px;
        color: #999999;
    }
}
.notebook-tool-btn {
    position: relative;
    .zeta-icon-optimize{
        transition: .3s;
        font-size: 20px;
    }
    .params-title{
        font-size: 12px;
        line-height: 20px;
        transition: .3s;
        position: absolute;
        display: none;
    }
    &:hover{
        .zeta-icon-optimize{
            margin-right: 30px;
        }
        .params-title{
            transform: translateX(-30px);
            display: inline-block;
        }
    }
}
</style>

