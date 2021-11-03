package com.ebay.dss.zds.service.authorization;

import com.ebay.dss.zds.model.authorization.AuthorizationInfo.AuthType;

public enum AuthorizationType {
  ZETA_SHEET("Zeta Sheet") {
    @Override
    String convertAuthTypeName(AuthType authType) {
      return authType == AuthType.OWNERS ? "Admin" : authType.name();
    }
  }, ZETA_SCHEDULE("Zeta Schedule") {
    @Override
    String convertAuthTypeName(AuthType authType) {
      return authType == AuthType.WRITERS ? "Editor" : authType.name();
    }
  };

  public String type;

  AuthorizationType(String type) {
    this.type = type;
  }

  abstract String convertAuthTypeName(AuthType authType);
}
