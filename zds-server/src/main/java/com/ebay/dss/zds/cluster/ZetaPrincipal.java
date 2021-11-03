package com.ebay.dss.zds.cluster;

import java.security.Principal;

/**
 * Created by tatian on 2021-02-05.
 */
public class ZetaPrincipal {

  public static Principal newPrincipal(String user) {
    return new NameBasedPrincipal(user);
  }

  public static class NameBasedPrincipal implements Principal {

    private final String name;
    private final int hashCode;

    public NameBasedPrincipal(String name) {
      this.name = name;
      this.hashCode = this.name != null? this.name.hashCode() : -1;
    }

    public boolean equals(Object another) {
      return (another instanceof Principal)
              && this.getName() != null
              && (this.getName().equals(((Principal) another).getName()));
    }

    public String toString() {
      return getName();
    }

    public int hashCode() {
      return this.hashCode;
    }

    public String getName() {
      return this.name;
    }
  }
}
