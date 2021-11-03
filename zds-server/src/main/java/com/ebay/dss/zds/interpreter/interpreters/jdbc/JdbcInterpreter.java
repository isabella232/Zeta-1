package com.ebay.dss.zds.interpreter.interpreters.jdbc;

import com.ebay.dss.zds.common.ExceptionUtil;
import com.ebay.dss.zds.exception.InterpreterStoppedException;
import com.ebay.dss.zds.exception.JDBCConnectionCheckException;
import com.ebay.dss.zds.exception.JDBCConnectionClosedException;
import com.ebay.dss.zds.interpreter.input.ExecutionContext;
import com.ebay.dss.zds.interpreter.interpreters.Interpreter;
import com.ebay.dss.zds.interpreter.listener.InterpreterListener;
import com.ebay.dss.zds.interpreter.output.InterpreterResultBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.zeppelin.interpreter.InterpreterResult;
import org.codehaus.plexus.util.ExceptionUtils;
import org.springframework.dao.DataAccessException;

import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

public class JdbcInterpreter extends Interpreter {

    protected IJdbcConf conf;
    protected JdbcCompleter completer;
    private Logger logger = LogManager.getLogger();

    public JdbcInterpreter(Properties properties) throws Exception {
        super(properties);
        this.conf = JdbcConfUtils.unwrap(properties);
    }

    public IJdbcConf getConf() {
        return conf;
    }

    @Override
    public boolean isOpened() {
        return Objects.nonNull(completer)
                && completer.isValid();
    }

    public boolean isLocked() {
        try {
            return Objects.nonNull(completer) && completer.isLocked();
        } catch (Exception ex) {
            logger.error("Error when check lock status: " + ex.toString());
            return false;
        }
    }

    JdbcCompleter getJdbcCompleter() {
        return new JdbcCompleter(conf);
    }

    @Override
    protected void construct() throws Exception {
        completer = getJdbcCompleter();
        tryConstruct();
        try {
            if (conf.getJdbcType().testOnConnection) {
                completer.doValidation();
            }
        } catch (Exception ex) {
            Exception rootCause = ExceptionUtil.rootCauseAsException(ex);
            if (ex instanceof JDBCConnectionCheckException) {
                throw new InterpreterStoppedException("Init connection error," +
                        " the underlying service is unstable." +
                        " details: " + rootCause.getMessage(), rootCause);
            } else if (ex instanceof JDBCConnectionClosedException) {
                throw new InterpreterStoppedException("Init connection error," +
                        " the connection already closed." +
                        " details: " + rootCause.getMessage(), rootCause);
            } else {
                throw new InterpreterStoppedException("Init connection error," +
                        " please check your password correct or maybe underlying service is unstable." +
                        " details: " + rootCause.getMessage(), rootCause);
            }
        }
    }

    private void tryConstruct() throws Exception {
        try {
            completer.init();
        } catch (Throwable e) {
            Throwable cause = e;
            boolean isTimeOut = false;
            while (cause != null) {
                if (cause instanceof SocketTimeoutException) {
                    isTimeOut = true;
                    break;
                }
                cause = cause.getCause();
            }

            if (isTimeOut) {
                logger.warn("jdbc connection of notebook {} to {} time out, retry...",
                        conf.getNotebookId(), conf.getHost());
                completer.init();
            } else {
                throw e;
            }
        }
    }

    @Override
    public void close() {
        completer.close();
        this.completer = null;
    }

    @Override
    protected InterpreterResult interpret(String code, String paragraphId) {
        return interpret(code, paragraphId, null);
    }

    @Override
    protected InterpreterResult interpret(String code, String paragraphId, InterpreterListener listener) {
        ExecutionContext context = new ExecutionContext(code);
        context.setParagraphId(Integer.valueOf(paragraphId));
        return interpret(context, listener);
    }

    protected InterpreterResult interpret(ExecutionContext context, InterpreterListener listener) {
        InterpreterResult result = null;
        try {
            // simple way
            logger.info("SQL started");
            result = completer.execute(context, listener);
            logger.info("SQL finished");
        } catch (DataAccessException e) {
            result = rootCaseIfException(e);
        } catch (Exception e) {
            result = errorMessageIfException(e, "Unchecked exception: ");
        } finally {
            result = replaceWithClosedMessageIfInterpreterClosed(result);
        }
        return result;
    }

    private InterpreterResult rootCaseIfException(Throwable e) {
        logger.error(e);
        return new InterpreterResultBuilder().error().rootCause(e);
    }

    private InterpreterResult errorMessageIfException(Throwable e, String message) {
        logger.error(message, e);
        return new InterpreterResultBuilder().error().because(message, e);
    }

    private InterpreterResult replaceWithClosedMessageIfInterpreterClosed(InterpreterResult result) {
        if (Objects.isNull(completer)
                || completer.isClosed()) {
            return new InterpreterResultBuilder().error().closed();
        }
        return result;
    }

    @Override
    protected InterpreterResult doExecute(ExecutionContext context) {
        return doExecute(context, null);
    }

    @Override
    protected InterpreterResult doExecute(ExecutionContext context, InterpreterListener listener) {
        return interpret(context, listener);
    }

    @Override
    public void cancel(ExecutionContext context) {
        completer.cancel();
    }

    @Override
    public List<Integer> cancelAll() {
        completer.cancel();
        return Collections.emptyList();
    }

    @Override
    public FormType getFormType() {
        // not support
        return FormType.NATIVE;
    }

    @Override
    public int getProgress(ExecutionContext context) {
        // not support
        return 0;
    }

    @Override
    public void doGc() {

    }
}
