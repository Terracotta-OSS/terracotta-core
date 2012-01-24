/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.core;

import com.tc.util.Assert;

public class ConnectionInfo implements java.io.Serializable {

  public static final ConnectionInfo[] EMPTY_ARRAY = new ConnectionInfo[0];
  private final String                 hostname;
  private final int                    port;
  private final int                    groupId;
  private final String                 groupName;

  public ConnectionInfo(String hostname, int port) {
    this(hostname, port, 0, null);
  }

  public ConnectionInfo(String hostname, int port, int groupId, String groupName) {
    Assert.assertNotNull(hostname);
    this.hostname = hostname;
    this.port = port;
    this.groupId = groupId;
    this.groupName = groupName;
  }

  public String getHostname() {
    return hostname;
  }

  public int getPort() {
    return port;
  }

  public int getGroupId() {
    return groupId;
  }

  public String getGroupName() {
    return groupName;
  }

  public boolean equals(Object o) {
    if (o == this) return true;
    if (o instanceof ConnectionInfo) {
      ConnectionInfo other = (ConnectionInfo) o;
      if (groupName == null) {
        return this.hostname.equals(other.getHostname()) && this.port == other.getPort();
      } else {
        return this.hostname.equals(other.getHostname()) && this.port == other.getPort()
               && this.groupName.equals(other.groupName);
      }
    }
    return false;
  }

  public int hashCode() {
    return toString().hashCode();
  }

  private String s;

  public String toString() {
    return (s == null ? (s = hostname + ":" + port + (groupName != null ? ":" + groupName : "")) : s);
  }
}
