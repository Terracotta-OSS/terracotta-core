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

import com.tc.async.api.StageManager;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.l2.api.L2Coordinator;
import com.tc.l2.api.ReplicatedClusterStateManager;
import com.tc.l2.operatorevent.OperatorEventsZapRequestListener;
import com.tc.l2.state.StateManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.net.groups.StripeIDStateManager;
import com.tc.objectserver.handler.ChannelLifeCycleHandler;
import com.tc.objectserver.impl.DistributedObjectServer;
import com.tc.objectserver.persistence.ClusterStatePersistor;
import com.tc.text.PrettyPrinter;



public class L2HACoordinator implements L2Coordinator {
  private static final TCLogger logger = TCLogging.getLogger(L2HACoordinator.class);

  private final TCLogger                                    consoleLogger;
  private final DistributedObjectServer                     server;
  private final GroupManager<AbstractGroupMessage> groupManager;
  private final GroupID                                     thisGroupID;

  private StateManager                                      stateManager;
  private ReplicatedClusterStateManager                     rClusterStateMgr;

  private final L2ConfigurationSetupManager                 configSetupManager;

  public L2HACoordinator(TCLogger consoleLogger, DistributedObjectServer server,
                         StageManager stageManager, StateManager stateManager, 
                         GroupManager<AbstractGroupMessage> groupCommsManager,
                         ClusterStatePersistor clusterStatePersistor,
                         WeightGeneratorFactory weightGeneratorFactory,
                         L2ConfigurationSetupManager configurationSetupManager,
                         GroupID thisGroupID, StripeIDStateManager stripeIDStateManager, 
                         ChannelLifeCycleHandler clm) {
    this.consoleLogger = consoleLogger;
    this.server = server;
    this.groupManager = groupCommsManager;
    this.stateManager = stateManager;
    this.thisGroupID = thisGroupID;
    this.configSetupManager = configurationSetupManager;

    init(stageManager, clusterStatePersistor,
        weightGeneratorFactory, stripeIDStateManager, clm);
  }

  private void init(StageManager stageManager, ClusterStatePersistor statePersistor,
                    WeightGeneratorFactory weightGeneratorFactory,
                    StripeIDStateManager stripeIDStateManager, ChannelLifeCycleHandler clm) {
    final ClusterState clusterState = new ClusterStateImpl(statePersistor,
                                                           this.server.getConnectionIdFactory(),
                                                       this.thisGroupID,
                                                       stripeIDStateManager);

    final L2HAZapNodeRequestProcessor zapProcessor = new L2HAZapNodeRequestProcessor(this.consoleLogger,
                                                                                     this.stateManager,
                                                                                     this.groupManager,
                                                                                     weightGeneratorFactory,
                                                                                     statePersistor);
    zapProcessor.addZapEventListener(new OperatorEventsZapRequestListener(this.configSetupManager));
    this.groupManager.setZapNodeRequestProcessor(zapProcessor);

    this.rClusterStateMgr = new ReplicatedClusterStateManagerImpl(
                                                                  this.groupManager,
                                                                  this.stateManager,
                                                                  clusterState,
                                                                  this.server.getConnectionIdFactory(),
                                                                  clm);
    
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
    } else {
      this.stateManager.startElectionIfNecessary(nodeID);
    }
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
}
