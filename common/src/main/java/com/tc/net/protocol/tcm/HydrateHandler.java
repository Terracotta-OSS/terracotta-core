/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.MultiThreadedEventContext;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;

public class HydrateHandler extends AbstractEventHandler<HydrateContext> {
  private static TCLogger logger = TCLogging.getLogger(HydrateHandler.class);

  @Override
  public void handleEvent(HydrateContext hc) {
    TCMessage message = hc.getMessage();

    try {
      message.hydrate();
    } catch (Throwable t) {
      try {
        logger.error("Error hydrating message of type " + message.getMessageType(), t);
      } catch (Throwable t2) {
        // oh well
      }
      message.getChannel().close();
      return;
    }
    // TODO: Rationalize this hack to explicitly know whether this is multi-threaded, or not.
    // This hack is just a stop-gap to phase in the SEDA types in smaller changes.
    if (message instanceof MultiThreadedEventContext) {
      hc.getDestSink().addMultiThreaded(message);
    } else {
      hc.getDestSink().addSingleThreaded(message);
    }
  }

}
