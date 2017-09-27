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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.l2.msg.IBatchableGroupMessage;
import com.tc.net.NodeID;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import java.util.function.Consumer;
import java.util.function.Function;


public class GroupMessageBatchContext<M extends IBatchableGroupMessage<E>, E> {
  private static final Logger LOGGER = LoggerFactory.getLogger(GroupMessageBatchContext.class);
  
  private final Function<E, M> messageFactory;
  private final GroupManager<AbstractGroupMessage> groupManager;
  private final NodeID target;
  private final int maximumBatchSize;
  private final int idealMessagesInFlight;
  private final Consumer<NodeID> networkDoneTarget;
  
  private int messagesInFlight;
  private M cachedMessage;
  private long nextReplicationID;


  public GroupMessageBatchContext(Function<E, M> messageFactory, GroupManager<AbstractGroupMessage> groupManager, NodeID target, int maximumBatchSize, int idealMessagesInFlight, Consumer<NodeID> networkDoneTarget) {
    this.messageFactory = messageFactory;
    this.groupManager = groupManager;
    this.target = target;
    this.maximumBatchSize = maximumBatchSize;
    this.idealMessagesInFlight = idealMessagesInFlight;
    this.networkDoneTarget = networkDoneTarget;
  }

  /**
   * Called to send a new activity.  This might be added to an existing batch or used to create a new one.  In either
   *  case, this isn't sent now, but might be sent during the next call to flushBatch().
   *  
   * @param activity The activity to batch.
   * @return True if this required creating a new batch (the message is batched, either way).
   */
  public synchronized boolean batchMessage(E activity) {
    
    // See if we have an existing message we must batch.
    boolean didCreateNewBatch = false;
    if (null != this.cachedMessage) {
      // Just add to this batch.
      this.cachedMessage.addToBatch(activity);
    } else {
      // Create a new batch.
      this.cachedMessage = this.messageFactory.apply(activity);
      this.cachedMessage.setSequenceID(nextReplicationID++);
      didCreateNewBatch = true;
    }
    return didCreateNewBatch;
  }

  /**
   * Called by a thread which is expected to do the message serialization to determine if the current batch is ready to
   *  be flushed to the network.
   * 
   * @throws GroupException Something went wrong in the transmission (note that this same exception will be thrown on
   *  the next call to batchMessage).
   */
  public long flushBatch() throws GroupException {
    IBatchableGroupMessage<E> messageToSend = null;
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
        AbstractGroupMessage msg = messageToSend.asAbstractGroupMessage();
        this.groupManager.sendToWithSentCallback(this.target, msg, this::handleNetworkDone);
        return msg.getMessageID().toLong();
      } catch (GroupException e) {
        LOGGER.warn("replication message failed", e);
        //  message failed but we still need to reset state
        this.handleNetworkDone();
        throw e;
      }
    }
    return -1L;
  }

  public void handleNetworkDone() {
    synchronized (this) {
      this.messagesInFlight -= 1;
    }
    
    // Call the network done target so that our owner can decide how to enqueue the next flush.
    if (this.networkDoneTarget != null) {
      this.networkDoneTarget.accept(target);
    }
  }
}
