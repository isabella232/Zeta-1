package com.ebay.dss.zds.service.authorization;


import com.ebay.dss.zds.common.JsonUtil;
import com.ebay.dss.zds.exception.AuthorizationException;
import com.ebay.dss.zds.model.authorization.AuthorizationInfo;
import com.ebay.dss.zds.model.authorization.ZetaAuthorization;
import com.ebay.dss.zds.model.authorization.AuthorizationInfo.AuthType;
import com.ebay.dss.zds.model.authorization.AuthorizationMail;
import com.ebay.dss.zds.service.MailService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.jsonwebtoken.lang.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.ebay.dss.zds.model.authorization.AuthorizationInfo.AuthType.*;
import static com.ebay.dss.zds.service.authorization.AuthorizationConstant.*;

@Service
public class ZetaAuthorizationService {

  private static final Logger LOGGER = LoggerFactory.getLogger(ZetaAuthorizationService.class);

  @Autowired
  private MailService mailService;

  public boolean isReader(String nt, ZetaAuthorization zetaAuthorization) {
    String authInfo = zetaAuthorization.getAuthInfo();
    return isAdmin(nt, zetaAuthorization) ||
        isMember(nt, getAuthNtList(READERS, authInfo)) ||
        isMember(nt, getAuthNtList(WRITERS, authInfo)) ||
        isMember(nt, getAuthNtList(OWNERS, authInfo));
  }

  public boolean isWriter(String nt, ZetaAuthorization zetaAuthorization) {
    String authInfo = zetaAuthorization.getAuthInfo();
    return isAdmin(nt, zetaAuthorization) ||
        isMember(nt, getAuthNtList(WRITERS, authInfo)) ||
        isMember(nt, getAuthNtList(OWNERS, authInfo));
  }

  public boolean isOwner(String nt, ZetaAuthorization zetaAuthorization) {
    String authInfo = zetaAuthorization.getAuthInfo();
    return isAdmin(nt, zetaAuthorization) ||
        isMember(nt, getAuthNtList(OWNERS, authInfo));
  }

  private boolean isAdmin(String nt, ZetaAuthorization zetaAuthorization) {
    return nt.equalsIgnoreCase(zetaAuthorization.getNt());
  }

  private Set<String> getAuthNtList(AuthType authType, String authInfo) {
    Map<AuthType, Set<AuthorizationInfo>> parseAuthInfo = parseAuthInfo(authInfo);
    return parseAuthInfo.getOrDefault(authType, Sets.newLinkedHashSet())
        .stream().map(AuthorizationInfo::getNt).collect(Collectors.toSet());
  }

  public Map<AuthType, Set<AuthorizationInfo>> parseAuthInfo(String auth) {
    Assert.notNull(auth, "AuthInfo mustn't be null");
    return JsonUtil.fromJson(auth, new TypeReference<Map<AuthType, Set<AuthorizationInfo>>>() {
    });
  }

  private boolean isMember(String nt, Set<String> authList) {
    return authList.contains(nt);
  }

  public void checkAuthInfoFormat(Map<AuthType, Set<AuthorizationInfo>> authInfo) {
    for (AuthType authType : AuthType.values()) {
      if (!authInfo.containsKey(authType)) {
        throw new AuthorizationException(String.format(AUTH_INFO_MISS, authType.toString()));
      }
    }
  }

  public Map<AuthType, Set<AuthorizationInfo>> initAuthInfo() {
    Map<AuthType, Set<AuthorizationInfo>> authInfo = Maps.newLinkedHashMap();
    for (AuthType authType : AuthType.values()) {
      authInfo.put(authType, Sets.newHashSet());
    }
    return authInfo;
  }

  @Async("asyncEventExecutor")
  public void sendGrantEmail(String nt, AuthorizationMail authorizationMail) {
    Map<AuthType, Set<AuthorizationInfo>> originAuthInfo = authorizationMail.getOriginAuthInfo();
    Map<AuthType, Set<AuthorizationInfo>> authInfo = authorizationMail.getAuthInfo();
    String jobName = authorizationMail.getJobName();

    for (Map.Entry<AuthType, Set<AuthorizationInfo>> entry : authInfo.entrySet()) {
      AuthType authType = entry.getKey();
      List<String> originNtList = originAuthInfo.get(authType).stream()
          .map(AuthorizationInfo::getNt).collect(Collectors.toList());
      for (AuthorizationInfo info : entry.getValue()) {
        if (!originNtList.contains(info.getNt())) {
          LOGGER.info("Start to send {} grant auth email -{} to {}",
              authorizationMail.getAuthorizationType(), jobName, info.getName());
          String authName = authorizationMail.getAuthorizationType().convertAuthTypeName(authType);
          String subject = String.format(GRANT_AUTH_SUBJECT,
              authorizationMail.getAuthorizationType().type, authName, jobName);
          String content = String.format(GRANT_AUTH_CONTENT, info.getName(), authName
              , jobName, nt, authorizationMail.getLink());
          mailService.sendEmail(MailService.MailTemplate.HTML, subject, content
              , info.getEmail(), nt + EMAIL_POSTFIX);
        }
      }
    }
  }

  @Async("asyncEventExecutor")
  public void sendRequestEmail(String nt, AuthorizationMail authorizationMail) {
    String jobName = authorizationMail.getJobName();
    LOGGER.info("Start to send {} apply access email - {} for {}",
        authorizationMail.getAuthorizationType(), jobName, nt);
    String subject = String.format(APPLY_AUTH_SUBJECT,
        authorizationMail.getAuthorizationType().type, nt, jobName);
    String content = String.format(APPLY_AUTH_CONTENT, jobName, nt, jobName, authorizationMail.getLink());
    List<String> owners = authorizationMail.getOriginAuthInfo().get(AuthType.OWNERS)
        .stream().map(AuthorizationInfo::getEmail)
        .collect(Collectors.toList());
    owners.add(authorizationMail.getOwner() + EMAIL_POSTFIX);
    mailService.sendEmail(MailService.MailTemplate.HTML, subject, content
        , owners.stream().collect(Collectors.joining(";")), nt + EMAIL_POSTFIX);
  }
}
