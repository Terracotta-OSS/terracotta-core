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
import com.tc.l2.handler.L2IndexSyncHandler;
import com.tc.l2.handler.L2IndexSyncRequestHandler;
import com.tc.l2.handler.L2ObjectSyncHandler;
import com.tc.l2.handler.L2ObjectSyncRequestHandler;
import com.tc.l2.handler.L2ObjectSyncSendHandler;
import com.tc.l2.handler.L2StateChangeHandler;
import com.tc.l2.handler.L2StateMessageHandler;
import com.tc.l2.handler.ServerTransactionAckHandler;
import com.tc.l2.handler.TransactionRelayHandler;
import com.tc.l2.msg.GCResultMessage;
import com.tc.l2.msg.IndexSyncAckMessage;
import com.tc.l2.msg.IndexSyncCompleteMessage;
import com.tc.l2.msg.IndexSyncMessage;
import com.tc.l2.msg.IndexSyncStartMessage;
import com.tc.l2.msg.L2StateMessage;
import com.tc.l2.msg.ObjectSyncCompleteMessage;
import com.tc.l2.msg.ObjectSyncMessage;
import com.tc.l2.msg.RelayedCommitTransactionMessage;
import com.tc.l2.msg.ServerRelayedTxnAckMessage;
import com.tc.l2.msg.ServerSyncTxnAckMessage;
import com.tc.l2.objectserver.L2IndexStateManager;
import com.tc.l2.objectserver.L2ObjectStateManager;
import com.tc.l2.objectserver.L2PassiveSyncStateManager;
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
import com.tc.objectserver.search.IndexHACoordinator;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
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

public class L2HACoordinator implements L2Coordinator, GroupEventsListener, SequenceGeneratorListener, PrettyPrintable {

  private static final TCLogger                           logger    = TCLogging.getLogger(L2HACoordinator.class);

  private final TCLogger                                  consoleLogger;
  private final DistributedObjectServer                   server;
  private final GroupManager                              groupManager;
  private final GroupID                                   thisGroupID;

  private StateManager                                    stateManager;
  private ReplicatedObjectManagerImpl                     rObjectManager;
  private ReplicatedTransactionManager                    rTxnManager;
  private ReplicatedClusterStateManager                   rClusterStateMgr;
  private SequenceGenerator                               sequenceGenerator;

  private final SequenceGenerator                         indexSequenceGenerator;
  private final L2ConfigurationSetupManager               configSetupManager;
  private final CopyOnWriteArrayList<StateChangeListener> listeners = new CopyOnWriteArrayList<StateChangeListener>();
  private final L2PassiveSyncStateManager                 l2PassiveSyncStateManager;
  private final L2ObjectStateManager                      l2ObjectStateManager;

  public L2HACoordinator(final TCLogger consoleLogger, final DistributedObjectServer server,
                         final StageManager stageManager, final GroupManager groupCommsManager,
                         final PersistentMapStore persistentStateStore, final ObjectManager objectManager,
                         final IndexHACoordinator indexHACoordinator,
                         final L2PassiveSyncStateManager l2PassiveSyncStateManager,
                         final L2ObjectStateManager l2ObjectStateManager,
                         final L2IndexStateManager l2IndexStateManager,
                         final ServerTransactionManager transactionManager, final ServerGlobalTransactionManager gtxm,
                         final WeightGeneratorFactory weightGeneratorFactory,
                         final L2ConfigurationSetupManager configurationSetupManager, final MessageRecycler recycler,
                         final GroupID thisGroupID, final StripeIDStateManager stripeIDStateManager,
                         final ServerTransactionFactory serverTransactionFactory,
                         DGCSequenceProvider dgcSequenceProvider, SequenceGenerator indexSequenceGenerator) {
    this.consoleLogger = consoleLogger;
    this.server = server;
    this.groupManager = groupCommsManager;
    this.thisGroupID = thisGroupID;
    this.configSetupManager = configurationSetupManager;
    this.l2PassiveSyncStateManager = l2PassiveSyncStateManager;
    this.indexSequenceGenerator = indexSequenceGenerator;
    this.l2ObjectStateManager = l2ObjectStateManager;

    init(stageManager, persistentStateStore, l2ObjectStateManager, l2IndexStateManager, objectManager,
         indexHACoordinator, transactionManager, gtxm, weightGeneratorFactory, recycler, stripeIDStateManager,
         serverTransactionFactory, dgcSequenceProvider);
  }

  private void init(final StageManager stageManager, final PersistentMapStore persistentStateStore,
                    L2ObjectStateManager objectStateManager, L2IndexStateManager l2IndexStateManager,
                    final ObjectManager objectManager, IndexHACoordinator indexHACoordinator,
                    final ServerTransactionManager transactionManager, final ServerGlobalTransactionManager gtxm,
                    final WeightGeneratorFactory weightGeneratorFactory, final MessageRecycler recycler,
                    final StripeIDStateManager stripeIDStateManager,
                    final ServerTransactionFactory serverTransactionFactory, DGCSequenceProvider dgcSequenceProvider) {

    final boolean isCleanDB = isCleanDB(persistentStateStore);
    final int MAX_STAGE_SIZE = TCPropertiesImpl.getProperties().getInt(TCPropertiesConsts.L2_SEDA_STAGE_SINK_CAPACITY);

    final ClusterState clusterState = new ClusterState(persistentStateStore, this.server.getManagedObjectStore(),
                                                       this.server.getConnectionIdFactory(),
                                                       gtxm.getGlobalTransactionIDSequenceProvider(), this.thisGroupID,
                                                       stripeIDStateManager, dgcSequenceProvider);
    final Sink stateChangeSink = stageManager.createStage(ServerConfigurationContext.L2_STATE_CHANGE_STAGE,

    new L2StateChangeHandler(), 1, MAX_STAGE_SIZE).getSink();

    this.stateManager = new StateManagerImpl(this.consoleLogger, this.groupManager, stateChangeSink,
                                             new StateManagerConfigImpl(this.configSetupManager.haConfig()),
                                             createWeightGeneratorFactoryForStateManager(gtxm));
    this.sequenceGenerator = new SequenceGenerator(this);

    final L2HAZapNodeRequestProcessor zapProcessor = new L2HAZapNodeRequestProcessor(this.consoleLogger,
                                                                                     this.stateManager,
                                                                                     this.groupManager,
                                                                                     weightGeneratorFactory);
    zapProcessor.addZapEventListener(new OperatorEventsZapRequestListener(this.configSetupManager));
    this.groupManager.setZapNodeRequestProcessor(zapProcessor);

    final Sink objectsSyncRequestSink = stageManager.createStage(ServerConfigurationContext.OBJECTS_SYNC_REQUEST_STAGE,
                                                                 new L2ObjectSyncRequestHandler(this.sequenceGenerator,
                                                                                                objectStateManager), 1,
                                                                 MAX_STAGE_SIZE).getSink();
    final Sink objectsSyncSink = stageManager.createStage(ServerConfigurationContext.OBJECTS_SYNC_STAGE,
                                                          new L2ObjectSyncHandler(serverTransactionFactory), 1,
                                                          MAX_STAGE_SIZE).getSink();
    stageManager.createStage(ServerConfigurationContext.OBJECTS_SYNC_SEND_STAGE,
                             new L2ObjectSyncSendHandler(objectStateManager, serverTransactionFactory), 1,
                             MAX_STAGE_SIZE);
    stageManager.createStage(ServerConfigurationContext.TRANSACTION_RELAY_STAGE,
                             new TransactionRelayHandler(objectStateManager, this.sequenceGenerator, gtxm), 1,
                             MAX_STAGE_SIZE);
    final Sink ackProcessingSink = stageManager
        .createStage(ServerConfigurationContext.SERVER_TRANSACTION_ACK_PROCESSING_STAGE,
                     new ServerTransactionAckHandler(), 1, MAX_STAGE_SIZE).getSink();
    final Sink stateMessageSink = stageManager.createStage(ServerConfigurationContext.L2_STATE_MESSAGE_HANDLER_STAGE,
                                                           new L2StateMessageHandler(), 1, MAX_STAGE_SIZE).getSink();
    final Sink gcResultSink = stageManager.createStage(ServerConfigurationContext.GC_RESULT_PROCESSING_STAGE,
                                                       new GCResultHandler(), 1, MAX_STAGE_SIZE).getSink();

    // Right now, Index Sync stages should be single threaded.
    final short INDEX_SYNC_STAGE_THREADS = 1;
    final Sink indexSyncRequestSink = stageManager.createStage(ServerConfigurationContext.INDEXES_SYNC_REQUEST_STAGE,
                                                               new L2IndexSyncRequestHandler(l2IndexStateManager),
                                                               INDEX_SYNC_STAGE_THREADS, Integer.MAX_VALUE).getSink();
    final Sink indexSyncSink = stageManager.createStage(ServerConfigurationContext.INDEXES_SYNC_STAGE,
                                                        new L2IndexSyncHandler(indexHACoordinator),
                                                        INDEX_SYNC_STAGE_THREADS, Integer.MAX_VALUE).getSink();

    this.rClusterStateMgr = new ReplicatedClusterStateManagerImpl(
                                                                  this.groupManager,
                                                                  this.stateManager,
                                                                  clusterState,
                                                                  this.server.getConnectionIdFactory(),
                                                                  stageManager
                                                                      .getStage(ServerConfigurationContext.CHANNEL_LIFE_CYCLE_STAGE)
                                                                      .getSink());

    final OrderedSink orderedObjectsSyncSink = new OrderedSink(logger, objectsSyncSink);
    final OrderedSink orderedIndexSyncSink = new OrderedSink(logger, indexSyncSink);

    this.rTxnManager = new ReplicatedTransactionManagerImpl(this.groupManager, orderedObjectsSyncSink,
                                                            transactionManager, gtxm, recycler);

    this.rObjectManager = new ReplicatedObjectManagerImpl(this.groupManager, this.stateManager,
                                                          this.l2PassiveSyncStateManager, this.l2ObjectStateManager,
                                                          this.rTxnManager, objectManager, transactionManager,
                                                          objectsSyncRequestSink, indexSyncRequestSink,
                                                          this.sequenceGenerator, this.indexSequenceGenerator,
                                                          isCleanDB);

    objectStateManager.registerForL2ObjectStateChangeEvents(this.rObjectManager);
    l2IndexStateManager.registerForL2IndexStateChangeEvents(this.rObjectManager);

    this.groupManager.routeMessages(ObjectSyncMessage.class, orderedObjectsSyncSink);
    this.groupManager.routeMessages(ObjectSyncCompleteMessage.class, orderedObjectsSyncSink);

    this.groupManager.routeMessages(IndexSyncStartMessage.class, orderedIndexSyncSink);
    this.groupManager.routeMessages(IndexSyncMessage.class, orderedIndexSyncSink);
    this.groupManager.routeMessages(IndexSyncAckMessage.class, indexSyncRequestSink);
    this.groupManager.routeMessages(IndexSyncCompleteMessage.class, orderedIndexSyncSink);

    this.groupManager.routeMessages(RelayedCommitTransactionMessage.class, orderedObjectsSyncSink);
    this.groupManager.routeMessages(ServerRelayedTxnAckMessage.class, ackProcessingSink);
    this.groupManager.routeMessages(ServerSyncTxnAckMessage.class, ackProcessingSink);
    this.groupManager.routeMessages(L2StateMessage.class, stateMessageSink);
    this.groupManager.routeMessages(GCResultMessage.class, gcResultSink);

    GroupEventsDispatchHandler dispatchHandler = new GroupEventsDispatchHandler();
    dispatchHandler.addListener(this);

    final Sink groupEventsSink = stageManager.createStage(ServerConfigurationContext.GROUP_EVENTS_DISPATCH_STAGE,
                                                          dispatchHandler, 1, MAX_STAGE_SIZE).getSink();
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

  public L2ObjectStateManager getL2ObjectStateManager() {
    return this.l2ObjectStateManager;
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
      this.server.startActiveMode();
      this.rObjectManager.sync();
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
    strBuilder.append(this.thisGroupID).append(" ").append(this.l2PassiveSyncStateManager);
    strBuilder.append(" ]");
    out.indent().print(strBuilder.toString()).flush();
    out.indent().print("ReplicatedClusterStateMgr").visit(this.rClusterStateMgr).flush();
    return out;
  }

  public StateSyncManager getStateSyncManager() {
    return this.l2PassiveSyncStateManager;
  }
}
