package com.ebay.dss.zds.exception;

import javax.annotation.Nullable;

/**
 * Created by tatian on 2018/5/31.
 */
public class InterpreterStoppedException extends InterpreterException{

    private String interpreterGroupId;
    private Class interpreterClass;

    public InterpreterStoppedException(String message) {
        super(ErrorCode.INTERPRETER_STOPPED_EXCEPTION, message);
    }

    public InterpreterStoppedException(String message, Exception ex) {
        super(ErrorCode.INTERPRETER_STOPPED_EXCEPTION, message, ex);
    }

    public InterpreterStoppedException(Exception ex) {
        super(ErrorCode.INTERPRETER_STOPPED_EXCEPTION, ex);
    }

    public InterpreterStoppedException(String interpreterGroupId, Class interpreterClass, String message) {
        super(ErrorCode.INTERPRETER_STOPPED_EXCEPTION, message);
        this.interpreterGroupId = interpreterGroupId;
        this.interpreterClass = interpreterClass;
    }

    public InterpreterStoppedException(String interpreterGroupId, Class interpreterClass, Exception ex) {
        super(ErrorCode.INTERPRETER_STOPPED_EXCEPTION, ex);
        this.interpreterGroupId = interpreterGroupId;
        this.interpreterClass = interpreterClass;
    }

    @Nullable
    public String getInterpreterGroupId() {
        return interpreterGroupId;
    }

    public void setInterpreterGroupId(String interpreterGroupId) {
        this.interpreterGroupId = interpreterGroupId;
    }

    @Nullable
    public Class getInterpreterClass() {
        return interpreterClass;
    }

    public void setInterpreterClass(Class interpreterClass) {
        this.interpreterClass = interpreterClass;
    }
}
