package com.ebay.dss.zds.kerberos;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class KerberosContextUser {

    private final boolean isPasswordBased;
    private final boolean isTicketCacheBased;
    private final String username;
    private final String fileLoc;
    private final char[] password;

    private KerberosContextUser(boolean isPasswordBased, boolean isTicketCacheBased, String username, String fileLoc, char[] password) {
        this.isPasswordBased = isPasswordBased;
        this.isTicketCacheBased = isTicketCacheBased;
        this.username = username;
        this.fileLoc = isPasswordBased ? "" : fileLoc;
        this.password = isPasswordBased ? password : new char[0];
    }

    public KerberosContextUser(String username, String fileLoc, boolean isTicketCacheBased) {
        this(false, isTicketCacheBased, username, fileLoc, null);
    }

    public KerberosContextUser(String username, String fileLoc) {
        this(false, false, username, fileLoc, null);
    }

    public KerberosContextUser(String username, char[] password) {
        this(true, false, username, null, password);
    }

    public boolean isTicketCacheBased() {
        return isTicketCacheBased;
    }

    public boolean isPasswordBased() {
        return isPasswordBased;
    }

    public String getUsername() {
        return username;
    }

    public String getFileLoc() {
        return fileLoc;
    }

    public char[] getPassword() {
        return password;
    }

    @Override
    public String toString() {
        return "KerberosContextUser{" +
                "isPasswordBased=" + isPasswordBased +
                ", isTicketCacheBased=" + isTicketCacheBased +
                ", username='" + username + '\'' +
                ", fileLoc='" + fileLoc + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof KerberosContextUser)) return false;

        KerberosContextUser that = (KerberosContextUser) o;

        return new EqualsBuilder()
                .append(username, that.username)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(username)
                .toHashCode();
    }
}
