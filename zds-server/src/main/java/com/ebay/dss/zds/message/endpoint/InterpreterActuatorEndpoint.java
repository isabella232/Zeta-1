package com.ebay.dss.zds.message.endpoint;

import com.ebay.dss.zds.interpreter.InterpreterManager;
import com.ebay.dss.zds.service.MonitorService;
import com.ebay.dss.zds.message.status.LivyStatementStatusStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by tatian on 2019-06-19.
 */
@Endpoint(id = "interpreter")// add this only for actuator exposure purpose
@Component
public class InterpreterActuatorEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(InterpreterActuatorEndpoint.class);

    @Autowired
    private InterpreterManager manager;

    @Autowired
    private MonitorService monitorService;

    @Value("${zds.track.long-running.threshold}")
    private long longRunningThreshold;

    @Value("${zds.track.big-cpu-time.threshold}")
    private long bigCPUTimeThreshold;

    @Value("${zds.track.big-memory-seconds.threshold}")
    private long bigMemorySecondsThreshold;

    @ReadOperation
    public Map<String, String> ping(@Nullable String pattern) {
        Map<String, String> result = new HashMap<>();
        result.put("endpoint", this.getClass().getSimpleName());
        return result;
    }

    @ReadOperation
    public Map<String, String> invoke(@Selector String pattern) {
        // todo: here we defined the
        Map<String, String> result = new HashMap<>();
        result.put("endpoint", this.getClass().getSimpleName());
        result.put(pattern, String.valueOf(getInterpreterStatusCount(pattern)));
        return result;
    }

    public long getInterpreterStatusCount(String pattern) {
        long result;
        switch (pattern) {
            case "created":
                result = manager.getCreatedInterpreterGroupCount();
                break;
            case "idle":
                // including dead one
                result = manager.getIdleInterpreterGroupCount();
                break;
            case "running":
                result = manager.getRunningInterpreterGroupCount();
                break;
            case "connecting":
                result = manager.getConnectingInterpreterGroupCount();
                break;
            case "all":
                result = manager.getAllInterpreterGroupCount();
                break;
            default:
                logger.error("unknown pattern: {}", pattern);
                result = 0;
                break;
        }
        return result;
    }

    public long getLongRunningCount() {
        if (longRunningThreshold == 0) return 0;
        LivyStatementStatusStore store = monitorService.getLivyStatementStatusStore();
        return store.getLiveStatements()
                .values()
                .stream()
                .filter(entry -> entry.getDuration() >= longRunningThreshold)
                .count();
    }

    public long getBigCPUTimeCount() {
        if (bigCPUTimeThreshold == 0) return 0;
        LivyStatementStatusStore store = monitorService.getLivyStatementStatusStore();
        return store.getLiveStatements()
                .values()
                .stream()
                .filter(entry -> {
                    long cputTime = Long.valueOf(entry.metrics.getOrDefault("totalCPUTime", "0"));
                    return cputTime >= bigCPUTimeThreshold;
                }).count();
    }

    public long getBigMemorySecondsCount() {
        if (bigMemorySecondsThreshold == 0) return 0;
        LivyStatementStatusStore store = monitorService.getLivyStatementStatusStore();
        return store.getLiveStatements()
                .values()
                .stream()
                .filter(entry -> {
                    long memorySeconds = Long.valueOf(entry.metrics.getOrDefault("totalMemorySeconds", "0"));
                    return memorySeconds >= bigMemorySecondsThreshold;
                }).count();
    }

}
