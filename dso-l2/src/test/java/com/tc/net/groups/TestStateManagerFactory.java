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
package com.tc.net.groups;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventHandlerException;
import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.async.api.StageManager;
import com.tc.l2.context.StateChangedEvent;
import com.tc.l2.ha.RandomWeightGenerator;
import com.tc.l2.msg.L2StateMessage;
import com.tc.l2.state.StateManager;
import com.tc.l2.state.StateManagerImpl;
import com.tc.logging.TCLogger;
import com.tc.net.NodeID;
import static com.tc.net.groups.MockStageManagerFactory.createEventHandler;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.persistence.TestClusterStatePersistor;

/**
 *
 */
public class TestStateManagerFactory {
  
  private final MockStageManagerFactory stageCreator;
  private final TCLogger logging;

  private StateManagerImpl mgr;
  private Sink stateMsgs;
  
  public TestStateManagerFactory(MockStageManagerFactory stageCreator, GroupManager groupMgr, TCLogger logging) throws Exception {
    this.stageCreator = stageCreator;
    this.logging = logging;
    this.mgr = createStateManager(groupMgr);
  }

  private StateManagerImpl createStateManager(GroupManager groupMgr) throws Exception {
    MyGroupEventListener gel = new MyGroupEventListener(groupMgr.getLocalNodeID());
    groupMgr.registerForGroupEvents(gel);
     
    StageManager stages = stageCreator.createStageManager();
    
    LateLoadingEventHandler handler = new LateLoadingEventHandler();
    Stage stateChange = stages.createStage(ServerConfigurationContext.L2_STATE_CHANGE_STAGE, StateChangedEvent.class, handler, 0, 1024);
    StateManagerImpl mgr = new StateManagerImpl(logging, groupMgr, stateChange.getSink(), stages, 5, RandomWeightGenerator.createTestingFactory(2), new TestClusterStatePersistor());
    handler.setMgr(mgr);

    stateMsgs = stages.createStage(ServerConfigurationContext.L2_STATE_MESSAGE_HANDLER_STAGE, L2StateMessage.class, createEventHandler((msg)->mgr.handleClusterStateMessage(msg)), 0, 1024).getSink();

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
