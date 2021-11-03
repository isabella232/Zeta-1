package com.ebay.dss.zds.message.event;

import com.ebay.dss.zds.interpreter.interpreters.Interpreter;
import com.ebay.dss.zds.interpreter.monitor.modle.JobReport;
import com.ebay.dss.zds.message.event.ZetaOperationEvent.*;
import com.ebay.dss.zds.message.event.ZetaMonitorEvent.*;
import com.ebay.dss.zds.message.event.ZetaCommonEvent.*;
import com.ebay.dss.zds.message.event.ZetaStateEvent.*;
import com.ebay.dss.zds.websocket.WebSocketResp;
import org.joda.time.DateTime;

/**
 * Created by tatian on 2019-06-11.
 */
public class Event {


    // operation
    public static ZetaConnectionEvent ZetaConnectionEvent(String nt,
                                                          String noteId,
                                                          String interpreterClass) {
        return new ZetaConnectionEvent(nt, noteId, interpreterClass);
    }

    public static ZetaConnectionSocketSentEvent ZetaConnectionSocketSentEvent(String nt,
                                                                              String noteId,
                                                                              String interpreterClass,
                                                                              WebSocketResp webSocketResp) {
        return new ZetaConnectionSocketSentEvent(nt, noteId, interpreterClass, webSocketResp);
    }


    public static ZetaConnectionSuccessEvent ZetaConnectionSuccessEvent(String nt,
                                                                        String noteId,
                                                                        String interpreterClass,
                                                                        DateTime startTime) {
        return new ZetaConnectionSuccessEvent(nt, noteId, interpreterClass, startTime);
    }

    public static ZetaConnectionFailedEvent ZetaConnectionFailedEvent(String nt,
                                                                      String noteId,
                                                                      String interpreterClass,
                                                                      String reason,
                                                                      DateTime startTime) {
        return new ZetaConnectionFailedEvent(nt, noteId, interpreterClass, reason, startTime);
    }

    public static ZetaDisconnectionEvent ZetaDisconnectionEvent(String nt,
                                                                String noteId,
                                                                String interpreterClass) {
        return new ZetaDisconnectionEvent(nt, noteId, interpreterClass);
    }

    public static ZetaDisconnectionSuccessEvent ZetaDisconnectionSuccessEvent(String nt,
                                                                              String noteId,
                                                                              String interpreterClass, DateTime startTime) {
        return new ZetaDisconnectionSuccessEvent(nt, noteId, interpreterClass, startTime);
    }

    public static ZetaDisconnectionFailedEvent ZetaDisconnectionFailedEvent(String nt,
                                                                            String noteId,
                                                                            String interpreterClass,
                                                                            String reason, DateTime startTime) {
        return new ZetaDisconnectionFailedEvent(nt, noteId, interpreterClass, reason, startTime);
    }

    // monitor
    public static YarnQueueMonitorEvent YarnQueueMonitorEvent(String clusterName,
                                                              String queueName,
                                                              String alterContext) {
        return new YarnQueueMonitorEvent(clusterName, queueName, alterContext);
    }

    // for livy connection monitor
    public static ZetaLivyConnectionStart ZetaLivyConnectionStart(String nt,
                                                                  String proxyUser,
                                                                  int clusterId,
                                                                  String targetQueue,
                                                                  String endpoint,
                                                                  String noteId,
                                                                  String interpreterClass) {
        return new ZetaLivyConnectionStart(nt, proxyUser, clusterId, targetQueue, endpoint, noteId, interpreterClass);
    }

    public static ZetaLivyConnectionSuccess ZetaLivyConnectionSuccess(String nt,
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
        return new ZetaLivyConnectionSuccess(nt, proxyUser, clusterId, targetQueue, endpoint, noteId, interpreterClass, usingConnectionQueue,
                connectionQueueSuccess, connectionQueueFailReason, connectionQueueCost, totalCost);
    }

    public static ZetaLivyConnectionFail ZetaLivyConnectionFail(String nt,
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
        return new ZetaLivyConnectionFail(nt, proxyUser, clusterId, targetQueue, endpoint, noteId, interpreterClass,
                usingConnectionQueue, connectionQueueSuccess, connectionQueueFailReason,
                connectionQueueCost, totalCost, reason);
    }

    public static LivyStatementMonitorEvent LivyStatementMonitorEvent(StatementStatus status,
                                                                      int statementId,
                                                                      String nt,
                                                                      String proxyUser,
                                                                      String noteId,
                                                                      String interpreterClassName,
                                                                      Integer clusterId,
                                                                      JobReport jobReport) {
        return new LivyStatementMonitorEvent(status, statementId, nt, proxyUser, noteId, interpreterClassName, clusterId, jobReport);
    }

    public static LivyDumpStatementMonitorEvent LivyDumpStatementMonitorEvent(StatementStatus status,
                                                                      int statementId,
                                                                      String nt,
                                                                      String proxyUser,
                                                                      String noteId,
                                                                      String interpreterClassName,
                                                                      Integer clusterId,
                                                                      JobReport jobReport) {
        return new LivyDumpStatementMonitorEvent(status, statementId, nt, proxyUser, noteId, interpreterClassName, clusterId, jobReport);
    }

    //common
    public static ZetaWebSocketContentEvent ZetaWebSocketContentEvent(String nt, WebSocketResp rsp) {
        return new ZetaWebSocketContentEvent(nt, rsp);
    }

    //SQL Convert Event
    public static ZetaManualSQLConvertSuccessEvent ZetaManualSQLConvertSuccessEvent(String nt, String query) {
        return new ZetaManualSQLConvertSuccessEvent(nt, query);
    }

    public static ZetaManualSQLConvertFailedEvent ZetaManualSQLConvertFailedEvent(String nt, String query, String error) {
        return new ZetaManualSQLConvertFailedEvent(nt, query, error);
    }

    public static ZetaBatchSQLConvertSuccessEvent ZetaBatchSQLConvertSuccessEvent(String nt, String platform, String table) {
        return new ZetaBatchSQLConvertSuccessEvent(nt, platform, table);
    }

    public static ZetaBatchSQLConvertFailedEvent ZetaBatchSQLConvertFailedEvent(String nt, String platform, String table) {
        return new ZetaBatchSQLConvertFailedEvent(nt, platform, table);
    }

    //state
    public static ZetaStatePersistEvent ZetaStatePersistEvent(String nt, String noteId, Interpreter interpreter) {
        return new ZetaStatePersistEvent(nt, noteId, interpreter);
    }

    public static ZetaStateUnPersistEvent ZetaStateUnPersistEvent(String nt, String noteId, Interpreter interpreter) {
        return new ZetaStateUnPersistEvent(nt, noteId, interpreter);
    }
}
