const TIMESTAMP = Cypress.moment().format('x')
const NEW_NB_NAME = `E2E_Test_${TIMESTAMP}`
const CRT_CNT = 5;
const $ = Cypress.$;
/**
 * Drop all notebook script
 * DELETE FROM `zeta_notebook` WHERE `nt` = 'zeta_tester' AND `title` LIKE 'E2E_Test_%'
 */
describe('Zeta repository', () => {
    before(() => {
        cy.visit("#/repository");

    })
    it('Create SPARK notebook', () => {
        /** mark create new nb  */
        cy.server();
        cy.route({
            method: 'POST',
            url: '/notebook/notebooks'
        }).as("createNotebook")

        let i = 0;
        while(i < CRT_CNT){
            const NB_NAME = `${NEW_NB_NAME}_NO${i++}`
            cy.get("#createBtn").click();
            cy.wait(100)
            .get('.crt-notebook-dialog')
            .should('be.exist')
            .should('be.visible');
            cy.get("#newNBName").clear().type(NB_NAME);
            cy.get('.crt-notebook-dialog .interpreter-select').click().wait(300);
            cy.get('.interpreter-option').contains('SPARK SQL').click();
            cy.get(".crt-notebook-dialog .dialog-footer button.el-button.el-button--primary").click()
            cy.wait('@createNotebook')
            .wait(300)

            cy.get('.crt-notebook-dialog').should('not.visible')
            /** check success alert */
            cy.get('.crt-success-message')
            .should('be.exist')
            .should('have.class','el-message--success')
            .should('be.visible');
            /** check table display */
            cy.get("#pane-notebook > div > div.el-table__body-wrapper > table tbody > tr")
            .contains(NB_NAME)
            .should('be.exist')

        }
    })
    it('Create SPARK notebook with duplicate name', () => {
        /** mark create new nb  */
        cy.server();
        cy.route({
            method: 'POST',
            url: '/notebook/notebooks'
        }).as("createNotebook")

        const NB_NAME = `${NEW_NB_NAME}_NO0`;
        // const NB_NAME = 'E2E_Test_1552034335776_NO0';
        cy.get("#createBtn").click();
        cy.wait(100)
        .get('.crt-notebook-dialog')
        .should('be.exist')
        .should('be.visible');
        cy.get("#newNBName").clear().type(NB_NAME);
        cy.get('.crt-notebook-dialog .interpreter-select').click().wait(300);
        cy.get('.interpreter-option').contains('SPARK SQL').click();
        cy.get(".crt-notebook-dialog .dialog-footer button.el-button.el-button--primary").click()
        cy.wait('@createNotebook')
        .wait(300)
        cy.get('.crt-fail-message')
        .should('be.exist')
        .should('have.class','el-message--error')
        .should('be.visible')

        /** manually close dialog */
        cy.get('.crt-notebook-dialog .el-dialog__header .el-dialog__close')
        .click()
        .wait(100)
        cy.get('.crt-notebook-dialog').should('not.visible')

    })
    it('Rename notebook',() => {
        /** handler error in repositpry page
         *  caused by EL-table
         */
        cy.on('uncaught:exception', (err, runnable) => {
            return false
        })
        cy.server();
        cy.route({
            method: 'PUT',
            url: '/notebook/notebooks'
        }).as("renameNotebook")
        const NB_NAME = `${NEW_NB_NAME}_NO0`;
        const NB_NAME_RENAME = `${NEW_NB_NAME}_RENAME`;
        cy.get("#pane-notebook > div > div.el-table__body-wrapper > table tbody > tr")
        .contains(NB_NAME)
        .should('be.exist')
        .then(($el) => {
            cy.wrap($el)
            .parentsUntil('tbody')
            .find('td:nth-child(1) > div.cell')
            .click()
            cy.get('#renameBtn')
            .click()
            .wait(300);
            cy.get('.rename-message-box')
            .should('be.exist')
            .should('be.visible')
            .find('input')
            .clear()
            .type(NB_NAME_RENAME)

            cy.get('.rename-message-box .el-message-box__btns > .el-button--primary')
            .should('be.exist')
            .click()
            .wait('@renameNotebook')

            cy.get('.rename-success-message')
            .should('be.exist')
            .should('have.class','el-message--success')
            .should('be.visible')
            .should('to.contain','Successfully rename.');

            cy.get("#pane-notebook > div > div.el-table__body-wrapper > table tbody > tr")
            .contains(NB_NAME)
            .should('not.exist')

            cy.get("#pane-notebook > div > div.el-table__body-wrapper > table tbody > tr")
            .contains(NB_NAME_RENAME)
            .should('be.exist')
        })
    })
    it('Create Teradata notebook',() => {
        /** mark create new nb  */
        cy.server();
        cy.route({
            method: 'POST',
            url: '/notebook/notebooks'
        }).as("createNotebook")
        let i = 0;
        while(i < CRT_CNT){
            const NB_NAME = `${NEW_NB_NAME}_TD_NO${i++}`
            cy.get("#createBtn").click();
            cy.wait(100)
            .get('.crt-notebook-dialog')
            .should('be.exist')
            .should('be.visible');
            cy.get("#newNBName").clear().type(NB_NAME);

            cy.get('.crt-notebook-dialog .interpreter-select').click().wait(300);
            cy.get('.interpreter-option').contains('TD SQL').click();
            cy.get(".crt-notebook-dialog .dialog-footer button.el-button.el-button--primary").click()
            cy.wait('@createNotebook')
            .wait(300)

            cy.get('.crt-notebook-dialog').should('not.visible')
            /** check success alert */
            cy.get('.crt-success-message')
            .should('be.exist')
            .should('have.class','el-message--success')
            .should('be.visible')
            /** check table display */
            cy.get("#pane-notebook > div > div.el-table__body-wrapper > table tbody > tr")
            .contains(NB_NAME)
            .should('be.exist')
        }

    })
    it('Delete all nb created by e2e', () => {
        /** handler error in repositpry page
         *  caused by EL-table
         */
        cy.on('uncaught:exception', (err, runnable) => {
            return false
        })

        cy.server();
        cy.route({
            method: 'DELETE',
            url: '/notebook/notebooks/*'
        }).as("deleteNotebook")
        

        const loopRemove = (i) => {
            if(i >= CRT_CNT * 2) return;
            cy.get("#pane-notebook > div > div.el-table__body-wrapper > table tbody > tr")
            .contains(NEW_NB_NAME).first()
            .should('be.exist')
            .then( $el => {
                const name = $el.text().trim();
                cy.wrap($el)
                .parentsUntil('tbody')
                .find('td:nth-child(1) > div.cell')
                .click()
                cy.get('#deleteBtn').click();

                /** assert message box exist */ 
                cy.get(".del-file-message")
                .should('be.visible')
                /** confirm del */
                .get('div.el-message-box__btns > button.el-button--primary')
                .click()

                cy.wait('@deleteNotebook')
                .wait(100)
                cy.get("#pane-notebook > div > div.el-table__body-wrapper > table tbody > tr")
                .contains(name)
                .should('not.exist');
                cy.log('rm nb No:' + i)
                loopRemove(++i);
            });
        }
        loopRemove(0)
    })
  })
  