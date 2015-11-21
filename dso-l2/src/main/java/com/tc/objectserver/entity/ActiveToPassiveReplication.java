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
  private final Iterable<ManagedEntity> entities;
  private final Set<NodeID> passiveNodes = new CopyOnWriteArraySet<NodeID>();
  private final ConcurrentHashMap<MessageID, Set<NodeID>> waiters = new ConcurrentHashMap<>();
  private final Sink<ReplicationEnvelope> replicate;
  private final Executor passiveSyncPool = Executors.newCachedThreadPool();

  public ActiveToPassiveReplication(Iterable<ManagedEntity> entities, Sink<ReplicationEnvelope> replicate) {
    this.entities = entities;
    this.replicate = replicate;
  }
  
  public void startPassiveSync(NodeID newNode) {
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
        for (ManagedEntity entity : entities) {
            entity.sync(newNode);
        }
    //  passive sync done message.  causes passive to go into passive standby mode
        replicate.addSingleThreaded(new PassiveSyncMessage(false).target(newNode));
      }
    });
  }

  public void acknowledge(GroupMessage msg) {
    Set<NodeID> plist = waiters.get(msg.inResponseTo());
    if (plist != null) {
      synchronized(plist) {
        if (plist.remove(msg.messageFrom()) && plist.isEmpty()) {
          waiters.remove(msg.inResponseTo());
          plist.notifyAll();
        }
      }
    }
  }    

  @Override
  public Set<NodeID> passives() {
    return passiveNodes;
  }

  //  this method is synchronized to protect the passiveNodes list.  It will compete
  // with passive node removal.  this is only called by a single thread as is node disconnect
  @Override
  public synchronized Future<Void> replicateMessage(ReplicationMessage msg, Set<NodeID> all) {
    Set<NodeID> copy = new HashSet<NodeID>(all); 
    copy.removeIf(node -> !passiveNodes.contains(node));
    if (!copy.isEmpty()) {
      waiters.put(msg.getMessageID(), copy);
      for (NodeID node : copy) {
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
    replicate.addSingleThreaded(new ReplicationEnvelope(nodeID, null));
  }

  @Override
  public void nodeJoined(NodeID nodeID) {

  }

  @Override
  public void nodeLeft(NodeID nodeID) {
    removePassive(nodeID);
  }
}
