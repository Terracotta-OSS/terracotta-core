/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.operatorevent;

import java.util.ResourceBundle;

class TerracottaOperatorEventResources {
  private static final TerracottaOperatorEventResources instance = new TerracottaOperatorEventResources();
  private final ResourceBundle                          resources;

  private TerracottaOperatorEventResources() {
    this.resources = ResourceBundle.getBundle(getClass().getPackage().getName() + ".messages");
  }

  /**
   * Memory Manager messages
   */
  static String getLongGCMessage() {
    return instance.resources.getString("long.gc");
  }

  static String getLongGCAndOffheapRecommendationMessage() {
    return instance.resources.getString("long.gc.no.offheap");
  }

  static String getHighMemoryUsageMessage() {
    return instance.resources.getString("high.memory.usage");
  }

  static String getOffHeapMemoryUsageMessage() {
    return instance.resources.getString("offheap.memory.usage");
  }

  static String getOffHeapMemoryEvictionMessage() {
    return instance.resources.getString("offheap.memory.eviction");
  }

  static String getOffHeapObjectCachedMessage() {
    return instance.resources.getString("offheap.memory.objectCached");
  }

  /**
   * DGC messages
   */
  static String getDGCStartedMessage() {
    return instance.resources.getString("dgc.started");
  }

  static String getDGCFinishedMessage() {
    return instance.resources.getString("dgc.finished");
  }

  static String getDGCCanceledMessage() {
    return instance.resources.getString("dgc.canceled");
  }

  /**
   * HA Messages
   */
  static String getNodeAvailabiltyMessage() {
    return instance.resources.getString("node.availability");
  }

  static String getOOODisconnectMessage() {
    return instance.resources.getString("ooo.disconnect");
  }

  static String getOOOConnectMessage() {
    return instance.resources.getString("ooo.connect");
  }

  static String getClusterNodeStateChangedMessage() {
    return instance.resources.getString("node.state");
  }

  static String getHandshakeRejectedMessage() {
    return instance.resources.getString("handshake.reject");
  }

  /**
   * Zap Messagse
   */
  static String getZapRequestReceivedMessage() {
    return instance.resources.getString("zap.received");
  }

  static String getZapRequestAcceptedMessage() {
    return instance.resources.getString("zap.accepted");
  }

  static String getDirtyDBMessage() {
    return instance.resources.getString("dirty.db");
  }

  /**
   * Servermap Message
   */
  static String getServerMapEvictionMessage() {
    return instance.resources.getString("servermap.eviction");
  }

  /**
   * Cluster configuration events
   */
  static String getTimeDifferentMessage() {
    return instance.resources.getString("time.different");
  }

  static String getConfigReloadedMessage() {
    return instance.resources.getString("config.reloaded");
  }

  /**
   * Cluster state events
   */
  static String getActiveStartedWithOldDBMessage() {
    return instance.resources.getString("db.state");
  }
}
