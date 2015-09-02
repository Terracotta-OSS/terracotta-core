/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link TCManagementEvent} payload that originates from a server.
 *
 * @author Ludovic Orban
 */
public class TSAManagementEventPayload implements Serializable {

  private String type;
  private final Map<String, Object> attributes = new HashMap<String, Object>();

  public TSAManagementEventPayload() {
  }

  public TSAManagementEventPayload(String type) {
    this.type = type;
  }

  public Map<String, Object> getAttributes() {
    return attributes;
  }

  public String getType() {
    return type;
  }

  public TCManagementEvent toManagementEvent() {
    return new TCManagementEvent(this, getType());
  }

}
