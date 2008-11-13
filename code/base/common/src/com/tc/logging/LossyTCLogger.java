/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.logging;

import com.tc.util.Assert;

/*
 * Useful while developement Note:: This class is not synchronized
 */
public class LossyTCLogger implements TCLogger {

  public static final int DEFAULT_LOG_TIME_INTERVAL  = 5000; // 5 sec
  public static final int DEFAULT_LOG_COUNT_INTERVAL = 10000; // 10000 Messages once

  public static final int TIME_BASED                 = 0;
  public static final int COUNT_BASED                = 1;

  private final TCLogger  logger;
  private final String    lossyInfo;
  private LogOrNot        decider;

  // for testing
  private long            logCount                   = 0;

  public LossyTCLogger(TCLogger logger) {
    this(logger, DEFAULT_LOG_TIME_INTERVAL);
  }

  public LossyTCLogger(TCLogger logger, int logInterval) {
    this(logger, logInterval, TIME_BASED);
  }

  public LossyTCLogger(TCLogger logger, int logInterval, int type) {
    this(logger, logInterval, type, false);
  }

  public LossyTCLogger(TCLogger logger, int logInterval, int type, boolean lossyLogOnlyIfSameContent) {
    Assert.eval(logInterval > 0);
    this.logger = logger;
    if (type == TIME_BASED) {
      this.decider = new TimeBasedDecider(logInterval, lossyLogOnlyIfSameContent);
    } else {
      this.decider = new CountBasedDecider(logInterval, lossyLogOnlyIfSameContent);
    }
    this.lossyInfo = " [lossy interval: " + logInterval + (type == TIME_BASED ? "ms]" : "]");
    this.logCount = 0;
  }

  public void debug(Object message) {
    if (decider.canLog(message)) {
      logger.debug(message + lossyInfo);
    }
  }

  public void debug(Object message, Throwable t) {
    if (decider.canLog(message)) {
      logger.debug(message + lossyInfo, t);
    }

  }

  // XXX:: Maybe errors should always be logged
  public void error(Object message) {
    if (decider.canLog(message)) {
      logger.error(message);
    }
  }

  public void error(Object message, Throwable t) {
    if (decider.canLog(message)) {
      logger.error(message, t);
    }
  }

  public void fatal(Object message) {
    if (decider.canLog(message)) {
      logger.fatal(message);
    }
  }

  public void fatal(Object message, Throwable t) {
    if (decider.canLog(message)) {
      logger.fatal(message, t);
    }
  }

  public void info(Object message) {
    if (decider.canLog(message)) {
      logger.info(message);
    }
  }

  public void info(Object message, Throwable t) {
    if (decider.canLog(message)) {
      logger.info(message, t);
    }
  }

  public void warn(Object message) {
    if (decider.canLog(message)) {
      logger.warn(message + lossyInfo);
    }
  }

  public void warn(Object message, Throwable t) {
    if (decider.canLog(message)) {
      logger.warn(message, t);
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
    boolean canLog(final Object message);
  }

  private class TimeBasedDecider implements LogOrNot {
    private final int     timeInterval;
    private final boolean lossyLogOnlyIfSameContent;
    private long          then;
    private Object        prevMessage = null;

    TimeBasedDecider(int logInterval, boolean lossyLogOnlyIfSameContent) {
      this.timeInterval = logInterval;
      this.lossyLogOnlyIfSameContent = lossyLogOnlyIfSameContent;
    }

    public synchronized boolean canLog(final Object message) {
      long now = System.currentTimeMillis();

      if (lossyLogOnlyIfSameContent) {
        if ((prevMessage == null) || !(prevMessage.equals(message))) {
          prevMessage = message;
          then = System.currentTimeMillis();
          updateLogCount();
          return true;
        }
        Assert.assertEquals(message, prevMessage);
      }

      prevMessage = message;
      if (now > (then + timeInterval)) {
        then = now;
        updateLogCount();
        return true;
      }
      return false;
    }
  }

  private class CountBasedDecider implements LogOrNot {
    private final int     countInterval;
    private final boolean lossyLogOnlyIfSameContent;
    private Object        prevMessage = null;
    private int           count;

    CountBasedDecider(int logInterval, boolean lossyLogOnlyIfSameContent) {
      this.countInterval = logInterval;
      this.lossyLogOnlyIfSameContent = lossyLogOnlyIfSameContent;
      this.count = 0;
    }

    public synchronized boolean canLog(final Object message) {
      if (lossyLogOnlyIfSameContent) {
        if ((prevMessage == null) || !(prevMessage.equals(message))) {
          prevMessage = message;
          count = 1;
          updateLogCount();
          return true;
        }
        Assert.assertEquals(message, prevMessage);
      }

      prevMessage = message;
      if (count++ % countInterval == 0) {
        count = count % countInterval;
        updateLogCount();
        return true;
      }
      return false;
    }
  }

  // following methods are strictly for testing
  private synchronized void updateLogCount() {
    logCount++;
  }

  synchronized long getLogCount() {
    return logCount;
  }

}
