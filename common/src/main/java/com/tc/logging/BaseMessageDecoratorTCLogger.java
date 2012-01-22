/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.logging;

public abstract class BaseMessageDecoratorTCLogger implements TCLogger {

  private final TCLogger          logger;
  
  public BaseMessageDecoratorTCLogger(TCLogger logger) {
    this.logger = logger;
  }

  // Method base classes should implement
  protected abstract Object decorate(Object message);
  
  public String getName() {
    return logger.getName();
  }

  public void debug(Object message) {
    logger.debug(decorate(message));
  }

  public void debug(Object message, Throwable t) {
    logger.debug(decorate(message), t);
  }

  public void error(Object message) {
    logger.error(decorate(message));
  }

  public void error(Object message, Throwable t) {
    logger.error(decorate(message), t);
  }

  public void fatal(Object message) {
    logger.fatal(decorate(message));
  }

  public void fatal(Object message, Throwable t) {
    logger.fatal(decorate(message), t);
  }

  public void info(Object message) {
    logger.info(decorate(message));
  }

  public void info(Object message, Throwable t) {
    logger.info(decorate(message), t);
  }

  public void warn(Object message) {
    logger.warn(decorate(message));
  }

  public void warn(Object message, Throwable t) {
    logger.warn(decorate(message), t);
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
}
