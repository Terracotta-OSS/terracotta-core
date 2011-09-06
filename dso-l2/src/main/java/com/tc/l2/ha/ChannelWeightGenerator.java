/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.ha;

import com.tc.l2.ha.WeightGeneratorFactory.WeightGenerator;
import com.tc.object.net.DSOChannelManager;
import com.tc.util.Assert;

public class ChannelWeightGenerator implements WeightGenerator {
  private final DSOChannelManager channelManager;

  public ChannelWeightGenerator(final DSOChannelManager channelManager) {
    Assert.assertNotNull(channelManager);
    this.channelManager = channelManager;
  }

  public long getWeight() {
    // return number of connected clients and are active
    return channelManager.getAllActiveClientConnections().length;
  }

}
