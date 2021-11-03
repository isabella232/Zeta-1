const DS_BE_ASSET_USAGE_SD = "DS_BE_ASSET_USAGE_SD";
import _ from 'lodash';

describe('Metadata comment', () => {

    it('check comment content', () => {
        cy.server();
        cy.route('GET', 'https://doe.corp.ebay.com/api/asset/tableInfoService?table=' + DS_BE_ASSET_USAGE_SD + '&zeta=1').as('getMetaDataTable');
        cy.route('GET', 'https://doe.corp.ebay.com/api/asset/viewInfoService?view=' + DS_BE_ASSET_USAGE_SD + '&zeta=1').as('getMetaDataView');
        cy.route('POST', 'https://doe.corp.ebay.com/api/asset/getTableComment').as('getComment');
        cy.visit("/#/metadata?tableName=" + DS_BE_ASSET_USAGE_SD);
        cy.wait(['@getMetaDataTable', '@getMetaDataView', '@getComment']).then((xhr) => {
            expect(xhr[0].status).to.equal(200);
            expect(xhr[1].status).to.equal(200);
            expect(xhr[2].status).to.equal(200);
        });

        // check badge value
        cy.get('.el-badge').should('be.visible');
        cy.get('.el-badge .el-badge__content').then($el => {
            cy.get('@getComment').then((xhr) => {
                expect(parseInt($el.text())).to.equal(_.size(xhr.response.body.data.value));
            })
        });

        // check diaglog title
        cy.get('.el-badge').find('[type="button"]').should('be.visible').click();
        cy.get('.metadata-comment .comment-title').then($el => {
            cy.get('@getComment').then((xhr) => {
                expect($el.text()).to.equal('Comments (' + _.size(xhr.response.body.data.value) + ')');
            });
        });

        // check dialog content
        cy.get('@getComment').then((xhr) => {
            cy.get('.metadata-comment .metadata-comment-content').children().its('length').should('eq', _.size(xhr.response.body.data.value));
            _.forEach(xhr.response.body.data.value, (v, k) => {
                cy.get('.metadata-comment .metadata-comment-content').children().eq(k).find('.card-content').then($el => {
                    expect($el.html().replace(/<br>/g,"\n")).to.equal(v.comment);
                })
            })
        });
    })

    it('check comment add', () => {
        cy.server();
        cy.route('POST', 'https://doe.corp.ebay.com/api/asset/addTableComment').as('addTableComment');
        cy.route('POST', 'https://doe.corp.ebay.com/api/asset/getTableComment').as('getComment');
        cy.get('.metadata-comment .metadata-comment-input').find('textarea').should('not.be.exist');
        cy.get('.metadata-comment .metadata-comment-input').find('[type="text"]').should('be.visible').click();
        cy.get('.metadata-comment .metadata-comment-input').find('textarea').should('be.visible');
        cy.get('.metadata-comment .metadata-comment-input .cancel-btn').should('be.visible');
        cy.get('.metadata-comment .metadata-comment-input .submit-btn').should('be.visible');
        cy.get('.metadata-comment .metadata-comment-input').find('[type="text"]').should('not.be.exist');
        cy.get('.metadata-comment .metadata-comment-input').find('textarea').clear();
        cy.get('.metadata-comment .metadata-comment-input .submit-btn').should('have.disabled', 'disabled');
        const $random = parseInt(Math.random() * 1000000);
        cy.get('.metadata-comment .metadata-comment-input').find('textarea').clear().type('test-' + $random);
        cy.get('.metadata-comment .metadata-comment-input .submit-btn').should('not.have.disabled');
        cy.get('.metadata-comment .metadata-comment-input .submit-btn').click();
        cy.wait(['@addTableComment', '@getComment']).then((xhr) => {
            expect(xhr[0].status).to.equal(200);
            expect(xhr[1].status).to.equal(200);
        });
        // check dialog content
        cy.get('@getComment').then((xhr) => {
            cy.get('.metadata-comment .metadata-comment-content').children().its('length').should('eq', _.size(xhr.response.body.data.value));
            _.forEach(xhr.response.body.data.value, (v, k) => {
                cy.get('.metadata-comment .metadata-comment-content').children().eq(k).find('.card-content').then($el => {
                    expect($el.html().replace(/<br>/g,"\n")).to.equal(v.comment);
                })
            })
        });
    })
})