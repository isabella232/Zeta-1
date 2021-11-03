package com.ebay.dss.zds.service.authorization;

public class AuthorizationConstant {

  public static final String NO_ACCESS_GRANT = "Insufficient privileges to grant permission";
  public static final String AUTH_INFO_MISS = "AuthInfo miss %s";
  public static final String NO_ACCESS_READ = "Insufficient privileges to read";
  public static final String NO_ACCESS_EDIT = "Insufficient privileges to write";
  public static final String NO_ACCESS_UPDATE = "Insufficient privileges to update";
  public static final String NO_ACCESS_DELETE = "Insufficient privileges to delete";
  public static final String NO_ACCESS_OPERATE = "Insufficient privileges to operate";

  public static final String EMAIL_POSTFIX = "@ebay.com";

  public static final String GRANT_AUTH_SUBJECT = "[%s]-You have been added as a %s of \"%s\"";
  public static final String GRANT_AUTH_CONTENT = "<div>Hi %s,</div><br> <div>You have been added as a %s of the [%s] by %s." +
      " Please <a href=\"%s\">click</a> here to check details.</div>";

  public static final String APPLY_AUTH_SUBJECT = "[%s]-%s apply access to \"%s\"";
  public static final String APPLY_AUTH_CONTENT = "<div>Hi %s Admin,</div><br> <div>%s apply the access permission to [%s]." +
      " Please <a href=\"%s\">click</a> here to grant access.</div>";

}
