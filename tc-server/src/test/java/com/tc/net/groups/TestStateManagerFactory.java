/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.net.groups;

import org.slf4j.Logger;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventHandlerException;
import com.tc.async.api.Sink;
import com.tc.async.api.StageManager;
import com.tc.async.impl.StageController;
import com.tc.l2.context.StateChangedEvent;
import com.tc.l2.ha.RandomWeightGenerator;
import com.tc.l2.msg.L2StateMessage;
import com.tc.l2.state.StateManager;
import com.tc.l2.state.StateManagerImpl;
import com.tc.net.NodeID;
import static com.tc.net.groups.MockStageManagerFactory.createEventHandler;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.impl.TopologyManager;
import com.tc.objectserver.persistence.TestClusterStatePersistor;
import com.tc.l2.state.ConsistencyManager;
import com.tc.l2.state.ConsistencyManager.Transition;
import com.tc.l2.state.ServerMode;
import com.tc.objectserver.core.impl.ManagementTopologyEventCollector;
import com.tc.objectserver.persistence.ClusterPersistentState;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class TestStateManagerFactory {
  
  private final MockStageManagerFactory stageCreator;
  private final Logger logging;

  private StateManagerImpl mgr;
  private Sink stateMsgs;
  
  public TestStateManagerFactory(MockStageManagerFactory stageCreator, GroupManager groupMgr, Logger logging) throws Exception {
    this.stageCreator = stageCreator;
    this.logging = logging;
    this.mgr = createStateManager(groupMgr);
  }

  private StateManagerImpl createStateManager(GroupManager groupMgr) throws Exception {
    MyGroupEventListener gel = new MyGroupEventListener(groupMgr.getLocalNodeID());
    groupMgr.registerForGroupEvents(gel);
     
    StageManager stages = stageCreator.createStageManager();
    
    LateLoadingEventHandler handler = new LateLoadingEventHandler();
    ConsistencyManager cmgr = mock(ConsistencyManager.class);
    StageController controller = mock(StageController.class);
    ManagementTopologyEventCollector mgmt = mock(ManagementTopologyEventCollector.class);
    when(cmgr.requestTransition(any(ServerMode.class), any(NodeID.class), any(Transition.class))).thenReturn(Boolean.TRUE);
    StateManagerImpl mgr = new StateManagerImpl(logging, (n)->true, groupMgr, controller, mgmt, stages, 1, 5,
                                                RandomWeightGenerator.createTestingFactory(2), cmgr,
                                                new ClusterPersistentState(new TestClusterStatePersistor()), mock(TopologyManager.class));
    handler.setMgr(mgr);

    stateMsgs = stages.createStage(ServerConfigurationContext.L2_STATE_MESSAGE_HANDLER_STAGE, L2StateMessage.class, createEventHandler((msg)->mgr.handleClusterStateMessage(msg)), 0).getSink();

    return (mgr);
  }

  public StateManagerImpl getStateManager() {
    return mgr;
  }

  public Sink getStateMessageSink() {
    return stateMsgs;
  }
  
  private class LateLoadingEventHandler extends AbstractEventHandler<StateChangedEvent> {
    
    private StateManager mgr;

    @Override
    public void handleEvent(StateChangedEvent context) throws EventHandlerException {
      mgr.fireStateChangedEvent(context);
    }

    public void setMgr(StateManager mgr) {
      this.mgr = mgr;
    }
  }

  private static class MyGroupEventListener implements GroupEventsListener {

    private final NodeID gmNodeID;

    public MyGroupEventListener(NodeID nodeID) {
      this.gmNodeID = nodeID;
    }

    @Override
    public void nodeJoined(NodeID nodeID) {
      System.err.println("\n### " + gmNodeID + ": nodeJoined -> " + nodeID);
    }

    @Override
    public void nodeLeft(NodeID nodeID) {
      System.err.println("\n### " + gmNodeID + ": nodeLeft -> " + nodeID);
    }
  }  
}
