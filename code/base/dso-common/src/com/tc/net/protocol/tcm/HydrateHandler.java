/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;

public class HydrateHandler extends AbstractEventHandler {
  private static TCLogger logger = TCLogging.getLogger(HydrateHandler.class);

  public void handleEvent(EventContext context) {
    HydrateContext hc = (HydrateContext) context;
    TCMessage message = hc.getMessage();

    final MessageChannel channel = message.getChannel();
    final ChannelID channelID = channel.getChannelID();
    try {
      message.hydrate();
    } catch (Throwable t) {
      try {
        logger.error("Error hydrating message of type " + message.getMessageType() + " on channel " + channelID, t);
      } catch (Throwable t2) {
        // oh well
      }
      message.getChannel().close();
      return;
    }
    hc.getDestSink().add(message);
  }

}
