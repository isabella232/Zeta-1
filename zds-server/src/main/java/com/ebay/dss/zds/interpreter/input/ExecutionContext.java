package com.ebay.dss.zds.interpreter.input;

import com.ebay.dss.zds.interpreter.interpreters.Interpreter;
import com.ebay.dss.zds.service.DumpFileService;
import org.apache.zeppelin.interpreter.InterpreterResult;
import org.springframework.data.annotation.Transient;

import java.util.HashMap;
import java.util.Properties;
import java.util.function.Function;

/**
 * Created by tatian on 2018/4/23.
 */
public class ExecutionContext {

    private String noteId;
    private Integer paragraphId;
    private String replName;
    private Properties prop = new Properties();
    private String code;
    @Transient
    private Interpreter interpreter;
    // request id generated in client side
    private String requestId;
    // job id generated in server side
    private String jobId;
    private HashMap<String, Function<Object, InterpreterResult>> operationCallBacks = new HashMap<>();
    @Transient
    private DumpFileService dumpFileService;

    public ExecutionContext(String noteId, Integer seq, String replName, String code) {
        this.noteId = noteId;
        this.paragraphId = seq;
        this.replName = replName;
        this.code = code;
    }

    public ExecutionContext(String noteId, Integer seq, String replName) {
        this(noteId, seq, replName, null);
    }

    public ExecutionContext(String noteId, Integer seq) {
        this(noteId, seq, null);
    }

    public ExecutionContext(String code) {
        this(null, null, null, code);
    }

    public String getNoteId() {
        return noteId;
    }

    public void setNoteId(String noteId) {
        this.noteId = noteId;
    }

    public Integer getParagraphId() {
        return paragraphId;
    }

    public void setParagraphId(Integer paragraphId) {
        this.paragraphId = paragraphId;
    }

    public String getReplName() {
        return replName;
    }

    public void setReplName(String replName) {
        this.replName = replName;
    }

    public String getCode() {
        return code == null ? "" : code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Properties getProp() {
       return this.prop;
    }

    public void setProp(Properties prop) {
        this.prop = prop;
    }

    public String getProperty(String key) {
        return getProp().getProperty(key);
    }

    public String getOrElse(String key, String defaultValue) {
        return getProp().getProperty(key, defaultValue);
    }

    public void setProperty(String key, String value) {
        getProp().setProperty(key, value);
    }

    public void put(String key, Object value) {
        getProp().put(key, value);
    }

    public String getOperationType() {
       return prop.getProperty(Interpreter.OperationType.OP_KEY, Interpreter.OperationType.STATEMENT);
    }

    public void setOperationType(String type) {
        prop.setProperty(Interpreter.OperationType.OP_KEY, type);
    }

    public Interpreter getInterpreter() {
        return interpreter;
    }

    public void setInterpreter(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public void addOperationCallback(String callbackKey, Function<Object, InterpreterResult> callback) {
        this.operationCallBacks.put(callbackKey, callback);
    }

    public Function<Object, InterpreterResult> getOperationCallback(String callbackKey) {
        return this.operationCallBacks.get(callbackKey);
    }

    public DumpFileService getDumpFileService() {
        return dumpFileService;
    }

    public void setDumpFileService(DumpFileService dumpFileService) {
        this.dumpFileService = dumpFileService;
    }
}
