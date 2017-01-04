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
import com.tc.util.Assert;


public class GroupMessageBatchContext {
  private static final TCLogger LOGGER = TCLogging.getLogger(GroupMessageBatchContext.class);
  
  private final GroupManager<AbstractGroupMessage> groupManager;
  private final NodeID target;
  private final int maximumBatchSize;
  private final int idealMessagesInFlight;
  private int messagesInFlight;
  private ReplicationMessage cachedMessage;
  private long nextReplicationID;
  // Note that we may see this exception, asynchronously.  In that case, we will just hold it and fail in the next call.
  private GroupException mostRecentException;


  public GroupMessageBatchContext(GroupManager<AbstractGroupMessage> groupManager, NodeID target, int maximumBatchSize, int idealMessagesInFlight) {
    this.groupManager = groupManager;
    this.target = target;
    this.maximumBatchSize = maximumBatchSize;
    this.idealMessagesInFlight = idealMessagesInFlight;
  }

  private final Runnable handleMessageSend = new Runnable() {
    @Override
    public void run() {
      handleNetworkDone();
    }
  };

  public void batchAndSend(SyncReplicationActivity activity) throws GroupException {
    // Determine if batching is even enabled.
    if (0 == this.idealMessagesInFlight) {
      // There is no batching - send directly.
      Assert.assertTrue(null == this.cachedMessage);
      ReplicationMessage message = ReplicationMessage.createActivityContainer(activity);
      this.groupManager.sendTo(this.target, message);
    } else {
      // We are using batching.
      batchAndSendEnabled(activity);
    }
  }

  private synchronized void batchAndSendEnabled(SyncReplicationActivity activity) throws GroupException {
    if (null != this.mostRecentException) {
      throw this.mostRecentException;
    }
    boolean mustFlush = false;
    if (null == this.cachedMessage) {
      this.cachedMessage = ReplicationMessage.createActivityContainer(activity);
      this.cachedMessage.setReplicationID(this.nextReplicationID++);
    } else {
      int currentBatchSize = this.cachedMessage.addActivity(activity);
      mustFlush = (currentBatchSize >= this.maximumBatchSize);
    }
    
    if (mustFlush || (this.messagesInFlight < this.idealMessagesInFlight)) {
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
    this.messagesInFlight -= 1;
    // Note that we might still be over the ideal number of in-flight messages if they are being flushed due to batch
    //  size so check this limit.
    if ((null != this.cachedMessage) && (this.messagesInFlight < this.idealMessagesInFlight)) {
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
    this.messagesInFlight += 1;
    try {
      this.groupManager.sendToWithSentCallback(this.target, cachedBatch, this.handleMessageSend);
    } catch (GroupException e) {
      // We aren't going to be hearing back from this decrement our in-flight counter.
      this.messagesInFlight -= 1;
      throw e;
    }
  }
}
