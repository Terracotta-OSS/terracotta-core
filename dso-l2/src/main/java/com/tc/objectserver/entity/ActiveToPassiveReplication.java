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
import com.tc.l2.msg.PassiveSyncMessage;
import com.tc.l2.msg.ReplicationEnvelope;
import com.tc.l2.msg.ReplicationMessage;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.net.groups.GroupEventsListener;
import com.tc.net.groups.GroupMessage;
import com.tc.net.groups.MessageID;
import com.tc.objectserver.api.ManagedEntity;
import com.tc.objectserver.handler.ProcessTransactionHandler;
import com.tc.objectserver.handler.ReplicationSender;
import com.tc.util.Assert;
import java.util.Collections;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;


/**
 *  This class acts to connect {@link ProcessTransactionHandler} to the {@link ReplicationSender}
 *
 *  This class lies idle until activated by setting the current passive nodes.  This should 
 *  occur only when the server is transitioning from passive-standby to active
 */
public class ActiveToPassiveReplication implements PassiveReplicationBroker, GroupEventsListener {
  
  private static final TCLogger logger           = TCLogging.getLogger(PassiveReplicationBroker.class);
  private final Iterable<ManagedEntity> entities;
  private final Iterable<NodeID> passives;
  private boolean activated = false;
  private final Set<NodeID> passiveNodes = new CopyOnWriteArraySet<>();
  private final Set<NodeID> standByNodes = new HashSet<>();
  private final ConcurrentHashMap<MessageID, ActivePassiveAckWaiter> waiters = new ConcurrentHashMap<>();
  private final Sink<ReplicationEnvelope> replicate;
  private final Executor passiveSyncPool = Executors.newCachedThreadPool();

  public ActiveToPassiveReplication(Iterable<NodeID> passives, Iterable<ManagedEntity> entities, Sink<ReplicationEnvelope> replicate) {
    this.entities = entities;
    this.replicate = replicate;
    this.passives = passives;
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
      ReplicationMessage resetOrderedSink = ReplicationMessage.createStartMessage();
      Semaphore block = new Semaphore(0);
      replicate.addSingleThreaded(resetOrderedSink.target(node,()->block.release()));
      waitOnSemaphore(block);
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
        try {
          replicateMessage(PassiveSyncMessage.createStartSyncMessage(), Collections.singleton(newNode)).waitForCompleted();
          for (ManagedEntity entity : entities) {
            logger.debug("starting sync for entity " + newNode + "/" + entity.getID());
            entity.sync(newNode);
            logger.debug("ending sync for entity " + newNode + "/" + entity.getID());
          }
      //  passive sync done message.  causes passive to go into passive standby mode
          logger.debug("ending sync " + newNode);
          replicateMessage(PassiveSyncMessage.createEndSyncMessage(), Collections.singleton(newNode)).waitForCompleted();
        } catch (InterruptedException e) {
          throw new AssertionError("error during passive sync", e);
        }
      }
    });
  }

  public void ackReceived(GroupMessage msg) {
    ActivePassiveAckWaiter waiter = waiters.get(msg.inResponseTo());
    if (null != waiter) {
      waiter.didReceiveOnPassive(msg.messageFrom());
    }
  }

  public void ackCompleted(GroupMessage msg) {
    // This is a normal completion.
    boolean isNormalComplete = true;
    internalAckCompleted(msg.inResponseTo(), msg.messageFrom(), isNormalComplete);
  }

  /**
   * This internal handling for completed is split out since it happens for both completed acks but also situations which
   * implies no ack is forthcoming (the passive disappearing, for example).
   */
  private void internalAckCompleted(MessageID mid, NodeID passive, boolean isNormalComplete) {
    ActivePassiveAckWaiter waiter = waiters.get(mid);
    if (null != waiter) {
      boolean shouldDiscardWaiter = waiter.didCompleteOnPassive(passive, isNormalComplete);
      if (shouldDiscardWaiter) {
        waiters.remove(mid);
      }
    }
  }

  @Override
  public Set<NodeID> passives() {
    return passiveNodes;
  }

  @Override
  public ActivePassiveAckWaiter replicateMessage(ReplicationMessage msg, Set<NodeID> all) {
    Set<NodeID> copy = new HashSet<>(all); 
// don't replicate to a passive that is no longer there
    copy.retainAll(passives());
    ActivePassiveAckWaiter waiter = new ActivePassiveAckWaiter(copy);
    if (!copy.isEmpty()) {
      waiters.put(msg.getMessageID(), waiter);
      for (NodeID node : copy) {
        // This is a normal completion.
        boolean isNormalComplete = true;
        replicate.addSingleThreaded(msg.target(node, ()->internalAckCompleted(msg.getMessageID(), node, isNormalComplete)));
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
      waiters.forEach((key, value)->internalAckCompleted(key, nodeID, isNormalComplete));
//  this is a flush message (null).  Tell the sink there will be no more 
//  messages targeted at this nodeid
      Semaphore block = new Semaphore(0);
      replicate.addSingleThreaded(new ReplicationEnvelope(nodeID, null, ()->block.release()));
      waitOnSemaphore(block);
    }
  }
  
  private void waitOnSemaphore(Semaphore block) {
    try {
      block.acquire();
    } catch (InterruptedException e) {
      throw new AssertionError(e);
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
    }
  }
}
