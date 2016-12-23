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
import com.tc.l2.msg.SyncReplicationActivity;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.object.EntityID;
import com.tc.objectserver.entity.MessagePayload;
import com.tc.util.Assert;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.terracotta.entity.ConcurrencyStrategy;


public class ReplicationSender extends AbstractEventHandler<ReplicationEnvelope> {
  //  this is all single threaded.  If there is any attempt to make this multi-threaded,
  //  control structures must be fixed
  private final GroupManager<AbstractGroupMessage> group;
  private final Map<NodeID, SyncState> filtering = new HashMap<>();
  private static final TCLogger logger           = TCLogging.getLogger(ReplicationSender.class);
  private static final TCLogger PLOGGER = TCLogging.getLogger(MessagePayload.class);
  private static final boolean debugLogging = logger.isDebugEnabled();
  private static final boolean debugMessaging = PLOGGER.isDebugEnabled();

  public ReplicationSender(GroupManager<AbstractGroupMessage> group) {
    this.group = group;
  }

  @Override
  public void handleEvent(ReplicationEnvelope context) throws EventHandlerException {
    NodeID nodeid = context.getDestination();
    ReplicationMessage msg = context.getMessage();
    
    if (context.isRemovePassiveMessage()) {
// this is a flush of the replication channel.  shut it down and return;
      filtering.remove(nodeid);
      context.droppedWithoutSend();
    } else if (context.isAddPassiveMessage()) {
      // Set up the sync state.
      SyncState syncing = createAndRegisterSyncState(nodeid);
      // Send the message.
      tagAndSendMessageCompletingContext(context, nodeid, msg, syncing);
    } else {
      SyncState syncing = getSyncState(nodeid, msg);
      
      // See if the message needs to be filtered out of the stream.
      boolean shouldRemoveFromStream = shouldRemoveMessageFromReplicationStream(msg, syncing);
      if (!shouldRemoveFromStream) {
        // We want to send this message.
        syncing.validateSending(msg);
        tagAndSendMessageCompletingContext(context, nodeid, msg, syncing);
      } else {
        // We are filtering this out so don't send it.
        // TODO:  Does this message need to be converted to a NOOP to preserve passive-side ordering?
        // Log that this is dropped due to filtering.
        if (debugLogging) {
          logger.debug("FILTERING:" + msg);
        }
        // Call the dropped callback on the context.
        context.droppedWithoutSend();
      }
    }
    Assert.assertTrue(context.wasSentOrDropped());
  }

  private boolean shouldRemoveMessageFromReplicationStream(ReplicationMessage msg, SyncState syncing) {
    // By default, we want to filter out messages for which there is no syncing state.
    boolean shouldRemoveFromStream = true;
    if (syncing != null) {
      // If there is a valid syncing state, we should only remove this from the stream if the state doesn't think it should be replicated.
      shouldRemoveFromStream = !syncing.shouldMessageBeReplicated(msg);
    }
    return shouldRemoveFromStream;
  }

  private void tagAndSendMessageCompletingContext(ReplicationEnvelope context, NodeID nodeid, ReplicationMessage msg, SyncState syncing) {
    long replicationID = syncing.nextMessageID();
    try {
      doSendMessage(nodeid, msg, replicationID);
      context.sent();
    }  catch (GroupException ge) {
      logger.info(msg, ge);
      context.droppedWithoutSend();
    }
  }

  private void doSendMessage(NodeID nodeid, ReplicationMessage msg, long replicationID) throws GroupException {
    msg.setReplicationID(replicationID);
    if (debugLogging) {
      logger.debug("WIRE:" + msg);
    }
    if (debugMessaging) {
      PLOGGER.debug("SENDING:" + msg.getDebugId());
    }
    group.sendTo(nodeid, msg);
  }
  
  private SyncState createAndRegisterSyncState(NodeID nodeid) {
    // We can't already have a state for this passive.
    Assert.assertTrue(!filtering.containsKey(nodeid));
    SyncState state = new SyncState();
    filtering.put(nodeid, state);
    return state;
  }

  private SyncState getSyncState(NodeID nodeid, ReplicationMessage msg) {
    SyncState state = filtering.get(nodeid);
    if (null == state) {
      // We don't know anything about this passive so drop the message.
      dropMessageForDisconnectedServer(nodeid, msg);
    }
    return state;
  }

  private void dropMessageForDisconnectedServer(NodeID nodeid, ReplicationMessage msg) {
//  make sure node is not connected
    Assert.assertFalse("node is not connected for:" + msg, group.isNodeConnected(nodeid));
//  passive must have died during passive sync, ignore this message
    logger.info("ignoring " + msg + " target " + nodeid + " no longer exists");
  }

  @Override
  protected void initialize(ConfigurationContext context) {
    super.initialize(context);
  }
  
  private static class SyncState {
    private final Set<EntityID> liveSet = new HashSet<>();
    private final Set<Integer> syncdID = new HashSet<>();
    private EntityID syncingID = EntityID.NULL_ID;
    private int syncingConcurrency = -1;
    boolean begun = false;
    private SyncReplicationActivity.ActivityType lastSeen;
    private SyncReplicationActivity.ActivityType lastSent;
    private long messageId;
    
    public boolean shouldMessageBeReplicated(ReplicationMessage msg) {
      final EntityID eid = msg.getEntityDescriptor().getEntityID();
        
        switch (validateInput(msg)) {
          case SYNC_BEGIN:
            begun = true;
            return true;
          case SYNC_ENTITY_BEGIN:
            syncingID = eid;
            syncdID.clear();
            syncdID.add(ConcurrencyStrategy.MANAGEMENT_KEY);
            syncdID.add(ConcurrencyStrategy.UNIVERSAL_KEY);
            syncingConcurrency = 0;
//  if the entity is created through the create message replication, the entity should not be sync'd
//  set syncid to null so all messages until end get filtered out.  this should not be alot, it just got created
            if (liveSet.contains(syncingID)) {
              syncingID = EntityID.NULL_ID;
              if (debugLogging) {
                logger.debug("Drop: entity " + syncingID + " was created no sync required");
              }
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
            return (syncingID != EntityID.NULL_ID);
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
            liveSet.add(syncingID);
            syncingID = EntityID.NULL_ID;
            return true;
          case SYNC_END:
            return true;
          case CREATE_ENTITY:
// if this create came through, it is not part of the snapshot set so everything
// applies
            if (begun) {
              liveSet.add(eid);
            }
//  fall-through
          case RECONFIGURE_ENTITY:
          case FETCH_ENTITY:
          case RELEASE_ENTITY:
          case DESTROY_ENTITY:
            return begun;
          case INVOKE_ACTION:
            if (liveSet.contains(eid)) {
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

    public SyncReplicationActivity.ActivityType validateInput(ReplicationMessage msg) {
      SyncReplicationActivity.ActivityType type = msg.getReplicationType();
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
    
    public long nextMessageID() {
      return messageId++;
    }
    
    private SyncReplicationActivity.ActivityType validate(SyncReplicationActivity.ActivityType type, SyncReplicationActivity.ActivityType compare) {
      switch (type) {
        case SYNC_BEGIN:
          Assert.assertNull(compare);
          break;
        case SYNC_ENTITY_BEGIN:
          Assert.assertTrue(type + " " + compare, EnumSet.of(SyncReplicationActivity.ActivityType.SYNC_BEGIN, SyncReplicationActivity.ActivityType.SYNC_ENTITY_END).contains(compare));
          break;
        case SYNC_ENTITY_CONCURRENCY_BEGIN:
          Assert.assertTrue(type + " " + compare, EnumSet.of(SyncReplicationActivity.ActivityType.SYNC_ENTITY_BEGIN, SyncReplicationActivity.ActivityType.SYNC_ENTITY_CONCURRENCY_END).contains(compare));
          break;
        case SYNC_ENTITY_CONCURRENCY_PAYLOAD:
          Assert.assertTrue(type + " " + compare, EnumSet.of(SyncReplicationActivity.ActivityType.SYNC_ENTITY_CONCURRENCY_BEGIN, SyncReplicationActivity.ActivityType.SYNC_ENTITY_CONCURRENCY_PAYLOAD).contains(compare));
          break;
        case SYNC_ENTITY_CONCURRENCY_END:
          Assert.assertTrue(type + " " + compare, EnumSet.of(SyncReplicationActivity.ActivityType.SYNC_ENTITY_CONCURRENCY_BEGIN, SyncReplicationActivity.ActivityType.SYNC_ENTITY_CONCURRENCY_PAYLOAD).contains(compare));
          break;
        case SYNC_ENTITY_END:
          Assert.assertTrue(type + " " + compare, EnumSet.of(SyncReplicationActivity.ActivityType.SYNC_ENTITY_BEGIN, SyncReplicationActivity.ActivityType.SYNC_ENTITY_CONCURRENCY_END).contains(compare));
          break;
        case SYNC_END:
          Assert.assertTrue(type + " " + compare, EnumSet.of(SyncReplicationActivity.ActivityType.SYNC_ENTITY_END, SyncReplicationActivity.ActivityType.SYNC_BEGIN).contains(compare));
          break;
        default:
          throw new AssertionError("unexpected message type");
      }
      return type;
    }
  }
}
