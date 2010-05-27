/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.remote.protocol.terracotta;

import com.tc.async.api.EventContext;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.JMXAttributeContext;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RemoteJMXAttributeProcessor {
  private final static int      MAXTHREADS = TCPropertiesImpl.getProperties()
                                               .getInt(TCPropertiesConsts.L2_REMOTEJMX_MAXTHREADS);
  private final static int      IDLETIME   = TCPropertiesImpl.getProperties()
                                               .getInt(TCPropertiesConsts.L2_REMOTEJMX_IDLETIME);

  private final static Executor executor   = new ThreadPoolExecutor(1, MAXTHREADS, IDLETIME, TimeUnit.SECONDS,
                                                                    new LinkedBlockingQueue<Runnable>());
  private final static TCLogger logger     = TCLogging.getLogger(RemoteJMXAttributeProcessor.class);

  public void add(final EventContext context) {
    try {
      executor.execute(new Runnable() {
        public void run() {
          JMXAttributeContext attributeContext = (JMXAttributeContext) context;

          JmxRemoteTunnelMessage messageEnvelope = (JmxRemoteTunnelMessage) attributeContext.getChannel()
              .createMessage(TCMessageType.JMXREMOTE_MESSAGE_CONNECTION_MESSAGE);
          messageEnvelope.setTunneledMessage(attributeContext.getOutboundMessage());
          messageEnvelope.send();
        }
      });
    } catch (Throwable t) {
      logger.warn("Got an exception while trying to execute in thread pool: " + t.getMessage());
    }
  }

}
