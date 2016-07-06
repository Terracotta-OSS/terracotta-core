package com.tc.classloader;

import com.tc.logging.LogLevel;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLoggingService;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *
 * A {@link TCLoggingService} for testing logging
 * @author vmad
 */
public class TestLoggingService implements TCLoggingService {

  private final ConcurrentMap<String, TestTCLogger> loggerMappings = new ConcurrentHashMap<>();

  @Override
  public TestTCLogger getLogger(Class<?> className) {
    return getLogger(className.getName());
  }

  @Override
  public TestTCLogger getLogger(String className) {
    loggerMappings.putIfAbsent(className, new TestTCLogger(className));
    return loggerMappings.get(className);
  }

  @Override
  public TCLogger getTestingLogger(String name) {
    return getLogger(name);
  }

  @Override
  public TCLogger getConsoleLogger() {
    return getLogger("console");
  }

  @Override
  public TCLogger getOperatorEventLogger() {
    return getLogger("operator");
  }

  @Override
  public TCLogger getDumpLogger() {
    return getLogger("dump");
  }

  @Override
  public TCLogger getCustomerLogger(String name) {
    return getLogger("customer");
  }

  @Override
  public void setLogLocationAndType(URI location, int processType) {
    //no-op
  }

  /**
   * A {@link TCLogger} which stores logs in a list for later inspection
   */
  public static class TestTCLogger implements TCLogger {

    private final List<Object> LOGS = Collections.synchronizedList(new ArrayList());
    private final String loggerName;

    public TestTCLogger(String loggerName) {
      this.loggerName = loggerName;
    }

    @Override
    public void debug(Object message) {
      LOGS.add(message);
    }

    @Override
    public void debug(Object message, Throwable t) {
      debug(message + " : " + t);
    }

    @Override
    public void error(Object message) {
      LOGS.add(message);
    }

    @Override
    public void error(Object message, Throwable t) {
      error(message + " : " + t);
    }

    @Override
    public void fatal(Object message) {
      LOGS.add(message);
    }

    @Override
    public void fatal(Object message, Throwable t) {
      fatal(message + " : " + t);
    }

    @Override
    public void info(Object message) {
      LOGS.add(message);
    }

    @Override
    public void info(Object message, Throwable t) {
      info(message + " : " + t);
    }

    @Override
    public void warn(Object message) {
      LOGS.add(message);
    }

    @Override
    public void warn(Object message, Throwable t) {
      warn(message + " : " + t);
    }

    @Override
    public boolean isDebugEnabled() {
      return true;
    }

    @Override
    public boolean isInfoEnabled() {
      return true;
    }

    @Override
    public void setLevel(LogLevel level) {
      //no-op
    }

    @Override
    public LogLevel getLevel() {
      return null;
    }

    @Override
    public String getName() {
      return loggerName;
    }

    public List<Object> getLogs() {
      return Collections.unmodifiableList(LOGS);
    }
  }
}
