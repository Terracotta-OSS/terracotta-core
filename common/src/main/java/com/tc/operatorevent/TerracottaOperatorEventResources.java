/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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

  static String getInlineDGCReferenceCleanupStartedMessage() {
    return instance.resources.getString("inlineDgc.cleanup.started");
  }

  static String getInlineDGCReferenceCleanupFinishedMessage() {
    return instance.resources.getString("inlineDgc.cleanup.finished");
  }

  static String getInlineDGCReferenceCleanupCanceledMessage() {
    return instance.resources.getString("inlineDgc.cleanup.canceled");
  }

  /**
   * HA Messages
   */
  static String getNodeAvailabiltyMessage() {
    return instance.resources.getString("node.availability");
  }


  static String getClusterNodeStateChangedMessage() {
    return instance.resources.getString("node.state");
  }

  static String getHandshakeRejectedMessage() {
    return instance.resources.getString("handshake.reject");
  }

  static String getActiveServerDisconnectMessage() {
    return instance.resources.getString("active.server.disconnect");
  }

  static String getMirrorServerDisconnectMessage() {
    return instance.resources.getString("mirror.server.disconnect");
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
   * resource management
   */
  static String getNearResourceCapacityLimit() {
    return instance.resources.getString("resource.nearcapacity");
  }
  static String getFullResourceCapacityLimit() {
    return instance.resources.getString("resource.fullcapacity");
  }
    static String getRestoredNormalResourceCapacity() {
    return instance.resources.getString("resource.capacityrestored");
  }
}
