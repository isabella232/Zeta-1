<template>
    <el-popover width=530 placement="bottom" trigger="click" class="schedule-tool" v-model="popoverShow">
        <el-button type="text" slot="reference" class="notebook-tool-btn" :disabled="disable" 
            v-click-metric:NB_TOOLBAR_CLICK="{name: 'schedule'}">
            <i class="zeta-icon-history" ></i>
            <span class="params-title">{{title}}</span>
        </el-button>
        <schedule-container
            type="Notebook"
            :notebookId="notebookId"
            :scheduleInfo="scheduleInfo"
            v-if="popoverShow"
            @visableChange="handleVisableChange"
        />
    </el-popover>
</template>
<script lang="ts">
import { Component, Vue, Prop, Watch, Inject } from "vue-property-decorator";
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import { EventBus } from "@/components/EventBus";
import _ from "lodash";
import ScheduleContainer, {
    NotebookSchedule,
    ScheduleConfig
} from "@/components/common/schedule-container";
import config from "@/config/config";
import { CodeType, CodeTypes, INotebook } from "@/types/workspace";
import Util from "@/services/Util.service";

const DEFAULT_CONNECTION = config.zeta.notebook.connection.defaultConnection;
const TD_DEFAULT_CONNECTION =
    config.zeta.notebook.tdConnection.defaultConnection;
const HIVE_DEFAULT_CONNECTION = config.zeta.notebook.hiveConnection.defaultConnection

@Component({
    components: {
        ScheduleContainer
    }
})
export default class ScheduleTool extends Vue {
    @Inject()
    notebookRemoteService: NotebookRemoteService
    @Prop()
    notebookId: string;
    @Prop({ default: false, type: Boolean }) disable: boolean;
    popoverShow: boolean = false;
    @Prop()
    currentCodeType: CodeType;
    @Prop() title: string;
    get scheduleInfo(): ScheduleConfig {
        return this.$store.getters.taskByNotebookId(this.notebookId);
    }

    // get detail(): NotebookSchedule {
    //     let scheduleDetail: NotebookSchedule = {
    //         proxyUser: "",
    //         req: {
    //             notebookId: this.notebookId,
    //             reqId: null,
    //             codes: null,
    //             interpreter: CodeTypes[this.connection.codeType].interpreter
    //         }
    //     };
    //     const isHermes = this.connection.codeType === CodeType.SQL && this.connection.clusterId == 16;
    //     if (this.connection.codeType === CodeType.TERADATA) {
    //         scheduleDetail.host = this.connection.source;
    //         scheduleDetail.proxyUser = Util.getNt();
    //         scheduleDetail.jdbcType = "teradata";
    //     } else if(this.connection.codeType === CodeType.HIVE) {
    //         const alias = this.connection.alias;
    //         const host = Util.getDefaultConnectionByPlat(CodeType.HIVE).hostMap[alias]
    //         const principal = Util.getDefaultConnectionByPlat(CodeType.HIVE).principalMap[alias]
    //         scheduleDetail.proxyUser = Util.getNt();
    //         const prop = {
    //             host,
    //             user: Util.getNt(),
    //             password: '',
    //             database: "default",
    //             "jdbc.props.hive.server2.remote.principal": principal,
    //             port: 10000,
    //             jdbc_type:  "hive"
    //         }
    //         Object.assign(scheduleDetail,{prop})
    //     } else if(isHermes) {
    //         const host = 'hermes.prod.vip.ebay.com';
    //         const interpreter = 'carmel';
    //         const jdbcType = 'carmel';
    //         const pricipal = 'b_carmel/hermes.prod.vip.ebay.com@PROD.EBAY.COM';
    //         const prop =  {
	// 			host,
	// 			user: Util.getNt(),
	// 			password: '',
	// 			database: 'default',
	// 			'jdbc.props.hive.server2.remote.principal': pricipal,
	// 			port: 10000,
	// 			jdbc_type: jdbcType,
    //         }
    //         scheduleDetail.proxyUser = Util.getNt();
    //         scheduleDetail.req.interpreter = interpreter
    //         scheduleDetail.clusterId = this.connection.clusterId;
    //         Object.assign(scheduleDetail,{prop})
    //     } else if (this.connection.codeType === CodeType.SQL) {
    //         scheduleDetail.clusterId = this.connection.clusterId;
    //         scheduleDetail.proxyUser = this.connection.batchAccount;
    //     }
    //     console.log('scheduleDetail',scheduleDetail)
    //     return scheduleDetail;
    // }

    get connection() {
        if (this.currentConnection !== undefined) {
            if (this.currentConnection.clusterId === undefined) {
                if (this.currentConnection.codeType === CodeType.SQL) {
                    return DEFAULT_CONNECTION;
                } else if (this.currentConnection.codeType === CodeType.TERADATA) {
                    return TD_DEFAULT_CONNECTION;
                } else if (this.currentConnection.codeType === CodeType.HIVE) {
                    return HIVE_DEFAULT_CONNECTION
                }
            }
            return this.currentConnection;
        }
        return DEFAULT_CONNECTION;
    }

    get currentConnection() {
        return this.$store.getters.currentConnection;
    }

    handleVisableChange(val: boolean) {
        this.popoverShow = val;
    }
}
</script>
<style lang="scss" scoped>
.schedule-tool {
    > .notebook-tool-btn[disabled="disabled"] {
        cursor: not-allowed;
    }
}
.notebook-tool-btn {
    position: relative;
    .zeta-icon-history{
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
        .zeta-icon-history{
            margin-right: 40px;
        }
        .params-title{
            transform: translateX(-40px);
            display: inline-block;
        }
    }
}
</style>
