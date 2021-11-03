before(function(){
    cy.SSO().init();
})
after(function(){
    cy.clearBeforeUnload()
})