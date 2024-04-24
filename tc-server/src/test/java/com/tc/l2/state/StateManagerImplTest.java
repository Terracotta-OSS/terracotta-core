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
package com.tc.l2.state;

import com.tc.async.api.EventHandler;
import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.async.api.StageManager;
import com.tc.async.impl.StageController;
import com.tc.async.impl.StageManagerImpl;
import com.tc.config.GroupConfiguration;
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
import com.tc.objectserver.impl.TopologyManager;
import com.tc.util.State;
import com.tc.util.concurrent.QueueFactory;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.tc.l2.state.StateManager.ACTIVE_COORDINATOR;
import static com.tc.l2.state.StateManager.BOOTSTRAP_STATE;
import static com.tc.l2.state.StateManager.PASSIVE_UNINITIALIZED;
import com.tc.net.ServerID;
import com.tc.objectserver.core.impl.ManagementTopologyEventCollector;
import com.tc.util.concurrent.ThreadUtil;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.Ignore;
import org.terracotta.utilities.test.net.PortManager;

import static java.util.stream.Collectors.toSet;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.terracotta.server.Server;
import org.terracotta.server.ServerEnv;
import com.tc.objectserver.persistence.ServerPersistentState;


public class StateManagerImplTest {

  private static final int NUM_OF_SERVERS = 3;
  private static final String LOCALHOST = "localhost";
  private List<PortManager.PortRef> ports;
  private List<PortManager.PortRef> groupPorts;
  private final Node[] nodes = new Node[NUM_OF_SERVERS];
  private final TCGroupManagerImpl[] groupManagers = new TCGroupManagerImpl[NUM_OF_SERVERS];
  private final Set<Node> nodeSet = new HashSet<>();
  private TopologyManager topologyManager;

//  private final TCServer[] tcServers = new TCServer[NUM_OF_SERVERS];
  private final StageController[] stageControllers = new StageController[NUM_OF_SERVERS];
  private final ManagementTopologyEventCollector[] mgmt = new ManagementTopologyEventCollector[NUM_OF_SERVERS];
  private final StateManagerImpl[] stateManagers = new StateManagerImpl[NUM_OF_SERVERS];

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() throws Exception {
    ServerEnv.setDefaultServer(mock(Server.class));
    Logger tcLogger = mock(Logger.class);
//    WeightGeneratorFactory weightGeneratorFactory = RandomWeightGenerator.createTestingFactory(2);
    StageManager[] stageManagers = new StageManager[NUM_OF_SERVERS];

    Server server = mock(Server.class);
    when(server.getActivateTime()).thenReturn(System.currentTimeMillis());

    PortManager portManager = PortManager.getInstance();
    ports = portManager.reservePorts(NUM_OF_SERVERS);
    groupPorts = portManager.reservePorts(NUM_OF_SERVERS);
    Set<String> servers = ports.stream().map(r -> "localhost:" + r.port()).collect(toSet());

    this.topologyManager = new TopologyManager(()->servers, ()->-1);
    for(int i = 0; i < NUM_OF_SERVERS; i++) {
      WeightGeneratorFactory wgf = RandomWeightGenerator.createTestingFactory(2);
      ConsistencyManager mgr = mock(ConsistencyManager.class);
      when(mgr.requestTransition(any(ServerMode.class), any(NodeID.class), any(Transition.class))).thenReturn(Boolean.TRUE);
      when(mgr.requestTransition(any(ServerMode.class), any(NodeID.class), any(), any(Transition.class))).thenReturn(Boolean.TRUE);
      when(mgr.createVerificationEnrollment(any(NodeID.class), any(WeightGeneratorFactory.class))).then(invoke->{
        return EnrollmentFactory.createTrumpEnrollment((NodeID)invoke.getArguments()[0], wgf);
      });
      nodes[i] = new Node(LOCALHOST, ports.get(i).port(), groupPorts.get(i).port());
      nodeSet.add(nodes[i]);
      stageControllers[i] = mock(StageController.class);
      mgmt[i] = mock(ManagementTopologyEventCollector.class);
      ServerPersistentState clusterStatePersistorMock = mock(ServerPersistentState.class);
      when(clusterStatePersistorMock.isDBClean()).thenReturn(Boolean.TRUE);
      when(clusterStatePersistorMock.getInitialMode()).thenReturn(ServerMode.INITIAL);
//      tcServers[i] = mock(TCServer.class);
      stageManagers[i] = new StageManagerImpl(new ThreadGroup("test"), new QueueFactory());
      groupManagers[i] = new TCGroupManagerImpl(new NullConnectionPolicy(), LOCALHOST, ports.get(i).port(), groupPorts.get(i).port(),
                                                stageManagers[i], wgf, nodes);

      stateManagers[i] = new StateManagerImpl(tcLogger, (n)->true, groupManagers[i], stageControllers[i], mgmt[i], stageManagers[i], NUM_OF_SERVERS, 5, wgf, mgr,
                                              clusterStatePersistorMock, topologyManager);
      Sink<L2StateMessage> stateMessageSink = stageManagers[i].createStage(ServerConfigurationContext.L2_STATE_MESSAGE_HANDLER_STAGE, L2StateMessage.class, new L2StateMessageHandler(), 1).getSink();
      groupManagers[i].routeMessages(L2StateMessage.class, stateMessageSink);
      groupManagers[i].setDiscover(new TCGroupMemberDiscoveryStatic(groupManagers[i], nodes[i]));

      L2Coordinator l2CoordinatorMock = mock(L2Coordinator.class);
      when(l2CoordinatorMock.getStateManager()).thenReturn(stateManagers[i]);
      ServerConfigurationContext serverConfigurationContextMock = new ServerConfigurationContextImpl("", stageManagers[i], null, null, null, l2CoordinatorMock);
      stageManagers[i].startAll(serverConfigurationContextMock, new ArrayList<>());
    }
  }

  @After
  public void tearDown() {
    if (ports != null) {
      ports.forEach(PortManager.PortRef::close);
    }
    if (groupPorts != null) {
      groupPorts.forEach(PortManager.PortRef::close);
    }
  }

  @Test
  public void testElection() throws Exception {
    for(int i = 0; i < NUM_OF_SERVERS; i++) {
      GroupConfiguration groupConfiguration = mock(GroupConfiguration.class);
      when(groupConfiguration.getNodes()).thenReturn(nodeSet);
      when(groupConfiguration.getCurrentNode()).thenReturn(nodes[i]);
      groupManagers[i].join(groupConfiguration);
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

    int activeIndex = -1;
    for (int i = 0; i < NUM_OF_SERVERS; i++) {
      if (groupManagers[i].getLocalNodeID().equals(expectedActive)) {
        activeIndex = i;
        break;
      }
    }
    
    for (int i = 0; i < NUM_OF_SERVERS; i++) {
      while (stateManagers[i].getCurrentMode() == ServerMode.INITIAL) {
        ThreadUtil.reallySleep(1000);
      }
      while (stateManagers[i].getCurrentMode() == ServerMode.START) {
        ThreadUtil.reallySleep(1000);
      }
      while (stateManagers[i].getCurrentMode() == ServerMode.DIAGNOSTIC) {
        ThreadUtil.reallySleep(1000);
      }
    }

    for (int i = 0; i < NUM_OF_SERVERS; i++) {
      ArgumentCaptor<State> cap = ArgumentCaptor.forClass(State.class);
      if (activeIndex == i) {
        verify(stageControllers[i]).transition(eq(BOOTSTRAP_STATE), cap.capture());
        if (cap.getValue() == StateManager.START_STATE) {
          verify(stageControllers[i]).transition(eq(StateManager.START_STATE), eq(ACTIVE_COORDINATOR));
        }
      } else {
        verify(stageControllers[i]).transition(eq(BOOTSTRAP_STATE), cap.capture());
        if (cap.getValue() == StateManager.START_STATE) {
          verify(stageControllers[i]).transition(eq(StateManager.START_STATE), eq(PASSIVE_UNINITIALIZED));
        }
      }
    }
  }


  @Test @Ignore("no longer a valid test.  Passives can become actives")
  public void testInitialElectionDoesNotSetStandbyAsActive() throws Exception {
    Logger logger = mock(Logger.class);
    GroupManager grp = mock(GroupManager.class);

    StageManager stageManager = mock(StageManager.class);
    StageController stageController = mock(StageController.class);
    ManagementTopologyEventCollector mgmtController = mock(ManagementTopologyEventCollector.class);
    WeightGeneratorFactory weightGeneratorFactory = RandomWeightGenerator.createTestingFactory(2);
    ServerPersistentState statePersistor = mock(ServerPersistentState.class);
    when(statePersistor.isDBClean()).thenReturn(Boolean.TRUE);
    when(statePersistor.getInitialMode()).thenReturn(ServerMode.PASSIVE);

    ServerID node = mock(ServerID.class);
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

    ExecutorService threads = Executors.newCachedThreadPool();
    when(stageManager.createStage(anyString(), any(Class.class), any(EventHandler.class), anyInt()))
        .then((invoke)->{
          Stage election = mock(Stage.class);
          Sink electionSink = mock(Sink.class);
          doAnswer((invoke2)-> {
              threads.execute(()->{
            try {
                ((EventHandler)invoke.getArguments()[2]).handleEvent(invoke2.getArguments()[0]);
            } catch (Throwable t) {
              t.printStackTrace();
            }
              });
            return null;
          }).when(electionSink).addToSink(any());
          when(election.getSink()).thenReturn(electionSink);
          return election;
        });

    ConsistencyManager mgr = mock(ConsistencyManager.class);
    when(mgr.requestTransition(any(ServerMode.class), any(NodeID.class), any(), any(Transition.class))).thenReturn(Boolean.TRUE);
    when(mgr.createVerificationEnrollment(any(NodeID.class), any(WeightGeneratorFactory.class))).then(i->{
      return EnrollmentFactory.createTrumpEnrollment((NodeID)i.getArguments()[0], weightGeneratorFactory);
    });
    StateManagerImpl state = new StateManagerImpl(logger, (n)->true, grp, stageController, mgmtController, stageManager, 1, 5, weightGeneratorFactory, mgr,
                                                  statePersistor, topologyManager);
    state.initializeAndStartElection();

    state.startElectionIfNecessary(mock(NodeID.class));
    state.waitForElectionsToFinish();
    Assert.assertTrue(state.getActiveNodeID().isNull());
    threads.shutdown();
  }

  @Test
  public void testInitialElection() throws Exception {
    Logger logger = mock(Logger.class);
    GroupManager grp = mock(GroupManager.class);

    StageController stageController = mock(StageController.class);
    ManagementTopologyEventCollector mgmtController = mock(ManagementTopologyEventCollector.class);
    StageManager stageManager = mock(StageManager.class);
    WeightGeneratorFactory weightGeneratorFactory = RandomWeightGenerator.createTestingFactory(2);
    ServerPersistentState statePersistor = mock(ServerPersistentState.class);
    when(statePersistor.getInitialMode()).thenReturn(ServerMode.INITIAL);
    when(statePersistor.isDBClean()).thenReturn(Boolean.TRUE);

    ServerID node = mock(ServerID.class);
    when(grp.getLocalNodeID()).thenReturn(node);
    when(grp.sendToAndWaitForResponse(anySet(), any())).thenReturn(new GroupResponse() {
      @Override
      public List getResponses() {
        return Collections.emptyList();
      }

      @Override
      public GroupMessage getResponse(NodeID nodeID) {
        throw new UnsupportedOperationException("Not supported yet.");
      }
    });

    when(stageManager.createStage(anyString(), any(Class.class), any(EventHandler.class), anyInt(), anyInt(), anyBoolean(), anyBoolean()))
        .then((invoke)->mockStage((EventHandler)invoke.getArguments()[2]));
    when(stageManager.createStage(anyString(), any(Class.class), any(EventHandler.class), anyInt()))
        .then((invoke)->mockStage((EventHandler)invoke.getArguments()[2]));
    
    ConsistencyManager mgr = mock(ConsistencyManager.class);
    when(mgr.requestTransition(any(ServerMode.class), any(NodeID.class), any(Transition.class))).thenReturn(Boolean.TRUE);
    when(mgr.requestTransition(any(ServerMode.class), any(NodeID.class), any(), any(Transition.class))).thenReturn(Boolean.TRUE);
    when(mgr.createVerificationEnrollment(any(NodeID.class), any(WeightGeneratorFactory.class))).then(i->{
      return EnrollmentFactory.createTrumpEnrollment((NodeID)i.getArguments()[0], weightGeneratorFactory);
    });
    StateManagerImpl state = new StateManagerImpl(logger, (n)->true, grp, stageController, mgmtController, stageManager, 1, 5, weightGeneratorFactory, mgr,
                                                  statePersistor, topologyManager);
    state.initializeAndStartElection();

    state.startElectionIfNecessary(mock(NodeID.class));
    state.waitForElectionsToFinish();
    Assert.assertEquals(node, state.getActiveNodeID());
  }

  private Stage mockStage(EventHandler handler) {
    Stage election = mock(Stage.class);
    Sink electionSink = mock(Sink.class);
    doAnswer((invoke)-> {
        new Thread(() -> {
          try {
            handler.handleEvent(invoke.getArguments()[0]);
          } catch (Throwable t) {
            t.printStackTrace();
          }
        }).start();
      return null;
    }).when(electionSink).addToSink(any());
    when(election.getSink()).thenReturn(electionSink);
    return election;
  }

  @Test
  public void testElectionWithNodeJoiningLater() throws Exception {

    GroupConfiguration[] groupConfiguration = new GroupConfiguration[NUM_OF_SERVERS];
    for (int i = 0; i < 3; i++) {
      groupConfiguration[i] = mock(GroupConfiguration.class);
      when(groupConfiguration[i].getNodes()).thenReturn(nodeSet);
      when(groupConfiguration[i].getCurrentNode()).thenReturn(nodes[i]);
    }

    groupManagers[0].join(groupConfiguration[0]);
    stateManagers[0].initializeAndStartElection();
    stateManagers[0].waitForDeclaredActive();
    NodeID active = stateManagers[0].getActiveNodeID();

    groupManagers[1].join(groupConfiguration[1]);
    int spin = 0;
    while (spin++ < 5 && 1 > groupManagers[1].getMembers().size()) {
      TimeUnit.SECONDS.sleep(2);
    }
    Assert.assertEquals(1, groupManagers[1].getMembers().size());
    stateManagers[1].initializeAndStartElection();
    stateManagers[1].waitForDeclaredActive();
    Assert.assertEquals(active, stateManagers[1].getActiveNodeID());

    groupManagers[2].join(groupConfiguration[2]);
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
  public void testZapAndSync() throws Exception {
    Logger tcLogger = mock(Logger.class);
    GroupManager groupManager = mock(GroupManager.class);
    StageController stageController = mock(StageController.class);
    ManagementTopologyEventCollector mgmtController = mock(ManagementTopologyEventCollector.class);
    StageManager stageMgr = new StageManagerImpl(new ThreadGroup("test"), new QueueFactory());
    WeightGeneratorFactory weightGeneratorFactory = RandomWeightGenerator.createTestingFactory(2);
    ConsistencyManager availabilityMgr = mock(ConsistencyManager.class);
    when(availabilityMgr.requestTransition(any(ServerMode.class), any(NodeID.class), any(Transition.class))).thenReturn(Boolean.TRUE);
    when(availabilityMgr.createVerificationEnrollment(any(NodeID.class), any(WeightGeneratorFactory.class))).then(i->{
      return EnrollmentFactory.createTrumpEnrollment((NodeID)i.getArguments()[0], weightGeneratorFactory);
    });
    ServerPersistentState persistor = mock(ServerPersistentState.class);
    when(persistor.isDBClean()).thenReturn(Boolean.TRUE);
    when(persistor.getInitialMode()).thenReturn(ServerMode.SYNCING);
    StateManagerImpl mgr = new StateManagerImpl(tcLogger, (n)->true, groupManager, stageController, mgmtController, stageMgr, 2, 5, weightGeneratorFactory, availabilityMgr, persistor, this.topologyManager);
    mgr.initializeAndStartElection();
    Assert.assertEquals(ServerMode.INITIAL, mgr.getCurrentMode());
    Assert.assertEquals(ServerMode.SYNCING, mgr.getStateMap().get("startState"));

    L2StateMessage sw = mock(L2StateMessage.class);
    when(sw.getType()).thenReturn(L2StateMessage.ABORT_ELECTION);
    when(sw.getState()).thenReturn(StateManager.ACTIVE_COORDINATOR);
    Enrollment winner = mock(Enrollment.class);
    when(sw.getEnrollment()).thenReturn(winner);
    ServerID active = mock(ServerID.class);
    when(winner.getNodeID()).thenReturn(active);
    when(winner.wins(any(Enrollment.class))).thenReturn(Boolean.TRUE);
    when(winner.getWeights()).thenReturn(new long[0]);
    when(sw.messageFrom()).thenReturn(active);
    try {
      mgr.handleClusterStateMessage(sw);
      Assert.fail("restart exception expected");
    } catch (Throwable t) {
      t.printStackTrace();
      Assert.assertTrue(t.toString(), t instanceof TCServerRestartException);
    }
  }

  private ArgumentMatcher<StateChangedEvent> stateChangeEvent(State oldState, State currentState) {
    return new ArgumentMatcher<StateChangedEvent>() {
      @Override
      public boolean matches(StateChangedEvent o) {
        if (o instanceof StateChangedEvent) {
          return oldState.equals(((StateChangedEvent)o).getOldState()) && currentState.equals(((StateChangedEvent)o).getCurrentState());
        }
        return false;
      }
    };
  }
}
