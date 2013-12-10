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
import com.tc.l2.msg.PassiveSyncBeginMessage;
import com.tc.l2.msg.PassiveSyncBeginMessageFactory;
import com.tc.l2.state.StateManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.net.groups.GroupMessage;
import com.tc.net.groups.GroupMessageListener;
import com.tc.net.groups.GroupResponse;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.context.DGCResultContext;
import com.tc.objectserver.dgc.api.GarbageCollectionInfo;
import com.tc.objectserver.dgc.impl.GarbageCollectorEventListenerAdapter;
import com.tc.objectserver.persistence.ClusterStatePersistor;
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
import com.terracottatech.config.DataStorage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicBoolean;

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
  private final L2PassiveSyncStateManager    passiveSyncStateManager;
  private final L2ObjectStateManager         l2ObjectStateManager;
  private final ClusterStatePersistor        clusterStatePersistor;
  private final DataStorage                  dataStorage;

  private final AtomicBoolean                syncStarted = new AtomicBoolean();

  public ReplicatedObjectManagerImpl(final GroupManager groupManager, final StateManager stateManager,
                                     final L2PassiveSyncStateManager l2PassiveSyncStateManager,
                                     L2ObjectStateManager l2ObjectStateManager,
                                     final ObjectManager objectManager,
                                     final ServerTransactionManager transactionManager,
                                     final Sink objectsSyncRequestSink, final Sink indexSyncRequestSink,
                                     final Sink transactionRelaySink, final SequenceGenerator sequenceGenerator,
                                     final SequenceGenerator indexSequenceGenerator, final DataStorage dataStorage,
                                     final ClusterStatePersistor clusterStatePersistor) {
    this.groupManager = groupManager;
    this.stateManager = stateManager;
    this.objectManager = objectManager;
    this.transactionManager = transactionManager;
    this.objectsSyncRequestSink = objectsSyncRequestSink;
    this.indexSyncRequestSink = indexSyncRequestSink;
    this.transactionRelaySink = transactionRelaySink;
    this.sequenceGenerator = sequenceGenerator;
    this.indexSequenceGenerator = indexSequenceGenerator;
    this.clusterStatePersistor = clusterStatePersistor;
    this.gcMonitor = new GCMonitor();
    this.objectManager.getGarbageCollector().addListener(this.gcMonitor);
    this.groupManager.registerForMessages(PassiveSyncBeginMessage.class, this);
    this.groupManager.registerForMessages(ObjectListSyncMessage.class, this);
    this.groupManager.registerForMessages(ObjectSyncCompleteAckMessage.class, this);
    this.groupManager.registerForMessages(IndexSyncCompleteAckMessage.class, this);
    this.passiveSyncStateManager = l2PassiveSyncStateManager;
    this.l2ObjectStateManager = l2ObjectStateManager;
    this.dataStorage = dataStorage;
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
      synchronized (this) {
        final Map<NodeID, PassiveSyncState> nodeIDSyncingPassives = new LinkedHashMap<NodeID, PassiveSyncState>();
        for (GroupMessage groupMessage : gr.getResponses()) {
          final ObjectListSyncMessage msg = (ObjectListSyncMessage)groupMessage;
          if (msg.getType() == ObjectListSyncMessage.RESPONSE) {
            State curState = msg.getCurrentState();
            if (gcMonitor.isPassiveSyncedOrSyncing(msg.messageFrom())) {
              logger.info("Passive " + msg.messageFrom() + " is already syncing.");
            } else if (StateManager.PASSIVE_UNINITIALIZED.equals(curState) && !msg.isSyncAllowed()) {
              // Zap all uninitialized passives joining with # of objects > 0
              logger.error("Syncing to partially synced passives not supported, msg: " + msg);
              this.groupManager.zapNode(msg.messageFrom(), L2HAZapNodeRequestProcessor.PARTIALLY_SYNCED_PASSIVE_JOINED,
                  "Passive : " + msg.messageFrom() + " joined in partially synced state. "
                  + L2HAZapNodeRequestProcessor.getErrorString(new Throwable()));
            } else if (checkForSufficientResources(msg)) {
              nodeIDSyncingPassives.put(msg.messageFrom(), new PassiveSyncState(curState));
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
    if (msg instanceof PassiveSyncBeginMessage) {
      PassiveSyncBeginMessage message = (PassiveSyncBeginMessage) msg;
      handlePassiveSyncBeginMessage(fromNode, message);
    } else if (msg instanceof ObjectListSyncMessage) {
      final ObjectListSyncMessage clusterMsg = (ObjectListSyncMessage)msg;
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

  private void handlePassiveSyncBeginMessage(final NodeID fromNode, final PassiveSyncBeginMessage message) {
    if (message.isRequest()) {
      // Reject subsequent sync begin requests
      try {
        if (syncStarted.compareAndSet(false, true)) {
          groupManager.sendTo(fromNode, PassiveSyncBeginMessageFactory.beginResponse(stateManager.getCurrentState()));
        } else {
          groupManager.sendTo(fromNode, PassiveSyncBeginMessageFactory.beginError());
        }
      } catch (GroupException e) {
        logger.error("Error sending response to active.", e);
      }
    } else if (message.isResponse()) {
      if (!add2L2StateManager(fromNode, message.getCurrentState())) {
        logger.info("Passive sync is already completed for node " + fromNode);
        gcMonitor.syncCompleteFor(fromNode);
      }
    } else {
      groupManager.zapNode(fromNode, L2HAZapNodeRequestProcessor.PROGRAM_ERROR,
          "Incorrect response received from passive to sync begin. msg=" + message );
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
    gcMonitor.garbageCollectorCycleCompleted(gcMsg.getGCInfo(), TCCollections.EMPTY_OBJECT_ID_SET);
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

  private synchronized void handleObjectListResponse(final NodeID nodeID, final ObjectListSyncMessage clusterMsg) {
    Assert.assertTrue(this.stateManager.isActiveCoordinator());

    if (gcMonitor.isPassiveSyncedOrSyncing(nodeID)) {
      logger.info("Sync for passive node " + nodeID + " has already been initiated. Ignoring second request.");
      return;
    }

    if (StateManager.PASSIVE_STANDBY.equals(clusterMsg.getCurrentState())) {
      logger.error("Node " + nodeID + " joined as " + StateManager.PASSIVE_STANDBY + " without a sync.");
      this.groupManager.zapNode(nodeID, L2HAZapNodeRequestProcessor.NODE_JOINED_WITH_DIRTY_DB,
                                "Already passive standby. " + L2HAZapNodeRequestProcessor.getErrorString(new Throwable()));
    } else if (!clusterMsg.isSyncAllowed()) {
      logger.error("Node " + nodeID +" has declared that it is not allowed to sync. Zapping it so it can rejoin.");
      this.groupManager.zapNode(nodeID, L2HAZapNodeRequestProcessor.NODE_JOINED_WITH_DIRTY_DB,
          "Already synced once. " + L2HAZapNodeRequestProcessor.getErrorString(new Throwable()));
    } else {
      // DEV-1944 : We don't want newly joined nodes to be syncing the Objects while the active is receiving the re-sent
      // transactions. If we do that there is a race where passive can apply already applied transactions twice.
      // XXX:: 3 passives - partial sync.
      if (!checkForSufficientResources(clusterMsg)) {
        return;   // Only check for sufficient resources on the new passive if it was shut down after the active and is not a candidate for zapping due to dirty db.
      }
      gcMonitor.addL2StateOnTransactionCompletion(nodeID, clusterMsg.getCurrentState());
    }
  }

  private void startSyncFor(final NodeID nodeID) {
    try {
      groupManager.sendTo(nodeID, PassiveSyncBeginMessageFactory.beginRequest());
    } catch (GroupException e) {
      logger.error("Failed to send begin sync message to node " + nodeID);
      groupManager.zapNode(nodeID, L2HAZapNodeRequestProcessor.COMMUNICATION_ERROR, "Failed to send being sync message.");
    }
  }

  private boolean add2L2StateManager(final NodeID nodeID, final State currentState) {
    return passiveSyncStateManager.addL2(nodeID, currentState);
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
  public void indexSyncStartFor(NodeID nodeID, int idxCtPerCache) {
    try {
      logger.info("Index Sync started for " + nodeID);
      final IndexSyncStartMessage msg = IndexSyncMessageFactory.createIndexSyncStartMessage(this.indexSequenceGenerator
          .getNextSequence(nodeID), idxCtPerCache);
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
          boolean syncAllowed = !syncStarted.get() && clusterStatePersistor.getInitialState() == null;
          logger.info("Send response to Active's query : syncAllowed = " + syncAllowed +
                      " currentState=" + stateManager.getCurrentState() +
                      " resource total=" + getDataStorageSize());
          try {
            groupManager.sendTo(nodeID, ObjectListSyncMessageFactory
                .createObjectListSyncResponseMessage(clusterMsg, stateManager.getCurrentState(), syncAllowed,
                    getDataStorageSize(), getOffheapSize()));
          } catch (GroupException e) {
            logger.error("Failed to send object list response to the active.", e);
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

  private long getDataStorageSize() {
    try {
      return Conversion.memorySizeAsLongBytes(dataStorage.getSize());
    } catch (Conversion.MetricsFormatException e) {
      throw new RuntimeException(e);
    }
  }

  private long getOffheapSize() {
    try {
      return Conversion.memorySizeAsLongBytes(dataStorage.getOffheap().getSize());
    } catch (Conversion.MetricsFormatException e) {
      throw new RuntimeException(e);
    }
  }

  private boolean checkForSufficientResources(ObjectListSyncMessage response) {
    if (response.getType() != ObjectListSyncMessage.RESPONSE) {
      throw new AssertionError();
    }

    // We don't want to allow a passive with less than the active's resources to join the cluster. That could lead
    // to the passive crashing randomly at some point in the future, or maybe even during passive sync.
    if (getDataStorageSize() > response.getDataStorageSize()) {
      groupManager.zapNode(response.messageFrom(), L2HAZapNodeRequestProcessor.INSUFFICIENT_RESOURCES,
          "Node " + response.messageFrom() + " does not have enough dataStorage space to join the cluster. Required " +
          getDataStorageSize() + " got " + response.getDataStorageSize());
      return false;
    }

    if (getOffheapSize() > response.getOffheapSize()) {
      groupManager.zapNode(response.messageFrom(), L2HAZapNodeRequestProcessor.INSUFFICIENT_RESOURCES,
          "Node " + response.messageFrom() + " does not have enough offheap space to join the cluster. Required " +
          getOffheapSize() + " got " + response.getOffheapSize());
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

  private static final PassiveSyncState ADDED = new PassiveSyncState();
  private static final PassiveSyncState WAITING_FOR_TXN = new PassiveSyncState();

  final class GCMonitor extends GarbageCollectorEventListenerAdapter {

    boolean                          disabled        = false;
    final Map<NodeID, PassiveSyncState> syncingPassives = new HashMap<NodeID, PassiveSyncState>();
    final Set<NodeID> syncedPassives = new HashSet<NodeID>();

    @Override
    public void garbageCollectorCycleCompleted(final GarbageCollectionInfo info, final ObjectIDSet toDeleted) {
      Map<NodeID, PassiveSyncState> toAdd = null;
      notifyGCResultToPassives(info, toDeleted);
      boolean havePassivesToSync = false;
      synchronized (this) {
        if (syncingPassives.isEmpty()) { return; }
        toAdd = new LinkedHashMap<NodeID, PassiveSyncState>();
        for (Entry<NodeID, PassiveSyncState> e : syncingPassives.entrySet()) {
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

    synchronized boolean isPassiveSyncedOrSyncing(NodeID nodeID) {
      return syncingPassives.containsKey(nodeID) || syncedPassives.contains(nodeID);
    }

    synchronized void addL2StateOnTransactionCompletion(final NodeID nodeID, final State state) {
      syncingPassives.put(nodeID, WAITING_FOR_TXN);
      transactionManager.callBackOnResentTxnsInSystemCompletion(new TxnsInSystemCompletionListener() {
        @Override
        public void onCompletion() {
          add2L2StateManagerWhenGCDisabled(nodeID, state);
        }
      });
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

    private void add2L2StateManager(final Map<NodeID, PassiveSyncState> toAdd) {
      for (Entry<NodeID, PassiveSyncState> e : toAdd.entrySet()) {
        final NodeID nodeID = e.getKey();
        startSyncFor(nodeID);
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

    public void add2L2StateManagerWhenGCDisabled(final NodeID nodeID, final State currentState) {
      boolean toAdd = false;
      synchronized (this) {
        disableGCIfPossible();
        if (syncingPassives.containsKey(nodeID) && syncingPassives.get(nodeID) != WAITING_FOR_TXN) {
          logger.warn("Not adding " + nodeID + " since it is already present in syncingPassives : "
                      + this.syncingPassives.keySet());
          return;
        }
        if (this.disabled) {
          syncingPassives.put(nodeID, ADDED);
          toAdd = true;
        } else {
          logger
              .info("Couldnt disable DGC, probably because DGC is currently running. So scheduling passive sync up for later after DGC completion");
          syncingPassives.put(nodeID, new PassiveSyncState(currentState));
        }
      }
      if (toAdd) {
        startSyncFor(nodeID);
      }
    }

    public synchronized void clear(final NodeID nodeID) {
      final Object val = syncingPassives.remove(nodeID);
      if (val != null) {
        enableGCIfNecessary();
      }
      syncedPassives.remove(nodeID);
    }

    private void enableGCIfNecessary() {
      if (syncingPassives.isEmpty() && disabled) {
        logger.info("Reenabling DGC as all passive are synced up");
        ReplicatedObjectManagerImpl.this.objectManager.getGarbageCollector().enableGC();
        this.disabled = false;
      }
    }

    public synchronized void syncCompleteFor(final NodeID nodeID) {
      final Object val = syncingPassives.remove(nodeID);
      // value could be null if the node disconnects before fully synching up.
      Assert.assertTrue(val == WAITING_FOR_TXN || val == ADDED || val == null);
      if (val != null) {
        syncedPassives.add(nodeID);
        Assert.assertTrue(this.disabled);
        enableGCIfNecessary();
      }
    }

    public void disableAndAdd2L2StateManager(final Map<NodeID, PassiveSyncState> nodeID2ObjectIDs) {
      synchronized (this) {
        if (nodeID2ObjectIDs.size() > 0 && !this.disabled) {
          logger.info("Disabling DGC since " + nodeID2ObjectIDs.size() + " passives [" + nodeID2ObjectIDs.keySet()
                      + "] needs to sync up");
          disableGC();
          assertGCDisabled();
        }
        for (final Iterator<Entry<NodeID, PassiveSyncState>> i = nodeID2ObjectIDs.entrySet().iterator(); i.hasNext();) {
          final Entry<NodeID, PassiveSyncState> e = i.next();
          final NodeID nodeID = e.getKey();
          // XXX: syncingPassives can contain the node and still not ADDED state as the DGC was not disabled. They
          // contain syncingPassiveValue. So, you are missing to sync them here.
          if (!syncingPassives.containsKey(nodeID)) {
            syncingPassives.put(nodeID, ADDED);
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

  private static class PassiveSyncState {
    // protected final State currentState;

    public PassiveSyncState() {
      this(new State("NO_STATE"));
    }

    public PassiveSyncState(State currentState) {
      // this.currentState = currentState;
    }

    // public State getCurrentState() {
    // return currentState;
    // }
  }
}
