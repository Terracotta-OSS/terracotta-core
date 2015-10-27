/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */

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

  public SecurityInfo(boolean secure, String username) {
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
  public boolean equals(Object o) {
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
