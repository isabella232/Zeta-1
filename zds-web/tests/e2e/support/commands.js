// ***********************************************
// This example commands.js shows you how to
// create various custom commands and overwrite
// existing commands.
//
// For more comprehensive examples of custom
// commands please read more here:
// https://on.cypress.io/custom-commands
// ***********************************************
//
//
// -- This is a parent command --
const nt_login = Cypress.config("NT_LOGIN");
const zetaToken = Cypress.config("TOKEN");

const isMockData = Cypress.config("isMockData");

const USER_INFO_RESPONSE = {"nt":"zeta_tester","name":"","token":"***","tdPass":null,"githubToken":null,"preference":null,"createDt":null,"updateDt":null,"batchAccounts":null,"admin":111}
const REPO_RESPONSE = [{"id":"21f30b71-fb80-48f2-9b88-b9f990e070d7","title":"preset_0","path":"/","updateDt":1552033587000,"platform":null,"proxyUser":null,"lastRunDt":null,"opened":false,"seq":false,"preference":"{\"notebook.connection\":{\"codeType\":\"SQL\"},\"notebook.profile\":\"default\"}","sha":null,"git_repo":null},{"id":"39bc30f5-6711-467e-a001-2d2fa281dd48","title":"preset_1","path":"/","updateDt":1552033594000,"platform":null,"proxyUser":null,"lastRunDt":null,"opened":false,"seq":true,"preference":"{\"notebook.connection\":{\"codeType\":\"TERADATA\"},\"notebook.profile\":\"default\"}","sha":null,"git_repo":null},{"path":"/conf","id":"default","title":"default","content":"{\n\t\"zds.livy\": {\n\t\t\"zds.livy.spark.yarn.queue\": \"hdlq-data-default\",\n\t\t\"zds.livy.spark.driver.memory\": \"8g\",\n\t\t\"zds.livy.spark.executor.cores\": \"3\",\n\t\t\"zds.livy.spark.executor.memory\": \"4g\",\n\t\t\"zds.livy.spark.yarn.executor.memoryOverhead\": \"1024\",\n\t\t\"zds.livy.spark.yarn.am.extraJavaOptions\": \"\",\n\t\t\"zds.livy.spark.executor.extraJavaOptions\": \"\",\n\t\t\"zds.livy.spark.driver.extraJavaOptions\": \"\",\n\t\t\"zds.livy.spark.sql.shuffle.partitions\": \"100\",\n\t\t\"zds.livy.spark.rdd.compress\": \"true\",\n\t\t\"zds.livy.spark.sql.autoBroadcastJoinThreshold\": \"104857600\",\n\t\t\"zds.livy.spark.dynamicAllocation.initialExecutors\": \"1\",\n\t\t\"zds.livy.spark.dynamicAllocation.minExecutors\": \"1\",\n\t\t\"zds.livy.spark.dynamicAllocation.maxExecutors\": \"1000\",\n\t\t\"zds.livy.spark.sql.caseSensitive\": \"false\",\n\t\t\"zds.livy.spark.sql.crossJoin.enabled\": \"true\",\n\t\t\"zds.livy.spark.sql.parquet.writeLegacyFormat\": \"true\",\n\t\t\"zds.livy.hive.exec.dynamic.partition\": \"true\",\n\t\t\"zds.livy.hive.exec.dynamic.partition.mode\": \"nonstrict\",\n\t\t\"zds.livy.spark.speculation\": \"true\",\n\t\t\"zds.livy.spark.speculation.quantile\": \"0.90\",\n\t\t\"zds.livy.spark.speculation.multiplier\": \"2\",\n\t\t\"zds.livy.spark.jars\": \"\"\n\t}\n}"}]
const DASHBOARD_RESPONSE = [];
const OPENED_RESPONSE = []
const SCHEDULE_RESPONSE = []

/**
 * injectCypress
 */
Cypress.Commands.overwrite('route', (originalFn, ...arg) => {
    // if(url.constructor == String && options) {

    // }
    let options;
    if(arg[0].constructor === String && arg[1]){
        if(!isMockData) {
            return originalFn(arg[0])
        }
        else {
            return originalFn(...arg)
        }
    }
    else if(arg[0].constructor === Object && arg[0].response) {
        if(!isMockData) {
            delete arg[0].response
        }
        return originalFn(...arg)
    }
    return originalFn(...arg)
})
Cypress.Commands.add("SSO", () => {
    cy.setCookie('ihub-nt',nt_login)
    window.sessionStorage.clear();
    window.sessionStorage.setItem('zetaToken',zetaToken)
    cy.log("inject zeta & sso token")
})
Cypress.Commands.add("init",(m = isMockData) => {
    cy.server();
    /** mark last request when initial */
    cy.route('/notebook/users/me', USER_INFO_RESPONSE).as('userInfo');
    cy.route('/notebook/notebooks/nt/*', REPO_RESPONSE).as('getRepo');
    cy.route('/notebook/dashboard', DASHBOARD_RESPONSE).as('getDashboard')
    cy.route('/notebook/notebooks/opened', OPENED_RESPONSE).as('getOpenedNote');
    cy.route('/notebook/Scheduler/jobList*', SCHEDULE_RESPONSE).as("getScheduler");

    cy.log("go to home page")
    cy.visit("/")
    cy.wait('@userInfo')
    .wait('@getRepo')
    .wait('@getDashboard')
    .wait('@getOpenedNote')
    .wait('@getScheduler')
})
Cypress.Commands.add('clearBeforeUnload', () => {
    cy.window().then($win => {
        $win.onbeforeunload= null
    })
})
//
//
// -- This is a child command --
// Cypress.Commands.add("drag", { prevSubject: 'element'}, (subject, options) => { ... })
//
//
// -- This is a dual command --
// Cypress.Commands.add("dismiss", { prevSubject: 'optional'}, (subject, options) => { ... })
//
//
// -- This is will overwrite an existing command --
// Cypress.Commands.overwrite("visit", (originalFn, url, options) => { ... })
