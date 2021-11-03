package com.ebay.dss.zds.rest.error;

import com.ebay.dss.zds.exception.ErrorCode;

/**
 * Created by wenliu2 on 4/3/18.
 * This is error dto for all rest response and websocket message when any exception occurs.
 */
public class ErrorDTO {

    private String code;
    private BaseErrorDetail errorDetail;

    @Deprecated
    public ErrorDTO(String code, BaseErrorDetail errorDetail) {
        this.code = code;
        this.errorDetail = errorDetail;
    }

    public ErrorDTO(ErrorCode errorCode, BaseErrorDetail errorDetail) {
        this.code = errorCode.name();
        this.errorDetail = errorDetail;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public BaseErrorDetail getErrorDetail() {
        return errorDetail;
    }

    public void setErrorDetail(BaseErrorDetail errorDetail) {
        this.errorDetail = errorDetail;
    }
}
