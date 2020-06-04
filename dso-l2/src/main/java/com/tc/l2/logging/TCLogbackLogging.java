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
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.util.FileSize;
import com.tc.logging.TCLogging;
import com.tc.util.Assert;
import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TCLogbackLogging {

  private static final String TC_PATTERN = "%d [%t] %p %c - %m%n";

  private static final Logger LOGGER = LoggerFactory.getLogger(TCLogbackLogging.class);

  public static void redirectLogging(File logDirFile) {
    String logDir = getPathString(logDirFile);
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    ch.qos.logback.classic.Logger internalLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);

    Appender<ILoggingEvent> appender = internalLogger.getAppender("TC_BASE");
    if (appender != null) {
      Appender<ILoggingEvent> continuingAppender = (logDir != null) ? installFileAppender(logDir, loggerContext) : consoleAppender(loggerContext);
      if (continuingAppender != null) {
        if (appender instanceof BufferingAppender) {
          BufferingAppender base = (BufferingAppender)appender;
          base.sendContentsTo(continuingAppender);
          internalLogger.addAppender(continuingAppender);
          internalLogger.detachAppender(base);
        } else {
          TCLogging.getConsoleLogger().warn("Appender named TC_BASE in the overridden logging configuration is not a BufferingAppender. Logging will proceed to both TC_BASE and {}", logDir);
          internalLogger.addAppender(continuingAppender);
        }
      } else {
        throw new IllegalStateException("continuing log appender cannot be null");
      }
    } else {
      TCLogging.getConsoleLogger().warn("Terracotta base logging configuration has been overridden. Log path provided in server config will be ignored.");
    }
  }

  private static Appender<ILoggingEvent> consoleAppender(LoggerContext loggerContext) {
    ch.qos.logback.classic.Logger internalLogger = loggerContext.getLogger("org.terracotta.console");
    Appender<ILoggingEvent> appender = null;
    if (internalLogger == null) {
      appender = internalLogger.getAppender("STDOUT");
    }
    return appender;
  }

  private static Appender<ILoggingEvent> installFileAppender(String logDir, LoggerContext loggerContext) {
    String logLocation = logDir + File.separator + "terracotta.server.log";
    TCLogging.getConsoleLogger().info("Log file: {}", logLocation);

    RollingFileAppender fileAppender = new RollingFileAppender();
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

    StartupAndSizeBasedTriggeringPolicy triggeringPolicy = new StartupAndSizeBasedTriggeringPolicy();
    triggeringPolicy.setMaxFileSize(FileSize.valueOf("512MB"));
    triggeringPolicy.setContext(loggerContext);
    triggeringPolicy.start();
    fileAppender.setTriggeringPolicy(triggeringPolicy);

    fileAppender.start();

    return fileAppender;
  }

  private static String getPathString(File logDir) {
    try {
      if (logDir == null) {
        LOGGER.info("Logging directory is not set.  Logging only to the console");
        return null;
      } else if (!logDir.exists()) {
        LOGGER.warn("Logging directory {} does not exist.  Logging only to the console", logDir);
        return null;
      } else if (!logDir.isDirectory()) {
        LOGGER.warn("Logging path {} is not a directory.  Logging only to the console", logDir);
        return null;
      } else {
        return logDir.getCanonicalPath();
      }
    } catch (IOException ioe) {
      LOGGER.warn("Error setting the logging directory.  Logging only to the console", ioe);
      return null;
    }
  }

}
