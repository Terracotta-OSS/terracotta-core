/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2026
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
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.classic.spi.ConfiguratorRank;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.util.FileSize;
import com.tc.classloader.ServiceLocator;
import com.tc.logging.TCLogging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.tripwire.EventAppender;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TCLogbackLogging {

  public static final String CONSOLE = TCLogging.CONSOLE_LOGGER_NAME;
  public static final String STDOUT_APPENDER = "STDOUT";
  private static final String TC_PATTERN = "%d [%t] %p %c - %m%n";
  private static final Logger LOGGER = LoggerFactory.getLogger(CONSOLE);

  public static void resetLogging() {
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    ch.qos.logback.classic.Logger root = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
    ch.qos.logback.classic.Logger console = loggerContext.getLogger(CONSOLE);
    root.detachAndStopAllAppenders();
    console.detachAndStopAllAppenders();
    loggerContext.reset();
  }

  public static void setServerName(String name) {
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    String currentName = loggerContext.getName();
    if (currentName == null || currentName.equals(CoreConstants.DEFAULT_CONTEXT_NAME)) {
      loggerContext.setName(name);
    } else if (!name.equals(currentName)) {
      throw new RuntimeException("server names do not match exsiting:" + loggerContext.getName() + " given:" + name);
    }
  }

  public static void bootstrapLogging(OutputStream out, ServiceLocator locator) {
    bootstrapLogging(out);
    applyExtraLoggingConfig(locator);
  }

  private static void applyExtraLoggingConfig(ServiceLocator locator) {
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    List<Class<? extends Configurator>> configEx = new ArrayList<>(locator.getImplementations(Configurator.class));
    configEx.sort((o1, o2) -> {
      ConfiguratorRank r1 = o1.getAnnotation(ConfiguratorRank.class);
      ConfiguratorRank r2 = o2.getAnnotation(ConfiguratorRank.class);
      int value1 = r1 == null ? ConfiguratorRank.DEFAULT : r1.value();
      int value2 = r2 == null ? ConfiguratorRank.DEFAULT : r2.value();
      return Integer.compare(value2, value1);
    });
    Iterator<Class<? extends Configurator>> clist = configEx.iterator();
    while (clist.hasNext()) {
      try {
        Class<? extends Configurator> c = clist.next();
        Configurator config = c.getDeclaredConstructor().newInstance();
        if (config.configure(loggerContext) == Configurator.ExecutionStatus.DO_NOT_INVOKE_NEXT_IF_ANY) {
          break;
        }
      } catch (Throwable e) {
        clist.remove();
      }
    }
  }

  public static void bootstrapLogging(OutputStream out) {
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    ch.qos.logback.classic.Logger root = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);

    Iterator<Appender<ILoggingEvent>> appenders = root.iteratorForAppenders();
    boolean hasBuffer = false;
    boolean hasJfr = false;
    while (appenders.hasNext()) {
      Appender<ILoggingEvent> check = appenders.next();
      if (check instanceof BufferingAppender) {
        hasBuffer = true;
        if (out != null) {
          ((BufferingAppender)check).setOutputStream(out);
        }
      } else if (check instanceof EventAppender) {
        hasJfr = true;
      } else if (check instanceof ConsoleAppender && check.getName().equals("console")) {
        // this is from the basic configurator in logback, we don't want it.
        check.stop();
        root.detachAppender(check);
      }
    }
    if (!hasBuffer) {
      BufferingAppender appender = new BufferingAppender();
      appender.setName("TC_BASE");
      appender.setContext(loggerContext);
      if (out != null) {
        appender.setOutputStream(out);
      }
      appender.start();
      root.addAppender(appender);
    }

    if (!hasJfr && EventAppender.isEnabled()) {
      EventAppender events = new EventAppender();
      events.setName("LogToJFR");
      events.setContext(loggerContext);
      events.start();
      root.addAppender(events);
    }

    ch.qos.logback.classic.Logger silent = loggerContext.getLogger(TCLogging.SILENT_LOGGER_NAME);
    silent.setAdditive(false);
    silent.setLevel(Level.OFF);
  }

  public static void redirectLogging(File logDirFile) {
    String logDir = getPathString(logDirFile);
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    ch.qos.logback.classic.Logger root = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);

    if (root.getAppender("TC_BASE") != null && logDirFile != null) {
      Appender<ILoggingEvent> continuingAppender = installFileAppender(logDir, loggerContext);
      root.addAppender(continuingAppender);
      disableBufferingAppender(continuingAppender);
    } else {
      disableBufferingAppender(null);
    }
  }

  private static void disableBufferingAppender(Appender<ILoggingEvent> continuingAppender) {
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    ch.qos.logback.classic.Logger root = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
    ch.qos.logback.classic.Logger console = loggerContext.getLogger(CONSOLE);

    Iterator<Appender<ILoggingEvent>> appenders = root.iteratorForAppenders();
    if (appenders != null) {
      while (appenders.hasNext()) {
        Appender<ILoggingEvent> current = appenders.next();
        if (current instanceof BufferingAppender) {
          // if there is a continuing appender, move the buffering to console and turn off
          // buffering.  If no continuing appender just shut off buffering
          if (continuingAppender != null) {
            root.detachAppender(current);
            ((BufferingAppender) current).sendContentsTo(continuingAppender::doAppend);
            root.addAppender(continuingAppender);
            console.addAppender(current);
          } else {
            ((BufferingAppender) current).sendContentsTo(e->{});
          }
        }
      }
    }
  }

  private static Appender<ILoggingEvent> installFileAppender(String logDir, LoggerContext loggerContext) {
    String logLocation = logDir + File.separator + "terracotta.server.log";
    LOGGER.info("Log file: {}", logLocation);

    RollingFileAppender<ILoggingEvent> fileAppender = new RollingFileAppender<>();
    fileAppender.setName("ROLLING");
    fileAppender.setContext(loggerContext);
    fileAppender.setFile(logLocation);

    PatternLayoutEncoder logEncoder = new PatternLayoutEncoder();
    logEncoder.setContext(loggerContext);
    logEncoder.setPattern(TC_PATTERN);
    logEncoder.start();

    fileAppender.setEncoder(logEncoder);

    FixedWindowRollingPolicy rollingPolicy = new FixedWindowRollingPolicy();
    rollingPolicy.setMinIndex(1);
    rollingPolicy.setMaxIndex(20);
    rollingPolicy.setFileNamePattern(logDir + File.separator + "terracotta.server.%i.log");
    rollingPolicy.setContext(loggerContext);
    rollingPolicy.setParent(fileAppender);
    rollingPolicy.start();
    fileAppender.setRollingPolicy(rollingPolicy);

    StartupAndSizeBasedTriggeringPolicy<ILoggingEvent> triggeringPolicy = new StartupAndSizeBasedTriggeringPolicy<>();
    triggeringPolicy.setMaxFileSize(FileSize.valueOf("512MB"));
    triggeringPolicy.setContext(loggerContext);
    triggeringPolicy.start();
    fileAppender.setTriggeringPolicy(triggeringPolicy);

    fileAppender.start();

    return fileAppender;
  }

  private static String getPathString(File logPath) {
    if (logPath == null) {
      LOGGER.info("Logging directory is not set. Logging only to the console");
      return null;
    } else if (!logPath.exists()) {
      if (!logPath.mkdirs()) {
        throw new RuntimeException("Failed to created logging directory " + logPath);
      } else {
        LOGGER.info("Created logging directory {}", logPath);
      }
    } else if (!logPath.isDirectory()) {
      throw new RuntimeException("Logging path " + logPath + " is not a directory");
    }

    try {
      return logPath.getCanonicalPath();
    } catch (IOException ioe) {
      throw new UncheckedIOException("Error getting canonical path for the logging directory", ioe);
    }
  }
}
