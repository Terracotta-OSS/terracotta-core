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
package com.tc.logging;

import org.slf4j.Logger;
import org.slf4j.Marker;

import com.tc.util.Assert;

/*
 * Useful while development Note:: This class is not synchronized
 */
public class LossyTCLogger implements Logger {

  public static final long DEFAULT_LOG_TIME_INTERVAL  = 5000; // 5 sec

  public static enum LossyTCLoggerType {
    TIME_BASED, COUNT_BASED
  }

  private final Logger logger;
  private final String   lossyInfo;
  private LogOrNot       decider;

  // for testing
  private long           logCount = 0;

  /**
   * Creates a Time based Lossy Logger with default log time interval (5 seconds)
   */
  public LossyTCLogger(Logger logger) {
    this(logger, DEFAULT_LOG_TIME_INTERVAL);
  }

  /**
   * Creates a Time based Lossy Logger with logInterval log time interval
   */
  public LossyTCLogger(Logger logger, long logInterval) {
    this(logger, logInterval, LossyTCLoggerType.TIME_BASED);
  }

  /**
   * Creates a Time based or Count based LossyLogger with lossyLogOnlyIfSameContent set to false.
   */
  public LossyTCLogger(Logger logger, long logInterval, LossyTCLoggerType type) {
    this(logger, logInterval, type, false);
  }

  /**
   * Creates a Time based or Count based LossyLogger with all configured parameters
   */
  public LossyTCLogger(Logger logger, long logInterval, LossyTCLoggerType type, boolean lossyLogOnlyIfSameContent) {
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

  @Override
  public boolean isDebugEnabled() {
    return this.logger.isDebugEnabled();
  }

  @Override
  public void debug(String s) {
    if (this.decider.canLog(s)) {
      this.logger.debug(s + this.lossyInfo);
    }
  }

  @Override
  public void debug(String s, Object o) {

  }

  @Override
  public void debug(String s, Object o, Object o1) {

  }

  @Override
  public void debug(String s, Object... objects) {

  }

  @Override
  public void debug(String s, Throwable throwable) {
    if (this.decider.canLog(s)) {
      this.logger.debug(s + this.lossyInfo, throwable);
    }
  }

  @Override
  public boolean isDebugEnabled(Marker marker) {
    return false;
  }

  @Override
  public void debug(Marker marker, String s) {

  }

  @Override
  public void debug(Marker marker, String s, Object o) {

  }

  @Override
  public void debug(Marker marker, String s, Object o, Object o1) {

  }

  @Override
  public void debug(Marker marker, String s, Object... objects) {

  }

  @Override
  public void debug(Marker marker, String s, Throwable throwable) {

  }

  @Override
  public boolean isInfoEnabled() {
    return this.logger.isInfoEnabled();
  }

  @Override
  public void info(String s) {
    if (this.decider.canLog(s)) {
      this.logger.info(s + this.lossyInfo);
    }
  }

  @Override
  public void info(String s, Object o) {

  }

  @Override
  public void info(String s, Object o, Object o1) {

  }

  @Override
  public void info(String s, Object... objects) {

  }

  @Override
  public void info(String s, Throwable throwable) {
    if (this.decider.canLog(s)) {
      this.logger.info(s + this.lossyInfo, throwable);
    }
  }

  @Override
  public boolean isInfoEnabled(Marker marker) {
    return false;
  }

  @Override
  public void info(Marker marker, String s) {

  }

  @Override
  public void info(Marker marker, String s, Object o) {

  }

  @Override
  public void info(Marker marker, String s, Object o, Object o1) {

  }

  @Override
  public void info(Marker marker, String s, Object... objects) {

  }

  @Override
  public void info(Marker marker, String s, Throwable throwable) {

  }

  @Override
  public boolean isWarnEnabled() {
    return logger.isWarnEnabled();
  }

  @Override
  public void warn(String s) {
    if (this.decider.canLog(s)) {
      this.logger.warn(s + this.lossyInfo);
    }
  }

  @Override
  public void warn(String s, Object o) {

  }

  @Override
  public void warn(String s, Object... objects) {

  }

  @Override
  public void warn(String s, Object o, Object o1) {

  }

  @Override
  public void warn(String s, Throwable throwable) {
    if (this.decider.canLog(s)) {
      this.logger.warn(s + this.lossyInfo, throwable);
    }
  }

  @Override
  public boolean isWarnEnabled(Marker marker) {
    return false;
  }

  @Override
  public void warn(Marker marker, String s) {

  }

  @Override
  public void warn(Marker marker, String s, Object o) {

  }

  @Override
  public void warn(Marker marker, String s, Object o, Object o1) {

  }

  @Override
  public void warn(Marker marker, String s, Object... objects) {

  }

  @Override
  public void warn(Marker marker, String s, Throwable throwable) {

  }

  @Override
  public boolean isErrorEnabled() {
    return false;
  }

  @Override
  public void error(String s) {
    // Errors are always logged
    this.logger.error(s);
  }

  @Override
  public void error(String s, Object o) {

  }

  @Override
  public void error(String s, Object o, Object o1) {

  }

  @Override
  public void error(String s, Object... objects) {

  }

  @Override
  public void error(String s, Throwable throwable) {
    // Errors are always logged
    this.logger.error(s, throwable);
  }

  @Override
  public boolean isErrorEnabled(Marker marker) {
    return false;
  }

  @Override
  public void error(Marker marker, String s) {

  }

  @Override
  public void error(Marker marker, String s, Object o) {

  }

  @Override
  public void error(Marker marker, String s, Object o, Object o1) {

  }

  @Override
  public void error(Marker marker, String s, Object... objects) {

  }

  @Override
  public void error(Marker marker, String s, Throwable throwable) {

  }

  @Override
  public String getName() {
    return this.logger.getName();
  }

  @Override
  public boolean isTraceEnabled() {
    return false;
  }

  @Override
  public void trace(String s) {

  }

  @Override
  public void trace(String s, Object o) {

  }

  @Override
  public void trace(String s, Object o, Object o1) {

  }

  @Override
  public void trace(String s, Object... objects) {

  }

  @Override
  public void trace(String s, Throwable throwable) {

  }

  @Override
  public boolean isTraceEnabled(Marker marker) {
    return false;
  }

  @Override
  public void trace(Marker marker, String s) {

  }

  @Override
  public void trace(Marker marker, String s, Object o) {

  }

  @Override
  public void trace(Marker marker, String s, Object o, Object o1) {

  }

  @Override
  public void trace(Marker marker, String s, Object... objects) {

  }

  @Override
  public void trace(Marker marker, String s, Throwable throwable) {

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
    boolean canLog(Object message);

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

    @Override
    public synchronized boolean canLog(Object message) {
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

    @Override
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

    @Override
    public synchronized boolean canLog(Object message) {
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

    @Override
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
