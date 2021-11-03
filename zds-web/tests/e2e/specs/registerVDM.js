const VALIDATE_BTN = "Validate";
const DB_ALLREADY_REGISTERED = "P_DATA_ASSET_T";
const TABLE_ALLREADY_REGISTERED = "BIG_QUERY_LIST";
const DB = "P_WENJSUN_1";//P_WENJSUN_1(Hermes), DEFAULT(Hercules)
const TABLE = "IES_CLICK_SUM";//IES_CLICK_SUM, AMS_CLICK_BKP
const $random = parseInt(Math.random() * 1000000);

describe('Metadata', () => {
    before(() => {
        cy.visit("/#/registervdm");
    })

    it('step one validate btn', () => {
        cy.get('.step-one-content').find('.validate-btn').should('be.exist').then($el => {
            expect($el.text()).to.equal(VALIDATE_BTN);
        }).click();

        cy.get('.step-one-content').find('.el-form-item__error').its('length').should('eq', 3);
    })

    it('step one next btn', () => {
        cy.get('.button-group').find('.next-btn').should('be.exist').click();

        cy.get('.step-one-content').find('.el-form-item__error').its('length').should('eq', 6);
    })

    it('check error message', () => {
        cy.get('.owner-content .el-form').eq(0).children().eq(0).find('[type="text"]').clear().type("Jin bingsheng");
        cy.get('.button-group').find('.next-btn').should('be.exist').click();
        cy.get('.owner-content .el-form .el-form-item').eq(0).should('have.class', 'is-error');
        cy.get('.owner-content .el-form').eq(0).children().eq(0).find('[type="text"]').clear().type("Jin bingsheng");
        cy.wait(3000);
        cy.get('.el-autocomplete-suggestion li').eq(0).click();
        cy.get('.button-group').find('.next-btn').should('be.exist').click();
        cy.get('.owner-content .el-form .el-form-item').eq(0).should('not.have.class', 'is-error');

        cy.get('.owner-content .el-form').eq(0).children().eq(1).find('[type="text"]').clear().type("Jin bingsheng");
        cy.get('.button-group').find('.next-btn').should('be.exist').click();
        cy.get('.owner-content .el-form .el-form-item').eq(1).should('have.class', 'is-error');
        cy.get('.owner-content .el-form').eq(0).children().eq(1).find('[type="text"]').clear().type("bjin@ebay.com");
        cy.get('.button-group').find('.next-btn').should('be.exist').click();
        cy.get('.owner-content .el-form .el-form-item').eq(1).should('not.have.class', 'is-error');
    })

    it('check validate btn', () => {
        cy.get('.owner-content .el-form').eq(1).children().eq(0).find('.el-checkbox-button__inner').eq(0).click();
        cy.get('.owner-content .el-form').eq(1).children().eq(1).find('[type="text"]').clear().type(DB_ALLREADY_REGISTERED);
        cy.get('.owner-content .el-form').eq(1).children().eq(2).find('[type="text"]').clear().type(TABLE_ALLREADY_REGISTERED);
        cy.get('.step-one-content').find('.validate-btn').click();
        cy.wait(3000);
        cy.get('.owner-content .el-form').eq(1).children().eq(1).find('.el-tag').should('be.exist').children().eq(0).then($el => {
            expect($el.text()).to.equal("Already Registered");
        });
        cy.get('.owner-content .el-form').eq(1).children().eq(1).find('[type="text"]').clear().type(DB);
        cy.get('.owner-content .el-form').eq(1).children().eq(2).find('[type="text"]').clear().type(TABLE);
        cy.get('.step-one-content').find('.validate-btn').click();
        cy.wait(3000);
        cy.get('.owner-content .el-form').eq(1).children().eq(1).find('.el-tag').should('be.exist').should('have.class', 'el-tag--danger').children().eq(1).then($el => {
            expect($el.text()).to.equal("Apollo_rno platform does not exist");
        });
        cy.get('.owner-content .el-form').eq(1).children().eq(0).find('.el-checkbox-button__inner').eq(0).click();
        cy.get('.owner-content .el-form').eq(1).children().eq(0).find('.el-checkbox-button__inner').eq(1).click();
        cy.get('.step-one-content').find('.validate-btn').click();
        cy.wait(3000);
        cy.get('.owner-content .el-form').eq(1).children().eq(1).find('.el-tag').should('be.exist').should('have.class', 'el-tag--success').children().eq(1).then($el => {
            expect($el.text()).to.equal("Success");
        });
    })

    it('register vdm', () => {
        cy.get('.owner-content .el-form').eq(2).children().eq(0).find('textarea').clear().type("auto test case " + $random);
        cy.get('.owner-content .el-form').eq(2).children().eq(2).find('textarea').clear().type("auto test case");
        cy.get('.button-group').find('.next-btn').should('be.exist').click();
        cy.wait(3000);
        cy.get('.button-group').find('.pre-btn').should('be.exist');
        cy.get('.button-group').find('.next-btn').should('be.exist').click();
        cy.wait(3000);
        cy.get('.button-group').find('.pre-btn').should('be.exist');
        cy.get('.button-group').find('.submit-btn').should('be.exist').click();
        cy.wait(3000);
        cy.get('.dialog-header').should('be.exist').find('span').then($el => {
            expect($el.text()).to.equal("Success!");
        });
        cy.get('.dialog-footer').eq(0).children().eq(1).click();
        cy.wait(3000);

        // check register info in metadata overview
        cy.get('.title').children().eq(0).then($el => {
            expect($el.text().trim()).to.equal(DB + "." +TABLE);
        });
        cy.get('.overview').find('.metadata').find('.display').find('tr').eq(0).find('span').then($el => {
            expect($el.text()).to.equal("auto test case " + $random);
        });

        // check register info in metadata search
        cy.get('.zeta-icon-medatada').click();
        cy.get('.search-input').find('[type="text"]').clear().type(DB + "." +TABLE);
        cy.wait(3000);
        cy.get('.meta-display ul').children().eq(0).find('.meta-desc').then($el => {
            expect($el.text().toLowerCase()).to.equal("auto test case " + $random);
        });
    })
})