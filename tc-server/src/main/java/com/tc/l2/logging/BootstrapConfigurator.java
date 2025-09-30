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
package com.tc.l2.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.core.spi.ContextAwareBase;
import com.tc.logging.TCLogging;
import org.slf4j.Logger;
import org.terracotta.tripwire.EventAppender;

/**
 *
 */
public class BootstrapConfigurator extends ContextAwareBase implements Configurator {
  
  @Override
  public ExecutionStatus configure(LoggerContext loggerContext) {
    ch.qos.logback.classic.Logger root = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
    
      BufferingAppender appender = new BufferingAppender();
      appender.setName("TC_BASE");
      appender.setContext(loggerContext);
      appender.start();
      root.addAppender(appender);
    
    if (EventAppender.isEnabled()) {
      EventAppender events = new EventAppender();
      events.setName("LogToJFR");
      events.setContext(loggerContext);
      events.start();
      root.addAppender(events);
    }

    ch.qos.logback.classic.Logger silent = loggerContext.getLogger(TCLogging.SILENT_LOGGER_NAME);
    silent.setAdditive(false);
    silent.setLevel(Level.OFF);
    
    if (!loggerContext.isStarted()) {
      root.setLevel(Level.INFO);
      loggerContext.start();
    }
    return ExecutionStatus.INVOKE_NEXT_IF_ANY;
  }
}
