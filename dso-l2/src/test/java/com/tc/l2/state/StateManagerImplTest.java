package com.tc.l2.state;

import com.tc.async.api.Sink;
import com.tc.async.api.StageManager;
import com.tc.async.impl.StageManagerImpl;
import com.tc.config.NodesStore;
import com.tc.config.NodesStoreImpl;
import com.tc.l2.api.L2Coordinator;
import com.tc.l2.context.StateChangedEvent;
import com.tc.l2.ha.RandomWeightGenerator;
import com.tc.l2.ha.WeightGeneratorFactory;
import com.tc.l2.handler.L2StateMessageHandler;
import com.tc.l2.msg.L2StateMessage;
import com.tc.logging.TCLogger;
import com.tc.net.NodeID;
import com.tc.net.groups.Node;
import com.tc.net.groups.TCGroupManagerImpl;
import com.tc.net.groups.TCGroupMemberDiscoveryStatic;
import com.tc.net.protocol.transport.NullConnectionPolicy;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.core.impl.ServerConfigurationContextImpl;
import com.tc.objectserver.persistence.ClusterStatePersistor;
import com.tc.util.PortChooser;
import com.tc.util.concurrent.QueueFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;
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
    TCLogger tcLogger = mock(TCLogger.class);
    PortChooser pc = new PortChooser();
    WeightGeneratorFactory weightGeneratorFactory = RandomWeightGenerator.createTestingFactory(2);
    StageManager[] stageManagers = new StageManager[NUM_OF_SERVERS];

    for(int i = 0; i < NUM_OF_SERVERS; i++) {
      int port = pc.chooseRandom2Port();
      ports[i] = port;
      groupPorts[i] = port + 1;
      nodes[i] = new Node(LOCALHOST, ports[i], groupPorts[i]);
      nodeSet.add(nodes[i]);
      Sink<StateChangedEvent> stageChangeSinkMock = mock(Sink.class);
      ClusterStatePersistor clusterStatePersistorMock = mock(ClusterStatePersistor.class);
      stageManagers[i] = new StageManagerImpl(new ThreadGroup("test"), new QueueFactory<>());
      groupManagers[i] = new TCGroupManagerImpl(new NullConnectionPolicy(), LOCALHOST, ports[i], groupPorts[i], stageManagers[i], null, weightGeneratorFactory);
      stateManagers[i] = new StateManagerImpl(tcLogger, groupManagers[i], stageChangeSinkMock, stageManagers[i], NUM_OF_SERVERS, 5, weightGeneratorFactory,
        clusterStatePersistorMock);
      Sink<L2StateMessage> stateMessageSink = stageManagers[i].createStage(ServerConfigurationContext.L2_STATE_MESSAGE_HANDLER_STAGE, L2StateMessage.class, new L2StateMessageHandler(), 1, 1).getSink();
      groupManagers[i].routeMessages(L2StateMessage.class, stateMessageSink);
      groupManagers[i].setDiscover(new TCGroupMemberDiscoveryStatic(groupManagers[i], nodes[i]));

      L2Coordinator l2CoordinatorMock = mock(L2Coordinator.class);
      when(l2CoordinatorMock.getStateManager()).thenReturn(stateManagers[i]);
      ServerConfigurationContext serverConfigurationContextMock = new ServerConfigurationContextImpl(stageManagers[i], null, null, null, null, l2CoordinatorMock);
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


}
