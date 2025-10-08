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

import static ch.qos.logback.classic.spi.Configurator.ExecutionStatus.NEUTRAL;
import static com.tc.l2.logging.TCLogbackLogging.CONSOLE;

/**
 *
 */
public class BootstrapConfigurator extends ContextAwareBase implements Configurator {

  @Override
  public ExecutionStatus configure(LoggerContext loggerContext) {
    ch.qos.logback.classic.Logger root = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
    root.detachAndStopAllAppenders();
    root.setLevel(Level.INFO);
    ch.qos.logback.classic.Logger console = loggerContext.getLogger(CONSOLE);
    console.setLevel(Level.INFO);
    return NEUTRAL;
  }
}
