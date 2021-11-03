package com.ebay.dss.zds.interpreter.lifecycle;

import com.ebay.dss.zds.exception.LifeCycleException;
import com.ebay.dss.zds.interpreter.InterpreterFactory;
import com.ebay.dss.zds.interpreter.interpreters.InterpreterGroup;
import com.ebay.dss.zds.interpreter.interpreters.jdbc.JdbcInterpreter;
import com.ebay.dss.zds.message.EventQueueIdentifier;
import com.ebay.dss.zds.message.ZetaEvent;
import com.ebay.dss.zds.message.event.ZetaLifecycleEvent;
import com.ebay.dss.zds.message.queue.AbstractAsyncEventQueueGroup;
import com.ebay.dss.zds.message.queue.AsyncEventQueue;
import com.ebay.dss.zds.message.queue.AsyncPriorityEventQueue;
import com.ebay.dss.zds.message.queue.RoundRobinAsyncEventQueueGroup;
import com.ebay.dss.zds.message.sink.LifecycleEventSink;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Created by tatian on 2018/5/28.
 */
public class LifeCycleManager {

    protected static final Logger logger = LoggerFactory.getLogger(LifeCycleManager.class);

    private static final ConcurrentHashMap<InterpreterGroup, LifeCycleModel> lifeCycleMap =
            new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<String, Filter> userFilterMap =
            new ConcurrentHashMap<>();

    private static InterpreterFactory _factory;

    private static volatile boolean started = false;

    private Thread monitorThread;

    private static Long defaultMonitorInterval = 60000L;

    public static final String lifeCycleModeNamePrefix = "zds.interpreter.lifecycle.model";

    public static final String lifeCycleMonitorIntervalPrefix = "zds.interpreter.lifecycle.monitor.interval";

    public static final String userFilterModelPrefix = "zds.interpreter.lifecycle.monitor.filter.model";

    public static final String whitelistClassKey = "zds.interpreter.lifecycle.whitelist.class";
    public static final String whitelistNoteKey = "zds.interpreter.lifecycle.whitelist.note.prefix";

    public static final String monitorQueueNum = "zds.interpreter.lifecycle.monitor.queue.num";
    public static final String monitorQueueThreadNum = "zds.interpreter.lifecycle.monitor.queue.thread.num";
    public static final String monitorQueueSize = "zds.interpreter.lifecycle.monitor.queue.size";
    public static final String longCheckTimeOutKey = "zds.interpreter.lifecycle.monitor.check.timeout";

    private static final ArrayList<String> whitelistClass = new ArrayList<String>();
    private static final ArrayList<String> whitelistNotePrefix = new ArrayList<String>();

    private Properties prop;

    private Filter filter;

    private AbstractAsyncEventQueueGroup asyncEventQueueGroup;

    public LifeCycleManager(InterpreterFactory factory) {
        _factory = factory;
        prop = new Properties();
        initEventGroup();
    }

    public LifeCycleManager(InterpreterFactory factory, Properties prop) {
        this.prop = prop;
        _factory = factory;
        defaultMonitorInterval = Long.valueOf(prop.getProperty(lifeCycleMonitorIntervalPrefix, "60000"));
        if (prop.containsKey(userFilterModelPrefix)) {
            try {
                filter = (Filter) Class.forName(prop.getProperty(userFilterModelPrefix)).newInstance();
            } catch (Exception ex) {
                filter = new NumberFilter();
            }
            filter.apply(prop);
        }
        String whitelistClassValue = prop.getProperty(whitelistClassKey);
        if(StringUtils.isNoneEmpty(whitelistClassValue)) {
            Arrays.stream(whitelistClassValue.split(",")).forEach(c -> {
                whitelistClass.add(c);
                logger.info("LifeCycle whitelist class: " + c + " added");
            });
        }
        String whitelistNotePrefixValue = prop.getProperty(whitelistNoteKey);
        if(StringUtils.isNoneEmpty(whitelistNotePrefixValue)) {
            Arrays.stream(whitelistNotePrefixValue.split(",")).forEach(c -> {
                whitelistNotePrefix.add(c);
                logger.info("LifeCycle whitelist note prefix: " + c + " added");
            });
        }

        initEventGroup();
    }

    private Comparator<ZetaEvent> lifeCycleEventComparator = (ZetaEvent l, ZetaEvent r) -> {
        if ((l instanceof ZetaLifecycleEvent) && (r instanceof ZetaLifecycleEvent)) {
            ZetaLifecycleEvent e1 = (ZetaLifecycleEvent) l;
            ZetaLifecycleEvent e2 = (ZetaLifecycleEvent) r;
            int value1 = (e1.getLifeCycle().getCompanionObject() instanceof JdbcInterpreter) ? 1 : 0;
            int value2 = (e2.getLifeCycle().getCompanionObject() instanceof JdbcInterpreter) ? 1 : 0;
            return (value2 - value1);
        } else return 0;
    };

    private void initEventGroup() {
        int queueNum = Integer.valueOf(prop.getProperty(monitorQueueNum, "1"));
        int queueThreadNum = Integer.valueOf(prop.getProperty(monitorQueueThreadNum, "4"));
        int queueSize = Integer.valueOf(prop.getProperty(monitorQueueSize, "1000"));
        asyncEventQueueGroup = new RoundRobinAsyncEventQueueGroup();
        LifecycleEventSink lifecycleEventSink = new LifecycleEventSink(this);
        IntStream.rangeClosed(1, queueNum).forEach(i -> {
            AsyncEventQueue queue = new AsyncPriorityEventQueue(EventQueueIdentifier.LIFECYCLE.name() + "_" + i,
                    queueSize, lifeCycleEventComparator, queueThreadNum);
            boolean added = queue.addListener(lifecycleEventSink);
            if (!added) throw new LifeCycleException("The LifecycleEventSink is not added!");
            asyncEventQueueGroup.addQueue(queue);
        });
        asyncEventQueueGroup.start();
    }

    public LifeCycleModel createLifeCycle(Class cls, Properties properties) {
        LifeCycleModel life = null;
        try {
            life = (LifeCycleModel) Class.forName(cls.getName()).getConstructor(LifeCycleManager.class, Properties.class).newInstance(this, properties);
        } catch (Exception ex) {
            properties.setProperty(TimeoutModel.TIME_OUT, TimeoutModel.default_timeout);
            life = new TimeoutModel(this, properties);
        }
        return life;
    }

    public LifeCycleModel createLifeCycle(Properties properties) {
        LifeCycleModel life = null;
        try {
            life = (LifeCycleModel) Class.forName(properties.getProperty(lifeCycleModeNamePrefix, TimeoutModel.class.getName()))
                    .getConstructor(LifeCycleManager.class, Properties.class).newInstance(this, properties);
        } catch (Exception ex) {
            properties.setProperty(TimeoutModel.TIME_OUT, TimeoutModel.default_timeout);
            life = new TimeoutModel(this, properties);
        }
        return life;
    }

    public LifeCycleModel createLifeCycle() {
        LifeCycleModel life = null;
        try {
            life = (LifeCycleModel) Class.forName(prop.getProperty(lifeCycleModeNamePrefix, TimeoutModel.class.getName()))
                    .getConstructor(LifeCycleManager.class, Properties.class).newInstance(this, prop);
        } catch (Exception ex) {
            prop.setProperty(TimeoutModel.TIME_OUT, TimeoutModel.default_timeout);
            life = new TimeoutModel(this, prop);
        }
        return life;
    }

    public void register(InterpreterGroup intp, LifeCycleModel lifeCycle) {
        lifeCycleMap.put(intp.setLifeCycle(lifeCycle), lifeCycle.bind(intp));
    }

    public void unregister(InterpreterGroup intp) {
        lifeCycleMap.remove(intp);
    }

    public LifeCycleManager start(Long thread_interval) {
        started = true;
        Long interval = thread_interval == null && thread_interval <= 0L ? defaultMonitorInterval : thread_interval;
        if (this.monitorThread == null || !this.monitorThread.isAlive()) {
            this.monitorThread = new Thread(() ->
            {
                while (started) {
                    try {
                        Thread.sleep(interval);
                    } catch (InterruptedException ex) {
                        logger.info(ex.getMessage());
                    }
                    lifeCycleMap.values().forEach(x -> CheckCycle.apply(x));
                }
            });
            monitorThread.setName("Interpreters-Lifecycle-thread");
            logger.info("Starting lifecycle monitor thread...");
            monitorThread.start();
            logger.info("Thread started");
        }
        return this;
    }

    private Function<LifeCycleModel, Boolean> CheckCycle = (LifeCycleModel cycle) ->
        asyncEventQueueGroup.post(new ZetaLifecycleEvent.ZetaLifecycleCheckEvent(cycle));


    public boolean filtered(String userId) {
        if (filter != null && userFilterMap.containsKey(userId)) {
            return userFilterMap.get(userId).filtered();
        } else {
            return false;
        }
    }

    public void addRule(String userId) {
        if (filter == null) {
            logger.info("No rules for user: {} to add", userId);
        } else {
            if (userFilterMap.containsKey(userId)) {
                logger.info("Add rule for user: {} of Rule: {}", userId, filter.getClass().getName());
                Filter filter = userFilterMap.get(userId);
                filter.addRule();
                logger.info("Add rule for user: {} of Rule: {} done. User: {}'s status: {}",
                        userId, filter.getClass().getName(), userId, filter.introduce());
            } else {
                userFilterMap.put(userId, filter.create());
                logger.info("Add rule for user: {} of Rule: {} done", userId, filter.getClass().getName());
            }
        }
    }

    public void removeRule(String userId) {
        if (filter != null && userFilterMap.containsKey(userId)) {
            logger.info("Remove rule for user: {} of Rule: {}", userId, filter.getClass().getName());
            Filter filter = userFilterMap.get(userId);
            filter.removeRule();
            logger.info("Remove rule for user: {} of Rule: {} done. User: {}'s status: {}",
                    userId, filter.getClass().getName(), userId, filter.introduce());
        } else {
            logger.info("No rules for user: {} to remove", userId);
        }
    }

    public void removeRules(String userId) {
        if (userFilterMap.containsKey(userId)) {
            logger.info("Remove user: {} out of Rule: {}", userId, filter.getClass().getName());
            userFilterMap.remove(userId);
            logger.info("Remove user: {} out of Rule: {} done", userId, filter.getClass().getName());
        } else {
            logger.info("User: {} is not added in the filter list", userId);
        }
    }

    public String explainUserRules(String userId) {
        if (userFilterMap.containsKey(userId)) {
            return userFilterMap.get(userId).explain();
        } else {
            return "No rules for user: {} to explain";
        }
    }

    public String introduceUserRules(String userId) {
        if (userFilterMap.containsKey(userId)) {
            return userFilterMap.get(userId).introduce();
        } else {
            return "No rules for user: {} to introduce";
        }
    }

    public void stop() {
        started = false;
    }

    protected InterpreterFactory getInterpreterFactory() {
        return _factory;
    }

    public boolean whiteListClassName(String className) {
        return whitelistClass.contains(className);
    }

    public boolean whiteListNoteId(String nodeId) {
        return whitelistNotePrefix.stream().anyMatch(prefix -> nodeId.contains(prefix));
    }

    public Map<InterpreterGroup, LifeCycleModel> getAllLifecycles() {
        return this.lifeCycleMap;
    }

    public Properties getProp() {
        return this.prop;
    }
}
