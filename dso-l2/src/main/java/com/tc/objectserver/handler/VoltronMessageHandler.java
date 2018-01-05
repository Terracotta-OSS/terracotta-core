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
import com.tc.async.api.EventHandlerException;
import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.async.impl.DirectEventCreator;
import com.tc.async.impl.MonitoringEventCreator;
import com.tc.entity.VoltronEntityMessage;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VoltronMessageHandler extends AbstractEventHandler<VoltronEntityMessage> {
  private final Sink<VoltronEntityMessage> destSink;
  private boolean useDirect = false;
  private Stage<VoltronEntityMessage> fastPath;
  private boolean activated = false;
  private static final Logger LOGGER = LoggerFactory.getLogger(VoltronMessageHandler.class);

  public VoltronMessageHandler(Sink<VoltronEntityMessage> destSink, boolean use_direct) {
    this.destSink = destSink;
    this.useDirect = use_direct;
  }

  @Override
  public void handleEvent(VoltronEntityMessage message) throws EventHandlerException {
    if (useDirect) {
      boolean fast = fastPath.size() < 2;
      if (fast != activated) {
        activated = fast;
        DirectEventCreator.activate(activated);
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("switching to direct sink activated:" + activated + " with " + fastPath.size());
        }
      }
    }
    if (message.getVoltronType() == VoltronEntityMessage.Type.INVOKE_ACTION) {
      MonitoringEventCreator.start();
    }
    destSink.addSingleThreaded(message);
  }

  @Override
  protected void initialize(ConfigurationContext context) {
    super.initialize(context);
    fastPath = context.getStage(ServerConfigurationContext.SINGLE_THREADED_FAST_PATH, VoltronEntityMessage.class);
  }
  
  
}
