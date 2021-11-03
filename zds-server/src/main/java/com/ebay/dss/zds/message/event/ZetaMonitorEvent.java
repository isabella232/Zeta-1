package com.ebay.dss.zds.message.event;

import com.ebay.dss.zds.dao.ZetaEventRepository;
import com.ebay.dss.zds.interpreter.monitor.modle.JobReport;
import com.ebay.dss.zds.message.EventQueueIdentifier;
import com.ebay.dss.zds.message.TaggedEvent;
import com.ebay.dss.zds.message.ZetaEvent;
import com.google.gson.JsonObject;

import javax.validation.constraints.NotNull;

/**
 * Created by tatian on 2019-06-18.
 */
public abstract class ZetaMonitorEvent extends ZetaEvent {

    public enum StatementStatus {
        START, RUNNING, END
    }

    public EventQueueIdentifier getIdentifier() {
        return EventQueueIdentifier.MONITOR;
    }

    public static class YarnQueueMonitorEvent extends ZetaMonitorEvent
            implements TaggedEvent.EmailStorable {

        private final String clusterName;
        private final String queueName;
        private String alterContext;

        public YarnQueueMonitorEvent(String clusterName, String queueName) {
            this(clusterName, queueName, null);
        }

        public YarnQueueMonitorEvent(String clusterName, String queueName, String alterContext) {
            this.clusterName = clusterName;
            this.queueName = queueName;
            this.alterContext = alterContext;
        }
        @Override
        public JsonObject toJsonObject() {
            JsonObject jo = new JsonObject();
            jo.addProperty("identifier", getIdentifier().name());
            jo.addProperty("eventClass", this.getClass().getSimpleName());
            JsonObject eventProperties = new JsonObject();
            eventProperties.addProperty("clusterName", getClusterName());
            eventProperties.addProperty("queueName", getQueueName());
            eventProperties.addProperty("alterContext", getAlterContext());
            eventProperties.addProperty("recordTime", recordTime.toString("yyyy-MM-dd HH:mm:ss"));
            eventProperties.addProperty("externalContext", getExternalContext().toString());
            jo.add("eventContext", eventProperties);
            return jo;
        }

        public String getClusterName() {
            return clusterName;
        }

        public String getQueueName() {
            return queueName;
        }

        public String getAlterContext() {
            return alterContext;
        }

        public String getReceiver() {
            return getExternalContext().getProperty("emailReceiver");
        }

        public String getSender() {
            return getExternalContext().getProperty("emailSender");
        }

        public String getSubject() {
            return getExternalContext().getProperty("subject");
        }

        public String getContent() {
            return getAlterContext();
        }
    }

    public static class LivyStatementMonitorEvent extends ZetaMonitorEvent implements TaggedEvent.NTTagged {

        private final StatementStatus status;
        private final int statementId;
        private final String nt;
        private final String proxyUser;
        private final String noteId;
        private final String interpreterClassName;
        private final Integer clusterId;
        @NotNull
        private final JobReport jobReport;

        public LivyStatementMonitorEvent(StatementStatus status,
                                         int statementId,
                                         String nt,
                                         String proxyUser,
                                         String noteId,
                                         String interpreterClassName,
                                         Integer clusterId,
                                         JobReport jobReport) {
            this.status = status;
            this.statementId = statementId;
            this.nt = nt;
            this.proxyUser = proxyUser;
            this.noteId = noteId;
            this.interpreterClassName = interpreterClassName;
            this.clusterId = clusterId;
            this.jobReport = jobReport;
        }

        @Override
        public JsonObject toJsonObject() {
            JsonObject jo = new JsonObject();
            jo.addProperty("identifier", getIdentifier().name());
            jo.addProperty("eventClass", this.getClass().getSimpleName());
            JsonObject eventProperties = new JsonObject();
            eventProperties.addProperty("status", getStatus().name());
            eventProperties.addProperty("statementId", getStatementId());
            eventProperties.addProperty("nt", getNt());
            eventProperties.addProperty("proxyUser", getProxyUser());
            eventProperties.addProperty("noteId", getNoteId());
            eventProperties.addProperty("interpreterClassName", getInterpreterClassName());
            eventProperties.addProperty("clusterId", getClusterId());
            eventProperties.addProperty("progress", jobReport.progress + "");
            jobReport.externalContext.keySet().forEach(key ->
                eventProperties.addProperty(key, jobReport.externalContext.get(key)));
            eventProperties.addProperty("recordTime", recordTime.toString("yyyy-MM-dd HH:mm:ss"));
            eventProperties.addProperty("externalContext", getExternalContext().toString());
            jo.add("eventContext", eventProperties);
            return jo;
        }

        public StatementStatus getStatus() {
            return status;
        }

        public int getStatementId() {
            return statementId;
        }

        public String getNt() {
            return nt;
        }

        public String getProxyUser() {
            return proxyUser;
        }

        public String getNoteId() {
            return noteId;
        }

        public String getInterpreterClassName() {
            return interpreterClassName;
        }

        public Integer getClusterId() {
            return clusterId;
        }

        @NotNull
        public JobReport getJobReport() {
            return jobReport;
        }
    }


    public static class LivyDumpStatementMonitorEvent extends LivyStatementMonitorEvent implements TaggedEvent.MysqlStorable {

        public LivyDumpStatementMonitorEvent(StatementStatus status,
                                             int statementId,
                                             String nt,
                                             String proxyUser,
                                             String noteId,
                                             String interpreterClassName,
                                             Integer clusterId,
                                             JobReport jobReport) {
            super(status, statementId, nt, proxyUser, noteId, interpreterClassName, clusterId, jobReport);
        }

        @Override
        public long store(ZetaEventRepository zetaEventRepository) {
            // todo: implements the logic
            return 0;
        }
    }

    public static class HermesQueueMonitorEvent extends ZetaMonitorEvent {
        private final int clusterId;
        private final String queue;
        private final String appId;

        public HermesQueueMonitorEvent(int clusterId, String queue, String appId) {
            this.clusterId = clusterId;
            this.queue = queue;
            this.appId = appId;
        }

        @Override
        public JsonObject toJsonObject() {
            JsonObject jo = new JsonObject();
            jo.addProperty("identifier", getIdentifier().name());
            jo.addProperty("eventClass", this.getClass().getSimpleName());
            JsonObject eventProperties = new JsonObject();
            eventProperties.addProperty("clusterId", clusterId);
            eventProperties.addProperty("queue", queue);
            eventProperties.addProperty("appId", appId);
            jo.add("eventContext", eventProperties);
            return jo;
        }

        public int getClusterId() {
            return clusterId;
        }

        public String getQueue() {
            return queue;
        }

        public String getAppId() {
            return appId;
        }
    }

}
