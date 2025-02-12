/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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
