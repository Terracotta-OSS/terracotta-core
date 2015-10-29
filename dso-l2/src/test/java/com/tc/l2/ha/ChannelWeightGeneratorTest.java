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

import org.junit.Assert;

import com.tc.net.core.TCConnection;
import com.tc.object.net.DSOChannelManager;
import com.tc.test.TCTestCase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class ChannelWeightGeneratorTest extends TCTestCase {
  public void testSimpleIncreaseDecrease() throws Exception {
    DSOChannelManager mockChannelManager = mock(DSOChannelManager.class);
    ChannelWeightGenerator generator = new ChannelWeightGenerator(mockChannelManager);
    
    TCConnection[] zeroConnections = new TCConnection[0];
    TCConnection[] oneConnection = new TCConnection[1];
    TCConnection[] twoConnections = new TCConnection[2];
    when(mockChannelManager.getAllActiveClientConnections()).thenReturn(zeroConnections);
    Assert.assertTrue(0L == generator.getWeight());
    when(mockChannelManager.getAllActiveClientConnections()).thenReturn(oneConnection);
    Assert.assertTrue(1L == generator.getWeight());
    when(mockChannelManager.getAllActiveClientConnections()).thenReturn(twoConnections);
    Assert.assertTrue(2L == generator.getWeight());
    when(mockChannelManager.getAllActiveClientConnections()).thenReturn(oneConnection);
    Assert.assertTrue(1L == generator.getWeight());
  }
}
