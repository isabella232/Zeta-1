package com.ebay.dss.zds.magic.workflow;

import com.ebay.dss.zds.magic.ParserUtils;
import com.ebay.dss.zds.websocket.notebook.dto.CodeWithSeq;

import java.util.List;

/**
 * Created by tatian on 2021/4/15.
 */
public class WorkflowParseListener implements ParserUtils.ParseListener {

  private Workflow.WorkflowBuilder<CodeWithSeq> builder = new Workflow.WorkflowBuilder<>();
  private int currentCounter = 0;

  public void onNewSql(String sql, int index, List<ParserUtils.SQLSegment> currentSQLs) {

  }
}
