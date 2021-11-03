const DS_BE_ASSET_USAGE_SD = "test_table";
const PLATFORM = "Hercules";
const DB_NAME = "test_db";
const SUBJECT_AREA = "ACCT";
const TABLE_DESCRIPTION = "test add descrition";
const JIRA = "DWRM-1000";
const HDFS = "/test";
const COL = "COL1";
const DATA_TYPE = "String";
const COL_DESC = "col desc";
const NEW = "new";
const FALSE = "FALSE";
const TRUE = "TRUE";
const UPDATE_TABLE_DESCRIPTION = "test update descrition";
const UPDATE_SUBJECT_AREA = "ACCTB";
const UPDATE_JIRA = "DWRM-2000";
const UPDATE_HDFS = "/test/update"
const UPDATE_COL = "COL1_UPDATE";
const UPDATE_DATA_TYPE = "Varchar";
const UPDATE_COL_DESC = "col desc update";
const IMPORT_PLATFORM = "Hercules";
const IMPORT_DB = "GDW_TABLES";
const IMPORT_TABLE = "DW_CK_SHOP_CART_SLCTN_STS";

describe('Da developer', () => {
    before(() => {
        cy.visit("/#/da");
    })

    // home page test case
    it('check init home-page', () => {
        cy.get('.new-btn').should('be.visible');
        cy.get('.el-table').should('be.visible');
        cy.get('.footer .el-pagination').should('be.visible');
    })

    it('check home-page new-btn', () => {
        cy.get('.new-btn').click();

        // developer page show and clear
        cy.get('.content').should('be.visible');
        cy.get('.save-btn').should('be.visible');
        cy.get('.submit-btn').should('be.visible');
        cy.get('.el-form').children().eq(0).find('[type="text"]').should('have.value', ''); // platform
        cy.get('.el-form').children().eq(1).find('[type="text"]').should('have.value', ''); // da
        cy.get('.el-form').children().eq(2).find('[type="text"]').should('have.value', ''); // table
        cy.get('.back-btn').should('be.visible').click();
    })

    it('check home-page search', () => {
        cy.get('.el-table .el-table__header-wrapper').find('[type="text"]').clear().type(DS_BE_ASSET_USAGE_SD);
        cy.get('.el-table .el-table__header-wrapper').find('.el-icon-search').click();
        
        cy.wait(2000);
        cy.get('.el-table .el-table__body-wrapper tbody').children().each($el => {
            expect($el.context.textContent.indexOf(DS_BE_ASSET_USAGE_SD)).to.not.equal(-1);
        })
    })

    it('check developer-page error message && switch platform', () => {
        cy.get('.new-btn').click();
        cy.get('.submit-btn').should('be.visible').click();
        cy.get('.el-form .el-form-item').its('length').should('eq', 8);
        cy.get('.el-form .el-form-item__error').its('length').should('eq', 5);
        cy.get('.el-form').children().eq(0).find('[type="text"]').click();
        cy.get('.el-select-dropdown').should('be.visible');
        cy.get('.el-select-dropdown').eq(1).find('.el-select-dropdown__list li').eq(5).click();
        cy.get('.el-form').children().eq(0).find('[type="text"]').should('have.value', 'Hercules');
        cy.get('.el-form .el-form-item').its('length').should('eq', 9);
        cy.get('.back-btn').should('be.visible').click();
    })

    it('check developer-page add', () => {
        if (cy.get('.new-btn').should('be.visible')) cy.get('.new-btn').click();
        cy.get('.el-form').children().eq(0).find('[type="text"]').click();
        cy.get('.el-select-dropdown').should('be.visible');
        cy.get('.el-select-dropdown').eq(1).find('.el-select-dropdown__list li').eq(5).click();
        cy.get('.el-form').children().eq(1).find('[type="text"]').clear().type(DB_NAME);
        cy.get('.el-form').children().eq(2).find('[type="text"]').clear().type(DS_BE_ASSET_USAGE_SD);
        cy.get('.content').click();
        cy.wait(2000);
        cy.get('.el-form').children().eq(3).click();
        cy.get('.el-select-dropdown').eq(1).find('.el-select-dropdown__list li').eq(0).click();
        cy.get('.el-form').children().eq(4).find('textarea').clear().type(TABLE_DESCRIPTION);
        cy.get('.el-form').children().eq(5).find('[type="text"]').clear().type(JIRA);
        cy.get('.el-form').children().eq(6).find('[type="text"]').clear().type(HDFS);
        cy.get('.content .el-table__fixed-header-wrapper .column-add-btn').click();
        cy.get('.content .el-table__body-wrapper tbody').children().its('length').should('eq', 1);
        cy.get('.content .el-table__body-wrapper tbody').children().eq(0).find('textarea').its('length').should('eq', 1);
        cy.get('.content .el-table__body-wrapper tbody').children().eq(0).find('[type="text"]').its('length').should('eq', 7);
        cy.get('.content .el-table__body-wrapper tbody').children().eq(0).find('[type="text"]').eq(0).clear().type(COL);
        cy.get('.content .el-table__body-wrapper tbody').children().eq(0).find('[type="text"]').eq(1).clear().type(DATA_TYPE);
        cy.get('.content .el-table__body-wrapper tbody').children().eq(0).find('textarea').clear().type(COL_DESC);
        cy.get('.submit-btn').should('be.visible').click({force: true});
        cy.wait(2000);
        cy.get('.back-btn').click();
    })

    it('check home-page added-data', () => {
        cy.wait(2000);
        cy.get('.el-table .el-table__body-wrapper tbody').children().eq(0).find('.cell').eq(0).then($el => {
            expect($el.text()).to.equal(PLATFORM.toLowerCase());
        });
        cy.get('.el-table .el-table__body-wrapper tbody').children().eq(0).find('.cell').eq(1).then($el => {
            expect($el.text()).to.equal(DB_NAME);
        });
        cy.get('.el-table .el-table__body-wrapper tbody').children().eq(0).find('.cell').eq(2).then($el => {
            expect($el.text()).to.equal(DS_BE_ASSET_USAGE_SD);
        });
        cy.get('.el-table .el-table__body-wrapper tbody').children().eq(0).find('.cell').eq(3).then($el => {
            expect($el.text()).to.equal(JIRA);
        });
        cy.get('.el-table .el-table__body-wrapper tbody').children().eq(0).find('.cell').eq(4).then($el => {
            expect($el.text()).to.equal(NEW);
        });
    })

    it('check developer-page added-data', () => {
        cy.get('.el-table .el-table__body-wrapper tbody').children().eq(0).find('.cell .detail-btn').click({force: true});
        cy.wait(2000);
        cy.get('.el-form').children().eq(0).find('[type="text"]').should('have.value', PLATFORM);
        cy.get('.el-form').children().eq(1).find('[type="text"]').should('have.value', DB_NAME);
        cy.get('.el-form').children().eq(2).find('[type="text"]').should('have.value', DS_BE_ASSET_USAGE_SD);
        cy.get('.el-form').children().eq(3).find('[type="text"]').should('have.value', SUBJECT_AREA);
        cy.get('.el-form').children().eq(4).find('textarea').should('have.value', TABLE_DESCRIPTION);
        cy.get('.el-form').children().eq(5).find('[type="text"]').should('have.value', JIRA);
        cy.get('.el-form').children().eq(6).find('[type="text"]').should('have.value', HDFS);

        cy.get('.content .el-table__body-wrapper tbody').children().its('length').should('eq', 1);
        cy.get('.content .el-table__body-wrapper tbody').children().eq(0).find('span').eq(0).then($el => {
            expect($el.text()).to.equal(COL);
        });
        cy.get('.content .el-table__body-wrapper tbody').children().eq(0).find('span').eq(1).then($el => {
            expect($el.text()).to.equal(DATA_TYPE);
        });
        cy.get('.content .el-table__body-wrapper tbody').children().eq(0).find('span').eq(2).then($el => {
            expect($el.text()).to.equal(COL_DESC);
        });
        cy.get('.content .el-table__body-wrapper tbody').children().eq(0).find('span').eq(4).then($el => {
            expect($el.text()).to.equal(FALSE);
        });
        cy.get('.content .el-table__body-wrapper tbody').children().eq(0).find('span').eq(5).then($el => {
            expect($el.text()).to.equal(FALSE);
        });
        cy.get('.content .el-table__body-wrapper tbody').children().eq(0).find('span').eq(6).then($el => {
            expect($el.text()).to.equal(FALSE);
        });
        cy.get('.content .el-table__body-wrapper tbody').children().eq(0).find('span').eq(7).then($el => {
            expect($el.text()).to.equal(FALSE);
        });
        cy.get('.back-btn').click();
    })

    it('check home/developer-page update', () => {
        if (cy.get('.new-btn').should('be.visible')) {
            cy.get('.el-table .el-table__body-wrapper tbody').children().eq(0).find('.cell .detail-btn').click({force: true});
            cy.wait(2000);
        }

        cy.get('.el-form').children().eq(3).click();
        cy.get('.el-select-dropdown').eq(1).find('.el-select-dropdown__list li').eq(1).click();
        cy.get('.el-form').children().eq(4).find('textarea').clear().type(UPDATE_TABLE_DESCRIPTION);
        cy.get('.el-form').children().eq(5).find('[type="text"]').clear().type(UPDATE_JIRA);
        cy.get('.el-form').children().eq(6).find('[type="text"]').clear().type(UPDATE_HDFS);

        cy.get('.content .el-table__body-wrapper tbody').children().eq(0).find('[type="text"]').should('not.be.exist');
        cy.get('.content .el-table__fixed-body-wrapper tbody').children().eq(0).find('.cell .zeta-icon-edit').click()
        cy.get('.content .el-table__body-wrapper tbody').children().eq(0).find('[type="text"]').its('length').should('eq', 7);
        cy.get('.content .el-table__body-wrapper tbody').children().eq(0).find('[type="text"]').eq(0).clear().type(UPDATE_COL);
        cy.get('.content .el-table__body-wrapper tbody').children().eq(0).find('[type="text"]').eq(1).clear().type(UPDATE_DATA_TYPE);
        cy.get('.content .el-table__body-wrapper tbody').children().eq(0).find('textarea').clear().type(UPDATE_COL_DESC);
        cy.get('.submit-btn').should('be.visible').click({force: true});
        cy.wait(2000);
        cy.get('.back-btn').click();
    })

    it('check home/developer-page updated-data', () => {
        cy.wait(2000);
        cy.get('.el-table .el-table__body-wrapper tbody').children().eq(0).find('.cell').eq(0).then($el => {
            expect($el.text()).to.equal(PLATFORM.toLowerCase());
        });
        cy.get('.el-table .el-table__body-wrapper tbody').children().eq(0).find('.cell').eq(1).then($el => {
            expect($el.text()).to.equal(DB_NAME);
        });
        cy.get('.el-table .el-table__body-wrapper tbody').children().eq(0).find('.cell').eq(2).then($el => {
            expect($el.text()).to.equal(DS_BE_ASSET_USAGE_SD);
        });
        cy.get('.el-table .el-table__body-wrapper tbody').children().eq(0).find('.cell').eq(3).then($el => {
            expect($el.text()).to.equal(UPDATE_JIRA);
        });
        cy.get('.el-table .el-table__body-wrapper tbody').children().eq(0).find('.cell').eq(4).then($el => {
            expect($el.text()).to.equal(NEW);
        });
    })

    it('check developer-page updated-data', () => {
        cy.get('.el-table .el-table__body-wrapper tbody').children().eq(0).find('.cell .detail-btn').click({force: true});
        cy.wait(2000);
        cy.get('.el-form').children().eq(0).find('[type="text"]').should('have.value', PLATFORM);
        cy.get('.el-form').children().eq(1).find('[type="text"]').should('have.value', DB_NAME);
        cy.get('.el-form').children().eq(2).find('[type="text"]').should('have.value', DS_BE_ASSET_USAGE_SD);
        cy.get('.el-form').children().eq(3).find('[type="text"]').should('have.value', UPDATE_SUBJECT_AREA);
        cy.get('.el-form').children().eq(4).find('textarea').should('have.value', UPDATE_TABLE_DESCRIPTION);
        cy.get('.el-form').children().eq(5).find('[type="text"]').should('have.value', UPDATE_JIRA);
        cy.get('.el-form').children().eq(6).find('[type="text"]').should('have.value', UPDATE_HDFS);

        cy.get('.content .el-table__body-wrapper tbody').children().its('length').should('eq', 1);
        cy.get('.content .el-table__body-wrapper tbody').children().eq(0).find('span').eq(0).then($el => {
            expect($el.text()).to.equal(UPDATE_COL);
        });
        cy.get('.content .el-table__body-wrapper tbody').children().eq(0).find('span').eq(1).then($el => {
            expect($el.text()).to.equal(UPDATE_DATA_TYPE);
        });
        cy.get('.content .el-table__body-wrapper tbody').children().eq(0).find('span').eq(2).then($el => {
            expect($el.text()).to.equal(UPDATE_COL_DESC);
        });
        cy.get('.content .el-table__body-wrapper tbody').children().eq(0).find('span').eq(4).then($el => {
            expect($el.text()).to.equal(FALSE);
        });
        cy.get('.content .el-table__body-wrapper tbody').children().eq(0).find('span').eq(5).then($el => {
            expect($el.text()).to.equal(FALSE);
        });
        cy.get('.content .el-table__body-wrapper tbody').children().eq(0).find('span').eq(6).then($el => {
            expect($el.text()).to.equal(FALSE);
        });
        cy.get('.content .el-table__body-wrapper tbody').children().eq(0).find('span').eq(7).then($el => {
            expect($el.text()).to.equal(FALSE);
        });
        cy.get('.back-btn').click();
    })

    it('check developer-page remove', () => {
        if (cy.get('.new-btn').should('be.visible')) {
            cy.get('.el-table .el-table__body-wrapper tbody').children().eq(0).find('.cell .detail-btn').click({force: true});
            cy.wait(2000);
        }

        cy.get('.content .el-table__fixed-body-wrapper tbody').children().eq(0).find('.cell .zeta-icon-delet').click();
        cy.get('.submit-btn').should('be.visible').click({force: true});
        cy.wait(2000);
        cy.get('.back-btn').click();
    })

    it('check developer-page remove data', () => {
        if (cy.get('.new-btn').should('be.visible')) {
            cy.get('.el-table .el-table__body-wrapper tbody').children().eq(0).find('.cell .detail-btn').click({force: true});
            cy.wait(2000);
        }

        cy.get('.content .el-table__body-wrapper .el-table__empty-block').scrollIntoView().should('be.visible');
        cy.get('.back-btn').click();
    })

    it('check developer-page import table info', () => {
        if (cy.get('.new-btn').should('be.visible')) {
            cy.get('.el-table .el-table__body-wrapper tbody').children().eq(0).find('.cell .detail-btn').click({force: true});
            cy.wait(2000);
        }

        cy.get('.content .el-table__fixed-header-wrapper .column-imp-btn').click({force: true});
        cy.get('.el-dialog__wrapper').should('be.visible');
        cy.get('.el-dialog__wrapper .el-dialog__title').then($el => {
            expect($el.text()).to.equal('Import');
        });

        cy.get('.el-dialog__body .el-form').children().eq(0).find('[type="text"]').should('have.value', PLATFORM);
        cy.get('.el-dialog__body .el-form').children().eq(1).find('[type="text"]').should('have.value', DB_NAME);
        cy.get('.el-dialog__body .el-form').children().eq(2).find('[type="text"]').should('have.value', DS_BE_ASSET_USAGE_SD);

        cy.get('.el-dialog__body .el-form').children().eq(1).find('[type="text"]').clear().type(IMPORT_DB);
        cy.get('.el-dialog__body .el-form').children().eq(2).find('[type="text"]').clear().type(IMPORT_TABLE);

        cy.get('.el-dialog__wrapper .el-dialog__footer .clone-btn').click({force: true});
        cy.wait(2000);
        cy.get('.content .el-table__body-wrapper tbody').children().its('length').should('eq', 6);
        cy.get('.save-btn').click({force: true});
        cy.get('.back-btn').click();
    })

    it('check developer-page import data', () => {
        if (cy.get('.new-btn').should('be.visible')) {
            cy.get('.el-table .el-table__body-wrapper tbody').children().eq(0).find('.cell .detail-btn').click({force: true});
            cy.wait(2000);
        }

        cy.get('.content .el-table__body-wrapper tbody').children().its('length').should('eq', 6);
        cy.get('.back-btn').click();
    })

    it('check developer-page clone data', () => {
        if (cy.get('.new-btn').should('be.visible')) {
            cy.get('.el-table .el-table__body-wrapper tbody').children().eq(0).find('.cell .clone-btn').click({force: true});
            cy.wait(2000);
        }

        cy.get('.el-form').children().eq(0).find('[type="text"]').should('have.value', '');
        cy.get('.el-form').children().eq(1).find('[type="text"]').should('have.value', '');
        cy.get('.el-form').children().eq(2).find('[type="text"]').should('have.value', '');
        cy.get('.el-form').children().eq(3).find('[type="text"]').should('have.value', UPDATE_SUBJECT_AREA);
        cy.get('.el-form').children().eq(4).find('textarea').should('have.value', UPDATE_TABLE_DESCRIPTION);
        cy.get('.el-form').children().eq(5).find('[type="text"]').should('have.value', UPDATE_JIRA);

        cy.get('.content .el-table__body-wrapper tbody').children().its('length').should('eq', 6);
        cy.get('.back-btn').click();
    })

    it('check developer-page reset table info', () => {
        if (cy.get('.new-btn').should('be.visible')) {
            cy.get('.el-table .el-table__body-wrapper tbody').children().eq(0).find('.cell .detail-btn').click({force: true});
            cy.wait(2000);
        }

        cy.get('.content .el-table__fixed-header-wrapper .column-imp-btn').click({force: true});
        cy.get('.el-dialog__wrapper .el-dialog__footer .clone-btn').click({force: true});
        cy.wait(2000);
        cy.get('.content .el-table__body-wrapper .el-table__empty-block').scrollIntoView().should('be.visible');
        cy.get('.save-btn').click({force: true});
        cy.get('.back-btn').click();
    })
})