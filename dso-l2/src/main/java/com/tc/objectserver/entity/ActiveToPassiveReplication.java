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
import com.tc.l2.state.StateManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.net.groups.GroupEventsListener;
import com.tc.net.groups.GroupMessage;
import com.tc.net.groups.MessageID;
import com.tc.object.EntityDescriptor;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ManagedEntity;
import com.tc.objectserver.api.ServerEntityAction;
import com.tc.util.Assert;

import java.util.Collection;
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
 *
 */
public class ActiveToPassiveReplication implements PassiveReplicationBroker, GroupEventsListener {
  
  private static final TCLogger logger           = TCLogging.getLogger(PassiveReplicationBroker.class);
  private final EntityManagerImpl entities;
  private final StateManager state;
  private final Set<NodeID> passiveNodes = new CopyOnWriteArraySet<NodeID>();
  private final ConcurrentHashMap<MessageID, Set<NodeID>> waiters = new ConcurrentHashMap<>();
  private final Sink<ReplicationEnvelope> replicate;
  private final Executor passiveSyncPool = Executors.newCachedThreadPool();

  public ActiveToPassiveReplication(EntityManagerImpl entities, StateManager state, Sink<ReplicationEnvelope> replicate) {
    this.entities = entities;
    this.state = state;
    this.replicate = replicate;
  }
  
  private void startPassiveSync(NodeID newNode) {
      passiveNodes.add(newNode);
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
          replicate.addSingleThreaded(new PassiveSyncMessage(true).target(newNode));
          Collection<ManagedEntity> currentEntities = entities.getAll();
          for (ManagedEntity entity : currentEntities) {
      // TODO: this is a stub implementation and needs to be fully designed
              entity.sync(newNode);
      //  create entity on passive
      //  start passive sync for entity      
          }
      //  passive sync done message.  causes passive to go into passive standby mode
          replicate.addSingleThreaded(new PassiveSyncMessage(false).target(newNode));
      }
    });
  }

  public void acknowledge(GroupMessage msg) {
//      assert that msg.getType() == REPLICATED_RESPONSE
    Set<NodeID> plist = waiters.get(msg.inResponseTo());
    synchronized(plist) {
      if (plist.remove(msg.messageFrom()) && plist.isEmpty()) {
        waiters.remove(msg.inResponseTo());
        plist.notifyAll();
      }
    }
  }    
  
  @Override
  public Future<Void> replicateSync(ReplicationMessage msg, Set<NodeID> all) {
    
    if (!all.isEmpty()) {
      waiters.put(msg.getMessageID(), all);
      for (NodeID node : all) {
        replicate.addSingleThreaded(msg.target(node));
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
        synchronized(all) {
          return all.isEmpty();
        }
      }

      @Override
      public Void get() throws InterruptedException, ExecutionException {
        synchronized (all) {
          while (!all.isEmpty()) {
            all.wait();
          }
        }
        return null;
      }

      @Override
      public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        synchronized (all) {
          while (!all.isEmpty()) {
            all.wait(unit.toMillis(timeout));
          }
        }
        return null;
      }
    };
  }

  @Override
  public Future<Void> replicateMessage(EntityDescriptor id, long version, NodeID src,
      ServerEntityAction type, TransactionID tid, TransactionID oldest, byte[] payload, int concurrency) {
    if (!state.isActiveCoordinator() || passiveNodes.isEmpty()) {
      return NoReplicationBroker.NOOP_FUTURE;
    }
    
    ReplicationMessage.ReplicationType actionCode = ReplicationMessage.ReplicationType.NOOP;
    switch (type) {
      case CREATE_ENTITY:
        actionCode = ReplicationMessage.ReplicationType.CREATE_ENTITY;
        break;
      case DESTROY_ENTITY:
        actionCode = ReplicationMessage.ReplicationType.DESTROY_ENTITY;
        break;
      case FETCH_ENTITY:
        //  TODO: probably shouldn't replicate this
        actionCode = ReplicationMessage.ReplicationType.NOOP;
        break;
      case INVOKE_ACTION:
        actionCode = ReplicationMessage.ReplicationType.INVOKE_ACTION;
        break;
      case NOOP:
        //  TODO: probably shouldn't replicate this
        actionCode = ReplicationMessage.ReplicationType.NOOP;
        break;
      case PROMOTE_ENTITY_TO_ACTIVE:
        //  TODO: probably shouldn't replicate this
//        actionCode = ReplicationMessage.ReplicationType.PROMOTE_ENTITY_TO_ACTIVE;
        actionCode = ReplicationMessage.ReplicationType.NOOP;
        break;
      case RELEASE_ENTITY:
        actionCode = ReplicationMessage.ReplicationType.RELEASE_ENTITY;
        break;
      case REQUEST_SYNC_ENTITY:
//  this marks the start of entity sync for a concurrency key.  practically, this means that
//  all replicated messages for this key and entity must be forwarded to passives
        actionCode = ReplicationMessage.ReplicationType.SYNC_ENTITY_CONCURRENCY_BEGIN;
        break;
      default:
        break;
    }
    final ReplicationMessage msg = new ReplicationMessage(id, src, tid, oldest, actionCode, payload, concurrency);
    return replicateSync(msg, new HashSet<NodeID>(passiveNodes));
  }
  
  public void removePassive(NodeID nodeID) {
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
    replicate.addSingleThreaded(new ReplicationEnvelope(nodeID, null));
  }
  
  @Override
  public void nodeJoined(NodeID nodeID) {
    if (state.isActiveCoordinator()) {
      startPassiveSync(nodeID);
    }
  }

  @Override
  public void nodeLeft(NodeID nodeID) {
    removePassive(nodeID);
  }
}
