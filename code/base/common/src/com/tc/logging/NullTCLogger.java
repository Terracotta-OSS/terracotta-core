/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.logging;

import com.tc.exception.ImplementMe;

/**
 * @author steve
 */
public class NullTCLogger implements TCLogger {

  public void debug(Object message) {
  //
  }

  public void debug(Object message, Throwable t) {
  //
  }

  public void error(Object message) {
  //
  }

  public void error(Object message, Throwable t) {
  //
  }

  public void fatal(Object message) {
  //
  }

  public void fatal(Object message, Throwable t) {
  //
  }

  public void info(Object message) {
  //
  }

  public void info(Object message, Throwable t) {
  //
  }

  public void warn(Object message) {
  //
  }

  public void warn(Object message, Throwable t) {
  //
  }

  public boolean isDebugEnabled() {
    return false;
  }

  public boolean isInfoEnabled() {
    return false;
  }

  public void log(LogLevel level, Object message) {
  //
  }

  public void log(LogLevel level, Object message, Throwable t) {
  //
  }

  public void setLevel(LogLevel level) {
  //
  }

  public LogLevel getLevel() {
    if (true) throw new ImplementMe();
    return null;
  }

  public String getName() {
    return "";
  }

}