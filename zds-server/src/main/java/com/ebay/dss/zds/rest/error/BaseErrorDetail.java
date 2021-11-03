package com.ebay.dss.zds.rest.error;

import com.ebay.dss.zds.model.ExceptionRule;

/**
 * Created by wenliu2 on 4/3/18.
 */
public class BaseErrorDetail {

    private ExceptionRule rule;
    private ErrorAction action;
    private ErrorContext context;

    public ErrorAction getAction() {
        return action;
    }

    public BaseErrorDetail setAction(ErrorAction action) {
        this.action = action;
        return this;
    }

    public ExceptionRule getRule() {
        return rule;
    }

    public BaseErrorDetail setRule(ExceptionRule rule) {
        this.rule = rule;
        return this;
    }

    public ErrorContext getContext() {
        return context;
    }

    public BaseErrorDetail setContext(ErrorContext context) {
        this.context = context;
        return this;
    }
}
