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
import com.tc.l2.msg.ReplicationMessage;
import com.tc.l2.msg.SyncReplicationActivity;
import com.tc.net.NodeID;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.object.FetchID;
import com.tc.objectserver.entity.MessagePayload;
import com.tc.objectserver.handler.GroupMessageBatchContext.IBatchableMessageFactory;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.entity.ConcurrencyStrategy;


public class ReplicationSender extends AbstractEventHandler<NodeID> {
  private static final int DEFAULT_BATCH_LIMIT = 64;
  private static final int DEFAULT_INFLIGHT_MESSAGES = 1;
  
  //  this is all single threaded.  If there is any attempt to make this multi-threaded,
  //  control structures must be fixed
  private final GroupManager<AbstractGroupMessage> group;
  private final Map<NodeID, SyncState> filtering = new HashMap<>();
  private static final Logger logger = LoggerFactory.getLogger(ReplicationSender.class);
  private static final Logger PLOGGER = LoggerFactory.getLogger(MessagePayload.class);
  private static final boolean debugLogging = logger.isDebugEnabled();
  private static final boolean debugMessaging = PLOGGER.isDebugEnabled();
  private final Map<NodeID, GroupMessageBatchContext<ReplicationMessage, SyncReplicationActivity>> batchContexts = new ConcurrentHashMap<>();
  private final Collection<Consumer<NodeID>> failureListeners = new CopyOnWriteArrayList<>();
  private Sink<NodeID> selfSink;

  public ReplicationSender(GroupManager<AbstractGroupMessage> group) {
    this.group = group;
  }
  
  public void addFailedToSendListener(Consumer<NodeID> failed) {
    failureListeners.add(failed);
  }
  
  private void notifySendFailure(NodeID node) {
    failureListeners.forEach(c->c.accept(node));
  }

  public void setSelfSink(Sink<NodeID> sink) {
    this.selfSink = sink;
  }

  public void removePassive(NodeID dest) {
    // this is a flush of the replication channel.  shut it down and return;
    synchronized (this) {
      filtering.remove(dest);
    }
    this.batchContexts.remove(dest);
  }

  public void addPassive(NodeID dest, SyncReplicationActivity activity) {
    // Set up the sync state.
    synchronized (this) {
      createAndRegisterSyncState(dest);
    }
    // Send the message.
    if (doSendActivity(dest, activity)) {
    // Try to flush the message.
      this.selfSink.addSingleThreaded(dest);
    }
  }

  public boolean replicateMessage(NodeID dest, SyncReplicationActivity activity) {
    SyncState syncing = getSyncState(dest, activity);
    
    boolean didSend = false;
    // See if the message needs to be filtered out of the stream.
    boolean shouldRemoveFromStream = shouldRemoveActivityFromReplicationStream(activity, syncing);
    if (!shouldRemoveFromStream) {
      // We want to send this message.
      syncing.validateSending(activity);

      if (doSendActivity(dest, activity)) {  
        // We were able to add the message to the batch so try to flush it.
        this.selfSink.addSingleThreaded(dest);
      }
      didSend = true;
    } else {
      // We are filtering this out so don't send it.
      // TODO:  Does this message need to be converted to a NOOP to preserve passive-side ordering?
      // Log that this is dropped due to filtering.
      if (debugLogging) {
        logger.debug("FILTERING:" + activity);
      }
    }
    // If we didn't filter the message or trigger an exception, we sent/batched it so let the caller know.
    return didSend;
  }

  @Override
  public void handleEvent(NodeID nodeToFlush) throws EventHandlerException {
    try {
      GroupMessageBatchContext cxt = this.batchContexts.get(nodeToFlush);
      if (cxt != null) {
        cxt.flushBatch();
      } else {
        notifySendFailure(nodeToFlush);
      }
    } catch (GroupException e) {
      logger.error("Exception flushing batch context", e);
      notifySendFailure(nodeToFlush);
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

  private boolean doSendActivity(NodeID nodeid, SyncReplicationActivity activity) {
    if (debugLogging) {
      logger.debug("WIRE:" + activity);
    }
    if (debugMessaging) {
      PLOGGER.debug("SENDING:" + activity.getDebugID());
    }
    return this.batchContexts.get(nodeid).batchMessage(activity);
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
    
    IBatchableMessageFactory<ReplicationMessage, SyncReplicationActivity> factory = new IBatchableMessageFactory<ReplicationMessage, SyncReplicationActivity>() {
      @Override
      public ReplicationMessage createNewBatch(SyncReplicationActivity initialActivity, long id) {
        ReplicationMessage message = ReplicationMessage.createActivityContainer(initialActivity);
        message.setReplicationID(id);
        return message;
      }
    };
    this.batchContexts.put(nodeid, new GroupMessageBatchContext<>(factory, this.group, nodeid, maximumBatchSize, idealMessagesInFlight, networkDoneTarget));
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
    private final Set<FetchID> liveFetch = new HashSet<>();
    // syncdID is the set of concurrency keys, of the entity syncingID, which we believe have finished syncing and fully
    //  exist on the passive.
    private final Set<Integer> syncdID = new HashSet<>();
    // syncingID is the entity currently being synced to this passive.
    private FetchID syncingFetch = FetchID.NULL_ID;
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
        switch (validateInput(activity)) {
          case SYNC_BEGIN:
            begun = true;
            return true;
          case SYNC_ENTITY_BEGIN:
            if (liveFetch.contains(activity.getFetchID())) {
              return false;
            } else {
              syncingFetch = activity.getFetchID();
              syncdID.clear();
              syncdID.add(ConcurrencyStrategy.MANAGEMENT_KEY);
              syncdID.add(ConcurrencyStrategy.UNIVERSAL_KEY);
              syncingConcurrency = 0;
              return true;
            }
          case SYNC_ENTITY_CONCURRENCY_BEGIN:
            if (syncingFetch.equals(activity.getFetchID())) {
              Assert.assertEquals(syncingConcurrency, 0);
              syncingConcurrency = activity.getConcurrency();
              return true;
            } else {
              return false;
            }
          case SYNC_ENTITY_CONCURRENCY_PAYLOAD:
            if (syncingFetch.equals(activity.getFetchID())) {
              return true;
            } else {
              return false;
            }
          case SYNC_ENTITY_CONCURRENCY_END:
            if (syncingFetch.equals(activity.getFetchID())) {
              syncdID.add(syncingConcurrency);
              syncingConcurrency = 0;
              return true;
            } else {
              return false;
            }
          case SYNC_ENTITY_END:
            if (syncingFetch.equals(activity.getFetchID())) {
              liveFetch.add(syncingFetch);
              syncingFetch = FetchID.NULL_ID;
              return true;
            } else {
              return false;
            }
          case SYNC_END:
 //  sync is complete, clear all collections and let everything pass
            complete = true;
            liveFetch.clear();
            syncdID.clear();
            syncingFetch = FetchID.NULL_ID;
            return true;
          case CREATE_ENTITY:
// if this create came through, it is not part of the snapshot set so everything
// applies
            if (begun) {
              liveFetch.add(activity.getFetchID());
            }
//  fall-through
          case RECONFIGURE_ENTITY:
          case FETCH_ENTITY:
          case RELEASE_ENTITY:
          case DESTROY_ENTITY:
            return begun;
          case INVOKE_ACTION:
            if (liveFetch.contains(activity.getFetchID())) {
              return true;
            } else if (syncingFetch.equals(activity.getFetchID())) {
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
          case LOCAL_ENTITY_GC:
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
