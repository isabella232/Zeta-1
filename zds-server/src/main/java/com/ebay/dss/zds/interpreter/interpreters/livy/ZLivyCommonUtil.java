package com.ebay.dss.zds.interpreter.interpreters.livy;

import com.ebay.dss.zds.interpreter.listener.InterpreterListener;
import com.ebay.dss.zds.interpreter.input.ExecutionContext;
import com.ebay.dss.zds.interpreter.interpreters.Interpreter;
import com.ebay.dss.zds.interpreter.output.InterpreterResultBuilder;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.zeppelin.interpreter.InterpreterResult;
import org.apache.zeppelin.interpreter.InterpreterResultMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.List;
import java.util.function.Function;

import static com.ebay.dss.zds.common.Constant.SHARED_SESSION_KIND;
import static com.ebay.dss.zds.interpreter.interpreters.Interpreter.OperationType.*;
import static com.ebay.dss.zds.interpreter.interpreters.livy.LivyResultParser.parseJsonSQLOutput;
import static com.ebay.dss.zds.interpreter.interpreters.livy.LivyResultParser.parseSQLOutput;

public class ZLivyCommonUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZLivyCommonUtil.class);

    static String extractAppId(ZBaseLivyInterpreter interpreter) {
        return extractStatementResult(
                interpreter.interpret("sc.applicationId", "spark", null, false, false).message()
                        .get(0).getData());
    }

    static String extractWebUIAddress(ZBaseLivyInterpreter interpreter) {
        return extractStatementResult(
                interpreter.interpret(
                        "sc.uiWebUrl.get", "spark", null, false, false)
                        .message().get(0).getData());
    }

    /**
     * Extract the eval result of spark shell, e.g. extract application_1473129941656_0048
     * from following:
     * res0: String = application_1473129941656_0048
     *
     * @param result
     * @return
     */
    static String extractStatementResult(String result) {
        int pos = -1;
        if ((pos = result.indexOf("=")) >= 0) {
            return result.substring(pos + 1).trim();
        } else {
            throw new RuntimeException("No result can be extracted from '" + result + "', " +
                    "something must be wrong");
        }
    }

    static InterpreterResult resolveSQLOutput(String sessionKind, InterpreterResult result, int maxResult) {
        if (result.code() == InterpreterResult.Code.SUCCESS) {
            InterpreterResult result2 = new InterpreterResult(InterpreterResult.Code.SUCCESS);
            try {
                for (InterpreterResultMessage message : result.message()) {
                    // convert Text type to Table type. We assume the text type must be the sql output. This
                    // assumption is correct for now. Ideally livy should return table type. We may do it in
                    // the future release of livy.
                    if (message.getType() == InterpreterResult.Type.TEXT) {
                        JsonObject rows = (SHARED_SESSION_KIND).equals(sessionKind) ?
                                parseJsonSQLOutput(message.getData()) : parseSQLOutput(message.getData());
                        result2.add(InterpreterResult.Type.TABLE, rows.toString());
                        if (rows.getAsJsonArray("rows").size() >= (maxResult + 1)) {
                            result2.add(InterpreterResult.Type.HTML,
                                    "<font color=red>Results are limited by " + maxResult + ".</font>");
                        }
                    } else {
                        result2.add(message.getType(), message.getData());
                    }
                }
            } catch (Exception ex) {
                return result;
            }
            return result2;
        } else {
            return result;
        }
    }

    public static InterpreterResult limitCSVResult(InterpreterResult intpResult, int limit) {
        if (intpResult.code() == InterpreterResult.Code.SUCCESS) {
            InterpreterResult limitedResult = new InterpreterResult(InterpreterResult.Code.SUCCESS);
            try {
                for (InterpreterResultMessage message : intpResult.message()) {
                    // convert Text type to Table type. We assume the text type must be the csv output. This
                    // assumption is correct for now. Ideally livy should return table type. We may do it in
                    // the future release of livy.
                    if (message.getType() == InterpreterResult.Type.TEXT) {
                        String data = message.getData();
                        BufferedReader bufferedReader = new BufferedReader(new StringReader(data));
                        String line = null;
                        int cnt = 0;
                        StringBuilder sb = new StringBuilder();
                        while (cnt < limit + 1 && (line = bufferedReader.readLine()) != null) {
                            sb.append(line + "\n");
                            cnt++;
                        }
                        limitedResult.add(InterpreterResult.Type.CSV, sb.toString().trim());
                    } else {
                        limitedResult.add(message.getType(), message.getData());
                    }
                }
            } catch (Exception e) {
                return intpResult;
            }
            return limitedResult;
        } else return intpResult;
    }

    public static InterpreterResult dumpOrDryRun(
            ExecutionContext context,
            String codeType,
            int maxDumpResult,
            ZBaseLivyInterpreter interpreter,
            InterpreterListener listener) {

        String code = context.getCode();
        String opType = context.getProp().getProperty(OP_KEY, STATEMENT);
        int limit = Integer.parseInt(context.getOrElse(
                LivyClient.DUMP_LIMIT, String.valueOf(maxDumpResult)));
        String paragraphId = context.getParagraphId().toString();

        return dumpOrDryRun(code, opType, codeType, limit, paragraphId, interpreter, listener);
    }

    public static InterpreterResult dumpOrDryRun(
            String code,
            String opType,
            String codeType,
            int maxDumpResult,
            String paragraphId,
            ZBaseLivyInterpreter interpreter,
            InterpreterListener listener) {

        LivyMessage.ExecuteRequest executeRequest;

        if (StringUtils.isEmpty(code)) {
            return new InterpreterResultBuilder().emptySuccess();
        }

        if (DUMP.equals(opType)) {
            executeRequest = new LivyMessage.ExecuteDumpRequest(code, codeType, maxDumpResult);
        } else {
            executeRequest = new LivyMessage.ExecuteRequest(code, codeType);
        }
        InterpreterResult result;
        if (listener != null) {
            result = interpreter.interpret(executeRequest, paragraphId,
                    interpreter.displayAppInfo, true, listener);
        } else {
            result = interpreter.interpret(executeRequest, paragraphId,
                    interpreter.displayAppInfo, true);
        }
        return result;
    }

    public static String resolve2Sql(String code, boolean isSpark2, int maxResult) {
        String sqlQuery = null;
        if (isSpark2) {
            sqlQuery = "spark.sql(\"\"\"" + code + "\"\"\").show(" + maxResult + ",false)";
        } else {
            sqlQuery = "sqlContext.sql(\"\"\"" + code + "\"\"\").show(" + maxResult + ",false)";
        }
        return sqlQuery;
    }


    public static boolean wrapFatalError(InterpreterResult result, List<LivyClient.LivyResponse> extractors) {
        boolean hasFatal = false;
        if (InterpreterResult.Code.ERROR == result.code()) {
            for (InterpreterResultMessage message: result.message()) {
                String original = message.getData();
                for (LivyClient.LivyResponse extractor : extractors) {
                    if (extractor.equals(original)) {
                        message.setData(extractor.tryExtract(original));
                        hasFatal = true;
                        break;
                    }
                }
            }
        }
        return hasFatal;
    }

    public static <T extends Interpreter> InterpreterResult interpretAndRetryWhenFatalError(T interpreter, Function<T, InterpreterResult> sp, int retryNum) {
        int tried = 0;
        List<LivyClient.LivyResponse> extractors = Lists.newArrayList(new LivyClient.GSSInitialFailedResponse(), new LivyClient.ErrorAcquiringPasswordResponse());
        InterpreterResult result = sp.apply(interpreter);
        boolean hasFatal = wrapFatalError(result, extractors);
        while (hasFatal && tried <= retryNum) {
            tried++;
            try {
                LOGGER.error("Fatal error happened: {}, retry({}/{})...", result.toString(), tried, retryNum);
                interpreter.destroy();
                interpreter.open();
                result = sp.apply(interpreter);
                hasFatal = wrapFatalError(result, extractors);
            } catch (Exception ex) {
                LOGGER.error("Error when retry({}/{}): ", tried, retryNum, ex);
            }
        }
        return result;
    }
}
