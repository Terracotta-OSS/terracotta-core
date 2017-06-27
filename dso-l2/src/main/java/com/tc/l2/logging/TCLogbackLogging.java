package com.tc.l2.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TCLogbackLogging {

  private static final String TC_PATTERN = "%d [%t] %p %c - %m%n";

  private static final Logger LOGGER = LoggerFactory.getLogger(TCLogbackLogging.class);

  public static void redirectLogging(String logDir) {
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    ch.qos.logback.classic.Logger internalLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
    Appender<ILoggingEvent> appender = internalLogger.getAppender("TC_BASE");
    if (appender != null) {
      String logLocation = logDir + "/terracotta.server.log";
      LOGGER.info("Log file: {}", logLocation);

      RollingFileAppender fileAppender = new RollingFileAppender();
      fileAppender.setName("ROLLING");
      fileAppender.setContext(loggerContext);
      fileAppender.setFile(logDir + "/terracotta.server.log");

      PatternLayoutEncoder logEncoder = new PatternLayoutEncoder();
      logEncoder.setContext(loggerContext);
      logEncoder.setPattern(TC_PATTERN);
      logEncoder.start();

      fileAppender.setEncoder(logEncoder);

      FixedWindowRollingPolicy rollingPolicy = new FixedWindowRollingPolicy();
      rollingPolicy.setMinIndex(1);
      rollingPolicy.setMaxIndex(20);
      rollingPolicy.setFileNamePattern(logDir + "/terracotta.server.%i.log");
      rollingPolicy.setContext(loggerContext);
      rollingPolicy.setParent(fileAppender);
      rollingPolicy.start();
      fileAppender.setRollingPolicy(rollingPolicy);

      StartupAndSizeBasedTriggeringPolicy triggeringPolicy = new StartupAndSizeBasedTriggeringPolicy();
      triggeringPolicy.setMaxFileSize("512MB");
      triggeringPolicy.setContext(loggerContext);
      triggeringPolicy.start();
      fileAppender.setTriggeringPolicy(triggeringPolicy);

      fileAppender.start();

      if (appender instanceof BufferingAppender) {
        BufferingAppender<ILoggingEvent> base = (BufferingAppender) appender;
        base.sendContentsTo(fileAppender);
        internalLogger.addAppender(fileAppender);
        internalLogger.detachAppender(base);
      } else {
        LOGGER.warn("Appender named TC_BASE in the overridden logging configuration is not a BufferingAppender. Logging will proceed to both TC_BASE and {}", logLocation);
        internalLogger.addAppender(fileAppender);
      }
    } else {
      LOGGER.warn("Terracotta base logging configuration has been overridden. Log path provided in server config will be ignored.");
    }
  }

}
