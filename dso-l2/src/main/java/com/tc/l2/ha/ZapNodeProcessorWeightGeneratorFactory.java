/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.ha;

import com.tc.object.net.DSOChannelManager;

public class ZapNodeProcessorWeightGeneratorFactory extends WeightGeneratorFactory {
  public ZapNodeProcessorWeightGeneratorFactory(DSOChannelManager channelManager,
                                                String host, int port) {
    super();

    add(new ChannelWeightGenerator(channelManager));
    add(new ServerIdentifierWeightGenerator(host, port));
    // add a random generator to break tie
    add(RANDOM_WEIGHT_GENERATOR);

  }
}
