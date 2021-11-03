package com.ebay.dss.zds.model;

public class AuthorizationInfo {

  String nt;

  String email;

  String name;

  public String getNt() {
    return nt;
  }

  public void setNt(String nt) {
    this.nt = nt;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public enum AuthType {
    OWNERS, READERS, WRITERS
  }
}
