/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2026
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.objectserver.handler;

import com.tc.async.api.Sink;
import com.tc.l2.msg.ReplicationAckTuple;
import com.tc.l2.msg.ReplicationMessageAck;
import com.tc.l2.msg.ReplicationResultCode;
import com.tc.l2.msg.SyncReplicationActivity;
import static com.tc.l2.msg.SyncReplicationActivity.ActivityType.CREATE_ENTITY;
import static com.tc.l2.msg.SyncReplicationActivity.ActivityType.DESTROY_ENTITY;
import static com.tc.l2.msg.SyncReplicationActivity.ActivityType.DISCONNECT_CLIENT;
import static com.tc.l2.msg.SyncReplicationActivity.ActivityType.FETCH_ENTITY;
import static com.tc.l2.msg.SyncReplicationActivity.ActivityType.FLUSH_LOCAL_PIPELINE;
import static com.tc.l2.msg.SyncReplicationActivity.ActivityType.INVOKE_ACTION;
import static com.tc.l2.msg.SyncReplicationActivity.ActivityType.LOCAL_ENTITY_GC;
import static com.tc.l2.msg.SyncReplicationActivity.ActivityType.ORDERING_PLACEHOLDER;
import static com.tc.l2.msg.SyncReplicationActivity.ActivityType.RECONFIGURE_ENTITY;
import static com.tc.l2.msg.SyncReplicationActivity.ActivityType.RELEASE_ENTITY;
import static com.tc.l2.msg.SyncReplicationActivity.ActivityType.SYNC_BEGIN;
import static com.tc.l2.msg.SyncReplicationActivity.ActivityType.SYNC_END;
import static com.tc.l2.msg.SyncReplicationActivity.ActivityType.SYNC_ENTITY_BEGIN;
import static com.tc.l2.msg.SyncReplicationActivity.ActivityType.SYNC_ENTITY_CONCURRENCY_BEGIN;
import static com.tc.l2.msg.SyncReplicationActivity.ActivityType.SYNC_ENTITY_CONCURRENCY_END;
import static com.tc.l2.msg.SyncReplicationActivity.ActivityType.SYNC_ENTITY_CONCURRENCY_PAYLOAD;
import static com.tc.l2.msg.SyncReplicationActivity.ActivityType.SYNC_ENTITY_END;
import static com.tc.l2.msg.SyncReplicationActivity.ActivityType.SYNC_START;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.net.groups.GroupMessage;
import com.tc.net.utils.L2Utils;
import com.tc.object.ClientInstanceID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ServerEntityAction;
import com.tc.objectserver.api.ServerEntityRequest;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class PassiveAckSender implements PassiveMessageResultCollector {

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

  @Override
  public void acknowledge(ServerID activeSender, SyncReplicationActivity activity, ReplicationResultCode code) {
//  when is the right time to send the ack?
    if (!activeSender.equals(ServerID.NULL_ID)) {
      LOGGER.debug("{} acking {} as {}", activity.getTransactionID(), activity.getActivityID().id, code);
      prepareAckForSend(activeSender, activity.getActivityID(), code);
    }
  }

  @Override
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

  @Override
  public ServerID getLocalNodeID() {
    return local;
  }

  @Override
  public void requestPassiveSync(NodeID target) {
    try {
      groupManager.sendTo(target, ReplicationMessageAck.createSyncRequestMessage());
    } catch (GroupException ge) {
      LOGGER.warn("can't request passive sync", ge);
    }
  }

  @Override
  public ServerEntityRequest transform(SyncReplicationActivity activity) {
    SyncReplicationActivity.ActivityType activityType = activity.getActivityType();
    ClientID source = activity.getSource();
    ClientInstanceID instance = activity.getClientInstanceID();
    TransactionID transactionID = activity.getTransactionID();
    TransactionID oldestTransactionID = activity.getOldestTransactionOnClient();
    Assert.assertTrue(SyncReplicationActivity.ActivityType.SYNC_BEGIN != activityType);
    return new ReplicatedTransactionHandler.BasicServerEntityRequest(decodeReplicationType(activityType), source, instance, transactionID, oldestTransactionID);
  }


  public static ServerEntityAction decodeReplicationType(SyncReplicationActivity.ActivityType networkType) {
    switch(networkType) {
      case SYNC_BEGIN:
        throw Assert.failure("Shouldn't decode this type into an internal action");
      case SYNC_START:
      case SYNC_END:
      case ORDERING_PLACEHOLDER:
        return ServerEntityAction.ORDER_PLACEHOLDER_ONLY;
      case LOCAL_ENTITY_GC:
        return ServerEntityAction.MANAGED_ENTITY_GC;
      case FLUSH_LOCAL_PIPELINE:
        // Note that these are never replicated from the active but we do synthesize them, internally, in some cases.
        return ServerEntityAction.LOCAL_FLUSH;
      case CREATE_ENTITY:
        return ServerEntityAction.CREATE_ENTITY;
      case RECONFIGURE_ENTITY:
        return ServerEntityAction.RECONFIGURE_ENTITY;
      case INVOKE_ACTION:
        return ServerEntityAction.INVOKE_ACTION;
      case DESTROY_ENTITY:
        return ServerEntityAction.DESTROY_ENTITY;
      case FETCH_ENTITY:
        return ServerEntityAction.FETCH_ENTITY;
      case RELEASE_ENTITY:
        return ServerEntityAction.RELEASE_ENTITY;
      case SYNC_ENTITY_BEGIN:
        return ServerEntityAction.RECEIVE_SYNC_ENTITY_START_SYNCING;
      case SYNC_ENTITY_CONCURRENCY_BEGIN:
        return ServerEntityAction.RECEIVE_SYNC_ENTITY_KEY_START;
      case SYNC_ENTITY_CONCURRENCY_PAYLOAD:
        return ServerEntityAction.RECEIVE_SYNC_PAYLOAD;
      case SYNC_ENTITY_CONCURRENCY_END:
        return ServerEntityAction.RECEIVE_SYNC_ENTITY_KEY_END;
      case SYNC_ENTITY_END:
        return ServerEntityAction.RECEIVE_SYNC_ENTITY_END;
      case DISCONNECT_CLIENT:
        return ServerEntityAction.DISCONNECT_CLIENT;
      default:
        throw new AssertionError("bad replication type: " + networkType);
    }
  }
}
