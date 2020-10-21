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
package com.tc.l2.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.util.FileSize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.tripwire.EventAppender;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Iterator;

public class TCLogbackLogging {

  public static final String CONSOLE = "org.terracotta.console";
  private static final String TC_PATTERN = "%d [%t] %p %c - %m%n";
  private static final Logger LOGGER = LoggerFactory.getLogger(CONSOLE);

  public static void resetLogging() {
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    ch.qos.logback.classic.Logger root = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
    ch.qos.logback.classic.Logger console = loggerContext.getLogger(CONSOLE);
//    Iterator<Appender<ILoggingEvent>> appenders = root.iteratorForAppenders();
//    while (appenders.hasNext()) {
//      Appender<ILoggingEvent> a = appenders.next();
//      if (a instanceof BufferingAppender) {
//        root.detachAppender(a);
//        a.stop();
//      }
//    }
    root.detachAndStopAllAppenders();
    console.detachAndStopAllAppenders();
    loggerContext.reset();
  }

  public static void setServerName(String name) {
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    loggerContext.setName(name);
  }

  public static void bootstrapLogging() {
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    ch.qos.logback.classic.Logger root = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
    ch.qos.logback.classic.Logger console = loggerContext.getLogger(CONSOLE);

    Iterator<Appender<ILoggingEvent>> appenders = root.iteratorForAppenders();
    boolean hasBuffer = false;
    boolean hasJfr = false;
    while (appenders.hasNext()) {
      Appender<ILoggingEvent> check = appenders.next();
      if (check instanceof BufferingAppender) {
        hasBuffer = true;
      } else if (check instanceof EventAppender) {
        hasJfr = true;
      }
    }
    if (!hasBuffer) {
      BufferingAppender<ILoggingEvent> appender = new BufferingAppender<>();
      appender.setName("TC_BASE");
      appender.setContext(loggerContext);
      appender.setTarget("System.out");
      PatternLayoutEncoder encoder = new PatternLayoutEncoder();
      encoder.setContext(loggerContext);
      encoder.setPattern("%d [%t] %p %c - %m%n");
      encoder.start();
      appender.setEncoder(encoder);
      appender.start();
      root.addAppender(appender);
    }
    if (!hasJfr) {
      EventAppender events = new EventAppender();
      events.setName("LogToJFR");
      events.setContext(loggerContext);
      events.start();
      root.addAppender(events);
    }
    if (!console.iteratorForAppenders().hasNext()) {
      attachConsoleLogger(loggerContext, console);
    }
  }

  public static void redirectLogging(File logDirFile) {
    String logDir = getPathString(logDirFile);
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    ch.qos.logback.classic.Logger root = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
    ch.qos.logback.classic.Logger console = loggerContext.getLogger(CONSOLE);

    Iterator<Appender<ILoggingEvent>> appenders = root.iteratorForAppenders();
    if (appenders != null) {
      if (logDir != null) {
        Appender<ILoggingEvent> continuingAppender = installFileAppender(logDir, loggerContext);
        while (appenders.hasNext()) {
          Appender<ILoggingEvent> current = appenders.next();
          if (current instanceof BufferingAppender) {
            root.detachAppender(current);
            current.stop();
            ((BufferingAppender<ILoggingEvent>) current).sendContentsTo(continuingAppender);
            root.addAppender(continuingAppender);
          }
        }
      } else {
        while (appenders.hasNext()) {
          Appender<ILoggingEvent> current = appenders.next();
          if (current instanceof BufferingAppender) {
            ((BufferingAppender<ILoggingEvent>) current).disableBuffering();
            console.detachAndStopAllAppenders();
          }
        }
      }
    } else {
      LOGGER.warn("Terracotta base logging configuration has been overridden. Log path provided in server config will be ignored.");
    }
  }

  private static void attachConsoleLogger(LoggerContext cxt, ch.qos.logback.classic.Logger console) {
    ConsoleAppender<ILoggingEvent> append = new ConsoleAppender<>();
    append.setContext(cxt);
    append.setName("STDOUT");
    append.setTarget("System.out");
    PatternLayoutEncoder encoder = new PatternLayoutEncoder();
    encoder.setContext(cxt);
    encoder.setParent(append);
    encoder.setPattern("%d %p - %m%n");
    encoder.start();
    append.setEncoder(encoder);
    append.start();
    console.addAppender(append);
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
