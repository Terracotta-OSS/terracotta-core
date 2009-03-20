/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.tcm;


public class TestChannelIDProvider implements ChannelIDProvider {
  public ChannelID channelID;

  public TestChannelIDProvider() {
    this(new ChannelID(1));
  }

  public TestChannelIDProvider(ChannelID channelID) {
    this.channelID = channelID;
  }

  public ChannelID getChannelID() {
    return this.channelID;
  }
}