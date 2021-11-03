package com.ebay.dss.zds.websocket.notebook.dto;

import com.ebay.dss.zds.magic.ParserUtils;

import java.io.Serializable;
import java.util.List;

import static com.ebay.dss.zds.websocket.notebook.dto.CodeWithSeq.Status.DONE;
import static com.ebay.dss.zds.websocket.notebook.dto.CodeWithSeq.Status.FAIL;

/**
 * Created by wenliu2 on 5/4/18.
 */
public class CodeWithSeq implements Serializable {

  public enum Status {
    READY, RUNNING, DONE, FAIL
  }

  private volatile String code;
  private int seq;
  private volatile Status status;
  private volatile long statementId;
  private transient List<ParserUtils.Partition> commentPos;

  public CodeWithSeq(String code, int seq) {
    this.code = code;
    this.seq = seq;
    this.status = Status.READY;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public int getSeq() {
    return seq;
  }

  public void setSeq(int seq) {
    this.seq = seq;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public Status getStatus() {
    return this.status;
  }

  public void setStatementId(long id) {
    this.statementId= id;
  }

  public long getStatementId() {
    return this.statementId;
  }

  public boolean finished() {
    return status == DONE || status == FAIL;
  }

  public List<ParserUtils.Partition> getCommentPos() {
    return commentPos;
  }

  public void setCommentPos(List<ParserUtils.Partition> commentPos) {
    this.commentPos = commentPos;
  }
}
