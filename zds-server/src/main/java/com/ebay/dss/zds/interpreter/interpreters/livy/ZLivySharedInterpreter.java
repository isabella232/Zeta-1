package com.ebay.dss.zds.interpreter.interpreters.livy;

import com.ebay.dss.zds.common.Constant;
import com.ebay.dss.zds.interpreter.listener.InterpreterListener;
import com.ebay.dss.zds.interpreter.input.ExecutionContext;
import org.apache.zeppelin.interpreter.InterpreterResult;
import org.apache.zeppelin.livy.LivyException;

import java.util.Properties;

import static com.ebay.dss.zds.common.Constant.SHARED_SESSION_KIND;

public class ZLivySharedInterpreter extends ZBaseLivyInterpreter {

    private final LivyCodeKind sessionKind;//"shared" for 0.5.0

    public ZLivySharedInterpreter(Properties property) {
        super(property);
        // overwrite default livy client
        String sessionKindName = property.getProperty(Constant.SHARED_LIVY_CODE_TYPE);
        property.remove(Constant.SHARED_LIVY_CODE_TYPE);
        this.sessionKind = LivyCodeKind.valueOf(sessionKindName);
        this.setLivyClient(new LivyClient(property, this.sessionKind.name()));
    }

    @Override
    public String getSessionKind() {
        return SHARED_SESSION_KIND;
    }

    @Override
    public String setSessionKind(String kind) {
        return null;
    }


    @Override
    public String getCodeType() {
        return null;
    }

    @Override
    public String setCodeType(String type) {
        return null;
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
        return ZLivyCommonUtil.resolveSQLOutput(SHARED_SESSION_KIND, result, maxResult);
    }

    @Override
    protected InterpreterResult doExecute(ExecutionContext context) {
        return doExecute(context, null);
    }

    @Override
    protected InterpreterResult doExecute(ExecutionContext context, InterpreterListener listener) {
        String codeKindName = context.getProperty(Constant.SHARED_LIVY_CODE_TYPE);
        // do check if name valid
        LivyCodeKind codeKind = LivyCodeKind.valueOf(codeKindName);
        return ZLivyCommonUtil.dumpOrDryRun(context, codeKind.name(), maxDumpResult, this, listener);
    }

}
