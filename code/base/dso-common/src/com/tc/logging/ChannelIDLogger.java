/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.logging;

import com.tc.net.protocol.tcm.ChannelIDProvider;

public class ChannelIDLogger extends BaseMessageDecoratorTCLogger {

  private final ChannelIDProvider cidp;

  public ChannelIDLogger(ChannelIDProvider channelIDProvider, TCLogger logger) {
    super(logger);
    this.cidp = channelIDProvider;
  }

  protected Object decorate(Object msg) {
    return cidp.getChannelID() + ": " + msg;
  }

}
