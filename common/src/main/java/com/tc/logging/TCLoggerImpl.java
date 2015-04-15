/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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

  @Override
  public void debug(Object message) {
    if (message instanceof Throwable) {
      debug("Exception thrown", (Throwable) message);
    } else {
      logger.debug(message);
    }
  }

  @Override
  public void debug(Object message, Throwable t) {
    logger.debug(message, t);
  }

  @Override
  public void error(Object message) {
    if (message instanceof Throwable) {
      error("Exception thrown", (Throwable) message);
    } else {
      logger.error(message);
    }
  }

  @Override
  public void error(Object message, Throwable t) {
    logger.error(message, t);
  }

  @Override
  public void fatal(Object message) {
    if (message instanceof Throwable) {
      fatal("Exception thrown", (Throwable) message);
    } else {
      logger.fatal(message);
    }
  }

  @Override
  public void fatal(Object message, Throwable t) {
    logger.fatal(message, t);
  }

  @Override
  public void info(Object message) {
    if (message instanceof Throwable) {
      info("Exception thrown", (Throwable) message);
    } else {
      logger.info(message);
    }
  }

  @Override
  public void info(Object message, Throwable t) {
    logger.info(message, t);
  }

  @Override
  public void warn(Object message) {
    if (message instanceof Throwable) {
      warn("Exception thrown", (Throwable) message);
    } else {
      logger.warn(message);
    }
  }

  @Override
  public void warn(Object message, Throwable t) {
    logger.warn(message, t);
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
  public void setLevel(LogLevel level) {
    logger.setLevel(LogLevelImpl.toLog4JLevel(level));
  }

  @Override
  public LogLevel getLevel() {
    return LogLevelImpl.fromLog4JLevel(logger.getLevel());
  }

  @Override
  public String getName() {
    return logger.getName();
  }
}
