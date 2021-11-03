const DS_BE_ASSET_USAGE_SD = "DS_BE_ASSET_USAGE_SD"; // checkout_metric_item_ext,dw_gem2_cmn_actvty_cd
const DW_USERS_INFO = "DW_USERS_INFO";
const OVERVIEW_TAB = "Overview";
const COLUMNS_TAB = "Columns";
const SAMPLE_QUERIES_TAB = "Sample Queries";
const SHOW_MORE_BTN = "Show More";
const SHOW_LESS_BTN = "Show Less";
const HERCULES = "Hercules";
const HADOOP_PLATFORM = ["Hermes", "Apollo_rno", "Hercules", "Ares", "Apollo"];
const TD_PLATFORM = ["Mozart", "Hopper"];
const EDIT_SPAN = "Edit";
const CANCEL_SPAN = "Cancel";
const SAVE_SPAN = "Save";
const TABLE_DESC_INIT = "init";
const TABLE_DESC = "test add table description";
const SAMPLE_DATA = "test add column sample data";
const CONFIRM_SPAN = "Confirm";
const COLUMN_DESC_INIT = "Date on which data was collected.";
const COLUMN_DESC = "test add column description";
const COLUMN_DIALOG_TITLE = "Review the change";
const ALL_PLATFORM = "All Platform";
const SAMPLE_QUERY_TITLE = "Sample query - ";
import _ from 'lodash';

describe('Metadata', () => {
    before(() => {
        cy.visit("/#/metadata?tableName=" + DW_USERS_INFO);
    })

    // visit case
    it('check metadata allow visit with url', () => {
        cy.wait(1000);
        cy.get('.title').children().eq(0).then($el => {
            expect($el.text().trim()).to.equal(DW_USERS_INFO);
        })
    })

    it('check metadata allow visit with search', () => {
        cy.server();
        cy.route('POST',  '/metadata-es/metadata_tv_summary/_search').as('search');
        cy.route('GET',  'https://doe.corp.ebay.com/api/asset/tableInfoService?table=' + DS_BE_ASSET_USAGE_SD + '&zeta=1').as('getMetaDataTable');
        cy.route('GET',  'https://doe.corp.ebay.com/api/asset/viewInfoService?view=' + DS_BE_ASSET_USAGE_SD + '&zeta=1').as('getMetaDataView');
        cy.get('.zeta-icon-medatada').click();
        cy.wait('@search').then((xhr) => {
            expect(xhr.status).to.equal(200);
        });
        cy.get('.search-input').find('[type="text"]').clear().type(DS_BE_ASSET_USAGE_SD);
        cy.wait('@search').then((xhr) => {
            expect(xhr.status).to.equal(200);
        });
        cy.get('.meta-item').eq(0).click();
        cy.wait(['@getMetaDataTable', '@getMetaDataView']).then((xhr) => {
            expect(xhr[0].status).to.equal(200);
            expect(xhr[1].status).to.equal(200);
        });
        cy.get('.title').children().eq(0).then($el => {
            expect($el.text().trim()).to.equal(DS_BE_ASSET_USAGE_SD);
        });
    })

    it('check metadata-home tab', () => {
        cy.get('.metadata-tabs').children().its('length').should('be', 3);
        cy.get('.metadata-tabs').children().eq(0).click();
        cy.get('.metadata-tabs').then($el => {
            expect($el.find('.active').text()).to.equal(OVERVIEW_TAB);
        });
        cy.get('.metadata-display').find('.overview').should('be.visible');
        cy.get('.metadata-display').find('.columns').should('not.be.visible');
        cy.get('.metadata-display').find('.sample').should('not.be.visible');

        cy.get('.metadata-tabs').children().eq(1).click();
        cy.get('.metadata-tabs').then($el => {
            expect($el.find('.active').text()).to.equal(COLUMNS_TAB);
        });
        cy.get('.metadata-display').find('.overview').should('not.be.visible');
        cy.get('.metadata-display').find('.columns').should('be.visible');
        cy.get('.metadata-display').find('.sample').should('not.be.visible');

        cy.get('.metadata-tabs').children().eq(2).click();
        cy.get('.metadata-tabs').then($el => {
            expect($el.find('.active').text()).to.equal(SAMPLE_QUERIES_TAB);
        });
        cy.get('.metadata-display').find('.overview').should('not.be.visible');
        cy.get('.metadata-display').find('.columns').should('not.be.visible');
        cy.get('.metadata-display').find('.sample').should('be.visible');
    })

    it('check metadata-home sharelink', () => {
        cy.get('.link').should('not.be.visible');
        cy.get('.zeta-icon-share').click();
        cy.get('.link').should('be.visible');
        cy.get('.zeta-icon-share').click();
        cy.get('.link').should('not.be.visible');
    })

    // metadata-overview case
    it('check metadata-overview platform', () => {
        cy.get('.metadata-tabs').children().eq(0).click();
        if (cy.get('.overview').find('.metadata').find('.el-radio-group').children().its('length').should('be.gt', 0)) {
            cy.get('.overview').find('.metadata').find('.el-radio-group').children().eq(0).should('have.class', 'is-active');
        }
    })

    it('check metadata-overview show-more btn, show-less btn', () => {
        cy.get('.overview').find('.metadata').find('.show-more').then($el => {
            expect($el.text()).to.equal(SHOW_MORE_BTN);
        }).click();
        cy.get('.overview').find('.metadata').find('.show-more').then($el => {
            expect($el.text()).to.equal(SHOW_LESS_BTN);
        }).click();
        cy.get('.overview').find('.metadata').find('.show-more').then($el => {
            expect($el.text()).to.equal(SHOW_MORE_BTN);
        }).click();
    })

    it('check metadata-overview switch platform', () => {
        cy.get('.overview').find('.metadata').find('.el-radio-group').find('span').each($el => {
            $el.click();
            cy.wait(1000);
            
            if ($el.text() == HERCULES) {
                cy.get('.overview').find('.metadata').find('.display').find('th').contains('Table Size').not();
                cy.get('.overview').find('.metadata').find('.display').find('th').contains('Batch Account');
                cy.get('.overview').find('.metadata').find('.display').find('th').contains('ETL Script');
                cy.get('.overview').find('.metadata').find('.display').find('th').contains('Hadoop Format').not();
            }else if (_.indexOf(HADOOP_PLATFORM, $el.text()) > -1) {
                cy.get('.overview').find('.metadata').find('.display').find('th').contains('Table Size').not();
                cy.get('.overview').find('.metadata').find('.display').find('th').contains('Batch Account').not();
                cy.get('.overview').find('.metadata').find('.display').find('th').contains('ETL Script').not();
                cy.get('.overview').find('.metadata').find('.display').find('th').contains('Hadoop Format').not();
            }else {
                cy.get('.overview').find('.metadata').find('.display').find('th').contains('Table Size');
                cy.get('.overview').find('.metadata').find('.display').find('th').contains('Batch Account');
                cy.get('.overview').find('.metadata').find('.display').find('th').contains('ETL Script');
                cy.get('.overview').find('.metadata').find('.display').find('th').contains('Hadoop Format');
            }
        })
    })

    it('check metadata-overview description edit', () => {
        cy.server();
        cy.route('POST',  'https://doe.corp.ebay.com/api/asset/addTblDesc').as('addTblDesc');
        cy.route('GET',  'https://doe.corp.ebay.com/api/asset/tableInfoService?table=' + DS_BE_ASSET_USAGE_SD + '&zeta=1').as('getMetaDataTable');
        cy.route('GET',  'https://doe.corp.ebay.com/api/asset/viewInfoService?view=' + DS_BE_ASSET_USAGE_SD + '&zeta=1').as('getMetaDataView');
        cy.get('.metadata-tools-bar').find('.btn-group').find('[type="button"]').its('length').should('be', 1);
        cy.get('.overview').find('.metadata').find('.display').find('tr').eq(0).find('.quill-editor').should('not.be.exist');
        cy.get('.overview').find('.metadata').find('.display').find('tr').eq(0).find('span').then($el => {
            expect($el.text()).to.equal(TABLE_DESC_INIT);
        });
        cy.get('.metadata-tools-bar').find('.btn-group').find('[type="button"]').find('span').then($el => {
            expect($el.text()).to.equal(EDIT_SPAN);
        }).click();

        cy.wait(1000);
        cy.get('.overview').find('.metadata').find('.display').find('tr').eq(0).find('.quill-editor').should('be.exist');
        cy.get('.overview').find('.metadata').find('.display').find('tr').eq(0).find('.quill-editor').find('.ql-editor').clear().type(TABLE_DESC);

        // click cancel
        cy.get('.metadata-tools-bar').find('.btn-group').find('[type="button"]').its('length').should('be', 2);
        cy.get('.metadata-tools-bar').find('.btn-group').find('[type="button"]').eq(0).should('be.enabled');
        cy.get('.metadata-tools-bar').find('.btn-group').find('[type="button"]').find('span').eq(0).then($el => {
            expect($el.text()).to.equal(CANCEL_SPAN);
        }).click();

        cy.wait(1000);
        cy.get('.overview').find('.metadata').find('.display').find('tr').eq(0).find('span').then($el => {
            expect($el.text()).to.equal(TABLE_DESC_INIT);
        });
        cy.get('.metadata-tools-bar').find('.btn-group').find('[type="button"]').its('length').should('be', 1);
        cy.get('.metadata-tools-bar').find('.btn-group').find('[type="button"]').find('span').then($el => {
            expect($el.text()).to.equal(EDIT_SPAN);
        }).click();

        // click save
        cy.wait(1000);
        cy.get('.overview').find('.metadata').find('.display').find('tr').eq(0).find('.quill-editor').find('.ql-editor').clear().type(TABLE_DESC);
        cy.get('.metadata-tools-bar').find('.btn-group').find('[type="button"]').eq(1).should('be.enabled');
        cy.get('.metadata-tools-bar').find('.btn-group').find('[type="button"]').find('span').eq(1).then($el => {
            expect($el.text()).to.equal(SAVE_SPAN);
        }).click();
        cy.wait(['@addTblDesc', '@getMetaDataTable', '@getMetaDataView']).then((xhr) => {
            expect(xhr[0].status).to.equal(200);
            expect(xhr[1].status).to.equal(200);
            expect(xhr[2].status).to.equal(200);
        });
        cy.get('.overview').find('.metadata').find('.display').find('tr').eq(0).find('span').then($el => {
            expect($el.text()).to.equal(TABLE_DESC);
        });
        cy.get('.metadata-tools-bar').find('.btn-group').find('[type="button"]').its('length').should('be', 1);
        cy.get('.metadata-tools-bar').find('.btn-group').find('[type="button"]').find('span').then($el => {
            expect($el.text()).to.equal(EDIT_SPAN);
        }).click();

        cy.wait(1000);
        cy.get('.overview').find('.metadata').find('.display').find('tr').eq(0).find('.quill-editor').find('.ql-editor').clear().type(TABLE_DESC_INIT);
        cy.get('.metadata-tools-bar').find('.btn-group').find('[type="button"]').eq(1).should('be.enabled');
        cy.get('.metadata-tools-bar').find('.btn-group').find('[type="button"]').find('span').eq(1).then($el => {
            expect($el.text()).to.equal(SAVE_SPAN);
        }).click();
        cy.wait(['@addTblDesc', '@getMetaDataTable', '@getMetaDataView']).then((xhr) => {
            expect(xhr[0].status).to.equal(200);
            expect(xhr[1].status).to.equal(200);
            expect(xhr[2].status).to.equal(200);
        });
        cy.get('.overview').find('.metadata').find('.display').find('tr').eq(0).find('span').then($el => {
            expect($el.text()).to.equal(TABLE_DESC_INIT);
        });
        cy.get('.metadata-tools-bar').find('.btn-group').find('[type="button"]').its('length').should('be', 1);
        cy.get('.metadata-tools-bar').find('.btn-group').find('[type="button"]').find('span').then($el => {
            expect($el.text()).to.equal(EDIT_SPAN);
        });
    })

    // metadata-columns case
    it('check metadata-columns platform', () => {
        cy.get('.metadata-tabs').children().eq(1).click();
        if (cy.get('.columns .el-radio-group .el-radio-button').children().its('length').should('be.gt', 0)) {
            cy.get('.columns .el-radio-group .el-radio-button').eq(0).should('have.class', 'is-active');
        }
    })

    it('check metadata-columns switch platform', () => {
        cy.get('.columns .el-radio-group .el-radio-button').each($el => {
            $el.click();
        })
    })

    it('check metadata-columns sample data', () => {
        cy.get('.columns .el-table .el-table__body tbody').children().its('length').should('be.gt', 0);
        cy.get('.columns .el-table .el-table__body tbody').children().eq(0).children().eq(5).find('.zeta-icon-result').should('be.visible').click();
        cy.get('.columns .el-dialog__title').should('be.visible');
        cy.get('.columns .el-dialog__close').should('be.visible');
        cy.get('.columns .add-btn').should('be.visible');
        
        // test cancel
        cy.get('.columns .el-dialog__body .el-table__fixed-body-wrapper .el-table__body tbody .el-icon-delete').should('not.be.exist');
        cy.get('.columns').find('.add-btn').click();
        cy.get('.columns .el-dialog__body .el-table__fixed-body-wrapper .el-table__body tbody .el-icon-delete').its('length').should('eq', 1);
        cy.get('.columns .dialog-footer .el-button--default').find('span').then($el => {
            expect($el.text()).to.equal(CANCEL_SPAN);
        }).click();

        // test add
        cy.get('.columns .el-table .el-table__body tbody').children().eq(0).children().eq(5).find('.zeta-icon-result').should('be.visible').click();
        cy.get('.columns .el-dialog__body .el-table__fixed-body-wrapper .el-table__body tbody .el-icon-delete').should('not.be.exist');
        cy.get('.columns').find('.add-btn').click();
        cy.get('.columns .el-dialog__body .el-table__fixed-body-wrapper .el-table__body tbody .el-icon-delete').its('length').should('eq', 1);
        cy.get('.columns .el-dialog__body .is-scrolling-none .el-table__body tbody').children().eq(0).find('[type="text"]').type(SAMPLE_DATA);
        cy.get('.columns .dialog-footer .el-button--primary').find('span').then($el => {
            expect($el.text()).to.equal(CONFIRM_SPAN);
        }).click();

        // test delete
        cy.get('.columns .el-table .el-table__body tbody').children().eq(0).children().eq(5).find('.zeta-icon-result').should('be.visible').click();
        cy.get('.columns .el-dialog__body .el-table__fixed-body-wrapper .el-table__body tbody .el-icon-delete').its('length').should('eq', 1);
        cy.get('.columns .el-dialog__body .el-table__fixed-body-wrapper .el-table__body tbody .el-icon-delete').click();
        cy.get('.columns .dialog-footer .el-button--primary').find('span').then($el => {
            expect($el.text()).to.equal(CONFIRM_SPAN);
        }).click();

        cy.get('.columns .el-table .el-table__body tbody').children().eq(0).children().eq(5).find('.zeta-icon-result').should('be.visible').click();
        cy.get('.columns .el-dialog__body .el-table__fixed-body-wrapper .el-table__body tbody .el-icon-delete').should('not.be.exist');
        cy.get('.columns .el-dialog__header .el-icon-close').click();
    })

    it('check metadata-columns update description', () => {
        // test cancel
        cy.get('.metadata-tools-bar').find('.btn-group').find('[type="button"]').find('span').then($el => {
            expect($el.text()).to.equal(EDIT_SPAN);
        }).click();
        cy.get('.columns .el-table .el-table__body tbody').children().eq(0).children().eq(1).click();
        cy.get('.columns .el-table .el-table__body tbody').children().eq(0).children().eq(1).find('.quill-editor').should('be.visible');
        cy.get('.columns .el-table .el-table__body tbody').children().eq(0).children().eq(1).find('.quill-editor').find('.ql-editor').clear().type(COLUMN_DESC);
        cy.get('.metadata-tools-bar').find('.btn-group').find('[type="button"]').find('span').eq(0).then($el => {
            expect($el.text()).to.equal(CANCEL_SPAN);
        }).click();
        cy.get('.columns .el-table .el-table__body tbody').children().eq(0).children().eq(1).find('.col-desc-content').then($el => {
            expect($el.text()).to.equal(COLUMN_DESC_INIT);
        })

        // test dialog-cancel
        cy.get('.metadata-tools-bar').find('.btn-group').find('[type="button"]').find('span').then($el => {
            expect($el.text()).to.equal(EDIT_SPAN);
        }).click();
        cy.get('.columns .el-table .el-table__body tbody').children().eq(0).children().eq(1).click();
        cy.get('.columns .el-table .el-table__body tbody').children().eq(0).children().eq(1).find('.quill-editor').should('be.visible');
        cy.get('.columns .el-table .el-table__body tbody').children().eq(0).children().eq(1).find('.quill-editor').find('.ql-editor').clear().type(COLUMN_DESC);
        cy.get('.metadata-tools-bar').find('.btn-group').find('[type="button"]').find('span').eq(1).then($el => {
            expect($el.text()).to.equal(SAVE_SPAN);
        }).click();
        cy.get('.metadata .el-dialog__wrapper .el-dialog__title').then($el => {
            expect($el.text()).to.equal(COLUMN_DIALOG_TITLE);
        })
        cy.get('.metadata .el-dialog__body tbody').children().its('length').should('eq', 1);
        cy.get('.metadata .el-dialog__footer .dialog-footer .el-button--default').find('span').then($el => {
            expect($el.text()).to.equal(CANCEL_SPAN);
        }).click();
        cy.get('.columns .el-table .el-table__body tbody').children().eq(0).children().eq(1).find('.quill-editor').find('.ql-editor').then($el => {
            expect($el.text()).to.equal(COLUMN_DESC)
        })

        // test dialog-confirm
        cy.get('.metadata-tools-bar').find('.btn-group').find('[type="button"]').find('span').eq(1).then($el => {
            expect($el.text()).to.equal(SAVE_SPAN);
        }).click();
        cy.get('.metadata .el-dialog__wrapper .el-dialog__title').then($el => {
            expect($el.text()).to.equal(COLUMN_DIALOG_TITLE);
        })
        cy.get('.metadata .el-dialog__body tbody').children().its('length').should('eq', 1);
        cy.get('.metadata .el-dialog__footer .dialog-footer .el-button--primary').find('span').then($el => {
            expect($el.text()).to.equal(CONFIRM_SPAN);
        }).click();
        cy.wait(2000);
        cy.get('.columns .el-table .el-table__body tbody').children().eq(0).children().eq(1).find('.col-desc-content').then($el => {
            expect($el.text()).to.equal(COLUMN_DESC);
        })

        // reset column description
        cy.get('.metadata-tools-bar').find('.btn-group').find('[type="button"]').find('span').then($el => {
            expect($el.text()).to.equal(EDIT_SPAN);
        }).click();
        cy.get('.columns .el-table .el-table__body tbody').children().eq(0).children().eq(1).click();
        cy.get('.columns .el-table .el-table__body tbody').children().eq(0).children().eq(1).find('.quill-editor').find('.ql-editor').clear().type(COLUMN_DESC_INIT);
        cy.get('.metadata-tools-bar').find('.btn-group').find('[type="button"]').find('span').eq(1).then($el => {
            expect($el.text()).to.equal(SAVE_SPAN);
        }).click();
        cy.get('.metadata .el-dialog__footer .dialog-footer .el-button--primary').find('span').then($el => {
            expect($el.text()).to.equal(CONFIRM_SPAN);
        }).click();
        cy.wait(2000);
    })

    // metadata-sample-queries case
    it('check metadata-sample-queries platform', () => {
        cy.get('.metadata-tabs').children().eq(2).click();
        cy.get('.sample .sample-query-filter .el-checkbox-group').children().its('length').should('eq', 7);
        cy.get('.sample .sample-query-filter .el-checkbox-group .is-checked').its('length').should('eq', 1)
        cy.get('.sample .sample-query-filter .el-checkbox-group .is-checked').find('span').then($el => {
            expect($el.text()).to.equal(ALL_PLATFORM);
        });
    })

    it('check metadata-sample-queries add and update', () => {
        cy.server();
        cy.route('POST', 'https://doe.corp.ebay.com/api/asset/addSampleQuery').as('addSampleQuery');
        cy.route('GET',  'https://doe.corp.ebay.com/api/asset/tableInfoService?table=' + DS_BE_ASSET_USAGE_SD + '&zeta=1').as('getMetaDataTable');
        cy.route('GET',  'https://doe.corp.ebay.com/api/asset/viewInfoService?view=' + DS_BE_ASSET_USAGE_SD + '&zeta=1').as('getMetaDataView');
        cy.route('POST', 'https://doe.corp.ebay.com/api/asset/getSampleQuery').as('getSampleQuery');
        // add
        cy.get('.sample .sample-query-input .write-query-input').should('be.visible');
        cy.get('.sample .sample-query-input .write-query-input').find('[type="text"]').should('be.visible').click();
        cy.get('.sample .sample-query-input .write-query-input').should('not.be.exist');
        cy.get('.sample .sample-query-input .el-form').should('be.visible');
        const $random = parseInt(Math.random() * 100);
        cy.get('.sample .sample-query-input .el-form').children().eq(0).find('[type="text"]').clear().type(SAMPLE_QUERY_TITLE + $random);
        cy.get('.sample .sample-query-input .el-form').children().eq(2).find('textarea').clear().type($random);
        cy.get('.sample .sample-query-input .el-form').children().eq(3).find('.el-button--primary').click();

        // check
        cy.wait(['@addSampleQuery', '@getMetaDataTable', '@getMetaDataView', '@getSampleQuery']).then((xhr) => {
            expect(xhr[0].status).to.equal(200);
            expect(xhr[1].status).to.equal(200);
            expect(xhr[2].status).to.equal(200);
            expect(xhr[3].status).to.equal(200);
        });
        cy.get('.sample .sample-query-content').children().eq(0).find('.card-title').then($el => {
            expect($el.text()).to.equal(SAMPLE_QUERY_TITLE + $random);
        });
        cy.get('.sample .sample-query-content').children().eq(0).find('.card-head .platform-div').its('length').should('eq', 6);
        cy.get('.sample .sample-query-content').children().eq(0).find('.card-content').then($el => {
            expect($el.text()).to.equal($random.toString());
        });

        // update
        cy.get('.sample .sample-query-content .card-end .zeta-icon-edit').eq(0).should('be.visible').click();
        cy.get('.sample .sample-query-content .sample-query-input').should('be.visible');
        cy.get('.sample .sample-query-content .sample-query-input .el-form').children().eq(0).find('[type="text"]').should('have.value', SAMPLE_QUERY_TITLE + $random);
        cy.get('.sample .sample-query-content .sample-query-input .el-form').children().eq(1).find('.el-checkbox-group').find('.is-checked').its('length').should('eq', 6);
        cy.get('.sample .sample-query-content .sample-query-input .el-form').children().eq(2).find('textarea').should('have.value', $random.toString());

        const $platform = parseInt(Math.random() * 5 + 1);
        cy.get('.sample .sample-query-content .sample-query-input .el-form .el-checkbox-group').children().eq($platform).find('span').click();
        cy.get('.sample .sample-query-content .sample-query-input .el-form').children().eq(2).find('textarea').clear().type($random + " -- " + $platform);
        cy.get('.sample .sample-query-content .sample-query-input .el-form').children().eq(3).find('.el-button--primary').click();

        //check
        cy.wait(['@addSampleQuery', '@getMetaDataTable', '@getMetaDataView', '@getSampleQuery']).then((xhr) => {
            expect(xhr[0].status).to.equal(200);
            expect(xhr[1].status).to.equal(200);
            expect(xhr[2].status).to.equal(200);
            expect(xhr[3].status).to.equal(200);
        });
        cy.get('.sample .sample-query-content .card-head').eq(0).find('.card-title').then($el => {
            expect($el.text()).to.equal(SAMPLE_QUERY_TITLE + $random);
        });
        cy.get('.sample .sample-query-content .card-head').eq(0).find('.platform-div').its('length').should('eq', 5);
        cy.get('.sample .sample-query-content').children().eq(0).find('.card-content').then($el => {
            expect($el.text()).to.equal($random + " -- " + $platform);
        });
    })

    it('check metadata-sample-queries filter', () => {
        cy.get('.sample .sample-query-filter .el-checkbox-group').children().eq(1).click();
        cy.get('.sample .sample-query-filter .el-checkbox-group').children().eq(0).should('not.have.class', 'is-checked');
        cy.get('.sample .sample-query-filter .el-checkbox-group').children().eq(1).should('have.class', 'is-checked');
        cy.get('.sample .sample-query-content .card-head').each($el => {
            expect($el.context.textContent.indexOf("Apollo_rno")).to.not.equal(-1);
        })
        cy.get('.sample .sample-query-filter .el-checkbox-group').children().eq(1).click();

        cy.get('.sample .sample-query-filter .el-checkbox-group').children().eq(2).click();
        cy.get('.sample .sample-query-filter .el-checkbox-group').children().eq(0).should('not.have.class', 'is-checked');
        cy.get('.sample .sample-query-filter .el-checkbox-group').children().eq(2).should('have.class', 'is-checked');
        cy.get('.sample .sample-query-content .card-head').each($el => {
            expect($el.context.textContent.indexOf("Hercules")).to.not.equal(-1);
        })
        cy.get('.sample .sample-query-filter .el-checkbox-group').children().eq(2).click();

        cy.get('.sample .sample-query-filter .el-checkbox-group').children().eq(3).click();
        cy.get('.sample .sample-query-filter .el-checkbox-group').children().eq(0).should('not.have.class', 'is-checked');
        cy.get('.sample .sample-query-filter .el-checkbox-group').children().eq(3).should('have.class', 'is-checked');
        cy.get('.sample .sample-query-content .card-head').each($el => {
            expect($el.context.textContent.indexOf("Ares")).to.not.equal(-1);
        })
        cy.get('.sample .sample-query-filter .el-checkbox-group').children().eq(3).click();

        cy.get('.sample .sample-query-filter .el-checkbox-group').children().eq(4).click();
        cy.get('.sample .sample-query-filter .el-checkbox-group').children().eq(0).should('not.have.class', 'is-checked');
        cy.get('.sample .sample-query-filter .el-checkbox-group').children().eq(4).should('have.class', 'is-checked');
        cy.get('.sample .sample-query-content .card-head').each($el => {
            expect($el.context.textContent.indexOf("Apollo")).to.not.equal(-1);
        })
        cy.get('.sample .sample-query-filter .el-checkbox-group').children().eq(4).click();

        cy.get('.sample .sample-query-filter .el-checkbox-group').children().eq(5).click();
        cy.get('.sample .sample-query-filter .el-checkbox-group').children().eq(0).should('not.have.class', 'is-checked');
        cy.get('.sample .sample-query-filter .el-checkbox-group').children().eq(5).should('have.class', 'is-checked');
        cy.get('.sample .sample-query-content .card-head').each($el => {
            expect($el.context.textContent.indexOf("Mozart")).to.not.equal(-1);
        })
        cy.get('.sample .sample-query-filter .el-checkbox-group').children().eq(5).click();

        cy.get('.sample .sample-query-filter .el-checkbox-group').children().eq(6).click();
        cy.get('.sample .sample-query-filter .el-checkbox-group').children().eq(0).should('not.have.class', 'is-checked');
        cy.get('.sample .sample-query-filter .el-checkbox-group').children().eq(6).should('have.class', 'is-checked');
        cy.get('.sample .sample-query-content .card-head').each($el => {
            expect($el.context.textContent.indexOf("Hopper")).to.not.equal(-1);
        })
        cy.get('.sample .sample-query-filter .el-checkbox-group').children().eq(6).click();
    })

    it('check metadata-sample-queries edit platform', () => {
        cy.get('.sample .sample-query-filter .el-checkbox-group').children().eq(0).click();
        cy.get('.sample .sample-query-input .write-query-input').find('[type="text"]').should('be.visible').click();
        cy.get('.sample .sample-query-input .el-form').children().eq(1).find('.el-checkbox-group .is-checked span').then($el => {
            expect($el.text()).to.equal('All Platform')
        });
        cy.get('.sample .sample-query-input .el-form').children().eq(3).find('.el-button--default').click();
        cy.get('.sample .sample-query-filter .el-checkbox-group').children().eq(0).click();

        cy.get('.sample .sample-query-filter .el-checkbox-group').children().eq(1).click();
        cy.get('.sample .sample-query-input .write-query-input').find('[type="text"]').should('be.visible').click();
        cy.get('.sample .sample-query-input .el-form').children().eq(1).find('.el-checkbox-group .is-checked span').then($el => {
            expect($el.text()).to.equal('Apollo_rno')
        });
        cy.get('.sample .sample-query-input .el-form').children().eq(3).find('.el-button--default').click();
        cy.get('.sample .sample-query-filter .el-checkbox-group').children().eq(1).click();

        cy.get('.sample .sample-query-filter .el-checkbox-group').children().eq(2).click();
        cy.get('.sample .sample-query-input .write-query-input').find('[type="text"]').should('be.visible').click();
        cy.get('.sample .sample-query-input .el-form').children().eq(1).find('.el-checkbox-group .is-checked span').then($el => {
            expect($el.text()).to.equal('Hercules')
        });
        cy.get('.sample .sample-query-input .el-form').children().eq(3).find('.el-button--default').click();
        cy.get('.sample .sample-query-filter .el-checkbox-group').children().eq(2).click();

        cy.get('.sample .sample-query-filter .el-checkbox-group').children().eq(3).click();
        cy.get('.sample .sample-query-input .write-query-input').find('[type="text"]').should('be.visible').click();
        cy.get('.sample .sample-query-input .el-form').children().eq(1).find('.el-checkbox-group .is-checked span').then($el => {
            expect($el.text()).to.equal('Ares')
        });
        cy.get('.sample .sample-query-input .el-form').children().eq(3).find('.el-button--default').click();
        cy.get('.sample .sample-query-filter .el-checkbox-group').children().eq(3).click();

        cy.get('.sample .sample-query-filter .el-checkbox-group').children().eq(4).click();
        cy.get('.sample .sample-query-input .write-query-input').find('[type="text"]').should('be.visible').click();
        cy.get('.sample .sample-query-input .el-form').children().eq(1).find('.el-checkbox-group .is-checked span').then($el => {
            expect($el.text()).to.equal('Apollo')
        });
        cy.get('.sample .sample-query-input .el-form').children().eq(3).find('.el-button--default').click();
        cy.get('.sample .sample-query-filter .el-checkbox-group').children().eq(4).click();

        cy.get('.sample .sample-query-filter .el-checkbox-group').children().eq(5).click();
        cy.get('.sample .sample-query-input .write-query-input').find('[type="text"]').should('be.visible').click();
        cy.get('.sample .sample-query-input .el-form').children().eq(1).find('.el-checkbox-group .is-checked span').then($el => {
            expect($el.text()).to.equal('Mozart')
        });
        cy.get('.sample .sample-query-input .el-form').children().eq(3).find('.el-button--default').click();
        cy.get('.sample .sample-query-filter .el-checkbox-group').children().eq(5).click();

        cy.get('.sample .sample-query-filter .el-checkbox-group').children().eq(6).click();
        cy.get('.sample .sample-query-input .write-query-input').find('[type="text"]').should('be.visible').click();
        cy.get('.sample .sample-query-input .el-form').children().eq(1).find('.el-checkbox-group .is-checked span').then($el => {
            expect($el.text()).to.equal('Hopper')
        });
        cy.get('.sample .sample-query-input .el-form').children().eq(3).find('.el-button--default').click();
        cy.get('.sample .sample-query-filter .el-checkbox-group').children().eq(6).click();
    })
})