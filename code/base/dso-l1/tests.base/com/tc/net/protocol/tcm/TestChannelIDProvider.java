/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.ChannelIDProvider;

public class TestChannelIDProvider implements ChannelIDProvider {
  public ChannelID channelID = new ChannelID(1);

  public ChannelID getChannelID() {
    return this.channelID;
  }
}