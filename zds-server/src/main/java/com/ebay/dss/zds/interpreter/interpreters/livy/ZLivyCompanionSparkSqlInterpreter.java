package com.ebay.dss.zds.interpreter.interpreters.livy;

import com.ebay.dss.zds.exception.InterpreterException;
import com.ebay.dss.zds.interpreter.listener.InterpreterListener;
import com.ebay.dss.zds.interpreter.input.ExecutionContext;
import com.ebay.dss.zds.interpreter.interpreters.Interpreter;
import com.ebay.dss.zds.interpreter.interpreters.InterpreterGroup;
import org.apache.commons.lang.StringUtils;
import org.apache.zeppelin.interpreter.InterpreterResult;
import org.apache.zeppelin.interpreter.InterpreterUtils;
import org.apache.zeppelin.livy.LivyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/**This interpreter share the same sparkContext of existing spark interpreter within the same notebook**/

public class ZLivyCompanionSparkSqlInterpreter extends ZBaseLivyInterpreter {

    private static final Logger sparkSqllogger = LoggerFactory.getLogger(ZLivyCompanionSparkSqlInterpreter.class);
    private com.ebay.dss.zds.interpreter.interpreters.InterpreterGroup intpGrp = null;
    private ZLivySparkInterpreter livySparkInterpreter;
    private boolean isSpark2 = true;
    private String preRegisterUDFPath;

    public ZLivyCompanionSparkSqlInterpreter(Properties prop) {
        super(prop);
        this.init(prop);
    }

    protected void init(Properties prop) {
        this.preRegisterUDFPath=prop.getProperty("zds.livy.spark.sql.preRegisterUDF.path","");
    }

    public com.ebay.dss.zds.interpreter.interpreters.InterpreterGroup getInterpreterGroup() {
        return this.intpGrp;
    }

    public void setInterpreterGroup(InterpreterGroup intpGrp) {
        this.intpGrp = intpGrp;
    }

    @Override
    public boolean getPauseFlag(){
        return this.livySparkInterpreter!=null && this.livySparkInterpreter.getPauseFlag();
    }

    @Override
    public void setPauseFlag(boolean flag){
        if(this.livySparkInterpreter!=null){
            this.livySparkInterpreter.setPauseFlag(flag);
        }
    }

    @Override
    public boolean isPaused(){
        return this.getPauseFlag();
    }

    @Override
    public boolean checkPaused(){
        boolean flag=this.isPaused();
        /**make sure only pause once**/
        this.setPauseFlag(false);
        return flag;
    }

    @Override
    public void pause(){
        this.setPauseFlag(true);
    }

    @Override
    public void open() {
        this.livySparkInterpreter = findSparkInterpreterInGroup();
        /**Follow the logic of zeppelin's LivySparkSqlInterpreter**/
        // As we don't know whether livyserver use spark2 or spark1, so we will detect SparkSession
        // to judge whether it is using spark2.
        isSpark2 = true;
        //isSpark2x();
        registerUDF(preRegisterUDFPath);
    }

    @Override
    public List<Integer> cancelAll(){
        if(this.livySparkInterpreter!=null){
            return this.livySparkInterpreter.cancelAll();
        }else{
            return new ArrayList<>();
        }
    }

    @Override
    public void cancel(ExecutionContext context) {
        livySparkInterpreter.cancel(context);
    }

    @Override
    public void close() {
        if(livySparkInterpreter!=null) {
            getInterpreterGroup().removeInterpreter(livySparkInterpreter.getClassName());
            this.livySparkInterpreter = null;
        }
    }

    private ZLivySparkInterpreter findSparkInterpreterInGroup() {
        if (getInterpreterGroup() == null) {
            throw new InterpreterException(this.getClass().getName() + ": The interpreter group is not set");
        } else {
            return (ZLivySparkInterpreter) getInterpreterGroup().getOrCreateInterpreter(ZLivySparkInterpreter.class.getName(), getProperties());
        }
    }

    @Override
    public InterpreterResult interpret(String line, ExecutionContext context, InterpreterListener listener) {
        try {
            if (StringUtils.isEmpty(line)) {
                return new InterpreterResult(InterpreterResult.Code.SUCCESS, "");
            }
            // use triple quote so that we don't need to do string escape.
            String sqlQuery=getSessionKind().equals("shared")?line:ZLivyCommonUtil.resolve2Sql(line, isSpark2, maxResult);

            if(livySparkInterpreter==null){
                Interpreter.logger.info("The inner interpreter is null, find or create another one...");
                livySparkInterpreter=findSparkInterpreterInGroup();
            }
            InterpreterResult result = livySparkInterpreter.interpret(sqlQuery,getCodeType(), context.getParagraphId().toString(),
                    this.displayAppInfo, true, listener);

            return result;
        } catch (Exception e) {
            LOGGER.error("Exception in LivySparkSQLInterpreter while interpret ", e);
            return new InterpreterResult(InterpreterResult.Code.ERROR,
                    InterpreterUtils.getMostRelevantMessage(e));
        }
    }

    @Override
    public InterpreterResult interpret(String line, ExecutionContext context) {
        try {
            if (StringUtils.isEmpty(line)) {
                return new InterpreterResult(InterpreterResult.Code.SUCCESS, "");
            }
            // use triple quote so that we don't need to do string escape.
            String sqlQuery=ZLivyCommonUtil.resolve2Sql(line, isSpark2, maxResult);
            if(livySparkInterpreter==null){
                Interpreter.logger.info("The inner interpreter is null, find or create another one...");
                livySparkInterpreter=findSparkInterpreterInGroup();
            }

            InterpreterResult result = livySparkInterpreter.interpret(sqlQuery,getCodeType(), context.getParagraphId().toString(),
                    this.displayAppInfo, true);

            return resolveSQLOutput(result,maxResult);
        } catch (Exception e) {
            LOGGER.error("Exception in LivySparkSQLInterpreter while interpret ", e);
            return new InterpreterResult(InterpreterResult.Code.ERROR,
                    InterpreterUtils.getMostRelevantMessage(e));
        }
    }

    @Override
    protected InterpreterResult doExecute(ExecutionContext context) {
        return livySparkInterpreter.doExecute(context);

    }

    @Override
    protected InterpreterResult doExecute(ExecutionContext context, InterpreterListener listener) {
        return livySparkInterpreter.doExecute(context, listener);

    }

    public void registerUDF(String path){
        if(!StringUtils.isEmpty(path)){
            try {
                InterpreterResult result=livySparkInterpreter
                        .interpret(Files.lines(Paths.get(path))
                                .map(line -> ZLivyCommonUtil.resolve2Sql(line.replaceAll(";", ""), isSpark2, maxResult))
                                .collect(Collectors.joining(";")),resolveType(), "1", false, false);
                if(result.code().equals(InterpreterResult.Code.SUCCESS)) {
                    sparkSqllogger.info("UDFs are all registered");
                }else{
                    sparkSqllogger.info("Failed to register UDFs:"+result.message().get(0).getData());
                }
            }catch (Exception ex){
                sparkSqllogger.error(ex.getMessage());
            }
        }
    }

    @Override
    protected InterpreterResult resolveSQLOutput( InterpreterResult result, int maxResult) {
        return livySparkInterpreter.resolveSQLOutput(result,maxResult);
    }

    @Override
    public int getProgress(ExecutionContext context) {
        if (this.livySparkInterpreter != null) {
            return this.livySparkInterpreter.getProgress(context);
        } else {
            return 0;
        }
    }

    @Override
    public String getSessionKind() {
        return "shared";
    }

    @Override
    public String setSessionKind(String kind) {
        return livySparkInterpreter.setSessionKind(kind);
    }

    @Override
    public String getCodeType() {
        return livySparkInterpreter.getCodeType();
    }

    @Override
    public String setCodeType(String type) {
        return livySparkInterpreter.setCodeType(type);
    }

    public String resolveType(){
        return getSessionKind().equals("shared")?"spark":null;
    }

    @Override
    protected String extractAppId() throws LivyException {
        // it wont' be called because it would delegate to LivySparkInterpreter
        throw new UnsupportedOperationException();
    }

    @Override
    protected String extractWebUIAddress() throws LivyException {
        // it wont' be called because it would delegate to LivySparkInterpreter
        throw new UnsupportedOperationException();
    }

    @Override
    public Interpreter.FormType getFormType() {
        return Interpreter.FormType.SIMPLE;
    }

}
