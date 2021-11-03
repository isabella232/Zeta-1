const Util = require("../util")
/** variables */
const TABLE_NAME = 'gdw_tables.dw_sites';
const INPUT_SCRIPT = 'select count(1) from gdw_tables.dw_sites'
const OUTPUT_SCRIPT = `selectcount(1)from\$\{gdwDB\}.dw_sites`;


const CONVERT_TABLE_RESPONSE = {"id":397991,"name":"dw_sites","tableName":"dw_sites","databaseName":"gdw_tables","platformName":"mozart","rowCount":19832,"tableSize":3966464,"tableType":"target","etlId":"dw_dim.dw_sites","wklyProcessingCpu":0.5600000000000002,"wklyTotCpu":48.582709677419345,"maxLvlNum":1,"t30dDistinctUserCnt":261,"t30dDistinctUsageCnt":55,"sqlFileCnt":1,"sqlStatementCnt":2,"sqlLineCnt":80,"ppiColumns":"NULL","priIndexColumns":"SITE_ID","secIndexColumns":"NULL","saCode":"dw_dim","saName":"LKP","saUc4Path":"LKP","teamName":"Core Insights","batchAccount":"b_lkp","hdBatchAccount":"b_lkp","ownerNt":"zzhou3","devManagerNt":"rofu","directDownstreamTblCnt":72,"totUpdTgtStepCnt":1,"totInsTgtStepCnt":1,"inclXfrYn":"NULL","dssFlag":"Y","uowFlag":"Y","extractUowFlag":"Y","inclRelationalFlag":"Y","useAsSourceFlag":"Y","useAsTargetFlag":"Y","hostList":"NULL","postExtractJobsFlag":"NULL","cndtlReformatFlag":"NULL","cndtlExtractReformatFlag":"NULL","cndtlUnloadReformatFlag":"NULL","vegaStatus":"Auto Migration","vegaSubStatus":"Ready For Cutover","steFlag":"N","uniqueFlag":"N","plannerWatchFlag":"Y","uc4Path":"/CORE/LKP","autoFlag":"Y","controllerConvertStatus":"succ","cutoverCompleteFlag":"Y","readyForUseFlag":"Y","dqReconSuccCount":3,"lstFnlWrkngTbl":"working.stg_sites_w","tableStatus":{"platformName":"mozart","databaseName":"gdw_tables","tableName":"dw_sites","rowCount":19832,"tableSize":3966464,"tableType":"target","etlId":"dw_dim.dw_sites","wklyProcessingCpu":0.56,"wklyTotCpu":48.5827096774194,"maxLvlNum":1,"t30dDistinctUserCnt":261,"t30dDistinctUsageCnt":55,"sqlFileCnt":1,"sqlStatementCnt":2,"sqlLineCnt":80,"ppiColumns":"NULL","priIndexColumns":"SITE_ID","secIndexColumns":"NULL","saCode":"dw_dim","saName":"LKP","saUc4Path":"LKP","uc4Path":"/CORE/LKP","teamName":"Core Insights","batchAccount":"b_lkp","hdBatchAccount":"b_lkp","ownerNt":"zzhou3","devManagerNt":"rofu","directDownstreamTblCnt":72,"totUpdTgtStepCnt":1,"totInsTgtStepCnt":1,"inclXfrYn":"N","dssFlag":"Y","uowFlag":"Y","extractUowFlag":"Y","inclRelationalFlag":"Y","useAsSourceFlag":"Y","useAsTargetFlag":"Y","hostList":"NULL","postExtractJobsFlag":"N","cndtlReformatFlag":"N","cndtlExtractReformatFlag":"N","cndtlUnloadReformatFlag":"N","logDt":"20190311","vegaStatus":"Auto Migration","vegaSubStatus":"Ready For Cutover","steFlag":"N","uniqueFlag":"N","cutoverCompleteFlag":"Y","readyForUseFlag":"Y","autoFlag":"Y","skipAutoReason":"","platformFlag":"both_mozart_and_vivaldi","primaryPlatformYn":"Y","staticLkp":false,"maintainLkp":false,"ddlReady":true,"ddlErrorMessage":null,"dmFirstRoundReady":true,"dmUowFrom1":"19691231000000","dmUowTo1":"19691231000000","dmExeCount":0,"dmSecondRoundReady":true,"dmProcessTrack":0,"dmUowFrom2":"20180729000000","dmUowTo2":"20180730000000","dmInProd":true,"dmErrorMessage":null,"sqlConvertStatus":true,"lstFnlWrkngTbl":"working.stg_sites_w","shiftLoadFlag":false,"sqlConvertErrorMessage":null,"sqlHandlerStatus":false,"sqlHandlerErrorMessage":null,"dataValidationStatus":true,"dataValidationErrorMessage":null,"dqreconId":"1914","controllerConvertStatus":"succ","controllerErrorMessage":null,"jobOptimizerStatus":"not_start","jobOptimizerErrorMessage":null,"cfgGenStatus":"not_start","cfgGenErrorMessage":null,"uc4GenStatus":"not_start","uc4GenErrorMessage":null,"controlFailedStep":"PipleGernerator","controlCurrentStep":"PipleGernerator","releaseId":null,"releaseReady":true,"releaseStatus":"not start","ddlInProd":true,"eltInProd":false,"uc4ScheduleInProd":"0","dqReconSuccCount":3,"cutoverInProd":"not start","cutoverSystem":"0","gnrtdSaTopContainer":"C_LKP_ADPO_SP2,C_LKP_ADPO_TD7,C_LKP_ADPO_HD1","gnrtdTblUowContainer":"C_DW_SITES_ADPO_SP2_UOW,C_DW_SITES_ADPO_TD7_UOW,C_DW_SITES_ADPO_HD1_UOW","gnrtdTblEnvContainer":"C_DW_SITES_ADPO_SP2,C_DW_SITES_ADPO_TD7,C_DW_SITES_ADPO_HD1","gnrtdStmContainer":"C_DW_SITES_STM","gnrtdSttJobs":"","gnrtdStmJobs":"DW_SITES_STM","gnrtdWfJobs":"","uc4MdfdIndctr":"","creDate":1524644815000,"updDate":1546298601000,"fullName":"gdw_tables.dw_sites"},"upstreamTbls":[{"id":403392,"name":"stg_sites_w","tableName":"stg_sites_w","databaseName":"working","platformName":"mozart","rowCount":0,"tableSize":3954688,"tableType":"staging","etlId":"dw_dim.dw_sites","ppiColumns":"NULL","priIndexColumns":"NULL","secIndexColumns":"NULL","teamName":"NULL","batchAccount":"b_lkp","ownerNt":"NULL","devManagerNt":"NULL","inclXfrYn":"NULL","dssFlag":"NULL","uowFlag":"N","extractUowFlag":"N","inclRelationalFlag":"NULL","useAsSourceFlag":"Y","useAsTargetFlag":"NULL","hostList":"phxdpeetl023-be.phx.ebay.com","postExtractJobsFlag":"NULL","cndtlReformatFlag":"NULL","cndtlExtractReformatFlag":"NULL","cndtlUnloadReformatFlag":"NULL","vegaStatus":"NULL","vegaSubStatus":"NULL","steFlag":"N","uniqueFlag":"N","fullName":"working.stg_sites_w"}],"sqlScripts":[{"id":1415450,"saCode":"dw_dim","sqlFileName":"dw_dim.dw_sites.ins_upd.sql","seqNum":1,"cntSqlStatement":2,"cntSqlLine":80,"cntInsTgtStep":1,"cntUpdTgtStep":1,"flagConvertable":"y","flagAutoGen":"n","tableName":"dw_sites","databaseName":"gdw_tables","platformName":"mozart","uc4JobName":"dw_sites_ins_upd","uc4VarValues":"&UOW_ID#=C_DW_DIM_MAIN_UOW_TD2\\n&UOW_PERSIST#=true","cntProcessedRow":0,"l30dExecTimes":31}],"uc4Jobs":[{"id":1501566,"tableName":"dw_sites","databaseName":"gdw_tables","platform":"mozart","uc4JobName":"dw_sites_ins_upd","uc4JobPlanName":"C_DW_DIM_TR_DW_SITES","uc4JobType":"TR","uc4JobProcess":"$DW_EXE/target_table_load_handler.ksh dw_dim.dw_sites &Dual# dw_dim.dw_sites.ins_upd.sql -f &UOW_FROM# -t &UOW_TO#","uc4JobPreProcess":":INC INTERFACE.AB.TARGET_TABLE_LOAD","uc4JobPostProcess":"!************************** Chain / Module Conditions  **************************\\n!\\n!************************** Chain / Module Arguments **************************","uc4VarList":"&UOW_ID#=C_DW_DIM_MAIN_UOW_TD2\\n&UOW_PERSIST#=true","uc4TopContainerName":"C_DW_BATCH_SLA_SECONDARY","uc4ScheduledTgt":"JSCH.DW","uc4Client":"1000","uc4Path":"/CORE/LKP","flagActive":"y"}],"fullName":"gdw_tables.dw_sites"};
const TABLE_INFO_RESPONSE = {"dw_dim.dw_sites.ins_upd.sql":["gdw_tables.dw_sites","working.stg_sites_w"]}

describe('SQL convert', () => {
    before(() => {
        cy.visit("/#/notebook")
    })
    it('open sql convert', () => {
        cy.get('[title="SQL Convert"] > .nav-item-icon').click();

        /** check tabs */
        cy.wait(500)
        .get('.ws-tabs-nav-bar > ul > li').last()
        .should('have.class','active')
        .should('contain', 'SQL Convert')

        /** check content */
        cy.get('.nb-tabs-wrapper')
        .find('.workspace-container')
        .should('have.id','editorContainer');
    })
    it(`convert table => search tabel \`${TABLE_NAME}\``,() => {
        cy.server();
        cy.route('/notebook/metadata/findFDTWithSQLScripts?platform=MOZART&dbName=gdw_tables&tblName=dw_sites',CONVERT_TABLE_RESPONSE).as('findTable')
        cy.route({
            url:'/notebook/SQLConvert/getTdTableSource',
            method: 'POST',
            response: TABLE_INFO_RESPONSE
        }).as('getTableSource')
        cy.get('.table-info-input .el-input__inner').clear().type(TABLE_NAME);
        cy.get('.table-info-input .el-input-group__append > .el-button').click()
        .wait('@findTable')
        .wait('@getTableSource')

        cy.get('.input-script .table-info').as('tableInfo').should('be.exist').end()
        .get('.info-sa').should('be.exist').and('contain','SA').end()
        .get('.info-owner').should('be.exist').and('contain','Owner').end()
        .get('.info-status').should('be.exist').and('contain','Status').end()
    })
    it('convert table => check search result', () => {
        cy.get('.input-script .script-list > .el-tree > .el-tree-node .el-tree-node__expand-icon')
        .should('be.exist')
        .click()
        .end()
        .get('.input-script .script-list > .el-tree > .el-tree-node .el-tree-node__children')
        .each( $el => {
            cy.log($el.text())
            expect($el.text()).to.exist;
        })
    })
    it('convert table => check convert result',() => {
        cy.server();
        cy.route('/notebook/metadata/findFDTWithSQLScripts').as('findFDT')
        cy.route({
            url:'/notebook/SQLConvert/convert',
            method:'POST'
        }).as('convert')
        cy.get('.cvt-btn').click()
        .wait('@convert')
        cy.get('.out-file').should('be.exist')
        .get('#pane-0')
        .should('be.visible')
        .contains('dw_dim.dw_sites.ins_upd.spark.sql')
    })
    it('convert table => save to repo', () => {
        cy.server();
        cy.route({
            method:'POST',
            url: "/notebook/SQLConvert/saveToNotebook"
        }).as("saveToRepo")
        cy.get('#sv2Repo')
        .click()
        .wait("@saveToRepo")
        cy.get('.out-file').should('be.exist')
        .get('#pane-0')
        .should('be.visible')
        .find('.el-icon-error')
        .should('be.visible')
    })

    it('convert Sql script => revert Tool', () => {

        cy.get('.rvt-btn').click();
        cy.get('.left-editor .editor-container .code-display').should('be.visible')

    })
    it('convert Sql script => convert script', () => {
        cy.server()
        cy.route({
            method: "POST",
            url:"/notebook/SQLConvert/manualconvertsql"
        }).as("convertSQL")

        cy.get('.left-editor .editor-container .code-display > .CodeMirror textarea')
        .type(INPUT_SCRIPT,{force: true})
        cy.get('.cvt-btn')
        .click()
        .wait("@convertSQL")
        cy.get(".right-editor .editor-container .code-display > .CodeMirror")
        .then($el => {
            const text = Util.getCodeMirrorValue($el)
            console.log(text.trim())
            console.log(OUTPUT_SCRIPT.trim())
            expect(text.trim()).to.equal(OUTPUT_SCRIPT.trim()) 
        })
    })
})