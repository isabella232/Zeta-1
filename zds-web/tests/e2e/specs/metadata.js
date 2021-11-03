const TABLE_NAME = 'dw_sites';
describe('Zeta Metadata', () => {
    before(() => {
        cy.visit("/#/repository")
    })
    it('open metadata',() => {
        cy.server();
        /** define ajax request */
        cy.route({
            method: 'POST',
            url: '/metadata-es/metadata_tv_summary/_search'
        }).as('metadataListQuery');

        cy.get('[title="Metadata"] > .nav-item-icon').click()
        cy.wait('@metadataListQuery').wait(100);

        cy.get('.nav-metadata .nav-subview-content .search-input > .el-input__inner')
        .should('be.exist')
        .clear().type(TABLE_NAME)
        .wait('@metadataListQuery').wait(100);

        cy.get('div.nav > div.nav-metadata.nav-subview')
        .should('be.exist')
        .should('be.visible')
        .wait(100)

        /** check title */
        cy.get('.nav-subview-content > .meta-display > ul').then($el => {
            expect($el.children().length).to.be.at.least(1);
            const $itemTitle = $el.children().eq(0).find('.meta-name');
            expect($itemTitle.text().trim()).to.equal(TABLE_NAME.toUpperCase())
        })
    })
    it('go to metadata detail', () => {
        cy.server();
        cy.route('/api/asset/tableInfoService?table=*').as('metadataFromDoe')
        cy.route('/notebook/doemetadata/tables?name*').as('metadataInfoQuery')
        cy.route('/notebook/doemetadata/sampleQueries/*').as('metadataSampleQueryQuery')

        cy.get('.nav-subview-content > .meta-display > ul > li:nth-child(1)').click()

        cy.wait('@metadataInfoQuery').wait('@metadataFromDoe').wait('@metadataSampleQueryQuery')
        .wait(300)
        .url().should('include','#/notebook')
        /** check tabs */
        .get('.ws-tabs-nav-bar > ul > li').last()
        .should('have.class','active')
        .should('contain', TABLE_NAME.toUpperCase())

        /** check content */
        cy.get('.nb-tabs-wrapper')
        .find('.workspace-container')
        .should('have.class','metadata')

        cy.get('.metadata-tools-bar > .metadata-tabs li')
        .contains('Overview')
        .should('have.class','active');
    })
    it('metadata columns',() => {
        cy.get('.metadata-tools-bar > .metadata-tabs li')
        .contains('Columns')
        .click()
        .wait(100)
        .should('have.class','active');

        cy.get('.table-container > table > tbody > tr').eq(0)
        .find('td').eq(0)
        .should('contain','SITE_ID')

        // search for SITE_NAME
        cy.get('.columns-display .search-box .el-input__inner').clear().type('SITE_NAME')
        cy.get('.table-container > table > tbody').contains('SITE_ID').should('not.exist')
        cy.get('.table-container > table > tbody > tr').eq(0)
        .find('td').eq(0)
        .should('contain','SITE_NAME')
    })
    it('metadata sample query', () => {
        /** handler error
         *  caused by EL-table
         */
        cy.on('uncaught:exception', (err, runnable) => {
            return false
        })
        cy.get('.metadata-tools-bar > .metadata-tabs li')
        .contains('Sample Queries')
        .click()
        .wait(100)
        .should('have.class','active');
    })
  })