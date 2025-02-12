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

import com.tc.l2.ha.WeightGeneratorFactory.WeightGenerator;
import com.tc.l2.state.StateManager;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.net.DSOChannelManager;
import com.tc.util.Assert;
import java.util.function.Supplier;

public class ChannelWeightGenerator implements WeightGenerator {
  private final DSOChannelManager channelManager;
  private final Supplier<StateManager> stateManager;
  private final boolean isAvailable;

  public ChannelWeightGenerator(Supplier<StateManager> stateManager, DSOChannelManager channelManager, boolean isAvailable) {
    Assert.assertNotNull(channelManager);
    Assert.assertNotNull(stateManager);
    this.channelManager = channelManager;
    this.stateManager = stateManager;
    this.isAvailable = isAvailable;
  }

  @Override
  public long getWeight() {
    int count = 0;
    if (stateManager.get().isActiveCoordinator()) {
      // return number of connected clients and are active
      MessageChannel[] connections = channelManager.getActiveChannels();
      for (MessageChannel c : connections) {
        if (!c.getProductID().isInternal()) {
          count += 1;
        }
      }
    }
    return count;
  }

  @Override
  public boolean isVerificationWeight() {
    return false;
  }
}
