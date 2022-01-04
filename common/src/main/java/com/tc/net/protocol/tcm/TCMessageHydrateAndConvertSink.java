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
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TCMessageHydrateAndConvertSink<T, C> implements TCMessageSink {
  private final Sink<C> destSink;
  private final Function<T, C> converter;
  private final static Logger LOGGER = LoggerFactory.getLogger(TCMessageHydrateAndConvertSink.class);

  public TCMessageHydrateAndConvertSink(Sink<C> destSink, Function<T, C> f) {
    this.destSink = destSink;
    converter = f;
  }

  @Override
  public void putMessage(TCAction message) {    
      try {
        message.hydrate();
        C converted = converter.apply((T)message);
        if (converted != null) {
          this.destSink.addToSink(converted);
        }
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
