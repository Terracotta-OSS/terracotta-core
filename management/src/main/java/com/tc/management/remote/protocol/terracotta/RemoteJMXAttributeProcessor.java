/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.management.remote.protocol.terracotta;

import com.tc.async.api.EventContext;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.JMXAttributeContext;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.concurrent.NamedThreadFactory;
import com.tc.util.concurrent.ThreadUtil;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RemoteJMXAttributeProcessor {
  private final static TCLogger    logger = TCLogging.getLogger(RemoteJMXAttributeProcessor.class);

  private final ThreadPoolExecutor executor;

  public RemoteJMXAttributeProcessor() {
    int maxThreads = TCPropertiesImpl.getProperties().getInt(TCPropertiesConsts.L2_REMOTEJMX_MAXTHREADS);
    int idleTime = TCPropertiesImpl.getProperties().getInt(TCPropertiesConsts.L2_REMOTEJMX_IDLETIME);

    executor = new ThreadPoolExecutor(1, maxThreads, idleTime, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),
                                      new NamedThreadFactory("RemoteJMXAttributeProcessorWorker"));
  }

  public void add(final EventContext context) {
    try {
      int retries = 0;
      while (true) {
        try {
          executor.execute(new Runnable() {
            @Override
            public void run() {
              JMXAttributeContext attributeContext = (JMXAttributeContext) context;

              JmxRemoteTunnelMessage messageEnvelope = (JmxRemoteTunnelMessage) attributeContext.getChannel()
                  .createMessage(TCMessageType.JMXREMOTE_MESSAGE_CONNECTION_MESSAGE);
              messageEnvelope.setTunneledMessage(attributeContext.getOutboundMessage());
              messageEnvelope.send();
            }
          });
          break;
        } catch (RejectedExecutionException e) {
          ThreadUtil.reallySleep(10);
          retries++;
        }
        if (retries % 100 == 0) {
          logger.warn("JMX Attribute Processor is saturated. Retried processing a request " + retries + " times.");
        }
      }
    } catch (Throwable t) {
      logger.warn("Got an exception while trying to execute in thread pool: " + t.getMessage());
    }
  }

  public void close() {
    executor.shutdownNow();
  }

}
