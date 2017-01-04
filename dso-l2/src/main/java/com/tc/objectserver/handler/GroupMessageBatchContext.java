/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.objectserver.handler;

import com.tc.l2.msg.ReplicationMessage;
import com.tc.l2.msg.SyncReplicationActivity;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;


public class GroupMessageBatchContext {
  private static final TCLogger LOGGER = TCLogging.getLogger(GroupMessageBatchContext.class);
  
  private final GroupManager<AbstractGroupMessage> groupManager;
  private final NodeID target;
  private boolean isWaitingForNetwork;
  private ReplicationMessage cachedMessage;
  private long nextReplicationID;
  // Note that we may see this exception, asynchronously.  In that case, we will just hold it and fail in the next call.
  private GroupException mostRecentException;


  public GroupMessageBatchContext(GroupManager<AbstractGroupMessage> groupManager, NodeID target) {
    this.groupManager = groupManager;
    this.target = target;
  }

  private final Runnable handleMessageSend = new Runnable() {
    @Override
    public void run() {
      handleNetworkDone();
    }
  };

  public synchronized void batchAndSend(SyncReplicationActivity activity) throws GroupException {
    if (null != this.mostRecentException) {
      throw this.mostRecentException;
    }
    if (null == this.cachedMessage) {
      this.cachedMessage = ReplicationMessage.createActivityContainer(activity);
      this.cachedMessage.setReplicationID(this.nextReplicationID++);
    } else {
      this.cachedMessage.addActivity(activity);
    }
    
    if (!isWaitingForNetwork) {
      try {
        synchronizedSendBatch();
      } catch (GroupException e) {
        // Set the exception, before throwing it, so the next call also fails.
        this.mostRecentException = e;
        throw e;
      }
    }
  }

  public synchronized void handleNetworkDone() {
    this.isWaitingForNetwork = false;
    if (null != this.cachedMessage) {
      try {
        synchronizedSendBatch();
      } catch (GroupException e) {
        // This happened asynchronously so we can't throw back to the caller (it thinks we already send this).
        // Log that this happened and set our exception state so that this batch context will be assumed invalid.
        LOGGER.warn("Asynchronous group exception in batched replication message", e);
        this.mostRecentException = e;
      }
    }
  }

  private void synchronizedSendBatch() throws GroupException {
    ReplicationMessage cachedBatch = this.cachedMessage;
    this.cachedMessage = null;
    this.isWaitingForNetwork = true;
    try {
      this.groupManager.sendToWithSentCallback(this.target, cachedBatch, this.handleMessageSend);
    } catch (GroupException e) {
      // We aren't going to be hearing back from this unset our waiting state.
      this.isWaitingForNetwork = false;
      throw e;
    }
  }
}
