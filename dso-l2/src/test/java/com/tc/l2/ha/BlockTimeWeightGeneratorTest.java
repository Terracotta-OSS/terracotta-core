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
import com.tc.test.TCTestCase;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;


public class BlockTimeWeightGeneratorTest extends TCTestCase {
  public void testSimpleNotBlocked() throws Exception {
    ConsistencyManagerImpl mgr = new ConsistencyManagerImpl(1, 1);
    ConsistencyManagerImpl spyMgr = spy(mgr);

    BlockTimeWeightGenerator gen = new BlockTimeWeightGenerator(spyMgr);
    assertTrue(gen.getWeight() > 0);

  }
  
  public void testSimpleBlocked() throws Exception {
    ConsistencyManagerImpl mgr = new ConsistencyManagerImpl(1, 1);
    ConsistencyManagerImpl spyMgr = spy(mgr);

    BlockTimeWeightGenerator gen = new BlockTimeWeightGenerator(spyMgr);
    long value = System.currentTimeMillis() - 1000;
    when(spyMgr.getBlockingTimestamp()).thenReturn(value);
    assertTrue(gen.getWeight() < 0);
  }
  
  
  public void testCompareBlocked() throws Exception {
    ConsistencyManagerImpl mgr = new ConsistencyManagerImpl(1, 1);
    ConsistencyManagerImpl spyMgr = spy(mgr);

    BlockTimeWeightGenerator gen = new BlockTimeWeightGenerator(spyMgr);
    when(spyMgr.getBlockingTimestamp()).thenReturn(System.currentTimeMillis() - 100000);
    long val1 = gen.getWeight();
    
    when(spyMgr.getBlockingTimestamp()).thenReturn(System.currentTimeMillis());
    long val2 = gen.getWeight();
    
    assertTrue(val2 > val1);
  }  
}
