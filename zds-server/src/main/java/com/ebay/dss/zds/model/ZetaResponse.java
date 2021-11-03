package com.ebay.dss.zds.model;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * @author shighuang
 * <p>
 * This class is a representation Object to return to front end extending Spring ResponseEntity
 */
public class ZetaResponse<T> extends ResponseEntity<T> {

    private ZetaStatus zetaStatus;

    public ZetaResponse(HttpStatus httpStatus, ZetaStatus zetaStatus) {
        super(httpStatus);
        this.zetaStatus = zetaStatus;
    }

    public ZetaResponse(T body, HttpStatus httpStatus, ZetaStatus zetaStatus) {
        super(body, httpStatus);
        this.zetaStatus = zetaStatus;
    }

    public ZetaResponse(T body, HttpStatus httpStatus) {
        super(body, httpStatus);
        this.zetaStatus = ZetaStatus.SUCCESS;
    }

    public ZetaStatus getZetaStatus() {
        return this.zetaStatus;
    }

    public static <T> ZetaResponse<T> success(T body) {
        return new ZetaResponse<>(body, HttpStatus.OK);
    }

    public static ZetaResponse emptySuccess() {
        return new ZetaResponse(HttpStatus.OK, ZetaStatus.SUCCESS);
    }

    public static ZetaResponse notFoundResponse() {
        return new ZetaResponse(HttpStatus.NOT_FOUND, ZetaStatus.ENTITY_NOT_FOUND);
    }
}
