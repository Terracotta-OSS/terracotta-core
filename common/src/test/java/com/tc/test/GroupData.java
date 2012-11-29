/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test;

public class GroupData {
  private final String   groupName;
  private int[]          tsaPorts;
  private final int[]    jmxPorts;
  private final int[]    l2GroupPorts;
  private final String[] serverNames;

  public GroupData(int[] tsaPorts, int[] jmxPorts, int[] l2GroupPorts, String[] serverNames) {
    this(null, tsaPorts, jmxPorts, l2GroupPorts, serverNames);
  }

  public GroupData(String groupName, int[] tsaPorts, int[] jmxPorts, int[] l2GroupPorts, String[] serverNames) {
    this.groupName = groupName;
    this.serverNames = serverNames;
    this.tsaPorts = tsaPorts;
    this.jmxPorts = jmxPorts;
    this.l2GroupPorts = l2GroupPorts;
  }

  public void setTsaPorts(int[] tsaPorts) {
    this.tsaPorts = tsaPorts;
  }

  public String getGroupName() {
    return this.groupName;
  }

  public int[] getTsaPorts() {
    return tsaPorts;
  }

  public int[] getJmxPorts() {
    return jmxPorts;
  }

  public int[] getL2GroupPorts() {
    return l2GroupPorts;
  }

  public String[] getServerNames() {
    return serverNames;
  }

  public int getServerCount() {
    return tsaPorts.length;
  }

}
