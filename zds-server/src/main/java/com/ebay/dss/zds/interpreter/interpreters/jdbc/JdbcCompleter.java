package com.ebay.dss.zds.interpreter.interpreters.jdbc;

import com.ebay.dss.zds.common.ExceptionUtil;
import com.ebay.dss.zds.exception.InterpreterException;
import com.ebay.dss.zds.exception.JDBCConnectionCheckException;
import com.ebay.dss.zds.exception.JDBCConnectionClosedException;
import com.ebay.dss.zds.interpreter.input.ExecutionContext;
import com.ebay.dss.zds.interpreter.listener.InterpreterListener;
import com.ebay.dss.zds.interpreter.listener.InterpreterListenerData;
import com.ebay.dss.zds.interpreter.monitor.modle.JobReport;
import com.ebay.dss.zds.interpreter.monitor.modle.Status;
import com.ebay.dss.zds.interpreter.output.InterpreterResultBuilder;
import com.ebay.dss.zds.interpreter.output.result.JsonResult;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.zeppelin.interpreter.InterpreterResult;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.SQLErrorCodes;
import org.springframework.jdbc.support.SQLErrorCodesFactory;

import javax.sql.DataSource;
import java.io.Closeable;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import static com.ebay.dss.zds.interpreter.interpreters.Interpreter.OperationType.DUMP;
import static com.ebay.dss.zds.interpreter.interpreters.Interpreter.OperationType.STATEMENT;

public class JdbcCompleter implements AutoCloseable {
    private static final Logger logger = LogManager.getLogger();
    private JdbcTemplate template;
    protected IJdbcConf conf;
    protected volatile boolean busy = false;
    private volatile boolean closed = false;
    private volatile CancelWatcher cancelWatcher;
    protected InterpreterListenerData listenerData;
    protected volatile ExecutionContext currentContext;
    private SQLErrorCodesFactory sqlErrorCodesFactory;
    private final ReentrantLock _lock = new ReentrantLock();

    JdbcCompleter(IJdbcConf conf) {
        this.conf = conf;
        this.sqlErrorCodesFactory = SQLErrorCodesFactory.getInstance();
    }

    public IJdbcConf getConf() {
        return conf;
    }

    protected static StatementPostProcessor getPostProcessor(ExecutionContext context,
                                                             JdbcCompleter completer) {
        switch (context.getOperationType()) {
            case DUMP:
                return new DumpStatementPostProcessor(context, completer);
            case STATEMENT:
            default:
                return new SimpleQueryStatementPostProcessor(completer);
        }
    }

    void init() throws Exception {
        Callable<DataSource> dataSourceSupplier = this.conf.getDataSourceSupplier();
        DataSource dataSource = dataSourceSupplier.call();
        if (Objects.isNull(dataSource)) {
            throw new InterpreterException("JDBC DataSource is not established correctly, please check if connection params are correct.");
        }
        this.template = getJdbcTemplate(dataSource);
        this.registerDataSource(dataSource);
    }

    private void registerDataSource(DataSource dataSource) {
        try {
            this.sqlErrorCodesFactory.getErrorCodes(dataSource);
            logger.info("DataSource: {} registered", dataSource.getClass().getName());
        } catch (Exception ex) {
            logger.error("Error when register database: " + ex.getMessage());
        }
    }

    private void unregisterDataSource(DataSource dataSource) {
        try {
            this.sqlErrorCodesFactory.unregisterDatabase(dataSource);
            logger.info("DataSource: {} unregistered", dataSource.getClass().getName());
        } catch (Exception ex) {
            logger.error("Error when unregister database: " + ex.getMessage());
        }
    }

    JdbcTemplate getJdbcTemplate(DataSource ds) {
        return new JdbcTemplate(ds);
    }

    JdbcTemplate getJdbcTemplate() {
        return this.template;
    }

    public boolean isClosed() {
        return closed;
    }

    public boolean isLocked() {
        return this._lock.isLocked();
    }

    @Override
    public void close() {
        this.closed = true;
        if (Objects.isNull(template)) {
            logger.warn("current jdbc completer is already closed");
            return;
        }
        DataSource ds = template.getDataSource();
        if (ds instanceof Closeable) {
            try {
                ((Closeable) ds).close();
            } catch (IOException e) {
                logger.error(e);
            } finally {
                unregisterDataSource(ds);
            }
        }
    }

    public boolean isValid() {
        try {
            if (conf.getJdbcType().testPeriodically) {
                doValidation();
            } else {
                logger.debug("JDBC connection test sql is disabled, skip test query as valid...");
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    void doValidation() throws JDBCConnectionClosedException, JDBCConnectionCheckException {
        logger.debug("Check if JDBC connection is valid...");
        if (closed) {
            logger.error(JDBCConnectionClosedException.defaultMessage + ", invalid...");
            throw new JDBCConnectionClosedException("Connection is marked as closed");
        }
        if (busy) {
            logger.debug("JDBC connection is busy, skip test query as valid...");
            return;
        }
        try {
            _lock.lock();
            template.execute(conf.getJdbcType().testSQL);
            logger.debug("Test query done, valid...");
        } catch (DataAccessException e) {
            logger.error(JDBCConnectionCheckException.defaultMessage + ", invalid...");
            throw new JDBCConnectionCheckException(JDBCConnectionCheckException.defaultMessage
                    + ": " + ExceptionUtil.locate(e, SQLException.class, false).toString(), e);
        } catch (Exception e) {
            logger.error("Jdbc exception: ", e);
            throw e;
        } finally {
            _lock.unlock();
        }
    }

    public synchronized InterpreterResult execute(final ExecutionContext context, InterpreterListener listener) {
        logger.debug(context.getCode());
        try {
            _lock.lock();
            return template.execute(
                    (PreparedStatementCreator) con -> con.prepareStatement(context.getCode()),
                    ps -> proxyPreparedStatementCallback(context, ps, listener));
        } finally {
            _lock.unlock();
        }
    }

    InterpreterResult proxyPreparedStatementCallback(
            ExecutionContext context,
            PreparedStatement ps,
            InterpreterListener listener) {
        preExecution(context, listener);
        InterpreterResult result = checkCancelBeforeReturnOrThrow(context, ps, listener);
        postExecution(result, listener);
        return result;
    }

    private InterpreterResult checkCancelBeforeReturnOrThrow(ExecutionContext context,
                                                             PreparedStatement ps,
                                                             InterpreterListener listener) {
        InterpreterResult result;
        try {
            result = runPreparedStatement(context, ps, listener);
        } catch (Exception e) {
            result = new InterpreterResultBuilder()
                    .error()
                    .rootCause(e); // use root cause here rather than because
                    //.because(e.getMessage(), e);
        }

        return result;
    }

    protected FutureTask<Boolean> asyncExecute(ExecutionContext context,
                                               PreparedStatement ps,
                                               InterpreterListener listener) {
        FutureTask<Boolean> isResultSetFutureTask = new FutureTask<>(ps::execute);
        Thread statementExecutionThread = new Thread(isResultSetFutureTask);
        String threadName = String.format("jdbc-statement-execution-%s-%s-%s",
                conf.getUser(), conf.getJdbcType(), context.getJobId());
        statementExecutionThread.setName(threadName);
        statementExecutionThread.start();
        return isResultSetFutureTask;
    }

    protected InterpreterResult runPreparedStatement(ExecutionContext context,
                                                     PreparedStatement ps,
                                                     InterpreterListener listener) throws Exception {
        if (cancelled(context)) {
            deregisterCancellation();
            return new InterpreterResultBuilder()
                    .error()
                    .cancelled();
        }
        FutureTask<Boolean> isResultSetFutureTask = asyncExecute(context, ps, listener);
        registerCancellation(new CancelWatcher(context, ps));
        inExecution(context, isResultSetFutureTask, listener);

        return handleResultTask(context, ps, isResultSetFutureTask);
    }

    protected InterpreterResult handleResultTask(
            ExecutionContext context,
            PreparedStatement ps,
            FutureTask<Boolean> isResultSetFutureTask) throws Exception {
        try {
            boolean isResultSet = isResultSetFutureTask.get();
            InterpreterResult result;
            logger.info("jdbc sql statement execution done, start result set resolving...");
            if (!isResultSet) {
                result = new SimpleUpdateStatementPostProcessor().execute(ps);
            } else {
                StatementPostProcessor postProcessor = getPostProcessor(context, this);
                result = (InterpreterResult) postProcessor.execute(ps);
            }
            logger.info("interpreter result building done!");
            return result;
        } catch (Exception ex) {
            if (cancelled(context)) {
                logger.info("The job: {} already been canceled, ignore the result", context.getJobId());
                return new InterpreterResultBuilder()
                        .error()
                        .cancelled();
            } else throw ex;
        }
    }

    private void executeStatementCallback(StatementContext context) {
        Function<Object, InterpreterResult> statementCallback =
                context.getExecutionContext().getOperationCallback(STATEMENT);
        if (Objects.nonNull(statementCallback)) {
            statementCallback.apply(context);
        }
    }

    protected void preExecution(ExecutionContext context,
                                InterpreterListener listener) {
        currentContext = context;
        busy = true;
        listenerData = new InterpreterListenerData();
        if (Objects.nonNull(listener)) {
            listener.beforeStatementSubmit(listenerData);
            JobReport report = new JobReport();
            report.setStatus(Status.StatusEnum.RUNNING);
            listener.statementProgress(listenerData, report);
            listener.afterStatementSubmit(listenerData);
        }
    }

    protected void inExecution(ExecutionContext context,
                               FutureTask<Boolean> isResultSetFutureTask,
                               InterpreterListener listener) {
        if (listener == null) return;
        if (this.cancelWatcher != null) executeStatementCallback(this.cancelWatcher);
        InterpreterListenerData data = InterpreterListenerData.Empty;
        while (!isResultSetFutureTask.isDone()) {
            listener.statementProgress(data, new JobReport().setStatus(Status.StatusEnum.RUNNING));
            try {
                Thread.sleep(1000L);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void postExecution(InterpreterResult result,
                               InterpreterListener listener) {
        if (Objects.nonNull(listener)) {
            listener.afterStatementFinish(listenerData, new JsonResult(result));
            listenerData = null;
        }
        busy = false;
    }

    protected void deregisterCancellation() {
        this.cancelWatcher = null;
    }

    protected void registerCancellation(CancelWatcher cancelWatcher) {
        this.cancelWatcher = cancelWatcher;
    }

    protected boolean cancelled(ExecutionContext context) {
        if (Objects.nonNull(cancelWatcher)) {
            return cancelWatcher.cancelled(context);
        }
        return false;
    }

    public void cancel() {
        if (Objects.nonNull(currentContext)) {
            cancelCurrent();
        } else {
            cancelNext();
        }
    }

    private void cancelCurrent() {
        if (Objects.nonNull(cancelWatcher)) {
            logger.info("Try cancel current JDBC execution at job level, note: " +
                    currentContext.getNoteId() + ", job: " + currentContext.getJobId());
            cancelWatcher.cancel(currentContext);
        }
    }


    private void cancelNext() {
        registerCancellation(new CancelledWatcher(null, null));
    }

    private static class CancelledWatcher extends CancelWatcher {

        CancelledWatcher(ExecutionContext context, PreparedStatement ps) {
            super(context, ps);
        }

        @Override
        public boolean cancelled(ExecutionContext context) {
            return true;
        }
    }

    public static class CancelWatcher implements StatementContext {

        protected ExecutionContext context;
        protected PreparedStatement ps;
        protected boolean cancelled = false;

        CancelWatcher(ExecutionContext context, PreparedStatement ps) {
            this.context = context;
            this.ps = ps;
        }

        private boolean sameJob(ExecutionContext context) {
            return StringUtils.equals(context.getJobId(), this.context.getJobId());
        }

        public boolean cancelled(ExecutionContext context) {
            if (sameJob(context)) {
                return cancelled;
            }
            return false;
        }

        public void cancel(ExecutionContext context) {
            if (sameJob(context)) {
                cancelled = true;
                try {
                    logger.info("Execute cancelling for ps, noteId: {}, jobId: {}", context.getNoteId(), context.getJobId());
                    ps.cancel();
                    logger.info("Ps cancelling finished, noteId: {}, jobId: {}", context.getNoteId(), context.getJobId());
                } catch (SQLException e) {
                    logger.error("cancel statement error, ", e);
                }
            }
        }

        @Override
        public ExecutionContext getExecutionContext() {
            return context;
        }

        @Override
        public PreparedStatement getPreparedStatement() {
            return ps;
        }
    }
}
