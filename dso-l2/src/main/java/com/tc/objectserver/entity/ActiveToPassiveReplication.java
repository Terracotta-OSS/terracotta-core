/*
 * All content copyright (c) 2014 Terracotta, Inc., except as may otherwise
 * be noted in a separate copyright notice. All rights reserved.
 */
package com.tc.objectserver.entity;

import com.tc.async.api.Sink;
import com.tc.l2.msg.PassiveSyncMessage;
import com.tc.l2.msg.ReplicationMessage;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.net.groups.GroupEventsListener;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.net.groups.GroupMessage;
import com.tc.net.groups.MessageID;
import com.tc.object.EntityDescriptor;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ManagedEntity;
import com.tc.objectserver.api.ServerEntityAction;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 */
public class ActiveToPassiveReplication implements PassiveReplicationBroker {
  
  private static final TCLogger logger           = TCLogging.getLogger(PassiveReplicationBroker.class);
  private final EntityManagerImpl entities;
  private final GroupManager groups;
  private final Set<NodeID> passiveNodes = new CopyOnWriteArraySet<NodeID>();
  private final ConcurrentHashMap<MessageID, Set<NodeID>> waiters = new ConcurrentHashMap<>();
  private final Sink<ReplicationMessage> replicate;
  private final AtomicLong rid = new AtomicLong();
  private boolean isActive;

  public ActiveToPassiveReplication(GroupManager group, EntityManagerImpl entities, Sink<ReplicationMessage> replicate) {
    this.entities = entities;
    this.replicate = replicate;
    this.groups = group;
    this.groups.registerForGroupEvents(new GroupEvents());
  }

  @Override
  public boolean isActive() {
    return this.isActive;
  }

  @Override
  public void setActive(boolean active) {
    this.isActive = active;
  }
  
  private void startPassiveSync(GroupManager groups, NodeID newNode) {
    try {
      passiveNodes.add(newNode);
      groups.sendTo(newNode, new PassiveSyncMessage(true));
      Collection<ManagedEntity> currentEntities = entities.getAll();
      for (ManagedEntity entity : currentEntities) {
  // TODO: this is a stub implementation and needs to be fully designed
          entity.sync(newNode, groups);
  //  create entity on passive
  //  start passive sync for entity      
      }
      groups.sendTo(newNode, new PassiveSyncMessage(false));
    }  catch (GroupException ge) {
      logger.info(ge);
    }
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
  public Future<Void> replicateMessage(EntityDescriptor id, long version, NodeID src,
      ServerEntityAction type, TransactionID tid, TransactionID oldest, byte[] payload) {
    if (!isActive || passiveNodes.isEmpty()) {
      return NoReplicationBroker.NOOP_FUTURE;
    }
    
    Set<NodeID> all = new HashSet<NodeID>(passiveNodes);
    int actionCode = -1;
    switch (type) {
      case CREATE_ENTITY:
        actionCode = ReplicationMessage.CREATE_ENTITY;
        break;
      case DESTROY_ENTITY:
        actionCode = ReplicationMessage.DESTROY_ENTITY;
        break;
      case FETCH_ENTITY:
        //  TODO: probably shouldn't replicate this
        actionCode = ReplicationMessage.GET_ENTITY;
        break;
      case INVOKE_ACTION:
        actionCode = ReplicationMessage.INVOKE_ACTION;
        break;
      case NOOP:
        //  TODO: probably shouldn't replicate this
        actionCode = ReplicationMessage.NOOP;
        break;
      case PROMOTE_ENTITY_TO_ACTIVE:
        //  TODO: probably shouldn't replicate this
        actionCode = ReplicationMessage.PROMOTE_ENTITY_TO_ACTIVE;
        break;
      case RELEASE_ENTITY:
        actionCode = ReplicationMessage.RELEASE_ENTITY;
        break;
      case SYNC_ENTITY:
        actionCode = ReplicationMessage.SYNC_ENTITY;
        break;
      default:
        break;
    }
    final ReplicationMessage msg = new ReplicationMessage(id, version, src, all, tid, oldest, actionCode, payload, rid.incrementAndGet());
    
    if (!all.isEmpty()) {
      waiters.put(msg.getMessageID(), all);
      replicate.addSingleThreaded(msg);
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
  }
  
  class GroupEvents implements GroupEventsListener { 

    @Override
    public void nodeJoined(NodeID nodeID) {
      if (isActive()) {
        startPassiveSync(groups, nodeID);
      }
    }

    @Override
    public void nodeLeft(NodeID nodeID) {
      removePassive(nodeID);
    }
  }
}
