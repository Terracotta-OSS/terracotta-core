/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.tcm;


public class MockMessageChannelFactory implements ServerMessageChannelFactory {

  public MessageChannelInternal channel;
  public int                    callCount;

  public MessageChannelInternal createNewChannel(ChannelID id) {
    callCount++;
    return channel;
  }

}
