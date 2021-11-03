package com.ebay.dss.zds.interpreter;

import com.ebay.dss.zds.common.Constant;
import com.ebay.dss.zds.exception.InterpreterException;
import com.ebay.dss.zds.exception.InterpreterStoppedException;
import com.ebay.dss.zds.exception.LifeCycleException;
import com.ebay.dss.zds.interpreter.interpreters.Interpreter;
import com.ebay.dss.zds.interpreter.interpreters.InterpreterGroup;
import com.ebay.dss.zds.interpreter.lifecycle.LifeCycleManager;
import com.ebay.dss.zds.interpreter.lifecycle.LifeCycleModel;
import com.ebay.dss.zds.packages.PackagesSubject;
import com.ebay.dss.zds.state.Recoverable;
import com.ebay.dss.zds.state.StateManager;
import com.ebay.dss.zds.state.StateSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Created by tatian on 2018/4/23.
 */
@Component
public class InterpreterFactory implements Recoverable {

    public static final String KEY_SEP = Constant.GLOBAL.KEY_SEP;
    private static final Logger logger = LoggerFactory.getLogger(InterpreterFactory.class);
    private final transient ReentrantReadWriteLock.ReadLock interpreterGroupReadLock;
    private final transient ReentrantReadWriteLock.WriteLock interpreterGroupWriteLock;
    private final ConcurrentHashMap<String, InterpreterGroup> allInterpreterGroups =
            new ConcurrentHashMap<>();
    @Value("${zds.interpreter.config.path}")
    private String confPath;
    @Value("${zds.interpreter.lifecycle.monitor.open}")
    private boolean monitored;
    private LifeCycleManager lifeCycleManager;
    private LifeCycleModel lifeCycle;
    private boolean isMonitored = false;
    private InterpreterBaseFactory baseFactory;
    private StateManager stateManager;

    public InterpreterFactory(InterpreterBaseFactory baseFactory,
                              StateManager stateManager) {
        this.baseFactory = baseFactory;
        this.stateManager = stateManager;
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        interpreterGroupReadLock = lock.readLock();
        interpreterGroupWriteLock = lock.writeLock();
    }

    @PostConstruct
    private void init() {
        if (monitored) {
            enableLifeCycleMonitor(confPath);
        }
    }

    public InterpreterBaseFactory getBaseFactory() {
        return baseFactory;
    }

    public InterpreterFactory enableLifeCycleMonitor(String configPath) {
        if (!isMonitored) {
            synchronized (this) {
                isMonitored = true;
                InterpreterConfiguration conf = new InterpreterConfiguration(configPath);
                lifeCycleManager = new LifeCycleManager(this, conf.getProperties()).start(
                        Long.valueOf(conf.getOrDefault(LifeCycleManager.lifeCycleMonitorIntervalPrefix, "60000")));
                lifeCycle = lifeCycleManager.createLifeCycle(conf.getProperties());
            }
        }
        return this;
    }

    public InterpreterFactory enableLifeCycleMonitor(InterpreterConfiguration conf) {
        if (!isMonitored) {
            synchronized (this) {
                isMonitored = true;
                lifeCycleManager = new LifeCycleManager(this, conf.getProperties()).start(
                        Long.valueOf(conf.getOrDefault(LifeCycleManager.lifeCycleMonitorIntervalPrefix, "60000")));
                lifeCycle = lifeCycleManager.createLifeCycle(conf.getProperties());
            }
        }
        return this;
    }

    public Interpreter getInterpreter(String userId, String noteId, String className, InterpreterConfiguration conf) {
        return getInterpreter(userId, noteId, className, conf.getProperties());
    }

    public Interpreter getInterpreter(String userId, String noteId, String className, Properties prop) throws InterpreterException, LifeCycleException {
        if (userId == null) {
            throw new InterpreterException("The user id is null");
        }
        if (noteId == null) {
            throw new InterpreterException("The note id is null");
        }
        InterpreterGroup intpGrp = getOrCreateInterpreterGroup(userId, noteId);
        Interpreter intp = intpGrp.getOrCreateInterpreter(className, prop);
        intp.setPauseFlag(false);
        return intp;
    }


    public InterpreterGroup getOrCreateInterpreterGroup(String userId, String noteId) throws LifeCycleException {
        InterpreterGroup intpGrp = searchInterpreterGroup(userId, noteId);
        if (intpGrp == null) {
            logger.info("Create interpreter group for user: {}", userId);
            intpGrp = createInterpreterGroup(userId, noteId);
        }
        return intpGrp;

    }

    public void openInterpreter(String userId, String noteId, String className, Properties prop) throws Exception {
        InterpreterGroup intpGrp = searchInterpreterGroup(userId, noteId);
        if (intpGrp == null || intpGrp.getInterpreter(className) == null) {
            getInterpreter(userId, noteId, className, prop);
        } else {
            Interpreter intp = intpGrp.getInterpreter(className);
            intp.pause();
            intp.destroy();
            intp.open();
        }
    }

    public Interpreter reopenInterpreter(String userId, String noteId, String className, Properties prop) throws LifeCycleException, InterpreterException {
        logger.info("Delete interpreter group of user: {} with note: {}", userId, noteId);
        try {
            removeInterpreterGroup(userId, noteId, true);
        } catch (InterpreterStoppedException ex) {
            logger.info("Stopped previous connecting process");
        }
        logger.info("Reopen interpreter of user: {} with note: {}", userId, noteId);
        Interpreter interpreter = getInterpreter(userId, noteId, className, prop);
        logger.info("Reopen interpreter of user: {} with note: {} done", userId, noteId);
        return interpreter;
    }

    private InterpreterGroup createInterpreterGroup(String userId, String noteId) throws LifeCycleException {
        String grpKey = getGrpKey(userId, noteId);

        try {
            interpreterGroupWriteLock.lock();
            logger.debug("create interpreter group with groupId:" + grpKey);
            InterpreterGroup intpGrp = new InterpreterGroup(grpKey);
            intpGrp.setUserName(userId);
            intpGrp.setNoteId(noteId);
            intpGrp.setInterpreterFactory(this);
            allInterpreterGroups.put(grpKey, intpGrp);

            /**register life cycle**/
            registerLifeCycle(intpGrp, userId);
            /**register life cycle**/
            logger.info("Interpreter group: {} of user: {} is created ", intpGrp.getGroupId(), userId);
            return intpGrp;
        } finally {
            interpreterGroupWriteLock.unlock();
        }
    }

    protected void removeRules(String userId) throws LifeCycleException {
        if (isMonitored) {
            lifeCycleManager.removeRules(userId);
        }
    }

    private void registerLifeCycle(InterpreterGroup intpGrp, String userId) {
        /**register life cycle**/
        if (isMonitored) {
            logger.info("Register to lifecycle manager...");
            lifeCycleManager.register(intpGrp, lifeCycle.create());
            logger.info("done");
        }
        /**register life cycle**/
    }

    public InterpreterGroup searchInterpreterGroup(String userId, String noteId) {
        InterpreterGroup intpGrp = null;
        try {
            logger.info("Search interpreter group of userId = [{}], noteId = [{}]...", userId, noteId);
            interpreterGroupReadLock.lock();
            intpGrp = allInterpreterGroups.getOrDefault(getGrpKey(userId, noteId), null);
            //interpreterGroupReadLock.unlock();
        } catch (Exception e) {
            logger.error(e.getMessage());
        } finally {
            interpreterGroupReadLock.unlock();
            if (intpGrp != null) {
                logger.info("Found one: userId = [{}], noteId = [{}]", userId, noteId);
            } else {
                logger.info("No interpreter group found");
            }
        }
        return intpGrp;
    }

    @SuppressWarnings("this is a method without lock, shouldn't use it in interpreter creation transaction")
    public InterpreterGroup findInterpreterGroup(String grpKey) {
        return allInterpreterGroups.get(grpKey);
    }
    public InterpreterGroup findInterpreterGroup(String userId, String noteId) {
        return findInterpreterGroup(getGrpKey(userId, noteId));
    }

    public Interpreter findInterpreter(String userId, String noteId, String className) throws InterpreterException {
        InterpreterGroup intpGrp = searchInterpreterGroup(userId, noteId);
        Interpreter interpreter = null;
        if (intpGrp == null) {
            throw new InterpreterException("The previous session is closed, please reconnect");
        } else {
            interpreter = intpGrp.getInterpreter(className);
            if (interpreter == null) {
                throw new InterpreterException("The previous session is closed, please reconnect");
            } else {
                interpreter.setPauseFlag(false);
            }
        }
        return interpreter;
    }

    public List<Map.Entry<String, InterpreterGroup>> searchInterpreterGroups(String userId) {
        List<Map.Entry<String, InterpreterGroup>> intpGrps = null;
        try {
            interpreterGroupReadLock.lock();
            intpGrps = allInterpreterGroups
                    .entrySet()
                    .stream()
                    .filter(kv -> {
                        return isGrpKeyPrefix(userId, kv.getKey());
                    })
                    .collect(Collectors.toList());
            if (intpGrps.size() == 0) intpGrps = null;
        } finally {
            interpreterGroupReadLock.unlock();
        }
        return intpGrps;
    }

    public boolean removeInterpreterGroup(String grpKey, boolean forceShutDown) {
        return removeInterpreterGroup(grpKey, forceShutDown, true);
    }

    public boolean removeInterpreterGroup(String grpKey, boolean forceShutDown, boolean async) {
        InterpreterGroup intpGrp;
        try {
            interpreterGroupWriteLock.lock();
            if (!allInterpreterGroups.containsKey(grpKey)) {
                logger.info("The interpreter group of " + grpKey + " is not exist");
                return true;
            } else {
                intpGrp = allInterpreterGroups.remove(grpKey);
                intpGrp.setShutDownAsync(async);
                if (isMonitored) {
                    // use the grp key to remove rather than instance
                    lifeCycleManager.unregister(intpGrp);
                    logger.info("Interpreter group:" + grpKey + " is removed from life cycle management");
                    // push down to do in the InterpreterGroup
                    //lifeCycleManager.removeRule(getUserFromKey(grpKey));
                    //logger.info("One rule removed from user");
                }
                logger.debug("Closing interpreter group: " + grpKey);
                if (forceShutDown) {
                    intpGrp.shutdownAllInterpreters();
                } else {
                    intpGrp.safeClose();
                }
                logger.info("Interpreter group:" + grpKey + " closed");
                return true;
            }
        } finally {
            interpreterGroupWriteLock.unlock();
        }
    }

    public boolean removeInterpreterGroup(String userId, String noteId, boolean forceShutDown) {
        String grpKey = getGrpKey(userId, noteId);
        return removeInterpreterGroup(grpKey, forceShutDown);
    }

    public void closeInterpreter(String userId, String noteId, String className) {
        InterpreterGroup intpGrp = searchInterpreterGroup(userId, noteId);
        if (intpGrp != null) {
            intpGrp.removeInterpreter(className);
        }
    }

    public List<Integer> cleanStatements(String userId, String noteId, Class cls) {
        InterpreterGroup intpGrp = searchInterpreterGroup(userId, noteId);
        List<Integer> stmtids = new ArrayList<>();
        if (intpGrp == null) {
            return stmtids;
        } else {
            Interpreter intp = intpGrp.getInterpreter(cls.getName());
            if (intp != null) {
                logger.info("Stopping the progress of interpreter: [" + intp.getClassName() + "] of user: [" + userId + "] 's notebook: [" + noteId + "]");

                /**1.interrupt the current process**/
                logger.debug("Stopping: 1.interrupt the current process...");
                intp.pause();
                logger.debug("Stopping: 1.interrupt the current process: done");

                /**kill all the statements**/
                logger.debug("Stopping: 2.kill all the statements...");
                stmtids.addAll(intp.cancelAll());
                logger.debug("Stopping: 2.kill all the statements: done");

                logger.info("Successfully stopped the interpreter: [" + intp.getClassName() + "] of user: [" + userId + "] 's notebook: [" + noteId + "]");
            }
            return stmtids;
        }
    }


    public static String getGrpKey(String userId, String noteId) {
        return Constant.GLOBAL.workingUnitKey(userId, noteId);
    }

    public String getGrpKey(PackagesSubject.SubjectKey subjectKey) {
        return subjectKey.toString();
    }

    protected boolean isGrpKeyPrefix(String pre, String key) {
        return key.startsWith(pre + KEY_SEP);
    }

    protected boolean clear(boolean forceShutDown) {
        logger.info("Shutdown the interpreter factory...");
        synchronized (allInterpreterGroups) {
            allInterpreterGroups
                    .forEach((key, value) -> {
                        logger.info("Shutdown interpreter group: " + key + "...");
                        if (forceShutDown) {
                            value.shutdownAllInterpreters();
                        } else {
                            value.safeClose();
                        }
                        if (isMonitored) {
                            lifeCycleManager.unregister(value);
                            lifeCycleManager.removeRule(value.getUserName());
                        }
                        logger.info("Shutdown interpreter group: ok");
                    });
            allInterpreterGroups.clear();
        }
        logger.info("Shutdown the interpreter factory: finished");
        return true;
    }

    protected ConcurrentHashMap<String, InterpreterGroup> getAllInterpreterGrps() {
        return allInterpreterGroups;
    }

    protected boolean shutDown(boolean forceShutDown) {
        clear(forceShutDown);
        return true;
    }

    public LifeCycleManager getLifeCycleManager() {
        return lifeCycleManager;
    }

    public boolean isMonitored() {
        return isMonitored;
    }

    public StateManager getStateManager() {
        return this.stateManager;
    }

    @Override
    public StateSnapshot createSnapshot() {
        // todo: implement
        return StateSnapshot.EMPTY;
    }

    @Override
    public boolean doRecover(StateSnapshot stateSnapshot) {
        logger.info("Start to recover from previous status...");
        // todo: implement
        logger.info("No need to recover status");
        return false;
    }

    @Override
    public String recoverKey() {
        return "";
    }
}
