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
import com.tc.async.api.EventHandlerException;
import com.tc.async.impl.MonitoringSink;
import com.tc.async.impl.PipelineMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MonitorHandler extends AbstractEventHandler<Runnable> {
  
  private static final Logger LOGGER = LoggerFactory.getLogger(MonitorHandler.class);

  @Override
  public void handleEvent(Runnable context) throws EventHandlerException {
    PipelineMonitor monitor = MonitoringSink.finish();
    LOGGER.info(monitor.toString());
  }
  
}