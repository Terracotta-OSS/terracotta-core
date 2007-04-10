/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.async.api.EventContext;
import com.tc.net.protocol.tcm.ChannelID;

public class ChannelStateEventContext implements EventContext {
  public static final int CREATE = 0;
  public static final int REMOVE = 1;

  private final int       type;
  private final ChannelID channelID;

  public ChannelStateEventContext(int type, ChannelID channelID) {
    this.type = type;
    this.channelID = channelID;
    if ((type != CREATE) && (type != REMOVE)) { throw new IllegalArgumentException("invalid type: " + type); }
  }

  public int getType() {
    return type;
  }

  public ChannelID getChannelID() {
    return channelID;
  }
}
