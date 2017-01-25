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
import com.tc.async.api.Sink;
import com.tc.l2.msg.SyncReplicationActivity;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.object.EntityID;
import com.tc.objectserver.entity.MessagePayload;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.terracotta.entity.ConcurrencyStrategy;


public class ReplicationSender extends AbstractEventHandler<NodeID> {
  private static final int DEFAULT_BATCH_LIMIT = 64;
  private static final int DEFAULT_INFLIGHT_MESSAGES = 1;
  
  //  this is all single threaded.  If there is any attempt to make this multi-threaded,
  //  control structures must be fixed
  private final GroupManager<AbstractGroupMessage> group;
  private final Map<NodeID, SyncState> filtering = new HashMap<>();
  private static final TCLogger logger           = TCLogging.getLogger(ReplicationSender.class);
  private static final TCLogger PLOGGER = TCLogging.getLogger(MessagePayload.class);
  private static final boolean debugLogging = logger.isDebugEnabled();
  private static final boolean debugMessaging = PLOGGER.isDebugEnabled();
  private final Map<NodeID, GroupMessageBatchContext> batchContexts = new HashMap<>();
  private Sink<NodeID> selfSink;

  public ReplicationSender(GroupManager<AbstractGroupMessage> group) {
    this.group = group;
  }

  public void setSelfSink(Sink<NodeID> sink) {
    this.selfSink = sink;
  }

  public void removePassive(NodeID dest, Runnable droppedWithoutSend) {
    // this is a flush of the replication channel.  shut it down and return;
    filtering.remove(dest);
    this.batchContexts.remove(dest);
    droppedWithoutSend.run();
  }

  public void addPassive(NodeID dest, SyncReplicationActivity activity, Runnable sent, Runnable droppedWithoutSend) {
    // Set up the sync state.
    createAndRegisterSyncState(dest);
    // Send the message.
    tagAndSendActivityCompletingContext(dest, activity, sent, droppedWithoutSend);
    // Try to flush the message.
    this.selfSink.addSingleThreaded(dest);
  }

  public void replicateMessage(NodeID dest, SyncReplicationActivity activity, Runnable sent, Runnable droppedWithoutSend) {
    SyncState syncing = getSyncState(dest, activity);
    
    // See if the message needs to be filtered out of the stream.
    boolean shouldRemoveFromStream = shouldRemoveActivityFromReplicationStream(activity, syncing);
    if (!shouldRemoveFromStream) {
      // We want to send this message.
      syncing.validateSending(activity);
      tagAndSendActivityCompletingContext(dest, activity, sent, droppedWithoutSend);
      // Try to flush the message.
      this.selfSink.addSingleThreaded(dest);
    } else {
      // We are filtering this out so don't send it.
      // TODO:  Does this message need to be converted to a NOOP to preserve passive-side ordering?
      // Log that this is dropped due to filtering.
      if (debugLogging) {
        logger.debug("FILTERING:" + activity);
      }
      // Call the dropped callback on the context.
      if (null != droppedWithoutSend) {
        droppedWithoutSend.run();
      }
    }
  }

  @Override
  public void handleEvent(NodeID nodeToFlush) throws EventHandlerException {
    try {
      this.batchContexts.get(nodeToFlush).flushBatch();
    } catch (GroupException e) {
      // We can't handle this here, but the next attempt to add to a batch will see the exception from this same
      //  context.
      logger.error("Exception flushing batch context", e);
    }
  }

  private boolean shouldRemoveActivityFromReplicationStream(SyncReplicationActivity activity, SyncState syncing) {
    // By default, we want to filter out messages for which there is no syncing state.
    boolean shouldRemoveFromStream = true;
    if (syncing != null) {
//  first check if the sync has finished, if it has replicate everything
//  if it hasn't, check if the message should be replicated based on sync state.  Do this here in case the message is SYNC_BEGIN
//  if still false, if sync hasn't yet started, replicate and the passive 
//      will either NOOP ack or apply based on it's own state (STANDBY or UNINITIALIZED)
      shouldRemoveFromStream = !(syncing.hasSyncFinished() 
              || syncing.shouldMessageBeReplicated(activity)
              || !syncing.hasSyncBegun());
    }
    return shouldRemoveFromStream;
  }

  private void tagAndSendActivityCompletingContext(NodeID nodeid, SyncReplicationActivity activity, Runnable onSent, Runnable onDroppedWithoutSend) {
    try {
      doSendActivity(nodeid, activity);
      if (null != onSent) {
        onSent.run();
      }
    }  catch (GroupException ge) {
      logger.info(activity, ge);
      onDroppedWithoutSend.run();
    }
  }

  private void doSendActivity(NodeID nodeid, SyncReplicationActivity activity) throws GroupException {
    if (debugLogging) {
      logger.debug("WIRE:" + activity);
    }
    if (debugMessaging) {
      PLOGGER.debug("SENDING:" + activity.getDebugID());
    }
    this.batchContexts.get(nodeid).batchMessage(activity);
  }
  
  private void createAndRegisterSyncState(NodeID nodeid) {
    // We can't already have a state for this passive.
    Assert.assertTrue(!filtering.containsKey(nodeid));
    Assert.assertTrue(!batchContexts.containsKey(nodeid));
    SyncState state = new SyncState();
    filtering.put(nodeid, state);
    // Find out how many messages we should keep in-flight and our maximum batch size.
    int maximumBatchSize = TCPropertiesImpl.getProperties().getInt("active-passive.batchsize", DEFAULT_BATCH_LIMIT);
    int idealMessagesInFlight = TCPropertiesImpl.getProperties().getInt("active-passive.inflight", DEFAULT_INFLIGHT_MESSAGES);
    logger.info("Created batch context for passive " + nodeid + " with max batch size " + maximumBatchSize + " and ideal messages in flight " + idealMessagesInFlight);
    // Create the runnable which will be called, on the network thread, to notify us when a message has been sent.  In
    //  those cases, we want to incur a new flush operation into our internal thread.
    Runnable networkDoneTarget = new Runnable() {
      @Override
      public void run() {
        ReplicationSender.this.selfSink.addSingleThreaded(nodeid);
      }
    };
    this.batchContexts.put(nodeid, new GroupMessageBatchContext(this.group, nodeid, maximumBatchSize, idealMessagesInFlight, networkDoneTarget));
  }

  private SyncState getSyncState(NodeID nodeid, SyncReplicationActivity activity) {
    SyncState state = filtering.get(nodeid);
    if (null == state) {
      // We don't know anything about this passive so drop the message.
      dropActivityForDisconnectedServer(nodeid, activity);
    }
    return state;
  }

  private void dropActivityForDisconnectedServer(NodeID nodeid, SyncReplicationActivity activity) {
//  make sure node is not connected
    Assert.assertFalse("node is not connected for:" + activity, group.isNodeConnected(nodeid));
//  passive must have died during passive sync, ignore this message
    logger.info("ignoring " + activity + " target " + nodeid + " no longer exists");
  }
// for testing only
  boolean isSyncOccuring(NodeID origin) {
    SyncState state = filtering.get(origin);
    if (state != null) {
      return state.isSyncOccuring();
    }
    return false;
  }  
 
  @Override
  protected void initialize(ConfigurationContext context) {
    super.initialize(context);
  }
  
  private static class SyncState {
    // liveSet is the total set of entities which we believe have finished syncing and fully exist on the passive.
    private final Set<EntityID> liveSet = new HashSet<>();
    // syncdID is the set of concurrency keys, of the entity syncingID, which we believe have finished syncing and fully
    //  exist on the passive.
    private final Set<Integer> syncdID = new HashSet<>();
    // syncingID is the entity currently being synced to this passive.
    private EntityID syncingID = EntityID.NULL_ID;
    // syncingConcurrency is the concurrency key we are currently syncing to syncingID, 0 if none is in progress.
    private int syncingConcurrency = -1;
    // begun is true when we decide to start syncing to this passive node (triggered by SYNC_BEGIN).
    boolean begun = false;
    // complete is true when we decide that syncing to this node is now complete (triggered by SYNC_END).
    boolean complete = false;
    private SyncReplicationActivity.ActivityType lastSeen;
    private SyncReplicationActivity.ActivityType lastSent;
    
    public boolean isSyncOccuring() {
      return (begun && !complete);
    }
    
    public boolean hasSyncBegun() {
      return begun;
    }
    
    public boolean hasSyncFinished() {
      return complete;
    }
    
    public boolean shouldMessageBeReplicated(SyncReplicationActivity activity) {
      final EntityID eid = activity.getEntityID();
        
        switch (validateInput(activity)) {
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
            syncingConcurrency = activity.getConcurrency();
            return true;
          case SYNC_ENTITY_CONCURRENCY_PAYLOAD:
            return (syncingID != EntityID.NULL_ID);
          case SYNC_ENTITY_CONCURRENCY_END:
            if (syncingID == EntityID.NULL_ID) {
              return false;
            }
            Assert.assertEquals(syncingConcurrency, activity.getConcurrency());
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
 //  sync is complete, clear all collections and let everything pass
            complete = true;
            liveSet.clear();
            syncdID.clear();
            syncingID = EntityID.NULL_ID;
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
              int concurrencyKey = activity.getConcurrency();
              if (syncingConcurrency == concurrencyKey) {
//  special case.  passive will apply this after sync of the key is complete
                return true;
              }
              return syncdID.contains(concurrencyKey);
            } else {
// hasn't been sync'd yet.  state will be captured in sync
              return false;
            }
          case FLUSH_LOCAL_PIPELINE:
          case ORDERING_PLACEHOLDER:
            // TODO: Should we handle this placeholder a different way - excluding it at this level seems counter-intuitive.
            return false;
          case SYNC_START:
            // SYNC_START shouldn't go down this path - it is handled, explicitly, at a higher level.
          default:
            throw new AssertionError("unknown replication activity:" + activity);
        }
    }

    public SyncReplicationActivity.ActivityType validateInput(SyncReplicationActivity activity) {
      SyncReplicationActivity.ActivityType type = activity.getActivityType();
      if (activity.isSyncActivity()) {
        lastSeen = validate(type, lastSeen);
      }
      return type;
    }
    
    public void validateSending(SyncReplicationActivity activity) {
      if (activity.isSyncActivity()) {
        lastSent = validate(activity.getActivityType(), lastSent);
      }
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
        case SYNC_START:
          // SYNC_START shouldn't go down this path - it is handled, explicitly, at a higher level.
        default:
          throw new AssertionError("unexpected message type");
      }
      return type;
    }
  }
}
