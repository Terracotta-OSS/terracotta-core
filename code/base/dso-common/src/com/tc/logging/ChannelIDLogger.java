/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.logging;

import com.tc.net.protocol.tcm.ChannelIDProvider;

public class ChannelIDLogger implements TCLogger {

  private final ChannelIDProvider cidp;
  private final TCLogger logger;

  public ChannelIDLogger(ChannelIDProvider channelIDProvider, TCLogger logger) {
    this.cidp = channelIDProvider;
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

  public void log(LogLevel level, Object message) {
    logger.log(level, msg(message));
  }

  public void log(LogLevel level, Object message, Throwable t) {
    logger.log(level, msg(message), t);
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
    return cidp.getChannelID() + ": " + msg;
  }

  public String getName() {
    return logger.getName();
  }

}
