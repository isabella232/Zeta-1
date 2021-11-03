export enum ZETA_EXCEPTION_TAG {

    /**
     * Global Exception
     */
    GLOBAL_ERR = "GLOBAL_ERR",
    /**
     * Exception caused by REST
     */
    REST_ERR = "REST_ERR",
    /**
     * Exception caused by web socket
     */
    WS_ERR = "WS_ERR",

    /**
     * should not popup ERRs with this category by default
     * 
     */
    INVISIBLE_ERR = "INVISIBLE_ERR",



    /**
     * notebook ERR
     */
    PATH_NOTEBOOK_ERR = 'PATH_NOTEBOOK_ERR',
    /**
     * workspace ERR exclude notebook
     */
    PATH_WORKSPACE_ERR = 'PATH_WORKSPACE_ERR',
    /**
     * metadata ERR
     */
    PATH_METADATA_ERR = "PATH_METADATA_ERR",
    /**
     * repo
     */
    PATH_REPOSITORY_ERR = 'PATH_REPOSITORY_ERR',

    PATH_RELEASE_ERR = 'PATH_RELEASE_ERR',

    PATH_SCHEDULE_ERR = 'PATH_SCHEDULE_ERR',

    PATH_SETTING_ERR = 'PATH_SETTING_ERR',

    PATH_DA_ERR = 'PATH_DA_ERR',

    CSTM_CONNECTION_ERR = 'CSTM_CONNECTION_ERR'
}
//ZETA_EXCEPTION_TAG
