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

import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.async.api.StageManager;
import com.tc.l2.ha.WeightGeneratorFactory;
import com.tc.logging.TCLogging;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.GroupManager;
import com.tc.objectserver.persistence.ClusterStatePersistor;
import com.tc.objectserver.persistence.TestClusterStatePersistor;
import com.tc.test.TCTestCase;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.mockito.Matchers;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author tim
 */
public class StateManagerTest extends TCTestCase {
  private GroupManager groupManager;
  private StateManager stateManager;
  private StateManagerConfig stateManagerConfig;
  private final StageManager mgr = mock(StageManager.class);
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
    Stage stage = mock(Stage.class);
    when(mgr.createStage(Matchers.anyString(), Matchers.any(), Matchers.any(), Matchers.anyInt(), Matchers.anyInt()))
        .thenReturn(stage);
    Sink sink = mock(Sink.class);
    when(stage.getSink()).thenReturn(sink);    
    stateManager = new StateManagerImpl(TCLogging.getLogger(getClass()), groupManager,
      stateChangeSink, mgr, stateManagerConfig, weightGeneratorFactory, clusterStatePersistor);
  }

  public void testSkipElectionWhenRecoveredPassive() throws Exception {
    Map<String, Serializable> clusterStateMap = new HashMap<>();
    // Simulate going down as PASSIVE_STANDBY, by restarting the clusterStatePersistor and stateManager.
    new TestClusterStatePersistor(clusterStateMap).setCurrentL2State(StateManager.PASSIVE_STANDBY);
    clusterStatePersistor = new TestClusterStatePersistor(clusterStateMap);
    stateManager = new StateManagerImpl(TCLogging.getLogger(getClass()), groupManager,
        stateChangeSink, mgr, stateManagerConfig, weightGeneratorFactory, clusterStatePersistor);
    stateManager.startElection();
    verifyElectionDidNotStart();
  }

  public void testSkipElectionWhenRecoveredUnitialized() throws Exception {
    Map<String, Serializable> clusterStateMap = new HashMap<>();
    new TestClusterStatePersistor(clusterStateMap).setCurrentL2State(StateManager.PASSIVE_UNINITIALIZED);
    clusterStatePersistor = new TestClusterStatePersistor(clusterStateMap);
    stateManager = new StateManagerImpl(TCLogging.getLogger(getClass()), groupManager,
        stateChangeSink, mgr, stateManagerConfig, weightGeneratorFactory, clusterStatePersistor);
    stateManager.startElection();
    verifyElectionDidNotStart();
  }

  private void verifyElectionDidNotStart() {
    verify(groupManager, never()).sendAll(any(AbstractGroupMessage.class));
  }
}
