/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit;

import org.terracotta.toolkit.internal.ToolkitLogger;

import com.tc.logging.TCLogger;
import com.tc.object.bytecode.ManagerUtil;

public class TerracottaLogger implements ToolkitLogger {
  private final static String TOOLKIT_LOGGER_NAMESPACE = "org.terracotta.toolkit";

  private final TCLogger      tclogger;

  /**
   * Creates a TerracottaLogger implementation whose name ends with the given string.
   * <p>
   * The supplied name is prefixed with a fixed value to ensure toolkit loggers are confined within a namespace.
   * 
   * @param name logger identifier
   * @throws NullPointerException if the supplied name is null
   */
  public TerracottaLogger(String name) {
    if (null == name) { throw new NullPointerException("Logger name can't be null"); }

    StringBuilder fullName = new StringBuilder(TOOLKIT_LOGGER_NAMESPACE);
    if (!name.startsWith(".")) {
      fullName.append(".");
    }
    fullName.append(name);
    tclogger = ManagerUtil.getLogger(name);
  }

  public TerracottaLogger(Class klass) {
    this(klass.getName());
  }

  public void debug(Object message, Throwable t) {
    tclogger.debug(message, t);
  }

  public void debug(Object message) {
    tclogger.debug(message);
  }

  public void info(Object message, Throwable t) {
    tclogger.info(message, t);
  }

  public void info(Object message) {
    tclogger.info(message);
  }

  public void warn(Object message, Throwable t) {
    tclogger.warn(message, t);
  }

  public void warn(Object message) {
    tclogger.warn(message);
  }

  public void error(Object message, Throwable t) {
    tclogger.error(message, t);
  }

  public void error(Object message) {
    tclogger.error(message);
  }

  public void fatal(Object message, Throwable t) {
    tclogger.fatal(message, t);
  }

  public void fatal(Object message) {
    tclogger.fatal(message);
  }

  public String getName() {
    return tclogger.getName();
  }

  public boolean isDebugEnabled() {
    return tclogger.isDebugEnabled();
  }

  public boolean isInfoEnabled() {
    return tclogger.isInfoEnabled();
  }

}
