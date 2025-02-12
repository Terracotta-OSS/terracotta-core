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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TCMessageHydrateSink<T> implements TCMessageSink {
  private final Sink<T> destSink;
  private final static Logger LOGGER = LoggerFactory.getLogger(TCMessageHydrateSink.class);

  public TCMessageHydrateSink(Sink<T> destSink) {
    this.destSink = destSink;
  }

  @Override
  public void putMessage(TCAction message) {    
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
