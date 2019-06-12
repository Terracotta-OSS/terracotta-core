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
import com.tc.net.protocol.tcm.ChannelManagerEventListener;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.net.DSOChannelManager;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.text.PrettyPrintable;
import com.tc.util.Assert;
import com.tc.util.ProductID;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VoltronMessageHandler extends AbstractEventHandler<VoltronEntityMessage> implements PrettyPrintable {
  private Sink<VoltronEntityMessage> destSink;
  private final boolean useDirect;
  private Stage<VoltronEntityMessage> fastPath;
  private Stage<VoltronEntityMessage> destPath;
  private Stage<?> requestProcessor;
  private Stage<?> requestProcessorSync;
  private boolean activated = false;
  private static final Logger LOGGER = LoggerFactory.getLogger(VoltronMessageHandler.class);
  private final AtomicInteger clientsConnected = new AtomicInteger();
  private boolean ALWAYS_DIRECT = TCPropertiesImpl.getProperties().getBoolean(TCPropertiesConsts.L2_SEDA_STAGE_SINGLE_THREAD, false);
  private boolean USE_BACKOFF = TCPropertiesImpl.getProperties().getBoolean(TCPropertiesConsts.L2_SEDA_STAGE_USE_BACKOFF, false);
  private final TimedActivation timer = new TimedActivation();
  
  public VoltronMessageHandler(DSOChannelManager clients, boolean use_direct) {
    this.useDirect = use_direct;
    clients.addEventListener(new ChannelManagerEventListener() {
      @Override
      public void channelCreated(MessageChannel channel) {
        if (channel.getProductID() != ProductID.DIAGNOSTIC) {
          clientsConnected.incrementAndGet();
        }
      }

      @Override
      public void channelRemoved(MessageChannel channel) {
        if (channel.getProductID() != ProductID.DIAGNOSTIC) {
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
      boolean fast = fastPath.size() < 2 && destPath.isEmpty() 
              && requestProcessor.isEmpty() 
              && requestProcessorSync.isEmpty() 
              && clientsConnected.get() == 1;
      timer.update(fast);
      if (activated != fast && timer.shouldFlip(fast)) {
        activated = fast;
        DirectExecutionMode.activate(activated);
        LOGGER.debug("switching to direct sink activated:{} with {}", activated , fastPath.size());
      }
    }
    
    if (!activated && USE_BACKOFF) {
      if (destPath.size() > 8 && fastPath.size() <= 1) {
        timer.backoffWait();
      } else {
        timer.accelerate();
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
  
  public int currentBackoff() {
    return (int)timer.backoffTime;
  }
  
  public boolean currentlyDirect() {
    return activated;
  }
  
  public boolean isDirect() {
    return ALWAYS_DIRECT;
  }
  
  public void setDirect(boolean direct) {
    ALWAYS_DIRECT = direct;
  }
  
  public void setUseBackoff(boolean use) {
    USE_BACKOFF = use;
  }
  
  public boolean isUseBackoff() {
    return USE_BACKOFF;
  }
  
  public long backoffCount() {
    return timer.backoffCount;
  }
  
  public long getMaxBackoffTime() {
    return timer.maxBackoffTime;
  }
  
  private static class TimedActivation {
    private long lastChange;
    private boolean state;
    
    private long backoffTime = 0;
    private long maxBackoffTime = 0;
    private long backoffCount = 0;
    private static final long MAX_BACKOFF = TimeUnit.MICROSECONDS.toNanos(10);
    
    private void update(boolean activate) {
      if (activate != this.state) {
        lastChange = System.currentTimeMillis();
        this.state = activate;
      }
    }
    
    private boolean shouldFlip(boolean requested) {
      return !requested || System.currentTimeMillis() - lastChange > 5000;
    }
    
    private void backoffWait() {
      backoffTime += 2;
      if (backoffTime > MAX_BACKOFF) {
        backoffTime = MAX_BACKOFF;
      }
      try {
        Assert.assertTrue(backoffTime < Integer.MAX_VALUE);
        Thread.sleep(0, (int)backoffTime);
        backoffCount++;
        if (backoffTime > maxBackoffTime) {
          maxBackoffTime = backoffTime;
        }
      } catch (InterruptedException ie) {
        throw new RuntimeException(ie);
      }
    }
    
    private void accelerate() {
      backoffTime >>= 1;
    }
  }

  @Override
  public Map<String, ?> getStateMap() {
    LinkedHashMap<String, Object> map = new LinkedHashMap<>();
    map.put("backoffTime", timer.backoffTime);
    map.put("backoffCount", timer.backoffCount);
    map.put("maxBackoffTime", timer.maxBackoffTime);
    map.put("directMode", this.activated);
    map.put("clientsConnected", this.clientsConnected.get());
    return map;
  }
}
