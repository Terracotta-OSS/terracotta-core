/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.operatorevent;

import com.tc.net.NodeID;
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

  public static TerracottaOperatorEvent createLongGCAndRecommendationOperatorEvent(Object[] arguments) {
    return new TerracottaOperatorEventImpl(EventType.WARN, EventSubsystem.MEMORY_MANAGER, MessageFormat
        .format(TerracottaOperatorEventResources.getLongGCAndOffheapRecommendationMessage(), arguments), "");
  }

  public static TerracottaOperatorEvent createHighMemoryUsageEvent(int memoryUsage, int critcalThreshold) {
    return new TerracottaOperatorEventImpl(EventType.WARN, EventSubsystem.MEMORY_MANAGER, MessageFormat
        .format(TerracottaOperatorEventResources.getHighMemoryUsageMessage(), new Object[] { memoryUsage,
            critcalThreshold }), "");
  }

  public static TerracottaOperatorEvent createOffHeapMemoryUsageEvent(String allocated, String maxSize,
                                                                      int percentageUsed) {
    return new TerracottaOperatorEventImpl(EventType.INFO, EventSubsystem.MEMORY_MANAGER, MessageFormat
        .format(TerracottaOperatorEventResources.getOffHeapMemoryUsageMessage(), new Object[] { allocated, maxSize,
            percentageUsed }), "");
  }

  public static TerracottaOperatorEvent createOffHeapMemoryEvictionEvent() {
    return new TerracottaOperatorEventImpl(EventType.WARN, EventSubsystem.MEMORY_MANAGER,
                                           TerracottaOperatorEventResources.getOffHeapMemoryEvictionMessage(), "");
  }

  public static TerracottaOperatorEvent createOffHeapObjectCachedEvent(int objectCachedPercentage) {
    return new TerracottaOperatorEventImpl(EventType.INFO, EventSubsystem.MEMORY_MANAGER, MessageFormat
        .format(TerracottaOperatorEventResources.getOffHeapObjectCachedMessage(),
                new Object[] { objectCachedPercentage }), "");
  }

  /**
   * DGC events
   */
  public static TerracottaOperatorEvent createDGCStartedEvent(int gcIteration) {
    return new TerracottaOperatorEventImpl(EventType.INFO, EventSubsystem.DGC, MessageFormat
        .format(TerracottaOperatorEventResources.getDGCStartedMessage(), new Object[] { gcIteration }), "dgc started");
  }

  public static TerracottaOperatorEvent createDGCFinishedEvent(int gcIteration, long beginObjectCount, long collected,
                                                               long elapsedTime, long endObjectCount) {
    return new TerracottaOperatorEventImpl(EventType.INFO, EventSubsystem.DGC, MessageFormat
        .format(TerracottaOperatorEventResources.getDGCFinishedMessage(), new Object[] { gcIteration, beginObjectCount,
            collected, elapsedTime, endObjectCount }), "dgc finished");
  }

  public static TerracottaOperatorEvent createDGCCanceledEvent(int gcIteration) {
    return new TerracottaOperatorEventImpl(EventType.INFO, EventSubsystem.DGC, MessageFormat
        .format(TerracottaOperatorEventResources.getDGCCanceledMessage(), new Object[] { gcIteration }), "dgc canceled");
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

  public static TerracottaOperatorEvent createClusterNodeStateChangedEvent(String newState) {
    return new TerracottaOperatorEventImpl(EventType.INFO, EventSubsystem.CLUSTER_TOPOLOGY, MessageFormat
        .format(TerracottaOperatorEventResources.getClusterNodeStateChangedMessage(), new Object[] { newState }), "");
  }

  public static TerracottaOperatorEvent createHandShakeRejectedEvent(String clientVersion, NodeID remoteNodeID,
                                                                     String serverVersion) {
    return new TerracottaOperatorEventImpl(EventType.ERROR, EventSubsystem.CLUSTER_TOPOLOGY, MessageFormat
        .format(TerracottaOperatorEventResources.getHandshakeRejectedMessage(), new Object[] { clientVersion,
            remoteNodeID.toString(), serverVersion }), "handshake rejected");
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

  public static TerracottaOperatorEvent createSystemTimeDifferentEvent(NodeID remoteNodeID, String desp,
                                                                       String serverName, long timeDiff) {
    return new TerracottaOperatorEventImpl(EventType.WARN, EventSubsystem.SYSTEM_SETUP, MessageFormat
        .format(TerracottaOperatorEventResources.getTimeDifferentMessage(), new Object[] { remoteNodeID, desp,
            serverName, timeDiff }), "time difference");
  }

  public static TerracottaOperatorEvent createConfigReloadedEvent(String description) {
    return new TerracottaOperatorEventImpl(EventType.INFO, EventSubsystem.CLUSTER_TOPOLOGY, MessageFormat
        .format(TerracottaOperatorEventResources.getConfigReloadedMessage(), new Object[] { description }),
                                           "config reload");
  }

}
