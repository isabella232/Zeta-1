const DW_USERS_INFO = "DW_USERS_INFO";
import _ from 'lodash';

describe('Metadata Search', () => {

    it('search DW_USERS_INFO', () => {
        cy.server();
        cy.route('POST',  '/metadata-es/metadata_tv_summary/_search').as('search');
        cy.get('.zeta-icon-medatada').click();
        cy.wait('@search').then((xhr) => {
            expect(xhr.status).to.equal(200);
        });
        cy.get('.search-input').find('[type="text"]').clear().type(DW_USERS_INFO);
        cy.wait('@search').then((xhr) => {
            expect(xhr.status).to.equal(200);
            cy.get('.meta-display ul').children().its('length').should('eq', _.size(xhr.response.body.hits.hits));

            _.forEach(xhr.response.body.hits.hits, (v, k) => {
                cy.get('.meta-display ul').children().eq(k).find('.meta-name').then($el => {
                    expect($el.text().toLowerCase()).to.equal(v._source.name);
                });
                cy.get('.meta-display ul').children().eq(k).find('.meta-desc').then($el => {
                    if (v._source.desc && v._source.desc != '') {
                        expect($el.text()).to.equal(v._source.desc);
                    }else {
                        expect($el.text()).to.equal('N/A');
                    }
                });
                cy.get('.meta-display ul').children().eq(k).find('.meta-platform').children().its('length').should('eq', _.size(v._source.platform.split(',')));
            })
        });
    })
})