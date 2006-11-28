/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.exception.ImplementMe;

public class MockMessageChannelFactory implements ServerMessageChannelFactory {

  public MessageChannelInternal channel;
  public int                    callCount;

  public MessageChannelInternal createNewChannel(ChannelID id) {
    callCount++;
    return channel;
  }

  public TCMessageFactory getMessageFactory() {
    throw new ImplementMe();
  }

  public TCMessageRouter getMessageRouter() {
    throw new ImplementMe();
  }

}