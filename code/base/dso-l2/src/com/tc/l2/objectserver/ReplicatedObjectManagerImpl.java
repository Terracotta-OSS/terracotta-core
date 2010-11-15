/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.objectserver;

import com.tc.async.api.Sink;
import com.tc.l2.context.SyncObjectsRequest;
import com.tc.l2.ha.L2HAZapNodeRequestProcessor;
import com.tc.l2.msg.GCResultMessage;
import com.tc.l2.msg.GCResultMessageFactory;
import com.tc.l2.msg.ObjectListSyncMessage;
import com.tc.l2.msg.ObjectListSyncMessageFactory;
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
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.context.GCResultContext;
import com.tc.objectserver.dgc.api.GarbageCollectionInfo;
import com.tc.objectserver.dgc.impl.GarbageCollectorEventListenerAdapter;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.objectserver.tx.TxnsInSystemCompletionListener;
import com.tc.util.Assert;
import com.tc.util.ObjectIDSet;
import com.tc.util.concurrent.ThreadUtil;
import com.tc.util.sequence.SequenceGenerator;
import com.tc.util.sequence.SequenceGenerator.SequenceGeneratorException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

public class ReplicatedObjectManagerImpl implements ReplicatedObjectManager, GroupMessageListener,
    L2ObjectStateListener {

  private static final TCLogger              logger        = TCLogging.getLogger(ReplicatedObjectManagerImpl.class);

  private final ObjectManager                objectManager;
  private final GroupManager                 groupManager;
  private final StateManager                 stateManager;
  private final L2ObjectStateManager         l2ObjectStateManager;
  private final ReplicatedTransactionManager rTxnManager;
  private final ServerTransactionManager     transactionManager;
  private final Sink                         objectsSyncRequestSink;
  private final SequenceGenerator            sequenceGenerator;
  private final GCMonitor                    gcMonitor;
  private final AtomicLong                   gcIdGenerator = new AtomicLong();
  private final boolean                      isCleanDB;

  public ReplicatedObjectManagerImpl(final GroupManager groupManager, final StateManager stateManager,
                                     final L2ObjectStateManager l2ObjectStateManager,
                                     final ReplicatedTransactionManager txnManager, final ObjectManager objectManager,
                                     final ServerTransactionManager transactionManager,
                                     final Sink objectsSyncRequestSink, final SequenceGenerator sequenceGenerator,
                                     final boolean isCleanDB) {
    this.groupManager = groupManager;
    this.stateManager = stateManager;
    this.rTxnManager = txnManager;
    this.objectManager = objectManager;
    this.transactionManager = transactionManager;
    this.objectsSyncRequestSink = objectsSyncRequestSink;
    this.l2ObjectStateManager = l2ObjectStateManager;
    this.sequenceGenerator = sequenceGenerator;
    this.gcMonitor = new GCMonitor();
    this.objectManager.getGarbageCollector().addListener(this.gcMonitor);
    l2ObjectStateManager.registerForL2ObjectStateChangeEvents(this);
    this.groupManager.registerForMessages(ObjectListSyncMessage.class, this);
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
      final Map nodeID2ObjectIDs = new LinkedHashMap();
      for (final Iterator i = gr.getResponses().iterator(); i.hasNext();) {
        final ObjectListSyncMessage msg = (ObjectListSyncMessage) i.next();
        if (msg.getType() == ObjectListSyncMessage.RESPONSE) {
          nodeID2ObjectIDs.put(msg.messageFrom(), msg.getObjectIDs());
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
  }

  public void clear(final NodeID nodeID) {
    this.l2ObjectStateManager.removeL2(nodeID);
    this.gcMonitor.clear(nodeID);
  }

  public void messageReceived(final NodeID fromNode, final GroupMessage msg) {
    if (msg instanceof ObjectListSyncMessage) {
      final ObjectListSyncMessage clusterMsg = (ObjectListSyncMessage) msg;
      handleClusterObjectMessage(fromNode, clusterMsg);
    } else {
      throw new AssertionError("ReplicatedObjectManagerImpl : Received wrong message type :" + msg.getClass().getName()
                               + " : " + msg);
    }
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
      this.transactionManager.callBackOnResentTxnsInSystemCompletion(new TxnsInSystemCompletionListener() {
        public void onCompletion() {
          ReplicatedObjectManagerImpl.this.gcMonitor.add2L2StateManagerWhenGCDisabled(nodeID, oids);
        }
      });
    }
  }

  private boolean add2L2StateManager(final NodeID nodeID, final Set oids) {
    return this.l2ObjectStateManager.addL2(nodeID, oids);
  }

  public void missingObjectsFor(final NodeID nodeID, final int missingObjects) {
    if (missingObjects == 0) {
      this.stateManager.moveNodeToPassiveStandby(nodeID);
      this.gcMonitor.syncCompleteFor(nodeID);
    } else {
      this.objectsSyncRequestSink.add(new SyncObjectsRequest(nodeID));
    }
  }

  public void objectSyncCompleteFor(final NodeID nodeID) {
    try {
      this.gcMonitor.syncCompleteFor(nodeID);
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

  private static final Object ADDED = new Object();

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
      final long id = ReplicatedObjectManagerImpl.this.gcIdGenerator.incrementAndGet();
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
        if (!ReplicatedObjectManagerImpl.this.add2L2StateManager(nodeID, (Set) e.getValue())) {
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

    public void add2L2StateManagerWhenGCDisabled(final NodeID nodeID, final Set oids) {
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
          this.syncingPassives.put(nodeID, oids);
        }
      }
      if (toAdd) {
        if (!ReplicatedObjectManagerImpl.this.add2L2StateManager(nodeID, oids)) {
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
}
