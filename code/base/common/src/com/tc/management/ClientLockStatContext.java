/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management;

import com.tc.net.groups.NodeID;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesImpl;

import java.util.HashSet;
import java.util.Set;

public class ClientLockStatContext {
  private final static int DEFAULT_DEPTH             = 0;
  private final static int DEFAULT_COLLECT_FREQUENCY = 10;

  private int              collectFrequency;
  private int              stackTraceDepth           = 0;
  private int              lockAccessedFrequency     = 0;
  private Set              statEnabledClients        = new HashSet();

  public ClientLockStatContext() {
    TCProperties tcProperties = TCPropertiesImpl.getProperties().getPropertiesFor("l1.lock.stacktrace");
    if (tcProperties != null) {
      this.stackTraceDepth = tcProperties.getInt("defaultDepth", DEFAULT_DEPTH);
    }
    tcProperties = TCPropertiesImpl.getProperties().getPropertiesFor("l1.lock");
    if (tcProperties != null) {
      this.collectFrequency = tcProperties.getInt("collectFrequency", DEFAULT_COLLECT_FREQUENCY);
    }
  }

  public ClientLockStatContext(int collectFrequency, int stackTraceDepth) {
    this.collectFrequency = collectFrequency;
    this.stackTraceDepth = stackTraceDepth;
  }

  public int getCollectFrequency() {
    return collectFrequency;
  }

  public void setCollectFrequency(int collectFrequency) {
    this.collectFrequency = collectFrequency;
  }

  public int getStackTraceDepth() {
    return stackTraceDepth;
  }

  public void setStackTraceDepth(int stackTraceDepth) {
    this.stackTraceDepth = stackTraceDepth;
  }

  public void addClient(NodeID nodeID) {
    statEnabledClients.add(nodeID);
  }

  public boolean isClientLockStatEnabled(NodeID nodeID) {
    return statEnabledClients.contains(nodeID);
  }

  public Set getStatEnabledClients() {
    return statEnabledClients;
  }

  public int getLockAccessedFrequency() {
    return this.lockAccessedFrequency;
  }

  public void lockAccessed() {
    this.lockAccessedFrequency = (this.lockAccessedFrequency + 1) % collectFrequency;
  }
}
