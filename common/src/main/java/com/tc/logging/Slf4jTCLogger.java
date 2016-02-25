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
import org.slf4j.LoggerFactory;

/**
 * @author Mathoeu Carbou
 */
class Slf4jTCLogger implements TCLogger {

  private final Logger logger;

  Slf4jTCLogger(String name) {
    if (name == null) { throw new IllegalArgumentException("Logger name cannot be null"); }
    logger = LoggerFactory.getLogger(name);
  }

  Logger getLogger() {
    return logger;
  }

  @Override
  public void debug(Object message) {
    if (message instanceof Throwable) {
      debug("Exception thrown", (Throwable) message);
    } else {
      logger.debug(String.valueOf(message));
    }
  }

  @Override
  public void debug(Object message, Throwable t) {
    logger.debug(String.valueOf(message), t);
  }

  @Override
  public void error(Object message) {
    if (message instanceof Throwable) {
      error("Exception thrown", (Throwable) message);
    } else {
      logger.error(String.valueOf(message));
    }
  }

  @Override
  public void error(Object message, Throwable t) {
    logger.error(String.valueOf(message), t);
  }

  @Override
  public void fatal(Object message) {
    error(message);
  }

  @Override
  public void fatal(Object message, Throwable t) {
    error(message, t);
  }

  @Override
  public void info(Object message) {
    if (message instanceof Throwable) {
      info("Exception thrown", (Throwable) message);
    } else {
      logger.info(String.valueOf(message));
    }
  }

  @Override
  public void info(Object message, Throwable t) {
    logger.info(String.valueOf(message), t);
  }

  @Override
  public void warn(Object message) {
    if (message instanceof Throwable) {
      warn("Exception thrown", (Throwable) message);
    } else {
      logger.warn(String.valueOf(message));
    }
  }

  @Override
  public void warn(Object message, Throwable t) {
    logger.warn(String.valueOf(message), t);
  }

  @Override
  public boolean isDebugEnabled() {
    return logger.isDebugEnabled();
  }

  @Override
  public boolean isInfoEnabled() {
    return logger.isInfoEnabled();
  }

  @Override
  public LogLevel getLevel() {
    if(logger.isTraceEnabled()) return LogLevelImpl.DEBUG;
    if(logger.isDebugEnabled()) return LogLevelImpl.DEBUG;
    if(logger.isInfoEnabled()) return LogLevelImpl.INFO;
    if(logger.isWarnEnabled()) return LogLevelImpl.WARN;
    if(logger.isErrorEnabled()) return LogLevelImpl.ERROR;
    return LogLevelImpl.OFF;
  }

  @Override
  public String getName() {
    return logger.getName();
  }
}
