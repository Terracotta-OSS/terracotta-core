package com.tc.l2.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.logging.TCLogging;
import com.tc.util.ProductInfo;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class TCLogbackLogging {

  private static final Logger LOGGER = LoggerFactory.getLogger(TCLogbackLogging.class);
  private static final Logger CONSOLE_LOGGER = TCLogging.getConsoleLogger();

  public static void initLogging() {
    writeVersion();
    writePID();
  }

  public static void redirectLogging(String logDir) {
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    ch.qos.logback.classic.Logger internalLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
    Appender<ILoggingEvent> appender = internalLogger.getAppender("TC_BASE");
    if (appender != null) {
      String logLocation = logDir + "/terracotta.server.log";
      LOGGER.info("Log file: {}", logLocation);
      BufferingAppender<ILoggingEvent> base = (BufferingAppender) appender;

      RollingFileAppender fileAppender = new RollingFileAppender();
      fileAppender.setName("ROLLING");
      fileAppender.setContext(loggerContext);
      fileAppender.setFile(logDir + "/terracotta.server.log");

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

      fileAppender.setEncoder(base.getEncoder());
      fileAppender.start();
      base.stopAndSendContentsTo(fileAppender);
      internalLogger.addAppender(fileAppender);
      internalLogger.detachAppender(base);

      loggerContext.getLogger(TCLogging.CONSOLE_LOGGER_NAME).setAdditive(true);
    } else {
      LOGGER.warn("Terracotta base logging configuration has been overridden. Log path provided in server config will be ignored.");
    }

    writeSystemProperties();
  }

  private static void writeVersion() {
    ProductInfo info = ProductInfo.getInstance();

    // Write build info always
    String longProductString = info.toLongString();
    CONSOLE_LOGGER.info(longProductString);

    // Write patch info, if any
    if (info.isPatched()) {
      String longPatchString = info.toLongPatchString();
      CONSOLE_LOGGER.info(longPatchString);
    }
  }

  private static void writePID() {
    try {
      String processName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
      long pid = Long.parseLong(processName.split("@")[0]);
      CONSOLE_LOGGER.info("PID is {}", pid);
    } catch (Throwable t) {
      // ignore, not fatal if this doesn't work for some reason
    }
  }

  private static void writeSystemProperties() {
    try {
      Properties properties = System.getProperties();
      int maxKeyLength = 1;

      List<String> keys = new ArrayList<String>();
      for (Map.Entry<Object, Object> entry : properties.entrySet()) {
        Object objKey = entry.getKey();
        Object objValue = entry.getValue();

        // Filter out any bad non-String keys or values in system properties
        if (objKey instanceof String && objValue instanceof String) {
          String key = (String) objKey;
          keys.add(key);
          maxKeyLength = Math.max(maxKeyLength, key.length());
        }
      }

      String inputArguments = null;
      try {
        RuntimeMXBean mxbean = ManagementFactory.getRuntimeMXBean();
        inputArguments = mxbean.getInputArguments().toString();
      } catch (SecurityException se) {
        inputArguments = "unknown";
      }
      String nl = System.getProperty("line.separator");
      StringBuffer data = new StringBuffer();
      data.append("All Java System Properties for this Terracotta instance:");
      data.append(nl);
      data.append("========================================================================");
      data.append(nl);
      data.append("JVM arguments: " + inputArguments);
      data.append(nl);

      String[] sortedKeys = keys.toArray(new String[keys.size()]);
      Arrays.sort(sortedKeys);
      for (String key : sortedKeys) {
        data.append(key);
        for (int i = 0; i < maxKeyLength - key.length(); i++) {
          data.append(' ');
        }
        data.append(" : ");
        data.append(properties.get(key));
        data.append(nl);
      }
      data.append("========================================================================");

      LoggerFactory.getLogger(TCLogbackLogging.class).info(data.toString());
    } catch (Throwable t) {
      // don't let exceptions here be fatal
      t.printStackTrace();
    }
  }
}
