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
