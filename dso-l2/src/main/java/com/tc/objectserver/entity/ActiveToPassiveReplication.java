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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
  private final ConcurrentHashMap<MessageID, Set<NodeID>> waiters = new ConcurrentHashMap<>();
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
      ReplicationMessage resetOrderedSink = new ReplicationMessage(ReplicationMessage.START);
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
          replicateMessage(PassiveSyncMessage.createStartSyncMessage(), Collections.singleton(newNode)).get();
          for (ManagedEntity entity : entities) {
            logger.debug("starting sync for entity " + newNode + "/" + entity.getID());
            entity.sync(newNode);
            logger.debug("ending sync for entity " + newNode + "/" + entity.getID());
          }
      //  passive sync done message.  causes passive to go into passive standby mode
          logger.debug("ending sync " + newNode);
          replicateMessage(PassiveSyncMessage.createEndSyncMessage(), Collections.singleton(newNode)).get();
        } catch (InterruptedException | ExecutionException e) {
          throw new AssertionError("error during passive sync", e);
        }
      }
    });
  }

  private void acknowledge(MessageID mid, NodeID releaser) {
    Set<NodeID> plist = waiters.get(mid);
    if (plist != null) {
      synchronized(plist) {
        if (plist.remove(releaser)) {
          if (plist.isEmpty()) {
            if (!waiters.remove(mid, plist)) {
              throwAssertionError();
            }
            plist.notifyAll();
          }
        }
      }
    }
  }    

  public void acknowledge(GroupMessage msg) {
    acknowledge(msg.inResponseTo(), msg.messageFrom());
  }    

  @Override
  public Set<NodeID> passives() {
    return passiveNodes;
  }

  @Override
  public Future<Void> replicateMessage(ReplicationMessage msg, Set<NodeID> all) {
    Set<NodeID> copy = new HashSet<>(all); 
// don't replicate to a passive that is no longer there
    copy.retainAll(passives());
    if (!copy.isEmpty()) {
      waiters.put(msg.getMessageID(), copy);
      for (NodeID node : copy) {
        replicate.addSingleThreaded(msg.target(node, ()->acknowledge(msg.getMessageID(), node)));
      }
    }
    
    return new Future<Void>() {

      @Override
      public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
      }

      @Override
      public boolean isCancelled() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
      }

      @Override
      public boolean isDone() {
        synchronized(copy) {
          return copy.isEmpty();
        }
      }

      @Override
      public Void get() throws InterruptedException, ExecutionException {
        synchronized (copy) {
          copy.retainAll(passiveNodes);
          while (!copy.isEmpty()) {
            copy.wait();
          }
        }
        return null;
      }

      @Override
      public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        throw new UnsupportedOperationException("not implemented");
      }
    };
  }

  public void removePassive(NodeID nodeID) {
// first remove it from the list of passive nodes so that anything sending new messages 
// will have to remove it from the list of nodes to send to
    passiveNodes.remove(nodeID);
//  acknowledge all the messages for this node because it is gone, this may result in 
//  a double ack locally but that is ok.  acknowledge is loose and can tolerate it. 
    if (activated) {
      waiters.forEach((key, value)->acknowledge(key, nodeID));
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
  
  private void throwAssertionError() {
    throw new AssertionError("an error in the implementation has occurred standby nodes:" 
        + standByNodes + " passivesNodes:" + passiveNodes);
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
