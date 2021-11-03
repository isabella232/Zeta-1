package com.ebay.dss.zds.service.authorization;


import com.ebay.dss.zds.model.AuthorizationInfo;
import com.ebay.dss.zds.model.AuthorizationInfo.AuthType;

import java.util.Map;
import java.util.Set;

public class AuthorizationMail {

  AuthorizationType authorizationType;
  String link;
  String jobName;
  String admin;
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

  public String getAdmin() {
    return admin;
  }

  public void setAdmin(String admin) {
    this.admin = admin;
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
