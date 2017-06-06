/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.net.protocol.tcm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.MultiThreadedEventContext;

public class HydrateHandler extends AbstractEventHandler<HydrateContext> {
  private static Logger logger = LoggerFactory.getLogger(HydrateHandler.class);

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
