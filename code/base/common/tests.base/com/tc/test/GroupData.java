/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test;

public class GroupData {
  private final int[]    dsoPorts;
  private final int[]    jmxPorts;
  private final int[]    l2GroupPorts;
  private final String[] serverNames;

  public GroupData(int[] dsoPorts, int[] jmxPorts, int[] l2GroupPorts, String[] serverNames) {
    this.serverNames = serverNames;
    this.dsoPorts = dsoPorts;
    this.jmxPorts = jmxPorts;
    this.l2GroupPorts = l2GroupPorts;
  }

  public int[] getDsoPorts() {
    return dsoPorts;
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
    return dsoPorts.length;
  }

}
