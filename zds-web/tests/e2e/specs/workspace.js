const PRESET_NB_SPARK = 'preset_0'
const PRESET_NB_TD = 'preset_1'
const METADATA_LIST_RESPONSE = {
    "took":8,
    "timed_out":false,
    "_shards":{
        "total":3,
        "successful":3,
        "skipped":0,
        "failed":0
    },
    "hits":{
        "total":30102,
        "max_score":null,
        "hits":[{
            "_index":"metadata_tv_summary",
            "_type":"summary",
            "_id":"dw_users_info",
            "_version":283,
            "_score":null,
            "_source":{
                "name":"dw_users_info",
                "row_type":"table,view",
                "platform":"Apollo,Ares,Hercules,Hopper,Mozart",
                "desc":"Stores the information of users who have registered on any of the eBay sites. Users can be buyers or sellers.This version of the view does not contain PII (see PII Data Element Classification ). Use the PRS_SECURE_V.DW_USERS (Users) (Mozart or Hopper) versions of this table if you need PII.The corresponding table in Hadoop will have PII columns encrypted.  \n",
                "total_score":1317
            },
            "sort":[1317]
        }]
    }
}
var tabCnt = 0;
describe('Zeta workspace', () => {
    before(() => {
        cy.visit("/#/notebook")
        tabCnt = 0;
    })
    it('close workspace tabs', () => { 
        /** close all current tabs */
        if(Cypress.$('.ws-tabs-nav-bar > ul').children().length > 0){
            cy.get('.ws-tabs-nav-bar > ul').children()
            .each( $el => {
                cy.wrap($el).find('.tab-close .zeta-icon-close').click();
            })
        }
        cy.get('.ws-tabs-nav-bar > ul').children().should('have.length',0)
    })
    it('open workspace tab in workspace', () => {
        /**
         * Element-ui is based on popper.js 
         * which need be triggered by @mouseenter event
         */
        cy.get("div.ws-tabs-nav-bar .new").trigger('mouseenter')
        .wait(300)

        cy.get(".workspace-add-drop-down li:nth-child(1)")
        .should('be.exist')
        .should('be.visible')
        .click()

        cy.wait(300).url().should('include','#/repository')

        cy.get("#pane-notebook > div > div.el-table__body-wrapper > table tbody > tr")
        .contains(PRESET_NB_SPARK).click()
        .wait(300)
        .url().should('include','#/notebook');

        cy.wait(500)
        .get('.ws-tabs-nav-bar > ul > li').first()
        .should('have.class','active')
        .should('contain',PRESET_NB_SPARK)
        .should('contain','SPARK')

        cy.get('.nb-tabs-wrapper')
        .find('.workspace-container')
        .should('have.class','notebook');
        tabCnt ++;
    })
    it('open workspace tab in repo', () => {
        /** handler error in repositpry page
         *  caused by EL-table
         */
        cy.on('uncaught:exception', (err, runnable) => false);


        cy.get('[title="Repository"] > .nav-item-icon').click()
        .wait(300)
        .url()
        .should('include','#/repository');

        cy.get("#pane-notebook > div > div.el-table__body-wrapper > table tbody > tr")
        .contains(PRESET_NB_TD).click()
        .wait(300)
        .url().should('include','#/notebook');

        cy.wait(500)
        .get('.ws-tabs-nav-bar > ul > li').last()
        .should('have.class','active')
        .should('contain',PRESET_NB_TD)
        .should('contain','TD')

        cy.get('.nb-tabs-wrapper')
        .find('.workspace-container')
        .should('have.class','notebook');
        tabCnt ++;
    })

    it('open sql convert',() => {
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
        tabCnt ++;
    })
    it('open datamove',() => {
        cy.get('[title="Data Move"] > .nav-item-icon').click();

        /** check tabs */
        cy.wait(500)
        .get('.ws-tabs-nav-bar > ul > li').last()
        .should('have.class','active')
        .should('contain', 'DataMove')

        /** check content */
        cy.get('.nb-tabs-wrapper')
        .find('.workspace-container')
        .should('have.id','dataMove');
        tabCnt ++;
    })

    it('open datavalidation',() => {
        cy.get('[title="Data Validation"] > .nav-item-icon').click();

        /** check tabs */
        cy.wait(500)
        .get('.ws-tabs-nav-bar > ul > li').last()
        .should('have.class','active')
        .should('contain', 'DataValidation')

        /** check content */
        cy.get('.nb-tabs-wrapper')
        .find('.workspace-container')
        .should('have.id','validation');
        tabCnt ++;
    })
    it('open metadata',() => {
        cy.server();
        /** define ajax request */
        cy.route({
            method: 'POST',
            url: '/metadata-es/metadata_tv_summary/_search',
            response: METADATA_LIST_RESPONSE
        }).as('metadataListQuery');
        cy.route('/notebook/doemetadata/tables?name*').as('metadataInfoQuery')
        cy.route('/notebook/doemetadata/sampleQueries/*').as('metadataSampleQueryQuery')


        cy.get('[title="Metadata"] > .nav-item-icon').click()
        cy.wait('@metadataListQuery').wait(100);

        cy.get('div.nav > div.nav-metadata.nav-subview')
        .should('be.exist')
        .should('be.visible')
        .find('.nav-subview-content > .meta-display')
        .find('ul > li').first().click()

        cy.wait('@metadataInfoQuery').wait('@metadataSampleQueryQuery')
        .wait(300)

        /** check tabs */
        cy.wait(500)
        .get('.ws-tabs-nav-bar > ul > li').last()
        .should('have.class','active')
        .should('contain', 'dw_users_info'.toUpperCase())

        /** check content */
        cy.get('.nb-tabs-wrapper')
        .find('.workspace-container')
        .should('have.class','metadata');
        tabCnt ++;
    })
    it('click each tab', () => {
        let i = 0;
        while(i < tabCnt){
            cy.get('.ws-tabs-nav-bar > ul > li').eq(i++)
            .click()
            .wait(100)
            .should('have.class','active');
        }
        
    })
    it('close each tab', () => {
        let i = 0;
        cy.get('.ws-tabs-nav-bar > ul > li').last().as('currentActiveTab').click()

        while(i < tabCnt){
            cy.get('@currentActiveTab')
            .should('have.class','active')
            .find('.tab-close .zeta-icon-close')
            .click()
            .wait(100)
            .should('not.exist');
            i++;
        }
        
    })
  })
  