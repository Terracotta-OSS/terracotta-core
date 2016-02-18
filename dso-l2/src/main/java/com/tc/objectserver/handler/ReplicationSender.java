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
import com.tc.l2.msg.PassiveSyncMessage;
import com.tc.l2.msg.ReplicationEnvelope;
import com.tc.l2.msg.ReplicationMessage;
import static com.tc.l2.msg.ReplicationMessage.ReplicationType.SYNC_BEGIN;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.object.EntityID;
import com.tc.util.Assert;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 */
public class ReplicationSender extends AbstractEventHandler<ReplicationEnvelope> {
  //  this is all single threaded.  If there is any attempt to make this multi-threaded,
  //  control structures must be fixed
  private final GroupManager group;
  private final Map<NodeID, AtomicLong> ordering = new HashMap<NodeID, AtomicLong>();
  private final Map<NodeID, SyncState> filtering = new HashMap<NodeID, SyncState>();
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
    } else {
      AtomicLong rOrder = ordering.get(nodeid);
      SyncState syncing = null;

      if (rOrder == null) {
        rOrder = new AtomicLong();
        ordering.put(nodeid, rOrder);
        
        if (msg.getType() == ReplicationMessage.START) {
//  do nothing, the server was in standby state when added          
        } else {
          if (msg instanceof PassiveSyncMessage) {
//  passive must have died during passive sync, ignore this message
            return;
          } else {
            logger.info("ignoring " + msg + " target " + nodeid + " no longer exists");
            return;
          }
        }
      } else {
        if (msg.getReplicationType() == SYNC_BEGIN) {
          syncing = new SyncState();
          filtering.put(nodeid, syncing);
        } else {
          syncing = filtering.get(nodeid);
        }
      }
      if (syncing != null) {
//  there is an active sync going on, need to filter out messages that should not be replicated
        if (!syncing.filter(msg)) {
          if (logger.isDebugEnabled()) {
            logger.debug(nodeid + ":Filtering " + msg.getReplicationType() + " for " + msg.getEntityDescriptor().getEntityID() + "/" + msg.getConcurrency());
          }
          if (msg.getType() == ReplicationMessage.REPLICATE) {
            context.release();
          }
          return;
        } else if (msg.getReplicationType() == ReplicationMessage.ReplicationType.SYNC_END) {
          filtering.remove(nodeid);
        }
      }
      try {
        msg.setReplicationID(rOrder.getAndIncrement());
        if (logger.isDebugEnabled()) {
          logger.debug(nodeid + ":Sending " + msg.getReplicationType() + " for " + msg.getEntityID() + "/" + msg.getConcurrency() + "-" + msg.getSequenceID());
        }
        group.sendTo(nodeid, msg);
      }  catch (GroupException ge) {
        logger.info(msg, ge);
      }
    }
  }

  @Override
  protected void initialize(ConfigurationContext context) {
    super.initialize(context);
  }
  
  private static class SyncState {
    private final Set<EntityID> syncd = new HashSet<EntityID>();
    private EntityID syncingID = EntityID.NULL_ID;
    private final Set<Integer> syncdID = new HashSet<Integer>();
    private int syncingConcurrency = -1;
    
    public boolean filter(ReplicationMessage msg) {
      final EntityID eid = msg.getEntityDescriptor().getEntityID();
        switch (msg.getReplicationType()) {
          case SYNC_BEGIN:
            return true;
          case SYNC_ENTITY_BEGIN:
            syncingID = eid;
            syncdID.clear();
            syncingConcurrency = 0;
//  if the entity is created through the create message replication, the entity should not be sync'd
//  set syncid to null so all messages until end get filtered out.  this should not be alot, it just got created
            if (syncd.contains(syncingID)) {
              syncingID = EntityID.NULL_ID;
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
            Assert.assertEquals(syncingID, eid);
            syncd.add(syncingID);
            syncingID = EntityID.NULL_ID;
            return true;
          case SYNC_END:
            return true;
          case CREATE_ENTITY:
            if (syncd.contains(eid) || eid.equals(syncingID)) {
//  this entity is being or has been replicated, don't create it on the passive
              return false;
            } else {
//  add this to the list of entities already syncd
              syncd.add(eid);
              return true;
            }
          case RECONFIGURE_ENTITY:
            if (syncd.contains(eid) || eid.equals(syncingID)) {
//  this entity is being or has been replicated, send the reconfigure through
              return true;
            } else {
//  this entity has not been on the passive yet, reconfigure will go with replication
              return false;
            }
          case DESTROY_ENTITY:
            if (syncd.contains(eid)) {
              return true;
            } else if (syncingID.equals(eid)) {
 //  tricky.  this one needs to pass but only be applied after sync of this entity is complete
              return true;
            } else {
 //  hasn't been syncd yet.  never sync it
              syncd.add(eid);
              return false;
            }
          case INVOKE_ACTION:
            if (syncd.contains(eid)) {
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
          case RELEASE_ENTITY:
            return false;
          default:
            throw new AssertionError("unknown replication message:" + msg);
        }
    }
  }
  
}
