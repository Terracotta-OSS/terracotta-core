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

import com.tc.l2.state.ConsistencyManagerImpl;
import com.tc.l2.state.ServerMode;
import com.tc.l2.state.StateManager;
import com.tc.test.TCTestCase;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;


public class ConsistencyManagerWeightGeneratorTest extends TCTestCase {
  public void testSimplePassive() throws Exception {
    ConsistencyManagerImpl mgr = new ConsistencyManagerImpl(1, 1);
    ConsistencyManagerImpl spyMgr = spy(mgr);
    StateManager state = mock(StateManager.class);

    when(state.getCurrentMode()).thenReturn(ServerMode.PASSIVE);
    when(state.isActiveCoordinator()).thenReturn(false);
    ConsistencyManagerWeightGenerator gen = new ConsistencyManagerWeightGenerator(()->state, spyMgr);
    assertTrue(gen.getWeight() == 0);
  }
  
  public void testActiveNotBlockedIsHeavierThanBlocked() throws Exception {
    ConsistencyManagerImpl mgr = new ConsistencyManagerImpl(1, 1);
    ConsistencyManagerImpl spyMgr = spy(mgr);
    StateManager state = mock(StateManager.class);
    ConsistencyManagerWeightGenerator gen = new ConsistencyManagerWeightGenerator(()->state, spyMgr);

    when(state.getCurrentMode()).thenReturn(ServerMode.ACTIVE);
    when(state.isActiveCoordinator()).thenReturn(true);
    when(spyMgr.isBlocked()).thenReturn(false);
    long weight = gen.getWeight();
    
    when(spyMgr.isBlocked()).thenReturn(true);
    long w2 = gen.getWeight();
    
    assertTrue(weight > w2);
    assertTrue(weight > 0);
    assertTrue(w2 > 0);
  }
}
