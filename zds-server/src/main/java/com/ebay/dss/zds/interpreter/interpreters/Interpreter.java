package com.ebay.dss.zds.interpreter.interpreters;

import com.ebay.dss.zds.exception.InterpreterStoppedException;
import com.ebay.dss.zds.interpreter.interpreters.livy.ZLivySparkSqlInterpreter;
import com.ebay.dss.zds.interpreter.listener.InterpreterListener;
import com.ebay.dss.zds.interpreter.input.ExecutionContext;
import com.ebay.dss.zds.interpreter.output.result.JsonResult;
import com.ebay.dss.zds.interpreter.output.result.Result;
import com.ebay.dss.zds.interpreter.store.MemoryStore;
import com.ebay.dss.zds.interpreter.store.Store;
import org.apache.commons.lang.StringUtils;
import org.apache.zeppelin.interpreter.*;
import com.ebay.dss.zds.interpreter.interpreters.InterpreterGroup.StatusCode;

import org.apache.zeppelin.interpreter.thrift.InterpreterCompletion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * Created by tatian on 2018/4/25.
 */
public abstract class Interpreter {

    public static Logger logger = LoggerFactory.getLogger(Interpreter.class);
    private InterpreterGroup interpreterGroup;
    private String userName;

    private volatile Store<Object, Object> store;
    private final Integer lockObj = 0;
    private final Integer hookLock = 0;
    protected volatile List<Runnable> connectionTasks;
    protected volatile BlockingQueue<Runnable> closeHooks;

    public Interpreter(Properties prop) {
        this.prop = prop;
    }

    public void open() throws Exception {
        getInterpreterGroup().setStatus(StatusCode.STARTING);
        try {
            beforeConstruct();
            construct();
            afterConstruct();
        } catch (Exception e) {
            if (e instanceof InterpreterStoppedException) {
                throw e;
            } else throw new InterpreterException(e.getMessage());
        } finally {
            getInterpreterGroup().setSpare();
        }
    }

    public abstract boolean isOpened();

    // this method shouldn't impact the open process if fail
    protected void beforeConstruct() {}

    protected abstract void construct() throws Exception;

    // this method shouldn't impact the open process if fail
    protected void afterConstruct() {
        doConnectionTasks();
    }

    /**
     * Closes interpreter. You may want to free your resources up here.
     * close() is called only once
     */
    protected abstract void close();
    public void destroy() {
        try {
            close();
        } finally {
            doCloseHooks();
        }
    }

    public boolean addCloseHooks(Runnable hook) {
        if (closeHooks == null) {
            synchronized (hookLock) {
                if (closeHooks == null) {
                    closeHooks = new LinkedBlockingQueue<>();
                }
            }
        }
        return closeHooks.add(hook);
    }

    public boolean hasCloseHooks() {
        return this.closeHooks != null && this.closeHooks.size() > 0;
    }

    public void removeHooks(Runnable hook) {
        if (this.closeHooks != null && this.closeHooks.size() > 0) {
            this.closeHooks.remove(hook);
        }
    }

    protected BlockingQueue<Runnable> getCloseHooks() {
        return this.closeHooks;
    }

    // todo: maybe we don't need to clear it in another implementation
    protected void clearCloseHooks() {
        if (closeHooks != null) {
            closeHooks.clear();
        }
    }

    // We don't want to execute the hook for twice
    protected void doCloseHooks() {
        if (closeHooks != null && closeHooks.size() > 0) {
            while (closeHooks.size() > 0) {
                try {
                    Runnable runnable = closeHooks.poll();
                    if (runnable != null) {
                        runnable.run();
                    }
                } catch (Exception ex) {
                    logger.warn("Failed to execute close hook for interpreter: "
                            + getInterpreterGroup().getNoteId() + " class: " + this.getClassName() + " reason: ", ex);
                }
            }
        }
    }

    public boolean addConnectionTasks(Runnable task) {
        if (connectionTasks == null) {
            synchronized (closeHooks) {
                if (connectionTasks == null) {
                    connectionTasks = new LinkedList<>();
                }
            }
        }
        return connectionTasks.add(task);
    }

    protected List<Runnable> getConnectionTasks() {
        return this.connectionTasks;
    }

    // todo: maybe we don't need to clear it in another implementation
    protected void clearConnectionTasks() {
        if (connectionTasks != null) {
            connectionTasks.clear();
        }
    }

    protected void doConnectionTasks() {
        List<Runnable> tasks = getConnectionTasks();
        if (tasks != null && tasks.size() > 0) {
            for(Runnable runnable: tasks) {
                try {
                    runnable.run();
                } catch (Exception ex) {
                    logger.warn("Failed to execute connection task for interpreter: "
                                    + getInterpreterGroup().getNoteId() + " class: " + this.getClassName() + " reason: ", ex);
                }
            }
            clearConnectionTasks();
        }
    }

    public void clearStore() {
        if (store != null) store.clear();
    }

    public Store<Object, Object> getStore() {
        if (store == null) {
            synchronized (lockObj) {
                if (store == null) {
                    store = new MemoryStore<>();
                }
            }
        }
        return store;
    }

    public boolean hasStore() {
        return store != null && store.size() > 0;
    }

    @Deprecated
    public InterpreterResult interpret(String st, ExecutionContext context, InterpreterListener listener) {
        try {
            setPauseFlag(false);
            /**For life Cycle Management**/
            getInterpreterGroup().setStatus(StatusCode.RUNNING);
            if (StringUtils.isEmpty(st)) {
                return new InterpreterResult(InterpreterResult.Code.SUCCESS, "");
            }
            context.setInterpreter(this);
            return interpret(st, context.getParagraphId().toString(), listener);
        } finally {
            getInterpreterGroup().setSpare();
            getInterpreterGroup().keepAlive();
        }
    }

    @Deprecated
    public InterpreterResult interpret(String st, ExecutionContext context) {
        try {
            setPauseFlag(false);
            /**For life Cycle Management**/
            getInterpreterGroup().setStatus(StatusCode.RUNNING);
            if (StringUtils.isEmpty(st)) {
                return new InterpreterResult(InterpreterResult.Code.SUCCESS, "");
            }
            context.setInterpreter(this);
            return interpret(st, context.getParagraphId().toString());
        } finally {
            getInterpreterGroup().setSpare();
            getInterpreterGroup().keepAlive();
        }
    }

    @Deprecated
    public Result execute(String st, ExecutionContext context) {
        return new JsonResult(interpret(st, context));
    }

    public InterpreterResult execute(ExecutionContext context) {
        try {
            setPauseFlag(false);
            /**For life Cycle Management**/
            getInterpreterGroup().setStatus(StatusCode.RUNNING);
            if (StringUtils.isEmpty(context.getCode())) {
                return new InterpreterResult(InterpreterResult.Code.SUCCESS, "");
            }
            context.setInterpreter(this);
            return doExecute(context);
        } finally {
            getInterpreterGroup().setSpare();
            getInterpreterGroup().keepAlive();
        }
    }

    public InterpreterResult execute(ExecutionContext context, InterpreterListener listener) {
        try {
            setPauseFlag(false);
            /**For life Cycle Management**/
            getInterpreterGroup().setStatus(StatusCode.RUNNING);
            if (StringUtils.isEmpty(context.getCode())) {
                return new InterpreterResult(InterpreterResult.Code.SUCCESS, "");
            }
            context.setInterpreter(this);
            return doExecute(context, listener);
        } finally {
            getInterpreterGroup().setSpare();
            getInterpreterGroup().keepAlive();
        }
    }

    protected abstract InterpreterResult interpret(String code,
                                                   String paragraphId);

    protected abstract InterpreterResult interpret(String code,
                                                   String paragraphId,
                                                   InterpreterListener listener);

    protected abstract InterpreterResult doExecute(ExecutionContext context);

    protected abstract InterpreterResult doExecute(ExecutionContext context, InterpreterListener listener);

    /**
     * Optionally implement the canceling routine to abort interpret() method
     */
    public abstract void cancel(ExecutionContext context);

    public abstract List<Integer> cancelAll();

    /**
     * Dynamic form handling
     * see http://zeppelin.apache.org/docs/dynamicform.html
     *
     * @return FormType.SIMPLE enables simple pattern replacement (eg. Hello ${name=world}),
     * FormType.NATIVE handles form in API
     */
    public abstract FormType getFormType();

    /**
     * get interpret() method running process in percentage.
     *
     * @return number between 0-100
     */
    public abstract int getProgress(ExecutionContext context);


    /**
     * Get completion list based on cursor position.
     * By implementing this method, it enables auto-completion.
     *
     * @param buf    statements
     * @param cursor cursor position in statements
     * @return list of possible completion. Return empty list if there're nothing to return.
     */
    public List<InterpreterCompletion> completion(String buf, int cursor) {
        return null;
    }

    private volatile boolean paused = false;

    public boolean getPauseFlag() {
        return paused;
    }

    public void setPauseFlag(boolean flag) {
        this.paused = flag;
    }

    public boolean isPaused(){
        return getPauseFlag();
    }

    public boolean checkPaused(){
        boolean flag = isPaused();
        /**make sure only pause once**/
        setPauseFlag(false);
        return flag;
    }

    public void pause(){
        setPauseFlag(true);
    }

    public abstract void doGc();

    public void setProperties(Properties properties) {
        this.prop = properties;
    }

    public void setProperty(String key, String value) {
        this.prop.setProperty(key, value);
    }

    private Properties prop;

    public Properties getProperties() {
        return this.prop;
    }

    public String getProperty(String key) {
        String value = getProperties().getProperty(key);
        logger.debug("key: {}, value: {}", key, value);

        return value;
    }

    public String getProperty(String key, String defaultValue) {
        String value = getProperties().getProperty(key);
        logger.debug("key: {}, value: {}", key, value == null? defaultValue + " (default)" : value);

        return value == null? defaultValue : value;
    }


    public String getClassName() {
        return this.getClass().getName();
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserName() {
        return this.userName;
    }

    public void setInterpreterGroup(InterpreterGroup interpreterGroup) {
        this.interpreterGroup = interpreterGroup;
    }

    public InterpreterGroup getInterpreterGroup() {
        return this.interpreterGroup;
    }

    /**
     * Type of interpreter.
     */
    public static enum FormType {
        NATIVE, SIMPLE, NONE
    }

    public static class OperationType {
        public static final String OP_KEY = "intp_op_key";
        public static final String STATEMENT = "statement";
        public static final String DUMP = "dump";
    }

    public String toJson() {
        return "{\"className\": \"" + getClassName() + "\"}";
    }

    public void propsDescribe(Map<String, Object> describe) {
    }

}
