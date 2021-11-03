package com.ebay.dss.zds.model;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * This Enum class is for all status description in Zeta
 */

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum ZetaStatus {
    SUCCESS("Success", "2000"),
    FINISHED("Finished", "2001"),
    CREATED("Created", "2002"),
    RUNNING("Running", "2003"),
    WAITING("Waiting", "2004"),
    CONNECTED("Connected","2005"),
    SUBMITTED("Submitted", "2006"),
    CANCELED("Canceled","2007"),
    UNKNOW_EXCEPTION("Unknown exception","4009"),

    FAIL("Fail", "4001"),
    TIMEOUT("Timeout", "4002"),
    DISCONNECTED("Disconnected","4003"),
    NOTEBOOK_IS_RUNNING("Notebook is running!","4004"),

    //DataAcces
    DUPLICATION_VIOLATION("Duplication Violation", "6001"),
    INVALID_INPUT("Invalid input parameter!","6002"),
    ENTITY_IS_NULL("Entity is NULL!", "6003"),
    ENTITY_ID_IS_EMPTY("Entity ID is NULL","6004"),
    ENTITY_NOT_FOUND("Entity not found!","6005"),
    BAD_SQL_GRAMMER("Bad SQL Grammer Exception","6006"),
    CONFLICT_EXCEPTION("Conflict exception","6007"),
    NO_JDBC_CONNECTION("Cannot get Jdbc connection","6008"),
    INCORRECT_SET_COLUMN_COUNT("Incorrect result set column count!","6009"),

    //Livy exception
    ILLEGAL_STATUS("Illegal status","7001"),


    //interpreter
    INTP_SESSION_OPENED("Notebook Session is restricted","8001"),
    INTP_SESSION_RESTRICTED_ERROR("Notebook Session is restricted","8002"),
    INTP_SESSION_CONNECTION_ERROR("Failed to connect","8003"),
    INTP_API_UNSUPPORTED("The interpreter api is no open yet","8004"),
    INTP_UNAUTHORIZED("Connection credential is not valid","8005"),
    INTP_SESSION_CONNECTION_ABORT("Connection aborted","8006"),
    //Spark/Hive exception

    // Hermes exception
    HERMES_PRE_AUTH_EXCEPTION("Your PET Production NT password is not correct","9001"),
    HERMES_ACCOUNT_REVOKE_EXCEPTION("Your PET Production NT account is locked","9002"),
    HERMES_PWD_EXPIRED_EXCEPTION("Your PET Production NT password is expired","9003"),
    HERMES_ACCOUNT_NOT_FOUND_EXCEPTION("Your PET Production NT is not activated","9004"),
    // Teradata exception
    TERADATA_CONNECTION_EXCEPTION("Teradata connection failed", "9100");
    ;


    private final String status;
    private final String statusCode;

    ZetaStatus(String status, String statusCode) {
        this.status = status;
        this.statusCode = statusCode;
    }


    public String getStatusCode(){
        return this.statusCode;
    }

    public String getStatus(){
        return this.status;
    }
}
