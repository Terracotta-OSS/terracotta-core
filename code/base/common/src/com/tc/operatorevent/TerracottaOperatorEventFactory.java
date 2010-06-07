/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.operatorevent;

import com.tc.net.NodeID;
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
  public static TerracottaOperatorEvent createNodeConnectedEvent(NodeID nodeID) {
    return new TerracottaOperatorEventImpl(EventType.INFO, EventSubsystem.HA, MessageFormat
        .format(TerracottaOperatorEventResources.getNodeAvailabiltyMessage(), new Object[] { nodeID.toString(),
            "joined" }));
  }
  
  public static TerracottaOperatorEvent createNodeDisconnectedEvent(NodeID nodeID) {
    return new TerracottaOperatorEventImpl(EventType.INFO, EventSubsystem.HA, MessageFormat
        .format(TerracottaOperatorEventResources.getNodeAvailabiltyMessage(), new Object[] { nodeID.toString(),
            "left" }));
  }

  public static TerracottaOperatorEvent createMoveToPassiveStandByEvent() {
    return new TerracottaOperatorEventImpl(EventType.INFO, EventSubsystem.HA, TerracottaOperatorEventResources
        .getMoveToPassiveStandByMessage());
  }
  
  /**
   * Lock manager events
   */
  public static TerracottaOperatorEvent createLockGCEvent(Object[] arguments) {
    return new TerracottaOperatorEventImpl(EventType.WARN, EventSubsystem.LOCK_MANAGER, MessageFormat
                                           .format(TerracottaOperatorEventResources.getLockGCMessage(), arguments));
  }
}
