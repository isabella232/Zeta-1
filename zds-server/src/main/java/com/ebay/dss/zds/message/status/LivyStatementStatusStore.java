package com.ebay.dss.zds.message.status;

import com.ebay.dss.zds.interpreter.monitor.modle.JobReport;
import com.ebay.dss.zds.message.event.ZetaMonitorEvent;

import javax.annotation.concurrent.NotThreadSafe;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import com.ebay.dss.zds.message.event.ZetaMonitorEvent.StatementStatus;
import com.ebay.dss.zds.message.status.persistence.Persistence;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by tatian on 2019-07-20.
 */
public class LivyStatementStatusStore extends StatusStore<ZetaMonitorEvent.LivyStatementMonitorEvent,
        LivyStatementStatusStore.StatusEntry> {

    private static final Logger logger = LoggerFactory.getLogger(LivyStatementStatusStore.class);

    private ConcurrentHashMap<String, StatusEntry> liveStatements = new ConcurrentHashMap<>();

    private Persistence<StatusEntry> persistence;

    /**
     * This method is not totally thread safe because the StatusEntry is not thread safe,
     * but as long as one StatusEntry is always handled by a same thread, it should be thread safe
     **/
    @Override
    protected Optional<StatusEntry> flushBy(ZetaMonitorEvent.LivyStatementMonitorEvent event) {
        try {
            StatusEntry updateEntry = null;
            if (event.getStatus().equals(StatementStatus.START)) {
                updateEntry = StatusEntry.fromStatementEvent(event);
                String key = updateEntry.getKey();
                liveStatements.put(key, updateEntry);
                logger.info("Got new statement: " + key);
            } else if (event.getStatus().equals(StatementStatus.RUNNING)) {
                String key = generateKey(event);
                if (liveStatements.containsKey(key)) {
                    updateEntry = liveStatements.get(key);
                    updateEntry.update(event);
                } else {
                    updateEntry = StatusEntry.fromStatementEvent(event);
                    String newKey = updateEntry.getKey();
                    liveStatements.put(newKey, updateEntry);
                    logger.warn("Got new statement: {} in wrong status, but still update", key);
                }
            } else if (event.getStatus().equals(StatementStatus.END)) {
                String key = generateKey(event);
                updateEntry = liveStatements.remove(key);
                if (updateEntry != null) {
                    updateEntry.status = StatementStatus.END;
                    logger.info("Removed statement: " + key);
                }
            }
            return Optional.ofNullable(updateEntry);
        } catch (Exception ex) {
            logger.error("Error when flush livy statement status! ", ex);
            return Optional.empty();
        }
    }

    @NotThreadSafe
    public static class StatusEntry {

        public StatementStatus status;
        @NotEmpty
        public int statementId;
        @NotBlank
        public String nt;
        public String proxyUser;
        @NotBlank
        public String noteId;
        @NotBlank
        public String interpreterClass;
        public int clusterId;
        @NotNull
        public DateTime startTime;
        
        public HashMap<String, String> metrics = new HashMap<>();

        private StatusEntry(StatementStatus status,
                            int statementId,
                            String nt,
                            String noteId,
                            String interpreterClass,
                            DateTime startTime) {
            this.status = status;
            this.statementId = statementId;
            this.nt = nt;
            this.noteId = noteId;
            this.interpreterClass = interpreterClass;
            this.startTime = startTime;
        }

        public String getKey() {
            return generateKey(statementId, nt, noteId, interpreterClass);
        }

        public void update(ZetaMonitorEvent.LivyStatementMonitorEvent event) {
            status = event.getStatus();
            Map<String, String> context = event.getJobReport().externalContext;
            if (context != null) metrics.putAll(event.getJobReport().externalContext);
        }
        
        public long getDuration() {
            return System.currentTimeMillis() - startTime.getMillis();
        }

        public static StatusEntry fromStatementEvent(ZetaMonitorEvent.LivyStatementMonitorEvent event) {
            StatusEntry statusEntry = new StatusEntry(event.getStatus(),
                    event.getStatementId(),
                    event.getNt(),
                    event.getNoteId(),
                    event.getInterpreterClassName(),
                    event.getRecordTime());
            statusEntry.proxyUser = event.getProxyUser();
            JobReport jobReport = event.getJobReport();
            if (jobReport != null && jobReport.externalContext != null) {
                statusEntry.metrics.putAll(jobReport.externalContext);
            }
            return statusEntry;
        }
    }
    
    private static String generateKey(ZetaMonitorEvent.LivyStatementMonitorEvent event) {
        return generateKey(event.getStatementId(),
                event.getNt(),
                event.getNoteId(),
                event.getInterpreterClassName());
    }

    private static String generateKey(int statementId, String nt, String noteId, String interpreterClass) {
        return statementId + "_" + nt + "_" + noteId + "_" + interpreterClass;
    }

    public ConcurrentHashMap<String, StatusEntry> getLiveStatements() {
        return liveStatements;
    }

    @Override
    protected Persistence<StatusEntry> getPersistence() {
        return persistence;
    }

    public Persistence<StatusEntry> addPersistence(Persistence<StatusEntry> persistence) {
        this.persistence = persistence;
        return persistence;
    }
}
