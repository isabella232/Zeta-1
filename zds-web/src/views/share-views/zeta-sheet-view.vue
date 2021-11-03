<template>
    <div class="sheet-view-container" v-loading="flag.metaSheetLoading">
        <ReadOnly :message="message"/>
        <div class="header">
            <span class="name">{{sheetName}}</span>
            <div class="tool">
                <el-tooltip placement="bottom" effect="light">
                    <div slot="content">
                        Request for sheet edit access and we will send an email to owner for approval.
                    </div>
                    <el-button @click="apply" type="primary" v-click-metric:ZS_CLICK="{name: 'Apply for access, nt: ' +  nt}">Request Edit</el-button>
                </el-tooltip>
            </div>
       </div>
       <div ref="spreadsheet" id="spreadsheet"></div>

    </div>
</template>

<script lang="ts">
import { Component, Vue, Inject, Prop, Ref, Watch, Provide,Mixins } from "vue-property-decorator";
import { MetaConfigTableRow, MetaSheetTableRow, MetaSheetConfig,Mode, MODE, PLATFORM, ACCESS, MS_SYS, MSG_TYPE, MS_STATUS, SourceConfig} from '@/types/meta-sheet';
import MetaShare from './meta-share.vue';
import _ from "lodash";
import Handsontable from 'handsontable';
import MetaSheetService from '@/services/remote/MetaSheetService';
import MetaSourceConfig from './source-config.vue';
import MetaSheetUtil, {_ID_, _VERSION_} from '@/components/MetaSheet/util';
import moment from 'moment';
import { WorkspaceSrv } from '@/services/Workspace.service';
import ReadOnly from '@/components/WorkSpace/Notebook/Editor/Tools/ReadOnly.vue';
import Util from "@/services/Util.service";
let decodeHTML = (html) =>{
	let txt = document.createElement('textarea');
	txt.innerHTML = html;
	return txt.value;
};
@Component({
  components: {
	 ReadOnly
  },
})
export default class MetaSheetView extends Vue{
    @Ref('spreadsheet') tableEl: HTMLElement;

    sheet: Handsontable;
    debounceRender: Function;
    metaSheetData:any = [];
    metasheetId: string;
    metaSheetRow:MetaSheetTableRow;
    metaSheetService  = new MetaSheetService();
    columnTypeMap:any = null;
    columns:any =  null;
    message = 'This Sheet is Read Only';
    sheetName = '';

    flag = {
        editable: false,
        metaSheetLoading: true
    }

    constructor() {
        super();
        this.debounceRender = _.debounce(this.renderSheet, 300, { trailing: true });
    }

    get headers() {
        return _.map(this.columns, d => d.column);
    }

    get colHeaders() {
        return [_ID_, _VERSION_, ...this.headers];
    }

    get nt() {
        return Util.getNt();
    }

    async created() {
        this.metasheetId = this.$route.query.dashboardId as string;
        await this.getMetaSheetData();
    }

    async apply() {
        try {
            const nt = Util.getNt();
            let response = await this.metaSheetService.applyForAccess(nt, this.metaSheetRow!.id!);
            if(MetaSheetUtil.isOk(response)) {
                MetaSheetUtil.showMessage(this, MSG_TYPE.SUCCESS, 'Request Accepted!');
            } else {
                MetaSheetUtil.showMessage(this, MSG_TYPE.ERROR, response.data.msg);
            }
        }
        catch(e) {
            MetaSheetUtil.showMessage(this, MSG_TYPE.ERROR, 'Fail, please refresh page and try again');
        }
    }

    renderSheet() {
        if(this.sheet && !this.sheet.isDestroyed){
            this.sheet.render();
        }
        else{
            this.hook();
        }
        this.flag.metaSheetLoading = false;
    }

    hook() {
        let spreadsheetEl = document.querySelector('#spreadsheet');
        if(spreadsheetEl) {
            const rowsLimit = 5000;
            this.sheet = new Handsontable(this.tableEl, {
                licenseKey: 'non-commercial-and-evaluation',
                // readOnly: this.isSheetReadyOnly,
                // readOnly: true,
                data: this.metaSheetData,
                rowHeaders: true,
                colHeaders: this.colHeaders,
                colWidths: _.times(this.headers.length + 2, () => 100),
                filters: false,
                height: '90%',
                contextMenu: ['row_above','row_below','remove_row'],
                columnSorting: false,
                hiddenColumns: {
                    columns: [0, 1],
                    indicators: false
                },
                autoRowSize: false,
                autoColumnSize: false,
                manualColumnResize: true,
                undo: false,
                rowHeights: 23,
                maxCols: this.colHeaders.length,
                copyPaste: false,
                afterBeginEditing: (row, column) => {
                    let $input = document.querySelector(".handsontableInput");
                    if($input) {
                        ($input as HTMLTextAreaElement).setAttribute('readonly', 'true');
                    }
                },
                afterColumnResize: () => {
                    setTimeout(() => {
                        this.sheet.render()
                    }, 20);
                },
            });
        }
    }

    processedData(number) {
        let data = new Array(number);
        for(let i = 0; i < number; i++) {
            data[i] = [null,null]; //_ID_, _VERSION_
            for(let j = 0; j < this.headers.length; j++) {
                data[i].push(null)
            }
        }
        return data;
    }

    async getMetaSheetData() {
        this.flag.metaSheetLoading = true;
        let sheetData:any = [];
        try {
            let response = await this.metaSheetService.getZetaSheetData(this.metasheetId);
            if(MetaSheetUtil.isOk(response)) {
                let {zetaMetaTable, data} = response.data.content;
                sheetData = data;
                this.metaSheetRow = MetaSheetUtil.processSingleItem(zetaMetaTable);
                this.sheetName = this.metaSheetRow.metaTableName;
                let {columnTypeMap, columns} = WorkspaceSrv.getMetaSheetColumn(this.metaSheetRow!.schemeConfig!.tableData);
                this.columnTypeMap = columnTypeMap;
                this.columns = columns;
            } else {
                MetaSheetUtil.showMessage(this, MSG_TYPE.ERROR, response.data.msg);
            }
        }
        catch(e) {
            this.flag.metaSheetLoading = false;
            MetaSheetUtil.showMessage(this, MSG_TYPE.ERROR, e.message);
        }
        sheetData = _.map(sheetData, (d:any) => {
            let arr:any = [];
            arr.push(d[_ID_]);
            arr.push(d[_VERSION_]);
            for(let j = 0; j < this.headers.length; j++) {
                if(this.columnTypeMap[j+2]) {
                   arr.push(this.toThousands(d[this.headers[j]]))
                } else {
                    arr.push(d[this.headers[j]])
                }
            }
            return arr;
        })
        let newRows = this.processedData(100);
        this.metaSheetData = sheetData.concat(newRows);
        this.debounceRender();
    }

    toThousands(num) {
        if(!_.isNil(num) && !_.isNaN(Number(num))) {
            let numStr = num + '';
            let splits = numStr.split(".");
            let value = splits[0];
            let decimal = splits[1];
            let result = value.replace(/(\d)(?=(?:\d{3})+$)/g, '$1,') ;
            if(splits.length > 1) {
                result = result + "." + decimal;
            }
            return result;
        }
        return num;
    }

}
</script>

<style lang="scss" scoped>
@import '@/styles/global.scss';
$grey: #C4C4C4;
$margin10: 10px;
$bfs: 18px;
.sheet-view-container {
    .header {
        display: flex;
        margin-top: $margin10;
        margin-bottom: $margin10;

        .name {
            width: 50%;
            font-size: 20px;
            font-weight: 700;
        }
        .tool {
            flex: 1;
            text-align: right;
            font-size: $input-font-size;
            font-size: $bfs;
            cursor: pointer;

        }
    }

    #spreadsheet {
        overflow: hidden;
        height: calc(100% - 50px);
        margin-right: -10px;
    }

}


/deep/ .htCore td {
    white-space: nowrap;
}


/deep/ textarea.handsontableInput {
    overflow-y: auto !important;
}


</style>
