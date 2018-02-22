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
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.DirectExecutionMode;
import com.tc.async.api.EventHandlerException;
import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.async.impl.MonitoringEventCreator;
import com.tc.entity.VoltronEntityMessage;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.DSOChannelManagerEventListener;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VoltronMessageHandler extends AbstractEventHandler<VoltronEntityMessage> {
  private Sink<VoltronEntityMessage> destSink;
  private boolean useDirect = false;
  private Stage<VoltronEntityMessage> fastPath;
  private Stage<VoltronEntityMessage> destPath;
  private Stage<?> requestProcessor;
  private Stage<?> requestProcessorSync;
  private boolean activated = false;
  private static final Logger LOGGER = LoggerFactory.getLogger(VoltronMessageHandler.class);
  private final AtomicInteger clientsConnected = new AtomicInteger();
  private final boolean ALWAYS_DIRECT = TCPropertiesImpl.getProperties().getBoolean(TCPropertiesConsts.L2_SEDA_STAGE_SINGLE_THREAD, false);

  public VoltronMessageHandler(DSOChannelManager clients, boolean use_direct) {
    this.useDirect = use_direct;
    clients.addEventListener(new DSOChannelManagerEventListener() {
      @Override
      public void channelCreated(MessageChannel channel) {
        if (!channel.getProductId().isInternal()) {
          clientsConnected.incrementAndGet();
        }
      }

      @Override
      public void channelRemoved(MessageChannel channel, boolean wasActive) {
        if (!channel.getProductId().isInternal()) {
          clientsConnected.decrementAndGet();
        }
      }
    });
  }

  @Override
  public void handleEvent(VoltronEntityMessage message) throws EventHandlerException {
    if (ALWAYS_DIRECT) {
      if (!activated) {
        DirectExecutionMode.activate(true);
        activated = true;
      }
    } else if (useDirect) {
      // only use the fastpath if there is one client connected and nothing in the pipeline
      boolean fast = fastPath.size() < 2 && destPath.isEmpty() && requestProcessor.isEmpty() 
          && requestProcessorSync.isEmpty() && clientsConnected.get() == 1;
      if (fast != activated) {
        activated = fast;
        DirectExecutionMode.activate(activated);
        LOGGER.debug("switching to direct sink activated:{} with {}", activated , fastPath.size());
      }
    }
    if (message.getVoltronType() == VoltronEntityMessage.Type.INVOKE_ACTION) {
      MonitoringEventCreator.start();
    }
    destSink.addToSink(message);
  }

  @Override
  protected void initialize(ConfigurationContext context) {
    ServerConfigurationContext cxt = (ServerConfigurationContext)context;
    super.initialize(context);
    fastPath = context.getStage(ServerConfigurationContext.SINGLE_THREADED_FAST_PATH, VoltronEntityMessage.class);
    destPath = context.getStage(ServerConfigurationContext.VOLTRON_MESSAGE_STAGE, VoltronEntityMessage.class);
    requestProcessor = context.getStage(ServerConfigurationContext.REQUEST_PROCESSOR_STAGE, Object.class);
    requestProcessorSync = context.getStage(ServerConfigurationContext.REQUEST_PROCESSOR_DURING_SYNC_STAGE, Object.class);
    destSink = destPath.getSink();
  }
  
  
}
