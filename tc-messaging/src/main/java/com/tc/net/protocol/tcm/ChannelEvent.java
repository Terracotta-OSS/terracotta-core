/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import java.util.Date;

public interface ChannelEvent {

  public MessageChannel getChannel();

  public Date getTimestamp();

  public ChannelEventType getType();

  public ChannelID getChannelID();

}
