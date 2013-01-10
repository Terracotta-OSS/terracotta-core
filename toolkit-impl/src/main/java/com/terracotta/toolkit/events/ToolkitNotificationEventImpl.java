/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.events;

import org.terracotta.toolkit.cluster.ClusterNode;
import org.terracotta.toolkit.events.ToolkitNotificationEvent;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.terracotta.toolkit.object.serialization.SerializationStrategy;

public class ToolkitNotificationEventImpl<T> implements ToolkitNotificationEvent<T> {
  private static final TCLogger       LOGGER = TCLogging.getLogger(ToolkitNotificationEventImpl.class);
  private final SerializationStrategy strategy;
  private final String                remoteNodeSerializedForm;
  private final String                msgSerializedForm;
  private volatile ClusterNode        remoteNode;
  private volatile T                  msg;

  public ToolkitNotificationEventImpl(SerializationStrategy strategy, String remoteNode, String msg) {
    this.strategy = strategy;
    this.remoteNodeSerializedForm = remoteNode;
    this.msgSerializedForm = msg;
  }

  @Override
  public T getMessage() {
    if (msg == null) {
      try {
        msg = (T) strategy.deserializeFromString(msgSerializedForm, false);
      } catch (Exception e) {
        LOGGER.warn("Ignoring toolkit notifier notification. Failed to deserialize notification msg - "
                    + msgSerializedForm, e);
      }
    }
    return msg;
  }

  @Override
  public ClusterNode getRemoteNode() {
    if (remoteNode == null) {
      try {
        remoteNode = (ClusterNode) strategy.deserializeFromString(remoteNodeSerializedForm, false);
      } catch (Exception e) {
        LOGGER.warn("Ignoring toolkit notifier notification. Failed to deserialize notification remote node - "
                    + remoteNodeSerializedForm, e);
      }
    }
    return remoteNode;
  }

  @Override
  public String toString() {
    return "ToolkitNotificationEventImpl [remoteNode=" + getRemoteNode() + ", msg=" + getMessage() + "]";
  }

}
