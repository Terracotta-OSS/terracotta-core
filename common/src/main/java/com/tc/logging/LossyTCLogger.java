/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.logging;

import com.tc.util.Assert;

/*
 * Useful while development Note:: This class is not synchronized
 */
public class LossyTCLogger implements TCLogger {

  public static final long DEFAULT_LOG_TIME_INTERVAL  = 5000; // 5 sec
  public static final long DEFAULT_LOG_COUNT_INTERVAL = 10000; // 10000 Messages once

  public static enum LossyTCLoggerType {
    TIME_BASED, COUNT_BASED
  }

  private final TCLogger logger;
  private final String   lossyInfo;
  private LogOrNot       decider;

  // for testing
  private long           logCount = 0;

  /**
   * Creates a Time based Lossy Logger with default log time interval (5 seconds)
   */
  public LossyTCLogger(TCLogger logger) {
    this(logger, DEFAULT_LOG_TIME_INTERVAL);
  }

  /**
   * Creates a Time based Lossy Logger with logInterval log time interval
   */
  public LossyTCLogger(TCLogger logger, long logInterval) {
    this(logger, logInterval, LossyTCLoggerType.TIME_BASED);
  }

  /**
   * Creates a Time based or Count based LossyLogger with lossyLogOnlyIfSameContent set to false.
   */
  public LossyTCLogger(TCLogger logger, long logInterval, LossyTCLoggerType type) {
    this(logger, logInterval, type, false);
  }

  /**
   * Creates a Time based or Count based LossyLogger with all configured parameters
   */
  public LossyTCLogger(TCLogger logger, long logInterval, LossyTCLoggerType type, boolean lossyLogOnlyIfSameContent) {
    Assert.eval(logInterval > 0);
    this.logger = logger;
    if (type == LossyTCLoggerType.TIME_BASED) {
      this.decider = new TimeBasedDecider(logInterval, lossyLogOnlyIfSameContent);
    } else {
      this.decider = new CountBasedDecider(logInterval, lossyLogOnlyIfSameContent);
    }
    this.lossyInfo = " [lossy interval: " + logInterval + (type == LossyTCLoggerType.TIME_BASED ? "ms]" : "]");
    this.logCount = 0;
  }

  public void debug(Object message) {
    if (this.decider.canLog(message)) {
      this.logger.debug(message + this.lossyInfo);
    }
  }

  public void debug(Object message, Throwable t) {
    if (this.decider.canLog(message)) {
      this.logger.debug(message + this.lossyInfo, t);
    }

  }

  public void error(Object message) {
    // Errors are always logged
    this.logger.error(message);
  }

  public void error(Object message, Throwable t) {
    // Errors are always logged
    this.logger.error(message, t);
  }

  public void fatal(Object message) {
    // Fatal messages are always logged
    this.logger.fatal(message);
  }

  public void fatal(Object message, Throwable t) {
    // Fatal messages are always logged
    this.logger.fatal(message, t);
  }

  public void info(Object message) {
    if (this.decider.canLog(message)) {
      this.logger.info(message);
    }
  }

  public void info(Object message, Throwable t) {
    if (this.decider.canLog(message)) {
      this.logger.info(message, t);
    }
  }

  public void warn(Object message) {
    if (this.decider.canLog(message)) {
      this.logger.warn(message + this.lossyInfo);
    }
  }

  public void warn(Object message, Throwable t) {
    if (this.decider.canLog(message)) {
      this.logger.warn(message, t);
    }
  }

  public boolean isDebugEnabled() {
    return this.logger.isDebugEnabled();
  }

  public boolean isInfoEnabled() {
    return this.logger.isInfoEnabled();
  }

  public void setLevel(LogLevel level) {
    this.logger.setLevel(level);
  }

  public LogLevel getLevel() {
    return this.logger.getLevel();
  }

  public String getName() {
    return this.logger.getName();
  }

  /**
   * Returns according to the count or time based logging if a log entry will be printed or not, if a log method called
   * immediately. This is only best efforts and helps in avoiding creating unnecessary String for logging that wouldnt
   * be logged anyways. Note one should also test the log level along with this method to be sure that the following
   * call to log will actually log.
   * <p>
   * if(lossyLogger.isDebugEnabled() && lossyLogger.isLoggingEnabledNow()) lossyLogger.debug(message);
   * <p>
   */
  public boolean isLoggingEnabledNow() {
    return this.decider.isLoggingEnabledNow();
  }

  interface LogOrNot {
    boolean canLog(final Object message);

    boolean isLoggingEnabledNow();
  }

  private class TimeBasedDecider implements LogOrNot {
    private final long    timeInterval;
    private final boolean lossyLogOnlyIfSameContent;
    private long          then;
    private Object        prevMessage = null;

    TimeBasedDecider(long logInterval, boolean lossyLogOnlyIfSameContent) {
      this.timeInterval = logInterval;
      this.lossyLogOnlyIfSameContent = lossyLogOnlyIfSameContent;
    }

    public synchronized boolean canLog(final Object message) {
      long now = System.currentTimeMillis();

      if (this.lossyLogOnlyIfSameContent) {
        if ((this.prevMessage == null) || !(this.prevMessage.equals(message))) {
          this.prevMessage = message;
          this.then = System.currentTimeMillis();
          updateLogCount();
          return true;
        }
      }

      this.prevMessage = message;
      if (now > (this.then + this.timeInterval)) {
        this.then = now;
        updateLogCount();
        return true;
      }
      return false;
    }

    public boolean isLoggingEnabledNow() {
      return (System.currentTimeMillis() > (this.then + this.timeInterval));
    }
  }

  private class CountBasedDecider implements LogOrNot {
    private final long    countInterval;
    private final boolean lossyLogOnlyIfSameContent;
    private Object        prevMessage = null;
    private long          count;

    CountBasedDecider(long logInterval, boolean lossyLogOnlyIfSameContent) {
      this.countInterval = logInterval;
      this.lossyLogOnlyIfSameContent = lossyLogOnlyIfSameContent;
      this.count = 0;
    }

    public synchronized boolean canLog(final Object message) {
      if (this.lossyLogOnlyIfSameContent) {
        if ((this.prevMessage == null) || !(this.prevMessage.equals(message))) {
          this.prevMessage = message;
          this.count = 1;
          updateLogCount();
          return true;
        }
      }

      this.prevMessage = message;
      if (this.count++ % this.countInterval == 0) {
        this.count = this.count % this.countInterval;
        updateLogCount();
        return true;
      }
      return false;
    }

    public boolean isLoggingEnabledNow() {
      long remainder = (this.count % this.countInterval);
      if (remainder != 0) {
        // For count based decider, the act of calling into isLoggingEnabledNow() is counted as a log message coming our
        // way as returning just false means count will never be increased hence logging never incremented.
        this.count++;
        return false;
      }
      return true;
    }
  }

  // following methods are strictly for testing
  private synchronized void updateLogCount() {
    this.logCount++;
  }

  synchronized long getLogCount() {
    return this.logCount;
  }

}
