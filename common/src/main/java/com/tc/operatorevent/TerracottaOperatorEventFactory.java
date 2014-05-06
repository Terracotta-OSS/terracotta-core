/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.operatorevent;

import com.tc.net.NodeID;
import com.tc.operatorevent.TerracottaOperatorEvent.EventLevel;
import com.tc.operatorevent.TerracottaOperatorEvent.EventSubsystem;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;

import java.text.MessageFormat;

public class TerracottaOperatorEventFactory {

  /**
   * Memory Manager Events
   */
  public static TerracottaOperatorEvent createLongGCOperatorEvent(Object[] arguments) {
    return new TerracottaOperatorEventImpl(EventLevel.WARN, EventSubsystem.MEMORY_MANAGER,
                                           MessageFormat.format(TerracottaOperatorEventResources.getLongGCMessage(),
                                                                arguments), "");
  }

  /**
   * DGC events
   */
  public static TerracottaOperatorEvent createDGCStartedEvent(int gcIteration) {
    return new TerracottaOperatorEventImpl(
                                           EventLevel.INFO,
                                           EventSubsystem.DGC,
                                           MessageFormat.format(TerracottaOperatorEventResources.getDGCStartedMessage(),
                                                                new Object[] { gcIteration }), "dgc started");
  }

  public static TerracottaOperatorEvent createDGCFinishedEvent(int gcIteration, long beginObjectCount, long collected,
                                                               long elapsedTime, long endObjectCount) {
    return new TerracottaOperatorEventImpl(EventLevel.INFO, EventSubsystem.DGC,
                                           MessageFormat.format(TerracottaOperatorEventResources
                                               .getDGCFinishedMessage(), new Object[] { gcIteration, beginObjectCount,
                                               collected, elapsedTime, endObjectCount }), "dgc finished");
  }

  public static TerracottaOperatorEvent createDGCCanceledEvent(int gcIteration) {
    return new TerracottaOperatorEventImpl(EventLevel.INFO, EventSubsystem.DGC,
                                           MessageFormat.format(TerracottaOperatorEventResources
                                               .getDGCCanceledMessage(), new Object[] { gcIteration }), "dgc canceled");
  }

  public static TerracottaOperatorEvent createInlineDGCCleanupStartedEvent(int gcIteration) {
    return new TerracottaOperatorEventImpl(EventLevel.INFO, EventSubsystem.DGC,
                                           MessageFormat.format(TerracottaOperatorEventResources
                                               .getInlineDGCReferenceCleanupStartedMessage(), gcIteration),
                                           "cleanup started.");
  }

  public static TerracottaOperatorEvent createInlineDGCCleanupFinishedEvent(int gcIteration, long beginObjectCount,
                                                                            long collected, long elapsedTime,
                                                                            long endObjectCount) {
    return new TerracottaOperatorEventImpl(EventLevel.INFO, EventSubsystem.DGC,
                                           MessageFormat.format(TerracottaOperatorEventResources
                                                                    .getInlineDGCReferenceCleanupFinishedMessage(),
                                                                gcIteration,
                                                                beginObjectCount, collected, elapsedTime,
                                                                endObjectCount), "cleanup finished.");
  }

  public static TerracottaOperatorEvent createInlineDGCCleanupCanceledEvent(int gcIteration) {
    return new TerracottaOperatorEventImpl(EventLevel.INFO, EventSubsystem.DGC,
                                           MessageFormat.format(TerracottaOperatorEventResources
                                               .getInlineDGCReferenceCleanupCanceledMessage(), gcIteration),
                                           "cleanup canceled.");
  }

  /**
   * High availability events
   */
  public static TerracottaOperatorEvent createNodeConnectedEvent(String nodeName) {
    return new TerracottaOperatorEventImpl(EventLevel.INFO, EventSubsystem.CLUSTER_TOPOLOGY,
                                           MessageFormat.format(TerracottaOperatorEventResources
                                               .getNodeAvailabiltyMessage(), new Object[] { nodeName, "joined" }),
                                           nodeName + "joined");
  }

  public static TerracottaOperatorEvent createNodeDisconnectedEvent(String nodeName) {
    return new TerracottaOperatorEventImpl(EventLevel.WARN, EventSubsystem.CLUSTER_TOPOLOGY,
                                           MessageFormat.format(TerracottaOperatorEventResources
                                               .getNodeAvailabiltyMessage(), new Object[] { nodeName, "left" }),
                                           nodeName + "left");
  }

  public static TerracottaOperatorEvent createClusterNodeStateChangedEvent(String newState) {
    return new TerracottaOperatorEventImpl(EventLevel.INFO, EventSubsystem.CLUSTER_TOPOLOGY,
                                           MessageFormat.format(TerracottaOperatorEventResources
                                               .getClusterNodeStateChangedMessage(), new Object[] { newState }), "");
  }

  public static TerracottaOperatorEvent createHandShakeRejectedEvent(String clientVersion, NodeID remoteNodeID,
                                                                     String serverVersion) {
    return new TerracottaOperatorEventImpl(EventLevel.ERROR, EventSubsystem.CLUSTER_TOPOLOGY,
                                           MessageFormat.format(TerracottaOperatorEventResources
                                               .getHandshakeRejectedMessage(), new Object[] { clientVersion,
                                               remoteNodeID.toString(), serverVersion }), "handshake rejected");
  }

  public static TerracottaOperatorEvent createActiveL2DisconnectedEvent(String serverName) {
    return new TerracottaOperatorEventImpl(EventLevel.WARN, EventSubsystem.CLUSTER_TOPOLOGY,
                                           MessageFormat.format(TerracottaOperatorEventResources
                                               .getActiveServerDisconnectMessage(), new Object[] { serverName }), "");
  }

  public static TerracottaOperatorEvent createPassiveL2DisconnectedEvent(String serverName) {
    return new TerracottaOperatorEventImpl(EventLevel.WARN, EventSubsystem.CLUSTER_TOPOLOGY,
                                           MessageFormat.format(TerracottaOperatorEventResources
                                               .getMirrorServerDisconnectMessage(), new Object[] { serverName }), "");
  }

  /**
   * zap events
   */
  public static TerracottaOperatorEvent createZapRequestReceivedEvent(Object[] arguments) {
    return new TerracottaOperatorEventImpl(EventLevel.CRITICAL, EventSubsystem.CLUSTER_TOPOLOGY,
                                           MessageFormat.format(TerracottaOperatorEventResources
                                               .getZapRequestReceivedMessage(), arguments), "");
  }

  public static TerracottaOperatorEvent createZapRequestAcceptedEvent(Object[] arguments) {
    return new TerracottaOperatorEventImpl(EventLevel.CRITICAL, EventSubsystem.CLUSTER_TOPOLOGY,
                                           MessageFormat.format(TerracottaOperatorEventResources
                                               .getZapRequestAcceptedMessage(), arguments), "");
  }

  public static TerracottaOperatorEvent createDirtyDBEvent() {
    String restart;
    EventLevel type;
    if (TCPropertiesImpl.getProperties().getBoolean(TCPropertiesConsts.L2_NHA_DIRTYDB_AUTODELETE)) {
      type = EventLevel.INFO;
      restart = "enabled";
    } else {
      type = EventLevel.ERROR;
      restart = "disabled";
    }
    return new TerracottaOperatorEventImpl(type, EventSubsystem.CLUSTER_TOPOLOGY,
                                           MessageFormat.format(TerracottaOperatorEventResources.getDirtyDBMessage(),
                                                                new Object[] { restart }), "");
  }

  public static TerracottaOperatorEvent createServerMapEvictionOperatorEvent(Object[] arguments) {
    return new TerracottaOperatorEventImpl(EventLevel.INFO, EventSubsystem.DCV2,
                                           MessageFormat.format(TerracottaOperatorEventResources
                                               .getServerMapEvictionMessage(), arguments), "");
  }

  public static TerracottaOperatorEvent createSystemTimeDifferentEvent(NodeID remoteNodeID, String desp,
                                                                       String serverName, long timeDiff) {
    return new TerracottaOperatorEventImpl(EventLevel.WARN, EventSubsystem.SYSTEM_SETUP,
                                           MessageFormat.format(TerracottaOperatorEventResources
                                               .getTimeDifferentMessage(), new Object[] { remoteNodeID, desp,
                                               serverName, timeDiff }), "time difference");
  }

  public static TerracottaOperatorEvent createConfigReloadedEvent(String description) {
    return new TerracottaOperatorEventImpl(EventLevel.INFO, EventSubsystem.CLUSTER_TOPOLOGY,
                                           MessageFormat.format(TerracottaOperatorEventResources
                                               .getConfigReloadedMessage(), new Object[] { description }),
                                           "config reload");
  }

  /**
   * Resource Management
   */
  public static TerracottaOperatorEvent createNearResourceCapacityEvent(String rsrcName, long percent) {
    return new TerracottaOperatorEventImpl(EventLevel.WARN, EventSubsystem.RESOURCE,
                                           MessageFormat.format(TerracottaOperatorEventResources
                                               .getNearResourceCapacityLimit(), new Object[] { rsrcName,percent }),
                                           "near capacity");
  }
  
  public static TerracottaOperatorEvent createFullResourceCapacityEvent(String rsrcName, long percent){
    return new TerracottaOperatorEventImpl(EventLevel.ERROR, EventSubsystem.RESOURCE,
                                           MessageFormat.format(TerracottaOperatorEventResources
                                               .getFullResourceCapacityLimit(), new Object[] { rsrcName,percent }),
                                           "full capacity");
  }
  
    public static TerracottaOperatorEvent createNormalResourceCapacityEvent(String rsrcName, long percent) {
    return new TerracottaOperatorEventImpl(EventLevel.INFO, EventSubsystem.RESOURCE,
                                           MessageFormat.format(TerracottaOperatorEventResources
                                               .getRestoredNormalResourceCapacity(), new Object[] { rsrcName,percent }),
                                           "normal capacity restored");
  }
}
