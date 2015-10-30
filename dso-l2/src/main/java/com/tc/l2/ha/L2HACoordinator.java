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
package com.tc.l2.ha;

import com.tc.async.api.Sink;
import com.tc.async.api.StageManager;
import com.tc.async.impl.StageController;
import com.tc.config.NodesStore;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.l2.api.L2Coordinator;
import com.tc.l2.api.ReplicatedClusterStateManager;
import com.tc.l2.context.StateChangedEvent;
import com.tc.l2.handler.GroupEvent;
import com.tc.l2.handler.GroupEventsDispatchHandler;
import com.tc.l2.handler.GroupEventsDispatchHandler.GroupEventsDispatcher;
import com.tc.l2.handler.L2StateChangeHandler;
import com.tc.l2.handler.L2StateMessageHandler;
import com.tc.l2.msg.L2StateMessage;
import com.tc.l2.operatorevent.OperatorEventsPassiveServerConnectionListener;
import com.tc.l2.operatorevent.OperatorEventsZapRequestListener;
import com.tc.l2.state.StateChangeListener;
import com.tc.l2.state.StateManager;
import com.tc.l2.state.StateManagerConfigImpl;
import com.tc.l2.state.StateManagerImpl;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.GroupEventsListener;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.net.groups.PassiveServerListener;
import com.tc.net.groups.StripeIDStateManager;
import com.tc.objectserver.context.NodeStateEventContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.impl.DistributedObjectServer;
import com.tc.objectserver.persistence.ClusterStatePersistor;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.text.PrettyPrinter;
import com.tc.util.sequence.SequenceGenerator;
import com.tc.util.sequence.SequenceGenerator.SequenceGeneratorException;
import com.tc.util.sequence.SequenceGenerator.SequenceGeneratorListener;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;


public class L2HACoordinator implements L2Coordinator, GroupEventsListener, SequenceGeneratorListener {
  private static final TCLogger logger = TCLogging.getLogger(L2HACoordinator.class);

  private final TCLogger                                    consoleLogger;
  private final DistributedObjectServer                     server;
  private final GroupManager<AbstractGroupMessage> groupManager;
  private final GroupID                                     thisGroupID;

  private StateManager                                      stateManager;
  private ReplicatedClusterStateManager                     rClusterStateMgr;
  private SequenceGenerator                                 sequenceGenerator;

  private final L2ConfigurationSetupManager                 configSetupManager;
  private final CopyOnWriteArrayList<StateChangeListener>   listeners        = new CopyOnWriteArrayList<>();
  private final CopyOnWriteArrayList<PassiveServerListener> passiveListeners = new CopyOnWriteArrayList<>();

  // private final ClusterStatePersistor clusterStatePersistor;

  public L2HACoordinator(TCLogger consoleLogger, DistributedObjectServer server,
                         StageManager stageManager, GroupManager<AbstractGroupMessage> groupCommsManager,
                         ClusterStatePersistor clusterStatePersistor,
                         WeightGeneratorFactory weightGeneratorFactory,
                         L2ConfigurationSetupManager configurationSetupManager,
                         GroupID thisGroupID, StripeIDStateManager stripeIDStateManager,
                         int electionTimInSecs,
                         NodesStore nodesStore) {
    this.consoleLogger = consoleLogger;
    this.server = server;
    this.groupManager = groupCommsManager;
    this.thisGroupID = thisGroupID;
    this.configSetupManager = configurationSetupManager;

    init(stageManager, clusterStatePersistor,
        weightGeneratorFactory, stripeIDStateManager, electionTimInSecs, nodesStore);
  }

  private void init(StageManager stageManager, ClusterStatePersistor statePersistor,
                    WeightGeneratorFactory weightGeneratorFactory,
                    StripeIDStateManager stripeIDStateManager,
                    int electionTimeInSecs,
                    NodesStore nodesStore) {

    final int MAX_STAGE_SIZE = TCPropertiesImpl.getProperties().getInt(TCPropertiesConsts.L2_SEDA_STAGE_SINK_CAPACITY);

    final ClusterState clusterState = new ClusterStateImpl(statePersistor,
                                                           this.server.getConnectionIdFactory(),
                                                       this.thisGroupID,
                                                       stripeIDStateManager);

    
    final Sink<StateChangedEvent> stateChangeSink = stageManager.createStage(ServerConfigurationContext.L2_STATE_CHANGE_STAGE, StateChangedEvent.class, new L2StateChangeHandler(createStageController()), 1, MAX_STAGE_SIZE).getSink();

    this.stateManager = new StateManagerImpl(this.consoleLogger, this.groupManager, stateChangeSink,
                                             new StateManagerConfigImpl(electionTimeInSecs),
                                             weightGeneratorFactory, statePersistor);
    this.sequenceGenerator = new SequenceGenerator(this);

    final L2HAZapNodeRequestProcessor zapProcessor = new L2HAZapNodeRequestProcessor(this.consoleLogger,
                                                                                     this.stateManager,
                                                                                     this.groupManager,
                                                                                     weightGeneratorFactory,
                                                                                     statePersistor);
    zapProcessor.addZapEventListener(new OperatorEventsZapRequestListener(this.configSetupManager));
    this.groupManager.setZapNodeRequestProcessor(zapProcessor);

    final Sink<L2StateMessage> stateMessageSink = stageManager.createStage(ServerConfigurationContext.L2_STATE_MESSAGE_HANDLER_STAGE, L2StateMessage.class, new L2StateMessageHandler(), 1, MAX_STAGE_SIZE).getSink();

    this.rClusterStateMgr = new ReplicatedClusterStateManagerImpl(
                                                                  this.groupManager,
                                                                  this.stateManager,
                                                                  clusterState,
                                                                  this.server.getConnectionIdFactory(),
                                                                  stageManager
                                                                      .getStage(ServerConfigurationContext.CHANNEL_LIFE_CYCLE_STAGE, NodeStateEventContext.class)
                                                                      .getSink());
    this.groupManager.routeMessages(L2StateMessage.class, stateMessageSink);

    GroupEventsDispatchHandler dispatchHandler = new GroupEventsDispatchHandler();
    dispatchHandler.addListener(this);

    final Sink<GroupEvent> groupEventsSink = stageManager.createStage(ServerConfigurationContext.GROUP_EVENTS_DISPATCH_STAGE, GroupEvent.class, dispatchHandler, 1, MAX_STAGE_SIZE).getSink();
    final GroupEventsDispatcher dispatcher = new GroupEventsDispatcher(groupEventsSink);

    this.groupManager.registerForGroupEvents(dispatcher);

    passiveListeners.add(new OperatorEventsPassiveServerConnectionListener(nodesStore));
  }

  private StageController createStageController() {
    StageController control = new StageController();
//  PASSIVE-UNINITIALIZED handle replicate messages right away.  SYNC also needs to be handled
    control.addStageToState(StateManager.PASSIVE_UNINITIALIZED, ServerConfigurationContext.PASSIVE_SYNCHRONIZATION_STAGE);
    control.addStageToState(StateManager.PASSIVE_UNINITIALIZED, ServerConfigurationContext.PASSIVE_REPLICATION_STAGE);
//  REPLICATION needs to continue in STANDBY so include that stage here.  SYNC goes away
    control.addStageToState(StateManager.PASSIVE_STANDBY, ServerConfigurationContext.PASSIVE_REPLICATION_STAGE);
//  turn on the process transaction handler, the active to passive driver, and the replication ack handler, replication handler needs to be shutdown and empty for 
//  active to start
    control.addStageToState(StateManager.ACTIVE_COORDINATOR, ServerConfigurationContext.VOLTRON_MESSAGE_STAGE);
    control.addStageToState(StateManager.ACTIVE_COORDINATOR, ServerConfigurationContext.ACTIVE_TO_PASSIVE_DRIVER_STAGE);
    control.addStageToState(StateManager.ACTIVE_COORDINATOR, ServerConfigurationContext.PASSIVE_REPLICATION_ACK_STAGE);
    return control;
  }

  @Override
  public void start() {
    this.stateManager.startElection();
  }

  @Override
  public StateManager getStateManager() {
    return this.stateManager;
  }

  @Override
  public ReplicatedClusterStateManager getReplicatedClusterStateManager() {
    return this.rClusterStateMgr;
  }

  @Override
  public GroupManager<AbstractGroupMessage> getGroupManager() {
    return this.groupManager;
  }

  @Override
  public void l2StateChanged(StateChangedEvent sce) {
    // someone wants to be notified earlier
    fireStateChangedEvent(sce);

    this.rClusterStateMgr.setCurrentState(sce.getCurrentState());
    if (sce.movedToActive()) {
      this.rClusterStateMgr.goActiveAndSyncState();
      this.server.startActiveMode();
      startL1Listener();
    }
  }

  private void fireStateChangedEvent(StateChangedEvent sce) {
    for (final StateChangeListener listener : this.listeners) {
      listener.l2StateChanged(sce);
    }
  }

  public void registerForStateChangeEvents(StateChangeListener listener) {
    this.listeners.add(listener);
  }

  protected void startL1Listener() {
    try {
      this.server.startL1Listener();
    } catch (final IOException e) {
      throw new AssertionError(e);
    }
  }

  @Override
  public void nodeJoined(NodeID nodeID) {
    log(nodeID + " joined the cluster");
    if (this.stateManager.isActiveCoordinator()) {
      try {
        this.stateManager.publishActiveState(nodeID);
        this.rClusterStateMgr.publishClusterState(nodeID);
      } catch (final GroupException ge) {
        final String errMesg = "A Terracotta server tried to join the mirror group as a second ACTIVE: " + nodeID
                               + " Zapping it to allow it to join as PASSIVE standby (backup): ";
        logger.error(errMesg, ge);
        this.groupManager.zapNode(nodeID, L2HAZapNodeRequestProcessor.COMMUNICATION_ERROR,
                                  errMesg + L2HAZapNodeRequestProcessor.getErrorString(ge));
      }
    } else {
      firePassiveEvent(nodeID, true);
    }
  }

  private void log(String message) {
    logger.info(message);
    this.consoleLogger.info(message);
  }

  private void warn(String message) {
    logger.warn(message);
    this.consoleLogger.warn(message);
  }

  @Override
  public void nodeLeft(NodeID nodeID) {
    warn(nodeID + " left the cluster");
    if (this.stateManager.isActiveCoordinator()) {
      this.rClusterStateMgr.fireNodeLeftEvent(nodeID);
      firePassiveEvent(nodeID, false);
    } else {
      this.stateManager.startElectionIfNecessary(nodeID);
    }
    this.sequenceGenerator.clearSequenceFor(nodeID);
  }

  @Override
  public void sequenceCreatedFor(Object key) throws SequenceGeneratorException {
  }

  @Override
  public void sequenceDestroyedFor(Object key) {
    // NOP
  }

  @Override
  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    final StringBuilder strBuilder = new StringBuilder();
    strBuilder.append(L2HACoordinator.class.getSimpleName() + " [ ");
    strBuilder.append(this.thisGroupID);
    strBuilder.append(" ]");
    out.indent().print(strBuilder.toString()).flush();
    out.indent().print("ReplicatedClusterStateMgr").visit(this.rClusterStateMgr).flush();
    return out;
  }

  private void firePassiveEvent(NodeID nodeID, boolean joined) {
    for (PassiveServerListener listener : this.passiveListeners) {
      if (joined) {
        listener.passiveServerJoined((ServerID) nodeID);
      } else {
        listener.passiveServerLeft((ServerID) nodeID);
      }
    }
  }

}
