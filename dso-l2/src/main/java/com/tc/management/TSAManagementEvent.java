/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management;

import java.io.Serializable;

/**
 *
 */
public class TSAManagementEvent implements Serializable {

  private String type;
  private String targetNodeId;

  public TSAManagementEvent() {
  }

  public TSAManagementEvent(String type, String targetNodeId) {
    this.type = type;
    this.targetNodeId = targetNodeId;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getTargetNodeId() {
    return targetNodeId;
  }

  public void setTargetNodeId(String targetNodeId) {
    this.targetNodeId = targetNodeId;
  }
}
