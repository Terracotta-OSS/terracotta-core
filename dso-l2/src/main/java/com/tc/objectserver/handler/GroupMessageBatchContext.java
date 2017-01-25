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
import com.tc.net.NodeID;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;


public class GroupMessageBatchContext {
  private final GroupManager<AbstractGroupMessage> groupManager;
  private final NodeID target;
  private final int maximumBatchSize;
  private final int idealMessagesInFlight;
  private final Runnable networkDoneTarget;
  
  private int messagesInFlight;
  private ReplicationMessage cachedMessage;
  private long nextReplicationID;
  // Note that we may see this exception, asynchronously.  In that case, we will just hold it and fail in the next call.
  private GroupException mostRecentException;


  public GroupMessageBatchContext(GroupManager<AbstractGroupMessage> groupManager, NodeID target, int maximumBatchSize, int idealMessagesInFlight, Runnable networkDoneTarget) {
    this.groupManager = groupManager;
    this.target = target;
    this.maximumBatchSize = maximumBatchSize;
    this.idealMessagesInFlight = idealMessagesInFlight;
    this.networkDoneTarget = networkDoneTarget;
  }

  private final Runnable handleMessageSend = new Runnable() {
    @Override
    public void run() {
      handleNetworkDone();
    }
  };

  /**
   * Called to send a new activity.  This might be added to an existing batch or used to create a new one.  In either
   *  case, this isn't sent now, but might be sent during the next call to flushBatch().
   *  
   * @param activity The activity to batch.
   * @throws GroupException The exception cached from the most recent attempt to send.
   */
  public synchronized void batchMessage(SyncReplicationActivity activity) throws GroupException {
    // Throw any async exception.
    if (null != this.mostRecentException) {
      throw this.mostRecentException;
    }
    
    // See if we have an existing message we must batch.
    if (null != this.cachedMessage) {
      // Just add to this batch.
      this.cachedMessage.addActivity(activity);
    } else {
      // Create a new batch.
      this.cachedMessage = ReplicationMessage.createActivityContainer(activity);
      this.cachedMessage.setReplicationID(this.nextReplicationID++);
    }
  }

  /**
   * Called by a thread which is expected to do the message serialization to determine if the current batch is ready to
   *  be flushed to the network.
   * 
   * @throws GroupException Something went wrong in the transmission (note that this same exception will be thrown on
   *  the next call to batchMessage).
   */
  public void flushBatch() throws GroupException {
    ReplicationMessage messageToSend = null;
    synchronized (this) {
      // See if we have a batched message and are ready to send one.
      // Note that we will override the ideal number of in-flight messages if the batch is getting too large.
      if ((null != this.cachedMessage) && (
          ((0 == this.idealMessagesInFlight) || (this.messagesInFlight < this.idealMessagesInFlight))
          || (this.cachedMessage.getBatchSize() >= this.maximumBatchSize))
        ) {
        // There is a batched message so send it.
        messageToSend = this.cachedMessage;
        this.cachedMessage = null;
        this.messagesInFlight += 1;
      }
    }
    
    // Note that we don't want to make this call to send the message under lock since it results in the message
    //  serialization, which is potentially slow and shouldn't block other attempts to batch.
    if (null != messageToSend) {
      try {
        this.groupManager.sendToWithSentCallback(this.target, messageToSend, this.handleMessageSend);
      } catch (GroupException e) {
        // Set the exception, before throwing it, so the next call also fails.
        synchronized (this) {
          this.mostRecentException = e;
          this.messagesInFlight -= 1;
        }
        throw e;
      }
    }
  }

  public void handleNetworkDone() {
    synchronized (this) {
      this.messagesInFlight -= 1;
    }
    
    // Call the network done target so that our owner can decide how to enqueue the next flush.
    this.networkDoneTarget.run();
  }
}
