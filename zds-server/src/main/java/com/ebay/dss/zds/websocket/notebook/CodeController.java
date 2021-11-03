package com.ebay.dss.zds.websocket.notebook;

import com.ebay.dss.zds.common.Constant;
import com.ebay.dss.zds.common.QueueDestination;
import com.ebay.dss.zds.rest.error.match.ExceptionRuleService;
import com.ebay.dss.zds.runner.Tenant;
import com.ebay.dss.zds.runner.ZetaExecutionPool;
import com.ebay.dss.zds.service.ZetaNotebookService;
import com.ebay.dss.zds.websocket.AbstractWSController;
import com.ebay.dss.zds.websocket.WebSocketResp;
import com.ebay.dss.zds.websocket.notebook.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.WebSocketSession;

import javax.validation.Valid;
import java.security.Principal;

/**
 * Created by wenliu2 on 4/13/18.
 */
@Controller
@MessageMapping("/code")
public class CodeController extends AbstractWSController {

  private static final Logger log = LoggerFactory.getLogger(CodeController.class);

  private final ZetaNotebookService notebookService;
  private final ZetaExecutionPool zetaExecutionPool;

  @Autowired
  public CodeController(SimpMessagingTemplate template,
                        ZetaNotebookService notebookService,
                        ExceptionRuleService ruleService,
                        ZetaExecutionPool zetaExecutionPool) {
    super(template, ruleService);
    this.notebookService = notebookService;
    this.zetaExecutionPool = zetaExecutionPool;
  }

  private QueueDestination getQueueDestination(Principal principal) {
    return new QueueDestination(principal.getName(), this.getTemplate(), this.getQueue());
  }

  @MessageMapping("/execute")
  public void executeSQL(@Valid ExecuteCodeReq req, MessageHeaderAccessor accessor, Principal principal) {
    Runnable task = () -> {
      this.getLogger().info("code: {}", req);
      QueueDestination dest = getQueueDestination(principal);
      notebookService.executeCode(dest, req);
    };
    if (zetaExecutionPool.isEnabled()) {
      zetaExecutionPool.submit(Tenant.getNameByReq(req), attachCurrentSecurityContext(principal, task));
    } else {
      task.run();
    }
  }

  @MessageMapping("/dump")
  public void dumpSQLResult(@Valid ExecuteDumpReq dump, MessageHeaderAccessor accessor, Principal principal) {
    Runnable task = () -> {
      this.getLogger().info("code: {}", dump);
      QueueDestination dest = getQueueDestination(principal);
      notebookService.executeCode(dest, dump);
    };
    if (zetaExecutionPool.isEnabled()) {
      zetaExecutionPool.submit(Tenant.getNameByReq(dump), attachCurrentSecurityContext(principal, task));
    } else {
      task.run();
    }
  }

  @MessageMapping("/cancel")
  public void cancelSQL(@Valid ExecuteCodeCancel req, MessageHeaderAccessor accessor, Principal principal) {
    this.getLogger().info("Cancel note: {} for user: {} jobId: {}",
            req.getNotebookId(), req.getUserName(), req.getJobId());
    QueueDestination dest = getQueueDestination(principal);
    notebookService.cancelCode(dest, req);
    this.getLogger().info("Cancel note: {} for user: {} jobId: {}",
            req.getNotebookId(), req.getUserName(), req.getJobId());
  }

  @MessageMapping("/connect")
  public void doConnect(@Valid ConnectionReq req, MessageHeaderAccessor accessor, Principal principal) {
    Runnable task = () -> {
      this.getLogger().info("connection: {}", req);
      QueueDestination dest = getQueueDestination(principal);
      notebookService.doConnect(dest, req);
    };
    if (zetaExecutionPool.isEnabled()) {
      zetaExecutionPool.submit(Tenant.getNameByReq(req), attachCurrentSecurityContext(principal, task));
    } else {
      task.run();
    }
  }

  @MessageMapping("/disconnect")
  public void doDisconnect(@Valid DisconnectReq req, MessageHeaderAccessor accessor, Principal principal) {
    this.getLogger().info("disconnect: {}", req);
    QueueDestination dest = getQueueDestination(principal);
    notebookService.doDisconnect(dest, req);
  }

  @MessageMapping("/recover")
  public void doRecover(@Valid RecoverReq req, MessageHeaderAccessor accessor, Principal principal) {
    this.getLogger().info("recover: {}", req);
    QueueDestination dest = getQueueDestination(principal);
    notebookService.doRecover(dest, req);
  }

}
