/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.logging;

import org.apache.log4j.Logger;

/**
 * An implementation of TCLogger that just delegates to a log4j Logger instance NOTE: This implementation differs from
 * log4j in at least one detail....When calling the various log methods (info, warn, etc..) that take a single
 * <code>Object</code> parameter (eg. <code>debug(Object message)</code>), if an instance of <code>Throwable</code> is
 * passed as the message paramater, the call will be translated to the <code>xxx(Object Message, Throwable t)</code>
 * signature
 * 
 * @author teck
 */
class TCLoggerImpl implements TCLogger {

  private final Logger logger;

  TCLoggerImpl(String name) {
    if (name == null) { throw new IllegalArgumentException("Logger name cannot be null"); }
    logger = Logger.getLogger(name);
  }

  Logger getLogger() {
    return logger;
  }

  public void debug(Object message) {
    if (message instanceof Throwable) {
      debug("Exception thrown", (Throwable) message);
    } else {
      logger.debug(message);
    }
  }

  public void debug(Object message, Throwable t) {
    logger.debug(message, t);
  }

  public void error(Object message) {
    if (message instanceof Throwable) {
      error("Exception thrown", (Throwable) message);
    } else {
      logger.error(message);
    }
  }

  public void error(Object message, Throwable t) {
    logger.error(message, t);
  }

  public void fatal(Object message) {
    if (message instanceof Throwable) {
      fatal("Exception thrown", (Throwable) message);
    } else {
      logger.fatal(message);
    }
  }

  public void fatal(Object message, Throwable t) {
    logger.fatal(message, t);
  }

  public void info(Object message) {
    if (message instanceof Throwable) {
      info("Exception thrown", (Throwable) message);
    } else {
      logger.info(message);
    }
  }

  public void info(Object message, Throwable t) {
    logger.info(message, t);
  }

  public void warn(Object message) {
    if (message instanceof Throwable) {
      warn("Exception thrown", (Throwable) message);
    } else {
      logger.warn(message);
    }
  }

  public void warn(Object message, Throwable t) {
    logger.warn(message, t);
  }

  public boolean isDebugEnabled() {
    return logger.isDebugEnabled();
  }

  public boolean isInfoEnabled() {
    return logger.isInfoEnabled();
  }

  public void setLevel(LogLevel level) {
    logger.setLevel(LogLevelImpl.toLog4JLevel(level));
  }

  public LogLevel getLevel() {
    return LogLevelImpl.fromLog4JLevel(logger.getLevel());
  }

  public String getName() {
    return logger.getName();
  }
}
