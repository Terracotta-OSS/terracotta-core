/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package com.tc.l2.ha;

import com.tc.l2.state.ConsistencyManager;
import com.tc.l2.state.ServerMode;
import com.tc.l2.state.StateManager;
import com.tc.net.NodeID;
import com.tc.net.groups.GroupManager;
import com.tc.objectserver.persistence.ClusterStatePersistor;
import com.tc.util.Assert;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mockito;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.slf4j.Logger;

/**
 *
 * @author mscott
 */
public class L2HAZapNodeRequestProcessorTest {
  
  public L2HAZapNodeRequestProcessorTest() {
  }
  
  @BeforeClass
  public static void setUpClass() {
  }
  
  @AfterClass
  public static void tearDownClass() {
  }
  
  @Before
  public void setUp() {
  }
  
  @After
  public void tearDown() {
  }

  @Test
  public void testAcceptingOutgoingZapRequests() {
    Logger logger = mock(Logger.class);
    StateManager state = mock(StateManager.class);
    GroupManager group = mock(GroupManager.class);
    WeightGeneratorFactory factory = mock(WeightGeneratorFactory.class);
    ClusterStatePersistor persistor = mock(ClusterStatePersistor.class);
    ConsistencyManager consistency = mock(ConsistencyManager.class);
    NodeID activeNode = mock(NodeID.class);
    L2HAZapNodeRequestProcessor processor = new L2HAZapNodeRequestProcessor(
          logger, 
          state, 
          group,
          factory, 
          persistor, 
          consistency);
    when(state.isActiveCoordinator()).thenReturn(true);
    when(state.getActiveNodeID()).thenReturn(activeNode);
    when(state.getCurrentMode()).thenReturn(ServerMode.ACTIVE);
    when(consistency.requestTransition(ServerMode.ACTIVE, activeNode, 
            ConsistencyManager.Transition.ZAP_NODE)).thenReturn(Boolean.TRUE);
    boolean accepted = processor.acceptOutgoingZapNodeRequest(
          activeNode, 
          L2HAZapNodeRequestProcessor.COMMUNICATION_ERROR, 
          "test");
    Assert.assertTrue(accepted);
    Mockito.verify(consistency).requestTransition(eq(ServerMode.ACTIVE), 
            eq(activeNode), eq(ConsistencyManager.Transition.ZAP_NODE));
  }
}
