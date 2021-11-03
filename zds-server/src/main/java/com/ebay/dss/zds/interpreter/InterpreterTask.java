package com.ebay.dss.zds.interpreter;

import avro.shaded.com.google.common.collect.Lists;
import com.ebay.dss.zds.state.Recoverable;
import com.ebay.dss.zds.state.StateManager;
import com.ebay.dss.zds.state.StateSnapshot;
import com.ebay.dss.zds.websocket.notebook.dto.CodeWithSeq;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by tatian on 2020-08-21.
 */
public abstract class InterpreterTask implements Runnable, Recoverable, Serializable {

  public final String noteId;
  public final String jobId;
  public final String reqId;
  public final List<CodeWithSeq> codeWithSeqs;

  public InterpreterTask(String noteId, String jobId, String reqId, List<CodeWithSeq> codes) {
    this.noteId = noteId;
    this.jobId = jobId;
    this.reqId = reqId;
    this.codeWithSeqs = new ArrayList<>();
    codeWithSeqs.addAll(codes);
  }

  @Override
  public StateSnapshot createSnapshot() {
    return new StateSnapshot<>(this);
  }

  @Override
  public boolean doRecover(StateSnapshot stateSnapshot) {
    // do no thing
    return false;
  }

}