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
package org.terracotta.exlogging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.spi.ContextAwareBase;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 *
 */
public class ExtendedLoggingConfig  extends ContextAwareBase implements Configurator {

  @Override
  public ExecutionStatus configure(LoggerContext context) {
    if (Boolean.getBoolean("CUSTOM_LOGGING")) {
//      context.reset();

      Logger root = context.getLogger(Logger.ROOT_LOGGER_NAME);
      OutputStreamAppender<ILoggingEvent> base = (OutputStreamAppender)root.getAppender("TC_BASE");
      OutputStream baseStream = base.getOutputStream();
      base.setOutputStream(null);
      root.detachAppender(base);
      base.stop();

      Logger console = context.getLogger("org.terracotta.console");
      OutputStreamAppender<ILoggingEvent> sub = new OutputStreamAppender<>();
      sub.setContext(context);
      sub.setOutputStream(baseStream);

      PatternLayoutEncoder defaultEncoder = new PatternLayoutEncoder();
      defaultEncoder.setPattern("%d %p - %m%n");
      defaultEncoder.setParent(this);
      defaultEncoder.setContext(context);
      defaultEncoder.start();

      sub.setEncoder(defaultEncoder);
      sub.start();

      console.addAppender(sub);
      console.setLevel(Level.INFO);

      OutputStreamAppender<ILoggingEvent> append = new OutputStreamAppender<>();
      append.setContext(context);
      append.setImmediateFlush(true);
      try {
        append.setOutputStream(new FileOutputStream("custom-logging.txt"));
      } catch (FileNotFoundException file) {
        
      }

      PatternLayoutEncoder encode = new PatternLayoutEncoder();
      encode.setContext(context);
      encode.setPattern("altered by new pattern - %msg - %d %p%n");
      encode.start();
      append.setEncoder(encode);

      // start the appender now that it has an encoder
      append.start();

      root.addAppender(append);
      context.start();
    }
    return ExecutionStatus.NEUTRAL;
  }

}
