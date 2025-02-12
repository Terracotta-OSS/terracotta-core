/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
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

import com.tc.l2.state.StateManager;
import org.junit.Assert;

import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.net.DSOChannelManager;
import com.tc.test.TCTestCase;
import com.tc.net.core.ProductID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class ChannelWeightGeneratorTest extends TCTestCase {
  public void testSimpleIncreaseDecrease() throws Exception {
    DSOChannelManager mockChannelManager = mock(DSOChannelManager.class);
    StateManager mockStateManager = mock(StateManager.class);
    ChannelWeightGenerator generator = new ChannelWeightGenerator(()->mockStateManager, mockChannelManager, true);

    Assert.assertEquals(mockPlatform(mockStateManager, mockChannelManager, false, 1, 1), generator.getWeight());
    Assert.assertEquals(mockPlatform(mockStateManager, mockChannelManager, false, 2, 1), generator.getWeight());
    Assert.assertEquals(mockPlatform(mockStateManager, mockChannelManager, true, 2, 1), generator.getWeight());
    Assert.assertEquals(mockPlatform(mockStateManager, mockChannelManager, true, 10, 2), generator.getWeight());
    Assert.assertEquals(mockPlatform(mockStateManager, mockChannelManager, true, 3, 0), generator.getWeight());
    Assert.assertEquals(mockPlatform(mockStateManager, mockChannelManager, true, 1, 1), generator.getWeight());
  }
  
  public int mockPlatform(StateManager state, DSOChannelManager dso, boolean active, int clients, int diagnostics) {
    when(state.isActiveCoordinator()).thenReturn(active);
    MessageChannel[] channels = new MessageChannel[clients + diagnostics];
    for (int x=0;x<clients;x++) {
      channels[x] = mock(MessageChannel.class);
      when(channels[x].getProductID()).thenReturn(ProductID.STRIPE);
    }
    for (int x=clients;x<clients + diagnostics;x++) {
      channels[x] = mock(MessageChannel.class);
      when(channels[x].getProductID()).thenReturn(ProductID.DIAGNOSTIC);
    }
    when(dso.getActiveChannels()).thenReturn(channels);
    return (active) ? clients : 0;
  }
}
