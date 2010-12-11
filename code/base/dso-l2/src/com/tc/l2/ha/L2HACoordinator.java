/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.ha;

import static com.tc.l2.ha.ClusterStateDBKeyNames.DATABASE_CREATION_TIMESTAMP_KEY;

import com.tc.async.api.Sink;
import com.tc.async.api.StageManager;
import com.tc.async.impl.OrderedSink;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.l2.api.L2Coordinator;
import com.tc.l2.api.ReplicatedClusterStateManager;
import com.tc.l2.context.StateChangedEvent;
import com.tc.l2.handler.GCResultHandler;
import com.tc.l2.handler.GroupEventsDispatchHandler;
import com.tc.l2.handler.GroupEventsDispatchHandler.GroupEventsDispatcher;
import com.tc.l2.handler.L2ObjectSyncDehydrateHandler;
import com.tc.l2.handler.L2ObjectSyncHandler;
import com.tc.l2.handler.L2ObjectSyncRequestHandler;
import com.tc.l2.handler.L2ObjectSyncSendHandler;
import com.tc.l2.handler.L2StateChangeHandler;
import com.tc.l2.handler.L2StateMessageHandler;
import com.tc.l2.handler.ServerTransactionAckHandler;
import com.tc.l2.handler.TransactionRelayHandler;
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
import com.tc.l2.objectserver.ServerTransactionFactory;
import com.tc.l2.operatorevent.OperatorEventsZapRequestListener;
import com.tc.l2.state.StateChangeListener;
import com.tc.l2.state.StateManager;
import com.tc.l2.state.StateManagerConfigImpl;
import com.tc.l2.state.StateManagerImpl;
import com.tc.l2.state.StateSyncManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
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
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;
import com.tc.util.sequence.DGCSequenceProvider;
import com.tc.util.sequence.SequenceGenerator;
import com.tc.util.sequence.SequenceGenerator.SequenceGeneratorException;
import com.tc.util.sequence.SequenceGenerator.SequenceGeneratorListener;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.CopyOnWriteArrayList;

public class L2HACoordinator implements L2Coordinator, StateChangeListener, GroupEventsListener,
    SequenceGeneratorListener, PrettyPrintable {

  private static final TCLogger                           logger    = TCLogging.getLogger(L2HACoordinator.class);

  private final TCLogger                                  consoleLogger;
  private final DistributedObjectServer                   server;
  private final GroupManager                              groupManager;
  private final GroupID                                   thisGroupID;

  private StateManager                                    stateManager;
  private ReplicatedObjectManager                         rObjectManager;
  private ReplicatedTransactionManager                    rTxnManager;
  private L2ObjectStateManager                            l2ObjectStateManager;
  private ReplicatedClusterStateManager                   rClusterStateMgr;
  private final StateSyncManager                          stateSyncManager;

  private SequenceGenerator                               sequenceGenerator;

  private final L2ConfigurationSetupManager               configSetupManager;
  private final CopyOnWriteArrayList<StateChangeListener> listeners = new CopyOnWriteArrayList<StateChangeListener>();

  public L2HACoordinator(final TCLogger consoleLogger, final DistributedObjectServer server,
                         final StageManager stageManager, final GroupManager groupCommsManager,
                         final PersistentMapStore persistentStateStore, final ObjectManager objectManager,
                         final ServerTransactionManager transactionManager, final ServerGlobalTransactionManager gtxm,
                         final WeightGeneratorFactory weightGeneratorFactory,
                         final L2ConfigurationSetupManager configurationSetupManager, final MessageRecycler recycler,
                         final GroupID thisGroupID, final StripeIDStateManager stripeIDStateManager,
                         final ServerTransactionFactory serverTransactionFactory,
                         DGCSequenceProvider dgcSequenceProvider, StateSyncManager stateSyncManager) {
    this.consoleLogger = consoleLogger;
    this.server = server;
    this.groupManager = groupCommsManager;
    this.thisGroupID = thisGroupID;
    this.configSetupManager = configurationSetupManager;
    this.stateSyncManager = stateSyncManager;

    init(stageManager, persistentStateStore, objectManager, transactionManager, gtxm, weightGeneratorFactory, recycler,
         stripeIDStateManager, serverTransactionFactory, dgcSequenceProvider);
  }

  private void init(final StageManager stageManager, final PersistentMapStore persistentStateStore,
                    final ObjectManager objectManager, final ServerTransactionManager transactionManager,
                    final ServerGlobalTransactionManager gtxm, final WeightGeneratorFactory weightGeneratorFactory,
                    final MessageRecycler recycler, final StripeIDStateManager stripeIDStateManager,
                    final ServerTransactionFactory serverTransactionFactory, DGCSequenceProvider dgcSequenceProvider) {

    final boolean isCleanDB = isCleanDB(persistentStateStore);

    final ClusterState clusterState = new ClusterState(persistentStateStore, this.server.getManagedObjectStore(),
                                                       this.server.getConnectionIdFactory(),
                                                       gtxm.getGlobalTransactionIDSequenceProvider(), this.thisGroupID,
                                                       stripeIDStateManager, dgcSequenceProvider);
    final Sink stateChangeSink = stageManager.createStage(ServerConfigurationContext.L2_STATE_CHANGE_STAGE,

    new L2StateChangeHandler(), 1, Integer.MAX_VALUE).getSink();

    this.stateManager = new StateManagerImpl(this.consoleLogger, this.groupManager, stateChangeSink,
                                             new StateManagerConfigImpl(this.configSetupManager.haConfig()),
                                             createWeightGeneratorFactoryForStateManager(gtxm));
    this.stateManager.registerForStateChangeEvents(this);

    this.l2ObjectStateManager = new L2ObjectStateManagerImpl(objectManager, transactionManager);
    this.sequenceGenerator = new SequenceGenerator(this);

    final L2HAZapNodeRequestProcessor zapProcessor = new L2HAZapNodeRequestProcessor(this.consoleLogger,
                                                                                     this.stateManager,
                                                                                     this.groupManager,
                                                                                     weightGeneratorFactory);
    zapProcessor.addZapEventListener(new OperatorEventsZapRequestListener(this.configSetupManager));
    this.groupManager.setZapNodeRequestProcessor(zapProcessor);

    final Sink objectsSyncRequestSink = stageManager
        .createStage(ServerConfigurationContext.OBJECTS_SYNC_REQUEST_STAGE,
                     new L2ObjectSyncRequestHandler(this.l2ObjectStateManager), 1, Integer.MAX_VALUE).getSink();
    final Sink objectsSyncSink = stageManager.createStage(ServerConfigurationContext.OBJECTS_SYNC_STAGE,
                                                          new L2ObjectSyncHandler(serverTransactionFactory), 1,
                                                          Integer.MAX_VALUE).getSink();
    stageManager.createStage(ServerConfigurationContext.OBJECTS_SYNC_DEHYDRATE_STAGE,
                             new L2ObjectSyncDehydrateHandler(this.sequenceGenerator), 1, Integer.MAX_VALUE);
    stageManager.createStage(ServerConfigurationContext.OBJECTS_SYNC_SEND_STAGE,
                             new L2ObjectSyncSendHandler(this.l2ObjectStateManager, serverTransactionFactory), 1,
                             Integer.MAX_VALUE);
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

    this.rClusterStateMgr = new ReplicatedClusterStateManagerImpl(
                                                                  this.groupManager,
                                                                  this.stateManager,
                                                                  clusterState,
                                                                  this.server.getConnectionIdFactory(),
                                                                  stageManager
                                                                      .getStage(ServerConfigurationContext.CHANNEL_LIFE_CYCLE_STAGE)
                                                                      .getSink());

    final OrderedSink orderedObjectsSyncSink = new OrderedSink(logger, objectsSyncSink);
    this.rTxnManager = new ReplicatedTransactionManagerImpl(this.groupManager, orderedObjectsSyncSink,
                                                            transactionManager, gtxm, recycler);

    this.rObjectManager = new ReplicatedObjectManagerImpl(this.groupManager, this.stateManager,
                                                          this.l2ObjectStateManager, this.rTxnManager, objectManager,
                                                          transactionManager, objectsSyncRequestSink,
                                                          this.sequenceGenerator, isCleanDB);

    this.stateSyncManager.setStateManager(stateManager);

    this.groupManager.routeMessages(ObjectSyncMessage.class, orderedObjectsSyncSink);
    this.groupManager.routeMessages(ObjectSyncCompleteMessage.class, orderedObjectsSyncSink);
    this.groupManager.routeMessages(RelayedCommitTransactionMessage.class, orderedObjectsSyncSink);
    this.groupManager.routeMessages(ServerTxnAckMessage.class, ackProcessingSink);
    this.groupManager.routeMessages(L2StateMessage.class, stateMessageSink);
    this.groupManager.routeMessages(GCResultMessage.class, gcResultSink);

    final Sink groupEventsSink = stageManager.createStage(ServerConfigurationContext.GROUP_EVENTS_DISPATCH_STAGE,
                                                          new GroupEventsDispatchHandler(this), 1, Integer.MAX_VALUE)
        .getSink();
    final GroupEventsDispatcher dispatcher = new GroupEventsDispatcher(groupEventsSink);
    this.groupManager.registerForGroupEvents(dispatcher);
  }

  private WeightGeneratorFactory createWeightGeneratorFactoryForStateManager(final ServerGlobalTransactionManager gtxm) {
    final WeightGeneratorFactory wgf = new WeightGeneratorFactory();
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
    this.stateManager.startElection();
  }

  public StateManager getStateManager() {
    return this.stateManager;
  }

  public ReplicatedClusterStateManager getReplicatedClusterStateManager() {
    return this.rClusterStateMgr;
  }

  public ReplicatedObjectManager getReplicatedObjectManager() {
    return this.rObjectManager;
  }

  public ReplicatedTransactionManager getReplicatedTransactionManager() {
    return this.rTxnManager;
  }

  public GroupManager getGroupManager() {
    return this.groupManager;
  }

  public void l2StateChanged(final StateChangedEvent sce) {
    // someone wants to be notified earlier
    fireStateChangedEvent(sce);

    this.rClusterStateMgr.setCurrentState(sce.getCurrentState());
    this.rTxnManager.l2StateChanged(sce);
    if (sce.movedToActive()) {
      this.rClusterStateMgr.goActiveAndSyncState();
      this.rObjectManager.sync();
      this.server.startActiveMode();
      startL1Listener();
    }
  }

  private void fireStateChangedEvent(final StateChangedEvent sce) {
    for (final StateChangeListener listener : this.listeners) {
      listener.l2StateChanged(sce);
    }
  }

  public void registerForStateChangeEvents(final StateChangeListener listener) {
    this.listeners.add(listener);
  }

  protected void startL1Listener() {
    try {
      this.server.startL1Listener();
    } catch (final IOException e) {
      throw new AssertionError(e);
    }
  }

  public void nodeJoined(final NodeID nodeID) {
    log(nodeID + " joined the cluster");
    if (this.stateManager.isActiveCoordinator()) {
      try {
        this.stateManager.publishActiveState(nodeID);
        this.rClusterStateMgr.publishClusterState(nodeID);
        this.rObjectManager.query(nodeID);
      } catch (final GroupException ge) {
        final String errMesg = "A Terracotta server tried to join the mirror group as a second ACTIVE: " + nodeID
                               + " Zapping it to allow it to join as PASSIVE standby (backup): ";
        logger.error(errMesg, ge);
        this.groupManager.zapNode(nodeID, L2HAZapNodeRequestProcessor.COMMUNICATION_ERROR,
                                  errMesg + L2HAZapNodeRequestProcessor.getErrorString(ge));
      }
    }
  }

  private void log(final String message) {
    logger.info(message);
    this.consoleLogger.info(message);
  }

  private void warn(final String message) {
    logger.warn(message);
    this.consoleLogger.warn(message);
  }

  public void nodeLeft(final NodeID nodeID) {
    warn(nodeID + " left the cluster");
    if (this.stateManager.isActiveCoordinator()) {
      this.rObjectManager.clear(nodeID);
      this.rClusterStateMgr.fireNodeLeftEvent(nodeID);
    } else {
      this.stateManager.startElectionIfNecessary(nodeID);
    }
    this.sequenceGenerator.clearSequenceFor(nodeID);
  }

  public void sequenceCreatedFor(final Object key) throws SequenceGeneratorException {
    final NodeID nodeID = (NodeID) key;
    try {
      this.rTxnManager.publishResetRequest(nodeID);
    } catch (final GroupException ge) {
      logger.error("Error publishing reset counter request node : " + nodeID + " Zapping it : ", ge);
      this.groupManager.zapNode(nodeID,
                                L2HAZapNodeRequestProcessor.COMMUNICATION_ERROR,
                                "Error publishing reset counter for " + nodeID
                                    + L2HAZapNodeRequestProcessor.getErrorString(ge));
      throw new SequenceGeneratorException(ge);
    }
  }

  public void sequenceDestroyedFor(final Object key) {
    // NOP
  }

  private boolean isCleanDB(final PersistentMapStore clusterStateStore) {
    if (clusterStateStore.get(DATABASE_CREATION_TIMESTAMP_KEY) == null) {
      final Calendar cal = Calendar.getInstance();
      final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
      clusterStateStore.put(DATABASE_CREATION_TIMESTAMP_KEY, sdf.format(cal.getTime()));
      return true;
    }
    return false;
  }

  public PrettyPrinter prettyPrint(final PrettyPrinter out) {
    final StringBuilder strBuilder = new StringBuilder();
    strBuilder.append(L2HACoordinator.class.getSimpleName() + " [ ");
    strBuilder.append(this.thisGroupID).append(" ").append(this.l2ObjectStateManager);
    strBuilder.append(" ]");
    out.indent().print(strBuilder.toString()).flush();
    out.indent().print("ReplicatedClusterStateMgr").visit(this.rClusterStateMgr).flush();
    return out;
  }

  public StateSyncManager getStateSyncManager() {
    return stateSyncManager;
  }
}
