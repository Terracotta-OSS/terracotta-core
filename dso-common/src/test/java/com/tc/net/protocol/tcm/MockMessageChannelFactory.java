/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.tcm;


import com.tc.license.ProductID;

public class MockMessageChannelFactory implements ServerMessageChannelFactory {

  public MessageChannelInternal channel;
  public int                    callCount;

  @Override
  public MessageChannelInternal createNewChannel(ChannelID id, final ProductID productId) {
    callCount++;
    return channel;
  }

}
