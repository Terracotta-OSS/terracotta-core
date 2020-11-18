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
import com.tc.objectserver.persistence.ClusterStatePersistor;
import com.tc.server.TCServer;
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
import static com.tc.l2.state.StateManager.PASSIVE_UNINITIALIZED;
import static com.tc.l2.state.StateManager.START_STATE;
import com.tc.net.ServerID;
import com.tc.objectserver.core.impl.ManagementTopologyEventCollector;
import com.tc.server.TCServerMain;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.Ignore;
import org.terracotta.utilities.test.net.PortManager;

import static java.util.stream.Collectors.toSet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


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
    Logger tcLogger = mock(Logger.class);
    WeightGeneratorFactory weightGeneratorFactory = RandomWeightGenerator.createTestingFactory(2);
    StageManager[] stageManagers = new StageManager[NUM_OF_SERVERS];
    ConsistencyManager mgr = mock(ConsistencyManager.class);
    when(mgr.requestTransition(any(ServerMode.class), any(NodeID.class), any(Transition.class))).thenReturn(Boolean.TRUE);
    when(mgr.requestTransition(any(ServerMode.class), any(NodeID.class), any(), any(Transition.class))).thenReturn(Boolean.TRUE);
    when(mgr.createVerificationEnrollment(any(NodeID.class), any(WeightGeneratorFactory.class))).then(i->{
      return EnrollmentFactory.createTrumpEnrollment((NodeID)i.getArguments()[0], weightGeneratorFactory);
    });
    TCServerMain.server = mock(TCServer.class);
    when(TCServerMain.server.getActivateTime()).thenReturn(System.currentTimeMillis());

    PortManager portManager = PortManager.getInstance();
    ports = portManager.reservePorts(NUM_OF_SERVERS);
    groupPorts = portManager.reservePorts(NUM_OF_SERVERS);
    Set<String> servers = ports.stream().map(r -> "localhost:" + r.port()).collect(toSet());

    this.topologyManager = new TopologyManager(servers);
    for(int i = 0; i < NUM_OF_SERVERS; i++) {
      nodes[i] = new Node(LOCALHOST, ports.get(i).port(), groupPorts.get(i).port());
      nodeSet.add(nodes[i]);
      stageControllers[i] = mock(StageController.class);
      mgmt[i] = mock(ManagementTopologyEventCollector.class);
      ClusterStatePersistor clusterStatePersistorMock = mock(ClusterStatePersistor.class);
      when(clusterStatePersistorMock.isDBClean()).thenReturn(Boolean.TRUE);
//      tcServers[i] = mock(TCServer.class);
      stageManagers[i] = new StageManagerImpl(new ThreadGroup("test"), new QueueFactory());
      groupManagers[i] = new TCGroupManagerImpl(new NullConnectionPolicy(), LOCALHOST, ports.get(i).port(), groupPorts.get(i).port(),
                                                stageManagers[i], weightGeneratorFactory, topologyManager);

      stateManagers[i] = new StateManagerImpl(tcLogger, groupManagers[i], stageControllers[i], mgmt[i], stageManagers[i], NUM_OF_SERVERS, 5, weightGeneratorFactory, mgr,
                                              clusterStatePersistorMock, topologyManager);
      Sink<L2StateMessage> stateMessageSink = stageManagers[i].createStage(ServerConfigurationContext.L2_STATE_MESSAGE_HANDLER_STAGE, L2StateMessage.class, new L2StateMessageHandler(), 1, 1).getSink();
      groupManagers[i].routeMessages(L2StateMessage.class, stateMessageSink);
      groupManagers[i].setDiscover(new TCGroupMemberDiscoveryStatic(groupManagers[i], nodes[i]));

      L2Coordinator l2CoordinatorMock = mock(L2Coordinator.class);
      when(l2CoordinatorMock.getStateManager()).thenReturn(stateManagers[i]);
      ServerConfigurationContext serverConfigurationContextMock = new ServerConfigurationContextImpl(stageManagers[i], null, null, null, l2CoordinatorMock);
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
      if (activeIndex == i) {
//        verify(tcServers[i]).l2StateChanged(argThat(stateChangeEvent(START_STATE, ACTIVE_COORDINATOR)));
        verify(stageControllers[i]).transition(eq(START_STATE), eq(ACTIVE_COORDINATOR));
      } else {
//        verify(tcServers[i]).l2StateChanged(argThat(stateChangeEvent(START_STATE, PASSIVE_UNINITIALIZED)));
        verify(stageControllers[i]).transition(eq(START_STATE), eq(PASSIVE_UNINITIALIZED));
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

    ExecutorService threads = Executors.newCachedThreadPool();
    when(stageManager.createStage(anyString(), any(Class.class), any(EventHandler.class), anyInt(), anyInt()))
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
    StateManagerImpl state = new StateManagerImpl(logger, grp, stageController, mgmtController, stageManager, 1, 5, weightGeneratorFactory, mgr,
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
    ClusterStatePersistor statePersistor = mock(ClusterStatePersistor.class);
    when(statePersistor.isDBClean()).thenReturn(Boolean.TRUE);

    NodeID node = mock(NodeID.class);
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

    when(stageManager.createStage(anyString(), any(Class.class), any(EventHandler.class), anyInt(), anyInt()))
        .then((invoke)->{
          Stage election = mock(Stage.class);
          Sink electionSink = mock(Sink.class);
          doAnswer((invoke2)-> {
              new Thread(() -> {
                try {
                  ((EventHandler)invoke.getArguments()[2]).handleEvent(invoke2.getArguments()[0]);
                } catch (Throwable t) {
                  t.printStackTrace();
                }
              }).start();
            return null;
          }).when(electionSink).addToSink(any());
          when(election.getSink()).thenReturn(electionSink);
          return election;
        });

    ConsistencyManager mgr = mock(ConsistencyManager.class);
    when(mgr.requestTransition(any(ServerMode.class), any(NodeID.class), any(Transition.class))).thenReturn(Boolean.TRUE);
    when(mgr.requestTransition(any(ServerMode.class), any(NodeID.class), any(), any(Transition.class))).thenReturn(Boolean.TRUE);
    when(mgr.createVerificationEnrollment(any(NodeID.class), any(WeightGeneratorFactory.class))).then(i->{
      return EnrollmentFactory.createTrumpEnrollment((NodeID)i.getArguments()[0], weightGeneratorFactory);
    });
    StateManagerImpl state = new StateManagerImpl(logger, grp, stageController, mgmtController, stageManager, 1, 5, weightGeneratorFactory, mgr,
                                                  statePersistor, topologyManager);
    state.initializeAndStartElection();

    state.startElectionIfNecessary(mock(NodeID.class));
    state.waitForElectionsToFinish();
    Assert.assertEquals(node, state.getActiveNodeID());
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
    ClusterStatePersistor persistor = mock(ClusterStatePersistor.class);
    when(persistor.isDBClean()).thenReturn(Boolean.TRUE);
    when(persistor.getInitialState()).thenReturn(StateManager.PASSIVE_SYNCING);
    StateManagerImpl mgr = new StateManagerImpl(tcLogger, groupManager, stageController, mgmtController, stageMgr, 2, 5, weightGeneratorFactory, availabilityMgr, persistor, this.topologyManager);
    mgr.initializeAndStartElection();
    Assert.assertEquals(ServerMode.START, mgr.getCurrentMode());
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
