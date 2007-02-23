/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.logging;

/*
 * Useful while developement Note:: This class is not synchronized
 */
public class LossyTCLogger implements TCLogger {

  public static final int DEFAULT_LOG_TIME_INTERVAL  = 5000; // 5 sec
  public static final int DEFAULT_LOG_COUNT_INTERVAL = 10000; // 10000 Messages once

  public static final int TIME_BASED                 = 0;
  public static final int COUNT_BASED                = 1;

  private final TCLogger  logger;
  private LogOrNot        decider;

  public LossyTCLogger(TCLogger logger) {
    this(logger, DEFAULT_LOG_TIME_INTERVAL);
  }

  public LossyTCLogger(TCLogger logger, int logInterval) {
    this(logger, logInterval, TIME_BASED);
  }

  public LossyTCLogger(TCLogger logger, int logInterval, int type) {
    this.logger = logger;
    if (type == DEFAULT_LOG_TIME_INTERVAL) {
      this.decider = new TimeBasedDecider(logInterval);
    } else {
      this.decider = new CountBasedDecider(logInterval);
    }
  }

  public void debug(Object message) {
    if (decider.canLog()) {
      logger.debug(message);
    }
  }

  public void debug(Object message, Throwable t) {
    if (decider.canLog()) {
      logger.debug(message, t);
    }

  }

  // XXX:: Maybe errors should always be logged
  public void error(Object message) {
    if (decider.canLog()) {
      logger.error(message);
    }
  }

  public void error(Object message, Throwable t) {
    if (decider.canLog()) {
      logger.error(message, t);
    }
  }

  public void fatal(Object message) {
    if (decider.canLog()) {
      logger.fatal(message);
    }
  }

  public void fatal(Object message, Throwable t) {
    if (decider.canLog()) {
      logger.fatal(message, t);
    }
  }

  public void info(Object message) {
    if (decider.canLog()) {
      logger.info(message);
    }
  }

  public void info(Object message, Throwable t) {
    if (decider.canLog()) {
      logger.info(message, t);
    }
  }

  public void warn(Object message) {
    if (decider.canLog()) {
      logger.warn(message);
    }
  }

  public void warn(Object message, Throwable t) {
    if (decider.canLog()) {
      logger.warn(message, t);
    }
  }

  public void log(LogLevel level, Object message) {
    if (decider.canLog()) {
      logger.log(level, message);
    }
  }

  public void log(LogLevel level, Object message, Throwable t) {
    if (decider.canLog()) {
      logger.log(level, message, t);
    }
  }

  public boolean isDebugEnabled() {
    return logger.isDebugEnabled();
  }

  public boolean isInfoEnabled() {
    return logger.isInfoEnabled();
  }

  public void setLevel(LogLevel level) {
    logger.setLevel(level);
  }

  public LogLevel getLevel() {
    return logger.getLevel();
  }

  public String getName() {
    return logger.getName();
  }

  interface LogOrNot {
    boolean canLog();
  }

  static class TimeBasedDecider implements LogOrNot {
    private int  logInterval;
    private long then;

    TimeBasedDecider(int logInterval) {
      this.logInterval = logInterval;
    }

    public boolean canLog() {
      long now = System.currentTimeMillis();
      if (now > (then + logInterval)) {
        then = now;
        return true;
      }
      return false;
    }
  }

  static class CountBasedDecider implements LogOrNot {
    private int logInterval;
    private int count;

    CountBasedDecider(int logInterval) {
      this.logInterval = logInterval;
    }

    public boolean canLog() {
      if (++count >= logInterval) {
        count = 0;
        return true;
      }
      return false;
    }
  }

}
