/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.objectserver;

import com.tc.async.api.Sink;
import com.tc.l2.context.SyncIndexesRequest;
import com.tc.l2.context.SyncObjectsRequest;
import com.tc.l2.ha.L2HAZapNodeRequestProcessor;
import com.tc.l2.msg.GCResultMessage;
import com.tc.l2.msg.GCResultMessageFactory;
import com.tc.l2.msg.IndexSyncCompleteAckMessage;
import com.tc.l2.msg.IndexSyncCompleteMessage;
import com.tc.l2.msg.IndexSyncMessageFactory;
import com.tc.l2.msg.IndexSyncStartMessage;
import com.tc.l2.msg.ObjectListSyncMessage;
import com.tc.l2.msg.ObjectListSyncMessageFactory;
import com.tc.l2.msg.ObjectSyncCompleteAckMessage;
import com.tc.l2.msg.ObjectSyncCompleteMessage;
import com.tc.l2.msg.ObjectSyncCompleteMessageFactory;
import com.tc.l2.state.StateManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.net.groups.GroupMessage;
import com.tc.net.groups.GroupMessageListener;
import com.tc.net.groups.GroupResponse;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.context.DGCResultContext;
import com.tc.objectserver.dgc.api.GarbageCollectionInfo;
import com.tc.objectserver.dgc.impl.GarbageCollectorEventListenerAdapter;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.objectserver.tx.TransactionBatchContext;
import com.tc.objectserver.tx.TxnsInSystemCompletionListener;
import com.tc.util.Assert;
import com.tc.util.Conversion;
import com.tc.util.ObjectIDSet;
import com.tc.util.State;
import com.tc.util.TCCollections;
import com.tc.util.sequence.SequenceGenerator;
import com.tc.util.sequence.SequenceGenerator.SequenceGeneratorException;
import com.terracottatech.config.Offheap;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicReference;

public class ReplicatedObjectManagerImpl implements ReplicatedObjectManager, GroupMessageListener,
    L2ObjectStateListener, L2IndexStateListener {

  private static final TCLogger              logger = TCLogging.getLogger(ReplicatedObjectManagerImpl.class);

  private final ObjectManager                objectManager;
  private final GroupManager                 groupManager;
  private final StateManager                 stateManager;
  private final ServerTransactionManager     transactionManager;
  private final Sink                         objectsSyncRequestSink;
  private final Sink                         indexSyncRequestSink;
  private final Sink                         transactionRelaySink;
  private final SequenceGenerator            sequenceGenerator;
  private final SequenceGenerator            indexSequenceGenerator;
  private final GCMonitor                    gcMonitor;
  private final boolean                      isCleanDB;
  private final L2PassiveSyncStateManager    passiveSyncStateManager;
  private final L2ObjectStateManager         l2ObjectStateManager;
  private final Offheap                      offheapConfig;

  private final AtomicReference<NodeID>      syncedFrom = new AtomicReference<NodeID>();

  public ReplicatedObjectManagerImpl(final GroupManager groupManager, final StateManager stateManager,
                                     final L2PassiveSyncStateManager l2PassiveSyncStateManager,
                                     L2ObjectStateManager l2ObjectStateManager,
                                     final ObjectManager objectManager,
                                     final ServerTransactionManager transactionManager,
                                     final Sink objectsSyncRequestSink, final Sink indexSyncRequestSink,
                                     final Sink transactionRelaySink, final SequenceGenerator sequenceGenerator,
                                     final SequenceGenerator indexSequenceGenerator, final boolean isCleanDB, final Offheap offheapConfig) {
    this.groupManager = groupManager;
    this.stateManager = stateManager;
    this.objectManager = objectManager;
    this.transactionManager = transactionManager;
    this.objectsSyncRequestSink = objectsSyncRequestSink;
    this.indexSyncRequestSink = indexSyncRequestSink;
    this.transactionRelaySink = transactionRelaySink;
    this.sequenceGenerator = sequenceGenerator;
    this.indexSequenceGenerator = indexSequenceGenerator;
    this.gcMonitor = new GCMonitor();
    this.objectManager.getGarbageCollector().addListener(this.gcMonitor);
    this.groupManager.registerForMessages(ObjectListSyncMessage.class, this);
    this.groupManager.registerForMessages(ObjectSyncCompleteAckMessage.class, this);
    this.groupManager.registerForMessages(IndexSyncCompleteAckMessage.class, this);
    this.isCleanDB = isCleanDB;
    this.passiveSyncStateManager = l2PassiveSyncStateManager;
    this.l2ObjectStateManager = l2ObjectStateManager;
    this.offheapConfig = offheapConfig;
  }

  /**
   * This method is used to sync up all ObjectIDs from the remote ObjectManagers. It is synchronous and after when it
   * returns nobody is allowed to join the cluster with existing objects.
   */
  @Override
  public void sync() {
    try {
      final GroupResponse gr = this.groupManager.sendAllAndWaitForResponse(ObjectListSyncMessageFactory
          .createObjectListSyncRequestMessage());
      final Map<NodeID, SyncingPassiveValue> nodeIDSyncingPassives = new LinkedHashMap<NodeID, SyncingPassiveValue>();
      for (GroupMessage groupMessage : gr.getResponses()) {
        final ObjectListSyncMessage msg = (ObjectListSyncMessage) groupMessage;
        if (msg.getType() == ObjectListSyncMessage.RESPONSE) {
          State curState = msg.getCurrentState();
          // Zap all uninitialized passives joining with # of objects > 0
          if (StateManager.PASSIVE_UNINITIALIZED.equals(curState) && !msg.isSyncAllowed()) {
            logger.error("Syncing to partially synced passives not supported, msg: " + msg);
            this.groupManager.zapNode(msg.messageFrom(), L2HAZapNodeRequestProcessor.PARTIALLY_SYNCED_PASSIVE_JOINED,
                                      "Passive : " + msg.messageFrom() + " joined in partially synced state. "
                                          + L2HAZapNodeRequestProcessor.getErrorString(new Throwable()));
          } else if (StateManager.PASSIVE_UNINITIALIZED.equals(curState) && !msg.isCleanDB()) {
            logger.error("Syncing to passives which were restarted before active is not supported. msg : " + msg);
            this.groupManager.zapNode(msg.messageFrom(), L2HAZapNodeRequestProcessor.NODE_JOINED_WITH_DIRTY_DB,
                "Passive : " + msg.messageFrom() + " was restarted before active." +
                L2HAZapNodeRequestProcessor.getErrorString(new Throwable()));
          } else if (checkForSufficientResources(msg)) {
            nodeIDSyncingPassives.put(msg.messageFrom(), new SyncingPassiveValue(new ObjectIDSet(), curState));
          }
        } else {
          logger.error("Received wrong response for ObjectListSyncMessage Request  from " + msg.messageFrom()
                       + " : msg : " + msg);
          this.groupManager.zapNode(msg.messageFrom(),
                                    L2HAZapNodeRequestProcessor.PROGRAM_ERROR,
                                    "Recd wrong response from : " + msg.messageFrom()
                                        + " for ObjectListSyncMessage Request"
                                        + L2HAZapNodeRequestProcessor.getErrorString(new Throwable()));
        }
      }

      if (!nodeIDSyncingPassives.isEmpty()) {
        this.gcMonitor.disableAndAdd2L2StateManager(nodeIDSyncingPassives);
      }
    } catch (final GroupException e) {
      logger.error(e);
      throw new AssertionError(e);
    }
  }

  // Query current state of the other L2
  @Override
  public void query(final NodeID nodeID) throws GroupException {
    this.groupManager.sendTo(nodeID, ObjectListSyncMessageFactory.createObjectListSyncRequestMessage());
  }

  @Override
  public void clear(final NodeID nodeID) {
    this.passiveSyncStateManager.removeL2(nodeID);
    this.gcMonitor.clear(nodeID);
  }

  @Override
  public void messageReceived(final NodeID fromNode, final GroupMessage msg) {
    if (msg instanceof ObjectListSyncMessage) {
      final ObjectListSyncMessage clusterMsg = (ObjectListSyncMessage) msg;
      handleClusterObjectMessage(fromNode, clusterMsg);
    } else if (msg instanceof ObjectSyncCompleteAckMessage) {
      NodeID nodeID = msg.messageFrom();
      logger.info("Received ObjectSyncCompleteAckMessage from " + nodeID);
      this.passiveSyncStateManager.objectSyncComplete(nodeID);
      moveNodeToPassiveStandByIfPossible(nodeID);
    } else if (msg instanceof IndexSyncCompleteAckMessage) {
      NodeID nodeID = msg.messageFrom();
      logger.info("Received IndexSyncCompleteAckMessage from " + nodeID);
      indexesInSyncOnNode(nodeID);
    } else {
      throw new AssertionError("ReplicatedObjectManagerImpl : Received wrong message type :" + msg.getClass().getName()
                               + " : " + msg);
    }
  }

  private void moveNodeToPassiveStandByIfPossible(NodeID nodeID) {
    if (this.passiveSyncStateManager.isSyncComplete(nodeID)) {
      this.gcMonitor.syncCompleteFor(nodeID);
      this.stateManager.moveNodeToPassiveStandby(nodeID);
    }
  }

  @Override
  public void handleGCResult(final GCResultMessage gcMsg) {
    final SortedSet gcedOids = gcMsg.getGCedObjectIDs();
    if (this.stateManager.isActiveCoordinator()) {
      logger.warn("Received DGC Result from " + gcMsg.messageFrom() + " While this node is ACTIVE. Ignoring result : "
                  + gcMsg);
      return;
    }
    this.objectManager.getGarbageCollector().deleteGarbage(new DGCResultContext(gcedOids, gcMsg.getGCInfo()));
  }

  private void handleClusterObjectMessage(final NodeID nodeID, final ObjectListSyncMessage clusterMsg) {
    try {
      switch (clusterMsg.getType()) {
        case ObjectListSyncMessage.REQUEST:
          handleObjectListRequest(nodeID, clusterMsg);
          break;
        case ObjectListSyncMessage.RESPONSE:
          handleObjectListResponse(nodeID, clusterMsg);
          break;
        case ObjectListSyncMessage.FAILED_RESPONSE:
          handleObjectListFailedResponse(nodeID, clusterMsg);
          break;
        default:
          throw new AssertionError("This message shouldn't have been routed here : " + clusterMsg);
      }
    } catch (final GroupException e) {
      logger.error("Error handling message : " + clusterMsg, e);
      throw new AssertionError(e);
    }
  }

  private void handleObjectListFailedResponse(final NodeID nodeID, final ObjectListSyncMessage clusterMsg) {
    final String error = "Received wrong response from " + nodeID + " for Object List Query : " + clusterMsg;
    logger.error(error + " Forcing node to Quit !!");
    this.groupManager.zapNode(nodeID, L2HAZapNodeRequestProcessor.PROGRAM_ERROR,
                              error + L2HAZapNodeRequestProcessor.getErrorString(new Throwable()));
  }

  private void handleObjectListResponse(final NodeID nodeID, final ObjectListSyncMessage clusterMsg) {
    Assert.assertTrue(this.stateManager.isActiveCoordinator());


    if (!clusterMsg.isSyncAllowed() || !clusterMsg.isCleanDB()) {
      final StringBuilder error = new StringBuilder();
      if (!clusterMsg.isCleanDB()) {
        error.append("Node with a stale Database is trying to join the cluster. isCleanDB: " + clusterMsg.isCleanDB());
      } else {
        error.append("Node joined after being partially synced with another active.");
      }
      logger.error(error.toString() + " Forcing node to Quit !!");
      this.groupManager.zapNode(nodeID, L2HAZapNodeRequestProcessor.NODE_JOINED_WITH_DIRTY_DB,
                                error + L2HAZapNodeRequestProcessor.getErrorString(new Throwable()));
    } else {
      // DEV-1944 : We don't want newly joined nodes to be syncing the Objects while the active is receiving the re-sent
      // transactions. If we do that there is a race where passive can apply already applied transactions twice.
      // XXX:: 3 passives - partial sync.
      if (!checkForSufficientResources(clusterMsg)) {
        return;   // Only check for sufficient resources on the new passive if it was shut down after the active and is not a candidate for zapping due to dirty db.
      }
      ReplicatedObjectManagerImpl.this.transactionManager
          .callBackOnResentTxnsInSystemCompletion(new TxnsInSystemCompletionListener() {
            @Override
            public void onCompletion() {
              ReplicatedObjectManagerImpl.this.gcMonitor.add2L2StateManagerWhenGCDisabled(nodeID,
                                                                                          new ObjectIDSet(),
                                                                                          clusterMsg.getCurrentState());
            }
          });
    }
  }

  private boolean add2L2StateManager(final NodeID nodeID, final State currentState, final Set<ObjectID> oids) {
    return this.passiveSyncStateManager.addL2(nodeID, oids, currentState);
  }

  @Override
  public void missingObjectsFor(final NodeID nodeID, final int missingObjects) {
    if (missingObjects == 0) {
      this.passiveSyncStateManager.objectSyncComplete(nodeID);
      moveNodeToPassiveStandByIfPossible(nodeID);
    } else {
      final Runnable syncRunnable = new Runnable() {
        @Override
        public void run() {
          objectsSyncRequestSink.add(new SyncObjectsRequest(nodeID));
        }
      };
      this.l2ObjectStateManager.initiateSync(nodeID, syncRunnable);
    }
  }

  @Override
  public void objectSyncCompleteFor(final NodeID nodeID) {
    try {
      logger.info("Object Sync completed for " + nodeID);
      final ObjectSyncCompleteMessage msg = ObjectSyncCompleteMessageFactory
          .createObjectSyncCompleteMessageFor(nodeID, this.sequenceGenerator.getNextSequence(nodeID));
      this.groupManager.sendTo(nodeID, msg);
    } catch (final GroupException e) {
      logger.error("Error Sending Object Sync complete message  to : " + nodeID, e);
      this.groupManager.zapNode(nodeID,
                                L2HAZapNodeRequestProcessor.COMMUNICATION_ERROR,
                                "Error sending Object Sync complete message "
                                    + L2HAZapNodeRequestProcessor.getErrorString(e));
    } catch (final SequenceGeneratorException e) {
      logger.error("Error Sending Object Sync complete message  to : " + nodeID, e);
    }
  }

  @Override
  public void indexSyncStartFor(NodeID nodeID) {
    try {
      logger.info("Index Sync started for " + nodeID);
      final IndexSyncStartMessage msg = IndexSyncMessageFactory.createIndexSyncStartMessage(this.indexSequenceGenerator
          .getNextSequence(nodeID));
      this.groupManager.sendTo(nodeID, msg);
    } catch (final GroupException e) {
      logger.error("Error Sending Index Sync Start message  to : " + nodeID, e);
      this.groupManager.zapNode(nodeID,
                                L2HAZapNodeRequestProcessor.COMMUNICATION_ERROR,
                                "Error sending Index Sync Start message "
                                    + L2HAZapNodeRequestProcessor.getErrorString(e));
      clear(nodeID);
    } catch (SequenceGeneratorException e) {
      logger.error("Error Sending Index Sync Start message  to : " + nodeID, e);
      this.groupManager.zapNode(nodeID,
                                L2HAZapNodeRequestProcessor.COMMUNICATION_ERROR,
                                "Error sending Index Sync Start message "
                                    + L2HAZapNodeRequestProcessor.getErrorString(e));
      clear(nodeID);
    }

  }

  @Override
  public void indexFilesFor(NodeID nodeID, int indexFiles) {
    if (indexFiles == 0) {
      indexSyncCompleteFor(nodeID);
    } else {
      this.indexSyncRequestSink.add(new SyncIndexesRequest(nodeID));
    }
  }

  @Override
  public void indexesInSyncOnNode(NodeID node) {
    this.passiveSyncStateManager.indexSyncComplete(node);
    moveNodeToPassiveStandByIfPossible(node);
  }

  @Override
  public void indexSyncCompleteFor(NodeID nodeID) {
    try {
      logger.info("Index Sync completed for " + nodeID);
      final IndexSyncCompleteMessage msg = IndexSyncMessageFactory
          .createIndexSyncCompleteMessage(this.indexSequenceGenerator.getNextSequence(nodeID));
      this.groupManager.sendTo(nodeID, msg);
    } catch (final GroupException e) {
      logger.error("Error Sending Index Sync complete message  to : " + nodeID, e);
      this.groupManager.zapNode(nodeID,
                                L2HAZapNodeRequestProcessor.COMMUNICATION_ERROR,
                                "Error sending Index Sync complete message "
                                    + L2HAZapNodeRequestProcessor.getErrorString(e));
    } catch (final SequenceGeneratorException e) {
      logger.error("Error Sending Index Sync complete message  to : " + nodeID, e);
    }

  }

  /**
   * ACTIVE queries PASSIVES for the list of known object ids and this response is the one that opens up the
   * transactions from ACTIVE to PASSIVE. So the replicated transaction manager is initialized here.
   */
  private void handleObjectListRequest(final NodeID nodeID, final ObjectListSyncMessage clusterMsg)
      throws GroupException {
    if (!this.stateManager.isActiveCoordinator()) {
      transactionManager.callBackOnTxnsInSystemCompletion(new TxnsInSystemCompletionListener() {
        @Override
        public void onCompletion() {
          // Handle requests multiple list requests from the same instance of the active. This could happen when the active
          // first starts up. This node joining could trigger one object list request while the active running the sync() method
          // will trigger the other.
          boolean syncAllowed = syncedFrom.compareAndSet(null, clusterMsg.messageFrom()) || syncedFrom.get().equals(clusterMsg.messageFrom());
          logger.info("Send response to Active's query : syncAllowed = " + syncAllowed + " isCleanDB="
                      + isCleanDB + " currentState=" + clusterMsg.getCurrentState() + " offheapEnabled=" + offheapConfig.getEnabled() +
                      " resource total=" + getResourceTotal());
          try {
            groupManager.sendTo(nodeID, ObjectListSyncMessageFactory
                .createObjectListSyncResponseMessage(clusterMsg, stateManager.getCurrentState(), syncAllowed,
                    isCleanDB, offheapConfig.getEnabled(), getResourceTotal()));
          } catch (GroupException e) {
            logger.error("Failed to send object list response to the active.", e);
            throw new AssertionError(e);
          }
        }
      });
    } else {
      logger.error("Recd. ObjectListRequest when in ACTIVE state from " + nodeID + ". Zapping node ...");
      this.groupManager.sendTo(nodeID,
                               ObjectListSyncMessageFactory.createObjectListSyncFailedResponseMessage(clusterMsg));
      // Now ZAP the node
      this.groupManager.zapNode(nodeID, L2HAZapNodeRequestProcessor.SPLIT_BRAIN,
                                "Recd ObjectListRequest from : " + nodeID + " while in ACTIVE-COORDINATOR state"
                                    + L2HAZapNodeRequestProcessor.getErrorString(new Throwable()));
    }
  }

  private long getResourceTotal() {
    if (offheapConfig.getEnabled()) {
      try {
        return Conversion.memorySizeAsLongBytes(offheapConfig.getMaxDataSize());
      } catch (Conversion.MetricsFormatException e) {
        throw new RuntimeException(e);
      }
    } else {
      // If offheap is turned off we use heap
      return Runtime.getRuntime().maxMemory();
    }
  }

  private boolean checkForSufficientResources(ObjectListSyncMessage response) {
    if (response.getType() != ObjectListSyncMessage.RESPONSE) {
      throw new AssertionError();
    }

    // We don't want to allow a passive with less than the active's resources to join the cluster. That could lead
    // to the passive crashing randomly at some point in the future, or maybe even during passive sync.
    if (offheapConfig.getEnabled() && (!response.isOffheapEnabled() || getResourceTotal() > response.getResourceSize())) {
      groupManager.zapNode(response.messageFrom(), L2HAZapNodeRequestProcessor.INSUFFICIENT_RESOURCES,
          "Node " + response.messageFrom() + " does not have enough offheap space to join the cluster. Required " + getResourceTotal() + " got " + response.getResourceSize());
      return false;
    } else if (!offheapConfig.getEnabled() && response.isOffheapEnabled() && getResourceTotal() > response.getResourceSize()) {
      // Allow an upgrade from heap to offheap in the passive
      groupManager.zapNode(response.messageFrom(), L2HAZapNodeRequestProcessor.INSUFFICIENT_RESOURCES,
          "Node " + response.messageFrom() + " does not have enough offheap space to join the cluster. Required " + getResourceTotal() + " got " + response.getResourceSize());
      return false;
    } else if (!offheapConfig.getEnabled() && !response.isOffheapEnabled() && getResourceTotal() > response.getResourceSize()) {
      groupManager.zapNode(response.messageFrom(), L2HAZapNodeRequestProcessor.INSUFFICIENT_RESOURCES,
          "Node " + response.messageFrom() + " does not have enough heap space to join the cluster. Required " + getResourceTotal() + " got " + response.getResourceSize());
      return false;
    }
    return true;
  }

  @Override
  public void relayTransactions(final TransactionBatchContext transactionBatchContext) {
    if (passiveSyncStateManager.getL2Count() > 0) {
      transactionRelaySink.add(transactionBatchContext);
    } else {
      transactionManager.transactionsRelayed(transactionBatchContext.getSourceNodeID(), transactionBatchContext.getTransactionIDs());
    }
  }

  private static final SyncingPassiveValue ADDED = new SyncingPassiveValue();

  public final class GCMonitor extends GarbageCollectorEventListenerAdapter {

    boolean                          disabled        = false;
    Map<NodeID, SyncingPassiveValue> syncingPassives = new HashMap<NodeID, SyncingPassiveValue>();

    @Override
    public void garbageCollectorCycleCompleted(final GarbageCollectionInfo info, final ObjectIDSet toDeleted) {
      Map<NodeID, SyncingPassiveValue> toAdd = null;
      notifyGCResultToPassives(info, toDeleted);
      boolean havePassivesToSync = false;
      synchronized (this) {
        if (this.syncingPassives.isEmpty()) { return; }
        toAdd = new LinkedHashMap<NodeID, SyncingPassiveValue>();
        for (Entry<NodeID, SyncingPassiveValue> e : this.syncingPassives.entrySet()) {
          if (e.getValue() != ADDED) {
            final NodeID nodeID = e.getKey();
            logger.info("DGC Completed : Starting scheduled passive sync for " + nodeID);
            toAdd.put(nodeID, e.getValue());
            e.setValue(ADDED);
            havePassivesToSync = true;
          }
        }
      }
      if (havePassivesToSync) {
        disableGC();
        assertGCDisabled();
        add2L2StateManager(toAdd);
      }
    }

    private void notifyGCResultToPassives(final GarbageCollectionInfo gcInfo, final ObjectIDSet deleted) {
      if (deleted.isEmpty()) { return; }
      final GCResultMessage msg = GCResultMessageFactory.createGCResultMessage(gcInfo, deleted);
      final long id = gcInfo.getIteration();
      ReplicatedObjectManagerImpl.this.transactionManager
          .callBackOnTxnsInSystemCompletion(new TxnsInSystemCompletionListener() {
            @Override
            public void onCompletion() {
              ReplicatedObjectManagerImpl.this.groupManager.sendAll(msg);
            }

            @Override
            public String toString() {
              return "com.tc.l2.objectserver.ReplicatedObjectManagerImpl.GCMonitor ( " + id + " ) : DGC result size = "
                     + deleted.size();
            }
          });
    }

    private void add2L2StateManager(final Map<NodeID, SyncingPassiveValue> toAdd) {
      for (Entry<NodeID, SyncingPassiveValue> e : toAdd.entrySet()) {
        final NodeID nodeID = e.getKey();
        final SyncingPassiveValue value = e.getValue();
        if (!ReplicatedObjectManagerImpl.this.add2L2StateManager(nodeID, value.getCurrentState(), value.getOids())) {
          logger.warn(nodeID + " is already added to L2StateManager, clearing our internal data structures.");
          syncCompleteFor(nodeID);
        }
      }
    }

    private void disableGCIfPossible() {
      if (!this.disabled) {
        this.disabled = ReplicatedObjectManagerImpl.this.objectManager.getGarbageCollector().requestDisableGC();
        logger.info((this.disabled ? "DGC is disabled." : "DGC is not disabled."));
      }
    }

    private void disableGC() {
      ReplicatedObjectManagerImpl.this.objectManager.getGarbageCollector().waitToDisableGC();
      this.disabled = true;
    }

    private void assertGCDisabled() {
      if (!this.disabled) { throw new AssertionError("DGC is not disabled"); }
    }

    public void add2L2StateManagerWhenGCDisabled(final NodeID nodeID, final Set<ObjectID> oids, final State currentState) {
      boolean toAdd = false;
      synchronized (this) {
        disableGCIfPossible();
        if (this.syncingPassives.containsKey(nodeID)) {
          logger.warn("Not adding " + nodeID + " since it is already present in syncingPassives : "
                      + this.syncingPassives.keySet());
          return;
        }
        if (this.disabled) {
          this.syncingPassives.put(nodeID, ADDED);
          toAdd = true;
        } else {
          logger
              .info("Couldnt disable DGC, probably because DGC is currently running. So scheduling passive sync up for later after DGC completion");
          this.syncingPassives.put(nodeID, new SyncingPassiveValue(oids, currentState));
        }
      }
      if (toAdd) {
        if (!ReplicatedObjectManagerImpl.this.add2L2StateManager(nodeID, currentState, oids)) {
          logger.warn(nodeID + " is already added to L2StateManager, clearing our internal data structures.");
          syncCompleteFor(nodeID);
        }
      }
    }

    public synchronized void clear(final NodeID nodeID) {
      final Object val = this.syncingPassives.remove(nodeID);
      if (val != null) {
        enableGCIfNecessary();
      }
    }

    private void enableGCIfNecessary() {
      if (this.syncingPassives.isEmpty() && this.disabled) {
        logger.info("Reenabling DGC as all passive are synced up");
        ReplicatedObjectManagerImpl.this.objectManager.getGarbageCollector().enableGC();
        this.disabled = false;
      }
    }

    public synchronized void syncCompleteFor(final NodeID nodeID) {
      final Object val = this.syncingPassives.remove(nodeID);
      // value could be null if the node disconnects before fully synching up.
      Assert.assertTrue(val == ADDED || val == null);
      if (val != null) {
        Assert.assertTrue(this.disabled);
        enableGCIfNecessary();
      }
    }

    public void disableAndAdd2L2StateManager(final Map<NodeID, SyncingPassiveValue> nodeID2ObjectIDs) {
      synchronized (this) {
        if (nodeID2ObjectIDs.size() > 0 && !this.disabled) {
          logger.info("Disabling DGC since " + nodeID2ObjectIDs.size() + " passives [" + nodeID2ObjectIDs.keySet()
                      + "] needs to sync up");
          disableGC();
          assertGCDisabled();
        }
        for (final Iterator<Entry<NodeID, SyncingPassiveValue>> i = nodeID2ObjectIDs.entrySet().iterator(); i.hasNext();) {
          final Entry<NodeID, SyncingPassiveValue> e = i.next();
          final NodeID nodeID = e.getKey();
          // XXX: syncingPassives can contain the node and still not ADDED state as the DGC was not disabled. They
          // contain syncingPassiveValue. So, you are missing to sync them here.
          if (!this.syncingPassives.containsKey(nodeID)) {
            this.syncingPassives.put(nodeID, ADDED);
          } else {
            logger.info("Removing " + nodeID
                        + " from the list to add to L2ObjectStateManager since its present in syncingPassives : "
                        + this.syncingPassives.keySet());
            i.remove();
          }
        }
      }
      add2L2StateManager(nodeID2ObjectIDs);
    }
  }

  private static class SyncingPassiveValue {
    protected final Set<ObjectID> oids;
    protected final State         currentState;

    public SyncingPassiveValue() {
      this(TCCollections.EMPTY_OBJECT_ID_SET, new State("NO_STATE"));
    }

    public SyncingPassiveValue(Set<ObjectID> oids, State currentState) {
      this.oids = oids;
      this.currentState = currentState;
    }

    public Set<ObjectID> getOids() {
      return oids;
    }

    public State getCurrentState() {
      return currentState;
    }

  }

  // strictly for tests
  public GCMonitor getGCMonitor() {
    return this.gcMonitor;
  }

}
