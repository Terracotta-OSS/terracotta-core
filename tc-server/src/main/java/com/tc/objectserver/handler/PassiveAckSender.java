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

import com.tc.async.api.Sink;
import com.tc.l2.msg.ReplicationAckTuple;
import com.tc.l2.msg.ReplicationMessageAck;
import com.tc.l2.msg.ReplicationResultCode;
import com.tc.l2.msg.SyncReplicationActivity;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.net.groups.GroupMessage;
import com.tc.net.utils.L2Utils;
import com.tc.properties.TCPropertiesImpl;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class PassiveAckSender {

  private static final Logger LOGGER = LoggerFactory.getLogger(RelayTransactionHandler.class);

  private static final int DEFAULT_BATCH_LIMIT = 64;
  private static final int DEFAULT_INFLIGHT_MESSAGES = 1;
  private static final int MAXIMUMBATCHSIZE = TCPropertiesImpl.getProperties().getInt("passive-active.batchsize", DEFAULT_BATCH_LIMIT);
  private static final int IDEALMESSAGESINFLIGHT = TCPropertiesImpl.getProperties().getInt("passive-active.inflight", DEFAULT_INFLIGHT_MESSAGES);

  private final GroupManager<AbstractGroupMessage> groupManager;
  private final Predicate<GroupMessage> sendConfirm;
  private final ServerID local;

  // This MUST be manipulated under lock - it is the batch of ack messages we are accumulating until the network is ready for another message.
  private ServerID cachedMessageAckFrom;
  private GroupMessageBatchContext<ReplicationMessageAck, ReplicationAckTuple> cachedBatchAck;
  private final Sink<Runnable> sentToActive;

  public PassiveAckSender(GroupManager<AbstractGroupMessage> groupManager, Predicate<GroupMessage> msgCheck, Sink<Runnable> sentToActive) {
    this.groupManager = groupManager;
    this.sendConfirm = msgCheck;
    this.sentToActive = sentToActive;
    this.local = groupManager.getLocalNodeID();
  }

  public void acknowledge(ServerID activeSender, SyncReplicationActivity activity, ReplicationResultCode code) {
//  when is the right time to send the ack?
    if (!activeSender.equals(ServerID.NULL_ID)) {
      LOGGER.debug("{} acking {} as {}", activity.getTransactionID(), activity.getActivityID().id, code);
      prepareAckForSend(activeSender, activity.getActivityID(), code);
    }
  }

  public void ackReceived(ServerID activeSender, SyncReplicationActivity activity, Future<Void> future) {
    if (!activeSender.equals(ServerID.NULL_ID)) {
      if (future != null) {
        try {
          future.get();
        } catch (InterruptedException ie) {
          L2Utils.handleInterrupted(LOGGER, ie);
        } catch (ExecutionException e) {
          throw new RuntimeException("Caught exception while persisting transaction order", e);
        }
      }
      prepareAckForSend(activeSender, activity.getActivityID(), ReplicationResultCode.RECEIVED);
    }
  }

  private ReplicationMessageAck createAckMessage(ReplicationAckTuple initialActivity) {
    ReplicationMessageAck message = ReplicationMessageAck.createBatchAck();
    message.addToBatch(initialActivity);
    return message;
  }

  private synchronized void prepareAckForSend(ServerID sender, SyncReplicationActivity.ActivityID respondTo, ReplicationResultCode code) {
    // The batch context is cached and constructed lazily when the sender changes.
    if (!sender.equals(this.cachedMessageAckFrom)) {
      this.cachedMessageAckFrom = sender;
      this.cachedBatchAck = new GroupMessageBatchContext<>(this::createAckMessage, this.groupManager, this.cachedMessageAckFrom, MAXIMUMBATCHSIZE, IDEALMESSAGESINFLIGHT, (node) -> sendToActive());
    }

    boolean didCreate = this.cachedBatchAck.batchMessage(new ReplicationAckTuple(respondTo, code));

    // If we created this message, enqueue the decision to flush it (the other case where we may flush is network
    //  available).
    if (didCreate) {
      sendToActive();
    }
  }

  private void sendToActive() {
    // If we created this message, enqueue the decision to flush it (the other case where we may flush is network
    //  available).
    this.sentToActive.addToSink(() -> {
      int batchCount = 0;
      try {
        while (this.cachedBatchAck.sendBatch(sendConfirm)) {
          batchCount++;
        }
      } catch (GroupException group) {
        //  ignore, active is gone
      }
    });
  }
  
  public ServerID getLocalNodeID() {
    return local;
  }

  public void requestPassiveSync(NodeID target) {
    try {
      groupManager.sendTo(target, ReplicationMessageAck.createSyncRequestMessage());
    } catch (GroupException ge) {
      LOGGER.warn("can't request passive sync", ge);
    }
  }
}
