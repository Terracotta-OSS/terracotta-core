/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.ha;

import com.tc.async.api.Sink;
import com.tc.async.api.StageManager;
import com.tc.async.impl.OrderedSink;
import com.tc.config.schema.NewHaConfig;
import com.tc.l2.api.L2Coordinator;
import com.tc.l2.api.ReplicatedClusterStateManager;
import com.tc.l2.context.StateChangedEvent;
import com.tc.l2.handler.GCResultHandler;
import com.tc.l2.handler.GroupEventsDispatchHandler;
import com.tc.l2.handler.L2ObjectSyncDehydrateHandler;
import com.tc.l2.handler.L2ObjectSyncHandler;
import com.tc.l2.handler.L2ObjectSyncRequestHandler;
import com.tc.l2.handler.L2ObjectSyncSendHandler;
import com.tc.l2.handler.L2StateChangeHandler;
import com.tc.l2.handler.L2StateMessageHandler;
import com.tc.l2.handler.ServerTransactionAckHandler;
import com.tc.l2.handler.TransactionRelayHandler;
import com.tc.l2.handler.GroupEventsDispatchHandler.GroupEventsDispatcher;
import com.tc.l2.msg.GCResultMessage;
import com.tc.l2.msg.L2StateMessage;
import com.tc.l2.msg.ObjectSyncCompleteMessage;
import com.tc.l2.msg.ObjectSyncMessage;
import com.tc.l2.msg.RelayedCommitTransactionMessage;
import com.tc.l2.msg.ServerTxnAckMessage;
import com.tc.l2.objectserver.L2ObjectStateManager;
import com.tc.l2.objectserver.L2ObjectStateManagerImpl;
import com.tc.l2.objectserver.ReplicatedObjectManager;
import com.tc.l2.objectserver.ReplicatedObjectManagerImpl;
import com.tc.l2.objectserver.ReplicatedTransactionManager;
import com.tc.l2.objectserver.ReplicatedTransactionManagerImpl;
import com.tc.l2.state.StateChangeListener;
import com.tc.l2.state.StateManager;
import com.tc.l2.state.StateManagerConfigImpl;
import com.tc.l2.state.StateManagerImpl;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.groups.GroupEventsListener;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.net.groups.GroupManagerFactory;
import com.tc.net.groups.Node;
import com.tc.net.groups.NodeID;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.gtx.GlobalTransactionIDSequenceProvider;
import com.tc.objectserver.impl.DistributedObjectServer;
import com.tc.objectserver.persistence.api.PersistentMapStore;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.objectserver.tx.TransactionalObjectManager;
import com.tc.util.sequence.SequenceGenerator;
import com.tc.util.sequence.SequenceGenerator.SequenceGeneratorException;
import com.tc.util.sequence.SequenceGenerator.SequenceGeneratorListener;

import java.io.IOException;

public class L2HACoordinator implements L2Coordinator, StateChangeListener, GroupEventsListener,
    SequenceGeneratorListener {

  private static final TCLogger         logger = TCLogging.getLogger(L2HACoordinator.class);

  private final TCLogger                consoleLogger;
  private final DistributedObjectServer server;

  private GroupManager                  groupManager;
  private StateManager                  stateManager;
  private ReplicatedObjectManager       rObjectManager;
  private ReplicatedTransactionManager  rTxnManager;
  private L2ObjectStateManager          l2ObjectStateManager;
  private ReplicatedClusterStateManager rClusterStateMgr;

  private ClusterState                  clusterState;
  private SequenceGenerator             sequenceGenerator;

  private NewHaConfig                   haConfig;

  public L2HACoordinator(TCLogger consoleLogger, DistributedObjectServer server, StageManager stageManager,
                         PersistentMapStore clusterStateStore, ObjectManager objectManager,
                         ServerTransactionManager transactionManager, TransactionalObjectManager txnObjectManager,
                         GlobalTransactionIDSequenceProvider gidSequenceProvider, NewHaConfig haConfig) {
    this.consoleLogger = consoleLogger;
    this.server = server;
    this.haConfig = haConfig;

    init(stageManager, clusterStateStore, objectManager, transactionManager, txnObjectManager, gidSequenceProvider);
  }

  private void init(StageManager stageManager, PersistentMapStore clusterStateStore, ObjectManager objectManager,
                    ServerTransactionManager transactionManager, TransactionalObjectManager txnObjectManager,
                    GlobalTransactionIDSequenceProvider gidSequenceProvider) {
    try {
      basicInit(stageManager, clusterStateStore, objectManager, transactionManager, txnObjectManager,
                gidSequenceProvider);
    } catch (GroupException e) {
      logger.error(e);
      throw new AssertionError(e);
    }
  }

  private void basicInit(StageManager stageManager, PersistentMapStore clusterStateStore, ObjectManager objectManager,
                         ServerTransactionManager transactionManager, TransactionalObjectManager txnObjectManager,
                         GlobalTransactionIDSequenceProvider gidSequenceProvider) throws GroupException {

    this.clusterState = new ClusterState(clusterStateStore, server.getManagedObjectStore(), server
        .getConnectionIdFactory(), gidSequenceProvider);

    final Sink stateChangeSink = stageManager.createStage(ServerConfigurationContext.L2_STATE_CHANGE_STAGE,
                                                          new L2StateChangeHandler(), 1, Integer.MAX_VALUE).getSink();
    this.groupManager = GroupManagerFactory.createGroupManager();

    this.stateManager = new StateManagerImpl(consoleLogger, groupManager, stateChangeSink,
                                             new StateManagerConfigImpl(haConfig));
    this.stateManager.registerForStateChangeEvents(this);

    this.l2ObjectStateManager = new L2ObjectStateManagerImpl(objectManager, transactionManager);
    this.sequenceGenerator = new SequenceGenerator(this);

    final Sink objectsSyncRequestSink = stageManager
        .createStage(ServerConfigurationContext.OBJECTS_SYNC_REQUEST_STAGE,
                     new L2ObjectSyncRequestHandler(this.l2ObjectStateManager), 1, Integer.MAX_VALUE).getSink();
    final Sink objectsSyncSink = stageManager.createStage(ServerConfigurationContext.OBJECTS_SYNC_STAGE,
                                                          new L2ObjectSyncHandler(), 1, Integer.MAX_VALUE).getSink();
    stageManager.createStage(ServerConfigurationContext.OBJECTS_SYNC_DEHYDRATE_STAGE,
                             new L2ObjectSyncDehydrateHandler(this.sequenceGenerator), 1, Integer.MAX_VALUE);
    stageManager.createStage(ServerConfigurationContext.OBJECTS_SYNC_SEND_STAGE,
                             new L2ObjectSyncSendHandler(this.l2ObjectStateManager), 1, Integer.MAX_VALUE);
    stageManager.createStage(ServerConfigurationContext.TRANSACTION_RELAY_STAGE,
                             new TransactionRelayHandler(this.l2ObjectStateManager, this.sequenceGenerator), 1,
                             Integer.MAX_VALUE);
    final Sink ackProcessingStage = stageManager
        .createStage(ServerConfigurationContext.SERVER_TRANSACTION_ACK_PROCESSING_STAGE,
                     new ServerTransactionAckHandler(), 1, Integer.MAX_VALUE).getSink();
    final Sink stateMessageStage = stageManager.createStage(ServerConfigurationContext.L2_STATE_MESSAGE_HANDLER_STAGE,
                                                            new L2StateMessageHandler(), 1, Integer.MAX_VALUE)
        .getSink();
    final Sink gcResultStage = stageManager.createStage(ServerConfigurationContext.GC_RESULT_PROCESSING_STAGE,
                                                        new GCResultHandler(), 1, Integer.MAX_VALUE).getSink();

    this.rClusterStateMgr = new ReplicatedClusterStateManagerImpl(groupManager, stateManager, clusterState, server
        .getConnectionIdFactory(), stageManager.getStage(ServerConfigurationContext.CHANNEL_LIFE_CYCLE_STAGE).getSink());

    OrderedSink orderedObjectsSyncSink = new OrderedSink(logger, objectsSyncSink);
    this.rTxnManager = new ReplicatedTransactionManagerImpl(groupManager, orderedObjectsSyncSink, transactionManager,
                                                            txnObjectManager);

    this.rObjectManager = new ReplicatedObjectManagerImpl(groupManager, stateManager, l2ObjectStateManager,
                                                          rTxnManager, objectManager, objectsSyncRequestSink,
                                                          sequenceGenerator);

    this.groupManager.routeMessages(ObjectSyncMessage.class, orderedObjectsSyncSink);
    this.groupManager.routeMessages(ObjectSyncCompleteMessage.class, orderedObjectsSyncSink);
    this.groupManager.routeMessages(RelayedCommitTransactionMessage.class, orderedObjectsSyncSink);
    this.groupManager.routeMessages(ServerTxnAckMessage.class, ackProcessingStage);
    this.groupManager.routeMessages(L2StateMessage.class, stateMessageStage);
    this.groupManager.routeMessages(GCResultMessage.class, gcResultStage);

    final Sink groupEventsSink = stageManager.createStage(ServerConfigurationContext.GROUP_EVENTS_DISPATCH_STAGE,
                                                          new GroupEventsDispatchHandler(this), 1, Integer.MAX_VALUE)
        .getSink();
    GroupEventsDispatcher dispatcher = new GroupEventsDispatcher(groupEventsSink);
    groupManager.registerForGroupEvents(dispatcher);
  }

  public void start(final Node thisNode, final Node[] allNodes) {
    NodeID myNodeId;
    try {
      myNodeId = groupManager.join(thisNode, allNodes);
    } catch (GroupException e) {
      logger.error("Caught Exception :", e);
      throw new AssertionError(e);
    }
    logger.info("This L2 Node ID = " + myNodeId);
    stateManager.startElection();
  }

  public StateManager getStateManager() {
    return stateManager;
  }

  public ReplicatedClusterStateManager getReplicatedClusterStateManager() {
    return rClusterStateMgr;
  }

  public ReplicatedObjectManager getReplicatedObjectManager() {
    return rObjectManager;
  }

  public ReplicatedTransactionManager getReplicatedTransactionManager() {
    return rTxnManager;
  }

  public GroupManager getGroupManager() {
    return groupManager;
  }

  public void l2StateChanged(StateChangedEvent sce) {
    clusterState.setCurrentState(sce.getCurrentState());
    rTxnManager.l2StateChanged(sce);
    if (sce.movedToActive()) {
      l2ObjectStateManager.goActive();
      rClusterStateMgr.goActiveAndSyncState();
      rObjectManager.sync();
      try {
        server.startActiveMode();
      } catch (IOException e) {
        throw new AssertionError(e);
      }
    } else {
      // TODO:// handle
      logger.info("Recd. " + sce + " ! Ignoring for now !!!!");
    }
  }

  public void nodeJoined(NodeID nodeID) {
    log(nodeID + " joined the cluster");
    if (stateManager.isActiveCoordinator()) {
      try {
        stateManager.publishActiveState(nodeID);
        rClusterStateMgr.publishClusterState(nodeID);
        rObjectManager.query(nodeID);
      } catch (GroupException ge) {
        logger.error("Error publishing states to the newly joined node : " + nodeID + " Zapping it : ", ge);
        groupManager.zapNode(nodeID);
      }
    }
  }

  private void log(String message) {
    logger.info(message);
    consoleLogger.info(message);
  }

  private void warn(String message) {
    logger.warn(message);
    consoleLogger.warn(message);
  }

  public void nodeLeft(NodeID nodeID) {
    warn(nodeID + " left the cluster");
    if (stateManager.isActiveCoordinator()) {
      rObjectManager.clear(nodeID);
      rClusterStateMgr.fireNodeLeftEvent(nodeID);
    } else {
      stateManager.startElectionIfNecessary(nodeID);
    }
    this.sequenceGenerator.clearSequenceFor(nodeID);
  }

  public void sequenceCreatedFor(Object key) throws SequenceGeneratorException {
    NodeID nodeID = (NodeID) key;
    try {
      rTxnManager.publishResetRequest(nodeID);
    } catch (GroupException ge) {
      logger.error("Error publishing reset counter request node : " + nodeID + " Zapping it : ", ge);
      groupManager.zapNode(nodeID);
      throw new SequenceGeneratorException(ge);
    }
  }

  public void sequenceDestroyedFor(Object key) {
    // NOP
  }
}
