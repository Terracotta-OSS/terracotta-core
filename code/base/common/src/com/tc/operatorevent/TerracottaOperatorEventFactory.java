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
        .format(TerracottaOperatorEventResources.getLongGCMessage(), arguments), "");
  }

  public static TerracottaOperatorEvent createHighMemoryUsageEvent(int memoryUsage, int critcalThreshold) {
    return new TerracottaOperatorEventImpl(EventType.WARN, EventSubsystem.MEMORY_MANAGER, MessageFormat
        .format(TerracottaOperatorEventResources.getHighMemoryUsageMessage(), new Object[] { memoryUsage,
            critcalThreshold }), "");
  }

  public static TerracottaOperatorEvent createOffHeapMemoryUsageEvent(String allocated, String maxSize,
                                                                      int percentageUsed) {
    return new TerracottaOperatorEventImpl(EventType.WARN, EventSubsystem.MEMORY_MANAGER, MessageFormat
        .format(TerracottaOperatorEventResources.getOffHeapMemoryUsageMessage(), new Object[] { allocated, maxSize,
            percentageUsed }), "");
  }

  /**
   * DGC events
   */
  public static TerracottaOperatorEvent createDGCStartedEvent() {
    return new TerracottaOperatorEventImpl(EventType.INFO, EventSubsystem.DGC, TerracottaOperatorEventResources
        .getDGCStartedMessage(), "dgc started");
  }

  public static TerracottaOperatorEvent createDGCFinishedEvent(Object[] arguments) {
    return new TerracottaOperatorEventImpl(EventType.INFO, EventSubsystem.DGC, MessageFormat
        .format(TerracottaOperatorEventResources.getDGCFinishedMessage(), arguments), "dgc finished");
  }

  public static TerracottaOperatorEvent createDGCCanceledEvent() {
    return new TerracottaOperatorEventImpl(EventType.INFO, EventSubsystem.DGC, TerracottaOperatorEventResources
        .getDGCCanceledMessage(), "dgc canceled");
  }

  /**
   * High availability events
   */
  public static TerracottaOperatorEvent createNodeConnectedEvent(String nodeName) {
    return new TerracottaOperatorEventImpl(EventType.INFO, EventSubsystem.CLUSTER_TOPOLOGY, MessageFormat
        .format(TerracottaOperatorEventResources.getNodeAvailabiltyMessage(), new Object[] { nodeName, "joined" }),
                                           nodeName + "joined");
  }

  public static TerracottaOperatorEvent createNodeDisconnectedEvent(String nodeName) {
    return new TerracottaOperatorEventImpl(EventType.WARN, EventSubsystem.CLUSTER_TOPOLOGY, MessageFormat
        .format(TerracottaOperatorEventResources.getNodeAvailabiltyMessage(), new Object[] { nodeName, "left" }),
                                           nodeName + "left");
  }

  public static TerracottaOperatorEvent createClusterNodeStateChangedEvent(Object[] arguments) {
    return new TerracottaOperatorEventImpl(EventType.INFO, EventSubsystem.CLUSTER_TOPOLOGY, MessageFormat
        .format(TerracottaOperatorEventResources.getClusterNodeStateChangedMessage(), arguments), "");
  }

  public static TerracottaOperatorEvent createHandShakeRejectedEvent(String clientVersion, String serverVersion) {
    return new TerracottaOperatorEventImpl(EventType.ERROR, EventSubsystem.CLUSTER_TOPOLOGY, MessageFormat
        .format(TerracottaOperatorEventResources.getHandshakeRejectedMessage(), new Object[] { clientVersion,
            serverVersion }), "handshake rejected");
  }

  /**
   * zap events
   */
  public static TerracottaOperatorEvent createZapRequestReceivedEvent(Object[] arguments) {
    return new TerracottaOperatorEventImpl(EventType.CRITICAL, EventSubsystem.CLUSTER_TOPOLOGY, MessageFormat
        .format(TerracottaOperatorEventResources.getZapRequestReceivedMessage(), arguments), "");
  }

  public static TerracottaOperatorEvent createZapRequestAcceptedEvent(Object[] arguments) {
    return new TerracottaOperatorEventImpl(EventType.CRITICAL, EventSubsystem.CLUSTER_TOPOLOGY, MessageFormat
        .format(TerracottaOperatorEventResources.getZapRequestAcceptedMessage(), arguments), "");
  }

  public static TerracottaOperatorEvent createDirtyDBEvent() {
    String restart;
    if (TCPropertiesImpl.getProperties().getBoolean(TCPropertiesConsts.L2_NHA_DIRTYDB_AUTODELETE)) {
      restart = "enabled";
    } else {
      restart = "disabled";
    }
    return new TerracottaOperatorEventImpl(EventType.ERROR, EventSubsystem.CLUSTER_TOPOLOGY, MessageFormat
        .format(TerracottaOperatorEventResources.getDirtyDBMessage(), new Object[] { restart }), "");
  }

  public static TerracottaOperatorEvent createServerMapEvictionOperatorEvent(Object[] arguments) {
    return new TerracottaOperatorEventImpl(EventType.INFO, EventSubsystem.DCV2, MessageFormat
        .format(TerracottaOperatorEventResources.getServerMapEvictionMessage(), arguments), "");
  }
}
