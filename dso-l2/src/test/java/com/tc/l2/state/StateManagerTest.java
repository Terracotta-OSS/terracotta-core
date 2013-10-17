package com.tc.l2.state;

import com.tc.async.api.Sink;
import com.tc.l2.ha.WeightGeneratorFactory;
import com.tc.logging.TCLogging;
import com.tc.net.groups.GroupManager;
import com.tc.net.groups.GroupMessage;
import com.tc.objectserver.persistence.ClusterStatePersistor;
import com.tc.objectserver.persistence.TestClusterStatePersistor;
import com.tc.test.TCTestCase;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * @author tim
 */
public class StateManagerTest extends TCTestCase {
  private GroupManager groupManager;
  private StateManager stateManager;
  private StateManagerConfig stateManagerConfig;
  private Sink stateChangeSink;
  private WeightGeneratorFactory weightGeneratorFactory;
  private ClusterStatePersistor clusterStatePersistor;

  @Override
  public void setUp() throws Exception {
    groupManager = mock(GroupManager.class);
    stateManagerConfig = new StateManagerConfigImpl(5);
    stateChangeSink = mock(Sink.class);
    weightGeneratorFactory = spy(new WeightGeneratorFactory());
    clusterStatePersistor = new TestClusterStatePersistor();
    stateManager = new StateManagerImpl(TCLogging.getLogger(getClass()), groupManager,
        stateChangeSink, stateManagerConfig, weightGeneratorFactory, clusterStatePersistor);
  }

  public void testSkipElectionWhenRecoveredPassive() throws Exception {
    Map<String, String> clusterStateMap = new HashMap<String, String>();
    // Simulate going down as PASSIVE_STANDBY, by restarting the clusterStatePersistor and stateManager.
    new TestClusterStatePersistor(clusterStateMap).setCurrentL2State(StateManager.PASSIVE_STANDBY);
    clusterStatePersistor = new TestClusterStatePersistor(clusterStateMap);
    stateManager = new StateManagerImpl(TCLogging.getLogger(getClass()), groupManager,
        stateChangeSink, stateManagerConfig, weightGeneratorFactory, clusterStatePersistor);
    stateManager.startElection();
    verifyElectionDidNotStart();
  }

  public void testSkipElectionWhenRecoveredUnitialized() throws Exception {
    Map<String, String> clusterStateMap = new HashMap<String, String>();
    new TestClusterStatePersistor(clusterStateMap).setCurrentL2State(StateManager.PASSIVE_UNINITIALIZED);
    clusterStatePersistor = new TestClusterStatePersistor(clusterStateMap);
    stateManager = new StateManagerImpl(TCLogging.getLogger(getClass()), groupManager,
        stateChangeSink, stateManagerConfig, weightGeneratorFactory, clusterStatePersistor);
    stateManager.startElection();
    verifyElectionDidNotStart();
  }

  private void verifyElectionDidNotStart() {
    verify(groupManager, never()).sendAll(any(GroupMessage.class));
  }
}
