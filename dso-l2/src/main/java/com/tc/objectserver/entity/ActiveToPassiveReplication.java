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
package com.tc.objectserver.entity;

import com.tc.async.api.Sink;
import com.tc.l2.ha.L2HAZapNodeRequestProcessor;
import com.tc.l2.msg.ReplicationAddPassiveIntent;
import com.tc.l2.msg.ReplicationIntent;
import com.tc.l2.msg.ReplicationMessageAck;
import com.tc.l2.msg.ReplicationRemovePassiveIntent;
import com.tc.l2.msg.ReplicationReplicateMessageIntent;
import com.tc.l2.msg.ReplicationResultCode;
import com.tc.l2.msg.SyncReplicationActivity;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.net.groups.GroupEventsListener;
import com.tc.net.groups.GroupManager;
import com.tc.objectserver.api.ManagedEntity;
import com.tc.objectserver.handler.ProcessTransactionHandler;
import com.tc.objectserver.handler.ReplicationSender;
import com.tc.objectserver.persistence.EntityPersistor;
import com.tc.util.Assert;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;


/**
 *  This class acts to connect {@link ProcessTransactionHandler} to the {@link ReplicationSender}
 *
 *  This class lies idle until activated by setting the current passive nodes.  This should 
 *  occur only when the server is transitioning from passive-standby to active
 */
public class ActiveToPassiveReplication implements PassiveReplicationBroker, GroupEventsListener {
  
  private static final TCLogger logger           = TCLogging.getLogger(PassiveReplicationBroker.class);
  private final Iterable<NodeID> passives;
  private boolean activated = false;
  private final Set<NodeID> passiveNodes = new CopyOnWriteArraySet<>();
  private final Set<NodeID> standByNodes = new HashSet<>();
  private final ConcurrentHashMap<SyncReplicationActivity.ActivityID, ActivePassiveAckWaiter> waiters = new ConcurrentHashMap<>();
  private final ReplicationSender replicationSender;
  private final Executor passiveSyncPool = Executors.newCachedThreadPool();
  private final EntityPersistor persistor;
  private final GroupManager serverCheck;
  private final ProcessTransactionHandler snapshotter;

  public ActiveToPassiveReplication(ProcessTransactionHandler snapshotter, Iterable<NodeID> passives, EntityPersistor persistor, ReplicationSender replicationSender, GroupManager serverMatch) {
    this.replicationSender = replicationSender;
    this.passives = passives;
    this.persistor = persistor;
    this.serverCheck = serverMatch;
    this.snapshotter = snapshotter;
  }

  @Override
  public void zapAndWait(NodeID node) {
    synchronized(this.standByNodes) {
      logger.warn("ZAPPING " + node + " due to inconsistent lifecycle result");
      try {
        if (this.standByNodes.contains(node)) {
          this.serverCheck.zapNode(node,  L2HAZapNodeRequestProcessor.PROGRAM_ERROR, "inconsistent lifecycle");
        }
        while (this.standByNodes.contains(node)) {
          this.standByNodes.wait();
        }
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
      }
    }
  }
  
  @Override
  public void enterActiveState() {
    Assert.assertFalse(activated);
    primePassives();
    activated = true;
  }

/**
 * starts the stream of messages to each passive the server knows about.  This should only happen 
 * when a server enters active state.
 */
  private void primePassives() {
    passives.forEach(i -> {
      if (prime(i)) {
        passiveNodes.add(i);
      }
    });
  }
/**
 * prime the message channel to a node by setting the starting ordering id to zero.
 */
  private boolean prime(NodeID node) {
    if (!passiveNodes.contains(node)) {
      logger.info("Starting message sequence on " + node);
      BarrierCompletion block = new BarrierCompletion();
      this.replicationSender.addPassive(ReplicationAddPassiveIntent.createAddPassiveEnvelope(node, SyncReplicationActivity.createStartMessage(), ()->block.complete(), ()->block.complete()));
      block.waitForCompletion();
      return true;
    } else {
      return false;
    }
  }
  
  public void startPassiveSync(NodeID newNode) {
    Assert.assertTrue(activated);
    if (prime(newNode)) {
      passiveNodes.add(newNode);
    } else {
      Assert.assertTrue("passive node unable to prime and not in the list of passives", passiveNodes.contains(newNode));
    }
    logger.info("Starting sync to " + newNode);
    executePassiveSync(newNode);
  }
  /**
   * Using an executor service here to sync multiple passives at once
   * @param groups
   * @param newNode 
   */
  private void executePassiveSync(final NodeID newNode) {
    passiveSyncPool.execute(new Runnable() {
      @Override
      public void run() {    
        // start passive sync message
        logger.debug("starting sync for " + newNode);
        Iterable<ManagedEntity> e = snapshotter.snapshotEntityList(new Consumer<List<ManagedEntity>>() {
          @Override
          public void accept(List<ManagedEntity> sortedEntities) {
            // We want to create the array of activity data.
            List<SyncReplicationActivity.EntityCreationTuple> tuplesForCreation = new ArrayList<>();
            for (ManagedEntity e : sortedEntities) {
              SyncReplicationActivity.EntityCreationTuple data = e.startSync();
              // null creation data means that the entity, while in the list of entities, is 
              // not to be synced because it has been destroyed or not yet fully created and 
              // initiated
              if (data != null) {
                tuplesForCreation.add(data);
              }              
            }
            replicateActivity(SyncReplicationActivity.
                    createStartSyncMessage(tuplesForCreation.
                            toArray(new SyncReplicationActivity.EntityCreationTuple[tuplesForCreation.size()])), Collections.singleton(newNode)).waitForCompleted();
          }}
        );
        for (ManagedEntity entity : e) {
          logger.debug("starting sync for entity " + newNode + "/" + entity.getID());
          entity.sync(newNode);
          logger.debug("ending sync for entity " + newNode + "/" + entity.getID());
        }
    //  passive sync done message.  causes passive to go into passive standby mode
        logger.debug("ending sync " + newNode);
        replicateActivity(SyncReplicationActivity.createEndSyncMessage(replicateEntityPersistor()), Collections.singleton(newNode)).waitForCompleted();
      }
    });
  }
  
  private byte[] replicateEntityPersistor() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try {
      ObjectOutputStream data = new ObjectOutputStream(out);
      persistor.serialize(data);
      data.close();
      return out.toByteArray();
    } catch (IOException ioe) {
      
    }
    return null;
  }

  public void batchAckReceived(ReplicationMessageAck context) {
    NodeID messageFrom = context.messageFrom();
    for (ReplicationMessageAck.Tuple tuple : context.getBatch()) {
      if (ReplicationResultCode.RECEIVED == tuple.result) {
        ActivePassiveAckWaiter waiter = waiters.get(tuple.respondTo);
        if (null != waiter) {
          waiter.didReceiveOnPassive(messageFrom);
        }
      } else {
        // This is a normal completion.
        boolean isNormalComplete = true;
        internalAckCompleted(tuple.respondTo, messageFrom, tuple.result, isNormalComplete);
      }
    }
  }

  /**
   * This internal handling for completed is split out since it happens for both completed acks but also situations which
   * implies no ack is forthcoming (the passive disappearing, for example).
   */
  private void internalAckCompleted(SyncReplicationActivity.ActivityID activityID, NodeID passive, ReplicationResultCode payload, boolean isNormalComplete) {
    ActivePassiveAckWaiter waiter = waiters.get(activityID);
    if (null != waiter) {
      boolean shouldDiscardWaiter = waiter.didCompleteOnPassive(passive, isNormalComplete, payload);
      if (shouldDiscardWaiter) {
        waiters.remove(activityID);
      }
    }
  }

  @Override
  public Set<NodeID> passives() {
    return passiveNodes;
  }

  @Override
  public ActivePassiveAckWaiter replicateActivity(SyncReplicationActivity activity, Set<NodeID> all) {
    Set<NodeID> copy = new HashSet<>(all); 
// don't replicate to a passive that is no longer there
    copy.retainAll(passives());
    ActivePassiveAckWaiter waiter = new ActivePassiveAckWaiter(copy, this);
    if (!copy.isEmpty()) {
      SyncReplicationActivity.ActivityID activityID = activity.getActivityID();
      waiters.put(activityID, waiter);
      // Note that we want to explicitly create the ReplicationEnvelope using a different helper if it is a local flush
      //  command.
      boolean isLocalFlush = (SyncReplicationActivity.ActivityType.FLUSH_LOCAL_PIPELINE == activity.getActivityType());
      for (NodeID node : copy) {
        // This is a normal completion.
        boolean isNormalComplete = true;
        Runnable droppedWithoutSend = ()->internalAckCompleted(activityID, node, null, isNormalComplete);
        if (isLocalFlush) {
          // We aren't going to send this to the replication sender so just acknowledge that it was dropped without send, here.
          droppedWithoutSend.run();
        } else {
          this.replicationSender.replicateMessage(ReplicationReplicateMessageIntent.createReplicatedMessageEnvelope(node, activity, droppedWithoutSend));
        }
      }
    }
    return waiter;
  }

  public void removePassive(NodeID nodeID) {
// first remove it from the list of passive nodes so that anything sending new messages 
// will have to remove it from the list of nodes to send to
    passiveNodes.remove(nodeID);
//  acknowledge all the messages for this node because it is gone, this may result in 
//  a double ack locally but that is ok.  acknowledge is loose and can tolerate it. 
    if (activated) {
      // This is a an unexpected kind of completion.
      boolean isNormalComplete = false;
      waiters.forEach((key, value)->internalAckCompleted(key, nodeID, null,isNormalComplete));
//  this is a flush message (null).  Tell the sink there will be no more 
//  messages targeted at this nodeid
      BarrierCompletion block = new BarrierCompletion();
      this.replicationSender.removePassive(ReplicationRemovePassiveIntent.createRemovePassiveEnvelope(nodeID, ()->block.complete()));
      block.waitForCompletion();
    }
  }

  @Override
  public void nodeJoined(NodeID nodeID) {
//  standby nodes for tracking only.  no practical use
    synchronized(standByNodes) {
      standByNodes.add(nodeID);
    }
  }

  @Override
  public void nodeLeft(NodeID nodeID) {
    removePassive(nodeID);
//  standby nodes for tracking only.  no practical use
    synchronized(standByNodes) {
      standByNodes.remove(nodeID);
      standByNodes.notifyAll();
    }
  }
}
