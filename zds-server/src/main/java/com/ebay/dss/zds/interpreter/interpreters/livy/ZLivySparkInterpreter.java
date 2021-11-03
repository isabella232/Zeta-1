package com.ebay.dss.zds.interpreter.interpreters.livy;

import com.ebay.dss.zds.interpreter.listener.InterpreterListener;
import com.ebay.dss.zds.interpreter.input.ExecutionContext;
import com.ebay.dss.zds.interpreter.interpreters.InterpreterGroup;
import org.apache.zeppelin.interpreter.InterpreterException;
import org.apache.zeppelin.interpreter.InterpreterResult;
import org.apache.zeppelin.livy.LivyException;

import java.util.Properties;


/**
 * Created by tatian on 2018/4/23.
 */
public class ZLivySparkInterpreter extends ZBaseLivyInterpreter {

    private InterpreterGroup intpGrp = null;
    private String sessionKind = "shared";
    private String codeType = "spark";

    public ZLivySparkInterpreter(Properties prop) {
        super(prop);
        this.init(prop);
    }

    protected void init(Properties prop) {

    }

    public InterpreterGroup getInterpreterGroup() {
        return this.intpGrp;
    }

    public void setInterpreterGroup(InterpreterGroup intpGrp) {
        this.intpGrp = intpGrp;
    }

    public ZLivyCompanionSparkSqlInterpreter CompanionLivySparkSqlInterpreter() {
        ZLivyCompanionSparkSqlInterpreter sqlIntp = new ZLivyCompanionSparkSqlInterpreter(getProperties());
        sqlIntp.setInterpreterGroup(this.intpGrp);
        this.intpGrp.addInterpreter(ZLivyCompanionSparkSqlInterpreter.class.getName(), sqlIntp);
        return sqlIntp;
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
    protected InterpreterResult resolveSQLOutput(InterpreterResult result, int maxResult) {
        return ZLivyCommonUtil.resolveSQLOutput(getSessionKind(), result, maxResult);
    }

    @Override
    public String getSessionKind() {
        return this.sessionKind;
    }

    @Override
    public String setSessionKind(String kind) {
        this.sessionKind = kind;
        return sessionKind;
    }

    @Override
    public String getCodeType() {
        return this.codeType;
    }

    @Override
    public String setCodeType(String type) {
        this.codeType = type;
        return codeType;
    }

    @Override
    protected String extractAppId() throws LivyException {
        return ZLivyCommonUtil.extractAppId(this);
    }

    @Override
    protected String extractWebUIAddress() throws LivyException {
        return ZLivyCommonUtil.extractWebUIAddress(this);
    }
}
