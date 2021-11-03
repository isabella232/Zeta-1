package com.ebay.dss.zds.cluster;

import com.ebay.dss.zds.cluster.event.notebook.ExecuteCodeEvent;
import com.ebay.dss.zds.common.QueueDestination;
import com.ebay.dss.zds.exception.ErrorCode;
import com.ebay.dss.zds.exception.InterpreterExecutionException;
import com.ebay.dss.zds.exception.InterpreterServiceException;
import com.ebay.dss.zds.message.EventTracker;
import com.ebay.dss.zds.rest.error.ErrorDTO;
import com.ebay.dss.zds.rest.error.handler.InterpreterExceptionSupport;
import com.ebay.dss.zds.rpc.RpcCallContext;
import com.ebay.dss.zds.rpc.RpcEndpoint;
import com.ebay.dss.zds.rpc.RpcEnv;
import com.ebay.dss.zds.rpc.message.RpcMessage;
import com.ebay.dss.zds.runner.Tenant;
import com.ebay.dss.zds.runner.ZetaExecutionPool;
import com.ebay.dss.zds.service.ZetaNotebookService;
import com.ebay.dss.zds.websocket.AbstractWSController;
import com.ebay.dss.zds.websocket.WebSocketResp;
import com.ebay.dss.zds.websocket.notebook.WSConstants;
import com.ebay.dss.zds.websocket.notebook.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpAttributes;
import org.springframework.messaging.simp.SimpAttributesContextHolder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import javax.validation.Valid;
import java.security.Principal;
import java.util.Map;

import static com.ebay.dss.zds.message.event.Event.ZetaConnectionSocketSentEvent;

/**
 * Created by tatian on 2020-09-24.
 */
public class RemoteNotebookManager extends RpcEndpoint {

  private final Logger logger = LoggerFactory.getLogger(RemoteNotebookManager.class);

  public static final String name = RemoteNotebookManager.class.getSimpleName();

  private ZetaNotebookService notebookService;
  private ZetaExecutionPool zetaExecutionPool;
  private InterpreterExceptionSupport interpreterExceptionSupport;

  public RemoteNotebookManager(RpcEnv rpcEnv) throws Exception {
    super(rpcEnv, name);
  }

  public void handleReceive(RpcCallContext callContext) {
    RpcMessage rpcMessage = callContext.rpcMessage;
    if (rpcMessage instanceof ExecuteCodeEvent) {
      handelExecuteCodeEvent((ExecuteCodeEvent) rpcMessage, callContext);
    } else {
      // todo: implement more
    }

  }

  public RemoteQueueDestination createRemoteQueueDestination(String user, RpcCallContext callContext) {
    return new RemoteQueueDestination(user, WSConstants.queue, callContext.getBackRemoteRpcStreamingPipe());
  }

  public Logger getLogger() {
    return this.logger;
  }

  public void handelExecuteCodeEvent(ExecuteCodeEvent executeCodeEvent, RpcCallContext rpcCallContext) {
    String user = executeCodeEvent.getMessage().getUser();
    Principal principal = ZetaPrincipal.newPrincipal(user);
    ExecuteCodeReq req = (ExecuteCodeReq) executeCodeEvent.getMessage().getRequestEntity();
    RemoteQueueDestination remoteQueueDestination = createRemoteQueueDestination(user, rpcCallContext);
    executeSQL(req, principal, remoteQueueDestination);
  }

  public void executeSQL(@Valid ExecuteCodeReq req, Principal principal, QueueDestination dest) {
    Runnable task = () -> {
      this.getLogger().info("code: {}", req);
      notebookService.executeCode(dest, req);
    };
    if (zetaExecutionPool.isEnabled()) {
      zetaExecutionPool.submit(Tenant.getNameByReq(req), attachCurrentSecurityContext(principal, task, dest));
    } else {
      task.run();
    }
  }

  public void dumpSQLResult(@Valid ExecuteDumpReq dump, Principal principal, QueueDestination dest) {
    Runnable task = () -> {
      this.getLogger().info("code: {}", dump);
      notebookService.executeCode(dest, dump);
    };
    if (zetaExecutionPool.isEnabled()) {
      zetaExecutionPool.submit(Tenant.getNameByReq(dump), attachCurrentSecurityContext(principal, task, dest));
    } else {
      task.run();
    }
  }

  public void cancelSQL(@Valid ExecuteCodeCancel req, Principal principal, QueueDestination dest) {
    this.getLogger().info("Cancel note: {} for user: {} jobId: {}",
            req.getNotebookId(), req.getUserName(), req.getJobId());
    notebookService.cancelCode(dest, req);
    this.getLogger().info("Cancel note: {} for user: {} jobId: {}",
            req.getNotebookId(), req.getUserName(), req.getJobId());
  }

  public void doConnect(@Valid ConnectionReq req, Principal principal, QueueDestination dest) {
    Runnable task = () -> {
      this.getLogger().info("connection: {}", req);
      notebookService.doConnect(dest, req);
    };
    if (zetaExecutionPool.isEnabled()) {
      zetaExecutionPool.submit(Tenant.getNameByReq(req), attachCurrentSecurityContext(principal, task, dest));
    } else {
      task.run();
    }
  }

  public void doDisconnect(@Valid DisconnectReq req, Principal principal, QueueDestination dest) {
    this.getLogger().info("disconnect: {}", req);
    notebookService.doDisconnect(dest, req);
  }

  public void doRecover(@Valid RecoverReq req, Principal principal, QueueDestination dest) {
    this.getLogger().info("recover: {}", req);
    notebookService.doRecover(dest, req);
  }

  public void handleMessageException(Exception ex,
                                     Principal principal,
                                     QueueDestination dest) {
    WebSocketResp<ErrorDTO> resp = interpreterExceptionSupport.ruleMatch(ex, ErrorCode.UNKNOWN_EXCEPTION, WebSocketResp.OP.INTERNAL_ERROR);
    convertAndSendToUser(principal.getName(), resp, dest);
  }

  public void handleInterpreterConnectionException(InterpreterServiceException ise,
                                                   Principal principal,
                                                   QueueDestination dest) {
    WebSocketResp<ErrorDTO> resp = interpreterExceptionSupport.handleInterpreterConnectionException(ise);
    convertAndSendToUser(principal.getName(), resp, dest);
  }

  public void handleInterpreterExecutionException(InterpreterExecutionException iee,
                                                  Principal principal,
                                                  QueueDestination dest) {
    WebSocketResp<ErrorDTO> resp = interpreterExceptionSupport.handleInterpreterExecutionException(iee);
    convertAndSendToUser(principal.getName(), resp, dest);
  }

  public void convertAndSendToUser(String user,
                                   WebSocketResp<?> payload,
                                   QueueDestination dest) throws MessagingException {
    // todo: need to send more context back?
    dest.sendData(payload);
  }

  private void runWithExceptionHandler(Principal principal, Runnable runnable, QueueDestination dest) {
    try {
      runnable.run();
    } catch (InterpreterExecutionException iee) {
      handleInterpreterExecutionException(iee, principal, dest);
    } catch (InterpreterServiceException ise) {
      handleInterpreterConnectionException(ise, principal, dest);
    } catch (Exception ex) {
      handleMessageException(ex, principal, dest);
    }
  }

  // this must be execute in the thread that has available security context
  public Runnable attachCurrentSecurityContext(Principal principal, Runnable runnable, QueueDestination dest) {
    SecurityContext securityContext = SecurityContextHolder.getContext();
    RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
    SimpAttributes simpAttributes = SimpAttributesContextHolder.getAttributes();
    return () -> {
      try {
        SecurityContextHolder.setContext(securityContext);
        RequestContextHolder.setRequestAttributes(attributes);
        SimpAttributesContextHolder.setAttributes(simpAttributes);
        runWithExceptionHandler(principal, runnable, dest);
      } finally {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
        SimpAttributesContextHolder.resetAttributes();
      }
    };
  }


}
