/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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