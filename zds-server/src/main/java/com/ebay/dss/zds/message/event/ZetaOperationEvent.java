package com.ebay.dss.zds.message.event;

import com.ebay.dss.zds.dao.ZetaEventRepository;
import com.ebay.dss.zds.interpreter.ConfigurationManager;
import com.ebay.dss.zds.interpreter.InterpreterType;
import com.ebay.dss.zds.message.EventQueueIdentifier;
import com.ebay.dss.zds.message.TaggedEvent;
import com.ebay.dss.zds.message.ZetaEvent;
import com.ebay.dss.zds.websocket.WebSocketResp;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import javax.validation.constraints.NotNull;
import java.util.Map;

import static com.ebay.dss.zds.common.JsonUtil.GSON;

/**
 * Created by tatian on 2019-06-11.
 */
public abstract class ZetaOperationEvent extends ZetaEvent {

    public EventQueueIdentifier getIdentifier() {
        return EventQueueIdentifier.OPERATION;
    }

    public String getMemo() {
        return getExternalContext().getProperty("memo");
    }

    public ZetaOperationEvent setMemo(String memo) {
        getExternalContext().setProperty("memo", memo);
        return this;
    }

    public static class ZetaConnectionEvent extends ZetaOperationEvent {

        protected String nt;
        protected String noteId;
        protected String interpreterClass;

        public ZetaConnectionEvent(String nt, String noteId, String interpreterClass) {
            this.nt = nt;
            this.noteId = noteId;
            this.interpreterClass = interpreterClass;
        }

        public JsonObject toJsonObject() {
            JsonObject jo = new JsonObject();
            jo.addProperty("identifier", getIdentifier().name());
            jo.addProperty("eventClass", this.getClass().getSimpleName());
            JsonObject eventProperties = new JsonObject();
            eventProperties.addProperty("nt", getNt());
            eventProperties.addProperty("noteId", getNoteId());
            eventProperties.addProperty("interpreterClass", getInterpreterClass());
            eventProperties.addProperty("recordTime", recordTime.toString("yyyy-MM-dd HH:mm:ss"));
            eventProperties.addProperty("externalContext", getExternalContext().toString());
            jo.add("eventContext", eventProperties);
            return jo;
        }

        public String getNt() {
            return nt;
        }

        public String getNoteId() {
            return noteId;
        }

        public String getInterpreterClass() {
            return interpreterClass;
        }


    }

    // for websocket monitoring
    public static class ZetaConnectionSocketSentEvent extends ZetaConnectionEvent {
        protected WebSocketResp rsp;
        public ZetaConnectionSocketSentEvent(String nt, String noteId, String interpreterClass, WebSocketResp rsp) {
            super(nt, noteId, getInterpreterClassName(interpreterClass));
            this.rsp = rsp;
        }

        private static String getInterpreterClassName(String interpreter) {
            if (interpreter == null) return "Unknown";
            Class clazz = InterpreterType.toClass(interpreter);
            if (clazz == null) return "Unknown";
            String name = clazz.getName();
            return StringUtils.isEmpty(name)? interpreter : name;
        }

        @Override
        public JsonObject toJsonObject() {
            JsonObject base = super.toJsonObject();
            base.getAsJsonObject("eventContext").add("webSocketResp",
                    GSON.toJsonTree(rsp));
            return base;
        }

    }

    public static class ZetaDisconnectionEvent extends ZetaConnectionEvent {

        public ZetaDisconnectionEvent(String nt, String noteId, String interpreterClass) {
            super(nt, noteId, interpreterClass);
        }
    }

    public static class ZetaConnectionSuccessEvent extends ZetaConnectionEvent {

        protected final DateTime startTime;

        public ZetaConnectionSuccessEvent(String nt, String noteId,
                                          String interpreterClass,
                                          @NotNull DateTime startTime) {
            super(nt, noteId, interpreterClass);
            this.startTime = startTime;
        }

        @Override
        public JsonObject toJsonObject() {
            JsonObject base = super.toJsonObject();
            base.getAsJsonObject("eventContext").addProperty("startTime",
                    startTime.toString("yyyy-MM-dd HH:mm:ss"));
            base.getAsJsonObject("eventContext").addProperty("cost",
                    recordTime.getMillis() - startTime.getMillis());
            return base;
        }
    }

    public static class ZetaConnectionFailedEvent extends ZetaConnectionEvent {

        protected final String reason;
        protected final DateTime startTime;

        public ZetaConnectionFailedEvent(String nt, String noteId,
                                         String interpreterClass,
                                         String reason,
                                         @NotNull DateTime startTime) {
            super(nt, noteId, interpreterClass);
            this.reason = reason;
            this.startTime = startTime;
        }

        @Override
        public JsonObject toJsonObject() {
            JsonObject base = super.toJsonObject();
            base.getAsJsonObject("eventContext").addProperty("reason", reason);
            base.getAsJsonObject("eventContext").addProperty("startTime",
                    startTime.toString("yyyy-MM-dd HH:mm:ss"));
            base.getAsJsonObject("eventContext").addProperty("cost",
                    recordTime.getMillis() - startTime.getMillis());
            return base;
        }
    }

    public static class ZetaDisconnectionSuccessEvent extends ZetaConnectionSuccessEvent {

        public ZetaDisconnectionSuccessEvent(String nt, String noteId, String interpreterClass,
                                             @NotNull DateTime startTime) {
            super(nt, noteId, interpreterClass, startTime);
        }
    }

    public static class ZetaDisconnectionFailedEvent extends ZetaConnectionFailedEvent {

        public ZetaDisconnectionFailedEvent(String nt, String noteId, String interpreterClass,
                                            String reason, DateTime startTime) {
            super(nt, noteId, interpreterClass, reason, startTime);
        }
    }

    // for Livy connection queue only
    public static class ZetaLivyConnectionStart extends ZetaConnectionEvent {

        protected final String proxyUser;
        protected final int clusterId;
        protected final String targetQueue;
        protected final String endpoint;

        public ZetaLivyConnectionStart(String nt,
                                       String proxyUser,
                                       int clusterId,
                                       String targetQueue,
                                       String endpoint,
                                       String noteId,
                                       String interpreterClass) {
            super(nt, noteId, interpreterClass);
            this.proxyUser = proxyUser;
            this.clusterId = clusterId;
            this.targetQueue = targetQueue;
            this.endpoint = endpoint;
        }

        @Override
        public JsonObject toJsonObject() {
            JsonObject base = super.toJsonObject();
            JsonObject eventContext = base.getAsJsonObject("eventContext");
            eventContext.addProperty("proxyUser", getProxyUser());
            eventContext.addProperty("clusterId", getClusterId());
            eventContext.addProperty("clusterName",
                    ConfigurationManager
                            .Cluster
                            .valueOfClusterId(clusterId));
            eventContext.addProperty("targetQueue", targetQueue);
            eventContext.addProperty("endpoint", endpoint);
            return base;
        }

        public String getProxyUser() {
            return proxyUser;
        }

        public int getClusterId() {
            return clusterId;
        }

        public String getTargetQueue() {
            return targetQueue;
        }

        public String getEndpoint() { return endpoint; }
    }

    public static class ZetaLivyConnectionSuccess extends ZetaLivyConnectionStart
            implements TaggedEvent.MysqlStorable {

        protected final boolean usingConnectionQueue;
        protected final boolean connectionQueueSuccess;
        protected final String connectionQueueFailReason;
        protected final long connectionQueueCost;
        protected final long totalCost;

        public ZetaLivyConnectionSuccess(String nt,
                                         String proxyUser,
                                         int clusterId,
                                         String targetQueue,
                                         String endpoint,
                                         String noteId,
                                         String interpreterClass,
                                         boolean usingConnectionQueue,
                                         boolean connectionQueueSuccess,
                                         String connectionQueueFailReason,
                                         long connectionQueueCost,
                                         long totalCost) {
            super(nt, proxyUser, clusterId, targetQueue, endpoint, noteId, interpreterClass);
            this.usingConnectionQueue = usingConnectionQueue;
            this.connectionQueueSuccess = connectionQueueSuccess;
            this.connectionQueueFailReason = connectionQueueFailReason;
            this.connectionQueueCost = connectionQueueCost;
            this.totalCost = totalCost;
        }

        @Override
        public JsonObject toJsonObject() {
            JsonObject base = super.toJsonObject();
            JsonObject eventContext = base.getAsJsonObject("eventContext");
            eventContext.addProperty("usingConnectionQueue", isUsingConnectionQueue());
            eventContext.addProperty("connectionQueueSuccess", isConnectionQueueSuccess());
            eventContext.addProperty("connectionQueueFailReason", getConnectionQueueFailReason());
            eventContext.addProperty("connectionQueueCost", getConnectionQueueCost());
            eventContext.addProperty("totalCost", getTotalCost());
            return base;
        }

        public static ZetaLivyConnectionSuccess fromMap(Map<String, Object> map) {
            return new ZetaLivyConnectionSuccess((String) map.get("nt"),
                    (String) map.get("proxyUser"),
                    (int) map.get("clusterId"),
                    (String) map.get("targetQueue"),
                    (String) map.get("endpoint"),
                    (String) map.get("noteId"),
                    (String) map.get("interpreterClass"),
                    (boolean) map.get("usingConnectionQueue"),
                    (boolean) map.get("connectionQueueSuccess"),
                    (String) map.get("connectionQueueFailReason"),
                    (long) map.get("connectionQueueCost"),
                    (long) map.get("totalCost"));
        }

        public long store(ZetaEventRepository zetaEventRepository) {
            return zetaEventRepository.saveLivyConnectionSuccess(this);
        }

        public boolean isUsingConnectionQueue() {
            return usingConnectionQueue;
        }

        public boolean isConnectionQueueSuccess() {
            return connectionQueueSuccess;
        }

        public String getConnectionQueueFailReason() {
            return connectionQueueFailReason;
        }

        public long getConnectionQueueCost() {
            return connectionQueueCost;
        }

        public long getTotalCost() {
            return totalCost;
        }
    }

    public static class ZetaLivyConnectionFail extends ZetaLivyConnectionStart
            implements TaggedEvent.MysqlStorable {

        protected final boolean usingConnectionQueue;
        protected final boolean connectionQueueSuccess;
        protected final String connectionQueueFailReason;
        protected final long connectionQueueCost;
        protected final long totalCost;
        protected final String reason;

        public ZetaLivyConnectionFail(String nt,
                                      String proxyUser,
                                      int clusterId,
                                      String targetQueue,
                                      String endpoint,
                                      String noteId,
                                      String interpreterClass,
                                      boolean usingConnectionQueue,
                                      boolean connectionQueueSuccess,
                                      String connectionQueueFailReason,
                                      long connectionQueueCost,
                                      long totalCost,
                                      String reason) {
            super(nt, proxyUser, clusterId, targetQueue, endpoint, noteId, interpreterClass);
            this.usingConnectionQueue = usingConnectionQueue;
            this.connectionQueueSuccess = connectionQueueSuccess;
            this.connectionQueueFailReason = connectionQueueFailReason;
            this.connectionQueueCost = connectionQueueCost;
            this.totalCost = totalCost;
            this.reason = reason;
        }

        @Override
        public JsonObject toJsonObject() {
            JsonObject base = super.toJsonObject();
            JsonObject eventContext = base.getAsJsonObject("eventContext");
            eventContext.addProperty("usingConnectionQueue", isUsingConnectionQueue());
            eventContext.addProperty("connectionQueueSuccess", isConnectionQueueSuccess());
            eventContext.addProperty("connectionQueueFailReason", getConnectionQueueFailReason());
            eventContext.addProperty("connectionQueueCost", getConnectionQueueCost());
            eventContext.addProperty("totalCost", getTotalCost());
            eventContext.addProperty("reason", getReason());
            return base;
        }

        public static ZetaLivyConnectionFail fromMap(Map<String, Object> map) {
            return new ZetaLivyConnectionFail((String) map.get("nt"),
                    (String) map.get("proxyUser"),
                    (int) map.get("clusterId"),
                    (String) map.get("targetQueue"),
                    (String) map.get("endpoint"),
                    (String) map.get("noteId"),
                    (String) map.get("interpreterClass"),
                    (boolean) map.get("usingConnectionQueue"),
                    (boolean) map.get("connectionQueueSuccess"),
                    (String) map.get("connectionQueueFailReason"),
                    (long) map.get("connectionQueueCost"),
                    (long) map.get("totalCost"),
                    (String) map.get("reason"));

        }

        public long store(ZetaEventRepository zetaEventRepository) {
            return zetaEventRepository.saveLivyConnectionFail(this);
        }

        public boolean isUsingConnectionQueue() {
            return usingConnectionQueue;
        }

        public boolean isConnectionQueueSuccess() {
            return connectionQueueSuccess;
        }

        public String getConnectionQueueFailReason() {
            return connectionQueueFailReason;
        }

        public long getConnectionQueueCost() {
            return connectionQueueCost;
        }

        public long getTotalCost() {
            return totalCost;
        }

        public String getReason() {
            return reason;
        }
    }

    public static class ZetaManualSQLConvertEvent extends ZetaOperationEvent implements TaggedEvent.MysqlStorable {
        protected String nt;
        protected String query;

        public ZetaManualSQLConvertEvent(String nt, String query) {
            this.nt = nt;
            this.query = query;
        }

        @Override
        public JsonObject toJsonObject() {
            JsonObject jo = new JsonObject();
            jo.addProperty("identifier", getIdentifier().name());
            jo.addProperty("eventClass", this.getClass().getSimpleName());
            JsonObject eventContext = new JsonObject();
            eventContext.addProperty("nt", nt);
            eventContext.addProperty("query", query);
            jo.add("eventContext", eventContext);
            return jo;
        }

        public long store(ZetaEventRepository zetaEventRepository) {
            return zetaEventRepository.saveManualSQLConvertEvent(this);
        }

        public String getNt() {
            return nt;
        }

        public String getQuery() {
            return query;
        }
    }

    public static class ZetaManualSQLConvertSuccessEvent extends ZetaManualSQLConvertEvent {
        public ZetaManualSQLConvertSuccessEvent(String nt, String query) {
            super(nt, query);
        }
    }

    public static class ZetaManualSQLConvertFailedEvent extends ZetaManualSQLConvertEvent {
        protected String error;

        public ZetaManualSQLConvertFailedEvent(String nt, String query, String error) {
            super(nt, query);
            this.error = error;
        }

        @Override
        public JsonObject toJsonObject() {
            JsonObject jo = super.toJsonObject();
            JsonObject eventContext = jo.getAsJsonObject("eventContext");
            eventContext.addProperty("error", error);
            return jo;
        }

        public String getError() {
            return error;
        }
    }

    public static class ZetaBatchSQLConvertEvent extends ZetaOperationEvent implements TaggedEvent.MysqlStorable {
        protected String nt;
        protected String platform;
        protected String table;

        public ZetaBatchSQLConvertEvent(String nt, String platform, String table) {
            this.nt = nt;
            this.platform = platform;
            this.table = table;
        }

        @Override
        public JsonObject toJsonObject() {
            JsonObject jo = new JsonObject();
            jo.addProperty("identifier", getIdentifier().name());
            jo.addProperty("eventClass", this.getClass().getSimpleName());
            JsonObject eventContext = new JsonObject();
            eventContext.addProperty("nt", nt);
            eventContext.addProperty("platform", platform);
            eventContext.addProperty("table", table);
            jo.add("eventContext", eventContext);
            return jo;
        }

        public long store(ZetaEventRepository zetaEventRepository) {
            return zetaEventRepository.saveBatchSQLConvertEvent(this);
        }

        public String getNt() {
            return nt;
        }

        public String getPlatform() {
            return platform;
        }

        public String getTable() {
            return table;
        }
    }

    public static class ZetaBatchSQLConvertSuccessEvent extends ZetaBatchSQLConvertEvent {
        public ZetaBatchSQLConvertSuccessEvent(String nt, String platform, String table) {
            super(nt, platform, table);
        }
    }

    public static class ZetaBatchSQLConvertFailedEvent extends ZetaBatchSQLConvertEvent {
        public ZetaBatchSQLConvertFailedEvent(String nt, String platform, String table) {
            super(nt, platform, table);
        }
    }
}
