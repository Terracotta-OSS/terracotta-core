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

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventHandlerException;
import com.tc.l2.msg.ReplicationEnvelope;
import com.tc.l2.msg.ReplicationMessage;
import static com.tc.l2.msg.ReplicationMessage.ReplicationType.SYNC_BEGIN;
import static com.tc.l2.msg.ReplicationMessage.ReplicationType.SYNC_ENTITY_BEGIN;
import static com.tc.l2.msg.ReplicationMessage.ReplicationType.SYNC_ENTITY_CONCURRENCY_BEGIN;
import static com.tc.l2.msg.ReplicationMessage.ReplicationType.SYNC_ENTITY_CONCURRENCY_END;
import static com.tc.l2.msg.ReplicationMessage.ReplicationType.SYNC_ENTITY_CONCURRENCY_PAYLOAD;
import static com.tc.l2.msg.ReplicationMessage.ReplicationType.SYNC_ENTITY_END;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.object.EntityID;
import com.tc.util.Assert;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;


public class ReplicationSender extends AbstractEventHandler<ReplicationEnvelope> {
  //  this is all single threaded.  If there is any attempt to make this multi-threaded,
  //  control structures must be fixed
  private final GroupManager group;
  private final Map<NodeID, AtomicLong> ordering = new HashMap<>();
  private final Map<NodeID, SyncState> filtering = new HashMap<>();
  private static final TCLogger logger           = TCLogging.getLogger(ReplicationSender.class);

  public ReplicationSender(GroupManager group) {
    this.group = group;
  }

  @Override
  public void handleEvent(ReplicationEnvelope context) throws EventHandlerException {
    NodeID nodeid = context.getDestination();
    ReplicationMessage msg = context.getMessage();
    if (msg == null) {
// this is a flush of the replication channel.  shut it down and return;
      ordering.remove(nodeid);
      filtering.remove(nodeid);
      context.release();
    } else {
      SyncState syncing = getSyncState(nodeid, msg);
      AtomicLong rOrder = getOrdering(nodeid, msg);
      
      if (rOrder == null) {
// this is message priming, the order is being established or the passive is gone
        context.release();
        return;
      }
// check to make sure that this message is a type that is relevant to a passive
      if (!shouldReplicate(msg)) {
        context.release();
        return;
      }   
// filter out messages based on sync state.
      if (filterMessage(syncing, nodeid, msg)) {
//  if a message is filtered, it is turned to a NOOP so ordering can be preserved 
//  on the passive for possible resends
        msg.setNoop();
      }
//  sending message on to passive, additional filtering may happen on the other side.
//  the only messages that are relevant before passive sync starts are create messages
      try {
        msg.setReplicationID(rOrder.getAndIncrement());
        logger.debug("WIRE:" + msg);
        group.sendTo(nodeid, msg);
      }  catch (GroupException ge) {
        logger.info(msg, ge);
      }
    }
  }
  
  private AtomicLong getOrdering(NodeID nodeid, ReplicationMessage msg) {
    if (!ordering.containsKey(nodeid)) {
      if (msg.getType() == ReplicationMessage.START) {
        ordering.put(nodeid, new AtomicLong());
//  release the message so sync can continue
//  this is a priming event.  The passive does not need to receive this
        return null;
      } else {
        if (dropMessageForDisconnectedServer(nodeid, msg)) {
          return null;
        }
      }
    }
    return ordering.get(nodeid);
  }
  
  private SyncState getSyncState(NodeID nodeid, ReplicationMessage msg) {
    if (msg.getReplicationType() == SYNC_BEGIN) {
      SyncState syncing = new SyncState();
      filtering.put(nodeid, syncing);
      return syncing;
    } else {
      return filtering.get(nodeid);
    }
  }
  
  private boolean filterMessage(SyncState state, NodeID nodeid, ReplicationMessage msg) {
    if (state != null) {
//  there is an active sync going on, need to filter out messages that should not be replicated
      if (!state.filter(msg)) {
        if (logger.isDebugEnabled()) {
          logger.debug(nodeid + ":Filtering " + msg + " for " + msg.getEntityDescriptor().getEntityID());
        }
        return true;
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug(nodeid + ":Sending " + msg + " for " + msg.getEntityDescriptor().getEntityID());
        }
        state.validateSending(msg);
        if (state.isComplete()) {
          filtering.remove(nodeid);
        }
      }
    }
    return false;
  }
  
  private boolean dropMessageForDisconnectedServer(NodeID nodeid, ReplicationMessage msg) {
//  make sure node is not connected
    Assert.assertFalse("node is not connected for:" + msg, group.isNodeConnected(nodeid));
//  passive must have died during passive sync, ignore this message
    logger.info("ignoring " + msg + " target " + nodeid + " no longer exists");
    return true;
  }
  
  private boolean shouldReplicate(ReplicationMessage msg) {
    if (msg.getType() == ReplicationMessage.START) {
//  these types of messages are incoming types or internal server use, not outgoing
      throw new AssertionError("unexpected message type " + msg);
    }
    switch (msg.getReplicationType()) {
      case NOOP:
//  this is a special case noop that gets replicated to the passive to communicate that
//  the client has gone away and persistors should do cleanup
        return !msg.getSource().isNull();
      default:
        return true;
    }
  }

  @Override
  protected void initialize(ConfigurationContext context) {
    super.initialize(context);
  }
  
  private static class SyncState {
    private final Set<EntityID> syncd = new HashSet<>();
    private EntityID syncingID = EntityID.NULL_ID;
    private final Set<Integer> syncdID = new HashSet<>();
    private final Set<EntityID> created = new HashSet<>();
    private final Set<EntityID> destroyed = new HashSet<>();
    private int syncingConcurrency = -1;
    private ReplicationMessage.ReplicationType lastSeen;
    private ReplicationMessage.ReplicationType lastSent;
    
    public boolean filter(ReplicationMessage msg) {
      final EntityID eid = msg.getEntityDescriptor().getEntityID();
        switch (validateInput(msg)) {
          case SYNC_BEGIN:
            return true;
          case SYNC_ENTITY_BEGIN:
            syncingID = eid;
            syncdID.clear();
            syncingConcurrency = 0;
//  if the entity is created through the create message replication, the entity should not be sync'd
//  set syncid to null so all messages until end get filtered out.  this should not be alot, it just got created
            if (created.contains(syncingID) || destroyed.contains(eid)) {
              syncingID = EntityID.NULL_ID;
              logger.debug("Drop: entity " + syncingID + " was created no sync required");
              return false;
            }
            return true;
          case SYNC_ENTITY_CONCURRENCY_BEGIN:
            if (syncingID == EntityID.NULL_ID) {
              return false;
            }
            Assert.assertEquals(syncingID, eid);
            Assert.assertEquals(syncingConcurrency, 0);
            syncingConcurrency = msg.getConcurrency();
            return true;
          case SYNC_ENTITY_CONCURRENCY_PAYLOAD:
            if (syncingID == EntityID.NULL_ID) {
              return false;
            }
            if (destroyed.contains(eid)) {
              return false;
            }
            return true;
          case SYNC_ENTITY_CONCURRENCY_END:
            if (syncingID == EntityID.NULL_ID) {
              return false;
            }
            Assert.assertEquals(syncingConcurrency, msg.getConcurrency());
            syncdID.add(syncingConcurrency);
            syncingConcurrency = 0;
            return true;
          case SYNC_ENTITY_END:
            if (syncingID == EntityID.NULL_ID) {
              return false;
            }
            Assert.assertEquals(syncingID, eid);
            syncd.add(syncingID);
            syncingID = EntityID.NULL_ID;
            return true;
          case SYNC_END:
            return true;
          case CREATE_ENTITY:
            if (syncd.contains(eid)) {
              created.add(eid);
              destroyed.remove(eid);
              return true;
            } else if (destroyed.contains(eid)) {
              created.add(eid);
              destroyed.remove(eid);
              return true;
            } else if (eid.equals(syncingID)) {
              logger.debug("skipping create due to syncing id " + syncingID);
//  this entity is being or has been replicated, don't create it on the passive
              return false;
            } else {
//  add this to the list of entities already syncd
              created.add(eid);
              destroyed.remove(eid);
              return true;
            }
          case RECONFIGURE_ENTITY:
          case FETCH_ENTITY:
          case RELEASE_ENTITY:
            if (syncd.contains(eid) || eid.equals(syncingID)) {
//  this entity is being or has been replicated, send the reconfigure through
              return true;
            } else {
//  this entity has not been on the passive yet, reconfigure will go with replication
              return false;
            }
          case DESTROY_ENTITY:
            if (syncd.contains(eid)) {
//  this would only happen if an entity was destroyed
              destroyed.add(eid);
              return true;
            } else if (syncingID.equals(eid)) {
 //  tricky.  this one needs to pass but only be applied after sync of this entity is complete
              destroyed.add(eid);
              return true;
            } else if (created.contains(eid)) {
              destroyed.add(eid);
              created.remove(eid);
              return true;
            } else {
 //  hasn't been syncd yet.  never sync it
              destroyed.add(eid);
              return false;
            }
          case INVOKE_ACTION:
            if (syncd.contains(eid) || created.contains(eid)) {
              return true;
            } else if (syncingID.equals(eid)) {
              if (syncingConcurrency == msg.getConcurrency()) {
//  special case.  passive will apply this after sync of the key is complete
                return true;
              }
              return syncdID.contains(msg.getConcurrency());
            } else {
// hasn't been sync'd yet.  state will be captured in sync
              return false;
            }
          case NOOP:
            return false;
          default:
            throw new AssertionError("unknown replication message:" + msg);
        }
    }

    public ReplicationMessage.ReplicationType validateInput(ReplicationMessage msg) {
      ReplicationMessage.ReplicationType type = msg.getReplicationType();
      if (msg.getType() == ReplicationMessage.SYNC) {
        lastSeen = validate(msg.getReplicationType(), lastSeen);
      }
      return type;
    }
    
    public void validateSending(ReplicationMessage msg) {
      if (msg.getType() == ReplicationMessage.SYNC) {
        lastSent = validate(msg.getReplicationType(), lastSent);
      }
    }
    
    public boolean isComplete() {
      return lastSent == ReplicationMessage.ReplicationType.SYNC_END;
    }
    
    private ReplicationMessage.ReplicationType validate(ReplicationMessage.ReplicationType type,ReplicationMessage.ReplicationType compare) {
      switch (type) {
        case SYNC_BEGIN:
          Assert.assertNull(compare);
          break;
        case SYNC_ENTITY_BEGIN:
          Assert.assertTrue(type + " " + compare, EnumSet.of(SYNC_BEGIN, SYNC_ENTITY_END).contains(compare));
          break;
        case SYNC_ENTITY_CONCURRENCY_BEGIN:
          Assert.assertTrue(type + " " + compare, EnumSet.of(SYNC_ENTITY_BEGIN, SYNC_ENTITY_CONCURRENCY_END).contains(compare));
          break;
        case SYNC_ENTITY_CONCURRENCY_PAYLOAD:
          Assert.assertTrue(type + " " + compare, EnumSet.of(SYNC_ENTITY_CONCURRENCY_BEGIN, SYNC_ENTITY_CONCURRENCY_PAYLOAD).contains(compare));
          break;
        case SYNC_ENTITY_CONCURRENCY_END:
          Assert.assertTrue(type + " " + compare, EnumSet.of(SYNC_ENTITY_CONCURRENCY_BEGIN, SYNC_ENTITY_CONCURRENCY_PAYLOAD).contains(compare));
          break;
        case SYNC_ENTITY_END:
          Assert.assertTrue(type + " " + compare, EnumSet.of(SYNC_ENTITY_BEGIN, SYNC_ENTITY_CONCURRENCY_END).contains(compare));
          break;
        case SYNC_END:
          Assert.assertTrue(type + " " + compare, EnumSet.of(SYNC_ENTITY_END, SYNC_BEGIN).contains(compare));
          break;
        default:
          throw new AssertionError("unexpected message type");
      }
      return type;
    }
  }
  
}
