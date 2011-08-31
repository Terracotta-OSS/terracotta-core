/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.async.api.EventContext;

import java.util.Date;

public interface ChannelEvent extends EventContext {

  public MessageChannel getChannel();

  public Date getTimestamp();

  public ChannelEventType getType();

  public ChannelID getChannelID();

}