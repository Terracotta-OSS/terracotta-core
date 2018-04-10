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

import com.tc.l2.ha.WeightGeneratorFactory.WeightGenerator;
import com.tc.l2.state.StateManager;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.net.DSOChannelManager;
import com.tc.util.Assert;
import com.tc.util.ProductID;
import java.util.function.Supplier;

public class ChannelWeightGenerator implements WeightGenerator {
  private final DSOChannelManager channelManager;
  private final Supplier<StateManager> stateManager;

  public ChannelWeightGenerator(Supplier<StateManager> stateManager, DSOChannelManager channelManager) {
    Assert.assertNotNull(channelManager);
    Assert.assertNotNull(stateManager);
    this.channelManager = channelManager;
    this.stateManager = stateManager;
  }

  @Override
  public long getWeight() {
    int count = 0;
    if (stateManager.get().isActiveCoordinator()) {
      // return number of connected clients and are active
      MessageChannel[] connections = channelManager.getActiveChannels();
      for (MessageChannel c : connections) {
        if (!c.getProductId().isInternal()) {
          count += 1;
        }
      }
    }
    return count;
  }

}
