/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.operatorevent;

import com.tc.operatorevent.TerracottaOperatorEvent.EventSubsystem;
import com.tc.operatorevent.TerracottaOperatorEvent.EventType;

import java.text.MessageFormat;

public class TerracottaOperatorEventFactory {

  /**
   * Memory Manager Events
   */
  public static TerracottaOperatorEvent createLongGCOperatorEvent(Object[] arguments) {
    return new TerracottaOperatorEventImpl(EventType.WARN, EventSubsystem.MEMORY_MANAGER, MessageFormat
        .format(TerracottaOperatorEventResources.getLongGCMessage(), arguments));
  }

  public static TerracottaOperatorEvent createHighMemoryUsageEvent(Object[] arguments) {
    return new TerracottaOperatorEventImpl(EventType.WARN, EventSubsystem.MEMORY_MANAGER, MessageFormat
        .format(TerracottaOperatorEventResources.getHighMemoryUsageMessage(), arguments));
  }

  /**
   * DGC events
   */
  public static TerracottaOperatorEvent createDGCStartedEvent() {
    return new TerracottaOperatorEventImpl(EventType.INFO, EventSubsystem.DGC, TerracottaOperatorEventResources
        .getDGCStartedMessage());
  }

  public static TerracottaOperatorEvent createDGCFinishedEvent(Object[] arguments) {
    return new TerracottaOperatorEventImpl(EventType.INFO, EventSubsystem.DGC, MessageFormat
        .format(TerracottaOperatorEventResources.getDGCFinishedMessage(), arguments));
  }

  public static TerracottaOperatorEvent createDGCCanceledEvent() {
    return new TerracottaOperatorEventImpl(EventType.INFO, EventSubsystem.DGC, TerracottaOperatorEventResources
        .getDGCCanceledMessage());
  }

  /**
   * High availability events
   */
  public static TerracottaOperatorEvent createNodeConnectedEvent(String nodeName) {
    return new TerracottaOperatorEventImpl(EventType.INFO, EventSubsystem.HA, MessageFormat
        .format(TerracottaOperatorEventResources.getNodeAvailabiltyMessage(), new Object[] { nodeName, "joined" }));
  }

  public static TerracottaOperatorEvent createNodeDisconnectedEvent(String nodeName) {
    return new TerracottaOperatorEventImpl(EventType.WARN, EventSubsystem.HA, MessageFormat
        .format(TerracottaOperatorEventResources.getNodeAvailabiltyMessage(), new Object[] { nodeName, "left" }));
  }

  public static TerracottaOperatorEvent createClusterNodeStateChangedEvent(Object[] arguments) {
    return new TerracottaOperatorEventImpl(EventType.INFO, EventSubsystem.HA, MessageFormat
        .format(TerracottaOperatorEventResources.getClusterNodeStateChangedMessage(), arguments));
  }
  
  /**
   * Lock manager events
   */
  public static TerracottaOperatorEvent createLockGCEvent(Object[] arguments) {
    return new TerracottaOperatorEventImpl(EventType.WARN, EventSubsystem.LOCK_MANAGER, MessageFormat
                                           .format(TerracottaOperatorEventResources.getLockGCMessage(), arguments));
  }
  
  /**
   * zap events
   */
  public static TerracottaOperatorEvent createZapRequestReceivedEvent(Object[] arguments) {
    return new TerracottaOperatorEventImpl(EventType.CRITICAL, EventSubsystem.HA, MessageFormat
        .format(TerracottaOperatorEventResources.getZapRequestReceivedMessage(), arguments));
  }

  public static TerracottaOperatorEvent createZapRequestAcceptedEvent(Object[] arguments) {
    return new TerracottaOperatorEventImpl(EventType.CRITICAL, EventSubsystem.HA, MessageFormat
        .format(TerracottaOperatorEventResources.getZapRequestAcceptedMessage(), arguments));
  }
}
