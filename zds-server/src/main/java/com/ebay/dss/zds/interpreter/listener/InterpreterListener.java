package com.ebay.dss.zds.interpreter.listener;

import com.ebay.dss.zds.interpreter.monitor.modle.Status;
import com.ebay.dss.zds.interpreter.output.result.Result;
import org.joda.time.DateTime;

/**
 * Created by wenliu2 on 4/26/18.
 */
public interface InterpreterListener {

    void beforeStatementSubmit(InterpreterListenerData interpreterListenerData);

    Long afterStatementSubmit(InterpreterListenerData interpreterListenerData);

    void statementProgress(InterpreterListenerData interpreterListenerData, Status status);

    void afterStatementFinish(InterpreterListenerData interpreterListenerData, Result result);

    void handleCancelling(InterpreterListenerData interpreterListenerData);

    ListenerType getListenerType();

    void setListenerType(ListenerType type);

    long getCurrentStatementKey();

    DateTime getStartDt();

    DateTime getEndDt();

    public static enum ListenerType {
        CODE("CODE"),
        SQL("SQL");

        private String name;

        private ListenerType(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }
    }

}
