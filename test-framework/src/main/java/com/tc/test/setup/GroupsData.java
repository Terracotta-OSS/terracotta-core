/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.test.setup;

import java.io.Serializable;

import junit.framework.Assert;

public class GroupsData implements Serializable {
  private final String   groupName;
  private int[]          tsaPorts;
  private final int[]    jmxPorts;
  private final int[]    tsaGroupPorts;
  private final String[] serverNames;
  private final int[]    proxyTsaGroupPorts;
  private final int[]    proxyTsaPorts;
  private final String[] dataDirectoryPath;
  private final String[] logDirectoryPath;
  private final String[] backupDirectoryPath;
  private final int      groupIndex;
  private final int[]    managementPorts;

  public GroupsData(String groupName, int[] tsaPorts, int[] jmxPorts, int[] tsaGroupPorts, int[] managementPorts,
                    String[] serverNames, int[] proxyTsaPorts, int[] proxyTsaGroupPorts, String[] dataDirectoryPath,
                    String[] logDirectoryPath,
                    String[] backupDirectoryPath, final int groupIndex) {
    this.groupName = groupName;
    this.serverNames = serverNames;
    this.tsaPorts = tsaPorts;
    this.jmxPorts = jmxPorts;
    this.tsaGroupPorts = tsaGroupPorts;
    this.proxyTsaPorts = proxyTsaPorts;
    this.proxyTsaGroupPorts = proxyTsaGroupPorts;
    this.managementPorts = managementPorts;
    this.dataDirectoryPath = dataDirectoryPath;
    this.logDirectoryPath = logDirectoryPath;
    this.backupDirectoryPath = backupDirectoryPath;
    this.groupIndex = groupIndex;
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

  public int getManagementPort(final int serverIndex) {
    Assert.assertTrue("server index > numOfServers, serverIndex: " + serverIndex + " numOfServers: "
                      + this.jmxPorts.length, (serverIndex >= 0 && serverIndex < this.managementPorts.length));
    return managementPorts[serverIndex];
  }

  public int getTsaGroupPort(final int serverIndex) {
    Assert.assertTrue("server index > numOfServers, serverIndex: " + serverIndex + " numOfServers: "
                      + this.tsaGroupPorts.length, (serverIndex >= 0 && serverIndex < this.tsaGroupPorts.length));
    return tsaGroupPorts[serverIndex];
  }

  public int getProxyTsaGroupPort(final int serverIndex) {
    Assert.assertTrue("server index > numOfServers, serverIndex: " + serverIndex + " numOfServers: "
                          + this.proxyTsaGroupPorts.length,
                      (serverIndex >= 0 && serverIndex < this.proxyTsaGroupPorts.length));
    return proxyTsaGroupPorts[serverIndex];
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

  public int getGroupIndex() {
    return groupIndex;
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

    strBuilder.append("tsaGroupPorts: ");
    for (int tsaGroupPort : tsaGroupPorts) {
      strBuilder.append(tsaGroupPort + ", ");
    }

    strBuilder.append("managementPorts: ");
    for (int managementPort : managementPorts) {
      strBuilder.append(managementPort + ", ");
    }

    return strBuilder.toString();

  }

}
