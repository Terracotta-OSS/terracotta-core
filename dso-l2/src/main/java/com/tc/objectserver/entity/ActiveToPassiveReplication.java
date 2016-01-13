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
import java.util.Collection;
import java.util.Collections;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
  private final Set<NodeID> standByNodes = new CopyOnWriteArraySet<>();
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
    primePassives(passives);
  }
  
  private void primePassives(Iterable<NodeID> standbys) {
    Assert.assertFalse(activated);
//    Assert.assertTrue(passiveNodes.isEmpty());
    standbys.forEach(i -> {
      if (passiveNodes.add(i)) {
        logger.debug("sending reset to " + i);
        prime(i);
      }
    });
    activated = true;
  }
  
  private Future<Void> prime(NodeID node) {
    ReplicationMessage resetOrderedSink = new ReplicationMessage();
    return replicateMessage(resetOrderedSink, Collections.singleton(node));
  }
  
  public void startPassiveSync(NodeID newNode) {
    Assert.assertTrue(activated);
    if (passiveNodes.add(newNode)) {
      logger.debug("sending reset to " + newNode);
      prime(newNode);
    }
    logger.debug("starting sync to " + newNode);
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
        replicate.addSingleThreaded(PassiveSyncMessage.createStartSyncMessage().target(newNode));
        for (ManagedEntity entity : entities) {
          logger.debug("starting sync for entity " + newNode + "/" + entity.getID());
          entity.sync(newNode);
          logger.debug("ending sync for entity " + newNode + "/" + entity.getID());
        }
    //  passive sync done message.  causes passive to go into passive standby mode
        logger.debug("ending sync " + newNode);
        replicate.addSingleThreaded(PassiveSyncMessage.createEndSyncMessage().target(newNode));
      }
    });
  }

  private void acknowledge(MessageID mid, NodeID releaser) {
    Set<NodeID> plist = waiters.get(mid);
    if (plist != null) {
      synchronized(plist) {
        if (plist.remove(releaser) && plist.isEmpty()) {
          waiters.remove(mid);
          plist.notifyAll();
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

  //  this method is synchronized to protect the passiveNodes list.  It will compete
  // with passive node removal.  this is only called by a single thread as is node disconnect
  @Override
  public synchronized Future<Void> replicateMessage(ReplicationMessage msg, Set<NodeID> all) {
    Set<NodeID> copy = new HashSet<>(all); 
    copy.removeIf(node -> !passiveNodes.contains(node));
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
          while (!copy.isEmpty()) {
            copy.wait();
          }
        }
        return null;
      }

      @Override
      public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        synchronized (copy) {
          while (!copy.isEmpty()) {
            copy.wait(unit.toMillis(timeout));
          }
        }
        return null;
      }
    };
  }
  //  this method is synchronized to protect the passiveNodes list.  It will compete
  // with passive node replicate
  public synchronized void removePassive(NodeID nodeID) {
    passiveNodes.remove(nodeID);
    
    for (Map.Entry<MessageID, Set<NodeID>> entry : waiters.entrySet()) {
      Set<NodeID> all = entry.getValue();
      synchronized (all) {
        if (all.remove(nodeID) && all.isEmpty()) {
          waiters.remove(entry.getKey());
          all.notifyAll();
        }
      }
    }
//  this is a flush message (null).  Tell the sink there will be no more 
//  messages targeted at this nodeid
    replicate.addSingleThreaded(new ReplicationEnvelope(nodeID, null, null));
  }

  @Override
  public void nodeJoined(NodeID nodeID) {
    standByNodes.add(nodeID);
  }

  @Override
  public void nodeLeft(NodeID nodeID) {
    removePassive(nodeID);
    standByNodes.remove(nodeID);
  }
}
