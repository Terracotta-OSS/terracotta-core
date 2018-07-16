package com.tc.l2.state;

import com.tc.async.api.EventHandler;
import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.async.api.StageManager;
import com.tc.async.impl.StageManagerImpl;
import com.tc.config.NodesStore;
import com.tc.config.NodesStoreImpl;
import com.tc.exception.TCServerRestartException;
import com.tc.l2.api.L2Coordinator;
import com.tc.l2.context.StateChangedEvent;
import com.tc.l2.ha.RandomWeightGenerator;
import com.tc.l2.ha.WeightGeneratorFactory;
import com.tc.l2.handler.L2StateMessageHandler;
import com.tc.l2.msg.L2StateMessage;
import com.tc.l2.state.ConsistencyManager.Transition;
import com.tc.net.NodeID;
import com.tc.net.groups.GroupManager;
import com.tc.net.groups.GroupMessage;
import com.tc.net.groups.GroupResponse;
import com.tc.net.groups.Node;
import com.tc.net.groups.TCGroupManagerImpl;
import com.tc.net.groups.TCGroupMemberDiscoveryStatic;
import com.tc.net.protocol.transport.NullConnectionPolicy;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.core.impl.ServerConfigurationContextImpl;
import com.tc.objectserver.persistence.ClusterStatePersistor;
import com.tc.util.PortChooser;
import com.tc.util.State;
import com.tc.util.concurrent.QueueFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class StateManagerImplTest {

  private static final int NUM_OF_SERVERS = 3;
  private static final String LOCALHOST = "localhost";
  private final int[] ports = new int[NUM_OF_SERVERS];
  private final int[] groupPorts = new int[NUM_OF_SERVERS];
  private final Node[] nodes = new Node[NUM_OF_SERVERS];
  private final TCGroupManagerImpl[] groupManagers = new TCGroupManagerImpl[NUM_OF_SERVERS];
  private final Set<Node> nodeSet = new HashSet<>();

  private final StateManagerImpl[] stateManagers = new StateManagerImpl[NUM_OF_SERVERS];

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() throws Exception {
    Logger tcLogger = mock(Logger.class);
    PortChooser pc = new PortChooser();
    WeightGeneratorFactory weightGeneratorFactory = RandomWeightGenerator.createTestingFactory(2);
    StageManager[] stageManagers = new StageManager[NUM_OF_SERVERS];
    ConsistencyManager mgr = mock(ConsistencyManager.class);
    when(mgr.requestTransition(any(ServerMode.class), any(NodeID.class), any(Transition.class))).thenReturn(Boolean.TRUE);
    for(int i = 0; i < NUM_OF_SERVERS; i++) {
      int port = pc.chooseRandom2Port();
      ports[i] = port;
      groupPorts[i] = port + 1;
      nodes[i] = new Node(LOCALHOST, ports[i], groupPorts[i]);
      nodeSet.add(nodes[i]);
      Sink<StateChangedEvent> stageChangeSinkMock = mock(Sink.class);
      ClusterStatePersistor clusterStatePersistorMock = mock(ClusterStatePersistor.class);
      when(clusterStatePersistorMock.isDBClean()).thenReturn(Boolean.TRUE);
      stageManagers[i] = new StageManagerImpl(new ThreadGroup("test"), new QueueFactory());
      groupManagers[i] = new TCGroupManagerImpl(new NullConnectionPolicy(), LOCALHOST, ports[i], groupPorts[i], stageManagers[i], weightGeneratorFactory);

      stateManagers[i] = new StateManagerImpl(tcLogger, groupManagers[i], stageChangeSinkMock, stageManagers[i], NUM_OF_SERVERS, 5, weightGeneratorFactory, mgr, 
        clusterStatePersistorMock);
      Sink<L2StateMessage> stateMessageSink = stageManagers[i].createStage(ServerConfigurationContext.L2_STATE_MESSAGE_HANDLER_STAGE, L2StateMessage.class, new L2StateMessageHandler(), 1, 1).getSink();
      groupManagers[i].routeMessages(L2StateMessage.class, stateMessageSink);
      groupManagers[i].setDiscover(new TCGroupMemberDiscoveryStatic(groupManagers[i], nodes[i]));

      L2Coordinator l2CoordinatorMock = mock(L2Coordinator.class);
      when(l2CoordinatorMock.getStateManager()).thenReturn(stateManagers[i]);
      ServerConfigurationContext serverConfigurationContextMock = new ServerConfigurationContextImpl(stageManagers[i], null, null, null, l2CoordinatorMock);
      stageManagers[i].startAll(serverConfigurationContextMock, new ArrayList<>());
    }
  }

  @Test
  public void testElection() throws Exception {

    NodesStore nodesStore = new NodesStoreImpl(nodeSet);
    for(int i = 0; i < NUM_OF_SERVERS; i++) {
      groupManagers[i].join(nodes[i], nodesStore);
    }

    for(int i = 0; i < NUM_OF_SERVERS; i++) {
      int spin = 0;
      while (spin++ < 5 && 2 > groupManagers[i].getMembers().size()) {
        TimeUnit.SECONDS.sleep(2);
      }
      Assert.assertEquals(2, groupManagers[i].getMembers().size());
    }

    for(int i = 0; i < NUM_OF_SERVERS; i++) {
      stateManagers[i].initializeAndStartElection();
    }

    NodeID[] actives = new NodeID[NUM_OF_SERVERS];
    for(int i = 0; i < NUM_OF_SERVERS; i++) {
      stateManagers[i].waitForDeclaredActive();
      actives[i] = stateManagers[i].getActiveNodeID();
    }

    NodeID expectedActive = actives[0];
    for(int i = 1; i < NUM_OF_SERVERS; i++) {
      Assert.assertEquals(expectedActive, actives[i]);
    }
  }
  
    
  @Test
  public void testInitialElectionDoesNotSetStandbyAsActive() throws Exception {
    Logger logger = mock(Logger.class);
    GroupManager grp = mock(GroupManager.class);
    
    Sink stageChangeSinkMock = mock(Sink.class);
    StageManager stageManager = mock(StageManager.class);
    WeightGeneratorFactory weightGeneratorFactory = RandomWeightGenerator.createTestingFactory(2);
    ClusterStatePersistor statePersistor = mock(ClusterStatePersistor.class);
    when(statePersistor.isDBClean()).thenReturn(Boolean.TRUE);
    when(statePersistor.getInitialState()).thenReturn(new State("PASSIVE-STANDBY"));

    NodeID node = mock(NodeID.class);
    when(grp.getLocalNodeID()).thenReturn(node);
    when(grp.sendAllAndWaitForResponse(any())).thenReturn(new GroupResponse() {
      @Override
      public List getResponses() {
        return Collections.emptyList();
      }

      @Override
      public GroupMessage getResponse(NodeID nodeID) {
        throw new UnsupportedOperationException("Not supported yet.");
      }
    });
    
    when(stageManager.createStage(anyString(), any(Class.class), any(EventHandler.class), anyInt(), anyInt()))
        .then((invoke)->{
          Stage election = mock(Stage.class);
          Sink electionSink = mock(Sink.class);
          doAnswer((invoke2)-> {
            try {
              ((EventHandler)invoke.getArguments()[2]).handleEvent(invoke2.getArguments()[0]);
            } catch (Throwable t) {
              t.printStackTrace();
            }
            return null;
          }).when(electionSink).addToSink(any());
          when(election.getSink()).thenReturn(electionSink);
          return election;
        });
    
    ConsistencyManager mgr = mock(ConsistencyManager.class);
    when(mgr.requestTransition(any(ServerMode.class), any(NodeID.class), any(Transition.class))).thenReturn(Boolean.TRUE);
    StateManagerImpl state = new StateManagerImpl(logger, grp, stageChangeSinkMock, stageManager, 1, 5, weightGeneratorFactory, mgr, 
          statePersistor);
    state.initializeAndStartElection();
    
    state.startElectionIfNecessary(mock(NodeID.class));
    state.waitForElectionsToFinish();
    Assert.assertTrue(state.getActiveNodeID().isNull());
  }
  
  @Test
  public void testInitialElection() throws Exception {
    Logger logger = mock(Logger.class);
    GroupManager grp = mock(GroupManager.class);
    
    Sink stageChangeSinkMock = mock(Sink.class);
    StageManager stageManager = mock(StageManager.class);
    WeightGeneratorFactory weightGeneratorFactory = RandomWeightGenerator.createTestingFactory(2);
    ClusterStatePersistor statePersistor = mock(ClusterStatePersistor.class);
    when(statePersistor.isDBClean()).thenReturn(Boolean.TRUE);

    NodeID node = mock(NodeID.class);
    when(grp.getLocalNodeID()).thenReturn(node);
    when(grp.sendAllAndWaitForResponse(any())).thenReturn(new GroupResponse() {
      @Override
      public List getResponses() {
        return Collections.emptyList();
      }

      @Override
      public GroupMessage getResponse(NodeID nodeID) {
        throw new UnsupportedOperationException("Not supported yet.");
      }
    });
    
    when(stageManager.createStage(anyString(), any(Class.class), any(EventHandler.class), anyInt(), anyInt()))
        .then((invoke)->{
          Stage election = mock(Stage.class);
          Sink electionSink = mock(Sink.class);
          doAnswer((invoke2)-> {
            try {
              ((EventHandler)invoke.getArguments()[2]).handleEvent(invoke2.getArguments()[0]);
            } catch (Throwable t) {
              t.printStackTrace();
            }
            return null;
          }).when(electionSink).addToSink(any());
          when(election.getSink()).thenReturn(electionSink);
          return election;
        });
    
    ConsistencyManager mgr = mock(ConsistencyManager.class);
    when(mgr.requestTransition(any(ServerMode.class), any(NodeID.class), any(Transition.class))).thenReturn(Boolean.TRUE);
    StateManagerImpl state = new StateManagerImpl(logger, grp, stageChangeSinkMock, stageManager, 1, 5, weightGeneratorFactory, mgr, 
          statePersistor);
    state.initializeAndStartElection();
    
    state.startElectionIfNecessary(mock(NodeID.class));
    state.waitForElectionsToFinish();
    Assert.assertEquals(node, state.getActiveNodeID());
  }
  
  @Test
  public void testElectionWithNodeJoiningLater() throws Exception {

    NodesStore nodesStore = new NodesStoreImpl(nodeSet);
    groupManagers[0].join(nodes[0], nodesStore);
    stateManagers[0].initializeAndStartElection();
    stateManagers[0].waitForDeclaredActive();
    NodeID active = stateManagers[0].getActiveNodeID();

    groupManagers[1].join(nodes[1], nodesStore);
    int spin = 0;
    while (spin++ < 5 && 1 > groupManagers[1].getMembers().size()) {
      TimeUnit.SECONDS.sleep(2);
    }
    Assert.assertEquals(1, groupManagers[1].getMembers().size());
    stateManagers[1].initializeAndStartElection();
    stateManagers[1].waitForDeclaredActive();
    Assert.assertEquals(active, stateManagers[1].getActiveNodeID());

    groupManagers[2].join(nodes[2], nodesStore);
    spin = 0;
    while (spin++ < 5 && 2 > groupManagers[2].getMembers().size()) {
      TimeUnit.SECONDS.sleep(2);
    }
    Assert.assertEquals(2, groupManagers[2].getMembers().size());
    stateManagers[2].initializeAndStartElection();
    stateManagers[2].waitForDeclaredActive();
    Assert.assertEquals(active, stateManagers[2].getActiveNodeID());
  }

  @Test
  public void testStateConversion() {
    ServerMode mode = StateManager.convert(StateManager.ACTIVE_COORDINATOR);
    com.tc.util.Assert.assertEquals(ServerMode.ACTIVE, mode);
  }
  
  @Test
  public void testZapAndSync() {
    Logger tcLogger = mock(Logger.class);
    GroupManager groupManager = mock(GroupManager.class);
    Sink stateChangeSink = mock(Sink.class);
    StageManager stageMgr = new StageManagerImpl(new ThreadGroup("test"), new QueueFactory());
    WeightGeneratorFactory weightGeneratorFactory = RandomWeightGenerator.createTestingFactory(2);
    ConsistencyManager availabilityMgr = mock(ConsistencyManager.class);
    when(availabilityMgr.requestTransition(any(ServerMode.class), any(NodeID.class), any(Transition.class))).thenReturn(Boolean.TRUE);
    ClusterStatePersistor persistor = mock(ClusterStatePersistor.class);
    when(persistor.isDBClean()).thenReturn(Boolean.TRUE);
    when(persistor.getInitialState()).thenReturn(StateManager.PASSIVE_SYNCING);
    StateManagerImpl mgr = new StateManagerImpl(tcLogger, groupManager, stateChangeSink, stageMgr, 2, 5, weightGeneratorFactory, availabilityMgr, persistor);
    mgr.initializeAndStartElection();
    Assert.assertEquals(ServerMode.START, mgr.getCurrentMode());
    Assert.assertEquals(ServerMode.SYNCING, mgr.getStateMap().get("startState"));
    
    L2StateMessage sw = mock(L2StateMessage.class);
    when(sw.getType()).thenReturn(L2StateMessage.ABORT_ELECTION);
    when(sw.getState()).thenReturn(StateManager.ACTIVE_COORDINATOR);
    when(sw.getEnrollment()).thenReturn(mock(Enrollment.class));

    try {
      mgr.handleClusterStateMessage(sw);
      Assert.fail();
    } catch (TCServerRestartException expected) {
      
    }
    
    verify(persistor).setDBClean(eq(Boolean.FALSE));
  }
}
