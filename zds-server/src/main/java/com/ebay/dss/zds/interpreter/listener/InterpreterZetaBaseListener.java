package com.ebay.dss.zds.interpreter.listener;

import com.ebay.dss.zds.common.Constant;
import com.ebay.dss.zds.common.QueueDestination;
import com.ebay.dss.zds.exception.InvalidInterpreterTypeException;
import com.ebay.dss.zds.interpreter.InterpreterType;
import com.ebay.dss.zds.interpreter.interpreters.Interpreter;
import com.ebay.dss.zds.interpreter.interpreters.livy.LivyCodeKind;
import com.ebay.dss.zds.interpreter.monitor.modle.Status;
import com.ebay.dss.zds.interpreter.output.result.Result;
import com.ebay.dss.zds.model.ZetaNotebook;
import com.ebay.dss.zds.websocket.WebSocketResp;
import com.ebay.dss.zds.websocket.notebook.dto.CodeWithSeq;
import com.ebay.dss.zds.websocket.notebook.dto.ExecuteCodeProgress;
import com.ebay.dss.zds.websocket.notebook.dto.ExecuteCodeReq;
import com.ebay.dss.zds.websocket.notebook.dto.ExecuteCodeStart;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

public abstract class InterpreterZetaBaseListener implements InterpreterListener {

    private static final Logger logger = LogManager.getLogger();
    protected Interpreter interpreter;
    protected ZetaNotebook notebook;
    protected String jobId;
    protected CodeWithSeq codeWithSeq;
    protected String reqId;
    protected QueueDestination destination;
    protected ListenerType listenerType = ListenerType.SQL;
    protected DateTime startDt;
    protected DateTime endDt;

    public InterpreterZetaBaseListener(ZetaNotebook notebook,
                                       Interpreter interpreter) {
        this.notebook = notebook;
        this.interpreter = interpreter;
    }

    public Interpreter getInterpreter() {
        return interpreter;
    }

    public ZetaNotebook getNotebook() {
        return notebook;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }


    public CodeWithSeq getCodeWithSeq() {
        return codeWithSeq;
    }

    public void setCodeWithSeq(CodeWithSeq codeWithSeq) {
        this.codeWithSeq = codeWithSeq;
    }

    public String getReqId() {
        return reqId;
    }

    public void setReqId(String reqId) {
        this.reqId = reqId;
    }

    public QueueDestination getDestination() {
        return destination;
    }

    public void setDestination(QueueDestination destination) {
        this.destination = destination;
    }

    public ListenerType getListenerType() {
        return this.listenerType;
    }

    public void setListenerType(ListenerType type) {
        this.listenerType = type;
    }

    public void setListenerType(String interpreterType) {
        this.listenerType = getListenerType(interpreterType);
    }

    public void setListenerType(InterpreterType.EnumType type) {
        this.listenerType = getListenerType(type);
    }

    public ListenerType getListenerType(String type) {
        return getListenerType(InterpreterType.fromString(type));
    }

    public void setListenerType(ExecuteCodeReq req) {
        this.listenerType = getListenerType(req);
    }

    public static ListenerType getListenerType(InterpreterType.EnumType type) {
        switch (type) {
            case LIVY_SPARK:
            case SSH_RESTRICTED_HDFS:
            case LIVY_PYSPARK:
            case LIVY_SPARKR:
            case IMITATE:
                return ListenerType.CODE;
            case LIVY_SPARKSQL:
            case JDBC:
                return ListenerType.SQL;
            default:
                throw new InvalidInterpreterTypeException("Invalid interpreter type : " + type.getName());
        }
    }

    private static ListenerType getListenerType(ExecuteCodeReq req) {
        InterpreterType.EnumType type = InterpreterType.fromString(req.getInterpreter());
        switch (type) {
            case LIVY_SHARED:
                return getListenerTypeOfSharedLivy(req);
            default:
                return getListenerType(type);
        }
    }

    private static ListenerType getListenerTypeOfSharedLivy(ExecuteCodeReq req) {
        LivyCodeKind livyCodeKind = LivyCodeKind.valueOf(req.getProperty(Constant.SHARED_LIVY_CODE_TYPE));
        switch (livyCodeKind) {
            case sql:
                return ListenerType.SQL;
            case spark:
            case sparkr:
            case pyspark:
                return ListenerType.CODE;
            default:
                throw new InvalidInterpreterTypeException("Invalid shared livy interpreter code type: " + livyCodeKind);
        }
    }


    @Override
    public void beforeStatementSubmit(InterpreterListenerData data) {
        doBeforeStatementSubmit(data);
    }

    public abstract void doBeforeStatementSubmit(InterpreterListenerData data);

    @Override
    public Long afterStatementSubmit(InterpreterListenerData data) {
        startDt = new DateTime();
        Long id = doAfterStatementSubmit(data);
        codeWithSeq.setStatementId(id);
        destination.sendData(WebSocketResp.get(
                WebSocketResp.OP.NB_CODE_STATEMENT_START,
                new ExecuteCodeStart(jobId, notebook.getId(), codeWithSeq.getSeq(),
                        notebook.getLastRunDt().getTime(), id)));
        return id;
    }

    abstract Long doAfterStatementSubmit(InterpreterListenerData data);

    @Override
    public void statementProgress(InterpreterListenerData interpreterListenerData, Status status) {
        ExecuteCodeProgress codeProgress = new ExecuteCodeProgress(this.notebook.getId(), jobId, this.reqId);
        codeProgress.setCode(this.codeWithSeq);
        codeProgress.setStatus(status);
        codeProgress.setSparkJobUrl(interpreterListenerData.getSparkJobUrl());
        destination.sendData(WebSocketResp.get(WebSocketResp.OP.NB_CODE_STATEMENT_PROGRESS, codeProgress));
    }

    @Override
    public void afterStatementFinish(InterpreterListenerData data, Result result) {
        endDt = new DateTime();
        doAfterStatementFinish(data, result);
    }

    abstract void doAfterStatementFinish(InterpreterListenerData data, Result result);

    @Override
    public void handleCancelling(InterpreterListenerData data) {
        endDt = new DateTime();
        doHandleCancelling(data);
    }

    abstract void doHandleCancelling(InterpreterListenerData data);

    @Override
    public DateTime getStartDt() {
        return startDt;
    }

    @Override
    public DateTime getEndDt() {
        return endDt;
    }

    public Long duration() {
        if (startDt == null || endDt == null) {
            return 0L;
        } else {
            return endDt.getMillis() - startDt.getMillis();
        }
    }
}
