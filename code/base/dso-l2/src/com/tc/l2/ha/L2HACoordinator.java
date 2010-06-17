/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.ha;

import static com.tc.l2.ha.ClusterStateDBKeyNames.DATABASE_CREATION_TIMESTAMP_KEY;

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
import com.tc.logging.TerracottaOperatorEventLogger;
import com.tc.logging.TerracottaOperatorEventLogging;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.groups.GroupEventsListener;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.net.groups.StripeIDStateManager;
import com.tc.object.msg.MessageRecycler;
import com.tc.object.persistence.api.PersistentMapStore;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.gtx.ServerGlobalTransactionManager;
import com.tc.objectserver.impl.DistributedObjectServer;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.operatorevent.TerracottaOperatorEventFactory;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;
import com.tc.util.Assert;
import com.tc.util.sequence.SequenceGenerator;
import com.tc.util.sequence.SequenceGenerator.SequenceGeneratorException;
import com.tc.util.sequence.SequenceGenerator.SequenceGeneratorListener;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.CopyOnWriteArrayList;

public class L2HACoordinator implements L2Coordinator, StateChangeListener, GroupEventsListener,
    SequenceGeneratorListener, PrettyPrintable {

  private static final TCLogger                           logger              = TCLogging
                                                                                  .getLogger(L2HACoordinator.class);

  private final TCLogger                                  consoleLogger;
  private final DistributedObjectServer                   server;
  private final GroupManager                              groupManager;
  private final GroupID                                   thisGroupID;

  private StateManager                                    stateManager;
  private ReplicatedObjectManager                         rObjectManager;
  private ReplicatedTransactionManager                    rTxnManager;
  private L2ObjectStateManager                            l2ObjectStateManager;
  private ReplicatedClusterStateManager                   rClusterStateMgr;

  private SequenceGenerator                               sequenceGenerator;

  private final NewHaConfig                               haConfig;
  private final CopyOnWriteArrayList<StateChangeListener> listeners           = new CopyOnWriteArrayList<StateChangeListener>();
  private final TerracottaOperatorEventLogger             operatorEventLogger = TerracottaOperatorEventLogging
                                                                                  .getEventLogger();

  public L2HACoordinator(TCLogger consoleLogger, DistributedObjectServer server, StageManager stageManager,
                         GroupManager groupCommsManager, PersistentMapStore persistentStateStore,
                         ObjectManager objectManager, ServerTransactionManager transactionManager,
                         ServerGlobalTransactionManager gtxm, WeightGeneratorFactory weightGeneratorFactory,
                         NewHaConfig haConfig, MessageRecycler recycler, GroupID thisGroupID,
                         StripeIDStateManager stripeIDStateManager) {
    this.consoleLogger = consoleLogger;
    this.server = server;
    this.groupManager = groupCommsManager;
    this.thisGroupID = thisGroupID;
    this.haConfig = haConfig;

    init(stageManager, persistentStateStore, objectManager, transactionManager, gtxm, weightGeneratorFactory, recycler,
         stripeIDStateManager);
  }

  private void init(StageManager stageManager, PersistentMapStore persistentStateStore, ObjectManager objectManager,
                    ServerTransactionManager transactionManager, ServerGlobalTransactionManager gtxm,
                    WeightGeneratorFactory weightGeneratorFactory, MessageRecycler recycler,
                    StripeIDStateManager stripeIDStateManager) {

    boolean isCleanDB = isCleanDB(persistentStateStore);

    ClusterState clusterState = new ClusterState(persistentStateStore, server.getManagedObjectStore(), server
        .getConnectionIdFactory(), gtxm.getGlobalTransactionIDSequenceProvider(), thisGroupID, stripeIDStateManager);
    final Sink stateChangeSink = stageManager.createStage(ServerConfigurationContext.L2_STATE_CHANGE_STAGE,

    new L2StateChangeHandler(), 1, Integer.MAX_VALUE).getSink();

    this.stateManager = new StateManagerImpl(consoleLogger, groupManager, stateChangeSink,
                                             new StateManagerConfigImpl(haConfig),
                                             createWeightGeneratorFactoryForStateManager(gtxm));
    this.stateManager.registerForStateChangeEvents(this);

    this.l2ObjectStateManager = new L2ObjectStateManagerImpl(objectManager, transactionManager);
    this.sequenceGenerator = new SequenceGenerator(this);

    L2HAZapNodeRequestProcessor zapProcessor = new L2HAZapNodeRequestProcessor(consoleLogger, stateManager,
                                                                               groupManager, weightGeneratorFactory);
    this.groupManager.setZapNodeRequestProcessor(zapProcessor);

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
                             new TransactionRelayHandler(this.l2ObjectStateManager, this.sequenceGenerator, gtxm), 1,
                             Integer.MAX_VALUE);
    final Sink ackProcessingSink = stageManager
        .createStage(ServerConfigurationContext.SERVER_TRANSACTION_ACK_PROCESSING_STAGE,
                     new ServerTransactionAckHandler(), 1, Integer.MAX_VALUE).getSink();
    final Sink stateMessageSink = stageManager.createStage(ServerConfigurationContext.L2_STATE_MESSAGE_HANDLER_STAGE,
                                                           new L2StateMessageHandler(), 1, Integer.MAX_VALUE).getSink();
    final Sink gcResultSink = stageManager.createStage(ServerConfigurationContext.GC_RESULT_PROCESSING_STAGE,
                                                       new GCResultHandler(), 1, Integer.MAX_VALUE).getSink();

    this.rClusterStateMgr = new ReplicatedClusterStateManagerImpl(groupManager, stateManager, clusterState, server
        .getConnectionIdFactory(), stageManager.getStage(ServerConfigurationContext.CHANNEL_LIFE_CYCLE_STAGE).getSink());

    OrderedSink orderedObjectsSyncSink = new OrderedSink(logger, objectsSyncSink);
    this.rTxnManager = new ReplicatedTransactionManagerImpl(groupManager, orderedObjectsSyncSink, transactionManager,
                                                            gtxm, recycler);

    this.rObjectManager = new ReplicatedObjectManagerImpl(groupManager, stateManager, l2ObjectStateManager,
                                                          rTxnManager, objectManager, transactionManager,
                                                          objectsSyncRequestSink, sequenceGenerator, isCleanDB);

    this.groupManager.routeMessages(ObjectSyncMessage.class, orderedObjectsSyncSink);
    this.groupManager.routeMessages(ObjectSyncCompleteMessage.class, orderedObjectsSyncSink);
    this.groupManager.routeMessages(RelayedCommitTransactionMessage.class, orderedObjectsSyncSink);
    this.groupManager.routeMessages(ServerTxnAckMessage.class, ackProcessingSink);
    this.groupManager.routeMessages(L2StateMessage.class, stateMessageSink);
    this.groupManager.routeMessages(GCResultMessage.class, gcResultSink);

    final Sink groupEventsSink = stageManager.createStage(ServerConfigurationContext.GROUP_EVENTS_DISPATCH_STAGE,
                                                          new GroupEventsDispatchHandler(this), 1, Integer.MAX_VALUE)
        .getSink();
    GroupEventsDispatcher dispatcher = new GroupEventsDispatcher(groupEventsSink);
    groupManager.registerForGroupEvents(dispatcher);
  }

  private WeightGeneratorFactory createWeightGeneratorFactoryForStateManager(ServerGlobalTransactionManager gtxm) {
    WeightGeneratorFactory wgf = new WeightGeneratorFactory();
    // TODO::FIXME :: this is probably not the right thing to do since a runnign active might have current gid < curreng
    // gid in a just turned active because of how things are wired.
    //
    // final Sequence gidSequence = gtxm.getGlobalTransactionIDSequence();
    // wgf.add(new WeightGenerator() {
    // public long getWeight() {
    // return gidSequence.current();
    // }
    // });
    wgf.add(WeightGeneratorFactory.RANDOM_WEIGHT_GENERATOR);
    wgf.add(WeightGeneratorFactory.RANDOM_WEIGHT_GENERATOR);
    return wgf;
  }

  public void start() {
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
    // someone wants to be notified earlier
    fireStateChangedEvent(sce);

    this.rClusterStateMgr.setCurrentState(sce.getCurrentState());
    rTxnManager.l2StateChanged(sce);
    if (sce.movedToActive()) {
      rClusterStateMgr.goActiveAndSyncState();
      rObjectManager.sync();
      server.startActiveMode();
      startL1Listener();
    }
  }

  private void fireStateChangedEvent(StateChangedEvent sce) {
    for (StateChangeListener listener : listeners) {
      listener.l2StateChanged(sce);
    }
  }

  public void registerForStateChangeEvents(StateChangeListener listener) {
    listeners.add(listener);
  }

  protected void startL1Listener() {
    try {
      server.startL1Listener();
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  public void nodeJoined(NodeID nodeID) {
    log(nodeID + " joined the cluster");
    fireNodeJoinedOperatorEvent(nodeID);
    if (stateManager.isActiveCoordinator()) {
      try {
        stateManager.publishActiveState(nodeID);
        rClusterStateMgr.publishClusterState(nodeID);
        rObjectManager.query(nodeID);
      } catch (GroupException ge) {
        String errMesg = "A Terracotta server tried to join the mirror group as a second ACTIVE: " + nodeID
                         + " Zapping it to allow it to join as PASSIVE standby (backup): ";
        logger.error(errMesg, ge);
        groupManager.zapNode(nodeID, L2HAZapNodeRequestProcessor.COMMUNICATION_ERROR, errMesg
                                                                                      + L2HAZapNodeRequestProcessor
                                                                                          .getErrorString(ge));
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
    fireNodeLeftOperatorEvent(nodeID);
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
      groupManager.zapNode(nodeID, L2HAZapNodeRequestProcessor.COMMUNICATION_ERROR,
                           "Error publishing reset counter for " + nodeID
                               + L2HAZapNodeRequestProcessor.getErrorString(ge));
      throw new SequenceGeneratorException(ge);
    }
  }

  public void sequenceDestroyedFor(Object key) {
    // NOP
  }

  private boolean isCleanDB(PersistentMapStore clusterStateStore) {
    if (clusterStateStore.get(DATABASE_CREATION_TIMESTAMP_KEY) == null) {
      Calendar cal = Calendar.getInstance();
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
      clusterStateStore.put(DATABASE_CREATION_TIMESTAMP_KEY, sdf.format(cal.getTime()));
      return true;
    }
    return false;
  }

  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    StringBuilder strBuilder = new StringBuilder();
    strBuilder.append(L2HACoordinator.class.getSimpleName() + " [ ");
    strBuilder.append(this.thisGroupID).append(" ").append(this.l2ObjectStateManager);
    strBuilder.append(" ]");
    out.indent().print(strBuilder.toString()).flush();
    out.indent().print("ReplicatedClusterStateMgr").visit(this.rClusterStateMgr).flush();
    return out;
  }

  /**
   * these events would should only be fired for ServerID
   */
  private void fireNodeJoinedOperatorEvent(NodeID nodeID) {
    Assert.assertTrue(nodeID instanceof ServerID);
    ServerID serverID = (ServerID) nodeID;
    operatorEventLogger.fireOperatorEvent(TerracottaOperatorEventFactory.createNodeConnectedEvent(serverID
        .getServerName()));
  }

  private void fireNodeLeftOperatorEvent(NodeID nodeID) {
    Assert.assertTrue(nodeID instanceof ServerID);
    ServerID serverID = (ServerID) nodeID;
    operatorEventLogger.fireOperatorEvent(TerracottaOperatorEventFactory.createNodeDisconnectedEvent(serverID
        .getServerName()));
  }
}
