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
import com.tc.l2.msg.IndexCheckSyncMessage;
import com.tc.l2.msg.IndexSyncCompleteMessage;
import com.tc.l2.msg.IndexSyncMessageFactory;
import com.tc.l2.msg.ObjectListSyncMessage;
import com.tc.l2.msg.ObjectListSyncMessageFactory;
import com.tc.l2.msg.ObjectSyncCompleteMessage;
import com.tc.l2.msg.ObjectSyncCompleteMessageFactory;
import com.tc.l2.state.StateManager;
import com.tc.l2.state.StateSyncManager;
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
import com.tc.objectserver.context.GCResultContext;
import com.tc.objectserver.dgc.api.GarbageCollectionInfo;
import com.tc.objectserver.dgc.impl.GarbageCollectorEventListenerAdapter;
import com.tc.objectserver.search.IndexHACoordinator;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.objectserver.tx.TxnsInSystemCompletionListener;
import com.tc.util.Assert;
import com.tc.util.ObjectIDSet;
import com.tc.util.concurrent.ThreadUtil;
import com.tc.util.sequence.SequenceGenerator;
import com.tc.util.sequence.SequenceGenerator.SequenceGeneratorException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class ReplicatedObjectManagerImpl implements ReplicatedObjectManager, GroupMessageListener,
    L2ObjectStateListener, L2IndexStateListener {

  private static final TCLogger              logger                   = TCLogging
                                                                          .getLogger(ReplicatedObjectManagerImpl.class);

  private final ObjectManager                objectManager;
  private final IndexHACoordinator           indexHACoordinator;
  private final GroupManager                 groupManager;
  private final StateManager                 stateManager;
  private final StateSyncManager             stateSyncManager;
  private final L2ObjectStateManager         l2ObjectStateManager;
  private final L2IndexStateManager          l2IndexStateManager;
  private final ReplicatedTransactionManager rTxnManager;
  private final ServerTransactionManager     transactionManager;
  private final Sink                         objectsSyncRequestSink;
  private final Sink                         indexSyncRequestSink;
  private final SequenceGenerator            sequenceGenerator;
  private final SequenceGenerator            indexSequenceGenerator;
  private final GCMonitor                    gcMonitor;
  private final boolean                      isCleanDB;
  private final CheckSyncResponseHandler     checkSyncResponseHandler = new CheckSyncResponseHandler();

  public ReplicatedObjectManagerImpl(final GroupManager groupManager, final StateManager stateManager,
                                     final StateSyncManager stateSyncManager,
                                     final L2ObjectStateManager l2ObjectStateManager,
                                     final L2IndexStateManager l2IndexStateManager,
                                     final ReplicatedTransactionManager txnManager, final ObjectManager objectManager,
                                     final IndexHACoordinator indexHACoordinator,
                                     final ServerTransactionManager transactionManager,
                                     final Sink objectsSyncRequestSink, final Sink indexSyncRequestSink,
                                     final SequenceGenerator sequenceGenerator,
                                     final SequenceGenerator indexSequenceGenerator, final boolean isCleanDB) {
    this.groupManager = groupManager;
    this.stateManager = stateManager;
    this.stateSyncManager = stateSyncManager;
    this.rTxnManager = txnManager;
    this.objectManager = objectManager;
    this.indexHACoordinator = indexHACoordinator;
    this.transactionManager = transactionManager;
    this.objectsSyncRequestSink = objectsSyncRequestSink;
    this.indexSyncRequestSink = indexSyncRequestSink;
    this.l2ObjectStateManager = l2ObjectStateManager;
    this.l2IndexStateManager = l2IndexStateManager;
    this.sequenceGenerator = sequenceGenerator;
    this.indexSequenceGenerator = indexSequenceGenerator;
    this.gcMonitor = new GCMonitor();
    this.objectManager.getGarbageCollector().addListener(this.gcMonitor);
    l2ObjectStateManager.registerForL2ObjectStateChangeEvents(this);
    l2IndexStateManager.registerForL2IndexStateChangeEvents(this);
    this.groupManager.registerForMessages(ObjectListSyncMessage.class, this);
    this.groupManager.registerForMessages(IndexCheckSyncMessage.class, this);
    this.isCleanDB = isCleanDB;
  }

  /**
   * This method is used to sync up all ObjectIDs from the remote ObjectManagers. It is synchronous and after when it
   * returns nobody is allowed to join the cluster with existing objects.
   */
  public void sync() {
    try {
      final GroupResponse gr = this.groupManager.sendAllAndWaitForResponse(ObjectListSyncMessageFactory
          .createObjectListSyncRequestMessage());
      final Map<NodeID, SyncingPassiveValue> nodeID2ObjectIDs = new LinkedHashMap();
      for (final Iterator i = gr.getResponses().iterator(); i.hasNext();) {
        final ObjectListSyncMessage msg = (ObjectListSyncMessage) i.next();
        if (msg.getType() == ObjectListSyncMessage.RESPONSE) {

          SyncingPassiveValue value = nodeID2ObjectIDs.get(msg.messageFrom());
          if (value == null) {
            value = new SyncingPassiveValue();
            nodeID2ObjectIDs.put(msg.messageFrom(), value);
          }
          value.setOids(msg.getObjectIDs());

        } else {
          logger.error("Received wrong response for ObjectListSyncMessage Request  from " + msg.messageFrom()
                       + " : msg : " + msg);
          this.groupManager.zapNode(msg.messageFrom(), L2HAZapNodeRequestProcessor.PROGRAM_ERROR,
                                    "Recd wrong response from : " + msg.messageFrom()
                                        + " for ObjectListSyncMessage Request"
                                        + L2HAZapNodeRequestProcessor.getErrorString(new Throwable()));
        }
      }

      final GroupResponse indexResponse = this.groupManager.sendAllAndWaitForResponse(IndexSyncMessageFactory
          .createIndexCheckSyncRequestMessage());
      for (final Iterator i = indexResponse.getResponses().iterator(); i.hasNext();) {
        final IndexCheckSyncMessage msg = (IndexCheckSyncMessage) i.next();
        if (msg.getType() == IndexCheckSyncMessage.RESPONSE) {

          SyncingPassiveValue value = nodeID2ObjectIDs.get(msg.messageFrom());
          if (value == null) {
            value = new SyncingPassiveValue();
            nodeID2ObjectIDs.put(msg.messageFrom(), value);
          }
          value.setSyncIndex(msg.syncIndex());

        } else {
          logger.error("Received wrong response for ObjectListSyncMessage Request  from " + msg.messageFrom()
                       + " : msg : " + msg);
          this.groupManager.zapNode(msg.messageFrom(), L2HAZapNodeRequestProcessor.PROGRAM_ERROR,
                                    "Recd wrong response from : " + msg.messageFrom()
                                        + " for ObjectListSyncMessage Request"
                                        + L2HAZapNodeRequestProcessor.getErrorString(new Throwable()));
        }
      }

      if (!nodeID2ObjectIDs.isEmpty()) {
        this.gcMonitor.disableAndAdd2L2StateManager(nodeID2ObjectIDs);
      }
    } catch (final GroupException e) {
      logger.error(e);
      throw new AssertionError(e);
    }
  }

  // Query current state of the other L2
  public void query(final NodeID nodeID) throws GroupException {
    this.groupManager.sendTo(nodeID, ObjectListSyncMessageFactory.createObjectListSyncRequestMessage());
    this.groupManager.sendTo(nodeID, IndexSyncMessageFactory.createIndexCheckSyncRequestMessage());
  }

  public void clear(final NodeID nodeID) {
    this.l2ObjectStateManager.removeL2(nodeID);
    this.l2IndexStateManager.removeL2(nodeID);
    this.gcMonitor.clear(nodeID);
  }

  public void messageReceived(final NodeID fromNode, final GroupMessage msg) {
    if (msg instanceof ObjectListSyncMessage) {
      final ObjectListSyncMessage clusterMsg = (ObjectListSyncMessage) msg;
      handleClusterObjectMessage(fromNode, clusterMsg);
    } else if (msg instanceof IndexCheckSyncMessage) {
      final IndexCheckSyncMessage indexCheckMsg = (IndexCheckSyncMessage) msg;
      handleIndexCheckMessage(fromNode, indexCheckMsg);
    } else {
      throw new AssertionError("ReplicatedObjectManagerImpl : Received wrong message type :" + msg.getClass().getName()
                               + " : " + msg);
    }
  }

  private void handleIndexCheckMessage(final NodeID nodeID, IndexCheckSyncMessage indexCheckMsg) {
    try {
      switch (indexCheckMsg.getType()) {
        case IndexCheckSyncMessage.REQUEST:
          handleIndexCheckSyncRequest(nodeID, indexCheckMsg);
          break;
        case IndexCheckSyncMessage.RESPONSE:
          handleIndexCheckSyncResponse(nodeID, indexCheckMsg);
          break;
        case IndexCheckSyncMessage.FAILED_RESPONSE:
          handleIndexCheckSyncFailedResponse(nodeID, indexCheckMsg);
          break;
        default:
          throw new AssertionError("This message shouldn't have been routed here : " + indexCheckMsg);
      }
    } catch (final GroupException e) {
      logger.error("Error handling message : " + indexCheckMsg, e);
      throw new AssertionError(e);
    }
  }

  private void handleIndexCheckSyncRequest(NodeID nodeID, IndexCheckSyncMessage indexCheckMsg) throws GroupException {
    if (!this.stateManager.isActiveCoordinator()) {
      final boolean syncIndex = this.indexHACoordinator.syncIndex();
      logger.info("Send response to Active's query : needs index sync = " + syncIndex);
      this.groupManager.sendTo(nodeID, IndexSyncMessageFactory.createIndexCheckSyncResponseMessage(indexCheckMsg,
                                                                                                   syncIndex));
    } else {
      logger.error("Recd. ObjectListRequest when in ACTIVE state from " + nodeID + ". Zapping node ...");
      this.groupManager.sendTo(nodeID, IndexSyncMessageFactory.createIndexCheckSyncFailedMessage(indexCheckMsg));
      // Now ZAP the node
      this.groupManager.zapNode(nodeID, L2HAZapNodeRequestProcessor.SPLIT_BRAIN, "Recd IndexCheckSyncRequest from : "
                                                                                 + nodeID
                                                                                 + " while in ACTIVE-COORDINATOR state"
                                                                                 + L2HAZapNodeRequestProcessor
                                                                                     .getErrorString(new Throwable()));
    }

  }

  private void handleIndexCheckSyncResponse(NodeID nodeID, IndexCheckSyncMessage indexCheckMsg) {
    Assert.assertTrue(this.stateManager.isActiveCoordinator());
    final boolean syncIndex = indexCheckMsg.syncIndex();
    checkSyncResponseHandler.accept(nodeID, syncIndex);
  }

  private void handleIndexCheckSyncFailedResponse(NodeID nodeID, IndexCheckSyncMessage indexCheckMsg) {
    final String error = "Received wrong response from " + nodeID + " for Index Sync Query : " + indexCheckMsg;
    logger.error(error + " Forcing node to Quit !!");
    this.groupManager.zapNode(nodeID, L2HAZapNodeRequestProcessor.PROGRAM_ERROR, error
                                                                                 + L2HAZapNodeRequestProcessor
                                                                                     .getErrorString(new Throwable()));

  }

  public void handleGCResult(final GCResultMessage gcMsg) {
    final SortedSet gcedOids = gcMsg.getGCedObjectIDs();
    if (this.stateManager.isActiveCoordinator()) {
      logger.warn("Received DGC Result from " + gcMsg.messageFrom() + " While this node is ACTIVE. Ignoring result : "
                  + gcMsg);
      return;
    }
    final boolean deleted = this.objectManager.getGarbageCollector().deleteGarbage(
                                                                                   new GCResultContext(gcedOids, gcMsg
                                                                                       .getGCInfo()));
    if (deleted) {
      logger.info("Removed " + gcedOids.size() + " objects from passive ObjectManager from last DGC from Active");
    } else {
      logger.info("Skipped removing garbage since DGC is either running or disabled. garbage : " + gcMsg);
    }
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
    this.groupManager.zapNode(nodeID, L2HAZapNodeRequestProcessor.PROGRAM_ERROR, error
                                                                                 + L2HAZapNodeRequestProcessor
                                                                                     .getErrorString(new Throwable()));
  }

  private void handleObjectListResponse(final NodeID nodeID, final ObjectListSyncMessage clusterMsg) {
    Assert.assertTrue(this.stateManager.isActiveCoordinator());
    final Set oids = clusterMsg.getObjectIDs();
    if (!oids.isEmpty() || !clusterMsg.isCleanDB()) {
      final StringBuilder error = new StringBuilder();
      if (!clusterMsg.isCleanDB()) {
        error.append("Node with a stale Database is trying to join the cluster. isCleanDB: " + clusterMsg.isCleanDB());
      } else {
        error.append("Nodes joining the cluster after startup shouldnt have any Objects. " + nodeID + " contains "
                     + oids.size() + " Objects !!!");
      }
      logger.error(error.toString() + " Forcing node to Quit !!");
      this.groupManager.zapNode(nodeID, L2HAZapNodeRequestProcessor.NODE_JOINED_WITH_DIRTY_DB,
                                error + L2HAZapNodeRequestProcessor.getErrorString(new Throwable()));
    } else {
      // DEV-1944 : We don't want newly joined nodes to be syncing the Objects while the active is receiving the resent
      // transactions.
      // If we do that there is a race where passive can apply already applied transactions twice. XXX:: 3 passives -
      // partial sync.
      checkSyncResponseHandler.accept(nodeID, oids);
    }
  }

  private boolean add2L2StateManager(final NodeID nodeID, final Set oids, final boolean syncIndex) {
    return this.l2ObjectStateManager.addL2(nodeID, oids) && this.l2IndexStateManager.addL2(nodeID, syncIndex);
  }

  public void missingObjectsFor(final NodeID nodeID, final int missingObjects) {
    if (missingObjects == 0) {
      if (stateSyncManager.objectSyncComplete(nodeID)) {
        this.gcMonitor.syncCompleteFor(nodeID);
      }
    } else {
      this.objectsSyncRequestSink.add(new SyncObjectsRequest(nodeID));
    }
  }

  public void objectSyncCompleteFor(final NodeID nodeID) {

    try {
      if (stateSyncManager.objectSyncComplete(nodeID)) {
        this.gcMonitor.syncCompleteFor(nodeID);
      }
      final ObjectSyncCompleteMessage msg = ObjectSyncCompleteMessageFactory
          .createObjectSyncCompleteMessageFor(nodeID, this.sequenceGenerator.getNextSequence(nodeID));
      this.groupManager.sendTo(nodeID, msg);
    } catch (final GroupException e) {
      logger.error("Error Sending Object Sync complete message  to : " + nodeID, e);
      this.groupManager.zapNode(nodeID, L2HAZapNodeRequestProcessor.COMMUNICATION_ERROR,
                                "Error sending Object Sync complete message "
                                    + L2HAZapNodeRequestProcessor.getErrorString(e));
    } catch (final SequenceGeneratorException e) {
      logger.error("Error Sending Object Sync complete message  to : " + nodeID, e);
    }
  }

  public void indexFilesFor(NodeID nodeID, int indexFiles) {
    if (indexFiles == 0) {
      if (stateSyncManager.indexSyncComplete(nodeID)) {
        this.gcMonitor.syncCompleteFor(nodeID);
      }
    } else {
      this.indexSyncRequestSink.add(new SyncIndexesRequest(nodeID));
    }
  }

  public void indexSyncCompleteFor(NodeID nodeID) {
    try {
      if (stateSyncManager.indexSyncComplete(nodeID)) {
        this.gcMonitor.syncCompleteFor(nodeID);
      }
      final IndexSyncCompleteMessage msg = IndexSyncMessageFactory
          .createIndexSyncCompleteMessage(this.indexSequenceGenerator.getNextSequence(nodeID));
      this.groupManager.sendTo(nodeID, msg);
    } catch (final GroupException e) {
      logger.error("Error Sending Index Sync complete message  to : " + nodeID, e);
      this.groupManager.zapNode(nodeID, L2HAZapNodeRequestProcessor.COMMUNICATION_ERROR,
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
      final Set knownIDs = this.objectManager.getAllObjectIDs();
      this.rTxnManager.init(knownIDs);
      logger.info("Send response to Active's query : known id lists = " + knownIDs.size() + " isCleanDB: "
                  + this.isCleanDB);
      this.groupManager
          .sendTo(nodeID, ObjectListSyncMessageFactory.createObjectListSyncResponseMessage(clusterMsg, knownIDs,
                                                                                           this.isCleanDB));
    } else {
      logger.error("Recd. ObjectListRequest when in ACTIVE state from " + nodeID + ". Zapping node ...");
      this.groupManager.sendTo(nodeID, ObjectListSyncMessageFactory
          .createObjectListSyncFailedResponseMessage(clusterMsg));
      // Now ZAP the node
      this.groupManager.zapNode(nodeID, L2HAZapNodeRequestProcessor.SPLIT_BRAIN, "Recd ObjectListRequest from : "
                                                                                 + nodeID
                                                                                 + " while in ACTIVE-COORDINATOR state"
                                                                                 + L2HAZapNodeRequestProcessor
                                                                                     .getErrorString(new Throwable()));
    }
  }

  public boolean relayTransactions() {
    return this.l2ObjectStateManager.getL2Count() > 0;
  }

  private static final SyncingPassiveValue ADDED = new SyncingPassiveValue();

  private final class GCMonitor extends GarbageCollectorEventListenerAdapter {

    boolean disabled        = false;
    Map     syncingPassives = new HashMap();

    @Override
    public void garbageCollectorCycleCompleted(final GarbageCollectionInfo info, final ObjectIDSet toDeleted) {
      Map toAdd = null;
      notifyGCResultToPassives(info, toDeleted);
      synchronized (this) {
        if (this.syncingPassives.isEmpty()) { return; }
        toAdd = new LinkedHashMap();
        for (final Iterator i = this.syncingPassives.entrySet().iterator(); i.hasNext();) {
          final Entry e = (Entry) i.next();
          if (e.getValue() != ADDED) {
            final NodeID nodeID = (NodeID) e.getKey();
            logger.info("DGC Completed : Starting scheduled passive sync for " + nodeID);
            disableGCIfPossible();
            // Shouldn't happen as this is in DGC call back after DGC completion
            assertGCDisabled();
            toAdd.put(nodeID, e.getValue());
            e.setValue(ADDED);
          }
        }
      }
      add2L2StateManager(toAdd);
    }

    private void notifyGCResultToPassives(final GarbageCollectionInfo gcInfo, final ObjectIDSet deleted) {
      if (deleted.isEmpty()) { return; }
      final GCResultMessage msg = GCResultMessageFactory.createGCResultMessage(gcInfo, deleted);
      final long id = gcInfo.getIteration();
      ReplicatedObjectManagerImpl.this.transactionManager
          .callBackOnTxnsInSystemCompletion(new TxnsInSystemCompletionListener() {
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

    private void add2L2StateManager(final Map toAdd) {
      for (final Iterator i = toAdd.entrySet().iterator(); i.hasNext();) {
        final Entry e = (Entry) i.next();
        final NodeID nodeID = (NodeID) e.getKey();
        final SyncingPassiveValue value = (SyncingPassiveValue) e.getValue();
        if (!ReplicatedObjectManagerImpl.this.add2L2StateManager(nodeID, value.getOids(), value.isSyncIndex())) {
          logger.warn(nodeID + " is already added to L2StateManager, clearing our internal data structures.");
          syncCompleteFor(nodeID);
        }
      }
    }

    private void disableGCIfPossible() {
      if (!this.disabled) {
        this.disabled = ReplicatedObjectManagerImpl.this.objectManager.getGarbageCollector().disableGC();
        logger.info((this.disabled ? "DGC is disabled." : "DGC is is not disabled."));
      }
    }

    private void disableGC() {
      while (!this.disabled) {
        this.disabled = ReplicatedObjectManagerImpl.this.objectManager.getGarbageCollector().disableGC();
        if (!this.disabled) {
          logger.warn("DGC is running. Waiting for it to complete before disabling it...");
          ThreadUtil.reallySleep(3000); // FIXME:: use wait notify instead
        }
      }
    }

    private void assertGCDisabled() {
      if (!this.disabled) { throw new AssertionError("DGC is not disabled"); }
    }

    public void add2L2StateManagerWhenGCDisabled(final NodeID nodeID, final Set oids, final boolean syncIndex) {
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
          this.syncingPassives.put(nodeID, new SyncingPassiveValue(oids, syncIndex));
        }
      }
      if (toAdd) {
        if (!ReplicatedObjectManagerImpl.this.add2L2StateManager(nodeID, oids, syncIndex)) {
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

    public void disableAndAdd2L2StateManager(final Map nodeID2ObjectIDs) {
      synchronized (this) {
        if (nodeID2ObjectIDs.size() > 0 && !this.disabled) {
          logger.info("Disabling DGC since " + nodeID2ObjectIDs.size() + " passives [" + nodeID2ObjectIDs.keySet()
                      + "] needs to sync up");
          disableGC();
          assertGCDisabled();
        }
        for (final Iterator i = nodeID2ObjectIDs.entrySet().iterator(); i.hasNext();) {
          final Entry e = (Entry) i.next();
          final NodeID nodeID = (NodeID) e.getKey();
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
    protected Set<ObjectID>     oids;
    protected boolean syncIndex;

    public SyncingPassiveValue() {
      this(Collections.emptySet(), false);
    }

    public SyncingPassiveValue(Set oids, boolean syncIndex) {
      this.oids = oids;
      this.syncIndex = syncIndex;
    }

    public Set getOids() {
      return oids;
    }

    public boolean isSyncIndex() {
      return syncIndex;
    }

    public void setOids(Set oids) {
      this.oids = oids;
    }

    public void setSyncIndex(boolean syncIndex) {
      this.syncIndex = syncIndex;
    }

  }

  private final class CheckSyncResponseHandler {

    private final Map<NodeID, AcceptValue> checkResponses = new ConcurrentHashMap<NodeID, AcceptValue>();

    public void accept(NodeID nodeID, boolean syncIndex) {
      AcceptValue acceptValue = getAcceptValue(nodeID);
      acceptValue.setSyncIndex(syncIndex);
      processCompeleteIfNeccessary(nodeID, acceptValue);
    }

    public void accept(NodeID nodeID, Set oids) {
      AcceptValue acceptValue = getAcceptValue(nodeID);
      acceptValue.setOids(oids);
      processCompeleteIfNeccessary(nodeID, acceptValue);
    }

    private AcceptValue getAcceptValue(NodeID nodeID) {
      AcceptValue acceptValue = checkResponses.get(nodeID);
      if (acceptValue == null) {
        acceptValue = new AcceptValue();
        checkResponses.put(nodeID, acceptValue);
      }
      return acceptValue;
    }

    private void processCompeleteIfNeccessary(final NodeID nodeID, final AcceptValue acceptValue) {
      if (acceptValue.acceptedAllValues()) {
        ReplicatedObjectManagerImpl.this.transactionManager
            .callBackOnResentTxnsInSystemCompletion(new TxnsInSystemCompletionListener() {
              public void onCompletion() {
                ReplicatedObjectManagerImpl.this.gcMonitor.add2L2StateManagerWhenGCDisabled(nodeID, acceptValue
                    .getOids(), acceptValue.isSyncIndex());
              }
            });
      }
    }
  }

  private static final class AcceptValue extends SyncingPassiveValue {
    private volatile boolean acceptObjectSync = false;
    private volatile boolean acceptIndexSync  = false;

    public boolean acceptedAllValues() {
      return acceptObjectSync && acceptIndexSync;
    }

    @Override
    public void setOids(Set oids) {
      super.setOids(oids);
      this.acceptIndexSync = true;
    }

    @Override
    public void setSyncIndex(boolean syncIndex) {
      super.setSyncIndex(syncIndex);
      acceptObjectSync = true;
    }

  }
}
