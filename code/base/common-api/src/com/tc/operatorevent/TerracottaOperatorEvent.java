/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.operatorevent;

import java.io.Serializable;
import java.util.Date;

public interface TerracottaOperatorEvent extends Serializable, Cloneable {

  public static enum EventType {
    INFO, WARN, DEBUG, ERROR, CRITICAL
  }

  public static enum EventSubsystem {
    MEMORY_MANAGER, DGC, CLUSTER_TOPOLOGY, LOCK_MANAGER, DCV2, TOOLKIT, SYSTEM_SETUP
  }

  void addNodeName(String nodeId);

  EventType getEventType();

  String getNodeName();

  Date getEventTime();

  EventSubsystem getEventSubsystem();

  String getEventMessage();

  /**
   * These methods are there because devconsole does not take enum as the return type while updating the panel Should be
   * dealt with in future
   */
  String getEventTypeAsString();

  String getEventSubsystemAsString();

  /**
   * These methods are to decide whether one particular event can be collapsed into an existing row in dev console.
   */
  String getCollapseString();

  /**
   * These methods are to determine whether the event has been read before or not in the dev-console
   */
  void markRead();

  void markUnread();

  boolean isRead();

  /**
   * This method is used to get the event in String format.
   */
  String extractAsText();

  TerracottaOperatorEvent cloneEvent();
}
