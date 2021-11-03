package com.ebay.dss.zds.interpreter.listener;

import com.ebay.dss.zds.common.Metric2Report;
import com.ebay.dss.zds.interpreter.interpreters.Interpreter;
import com.ebay.dss.zds.interpreter.monitor.modle.Status;
import com.ebay.dss.zds.interpreter.output.ResultEnum;
import com.ebay.dss.zds.interpreter.output.result.Result;
import org.joda.time.DateTime;

import java.util.HashMap;
import java.util.Map;

import static com.ebay.dss.zds.common.Metric2Report.Utils.setSomeTagsFromInterpreter;
import static com.ebay.dss.zds.common.Metric2Report.Utils.setYearMonthTags;

public class InterpreterTrackListenerProxy implements InterpreterListener {

    private Boolean success;
    private InterpreterZetaBaseListener listener;

    public InterpreterTrackListenerProxy(InterpreterZetaBaseListener listener) {
        this.listener = listener;
    }

    private void log() {
        Interpreter interpreter = listener.getInterpreter();
        Map<String, String> tags = new HashMap<>();
        setSomeTagsFromInterpreter(interpreter, tags);
        setYearMonthTags(tags);
        if (success) {
            tags.put("status", "success");
        } else {
            tags.put("status", "failure");
        }
        long duration = getEndDt().getMillis() - getStartDt().getMillis();
    }

    @Override
    public void beforeStatementSubmit(InterpreterListenerData data) {
        listener.beforeStatementSubmit(data);
    }

    @Override
    public Long afterStatementSubmit(InterpreterListenerData data) {
        return listener.afterStatementSubmit(data);
    }

    @Override
    public void statementProgress(InterpreterListenerData interpreterListenerData, Status status) {
        listener.statementProgress(interpreterListenerData, status);
    }

    @Override
    public void afterStatementFinish(InterpreterListenerData data, Result result) {
        listener.afterStatementFinish(data, result);
        success = result.getResultCode() == ResultEnum.ResultCode.SUCCESS;
        log();
    }

    @Override
    public void handleCancelling(InterpreterListenerData data) {
        listener.handleCancelling(data);
    }

    @Override
    public ListenerType getListenerType() {
        return listener.getListenerType();
    }

    @Override
    public void setListenerType(ListenerType type) {
        listener.setListenerType(type);
    }

    @Override
    public long getCurrentStatementKey() {
        return listener.getCurrentStatementKey();
    }

    @Override
    public DateTime getEndDt() {
        return listener.getEndDt();
    }

    @Override
    public DateTime getStartDt() {
        return listener.getStartDt();
    }

    public InterpreterZetaBaseListener getInnerListenr() {
        return this.listener;
    }
}
