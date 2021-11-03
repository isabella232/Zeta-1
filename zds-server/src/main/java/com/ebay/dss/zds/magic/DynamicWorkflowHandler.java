package com.ebay.dss.zds.magic;

import com.ebay.dss.zds.magic.workflow.Workflow;
import com.ebay.dss.zds.websocket.notebook.dto.CodeWithSeq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Created by tatian on 2021/4/15.
 */
@Component
public class DynamicWorkflowHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(DynamicWorkflowHandler.class);

  @Value("${zds.notebook.dynamicWorkflow.enable:#{true}}")
  private volatile boolean enabled;

  // todo: support for other kind of code besides sql
  public Workflow<CodeWithSeq> handle(String fullSQL) {
    Workflow.WorkflowBuilder<CodeWithSeq> builder = new Workflow.WorkflowBuilder<>();
    ParserUtils.SplitSQL sqlList = new ParserUtils
            .SplitSQL(fullSQL)
            .trimResult()
            .omitEmptyStrings()
            .removeComment();
    internalHandleWorkflow(sqlList, builder);
    sqlList.parse();
    return builder.build();
  }

  protected void internalHandleWorkflow(ParserUtils.SplitSQL sqlList, Workflow.WorkflowBuilder<CodeWithSeq> builder) {

  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }
}
