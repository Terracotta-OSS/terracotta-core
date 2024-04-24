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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.l2.api.L2Coordinator;
import com.tc.l2.api.ReplicatedClusterStateManager;
import com.tc.l2.state.ConsistencyManager;
import com.tc.l2.state.ServerMode;
import com.tc.l2.state.StateManager;
import com.tc.net.NodeID;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.net.groups.StripeIDStateManager;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.net.protocol.transport.ConnectionIDFactoryListener;
import com.tc.objectserver.impl.DistributedObjectServer;
import com.tc.objectserver.persistence.Persistor;
import com.tc.util.Assert;
import com.tc.util.State;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;



public class L2HACoordinator implements L2Coordinator, ConnectionIDFactoryListener  {
  private static final Logger logger = LoggerFactory.getLogger(L2HACoordinator.class);

  private final Logger consoleLogger;
  private final DistributedObjectServer                     server;
  private final GroupManager<AbstractGroupMessage> groupManager;

  private final StateManager                                      stateManager;
  private ReplicatedClusterStateManager                     rClusterStateMgr;

  public L2HACoordinator(Logger consoleLogger, DistributedObjectServer server,
                         StateManager stateManager, 
                         GroupManager<AbstractGroupMessage> groupCommsManager,
                         Persistor persistor,
                         WeightGeneratorFactory weightGeneratorFactory,
                         StripeIDStateManager stripeIDStateManager,
                         ConsistencyManager consistencyMgr
                         ) {
    this.consoleLogger = consoleLogger;
    this.server = server;
    this.groupManager = groupCommsManager;
    this.stateManager = stateManager;
    
    init(persistor,
        weightGeneratorFactory, stripeIDStateManager, consistencyMgr);
  }

  private void init(Persistor persistor,
                    WeightGeneratorFactory weightGeneratorFactory,
                    StripeIDStateManager stripeIDStateManager, ConsistencyManager consistencyMgr) {
    final ClusterState clusterState = new ClusterStateImpl(persistor, this.server.getConnectionIdFactory(),
                                                       stripeIDStateManager);

    final L2HAZapNodeRequestProcessor zapProcessor = new L2HAZapNodeRequestProcessor(this.consoleLogger,
                                                                                     this.stateManager,
                                                                                     this.groupManager,
                                                                                     weightGeneratorFactory,
                                                                                     persistor.getClusterStatePersistor(),
                                                                                     consistencyMgr);
    this.groupManager.setZapNodeRequestProcessor(zapProcessor);

    this.rClusterStateMgr = new ReplicatedClusterStateManagerImpl(
                                                                  this.groupManager,
                                                                  stateManager::getCurrentMode,
                                                                  clusterState,
                                                                  this.server.getConfigSetupManager().getConfigurationProvider());
    this.server.getConnectionIdFactory().registerForConnectionIDEvents(this);
  }

  @Override
  public void connectionIDCreated(ConnectionID connectionID) {
    getReplicatedClusterStateManager().connectionIDCreated(connectionID);
  }

  @Override
  public void connectionIDDestroyed(ConnectionID connectionID) {
    getReplicatedClusterStateManager().connectionIDDestroyed(connectionID);
  }
  
  @Override
  public void start() {
    if (this.server.getConfigSetupManager().getConfiguration().isPartialConfiguration()) {
      this.stateManager.moveToDiagnosticMode();
      consoleLogger.info("Started the server in diagnostic mode");
    } else if (this.server.getConfigSetupManager().getConfiguration().isRelaySource()) {
      this.stateManager.moveToRelayMode();
    } else if (this.server.getConfigSetupManager().getConfiguration().isRelayDestination()) {
      this.stateManager.moveToRelayMode();
    }
    this.stateManager.initializeAndStartElection();
  }

  @Override
  public void shutdown() {
    this.stateManager.shutdown();
    shutdownReplicatedClusterStateManager();
  }

  @Override
  public StateManager getStateManager() {
    return this.stateManager;
  }

  private synchronized void shutdownReplicatedClusterStateManager() {
    this.rClusterStateMgr = new ReplicatedClusterStateManager() {
      @Override
      public void goActiveAndSyncState() {
        //  noop
      }

      @Override
      public void publishClusterState(NodeID nodeID) throws GroupException {
        //  noop
      }

      @Override
      public void setCurrentState(State currentState) {
        //  noop
      }

      @Override
      public void reportStateToMap(Map<String, Object> state) {
        state.put("type", "NOOP");
        state.put("state", "SHUTDOWN");
      }

      @Override
      public void connectionIDCreated(ConnectionID connectionID) {
        //  noop
      }

      @Override
      public void connectionIDDestroyed(ConnectionID connectionID) {
        //  noop
      }
    };
  }

  @Override
  public synchronized ReplicatedClusterStateManager getReplicatedClusterStateManager() {
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
      Assert.assertFalse(nodeID.getNodeType() == NodeID.CLIENT_NODE_TYPE);
    } else {
      this.stateManager.startElectionIfNecessary(nodeID);
    }
  }

  @Override
  public Map<String, ?> getStateMap() {
    if(rClusterStateMgr != null) {
      Map<String, Object> state = new LinkedHashMap<>();
      this.rClusterStateMgr.reportStateToMap(state);
      state.put("stateManager", this.stateManager.getStateMap());
      return state;
    } else {
      return Collections.emptyMap();
    }
  }
}
