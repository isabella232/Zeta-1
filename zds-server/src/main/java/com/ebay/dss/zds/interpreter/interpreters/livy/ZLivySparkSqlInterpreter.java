package com.ebay.dss.zds.interpreter.interpreters.livy;

import com.ebay.dss.zds.interpreter.listener.InterpreterListener;
import com.ebay.dss.zds.interpreter.input.ExecutionContext;
import com.ebay.dss.zds.interpreter.interpreters.Interpreter;
import com.ebay.dss.zds.interpreter.interpreters.InterpreterGroup;
import com.ebay.dss.zds.interpreter.output.InterpreterResultBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.zeppelin.interpreter.InterpreterResult;
import org.apache.zeppelin.interpreter.InterpreterUtils;
import org.apache.zeppelin.livy.LivyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.ebay.dss.zds.interpreter.interpreters.livy.ZLivyCommonUtil.resolve2Sql;

/**
 * Created by tatian on 2018/4/24.
 */
public class ZLivySparkSqlInterpreter extends ZBaseLivyInterpreter {

    private static final Logger sparkSqlLogger = LoggerFactory.getLogger(ZLivySparkSqlInterpreter.class);
    private com.ebay.dss.zds.interpreter.interpreters.InterpreterGroup intpGrp = null;
    private boolean isSpark2 = true;
    private String preRegisterUDFPath;
    private String sessionKind="shared";//"shared" for 0.5.0
    private String codeType="sql";//sql for 0.5.0
    private int maxRetryOnFail = 0;

    public ZLivySparkSqlInterpreter(Properties prop) {
        super(prop);
        this.init(prop);
    }

    protected void init(Properties prop) {
        this.preRegisterUDFPath = prop.getProperty("zds.livy.spark.sql.preRegisterUDF.path", "");
        this.maxRetryOnFail = Integer.valueOf(prop.getProperty("zds.livy.statement.maxRetry", "0"));
    }

    public com.ebay.dss.zds.interpreter.interpreters.InterpreterGroup getInterpreterGroup() {
        return this.intpGrp;
    }

    public void setInterpreterGroup(InterpreterGroup intpGrp) {
        this.intpGrp = intpGrp;
    }

    @Override
    public void open() throws Exception{
        super.open();
        /**Follow the logic of zeppelin's LivySparkSqlInterpreter**/
        // As we don't know whether livyserver use spark2 or spark1, so we will detect SparkSession
        // to judge whether it is using spark2.
        isSpark2 = true;
        registerUDF(preRegisterUDFPath);
    }

    @Override
    public InterpreterResult interpret(String line, ExecutionContext context, InterpreterListener listener) {
        try {
            if (StringUtils.isEmpty(line)) {
                return new InterpreterResultBuilder().emptySuccess();
            }
            // use triple quote so that we don't need to do string escape.
            String sqlQuery = getSessionKind().equals("shared")?line:resolve2Sql(line, isSpark2, maxResult);

            InterpreterResult result = ZLivyCommonUtil.interpretAndRetryWhenFatalError(this,
                    (ZLivySparkSqlInterpreter intp) ->
                    intp.interpret(sqlQuery,getCodeType(), context.getParagraphId().toString(),
                            this.displayAppInfo, true, listener),
                    1);

            return result;
        } catch (Exception e) {
            LOGGER.error("Exception in LivySparkSQLInterpreter while interpret ", e);
            return new InterpreterResultBuilder().error().because(InterpreterUtils.getMostRelevantMessage(e));
        }
    }

    @Override
    public InterpreterResult interpret(String line, ExecutionContext context) {
        try {
            if (StringUtils.isEmpty(line)) {
                return new InterpreterResultBuilder().emptySuccess();
            }
            // use triple quote so that we don't need to do string escape.
            String sqlQuery = getSessionKind().equals("shared")?line:resolve2Sql(line, isSpark2, maxResult);

            InterpreterResult result = ZLivyCommonUtil.interpretAndRetryWhenFatalError(this,
                    (ZLivySparkSqlInterpreter intp) -> interpret(sqlQuery,getCodeType(), context.getParagraphId().toString(),
                    this.displayAppInfo, true),
                    1);

            return resolveSQLOutput( result, maxResult);
        } catch (Exception e) {
            LOGGER.error("Exception in LivySparkSQLInterpreter while interpret ", e);
            return new InterpreterResultBuilder().error().because(InterpreterUtils.getMostRelevantMessage(e));
        }
    }

    @Override
    protected InterpreterResult doExecute(ExecutionContext context) {
        String line = context.getCode();
        String op_type = context.getProp().getProperty(OperationType.OP_KEY, OperationType.STATEMENT);

        if (StringUtils.isEmpty(line)) {
            return new InterpreterResultBuilder().emptySuccess();
        }
        // use triple quote so that we don't need to do string escape.
        String sqlQuery = getSessionKind().equals("shared")?line:resolve2Sql(line, isSpark2, maxResult);

        InterpreterResult result = ZLivyCommonUtil.interpretAndRetryWhenFatalError(this,
                (ZLivySparkSqlInterpreter intp) -> ZLivyCommonUtil.dumpOrDryRun(sqlQuery, op_type, getCodeType(), maxDumpResult,
                context.getParagraphId().toString(), this, null),
                maxRetryOnFail);
        return OperationType.DUMP.equals(op_type) ?
                resolveSQLOutput(result, maxDumpResult) : resolveSQLOutput(result, maxResult);
    }

    @Override
    protected InterpreterResult doExecute(ExecutionContext context, InterpreterListener listener) {
        String line = context.getCode();
        String op_type = context.getProp().getProperty(OperationType.OP_KEY, OperationType.STATEMENT);

        if (StringUtils.isEmpty(line)) {
            return new InterpreterResultBuilder().emptySuccess();
        }
        // use triple quote so that we don't need to do string escape.
        String sqlQuery = getSessionKind().equals("shared")?line:resolve2Sql(line, isSpark2, maxResult);
        return ZLivyCommonUtil.interpretAndRetryWhenFatalError(this,
                (ZLivySparkSqlInterpreter intp) -> ZLivyCommonUtil.dumpOrDryRun(sqlQuery, op_type, getCodeType(), maxDumpResult,
                context.getParagraphId().toString(), this, listener),
                maxRetryOnFail);
    }

    public void registerUDF(String path) {
        if (!StringUtils.isEmpty(path)) {
            long start = System.currentTimeMillis();
            try(Stream<String> fileLines = Files.lines(Paths.get(path))) {
                List<String> lines = fileLines.collect(Collectors.toList());
                InterpreterResult result = interpret(lines.stream()
                                .map(line -> resolve2Sql(line.replaceAll(";", ""), isSpark2, maxResult))
                                .collect(Collectors.joining(";")),resolveType(), "1", false, false);
                if (result.code().equals(InterpreterResult.Code.SUCCESS)) {
                    sparkSqlLogger.info("UDFs are all registered");
                } else {
                    sparkSqlLogger.info("Failed to register UDFs:" + result.message().get(0).getData());
                }
            } catch (Exception ex) {
                sparkSqlLogger.error(ex.getMessage());
            } finally {
                logger.info("UDF process cost: " + (System.currentTimeMillis() - start) / 1000 + " s");
            }
        }
    }

    @Override
    protected InterpreterResult resolveSQLOutput(InterpreterResult result, int maxResult) {
        return ZLivyCommonUtil.resolveSQLOutput(getSessionKind(), result, maxResult);
    }

    @Override
    public int getProgress(ExecutionContext context) {
            return 0;
    }

    @Override
    public String getSessionKind() {
        return this.sessionKind;
    }

    @Override
    public String setSessionKind(String kind) {
        this.sessionKind=kind;
        return sessionKind;
    }

    @Override
    public String getCodeType() {
        return this.codeType;
    }

    @Override
    public String setCodeType(String type) {
        this.codeType=type;
        return codeType;
    }

    public String resolveType(){
       return getSessionKind().equals("shared")?"spark":null;
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
    public Interpreter.FormType getFormType() {
        return Interpreter.FormType.SIMPLE;
    }

}
