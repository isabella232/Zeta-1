beforeEach(() => {
    cy.server();
    /** flake BDP request*/
    cy.route({
        method: 'GET',
        url: '/product/user/*',
        response: {"success":true,"result":[{"expireDate":null,"userName":"tianrsun","fullName":"Tianrui,Sun","status":"active","budgetGroup":"Data","sudoers":null,"admins":null,"managedByHiveDBA":false,"adminBatchList":[],"alladminBatchList":[],"sudoerBatchList":[],"allsudoerBatchList":[],"managedClusters":[],"managedGroupSpaces":[],"clusterAccess":[{"clusterId":2,"clusterName":"areslvs","status":"HAS_ACCESS","reason":null,"ownedQueues":[],"ownedDirectories":["/user/tianrsun"]},{"clusterId":3,"clusterName":"apollophx","status":"HAS_ACCESS","reason":null,"ownedQueues":[],"ownedDirectories":["/user/tianrsun"]},{"clusterId":8,"clusterName":"spades","status":"HAS_ACCESS","reason":null,"ownedQueues":[],"ownedDirectories":[]},{"clusterId":9,"clusterName":"artemislvs","status":"NO_ACCESS","reason":null,"ownedQueues":[],"ownedDirectories":null},{"clusterId":10,"clusterName":"herculeslvs","status":"HAS_ACCESS","reason":null,"ownedQueues":[],"ownedDirectories":["/user/tianrsun"]},{"clusterId":11,"clusterName":"herculessublvs","status":"HAS_ACCESS","reason":null,"ownedQueues":[],"ownedDirectories":["/user/tianrsun"]},{"clusterId":13,"clusterName":"herculesrno","status":"NO_ACCESS_NO_APPLY","reason":"You must have Windows PET account ready before sign up for production(RNO) clusters. Please check wiki https://wiki.vip.corp.ebay.com/display/HADOOP/Apply+Prod+AD+account+in+PET.","ownedQueues":[],"ownedDirectories":null},{"clusterId":14,"clusterName":"apollorno","status":"NO_ACCESS_NO_APPLY","reason":"You must have Windows PET account ready before sign up for production(RNO) clusters. Please check wiki https://wiki.vip.corp.ebay.com/display/HADOOP/Apply+Prod+AD+account+in+PET.","ownedQueues":[],"ownedDirectories":null}],"groups":["hdmi-etl-hercules-sub","hdmi-olympus","hdmi-spade","hdlq-data-default","hdlq-data-bpe-default","hdmi-employees","hdmi-cs","hdmi-employees"],"createdAt":1528249900000,"role":{"DELOS_ADMIN":false,"DELOS_USER":false,"BDP_ADMIN":false},"batchUser":false}],"message":""}
    })
    cy.route({
        method:'POST',
        url:'/es/zeta_log_staging/logs/_bulk',
        response: {}
    })

})