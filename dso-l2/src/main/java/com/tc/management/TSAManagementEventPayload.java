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

  public TSAManagementEventPayload() {
  }

  public TSAManagementEventPayload(String targetNodeId) {
    this.targetNodeId = targetNodeId;
  }

  public String getTargetNodeId() {
    return targetNodeId;
  }

  public void setTargetNodeId(String targetNodeId) {
    this.targetNodeId = targetNodeId;
  }
}
