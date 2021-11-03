package com.ebay.dss.zds.state.model;

import com.ebay.dss.zds.websocket.notebook.dto.ExecuteCodeResult;

/**
 * Created by tatian on 2020-08-26.
 */
public class NotebookTabState {

  public final ExecuteCodeResult executeCodeResult;
  public final long serverTimestamp;

  public NotebookTabState(ExecuteCodeResult executeCodeResult, long serverTimestamp) {
    this.executeCodeResult = executeCodeResult;
    this.serverTimestamp = serverTimestamp;
  }

}
