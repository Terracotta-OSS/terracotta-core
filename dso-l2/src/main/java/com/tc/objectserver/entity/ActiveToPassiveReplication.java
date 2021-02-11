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
import com.tc.bytes.TCByteBufferFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.l2.ha.L2HAZapNodeRequestProcessor;
import com.tc.l2.msg.ReplicationAckTuple;
import com.tc.l2.msg.ReplicationMessageAck;
import com.tc.l2.msg.ReplicationResultCode;
import com.tc.l2.msg.SyncReplicationActivity;
import com.tc.net.NodeID;
import com.tc.net.groups.GroupEventsListener;
import com.tc.net.groups.GroupManager;
import com.tc.objectserver.api.ManagedEntity;
import com.tc.objectserver.handler.ProcessTransactionHandler;
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
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import com.tc.l2.state.ConsistencyManager;
import com.tc.l2.state.ServerMode;
import com.tc.net.ServerID;
import com.tc.object.session.SessionID;
import com.tc.objectserver.handler.ReplicationReceivingAction;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;


/**
 *  This class acts to connect {@link ProcessTransactionHandler} to the {@link ReplicationSender}
 *
 *  This class lies idle until activated by setting the current passive nodes.  This should 
 *  occur only when the server is transitioning from passive-standby to active
 */
public class ActiveToPassiveReplication implements PassiveReplicationBroker, GroupEventsListener {
  
  private static final Logger LOGGER = LoggerFactory.getLogger(ActiveToPassiveReplication.class);
  private boolean activated = false;
  private final Map<ServerID, SessionID> passiveNodes = new ConcurrentHashMap<>();
  private final Set<NodeID> standByNodes = new HashSet<>();
  private final ConcurrentHashMap<SyncReplicationActivity.ActivityID, ActivePassiveAckWaiter> waiters = new ConcurrentHashMap<>();
  private final ReplicationSender replicationSender;
  private final ExecutorService passiveSyncPool = Executors.newCachedThreadPool();
  private final EntityPersistor persistor;
  private final GroupManager serverCheck;
  private final ProcessTransactionHandler snapshotter;
  private final ConsistencyManager consistencyMgr;
  
  private final Sink<ReplicationReceivingAction> receiveHandler;
  private final AtomicLong sessionMaker = new AtomicLong();

  public ActiveToPassiveReplication(ConsistencyManager consistencyMgr, ProcessTransactionHandler snapshotter, EntityPersistor persistor, ReplicationSender replicationSender, Sink<ReplicationReceivingAction> processor, GroupManager serverMatch) {
    this.consistencyMgr = consistencyMgr;
    this.replicationSender = replicationSender;
    this.persistor = persistor;
    this.serverCheck = serverMatch;
    this.snapshotter = snapshotter;
    this.receiveHandler = processor;
  }

  @Override
  public void zapAndWait(NodeID node) {
    synchronized(this.standByNodes) {
      LOGGER.warn("ZAPPING " + node + " due to inconsistent lifecycle result");
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
  
  public void enterActiveState(Set<ServerID> passives) {
    Assert.assertFalse(activated);
    primePassives(passives);
    activated = true;
  }
  /**
 * starts the stream of messages to each passive the server knows about.  This should only happen 
 * when a server enters active state.
 */
  private void primePassives(Set<ServerID> passives) {
    passives.forEach(i -> {
      SessionID session = prime(i);
      if (session.isNull()) {
        LOGGER.warn("add passive disallowed for " + i);
      } else {
        passiveNodes.putIfAbsent(i, session);
      }
    });
  }
/**
 * prime the message channel to a node by setting the starting ordering id to zero.
 */
  private SessionID prime(ServerID node) {
    Assert.assertFalse(node.isNull());
    SessionID current = passiveNodes.getOrDefault(node, SessionID.NULL_ID);
    if (standByNodes.contains(node) && serverCheck.isNodeConnected(node)) {
      if (!consistencyMgr.requestTransition(ServerMode.ACTIVE, node, ConsistencyManager.Transition.ADD_PASSIVE)) {
        serverCheck.zapNode(node, L2HAZapNodeRequestProcessor.SPLIT_BRAIN, "unable to verify active");
        return SessionID.NULL_ID;
      } else {
        if (serverCheck.isNodeConnected(node)) {
          LOGGER.debug("Starting message sequence on " + node);
          SessionID newSession = new SessionID(sessionMaker.incrementAndGet());

          boolean sent = this.replicationSender.addPassive(node, newSession, executionLane(newSession), SyncReplicationActivity.createStartMessage());
          Assert.assertTrue(sent);
          return newSession;
        }
      }
    }
    return current;
  }

  private static int executionLane(SessionID session) {
    return Long.hashCode(session.toLong());
  }
  
  public void startPassiveSync(ServerID newNode) {
    Assert.assertTrue(activated);
    SessionID session = prime(newNode);
    if (session.isValid()) {
      if (passiveNodes.putIfAbsent(newNode, session) == null) {
        LOGGER.info("Starting sync to node: {} session: {}", newNode, session);
        executePassiveSync(newNode, session);
      }
    } else {
      Assert.assertTrue("passive node unable to prime and not in the list of passives", passiveNodes.containsKey(newNode));
    }
  }
  /**
   * Using an executor service here to sync multiple passives at once
   * @param newNode
   */
  private void executePassiveSync(final NodeID newNode, SessionID session) {
    passiveSyncPool.execute(() -> {
      // start passive sync message
      LOGGER.debug("starting sync for " + newNode + " on session " + session);
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
                  toArray(new SyncReplicationActivity.EntityCreationTuple[tuplesForCreation.size()])), Collections.singleton(session)).waitForCompleted();
        }}
      );
      for (ManagedEntity entity : e) {
        LOGGER.debug("starting sync for entity " + newNode + "/" + entity.getID());
        entity.sync(session);
        LOGGER.debug("ending sync for entity " + newNode + "/" + entity.getID());
      }
      //  passive sync done message.  causes passive to go into passive standby mode
      LOGGER.debug("ending sync " + newNode);
      replicateActivity(SyncReplicationActivity.createEndSyncMessage(TCByteBufferFactory.wrap(replicateEntityPersistor())), Collections.singleton(session)).waitForCompleted();
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
    ServerID messageFrom = context.messageFrom();
    SessionID session = this.passiveNodes.getOrDefault(messageFrom, SessionID.NULL_ID);
    if (session.isValid()) {
      this.receiveHandler.addToSink(new ReplicationReceivingAction(executionLane(session), ()->{
        for (ReplicationAckTuple tuple : context.getBatch()) {
          if (ReplicationResultCode.RECEIVED == tuple.result) {
            ActivePassiveAckWaiter waiter = waiters.get(tuple.respondTo);
            if (null != waiter) {
              waiter.didReceiveOnPassive(messageFrom);
            }
          } else {
            // This is a normal completion.
            internalAckCompleted(tuple.respondTo, messageFrom, tuple.result);
          }
        }
      }));
    }
  }

  /**
   * This internal handling for completed is split out since it happens for both completed acks but also situations which
   * implies no ack is forthcoming (the passive disappearing, for example).
   */
  private void internalAckCompleted(SyncReplicationActivity.ActivityID activityID, ServerID passive, ReplicationResultCode payload) {
    ActivePassiveAckWaiter waiter = waiters.get(activityID);
    if (null != waiter) {
      boolean shouldDiscardWaiter = waiter.didCompleteOnPassive(passive, payload);
      if (shouldDiscardWaiter) {
        waiters.remove(activityID);
      }
    }
  }

  @Override
  public Set<SessionID> passives() {
    Collection<SessionID> copy = passiveNodes.values().stream().filter(SessionID::isValid).collect(Collectors.toCollection(()->new ArrayList<>(passiveNodes.size())));
    return new AbstractSet<SessionID>() {
      @Override
      public Iterator<SessionID> iterator() {
        return copy.iterator();
      }

      @Override
      public int size() {
        return copy.size();
      }
    };
  }

  @Override
  public ActivePassiveAckWaiter replicateActivity(SyncReplicationActivity activity, Set<SessionID> all) {
    ActivePassiveAckWaiter waiter = new ActivePassiveAckWaiter(this.passiveNodes, all, this);
    if (!all.isEmpty()) {
      SyncReplicationActivity.ActivityID activityID = activity.getActivityID();
      waiters.put(activityID, waiter);
      // Note that we want to explicitly create the ReplicationEnvelope using a different helper if it is a local flush
      //  command.
      boolean isLocalFlush = (SyncReplicationActivity.ActivityType.FLUSH_LOCAL_PIPELINE == activity.getActivityType());
      for (SessionID node : all) {
        if (!isLocalFlush) {
          // This isn't local-only so try to replicate.
          if (node.isValid()) {
            this.replicationSender.replicateMessage(node, activity, sent->{
              if (!sent) {
                boolean complete = waiter.failedToSendToPassive(node);
                if (complete) {
                  waiters.remove(activityID);
                }
              }
            });
          }
        }

      }
    }
    return waiter;
  }

  private void removePassive(NodeID nodeID) {
    SessionID session = this.passiveNodes.putIfAbsent((ServerID)nodeID, SessionID.NULL_ID);
    LOGGER.info("removing passive: {} with session: {}", nodeID, session);
    passiveSyncPool.execute(()->{
      while (!consistencyMgr.requestTransition(ServerMode.ACTIVE, nodeID, ConsistencyManager.Transition.REMOVE_PASSIVE)) {
        try {
          TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException ie) {
          LOGGER.info("interrupted while waiting for permission to remove node");
        }
      }
// first remove it from the list of passive nodes so that anything sending new messages 
// will have to remove it from the list of nodes to send to
//  acknowledge all the messages for this node because it is gone, this may result in 
//  a double ack locally but that is ok.  acknowledge is loose and can tolerate it. 
  //  remove the passive node from the sender first.  nothing else is going out
      if (passiveNodes.remove(nodeID, session)) {
        removePassiveSession(session);
        LOGGER.info("removed passive: {} with session {}", nodeID, session);
      } else {
        Assert.assertTrue(this.passiveNodes.remove(nodeID, SessionID.NULL_ID));
      }
    });
  }
  
  private void removePassiveSession(SessionID session) {
    this.replicationSender.removePassive(session);
    Iterator<Map.Entry<SyncReplicationActivity.ActivityID, ActivePassiveAckWaiter>> scan = waiters.entrySet().iterator();
    while (scan.hasNext()) {
      Map.Entry<SyncReplicationActivity.ActivityID, ActivePassiveAckWaiter> e = scan.next();
      if (e.getValue().failedToSendToPassive(session)) {
        scan.remove();
      }
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
    if (activated) {
      removePassive(nodeID);
    } else {
      LOGGER.info("not activated, no passives to remove: {}", nodeID);
    }
//  standby nodes for tracking only.  no practical use
    synchronized(standByNodes) {
      standByNodes.remove(nodeID);
      standByNodes.notifyAll();
    }
  }
  // for test
  Map<SyncReplicationActivity.ActivityID, ActivePassiveAckWaiter> getWaiters() {
    return waiters;
  }
  // for test
  void finishPassiveSync(long timeout) throws InterruptedException {
    passiveSyncPool.shutdown();
    passiveSyncPool.awaitTermination(timeout, TimeUnit.MILLISECONDS);
  }
  
  
}
