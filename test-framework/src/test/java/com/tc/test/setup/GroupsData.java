/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.setup;

import java.io.Serializable;

import junit.framework.Assert;

public class GroupsData implements Serializable {
  private final String   groupName;
  private int[]          dsoPorts;
  private final int[]    jmxPorts;
  private final int[]    l2GroupPorts;
  private final String[] serverNames;
  private final int[]    proxyL2GroupPorts;
  private final int[]    proxyDsoPorts;
  private final String[] dataDirectoryPath;

  public GroupsData(String groupName, int[] dsoPorts, int[] jmxPorts, int[] l2GroupPorts, String[] serverNames,
                    int[] proxyDsoPorts, int[] proxyL2GroupPorts, String[] dataDirectoryPath) {
    this.groupName = groupName;
    this.serverNames = serverNames;
    this.dsoPorts = dsoPorts;
    this.jmxPorts = jmxPorts;
    this.l2GroupPorts = l2GroupPorts;
    this.proxyDsoPorts = proxyDsoPorts;
    this.proxyL2GroupPorts = proxyL2GroupPorts;
    this.dataDirectoryPath = dataDirectoryPath;
  }

  public void setDsoPorts(int[] dsoPorts) {
    this.dsoPorts = dsoPorts;
  }

  public String getGroupName() {
    return this.groupName;
  }

  public int getDsoPort(final int serverIndex) {
    Assert.assertTrue("server index > numOfServers, serverIndex: " + serverIndex + " numOfServers: "
                      + this.dsoPorts.length, (serverIndex >= 0 && serverIndex < this.dsoPorts.length));
    return dsoPorts[serverIndex];
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

  public int getProxyDsoPort(final int serverIndex) {
    Assert.assertTrue("server index > numOfServers, serverIndex: " + serverIndex + " numOfServers: "
                      + this.proxyDsoPorts.length, (serverIndex >= 0 && serverIndex < this.proxyDsoPorts.length));
    return proxyDsoPorts[serverIndex];
  }

  public String getDataDirectoryPath(final int serverIndex) {
    Assert.assertTrue("server index > numOfServers, serverIndex: " + serverIndex + " numOfServers: "
                          + this.dataDirectoryPath.length,
                      (serverIndex >= 0 && serverIndex < this.dataDirectoryPath.length));
    return dataDirectoryPath[serverIndex];
  }

  public String[] getServerNames() {
    return serverNames;
  }

  public int getServerCount() {
    return dsoPorts.length;
  }

  @Override
  public String toString() {
    StringBuilder strBuilder = new StringBuilder();
    strBuilder.append("groupName: " + this.groupName + ", ");

    strBuilder.append("ServerNames: ");
    for (String servrName : serverNames) {
      strBuilder.append(servrName + ", ");
    }

    strBuilder.append("dsoPorts: ");
    for (int dsoPort : dsoPorts) {
      strBuilder.append(dsoPort + ", ");
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
