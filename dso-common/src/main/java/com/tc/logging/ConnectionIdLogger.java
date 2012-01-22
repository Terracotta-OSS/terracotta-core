/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.logging;

public class ConnectionIdLogger implements TCLogger {

  private final ConnectionIDProvider cidp;
  private final TCLogger             logger;

  public ConnectionIdLogger(ConnectionIDProvider connectionIDProvider, TCLogger logger) {
    this.cidp = connectionIDProvider;
    this.logger = logger;
  }

  public void debug(Object message) {
    logger.debug(msg(message));
  }

  public void debug(Object message, Throwable t) {
    logger.debug(msg(message), t);
  }

  public void error(Object message) {
    logger.error(msg(message));
  }

  public void error(Object message, Throwable t) {
    logger.error(msg(message), t);
  }

  public void fatal(Object message) {
    logger.fatal(msg(message));
  }

  public void fatal(Object message, Throwable t) {
    logger.fatal(msg(message), t);
  }

  public void info(Object message) {
    logger.info(msg(message));
  }

  public void info(Object message, Throwable t) {
    logger.info(msg(message), t);
  }

  public void warn(Object message) {
    logger.warn(msg(message));
  }

  public void warn(Object message, Throwable t) {
    logger.warn(msg(message), t);
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

  private Object msg(Object msg) {
    return cidp.getConnectionId() + ": " + msg;
  }

  public String getName() {
    return logger.getName();
  }

}
