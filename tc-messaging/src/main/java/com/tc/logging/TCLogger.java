/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.logging;

/**
 * Common TC logger interface (mostly a copy of the log4j logger interface)
 * 
 * @author teck
 */
public interface TCLogger {
  void debug(Object message);

  void debug(Object message, Throwable t);

  void error(Object message);

  void error(Object message, Throwable t);

  void fatal(Object message);

  void fatal(Object message, Throwable t);

  void info(Object message);

  void info(Object message, Throwable t);

  void warn(Object message);

  void warn(Object message, Throwable t);

  boolean isDebugEnabled();

  boolean isInfoEnabled();

  void setLevel(LogLevel level);

  LogLevel getLevel();

  String getName();
}
