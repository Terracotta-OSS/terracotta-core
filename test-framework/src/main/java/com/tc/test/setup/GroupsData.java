/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.setup;

import java.io.Serializable;

import junit.framework.Assert;

public class GroupsData implements Serializable {
  private final String   groupName;
  private int[]          tsaPorts;
  private final int[]    jmxPorts;
  private final int[]    l2GroupPorts;
  private final String[] serverNames;
  private final int[]    proxyL2GroupPorts;
  private final int[]    proxyTsaPorts;
  private final String[] dataDirectoryPath;
  private final String[] logDirectoryPath;
  private final String[] backupDirectoryPath;

  public GroupsData(String groupName, int[] tsaPorts, int[] jmxPorts, int[] l2GroupPorts, String[] serverNames,
                    int[] proxyTsaPorts, int[] proxyL2GroupPorts, String[] dataDirectoryPath,
                    String[] logDirectoryPath,
                    String[] backupDirectoryPath) {
    this.groupName = groupName;
    this.serverNames = serverNames;
    this.tsaPorts = tsaPorts;
    this.jmxPorts = jmxPorts;
    this.l2GroupPorts = l2GroupPorts;
    this.proxyTsaPorts = proxyTsaPorts;
    this.proxyL2GroupPorts = proxyL2GroupPorts;
    this.dataDirectoryPath = dataDirectoryPath;
    this.logDirectoryPath = logDirectoryPath;
    this.backupDirectoryPath = backupDirectoryPath;
  }

  public void setTsaPorts(int[] tsaPorts) {
    this.tsaPorts = tsaPorts;
  }

  public String getGroupName() {
    return this.groupName;
  }

  public int getTsaPort(final int serverIndex) {
    Assert.assertTrue("server index > numOfServers, serverIndex: " + serverIndex + " numOfServers: "
                      + this.tsaPorts.length, (serverIndex >= 0 && serverIndex < this.tsaPorts.length));
    return tsaPorts[serverIndex];
  }

  public int getJmxPort(final int serverIndex) {
    Assert.assertTrue("server index > numOfServers, serverIndex: " + serverIndex + " numOfServers: "
                      + this.jmxPorts.length, (serverIndex >= 0 && serverIndex < this.jmxPorts.length));
    return jmxPorts[serverIndex];
  }

  public int getL2GroupPort(final int serverIndex) {
    Assert.assertTrue("server index > numOfServers, serverIndex: " + serverIndex + " numOfServers: "
                      + this.l2GroupPorts.length, (serverIndex >= 0 && serverIndex < this.l2GroupPorts.length));
    return l2GroupPorts[serverIndex];
  }

  public int getProxyL2GroupPort(final int serverIndex) {
    Assert.assertTrue("server index > numOfServers, serverIndex: " + serverIndex + " numOfServers: "
                          + this.proxyL2GroupPorts.length,
                      (serverIndex >= 0 && serverIndex < this.proxyL2GroupPorts.length));
    return proxyL2GroupPorts[serverIndex];
  }

  public int getProxyTsaPort(final int serverIndex) {
    Assert.assertTrue("server index > numOfServers, serverIndex: " + serverIndex + " numOfServers: "
                      + this.proxyTsaPorts.length, (serverIndex >= 0 && serverIndex < this.proxyTsaPorts.length));
    return proxyTsaPorts[serverIndex];
  }

  public String getDataDirectoryPath(final int serverIndex) {
    Assert.assertTrue("server index > numOfServers, serverIndex: " + serverIndex + " numOfServers: "
                          + this.dataDirectoryPath.length,
                      (serverIndex >= 0 && serverIndex < this.dataDirectoryPath.length));
    return dataDirectoryPath[serverIndex];
  }

  public String getLogDirectoryPath(final int serverIndex) {
    Assert.assertTrue("server index > numOfServers, serverIndex: " + serverIndex + " numOfServers: "
                      + this.logDirectoryPath.length, (serverIndex >= 0 && serverIndex < this.logDirectoryPath.length));
    return logDirectoryPath[serverIndex];
  }

  public String getBackupDirectoryPath(final int serverIndex) {
    Assert.assertTrue("server index > numOfServers, serverIndex: " + serverIndex + " numOfServers: "
                      + this.backupDirectoryPath.length, (serverIndex >= 0 && serverIndex < this.backupDirectoryPath.length));
    return backupDirectoryPath[serverIndex];
  }

  public String[] getServerNames() {
    return serverNames;
  }

  public int getServerCount() {
    return tsaPorts.length;
  }

  @Override
  public String toString() {
    StringBuilder strBuilder = new StringBuilder();
    strBuilder.append("groupName: " + this.groupName + ", ");

    strBuilder.append("ServerNames: ");
    for (String servrName : serverNames) {
      strBuilder.append(servrName + ", ");
    }

    strBuilder.append("tsaPorts: ");
    for (int tsaPort : tsaPorts) {
      strBuilder.append(tsaPort + ", ");
    }

    strBuilder.append("jmxPorts: ");
    for (int jmxPort : jmxPorts) {
      strBuilder.append(jmxPort + ", ");
    }

    strBuilder.append("l2GroupPorts: ");
    for (int l2GroupPort : l2GroupPorts) {
      strBuilder.append(l2GroupPort + ", ");
    }

    return strBuilder.toString();

  }

}
