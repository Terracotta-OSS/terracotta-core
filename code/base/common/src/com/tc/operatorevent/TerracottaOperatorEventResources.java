/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.operatorevent;

import java.util.ResourceBundle;

class TerracottaOperatorEventResources {
  private static final TerracottaOperatorEventResources instance = new TerracottaOperatorEventResources();
  private final ResourceBundle resources;

  private TerracottaOperatorEventResources() {
    this.resources = ResourceBundle.getBundle(getClass().getPackage().getName() + ".messages");
  }
  
  static String getLongGCMessage() {
    return instance.resources.getString("long.gc");
  }
  
  static String getDGCStartedMessage() {
    return instance.resources.getString("dgc.started");
  }

  static String getDGCFinishedMessage() {
    return instance.resources.getString("dgc.finished");
  }

  static String getDGCCanceledMessage() {
    return instance.resources.getString("dgc.canceled");
  }
  
  static String getNodeAvailabiltyMessage() {
    return instance.resources.getString("node.availability");
  }

  static String getLockGCMessage() {
    return instance.resources.getString("lock.gc");
  }

  static String getHighMemoryUsageMessage() {
    return instance.resources.getString("high.memory.usage");
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

  static String getZapRequestReceivedMessage() {
    return instance.resources.getString("zap.received");
  }
  
  static String getZapRequestAcceptedMessage() {
    return instance.resources.getString("zap.accepted");
  }
}

