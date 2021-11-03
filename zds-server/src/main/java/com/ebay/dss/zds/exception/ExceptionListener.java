package com.ebay.dss.zds.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;

/**
 * Created by tatian on 2019-06-06.
 */
// todo: this class is for executing callback function in exception handler,
//  will implement more features in the future
public abstract class ExceptionListener {

    final static Logger logger = LoggerFactory.getLogger(ExceptionListener.class);

    @NotNull
    private final Throwable caught;

    public ExceptionListener(Throwable caught) {
        assert caught != null;
        this.caught = caught;
    }

    // should override
    public void onExceptionCaught() {
        logger.info("Exception caught: " + caught.getClass());
    }

    public Throwable getCaught() {
        return caught;
    }

}
