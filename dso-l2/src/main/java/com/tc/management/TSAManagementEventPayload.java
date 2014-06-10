/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management;

import java.io.Serializable;

/**
 *
 */
public class TSAManagementEventPayload implements Serializable {

  private String targetNodeId;
  private String targetJmxId;

  public TSAManagementEventPayload() {
  }

  public TSAManagementEventPayload(String targetNodeId, String targetJmxId) {
    this.targetNodeId = targetNodeId;
    this.targetJmxId = targetJmxId;
  }

  public String getTargetNodeId() {
    return targetNodeId;
  }

  public void setTargetNodeId(String targetNodeId) {
    this.targetNodeId = targetNodeId;
  }

  public String getTargetJmxId() {
    return targetJmxId;
  }

  public void setTargetJmxId(String targetJmxId) {
    this.targetJmxId = targetJmxId;
  }
}
