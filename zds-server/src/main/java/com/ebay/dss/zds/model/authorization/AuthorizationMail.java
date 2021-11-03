package com.ebay.dss.zds.model.authorization;


import com.ebay.dss.zds.model.authorization.AuthorizationInfo.AuthType;
import com.ebay.dss.zds.service.authorization.AuthorizationType;

import java.util.Map;
import java.util.Set;

public class AuthorizationMail {

  AuthorizationType authorizationType;
  String link;
  String jobName;
  String owner;
  Map<AuthType, Set<AuthorizationInfo>> originAuthInfo;
  Map<AuthType, Set<AuthorizationInfo>> authInfo;

  public AuthorizationType getAuthorizationType() {
    return authorizationType;
  }

  public void setAuthorizationType(AuthorizationType authorizationType) {
    this.authorizationType = authorizationType;
  }

  public String getLink() {
    return link;
  }

  public void setLink(String link) {
    this.link = link;
  }

  public String getJobName() {
    return jobName;
  }

  public void setJobName(String jobName) {
    this.jobName = jobName;
  }

  public String getOwner() {
    return owner;
  }

  public void setOwner(String owner) {
    this.owner = owner;
  }

  public Map<AuthType, Set<AuthorizationInfo>> getOriginAuthInfo() {
    return originAuthInfo;
  }

  public void setOriginAuthInfo(Map<AuthType, Set<AuthorizationInfo>> originAuthInfo) {
    this.originAuthInfo = originAuthInfo;
  }

  public Map<AuthType, Set<AuthorizationInfo>> getAuthInfo() {
    return authInfo;
  }

  public void setAuthInfo(Map<AuthType, Set<AuthorizationInfo>> authInfo) {
    this.authInfo = authInfo;
  }
}
