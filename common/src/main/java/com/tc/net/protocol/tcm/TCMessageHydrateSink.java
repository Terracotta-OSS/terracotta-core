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

import com.tc.async.api.Sink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TCMessageHydrateSink<T> implements TCMessageSink {
  private final Sink<T> destSink;
  private final static Logger LOGGER = LoggerFactory.getLogger(TCMessageHydrateSink.class);

  public TCMessageHydrateSink(Sink<T> destSink) {
    this.destSink = destSink;
  }

  @Override
  public void putMessage(TCMessage message) {    
      try {
        message.hydrate();
        this.destSink.addToSink((T)message);
      } catch (Throwable t) {
        try {
          LOGGER.error("Error hydrating message of type " + message.getMessageType(), t);
        } catch (Throwable t2) {
          // oh well
        }
        message.getChannel().close();
        return;
      }
  }
  
    
  
}
