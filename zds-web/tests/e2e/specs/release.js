const SA = 'sa';
const FILTER = 'preset';
const ETL = "ETL";
const ONETIME = "Onetime";
const NEW_PATH = "/etl/cfg/sa/conf/1111.cfg";
describe('Release', () => {
    before(() => {
        cy.visit("/#/release");
    })

    it('check release page', () => {
        cy.get('#releasePage').should('be.exist');
        cy.get('#step-one').should('be.exist');
        cy.get('#step-two').should('not.be.exist');
        cy.get('#step-three').should('not.be.exist');
        cy.get('#step-four').should('not.be.exist');
        cy.get('#step-five').should('not.be.exist');
        cy.get('#step-six').should('not.be.exist');
    })

    it('check init step one', () => {
        cy.get('#sa-ipt').clear().get('#etl-btn').should('be.disabled');
        cy.get('#next-btn').should('be.disabled');
        cy.get('#uc4-btn').should('be.exist');
        cy.get('#onetime-btn').should('be.exist');
    })

    it('check button', () => {
        cy.get('#sa-ipt').clear().type(SA);
        if (cy.get('#etl-btn').should('be.enabled')) {
            cy.get('#etl-btn').click();
            cy.get('.el-dialog__wrapper').should('be.exist')
                .find('.el-checkbox-group').children().its('length').should('be.gt', 0)
                .get('.el-dialog__headerbtn').click();
        }
        cy.wait(100);
        /*if (cy.get('#uc4-btn').its('0.classList.value').should('eq','el-button el-button--primary is-disabled')) {
            cy.get('#uc4-btn').click();
            cy.get('.el-dialog__wrapper').should('be.exist').get('.el-dialog__headerbtn').click();
        }*/
        if (cy.get('#onetime-btn').should('be.enabled')) {
            cy.get('#onetime-btn').click();
            cy.get('.el-dialog__wrapper').should('be.exist')
                .find('.el-checkbox-group').children().its('length').should('be.gt', 0)
                .get('.el-dialog__headerbtn').click();
        }
    })

    it('check add type', () => {
        cy.get('#etl-btn').click();
        cy.get('.el-checkbox-group').then($el => {
            const $firstItem = $el.children().eq(0).text().trim();
            cy.get('.el-dialog__wrapper').find('[type="text"]').type($firstItem);
        });
        cy.get('.el-checkbox-group').children().its('length').should('be', 1);
        cy.get('.el-dialog__wrapper').get('.el-checkbox__input').first().click()
        cy.get('#add-btn').click();
        cy.get('tbody').children().its('length').should('be', 1);

        cy.get('#onetime-btn').click();
        cy.get('.el-dialog__wrapper').get('.el-checkbox__input').first().find('[type="checkbox"]').should('have.disabled', 'disabled');
        cy.get('.el-dialog__wrapper').get('.el-checkbox__input').eq(1).click();
        cy.get('#add-btn').click();
        cy.get('tbody').children().its('length').should('be', 2);

        cy.get('tbody').children().eq(0).then($el => {
            expect($el.children().eq(0).find('.cell').text().trim()).to.equal(ETL);
            const $path = $el.children().eq(1).find('span').text().trim();
            $el.children().eq(2).find('button').eq(0).click();
            $el.children().eq(2).find('button').eq(3).click();
            expect($el.children().eq(1).find('span').text().trim()).to.equal($path);
            //$el.children().eq(2).find('button').eq(1).click();
            //expect($el.children().eq(1).find('span').text().trim()).to.equal(NEW_PATH);
        });
        cy.get('tbody').children().eq(0).children().eq(2).find('button').eq(0).click();
        cy.get('tbody').children().eq(0).children().eq(1).find('[type="text"]').clear().type(NEW_PATH);
        cy.get('tbody').children().eq(0).children().eq(2).find('button').eq(1).click();

        /*cy.get('tbody').children().eq(1).then($el => {
            expect($el.children().eq(0).find('.cell').text().trim()).to.equal(ONETIME);
        });*/
    })
})