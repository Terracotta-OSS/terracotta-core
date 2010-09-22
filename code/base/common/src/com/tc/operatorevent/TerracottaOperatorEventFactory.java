/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.operatorevent;

import com.tc.operatorevent.TerracottaOperatorEvent.EventSubsystem;
import com.tc.operatorevent.TerracottaOperatorEvent.EventType;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;

import java.text.MessageFormat;

public class TerracottaOperatorEventFactory {

  /**
   * Memory Manager Events
   */
  public static TerracottaOperatorEvent createLongGCOperatorEvent(Object[] arguments) {
    return new TerracottaOperatorEventImpl(EventType.WARN, EventSubsystem.MEMORY_MANAGER, MessageFormat
        .format(TerracottaOperatorEventResources.getLongGCMessage(), arguments));
  }

  public static TerracottaOperatorEvent createHighMemoryUsageEvent(int memoryUsage) {
    return new TerracottaOperatorEventImpl(EventType.WARN, EventSubsystem.MEMORY_MANAGER, MessageFormat
        .format(TerracottaOperatorEventResources.getHighMemoryUsageMessage(), new Object[] { memoryUsage }));
  }

  public static TerracottaOperatorEvent createOffHeapMapMemoryUsageEvent(String allocated, String maxSize,
                                                                         int percentageUsed) {
    return new TerracottaOperatorEventImpl(EventType.WARN, EventSubsystem.MEMORY_MANAGER, MessageFormat
        .format(TerracottaOperatorEventResources.getOffHeapMapMemoryUsageMessage(), new Object[] { allocated, maxSize,
            percentageUsed }));
  }

  public static TerracottaOperatorEvent createOffHeapObjectMemoryUsageEvent(String allocated, String maxSize,
                                                                            int percentageUsed) {
    return new TerracottaOperatorEventImpl(EventType.WARN, EventSubsystem.MEMORY_MANAGER, MessageFormat
        .format(TerracottaOperatorEventResources.getOffHeapObjectMemoryUsageMessage(), new Object[] { allocated,
            maxSize, percentageUsed }));
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

  public static TerracottaOperatorEvent createDirtyDBEvent() {
    String restart;
    if (TCPropertiesImpl.getProperties().getBoolean(TCPropertiesConsts.L2_NHA_DIRTYDB_AUTODELETE)) {
      restart = "enabled";
    } else {
      restart = "disabled";
    }
    return new TerracottaOperatorEventImpl(EventType.ERROR, EventSubsystem.HA, MessageFormat
        .format(TerracottaOperatorEventResources.getDirtyDBMessage(), new Object[] { restart }));
  }
}
