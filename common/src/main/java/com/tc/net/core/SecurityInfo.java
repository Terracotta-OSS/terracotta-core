package com.tc.net.core;

/**
 * @author Alex Snaps
 */
public class SecurityInfo {

  private final boolean secure;
  private final String username;

  public SecurityInfo() {
    this(false, null);
  }

  public SecurityInfo(final boolean secure, final String username) {
    this.secure = secure;
    this.username = username;
  }

  public boolean isSecure() {
    return secure;
  }

  public String getUsername() {
    return username;
  }

  @Override
  public String toString() {
    return "SecurityInfo{" +
           "secure=" + secure +
           ", username='" + username + '\'' +
           '}';
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final SecurityInfo that = (SecurityInfo)o;

    return secure == that.secure && !(username != null ? !username.equals(that.username) : that.username != null);
  }

  @Override
  public int hashCode() {
    int result = (secure ? 1 : 0);
    result = 31 * result + (username != null ? username.hashCode() : 0);
    return result;
  }

  public boolean hasCredentials() {
    return username != null;
  }
}
