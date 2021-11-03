package com.ebay.dss.zds.exception;

/**
 * Created by wenliu2 on 4/3/18.
 */
public class ClusterAccessDeniedException extends ApplicationBaseException {
  public ClusterAccessDeniedException(String message) {
    super(ErrorCode.CLUSTER_ACCESS_DENIED_EXCEPTION, message);
  }
}
