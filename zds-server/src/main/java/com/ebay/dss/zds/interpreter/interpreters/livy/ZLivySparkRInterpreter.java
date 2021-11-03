package com.ebay.dss.zds.interpreter.interpreters.livy;

import com.ebay.dss.zds.interpreter.listener.InterpreterListener;
import com.ebay.dss.zds.interpreter.input.ExecutionContext;
import com.ebay.dss.zds.interpreter.interpreters.InterpreterGroup;
import com.ebay.dss.zds.interpreter.output.result.JsonResult;
import com.ebay.dss.zds.interpreter.output.result.Result;
import org.apache.zeppelin.interpreter.InterpreterResult;
import org.apache.zeppelin.livy.LivyException;

import java.util.Properties;

public class ZLivySparkRInterpreter extends ZBaseLivyInterpreter {

    private static final String KIND = "sparkr";
    private InterpreterGroup iGroup;

    public ZLivySparkRInterpreter() {
    }

    public ZLivySparkRInterpreter(Properties property) {
        super(property);
    }

    @Override
    public String getSessionKind() {
        return KIND;
    }

    @Override
    public String setSessionKind(String kind) {
        return KIND;
    }

    @Override
    public String getCodeType() {
        return KIND;
    }

    @Override
    public String setCodeType(String type) {
        return KIND;
    }

    @Override
    protected String extractAppId() throws LivyException {
        return ZLivyCommonUtil.extractAppId(this);
    }

    @Override
    protected String extractWebUIAddress() throws LivyException {
        return ZLivyCommonUtil.extractWebUIAddress(this);
    }

    @Override
    protected InterpreterResult resolveSQLOutput(InterpreterResult result, int maxResult) {
        return ZLivyCommonUtil.resolveSQLOutput(getSessionKind(), result, maxResult);
    }

    @Override
    public Result execute(String st, ExecutionContext context) {
        return new JsonResult(interpret(st.trim(), context));
    }

    @Override
    protected InterpreterResult doExecute(ExecutionContext context) {
        return doExecute(context, null);
    }

    @Override
    protected InterpreterResult doExecute(ExecutionContext context, InterpreterListener listener) {
        return ZLivyCommonUtil.dumpOrDryRun(context, getCodeType(), maxDumpResult, this, listener);
    }

    @Override
    public void setInterpreterGroup(InterpreterGroup interpreterGroup) {
        this.iGroup = interpreterGroup;
    }

    @Override
    public InterpreterGroup getInterpreterGroup() {
        return iGroup;
    }
}
